package com.dnd.puzzlemeet.domain.meeting.scheduler;

import com.dnd.puzzlemeet.domain.meeting.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MeetingScheduler {

  private final MeetingService meetingService;

  // 매일 자정에 그날 열리는 약속을 시작 처리하고 퍼즐 그룹을 배정한다.
  @Scheduled(cron = "0 */5 * * * *")
  public void startTodaysMeetings() {
    meetingService.startTodaysMeetings();
  }

  // 매분 정각에 약속 시각이 지난 약속을 종료 처리한다.
  @Scheduled(cron = "0 * * * * *")
  public void completeExpiredMeetings() {
    meetingService.completeExpiredMeetings();
  }
}
