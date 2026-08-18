package com.dnd.puzzlemeet.domain.meeting.client;

import com.dnd.puzzlemeet.domain.meeting.entity.TransportType;
import java.util.List;

public record TmapRoute(int totalTimeSeconds, List<Leg> legs) {

  public record Leg(
      TransportType transportType,
      String routeName,
      String startName,
      String endName,
      int sectionTimeSeconds,
      int stationCount) {}
}
