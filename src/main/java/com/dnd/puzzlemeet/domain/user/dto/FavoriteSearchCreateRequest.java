package com.dnd.puzzlemeet.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FavoriteSearchCreateRequest(
    @Schema(
            description = "즐겨찾기에 저장할 장소명",
            example = "강남역",
            maxLength = 100,
            requiredMode = RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100)
        String keyword) {}
