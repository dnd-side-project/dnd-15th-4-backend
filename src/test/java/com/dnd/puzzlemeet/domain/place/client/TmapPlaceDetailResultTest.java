package com.dnd.puzzlemeet.domain.place.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class TmapPlaceDetailResultTest {

  private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

  @Test
  @DisplayName("장소 상세 응답의 주소와 운영 정보가 화면용 필드로 조립된다")
  void detailFieldsBindToPlace() {
    String json =
        """
        {
          "poiDetailInfo": {
            "id": "26338954",
            "name": "또봉이통닭 사당역점",
            "bizCatName": "한식",
            "address": "서울 동작구 사당동",
            "firstNo": "1031",
            "secondNo": "29",
            "bldAddr": "서울 동작구 동작대로7길",
            "bldNo1": "12",
            "bldNo2": "0",
            "lat": "37.4766",
            "lon": "126.9818",
            "frontlat": "37.4767",
            "frontlon": "126.9819",
            "tel": "02-123-4567",
            "parkFlag": "0",
            "twFlag": "0",
            "yaFlag": "1",
            "homepageURL": "https://example.com",
            "useTime": "매일 15:00~24:00"
          }
        }
        """;

    TmapPlaceDetailResult result =
        TmapPlaceDetailResult.from(parse(json), "requested-id").orElseThrow();

    assertThat(result.placeId()).isEqualTo("26338954");
    assertThat(result.placeName()).isEqualTo("또봉이통닭 사당역점");
    assertThat(result.categoryName()).isEqualTo("한식");
    assertThat(result.addressName()).isEqualTo("서울 동작구 사당동 1031-29");
    assertThat(result.roadAddressName()).isEqualTo("서울 동작구 동작대로7길 12");
    assertThat(result.latitude()).isEqualTo(37.4767);
    assertThat(result.longitude()).isEqualTo(126.9819);
    assertThat(result.phoneNumber()).isEqualTo("02-123-4567");
    assertThat(result.businessHoursText()).isEqualTo("매일 15:00~24:00");
    assertThat(result.open24HoursOnWeekdays()).isFalse();
    assertThat(result.openYearRound()).isTrue();
    assertThat(result.parkingAvailable()).isFalse();
    assertThat(result.homepageUrl()).isEqualTo("https://example.com");
  }

  @Test
  @DisplayName("상세 정보의 입구 좌표와 선택 정보가 없으면 중심 좌표와 null을 사용한다")
  void detailFallsBackToCenterCoordinatesAndNullableFields() {
    String json =
        """
        {
          "poiDetailInfo": {
            "name": "사당역 6번출구",
            "lat": "37.4765",
            "lon": "126.9816",
            "frontLat": "",
            "frontLon": "",
            "parkFlag": "",
            "twFlag": "",
            "yaFlag": ""
          }
        }
        """;

    TmapPlaceDetailResult result = TmapPlaceDetailResult.from(parse(json), "1234").orElseThrow();

    assertThat(result.placeId()).isEqualTo("1234");
    assertThat(result.latitude()).isEqualTo(37.4765);
    assertThat(result.longitude()).isEqualTo(126.9816);
    assertThat(result.categoryName()).isNull();
    assertThat(result.businessHoursText()).isNull();
    assertThat(result.open24HoursOnWeekdays()).isNull();
    assertThat(result.openYearRound()).isNull();
    assertThat(result.parkingAvailable()).isNull();
  }

  @Test
  @DisplayName("상세 정보 본문이 비어 있으면 장소 상세 결과가 없다")
  void emptyResponseBecomesEmptyResult() {
    assertThat(TmapPlaceDetailResult.from(parse("{}"), "1234")).isEmpty();
  }

  private TmapPoiDetailResponse parse(String json) {
    return JSON_MAPPER.readValue(json, TmapPoiDetailResponse.class);
  }
}
