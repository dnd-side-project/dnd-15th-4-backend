package com.dnd.puzzlemeet.domain.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dnd.puzzlemeet.domain.meeting.client.TmapCarClient;
import com.dnd.puzzlemeet.domain.meeting.client.TmapPedestrianClient;
import com.dnd.puzzlemeet.domain.meeting.client.TravelRoute;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingDetailResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingInProgressResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingInviteCodeResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingJoinRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingJoinResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberArrivalResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberDepartureCreateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberDepartureResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberDepartureUpdateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberNicknameUpdateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberNicknameUpdateResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberPuzzleImageUpdateResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingPreviewRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingPreviewResponse;
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
import com.dnd.puzzlemeet.domain.meeting.entity.ReactionPreset;
import com.dnd.puzzlemeet.domain.meeting.entity.TransportType;
import com.dnd.puzzlemeet.domain.meeting.entity.TravelMode;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRouteRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.ReactionMessageRepository;
import com.dnd.puzzlemeet.domain.notification.event.FriendArrivedEvent;
import com.dnd.puzzlemeet.domain.puzzle.entity.MemberImage;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

  @Mock private MeetingRepository meetingRepository;
  @Mock private MeetingMemberRepository meetingMemberRepository;
  @Mock private MeetingMemberRouteRepository meetingMemberRouteRepository;
  @Mock private MemberImageRepository memberImageRepository;
  @Mock private UserRepository userRepository;
  @Mock private AmazonS3Manager amazonS3Manager;
  @Mock private TransitRouteFacade transitRouteFacade;
  @Mock private TmapCarClient tmapCarClient;
  @Mock private TmapPedestrianClient tmapPedestrianClient;
  @Mock private PuzzlePageRepository puzzlePageRepository;
  @Mock private PuzzlePieceRepository puzzlePieceRepository;
  @Mock private PuzzleCollectionRepository puzzleCollectionRepository;
  @Mock private ReactionMessageRepository reactionMessageRepository;
  @Mock private ApplicationEventPublisher applicationEventPublisher;

  private MeetingService meetingService;

  @BeforeEach
  void setUp() {
    meetingService =
        new MeetingService(
            meetingRepository,
            meetingMemberRepository,
            meetingMemberRouteRepository,
            memberImageRepository,
            userRepository,
            amazonS3Manager,
            transitRouteFacade,
            tmapCarClient,
            tmapPedestrianClient,
            puzzlePageRepository,
            puzzlePieceRepository,
            puzzleCollectionRepository,
            reactionMessageRepository,
            applicationEventPublisher);
    lenient()
        .when(userRepository.findActiveByIdForUpdate(any()))
        .thenReturn(Optional.of(new User(100L, "효창", "https://img.kakao.com/a.jpg")));
  }

  @Test
  @DisplayName("약속 참여자가 상세를 조회하면 참여자별로 등록한 퍼즐 이미지를 함께 받는다")
  void returnsMeetingDetailWithMyPuzzleImage() {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "id", 10L);
    MeetingMember member =
        new MeetingMember(
            meeting,
            meeting.getHostUser(),
            MeetingMemberRole.HOST,
            "효창",
            "https://img.kakao.com/host.png");
    ReflectionTestUtils.setField(member, "id", 1L);
    member.markCustomImage();
    MemberImage myImage =
        new MemberImage(
            member, "https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/a.png", false);

    given(meetingRepository.findById(10L)).willReturn(Optional.of(meeting));
    given(meetingMemberRepository.existsByMeetingIdAndUserId(10L, 100L)).willReturn(true);
    given(meetingMemberRepository.findAllByMeetingIdInFetchUser(List.of(10L)))
        .willReturn(List.of(member));
    given(memberImageRepository.findAllByMeetingId(10L)).willReturn(List.of(myImage));

    MeetingDetailResponse response = meetingService.getMeetingDetail(100L, 10L);

    assertThat(response.meetingId()).isEqualTo(10L);
    assertThat(response.inviteCode()).isEqualTo("ABCD1234");
    assertThat(response.participants()).hasSize(1);
    assertThat(response.participants().get(0).puzzleImageUrl())
        .isEqualTo("https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/a.png");
    assertThat(response.participants().get(0).imageSet()).isTrue();
  }

  @Test
  @DisplayName("참여하지 않은 사용자가 약속 상세를 조회하면 AUTH_FORBIDDEN 예외가 발생한다")
  void throwsWhenNonMemberRequestsMeetingDetail() {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "id", 10L);

    given(meetingRepository.findById(10L)).willReturn(Optional.of(meeting));
    given(meetingMemberRepository.existsByMeetingIdAndUserId(10L, 999L)).willReturn(false);

    ApiException exception =
        assertThrows(ApiException.class, () -> meetingService.getMeetingDetail(999L, 10L));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_FORBIDDEN);
  }

  @Test
  @DisplayName("대기 중인 약속을 초대 코드로 조회하면 참여자 목록을 포함해 반환한다")
  void returnsMeetingPreviewWithParticipants() {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "id", 10L);
    MeetingMember member =
        new MeetingMember(
            meeting,
            meeting.getHostUser(),
            MeetingMemberRole.HOST,
            "효창",
            "https://img.kakao.com/host.png");

    given(meetingRepository.findByInviteCode("ABCD1234")).willReturn(Optional.of(meeting));
    given(meetingMemberRepository.findAllByMeetingIdInFetchUser(List.of(10L)))
        .willReturn(List.of(member));

    MeetingPreviewResponse response =
        meetingService.previewMeeting(new MeetingPreviewRequest("ABCD1234"));

    assertThat(response.meetingId()).isEqualTo(10L);
    assertThat(response.title()).isEqualTo("한강 피크닉");
    assertThat(response.participants()).hasSize(1);
    assertThat(response.participants().get(0).name()).isEqualTo("효창");
  }

  @Test
  @DisplayName("존재하지 않는 초대 코드로 조회하면 MEETING_INVITE_CODE_INVALID 예외가 발생한다")
  void throwsWhenInviteCodeNotFound() {
    given(meetingRepository.findByInviteCode("ZZZZ0000")).willReturn(Optional.empty());

    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> meetingService.previewMeeting(new MeetingPreviewRequest("ZZZZ0000")));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEETING_INVITE_CODE_INVALID);
  }

  @Test
  @DisplayName("취소된 약속을 초대 코드로 조회하면 MEETING_INVITE_CODE_INVALID 예외가 발생한다")
  void throwsWhenMeetingIsCanceled() {
    Meeting meeting = waitingMeeting();
    meeting.cancel();

    given(meetingRepository.findByInviteCode("ABCD1234")).willReturn(Optional.of(meeting));

    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> meetingService.previewMeeting(new MeetingPreviewRequest("ABCD1234")));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEETING_INVITE_CODE_INVALID);
  }

  @Test
  @DisplayName("정원이 남아있으면 초대 코드로 약속에 참여한다")
  void joinsMeetingWhenUnderCapacity() {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "id", 10L);
    ReflectionTestUtils.setField(meeting, "capacity", 2);
    User guest = new User(200L, "게스트", "https://img.kakao.com/b.jpg");

    given(userRepository.findActiveByIdForUpdate(200L)).willReturn(Optional.of(guest));
    given(meetingRepository.findByInviteCodeForUpdate("ABCD1234")).willReturn(Optional.of(meeting));
    given(meetingMemberRepository.existsByMeetingIdAndUserId(10L, 200L)).willReturn(false);
    given(meetingMemberRepository.countByMeetingId(10L)).willReturn(1L);

    MeetingJoinResponse response =
        meetingService.joinMeeting(
            200L, new MeetingJoinRequest("ABCD1234", null, false, false), null);

    assertThat(response.meetingId()).isEqualTo(10L);
    verify(meetingMemberRepository).save(any(MeetingMember.class));
  }

  @Test
  @DisplayName("정원이 다 찬 약속에 참여하면 MEETING_CAPACITY_EXCEEDED 예외가 발생한다")
  void throwsWhenMeetingIsFull() {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "id", 10L);
    ReflectionTestUtils.setField(meeting, "capacity", 2);
    User guest = new User(200L, "게스트", "https://img.kakao.com/b.jpg");

    given(userRepository.findActiveByIdForUpdate(200L)).willReturn(Optional.of(guest));
    given(meetingRepository.findByInviteCodeForUpdate("ABCD1234")).willReturn(Optional.of(meeting));
    given(meetingMemberRepository.existsByMeetingIdAndUserId(10L, 200L)).willReturn(false);
    given(meetingMemberRepository.countByMeetingId(10L)).willReturn(2L);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                meetingService.joinMeeting(
                    200L, new MeetingJoinRequest("ABCD1234", null, false, false), null));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEETING_CAPACITY_EXCEEDED);
    verify(meetingMemberRepository, never()).save(any(MeetingMember.class));
  }

  @Test
  @DisplayName("진행 중인 약속 데이터를 조회하면 퍼즐 그룹과 퀵메시지, 완료 여부를 반환한다")
  void returnsMeetingInProgressData() {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "id", 10L);
    ReflectionTestUtils.setField(meeting.getHostUser(), "id", 100L);
    meeting.complete();

    MeetingMember member =
        new MeetingMember(
            meeting,
            meeting.getHostUser(),
            MeetingMemberRole.HOST,
            "효창",
            "https://img.kakao.com/host.png");
    ReflectionTestUtils.setField(member, "id", 1L);
    member.updateDeparture(
        "회사", BigDecimal.valueOf(37.4979), BigDecimal.valueOf(127.0276), TravelMode.TRANSIT);

    PuzzlePage page = new PuzzlePage(meeting, 1);
    ReflectionTestUtils.setField(page, "id", 20L);

    PuzzlePiece assignedPiece = new PuzzlePiece(page, member, (byte) 1);
    PuzzlePiece emptyPiece = new PuzzlePiece(page, null, (byte) 2);

    ReactionPreset preset = new ReactionPreset("지금 출발");
    ReactionMessage message = new ReactionMessage(member, preset, "지금 출발");
    ReflectionTestUtils.setField(message, "id", 5L);

    given(meetingRepository.findById(10L)).willReturn(Optional.of(meeting));
    given(meetingMemberRepository.existsByMeetingIdAndUserId(10L, 100L)).willReturn(true);
    given(puzzlePageRepository.findAllByMeetingIdOrderByPageNumberAsc(10L))
        .willReturn(List.of(page));
    given(puzzlePieceRepository.findAllByPuzzlePageIdInFetchMember(List.of(20L)))
        .willReturn(List.of(assignedPiece, emptyPiece));
    given(reactionMessageRepository.findRecentByMeetingId(eq(10L), any(Pageable.class)))
        .willReturn(List.of(message));

    MeetingInProgressResponse response = meetingService.getMeetingInProgress(100L, 10L);

    assertThat(response.completed()).isTrue();
    assertThat(response.puzzleGroups()).hasSize(1);
    MeetingInProgressResponse.PuzzleGroup group = response.puzzleGroups().get(0);
    assertThat(group.puzzleGroupId()).isEqualTo(20L);
    assertThat(group.pageNumber()).isEqualTo(1);
    assertThat(group.members()).hasSize(2);
    assertThat(group.members().get(0).userId()).isEqualTo(100L);
    assertThat(group.members().get(0).pieceIndex()).isEqualTo(1);
    assertThat(group.members().get(0).departureLatitude()).isEqualTo(37.4979);
    assertThat(group.members().get(0).departureLongitude()).isEqualTo(127.0276);
    assertThat(group.members().get(1).userId()).isNull();
    assertThat(group.members().get(1).pieceIndex()).isEqualTo(2);
    assertThat(group.members().get(1).departureLatitude()).isNull();
    assertThat(group.members().get(1).departureLongitude()).isNull();
    assertThat(response.quickMessages()).hasSize(1);
    assertThat(response.quickMessages().get(0).content()).isEqualTo("지금 출발");
    assertThat(response.quickMessages().get(0).senderId()).isEqualTo(100L);
    assertThat(response.destinationLatitude()).isEqualTo(37.5283);
    assertThat(response.destinationLongitude()).isEqualTo(126.9320);
  }

  @Test
  @DisplayName("존재하지 않는 약속의 진행 데이터를 조회하면 MEETING_NOT_FOUND 예외가 발생한다")
  void throwsWhenMeetingInProgressMeetingNotFound() {
    given(meetingRepository.findById(10L)).willReturn(Optional.empty());

    ApiException exception =
        assertThrows(ApiException.class, () -> meetingService.getMeetingInProgress(100L, 10L));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEETING_NOT_FOUND);
  }

  @Test
  @DisplayName("참여자가 아니면 진행 중인 약속 데이터를 조회할 때 AUTH_FORBIDDEN 예외가 발생한다")
  void throwsWhenMeetingInProgressRequesterNotMember() {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "id", 10L);

    given(meetingRepository.findById(10L)).willReturn(Optional.of(meeting));
    given(meetingMemberRepository.existsByMeetingIdAndUserId(10L, 999L)).willReturn(false);

    ApiException exception =
        assertThrows(ApiException.class, () -> meetingService.getMeetingInProgress(999L, 10L));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_FORBIDDEN);
  }

  @Test
  @DisplayName("방장이 약속방의 초대 코드를 조회한다")
  void returnsInviteCode() {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "id", 10L);
    ReflectionTestUtils.setField(meeting.getHostUser(), "id", 100L);

    given(meetingRepository.findById(10L)).willReturn(Optional.of(meeting));

    MeetingInviteCodeResponse response = meetingService.getInviteCode(100L, 10L);

    assertThat(response.inviteCode()).isEqualTo("ABCD1234");
  }

  @Test
  @DisplayName("존재하지 않는 약속의 초대 코드를 조회하면 MEETING_NOT_FOUND 예외가 발생한다")
  void throwsWhenInviteCodeMeetingNotFound() {
    given(meetingRepository.findById(10L)).willReturn(Optional.empty());

    ApiException exception =
        assertThrows(ApiException.class, () -> meetingService.getInviteCode(100L, 10L));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEETING_NOT_FOUND);
  }

  @Test
  @DisplayName("방장이 아니면 초대 코드를 조회할 때 AUTH_FORBIDDEN 예외가 발생한다")
  void throwsWhenInviteCodeRequesterNotHost() {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "id", 10L);
    ReflectionTestUtils.setField(meeting.getHostUser(), "id", 100L);

    given(meetingRepository.findById(10L)).willReturn(Optional.of(meeting));

    ApiException exception =
        assertThrows(ApiException.class, () -> meetingService.getInviteCode(999L, 10L));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_FORBIDDEN);
  }

  @Test
  @DisplayName("아직 시작되지 않은 약속의 진행 데이터를 조회하면 MEETING_NOT_STARTED 예외가 발생한다")
  void throwsWhenMeetingInProgressMeetingNotStarted() {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "id", 10L);

    given(meetingRepository.findById(10L)).willReturn(Optional.of(meeting));
    given(meetingMemberRepository.existsByMeetingIdAndUserId(10L, 100L)).willReturn(true);

    ApiException exception =
        assertThrows(ApiException.class, () -> meetingService.getMeetingInProgress(100L, 10L));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEETING_NOT_STARTED);
  }

  @Test
  @DisplayName("활성 약속 참여자는 자신의 약속방 닉네임만 수정한다")
  void updatesNicknameForActiveMeetingMember() {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "id", 10L);
    MeetingMember member =
        new MeetingMember(
            meeting,
            meeting.getHostUser(),
            MeetingMemberRole.HOST,
            "이전닉네임",
            "https://img.kakao.com/host.png");
    given(meetingRepository.findById(10L)).willReturn(Optional.of(meeting));
    given(meetingMemberRepository.findByMeetingIdAndUserId(10L, 100L))
        .willReturn(Optional.of(member));

    MeetingMemberNicknameUpdateResponse response =
        meetingService.updateMemberNickname(
            100L, 10L, new MeetingMemberNicknameUpdateRequest("새닉네임"));

    assertThat(member.getNickname()).isEqualTo("새닉네임");
    assertThat(response.meetingId()).isEqualTo(10L);
    assertThat(response.nickname()).isEqualTo("새닉네임");
    assertThat(response.nicknameSet()).isTrue();
  }

  @Test
  @DisplayName("퍼즐 이미지를 교체하면 기존 이미지의 URL이 바뀌고 기본 이미지 표시가 해제된다")
  void replacesExistingMemberImage() {
    MeetingMember member = activeMember("효창");
    givenActiveMember(member);
    MemberImage memberImage = new MemberImage(member, "https://s3.test/puzzles/default.png", true);
    given(memberImageRepository.findByMeetingMemberId(1L)).willReturn(Optional.of(memberImage));
    given(amazonS3Manager.generatePuzzleKeyName(any())).willReturn("puzzles/new.png");
    given(amazonS3Manager.uploadFile(any(), any())).willReturn("https://s3.test/puzzles/new.png");

    MeetingMemberPuzzleImageUpdateResponse response =
        meetingService.updateMemberPuzzleImage(100L, 10L, puzzleImage());

    assertThat(memberImage.getImageUrl()).isEqualTo("https://s3.test/puzzles/new.png");
    assertThat(memberImage.isDefaultImage()).isFalse();
    assertThat(response.meetingId()).isEqualTo(10L);
    assertThat(response.imageUrl()).isEqualTo("https://s3.test/puzzles/new.png");
    assertThat(response.imageSet()).isTrue();
    verify(memberImageRepository, never()).save(any());
    verify(amazonS3Manager, never()).deletePuzzleImage(any());
  }

  @Test
  @DisplayName("업로드한 퍼즐 이미지를 교체하면 기존 S3 객체를 삭제한다")
  void deletesPreviousUploadedMemberImageAfterReplacement() {
    MeetingMember member = activeMember("효창");
    givenActiveMember(member);
    String previousImageUrl = "https://bucket.s3.ap-northeast-2.amazonaws.com/puzzles/old.png";
    MemberImage memberImage = new MemberImage(member, previousImageUrl, false);
    given(memberImageRepository.findByMeetingMemberId(1L)).willReturn(Optional.of(memberImage));
    given(amazonS3Manager.generatePuzzleKeyName(any())).willReturn("puzzles/new.png");
    given(amazonS3Manager.uploadFile(any(), any()))
        .willReturn("https://bucket.s3.ap-northeast-2.amazonaws.com/puzzles/new.png");

    meetingService.updateMemberPuzzleImage(100L, 10L, puzzleImage());

    verify(amazonS3Manager).deletePuzzleImage(previousImageUrl);
  }

  @Test
  @DisplayName("교체할 이미지가 비어 있으면 MEETING_MEMBER_PUZZLE_IMAGE_REQUIRED 예외가 발생한다")
  void rejectsEmptyMemberImage() {
    MockMultipartFile emptyImage =
        new MockMultipartFile("image", "empty.png", "image/png", new byte[0]);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> meetingService.updateMemberPuzzleImage(100L, 10L, emptyImage));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEETING_MEMBER_PUZZLE_IMAGE_REQUIRED);
    verify(amazonS3Manager, never()).uploadFile(any(), any());
  }

  @Test
  @DisplayName("약속이 시작된 뒤에는 퍼즐 이미지를 교체할 수 없다")
  void rejectsMemberImageReplacementAfterMeetingStarted() {
    MeetingMember member = activeMember("효창");
    member.getMeeting().start();
    givenActiveMember(member);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> meetingService.updateMemberPuzzleImage(100L, 10L, puzzleImage()));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEETING_NOT_WAITING);
    verify(amazonS3Manager, never()).uploadFile(any(), any());
  }

  @Test
  @DisplayName("탈퇴한 사용자는 약속 정보를 수정할 수 없다")
  void rejectsMutationFromInactiveUser() {
    given(userRepository.findActiveByIdForUpdate(100L)).willReturn(Optional.empty());

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                meetingService.updateMemberNickname(
                    100L, 10L, new MeetingMemberNicknameUpdateRequest("새닉네임")));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    verify(meetingRepository, never()).findById(any());
  }

  @Test
  @DisplayName("저장된 현재 위치가 목적지 도착 반경 안이면 도착 처리된다")
  void marksMemberArrivedWithinArrivalRadius() {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "id", 10L);
    MeetingMember member =
        new MeetingMember(
            meeting,
            meeting.getHostUser(),
            MeetingMemberRole.HOST,
            "효창",
            "https://img.kakao.com/host.png");
    ReflectionTestUtils.setField(member, "id", 1L);
    member.updateCurrentLocation(BigDecimal.valueOf(37.5283), BigDecimal.valueOf(126.9320));
    givenActiveMember(member);

    MeetingMemberArrivalResponse response = meetingService.markMemberArrived(100L, 10L);

    assertThat(member.getStatus()).isEqualTo(MeetingMemberStatus.ARRIVED);
    assertThat(response.meetingId()).isEqualTo(10L);
    assertThat(response.arrivalTime()).isNotNull();
    verify(applicationEventPublisher).publishEvent(new FriendArrivedEvent(10L, 1L));
  }

  @Test
  @DisplayName("진행 중인 약속에서 마지막 참여자가 도착하면 약속이 완료된다")
  void completesMeetingWhenLastMemberArrives() {
    MeetingMember member = arrivingMemberOfStartedMeeting("효창");
    givenActiveMember(member);
    given(meetingMemberRepository.existsNotArrivedMemberExcluding(10L, 1L)).willReturn(false);

    meetingService.markMemberArrived(100L, 10L);

    assertThat(member.getMeeting().getStatus()).isEqualTo(MeetingStatus.COMPLETED);
  }

  @Test
  @DisplayName("도착하지 않은 참여자가 남아 있으면 약속을 완료하지 않는다")
  void keepsMeetingInProgressWhenAnyMemberHasNotArrived() {
    MeetingMember member = arrivingMemberOfStartedMeeting("효창");
    givenActiveMember(member);
    given(meetingMemberRepository.existsNotArrivedMemberExcluding(10L, 1L)).willReturn(true);

    meetingService.markMemberArrived(100L, 10L);

    assertThat(member.getMeeting().getStatus()).isEqualTo(MeetingStatus.IN_PROGRESS);
  }

  @Test
  @DisplayName("시작하지 않은 약속은 전원이 도착해도 완료하지 않는다")
  void doesNotCompleteWaitingMeetingOnArrival() {
    MeetingMember member = activeMember("효창");
    member.updateCurrentLocation(BigDecimal.valueOf(37.5283), BigDecimal.valueOf(126.9320));
    givenActiveMember(member);

    meetingService.markMemberArrived(100L, 10L);

    assertThat(member.getMeeting().getStatus()).isEqualTo(MeetingStatus.WAITING);
    verify(meetingMemberRepository, never()).existsNotArrivedMemberExcluding(any(), any());
  }

  @Test
  @DisplayName("이미 도착한 참여자를 다시 도착 처리해도 친구 도착 이벤트는 발행하지 않는다")
  void doesNotPublishFriendArrivalEventTwice() {
    MeetingMember member = activeMember("효창");
    member.arrive();
    givenActiveMember(member);

    meetingService.markMemberArrived(100L, 10L);

    verify(applicationEventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("약속 시각이 변경되면 출발 준비 알림 시도 기록을 초기화한다")
  void resetsDepartureReminderAttemptWhenMeetingTimeChanges() {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "id", 10L);
    ReflectionTestUtils.setField(meeting.getHostUser(), "id", 100L);
    LocalDateTime changedMeetingAt = meeting.getMeetingAt().plusHours(1);
    given(meetingRepository.findById(10L)).willReturn(Optional.of(meeting));

    meetingService.updateMeeting(
        100L, 10L, new MeetingUpdateRequest(null, changedMeetingAt, null, null, null, null));

    assertThat(meeting.getMeetingAt()).isEqualTo(changedMeetingAt);
    verify(meetingMemberRepository).resetDepartureReminderAttemptedAtForNotStartedMembers(10L);
  }

  @Test
  @DisplayName("약속 시각을 그대로 두면 출발 준비 알림 시도 기록을 초기화하지 않는다")
  void keepsDepartureReminderAttemptWhenMeetingTimeDoesNotChange() {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "id", 10L);
    ReflectionTestUtils.setField(meeting.getHostUser(), "id", 100L);
    given(meetingRepository.findById(10L)).willReturn(Optional.of(meeting));

    meetingService.updateMeeting(
        100L,
        10L,
        new MeetingUpdateRequest("새 제목", meeting.getMeetingAt(), null, null, null, null));

    assertThat(meeting.getTitle()).isEqualTo("새 제목");
    verify(meetingMemberRepository, never())
        .resetDepartureReminderAttemptedAtForNotStartedMembers(any());
  }

  @Test
  @DisplayName("저장된 현재 위치가 목적지 도착 반경 밖이면 도착 처리에 실패한다")
  void rejectsArrivalOutsideArrivalRadius() {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "id", 10L);
    MeetingMember member =
        new MeetingMember(
            meeting,
            meeting.getHostUser(),
            MeetingMemberRole.HOST,
            "효창",
            "https://img.kakao.com/host.png");
    member.updateCurrentLocation(BigDecimal.valueOf(37.6), BigDecimal.valueOf(126.9320));
    givenActiveMember(member);

    ApiException exception =
        assertThrows(ApiException.class, () -> meetingService.markMemberArrived(100L, 10L));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEETING_ARRIVAL_LOCATION_INVALID);
  }

  @Test
  @DisplayName("출발 설정을 등록하면 이동 경로가 저장되고 예상 소요시간이 초 단위로 갱신된다")
  void createsDepartureWithTransitRoute() {
    MeetingMember member = activeMemberMeetingToday("효창");
    givenActiveMember(member);
    givenRouteSaveEchoesArgument();

    MeetingMemberDepartureResponse response =
        meetingService.createDeparture(
            100L,
            10L,
            departureRequest(
                "서울대학교",
                37.5665,
                126.9780,
                new MeetingMemberDepartureCreateRequest.NicknameSetting(true, "김땡땡"),
                transitRouteRequest()));

    assertThat(member.getEstimatedDurationSeconds()).isEqualTo(2400);
    assertThat(member.getTransportType()).isEqualTo(TransportType.SUBWAY);
    assertThat(member.getTransportLine()).isEqualTo("수도권6호선");
    assertThat(member.getStatus()).isEqualTo(MeetingMemberStatus.MOVING);
    assertThat(response.totalEstimatedTime()).isEqualTo(40);
    assertThat(response.routes()).hasSize(2);
    assertThat(response.routes().get(0).content()).isEqualTo("서울대학교");
    assertThat(response.routes().get(0).transportContent()).isEqualTo("도보");
    assertThat(response.routes().get(0).estimatedTime()).isEqualTo(10);
    assertThat(response.routes().get(1).content()).isEqualTo("태릉입구역 수도권6호선 승차");
    assertThat(response.routes().get(1).transportContent()).isEqualTo("27개 역 이동");
    assertThat(response.routes().get(0).station()).isNull();
    assertThat(response.routes().get(1).station().start()).isEqualTo("태릉입구역");
    assertThat(response.routes().get(1).station().end()).isEqualTo("디지털미디어시티역");
    assertThat(response.nicknameSetting().enabled()).isTrue();
    assertThat(response.nicknameSetting().nickname()).isEqualTo("김땡땡");
  }

  @Test
  @DisplayName("출발 설정을 등록하면 이동 중으로 전이되고 출발 시각이 기록된다")
  void marksMemberAsMovingWhenDepartureIsCreated() {
    MeetingMember member = activeMemberMeetingToday("효창");
    givenActiveMember(member);
    givenRouteSaveEchoesArgument();
    LocalDateTime before = LocalDateTime.now();

    meetingService.createDeparture(
        100L,
        10L,
        departureRequest(
            "서울대학교",
            37.5665,
            126.9780,
            new MeetingMemberDepartureCreateRequest.NicknameSetting(false, null),
            transitRouteRequest()));

    assertThat(member.getStatus()).isEqualTo(MeetingMemberStatus.MOVING);
    assertThat(member.getDepartedAt()).isNotNull();
    assertThat(member.getDepartedAt()).isBetween(before, LocalDateTime.now());
  }

  @Test
  @DisplayName("약속 당일이 아니면 출발 설정 등록에 실패한다")
  void rejectsDepartureBeforeMeetingDay() {
    MeetingMember member = activeMember("효창", LocalDate.now().plusDays(2).atTime(18, 0));
    givenActiveMember(member);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                meetingService.createDeparture(
                    100L,
                    10L,
                    departureRequest(
                        "서울대학교",
                        37.5665,
                        126.9780,
                        new MeetingMemberDepartureCreateRequest.NicknameSetting(false, null),
                        transitRouteRequest())));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEETING_NOT_STARTED);
    assertThat(member.getStatus()).isEqualTo(MeetingMemberStatus.NOT_STARTED);
    assertThat(member.getDepartedAt()).isNull();
  }

  @Test
  @DisplayName("다른 활성 약속에서 이동 중인 참여자는 출발 설정 등록에 실패한다")
  void rejectsDepartureWhenMemberIsMovingInOtherActiveMeeting() {
    MeetingMember member = activeMemberMeetingToday("효창");
    givenActiveMember(member);
    given(meetingMemberRepository.existsMovingMemberInOtherActiveMeeting(100L, 10L))
        .willReturn(true);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                meetingService.createDeparture(
                    100L,
                    10L,
                    departureRequest(
                        "서울대학교",
                        37.5665,
                        126.9780,
                        new MeetingMemberDepartureCreateRequest.NicknameSetting(false, null),
                        transitRouteRequest())));

    assertThat(exception.getErrorCode())
        .isEqualTo(ErrorCode.MEETING_MEMBER_MOVING_IN_OTHER_MEETING);
    assertThat(member.getStatus()).isEqualTo(MeetingMemberStatus.NOT_STARTED);
    verify(meetingMemberRouteRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("다른 활성 약속에 이동 중인 참여자가 없으면 출발 설정을 등록할 수 있다")
  void allowsDepartureWhenMemberIsNotMovingInOtherActiveMeeting() {
    MeetingMember member = activeMemberMeetingToday("효창");
    givenActiveMember(member);
    given(meetingMemberRepository.existsMovingMemberInOtherActiveMeeting(100L, 10L))
        .willReturn(false);
    givenRouteSaveEchoesArgument();

    meetingService.createDeparture(
        100L,
        10L,
        departureRequest(
            "서울대학교",
            37.5665,
            126.9780,
            new MeetingMemberDepartureCreateRequest.NicknameSetting(false, null),
            transitRouteRequest()));

    assertThat(member.getStatus()).isEqualTo(MeetingMemberStatus.MOVING);
  }

  @Test
  @DisplayName("대기 중인 약속은 첫 출발 설정 등록으로 진행 중이 되고 퍼즐 그룹이 배정된다")
  void startsWaitingMeetingOnFirstDeparture() {
    MeetingMember member = activeMemberMeetingToday("효창");
    givenActiveMember(member);
    givenRouteSaveEchoesArgument();
    given(memberImageRepository.findAllByMeetingId(10L))
        .willReturn(List.of(new MemberImage(member, "https://s3.test/puzzles/a.png", false)));

    meetingService.createDeparture(
        100L,
        10L,
        departureRequest(
            "서울대학교",
            37.5665,
            126.9780,
            new MeetingMemberDepartureCreateRequest.NicknameSetting(false, null),
            transitRouteRequest()));

    assertThat(member.getMeeting().getStatus()).isEqualTo(MeetingStatus.IN_PROGRESS);
    verify(puzzlePageRepository).save(any());
    verify(puzzlePieceRepository, times(4)).save(any());
  }

  @Test
  @DisplayName("이미 진행 중인 약속에 출발 설정을 등록하면 퍼즐 그룹을 다시 배정하지 않는다")
  void doesNotReassignPuzzleGroupsWhenMeetingAlreadyStarted() {
    MeetingMember member = activeMemberMeetingToday("효창");
    member.getMeeting().start();
    givenActiveMember(member);
    givenRouteSaveEchoesArgument();

    meetingService.createDeparture(
        100L,
        10L,
        departureRequest(
            "서울대학교",
            37.5665,
            126.9780,
            new MeetingMemberDepartureCreateRequest.NicknameSetting(false, null),
            transitRouteRequest()));

    assertThat(member.getMeeting().getStatus()).isEqualTo(MeetingStatus.IN_PROGRESS);
    verify(puzzlePageRepository, never()).save(any());
    verify(memberImageRepository, never()).findAllByMeetingId(any());
  }

  @Test
  @DisplayName("대중교통 경로가 없으면 도보로 다시 조회하라는 안내가 담긴다")
  void guidesToWalkWhenNoTransitRouteExists() {
    MeetingMember member = activeMember("효창", LocalDateTime.now().plusHours(3));
    givenActiveMember(member);
    givenTransitRoutes(List.of());

    MeetingRouteSearchResponse response =
        meetingService.searchRoutes(
            100L, 10L, searchRequest(37.5283, 126.9325, TravelMode.TRANSIT));

    assertThat(response.routes()).isEmpty();
    assertThat(response.guide().code()).isEqualTo("MEETING_MAP_TOO_CLOSE");
    assertThat(response.guide().message()).isEqualTo(ErrorCode.MEETING_MAP_TOO_CLOSE.getMessage());
    assertThat(response.guide().travelMode()).isEqualTo(TravelMode.WALK);
  }

  @Test
  @DisplayName("대중교통 조회 요청은 출발지 좌표, 약속 장소 좌표, 약속 시각 순으로 전달된다")
  void passesDepartureAndDestinationToTransitRouteFacade() {
    LocalDateTime meetingAt = LocalDateTime.now().plusHours(3);
    MeetingMember member = activeMember("효창", meetingAt);
    givenActiveMember(member);
    givenTransitRoutes(transitRoutes());

    meetingService.searchRoutes(100L, 10L, searchRequest(37.5045, 127.0247, TravelMode.TRANSIT));

    ArgumentCaptor<TransitRouteQuery> query = ArgumentCaptor.forClass(TransitRouteQuery.class);
    verify(transitRouteFacade).findRoutes(query.capture());
    assertThat(query.getValue())
        .isEqualTo(new TransitRouteQuery(37.5045, 127.0247, 37.5283, 126.9320, meetingAt));
  }

  @Test
  @DisplayName("출발지 좌표로 조회하면 약속 장소까지 가는 경로가 구간별로 반환된다")
  void searchesTransitRoutesToMeetingDestination() {
    MeetingMember member = activeMember("효창", LocalDateTime.now().plusHours(3));
    givenActiveMember(member);
    givenTransitRoutes(transitRoutes());

    MeetingRouteSearchResponse response =
        meetingService.searchRoutes(
            100L, 10L, searchRequest(37.5045, 127.0247, TravelMode.TRANSIT));

    assertThat(response.routes()).hasSize(1);
    MeetingRouteSearchResponse.Route route = response.routes().getFirst();
    assertThat(route.totalTime()).isEqualTo(2400);
    assertThat(route.fare()).isEqualTo(1850);
    assertThat(route.transferCount()).isEqualTo(1);
    assertThat(route.pathType()).isEqualTo(3);

    MeetingRouteSearchResponse.Step walkStep = route.steps().getFirst();
    assertThat(walkStep.type()).isEqualTo(TransportType.WALK);
    assertThat(walkStep.description()).isEqualTo("태릉입구역 이동");
    assertThat(walkStep.station().start()).isEqualTo("출발지");
    assertThat(walkStep.station().end()).isEqualTo("태릉입구역");
    assertThat(walkStep.stations()).isNull();
    assertThat(walkStep.startLocation().lat()).isEqualTo(37.5045);

    MeetingRouteSearchResponse.Step subwayStep = route.steps().get(1);
    assertThat(subwayStep.type()).isEqualTo(TransportType.SUBWAY);
    assertThat(subwayStep.line()).isEqualTo("수도권6호선");
    assertThat(subwayStep.color()).isEqualTo("CD7C2F");
    assertThat(subwayStep.description()).isNull();
    assertThat(subwayStep.station().start()).isEqualTo("태릉입구역");
    assertThat(subwayStep.station().end()).isEqualTo("성수역");
    assertThat(subwayStep.stations()).containsExactly("태릉입구역", "성수역");
  }

  @Test
  @DisplayName("약속 시각이 이미 지났으면 차량 경로도 도착 시각 없이 조회한다")
  void searchesCarRouteWithoutArrivalTimeForPastMeeting() {
    MeetingMember member = activeMember("효창", LocalDateTime.now().minusHours(1));
    givenActiveMember(member);
    given(
            tmapCarClient.findCarRoute(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(), any()))
        .willReturn(carRoute());

    meetingService.searchRoutes(100L, 10L, searchRequest(37.5045, 127.0247, TravelMode.CAR));

    ArgumentCaptor<LocalDateTime> arriveAt = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(tmapCarClient)
        .findCarRoute(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(), arriveAt.capture());
    assertThat(arriveAt.getValue()).isNull();
  }

  @Test
  @DisplayName("차량으로 조회하면 약속 시각 도착 기준으로 예상 택시비가 담긴 경로 한 건이 반환된다")
  void searchesCarRouteWithTaxiFare() {
    LocalDateTime meetingAt = LocalDateTime.now().plusHours(3);
    MeetingMember member = activeMember("효창", meetingAt);
    givenActiveMember(member);
    given(
            tmapCarClient.findCarRoute(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(), any()))
        .willReturn(carRoute());

    MeetingRouteSearchResponse response =
        meetingService.searchRoutes(100L, 10L, searchRequest(37.5045, 127.0247, TravelMode.CAR));

    ArgumentCaptor<LocalDateTime> arriveAt = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(tmapCarClient)
        .findCarRoute(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(), arriveAt.capture());
    assertThat(arriveAt.getValue()).isEqualTo(meetingAt);
    verify(transitRouteFacade, never()).findRoutes(any());

    assertThat(response.routes()).hasSize(1);
    MeetingRouteSearchResponse.Route route = response.routes().getFirst();
    assertThat(route.totalTime()).isEqualTo(2967);
    assertThat(route.fare()).isEqualTo(35400);
    assertThat(route.transferCount()).isZero();
    assertThat(route.pathType()).isNull();
    assertThat(route.steps()).hasSize(1);
    assertThat(route.steps().getFirst().type()).isEqualTo(TransportType.CAR);
  }

  @Test
  @DisplayName("도보로 조회하면 다시 조회하지 않고 경로 한 건이 반환된다")
  void searchesWalkingRouteOnce() {
    MeetingMember member = activeMember("효창", LocalDateTime.now().plusHours(3));
    givenActiveMember(member);
    given(
            tmapPedestrianClient.findWalkingRoute(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
        .willReturn(walkingRoute());

    MeetingRouteSearchResponse response =
        meetingService.searchRoutes(100L, 10L, searchRequest(37.5045, 127.0247, TravelMode.WALK));

    verify(tmapPedestrianClient)
        .findWalkingRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any());
    verify(transitRouteFacade, never()).findRoutes(any());

    assertThat(response.routes()).hasSize(1);
    MeetingRouteSearchResponse.Route route = response.routes().getFirst();
    assertThat(route.fare()).isZero();
    assertThat(route.steps()).hasSize(1);
    assertThat(route.steps().getFirst().type()).isEqualTo(TransportType.WALK);
  }

  @Test
  @DisplayName("차량으로 출발 설정하면 선택한 이동수단이 저장되고 차량 이동으로 표시된다")
  void createsDepartureWithCarRoute() {
    MeetingMember member = activeMemberMeetingToday("효창");
    givenActiveMember(member);
    givenRouteSaveEchoesArgument();

    MeetingMemberDepartureResponse response =
        meetingService.createDeparture(
            100L,
            10L,
            new MeetingMemberDepartureCreateRequest(
                new MeetingMemberDepartureCreateRequest.Departure("서울대학교", 37.5665, 126.9780),
                new MeetingMemberDepartureCreateRequest.NotificationSettings(true, true, false),
                new MeetingMemberDepartureCreateRequest.NicknameSetting(false, null),
                carRouteRequest(),
                TravelMode.CAR));

    assertThat(member.getTravelMode()).isEqualTo(TravelMode.CAR);
    assertThat(member.getTransportType()).isEqualTo(TransportType.CAR);
    assertThat(response.travelMode()).isEqualTo(TravelMode.CAR);
    assertThat(response.routes()).hasSize(1);
    assertThat(response.routes().getFirst().content()).isEqualTo("서울대학교");
    assertThat(response.routes().getFirst().transportContent()).isEqualTo("차량 이동");
    assertThat(response.routes().getFirst().station()).isNull();
  }

  @Test
  @DisplayName("대중교통 공급자가 던진 오류는 그대로 클라이언트에 전달된다")
  void propagatesTransitProviderError() {
    MeetingMember member = activeMember("효창", LocalDateTime.now().plusHours(3));
    givenActiveMember(member);
    given(transitRouteFacade.findRoutes(any()))
        .willThrow(ApiException.of(ErrorCode.MEETING_MAP_TOO_CLOSE));

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                meetingService.searchRoutes(
                    100L, 10L, searchRequest(37.5283, 126.9325, TravelMode.TRANSIT)));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEETING_MAP_TOO_CLOSE);
  }

  @Test
  @DisplayName("닉네임 사용에 동의했는데 닉네임이 비어 있으면 출발 설정에 실패한다")
  void rejectsDepartureWhenCustomNicknameIsBlank() {
    MeetingMember member = activeMemberMeetingToday("효창");
    givenActiveMember(member);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                meetingService.createDeparture(
                    100L,
                    10L,
                    departureRequest(
                        "서울대학교",
                        37.5665,
                        126.9780,
                        new MeetingMemberDepartureCreateRequest.NicknameSetting(true, " "),
                        transitRouteRequest())));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
  }

  @Test
  @DisplayName("닉네임 사용에 동의하지 않으면 약속방 닉네임이 사용자 기본 닉네임으로 저장된다")
  void fallsBackToUserNicknameWhenCustomNicknameDisabled() {
    MeetingMember member = activeMemberMeetingToday("이전닉네임");
    givenActiveMember(member);
    givenRouteSaveEchoesArgument();

    MeetingMemberDepartureResponse response =
        meetingService.createDeparture(
            100L,
            10L,
            departureRequest(
                "서울대학교",
                37.5665,
                126.9780,
                new MeetingMemberDepartureCreateRequest.NicknameSetting(false, null),
                transitRouteRequest()));

    assertThat(member.getNickname()).isEqualTo("효창");
    assertThat(member.isCustomNickname()).isFalse();
    assertThat(response.nicknameSetting().enabled()).isFalse();
  }

  @Test
  @DisplayName("이미 출발 설정을 마친 참여자가 다시 등록하면 실패한다")
  void rejectsDuplicatedDeparture() {
    MeetingMember member = activeMemberMeetingToday("효창");
    member.updateDeparture(
        "서울대학교", BigDecimal.valueOf(37.5665), BigDecimal.valueOf(126.9780), TravelMode.TRANSIT);
    givenActiveMember(member);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                meetingService.createDeparture(
                    100L,
                    10L,
                    departureRequest(
                        "서울역",
                        37.5547,
                        126.9707,
                        new MeetingMemberDepartureCreateRequest.NicknameSetting(false, null),
                        transitRouteRequest())));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEETING_DEPARTURE_ALREADY_SET);
  }

  @Test
  @DisplayName("출발 설정을 하지 않은 참여자가 조회하면 실패한다")
  void rejectsDepartureLookupBeforeItIsSet() {
    MeetingMember member = activeMember("효창");
    givenActiveMember(member);

    ApiException exception =
        assertThrows(ApiException.class, () -> meetingService.getDeparture(100L, 10L));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEETING_DEPARTURE_NOT_FOUND);
  }

  @Test
  @DisplayName("출발지를 수정하면 이동 경로를 다시 계산해 기존 경로를 전부 교체한다")
  void replacesRoutesWhenDepartureChanges() {
    MeetingMember member = activeMember("효창");
    member.updateDeparture(
        "서울역", BigDecimal.valueOf(37.5547), BigDecimal.valueOf(126.9707), TravelMode.TRANSIT);
    givenActiveMember(member);
    givenRouteSaveEchoesArgument();

    MeetingMemberDepartureResponse response =
        meetingService.updateDeparture(
            100L,
            10L,
            new MeetingMemberDepartureUpdateRequest(
                new MeetingMemberDepartureUpdateRequest.Departure("서울대학교", 37.5665, 126.9780),
                null,
                null,
                transitRouteRequest(),
                TravelMode.TRANSIT));

    verify(meetingMemberRouteRepository).deleteAllByMeetingMemberId(1L);
    assertThat(member.getDepartureName()).isEqualTo("서울대학교");
    assertThat(response.routes()).hasSize(2);
  }

  @Test
  @DisplayName("출발지만 넣고 선택한 경로를 빼면 출발 설정 수정에 실패한다")
  void rejectsDepartureUpdateWithoutSelectedRoute() {
    MeetingMember member = activeMember("효창");
    member.updateDeparture(
        "서울역", BigDecimal.valueOf(37.5547), BigDecimal.valueOf(126.9707), TravelMode.TRANSIT);
    givenActiveMember(member);

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                meetingService.updateDeparture(
                    100L,
                    10L,
                    new MeetingMemberDepartureUpdateRequest(
                        new MeetingMemberDepartureUpdateRequest.Departure(
                            "서울대학교", 37.5665, 126.9780),
                        null,
                        null,
                        null,
                        null)));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
  }

  @Test
  @DisplayName("알림 설정만 수정하면 지도 API를 호출하지 않는다")
  void doesNotCallMapApiWhenOnlyNotificationSettingsChange() {
    MeetingMember member = activeMember("효창");
    member.updateDeparture(
        "서울역", BigDecimal.valueOf(37.5547), BigDecimal.valueOf(126.9707), TravelMode.TRANSIT);
    givenActiveMember(member);
    given(meetingMemberRouteRepository.findAllByMeetingMemberIdOrderByRouteIndexAsc(1L))
        .willReturn(List.of());

    MeetingMemberDepartureResponse response =
        meetingService.updateDeparture(
            100L,
            10L,
            new MeetingMemberDepartureUpdateRequest(
                null,
                new MeetingMemberDepartureUpdateRequest.NotificationSettings(false, true, true),
                null,
                null,
                null));

    verify(transitRouteFacade, never()).findRoutes(any());
    assertThat(member.isLocationNotificationEnabled()).isFalse();
    assertThat(member.isChatBubbleNotificationEnabled()).isTrue();
    assertThat(response.departure().placeName()).isEqualTo("서울역");
  }

  @Test
  @DisplayName("출발지가 약속 장소와 너무 가까우면 도보 한 구간으로 출발 설정이 완료된다")
  void createsWalkingRouteWhenDepartureIsTooClose() {
    MeetingMember member = activeMemberMeetingToday("효창");
    givenActiveMember(member);
    givenRouteSaveEchoesArgument();

    MeetingMemberDepartureResponse response =
        meetingService.createDeparture(
            100L,
            10L,
            departureRequest(
                "여의도 나루터",
                37.5290,
                126.9320,
                new MeetingMemberDepartureCreateRequest.NicknameSetting(false, null),
                walkingRouteRequest()));

    assertThat(response.routes()).hasSize(1);
    assertThat(response.routes().get(0).transportType()).isEqualTo(TransportType.WALK);
    assertThat(response.routes().get(0).transportContent()).isEqualTo("도보");
    assertThat(response.routes().get(0).content()).isEqualTo("여의도 나루터");
    assertThat(response.totalEstimatedTime()).isEqualTo(1);
  }

  @Test
  @DisplayName("참여자가 약속방을 나가면 이동 경로와 퍼즐 이미지까지 함께 삭제된다")
  void deletesMemberWithRoutesAndImageOnLeave() {
    MeetingMember member = activeGuestMember("김땡땡");
    givenActiveMember(member);
    String imageUrl = "https://bucket.s3.ap-northeast-2.amazonaws.com/puzzles/guest.png";
    given(memberImageRepository.findByMeetingMemberId(1L))
        .willReturn(Optional.of(new MemberImage(member, imageUrl, false)));

    meetingService.leaveMeeting(100L, 10L);

    verify(meetingMemberRouteRepository).deleteAllByMeetingMemberId(1L);
    verify(memberImageRepository).deleteAllByMeetingMemberId(1L);
    verify(meetingMemberRepository).delete(member);
    verify(amazonS3Manager).deletePuzzleImage(imageUrl);
  }

  @Test
  @DisplayName("방장이 약속방 나가기를 요청하면 MEETING_HOST_CANNOT_LEAVE 예외가 발생한다")
  void rejectsLeaveRequestFromHost() {
    MeetingMember host = activeMember("효창");
    givenActiveMember(host);

    ApiException exception =
        assertThrows(ApiException.class, () -> meetingService.leaveMeeting(100L, 10L));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEETING_HOST_CANNOT_LEAVE);
    verify(meetingMemberRepository, never()).delete(any());
  }

  @Test
  @DisplayName("약속 다음 날 자정이 지나지 않으면 종료 처리를 미룬다")
  void keepsMeetingInProgressWithinGracePeriod() {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "meetingAt", LocalDate.now().minusDays(1).atTime(20, 0));
    given(meetingRepository.findAllExpired(any())).willReturn(List.of(meeting));

    meetingService.completeExpiredMeetings();

    assertThat(meeting.getStatus()).isNotEqualTo(MeetingStatus.COMPLETED);
  }

  @Test
  @DisplayName("약속 다음 날 자정이 지나면 종료 처리한다")
  void completesMeetingAfterGracePeriodEnds() {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "meetingAt", LocalDate.now().minusDays(2).atTime(20, 0));
    given(meetingRepository.findAllExpired(any())).willReturn(List.of(meeting));

    meetingService.completeExpiredMeetings();

    assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.COMPLETED);
  }

  private MeetingMember activeMember(String nickname) {
    return activeMember(nickname, LocalDateTime.now().plusHours(3));
  }

  private MeetingMember activeMemberMeetingToday(String nickname) {
    return activeMember(nickname, LocalDate.now().atTime(23, 30));
  }

  private MeetingMember activeMember(String nickname, LocalDateTime meetingAt) {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "id", 10L);
    ReflectionTestUtils.setField(meeting, "meetingAt", meetingAt);
    MeetingMember member =
        new MeetingMember(
            meeting,
            meeting.getHostUser(),
            MeetingMemberRole.HOST,
            nickname,
            "https://img.kakao.com/host.png");
    ReflectionTestUtils.setField(member, "id", 1L);
    return member;
  }

  private MeetingMember arrivingMemberOfStartedMeeting(String nickname) {
    MeetingMember member = activeMember(nickname);
    member.getMeeting().start();
    member.updateCurrentLocation(BigDecimal.valueOf(37.5283), BigDecimal.valueOf(126.9320));
    return member;
  }

  private MeetingMember activeGuestMember(String nickname) {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "id", 10L);
    User guest = new User(200L, nickname, "https://img.kakao.com/b.jpg");
    MeetingMember member =
        new MeetingMember(
            meeting, guest, MeetingMemberRole.GUEST, nickname, "https://img.kakao.com/guest.png");
    ReflectionTestUtils.setField(member, "id", 1L);
    return member;
  }

  private MockMultipartFile puzzleImage() {
    return new MockMultipartFile("image", "puzzle.png", "image/png", "puzzle".getBytes());
  }

  private void givenActiveMember(MeetingMember member) {
    given(meetingRepository.findById(10L)).willReturn(Optional.of(member.getMeeting()));
    lenient()
        .when(meetingRepository.findByIdForUpdate(10L))
        .thenReturn(Optional.of(member.getMeeting()));
    given(meetingMemberRepository.findByMeetingIdAndUserId(10L, 100L))
        .willReturn(Optional.of(member));
  }

  private void givenRouteSaveEchoesArgument() {
    given(meetingMemberRouteRepository.saveAll(any()))
        .willAnswer(invocation -> new ArrayList<MeetingMemberRoute>(invocation.getArgument(0)));
  }

  private void givenTransitRoutes(List<TravelRoute> routes) {
    given(transitRouteFacade.findRoutes(any())).willReturn(routes);
  }

  private List<TravelRoute> transitRoutes() {
    return List.of(
        new TravelRoute(
            2400,
            1850,
            1,
            3,
            List.of(
                new TravelRoute.Leg(
                    TransportType.WALK,
                    null,
                    null,
                    600,
                    420,
                    "출발지",
                    "태릉입구역",
                    37.5045,
                    127.0247,
                    37.5017,
                    127.0256,
                    List.of(),
                    null),
                new TravelRoute.Leg(
                    TransportType.SUBWAY,
                    "수도권6호선",
                    "CD7C2F",
                    1800,
                    27000,
                    "태릉입구역",
                    "성수역",
                    37.5017,
                    127.0256,
                    37.5446,
                    127.0559,
                    List.of("태릉입구역", "성수역"),
                    null))));
  }

  private List<TravelRoute> longTransitRoutes() {
    return List.of(
        new TravelRoute(
            5400,
            2150,
            2,
            3,
            List.of(
                new TravelRoute.Leg(
                    TransportType.SUBWAY,
                    "수인분당선",
                    "F5A200",
                    5400,
                    41000,
                    "죽전역",
                    "강남역",
                    37.3245,
                    127.1076,
                    37.4979,
                    127.0276,
                    List.of("죽전역", "강남역"),
                    null))));
  }

  private MeetingMemberDepartureCreateRequest departureRequest(
      String placeName,
      double latitude,
      double longitude,
      MeetingMemberDepartureCreateRequest.NicknameSetting nicknameSetting,
      MeetingRouteRequest route) {
    return new MeetingMemberDepartureCreateRequest(
        new MeetingMemberDepartureCreateRequest.Departure(placeName, latitude, longitude),
        new MeetingMemberDepartureCreateRequest.NotificationSettings(true, true, false),
        nicknameSetting,
        route,
        TravelMode.TRANSIT);
  }

  private TravelRoute carRoute() {
    return new TravelRoute(
        2967,
        35400,
        0,
        null,
        List.of(
            new TravelRoute.Leg(
                TransportType.CAR,
                null,
                null,
                2967,
                36945,
                null,
                "서울 여의도 한강공원",
                37.5045,
                127.0247,
                37.5283,
                126.9320,
                List.of(),
                null)));
  }

  private TravelRoute walkingRoute() {
    return new TravelRoute(
        286,
        0,
        0,
        null,
        List.of(
            new TravelRoute.Leg(
                TransportType.WALK,
                null,
                null,
                286,
                368,
                null,
                "서울 여의도 한강공원",
                37.5045,
                127.0247,
                37.5283,
                126.9320,
                List.of(),
                null)));
  }

  private MeetingRouteRequest carRouteRequest() {
    return new MeetingRouteRequest(
        2967,
        List.of(
            new MeetingRouteRequest.Step(
                TransportType.CAR,
                2967,
                null,
                new MeetingRouteRequest.Station(null, "서울 여의도 한강공원"),
                null)));
  }

  private MeetingRouteSearchRequest searchRequest(
      double latitude, double longitude, TravelMode travelMode) {
    return new MeetingRouteSearchRequest(
        new MeetingRouteSearchRequest.Start(latitude, longitude), travelMode);
  }

  private MeetingRouteRequest transitRouteRequest() {
    return new MeetingRouteRequest(
        2400,
        List.of(
            new MeetingRouteRequest.Step(
                TransportType.WALK,
                600,
                null,
                new MeetingRouteRequest.Station(null, "태릉입구역"),
                null),
            new MeetingRouteRequest.Step(
                TransportType.SUBWAY,
                1800,
                "수도권6호선",
                new MeetingRouteRequest.Station("태릉입구역", "디지털미디어시티역"),
                stationNames(27))));
  }

  private MeetingRouteRequest walkingRouteRequest() {
    return new MeetingRouteRequest(
        60,
        List.of(
            new MeetingRouteRequest.Step(
                TransportType.WALK,
                60,
                null,
                new MeetingRouteRequest.Station(null, "서울 여의도 한강공원"),
                null)));
  }

  private List<String> stationNames(int stationCount) {
    return IntStream.rangeClosed(0, stationCount).mapToObj(index -> "역" + index).toList();
  }

  private Meeting waitingMeeting() {
    User host = new User(100L, "효창", "https://img.kakao.com/a.jpg");
    return new Meeting(
        host,
        "한강 피크닉",
        LocalDateTime.of(2026, 8, 10, 14, 0),
        "서울 여의도 한강공원",
        null,
        BigDecimal.valueOf(37.5283),
        BigDecimal.valueOf(126.9320),
        50,
        100,
        "ABCD1234",
        null);
  }
}
