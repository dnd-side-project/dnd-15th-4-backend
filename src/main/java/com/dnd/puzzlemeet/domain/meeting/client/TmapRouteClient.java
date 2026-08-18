package com.dnd.puzzlemeet.domain.meeting.client;

import com.dnd.puzzlemeet.domain.meeting.entity.TransportType;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class TmapRouteClient {

  private static final int CONNECT_TIMEOUT_MILLIS = 2_000;
  private static final int READ_TIMEOUT_MILLIS = 5_000;
  private static final String APP_KEY_HEADER = "appKey";
  private static final int RESULT_COUNT = 1;
  private static final int TOO_CLOSE_STATUS = 11;
  private static final DateTimeFormatter SEARCH_DATE_TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmm");

  private final RestClient restClient;
  private final String transitRouteUri;
  private final String appKey;

  public TmapRouteClient(TmapProperties tmapProperties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
    requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);
    this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    this.transitRouteUri = tmapProperties.transitRouteUri();
    this.appKey = tmapProperties.appKey();
  }

  public Optional<TmapRoute> findTransitRoute(
      double startLatitude,
      double startLongitude,
      double endLatitude,
      double endLongitude,
      LocalDateTime departAt) {
    long start = System.currentTimeMillis();
    TmapTransitRouteResponse response;
    try {
      response =
          restClient
              .post()
              .uri(transitRouteUri)
              .header(APP_KEY_HEADER, appKey)
              .contentType(MediaType.APPLICATION_JSON)
              .body(requestBody(startLatitude, startLongitude, endLatitude, endLongitude, departAt))
              .retrieve()
              .body(TmapTransitRouteResponse.class);
    } catch (HttpStatusCodeException e) {
      log.info(
          "[지도 연동] 대중교통 경로 조회 실패, status={}, elapsedMs={}",
          e.getStatusCode().value(),
          System.currentTimeMillis() - start);
      throw ApiException.of(ErrorCode.MEETING_MAP_UNAVAILABLE);
    } catch (RestClientException e) {
      log.info("[지도 연동] 대중교통 경로 조회 실패, elapsedMs={}", System.currentTimeMillis() - start);
      throw ApiException.of(ErrorCode.MEETING_MAP_UNAVAILABLE);
    }

    long elapsedMs = System.currentTimeMillis() - start;
    if (response == null) {
      log.info("[지도 연동] 대중교통 경로 조회 실패, 응답 본문이 비어 있음, elapsedMs={}", elapsedMs);
      throw ApiException.of(ErrorCode.MEETING_MAP_UNAVAILABLE);
    }

    if (response.result() != null) {
      int status = response.result().status();
      log.info("[지도 연동] 대중교통 경로 없음, status={}, elapsedMs={}", status, elapsedMs);
      if (status == TOO_CLOSE_STATUS) {
        return Optional.empty();
      }
      throw ApiException.of(ErrorCode.MEETING_MAP_ROUTE_NOT_FOUND);
    }

    TmapTransitRouteResponse.Itinerary itinerary = firstItinerary(response);
    if (itinerary == null || itinerary.legs() == null || itinerary.legs().isEmpty()) {
      log.info("[지도 연동] 대중교통 경로 없음, elapsedMs={}", elapsedMs);
      throw ApiException.of(ErrorCode.MEETING_MAP_ROUTE_NOT_FOUND);
    }

    log.info("[지도 연동] 대중교통 경로 조회 성공, elapsedMs={}", elapsedMs);
    return Optional.of(toRoute(itinerary));
  }

  private Map<String, Object> requestBody(
      double startLatitude,
      double startLongitude,
      double endLatitude,
      double endLongitude,
      LocalDateTime departAt) {
    Map<String, Object> body =
        new HashMap<>(
            Map.of(
                "startX", String.valueOf(startLongitude),
                "startY", String.valueOf(startLatitude),
                "endX", String.valueOf(endLongitude),
                "endY", String.valueOf(endLatitude),
                "count", RESULT_COUNT,
                "format", "json"));
    if (departAt != null) {
      body.put("searchDttm", departAt.format(SEARCH_DATE_TIME_FORMAT));
    }
    return body;
  }

  private TmapTransitRouteResponse.Itinerary firstItinerary(TmapTransitRouteResponse response) {
    if (response.metaData() == null || response.metaData().plan() == null) {
      return null;
    }
    List<TmapTransitRouteResponse.Itinerary> itineraries = response.metaData().plan().itineraries();
    if (itineraries == null || itineraries.isEmpty()) {
      return null;
    }
    return itineraries.getFirst();
  }

  private TmapRoute toRoute(TmapTransitRouteResponse.Itinerary itinerary) {
    List<TmapRoute.Leg> legs = itinerary.legs().stream().map(this::toLeg).toList();
    return new TmapRoute(itinerary.totalTime(), legs);
  }

  private TmapRoute.Leg toLeg(TmapTransitRouteResponse.Leg leg) {
    return new TmapRoute.Leg(
        toTransportType(leg.mode()),
        leg.route(),
        placeName(leg.start()),
        placeName(leg.end()),
        leg.sectionTime(),
        stationCount(leg.passStopList()));
  }

  private TransportType toTransportType(String mode) {
    if (mode == null) {
      return TransportType.ETC;
    }
    return switch (mode) {
      case "WALK" -> TransportType.WALK;
      case "BUS", "EXPRESSBUS" -> TransportType.BUS;
      case "SUBWAY", "TRAIN" -> TransportType.SUBWAY;
      default -> TransportType.ETC;
    };
  }

  private String placeName(TmapTransitRouteResponse.Place place) {
    return place != null ? place.name() : null;
  }

  private int stationCount(TmapTransitRouteResponse.PassStopList passStopList) {
    if (passStopList == null || passStopList.stations() == null) {
      return 0;
    }
    return Math.max(passStopList.stations().size() - 1, 0);
  }
}
