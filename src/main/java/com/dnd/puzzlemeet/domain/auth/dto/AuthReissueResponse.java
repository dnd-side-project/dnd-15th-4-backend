package com.dnd.puzzlemeet.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record AuthReissueResponse(
    @Schema(
            description = "새로 발급된 access token. 유효기간 30분이며 Authorization 헤더에 Bearer로 실어 보낸다",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature",
            requiredMode = RequiredMode.REQUIRED)
        String accessToken) {

  public static AuthReissueResponse from(String accessToken) {
    return new AuthReissueResponse(accessToken);
  }
}
