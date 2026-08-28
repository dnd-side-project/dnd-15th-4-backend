package com.dnd.puzzlemeet.domain.meeting.service;

import com.dnd.puzzlemeet.domain.meeting.client.TmapCarClient;
import com.dnd.puzzlemeet.domain.meeting.client.TmapPedestrianClient;
import com.dnd.puzzlemeet.domain.meeting.client.TravelRoute;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingCreateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingCreateResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingDetailResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingInProgressResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingInviteCodeResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingJoinRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingJoinResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingListResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberArrivalResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberDepartureCreateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberDepartureResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberDepartureUpdateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberNicknameUpdateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberNicknameUpdateResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberPuzzleImageUpdateResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingPreviewRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingPreviewResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingResultResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingRouteRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingRouteSearchRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingRouteSearchResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingUpdateRequest;
import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRole;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRoute;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberStatus;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus;
import com.dnd.puzzlemeet.domain.meeting.entity.ReactionMessage;
import com.dnd.puzzlemeet.domain.meeting.entity.TransportType;
import com.dnd.puzzlemeet.domain.meeting.entity.TravelMode;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRouteRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.ReactionMessageRepository;
import com.dnd.puzzlemeet.domain.notification.event.FriendArrivedEvent;
import com.dnd.puzzlemeet.domain.puzzle.entity.MemberImage;
import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzleCollection;
import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzlePage;
import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzlePiece;
import com.dnd.puzzlemeet.domain.puzzle.repository.MemberImageRepository;
import com.dnd.puzzlemeet.domain.puzzle.repository.PuzzleCollectionRepository;
import com.dnd.puzzlemeet.domain.puzzle.repository.PuzzlePageRepository;
import com.dnd.puzzlemeet.domain.puzzle.repository.PuzzlePieceRepository;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.s3.AmazonS3Manager;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingService {

  private static final int ARRIVAL_RADIUS_M = 50;
  private static final double EARTH_RADIUS_M = 6_371_000;
  private static final int INVITE_CODE_LENGTH = 8;
  private static final String INVITE_CODE_CHARS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private static final String DEFAULT_MEMBER_IMAGE_URL =
      "https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/puzzles/_+(9)+4.png";
  private static final int RECENT_QUICK_MESSAGE_LIMIT = 20;
  private static final int PUZZLE_GROUP_SIZE = 4;

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
  private final MeetingMemberRouteRepository meetingMemberRouteRepository;
  private final MemberImageRepository memberImageRepository;
  private final UserRepository userRepository;
  private final AmazonS3Manager amazonS3Manager;
  private final TransitRouteFacade transitRouteFacade;
  private final TmapCarClient tmapCarClient;
  private final TmapPedestrianClient tmapPedestrianClient;
  private final PuzzlePageRepository puzzlePageRepository;
  private final PuzzlePieceRepository puzzlePieceRepository;
  private final PuzzleCollectionRepository puzzleCollectionRepository;
  private final ReactionMessageRepository reactionMessageRepository;
  private final ApplicationEventPublisher applicationEventPublisher;

  @Transactional
  public MeetingCreateResponse createMeeting(
      Long userId, MeetingCreateRequest request, MultipartFile image) {
    User host =
        userRepository
            .findActiveByIdForUpdate(userId)
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
            request.capacity(),
            generateInviteCode(),
            request.memo());
    meetingRepository.save(meeting);

    registerMember(
        meeting,
        host,
        MeetingMemberRole.HOST,
        request.nickname(),
        request.nicknameSet(),
        request.imageSet(),
        image);

    return MeetingCreateResponse.from(meeting);
  }

  @Transactional(readOnly = true)
  public MeetingDetailResponse getMeetingDetail(Long userId, Long meetingId) {
    Meeting meeting =
        meetingRepository
            .findById(meetingId)
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_NOT_FOUND));

    if (!meetingMemberRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
      throw ApiException.of(ErrorCode.AUTH_FORBIDDEN);
    }

    List<MeetingMember> members =
        meetingMemberRepository.findAllByMeetingIdInFetchUser(List.of(meetingId));
    Map<Long, MemberImage> imagesByMemberId =
        memberImageRepository.findAllByMeetingId(meetingId).stream()
            .collect(Collectors.toMap(image -> image.getMeetingMember().getId(), image -> image));

    return MeetingDetailResponse.from(meeting, members, imagesByMemberId);
  }

  @Transactional(readOnly = true)
  public MeetingPreviewResponse previewMeeting(MeetingPreviewRequest request) {
    Meeting meeting =
        meetingRepository
            .findByInviteCode(request.inviteCode())
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_INVITE_CODE_INVALID));

    if (!isMemberSetupOpen(meeting)) {
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
            .findActiveByIdForUpdate(userId)
            .orElseThrow(() -> ApiException.of(ErrorCode.USER_NOT_FOUND));

    Meeting meeting =
        meetingRepository
            .findByInviteCodeForUpdate(request.inviteCode())
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_INVITE_CODE_INVALID));

    if (!isMemberSetupOpen(meeting)) {
      throw ApiException.of(ErrorCode.MEETING_NOT_JOINABLE);
    }

    if (meetingMemberRepository.existsByMeetingIdAndUserId(meeting.getId(), userId)) {
      throw ApiException.of(ErrorCode.MEETING_MEMBER_ALREADY_JOINED);
    }

    if (meetingMemberRepository.countByMeetingId(meeting.getId()) >= meeting.getCapacity()) {
      throw ApiException.of(ErrorCode.MEETING_CAPACITY_EXCEEDED);
    }

    registerMember(
        meeting,
        user,
        MeetingMemberRole.GUEST,
        request.nickname(),
        request.nicknameSet(),
        request.imageSet(),
        image);

    return MeetingJoinResponse.from(meeting);
  }

  @Transactional
  public MeetingMemberNicknameUpdateResponse updateMemberNickname(
      Long userId, Long meetingId, MeetingMemberNicknameUpdateRequest request) {
    lockActiveUser(userId);

    MeetingMember member = getActiveMeetingMember(userId, meetingId);
    applyNicknameSetting(
        member, request.nicknameSet() == null || request.nicknameSet(), request.nickname());
    return MeetingMemberNicknameUpdateResponse.from(member);
  }

  @Transactional
  public MeetingMemberPuzzleImageUpdateResponse updateMemberPuzzleImage(
      Long userId, Long meetingId, MultipartFile image, Boolean imageSet) {
    lockActiveUser(userId);

    boolean useCustomImage = imageSet == null || imageSet;
    if (useCustomImage && (image == null || image.isEmpty())) {
      throw ApiException.of(ErrorCode.MEETING_MEMBER_PUZZLE_IMAGE_REQUIRED);
    }

    MeetingMember member = getActiveMeetingMember(userId, meetingId);
    if (!isMemberSetupOpen(member.getMeeting())) {
      throw ApiException.of(ErrorCode.MEETING_NOT_WAITING);
    }

    MemberImage memberImage =
        memberImageRepository
            .findByMeetingMemberId(member.getId())
            .orElseGet(
                () ->
                    memberImageRepository.save(
                        new MemberImage(member, DEFAULT_MEMBER_IMAGE_URL, true)));
    String previousUploadedImageUrl = resolveUploadedImageUrl(memberImage);

    if (useCustomImage) {
      memberImage.changeImage(uploadMemberImage(image));
    } else {
      memberImage.replaceWithDefaultImage(DEFAULT_MEMBER_IMAGE_URL);
    }
    deletePuzzleImageAfterCommit(previousUploadedImageUrl);

    return MeetingMemberPuzzleImageUpdateResponse.from(memberImage);
  }

  @Transactional
  public MeetingMemberArrivalResponse markMemberArrived(Long userId, Long meetingId) {
    lockActiveUser(userId);

    Meeting meeting =
        meetingRepository
            .findByIdForUpdate(meetingId)
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_NOT_FOUND));
    MeetingMember member = getActiveMeetingMember(userId, meetingId);
    if (member.getStatus() == MeetingMemberStatus.ARRIVED) {
      return MeetingMemberArrivalResponse.from(member);
    }

    if (member.getCurrentLatitude() == null || member.getCurrentLongitude() == null) {
      throw ApiException.of(ErrorCode.MEETING_ARRIVAL_LOCATION_INVALID);
    }

    if (!isWithinArrivalRadius(
        member.getCurrentLatitude(),
        member.getCurrentLongitude(),
        meeting.getDestinationLatitude(),
        meeting.getDestinationLongitude(),
        meeting.getArrivalRadiusM())) {
      throw ApiException.of(ErrorCode.MEETING_ARRIVAL_LOCATION_INVALID);
    }

    member.arrive();
    applicationEventPublisher.publishEvent(new FriendArrivedEvent(meeting.getId(), member.getId()));
    completeIfAllMembersArrived(meeting, member);
    return MeetingMemberArrivalResponse.from(member);
  }

  private void completeIfAllMembersArrived(Meeting meeting, MeetingMember arrivedMember) {
    if (meeting.getStatus() != MeetingStatus.IN_PROGRESS) {
      return;
    }
    if (meetingMemberRepository.existsNotArrivedMemberExcluding(
        meeting.getId(), arrivedMember.getId())) {
      return;
    }

    meeting.complete();
    collectCompletedPuzzles(meeting);
    log.info("[약속 전원 도착 종료] meetingId={}", meeting.getId());
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
    lockActiveUser(userId);

    Meeting meeting =
        meetingRepository
            .findById(meetingId)
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_NOT_FOUND));

    if (!meeting.getHostUser().getId().equals(userId)) {
      throw ApiException.of(ErrorCode.AUTH_FORBIDDEN);
    }

    if (!isMemberSetupOpen(meeting)) {
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
    boolean meetingAtChanged = !Objects.equals(meeting.getMeetingAt(), meetingAt);

    meeting.updateDetails(title, meetingAt, destination, latitude, longitude, memo);
    if (meetingAtChanged) {
      meetingMemberRepository.resetDepartureReminderAttemptedAtForNotStartedMembers(meetingId);
    }
  }

  @Transactional
  public void updateCurrentLocation(
      Long userId, Long meetingId, double latitude, double longitude) {
    meetingRepository
        .findById(meetingId)
        .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_NOT_FOUND));

    MeetingMember member =
        meetingMemberRepository
            .findByMeetingIdAndUserId(meetingId, userId)
            .orElseThrow(() -> ApiException.of(ErrorCode.AUTH_FORBIDDEN));

    member.updateCurrentLocation(BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude));
  }

  @Transactional(readOnly = true)
  public MeetingInProgressResponse getMeetingInProgress(Long userId, Long meetingId) {
    Meeting meeting =
        meetingRepository
            .findById(meetingId)
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_NOT_FOUND));

    if (!meetingMemberRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
      throw ApiException.of(ErrorCode.AUTH_FORBIDDEN);
    }

    if (meeting.getStatus() == MeetingStatus.WAITING) {
      throw ApiException.of(ErrorCode.MEETING_NOT_STARTED);
    }

    List<PuzzlePage> puzzlePages =
        puzzlePageRepository.findAllByMeetingIdOrderByPageNumberAsc(meetingId);
    List<Long> puzzlePageIds = puzzlePages.stream().map(PuzzlePage::getId).toList();
    Map<Long, List<PuzzlePiece>> puzzlePiecesByPageId =
        puzzlePageIds.isEmpty()
            ? Map.of()
            : puzzlePieceRepository.findAllByPuzzlePageIdInFetchMember(puzzlePageIds).stream()
                .collect(Collectors.groupingBy(piece -> piece.getPuzzlePage().getId()));

    List<MeetingInProgressResponse.PuzzleGroup> puzzleGroups =
        puzzlePages.stream()
            .map(
                page ->
                    toPuzzleGroup(page, puzzlePiecesByPageId.getOrDefault(page.getId(), List.of())))
            .toList();

    List<ReactionMessage> recentMessages =
        reactionMessageRepository.findRecentByMeetingId(
            meetingId, PageRequest.of(0, RECENT_QUICK_MESSAGE_LIMIT));
    List<MeetingInProgressResponse.QuickMessage> quickMessages =
        recentMessages.reversed().stream()
            .map(
                message ->
                    new MeetingInProgressResponse.QuickMessage(
                        message.getId(),
                        message.getContent(),
                        message.getSenderMember().getUser().getId()))
            .toList();

    return new MeetingInProgressResponse(
        puzzleGroups,
        quickMessages,
        meeting.getStatus() == MeetingStatus.COMPLETED,
        meeting.getDestinationLatitude().doubleValue(),
        meeting.getDestinationLongitude().doubleValue());
  }

  private MeetingInProgressResponse.PuzzleGroup toPuzzleGroup(
      PuzzlePage page, List<PuzzlePiece> pieces) {
    String puzzleImageUrl =
        page.getRepresentativeMemberImage() != null
            ? page.getRepresentativeMemberImage().getImageUrl()
            : DEFAULT_MEMBER_IMAGE_URL;

    List<MeetingInProgressResponse.Participant> members =
        pieces.stream()
            .map(
                piece ->
                    piece.getMeetingMember() != null
                        ? MeetingInProgressResponse.Participant.from(
                            piece.getMeetingMember(), piece.getPieceIndex(), piece.isRevealed())
                        : MeetingInProgressResponse.Participant.empty(
                            piece.getPieceIndex(), piece.isRevealed()))
            .toList();

    return new MeetingInProgressResponse.PuzzleGroup(
        puzzleImageUrl, page.getId(), page.getPageNumber(), members);
  }

  private boolean isPuzzleCompleted(List<PuzzlePiece> pieces) {
    List<PuzzlePiece> assignedPieces =
        pieces.stream().filter(piece -> piece.getMeetingMember() != null).toList();

    return !assignedPieces.isEmpty()
        && assignedPieces.stream()
            .allMatch(piece -> piece.getMeetingMember().getStatus() == MeetingMemberStatus.ARRIVED);
  }

  @Transactional(readOnly = true)
  public MeetingResultResponse getMeetingResult(Long userId, Long meetingId) {
    Meeting meeting =
        meetingRepository
            .findById(meetingId)
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_NOT_FOUND));

    if (!meetingMemberRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
      throw ApiException.of(ErrorCode.AUTH_FORBIDDEN);
    }

    if (meeting.getStatus() != MeetingStatus.COMPLETED) {
      throw ApiException.of(ErrorCode.MEETING_NOT_COMPLETED);
    }

    List<MeetingMember> members =
        meetingMemberRepository.findAllByMeetingIdInFetchUser(List.of(meetingId));

    List<MeetingResultResponse.RankingEntry> rankings =
        members.stream()
            .map(member -> toRankingEntry(member, meeting.getMeetingAt()))
            .sorted(
                Comparator.comparing(MeetingResultResponse.RankingEntry::late)
                    .thenComparing(
                        MeetingResultResponse.RankingEntry::arrivedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

    LocalDateTime myDepartedAt =
        members.stream()
            .filter(member -> member.getUser().getId().equals(userId))
            .findFirst()
            .map(MeetingMember::getDepartedAt)
            .orElse(null);

    List<PuzzlePage> puzzlePages =
        puzzlePageRepository.findAllByMeetingIdOrderByPageNumberAsc(meetingId);
    List<Long> puzzlePageIds = puzzlePages.stream().map(PuzzlePage::getId).toList();
    Map<Long, List<PuzzlePiece>> puzzlePiecesByPageId =
        puzzlePageIds.isEmpty()
            ? Map.of()
            : puzzlePieceRepository.findAllByPuzzlePageIdInFetchMember(puzzlePageIds).stream()
                .collect(Collectors.groupingBy(piece -> piece.getPuzzlePage().getId()));

    List<MeetingResultResponse.PuzzleFeedItem> puzzleFeed =
        puzzlePages.stream()
            .map(
                page -> {
                  List<PuzzlePiece> pieces =
                      puzzlePiecesByPageId.getOrDefault(page.getId(), List.of());
                  return MeetingResultResponse.PuzzleFeedItem.of(
                      page, pieces, isPuzzleCompleted(pieces));
                })
            .toList();

    Set<Long> representativeImageIds =
        puzzlePages.stream()
            .map(PuzzlePage::getRepresentativeMemberImage)
            .filter(Objects::nonNull)
            .map(MemberImage::getId)
            .collect(Collectors.toSet());

    List<MeetingResultResponse.UnselectedImage> unselectedImages =
        memberImageRepository.findAllByMeetingId(meetingId).stream()
            .filter(image -> !representativeImageIds.contains(image.getId()))
            .map(MeetingResultResponse.UnselectedImage::from)
            .toList();

    return new MeetingResultResponse(puzzleFeed, unselectedImages, rankings, myDepartedAt);
  }

  private MeetingResultResponse.RankingEntry toRankingEntry(
      MeetingMember member, LocalDateTime meetingAt) {
    boolean arrived = member.getStatus() == MeetingMemberStatus.ARRIVED;
    LocalDateTime arrivedAt = member.getArrivedAt();
    boolean late = !arrived || arrivedAt.isAfter(meetingAt);
    Long earlyArrivalMinutes = late ? null : Duration.between(arrivedAt, meetingAt).toMinutes();

    return new MeetingResultResponse.RankingEntry(
        member.getUser().getId(),
        member.getNickname(),
        member.getProfileImageUrl(),
        arrived,
        arrivedAt,
        earlyArrivalMinutes,
        late);
  }

  @Transactional(readOnly = true)
  public MeetingInviteCodeResponse getInviteCode(Long userId, Long meetingId) {
    Meeting meeting =
        meetingRepository
            .findById(meetingId)
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_NOT_FOUND));

    if (!meeting.getHostUser().getId().equals(userId)) {
      throw ApiException.of(ErrorCode.AUTH_FORBIDDEN);
    }

    return MeetingInviteCodeResponse.from(meeting);
  }

  @Transactional
  public void startTodaysMeetings() {
    LocalDate today = LocalDate.now();
    List<Meeting> meetings =
        meetingRepository.findAllStartingBetween(
            today.atStartOfDay(), today.plusDays(1).atStartOfDay());

    meetings.forEach(Meeting::start);

    if (!meetings.isEmpty()) {
      log.info("[약속 자동 시작] count={}", meetings.size());
    }
  }

  private boolean isMemberSetupOpen(Meeting meeting) {
    if (meeting.getStatus() == MeetingStatus.WAITING) {
      return true;
    }
    if (meeting.getStatus() != MeetingStatus.IN_PROGRESS) {
      return false;
    }
    return LocalDateTime.now().isBefore(meeting.getMeetingAt())
        && !puzzlePageRepository.existsByMeetingId(meeting.getId());
  }

  private void assignPuzzleGroups(Meeting meeting) {
    List<MemberImage> memberImages = memberImageRepository.findAllByMeetingId(meeting.getId());
    if (memberImages.isEmpty()) {
      return;
    }

    List<MemberImage> shuffledImages = new ArrayList<>(memberImages);
    Collections.shuffle(shuffledImages, RANDOM);

    int pageNumber = 1;
    for (int i = 0; i < shuffledImages.size(); i += PUZZLE_GROUP_SIZE) {
      List<MemberImage> group =
          shuffledImages.subList(i, Math.min(i + PUZZLE_GROUP_SIZE, shuffledImages.size()));

      PuzzlePage page = new PuzzlePage(meeting, pageNumber++);
      page.selectRepresentativeImage(group.get(RANDOM.nextInt(group.size())));
      puzzlePageRepository.save(page);

      for (int pieceIndex = 0; pieceIndex < PUZZLE_GROUP_SIZE; pieceIndex++) {
        MeetingMember assignedMember =
            pieceIndex < group.size() ? group.get(pieceIndex).getMeetingMember() : null;
        puzzlePieceRepository.save(new PuzzlePiece(page, assignedMember, (byte) (pieceIndex + 1)));
      }
    }
  }

  @Transactional
  public void completeExpiredMeetings() {
    List<Meeting> expiredMeetings =
        meetingRepository.findAllExpired(LocalDate.now().atStartOfDay());
    expiredMeetings.forEach(Meeting::complete);
    expiredMeetings.forEach(this::collectCompletedPuzzles);

    if (!expiredMeetings.isEmpty()) {
      log.info("[약속 자동 종료] count={}", expiredMeetings.size());
    }
  }

  private void collectCompletedPuzzles(Meeting meeting) {
    List<PuzzlePage> puzzlePages =
        puzzlePageRepository.findAllByMeetingIdOrderByPageNumberAsc(meeting.getId());
    if (puzzlePages.isEmpty()) {
      return;
    }

    List<Long> puzzlePageIds = puzzlePages.stream().map(PuzzlePage::getId).toList();
    Map<Long, List<PuzzlePiece>> puzzlePiecesByPageId =
        puzzlePieceRepository.findAllByPuzzlePageIdInFetchMember(puzzlePageIds).stream()
            .collect(Collectors.groupingBy(piece -> piece.getPuzzlePage().getId()));

    List<PuzzlePage> completedPages =
        puzzlePages.stream()
            .filter(
                page ->
                    isPuzzleCompleted(puzzlePiecesByPageId.getOrDefault(page.getId(), List.of())))
            .filter(page -> page.getRepresentativeMemberImage() != null)
            .toList();

    if (completedPages.isEmpty()) {
      return;
    }

    List<User> participants =
        meetingMemberRepository.findAllByMeetingIdInFetchUser(List.of(meeting.getId())).stream()
            .map(MeetingMember::getUser)
            .toList();

    List<PuzzleCollection> collections =
        completedPages.stream()
            .flatMap(
                page ->
                    participants.stream()
                        .map(
                            user ->
                                new PuzzleCollection(
                                    user, page, page.getRepresentativeMemberImage().getImageUrl())))
            .toList();

    puzzleCollectionRepository.saveAll(collections);
  }

  @Transactional
  public void cancelMeeting(Long userId, Long meetingId) {
    lockActiveUser(userId);

    Meeting meeting =
        meetingRepository
            .findById(meetingId)
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_NOT_FOUND));

    if (!meeting.getHostUser().getId().equals(userId)) {
      throw ApiException.of(ErrorCode.AUTH_FORBIDDEN);
    }

    if (!isMemberSetupOpen(meeting)) {
      throw ApiException.of(ErrorCode.MEETING_NOT_WAITING);
    }

    meeting.cancel();
  }

  @Transactional
  public void leaveMeeting(Long userId, Long meetingId) {
    lockActiveUser(userId);

    MeetingMember member = getActiveMeetingMember(userId, meetingId);
    if (member.getRole() == MeetingMemberRole.HOST) {
      throw ApiException.of(ErrorCode.MEETING_HOST_CANNOT_LEAVE);
    }

    MemberImage memberImage =
        memberImageRepository.findByMeetingMemberId(member.getId()).orElse(null);
    String uploadedImageUrl = resolveUploadedImageUrl(memberImage);

    meetingMemberRouteRepository.deleteAllByMeetingMemberId(member.getId());
    memberImageRepository.deleteAllByMeetingMemberId(member.getId());
    meetingMemberRepository.delete(member);
    deletePuzzleImageAfterCommit(uploadedImageUrl);
  }

  @Transactional
  public MeetingMemberDepartureResponse createDeparture(
      Long userId, Long meetingId, MeetingMemberDepartureCreateRequest request) {
    lockActiveUser(userId);

    Meeting meeting =
        meetingRepository
            .findByIdForUpdate(meetingId)
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_NOT_FOUND));
    MeetingMember member = getActiveMeetingMember(userId, meetingId);
    if (!meeting.getMeetingAt().toLocalDate().equals(LocalDate.now())) {
      throw ApiException.of(ErrorCode.MEETING_NOT_STARTED);
    }
    if (member.getDepartureName() != null) {
      throw ApiException.of(ErrorCode.MEETING_DEPARTURE_ALREADY_SET);
    }
    if (meetingMemberRepository.existsMovingMemberInOtherActiveMeeting(userId, meetingId)) {
      throw ApiException.of(ErrorCode.MEETING_MEMBER_MOVING_IN_OTHER_MEETING);
    }

    MeetingMemberDepartureCreateRequest.NicknameSetting nicknameSetting = request.nicknameSetting();
    applyNicknameSetting(member, nicknameSetting.enabled(), nicknameSetting.nickname());
    MeetingMemberDepartureCreateRequest.NotificationSettings notificationSettings =
        request.notificationSettings();
    member.updateNotificationSettings(
        notificationSettings.locationPermission(),
        notificationSettings.friendArrival(),
        notificationSettings.chatBubble());

    MeetingMemberDepartureCreateRequest.Departure departure = request.departure();
    if (meeting.getStatus() == MeetingStatus.WAITING) {
      meeting.start();
    }
    if (!puzzlePageRepository.existsByMeetingId(meeting.getId())) {
      assignPuzzleGroups(meeting);
    }
    member.depart();
    return applyDeparture(
        member,
        departure.placeName(),
        departure.latitude(),
        departure.longitude(),
        request.route(),
        request.travelMode());
  }

  @Transactional(readOnly = true)
  public MeetingMemberDepartureResponse getDeparture(Long userId, Long meetingId) {
    MeetingMember member = getActiveMeetingMember(userId, meetingId);
    if (member.getDepartureName() == null) {
      throw ApiException.of(ErrorCode.MEETING_DEPARTURE_NOT_FOUND);
    }
    return MeetingMemberDepartureResponse.of(member, findRoutes(member));
  }

  @Transactional
  public MeetingMemberDepartureResponse updateDeparture(
      Long userId, Long meetingId, MeetingMemberDepartureUpdateRequest request) {
    lockActiveUser(userId);

    MeetingMember member = getActiveMeetingMember(userId, meetingId);
    if (member.getDepartureName() == null) {
      throw ApiException.of(ErrorCode.MEETING_DEPARTURE_NOT_FOUND);
    }

    MeetingMemberDepartureUpdateRequest.NicknameSetting nicknameSetting = request.nicknameSetting();
    if (nicknameSetting != null) {
      applyNicknameSetting(member, nicknameSetting.enabled(), nicknameSetting.nickname());
    }
    MeetingMemberDepartureUpdateRequest.NotificationSettings notificationSettings =
        request.notificationSettings();
    if (notificationSettings != null) {
      member.updateNotificationSettings(
          notificationSettings.locationPermission(),
          notificationSettings.friendArrival(),
          notificationSettings.chatBubble());
    }

    MeetingMemberDepartureUpdateRequest.Departure departure = request.departure();
    if (departure != null) {
      if (request.route() == null) {
        throw ApiException.of(ErrorCode.INVALID_INPUT_VALUE);
      }
      return applyDeparture(
          member,
          departure.placeName(),
          departure.latitude(),
          departure.longitude(),
          request.route(),
          travelMode(request, member));
    }

    return MeetingMemberDepartureResponse.of(member, findRoutes(member));
  }

  @Transactional(readOnly = true)
  public MeetingRouteSearchResponse searchRoutes(
      Long userId, Long meetingId, MeetingRouteSearchRequest request) {
    MeetingMember member = getActiveMeetingMember(userId, meetingId);
    return MeetingRouteSearchResponse.from(
        resolveRoutes(
            member.getMeeting(),
            request.start().latitude(),
            request.start().longitude(),
            request.travelMode()));
  }

  private List<TravelRoute> resolveRoutes(
      Meeting meeting, double latitude, double longitude, TravelMode travelMode) {
    return switch (travelMode) {
      case TRANSIT ->
          transitRouteFacade.findRoutes(
              new TransitRouteQuery(
                  latitude,
                  longitude,
                  meeting.getDestinationLatitude().doubleValue(),
                  meeting.getDestinationLongitude().doubleValue(),
                  meeting.getMeetingAt()));
      case CAR ->
          List.of(
              tmapCarClient.findCarRoute(
                  latitude,
                  longitude,
                  meeting.getDestinationLatitude().doubleValue(),
                  meeting.getDestinationLongitude().doubleValue(),
                  meeting.getDestinationName(),
                  firstQueryDepartAt(meeting)));
      case WALK ->
          List.of(
              tmapPedestrianClient.findWalkingRoute(
                  latitude,
                  longitude,
                  meeting.getDestinationLatitude().doubleValue(),
                  meeting.getDestinationLongitude().doubleValue(),
                  meeting.getDestinationName()));
    };
  }

  private TravelMode travelMode(MeetingMemberDepartureUpdateRequest request, MeetingMember member) {
    if (request.travelMode() != null) {
      return request.travelMode();
    }
    return member.getTravelMode() != null ? member.getTravelMode() : TravelMode.TRANSIT;
  }

  private MeetingMemberDepartureResponse applyDeparture(
      MeetingMember member,
      String placeName,
      double latitude,
      double longitude,
      MeetingRouteRequest route,
      TravelMode travelMode) {
    member.updateDeparture(
        placeName, BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude), travelMode);
    member.updateEstimatedDuration(route.totalTime());
    MeetingRouteRequest.Step mainStep = mainTransportStep(route);
    member.updateTransport(mainStep.type(), mainStep.line());

    meetingMemberRouteRepository.deleteAllByMeetingMemberId(member.getId());
    return MeetingMemberDepartureResponse.of(member, saveRoutes(member, route));
  }

  private LocalDateTime firstQueryDepartAt(Meeting meeting) {
    LocalDateTime meetingAt = meeting.getMeetingAt();
    return meetingAt.isAfter(LocalDateTime.now()) ? meetingAt : null;
  }

  private void applyNicknameSetting(MeetingMember member, boolean enabled, String nickname) {
    if (!enabled) {
      member.resetNicknameToDefault(member.getUser().getNickname());
      return;
    }
    if (nickname == null || nickname.isBlank()) {
      throw ApiException.of(ErrorCode.INVALID_INPUT_VALUE);
    }
    member.changeNickname(nickname);
  }

  private List<MeetingMemberRoute> saveRoutes(MeetingMember member, MeetingRouteRequest route) {
    List<MeetingRouteRequest.Step> steps = route.steps();
    List<MeetingMemberRoute> routes = new ArrayList<>(steps.size());
    for (int index = 0; index < steps.size(); index++) {
      MeetingRouteRequest.Step step = steps.get(index);
      routes.add(
          new MeetingMemberRoute(
              member,
              index + 1,
              step.type(),
              step.line(),
              step.startName(),
              step.endName(),
              step.stationCount(),
              step.time()));
    }
    return meetingMemberRouteRepository.saveAll(routes);
  }

  private List<MeetingMemberRoute> findRoutes(MeetingMember member) {
    return meetingMemberRouteRepository.findAllByMeetingMemberIdOrderByRouteIndexAsc(
        member.getId());
  }

  private MeetingRouteRequest.Step mainTransportStep(MeetingRouteRequest route) {
    return route.steps().stream()
        .filter(step -> step.type() != TransportType.WALK)
        .findFirst()
        .orElse(route.steps().getFirst());
  }

  private void registerMember(
      Meeting meeting,
      User user,
      MeetingMemberRole role,
      String nickname,
      boolean nicknameSet,
      boolean imageSet,
      MultipartFile image) {
    String resolvedNickname = nicknameSet ? nickname : user.getNickname();
    MeetingMember member =
        new MeetingMember(meeting, user, role, resolvedNickname, pickRandomProfileImageUrl());
    if (nicknameSet) {
      member.changeNickname(resolvedNickname);
    }
    if (imageSet) {
      member.markCustomImage();
    }
    meetingMemberRepository.save(member);

    String imageUrl = uploadMemberImage(image);
    memberImageRepository.save(new MemberImage(member, imageUrl, !imageSet));
  }

  private void lockActiveUser(Long userId) {
    userRepository
        .findActiveByIdForUpdate(userId)
        .orElseThrow(() -> ApiException.of(ErrorCode.USER_NOT_FOUND));
  }

  private String resolveUploadedImageUrl(MemberImage memberImage) {
    if (memberImage == null
        || memberImage.isDefaultImage()
        || DEFAULT_MEMBER_IMAGE_URL.equals(memberImage.getImageUrl())) {
      return null;
    }
    return memberImage.getImageUrl();
  }

  private void deletePuzzleImageAfterCommit(String imageUrl) {
    if (imageUrl == null) {
      return;
    }

    if (!TransactionSynchronizationManager.isActualTransactionActive()
        || !TransactionSynchronizationManager.isSynchronizationActive()) {
      amazonS3Manager.deletePuzzleImage(imageUrl);
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            try {
              amazonS3Manager.deletePuzzleImage(imageUrl);
            } catch (RuntimeException e) {
              log.warn("[S3 퍼즐 이미지 후처리 삭제 실패] url={}", imageUrl, e);
            }
          }
        });
  }

  private MeetingMember getActiveMeetingMember(Long userId, Long meetingId) {
    Meeting meeting =
        meetingRepository
            .findById(meetingId)
            .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_NOT_FOUND));
    if (meeting.getStatus() != MeetingStatus.WAITING
        && meeting.getStatus() != MeetingStatus.IN_PROGRESS) {
      throw ApiException.of(ErrorCode.MEETING_MEMBER_NOT_ACTIVE);
    }

    return meetingMemberRepository
        .findByMeetingIdAndUserId(meetingId, userId)
        .orElseThrow(() -> ApiException.of(ErrorCode.MEETING_MEMBER_NOT_FOUND));
  }

  private boolean isWithinArrivalRadius(
      BigDecimal currentLatitude,
      BigDecimal currentLongitude,
      BigDecimal destinationLatitude,
      BigDecimal destinationLongitude,
      int arrivalRadiusM) {
    return distanceMeters(
            currentLatitude, currentLongitude, destinationLatitude, destinationLongitude)
        <= arrivalRadiusM;
  }

  private double distanceMeters(
      BigDecimal fromLatitude,
      BigDecimal fromLongitude,
      BigDecimal toLatitude,
      BigDecimal toLongitude) {
    double latitudeDifference =
        Math.toRadians(toLatitude.doubleValue() - fromLatitude.doubleValue());
    double longitudeDifference =
        Math.toRadians(toLongitude.doubleValue() - fromLongitude.doubleValue());
    double fromLatitudeRadians = Math.toRadians(fromLatitude.doubleValue());
    double toLatitudeRadians = Math.toRadians(toLatitude.doubleValue());

    double haversine =
        Math.pow(Math.sin(latitudeDifference / 2), 2)
            + Math.cos(fromLatitudeRadians)
                * Math.cos(toLatitudeRadians)
                * Math.pow(Math.sin(longitudeDifference / 2), 2);
    double centralAngle = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    return EARTH_RADIUS_M * centralAngle;
  }

  private String pickRandomProfileImageUrl() {
    String[] urls = DefaultProfileImageUrls.URLS;
    return urls[RANDOM.nextInt(urls.length)];
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
