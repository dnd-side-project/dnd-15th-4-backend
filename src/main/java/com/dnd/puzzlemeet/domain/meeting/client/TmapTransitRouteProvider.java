package com.dnd.puzzlemeet.domain.meeting.client;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TmapTransitRouteProvider implements TransitRouteProvider {

  private final TmapTransitClient tmapTransitClient;

  @Override
  public TransitRouteProviderType type() {
    return TransitRouteProviderType.TMAP;
  }

  @Override
  public boolean supportsDepartureTime() {
    return true;
  }

  @Override
  public List<TravelRoute> findRoutes(
      double startLatitude,
      double startLongitude,
      double endLatitude,
      double endLongitude,
      LocalDateTime departAt) {
    return tmapTransitClient.findTransitRoutes(
        startLatitude, startLongitude, endLatitude, endLongitude, departAt);
  }
}
