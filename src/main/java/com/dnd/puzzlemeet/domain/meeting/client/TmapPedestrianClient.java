package com.dnd.puzzlemeet.domain.meeting.client;

import com.dnd.puzzlemeet.domain.meeting.entity.TransportType;
import com.dnd.puzzlemeet.global.client.TmapProperties;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
public class TmapPedestrianClient {

  private static final int CONNECT_TIMEOUT_MILLIS = 2_000;
  private static final int READ_TIMEOUT_MILLIS = 5_000;
  private static final String APP_KEY_HEADER = "appKey";
  private static final String VERSION_QUERY = "?version=1";
  private static final String COORD_TYPE = "WGS84GEO";
  private static final String SEARCH_OPTION_SHORTEST = "0";
  private static final String SORT_BY_INDEX = "index";
  private static final String START_NAME = "출발지";

  private final RestClient restClient;
  private final String pedestrianRouteUri;
  private final String appKey;

  public TmapPedestrianClient(TmapProperties tmapProperties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
    requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);
    this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    this.pedestrianRouteUri = tmapProperties.pedestrianRouteUri();
    this.appKey = tmapProperties.appKey();
  }

  public TravelRoute findWalkingRoute(
      double startLatitude,
      double startLongitude,
      double endLatitude,
      double endLongitude,
      String destinationName) {
    TmapRouteSummaryResponse.Properties summary =
        requestSummary(startLatitude, startLongitude, endLatitude, endLongitude, destinationName);
    return new TravelRoute(
        summary.totalTime(),
        0,
        0,
        null,
        List.of(
            new TravelRoute.Leg(
                TransportType.WALK,
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
                List.of())));
  }

  private TmapRouteSummaryResponse.Properties requestSummary(
      double startLatitude,
      double startLongitude,
      double endLatitude,
      double endLongitude,
      String destinationName) {
    long start = System.currentTimeMillis();
    TmapRouteSummaryResponse response;
    try {
      response =
          restClient
              .post()
              .uri(pedestrianRouteUri + VERSION_QUERY)
              .header(APP_KEY_HEADER, appKey)
              .contentType(MediaType.APPLICATION_JSON)
              .body(
                  requestBody(
                      startLatitude, startLongitude, endLatitude, endLongitude, destinationName))
              .retrieve()
              .body(TmapRouteSummaryResponse.class);
    } catch (HttpStatusCodeException e) {
      log.info(
          "[지도 연동] 도보 경로 조회 실패, status={}, elapsedMs={}",
          e.getStatusCode().value(),
          System.currentTimeMillis() - start);
      throw ApiException.of(ErrorCode.MEETING_MAP_UNAVAILABLE);
    } catch (RestClientException e) {
      log.info("[지도 연동] 도보 경로 조회 실패, elapsedMs={}", System.currentTimeMillis() - start);
      throw ApiException.of(ErrorCode.MEETING_MAP_UNAVAILABLE);
    }

    long elapsedMs = System.currentTimeMillis() - start;
    if (response == null) {
      log.info("[지도 연동] 도보 경로 조회 실패, 응답 본문이 비어 있음, elapsedMs={}", elapsedMs);
      throw ApiException.of(ErrorCode.MEETING_MAP_UNAVAILABLE);
    }

    TmapRouteSummaryResponse.Properties summary = summary(response);
    if (summary == null) {
      log.info("[지도 연동] 도보 경로 없음, elapsedMs={}", elapsedMs);
      throw ApiException.of(ErrorCode.MEETING_MAP_ROUTE_NOT_FOUND);
    }

    log.info("[지도 연동] 도보 경로 조회 성공, elapsedMs={}", elapsedMs);
    return summary;
  }

  private Map<String, Object> requestBody(
      double startLatitude,
      double startLongitude,
      double endLatitude,
      double endLongitude,
      String destinationName) {
    return Map.of(
        "startX", String.valueOf(startLongitude),
        "startY", String.valueOf(startLatitude),
        "endX", String.valueOf(endLongitude),
        "endY", String.valueOf(endLatitude),
        "startName", encode(START_NAME),
        "endName", encode(destinationName),
        "reqCoordType", COORD_TYPE,
        "resCoordType", COORD_TYPE,
        "searchOption", SEARCH_OPTION_SHORTEST,
        "sort", SORT_BY_INDEX);
  }

  private String encode(String name) {
    return URLEncoder.encode(name, StandardCharsets.UTF_8);
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
