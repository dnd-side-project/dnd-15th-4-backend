package com.dnd.puzzlemeet.domain.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record MeetingCreateRequest(
    @Schema(
            description = "약속 제목",
            example = "한강 피크닉",
            maxLength = 50,
            requiredMode = RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 50)
        String title,
    @Schema(
            description = "약속 일시",
            example = "2026-08-10T14:00:00",
            requiredMode = RequiredMode.REQUIRED)
        @NotNull
        @Future
        LocalDateTime dateTime,
    @Schema(
            description = "약속 목적지명",
            example = "서울 여의도 한강공원",
            maxLength = 100,
            requiredMode = RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100)
        String destination,
    @Schema(description = "약속 장소 위도", example = "37.5283", requiredMode = RequiredMode.REQUIRED)
        @DecimalMin("-90")
        @DecimalMax("90")
        double latitude,
    @Schema(description = "약속 장소 경도", example = "126.9320", requiredMode = RequiredMode.REQUIRED)
        @DecimalMin("-180")
        @DecimalMax("180")
        double longitude,
    @Schema(description = "정원 (방장 포함)", example = "6", requiredMode = RequiredMode.REQUIRED) @Min(1)
        int capacity,
    @Schema(description = "약속 메모", example = "돗자리 챙기기", maxLength = 12, nullable = true)
        @Size(max = 12)
        String memo,
    @Schema(
            description = "약속방 내 닉네임. 없으면 기본 닉네임을 쓴다",
            example = "효창",
            maxLength = 30,
            nullable = true)
        @Size(max = 30)
        String nickname,
    @Schema(description = "닉네임 설정 여부", example = "true", requiredMode = RequiredMode.REQUIRED)
        @NotNull
        Boolean nicknameSet,
    @Schema(description = "퍼즐 이미지 설정 여부", example = "true", requiredMode = RequiredMode.REQUIRED)
        @NotNull
        Boolean imageSet) {}
