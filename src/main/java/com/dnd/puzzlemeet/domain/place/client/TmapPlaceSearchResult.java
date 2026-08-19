package com.dnd.puzzlemeet.domain.place.client;

import java.util.ArrayList;
import java.util.List;

public record TmapPlaceSearchResult(int totalCount, int fetchedCount, List<Place> places) {

  private static final String EMPTY_SUB_NUMBER = "0";

  public static TmapPlaceSearchResult from(TmapPoiSearchResponse response) {
    if (response == null || response.searchPoiInfo() == null) {
      return new TmapPlaceSearchResult(0, 0, List.of());
    }

    TmapPoiSearchResponse.SearchPoiInfo searchPoiInfo = response.searchPoiInfo();
    List<TmapPoiSearchResponse.Poi> pois =
        searchPoiInfo.pois() != null && searchPoiInfo.pois().poi() != null
            ? searchPoiInfo.pois().poi()
            : List.of();

    List<Place> places = new ArrayList<>(pois.size());
    for (TmapPoiSearchResponse.Poi poi : pois) {
      Place place = toPlace(poi);
      if (place != null) {
        places.add(place);
      }
    }
    return new TmapPlaceSearchResult(parseCount(searchPoiInfo.totalCount()), pois.size(), places);
  }

  private static Place toPlace(TmapPoiSearchResponse.Poi poi) {
    Double latitude = parseCoordinate(poi.frontLat(), poi.noorLat());
    Double longitude = parseCoordinate(poi.frontLon(), poi.noorLon());
    if (latitude == null || longitude == null) {
      return null;
    }
    return new Place(
        poi.id(), poi.name(), addressName(poi), roadAddressName(poi), latitude, longitude);
  }

  private static String addressName(TmapPoiSearchResponse.Poi poi) {
    return joinNonBlank(
        poi.upperAddrName(), poi.middleAddrName(), poi.lowerAddrName(), lotNumber(poi));
  }

  private static String lotNumber(TmapPoiSearchResponse.Poi poi) {
    if (isBlank(poi.firstNo())) {
      return null;
    }
    if (isBlank(poi.secondNo()) || EMPTY_SUB_NUMBER.equals(poi.secondNo())) {
      return poi.firstNo();
    }
    return poi.firstNo() + "-" + poi.secondNo();
  }

  private static String roadAddressName(TmapPoiSearchResponse.Poi poi) {
    if (poi.newAddressList() == null
        || poi.newAddressList().newAddress() == null
        || poi.newAddressList().newAddress().isEmpty()) {
      return null;
    }
    String fullAddressRoad = poi.newAddressList().newAddress().getFirst().fullAddressRoad();
    return isBlank(fullAddressRoad) ? null : fullAddressRoad;
  }

  private static String joinNonBlank(String... parts) {
    StringBuilder joined = new StringBuilder();
    for (String part : parts) {
      if (isBlank(part)) {
        continue;
      }
      if (!joined.isEmpty()) {
        joined.append(' ');
      }
      joined.append(part);
    }
    return joined.isEmpty() ? null : joined.toString();
  }

  private static Double parseCoordinate(String preferred, String fallback) {
    Double parsed = parseCoordinate(preferred);
    return parsed != null ? parsed : parseCoordinate(fallback);
  }

  private static Double parseCoordinate(String value) {
    if (isBlank(value)) {
      return null;
    }
    try {
      return Double.valueOf(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static int parseCount(String value) {
    if (isBlank(value)) {
      return 0;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  public record Place(
      String placeId,
      String placeName,
      String addressName,
      String roadAddressName,
      double latitude,
      double longitude) {}
}
