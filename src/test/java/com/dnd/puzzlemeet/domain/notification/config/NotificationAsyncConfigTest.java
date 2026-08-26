package com.dnd.puzzlemeet.domain.notification.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class NotificationAsyncConfigTest {

  @Test
  @DisplayName("알림 executor의 worker와 queue가 모두 차면 새 작업을 예외 없이 폐기한다")
  void dropsTaskWithoutThrowingWhenQueueIsFull() throws Exception {
    Executor configuredExecutor = new NotificationAsyncConfig().notificationExecutor();
    ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) configuredExecutor;
    CountDownLatch releaseGate = new CountDownLatch(1);
    CountDownLatch startedGate = new CountDownLatch(4);
    AtomicBoolean discardedTaskRan = new AtomicBoolean();

    try {
      for (int index = 0; index < 104; index++) {
        executor.execute(
            () -> {
              startedGate.countDown();
              await(releaseGate);
            });
      }

      assertThat(startedGate.await(5, TimeUnit.SECONDS)).isTrue();
      assertThat(executor.getThreadPoolExecutor().getActiveCount()).isEqualTo(4);
      assertThat(executor.getThreadPoolExecutor().getQueue().size()).isEqualTo(100);
      assertThatCode(() -> executor.execute(() -> discardedTaskRan.set(true)))
          .doesNotThrowAnyException();
    } finally {
      releaseGate.countDown();
      executor.shutdown();
      executor.getThreadPoolExecutor().awaitTermination(5, TimeUnit.SECONDS);
    }

    assertThat(discardedTaskRan).isFalse();
  }

  private void await(CountDownLatch gate) {
    try {
      gate.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
