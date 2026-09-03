package com.dnd.puzzlemeet.domain.meeting.repository;

import com.dnd.puzzlemeet.domain.meeting.entity.ReactionMessage;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReactionMessageRepository extends JpaRepository<ReactionMessage, Long> {

  @Query(
      """
      select rm from ReactionMessage rm
      join fetch rm.senderMember sm
      join fetch sm.user
      where sm.meeting.id = :meetingId
      order by rm.sentAt desc
      """)
  List<ReactionMessage> findRecentByMeetingId(
      @Param("meetingId") Long meetingId, Pageable pageable);

  void deleteAllBySenderMemberId(Long senderMemberId);
}
