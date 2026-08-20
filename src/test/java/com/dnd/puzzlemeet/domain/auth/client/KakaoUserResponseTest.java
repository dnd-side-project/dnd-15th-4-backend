package com.dnd.puzzlemeet.domain.auth.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.dnd.puzzlemeet.global.security.client.KakaoUserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class KakaoUserResponseTest {

  @Test
  @DisplayName("카카오 응답의 snake_case 필드가 그대로 매핑된다")
  void snakeCaseFieldsBindToRecordComponents() {
    String json =
        """
        {
          "id": 123456789,
          "kakao_account": {
            "email_needs_agreement": false,
            "is_email_valid": true,
            "is_email_verified": true,
            "email": "puzzlemeet@example.com",
            "profile": {
              "nickname": "효창",
              "profile_image_url": "https://img.kakao.com/profile.jpg"
            }
          }
        }
        """;
    JsonMapper jsonMapper = JsonMapper.builder().build();

    KakaoUserResponse response = jsonMapper.readValue(json, KakaoUserResponse.class);

    assertThat(response.id()).isEqualTo(123456789L);
    assertThat(response.kakaoAccount().emailNeedsAgreement()).isFalse();
    assertThat(response.kakaoAccount().isEmailValid()).isTrue();
    assertThat(response.kakaoAccount().isEmailVerified()).isTrue();
    assertThat(response.kakaoAccount().email()).isEqualTo("puzzlemeet@example.com");
    assertThat(response.kakaoAccount().profile().nickname()).isEqualTo("효창");
    assertThat(response.kakaoAccount().profile().profileImageUrl())
        .isEqualTo("https://img.kakao.com/profile.jpg");
  }
}
