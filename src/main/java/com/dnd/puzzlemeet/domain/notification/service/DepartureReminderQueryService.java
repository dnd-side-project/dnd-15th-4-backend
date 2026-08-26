package com.dnd.puzzlemeet.domain.notification.service;

import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepartureReminderQueryService {

  private final MeetingMemberRepository meetingMemberRepository;

  @Transactional(readOnly = true)
  public List<MeetingMemberRepository.DepartureReminderCandidate> findCandidates(
      LocalDateTime now, LocalDateTime windowEnd) {
    return meetingMemberRepository.findDepartureReminderCandidates(now, windowEnd);
  }
}
