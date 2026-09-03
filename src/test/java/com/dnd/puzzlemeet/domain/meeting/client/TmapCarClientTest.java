package com.dnd.puzzlemeet.domain.meeting.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dnd.puzzlemeet.global.client.TmapProperties;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TmapCarClientTest {

  private static final String APP_KEY = "test-app-key";
  private static final String CAR_ROUTE_URI = "https://example.test/tmap/routes/prediction";
  private static final DateTimeFormatter PREDICTION_TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
  private static final String SUMMARY_JSON =
      """
      {
        "type": "FeatureCollection",
        "features": [
          {
            "type": "Feature",
            "properties": {
              "totalDistance": 36945,
              "totalTime": 2967,
              "taxiFare": 35400
            }
          }
        ]
      }
      """;

  private MockRestServiceServer server;
  private TmapCarClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    TmapProperties properties =
        new TmapProperties(
            APP_KEY,
            "https://example.test/transit/routes",
            CAR_ROUTE_URI,
            "https://example.test/tmap/routes/pedestrian",
            "https://example.test/tmap/pois",
            "https://example.test/tmap/pois/search/around");
    client = new TmapCarClient(properties, builder.build());
  }

  @Test
  @DisplayName("약속 시각을 넘기면 그 시각에 도착하는 기준으로 예측을 요청한다")
  void requestsArrivalPredictionForMeetingTime() {
    LocalDateTime meetingAt = LocalDateTime.of(2026, 9, 10, 14, 0);
    expectRequestBody(
        body -> {
          assertThat(body).contains("\"predictionType\":\"arrival\"");
          assertThat(body)
              .contains(
                  "\"predictionTime\":\"%s\""
                      .formatted(
                          meetingAt
                              .atZone(ZoneId.of("Asia/Seoul"))
                              .format(PREDICTION_TIME_FORMAT)));
        });

    TravelRoute route =
        client.findCarRoute(37.5045, 127.0247, 37.5283, 126.9320, "서울 여의도 한강공원", meetingAt);

    server.verify();
    assertThat(route.totalTimeSeconds()).isEqualTo(2967);
  }

  @Test
  @DisplayName("약속 시각이 없으면 지금 출발하는 기준으로 예측을 요청한다")
  void requestsDeparturePredictionWhenMeetingTimeIsAbsent() {
    String todayPrefix = LocalDateTime.now().toLocalDate().toString();
    expectRequestBody(
        body -> {
          assertThat(body).contains("\"predictionType\":\"departure\"");
          assertThat(body).contains("\"predictionTime\":\"%s".formatted(todayPrefix));
        });

    client.findCarRoute(37.5045, 127.0247, 37.5283, 126.9320, "서울 여의도 한강공원", null);

    server.verify();
  }

  private void expectRequestBody(Consumer<String> bodyAssertions) {
    server
        .expect(method(POST))
        .andExpect(
            request -> bodyAssertions.accept(((MockClientHttpRequest) request).getBodyAsString()))
        .andRespond(withSuccess(SUMMARY_JSON, MediaType.APPLICATION_JSON));
  }
}
