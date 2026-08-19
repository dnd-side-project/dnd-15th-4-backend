package com.dnd.puzzlemeet.domain.place.dto;

import com.dnd.puzzlemeet.domain.place.client.TmapPlaceSearchResult;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.List;

public record PlaceSearchResponse(
    @Schema(description = "장소 검색 결과 목록", requiredMode = RequiredMode.REQUIRED) List<Place> places,
    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0", requiredMode = RequiredMode.REQUIRED)
        int page,
    @Schema(description = "페이지당 조회 개수", example = "20", requiredMode = RequiredMode.REQUIRED)
        int size,
    @Schema(description = "다음 페이지 존재 여부", example = "true", requiredMode = RequiredMode.REQUIRED)
        boolean hasNext,
    @Schema(description = "전체 검색 결과 개수", example = "127", requiredMode = RequiredMode.REQUIRED)
        int totalCount) {

  public static PlaceSearchResponse of(TmapPlaceSearchResult result, int page, int size) {
    List<Place> places = result.places().stream().map(Place::from).toList();
    return new PlaceSearchResponse(
        places,
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
      @Schema(description = "장소명", example = "카카오프렌즈 코엑스점", requiredMode = RequiredMode.REQUIRED)
          String placeName,
      @Schema(
              description = "지번 주소",
              example = "서울 강남구 삼성동 159",
              requiredMode = RequiredMode.REQUIRED)
          String addressName,
      @Schema(description = "도로명 주소", example = "서울 강남구 영동대로 513", nullable = true)
          String roadAddressName,
      @Schema(
              description = "장소 위도",
              example = "37.51207412593136",
              requiredMode = RequiredMode.REQUIRED)
          double latitude,
      @Schema(
              description = "장소 경도",
              example = "127.05902969025047",
              requiredMode = RequiredMode.REQUIRED)
          double longitude) {

    public static Place from(TmapPlaceSearchResult.Place place) {
      return new Place(
          place.placeId(),
          place.placeName(),
          place.addressName(),
          place.roadAddressName(),
          place.latitude(),
          place.longitude());
    }
  }
}
