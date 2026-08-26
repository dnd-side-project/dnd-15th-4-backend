package com.dnd.puzzlemeet.domain.notification.client;

import com.dnd.puzzlemeet.domain.notification.config.WebPushProperties;
import com.dnd.puzzlemeet.domain.notification.dto.PushNotificationPayload;
import com.dnd.puzzlemeet.domain.notification.repository.PushSubscriptionTarget;
import com.dnd.puzzlemeet.domain.notification.service.PushSubscriptionValidator;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebPushClient {

  private static final int NOT_FOUND = 404;
  private static final int GONE = 410;

  private final PushService pushService;
  private final CloseableHttpAsyncClient webPushHttpClient;
  private final WebPushProperties webPushProperties;
  private final PushSubscriptionValidator pushSubscriptionValidator;
  private final ObjectMapper objectMapper;

  public WebPushSendResult send(PushSubscriptionTarget target, PushNotificationPayload payload) {
    try {
      HttpResponse response = webPushHttpClient.execute(preparePost(target, payload), null).get();
      int status = response.getStatusLine().getStatusCode();
      EntityUtils.consume(response.getEntity());
      if (status >= 200 && status < 300) {
        return WebPushSendResult.success(status);
      }
      if (status == NOT_FOUND || status == GONE) {
        return WebPushSendResult.expired(status);
      }
      return WebPushSendResult.failed(status);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.info("[푸시 연동] 발송 중 스레드 interrupted");
      return WebPushSendResult.failed(null);
    } catch (ExecutionException e) {
      log.info("[푸시 연동] 발송 실패, reason={}", rootCauseName(e));
      return WebPushSendResult.failed(null);
    } catch (Exception e) {
      log.info("[푸시 연동] 발송 준비 실패, reason={}", e.getClass().getSimpleName());
      return WebPushSendResult.failed(null);
    }
  }

  HttpPost preparePost(PushSubscriptionTarget target, PushNotificationPayload payload)
      throws Exception {
    pushSubscriptionValidator.validateEndpointForSending(target.getEndpoint());
    byte[] payloadBytes = objectMapper.writeValueAsBytes(payload);
    Notification notification =
        new Notification(
            target.getEndpoint(),
            target.getP256dh(),
            target.getAuth(),
            payloadBytes,
            webPushProperties.ttlSeconds());
    return pushService.preparePost(notification, Encoding.AES128GCM);
  }

  private String rootCauseName(ExecutionException exception) {
    Throwable cause = exception.getCause();
    return cause == null ? exception.getClass().getSimpleName() : cause.getClass().getSimpleName();
  }
}
