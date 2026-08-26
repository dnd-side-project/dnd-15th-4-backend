package com.dnd.puzzlemeet.domain.notification.config;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@EnableAsync
@Configuration(proxyBeanMethods = false)
public class NotificationAsyncConfig {

  private static final int CORE_POOL_SIZE = 2;
  private static final int MAX_POOL_SIZE = 4;
  private static final int QUEUE_CAPACITY = 100;
  private static final int AWAIT_TERMINATION_SECONDS = 10;

  @Bean(name = "notificationExecutor")
  @DependsOn("webPushHttpClient")
  Executor notificationExecutor() {
    AtomicLong discardedTotal = new AtomicLong();
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(CORE_POOL_SIZE);
    executor.setMaxPoolSize(MAX_POOL_SIZE);
    executor.setQueueCapacity(QUEUE_CAPACITY);
    executor.setThreadNamePrefix("notification-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS);
    executor.setRejectedExecutionHandler(
        (task, threadPool) ->
            log.warn(
                "[알림 작업 폐기] active={}, queued={}, discardedTotal={}",
                threadPool.getActiveCount(),
                threadPool.getQueue().size(),
                discardedTotal.incrementAndGet()));
    executor.initialize();
    return executor;
  }
}
