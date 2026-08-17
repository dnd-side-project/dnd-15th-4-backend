package com.dnd.puzzlemeet.domain.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ReactionMessageSendRequest(
    @Schema(description = "리액션 프리셋 ID", example = "1") @NotNull Long presetId) {}
