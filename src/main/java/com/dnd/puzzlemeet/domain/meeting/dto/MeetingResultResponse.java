package com.dnd.puzzlemeet.domain.meeting.dto;

import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberStatus;
import com.dnd.puzzlemeet.domain.puzzle.entity.MemberImage;
import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzlePage;
import com.dnd.puzzlemeet.domain.puzzle.entity.PuzzlePiece;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.LocalDateTime;
import java.util.List;

public record MeetingResultResponse(
    @Schema(description = "퍼즐 피드 목록", requiredMode = RequiredMode.REQUIRED)
        List<PuzzleFeedItem> puzzleFeed,
    @Schema(
            description = "그룹에는 속했지만 대표 이미지로 뽑히지 않아 퍼즐로 맞추지 않은 이미지 목록",
            requiredMode = RequiredMode.REQUIRED)
        List<UnselectedImage> unselectedImages,
    @Schema(description = "도착 랭킹", requiredMode = RequiredMode.REQUIRED)
        List<RankingEntry> rankings,
    @Schema(description = "요청자 본인의 출발 시각", nullable = true) LocalDateTime myDepartedAt) {

  public record PuzzleFeedItem(
      @Schema(description = "퍼즐 페이지 ID", example = "1", requiredMode = RequiredMode.REQUIRED)
          Long puzzlePageId,
      @Schema(description = "대표로 뽑힌 퍼즐 이미지 URL", requiredMode = RequiredMode.REQUIRED)
          String imageUrl,
      @Schema(description = "네 조각이 모두 도착해 퍼즐을 완성했는지 여부", requiredMode = RequiredMode.REQUIRED)
          boolean completed,
      @Schema(description = "조각별 성공·실패 목록 (4개)", requiredMode = RequiredMode.REQUIRED)
          List<PuzzlePieceResult> pieces) {

    public static PuzzleFeedItem of(PuzzlePage page, List<PuzzlePiece> pieces, boolean completed) {
      String imageUrl =
          page.getRepresentativeMemberImage() != null
              ? page.getRepresentativeMemberImage().getImageUrl()
              : null;

      return new PuzzleFeedItem(
          page.getId(), imageUrl, completed, pieces.stream().map(PuzzlePieceResult::from).toList());
    }
  }

  public record PuzzlePieceResult(
      @Schema(
              description = "퍼즐 조각 위치 번호 (1~4)",
              example = "1",
              requiredMode = RequiredMode.REQUIRED)
          int pieceIndex,
      @Schema(description = "조각 담당자의 도착 여부(성공 여부)", requiredMode = RequiredMode.REQUIRED)
          boolean success,
      @Schema(description = "조각 담당자 사용자 ID", example = "1", nullable = true) Long uploaderId,
      @Schema(description = "조각 담당자 닉네임", example = "김나나", nullable = true) String uploaderNickname,
      @Schema(description = "조각 담당자 프로필 이미지 URL", nullable = true) String uploaderProfileImageUrl) {

    public static PuzzlePieceResult from(PuzzlePiece piece) {
      MeetingMember member = piece.getMeetingMember();
      boolean success = member != null && member.getStatus() == MeetingMemberStatus.ARRIVED;

      return new PuzzlePieceResult(
          piece.getPieceIndex(),
          success,
          member != null ? member.getUser().getId() : null,
          member != null ? member.getNickname() : null,
          member != null ? member.getProfileImageUrl() : null);
    }
  }

  public record UnselectedImage(
      @Schema(description = "퍼즐로 맞추지 않은 이미지 URL", requiredMode = RequiredMode.REQUIRED)
          String imageUrl,
      @Schema(description = "사진을 올린 사용자 ID", example = "1", requiredMode = RequiredMode.REQUIRED)
          Long uploaderId,
      @Schema(description = "사진을 올린 참여자 닉네임", example = "김나나", requiredMode = RequiredMode.REQUIRED)
          String uploaderNickname,
      @Schema(description = "사진을 올린 참여자 프로필 이미지 URL", requiredMode = RequiredMode.REQUIRED)
          String uploaderProfileImageUrl) {

    public static UnselectedImage from(MemberImage image) {
      return new UnselectedImage(
          image.getImageUrl(),
          image.getMeetingMember().getUser().getId(),
          image.getMeetingMember().getNickname(),
          image.getMeetingMember().getProfileImageUrl());
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
