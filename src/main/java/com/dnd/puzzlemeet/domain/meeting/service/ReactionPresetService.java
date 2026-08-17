package com.dnd.puzzlemeet.domain.meeting.service;

import com.dnd.puzzlemeet.domain.meeting.dto.ReactionPresetListResponse;
import com.dnd.puzzlemeet.domain.meeting.repository.ReactionPresetRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReactionPresetService {

  private final ReactionPresetRepository reactionPresetRepository;

  @Transactional(readOnly = true)
  public List<ReactionPresetListResponse> getReactionPresets() {
    return reactionPresetRepository.findAllByIsActiveTrueOrderByIdAsc().stream()
        .map(ReactionPresetListResponse::from)
        .toList();
  }
}
