package com.dnd.puzzlemeet.domain.meeting.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class TmapRouteSummaryResponseTest {

  @Test
  @DisplayName("차량 경로 요약 응답에서 소요시간과 예상 택시 요금이 매핑된다")
  void carRouteSummaryBindsToRecordComponents() {
    String json =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "properties": {
                "totalDistance": 36945,
                "totalTime": 2967,
                "totalFare": 0,
                "taxiFare": 35400,
                "departureTime": "2026-08-24T09:00:00+0900",
                "arrivalTime": "2026-08-24T09:49:27+0900"
              }
            }
          ]
        }
        """;

    TmapRouteSummaryResponse response =
        JsonMapper.builder().build().readValue(json, TmapRouteSummaryResponse.class);

    TmapRouteSummaryResponse.Properties properties = response.features().getFirst().properties();
    assertThat(properties.totalTime()).isEqualTo(2967);
    assertThat(properties.totalDistance()).isEqualTo(36945);
    assertThat(properties.taxiFare()).isEqualTo(35400);
  }

  @Test
  @DisplayName("도보 경로 응답은 첫 지점에만 총 소요시간이 담기고 턴바이턴 구간에는 없다")
  void pedestrianRouteSummaryComesFromFirstFeature() {
    String json =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": {"type": "Point", "coordinates": [127.0246, 37.5045]},
              "properties": {
                "totalDistance": 368,
                "totalTime": 286,
                "index": 0,
                "description": "44m 이동",
                "pointType": "SP"
              }
            },
            {
              "type": "Feature",
              "geometry": {"type": "LineString", "coordinates": [[127.0246, 37.5045]]},
              "properties": {"index": 1, "description": ", 44m", "distance": 44, "time": 44}
            }
          ]
        }
        """;

    TmapRouteSummaryResponse response =
        JsonMapper.builder().build().readValue(json, TmapRouteSummaryResponse.class);

    assertThat(response.features()).hasSize(2);
    assertThat(response.features().getFirst().properties().totalTime()).isEqualTo(286);
    assertThat(response.features().getFirst().properties().totalDistance()).isEqualTo(368);
    assertThat(response.features().get(1).properties().totalTime()).isNull();
  }
}
