package com.dnd.puzzlemeet.domain.meeting.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class TmapTransitRouteResponseTest {

  @Test
  @DisplayName("대중교통 경로 응답의 구간 정보와 경유 정류장 목록이 매핑된다")
  void transitRouteFieldsBindToRecordComponents() {
    String json =
        """
        {
          "metaData": {
            "requestParameters": {"startX": "127.0752", "startY": "37.6180"},
            "plan": {
              "itineraries": [
                {
                  "totalTime": 2696,
                  "totalWalkTime": 400,
                  "transferCount": 1,
                  "pathType": 1,
                  "legs": [
                    {
                      "mode": "WALK",
                      "sectionTime": 73,
                      "distance": 96,
                      "start": {"name": "출발지", "lon": 127.0752, "lat": 37.6180},
                      "end": {"name": "태릉입구", "lon": 127.07536, "lat": 37.61753}
                    },
                    {
                      "mode": "SUBWAY",
                      "routeColor": "CD7C2F",
                      "sectionTime": 1141,
                      "route": "수도권6호선",
                      "routeId": "110061007",
                      "distance": 9648,
                      "service": 0,
                      "start": {"name": "태릉입구", "lon": 127.07536, "lat": 37.61753},
                      "passStopList": {
                        "stations": [
                          {"index": 0, "stationName": "태릉입구", "lon": "127.075367", "lat": "37.617539", "stationID": "110636"},
                          {"index": 1, "stationName": "석계", "lon": "127.066108", "lat": "37.614994", "stationID": "110635"},
                          {"index": 2, "stationName": "돌곶이", "lon": "127.056508", "lat": "37.610550", "stationID": "110634"},
                          {"index": 3, "stationName": "청구", "lon": "127.013744", "lat": "37.560189", "stationID": "110625"}
                        ]
                      },
                      "end": {"name": "청구", "lon": 127.01374, "lat": 37.56018},
                      "type": 6
                    }
                  ]
                }
              ]
            }
          }
        }
        """;
    JsonMapper jsonMapper = JsonMapper.builder().build();

    TmapTransitRouteResponse response = jsonMapper.readValue(json, TmapTransitRouteResponse.class);

    TmapTransitRouteResponse.Itinerary itinerary =
        response.metaData().plan().itineraries().getFirst();
    assertThat(response.result()).isNull();
    assertThat(itinerary.totalTime()).isEqualTo(2696);
    assertThat(itinerary.legs()).hasSize(2);

    TmapTransitRouteResponse.Leg walkLeg = itinerary.legs().getFirst();
    assertThat(walkLeg.mode()).isEqualTo("WALK");
    assertThat(walkLeg.sectionTime()).isEqualTo(73);
    assertThat(walkLeg.start().name()).isEqualTo("출발지");
    assertThat(walkLeg.passStopList()).isNull();

    TmapTransitRouteResponse.Leg subwayLeg = itinerary.legs().get(1);
    assertThat(subwayLeg.mode()).isEqualTo("SUBWAY");
    assertThat(subwayLeg.route()).isEqualTo("수도권6호선");
    assertThat(subwayLeg.passStopList().stations()).hasSize(4);
    assertThat(subwayLeg.passStopList().stations().getFirst().stationName())
        .isEqualTo(subwayLeg.start().name());
    assertThat(subwayLeg.passStopList().stations().getLast().stationName())
        .isEqualTo(subwayLeg.end().name());
  }

  @Test
  @DisplayName("경로의 요금과 환승 횟수, 구간 거리와 좌표가 매핑된다")
  void fareAndSectionDetailsBindToRecordComponents() {
    String json =
        """
        {
          "metaData": {
            "plan": {
              "itineraries": [
                {
                  "fare": {"regular": {"totalFare": 1850, "currency": {"symbol": "₩", "currency": "원"}}},
                  "totalTime": 3780,
                  "totalDistance": 27420,
                  "transferCount": 2,
                  "pathType": 3,
                  "legs": [
                    {
                      "mode": "WALK",
                      "sectionTime": 480,
                      "distance": 420,
                      "start": {"name": "출발지", "lon": 127.0247, "lat": 37.5045},
                      "end": {"name": "태릉입구", "lon": 127.0256, "lat": 37.5017}
                    },
                    {
                      "mode": "SUBWAY",
                      "route": "수도권6호선",
                      "routeColor": "CD7C2F",
                      "sectionTime": 1620,
                      "distance": 27000,
                      "start": {"name": "태릉입구", "lon": 127.0256, "lat": 37.5017},
                      "end": {"name": "성수", "lon": 127.0559, "lat": 37.5446}
                    }
                  ]
                }
              ]
            }
          }
        }
        """;
    JsonMapper jsonMapper = JsonMapper.builder().build();

    TmapTransitRouteResponse response = jsonMapper.readValue(json, TmapTransitRouteResponse.class);

    TmapTransitRouteResponse.Itinerary itinerary =
        response.metaData().plan().itineraries().getFirst();
    assertThat(itinerary.fare().regular().totalFare()).isEqualTo(1850);
    assertThat(itinerary.transferCount()).isEqualTo(2);
    assertThat(itinerary.pathType()).isEqualTo(3);

    TmapTransitRouteResponse.Leg walkLeg = itinerary.legs().getFirst();
    assertThat(walkLeg.distance()).isEqualTo(420);
    assertThat(walkLeg.start().lat()).isEqualTo(37.5045);
    assertThat(walkLeg.start().lon()).isEqualTo(127.0247);

    TmapTransitRouteResponse.Leg subwayLeg = itinerary.legs().get(1);
    assertThat(subwayLeg.routeColor()).isEqualTo("CD7C2F");
    assertThat(subwayLeg.distance()).isEqualTo(27000);
    assertThat(subwayLeg.end().lat()).isEqualTo(37.5446);
  }

  @Test
  @DisplayName("출발지와 도착지가 너무 가까우면 경로 없이 result 상태만 담겨 온다")
  void tooCloseResponseBindsResultStatus() {
    String json =
        """
        {
          "result": {"message": "출발지와 도착지가 너무 가까움", "status": 11}
        }
        """;
    JsonMapper jsonMapper = JsonMapper.builder().build();

    TmapTransitRouteResponse response = jsonMapper.readValue(json, TmapTransitRouteResponse.class);

    assertThat(response.metaData()).isNull();
    assertThat(response.result().status()).isEqualTo(11);
    assertThat(response.result().message()).isEqualTo("출발지와 도착지가 너무 가까움");
  }
}
