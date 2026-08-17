package com.dnd.puzzlemeet.domain.meeting.repository;

import com.dnd.puzzlemeet.domain.meeting.entity.ReactionMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReactionMessageRepository extends JpaRepository<ReactionMessage, Long> {}
