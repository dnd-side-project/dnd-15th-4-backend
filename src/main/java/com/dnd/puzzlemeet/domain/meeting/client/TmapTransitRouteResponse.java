package com.dnd.puzzlemeet.domain.meeting.client;

import java.util.List;

public record TmapTransitRouteResponse(MetaData metaData, Result result) {

  public record MetaData(Plan plan) {}

  public record Plan(List<Itinerary> itineraries) {}

  public record Itinerary(
      int totalTime, int transferCount, Integer pathType, Fare fare, List<Leg> legs) {}

  public record Fare(Regular regular) {}

  public record Regular(int totalFare) {}

  public record Leg(
      String mode,
      String route,
      String routeColor,
      int sectionTime,
      int distance,
      Place start,
      Place end,
      PassStopList passStopList) {}

  public record Place(String name, Double lon, Double lat) {}

  public record PassStopList(List<Station> stations) {}

  public record Station(int index, String stationName) {}

  public record Result(int status, String message) {}
}
