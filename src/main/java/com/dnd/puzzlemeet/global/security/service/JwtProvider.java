package com.dnd.puzzlemeet.global.security.service;

import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.security.JwtProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

  private static final String CLAIM_TYPE = "typ";
  private static final String TOKEN_TYPE_ACCESS = "access";
  private static final String TOKEN_TYPE_REFRESH = "refresh";

  private final JwtEncoder jwtEncoder;
  private final JwtDecoder jwtDecoder;
  private final JwtProperties jwtProperties;

  public JwtProvider(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, JwtProperties jwtProperties) {
    this.jwtEncoder = jwtEncoder;
    this.jwtDecoder = jwtDecoder;
    this.jwtProperties = jwtProperties;
  }

  public String createAccessToken(Long userId) {
    return encode(userId, TOKEN_TYPE_ACCESS, jwtProperties.accessTokenExpiry());
  }

  public String createRefreshToken(Long userId) {
    return encode(userId, TOKEN_TYPE_REFRESH, jwtProperties.refreshTokenExpiry());
  }

  public Jwt decode(String token) {
    return jwtDecoder.decode(token);
  }

  public void validateRefreshToken(String token) {
    Jwt jwt = decode(token);
    if (!TOKEN_TYPE_REFRESH.equals(jwt.getClaimAsString(CLAIM_TYPE))) {
      throw ApiException.of(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
    }
  }

  private String encode(Long userId, String type, Duration expiry) {
    Instant now = Instant.now();
    JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .id(UUID.randomUUID().toString())
            .subject(String.valueOf(userId))
            .issuedAt(now)
            .expiresAt(now.plus(expiry))
            .claim(CLAIM_TYPE, type)
            .build();
    return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
  }
}
