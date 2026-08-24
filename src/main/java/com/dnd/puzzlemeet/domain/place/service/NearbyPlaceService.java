package com.dnd.puzzlemeet.domain.place.service;

import com.dnd.puzzlemeet.domain.place.client.TmapPlaceClient;
import com.dnd.puzzlemeet.domain.place.dto.PlaceNearbyCategory;
import com.dnd.puzzlemeet.domain.place.dto.PlaceNearbySearchResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NearbyPlaceService {

  private final TmapPlaceClient tmapPlaceClient;

  public PlaceNearbySearchResponse searchNearbyPlaces(
      double latitude,
      double longitude,
      int radiusKm,
      List<PlaceNearbyCategory> categories,
      int page,
      int size) {
    return PlaceNearbySearchResponse.of(
        tmapPlaceClient.searchNearbyPlaces(latitude, longitude, radiusKm, categories, page, size),
        page,
        size);
  }
}
