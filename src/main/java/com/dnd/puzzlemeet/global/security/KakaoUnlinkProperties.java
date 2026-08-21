package com.dnd.puzzlemeet.global.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kakao.unlink")
public record KakaoUnlinkProperties(@NotBlank String adminKey, @NotBlank String uri) {}
