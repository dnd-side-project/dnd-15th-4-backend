package com.dnd.puzzlemeet.domain.user.dto;

import com.dnd.puzzlemeet.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record UserNotificationSettingsResponse(
    @Schema(
            description = "위치 권한 허용 여부. 약속방 참여 시 이 값이 복사된다",
            example = "true",
            requiredMode = RequiredMode.REQUIRED)
        boolean locationPermission,
    @Schema(
            description = "친구 도착 알림 허용 여부. 약속방 참여 시 이 값이 복사된다",
            example = "true",
            requiredMode = RequiredMode.REQUIRED)
        boolean friendArrival,
    @Schema(
            description = "말풍선 알림 허용 여부. 약속방 참여 시 이 값이 복사된다",
            example = "true",
            requiredMode = RequiredMode.REQUIRED)
        boolean chatBubble) {

  public static UserNotificationSettingsResponse from(User user) {
    return new UserNotificationSettingsResponse(
        user.isLocationNotificationEnabled(),
        user.isFriendArrivalNotificationEnabled(),
        user.isChatBubbleNotificationEnabled());
  }
}
