package com.dnd.puzzlemeet.domain.meeting.dto;

import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record MeetingInviteCodeResponse(
    @Schema(description = "초대 코드", example = "aB3dEfGh", requiredMode = RequiredMode.REQUIRED)
        String inviteCode) {

  public static MeetingInviteCodeResponse from(Meeting meeting) {
    return new MeetingInviteCodeResponse(meeting.getInviteCode());
  }
}
