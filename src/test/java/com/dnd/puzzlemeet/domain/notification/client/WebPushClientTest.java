package com.dnd.puzzlemeet.domain.notification.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.dnd.puzzlemeet.domain.notification.config.WebPushProperties;
import com.dnd.puzzlemeet.domain.notification.dto.PushNotificationPayload;
import com.dnd.puzzlemeet.domain.notification.entity.NotificationType;
import com.dnd.puzzlemeet.domain.notification.repository.PushSubscriptionTarget;
import com.dnd.puzzlemeet.domain.notification.service.PushSubscriptionValidator;
import java.security.Security;
import java.util.concurrent.CompletableFuture;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.json.JsonMapper;

class WebPushClientTest {

  private static final String PUBLIC_KEY =
      "BCP0phORUOCEcxm7gu2GCS5Naf7I2fW-o0JRQoJYEj30MSKlqo9WbBRygbxx6mlLciR2loUhdd63WXCeP6aq7IY";
  private static final String PRIVATE_KEY = "Q7vvwVJlpEK7JcFumcvQPVLjkEAcQbfXL40_XqiRs9s";
  private static final String AUTH = "AAAAAAAAAAAAAAAAAAAAAA";

  @BeforeAll
  static void registerProvider() {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  @Test
  @DisplayName("WebPush 요청은 네트워크 호출 없이 AES128GCM payload와 VAPID 인증 헤더를 준비한다")
  void preparesAes128GcmRequest() throws Exception {
    WebPushClient client = client(mock(CloseableHttpAsyncClient.class));
    PushSubscriptionTarget target = target();
    PushNotificationPayload payload =
        new PushNotificationPayload(
            NotificationType.QUICK_MESSAGE, "PuzzleMeet", "새 퀵메시지가 도착했어요.", 1L);

    HttpPost post = client.preparePost(target, payload);

    assertThat(post.getFirstHeader("Content-Encoding").getValue()).isEqualTo("aes128gcm");
    assertThat(post.getFirstHeader("Authorization").getValue()).startsWith("vapid t=");
    assertThat(post.getFirstHeader("TTL").getValue()).isEqualTo("600");
    assertThat(post.getEntity().getContentLength()).isPositive();
    assertThat(new String(EntityUtils.toByteArray(post.getEntity())))
        .doesNotContain(payload.title(), payload.body(), payload.type().name());
  }

  @ParameterizedTest(name = "HTTP {0}은 {1} 결과로 분류된다")
  @CsvSource({
    "200,SUCCESS",
    "201,SUCCESS",
    "404,EXPIRED",
    "410,EXPIRED",
    "429,FAILED",
    "503,FAILED"
  })
  @DisplayName("WebPush provider HTTP 상태는 성공과 만료 및 실패로 분류된다")
  void mapsProviderHttpStatus(int httpStatus, WebPushSendResult.Status expectedStatus)
      throws Exception {
    CloseableHttpAsyncClient httpClient = mock(CloseableHttpAsyncClient.class);
    HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, httpStatus, "test");
    given(httpClient.execute(any(HttpUriRequest.class), isNull()))
        .willReturn(CompletableFuture.completedFuture(response));
    WebPushClient client = client(httpClient);

    WebPushSendResult result =
        client.send(
            target(),
            new PushNotificationPayload(
                NotificationType.FRIEND_ARRIVAL, "PuzzleMeet", "친구가 약속 장소에 도착했어요.", 1L));

    assertThat(result.status()).isEqualTo(expectedStatus);
    assertThat(result.httpStatus()).isEqualTo(httpStatus);
  }

  private WebPushClient client(CloseableHttpAsyncClient httpClient) throws Exception {
    WebPushProperties properties =
        new WebPushProperties(
            new WebPushProperties.Vapid(PUBLIC_KEY, PRIVATE_KEY, "mailto:test@puzzlemeet.example"),
            600);
    return new WebPushClient(
        new PushService(PUBLIC_KEY, PRIVATE_KEY, properties.vapid().subject()),
        httpClient,
        properties,
        new PushSubscriptionValidator(),
        JsonMapper.builder().build());
  }

  private PushSubscriptionTarget target() {
    PushSubscriptionTarget target = mock(PushSubscriptionTarget.class);
    given(target.getEndpoint()).willReturn("https://push.services.mozilla.com/wpush/v2/example");
    given(target.getP256dh()).willReturn(PUBLIC_KEY);
    given(target.getAuth()).willReturn(AUTH);
    return target;
  }
}
