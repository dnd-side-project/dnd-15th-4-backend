package com.dnd.puzzlemeet.domain.place.service;

import com.dnd.puzzlemeet.domain.place.client.TmapPlaceClient;
import com.dnd.puzzlemeet.domain.place.dto.PlaceDetailResponse;
import com.dnd.puzzlemeet.domain.place.dto.PlaceSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceService {

  private final TmapPlaceClient tmapPlaceClient;

  public PlaceSearchResponse searchPlaces(String keyword, int page, int size) {
    return PlaceSearchResponse.of(tmapPlaceClient.searchPlaces(keyword, page, size), page, size);
  }

  public PlaceDetailResponse getPlaceDetail(String placeId) {
    return PlaceDetailResponse.from(tmapPlaceClient.getPlaceDetail(placeId));
  }
}
