package com.dnd.puzzlemeet.domain.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.dnd.puzzlemeet.domain.meeting.client.TransitRouteProvider;
import com.dnd.puzzlemeet.domain.meeting.client.TransitRouteProviderType;
import com.dnd.puzzlemeet.domain.meeting.client.TravelRoute;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TransitRouteFacadeTest {

  private static final double START_LATITUDE = 37.5045;
  private static final double START_LONGITUDE = 127.0247;
  private static final double END_LATITUDE = 37.4979;
  private static final double END_LONGITUDE = 127.0276;

  private TransitRouteSelectionPolicy selectionPolicy;
  private TransitRouteProvider kakaoProvider;
  private TransitRouteProvider tmapProvider;
  private TransitRouteFacade facade;

  @BeforeEach
  void setUp() {
    selectionPolicy = mock(TransitRouteSelectionPolicy.class);
    kakaoProvider = mock(TransitRouteProvider.class);
    tmapProvider = mock(TransitRouteProvider.class);
    given(kakaoProvider.type()).willReturn(TransitRouteProviderType.KAKAO);
    given(kakaoProvider.supportsDepartureTime()).willReturn(false);
    given(tmapProvider.type()).willReturn(TransitRouteProviderType.TMAP);
    given(tmapProvider.supportsDepartureTime()).willReturn(true);
    facade = new TransitRouteFacade(selectionPolicy, List.of(kakaoProvider, tmapProvider));
  }

  @Test
  @DisplayName("시간을 지원하지 않는 공급자는 약속이 한참 남았어도 현재 시각 기준으로 한 번만 조회한다")
  void queriesProviderOnceWithoutDepartureTime() {
    givenSelected(TransitRouteProviderType.KAKAO);
    givenRoutes(kakaoProvider, routes(5400));

    List<TravelRoute> routes = facade.findRoutes(query(LocalDateTime.now().plusHours(5)));

    assertThat(routes).hasSize(1);
    verify(kakaoProvider)
        .findRoutes(START_LATITUDE, START_LONGITUDE, END_LATITUDE, END_LONGITUDE, null);
    verify(tmapProvider, never())
        .findRoutes(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any());
  }

  @Test
  @DisplayName("정책이 TMAP을 고르면 Kakao 공급자는 호출되지 않는다")
  void queriesOnlySelectedProvider() {
    givenSelected(TransitRouteProviderType.TMAP);
    givenRoutes(tmapProvider, routes(2400));

    facade.findRoutes(query(LocalDateTime.now().plusHours(3)));

    verify(tmapProvider, times(1))
        .findRoutes(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any());
    verify(kakaoProvider, never())
        .findRoutes(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any());
  }

  @Test
  @DisplayName("시간을 지원하는 공급자는 예상 소요시간이 1시간 이상이면 출발 시각을 역산해 다시 조회한다")
  void searchesAgainWithArrivalBasedDepartureTime() {
    LocalDateTime meetingAt = LocalDateTime.now().plusHours(5);
    givenSelected(TransitRouteProviderType.TMAP);
    givenRoutes(tmapProvider, routes(5400));

    facade.findRoutes(query(meetingAt));

    assertThat(capturedDepartAt(2)).containsExactly(meetingAt, meetingAt.minusSeconds(5400));
  }

  @Test
  @DisplayName("예상 소요시간이 1시간 미만이면 다시 조회하지 않는다")
  void skipsSecondSearchForShortRoute() {
    LocalDateTime meetingAt = LocalDateTime.now().plusHours(3);
    givenSelected(TransitRouteProviderType.TMAP);
    givenRoutes(tmapProvider, routes(2400));

    facade.findRoutes(query(meetingAt));

    assertThat(capturedDepartAt(1)).containsExactly(meetingAt);
  }

  @Test
  @DisplayName("약속 시각이 이미 지났으면 현재 시각 기준으로 한 번만 조회한다")
  void searchesOnceForPastMeeting() {
    givenSelected(TransitRouteProviderType.TMAP);
    givenRoutes(tmapProvider, routes(2400));

    facade.findRoutes(query(LocalDateTime.now().minusHours(1)));

    assertThat(capturedDepartAt(1)).containsOnlyNulls();
  }

  @Test
  @DisplayName("역산한 출발 시각이 이미 지났으면 현재 시각 기준으로 다시 조회한다")
  void searchesAgainWithoutDepartureTimeWhenReQueryTimeHasPassed() {
    LocalDateTime meetingAt = LocalDateTime.now().plusMinutes(30);
    givenSelected(TransitRouteProviderType.TMAP);
    givenRoutes(tmapProvider, routes(5400));

    facade.findRoutes(query(meetingAt));

    assertThat(capturedDepartAt(2)).containsExactly(meetingAt, null);
  }

  @Test
  @DisplayName("1차 조회 결과가 비어 있으면 다시 조회하지 않는다")
  void skipsSecondSearchWhenFirstResultIsEmpty() {
    givenSelected(TransitRouteProviderType.TMAP);
    givenRoutes(tmapProvider, List.of());

    assertThat(facade.findRoutes(query(LocalDateTime.now().plusHours(5)))).isEmpty();
    verify(tmapProvider, times(1))
        .findRoutes(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any());
  }

  @Test
  @DisplayName("재조회 결과가 비어 있으면 1차 조회 결과를 그대로 쓴다")
  void keepsFirstResultWhenReQueryReturnsNothing() {
    LocalDateTime meetingAt = LocalDateTime.now().plusHours(5);
    List<TravelRoute> firstRoutes = routes(5400);
    givenSelected(TransitRouteProviderType.TMAP);
    given(
            tmapProvider.findRoutes(
                anyDouble(), anyDouble(), anyDouble(), anyDouble(), eq(meetingAt)))
        .willReturn(firstRoutes);
    given(
            tmapProvider.findRoutes(
                anyDouble(),
                anyDouble(),
                anyDouble(),
                anyDouble(),
                eq(meetingAt.minusSeconds(5400))))
        .willReturn(List.of());

    assertThat(facade.findRoutes(query(meetingAt))).isEqualTo(firstRoutes);
  }

  private void givenSelected(TransitRouteProviderType type) {
    given(selectionPolicy.select(any())).willReturn(type);
  }

  private void givenRoutes(TransitRouteProvider provider, List<TravelRoute> routes) {
    given(provider.findRoutes(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
        .willReturn(routes);
  }

  private List<LocalDateTime> capturedDepartAt(int expectedCalls) {
    ArgumentCaptor<LocalDateTime> departAt = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(tmapProvider, times(expectedCalls))
        .findRoutes(anyDouble(), anyDouble(), anyDouble(), anyDouble(), departAt.capture());
    return departAt.getAllValues();
  }

  private TransitRouteQuery query(LocalDateTime meetingAt) {
    return new TransitRouteQuery(
        START_LATITUDE, START_LONGITUDE, END_LATITUDE, END_LONGITUDE, meetingAt);
  }

  private List<TravelRoute> routes(int totalTimeSeconds) {
    return List.of(new TravelRoute(totalTimeSeconds, 1850, 1, 1, List.of()));
  }
}
