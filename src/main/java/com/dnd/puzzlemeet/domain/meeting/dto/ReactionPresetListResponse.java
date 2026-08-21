package com.dnd.puzzlemeet.domain.meeting.dto;

import com.dnd.puzzlemeet.domain.meeting.entity.ReactionPreset;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReactionPresetListResponse(
    @Schema(description = "리액션 프리셋 ID", example = "1") Long id,
    @Schema(description = "프리셋 문구", example = "지금 출발") String content) {

  public static ReactionPresetListResponse from(ReactionPreset preset) {
    return new ReactionPresetListResponse(preset.getId(), preset.getContent());
  }
}
