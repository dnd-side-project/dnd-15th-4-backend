package com.dnd.puzzlemeet.domain.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dnd.puzzlemeet.domain.meeting.client.TransitRouteProviderType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransitRouteSelectionPolicyTest {

  private final TransitRouteSelectionPolicy policy = new TransitRouteSelectionPolicy();

  @Test
  @DisplayName("현재 정책은 약속 시각과 무관하게 언제나 Kakao 공급자를 고른다")
  void alwaysSelectsKakao() {
    TransitRouteQuery future =
        new TransitRouteQuery(
            37.5045, 127.0247, 37.4979, 127.0276, LocalDateTime.now().plusDays(1));
    TransitRouteQuery past =
        new TransitRouteQuery(
            37.5045, 127.0247, 37.4979, 127.0276, LocalDateTime.now().minusDays(1));

    assertThat(policy.select(future)).isEqualTo(TransitRouteProviderType.KAKAO);
    assertThat(policy.select(past)).isEqualTo(TransitRouteProviderType.KAKAO);
  }
}
