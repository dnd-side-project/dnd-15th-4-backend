package com.dnd.puzzlemeet.domain.notification.scheduler;

import com.dnd.puzzlemeet.domain.notification.service.DepartureReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

  private final DepartureReminderService departureReminderService;

  @Scheduled(cron = "0 * * * * *")
  public void sendDepartureReminders() {
    int claimed = departureReminderService.claimDepartureReminders();
    if (claimed > 0) {
      log.info("[출발 리마인더] 발송 시도 선점 완료, targets={}", claimed);
    }
  }
}
