package com.dnd.puzzlemeet.domain.meeting.dto;

import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.LocalDateTime;

public record MeetingMemberArrivalResponse(
    @Schema(description = "약속 식별자", example = "1", requiredMode = RequiredMode.REQUIRED)
        Long meetingId,
    @Schema(
            description = "도착 완료 시각",
            example = "2026-08-17T14:00:00",
            requiredMode = RequiredMode.REQUIRED)
        LocalDateTime arrivalTime) {

  public static MeetingMemberArrivalResponse from(MeetingMember member) {
    return new MeetingMemberArrivalResponse(member.getMeeting().getId(), member.getArrivedAt());
  }
}
