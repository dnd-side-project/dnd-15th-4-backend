package com.dnd.puzzlemeet.domain.meeting.dto;

import com.dnd.puzzlemeet.domain.meeting.entity.TravelMode;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record MeetingRouteSearchRequest(
    @Schema(description = "출발지 좌표", requiredMode = RequiredMode.REQUIRED) @NotNull @Valid
        Start start,
    @Schema(description = "이동수단. 넣지 않으면 대중교통으로 조회한다", example = "TRANSIT", nullable = true)
        TravelMode travelMode) {

  public MeetingRouteSearchRequest {
    travelMode = travelMode != null ? travelMode : TravelMode.TRANSIT;
  }

  public record Start(
      @Schema(description = "출발지 위도", example = "37.5045", requiredMode = RequiredMode.REQUIRED)
          @DecimalMin("-90")
          @DecimalMax("90")
          double latitude,
      @Schema(description = "출발지 경도", example = "127.0247", requiredMode = RequiredMode.REQUIRED)
          @DecimalMin("-180")
          @DecimalMax("180")
          double longitude) {

    @Override
    public String toString() {
      return "Start[latitude=***, longitude=***]";
    }
  }
}
