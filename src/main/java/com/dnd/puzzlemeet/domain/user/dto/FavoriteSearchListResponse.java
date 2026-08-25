package com.dnd.puzzlemeet.domain.user.dto;

import com.dnd.puzzlemeet.domain.user.entity.FavoriteSearch;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record FavoriteSearchListResponse(
    @Schema(description = "장소 즐겨찾기 식별자", example = "1", requiredMode = RequiredMode.REQUIRED)
        Long id,
    @Schema(description = "저장된 장소명", example = "강남역", requiredMode = RequiredMode.REQUIRED)
        String keyword,
    @Schema(description = "저장된 도로명 주소", example = "서울 강남구 강남대로 396", nullable = true)
        String roadAddressName) {

  public static FavoriteSearchListResponse from(FavoriteSearch favoriteSearch) {
    return new FavoriteSearchListResponse(
        favoriteSearch.getId(), favoriteSearch.getKeyword(), favoriteSearch.getRoadAddressName());
  }
}
