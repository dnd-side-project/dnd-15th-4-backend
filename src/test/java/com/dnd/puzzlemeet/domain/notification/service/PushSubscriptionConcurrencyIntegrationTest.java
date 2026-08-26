package com.dnd.puzzlemeet.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.dnd.puzzlemeet.TestcontainersConfiguration;
import com.dnd.puzzlemeet.domain.notification.client.WebPushClient;
import com.dnd.puzzlemeet.domain.notification.client.WebPushSendResult;
import com.dnd.puzzlemeet.domain.notification.dto.PushNotificationPayload;
import com.dnd.puzzlemeet.domain.notification.dto.PushSubscriptionCreateRequest;
import com.dnd.puzzlemeet.domain.notification.entity.NotificationType;
import com.dnd.puzzlemeet.domain.notification.entity.PushSubscription;
import com.dnd.puzzlemeet.domain.notification.repository.PushSubscriptionRepository;
import com.dnd.puzzlemeet.domain.notification.repository.PushSubscriptionTarget;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PushSubscriptionConcurrencyIntegrationTest {

  private static final String ENDPOINT =
      "https://push.services.mozilla.com/wpush/v2/shared-browser";
  private static final String PUBLIC_KEY =
      "BCP0phORUOCEcxm7gu2GCS5Naf7I2fW-o0JRQoJYEj30MSKlqo9WbBRygbxx6mlLciR2loUhdd63WXCeP6aq7IY";
  private static final String FIRST_AUTH = "AAAAAAAAAAAAAAAAAAAAAA";
  private static final String SECOND_AUTH = "AQEBAQEBAQEBAQEBAQEBAQ";

  @Autowired private PushSubscriptionService pushSubscriptionService;
  @Autowired private PushSubscriptionQueryService pushSubscriptionQueryService;
  @Autowired private ExpiredPushSubscriptionService expiredPushSubscriptionService;
  @Autowired private PushNotificationSender pushNotificationSender;
  @Autowired private PushSubscriptionRepository pushSubscriptionRepository;
  @Autowired private UserRepository userRepository;
  @MockitoBean private WebPushClient webPushClient;

  @BeforeEach
  void cleanBeforeTest() {
    deleteTestData();
  }

  @AfterEach
  void cleanAfterTest() {
    deleteTestData();
  }

  @Test
  @DisplayName("두 사용자가 같은 endpoint를 동시에 등록해도 한 행만 남고 소유자와 키가 한 요청 단위로 일치한다")
  void atomicallyTransfersOwnershipDuringConcurrentUpsert() throws Exception {
    User firstUser = saveUser(100L, "첫 사용자");
    User secondUser = saveUser(200L, "두 번째 사용자");
    CountDownLatch readyGate = new CountDownLatch(2);
    CountDownLatch startGate = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<?> first =
          executor.submit(
              () -> upsertAfterGates(firstUser.getId(), FIRST_AUTH, readyGate, startGate));
      Future<?> second =
          executor.submit(
              () -> upsertAfterGates(secondUser.getId(), SECOND_AUTH, readyGate, startGate));

      assertThat(readyGate.await(5, TimeUnit.SECONDS)).isTrue();
      startGate.countDown();
      first.get(10, TimeUnit.SECONDS);
      second.get(10, TimeUnit.SECONDS);
    } finally {
      executor.shutdownNow();
    }

    assertThat(pushSubscriptionRepository.count()).isEqualTo(1L);
    PushSubscription subscription = pushSubscriptionRepository.findAll().getFirst();
    if (subscription.getUser().getId().equals(firstUser.getId())) {
      assertThat(subscription.getAuth()).isEqualTo(FIRST_AUTH);
    } else {
      assertThat(subscription.getUser().getId()).isEqualTo(secondUser.getId());
      assertThat(subscription.getAuth()).isEqualTo(SECOND_AUTH);
    }
  }

  @Test
  @DisplayName("발송 대상 조회 뒤 같은 endpoint가 다른 사용자와 키로 갱신되면 만료 정리가 최신 구독을 보존한다")
  void preservesReRegisteredSubscriptionDuringExpiredCleanup() {
    User firstUser = saveUser(100L, "첫 사용자");
    User secondUser = saveUser(200L, "두 번째 사용자");
    pushSubscriptionService.upsert(firstUser.getId(), request(FIRST_AUTH));
    PushSubscriptionTarget staleTarget =
        pushSubscriptionQueryService.findTargets(List.of(firstUser.getId())).getFirst();

    pushSubscriptionService.upsert(secondUser.getId(), request(SECOND_AUTH));
    boolean deleted = expiredPushSubscriptionService.deleteIfUnchanged(staleTarget);

    assertThat(deleted).isFalse();
    PushSubscription latest = pushSubscriptionRepository.findAll().getFirst();
    assertThat(latest.getUser().getId()).isEqualTo(secondUser.getId());
    assertThat(latest.getAuth()).isEqualTo(SECOND_AUTH);
  }

  @Test
  @DisplayName("구독 조회 트랜잭션이 끝난 뒤 WebPush 외부 호출을 시작한다")
  void callsWebPushOutsideDatabaseTransaction() {
    User user = saveUser(100L, "효창");
    pushSubscriptionService.upsert(user.getId(), request(FIRST_AUTH));
    AtomicBoolean transactionActiveDuringSend = new AtomicBoolean(true);
    given(webPushClient.send(any(), any()))
        .willAnswer(
            invocation -> {
              transactionActiveDuringSend.set(
                  TransactionSynchronizationManager.isActualTransactionActive());
              return WebPushSendResult.success(201);
            });

    pushNotificationSender.send(
        List.of(user.getId()),
        new PushNotificationPayload(
            NotificationType.FRIEND_ARRIVAL, "PuzzleMeet", "친구가 약속 장소에 도착했어요.", 1L));

    assertThat(transactionActiveDuringSend).isFalse();
  }

  private void upsertAfterGates(
      Long userId, String auth, CountDownLatch readyGate, CountDownLatch startGate) {
    try {
      readyGate.countDown();
      startGate.await();
      pushSubscriptionService.upsert(userId, request(auth));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  private User saveUser(Long kakaoId, String nickname) {
    return userRepository.save(new User(kakaoId, nickname, "https://img.example/" + kakaoId));
  }

  private PushSubscriptionCreateRequest request(String auth) {
    return new PushSubscriptionCreateRequest(
        ENDPOINT, new PushSubscriptionCreateRequest.Keys(PUBLIC_KEY, auth));
  }

  private void deleteTestData() {
    pushSubscriptionRepository.deleteAll();
    userRepository.deleteAll();
  }
}
