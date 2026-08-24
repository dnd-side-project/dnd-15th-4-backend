package com.dnd.puzzlemeet.domain.place.client;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public record TmapNearbyPlaceSearchResult(int totalCount, int fetchedCount, List<Place> places) {

  private static final String EMPTY_SUB_NUMBER = "0";
  private static final BigDecimal METERS_PER_KILOMETER = BigDecimal.valueOf(1_000);
  private static final double EARTH_RADIUS_METERS = 6_371_000;

  public static TmapNearbyPlaceSearchResult from(
      TmapPoiSearchResponse response, double centerLatitude, double centerLongitude) {
    if (response == null || response.searchPoiInfo() == null) {
      return new TmapNearbyPlaceSearchResult(0, 0, List.of());
    }

    TmapPoiSearchResponse.SearchPoiInfo searchPoiInfo = response.searchPoiInfo();
    List<TmapPoiSearchResponse.Poi> pois =
        searchPoiInfo.pois() != null && searchPoiInfo.pois().poi() != null
            ? searchPoiInfo.pois().poi()
            : List.of();

    List<Place> places = new ArrayList<>(pois.size());
    for (TmapPoiSearchResponse.Poi poi : pois) {
      Place place = toPlace(poi, centerLatitude, centerLongitude);
      if (place != null) {
        places.add(place);
      }
    }
    return new TmapNearbyPlaceSearchResult(
        parseCount(searchPoiInfo.totalCount()), pois.size(), places);
  }

  private static Place toPlace(
      TmapPoiSearchResponse.Poi poi, double centerLatitude, double centerLongitude) {
    Coordinates coordinates = coordinates(poi);
    if (isBlank(poi.id()) || isBlank(poi.name()) || coordinates == null) {
      return null;
    }
    return new Place(
        poi.id(),
        poi.name(),
        addressName(poi),
        roadAddressName(poi),
        coordinates.latitude(),
        coordinates.longitude(),
        distanceMeters(
            poi.radius(),
            centerLatitude,
            centerLongitude,
            coordinates.latitude(),
            coordinates.longitude()));
  }

  private static Coordinates coordinates(TmapPoiSearchResponse.Poi poi) {
    Double frontLatitude = parseCoordinate(poi.frontLat());
    Double frontLongitude = parseCoordinate(poi.frontLon());
    if (frontLatitude != null && frontLongitude != null) {
      return new Coordinates(frontLatitude, frontLongitude);
    }

    Double centerLatitude = parseCoordinate(poi.noorLat());
    Double centerLongitude = parseCoordinate(poi.noorLon());
    return centerLatitude != null && centerLongitude != null
        ? new Coordinates(centerLatitude, centerLongitude)
        : null;
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
    if (poi.newAddressList() != null
        && poi.newAddressList().newAddress() != null
        && !poi.newAddressList().newAddress().isEmpty()) {
      String fullAddressRoad = poi.newAddressList().newAddress().getFirst().fullAddressRoad();
      if (!isBlank(fullAddressRoad)) {
        return fullAddressRoad;
      }
    }
    return joinNonBlank(
        poi.upperAddrName(),
        poi.middleAddrName(),
        poi.roadName(),
        buildingNumber(poi.buildingNo1(), poi.buildingNo2()));
  }

  private static String buildingNumber(String mainNumber, String subNumber) {
    if (isBlank(mainNumber)) {
      return null;
    }
    if (isBlank(subNumber) || EMPTY_SUB_NUMBER.equals(subNumber)) {
      return mainNumber;
    }
    return mainNumber + "-" + subNumber;
  }

  private static int distanceMeters(
      String radius,
      double centerLatitude,
      double centerLongitude,
      double placeLatitude,
      double placeLongitude) {
    if (!isBlank(radius)) {
      try {
        BigDecimal kilometers = new BigDecimal(radius);
        if (kilometers.signum() >= 0) {
          return kilometers
              .multiply(METERS_PER_KILOMETER)
              .setScale(0, RoundingMode.HALF_UP)
              .intValueExact();
        }
      } catch (ArithmeticException | NumberFormatException ignored) {
        // TMAP 거리값을 사용할 수 없으면 좌표로 근사 거리를 계산한다.
      }
    }
    return haversineDistanceMeters(centerLatitude, centerLongitude, placeLatitude, placeLongitude);
  }

  private static int haversineDistanceMeters(
      double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
    double latitudeDifference = Math.toRadians(endLatitude - startLatitude);
    double longitudeDifference = Math.toRadians(endLongitude - startLongitude);
    double startLatitudeRadians = Math.toRadians(startLatitude);
    double endLatitudeRadians = Math.toRadians(endLatitude);
    double haversine =
        Math.pow(Math.sin(latitudeDifference / 2), 2)
            + Math.cos(startLatitudeRadians)
                * Math.cos(endLatitudeRadians)
                * Math.pow(Math.sin(longitudeDifference / 2), 2);
    haversine = Math.clamp(haversine, 0, 1);
    double angularDistance = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    return (int) Math.round(EARTH_RADIUS_METERS * angularDistance);
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

  private static Double parseCoordinate(String value) {
    if (isBlank(value)) {
      return null;
    }
    try {
      double coordinate = Double.parseDouble(value);
      return Double.isFinite(coordinate) ? coordinate : null;
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
      double longitude,
      int distanceMeters) {}

  private record Coordinates(double latitude, double longitude) {}
}
