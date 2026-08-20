package com.dnd.puzzlemeet.domain.meeting.repository;

import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingMemberRepository extends JpaRepository<MeetingMember, Long> {

  boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

  Optional<MeetingMember> findByMeetingIdAndUserId(Long meetingId, Long userId);

  List<MeetingMember> findAllByUserId(Long userId);

  @Query(
      """
      select mm from MeetingMember mm
      join fetch mm.user
      where mm.meeting.id in :meetingIds
      order by mm.id asc
      """)
  List<MeetingMember> findAllByMeetingIdInFetchUser(@Param("meetingIds") List<Long> meetingIds);
}
