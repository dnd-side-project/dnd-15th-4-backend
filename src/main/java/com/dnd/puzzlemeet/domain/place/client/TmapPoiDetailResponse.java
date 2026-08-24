package com.dnd.puzzlemeet.domain.place.client;

public record TmapPoiDetailResponse(PoiDetailInfo poiDetailInfo) {

  public record PoiDetailInfo(
      String id,
      String name,
      String bizCatName,
      String address,
      String firstNo,
      String secondNo,
      String bldAddr,
      String bldNo1,
      String bldNo2,
      String lat,
      String lon,
      String frontLat,
      String frontLon,
      String frontlat,
      String frontlon,
      String tel,
      String parkFlag,
      String twFlag,
      String yaFlag,
      String homepageURL,
      String useTime) {}
}
