package com.dnd.puzzlemeet.domain.meeting.service;

import com.dnd.puzzlemeet.domain.meeting.client.TransitRouteProviderType;
import org.springframework.stereotype.Component;

@Component
public class TransitRouteSelectionPolicy {

  public TransitRouteProviderType select(TransitRouteQuery query) {
    return TransitRouteProviderType.KAKAO;
  }
}
