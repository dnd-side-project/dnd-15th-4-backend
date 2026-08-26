package com.dnd.puzzlemeet.domain.meeting.dto;

import com.dnd.puzzlemeet.domain.meeting.entity.Meeting;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.LocalDateTime;
import java.util.List;

public record MeetingPreviewResponse(
    @Schema(description = "약속 식별자", example = "1", requiredMode = RequiredMode.REQUIRED)
        Long meetingId,
    @Schema(description = "약속 제목", example = "한강 피크닉", requiredMode = RequiredMode.REQUIRED)
        String title,
    @Schema(
            description = "약속 일시",
            example = "2026-08-10T14:00:00",
            requiredMode = RequiredMode.REQUIRED)
        LocalDateTime dateTime,
    @Schema(description = "약속 목적지명", example = "서울 여의도 한강공원", requiredMode = RequiredMode.REQUIRED)
        String place,
    @Schema(description = "약속 장소 위도", example = "37.5283", requiredMode = RequiredMode.REQUIRED)
        double latitude,
    @Schema(description = "약속 장소 경도", example = "126.9320", requiredMode = RequiredMode.REQUIRED)
        double longitude,
    @Schema(description = "약속 상태", example = "WAITING", requiredMode = RequiredMode.REQUIRED)
        MeetingStatus status,
    @Schema(description = "정원 (방장 포함)", example = "6", requiredMode = RequiredMode.REQUIRED)
        int capacity,
    @Schema(description = "현재 인원수", example = "3", requiredMode = RequiredMode.REQUIRED)
        int currentMemberCount,
    @Schema(description = "참여자 목록", requiredMode = RequiredMode.REQUIRED)
        List<Participant> participants) {

  public static MeetingPreviewResponse from(Meeting meeting, List<MeetingMember> members) {
    return new MeetingPreviewResponse(
        meeting.getId(),
        meeting.getTitle(),
        meeting.getMeetingAt(),
        meeting.getDestinationName(),
        meeting.getDestinationLatitude().doubleValue(),
        meeting.getDestinationLongitude().doubleValue(),
        meeting.getStatus(),
        meeting.getCapacity(),
        members.size(),
        members.stream().map(Participant::from).toList());
  }

  public record Participant(
      @Schema(description = "사용자 식별자", example = "1", requiredMode = RequiredMode.REQUIRED) Long id,
      @Schema(description = "약속방 내 닉네임", example = "김나나", requiredMode = RequiredMode.REQUIRED)
          String name,
      @Schema(
              description = "프로필 이미지 URL",
              example = "https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/profiles/1.png",
              requiredMode = RequiredMode.REQUIRED)
          String profileImageUrl) {

    public static Participant from(MeetingMember member) {
      return new Participant(
          member.getUser().getId(), member.getNickname(), member.getProfileImageUrl());
    }
  }
}
