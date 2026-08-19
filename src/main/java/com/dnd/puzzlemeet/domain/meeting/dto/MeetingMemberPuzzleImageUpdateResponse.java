package com.dnd.puzzlemeet.domain.meeting.dto;

import com.dnd.puzzlemeet.domain.puzzle.entity.MemberImage;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record MeetingMemberPuzzleImageUpdateResponse(
    @Schema(description = "약속 식별자", example = "1", requiredMode = RequiredMode.REQUIRED)
        Long meetingId,
    @Schema(
            description = "교체된 참여자 이미지 URL",
            example = "https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/puzzles/1.png",
            maxLength = 500,
            requiredMode = RequiredMode.REQUIRED)
        String imageUrl) {

  public static MeetingMemberPuzzleImageUpdateResponse from(MemberImage memberImage) {
    return new MeetingMemberPuzzleImageUpdateResponse(
        memberImage.getMeetingMember().getMeeting().getId(), memberImage.getImageUrl());
  }
}
