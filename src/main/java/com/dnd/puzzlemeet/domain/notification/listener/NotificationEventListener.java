package com.dnd.puzzlemeet.domain.notification.listener;

import com.dnd.puzzlemeet.domain.notification.dto.PushNotificationPayload;
import com.dnd.puzzlemeet.domain.notification.entity.NotificationType;
import com.dnd.puzzlemeet.domain.notification.event.DepartureReminderClaimedEvent;
import com.dnd.puzzlemeet.domain.notification.event.FriendArrivedEvent;
import com.dnd.puzzlemeet.domain.notification.event.QuickMessageSentEvent;
import com.dnd.puzzlemeet.domain.notification.service.NotificationRecipientService;
import com.dnd.puzzlemeet.domain.notification.service.PushNotificationSender;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private static final String TITLE = "PuzzleMeet";
  private static final String FRIEND_ARRIVAL_BODY = "친구가 약속 장소에 도착했어요.";
  private static final String QUICK_MESSAGE_BODY = "새 퀵메시지가 도착했어요.";
  private static final String DEPARTURE_REMINDER_BODY = "약속까지 1시간 남았어요. 출발을 준비해 주세요.";

  private final NotificationRecipientService notificationRecipientService;
  private final PushNotificationSender pushNotificationSender;

  @Async("notificationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleFriendArrived(FriendArrivedEvent event) {
    handle(
        NotificationType.FRIEND_ARRIVAL,
        event.meetingId(),
        FRIEND_ARRIVAL_BODY,
        () ->
            notificationRecipientService.findFriendArrivalRecipients(
                event.meetingId(), event.arrivedMemberId()));
  }

  @Async("notificationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleQuickMessageSent(QuickMessageSentEvent event) {
    handle(
        NotificationType.QUICK_MESSAGE,
        event.meetingId(),
        QUICK_MESSAGE_BODY,
        () ->
            notificationRecipientService.findQuickMessageRecipients(
                event.meetingId(), event.senderMemberId()));
  }

  @Async("notificationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleDepartureReminderClaimed(DepartureReminderClaimedEvent event) {
    handle(
        NotificationType.DEPARTURE_REMINDER,
        event.meetingId(),
        DEPARTURE_REMINDER_BODY,
        () ->
            notificationRecipientService.findDepartureReminderRecipient(
                event.meetingId(), event.memberId(), event.userId()));
  }

  private void handle(
      NotificationType type, Long meetingId, String body, Supplier<List<Long>> recipientFinder) {
    try {
      pushNotificationSender.send(
          recipientFinder.get(), new PushNotificationPayload(type, TITLE, body, meetingId));
    } catch (RuntimeException e) {
      log.error("[알림 처리 실패] type={}, meetingId={}, message={}", type, meetingId, e.getMessage(), e);
    }
  }
}
