package com.dnd.puzzlemeet.domain.meeting.service;

import com.dnd.puzzlemeet.domain.meeting.client.TransitRouteProvider;
import com.dnd.puzzlemeet.domain.meeting.client.TransitRouteProviderType;
import com.dnd.puzzlemeet.domain.meeting.client.TravelRoute;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TransitRouteFacade {

  private static final int ROUTE_RESEARCH_THRESHOLD_SECONDS = 3_600;

  private final TransitRouteSelectionPolicy selectionPolicy;
  private final Map<TransitRouteProviderType, TransitRouteProvider> providers;

  public TransitRouteFacade(
      TransitRouteSelectionPolicy selectionPolicy, List<TransitRouteProvider> providers) {
    this.selectionPolicy = selectionPolicy;
    this.providers =
        providers.stream()
            .collect(Collectors.toMap(TransitRouteProvider::type, Function.identity()));
  }

  public List<TravelRoute> findRoutes(TransitRouteQuery query) {
    TransitRouteProvider provider = provider(selectionPolicy.select(query));
    if (!provider.supportsDepartureTime()) {
      return findRoutes(provider, query, null);
    }

    LocalDateTime firstDepartAt = firstQueryDepartAt(query);
    List<TravelRoute> routes = findRoutes(provider, query, firstDepartAt);

    if (routes.isEmpty()) {
      return routes;
    }

    int estimatedTimeSeconds = routes.getFirst().totalTimeSeconds();
    if (!needsReQuery(firstDepartAt, estimatedTimeSeconds)) {
      return routes;
    }

    List<TravelRoute> reQueried =
        findRoutes(provider, query, reQueryDepartAt(query, estimatedTimeSeconds));
    return reQueried.isEmpty() ? routes : reQueried;
  }

  private TransitRouteProvider provider(TransitRouteProviderType type) {
    TransitRouteProvider provider = providers.get(type);
    if (provider == null) {
      throw ApiException.of(ErrorCode.MEETING_MAP_UNAVAILABLE);
    }
    return provider;
  }

  private List<TravelRoute> findRoutes(
      TransitRouteProvider provider, TransitRouteQuery query, LocalDateTime departAt) {
    return provider.findRoutes(
        query.startLatitude(),
        query.startLongitude(),
        query.endLatitude(),
        query.endLongitude(),
        departAt);
  }

  private LocalDateTime firstQueryDepartAt(TransitRouteQuery query) {
    LocalDateTime meetingAt = query.meetingAt();
    return meetingAt != null && meetingAt.isAfter(LocalDateTime.now()) ? meetingAt : null;
  }

  private boolean needsReQuery(LocalDateTime firstDepartAt, int estimatedTimeSeconds) {
    return firstDepartAt != null && estimatedTimeSeconds >= ROUTE_RESEARCH_THRESHOLD_SECONDS;
  }

  private LocalDateTime reQueryDepartAt(TransitRouteQuery query, int estimatedTimeSeconds) {
    LocalDateTime departAt = query.meetingAt().minusSeconds(estimatedTimeSeconds);
    return departAt.isAfter(LocalDateTime.now()) ? departAt : null;
  }
}
