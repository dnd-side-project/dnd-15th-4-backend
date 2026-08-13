package com.dnd.puzzlemeet.domain.meeting.repository;

import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingMemberRepository extends JpaRepository<MeetingMember, Long> {}
