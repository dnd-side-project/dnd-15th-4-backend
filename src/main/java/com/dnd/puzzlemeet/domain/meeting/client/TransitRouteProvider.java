package com.dnd.puzzlemeet.domain.meeting.client;

import java.time.LocalDateTime;
import java.util.List;

public interface TransitRouteProvider {

  TransitRouteProviderType type();

  boolean supportsDepartureTime();

  List<TravelRoute> findRoutes(
      double startLatitude,
      double startLongitude,
      double endLatitude,
      double endLongitude,
      LocalDateTime departAt);
}
