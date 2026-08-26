package com.dnd.puzzlemeet.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PushSubscriptionDeleteRequest(
    @Schema(
            description = "해지할 브라우저 Push Service 구독 endpoint",
            example = "https://fcm.googleapis.com/fcm/send/example",
            maxLength = 2048,
            requiredMode = RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 2048)
        String endpoint) {

  @Override
  public String toString() {
    return "PushSubscriptionDeleteRequest[endpoint=***]";
  }
}
