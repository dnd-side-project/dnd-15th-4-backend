package com.dnd.puzzlemeet.global.security.client;

import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(SnakeCaseStrategy.class)
public record KakaoUserResponse(Long id, KakaoAccount kakaoAccount) {

  @JsonNaming(SnakeCaseStrategy.class)
  public record KakaoAccount(Profile profile) {

    @JsonNaming(SnakeCaseStrategy.class)
    public record Profile(String nickname, String profileImageUrl) {}
  }
}
