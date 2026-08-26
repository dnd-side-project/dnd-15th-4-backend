package com.dnd.puzzlemeet.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PushSubscriptionCreateRequest(
    @Schema(
            description = "브라우저 Push Service 구독 endpoint",
            example = "https://fcm.googleapis.com/fcm/send/example",
            maxLength = 2048,
            requiredMode = RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 2048)
        String endpoint,
    @Schema(description = "Web Push 암호화 키", requiredMode = RequiredMode.REQUIRED) @NotNull @Valid
        Keys keys) {

  @Override
  public String toString() {
    return "PushSubscriptionCreateRequest[endpoint=***, keys=***]";
  }

  public record Keys(
      @Schema(description = "브라우저 P-256 공개키", requiredMode = RequiredMode.REQUIRED)
          @NotBlank
          @Size(max = 100)
          String p256dh,
      @Schema(description = "브라우저 인증 secret", requiredMode = RequiredMode.REQUIRED)
          @NotBlank
          @Size(max = 30)
          String auth) {

    @Override
    public String toString() {
      return "Keys[p256dh=***, auth=***]";
    }
  }
}
