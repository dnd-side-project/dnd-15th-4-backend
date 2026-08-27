package com.dnd.puzzlemeet.domain.meeting.client;

import com.dnd.puzzlemeet.domain.meeting.entity.TransportType;
import com.dnd.puzzlemeet.global.client.TmapProperties;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class TmapCarClient {

  private static final int CONNECT_TIMEOUT_MILLIS = 2_000;
  private static final int READ_TIMEOUT_MILLIS = 5_000;
  private static final String APP_KEY_HEADER = "appKey";
  private static final String SUMMARY_QUERY = "?version=1&totalValue=2";
  private static final String SEARCH_OPTION_RECOMMENDED = "00";
  private static final String PREDICT_DEPARTURE_TIME = "departure";
  private static final String PREDICT_ARRIVAL_TIME = "arrival";
  private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
  private static final DateTimeFormatter PREDICTION_TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

  private final RestClient restClient;
  private final String carRouteUri;
  private final String appKey;

  public TmapCarClient(TmapProperties tmapProperties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
    requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);
    this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    this.carRouteUri = tmapProperties.carRouteUri();
    this.appKey = tmapProperties.appKey();
  }

  public TravelRoute findCarRoute(
      double startLatitude,
      double startLongitude,
      double endLatitude,
      double endLongitude,
      String destinationName,
      LocalDateTime arriveAt) {
    TmapRouteSummaryResponse.Properties summary =
        requestSummary(startLatitude, startLongitude, endLatitude, endLongitude, arriveAt);
    return new TravelRoute(
        summary.totalTime(),
        summary.taxiFare() != null ? summary.taxiFare() : 0,
        0,
        null,
        List.of(
            new TravelRoute.Leg(
                TransportType.CAR,
                null,
                null,
                summary.totalTime(),
                summary.totalDistance() != null ? summary.totalDistance() : 0,
                null,
                destinationName,
                startLatitude,
                startLongitude,
                endLatitude,
                endLongitude,
                List.of(),
                null)));
  }

  private TmapRouteSummaryResponse.Properties requestSummary(
      double startLatitude,
      double startLongitude,
      double endLatitude,
      double endLongitude,
      LocalDateTime arriveAt) {
    long start = System.currentTimeMillis();
    TmapRouteSummaryResponse response;
    try {
      response =
          restClient
              .post()
              .uri(carRouteUri + SUMMARY_QUERY)
              .header(APP_KEY_HEADER, appKey)
              .contentType(MediaType.APPLICATION_JSON)
              .body(requestBody(startLatitude, startLongitude, endLatitude, endLongitude, arriveAt))
              .retrieve()
              .body(TmapRouteSummaryResponse.class);
    } catch (HttpStatusCodeException e) {
      log.info(
          "[지도 연동] 차량 경로 조회 실패, status={}, elapsedMs={}",
          e.getStatusCode().value(),
          System.currentTimeMillis() - start);
      throw ApiException.of(ErrorCode.MEETING_MAP_UNAVAILABLE);
    } catch (RestClientException e) {
      log.info("[지도 연동] 차량 경로 조회 실패, elapsedMs={}", System.currentTimeMillis() - start);
      throw ApiException.of(ErrorCode.MEETING_MAP_UNAVAILABLE);
    }

    long elapsedMs = System.currentTimeMillis() - start;
    if (response == null) {
      log.info("[지도 연동] 차량 경로 조회 실패, 응답 본문이 비어 있음, elapsedMs={}", elapsedMs);
      throw ApiException.of(ErrorCode.MEETING_MAP_UNAVAILABLE);
    }

    TmapRouteSummaryResponse.Properties summary = summary(response);
    if (summary == null) {
      log.info("[지도 연동] 차량 경로 없음, elapsedMs={}", elapsedMs);
      throw ApiException.of(ErrorCode.MEETING_MAP_ROUTE_NOT_FOUND);
    }

    log.info("[지도 연동] 차량 경로 조회 성공, elapsedMs={}", elapsedMs);
    return summary;
  }

  private Map<String, Object> requestBody(
      double startLatitude,
      double startLongitude,
      double endLatitude,
      double endLongitude,
      LocalDateTime arriveAt) {
    LocalDateTime predictionTime = arriveAt != null ? arriveAt : LocalDateTime.now();
    return Map.of(
        "routesInfo",
        Map.of(
            "departure",
                Map.of(
                    "name", "출발지",
                    "lon", String.valueOf(startLongitude),
                    "lat", String.valueOf(startLatitude)),
            "destination",
                Map.of(
                    "name", "도착지",
                    "lon", String.valueOf(endLongitude),
                    "lat", String.valueOf(endLatitude)),
            "predictionType", arriveAt != null ? PREDICT_DEPARTURE_TIME : PREDICT_ARRIVAL_TIME,
            "predictionTime", predictionTime.atZone(KOREA).format(PREDICTION_TIME_FORMAT),
            "searchOption", SEARCH_OPTION_RECOMMENDED));
  }

  private TmapRouteSummaryResponse.Properties summary(TmapRouteSummaryResponse response) {
    if (response.features() == null) {
      return null;
    }
    return response.features().stream()
        .map(TmapRouteSummaryResponse.Feature::properties)
        .filter(properties -> properties != null && properties.totalTime() != null)
        .findFirst()
        .orElse(null);
  }
}
