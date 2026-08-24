package com.dnd.puzzlemeet.domain.place.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class TmapNearbyPlaceSearchResultTest {

  private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

  @Test
  @DisplayName("주변 장소의 거리와 주소가 앱 응답 단위로 조립된다")
  void nearbyPoiFieldsBindToPlace() {
    String json =
        """
        {
          "searchPoiInfo": {
            "totalCount": "48",
            "pois": {
              "poi": [
                {
                  "id": "26338954",
                  "name": "또봉이통닭 사당역점",
                  "frontLat": "37.4767",
                  "frontLon": "126.9819",
                  "upperAddrName": "서울",
                  "middleAddrName": "동작구",
                  "lowerAddrName": "사당동",
                  "firstNo": "1031",
                  "secondNo": "29",
                  "radius": "0.085",
                  "roadName": "동작대로7길",
                  "buildingNo1": "12",
                  "buildingNo2": "0"
                }
              ]
            }
          }
        }
        """;

    TmapNearbyPlaceSearchResult result =
        TmapNearbyPlaceSearchResult.from(parse(json), 37.4765, 126.9816);

    assertThat(result.totalCount()).isEqualTo(48);
    assertThat(result.fetchedCount()).isEqualTo(1);
    TmapNearbyPlaceSearchResult.Place place = result.places().getFirst();
    assertThat(place.placeId()).isEqualTo("26338954");
    assertThat(place.placeName()).isEqualTo("또봉이통닭 사당역점");
    assertThat(place.addressName()).isEqualTo("서울 동작구 사당동 1031-29");
    assertThat(place.roadAddressName()).isEqualTo("서울 동작구 동작대로7길 12");
    assertThat(place.latitude()).isEqualTo(37.4767);
    assertThat(place.longitude()).isEqualTo(126.9819);
    assertThat(place.distanceMeters()).isEqualTo(85);
  }

  @Test
  @DisplayName("TMAP 거리값이 없으면 요청 위치와 장소 좌표로 거리를 계산한다")
  void missingRadiusFallsBackToCoordinateDistance() {
    String json =
        """
        {
          "searchPoiInfo": {
            "totalCount": "1",
            "pois": {
              "poi": [
                {
                  "id": "1",
                  "name": "현재 위치와 같은 장소",
                  "frontLat": "37.4765",
                  "frontLon": "126.9816",
                  "radius": "invalid"
                }
              ]
            }
          }
        }
        """;

    TmapNearbyPlaceSearchResult result =
        TmapNearbyPlaceSearchResult.from(parse(json), 37.4765, 126.9816);

    assertThat(result.places().getFirst().distanceMeters()).isZero();
  }

  @Test
  @DisplayName("입구 좌표 한 쌍이 완전하지 않으면 중심 좌표 한 쌍을 사용한다")
  void partialFrontCoordinatesFallBackToCenterCoordinates() {
    String json =
        """
        {
          "searchPoiInfo": {
            "totalCount": "1",
            "pois": {
              "poi": [
                {
                  "id": "1",
                  "name": "중심 좌표를 쓸 장소",
                  "frontLat": "37.1",
                  "frontLon": "",
                  "noorLat": "37.5",
                  "noorLon": "127.0"
                }
              ]
            }
          }
        }
        """;

    TmapNearbyPlaceSearchResult result =
        TmapNearbyPlaceSearchResult.from(parse(json), 37.4765, 126.9816);

    assertThat(result.places().getFirst().latitude()).isEqualTo(37.5);
    assertThat(result.places().getFirst().longitude()).isEqualTo(127.0);
  }

  @Test
  @DisplayName("표시할 수 없는 장소가 제외돼도 원본 조회 건수는 유지된다")
  void invalidPoiDoesNotChangeFetchedCount() {
    String json =
        """
        {
          "searchPoiInfo": {
            "totalCount": "2",
            "pois": {
              "poi": [
                {"id": "1", "name": "좌표 없는 장소"},
                {"id": "2", "name": "정상 장소", "frontLat": "37.5", "frontLon": "127.0"}
              ]
            }
          }
        }
        """;

    TmapNearbyPlaceSearchResult result =
        TmapNearbyPlaceSearchResult.from(parse(json), 37.4765, 126.9816);

    assertThat(result.fetchedCount()).isEqualTo(2);
    assertThat(result.places()).hasSize(1);
    assertThat(result.places().getFirst().placeId()).isEqualTo("2");
  }

  @Test
  @DisplayName("주변 검색 결과가 없으면 빈 목록으로 조립된다")
  void emptyResponseBecomesEmptyResult() {
    TmapNearbyPlaceSearchResult result =
        TmapNearbyPlaceSearchResult.from(parse("{}"), 37.4765, 126.9816);

    assertThat(result.totalCount()).isZero();
    assertThat(result.places()).isEmpty();
  }

  private TmapPoiSearchResponse parse(String json) {
    return JSON_MAPPER.readValue(json, TmapPoiSearchResponse.class);
  }
}
