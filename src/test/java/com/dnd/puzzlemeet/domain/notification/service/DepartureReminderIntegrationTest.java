package com.dnd.puzzlemeet.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dnd.puzzlemeet.TestcontainersConfiguration;
import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRole;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingRepository;
import com.dnd.puzzlemeet.domain.notification.event.DepartureReminderClaimedEvent;
import com.dnd.puzzlemeet.domain.notification.repository.PushSubscriptionRepository;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.support.TransactionTemplate;

@Import({TestcontainersConfiguration.class, DepartureReminderIntegrationTest.EventTestConfig.class})
@SpringBootTest
class DepartureReminderIntegrationTest {

  private static final LocalDateTime NOW = LocalDate.now().plusDays(1).atTime(15, 0);

  @Autowired private DepartureReminderQueryService departureReminderQueryService;
  @Autowired private DepartureReminderClaimService departureReminderClaimService;
  @Autowired private MeetingMemberRepository meetingMemberRepository;
  @Autowired private MeetingRepository meetingRepository;
  @Autowired private PushSubscriptionRepository pushSubscriptionRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private DepartureReminderEventCounter eventCounter;

  private long sequence;

  @BeforeEach
  void cleanBeforeTest() {
    deleteTestData();
    eventCounter.reset();
    sequence = 0L;
  }

  @AfterEach
  void cleanAfterTest() {
    deleteTestData();
  }

  @Test
  @DisplayName("출발 리마인더 후보는 활성 약속과 사용자 및 60분 시간창의 미출발 미시도 참여자만 포함한다")
  void findsOnlyEligibleDepartureReminderCandidates() {
    MeetingMember waiting =
        saveMember(MeetingStatus.WAITING, NOW.plusMinutes(10), false, false, false);
    MeetingMember inProgress =
        saveMember(MeetingStatus.IN_PROGRESS, NOW.plusMinutes(20), false, false, false);
    saveMember(MeetingStatus.COMPLETED, NOW.plusMinutes(20), false, false, false);
    saveMember(MeetingStatus.CANCELED, NOW.plusMinutes(20), false, false, false);
    saveMember(MeetingStatus.WAITING, NOW, false, false, false);
    MeetingMember atWindowEnd =
        saveMember(MeetingStatus.WAITING, NOW.plusMinutes(60), false, false, false);
    saveMember(MeetingStatus.WAITING, NOW.plusMinutes(60).plusSeconds(1), false, false, false);
    saveMember(MeetingStatus.WAITING, NOW.plusMinutes(30), true, false, false);
    saveMember(MeetingStatus.WAITING, NOW.plusMinutes(30), false, true, false);
    saveMember(MeetingStatus.WAITING, NOW.plusMinutes(30), false, false, true);

    List<Long> candidateIds =
        departureReminderQueryService.findCandidates(NOW, NOW.plusMinutes(60)).stream()
            .map(MeetingMemberRepository.DepartureReminderCandidate::getMemberId)
            .toList();

    assertThat(candidateIds)
        .containsExactly(waiting.getId(), inProgress.getId(), atWindowEnd.getId());
  }

  @Test
  @DisplayName("두 실행자가 같은 참여자를 동시에 선점해도 한 번만 성공하고 이벤트도 한 번만 발행된다")
  void claimsSameCandidateOnlyOnceConcurrently() throws Exception {
    MeetingMember member =
        saveMember(MeetingStatus.IN_PROGRESS, NOW.plusMinutes(30), false, false, false);
    Candidate candidate =
        new Candidate(member.getId(), member.getMeeting().getId(), member.getUser().getId());
    CountDownLatch readyGate = new CountDownLatch(2);
    CountDownLatch startGate = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<Boolean> first =
          executor.submit(() -> claimAfterGates(candidate, readyGate, startGate));
      Future<Boolean> second =
          executor.submit(() -> claimAfterGates(candidate, readyGate, startGate));

      assertThat(readyGate.await(5, TimeUnit.SECONDS)).isTrue();
      startGate.countDown();
      assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(true, false);
    } finally {
      executor.shutdownNow();
    }

    assertThat(eventCounter.count()).isEqualTo(1);
    assertThat(
            meetingMemberRepository
                .findById(member.getId())
                .orElseThrow()
                .getDepartureReminderAttemptedAt())
        .isEqualTo(NOW);
  }

  @Test
  @DisplayName("약속 시각 변경을 위한 초기화는 미출발 참여자의 리마인더 시도 시각만 지운다")
  void resetsAttemptOnlyForNotStartedMembers() {
    MeetingMember notStarted =
        saveMember(MeetingStatus.WAITING, NOW.plusMinutes(30), false, false, true);
    MeetingMember moving =
        saveMember(MeetingStatus.WAITING, NOW.plusMinutes(30), true, false, true);

    transactionTemplate.executeWithoutResult(
        status ->
            meetingMemberRepository.resetDepartureReminderAttemptedAtForNotStartedMembers(
                notStarted.getMeeting().getId()));
    transactionTemplate.executeWithoutResult(
        status ->
            meetingMemberRepository.resetDepartureReminderAttemptedAtForNotStartedMembers(
                moving.getMeeting().getId()));

    assertThat(
            meetingMemberRepository
                .findById(notStarted.getId())
                .orElseThrow()
                .getDepartureReminderAttemptedAt())
        .isNull();
    assertThat(
            meetingMemberRepository
                .findById(moving.getId())
                .orElseThrow()
                .getDepartureReminderAttemptedAt())
        .isNotNull();
  }

  private boolean claimAfterGates(
      Candidate candidate, CountDownLatch readyGate, CountDownLatch startGate)
      throws InterruptedException {
    readyGate.countDown();
    startGate.await();
    return departureReminderClaimService.claim(candidate, NOW, NOW.plusMinutes(60));
  }

  private MeetingMember saveMember(
      MeetingStatus meetingStatus,
      LocalDateTime meetingAt,
      boolean moving,
      boolean withdrawn,
      boolean attempted) {
    long value = ++sequence;
    User user = new User(10_000L + value, "사용자 " + value, "https://img.example/" + value);
    if (withdrawn) {
      user.withdraw();
    }
    user = userRepository.save(user);

    Meeting meeting =
        new Meeting(
            user,
            "약속 " + value,
            meetingAt,
            "목적지",
            null,
            BigDecimal.valueOf(37.5),
            BigDecimal.valueOf(127.0),
            50,
            "invite" + value,
            null);
    applyStatus(meeting, meetingStatus);
    meeting = meetingRepository.save(meeting);

    MeetingMember member =
        new MeetingMember(
            meeting, user, MeetingMemberRole.HOST, "멤버 " + value, "https://img.example/" + value);
    if (moving) {
      member.depart();
    }
    if (attempted) {
      member.markDepartureReminderAttempted(NOW.minusMinutes(1));
    }
    return meetingMemberRepository.save(member);
  }

  private void applyStatus(Meeting meeting, MeetingStatus meetingStatus) {
    switch (meetingStatus) {
      case WAITING -> {
        // 생성 상태를 유지한다.
      }
      case IN_PROGRESS -> meeting.start();
      case COMPLETED -> {
        meeting.start();
        meeting.complete();
      }
      case CANCELED -> meeting.cancel();
    }
  }

  private void deleteTestData() {
    pushSubscriptionRepository.deleteAll();
    meetingMemberRepository.deleteAll();
    meetingRepository.deleteAll();
    userRepository.deleteAll();
  }

  private record Candidate(Long memberId, Long meetingId, Long userId)
      implements MeetingMemberRepository.DepartureReminderCandidate {

    @Override
    public Long getMemberId() {
      return memberId;
    }

    @Override
    public Long getMeetingId() {
      return meetingId;
    }

    @Override
    public Long getUserId() {
      return userId;
    }
  }

  static class DepartureReminderEventCounter {

    private final AtomicInteger count = new AtomicInteger();

    @EventListener
    public void handle(DepartureReminderClaimedEvent event) {
      count.incrementAndGet();
    }

    int count() {
      return count.get();
    }

    void reset() {
      count.set(0);
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class EventTestConfig {

    @Bean
    DepartureReminderEventCounter departureReminderEventCounter() {
      return new DepartureReminderEventCounter();
    }
  }
}
