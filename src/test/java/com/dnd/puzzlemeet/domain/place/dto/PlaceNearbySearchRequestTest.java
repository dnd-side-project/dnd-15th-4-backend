package com.dnd.puzzlemeet.domain.place.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlaceNearbySearchRequestTest {

  @Test
  @DisplayName("선택값을 생략하면 주변 검색 기본값이 적용된다")
  void optionalFieldsUseDefaults() {
    PlaceNearbySearchRequest request =
        new PlaceNearbySearchRequest(37.4765, 126.9816, null, null, null, null);

    assertThat(request.radiusKm()).isEqualTo(1);
    assertThat(request.categories())
        .containsExactly(
            PlaceNearbyCategory.RESTAURANT, PlaceNearbyCategory.CAFE, PlaceNearbyCategory.TRANSIT);
    assertThat(request.page()).isZero();
    assertThat(request.size()).isEqualTo(20);
  }

  @Test
  @DisplayName("빈 카테고리 목록에도 기본 주변 검색 카테고리가 적용된다")
  void emptyCategoriesUseDefaults() {
    PlaceNearbySearchRequest request =
        new PlaceNearbySearchRequest(37.4765, 126.9816, 1, List.of(), 0, 20);

    assertThat(request.categories()).hasSize(3);
  }

  @Test
  @DisplayName("주변 검색 요청 문자열에는 현재 위치 좌표가 노출되지 않는다")
  void toStringMasksCoordinates() {
    PlaceNearbySearchRequest request =
        new PlaceNearbySearchRequest(37.4765, 126.9816, 1, null, 0, 20);

    assertThat(request.toString())
        .doesNotContain("37.4765", "126.9816")
        .contains("latitude=***", "longitude=***");
  }
}
