package com.dnd.puzzlemeet.domain.notification.service;

import com.dnd.puzzlemeet.domain.notification.client.WebPushClient;
import com.dnd.puzzlemeet.domain.notification.client.WebPushSendResult;
import com.dnd.puzzlemeet.domain.notification.dto.PushNotificationPayload;
import com.dnd.puzzlemeet.domain.notification.repository.PushSubscriptionTarget;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationSender {

  private final PushSubscriptionQueryService pushSubscriptionQueryService;
  private final ExpiredPushSubscriptionService expiredPushSubscriptionService;
  private final WebPushClient webPushClient;

  public void send(Collection<Long> recipientUserIds, PushNotificationPayload payload) {
    List<PushSubscriptionTarget> targets =
        pushSubscriptionQueryService.findTargets(recipientUserIds);
    int success = 0;
    int expired = 0;
    int failed = 0;

    for (PushSubscriptionTarget target : targets) {
      try {
        WebPushSendResult result = webPushClient.send(target, payload);
        switch (result.status()) {
          case SUCCESS -> success++;
          case EXPIRED -> {
            deleteExpiredSubscription(target);
            expired++;
          }
          case FAILED -> failed++;
        }
      } catch (RuntimeException e) {
        failed++;
        log.error("[푸시 발송 처리 실패] reason={}", e.getClass().getSimpleName(), e);
      }
    }

    log.info(
        "[푸시 발송] type={}, meetingId={}, targets={}, success={}, expired={}, failed={}",
        payload.type(),
        payload.meetingId(),
        targets.size(),
        success,
        expired,
        failed);
  }

  private void deleteExpiredSubscription(PushSubscriptionTarget target) {
    try {
      expiredPushSubscriptionService.deleteIfUnchanged(target);
    } catch (RuntimeException e) {
      log.error("[만료 구독 정리 실패] reason={}", e.getClass().getSimpleName(), e);
    }
  }
}
