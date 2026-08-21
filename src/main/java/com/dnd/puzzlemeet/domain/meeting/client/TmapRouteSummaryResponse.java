package com.dnd.puzzlemeet.domain.meeting.client;

import java.util.List;

public record TmapRouteSummaryResponse(List<Feature> features) {

  public record Feature(Properties properties) {}

  public record Properties(Integer totalTime, Integer totalDistance, Integer taxiFare) {}
}
