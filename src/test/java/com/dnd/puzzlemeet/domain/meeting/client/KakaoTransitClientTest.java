package com.dnd.puzzlemeet.domain.meeting.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dnd.puzzlemeet.domain.meeting.entity.TransportType;
import com.dnd.puzzlemeet.global.config.KakaoMapProperties;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.security.KakaoProperties;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoTransitClientTest {

  private static final String REST_API_KEY = "test-rest-api-key";
  private static final String TRANSIT_URI = "https://example.test/v2/routing/publictraffic";

  private static final double START_LATITUDE = 37.5045;
  private static final double START_LONGITUDE = 127.0247;
  private static final double END_LATITUDE = 37.5446;
  private static final double END_LONGITUDE = 127.0559;

  private static final String SUBWAY_ROUTE_BODY =
      """
      {
        "status": "OK",
        "properties": {
          "total": 1, "bus": 0, "subway": 1, "busAndSubway": 0,
          "landingURL": "https://map.kakao.com/link/by/traffic/출발,37.5045,127.0247/도착,37.5446,127.0559"
        },
        "routes": [
          {
            "properties": {
              "type": "SUBWAY",
              "totalDistance": 27420,
              "totalTime": 2400,
              "transfers": 1,
              "fare": {"value": 1850}
            },
            "steps": [
              {
                "properties": {
                  "guidance": "6호선 (태릉입구 > 신사)",
                  "type": "SUBWAY",
                  "distance": 27000,
                  "time": 1800,
                  "stops": [{"name": "태릉입구"}, {"name": "석계"}, {"name": "신사"}],
                  "vehicles": [{"name": "수도권6호선", "type": "일반"}]
                },
                "path": {"points": [[127.0256, 37.5017], [127.0559, 37.5446]]}
              },
              {
                "properties": {
                  "guidance": "신사신사역 환승",
                  "type": "WALKING",
                  "distance": 98,
                  "time": 128,
                  "stops": [{"name": "신사"}, {"name": "신사"}]
                },
                "path": {"points": [[127.01955173, 37.51608111], [127.02030291, 37.51643327]]}
              }
            ]
          }
        ]
      }
      """;

  @Test
  @DisplayName("대중교통 경로는 REST API 키 헤더와 출발·도착 좌표 쿼리로 조회한다")
  void sendsRestApiKeyHeaderAndCoordinateQuery() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(method(GET))
        .andExpect(header("Authorization", "KakaoAK " + REST_API_KEY))
        .andExpect(
            request -> {
              assertThat(request.getURI().getPath()).isEqualTo("/v2/routing/publictraffic");
              assertThat(request.getURI().getRawQuery())
                  .isEqualTo("start_x=127.0247&start_y=37.5045&end_x=127.0559&end_y=37.5446");
            })
        .andRespond(withSuccess(SUBWAY_ROUTE_BODY, MediaType.APPLICATION_JSON));

    findRoutes(client(builder));

    server.verify();
  }

  @Test
  @DisplayName("지하철 경로는 소요시간·요금·환승 횟수와 노선명, 정류장 목록을 내부 모델로 옮긴다")
  void mapsSubwayRouteToTravelRoute() {
    List<TravelRoute> routes = findRoutes(clientRespondingWith(SUBWAY_ROUTE_BODY));

    assertThat(routes).hasSize(1);
    TravelRoute route = routes.getFirst();
    assertThat(route.totalTimeSeconds()).isEqualTo(2400);
    assertThat(route.fare()).isEqualTo(1850);
    assertThat(route.transferCount()).isEqualTo(1);
    assertThat(route.pathType()).isEqualTo(1);

    TravelRoute.Leg subway = route.legs().getFirst();
    assertThat(subway.transportType()).isEqualTo(TransportType.SUBWAY);
    assertThat(subway.routeName()).isEqualTo("수도권6호선");
    assertThat(subway.sectionTimeSeconds()).isEqualTo(1800);
    assertThat(subway.distanceMeters()).isEqualTo(27000);
    assertThat(subway.startName()).isEqualTo("태릉입구");
    assertThat(subway.endName()).isEqualTo("신사");
    assertThat(subway.stationNames()).containsExactly("태릉입구", "석계", "신사");
    assertThat(subway.startLatitude()).isEqualTo(37.5017);
    assertThat(subway.startLongitude()).isEqualTo(127.0256);
    assertThat(subway.endLatitude()).isEqualTo(37.5446);
    assertThat(subway.endLongitude()).isEqualTo(127.0559);
    assertThat(subway.description()).isNull();
  }

  @Test
  @DisplayName("카카오는 노선 색상을 주지 않으므로 노선 색상은 비어 있다")
  void leavesRouteColorEmpty() {
    List<TravelRoute> routes = findRoutes(clientRespondingWith(SUBWAY_ROUTE_BODY));

    assertThat(routes.getFirst().legs()).allSatisfy(leg -> assertThat(leg.routeColor()).isNull());
  }

  @Test
  @DisplayName("도보 구간은 안내 문구를 설명으로 담고 시작·끝 좌표를 그대로 옮긴다")
  void mapsWalkingGuidanceToDescription() {
    List<TravelRoute> routes = findRoutes(clientRespondingWith(SUBWAY_ROUTE_BODY));

    TravelRoute.Leg walk = routes.getFirst().legs().get(1);
    assertThat(walk.transportType()).isEqualTo(TransportType.WALK);
    assertThat(walk.description()).isEqualTo("신사신사역 환승");
    assertThat(walk.sectionTimeSeconds()).isEqualTo(128);
    assertThat(walk.distanceMeters()).isEqualTo(98);
    assertThat(walk.routeName()).isNull();
    assertThat(walk.startLatitude()).isEqualTo(37.51608111);
    assertThat(walk.endLongitude()).isEqualTo(127.02030291);
  }

  @Test
  @DisplayName("도보 구간의 stops는 환승 전후 역 이름이므로 경유 정류장으로 옮기지 않는다")
  void dropsWalkingStops() {
    List<TravelRoute> routes = findRoutes(clientRespondingWith(SUBWAY_ROUTE_BODY));

    TravelRoute.Leg walk = routes.getFirst().legs().get(1);
    assertThat(walk.stationNames()).isEmpty();
    assertThat(walk.startName()).isNull();
    assertThat(walk.endName()).isNull();
  }

  @Test
  @DisplayName("버스 경로와 버스·지하철 혼합 경로는 각각 경로 타입 2와 3으로 옮긴다")
  void mapsRouteTypeToPathType() {
    String body =
        "{\"status\":\"OK\",\"routes\":["
            + busRouteJson("BUS", 1200)
            + ","
            + busRouteJson("BUS_AND_SUBWAY", 1800)
            + "]}";

    List<TravelRoute> routes = findRoutes(clientRespondingWith(body));

    assertThat(routes).extracting(TravelRoute::pathType).containsExactly(2, 3);
    assertThat(routes.getFirst().legs().getFirst().transportType()).isEqualTo(TransportType.BUS);
  }

  @Test
  @DisplayName("경로가 다섯 건을 넘으면 카카오가 준 순서대로 앞의 다섯 건만 반환한다")
  void limitsRoutesToFive() {
    String body =
        "{\"status\":\"OK\",\"routes\":["
            + IntStream.rangeClosed(1, 7)
                .mapToObj(index -> busRouteJson("BUS", index * 100))
                .collect(Collectors.joining(","))
            + "]}";

    List<TravelRoute> routes = findRoutes(clientRespondingWith(body));

    assertThat(routes)
        .hasSize(5)
        .extracting(TravelRoute::totalTimeSeconds)
        .containsExactly(100, 200, 300, 400, 500);
  }

  @Test
  @DisplayName("요금이 구간으로만 오면 최소 요금을 쓴다")
  void usesMinimumFareWhenValueIsAbsent() {
    String body =
        """
        {
          "status": "OK",
          "routes": [
            {
              "properties": {"type": "BUS", "totalTime": 843, "transfers": 0,
                             "fare": {"min": 1500, "max": 2500}},
              "steps": [
                {
                  "properties": {"guidance": "간선 470", "type": "BUS", "distance": 1097, "time": 843,
                                 "stops": [{"name": "시청앞"}, {"name": "을지로입구"}],
                                 "vehicles": [{"name": "간선 470", "type": "간선"}]},
                  "path": {"points": [[127.0247, 37.5045], [127.0559, 37.5446]]}
                }
              ]
            }
          ]
        }
        """;

    assertThat(findRoutes(clientRespondingWith(body)).getFirst().fare()).isEqualTo(1500);
  }

  @Test
  @DisplayName("출발지와 도착지가 같으면 도보 이동을 권하는 오류로 바꾼다")
  void mapsEqualPointsToTooClose() {
    assertErrorCode("EQUAL_POINTS", ErrorCode.MEETING_MAP_TOO_CLOSE);
  }

  @Test
  @DisplayName("출발지·도착지 정류장이 없거나 결과가 없으면 경로를 찾을 수 없다고 응답한다")
  void mapsMissingNodeStatusesToRouteNotFound() {
    assertErrorCode("STARTNODES_NULL", ErrorCode.MEETING_MAP_ROUTE_NOT_FOUND);
    assertErrorCode("ENDNODES_NULL", ErrorCode.MEETING_MAP_ROUTE_NOT_FOUND);
    assertErrorCode("NO_RESULTS", ErrorCode.MEETING_MAP_ROUTE_NOT_FOUND);
  }

  @Test
  @DisplayName("잘못된 요청이나 알 수 없는 상태는 지도 서버 오류로 바꾼다")
  void mapsUnknownStatusToUnavailable() {
    assertErrorCode("INVALID_REQUEST", ErrorCode.MEETING_MAP_UNAVAILABLE);
    assertErrorCode("SOMETHING_NEW", ErrorCode.MEETING_MAP_UNAVAILABLE);
  }

  @Test
  @DisplayName("상태는 정상이어도 유효한 경로가 없으면 경로를 찾을 수 없다고 응답한다")
  void mapsEmptyRoutesToRouteNotFound() {
    assertThatThrownBy(() -> findRoutes(clientRespondingWith("{\"status\":\"OK\",\"routes\":[]}")))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.MEETING_MAP_ROUTE_NOT_FOUND));
  }

  @Test
  @DisplayName("카카오가 오류 응답을 주면 지도 서버 오류로 바꾼다")
  void mapsHttpErrorToUnavailable() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(request -> {}).andRespond(withServerError());

    assertThatThrownBy(() -> findRoutes(client(builder)))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEETING_MAP_UNAVAILABLE));
    server.verify();
  }

  @Test
  @DisplayName("응답 본문이 비어 있으면 지도 서버 오류로 바꾼다")
  void mapsEmptyBodyToUnavailable() {
    assertThatThrownBy(() -> findRoutes(clientRespondingWith("{}")))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEETING_MAP_UNAVAILABLE));
  }

  private void assertErrorCode(String status, ErrorCode errorCode) {
    KakaoTransitClient client = clientRespondingWith("{\"status\":\"" + status + "\"}");

    assertThatThrownBy(() -> findRoutes(client))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
  }

  private String busRouteJson(String routeType, int totalTime) {
    return """
        {
          "properties": {"type": "%s", "totalTime": %d, "transfers": 0, "fare": {"value": 1500}},
          "steps": [
            {
              "properties": {
                "type": "BUS",
                "distance": 5000,
                "time": %d,
                "stops": [{"name": "시청앞"}, {"name": "을지로입구"}],
                "vehicles": [{"name": "간선 470", "type": "간선"}]
              },
              "path": {"points": [[127.0247, 37.5045], [127.0559, 37.5446]]}
            }
          ]
        }
        """
        .formatted(routeType, totalTime, totalTime);
  }

  private List<TravelRoute> findRoutes(KakaoTransitClient client) {
    return client.findRoutes(START_LATITUDE, START_LONGITUDE, END_LATITUDE, END_LONGITUDE, null);
  }

  private KakaoTransitClient clientRespondingWith(String body) {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer.bindTo(builder)
        .build()
        .expect(request -> {})
        .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    return client(builder);
  }

  private KakaoTransitClient client(RestClient.Builder builder) {
    return new KakaoTransitClient(
        new KakaoMapProperties(TRANSIT_URI),
        new KakaoProperties(
            REST_API_KEY,
            null,
            "https://example.test/callback",
            "https://example.test/authorize",
            "https://example.test/token",
            "https://example.test/me"),
        builder.build());
  }
}
