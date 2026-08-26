package com.dnd.puzzlemeet.domain.notification.service;

import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import com.dnd.puzzlemeet.domain.notification.event.DepartureReminderClaimedEvent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepartureReminderClaimService {

  private final MeetingMemberRepository meetingMemberRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean claim(
      MeetingMemberRepository.DepartureReminderCandidate candidate,
      LocalDateTime now,
      LocalDateTime windowEnd) {
    int affectedRows =
        meetingMemberRepository.claimDepartureReminder(
            candidate.getMemberId(),
            candidate.getMeetingId(),
            candidate.getUserId(),
            now,
            windowEnd,
            now);
    if (affectedRows != 1) {
      return false;
    }

    eventPublisher.publishEvent(
        new DepartureReminderClaimedEvent(
            candidate.getMeetingId(), candidate.getMemberId(), candidate.getUserId()));
    return true;
  }
}
