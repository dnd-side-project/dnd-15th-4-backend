package com.dnd.puzzlemeet.domain.auth.service;

import com.dnd.puzzlemeet.global.security.JwtProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthCookieProvider {

  public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
  public static final String OAUTH_STATE_COOKIE = "oauth_state";

  private static final String COOKIE_PATH = "/api/v1/auth";
  private static final String SAME_SITE_CROSS_SITE = "None";
  private static final String SAME_SITE_TOP_LEVEL_NAVIGATION = "Lax";
  private static final Duration OAUTH_STATE_MAX_AGE = Duration.ofMinutes(5);

  private final JwtProperties jwtProperties;

  public ResponseCookie refreshToken(String token) {
    return refreshTokenCookie(token, jwtProperties.refreshTokenExpiry());
  }

  public ResponseCookie expiredRefreshToken() {
    return refreshTokenCookie("", Duration.ZERO);
  }

  public ResponseCookie oauthState(String state) {
    return oauthStateCookie(state, OAUTH_STATE_MAX_AGE);
  }

  public ResponseCookie expiredOauthState() {
    return oauthStateCookie("", Duration.ZERO);
  }

  private ResponseCookie refreshTokenCookie(String value, Duration maxAge) {
    return ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
        .httpOnly(true)
        .secure(true)
        .sameSite(SAME_SITE_CROSS_SITE)
        .path(COOKIE_PATH)
        .maxAge(maxAge)
        .build();
  }

  private ResponseCookie oauthStateCookie(String value, Duration maxAge) {
    return ResponseCookie.from(OAUTH_STATE_COOKIE, value)
        .httpOnly(true)
        .secure(true)
        .sameSite(SAME_SITE_TOP_LEVEL_NAVIGATION)
        .path(COOKIE_PATH)
        .maxAge(maxAge)
        .build();
  }
}
