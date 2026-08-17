package com.dnd.puzzlemeet.domain.meeting.dto;

import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record MeetingCreateResponse(
    @Schema(description = "생성된 약속 식별자", example = "1", requiredMode = RequiredMode.REQUIRED)
        Long meetingId) {

  public static MeetingCreateResponse from(Meeting meeting) {
    return new MeetingCreateResponse(meeting.getId());
  }
}
