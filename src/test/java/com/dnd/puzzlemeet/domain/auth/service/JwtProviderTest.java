package com.dnd.puzzlemeet.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.security.JwtProperties;
import com.dnd.puzzlemeet.global.security.service.JwtProvider;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtProviderTest {

  private static final String SECRET = "test-jwt-secret-key-for-unit-tests-please-ignore";

  @Test
  @DisplayName("발급한 토큰이 검증을 통과한다")
  void issuedTokenPassesValidation() {
    JwtProvider jwtProvider = jwtProvider(Duration.ofMinutes(30), Duration.ofDays(14));

    String accessToken = jwtProvider.createAccessToken(1L);
    Jwt decoded = jwtProvider.decode(accessToken);

    assertThat(decoded.getSubject()).isEqualTo("1");
  }

  @Test
  @DisplayName("만료된 토큰은 거절된다")
  void expiredTokenIsRejected() {
    SecretKeySpec secretKey =
        new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    JwtDecoder jwtDecoder =
        NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    JwtProvider jwtProvider =
        new JwtProvider(
            jwtEncoder,
            jwtDecoder,
            new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(14)));

    JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
    JwtClaimsSet expiredClaims =
        JwtClaimsSet.builder()
            .subject("1")
            .issuedAt(Instant.now().minus(Duration.ofHours(2)))
            .expiresAt(Instant.now().minus(Duration.ofHours(1)))
            .claim("typ", "access")
            .build();
    String expiredAccessToken =
        jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, expiredClaims)).getTokenValue();

    assertThrows(JwtException.class, () -> jwtProvider.decode(expiredAccessToken));
  }

  @Test
  @DisplayName("access 토큰으로는 재발급 시도가 거절된다")
  void accessTokenIsRejectedForReissue() {
    JwtProvider jwtProvider = jwtProvider(Duration.ofMinutes(30), Duration.ofDays(14));

    String accessToken = jwtProvider.createAccessToken(1L);

    ApiException exception =
        assertThrows(ApiException.class, () -> jwtProvider.validateRefreshToken(accessToken));
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
  }

  private JwtProvider jwtProvider(Duration accessTokenExpiry, Duration refreshTokenExpiry) {
    SecretKeySpec secretKey =
        new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    JwtDecoder jwtDecoder =
        NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    JwtProperties jwtProperties = new JwtProperties(SECRET, accessTokenExpiry, refreshTokenExpiry);
    return new JwtProvider(jwtEncoder, jwtDecoder, jwtProperties);
  }
}
