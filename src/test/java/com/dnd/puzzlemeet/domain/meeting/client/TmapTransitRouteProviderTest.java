package com.dnd.puzzlemeet.domain.meeting.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TmapTransitRouteProviderTest {

  private final TmapTransitClient tmapTransitClient = mock(TmapTransitClient.class);
  private final TmapTransitRouteProvider provider = new TmapTransitRouteProvider(tmapTransitClient);

  @Test
  @DisplayName("TMAP 공급자는 출발 시각 지정을 지원한다")
  void supportsDepartureTime() {
    assertThat(provider.type()).isEqualTo(TransitRouteProviderType.TMAP);
    assertThat(provider.supportsDepartureTime()).isTrue();
  }

  @Test
  @DisplayName("TMAP 공급자는 좌표와 출발 시각을 그대로 TMAP 클라이언트에 넘긴다")
  void delegatesCoordinatesAndDepartureTimeToTmapClient() {
    LocalDateTime departAt = LocalDateTime.of(2026, 8, 26, 19, 0);
    List<TravelRoute> expected = List.of(new TravelRoute(2400, 1850, 1, 1, List.of()));
    given(tmapTransitClient.findTransitRoutes(37.5045, 127.0247, 37.4979, 127.0276, departAt))
        .willReturn(expected);

    List<TravelRoute> routes = provider.findRoutes(37.5045, 127.0247, 37.4979, 127.0276, departAt);

    assertThat(routes).isEqualTo(expected);
    verify(tmapTransitClient).findTransitRoutes(37.5045, 127.0247, 37.4979, 127.0276, departAt);
  }
}
