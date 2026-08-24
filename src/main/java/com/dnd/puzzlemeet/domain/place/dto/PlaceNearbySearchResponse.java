package com.dnd.puzzlemeet.domain.place.dto;

import com.dnd.puzzlemeet.domain.place.client.TmapNearbyPlaceSearchResult;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.List;

public record PlaceNearbySearchResponse(
    @Schema(description = "주변 장소 목록", requiredMode = RequiredMode.REQUIRED) List<Place> places,
    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0", requiredMode = RequiredMode.REQUIRED)
        int page,
    @Schema(description = "페이지당 조회 개수", example = "20", requiredMode = RequiredMode.REQUIRED)
        int size,
    @Schema(description = "다음 페이지 존재 여부", example = "true", requiredMode = RequiredMode.REQUIRED)
        boolean hasNext,
    @Schema(description = "전체 검색 결과 개수", example = "48", requiredMode = RequiredMode.REQUIRED)
        int totalCount) {

  public static PlaceNearbySearchResponse of(
      TmapNearbyPlaceSearchResult result, int page, int size) {
    return new PlaceNearbySearchResponse(
        result.places().stream().map(Place::from).toList(),
        page,
        size,
        hasNext(result.fetchedCount(), page, size, result.totalCount()),
        result.totalCount());
  }

  private static boolean hasNext(int fetchedCount, int page, int size, int totalCount) {
    return fetchedCount == size && (long) (page + 1) * size < totalCount;
  }

  public record Place(
      @Schema(description = "장소 식별자", example = "26338954", requiredMode = RequiredMode.REQUIRED)
          String placeId,
      @Schema(description = "장소명", example = "또봉이통닭 사당역점", requiredMode = RequiredMode.REQUIRED)
          String placeName,
      @Schema(description = "지번 주소", example = "서울 동작구 사당동 1031-29", nullable = true)
          String addressName,
      @Schema(description = "도로명 주소", example = "서울 동작구 동작대로7길 12", nullable = true)
          String roadAddressName,
      @Schema(description = "장소 위도", example = "37.4767", requiredMode = RequiredMode.REQUIRED)
          double latitude,
      @Schema(description = "장소 경도", example = "126.9819", requiredMode = RequiredMode.REQUIRED)
          double longitude,
      @Schema(
              description = "검색 중심에서 장소까지의 거리(m)",
              example = "85",
              requiredMode = RequiredMode.REQUIRED)
          int distanceMeters) {

    public static Place from(TmapNearbyPlaceSearchResult.Place place) {
      return new Place(
          place.placeId(),
          place.placeName(),
          place.addressName(),
          place.roadAddressName(),
          place.latitude(),
          place.longitude(),
          place.distanceMeters());
    }
  }
}
