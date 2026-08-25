package com.dnd.puzzlemeet.domain.puzzle.repository;

import com.dnd.puzzlemeet.domain.puzzle.entity.MemberImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberImageRepository extends JpaRepository<MemberImage, Long> {

  Optional<MemberImage> findByMeetingMemberId(Long meetingMemberId);

  List<MemberImage> findAllByMeetingMemberIdIn(List<Long> meetingMemberIds);

  void deleteAllByMeetingMemberId(Long meetingMemberId);

  @Query(
      """
      select mi from MemberImage mi
      join fetch mi.meetingMember mm
      join fetch mm.user
      where mm.meeting.id = :meetingId
      """)
  List<MemberImage> findAllByMeetingId(@Param("meetingId") Long meetingId);
}
