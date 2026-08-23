package com.dnd.puzzlemeet.domain.meeting.dto;

import com.dnd.puzzlemeet.domain.puzzle.entity.MemberImage;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.LocalDateTime;
import java.util.List;

public record MeetingResultResponse(
    @Schema(description = "완성된 퍼즐 피드 목록", requiredMode = RequiredMode.REQUIRED)
        List<PuzzleFeedItem> puzzleFeed,
    @Schema(description = "도착 랭킹", requiredMode = RequiredMode.REQUIRED)
        List<RankingEntry> rankings,
    @Schema(description = "요청자 본인의 출발 시각", nullable = true) LocalDateTime myDepartedAt) {

  public record PuzzleFeedItem(
      @Schema(description = "완성된 퍼즐 이미지 URL", requiredMode = RequiredMode.REQUIRED) String imageUrl,
      @Schema(description = "사진을 올린 사용자 ID", example = "1", requiredMode = RequiredMode.REQUIRED)
          Long uploaderId,
      @Schema(description = "사진을 올린 참여자 닉네임", example = "김나나", requiredMode = RequiredMode.REQUIRED)
          String uploaderNickname,
      @Schema(description = "사진을 올린 참여자 프로필 이미지 URL", requiredMode = RequiredMode.REQUIRED)
          String uploaderProfileImageUrl) {

    public static PuzzleFeedItem from(MemberImage representativeMemberImage) {
      return new PuzzleFeedItem(
          representativeMemberImage.getImageUrl(),
          representativeMemberImage.getMeetingMember().getUser().getId(),
          representativeMemberImage.getMeetingMember().getNickname(),
          representativeMemberImage.getMeetingMember().getProfileImageUrl());
    }
  }

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
