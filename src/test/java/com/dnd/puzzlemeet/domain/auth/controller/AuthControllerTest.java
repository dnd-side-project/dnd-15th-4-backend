package com.dnd.puzzlemeet.domain.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dnd.puzzlemeet.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

  private static final String FRONTEND_URI = "http://localhost:3000/login/callback";

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("로그인을 시작하면 카카오 인가 페이지에 계정 선택을 요청한다")
  void authorizeRequestsKakaoAccountSelection() throws Exception {
    mockMvc
        .perform(get("/api/v1/auth/kakao/authorize"))
        .andExpect(status().isFound())
        .andExpect(
            header()
                .string(
                    HttpHeaders.LOCATION,
                    org.hamcrest.Matchers.containsString("prompt=select_account")));
  }

  @Test
  @DisplayName("로그인을 시작하면 state 쿠키를 심고 카카오 인가 페이지로 리다이렉트한다")
  void authorizeRedirectsToKakaoWithState() throws Exception {
    mockMvc
        .perform(get("/api/v1/auth/kakao/authorize"))
        .andExpect(status().isFound())
        .andExpect(
            header()
                .string(
                    HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("oauth_state=")))
        .andExpect(
            header()
                .string(
                    HttpHeaders.LOCATION,
                    org.hamcrest.Matchers.startsWith("https://kauth.kakao.com/oauth/authorize")));
  }

  @Test
  @DisplayName("state 쿠키와 쿼리 파라미터가 다르면 콜백이 거절된다")
  void callbackWithMismatchedStateIsRejected() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/auth/kakao/callback")
                .param("code", "kakao-authorization-code")
                .param("state", "state-from-query")
                .cookie(new Cookie("oauth_state", "state-from-cookie")))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl(FRONTEND_URI + "?error=AUTH_OAUTH_STATE_MISMATCH"));
  }

  @Test
  @DisplayName("state 쿠키가 없으면 콜백이 거절된다")
  void callbackWithoutStateCookieIsRejected() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/auth/kakao/callback")
                .param("code", "kakao-authorization-code")
                .param("state", "state-from-query"))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl(FRONTEND_URI + "?error=AUTH_OAUTH_STATE_MISMATCH"));
  }

  @Test
  @DisplayName("사용자가 카카오에서 동의를 거부하면 취소로 리다이렉트한다")
  void callbackWithKakaoErrorRedirectsAsCanceled() throws Exception {
    mockMvc
        .perform(get("/api/v1/auth/kakao/callback").param("error", "access_denied"))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl(FRONTEND_URI + "?error=AUTH_KAKAO_LOGIN_CANCELED"));
  }

  @Test
  @DisplayName("재발급을 application/json이 아닌 Content-Type으로 호출하면 415로 거절된다")
  void reissueRejectsNonJsonContentType() throws Exception {
    mockMvc
        .perform(post("/api/v1/auth/reissue").contentType(MediaType.TEXT_PLAIN))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test
  @DisplayName("refresh token 쿠키 없이 재발급을 요청하면 401로 거절된다")
  void reissueWithoutRefreshTokenCookieIsRejected() throws Exception {
    mockMvc
        .perform(post("/api/v1/auth/reissue").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("refresh token 쿠키 없이 로그아웃해도 쿠키를 만료시키고 성공으로 응답한다")
  void logoutWithoutRefreshTokenCookieSucceeds() throws Exception {
    mockMvc
        .perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    HttpHeaders.SET_COOKIE,
                    org.hamcrest.Matchers.containsString("refresh_token=;")));
  }

  @Test
  @DisplayName("이미 삭제된 refresh token으로 로그아웃해도 성공으로 응답한다")
  void logoutWithUnknownRefreshTokenSucceeds() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(new Cookie("refresh_token", "already-deleted-refresh-token")))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("access token이 유효하지 않아도 로그아웃은 성공한다")
  void logoutWithInvalidAccessTokenSucceeds() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer expired-access-token"))
        .andExpect(status().isOk());
  }
}
