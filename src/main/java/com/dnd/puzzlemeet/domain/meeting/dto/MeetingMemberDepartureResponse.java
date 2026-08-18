package com.dnd.puzzlemeet.domain.meeting.dto;

import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMember;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingMemberRoute;
import com.dnd.puzzlemeet.domain.meeting.entity.TransportType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.LocalDateTime;
import java.util.List;

public record MeetingMemberDepartureResponse(
    @Schema(description = "약속방 식별자", example = "1", requiredMode = RequiredMode.REQUIRED)
        Long meetingId,
    @Schema(description = "출발지 정보", requiredMode = RequiredMode.REQUIRED) Departure departure,
    @Schema(description = "알림 설정 정보", requiredMode = RequiredMode.REQUIRED)
        NotificationSettings notificationSettings,
    @Schema(description = "닉네임 설정 정보", requiredMode = RequiredMode.REQUIRED)
        NicknameSetting nicknameSetting,
    @Schema(description = "전체 예상 이동 시간(분)", example = "40", requiredMode = RequiredMode.REQUIRED)
        int totalEstimatedTime,
    @Schema(
            description = "약속 시각에 도착하려면 출발해야 하는 시각",
            example = "2026-08-20T13:20:00",
            requiredMode = RequiredMode.REQUIRED)
        LocalDateTime recommendedDepartureTime,
    @Schema(description = "이동 경로 목록", requiredMode = RequiredMode.REQUIRED) List<Route> routes) {

  private static final int SECONDS_PER_MINUTE = 60;

  public static MeetingMemberDepartureResponse of(
      MeetingMember member, List<MeetingMemberRoute> routes) {
    return new MeetingMemberDepartureResponse(
        member.getMeeting().getId(),
        new Departure(
            member.getDepartureName(),
            member.getDepartureLatitude().doubleValue(),
            member.getDepartureLongitude().doubleValue()),
        new NotificationSettings(
            member.isLocationNotificationEnabled(),
            member.isFriendArrivalNotificationEnabled(),
            member.isChatBubbleNotificationEnabled()),
        new NicknameSetting(member.isCustomNickname(), member.getNickname()),
        totalEstimatedMinutes(member),
        recommendedDepartureTime(member),
        routes.stream().map(Route::from).toList());
  }

  private static LocalDateTime recommendedDepartureTime(MeetingMember member) {
    Integer estimatedDurationSeconds = member.getEstimatedDurationSeconds();
    if (estimatedDurationSeconds == null) {
      return null;
    }
    return member.getMeeting().getMeetingAt().minusSeconds(estimatedDurationSeconds);
  }

  private static int totalEstimatedMinutes(MeetingMember member) {
    Integer estimatedDurationSeconds = member.getEstimatedDurationSeconds();
    return estimatedDurationSeconds != null ? estimatedDurationSeconds / SECONDS_PER_MINUTE : 0;
  }

  public record Departure(
      @Schema(description = "출발지 장소명", example = "서울대학교", requiredMode = RequiredMode.REQUIRED)
          String placeName,
      @Schema(description = "출발지 위도", example = "37.5665", requiredMode = RequiredMode.REQUIRED)
          double latitude,
      @Schema(description = "출발지 경도", example = "126.9780", requiredMode = RequiredMode.REQUIRED)
          double longitude) {}

  public record NotificationSettings(
      @Schema(
              description = "위치 권한 알림 설정 여부",
              example = "true",
              requiredMode = RequiredMode.REQUIRED)
          boolean locationPermission,
      @Schema(
              description = "친구 도착 알림 설정 여부",
              example = "true",
              requiredMode = RequiredMode.REQUIRED)
          boolean friendArrival,
      @Schema(description = "말풍선 알림 설정 여부", example = "false", requiredMode = RequiredMode.REQUIRED)
          boolean chatBubble) {}

  public record NicknameSetting(
      @Schema(description = "별도 닉네임 사용 여부", example = "true", requiredMode = RequiredMode.REQUIRED)
          boolean enabled,
      @Schema(description = "약속방에서 사용하는 닉네임", example = "김땡땡", requiredMode = RequiredMode.REQUIRED)
          String nickname) {}

  public record Route(
      @Schema(
              description = "이동 경로 설명",
              example = "태릉입구역 6호선 승차",
              requiredMode = RequiredMode.REQUIRED)
          String content,
      @Schema(description = "이동 수단 타입", example = "SUBWAY", requiredMode = RequiredMode.REQUIRED)
          TransportType transportType,
      @Schema(
              description = "이동 수단 상세 내용",
              example = "27개 역 이동",
              requiredMode = RequiredMode.REQUIRED)
          String transportContent,
      @Schema(
              description = "해당 경로 예상 소요 시간(분)",
              example = "30",
              requiredMode = RequiredMode.REQUIRED)
          int estimatedTime) {

    public static Route from(MeetingMemberRoute route) {
      return new Route(
          route.getContent(),
          route.getTransportType(),
          route.getTransportContent(),
          route.getEstimatedTimeMinutes());
    }
  }
}
