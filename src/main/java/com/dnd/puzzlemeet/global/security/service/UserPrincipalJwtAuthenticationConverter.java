package com.dnd.puzzlemeet.global.security.service;

import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPrincipalJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

  private final UserRepository userRepository;

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    Long userId;
    try {
      userId = Long.valueOf(jwt.getSubject());
    } catch (NumberFormatException e) {
      throw invalidTokenException();
    }

    if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
      throw invalidTokenException();
    }
    UserPrincipal principal = new UserPrincipal(userId);
    return new UserPrincipalAuthenticationToken(principal);
  }

  private OAuth2AuthenticationException invalidTokenException() {
    return new OAuth2AuthenticationException(
        new OAuth2Error("invalid_token", "탈퇴했거나 존재하지 않는 사용자입니다.", null));
  }
}
