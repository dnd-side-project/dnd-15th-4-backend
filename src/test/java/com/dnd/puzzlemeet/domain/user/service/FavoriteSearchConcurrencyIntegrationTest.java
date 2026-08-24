package com.dnd.puzzlemeet.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dnd.puzzlemeet.TestcontainersConfiguration;
import com.dnd.puzzlemeet.domain.user.dto.FavoriteSearchCreateRequest;
import com.dnd.puzzlemeet.domain.user.entity.User;
import com.dnd.puzzlemeet.domain.user.repository.FavoriteSearchRepository;
import com.dnd.puzzlemeet.domain.user.repository.UserRepository;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FavoriteSearchConcurrencyIntegrationTest {

  @Autowired private FavoriteSearchService favoriteSearchService;
  @Autowired private FavoriteSearchRepository favoriteSearchRepository;
  @Autowired private UserRepository userRepository;

  @BeforeEach
  void cleanBeforeTest() {
    deleteTestData();
  }

  @AfterEach
  void cleanAfterTest() {
    deleteTestData();
  }

  @Test
  @DisplayName("즐겨찾기가 4개인 사용자에게 동시 등록 요청이 와도 최종 개수는 5개를 넘지 않는다")
  void keepsMaximumCountUnderConcurrentCreation() throws Exception {
    User user = userRepository.save(new User(100L, "효창", "https://img.kakao.com/profile.png"));
    for (int index = 1; index <= 4; index++) {
      favoriteSearchService.createFavoriteSearch(
          user.getId(), new FavoriteSearchCreateRequest("기존 장소 " + index));
    }

    CountDownLatch readyGate = new CountDownLatch(2);
    CountDownLatch startGate = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<ErrorCode> first =
          executor.submit(() -> createAfterGates(user.getId(), "동시 장소 A", readyGate, startGate));
      Future<ErrorCode> second =
          executor.submit(() -> createAfterGates(user.getId(), "동시 장소 B", readyGate, startGate));

      assertThat(readyGate.await(5, TimeUnit.SECONDS)).isTrue();
      startGate.countDown();
      ErrorCode firstResult = first.get(10, TimeUnit.SECONDS);
      ErrorCode secondResult = second.get(10, TimeUnit.SECONDS);

      assertThat(Stream.of(firstResult, secondResult).filter(Objects::isNull).count())
          .isEqualTo(1L);
      assertThat(Stream.of(firstResult, secondResult).filter(Objects::nonNull).toList())
          .containsExactly(ErrorCode.FAVORITE_SEARCH_LIMIT_EXCEEDED);
      assertThat(favoriteSearchRepository.countByUserId(user.getId())).isEqualTo(5L);
    } finally {
      executor.shutdownNow();
    }
  }

  private ErrorCode createAfterGates(
      Long userId, String keyword, CountDownLatch readyGate, CountDownLatch startGate)
      throws InterruptedException {
    readyGate.countDown();
    startGate.await();
    try {
      favoriteSearchService.createFavoriteSearch(userId, new FavoriteSearchCreateRequest(keyword));
      return null;
    } catch (ApiException e) {
      return e.getErrorCode();
    }
  }

  private void deleteTestData() {
    favoriteSearchRepository.deleteAll();
    userRepository.deleteAll();
  }
}
