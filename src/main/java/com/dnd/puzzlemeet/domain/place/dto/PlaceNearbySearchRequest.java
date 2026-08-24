package com.dnd.puzzlemeet.domain.place.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PlaceNearbySearchRequest(
    @Schema(description = "현재 위치 위도", example = "37.4765", requiredMode = RequiredMode.REQUIRED)
        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        Double latitude,
    @Schema(description = "현재 위치 경도", example = "126.9816", requiredMode = RequiredMode.REQUIRED)
        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        Double longitude,
    @Schema(description = "검색 반경(km)", example = "1", defaultValue = "1") @Min(1) @Max(33)
        Integer radiusKm,
    @Schema(description = "검색할 주변 장소 카테고리", example = "[\"RESTAURANT\", \"CAFE\", \"TRANSIT\"]")
        @Size(max = 3)
        List<PlaceNearbyCategory> categories,
    @Schema(description = "조회할 페이지 번호 (0부터 시작)", example = "0", defaultValue = "0")
        @Min(0)
        @Max(199)
        Integer page,
    @Schema(description = "페이지당 조회 개수", example = "20", defaultValue = "20") @Min(1) @Max(20)
        Integer size) {

  private static final int DEFAULT_RADIUS_KM = 1;
  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_SIZE = 20;
  private static final List<PlaceNearbyCategory> DEFAULT_CATEGORIES =
      List.of(
          PlaceNearbyCategory.RESTAURANT, PlaceNearbyCategory.CAFE, PlaceNearbyCategory.TRANSIT);

  public PlaceNearbySearchRequest {
    radiusKm = radiusKm != null ? radiusKm : DEFAULT_RADIUS_KM;
    categories =
        categories == null || categories.isEmpty() ? DEFAULT_CATEGORIES : List.copyOf(categories);
    page = page != null ? page : DEFAULT_PAGE;
    size = size != null ? size : DEFAULT_SIZE;
  }

  @Override
  public String toString() {
    return "PlaceNearbySearchRequest[latitude=***, longitude=***, radiusKm="
        + radiusKm
        + ", categories="
        + categories
        + ", page="
        + page
        + ", size="
        + size
        + "]";
  }
}
