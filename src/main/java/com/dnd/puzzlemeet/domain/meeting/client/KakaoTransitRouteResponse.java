package com.dnd.puzzlemeet.domain.meeting.client;

import java.util.List;

public record KakaoTransitRouteResponse(String status, List<Route> routes) {

  public record Route(RouteProperties properties, List<Step> steps) {}

  public record RouteProperties(
      String type, Integer totalDistance, Integer totalTime, Integer transfers, Fare fare) {}

  public record Fare(Integer value, Integer min, Integer max) {}

  public record Step(StepProperties properties, Path path) {}

  public record StepProperties(
      String guidance,
      String type,
      Integer distance,
      Integer time,
      List<Stop> stops,
      List<Vehicle> vehicles) {}

  public record Stop(String name) {}

  public record Vehicle(String type, String name) {}

  public record Path(List<List<Double>> points) {}
}
