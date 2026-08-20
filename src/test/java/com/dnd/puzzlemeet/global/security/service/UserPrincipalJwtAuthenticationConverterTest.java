package com.dnd.puzzlemeet.global.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.security.UserPrincipal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class UserPrincipalJwtAuthenticationConverterTest {

  @Mock private UserRepository userRepository;

  private UserPrincipalJwtAuthenticationConverter converter;

  @BeforeEach
  void setUp() {
    converter = new UserPrincipalJwtAuthenticationConverter(userRepository);
  }

  @Test
  @DisplayName("활성 사용자의 access token은 사용자 인증으로 변환한다")
  void convertsAccessTokenForActiveUser() {
    given(userRepository.existsByIdAndDeletedAtIsNull(1L)).willReturn(true);

    AbstractAuthenticationToken authentication = converter.convert(jwt("1"));

    assertThat(authentication.isAuthenticated()).isTrue();
    assertThat(authentication.getPrincipal()).isEqualTo(new UserPrincipal(1L));
    verify(userRepository).existsByIdAndDeletedAtIsNull(1L);
  }

  @Test
  @DisplayName("탈퇴했거나 존재하지 않는 사용자의 access token은 거절한다")
  void rejectsAccessTokenForInactiveUser() {
    given(userRepository.existsByIdAndDeletedAtIsNull(1L)).willReturn(false);

    OAuth2AuthenticationException exception =
        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(jwt("1")));

    assertThat(exception.getError().getErrorCode()).isEqualTo("invalid_token");
    verify(userRepository).existsByIdAndDeletedAtIsNull(1L);
  }

  @Test
  @DisplayName("사용자 ID 형식이 아닌 access token subject는 인증 실패로 처리한다")
  void rejectsAccessTokenWithNonNumericSubject() {
    OAuth2AuthenticationException exception =
        assertThrows(OAuth2AuthenticationException.class, () -> converter.convert(jwt("invalid")));

    assertThat(exception.getError().getErrorCode()).isEqualTo("invalid_token");
    verifyNoInteractions(userRepository);
  }

  private Jwt jwt(String subject) {
    Instant issuedAt = Instant.now();
    return Jwt.withTokenValue("access-token")
        .header("alg", "HS256")
        .subject(subject)
        .issuedAt(issuedAt)
        .expiresAt(issuedAt.plusSeconds(1_800))
        .build();
  }
}
