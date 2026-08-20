package com.dnd.puzzlemeet.domain.meeting.dto;

import com.dnd.puzzlemeet.domain.meeting.entity.TransportType;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

@Schema(description = "이동 경로 조회로 받은 경로 중 사용자가 선택한 하나")
public record MeetingRouteRequest(
    @Schema(description = "총 소요시간(초)", example = "3780", requiredMode = RequiredMode.REQUIRED)
        @NotNull
        @Positive
        Integer totalTime,
    @Schema(description = "상세 이동 구간 목록", requiredMode = RequiredMode.REQUIRED) @NotEmpty @Valid
        List<Step> steps) {

  public record Step(
      @Schema(description = "이동수단 타입", example = "SUBWAY", requiredMode = RequiredMode.REQUIRED)
          @NotNull
          TransportType type,
      @Schema(description = "구간 소요시간(초)", example = "1620", requiredMode = RequiredMode.REQUIRED)
          @NotNull
          Integer time,
      @Schema(description = "노선명", example = "수도권6호선", nullable = true) String line,
      @Schema(description = "구간의 시작·끝 지점", nullable = true) @Valid Station station,
      @Schema(description = "이동 경로상의 정류장 목록", nullable = true) List<String> stations) {

    public String startName() {
      return station != null ? station.start() : null;
    }

    public String endName() {
      return station != null ? station.end() : null;
    }

    public int stationCount() {
      return stations != null ? Math.max(stations.size() - 1, 0) : 0;
    }
  }

  public record Station(
      @Schema(description = "출발 정류장·역", example = "태릉입구", nullable = true) String start,
      @Schema(description = "도착 정류장·역", example = "성수", nullable = true) String end) {}
}
