package com.dnd.puzzlemeet.domain.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record MeetingUpdateRequest(
    @Schema(
            description = "약속 제목. 값을 보내지 않으면 기존 값을 유지한다",
            example = "한강 피크닉",
            maxLength = 50,
            nullable = true)
        @Size(min = 1, max = 50)
        String title,
    @Schema(
            description = "약속 일시. 값을 보내지 않으면 기존 값을 유지한다",
            example = "2026-08-10T14:00:00",
            nullable = true)
        @Future
        LocalDateTime dateTime,
    @Schema(
            description = "약속 목적지명. 값을 보내지 않으면 기존 값을 유지한다",
            example = "서울 여의도 한강공원",
            maxLength = 100,
            nullable = true)
        @Size(min = 1, max = 100)
        String destination,
    @Schema(
            description = "약속 장소 위도. 경도와 함께 보내야 한다. 값을 보내지 않으면 기존 값을 유지한다",
            example = "37.5283",
            nullable = true)
        @DecimalMin("-90")
        @DecimalMax("90")
        Double latitude,
    @Schema(
            description = "약속 장소 경도. 위도와 함께 보내야 한다. 값을 보내지 않으면 기존 값을 유지한다",
            example = "126.9320",
            nullable = true)
        @DecimalMin("-180")
        @DecimalMax("180")
        Double longitude) {}
