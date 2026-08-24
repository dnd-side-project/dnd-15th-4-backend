package com.dnd.puzzlemeet.domain.place.dto;

public enum PlaceNearbyCategory {
  RESTAURANT("음식점"),
  CAFE("카페"),
  TRANSIT("교통");

  private final String providerValue;

  PlaceNearbyCategory(String providerValue) {
    this.providerValue = providerValue;
  }

  public String providerValue() {
    return providerValue;
  }
}
