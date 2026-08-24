package com.dnd.puzzlemeet.domain.place.dto;

import com.dnd.puzzlemeet.domain.place.client.TmapPlaceDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record PlaceDetailResponse(
    @Schema(description = "장소 식별자", example = "26338954", requiredMode = RequiredMode.REQUIRED)
        String placeId,
    @Schema(description = "장소명", example = "또봉이통닭 사당역점", requiredMode = RequiredMode.REQUIRED)
        String placeName,
    @Schema(description = "장소 업종", example = "한식", nullable = true) String categoryName,
    @Schema(description = "지번 주소", example = "서울 동작구 사당동 1031-29", nullable = true)
        String addressName,
    @Schema(description = "도로명 주소", example = "서울 동작구 동작대로7길 12", nullable = true)
        String roadAddressName,
    @Schema(description = "장소 위도", example = "37.4767", nullable = true) Double latitude,
    @Schema(description = "장소 경도", example = "126.9819", nullable = true) Double longitude,
    @Schema(description = "전화번호", example = "02-123-4567", nullable = true) String phoneNumber,
    @Schema(description = "TMAP이 제공한 영업시간 안내", example = "매일 15:00~24:00", nullable = true)
        String businessHoursText,
    @Schema(description = "평일 24시간 운영 여부", example = "false", nullable = true)
        Boolean open24HoursOnWeekdays,
    @Schema(description = "연중무휴 여부", example = "true", nullable = true) Boolean openYearRound,
    @Schema(description = "주차 가능 여부", example = "false", nullable = true) Boolean parkingAvailable,
    @Schema(description = "홈페이지 주소", example = "https://example.com", nullable = true)
        String homepageUrl) {

  public static PlaceDetailResponse from(TmapPlaceDetailResult result) {
    return new PlaceDetailResponse(
        result.placeId(),
        result.placeName(),
        result.categoryName(),
        result.addressName(),
        result.roadAddressName(),
        result.latitude(),
        result.longitude(),
        result.phoneNumber(),
        result.businessHoursText(),
        result.open24HoursOnWeekdays(),
        result.openYearRound(),
        result.parkingAvailable(),
        result.homepageUrl());
  }
}
