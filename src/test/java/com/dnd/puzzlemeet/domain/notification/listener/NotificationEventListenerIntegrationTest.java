package com.dnd.puzzlemeet.domain.notification.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.dnd.puzzlemeet.TestcontainersConfiguration;
import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRole;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.meeting.repository.MeetingRepository;
import com.dnd.puzzlemeet.domain.notification.dto.PushNotificationPayload;
import com.dnd.puzzlemeet.domain.notification.entity.NotificationType;
import com.dnd.puzzlemeet.domain.notification.event.DepartureReminderClaimedEvent;
import com.dnd.puzzlemeet.domain.notification.event.FriendArrivedEvent;
import com.dnd.puzzlemeet.domain.notification.event.QuickMessageSentEvent;
import com.dnd.puzzlemeet.domain.notification.repository.PushSubscriptionRepository;
import com.dnd.puzzlemeet.domain.notification.service.PushNotificationSender;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class NotificationEventListenerIntegrationTest {

  @Autowired private ApplicationEventPublisher eventPublisher;
  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private MeetingMemberRepository meetingMemberRepository;
  @Autowired private MeetingRepository meetingRepository;
  @Autowired private PushSubscriptionRepository pushSubscriptionRepository;
  @Autowired private UserRepository userRepository;
  @MockitoBean private PushNotificationSender pushNotificationSender;

  private MeetingMember origin;
  private MeetingMember enabled;
  private MeetingMember friendArrivalDisabled;
  private MeetingMember quickMessageDisabled;
  private MeetingMember withdrawn;

  @BeforeEach
  void setUp() {
    deleteTestData();
    saveMeetingMembers();
  }

  @AfterEach
  void tearDown() {
    deleteTestData();
  }

  @Test
  @DisplayName("친구 도착 이벤트는 commit 뒤 본인과 비활성 설정 및 탈퇴 사용자를 제외해 비동기로 발송한다")
  void sendsFriendArrivalAfterCommitToEligibleRecipients() {
    ArgumentCaptor<PushNotificationPayload> payloadCaptor =
        ArgumentCaptor.forClass(PushNotificationPayload.class);

    transactionTemplate.executeWithoutResult(
        status -> {
          eventPublisher.publishEvent(
              new FriendArrivedEvent(origin.getMeeting().getId(), origin.getId()));
          verifyNoInteractions(pushNotificationSender);
        });

    verify(pushNotificationSender, timeout(5_000))
        .send(
            eq(List.of(enabled.getUser().getId(), quickMessageDisabled.getUser().getId())),
            payloadCaptor.capture());
    assertThat(payloadCaptor.getValue())
        .isEqualTo(
            new PushNotificationPayload(
                NotificationType.FRIEND_ARRIVAL,
                "PuzzleMeet",
                "친구가 약속 장소에 도착했어요.",
                origin.getMeeting().getId()));
  }

  @Test
  @DisplayName("퀵메시지 이벤트는 발신자와 비활성 설정 및 탈퇴 사용자를 제외하고 원문 없는 일반 payload를 보낸다")
  void sendsGenericQuickMessageToEligibleRecipients() {
    ArgumentCaptor<PushNotificationPayload> payloadCaptor =
        ArgumentCaptor.forClass(PushNotificationPayload.class);

    transactionTemplate.executeWithoutResult(
        status ->
            eventPublisher.publishEvent(
                new QuickMessageSentEvent(origin.getMeeting().getId(), origin.getId())));

    verify(pushNotificationSender, timeout(5_000))
        .send(
            eq(List.of(enabled.getUser().getId(), friendArrivalDisabled.getUser().getId())),
            payloadCaptor.capture());
    PushNotificationPayload payload = payloadCaptor.getValue();
    assertThat(payload.type()).isEqualTo(NotificationType.QUICK_MESSAGE);
    assertThat(payload.body()).isEqualTo("새 퀵메시지가 도착했어요.");
    assertThat(PushNotificationPayload.class.getRecordComponents())
        .extracting(component -> component.getName())
        .containsExactly("type", "title", "body", "meetingId");
  }

  @Test
  @DisplayName("출발 리마인더 이벤트는 식별자가 모두 일치하는 활성 사용자 한 명만 대상으로 한다")
  void sendsDepartureReminderOnlyToExactActiveMember() {
    transactionTemplate.executeWithoutResult(
        status ->
            eventPublisher.publishEvent(
                new DepartureReminderClaimedEvent(
                    enabled.getMeeting().getId(), enabled.getId(), enabled.getUser().getId())));

    verify(pushNotificationSender, timeout(5_000))
        .send(
            eq(List.of(enabled.getUser().getId())),
            eq(
                new PushNotificationPayload(
                    NotificationType.DEPARTURE_REMINDER,
                    "PuzzleMeet",
                    "약속까지 1시간 남았어요. 출발을 준비해 주세요.",
                    enabled.getMeeting().getId())));
  }

  @Test
  @DisplayName("탈퇴한 참여자의 출발 리마인더 이벤트는 수신자 없는 발송으로 처리한다")
  void excludesWithdrawnDepartureReminderRecipient() {
    transactionTemplate.executeWithoutResult(
        status ->
            eventPublisher.publishEvent(
                new DepartureReminderClaimedEvent(
                    withdrawn.getMeeting().getId(),
                    withdrawn.getId(),
                    withdrawn.getUser().getId())));

    verify(pushNotificationSender, timeout(5_000))
        .send(
            eq(List.of()),
            eq(
                new PushNotificationPayload(
                    NotificationType.DEPARTURE_REMINDER,
                    "PuzzleMeet",
                    "약속까지 1시간 남았어요. 출발을 준비해 주세요.",
                    withdrawn.getMeeting().getId())));
  }

  @Test
  @DisplayName("이벤트를 발행한 트랜잭션이 rollback되면 푸시 발송을 시작하지 않는다")
  void doesNotSendAfterRollback() {
    transactionTemplate.executeWithoutResult(
        status -> {
          eventPublisher.publishEvent(
              new QuickMessageSentEvent(origin.getMeeting().getId(), origin.getId()));
          status.setRollbackOnly();
        });

    verify(pushNotificationSender, after(700).never()).send(any(), any());
  }

  private void saveMeetingMembers() {
    User originUser = saveUser(1L, "발신자", false);
    User enabledUser = saveUser(2L, "활성 수신자", false);
    User friendDisabledUser = saveUser(3L, "도착 알림 끔", false);
    User quickDisabledUser = saveUser(4L, "퀵메시지 알림 끔", false);
    User withdrawnUser = saveUser(5L, "탈퇴 사용자", true);
    Meeting meeting =
        meetingRepository.save(
            new Meeting(
                originUser,
                "약속",
                LocalDateTime.now().plusHours(2),
                "목적지",
                null,
                BigDecimal.valueOf(37.5),
                BigDecimal.valueOf(127.0),
                50,
                "invite01",
                null));

    origin = saveMember(meeting, originUser, "발신자");
    enabled = saveMember(meeting, enabledUser, "활성 수신자");
    friendArrivalDisabled = saveMember(meeting, friendDisabledUser, "도착 알림 끔");
    friendArrivalDisabled.updateNotificationSettings(true, false, true);
    friendArrivalDisabled = meetingMemberRepository.save(friendArrivalDisabled);
    quickMessageDisabled = saveMember(meeting, quickDisabledUser, "퀵메시지 알림 끔");
    quickMessageDisabled.updateNotificationSettings(true, true, false);
    quickMessageDisabled = meetingMemberRepository.save(quickMessageDisabled);
    withdrawn = saveMember(meeting, withdrawnUser, "탈퇴 사용자");
  }

  private User saveUser(Long kakaoId, String nickname, boolean withdrawnUser) {
    User user = new User(kakaoId, nickname, "https://img.example/" + kakaoId);
    if (withdrawnUser) {
      user.withdraw();
    }
    return userRepository.save(user);
  }

  private MeetingMember saveMember(Meeting meeting, User user, String nickname) {
    return meetingMemberRepository.save(
        new MeetingMember(
            meeting,
            user,
            MeetingMemberRole.GUEST,
            nickname,
            "https://img.example/member-" + nickname));
  }

  private void deleteTestData() {
    pushSubscriptionRepository.deleteAll();
    meetingMemberRepository.deleteAll();
    meetingRepository.deleteAll();
    userRepository.deleteAll();
  }
}
