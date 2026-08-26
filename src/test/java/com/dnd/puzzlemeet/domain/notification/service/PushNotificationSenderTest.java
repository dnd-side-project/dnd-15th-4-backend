package com.dnd.puzzlemeet.domain.notification.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.dnd.puzzlemeet.domain.notification.client.WebPushClient;
import com.dnd.puzzlemeet.domain.notification.client.WebPushSendResult;
import com.dnd.puzzlemeet.domain.notification.dto.PushNotificationPayload;
import com.dnd.puzzlemeet.domain.notification.entity.NotificationType;
import com.dnd.puzzlemeet.domain.notification.repository.PushSubscriptionTarget;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PushNotificationSenderTest {

  private final PushSubscriptionQueryService queryService =
      mock(PushSubscriptionQueryService.class);
  private final ExpiredPushSubscriptionService expiredService =
      mock(ExpiredPushSubscriptionService.class);
  private final WebPushClient webPushClient = mock(WebPushClient.class);
  private final PushNotificationSender sender =
      new PushNotificationSender(queryService, expiredService, webPushClient);

  @Test
  @DisplayName("만료 구독 삭제가 실패해도 다음 구독 발송은 계속된다")
  void continuesAfterExpiredSubscriptionCleanupFailure() {
    PushSubscriptionTarget expired = target(1L);
    PushSubscriptionTarget active = target(2L);
    PushNotificationPayload payload = payload();
    given(queryService.findTargets(List.of(10L))).willReturn(List.of(expired, active));
    given(webPushClient.send(expired, payload)).willReturn(WebPushSendResult.expired(410));
    given(webPushClient.send(active, payload)).willReturn(WebPushSendResult.success(201));
    doThrow(new IllegalStateException("database unavailable"))
        .when(expiredService)
        .deleteIfUnchanged(expired);

    sender.send(List.of(10L), payload);

    verify(webPushClient).send(expired, payload);
    verify(webPushClient).send(active, payload);
  }

  @Test
  @DisplayName("한 구독의 발송 실패는 다음 구독 발송을 막지 않는다")
  void continuesAfterPushFailure() {
    PushSubscriptionTarget failed = target(1L);
    PushSubscriptionTarget active = target(2L);
    PushNotificationPayload payload = payload();
    given(queryService.findTargets(List.of(10L))).willReturn(List.of(failed, active));
    given(webPushClient.send(failed, payload)).willReturn(WebPushSendResult.failed(503));
    given(webPushClient.send(active, payload)).willReturn(WebPushSendResult.success(200));

    sender.send(List.of(10L), payload);

    verify(webPushClient).send(failed, payload);
    verify(webPushClient).send(active, payload);
  }

  private PushSubscriptionTarget target(Long id) {
    PushSubscriptionTarget target = mock(PushSubscriptionTarget.class);
    given(target.getId()).willReturn(id);
    return target;
  }

  private PushNotificationPayload payload() {
    return new PushNotificationPayload(
        NotificationType.FRIEND_ARRIVAL, "PuzzleMeet", "친구가 약속 장소에 도착했어요.", 1L);
  }
}
