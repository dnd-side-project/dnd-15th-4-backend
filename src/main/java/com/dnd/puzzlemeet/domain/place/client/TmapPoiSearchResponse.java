package com.dnd.puzzlemeet.domain.place.client;

import java.util.List;

public record TmapPoiSearchResponse(SearchPoiInfo searchPoiInfo) {

  public record SearchPoiInfo(String totalCount, String count, String page, Pois pois) {}

  public record Pois(List<Poi> poi) {}

  public record Poi(
      String id,
      String name,
      String frontLat,
      String frontLon,
      String noorLat,
      String noorLon,
      String radius,
      String upperAddrName,
      String middleAddrName,
      String lowerAddrName,
      String firstNo,
      String secondNo,
      String roadName,
      String buildingNo1,
      String buildingNo2,
      NewAddressList newAddressList) {}

  public record NewAddressList(List<NewAddress> newAddress) {}

  public record NewAddress(String fullAddressRoad) {}
}
