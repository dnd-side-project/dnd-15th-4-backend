package com.dnd.puzzlemeet.domain.meeting.dto;

import com.dnd.puzzlemeet.domain.meeting.entity.TravelMode;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MeetingMemberDepartureCreateRequest(
    @Schema(description = "출발지 정보", requiredMode = RequiredMode.REQUIRED) @NotNull @Valid
        Departure departure,
    @Schema(description = "알림 설정 정보", requiredMode = RequiredMode.REQUIRED) @NotNull @Valid
        NotificationSettings notificationSettings,
    @Schema(description = "닉네임 설정 정보", requiredMode = RequiredMode.REQUIRED) @NotNull @Valid
        NicknameSetting nicknameSetting,
    @Schema(description = "이동 경로 조회로 받은 경로 중 선택한 하나", requiredMode = RequiredMode.REQUIRED)
        @NotNull
        @Valid
        MeetingRouteRequest route,
    @Schema(description = "이동수단. 넣지 않으면 대중교통으로 저장한다", example = "TRANSIT", nullable = true)
        TravelMode travelMode) {

  public MeetingMemberDepartureCreateRequest {
    travelMode = travelMode != null ? travelMode : TravelMode.TRANSIT;
  }

  public record Departure(
      @Schema(
              description = "출발지 장소명",
              example = "서울대학교",
              maxLength = 100,
              requiredMode = RequiredMode.REQUIRED)
          @NotBlank
          @Size(max = 100)
          String placeName,
      @Schema(description = "출발지 위도", example = "37.5665", requiredMode = RequiredMode.REQUIRED)
          @DecimalMin("-90")
          @DecimalMax("90")
          double latitude,
      @Schema(description = "출발지 경도", example = "126.9780", requiredMode = RequiredMode.REQUIRED)
          @DecimalMin("-180")
          @DecimalMax("180")
          double longitude) {

    @Override
    public String toString() {
      return "Departure[placeName=" + placeName + ", latitude=***, longitude=***]";
    }
  }

  public record NotificationSettings(
      @Schema(
              description = "위치 권한 알림 설정 여부",
              example = "true",
              requiredMode = RequiredMode.REQUIRED)
          @NotNull
          Boolean locationPermission,
      @Schema(
              description = "친구 도착 알림 설정 여부",
              example = "true",
              requiredMode = RequiredMode.REQUIRED)
          @NotNull
          Boolean friendArrival,
      @Schema(description = "말풍선 알림 설정 여부", example = "false", requiredMode = RequiredMode.REQUIRED)
          @NotNull
          Boolean chatBubble) {}

  public record NicknameSetting(
      @Schema(description = "별도 닉네임 사용 여부", example = "true", requiredMode = RequiredMode.REQUIRED)
          @NotNull
          Boolean enabled,
      @Schema(
              description = "설정할 닉네임. 사용 여부가 true면 필수",
              example = "김땡땡",
              maxLength = 30,
              nullable = true)
          @Size(max = 30)
          String nickname) {}
}
