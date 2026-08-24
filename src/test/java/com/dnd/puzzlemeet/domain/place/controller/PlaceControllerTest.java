package com.dnd.puzzlemeet.domain.place.controller;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dnd.puzzlemeet.TestcontainersConfiguration;
import com.dnd.puzzlemeet.domain.place.client.TmapNearbyPlaceSearchResult;
import com.dnd.puzzlemeet.domain.place.client.TmapPlaceClient;
import com.dnd.puzzlemeet.domain.place.client.TmapPlaceDetailResult;
import com.dnd.puzzlemeet.domain.place.client.TmapPlaceSearchResult;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.security.service.JwtProvider;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PlaceControllerTest {

  private static final long USER_ID = 1L;

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtProvider jwtProvider;
  @MockitoBean private TmapPlaceClient tmapPlaceClient;
  @MockitoBean private UserRepository userRepository;

  @Test
  @DisplayName("페이지당 조회 개수가 상한을 넘으면 입력값 검증에 실패한다")
  void searchPlacesRejectsSizeOverLimit() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/places")
                .param("keyword", "강남역")
                .param("size", "151")
                .header("Authorization", "Bearer " + accessToken()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
  }

  @Test
  @DisplayName("검색 키워드를 넣지 않으면 필수 요청 파라미터 누락으로 응답한다")
  void searchPlacesRequiresKeyword() throws Exception {
    mockMvc
        .perform(get("/api/v1/places").header("Authorization", "Bearer " + accessToken()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MISSING_REQUEST_PARAMETER"));
  }

  @Test
  @DisplayName("키워드로 검색한 장소 목록과 페이지 정보가 반환된다")
  void searchPlacesReturnsPlacesWithPageInfo() throws Exception {
    given(tmapPlaceClient.searchPlaces(anyString(), anyInt(), anyInt()))
        .willReturn(
            new TmapPlaceSearchResult(
                127,
                1,
                List.of(
                    new TmapPlaceSearchResult.Place(
                        "26338954",
                        "카카오프렌즈 코엑스점",
                        "서울 강남구 삼성동 159",
                        "서울 강남구 영동대로 513",
                        37.51207412593136,
                        127.05902969025047))));

    mockMvc
        .perform(
            get("/api/v1/places")
                .param("keyword", "카카오프렌즈")
                .param("size", "1")
                .header("Authorization", "Bearer " + accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.places[0].placeId").value("26338954"))
        .andExpect(jsonPath("$.data.places[0].placeName").value("카카오프렌즈 코엑스점"))
        .andExpect(jsonPath("$.data.places[0].addressName").value("서울 강남구 삼성동 159"))
        .andExpect(jsonPath("$.data.places[0].roadAddressName").value("서울 강남구 영동대로 513"))
        .andExpect(jsonPath("$.data.places[0].latitude").value(37.51207412593136))
        .andExpect(jsonPath("$.data.places[0].longitude").value(127.05902969025047))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(1))
        .andExpect(jsonPath("$.data.hasNext").value(true))
        .andExpect(jsonPath("$.data.totalCount").value(127));
  }

  @Test
  @DisplayName("좌표가 없어 빠진 장소가 있어도 다음 페이지 존재 여부는 유지된다")
  void hasNextIgnoresPlacesDroppedForMissingCoordinates() throws Exception {
    given(tmapPlaceClient.searchPlaces(anyString(), anyInt(), anyInt()))
        .willReturn(
            new TmapPlaceSearchResult(
                127,
                2,
                List.of(
                    new TmapPlaceSearchResult.Place(
                        "26338954",
                        "카카오프렌즈 코엑스점",
                        "서울 강남구 삼성동 159",
                        null,
                        37.512074,
                        127.059029))));

    mockMvc
        .perform(
            get("/api/v1/places")
                .param("keyword", "카카오프렌즈")
                .param("size", "2")
                .header("Authorization", "Bearer " + accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.places.length()").value(1))
        .andExpect(jsonPath("$.data.hasNext").value(true));
  }

  @Test
  @DisplayName("검색 결과가 없으면 빈 목록으로 응답한다")
  void searchPlacesReturnsEmptyListWhenNoResult() throws Exception {
    given(tmapPlaceClient.searchPlaces(anyString(), anyInt(), anyInt()))
        .willReturn(new TmapPlaceSearchResult(0, 0, List.of()));

    mockMvc
        .perform(
            get("/api/v1/places")
                .param("keyword", "없는장소")
                .header("Authorization", "Bearer " + accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.places").isEmpty())
        .andExpect(jsonPath("$.data.hasNext").value(false))
        .andExpect(jsonPath("$.data.totalCount").value(0));
  }

  @Test
  @DisplayName("현재 위치 주변의 장소 목록과 거리 정보가 반환된다")
  void searchNearbyPlacesReturnsPlacesWithDistance() throws Exception {
    given(
            tmapPlaceClient.searchNearbyPlaces(
                anyDouble(), anyDouble(), anyInt(), anyList(), anyInt(), anyInt()))
        .willReturn(
            new TmapNearbyPlaceSearchResult(
                48,
                1,
                List.of(
                    new TmapNearbyPlaceSearchResult.Place(
                        "26338954",
                        "또봉이통닭 사당역점",
                        "서울 동작구 사당동 1031-29",
                        "서울 동작구 동작대로7길 12",
                        37.4767,
                        126.9819,
                        85))));

    mockMvc
        .perform(
            post("/api/v1/places/nearby")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "latitude": 37.4765,
                      "longitude": 126.9816,
                      "radiusKm": 1,
                      "categories": ["RESTAURANT", "CAFE", "TRANSIT"],
                      "page": 0,
                      "size": 20
                    }
                    """)
                .header("Authorization", "Bearer " + accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.places[0].placeId").value("26338954"))
        .andExpect(jsonPath("$.data.places[0].placeName").value("또봉이통닭 사당역점"))
        .andExpect(jsonPath("$.data.places[0].distanceMeters").value(85))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.hasNext").value(false))
        .andExpect(jsonPath("$.data.totalCount").value(48));

    then(tmapPlaceClient).should(never()).getPlaceDetail(anyString());
  }

  @Test
  @DisplayName("주변 검색 요청의 현재 위치가 범위를 벗어나면 입력값 검증에 실패한다")
  void searchNearbyPlacesRejectsInvalidCoordinates() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/places/nearby")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\": 91, \"longitude\": 126.9816}")
                .header("Authorization", "Bearer " + accessToken()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
  }

  @Test
  @DisplayName("주변 검색 요청 본문이 없으면 본문 파싱 실패로 응답한다")
  void searchNearbyPlacesRequiresBody() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/places/nearby")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"));
  }

  @Test
  @DisplayName("인증하지 않은 사용자는 주변 장소를 검색할 수 없다")
  void searchNearbyPlacesRequiresAuthentication() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/places/nearby")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"latitude\": 37.4765, \"longitude\": 126.9816}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
  }

  @Test
  @DisplayName("선택한 장소의 업종과 운영 상세 정보가 반환된다")
  void getPlaceDetailReturnsDisplayInformation() throws Exception {
    given(tmapPlaceClient.getPlaceDetail("26338954"))
        .willReturn(
            new TmapPlaceDetailResult(
                "26338954",
                "또봉이통닭 사당역점",
                "한식",
                "서울 동작구 사당동 1031-29",
                "서울 동작구 동작대로7길 12",
                37.4767,
                126.9819,
                "02-123-4567",
                "매일 15:00~24:00",
                false,
                true,
                false,
                null));

    mockMvc
        .perform(get("/api/v1/places/26338954").header("Authorization", "Bearer " + accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.placeId").value("26338954"))
        .andExpect(jsonPath("$.data.placeName").value("또봉이통닭 사당역점"))
        .andExpect(jsonPath("$.data.categoryName").value("한식"))
        .andExpect(jsonPath("$.data.businessHoursText").value("매일 15:00~24:00"))
        .andExpect(jsonPath("$.data.open24HoursOnWeekdays").value(false))
        .andExpect(jsonPath("$.data.openYearRound").value(true))
        .andExpect(jsonPath("$.data.parkingAvailable").value(false));
  }

  private String accessToken() {
    given(userRepository.existsByIdAndDeletedAtIsNull(USER_ID)).willReturn(true);
    return jwtProvider.createAccessToken(USER_ID);
  }
}
