package com.dnd.puzzlemeet.domain.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dnd.puzzlemeet.TestcontainersConfiguration;
import com.dnd.puzzlemeet.domain.auth.entity.RefreshToken;
import com.dnd.puzzlemeet.domain.auth.repository.RefreshTokenRepository;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.security.client.KakaoUnlinkClient;
import com.dnd.puzzlemeet.global.security.service.JwtProvider;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private JwtProvider jwtProvider;
  @MockitoBean private KakaoUnlinkClient kakaoUnlinkClient;

  @Test
  @DisplayName("access token으로 인증된 사용자가 본인 정보를 조회한다")
  void getMeReturnsAuthenticatedUser() throws Exception {
    User user =
        userRepository.save(
            new User(100L, "효창", "https://img.kakao.com/a.jpg", "puzzlemeet@example.com"));
    String accessToken = jwtProvider.createAccessToken(user.getId());

    mockMvc
        .perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(user.getId()))
        .andExpect(jsonPath("$.data.email").value("puzzlemeet@example.com"))
        .andExpect(jsonPath("$.data.nickname").value("효창"))
        .andExpect(jsonPath("$.data.profileImageUrl").value("https://img.kakao.com/a.jpg"));
  }

  @Test
  @DisplayName("access token 없이 요청하면 401로 거절된다")
  void getMeWithoutTokenIsRejected() throws Exception {
    mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("이메일이 없는 사용자는 본인 조회에서 null 이메일을 받는다")
  void getMeReturnsNullEmailWhenUnavailable() throws Exception {
    User user = userRepository.save(new User(100L, "효창", "https://img.kakao.com/a.jpg"));
    String accessToken = jwtProvider.createAccessToken(user.getId());

    mockMvc
        .perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.email").isEmpty());
  }

  @Test
  @DisplayName("access token으로 인증된 사용자가 본인 닉네임을 수정한다")
  void updateMeUpdatesAuthenticatedUserNickname() throws Exception {
    User user =
        userRepository.save(
            new User(100L, "효창", "https://img.kakao.com/a.jpg", "puzzlemeet@example.com"));
    String accessToken = jwtProvider.createAccessToken(user.getId());

    mockMvc
        .perform(
            patch("/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"새닉네임\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(user.getId()))
        .andExpect(jsonPath("$.data.email").value("puzzlemeet@example.com"))
        .andExpect(jsonPath("$.data.nickname").value("새닉네임"))
        .andExpect(jsonPath("$.data.profileImageUrl").value("https://img.kakao.com/a.jpg"));
  }

  @Test
  @DisplayName("공백 닉네임으로 수정하면 입력값 검증에 실패한다")
  void updateMeRejectsBlankNickname() throws Exception {
    User user = userRepository.save(new User(100L, "효창", "https://img.kakao.com/a.jpg"));
    String accessToken = jwtProvider.createAccessToken(user.getId());

    mockMvc
        .perform(
            patch("/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"   \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
  }

  @Test
  @DisplayName("회원 탈퇴하면 카카오 연결과 모든 refresh token을 제거하고 기존 access token을 차단한다")
  void withdrawsUserAndInvalidatesAllTokens() throws Exception {
    User user =
        userRepository.save(
            new User(100L, "효창", "https://img.kakao.com/a.jpg", "withdraw@example.com"));
    refreshTokenRepository.save(
        new RefreshToken(user, "a".repeat(64), LocalDateTime.now().plusDays(1)));
    refreshTokenRepository.save(
        new RefreshToken(user, "b".repeat(64), LocalDateTime.now().plusDays(1)));
    String accessToken = jwtProvider.createAccessToken(user.getId());
    given(kakaoUnlinkClient.unlink(100L)).willReturn(100L);

    mockMvc
        .perform(delete("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").doesNotExist())
        .andExpect(
            header()
                .string(
                    HttpHeaders.SET_COOKIE,
                    org.hamcrest.Matchers.containsString("refresh_token=;")))
        .andExpect(
            header()
                .string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")))
        .andExpect(
            header()
                .string(
                    HttpHeaders.SET_COOKIE,
                    org.hamcrest.Matchers.containsString("Path=/api/v1/auth")));

    assertThat(refreshTokenRepository.findAll()).isEmpty();
    User withdrawnUser = userRepository.findById(user.getId()).orElseThrow();
    assertThat(withdrawnUser.getKakaoId()).isNull();
    assertThat(withdrawnUser.getEmail()).isNull();
    assertThat(withdrawnUser.getNickname()).isEqualTo("탈퇴한 사용자");
    assertThat(withdrawnUser.getProfileImageUrl()).isNull();
    assertThat(withdrawnUser.getDeletedAt()).isNotNull();

    mockMvc
        .perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
  }
}
