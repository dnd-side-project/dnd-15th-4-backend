package com.dnd.puzzlemeet.domain.meeting.repository;

import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

  boolean existsByInviteCode(String inviteCode);
}
