package com.dnd.puzzlemeet.domain.notification.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "webpush")
public record WebPushProperties(
    @NotNull @Valid Vapid vapid, @Min(1) @Max(2_419_200) int ttlSeconds) {

  public record Vapid(
      @NotBlank String publicKey,
      @NotBlank String privateKey,
      @NotBlank @Pattern(regexp = "^(mailto:.+|https://[^\\s]+)$") String subject) {}
}
