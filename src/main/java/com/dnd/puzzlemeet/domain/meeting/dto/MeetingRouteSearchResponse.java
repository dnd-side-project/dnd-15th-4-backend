package com.dnd.puzzlemeet.domain.meeting.dto;

import com.dnd.puzzlemeet.domain.meeting.client.TravelRoute;
import com.dnd.puzzlemeet.domain.meeting.entity.TransportType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.List;

public record MeetingRouteSearchResponse(
    @Schema(description = "추천 경로 목록", requiredMode = RequiredMode.REQUIRED) List<Route> routes) {

  public static MeetingRouteSearchResponse from(List<TravelRoute> routes) {
    return new MeetingRouteSearchResponse(routes.stream().map(Route::from).toList());
  }

  public record Route(
      @Schema(description = "총 소요시간(초)", example = "3780", requiredMode = RequiredMode.REQUIRED)
          int totalTime,
      @Schema(
              description = "요금(원). 대중교통은 운임, 차량은 예상 택시 요금, 도보는 0이다",
              example = "1850",
              requiredMode = RequiredMode.REQUIRED)
          int fare,
      @Schema(
              description = "환승 횟수. 차량과 도보는 0이다",
              example = "2",
              requiredMode = RequiredMode.REQUIRED)
          int transferCount,
      @Schema(
              description =
                  "경로 타입(1: 지하철, 2: 버스, 3: 버스+지하철, 4: 고속·시외버스, 5: 기차, 6: 항공, 7: 해운). 도보로만 이동하는 경로는 값이 없다",
              example = "3",
              nullable = true)
          Integer pathType,
      @Schema(description = "상세 이동 구간 목록", requiredMode = RequiredMode.REQUIRED) List<Step> steps) {

    public static Route from(TravelRoute route) {
      return new Route(
          route.totalTimeSeconds(),
          route.fare(),
          route.transferCount(),
          route.pathType(),
          route.legs().stream().map(Step::from).toList());
    }
  }

  public record Step(
      @Schema(description = "이동수단 타입", example = "SUBWAY", requiredMode = RequiredMode.REQUIRED)
          TransportType type,
      @Schema(description = "구간 소요시간(초)", example = "1620", requiredMode = RequiredMode.REQUIRED)
          int time,
      @Schema(description = "구간 이동거리(m)", example = "27000", requiredMode = RequiredMode.REQUIRED)
          int distance,
      @Schema(description = "이동 설명. 도보 구간에만 담긴다", example = "성수역 이동", nullable = true)
          String description,
      @Schema(description = "노선명", example = "수도권6호선", nullable = true) String line,
      @Schema(description = "노선 색상", example = "CD7C2F", nullable = true) String color,
      @Schema(description = "구간의 시작·끝 지점. 대중교통 구간은 승하차 역·정류장이다", nullable = true) Station station,
      @Schema(description = "이동 경로상의 정류장 목록", nullable = true) List<String> stations,
      @Schema(description = "시작 좌표", nullable = true) Location startLocation,
      @Schema(description = "종료 좌표", nullable = true) Location endLocation) {

    private static final String WALK_DESCRIPTION_SUFFIX = " 이동";
    private static final String WALK_DESCRIPTION_FALLBACK = "도보 이동";

    public static Step from(TravelRoute.Leg leg) {
      boolean walking = leg.transportType() == TransportType.WALK;
      return new Step(
          leg.transportType(),
          leg.sectionTimeSeconds(),
          leg.distanceMeters(),
          walking ? walkDescription(leg) : null,
          leg.routeName(),
          leg.routeColor(),
          Station.of(leg.startName(), leg.endName()),
          leg.stationNames().isEmpty() ? null : leg.stationNames(),
          Location.of(leg.startLatitude(), leg.startLongitude()),
          Location.of(leg.endLatitude(), leg.endLongitude()));
    }

    private static String walkDescription(TravelRoute.Leg leg) {
      return leg.endName() != null
          ? leg.endName() + WALK_DESCRIPTION_SUFFIX
          : WALK_DESCRIPTION_FALLBACK;
    }
  }

  public record Station(
      @Schema(description = "출발 정류장·역", example = "태릉입구", requiredMode = RequiredMode.REQUIRED)
          String start,
      @Schema(description = "도착 정류장·역", example = "성수", requiredMode = RequiredMode.REQUIRED)
          String end) {

    public static Station of(String start, String end) {
      return start != null || end != null ? new Station(start, end) : null;
    }
  }

  public record Location(
      @Schema(description = "위도", example = "37.5045", requiredMode = RequiredMode.REQUIRED)
          double lat,
      @Schema(description = "경도", example = "127.0247", requiredMode = RequiredMode.REQUIRED)
          double lon) {

    public static Location of(Double latitude, Double longitude) {
      return latitude != null && longitude != null ? new Location(latitude, longitude) : null;
    }
  }
}
