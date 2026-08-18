package com.dnd.puzzlemeet.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  private static final String SECRET = "test-jwt-secret-key-for-unit-tests-please-ignore";

  private static final String AUTHORIZATION_CODE = "kakao-authorization-code";
  private static final String KAKAO_ACCESS_TOKEN = "kakao-access-token";

  @Mock private UserRepository userRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private KakaoTokenClient kakaoTokenClient;
  @Mock private KakaoUserClient kakaoUserClient;

  private JwtProvider jwtProvider;
  private AuthService authService;

  @BeforeEach
  void setUp() {
    SecretKeySpec secretKey =
        new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    JwtDecoder jwtDecoder =
        NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    JwtProperties jwtProperties =
        new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(14));
    KakaoProperties kakaoProperties =
        new KakaoProperties(
            "test-kakao-client-id",
            null,
            "http://localhost:8080/api/v1/auth/kakao/callback",
            "https://kauth.kakao.com/oauth/authorize",
            "https://kauth.kakao.com/oauth/token",
            "https://kapi.kakao.com/v2/user/me");
    jwtProvider = new JwtProvider(jwtEncoder, jwtDecoder, jwtProperties);
    authService =
        new AuthService(
            userRepository,
            refreshTokenRepository,
            kakaoTokenClient,
            kakaoUserClient,
            jwtProvider,
            jwtProperties,
            kakaoProperties);
  }

  @Test
  @DisplayName("처음 로그인하면 회원이 새로 등록된다")
  void firstLoginRegistersNewUser() {
    given(kakaoTokenClient.exchangeAuthorizationCode(AUTHORIZATION_CODE))
        .willReturn(KAKAO_ACCESS_TOKEN);
    given(kakaoUserClient.getUserInfo(KAKAO_ACCESS_TOKEN))
        .willReturn(kakaoUserResponse(100L, "효창", "https://img.kakao.com/a.jpg"));
    given(userRepository.findByKakaoId(100L)).willReturn(Optional.empty());
    given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

    authService.loginWithKakao(AUTHORIZATION_CODE);

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());
    assertThat(userCaptor.getValue().getNickname()).isEqualTo("효창");
    assertThat(userCaptor.getValue().getProfileImageUrl()).isEqualTo("https://img.kakao.com/a.jpg");
  }

  @Test
  @DisplayName("이미 가입한 카카오 계정은 닉네임을 유지하고 프로필 이미지만 갱신한다")
  void existingUserKeepsNicknameAndUpdatesProfileImage() {
    User existingUser = new User(100L, "이전닉네임", "https://img.kakao.com/old.jpg");
    given(kakaoTokenClient.exchangeAuthorizationCode(AUTHORIZATION_CODE))
        .willReturn(KAKAO_ACCESS_TOKEN);
    given(kakaoUserClient.getUserInfo(KAKAO_ACCESS_TOKEN))
        .willReturn(kakaoUserResponse(100L, "새닉네임", "https://img.kakao.com/new.jpg"));
    given(userRepository.findByKakaoId(100L)).willReturn(Optional.of(existingUser));

    authService.loginWithKakao(AUTHORIZATION_CODE);

    verify(userRepository, never()).save(any(User.class));
    assertThat(existingUser.getNickname()).isEqualTo("이전닉네임");
    assertThat(existingUser.getProfileImageUrl()).isEqualTo("https://img.kakao.com/new.jpg");
  }

  @Test
  @DisplayName("프로필 정보 제공에 동의하지 않은 카카오 계정은 로그인이 거절된다")
  void loginWithoutProfileConsentIsRejected() {
    given(kakaoTokenClient.exchangeAuthorizationCode(AUTHORIZATION_CODE))
        .willReturn(KAKAO_ACCESS_TOKEN);
    given(kakaoUserClient.getUserInfo(KAKAO_ACCESS_TOKEN))
        .willReturn(new KakaoUserResponse(100L, null));

    ApiException exception =
        assertThrows(ApiException.class, () -> authService.loginWithKakao(AUTHORIZATION_CODE));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_KAKAO_PROFILE_REQUIRED);
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("닉네임을 내려주지 않은 카카오 계정은 로그인이 거절된다")
  void loginWithoutNicknameIsRejected() {
    given(kakaoTokenClient.exchangeAuthorizationCode(AUTHORIZATION_CODE))
        .willReturn(KAKAO_ACCESS_TOKEN);
    given(kakaoUserClient.getUserInfo(KAKAO_ACCESS_TOKEN))
        .willReturn(kakaoUserResponse(100L, null, "https://img.kakao.com/a.jpg"));

    ApiException exception =
        assertThrows(ApiException.class, () -> authService.loginWithKakao(AUTHORIZATION_CODE));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_KAKAO_PROFILE_REQUIRED);
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("프로필 이미지가 없어도 닉네임만 있으면 로그인된다")
  void loginWithoutProfileImageSucceeds() {
    given(kakaoTokenClient.exchangeAuthorizationCode(AUTHORIZATION_CODE))
        .willReturn(KAKAO_ACCESS_TOKEN);
    given(kakaoUserClient.getUserInfo(KAKAO_ACCESS_TOKEN))
        .willReturn(kakaoUserResponse(100L, "효창", null));
    given(userRepository.findByKakaoId(100L)).willReturn(Optional.empty());
    given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

    authService.loginWithKakao(AUTHORIZATION_CODE);

    verify(userRepository).save(any(User.class));
  }

  @Test
  @DisplayName("재발급하면 기존 refresh token이 무효가 되어 같은 토큰으로 다시 재발급받을 수 없다")
  void reissueInvalidatesPreviousRefreshToken() {
    User user = new User(100L, "효창", "https://img.kakao.com/a.jpg");
    ReflectionTestUtils.setField(user, "id", 1L);
    String rawRefreshToken = jwtProvider.createRefreshToken(1L);
    RefreshToken storedRefreshToken =
        new RefreshToken(user, sha256(rawRefreshToken), LocalDateTime.now().plusDays(1));

    given(refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken)))
        .willReturn(Optional.of(storedRefreshToken))
        .willReturn(Optional.empty());

    authService.reissue(rawRefreshToken);
    verify(refreshTokenRepository).delete(storedRefreshToken);

    assertThrows(ApiException.class, () -> authService.reissue(rawRefreshToken));
  }

  @Test
  @DisplayName("만료된 refresh token으로 재발급을 시도하면 거절된다")
  void reissueWithExpiredRefreshTokenIsRejected() {
    User user = new User(100L, "효창", "https://img.kakao.com/a.jpg");
    ReflectionTestUtils.setField(user, "id", 1L);
    String rawRefreshToken = jwtProvider.createRefreshToken(1L);
    RefreshToken expiredRefreshToken =
        new RefreshToken(user, sha256(rawRefreshToken), LocalDateTime.now().minusDays(1));

    given(refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken)))
        .willReturn(Optional.of(expiredRefreshToken));

    ApiException exception =
        assertThrows(ApiException.class, () -> authService.reissue(rawRefreshToken));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
  }

  private KakaoUserResponse kakaoUserResponse(
      Long kakaoId, String nickname, String profileImageUrl) {
    return new KakaoUserResponse(
        kakaoId,
        new KakaoUserResponse.KakaoAccount(
            new KakaoUserResponse.KakaoAccount.Profile(nickname, profileImageUrl)));
  }

  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
