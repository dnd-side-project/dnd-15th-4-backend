package com.dnd.puzzlemeet.domain.meeting.repository;

import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRoute;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingMemberRouteRepository extends JpaRepository<MeetingMemberRoute, Long> {

  List<MeetingMemberRoute> findAllByMeetingMemberIdOrderByRouteIndexAsc(Long meetingMemberId);

  void deleteAllByMeetingMemberId(Long meetingMemberId);
}
