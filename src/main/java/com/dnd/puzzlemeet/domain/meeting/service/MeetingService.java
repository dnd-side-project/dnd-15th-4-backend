package com.dnd.puzzlemeet.domain.meeting.service;

import com.dnd.puzzlemeet.domain.meeting.dto.MeetingCreateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingCreateResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingJoinRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingJoinResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingListResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingPreviewRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingPreviewResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingUpdateRequest;
import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRole;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingRepository;
import com.dnd.puzzlemeet.domain.puzzle.entity.MemberImage;
import com.dnd.puzzlemeet.domain.puzzle.repository.MemberImageRepository;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.s3.AmazonS3Manager;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingService {

  private static final int ARRIVAL_RADIUS_M = 50;
  private static final int INVITE_CODE_LENGTH = 8;
  private static final String INVITE_CODE_CHARS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private static final String DEFAULT_MEMBER_IMAGE_URL =
      "https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/puzzles/_+(9)+4.png";

  private static final SecureRandom RANDOM = new SecureRandom();

  private static final Comparator<Meeting> MEETING_LIST_ORDER =
      (m1, m2) -> {
        boolean completed1 = m1.getStatus() == MeetingStatus.COMPLETED;
        boolean completed2 = m2.getStatus() == MeetingStatus.COMPLETED;
        if (completed1 != completed2) {
          return completed1 ? 1 : -1;
        }
        return completed1
            ? m2.getMeetingAt().compareTo(m1.getMeetingAt())
            : m1.getMeetingAt().compareTo(m2.getMeetingAt());
      };

  private final MeetingRepository meetingRepository;
  private final MeetingMemberRepository meetingMemberRepository;
  private final MemberImageRepository memberImageRepository;
  private final UserRepository userRepository;
  private final AmazonS3Manager amazonS3Manager;

  @Transactional
  public MeetingCreateResponse createMeeting(
      Long userId, MeetingCreateRequest request, MultipartFile image) {
    User host =
        userRepository
            .findById(userId)
            .orElseThrow(() -> ApiException.of(ErrorCode.USER_NOT_FOUND));

    Meeting meeting =
        new Meeting(
            host,
            request.title(),
            request.dateTime(),
            request.destination(),
            null,
            BigDecimal.valueOf(request.latitude()),
            BigDecimal.valueOf(request.longitude()),
            ARRIVAL_RADIUS_M,
            generateInviteCode(),
            request.memo());
    meetingRepository.save(meeting);

    registerMember(meeting, host, MeetingMemberRole.HOST, request.nickname(), image);

    return MeetingCreateResponse.from(meeting);
  }

  @Transactional(readOnly = true)
  public MeetingPreviewResponse previewMeeting(MeetingPreviewRequest request) {
    Meeting meeting =
        meetingRepository
            .findByInviteCode(request.inviteCode())
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_INVITE_CODE_INVALID));

    if (meeting.getStatus() != MeetingStatus.WAITING) {
      throw ApiException.of(ErrorCode.MEETING_INVITE_CODE_INVALID);
    }

    List<MeetingMember> members =
        meetingMemberRepository.findAllByMeetingIdInFetchUser(List.of(meeting.getId()));

    return MeetingPreviewResponse.from(meeting, members);
  }

  @Transactional
  public MeetingJoinResponse joinMeeting(
      Long userId, MeetingJoinRequest request, MultipartFile image) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> ApiException.of(ErrorCode.USER_NOT_FOUND));

    Meeting meeting =
        meetingRepository
            .findByInviteCode(request.inviteCode())
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_INVITE_CODE_INVALID));

    if (meeting.getStatus() != MeetingStatus.WAITING) {
      throw ApiException.of(ErrorCode.MEETING_NOT_JOINABLE);
    }

    if (meetingMemberRepository.existsByMeetingIdAndUserId(meeting.getId(), userId)) {
      throw ApiException.of(ErrorCode.MEETING_MEMBER_ALREADY_JOINED);
    }

    registerMember(meeting, user, MeetingMemberRole.GUEST, request.nickname(), image);

    return MeetingJoinResponse.from(meeting);
  }

  @Transactional(readOnly = true)
  public List<MeetingListResponse> getMeetings(Long userId, MeetingStatus status) {
    List<Meeting> meetings =
        meetingRepository.findAllByParticipantUserIdAndStatus(userId, status).stream()
            .sorted(MEETING_LIST_ORDER)
            .toList();

    if (meetings.isEmpty()) {
      return List.of();
    }

    List<Long> meetingIds = meetings.stream().map(Meeting::getId).toList();
    Map<Long, List<MeetingMember>> membersByMeetingId =
        meetingMemberRepository.findAllByMeetingIdInFetchUser(meetingIds).stream()
            .collect(Collectors.groupingBy(mm -> mm.getMeeting().getId()));

    return meetings.stream()
        .map(meeting -> MeetingListResponse.from(meeting, membersByMeetingId.get(meeting.getId())))
        .toList();
  }

  @Transactional
  public void updateMeeting(Long userId, Long meetingId, MeetingUpdateRequest request) {
    Meeting meeting =
        meetingRepository
            .findById(meetingId)
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_NOT_FOUND));

    if (!meeting.getHostUser().getId().equals(userId)) {
      throw ApiException.of(ErrorCode.AUTH_FORBIDDEN);
    }

    if (meeting.getStatus() != MeetingStatus.WAITING) {
      throw ApiException.of(ErrorCode.MEETING_NOT_WAITING);
    }

    if ((request.latitude() == null) != (request.longitude() == null)) {
      throw ApiException.of(ErrorCode.INVALID_INPUT_VALUE);
    }

    String title = request.title() != null ? request.title() : meeting.getTitle();
    LocalDateTime meetingAt =
        request.dateTime() != null ? request.dateTime() : meeting.getMeetingAt();
    String destination =
        request.destination() != null ? request.destination() : meeting.getDestinationName();
    BigDecimal latitude =
        request.latitude() != null
            ? BigDecimal.valueOf(request.latitude())
            : meeting.getDestinationLatitude();
    BigDecimal longitude =
        request.longitude() != null
            ? BigDecimal.valueOf(request.longitude())
            : meeting.getDestinationLongitude();
    String memo = request.memo() != null ? request.memo() : meeting.getMemo();

    meeting.updateDetails(title, meetingAt, destination, latitude, longitude, memo);
  }

  @Transactional
  public void cancelMeeting(Long userId, Long meetingId) {
    Meeting meeting =
        meetingRepository
            .findById(meetingId)
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_NOT_FOUND));

    if (!meeting.getHostUser().getId().equals(userId)) {
      throw ApiException.of(ErrorCode.AUTH_FORBIDDEN);
    }

    if (meeting.getStatus() != MeetingStatus.WAITING) {
      throw ApiException.of(ErrorCode.MEETING_NOT_WAITING);
    }

    meeting.cancel();
  }

  private void registerMember(
      Meeting meeting, User user, MeetingMemberRole role, String nickname, MultipartFile image) {
    String resolvedNickname = nickname != null ? nickname : user.getNickname();
    MeetingMember member = new MeetingMember(meeting, user, role, resolvedNickname);
    meetingMemberRepository.save(member);

    boolean hasImage = image != null && !image.isEmpty();
    String imageUrl = hasImage ? uploadMemberImage(image) : DEFAULT_MEMBER_IMAGE_URL;
    memberImageRepository.save(new MemberImage(member, imageUrl, !hasImage));
  }

  private String uploadMemberImage(MultipartFile image) {
    String keyName = amazonS3Manager.generatePuzzleKeyName(UUID.randomUUID());
    return amazonS3Manager.uploadFile(keyName, image);
  }

  private String generateInviteCode() {
    String code;
    do {
      code = randomInviteCode();
    } while (meetingRepository.existsByInviteCode(code));
    return code;
  }

  private String randomInviteCode() {
    StringBuilder code = new StringBuilder(INVITE_CODE_LENGTH);
    for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
      code.append(INVITE_CODE_CHARS.charAt(RANDOM.nextInt(INVITE_CODE_CHARS.length())));
    }
    return code.toString();
  }
}
