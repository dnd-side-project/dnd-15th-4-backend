package com.dnd.puzzlemeet.domain.auth.service;

import com.dnd.puzzlemeet.domain.auth.entity.RefreshToken;
import com.dnd.puzzlemeet.domain.auth.repository.RefreshTokenRepository;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.security.JwtProperties;
import com.dnd.puzzlemeet.global.security.KakaoProperties;
import com.dnd.puzzlemeet.global.security.client.KakaoTokenClient;
import com.dnd.puzzlemeet.global.security.client.KakaoUserClient;
import com.dnd.puzzlemeet.global.security.client.KakaoUserResponse;
import com.dnd.puzzlemeet.global.security.service.JwtProvider;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  private static final String HASH_ALGORITHM = "SHA-256";
  private static final String RESPONSE_TYPE_CODE = "code";
  private static final String PROMPT_SELECT_ACCOUNT = "select_account";

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final KakaoTokenClient kakaoTokenClient;
  private final KakaoUserClient kakaoUserClient;
  private final JwtProvider jwtProvider;
  private final JwtProperties jwtProperties;
  private final KakaoProperties kakaoProperties;

  public URI buildKakaoAuthorizeUri(String state) {
    return UriComponentsBuilder.fromUriString(kakaoProperties.authorizeUri())
        .queryParam("response_type", RESPONSE_TYPE_CODE)
        .queryParam("client_id", kakaoProperties.clientId())
        .queryParam("redirect_uri", kakaoProperties.redirectUri())
        .queryParam("state", state)
        .queryParam("prompt", PROMPT_SELECT_ACCOUNT)
        .encode()
        .build()
        .toUri();
  }

  public void verifyKakaoCallback(String error, String state, String storedState) {
    if (StringUtils.hasText(error)) {
      log.info("[카카오 로그인] 인가 거부, error={}", error);
      throw ApiException.of(ErrorCode.AUTH_KAKAO_LOGIN_CANCELED);
    }
    if (!StringUtils.hasText(state)
        || !StringUtils.hasText(storedState)
        || !state.equals(storedState)) {
      log.warn("[카카오 로그인] state 불일치로 콜백을 거부했다");
      throw ApiException.of(ErrorCode.AUTH_OAUTH_STATE_MISMATCH);
    }
  }

  @Transactional
  public TokenPair loginWithKakao(String authorizationCode) {
    if (!StringUtils.hasText(authorizationCode)) {
      log.warn("[카카오 로그인] 인가 코드가 없는 콜백을 거부했다");
      throw ApiException.of(ErrorCode.AUTH_KAKAO_CODE_INVALID);
    }

    String kakaoAccessToken = kakaoTokenClient.exchangeAuthorizationCode(authorizationCode);
    KakaoUserResponse kakaoUser = kakaoUserClient.getUserInfo(kakaoAccessToken);
    KakaoUserResponse.KakaoAccount.Profile profile = requireProfile(kakaoUser);
    String email = resolveUsableEmail(kakaoUser.kakaoAccount());
    String nickname = profile.nickname();
    String profileImageUrl = profile.profileImageUrl();

    User user =
        userRepository
            .findActiveByKakaoIdForUpdate(kakaoUser.id())
            .map(
                existing -> {
                  existing.updateKakaoProfile(email, profileImageUrl);
                  return existing;
                })
            .orElseGet(
                () ->
                    userRepository.save(
                        new User(kakaoUser.id(), nickname, profileImageUrl, email)));

    return issueTokenPair(user);
  }

  @Transactional
  public TokenPair reissue(String rawRefreshToken) {
    requireRefreshTokenPresent(rawRefreshToken);
    rejectIfNotRefreshToken(rawRefreshToken);

    RefreshToken refreshToken =
        refreshTokenRepository
            .findByTokenHash(hash(rawRefreshToken))
            .orElseThrow(() -> ApiException.of(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));

    if (refreshToken.isExpired(LocalDateTime.now())) {
      throw ApiException.of(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
    }

    User user =
        userRepository
            .findActiveByIdForUpdate(refreshToken.getUser().getId())
            .orElseThrow(() -> ApiException.of(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
    refreshTokenRepository.delete(refreshToken);

    return issueTokenPair(user);
  }

  @Transactional
  public void logout(String rawRefreshToken) {
    if (!StringUtils.hasText(rawRefreshToken)) {
      return;
    }

    refreshTokenRepository
        .findByTokenHash(hash(rawRefreshToken))
        .ifPresent(refreshTokenRepository::delete);
  }

  private void requireRefreshTokenPresent(String rawRefreshToken) {
    if (!StringUtils.hasText(rawRefreshToken)) {
      throw ApiException.of(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
    }
  }

  private KakaoUserResponse.KakaoAccount.Profile requireProfile(KakaoUserResponse kakaoUser) {
    KakaoUserResponse.KakaoAccount kakaoAccount = kakaoUser.kakaoAccount();
    if (kakaoAccount == null
        || kakaoAccount.profile() == null
        || !StringUtils.hasText(kakaoAccount.profile().nickname())) {
      throw ApiException.of(ErrorCode.AUTH_KAKAO_PROFILE_REQUIRED);
    }
    return kakaoAccount.profile();
  }

  private String resolveUsableEmail(KakaoUserResponse.KakaoAccount kakaoAccount) {
    if (!Boolean.FALSE.equals(kakaoAccount.emailNeedsAgreement())
        || !Boolean.TRUE.equals(kakaoAccount.isEmailValid())
        || !Boolean.TRUE.equals(kakaoAccount.isEmailVerified())
        || !StringUtils.hasText(kakaoAccount.email())) {
      return null;
    }
    return kakaoAccount.email();
  }

  private TokenPair issueTokenPair(User user) {
    String accessToken = jwtProvider.createAccessToken(user.getId());
    String refreshToken = jwtProvider.createRefreshToken(user.getId());
    LocalDateTime expiresAt = LocalDateTime.now().plus(jwtProperties.refreshTokenExpiry());
    refreshTokenRepository.save(new RefreshToken(user, hash(refreshToken), expiresAt));
    return new TokenPair(accessToken, refreshToken);
  }

  private void rejectIfNotRefreshToken(String token) {
    try {
      jwtProvider.validateRefreshToken(token);
    } catch (JwtException e) {
    }
  }

  private String hash(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
      byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("%s 알고리즘을 사용할 수 없습니다.".formatted(HASH_ALGORITHM), e);
    }
  }

  public record TokenPair(String accessToken, String refreshToken) {}
}
