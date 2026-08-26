package com.dnd.puzzlemeet.domain.notification.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PushSubscriptionRequestTest {

  @Test
  @DisplayName("구독 등록 요청 문자열에는 endpoint와 브라우저 키가 노출되지 않는다")
  void masksCreateRequest() {
    PushSubscriptionCreateRequest request =
        new PushSubscriptionCreateRequest(
            "https://fcm.googleapis.com/fcm/send/secret-endpoint",
            new PushSubscriptionCreateRequest.Keys("secret-p256dh", "secret-auth"));

    assertThat(request.toString())
        .doesNotContain("secret-endpoint", "secret-p256dh", "secret-auth")
        .contains("endpoint=***", "keys=***");
    assertThat(request.keys().toString())
        .doesNotContain("secret-p256dh", "secret-auth")
        .contains("p256dh=***", "auth=***");
  }

  @Test
  @DisplayName("구독 해지 요청 문자열에는 endpoint가 노출되지 않는다")
  void masksDeleteRequest() {
    PushSubscriptionDeleteRequest request =
        new PushSubscriptionDeleteRequest("https://fcm.googleapis.com/fcm/send/secret-endpoint");

    assertThat(request.toString()).doesNotContain("secret-endpoint").contains("endpoint=***");
  }
}
