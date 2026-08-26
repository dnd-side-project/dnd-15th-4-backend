package com.dnd.puzzlemeet.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PushVapidPublicKeyResponse(
    @Schema(description = "Web Push 구독에 사용할 VAPID 공개키") String publicKey) {}
