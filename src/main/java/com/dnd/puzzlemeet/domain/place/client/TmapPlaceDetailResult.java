package com.dnd.puzzlemeet.domain.place.client;

import java.util.Optional;

public record TmapPlaceDetailResult(
    String placeId,
    String placeName,
    String categoryName,
    String addressName,
    String roadAddressName,
    Double latitude,
    Double longitude,
    String phoneNumber,
    String businessHoursText,
    Boolean open24HoursOnWeekdays,
    Boolean openYearRound,
    Boolean parkingAvailable,
    String homepageUrl) {

  private static final String EMPTY_SUB_NUMBER = "0";

  public static Optional<TmapPlaceDetailResult> from(
      TmapPoiDetailResponse response, String requestedPlaceId) {
    if (response == null || response.poiDetailInfo() == null) {
      return Optional.empty();
    }

    TmapPoiDetailResponse.PoiDetailInfo detail = response.poiDetailInfo();
    String placeName = normalize(detail.name());
    if (placeName == null) {
      return Optional.empty();
    }

    Coordinates coordinates = coordinates(detail);
    return Optional.of(
        new TmapPlaceDetailResult(
            firstNonBlank(detail.id(), requestedPlaceId),
            placeName,
            normalize(detail.bizCatName()),
            joinNonBlank(normalize(detail.address()), number(detail.firstNo(), detail.secondNo())),
            joinNonBlank(normalize(detail.bldAddr()), number(detail.bldNo1(), detail.bldNo2())),
            coordinates.latitude(),
            coordinates.longitude(),
            normalize(detail.tel()),
            normalize(detail.useTime()),
            parseFlag(detail.twFlag()),
            parseFlag(detail.yaFlag()),
            parseFlag(detail.parkFlag()),
            normalize(detail.homepageURL())));
  }

  private static Coordinates coordinates(TmapPoiDetailResponse.PoiDetailInfo detail) {
    Double frontLatitude = parseCoordinate(firstNonBlank(detail.frontlat(), detail.frontLat()));
    Double frontLongitude = parseCoordinate(firstNonBlank(detail.frontlon(), detail.frontLon()));
    if (frontLatitude != null && frontLongitude != null) {
      return new Coordinates(frontLatitude, frontLongitude);
    }
    return new Coordinates(parseCoordinate(detail.lat()), parseCoordinate(detail.lon()));
  }

  private static String number(String mainNumber, String subNumber) {
    String normalizedMainNumber = normalize(mainNumber);
    if (normalizedMainNumber == null) {
      return null;
    }
    String normalizedSubNumber = normalize(subNumber);
    if (normalizedSubNumber == null || EMPTY_SUB_NUMBER.equals(normalizedSubNumber)) {
      return normalizedMainNumber;
    }
    return normalizedMainNumber + "-" + normalizedSubNumber;
  }

  private static Boolean parseFlag(String value) {
    String normalized = normalize(value);
    if ("1".equals(normalized)) {
      return true;
    }
    if ("0".equals(normalized)) {
      return false;
    }
    return null;
  }

  private static Double parseCoordinate(String value) {
    String normalized = normalize(value);
    if (normalized == null) {
      return null;
    }
    try {
      double coordinate = Double.parseDouble(normalized);
      return Double.isFinite(coordinate) ? coordinate : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static String firstNonBlank(String first, String second) {
    String normalizedFirst = normalize(first);
    return normalizedFirst != null ? normalizedFirst : normalize(second);
  }

  private static String joinNonBlank(String... parts) {
    StringBuilder joined = new StringBuilder();
    for (String part : parts) {
      if (part == null) {
        continue;
      }
      if (!joined.isEmpty()) {
        joined.append(' ');
      }
      joined.append(part);
    }
    return joined.isEmpty() ? null : joined.toString();
  }

  private static String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private record Coordinates(Double latitude, Double longitude) {}
}
