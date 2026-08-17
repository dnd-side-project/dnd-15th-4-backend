package com.dnd.puzzlemeet.domain.meeting.repository;

import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

  boolean existsByInviteCode(String inviteCode);

  Optional<Meeting> findByInviteCode(String inviteCode);

  @Query(
      """
      select m from Meeting m
      where m.status <> com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus.CANCELED
        and (:status is null or m.status = :status)
        and exists (
          select 1 from MeetingMember mm
          where mm.meeting = m and mm.user.id = :userId
        )
      """)
  List<Meeting> findAllByParticipantUserIdAndStatus(
      @Param("userId") Long userId, @Param("status") MeetingStatus status);

  @Query(
      """
      select m from Meeting m
      where m.status <> com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus.CANCELED
        and m.status <> com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus.COMPLETED
        and m.meetingAt <= :now
      """)
  List<Meeting> findAllExpired(@Param("now") LocalDateTime now);

  @Query(
      """
      select m from Meeting m
      where m.status = com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus.WAITING
        and m.meetingAt >= :startOfDay
        and m.meetingAt < :endOfDay
      """)
  List<Meeting> findAllStartingBetween(
      @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
}
