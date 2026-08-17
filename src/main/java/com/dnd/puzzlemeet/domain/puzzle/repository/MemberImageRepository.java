package com.dnd.puzzlemeet.domain.puzzle.repository;

import com.dnd.puzzlemeet.domain.puzzle.entity.MemberImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberImageRepository extends JpaRepository<MemberImage, Long> {

  @Query(
      """
      select mi from MemberImage mi
      join fetch mi.meetingMember mm
      where mm.meeting.id = :meetingId
      """)
  List<MemberImage> findAllByMeetingId(@Param("meetingId") Long meetingId);
}
