package com.dnd.puzzlemeet.domain.meeting.dto;

import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record MeetingMemberNicknameUpdateResponse(
    @Schema(description = "약속 식별자", example = "1", requiredMode = RequiredMode.REQUIRED)
        Long meetingId,
    @Schema(
            description = "변경된 약속방 내 닉네임",
            example = "효창",
            maxLength = 30,
            requiredMode = RequiredMode.REQUIRED)
        String nickname) {

  public static MeetingMemberNicknameUpdateResponse from(MeetingMember member) {
    return new MeetingMemberNicknameUpdateResponse(
        member.getMeeting().getId(), member.getNickname());
  }
}
