package com.dnd.puzzlemeet.domain.meeting.repository;

import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingMemberRepository extends JpaRepository<MeetingMember, Long> {

  boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

  Optional<MeetingMember> findByMeetingIdAndUserId(Long meetingId, Long userId);

  List<MeetingMember> findAllByUserId(Long userId);

  @Query(
      """
      select case when count(mm) > 0 then true else false end
      from MeetingMember mm
      where mm.user.id = :userId
        and mm.meeting.id <> :meetingId
        and mm.status = com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberStatus.MOVING
        and mm.meeting.status in (
          com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus.WAITING,
          com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus.IN_PROGRESS
        )
      """)
  boolean existsMovingMemberInOtherActiveMeeting(
      @Param("userId") Long userId, @Param("meetingId") Long meetingId);

  @Query(
      """
      select mm from MeetingMember mm
      join fetch mm.user
      where mm.meeting.id in :meetingIds
      order by mm.id asc
      """)
  List<MeetingMember> findAllByMeetingIdInFetchUser(@Param("meetingIds") List<Long> meetingIds);

  @Query(
      """
      select mm.user.id
      from MeetingMember mm
      where mm.meeting.id = :meetingId
        and mm.id <> :arrivedMemberId
        and mm.isFriendArrivalNotificationEnabled = true
        and mm.user.deletedAt is null
      order by mm.id asc
      """)
  List<Long> findFriendArrivalNotificationRecipientUserIds(
      @Param("meetingId") Long meetingId, @Param("arrivedMemberId") Long arrivedMemberId);

  @Query(
      """
      select mm.user.id
      from MeetingMember mm
      where mm.meeting.id = :meetingId
        and mm.id <> :senderMemberId
        and mm.isChatBubbleNotificationEnabled = true
        and mm.user.deletedAt is null
      order by mm.id asc
      """)
  List<Long> findQuickMessageNotificationRecipientUserIds(
      @Param("meetingId") Long meetingId, @Param("senderMemberId") Long senderMemberId);

  @Query(
      """
      select mm.user.id
      from MeetingMember mm
      where mm.id = :memberId
        and mm.meeting.id = :meetingId
        and mm.user.id = :userId
        and mm.user.deletedAt is null
      """)
  Optional<Long> findActiveDepartureReminderRecipientUserId(
      @Param("meetingId") Long meetingId,
      @Param("memberId") Long memberId,
      @Param("userId") Long userId);

  @Query(
      """
      select mm.id as memberId, mm.meeting.id as meetingId, mm.user.id as userId
      from MeetingMember mm
      where mm.meeting.status in (
          com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus.WAITING,
          com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus.IN_PROGRESS
        )
        and mm.status = com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberStatus.NOT_STARTED
        and mm.user.deletedAt is null
        and mm.departureReminderAttemptedAt is null
        and mm.meeting.meetingAt > :now
        and mm.meeting.meetingAt <= :windowEnd
      order by mm.id asc
      """)
  List<DepartureReminderCandidate> findDepartureReminderCandidates(
      @Param("now") LocalDateTime now, @Param("windowEnd") LocalDateTime windowEnd);

  @Modifying(flushAutomatically = true)
  @Query(
      """
      update MeetingMember mm
      set mm.departureReminderAttemptedAt = :attemptedAt
      where mm.id = :memberId
        and mm.meeting.id = :meetingId
        and mm.user.id = :userId
        and mm.meeting.status in (
          com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus.WAITING,
          com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus.IN_PROGRESS
        )
        and mm.status = com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberStatus.NOT_STARTED
        and mm.user.deletedAt is null
        and mm.departureReminderAttemptedAt is null
        and mm.meeting.meetingAt > :now
        and mm.meeting.meetingAt <= :windowEnd
      """)
  int claimDepartureReminder(
      @Param("memberId") Long memberId,
      @Param("meetingId") Long meetingId,
      @Param("userId") Long userId,
      @Param("now") LocalDateTime now,
      @Param("windowEnd") LocalDateTime windowEnd,
      @Param("attemptedAt") LocalDateTime attemptedAt);

  @Modifying(flushAutomatically = true)
  @Query(
      """
      update MeetingMember mm
      set mm.departureReminderAttemptedAt = null
      where mm.meeting.id = :meetingId
        and mm.status = com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberStatus.NOT_STARTED
      """)
  int resetDepartureReminderAttemptedAtForNotStartedMembers(@Param("meetingId") Long meetingId);

  interface DepartureReminderCandidate {

    Long getMemberId();

    Long getMeetingId();

    Long getUserId();
  }
}
