package com.dnd.puzzlemeet.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import java.security.Security;
import java.util.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PushSubscriptionValidatorTest {

  private static final String VALID_PUBLIC_KEY =
      "BCP0phORUOCEcxm7gu2GCS5Naf7I2fW-o0JRQoJYEj30MSKlqo9WbBRygbxx6mlLciR2loUhdd63WXCeP6aq7IY";
  private static final String VALID_AUTH = "AAAAAAAAAAAAAAAAAAAAAA";

  private final PushSubscriptionValidator validator = new PushSubscriptionValidator();

  @BeforeAll
  static void registerProvider() {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  @Test
  @DisplayName("지원하는 Push Service의 HTTPS endpoint와 유효한 브라우저 키는 허용된다")
  void acceptsSupportedEndpointsAndValidKeys() {
    assertThatCode(
            () ->
                validator.validate(
                    "https://fcm.googleapis.com/fcm/send/token", VALID_PUBLIC_KEY, VALID_AUTH))
        .doesNotThrowAnyException();
    assertThatCode(
            () ->
                validator.validate(
                    "https://updates.push.services.mozilla.com/wpush/v2/token",
                    VALID_PUBLIC_KEY,
                    VALID_AUTH))
        .doesNotThrowAnyException();
    assertThatCode(
            () ->
                validator.validate(
                    "https://web.push.apple.com/QWERTY", VALID_PUBLIC_KEY, VALID_AUTH))
        .doesNotThrowAnyException();
    assertThatCode(
            () ->
                validator.validate(
                    "https://db5.notify.windows.com/w/?token=example",
                    VALID_PUBLIC_KEY,
                    VALID_AUTH))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("내부망과 유사 도메인 또는 비표준 HTTPS endpoint는 입력값 검증으로 거절된다")
  void rejectsUnsafeEndpoints() {
    assertInvalid("http://fcm.googleapis.com/fcm/send/token", VALID_PUBLIC_KEY, VALID_AUTH);
    assertInvalid("https://127.0.0.1/push", VALID_PUBLIC_KEY, VALID_AUTH);
    assertInvalid("https://push.apple.com.evil.example/push", VALID_PUBLIC_KEY, VALID_AUTH);
    assertInvalid("https://user@push.apple.com/push", VALID_PUBLIC_KEY, VALID_AUTH);
    assertInvalid("https://push.apple.com:8443/push", VALID_PUBLIC_KEY, VALID_AUTH);
    assertInvalid("https://push.apple.com/push#fragment", VALID_PUBLIC_KEY, VALID_AUTH);
  }

  @Test
  @DisplayName("길이나 곡선이 잘못된 p256dh와 auth secret은 입력값 검증으로 거절된다")
  void rejectsInvalidBrowserKeys() {
    assertInvalid("https://fcm.googleapis.com/fcm/send/token", "not-base64!", VALID_AUTH);
    assertInvalid(
        "https://fcm.googleapis.com/fcm/send/token",
        Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[65]),
        VALID_AUTH);
    assertInvalid("https://fcm.googleapis.com/fcm/send/token", VALID_PUBLIC_KEY, "too-short");
  }

  private void assertInvalid(String endpoint, String p256dh, String auth) {
    assertThatThrownBy(() -> validator.validate(endpoint, p256dh, auth))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE));
  }
}
