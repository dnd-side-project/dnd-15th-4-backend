package com.dnd.puzzlemeet.domain.meeting.service;

import com.dnd.puzzlemeet.domain.meeting.dto.MeetingCreateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingCreateResponse;
import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRole;
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
import java.util.UUID;
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
            generateInviteCode());
    meetingRepository.save(meeting);

    String nickname = request.nickname() != null ? request.nickname() : host.getNickname();
    MeetingMember hostMember = new MeetingMember(meeting, host, MeetingMemberRole.HOST, nickname);
    meetingMemberRepository.save(hostMember);

    boolean hasImage = image != null && !image.isEmpty();
    String imageUrl = hasImage ? uploadMemberImage(image) : DEFAULT_MEMBER_IMAGE_URL;
    memberImageRepository.save(new MemberImage(hostMember, imageUrl, !hasImage));

    return MeetingCreateResponse.from(meeting);
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
