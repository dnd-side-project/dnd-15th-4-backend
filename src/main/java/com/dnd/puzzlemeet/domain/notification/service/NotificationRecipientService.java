package com.dnd.puzzlemeet.domain.notification.service;

import com.dnd.puzzlemeet.domain.meeting.repository.MeetingMemberRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationRecipientService {

  private final MeetingMemberRepository meetingMemberRepository;

  @Transactional(readOnly = true)
  public List<Long> findFriendArrivalRecipients(Long meetingId, Long arrivedMemberId) {
    return meetingMemberRepository.findFriendArrivalNotificationRecipientUserIds(
        meetingId, arrivedMemberId);
  }

  @Transactional(readOnly = true)
  public List<Long> findQuickMessageRecipients(Long meetingId, Long senderMemberId) {
    return meetingMemberRepository.findQuickMessageNotificationRecipientUserIds(
        meetingId, senderMemberId);
  }

  @Transactional(readOnly = true)
  public List<Long> findDepartureReminderRecipient(Long meetingId, Long memberId, Long userId) {
    return meetingMemberRepository
        .findActiveDepartureReminderRecipientUserId(meetingId, memberId, userId)
        .stream()
        .toList();
  }
}
