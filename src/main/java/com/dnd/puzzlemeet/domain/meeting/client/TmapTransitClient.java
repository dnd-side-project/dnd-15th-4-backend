package com.dnd.puzzlemeet.domain.meeting.client;

import com.dnd.puzzlemeet.domain.meeting.entity.TransportType;
import com.dnd.puzzlemeet.global.client.TmapProperties;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
public class TmapTransitClient {

  private static final int CONNECT_TIMEOUT_MILLIS = 2_000;
  private static final int READ_TIMEOUT_MILLIS = 5_000;
  private static final String APP_KEY_HEADER = "appKey";
  private static final int ROUTE_SEARCH_COUNT = 5;
  private static final int TOO_CLOSE_STATUS = 11;
  private static final DateTimeFormatter SEARCH_DATE_TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmm");

  private final RestClient restClient;
  private final String transitRouteUri;
  private final String appKey;

  public TmapTransitClient(TmapProperties tmapProperties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
    requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);
    this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    this.transitRouteUri = tmapProperties.transitRouteUri();
    this.appKey = tmapProperties.appKey();
  }

  public List<TravelRoute> findTransitRoutes(
      double startLatitude,
      double startLongitude,
      double endLatitude,
      double endLongitude,
      LocalDateTime departAt) {
    return toRoutes(
        requestItineraries(
            startLatitude,
            startLongitude,
            endLatitude,
            endLongitude,
            departAt,
            ROUTE_SEARCH_COUNT));
  }

  private List<TmapTransitRouteResponse.Itinerary> requestItineraries(
      double startLatitude,
      double startLongitude,
      double endLatitude,
      double endLongitude,
      LocalDateTime departAt,
      int count) {
    long start = System.currentTimeMillis();
    TmapTransitRouteResponse response;
    try {
      response =
          restClient
              .post()
              .uri(transitRouteUri)
              .header(APP_KEY_HEADER, appKey)
              .contentType(MediaType.APPLICATION_JSON)
              .body(
                  requestBody(
                      startLatitude, startLongitude, endLatitude, endLongitude, departAt, count))
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
        throw ApiException.of(ErrorCode.MEETING_MAP_TOO_CLOSE);
      }
      throw ApiException.of(ErrorCode.MEETING_MAP_ROUTE_NOT_FOUND);
    }

    List<TmapTransitRouteResponse.Itinerary> itineraries = itineraries(response);
    if (itineraries.isEmpty() || !hasLegs(itineraries.getFirst())) {
      log.info("[지도 연동] 대중교통 경로 없음, elapsedMs={}", elapsedMs);
      throw ApiException.of(ErrorCode.MEETING_MAP_ROUTE_NOT_FOUND);
    }

    log.info("[지도 연동] 대중교통 경로 조회 성공, elapsedMs={}", elapsedMs);
    return itineraries;
  }

  private Map<String, Object> requestBody(
      double startLatitude,
      double startLongitude,
      double endLatitude,
      double endLongitude,
      LocalDateTime departAt,
      int count) {
    Map<String, Object> body =
        new HashMap<>(
            Map.of(
                "startX", String.valueOf(startLongitude),
                "startY", String.valueOf(startLatitude),
                "endX", String.valueOf(endLongitude),
                "endY", String.valueOf(endLatitude),
                "count", count,
                "format", "json"));
    if (departAt != null) {
      body.put("searchDttm", departAt.format(SEARCH_DATE_TIME_FORMAT));
    }
    return body;
  }

  private List<TmapTransitRouteResponse.Itinerary> itineraries(TmapTransitRouteResponse response) {
    if (response.metaData() == null || response.metaData().plan() == null) {
      return List.of();
    }
    List<TmapTransitRouteResponse.Itinerary> itineraries = response.metaData().plan().itineraries();
    return itineraries != null ? itineraries : List.of();
  }

  private boolean hasLegs(TmapTransitRouteResponse.Itinerary itinerary) {
    return itinerary.legs() != null && !itinerary.legs().isEmpty();
  }

  private List<TravelRoute> toRoutes(List<TmapTransitRouteResponse.Itinerary> itineraries) {
    return itineraries.stream().filter(this::hasLegs).map(this::toRoute).toList();
  }

  private TravelRoute toRoute(TmapTransitRouteResponse.Itinerary itinerary) {
    return new TravelRoute(
        itinerary.totalTime(),
        totalFare(itinerary),
        itinerary.transferCount(),
        itinerary.pathType(),
        itinerary.legs().stream().map(this::toLeg).toList());
  }

  private int totalFare(TmapTransitRouteResponse.Itinerary itinerary) {
    if (itinerary.fare() == null || itinerary.fare().regular() == null) {
      return 0;
    }
    return itinerary.fare().regular().totalFare();
  }

  private TravelRoute.Leg toLeg(TmapTransitRouteResponse.Leg leg) {
    return new TravelRoute.Leg(
        toTransportType(leg.mode()),
        leg.route(),
        leg.routeColor(),
        leg.sectionTime(),
        leg.distance(),
        placeName(leg.start()),
        placeName(leg.end()),
        latitude(leg.start()),
        longitude(leg.start()),
        latitude(leg.end()),
        longitude(leg.end()),
        stationNames(leg.passStopList()));
  }

  private List<String> stationNames(TmapTransitRouteResponse.PassStopList passStopList) {
    if (passStopList == null || passStopList.stations() == null) {
      return List.of();
    }
    return passStopList.stations().stream()
        .map(TmapTransitRouteResponse.Station::stationName)
        .toList();
  }

  private Double latitude(TmapTransitRouteResponse.Place place) {
    return place != null ? place.lat() : null;
  }

  private Double longitude(TmapTransitRouteResponse.Place place) {
    return place != null ? place.lon() : null;
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
}
