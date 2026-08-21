package com.dnd.puzzlemeet.domain.place.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class TmapPlaceSearchResultTest {

  private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

  @Test
  @DisplayName("장소 검색 응답의 문자열 좌표와 주소 조각이 장소 정보로 조립된다")
  void poiFieldsBindToPlace() {
    String json =
        """
        {
          "searchPoiInfo": {
            "totalCount": "127",
            "count": "1",
            "page": "1",
            "pois": {
              "poi": [
                {
                  "id": "1000123456",
                  "name": "카카오프렌즈코엑스점",
                  "telNo": "02-6002-1880",
                  "frontLat": "37.51207412",
                  "frontLon": "127.05902969",
                  "noorLat": "37.51210000",
                  "noorLon": "127.05910000",
                  "upperAddrName": "서울",
                  "middleAddrName": "강남구",
                  "lowerAddrName": "삼성동",
                  "detailAddrName": "",
                  "firstNo": "159",
                  "secondNo": "0",
                  "roadName": "영동대로",
                  "firstBuildNo": "513",
                  "newAddressList": {
                    "newAddress": [
                      {
                        "roadName": "영동대로",
                        "bldNo1": "513",
                        "bldNo2": "",
                        "fullAddressRoad": "서울 강남구 영동대로 513"
                      }
                    ]
                  }
                }
              ]
            }
          }
        }
        """;

    TmapPlaceSearchResult result = TmapPlaceSearchResult.from(parse(json));

    assertThat(result.totalCount()).isEqualTo(127);
    assertThat(result.places()).hasSize(1);

    TmapPlaceSearchResult.Place place = result.places().getFirst();
    assertThat(place.placeId()).isEqualTo("1000123456");
    assertThat(place.placeName()).isEqualTo("카카오프렌즈코엑스점");
    assertThat(place.addressName()).isEqualTo("서울 강남구 삼성동 159");
    assertThat(place.roadAddressName()).isEqualTo("서울 강남구 영동대로 513");
    assertThat(place.latitude()).isEqualTo(37.51207412);
    assertThat(place.longitude()).isEqualTo(127.05902969);
  }

  @Test
  @DisplayName("지번 부번이 있으면 지번 주소에 본번-부번으로 붙는다")
  void lotNumberKeepsSubNumberWhenPresent() {
    String json =
        """
        {
          "searchPoiInfo": {
            "totalCount": "1",
            "pois": {
              "poi": [
                {
                  "id": "1",
                  "name": "어떤가게",
                  "frontLat": "37.5",
                  "frontLon": "127.0",
                  "upperAddrName": "서울",
                  "middleAddrName": "동작구",
                  "lowerAddrName": "사당동",
                  "firstNo": "159",
                  "secondNo": "12"
                }
              ]
            }
          }
        }
        """;

    TmapPlaceSearchResult result = TmapPlaceSearchResult.from(parse(json));

    assertThat(result.places().getFirst().addressName()).isEqualTo("서울 동작구 사당동 159-12");
  }

  @Test
  @DisplayName("도로명 주소가 없는 장소의 도로명 주소는 비어 있다")
  void roadAddressIsNullWhenNewAddressMissing() {
    String json =
        """
        {
          "searchPoiInfo": {
            "totalCount": "1",
            "pois": {
              "poi": [
                {
                  "id": "1",
                  "name": "사당역 4번 출구",
                  "frontLat": "37.476559",
                  "frontLon": "126.981762",
                  "upperAddrName": "서울",
                  "middleAddrName": "동작구",
                  "lowerAddrName": "사당동"
                }
              ]
            }
          }
        }
        """;

    TmapPlaceSearchResult result = TmapPlaceSearchResult.from(parse(json));

    TmapPlaceSearchResult.Place place = result.places().getFirst();
    assertThat(place.roadAddressName()).isNull();
    assertThat(place.addressName()).isEqualTo("서울 동작구 사당동");
  }

  @Test
  @DisplayName("진입점 좌표가 없으면 중심점 좌표를 쓴다")
  void fallsBackToCenterCoordinates() {
    String json =
        """
        {
          "searchPoiInfo": {
            "totalCount": "1",
            "pois": {
              "poi": [
                {
                  "id": "1",
                  "name": "어떤장소",
                  "frontLat": "",
                  "frontLon": "",
                  "noorLat": "37.5665",
                  "noorLon": "126.9780"
                }
              ]
            }
          }
        }
        """;

    TmapPlaceSearchResult result = TmapPlaceSearchResult.from(parse(json));

    TmapPlaceSearchResult.Place place = result.places().getFirst();
    assertThat(place.latitude()).isEqualTo(37.5665);
    assertThat(place.longitude()).isEqualTo(126.9780);
  }

  @Test
  @DisplayName("좌표가 없는 장소는 검색 결과에서 빠진다")
  void placeWithoutCoordinatesIsExcluded() {
    String json =
        """
        {
          "searchPoiInfo": {
            "totalCount": "2",
            "pois": {
              "poi": [
                {"id": "1", "name": "좌표없는장소"},
                {"id": "2", "name": "좌표있는장소", "frontLat": "37.5", "frontLon": "127.0"}
              ]
            }
          }
        }
        """;

    TmapPlaceSearchResult result = TmapPlaceSearchResult.from(parse(json));

    assertThat(result.places()).hasSize(1);
    assertThat(result.places().getFirst().placeId()).isEqualTo("2");
    assertThat(result.fetchedCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("검색 결과가 없으면 빈 목록으로 조립된다")
  void emptyResponseBecomesEmptyResult() {
    TmapPlaceSearchResult result = TmapPlaceSearchResult.from(parse("{}"));

    assertThat(result.totalCount()).isZero();
    assertThat(result.places()).isEmpty();
  }

  private TmapPoiSearchResponse parse(String json) {
    return JSON_MAPPER.readValue(json, TmapPoiSearchResponse.class);
  }
}
