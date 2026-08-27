package com.dnd.puzzlemeet.domain.meeting.dto;

import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.LocalDateTime;
import java.util.List;

public record MeetingInProgressResponse(
    @Schema(description = "약속 퍼즐 그룹 목록", requiredMode = RequiredMode.REQUIRED)
        List<PuzzleGroup> puzzleGroups,
    @Schema(description = "퀵메시지 목록", requiredMode = RequiredMode.REQUIRED)
        List<QuickMessage> quickMessages,
    @Schema(description = "약속 세션 완료 여부", requiredMode = RequiredMode.REQUIRED) boolean completed) {

  public record QuickMessage(
      @Schema(description = "퀵메시지 ID", example = "1", requiredMode = RequiredMode.REQUIRED) Long id,
      @Schema(description = "퀵메시지 내용", example = "지금 출발", requiredMode = RequiredMode.REQUIRED)
          String content) {}

  public record PuzzleGroup(
      @Schema(description = "퍼즐 이미지 URL", requiredMode = RequiredMode.REQUIRED)
          String puzzleImageUrl,
      @Schema(description = "퍼즐 그룹 ID", requiredMode = RequiredMode.REQUIRED) Long puzzleGroupId,
      @Schema(description = "퍼즐 페이지 순번", example = "1", requiredMode = RequiredMode.REQUIRED)
          int pageNumber,
      @Schema(description = "참여자 목록", requiredMode = RequiredMode.REQUIRED)
          List<Participant> members) {}

  public record Participant(
      @Schema(description = "사용자 아이디", example = "1", nullable = true) Long userId,
      @Schema(description = "약속방 내 닉네임", example = "김나나", nullable = true) String nickname,
      @Schema(
              description = "프로필 이미지 URL",
              example = "https://puzzle-meet-s3.s3.ap-northeast-2.amazonaws.com/profiles/1.png",
              nullable = true)
          String profileImageUrl,
      @Schema(description = "출발 여부", requiredMode = RequiredMode.REQUIRED) boolean departed,
      @Schema(description = "출발 일시", example = "2026-08-17T09:00:00", nullable = true)
          LocalDateTime departedAt,
      @Schema(description = "도착 여부", requiredMode = RequiredMode.REQUIRED) boolean arrived,
      @Schema(description = "현재 위치 위도", example = "37.5283", nullable = true) Double latitude,
      @Schema(description = "현재 위치 경도", example = "126.9320", nullable = true) Double longitude,
      @Schema(description = "최근 위치 갱신 일시", example = "2026-08-17T10:00:00", nullable = true)
          LocalDateTime locationUpdatedAt,
      @Schema(description = "예상 도착 일시", example = "2026-08-17T10:30:00", nullable = true)
          LocalDateTime estimatedArrivalTime,
      @Schema(
              description = "퍼즐 조각 위치 번호 (1~4)",
              example = "1",
              requiredMode = RequiredMode.REQUIRED)
          int pieceIndex,
      @Schema(description = "퍼즐 조각 공개 여부", requiredMode = RequiredMode.REQUIRED) boolean revealed) {

    public static Participant from(MeetingMember member, int pieceIndex, boolean revealed) {
      return new Participant(
          member.getUser().getId(),
          member.getNickname(),
          member.getProfileImageUrl(),
          member.getDepartedAt() != null,
          member.getDepartedAt(),
          member.getStatus() == MeetingMemberStatus.ARRIVED,
          member.getCurrentLatitude() != null ? member.getCurrentLatitude().doubleValue() : null,
          member.getCurrentLongitude() != null ? member.getCurrentLongitude().doubleValue() : null,
          member.getLocationUpdatedAt(),
          estimatedArrivalTime(member),
          pieceIndex,
          revealed);
    }

    public static Participant empty(int pieceIndex, boolean revealed) {
      return new Participant(
          null, null, null, false, null, false, null, null, null, null, pieceIndex, revealed);
    }

    private static LocalDateTime estimatedArrivalTime(MeetingMember member) {
      if (member.getDepartedAt() == null || member.getEstimatedDurationSeconds() == null) {
        return null;
      }
      return member.getDepartedAt().plusSeconds(member.getEstimatedDurationSeconds());
    }
  }
}
