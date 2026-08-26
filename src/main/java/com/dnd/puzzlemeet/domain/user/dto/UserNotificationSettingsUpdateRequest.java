package com.dnd.puzzlemeet.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;

public record UserNotificationSettingsUpdateRequest(
    @Schema(description = "위치 권한 허용 여부", example = "true", requiredMode = RequiredMode.REQUIRED)
        @NotNull
        Boolean locationPermission,
    @Schema(description = "친구 도착 알림 허용 여부", example = "true", requiredMode = RequiredMode.REQUIRED)
        @NotNull
        Boolean friendArrival,
    @Schema(description = "말풍선 알림 허용 여부", example = "false", requiredMode = RequiredMode.REQUIRED)
        @NotNull
        Boolean chatBubble) {}
