package com.dnd.puzzlemeet.domain.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.dnd.puzzlemeet.domain.meeting.dto.MeetingInProgressResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingPreviewRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingPreviewResponse;
import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRole;
import com.dnd.puzzlemeet.domain.meeting.entity.ReactionMessage;
import com.dnd.puzzlemeet.domain.meeting.entity.ReactionPreset;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.ReactionMessageRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

  @Mock private MeetingRepository meetingRepository;
  @Mock private MeetingMemberRepository meetingMemberRepository;
  @Mock private MemberImageRepository memberImageRepository;
  @Mock private UserRepository userRepository;
  @Mock private AmazonS3Manager amazonS3Manager;
  @Mock private PuzzlePageRepository puzzlePageRepository;
  @Mock private PuzzlePieceRepository puzzlePieceRepository;
  @Mock private PuzzleCollectionRepository puzzleCollectionRepository;
  @Mock private ReactionMessageRepository reactionMessageRepository;

  private MeetingService meetingService;

  @BeforeEach
  void setUp() {
    meetingService =
        new MeetingService(
            meetingRepository,
            meetingMemberRepository,
            memberImageRepository,
            userRepository,
            amazonS3Manager,
            puzzlePageRepository,
            puzzlePieceRepository,
            puzzleCollectionRepository,
            reactionMessageRepository);
  }

  @Test
  @DisplayName("대기 중인 약속을 초대 코드로 조회하면 참여자 목록을 포함해 반환한다")
  void returnsMeetingPreviewWithParticipants() {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "id", 10L);
    MeetingMember member =
        new MeetingMember(meeting, meeting.getHostUser(), MeetingMemberRole.HOST, "효창");

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
  @DisplayName("진행 중인 약속 데이터를 조회하면 퍼즐 그룹과 퀵메시지, 완료 여부를 반환한다")
  void returnsMeetingInProgressData() {
    Meeting meeting = waitingMeeting();
    ReflectionTestUtils.setField(meeting, "id", 10L);
    ReflectionTestUtils.setField(meeting.getHostUser(), "id", 100L);
    meeting.complete();

    MeetingMember member =
        new MeetingMember(meeting, meeting.getHostUser(), MeetingMemberRole.HOST, "효창");
    ReflectionTestUtils.setField(member, "id", 1L);

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
    assertThat(group.members().get(1).userId()).isNull();
    assertThat(group.members().get(1).pieceIndex()).isEqualTo(2);
    assertThat(response.quickMessages()).hasSize(1);
    assertThat(response.quickMessages().get(0).content()).isEqualTo("지금 출발");
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
        "ABCD1234",
        null);
  }
}
