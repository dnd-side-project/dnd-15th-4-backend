package com.dnd.puzzlemeet.domain.meeting.client;

import com.dnd.puzzlemeet.domain.meeting.entity.TransportType;
import java.util.List;

public record TravelRoute(
    int totalTimeSeconds, int fare, int transferCount, Integer pathType, List<Leg> legs) {

  public record Leg(
      TransportType transportType,
      String routeName,
      String routeColor,
      int sectionTimeSeconds,
      int distanceMeters,
      String startName,
      String endName,
      Double startLatitude,
      Double startLongitude,
      Double endLatitude,
      Double endLongitude,
      List<String> stationNames) {

    public int stationCount() {
      return Math.max(stationNames.size() - 1, 0);
    }
  }
}
