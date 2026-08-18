package com.dnd.puzzlemeet.domain.meeting.client;

import java.util.List;

public record TmapTransitRouteResponse(MetaData metaData, Result result) {

  public record MetaData(Plan plan) {}

  public record Plan(List<Itinerary> itineraries) {}

  public record Itinerary(int totalTime, List<Leg> legs) {}

  public record Leg(
      String mode,
      String route,
      int sectionTime,
      Place start,
      Place end,
      PassStopList passStopList) {}

  public record Place(String name) {}

  public record PassStopList(List<Station> stations) {}

  public record Station(int index, String stationName) {}

  public record Result(int status, String message) {}
}
