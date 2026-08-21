package com.dnd.puzzlemeet.domain.meeting.repository;

import com.dnd.puzzlemeet.domain.meeting.entity.ReactionPreset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReactionPresetRepository extends JpaRepository<ReactionPreset, Long> {

  Optional<ReactionPreset> findByIdAndIsActiveTrue(Long id);

  List<ReactionPreset> findAllByIsActiveTrueOrderByIdAsc();
}
