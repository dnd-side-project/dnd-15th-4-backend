package com.dnd.puzzlemeet.domain.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.dnd.puzzlemeet.TestcontainersConfiguration;
import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRole;
import com.dnd.puzzlemeet.domain.meeting.entity.ReactionMessage;
import com.dnd.puzzlemeet.domain.meeting.entity.ReactionPreset;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRouteRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.ReactionMessageRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.ReactionPresetRepository;
import com.dnd.puzzlemeet.domain.puzzle.entity.MemberImage;
import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzlePage;
import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzlePiece;
import com.dnd.puzzlemeet.domain.puzzle.repository.MemberImageRepository;
import com.dnd.puzzlemeet.domain.puzzle.repository.PuzzleCollectionRepository;
import com.dnd.puzzlemeet.domain.puzzle.repository.PuzzlePageRepository;
import com.dnd.puzzlemeet.domain.puzzle.repository.PuzzlePieceRepository;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.s3.AmazonS3Manager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MeetingLeaveIntegrationTest {

  @Autowired private MeetingService meetingService;
  @Autowired private UserRepository userRepository;
  @Autowired private MeetingRepository meetingRepository;
  @Autowired private MeetingMemberRepository meetingMemberRepository;
  @Autowired private MeetingMemberRouteRepository meetingMemberRouteRepository;
  @Autowired private MemberImageRepository memberImageRepository;
  @Autowired private PuzzlePageRepository puzzlePageRepository;
  @Autowired private PuzzlePieceRepository puzzlePieceRepository;
  @Autowired private ReactionPresetRepository reactionPresetRepository;
  @Autowired private ReactionMessageRepository reactionMessageRepository;
  @Autowired private PuzzleCollectionRepository puzzleCollectionRepository;

  @MockitoBean private AmazonS3Manager amazonS3Manager;

  @BeforeEach
  void cleanBeforeTest() {
    deleteTestData();
  }

  @AfterEach
  void cleanAfterTest() {
    deleteTestData();
  }

  @Test
  @DisplayName("퍼즐 배정과 퀵메시지가 있는 참여자도 약속방을 나갈 수 있고 조각은 시스템 채움으로 회수된다")
  void leavesMeetingAfterPuzzleAssignmentWithoutViolatingForeignKeys() {
    User host = userRepository.save(new User(10_001L, "효창", "https://img.example/host.png"));
    User guest = userRepository.save(new User(10_002L, "김땡땡", "https://img.example/guest.png"));

    Meeting meeting = meetingRepository.save(inProgressMeeting(host));
    MeetingMember hostMember = saveMember(meeting, host, MeetingMemberRole.HOST, "효창");
    MeetingMember guestMember = saveMember(meeting, guest, MeetingMemberRole.GUEST, "김땡땡");

    MemberImage hostImage =
        memberImageRepository.save(
            new MemberImage(hostMember, "https://img.example/puzzles/host.png", false));
    MemberImage guestImage =
        memberImageRepository.save(
            new MemberImage(guestMember, "https://img.example/puzzles/guest.png", false));

    PuzzlePage page = new PuzzlePage(meeting, 1);
    page.selectRepresentativeImage(guestImage);
    page = puzzlePageRepository.save(page);
    PuzzlePiece hostPiece = puzzlePieceRepository.save(new PuzzlePiece(page, hostMember, (byte) 1));
    PuzzlePiece guestPiece =
        puzzlePieceRepository.save(new PuzzlePiece(page, guestMember, (byte) 2));

    ReactionPreset preset = reactionPresetRepository.save(new ReactionPreset("곧 도착해요"));
    reactionMessageRepository.save(new ReactionMessage(guestMember, preset, preset.getContent()));

    assertThatCode(() -> meetingService.leaveMeeting(guest.getId(), meeting.getId()))
        .doesNotThrowAnyException();

    assertThat(meetingMemberRepository.findById(guestMember.getId())).isEmpty();
    assertThat(memberImageRepository.findById(guestImage.getId())).isEmpty();
    assertThat(reactionMessageRepository.findAll()).isEmpty();

    PuzzlePiece releasedPiece = puzzlePieceRepository.findById(guestPiece.getId()).orElseThrow();
    assertThat(releasedPiece.getMeetingMember()).isNull();
    assertThat(releasedPiece.isSystemFilled()).isTrue();

    PuzzlePiece keptPiece = puzzlePieceRepository.findById(hostPiece.getId()).orElseThrow();
    assertThat(keptPiece.getMeetingMember().getId()).isEqualTo(hostMember.getId());

    PuzzlePage reloadedPage = puzzlePageRepository.findById(page.getId()).orElseThrow();
    assertThat(reloadedPage.getRepresentativeMemberImage().getId()).isEqualTo(hostImage.getId());
  }

  private Meeting inProgressMeeting(User host) {
    Meeting meeting =
        new Meeting(
            host,
            "한강 피크닉",
            LocalDate.now().atTime(23, 30),
            "서울 여의도 한강공원",
            null,
            BigDecimal.valueOf(37.5283),
            BigDecimal.valueOf(126.9320),
            500,
            6,
            "leaveTest",
            null);
    meeting.start();
    return meeting;
  }

  private MeetingMember saveMember(
      Meeting meeting, User user, MeetingMemberRole role, String nickname) {
    return meetingMemberRepository.save(
        new MeetingMember(meeting, user, role, nickname, "https://img.example/profile.png"));
  }

  private void deleteTestData() {
    reactionMessageRepository.deleteAll();
    reactionPresetRepository.deleteAll();
    puzzleCollectionRepository.deleteAll();
    puzzlePieceRepository.deleteAll();
    puzzlePageRepository.deleteAll();
    memberImageRepository.deleteAll();
    meetingMemberRouteRepository.deleteAll();
    meetingMemberRepository.deleteAll();
    meetingRepository.deleteAll();
    userRepository.deleteAll();
  }
}
