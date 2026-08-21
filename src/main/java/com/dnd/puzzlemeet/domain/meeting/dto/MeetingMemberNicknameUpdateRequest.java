package com.dnd.puzzlemeet.domain.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MeetingMemberNicknameUpdateRequest(
    @Schema(
            description = "약속방에서 사용할 닉네임",
            example = "효창",
            maxLength = 30,
            requiredMode = RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 30)
        String nickname) {}
