package com.dnd.puzzlemeet.domain.puzzle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.LocalDateTime;
import java.util.List;

public record MeetingCollectionResponse(
    @Schema(description = "약속 식별자", example = "1", requiredMode = RequiredMode.REQUIRED)
        Long meetingId,
    @Schema(description = "약속 제목", example = "한강 피크닉", requiredMode = RequiredMode.REQUIRED)
        String title,
    @Schema(
            description = "약속 일시",
            example = "2026-08-10T14:00:00",
            requiredMode = RequiredMode.REQUIRED)
        LocalDateTime meetingAt,
    @Schema(description = "약속 목적지명", example = "서울 여의도 한강공원", requiredMode = RequiredMode.REQUIRED)
        String destination,
    @Schema(description = "완성된 퍼즐 이미지 URL 목록", requiredMode = RequiredMode.REQUIRED)
        List<String> puzzleImageUrls,
    @Schema(description = "도착 랭킹", requiredMode = RequiredMode.REQUIRED)
        List<RankingEntry> rankings) {

  public record RankingEntry(
      @Schema(description = "사용자 ID", example = "1", requiredMode = RequiredMode.REQUIRED)
          Long userId,
      @Schema(description = "약속방 내 닉네임", example = "김나나", requiredMode = RequiredMode.REQUIRED)
          String nickname,
      @Schema(description = "프로필 이미지 URL", requiredMode = RequiredMode.REQUIRED)
          String profileImageUrl,
      @Schema(description = "도착 여부", requiredMode = RequiredMode.REQUIRED) boolean arrived,
      @Schema(description = "도착 시각", nullable = true) LocalDateTime arrivedAt,
      @Schema(description = "약속 시각보다 일찍 도착한 분 수. 지각·미도착이면 null", nullable = true)
          Long earlyArrivalMinutes,
      @Schema(description = "지각 여부 (미도착이거나 약속 시각 이후 도착)", requiredMode = RequiredMode.REQUIRED)
          boolean late) {}
}
