package com.dnd.puzzlemeet.domain.place.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dnd.puzzlemeet.TestcontainersConfiguration;
import com.dnd.puzzlemeet.domain.place.client.TmapPlaceClient;
import com.dnd.puzzlemeet.domain.place.client.TmapPlaceSearchResult;
import com.dnd.puzzlemeet.global.security.service.JwtProvider;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
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

  @Test
  @DisplayName("페이지당 조회 개수가 상한을 넘으면 입력값 검증에 실패한다")
  void searchPlacesRejectsSizeOverLimit() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/places")
                .param("keyword", "강남역")
                .param("size", "201")
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

  private String accessToken() {
    return jwtProvider.createAccessToken(USER_ID);
  }
}
