package com.dnd.puzzlemeet.domain.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dnd.puzzlemeet.TestcontainersConfiguration;
import com.dnd.puzzlemeet.domain.notification.entity.PushSubscription;
import com.dnd.puzzlemeet.domain.notification.repository.PushSubscriptionRepository;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.security.service.JwtProvider;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationControllerTest {

  private static final String SUBSCRIPTIONS_ENDPOINT = "/api/v1/notifications/push-subscriptions";
  private static final String PUSH_ENDPOINT =
      "https://push.services.mozilla.com/wpush/v2/browser-token";
  private static final String PUBLIC_KEY =
      "BCP0phORUOCEcxm7gu2GCS5Naf7I2fW-o0JRQoJYEj30MSKlqo9WbBRygbxx6mlLciR2loUhdd63WXCeP6aq7IY";
  private static final String SECOND_PUBLIC_KEY =
      "BN5gpwJsUb46nXaxn2OpGVO_f2t70tLLZbqQLSOicx2ZjF3Lx_jBnpsIzh8zRyKsRhiLtxeohtPJGMrf6odSuDY";
  private static final String FIRST_AUTH = "AAAAAAAAAAAAAAAAAAAAAA";
  private static final String SECOND_AUTH = "AQEBAQEBAQEBAQEBAQEBAQ";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private PushSubscriptionRepository pushSubscriptionRepository;
  @Autowired private JwtProvider jwtProvider;
  @Autowired private EntityManager entityManager;

  @Test
  @DisplayName("인증하지 않은 사용자의 VAPID 공개키와 푸시 구독 요청은 거절된다")
  void rejectsUnauthenticatedRequests() throws Exception {
    mockMvc
        .perform(get("/api/v1/notifications/vapid-public-key"))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post(SUBSCRIPTIONS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(PUSH_ENDPOINT, FIRST_AUTH)))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            delete(SUBSCRIPTIONS_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"endpoint\":\"" + PUSH_ENDPOINT + "\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("인증 사용자는 WebPush 구독에 사용할 VAPID 공개키를 조회한다")
  void returnsVapidPublicKey() throws Exception {
    User user = saveUser(100L, "효창");

    mockMvc
        .perform(
            get("/api/v1/notifications/vapid-public-key")
                .header("Authorization", bearerToken(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.publicKey").value(PUBLIC_KEY));
  }

  @Test
  @DisplayName("같은 브라우저 구독을 다른 계정이 등록하면 하나의 행을 유지하며 현재 사용자와 키로 이전된다")
  void transfersSubscriptionOwnershipAndKeys() throws Exception {
    User firstUser = saveUser(100L, "첫 사용자");
    User secondUser = saveUser(200L, "두 번째 사용자");
    upsert(firstUser, PUSH_ENDPOINT, FIRST_AUTH);

    upsert(secondUser, PUSH_ENDPOINT, SECOND_AUTH);

    List<PushSubscription> subscriptions = pushSubscriptionRepository.findAll();
    assertThat(subscriptions).hasSize(1);
    PushSubscription subscription = subscriptions.getFirst();
    assertThat(subscription.getUser().getId()).isEqualTo(secondUser.getId());
    assertThat(subscription.getEndpoint()).isEqualTo(PUSH_ENDPOINT);
    assertThat(subscription.getEndpointHash()).hasSize(64);
    assertThat(subscription.getP256dh()).isEqualTo(PUBLIC_KEY);
    assertThat(subscription.getAuth()).isEqualTo(SECOND_AUTH);
    assertThat(subscription.getRegisteredAt()).isNotNull();
    assertThat(subscription.getRefreshedAt()).isNotNull();
  }

  @Test
  @DisplayName("같은 사용자가 endpoint를 재등록하면 등록 시각과 행은 유지하고 브라우저 키만 갱신한다")
  void refreshesExistingSubscriptionForSameUser() throws Exception {
    User user = saveUser(100L, "효창");
    upsert(user, PUSH_ENDPOINT, PUBLIC_KEY, FIRST_AUTH);
    PushSubscription first = pushSubscriptionRepository.findAll().getFirst();
    Long subscriptionId = first.getId();
    LocalDateTime registeredAt = first.getRegisteredAt();

    upsert(user, PUSH_ENDPOINT, SECOND_PUBLIC_KEY, SECOND_AUTH);
    entityManager.clear();

    assertThat(pushSubscriptionRepository.count()).isEqualTo(1L);
    PushSubscription refreshed = pushSubscriptionRepository.findAll().getFirst();
    assertThat(refreshed.getId()).isEqualTo(subscriptionId);
    assertThat(refreshed.getRegisteredAt()).isEqualTo(registeredAt);
    assertThat(refreshed.getP256dh()).isEqualTo(SECOND_PUBLIC_KEY);
    assertThat(refreshed.getAuth()).isEqualTo(SECOND_AUTH);
    assertThat(refreshed.getRefreshedAt()).isAfterOrEqualTo(registeredAt);
  }

  @Test
  @DisplayName("푸시 구독 해지는 반복 호출해도 성공하고 다른 사용자의 같은 endpoint는 삭제하지 않는다")
  void deletesSubscriptionIdempotentlyWithinOwnerScope() throws Exception {
    User owner = saveUser(100L, "소유자");
    User otherUser = saveUser(200L, "다른 사용자");
    upsert(owner, PUSH_ENDPOINT, FIRST_AUTH);

    deleteSubscription(otherUser, PUSH_ENDPOINT);
    assertThat(pushSubscriptionRepository.count()).isEqualTo(1L);

    deleteSubscription(owner, PUSH_ENDPOINT);
    deleteSubscription(owner, PUSH_ENDPOINT);
    assertThat(pushSubscriptionRepository.count()).isZero();
  }

  @Test
  @DisplayName("만료 구독 정리는 발송 당시 사용자와 키가 대소문자까지 정확히 일치할 때만 삭제한다")
  void deletesExpiredSubscriptionOnlyWhenKeysMatchExactly() throws Exception {
    User user = saveUser(100L, "효창");
    upsert(user, PUSH_ENDPOINT, FIRST_AUTH);
    PushSubscription subscription = pushSubscriptionRepository.findAll().getFirst();

    int staleDelete =
        pushSubscriptionRepository.deleteExpiredIfUnchanged(
            subscription.getId(), user.getId(), subscription.getP256dh(), FIRST_AUTH.toLowerCase());
    assertThat(staleDelete).isZero();
    assertThat(pushSubscriptionRepository.count()).isEqualTo(1L);

    int exactDelete =
        pushSubscriptionRepository.deleteExpiredIfUnchanged(
            subscription.getId(), user.getId(), subscription.getP256dh(), FIRST_AUTH);
    assertThat(exactDelete).isEqualTo(1);
    assertThat(pushSubscriptionRepository.count()).isZero();
  }

  @Test
  @DisplayName("내부망 endpoint와 잘못된 브라우저 키는 푸시 구독 등록에서 거절된다")
  void rejectsUnsafeSubscriptionInput() throws Exception {
    User user = saveUser(100L, "효창");

    mockMvc
        .perform(
            post(SUBSCRIPTIONS_ENDPOINT)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest("https://127.0.0.1/internal", FIRST_AUTH)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    mockMvc
        .perform(
            post(SUBSCRIPTIONS_ENDPOINT)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"endpoint":"%s","keys":{"p256dh":"bad","auth":"%s"}}
                    """
                        .formatted(PUSH_ENDPOINT, FIRST_AUTH)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
  }

  @Test
  @DisplayName("잘못된 endpoint로 구독 해지를 요청하면 입력값 검증 오류를 반환한다")
  void rejectsUnsafeDeleteEndpoint() throws Exception {
    User user = saveUser(100L, "효창");

    mockMvc
        .perform(
            delete(SUBSCRIPTIONS_ENDPOINT)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"endpoint\":\"http://localhost/internal\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
  }

  private User saveUser(Long kakaoId, String nickname) {
    return userRepository.save(
        new User(kakaoId, nickname, "https://img.kakao.com/" + kakaoId + ".png"));
  }

  private void upsert(User user, String endpoint, String auth) throws Exception {
    upsert(user, endpoint, PUBLIC_KEY, auth);
  }

  private void upsert(User user, String endpoint, String p256dh, String auth) throws Exception {
    mockMvc
        .perform(
            post(SUBSCRIPTIONS_ENDPOINT)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequest(endpoint, p256dh, auth)))
        .andExpect(status().isOk());
  }

  private void deleteSubscription(User user, String endpoint) throws Exception {
    mockMvc
        .perform(
            delete(SUBSCRIPTIONS_ENDPOINT)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"endpoint\":\"" + endpoint + "\"}"))
        .andExpect(status().isOk());
  }

  private String createRequest(String endpoint, String auth) {
    return createRequest(endpoint, PUBLIC_KEY, auth);
  }

  private String createRequest(String endpoint, String p256dh, String auth) {
    return """
        {"endpoint":"%s","keys":{"p256dh":"%s","auth":"%s"}}
        """
        .formatted(endpoint, p256dh, auth);
  }

  private String bearerToken(User user) {
    return "Bearer " + jwtProvider.createAccessToken(user.getId());
  }
}
