package com.dnd.puzzlemeet.global.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kakao")
public record KakaoProperties(
    @NotBlank String clientId,
    String clientSecret,
    @NotBlank String redirectUri,
    @NotBlank String authorizeUri,
    @NotBlank String tokenUri,
    @NotBlank String userInfoUri) {}
