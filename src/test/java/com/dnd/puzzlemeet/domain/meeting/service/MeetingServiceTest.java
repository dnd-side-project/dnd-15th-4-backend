package com.dnd.puzzlemeet.domain.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import com.dnd.puzzlemeet.domain.meeting.dto.MeetingPreviewRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingPreviewResponse;
import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRole;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingRepository;
import com.dnd.puzzlemeet.domain.puzzle.repository.MemberImageRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

  @Mock private MeetingRepository meetingRepository;
  @Mock private MeetingMemberRepository meetingMemberRepository;
  @Mock private MemberImageRepository memberImageRepository;
  @Mock private UserRepository userRepository;
  @Mock private AmazonS3Manager amazonS3Manager;

  private MeetingService meetingService;

  @BeforeEach
  void setUp() {
    meetingService =
        new MeetingService(
            meetingRepository,
            meetingMemberRepository,
            memberImageRepository,
            userRepository,
            amazonS3Manager);
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
