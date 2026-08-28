package com.dnd.puzzlemeet.domain.puzzle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRole;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.puzzle.dto.MeetingCollectionResponse;
import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzleCollection;
import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzlePage;
import com.dnd.puzzlemeet.domain.puzzle.repository.PuzzleCollectionRepository;
import com.dnd.puzzlemeet.domain.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PuzzleServiceTest {

  @Mock private PuzzleCollectionRepository puzzleCollectionRepository;
  @Mock private MeetingMemberRepository meetingMemberRepository;

  private PuzzleService puzzleService;

  @BeforeEach
  void setUp() {
    puzzleService = new PuzzleService(puzzleCollectionRepository, meetingMemberRepository);
  }

  @Test
  @DisplayName("내가 모은 퍼즐 조회는 약속방에서 배정받은 프로필 이미지를 반환한다")
  void returnsMeetingMemberProfileImageUrl() {
    User user = new User(100L, "효창", null);
    Meeting meeting = completedMeeting(user);
    MeetingMember member =
        new MeetingMember(
            meeting,
            user,
            MeetingMemberRole.HOST,
            "효창",
            "https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/profiles/1.png");
    member.arrive();
    PuzzlePage page = new PuzzlePage(meeting, 1);
    PuzzleCollection collection =
        new PuzzleCollection(user, page, "https://s3.test/puzzles/complete.png");

    given(puzzleCollectionRepository.findAllByUserIdFetchMeeting(100L))
        .willReturn(List.of(collection));
    given(meetingMemberRepository.findAllByMeetingIdInFetchUser(List.of(10L)))
        .willReturn(List.of(member));

    List<MeetingCollectionResponse> responses = puzzleService.getMyPuzzleCollections(100L);

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).rankings().get(0).profileImageUrl())
        .isEqualTo("https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/profiles/1.png");
  }

  private Meeting completedMeeting(User host) {
    Meeting meeting =
        new Meeting(
            host,
            "한강 피크닉",
            LocalDateTime.now().plusHours(1),
            "서울 여의도 한강공원",
            null,
            BigDecimal.valueOf(37.5283),
            BigDecimal.valueOf(126.9320),
            50,
            100,
            "ABCD1234",
            null);
    ReflectionTestUtils.setField(meeting, "id", 10L);
    return meeting;
  }
}
