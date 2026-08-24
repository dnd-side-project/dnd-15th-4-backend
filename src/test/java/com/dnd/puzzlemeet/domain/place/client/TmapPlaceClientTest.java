package com.dnd.puzzlemeet.domain.place.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dnd.puzzlemeet.domain.place.dto.PlaceNearbyCategory;
import com.dnd.puzzlemeet.global.client.TmapProperties;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TmapPlaceClientTest {

  private static final String APP_KEY = "test-app-key";
  private static final String POI_URI = "https://example.test/tmap/pois";
  private static final String AROUND_URI = "https://example.test/tmap/pois/search/around";

  private MockRestServiceServer server;
  private TmapPlaceClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    TmapProperties properties =
        new TmapProperties(
            APP_KEY,
            "https://example.test/transit/routes",
            "https://example.test/tmap/routes/prediction",
            "https://example.test/tmap/routes/pedestrian",
            POI_URI,
            AROUND_URI);
    client = new TmapPlaceClient(properties, builder.build());
  }

  @Test
  @DisplayName("주변 검색은 현재 위치와 카테고리를 TMAP 거리순 검색 계약으로 전달한다")
  void nearbySearchUsesTmapAroundContract() {
    server
        .expect(
            request -> {
              assertThat(request.getURI().getPath()).isEqualTo("/tmap/pois/search/around");
              String query = decodedQuery(request.getURI().getRawQuery());
              assertThat(query)
                  .contains(
                      "version=1",
                      "centerLon=126.9816",
                      "centerLat=37.4765",
                      "radius=1",
                      "categories=음식점;카페;교통",
                      "page=2",
                      "count=20",
                      "sort=distance",
                      "reqCoordType=WGS84GEO",
                      "resCoordType=WGS84GEO")
                  .doesNotContain("appKey");
            })
        .andExpect(method(GET))
        .andExpect(header("appKey", APP_KEY))
        .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
        .andRespond(
            withSuccess(
                """
                {
                  "searchPoiInfo": {
                    "totalCount": "1",
                    "pois": {
                      "poi": [
                        {
                          "id": "1",
                          "name": "사당역 6번출구",
                          "frontLat": "37.4765",
                          "frontLon": "126.9816",
                          "radius": "0.01"
                        }
                      ]
                    }
                  }
                }
                """,
                MediaType.APPLICATION_JSON));

    TmapNearbyPlaceSearchResult result =
        client.searchNearbyPlaces(
            37.4765,
            126.9816,
            1,
            List.of(
                PlaceNearbyCategory.RESTAURANT,
                PlaceNearbyCategory.CAFE,
                PlaceNearbyCategory.TRANSIT),
            1,
            20);

    assertThat(result.places()).hasSize(1);
    assertThat(result.places().getFirst().distanceMeters()).isEqualTo(10);
    server.verify();
  }

  @Test
  @DisplayName("장소 상세 조회는 식별자를 경로로 전달하고 상세 정보를 반환한다")
  void placeDetailUsesTmapDetailContract() {
    server
        .expect(
            request -> {
              assertThat(request.getURI().getPath()).isEqualTo("/tmap/pois/26338954");
              assertThat(decodedQuery(request.getURI().getRawQuery()))
                  .contains("version=1", "findOption=id", "resCoordType=WGS84GEO")
                  .doesNotContain("appKey");
            })
        .andExpect(method(GET))
        .andExpect(header("appKey", APP_KEY))
        .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
        .andRespond(
            withSuccess(
                """
                {
                  "poiDetailInfo": {
                    "id": "26338954",
                    "name": "또봉이통닭 사당역점",
                    "frontlat": "37.4767",
                    "frontlon": "126.9819"
                  }
                }
                """,
                MediaType.APPLICATION_JSON));

    TmapPlaceDetailResult result = client.getPlaceDetail("26338954");

    assertThat(result.placeName()).isEqualTo("또봉이통닭 사당역점");
    server.verify();
  }

  @Test
  @DisplayName("주변 장소 서버 오류는 장소 검색 불가 오류로 변환한다")
  void nearbyServerErrorBecomesPlaceSearchUnavailable() {
    server.expect(request -> {}).andRespond(withServerError());

    assertThatThrownBy(
            () ->
                client.searchNearbyPlaces(
                    37.4765, 126.9816, 1, List.of(PlaceNearbyCategory.RESTAURANT), 0, 20))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PLACE_SEARCH_UNAVAILABLE));
    server.verify();
  }

  @Test
  @DisplayName("TMAP에 상세 장소가 없으면 장소 없음 오류로 변환한다")
  void detailNotFoundBecomesPlaceNotFound() {
    server.expect(request -> {}).andRespond(withResourceNotFound());

    assertThatThrownBy(() -> client.getPlaceDetail("99999999"))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PLACE_NOT_FOUND));
    server.verify();
  }

  private String decodedQuery(String rawQuery) {
    return URLDecoder.decode(rawQuery, StandardCharsets.UTF_8);
  }
}
