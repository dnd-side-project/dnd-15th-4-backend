package com.dnd.puzzlemeet.domain.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record MeetingMemberLocationUpdateRequest(
    @Schema(description = "현재 위치 위도", example = "37.5283", requiredMode = RequiredMode.REQUIRED)
        @DecimalMin("-90")
        @DecimalMax("90")
        double latitude,
    @Schema(description = "현재 위치 경도", example = "126.9320", requiredMode = RequiredMode.REQUIRED)
        @DecimalMin("-180")
        @DecimalMax("180")
        double longitude) {

  @Override
  public String toString() {
    return "MeetingMemberLocationUpdateRequest[masked]";
  }
}
