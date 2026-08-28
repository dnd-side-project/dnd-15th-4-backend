package com.dnd.puzzlemeet.domain.meeting.scheduler;

import com.dnd.puzzlemeet.domain.meeting.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MeetingScheduler {

  private final MeetingService meetingService;

  @Scheduled(cron = "0 */5 * * * *")
  public void startTodaysMeetings() {
    meetingService.startTodaysMeetings();
  }

  @Scheduled(cron = "0 * * * * *")
  public void completeExpiredMeetings() {
    meetingService.completeExpiredMeetings();
  }
}
