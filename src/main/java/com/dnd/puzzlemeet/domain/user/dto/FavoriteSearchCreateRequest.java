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
        String keyword,
    @Schema(
            description = "장소 검색 결과의 도로명 주소. 검색 결과에 없으면 생략하거나 null로 보낸다",
            example = "서울 강남구 강남대로 396",
            maxLength = 200,
            nullable = true)
        @Size(max = 200)
        String roadAddressName) {}
