package com.dnd.puzzlemeet.global.security.service;

import com.dnd.puzzlemeet.global.security.UserPrincipal;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class UserPrincipalJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    UserPrincipal principal = new UserPrincipal(Long.valueOf(jwt.getSubject()));
    return new UserPrincipalAuthenticationToken(principal);
  }
}
