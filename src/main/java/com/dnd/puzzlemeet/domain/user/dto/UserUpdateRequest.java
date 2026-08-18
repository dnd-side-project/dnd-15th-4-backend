package com.dnd.puzzlemeet.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
    @Schema(
            description = "수정할 닉네임",
            example = "효창",
            maxLength = 50,
            requiredMode = RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 50)
        String nickname) {}
