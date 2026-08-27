package com.dnd.puzzlemeet.domain.meeting.client;

import com.dnd.puzzlemeet.domain.meeting.entity.TransportType;
import com.dnd.puzzlemeet.global.config.KakaoMapProperties;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.security.KakaoProperties;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class KakaoTransitClient implements TransitRouteProvider {

  private static final int CONNECT_TIMEOUT_MILLIS = 2_000;
  private static final int READ_TIMEOUT_MILLIS = 5_000;
  private static final String AUTHORIZATION_PREFIX = "KakaoAK ";
  private static final int MAX_ROUTE_COUNT = 5;

  private static final String STATUS_OK = "OK";
  private static final String STATUS_EQUAL_POINTS = "EQUAL_POINTS";
  private static final String STATUS_STARTNODES_NULL = "STARTNODES_NULL";
  private static final String STATUS_ENDNODES_NULL = "ENDNODES_NULL";
  private static final String STATUS_NO_RESULTS = "NO_RESULTS";

  private static final String ROUTE_TYPE_SUBWAY = "SUBWAY";
  private static final String ROUTE_TYPE_BUS = "BUS";
  private static final String ROUTE_TYPE_BUS_AND_SUBWAY = "BUS_AND_SUBWAY";
  private static final int PATH_TYPE_SUBWAY = 1;
  private static final int PATH_TYPE_BUS = 2;
  private static final int PATH_TYPE_BUS_AND_SUBWAY = 3;

  private static final double EARTH_RADIUS_M = 6_371_000;
  private static final int TOO_CLOSE_DISTANCE_METERS = 300;

  private static final String STEP_TYPE_WALKING = "WALKING";
  private static final int LONGITUDE_INDEX = 0;
  private static final int LATITUDE_INDEX = 1;
  private static final int POINT_SIZE = 2;

  private final RestClient restClient;
  private final String transitRouteUri;
  private final String restApiKey;

  @Autowired
  public KakaoTransitClient(
      KakaoMapProperties kakaoMapProperties, KakaoProperties kakaoProperties) {
    this(kakaoMapProperties, kakaoProperties, createRestClient());
  }

  KakaoTransitClient(
      KakaoMapProperties kakaoMapProperties,
      KakaoProperties kakaoProperties,
      RestClient restClient) {
    this.restClient = restClient;
    this.transitRouteUri = kakaoMapProperties.transitRouteUri();
    this.restApiKey = kakaoProperties.clientId();
  }

  private static RestClient createRestClient() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
    requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);
    return RestClient.builder().requestFactory(requestFactory).build();
  }

  @Override
  public TransitRouteProviderType type() {
    return TransitRouteProviderType.KAKAO;
  }

  @Override
  public boolean supportsDepartureTime() {
    return false;
  }

  @Override
  public List<TravelRoute> findRoutes(
      double startLatitude,
      double startLongitude,
      double endLatitude,
      double endLongitude,
      LocalDateTime departAt) {
    return requestRoutes(startLatitude, startLongitude, endLatitude, endLongitude).stream()
        .map(this::toRoute)
        .toList();
  }

  private List<KakaoTransitRouteResponse.Route> requestRoutes(
      double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
    long start = System.currentTimeMillis();
    KakaoTransitRouteResponse response;
    try {
      response =
          restClient
              .get()
              .uri(routeUri(startLatitude, startLongitude, endLatitude, endLongitude))
              .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_PREFIX + restApiKey)
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .body(KakaoTransitRouteResponse.class);
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
    if (response == null || response.status() == null) {
      log.info("[지도 연동] 대중교통 경로 조회 실패, 응답 본문이 비어 있음, elapsedMs={}", elapsedMs);
      throw ApiException.of(ErrorCode.MEETING_MAP_UNAVAILABLE);
    }

    if (!STATUS_OK.equals(response.status())) {
      log.info("[지도 연동] 대중교통 경로 없음, status={}, elapsedMs={}", response.status(), elapsedMs);
      if (isTooClose(response.status(), startLatitude, startLongitude, endLatitude, endLongitude)) {
        return List.of();
      }
      throw ApiException.of(errorCode(response.status()));
    }

    List<KakaoTransitRouteResponse.Route> routes = validRoutes(response);
    if (routes.isEmpty()) {
      log.info("[지도 연동] 대중교통 경로 없음, elapsedMs={}", elapsedMs);
      throw ApiException.of(ErrorCode.MEETING_MAP_ROUTE_NOT_FOUND);
    }

    log.info("[지도 연동] 대중교통 경로 조회 성공, elapsedMs={}", elapsedMs);
    return routes;
  }

  private boolean isTooClose(
      String status,
      double startLatitude,
      double startLongitude,
      double endLatitude,
      double endLongitude) {
    if (STATUS_EQUAL_POINTS.equals(status)) {
      return true;
    }
    return STATUS_NO_RESULTS.equals(status)
        && distanceMeters(startLatitude, startLongitude, endLatitude, endLongitude)
            < TOO_CLOSE_DISTANCE_METERS;
  }

  private ErrorCode errorCode(String status) {
    return switch (status) {
      case STATUS_STARTNODES_NULL, STATUS_ENDNODES_NULL, STATUS_NO_RESULTS ->
          ErrorCode.MEETING_MAP_ROUTE_NOT_FOUND;
      default -> ErrorCode.MEETING_MAP_UNAVAILABLE;
    };
  }

  private double distanceMeters(
      double fromLatitude, double fromLongitude, double toLatitude, double toLongitude) {
    double latitudeDifference = Math.toRadians(toLatitude - fromLatitude);
    double longitudeDifference = Math.toRadians(toLongitude - fromLongitude);
    double fromLatitudeRadians = Math.toRadians(fromLatitude);
    double toLatitudeRadians = Math.toRadians(toLatitude);

    double haversine =
        Math.pow(Math.sin(latitudeDifference / 2), 2)
            + Math.cos(fromLatitudeRadians)
                * Math.cos(toLatitudeRadians)
                * Math.pow(Math.sin(longitudeDifference / 2), 2);
    double centralAngle = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    return EARTH_RADIUS_M * centralAngle;
  }

  private URI routeUri(
      double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
    return UriComponentsBuilder.fromUriString(transitRouteUri)
        .queryParam("start_x", startLongitude)
        .queryParam("start_y", startLatitude)
        .queryParam("end_x", endLongitude)
        .queryParam("end_y", endLatitude)
        .build()
        .encode()
        .toUri();
  }

  private List<KakaoTransitRouteResponse.Route> validRoutes(KakaoTransitRouteResponse response) {
    if (response.routes() == null) {
      return List.of();
    }
    return response.routes().stream().filter(this::hasSteps).limit(MAX_ROUTE_COUNT).toList();
  }

  private boolean hasSteps(KakaoTransitRouteResponse.Route route) {
    return route != null
        && route.properties() != null
        && route.steps() != null
        && route.steps().stream().anyMatch(this::hasProperties);
  }

  private boolean hasProperties(KakaoTransitRouteResponse.Step step) {
    return step != null && step.properties() != null;
  }

  private TravelRoute toRoute(KakaoTransitRouteResponse.Route route) {
    KakaoTransitRouteResponse.RouteProperties properties = route.properties();
    return new TravelRoute(
        intValue(properties.totalTime()),
        fare(properties),
        intValue(properties.transfers()),
        pathType(properties.type()),
        route.steps().stream().filter(this::hasProperties).map(this::toLeg).toList());
  }

  private int fare(KakaoTransitRouteResponse.RouteProperties properties) {
    KakaoTransitRouteResponse.Fare fare = properties.fare();
    if (fare == null) {
      return 0;
    }
    return fare.value() != null ? fare.value() : intValue(fare.min());
  }

  private Integer pathType(String routeType) {
    if (routeType == null) {
      return null;
    }
    return switch (routeType) {
      case ROUTE_TYPE_SUBWAY -> PATH_TYPE_SUBWAY;
      case ROUTE_TYPE_BUS -> PATH_TYPE_BUS;
      case ROUTE_TYPE_BUS_AND_SUBWAY -> PATH_TYPE_BUS_AND_SUBWAY;
      default -> null;
    };
  }

  private TravelRoute.Leg toLeg(KakaoTransitRouteResponse.Step step) {
    KakaoTransitRouteResponse.StepProperties properties = step.properties();
    TransportType transportType = toTransportType(properties.type());
    List<String> stopNames =
        transportType == TransportType.WALK ? List.of() : stopNames(properties);
    List<Double> startPoint = point(step.path(), true);
    List<Double> endPoint = point(step.path(), false);
    return new TravelRoute.Leg(
        transportType,
        vehicleName(properties),
        null,
        intValue(properties.time()),
        intValue(properties.distance()),
        firstStopName(stopNames),
        lastStopName(stopNames),
        latitude(startPoint),
        longitude(startPoint),
        latitude(endPoint),
        longitude(endPoint),
        stopNames,
        transportType == TransportType.WALK ? properties.guidance() : null);
  }

  private TransportType toTransportType(String stepType) {
    if (stepType == null) {
      return TransportType.ETC;
    }
    return switch (stepType) {
      case STEP_TYPE_WALKING -> TransportType.WALK;
      case ROUTE_TYPE_BUS -> TransportType.BUS;
      case ROUTE_TYPE_SUBWAY -> TransportType.SUBWAY;
      default -> TransportType.ETC;
    };
  }

  private String vehicleName(KakaoTransitRouteResponse.StepProperties properties) {
    if (properties.vehicles() == null) {
      return null;
    }
    return properties.vehicles().stream()
        .filter(vehicle -> vehicle != null && vehicle.name() != null)
        .map(KakaoTransitRouteResponse.Vehicle::name)
        .findFirst()
        .orElse(null);
  }

  private List<String> stopNames(KakaoTransitRouteResponse.StepProperties properties) {
    if (properties.stops() == null) {
      return List.of();
    }
    return properties.stops().stream()
        .filter(stop -> stop != null && stop.name() != null)
        .map(KakaoTransitRouteResponse.Stop::name)
        .toList();
  }

  private String firstStopName(List<String> stopNames) {
    return stopNames.isEmpty() ? null : stopNames.getFirst();
  }

  private String lastStopName(List<String> stopNames) {
    return stopNames.isEmpty() ? null : stopNames.getLast();
  }

  private List<Double> point(KakaoTransitRouteResponse.Path path, boolean first) {
    if (path == null || path.points() == null || path.points().isEmpty()) {
      return null;
    }
    List<Double> point = first ? path.points().getFirst() : path.points().getLast();
    return point != null && point.size() >= POINT_SIZE ? point : null;
  }

  private Double latitude(List<Double> point) {
    return point != null ? point.get(LATITUDE_INDEX) : null;
  }

  private Double longitude(List<Double> point) {
    return point != null ? point.get(LONGITUDE_INDEX) : null;
  }

  private int intValue(Integer value) {
    return value != null ? value : 0;
  }
}
