package com.dnd.puzzlemeet.global.security.client;

import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(SnakeCaseStrategy.class)
public record KakaoTokenResponse(String accessToken) {}
