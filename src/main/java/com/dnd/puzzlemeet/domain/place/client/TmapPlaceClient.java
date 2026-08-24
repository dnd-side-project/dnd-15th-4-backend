package com.dnd.puzzlemeet.domain.place.client;

import com.dnd.puzzlemeet.domain.place.dto.PlaceNearbyCategory;
import com.dnd.puzzlemeet.global.client.TmapProperties;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class TmapPlaceClient {

  private static final int CONNECT_TIMEOUT_MILLIS = 2_000;
  private static final int READ_TIMEOUT_MILLIS = 5_000;
  private static final String APP_KEY_HEADER = "appKey";
  private static final String API_VERSION = "1";
  private static final String REQUEST_COORD_TYPE = "WGS84GEO";
  private static final String RESPONSE_COORD_TYPE = "WGS84GEO";
  private static final String SORT_BY_ACCURACY = "A";
  private static final String SORT_BY_DISTANCE = "distance";
  private static final String FIND_BY_ID = "id";

  private final RestClient restClient;
  private final String poiSearchUri;
  private final String poiAroundSearchUri;
  private final String appKey;

  @Autowired
  public TmapPlaceClient(TmapProperties tmapProperties) {
    this(tmapProperties, createRestClient());
  }

  TmapPlaceClient(TmapProperties tmapProperties, RestClient restClient) {
    this.restClient = restClient;
    this.poiSearchUri = tmapProperties.poiSearchUri();
    this.poiAroundSearchUri = tmapProperties.poiAroundSearchUri();
    this.appKey = tmapProperties.appKey();
  }

  private static RestClient createRestClient() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
    requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);
    return RestClient.builder().requestFactory(requestFactory).build();
  }

  public TmapPlaceSearchResult searchPlaces(String keyword, int page, int size) {
    long start = System.currentTimeMillis();
    TmapPoiSearchResponse response;
    try {
      response =
          restClient
              .get()
              .uri(searchUri(keyword, page, size))
              .header(APP_KEY_HEADER, appKey)
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .body(TmapPoiSearchResponse.class);
    } catch (HttpStatusCodeException e) {
      log.info(
          "[지도 연동] 장소 검색 실패, status={}, elapsedMs={}",
          e.getStatusCode().value(),
          System.currentTimeMillis() - start);
      throw ApiException.of(ErrorCode.PLACE_SEARCH_UNAVAILABLE);
    } catch (RestClientException e) {
      log.info("[지도 연동] 장소 검색 실패, elapsedMs={}", System.currentTimeMillis() - start);
      throw ApiException.of(ErrorCode.PLACE_SEARCH_UNAVAILABLE);
    }

    long elapsedMs = System.currentTimeMillis() - start;
    if (response == null || response.searchPoiInfo() == null) {
      log.info("[지도 연동] 장소 검색 결과 없음, elapsedMs={}", elapsedMs);
      return new TmapPlaceSearchResult(0, 0, List.of());
    }

    log.info("[지도 연동] 장소 검색 성공, elapsedMs={}", elapsedMs);
    return TmapPlaceSearchResult.from(response);
  }

  public TmapNearbyPlaceSearchResult searchNearbyPlaces(
      double latitude,
      double longitude,
      int radiusKm,
      List<PlaceNearbyCategory> categories,
      int page,
      int size) {
    long start = System.currentTimeMillis();
    TmapPoiSearchResponse response;
    try {
      response =
          restClient
              .get()
              .uri(nearbySearchUri(latitude, longitude, radiusKm, categories, page, size))
              .header(APP_KEY_HEADER, appKey)
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .body(TmapPoiSearchResponse.class);
    } catch (HttpStatusCodeException e) {
      log.info(
          "[지도 연동] 주변 장소 검색 실패, status={}, elapsedMs={}",
          e.getStatusCode().value(),
          System.currentTimeMillis() - start);
      throw ApiException.of(ErrorCode.PLACE_SEARCH_UNAVAILABLE);
    } catch (RestClientException e) {
      log.info("[지도 연동] 주변 장소 검색 실패, elapsedMs={}", System.currentTimeMillis() - start);
      throw ApiException.of(ErrorCode.PLACE_SEARCH_UNAVAILABLE);
    }

    long elapsedMs = System.currentTimeMillis() - start;
    if (response == null || response.searchPoiInfo() == null) {
      log.info("[지도 연동] 주변 장소 검색 결과 없음, elapsedMs={}", elapsedMs);
      return new TmapNearbyPlaceSearchResult(0, 0, List.of());
    }

    log.info("[지도 연동] 주변 장소 검색 성공, elapsedMs={}", elapsedMs);
    return TmapNearbyPlaceSearchResult.from(response, latitude, longitude);
  }

  public TmapPlaceDetailResult getPlaceDetail(String placeId) {
    long start = System.currentTimeMillis();
    TmapPoiDetailResponse response;
    try {
      response =
          restClient
              .get()
              .uri(detailUri(placeId))
              .header(APP_KEY_HEADER, appKey)
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .body(TmapPoiDetailResponse.class);
    } catch (HttpStatusCodeException e) {
      log.info(
          "[지도 연동] 장소 상세 조회 실패, status={}, elapsedMs={}",
          e.getStatusCode().value(),
          System.currentTimeMillis() - start);
      if (e.getStatusCode().value() == 404) {
        throw ApiException.of(ErrorCode.PLACE_NOT_FOUND);
      }
      throw ApiException.of(ErrorCode.PLACE_SEARCH_UNAVAILABLE);
    } catch (RestClientException e) {
      log.info("[지도 연동] 장소 상세 조회 실패, elapsedMs={}", System.currentTimeMillis() - start);
      throw ApiException.of(ErrorCode.PLACE_SEARCH_UNAVAILABLE);
    }

    TmapPlaceDetailResult detail =
        TmapPlaceDetailResult.from(response, placeId)
            .orElseThrow(() -> ApiException.of(ErrorCode.PLACE_NOT_FOUND));
    log.info("[지도 연동] 장소 상세 조회 성공, elapsedMs={}", System.currentTimeMillis() - start);
    return detail;
  }

  private URI searchUri(String keyword, int page, int size) {
    return UriComponentsBuilder.fromUriString(poiSearchUri)
        .queryParam("version", API_VERSION)
        .queryParam("searchKeyword", keyword)
        .queryParam("page", page + 1)
        .queryParam("count", size)
        .queryParam("resCoordType", RESPONSE_COORD_TYPE)
        .queryParam("searchtypCd", SORT_BY_ACCURACY)
        .build()
        .encode()
        .toUri();
  }

  private URI nearbySearchUri(
      double latitude,
      double longitude,
      int radiusKm,
      List<PlaceNearbyCategory> categories,
      int page,
      int size) {
    String providerCategories =
        categories.stream()
            .map(PlaceNearbyCategory::providerValue)
            .distinct()
            .collect(Collectors.joining(";"));
    return UriComponentsBuilder.fromUriString(poiAroundSearchUri)
        .queryParam("version", API_VERSION)
        .queryParam("centerLon", longitude)
        .queryParam("centerLat", latitude)
        .queryParam("radius", radiusKm)
        .queryParam("categories", providerCategories)
        .queryParam("page", page + 1)
        .queryParam("count", size)
        .queryParam("sort", SORT_BY_DISTANCE)
        .queryParam("reqCoordType", REQUEST_COORD_TYPE)
        .queryParam("resCoordType", RESPONSE_COORD_TYPE)
        .build()
        .encode()
        .toUri();
  }

  private URI detailUri(String placeId) {
    return UriComponentsBuilder.fromUriString(poiSearchUri)
        .pathSegment(placeId)
        .queryParam("version", API_VERSION)
        .queryParam("findOption", FIND_BY_ID)
        .queryParam("resCoordType", RESPONSE_COORD_TYPE)
        .build()
        .encode()
        .toUri();
  }
}
