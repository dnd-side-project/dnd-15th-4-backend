package com.dnd.puzzlemeet.domain.place.client;

import com.dnd.puzzlemeet.global.client.TmapProperties;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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
  private static final String RESPONSE_COORD_TYPE = "WGS84GEO";
  private static final String SORT_BY_ACCURACY = "A";

  private final RestClient restClient;
  private final String poiSearchUri;
  private final String appKey;

  public TmapPlaceClient(TmapProperties tmapProperties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
    requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);
    this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    this.poiSearchUri = tmapProperties.poiSearchUri();
    this.appKey = tmapProperties.appKey();
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
}
