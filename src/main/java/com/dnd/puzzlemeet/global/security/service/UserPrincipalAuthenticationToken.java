package com.dnd.puzzlemeet.global.security.service;

import com.dnd.puzzlemeet.global.security.UserPrincipal;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;

public class UserPrincipalAuthenticationToken extends AbstractAuthenticationToken {

  private final UserPrincipal principal;

  public UserPrincipalAuthenticationToken(UserPrincipal principal) {
    super(List.of());
    this.principal = principal;
    setAuthenticated(true);
  }

  @Override
  public Object getPrincipal() {
    return principal;
  }

  @Override
  public Object getCredentials() {
    return null;
  }
}
