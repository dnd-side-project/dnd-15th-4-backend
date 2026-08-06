package com.dnd.puzzlemeet.domain.auth.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.dnd.puzzlemeet.global.security.client.KakaoTokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class KakaoTokenResponseTest {

  @Test
  @DisplayName("쓰지 않는 필드가 섞여 있어도 access_token만 뽑아낸다")
  void unusedFieldsDoNotBreakBinding() {
    String json =
        """
        {
          "token_type": "bearer",
          "access_token": "kakao-access-token",
          "expires_in": 21599,
          "refresh_token": "kakao-refresh-token",
          "refresh_token_expires_in": 5183999,
          "scope": "profile_nickname profile_image"
        }
        """;
    JsonMapper jsonMapper = JsonMapper.builder().build();

    KakaoTokenResponse response = jsonMapper.readValue(json, KakaoTokenResponse.class);

    assertThat(response.accessToken()).isEqualTo("kakao-access-token");
  }
}
