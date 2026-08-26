package com.dnd.puzzlemeet.domain.notification.service;

import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartureReminderService {

  private static final int REMINDER_WINDOW_MINUTES = 60;

  private final DepartureReminderQueryService departureReminderQueryService;
  private final DepartureReminderClaimService departureReminderClaimService;

  public int claimDepartureReminders() {
    return claimDepartureReminders(LocalDateTime.now());
  }

  int claimDepartureReminders(LocalDateTime now) {
    LocalDateTime windowEnd = now.plusMinutes(REMINDER_WINDOW_MINUTES);
    List<MeetingMemberRepository.DepartureReminderCandidate> candidates =
        departureReminderQueryService.findCandidates(now, windowEnd);

    int claimed = 0;
    int failed = 0;
    for (MeetingMemberRepository.DepartureReminderCandidate candidate : candidates) {
      try {
        if (departureReminderClaimService.claim(candidate, now, windowEnd)) {
          claimed++;
        }
      } catch (RuntimeException e) {
        failed++;
        log.error("[출발 리마인더 선점 실패] message={}", e.getMessage(), e);
      }
    }
    if (failed > 0) {
      log.info("[출발 리마인더] 선점 실패 집계, failed={}", failed);
    }
    return claimed;
  }
}
