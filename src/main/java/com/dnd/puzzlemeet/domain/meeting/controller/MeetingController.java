package com.dnd.puzzlemeet.domain.meeting.controller;

import com.dnd.puzzlemeet.domain.meeting.dto.MeetingCreateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingCreateResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingJoinRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingJoinResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingListResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingUpdateRequest;
import com.dnd.puzzlemeet.domain.meeting.entity.MeetingStatus;
import com.dnd.puzzlemeet.domain.meeting.service.MeetingService;
import com.dnd.puzzlemeet.global.annotation.ApiErrorCodeExamples;
import com.dnd.puzzlemeet.global.exception.ApiException;
import com.dnd.puzzlemeet.global.response.ApiResult;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.response.SuccessCode;
import com.dnd.puzzlemeet.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "약속", description = "약속 API")
@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
public class MeetingController {

  private final MeetingService meetingService;

  @Operation(summary = "약속방 생성", description = "약속방을 생성한다. 생성자는 자동으로 약속방의 참여자로 등록된다.")
  @ApiErrorCodeExamples({ErrorCode.AUTH_TOKEN_INVALID, ErrorCode.INVALID_INPUT_VALUE})
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResult<MeetingCreateResponse>> createMeeting(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestPart("request") MeetingCreateRequest request,
      @Parameter(description = "약속 대표 이미지") @RequestPart(value = "image", required = false)
          MultipartFile image) {
    return ApiResult.success(
        SuccessCode.CREATED, meetingService.createMeeting(principal.id(), request, image));
  }

  @Operation(summary = "초대 코드로 약속 참여", description = "초대 코드로 대기 중인 약속에 참여자로 등록된다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.INVALID_INPUT_VALUE,
    ErrorCode.MEETING_INVITE_CODE_INVALID,
    ErrorCode.MEETING_NOT_JOINABLE,
    ErrorCode.MEETING_MEMBER_ALREADY_JOINED
  })
  @PostMapping(path = "/join", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResult<MeetingJoinResponse>> joinMeeting(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestPart("request") MeetingJoinRequest request,
      @Parameter(description = "약속방 퍼즐 이미지") @RequestPart(value = "image", required = false)
          MultipartFile image) {
    return ApiResult.success(meetingService.joinMeeting(principal.id(), request, image));
  }

  @Operation(
      summary = "내 약속 목록 조회",
      description =
          "로그인한 사용자가 참여 중인 약속 목록을 조회한다. 상태(waiting, in-progress, completed)를 지정하지 않으면 전체를 조회한다.")
  @ApiErrorCodeExamples({ErrorCode.AUTH_TOKEN_INVALID, ErrorCode.INVALID_INPUT_VALUE})
  @GetMapping({"", "/{status}"})
  public ResponseEntity<ApiResult<List<MeetingListResponse>>> getMeetings(
      @AuthenticationPrincipal UserPrincipal principal,
      @Parameter(description = "약속 상태", example = "waiting") @PathVariable(required = false)
          String status) {
    return ApiResult.success(meetingService.getMeetings(principal.id(), resolveStatus(status)));
  }

  @Operation(
      summary = "약속 수정",
      description = "방장이 대기 중인 약속의 정보를 수정한다. 요청에 넣은 필드만 반영되고, 넣지 않은 필드는 기존 값을 유지한다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.INVALID_INPUT_VALUE,
    ErrorCode.AUTH_FORBIDDEN,
    ErrorCode.MEETING_NOT_FOUND,
    ErrorCode.MEETING_NOT_WAITING
  })
  @PatchMapping("/{meetingId}")
  public ResponseEntity<ApiResult<Void>> updateMeeting(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long meetingId,
      @Valid @RequestBody MeetingUpdateRequest request) {
    meetingService.updateMeeting(principal.id(), meetingId, request);
    return ApiResult.success(null);
  }

  @Operation(summary = "약속 삭제", description = "방장이 대기 중인 약속을 삭제한다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.AUTH_FORBIDDEN,
    ErrorCode.MEETING_NOT_FOUND,
    ErrorCode.MEETING_NOT_WAITING
  })
  @DeleteMapping("/{meetingId}")
  public ResponseEntity<ApiResult<Void>> deleteMeeting(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long meetingId) {
    meetingService.cancelMeeting(principal.id(), meetingId);
    return ApiResult.success(null);
  }

  private MeetingStatus resolveStatus(String rawStatus) {
    if (rawStatus == null) {
      return null;
    }
    return switch (rawStatus) {
      case "waiting" -> MeetingStatus.WAITING;
      case "in-progress" -> MeetingStatus.IN_PROGRESS;
      case "completed" -> MeetingStatus.COMPLETED;
      default -> throw ApiException.of(ErrorCode.INVALID_INPUT_VALUE);
    };
  }
}
