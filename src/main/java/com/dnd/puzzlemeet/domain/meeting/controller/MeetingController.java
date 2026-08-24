package com.dnd.puzzlemeet.domain.meeting.controller;

import com.dnd.puzzlemeet.domain.meeting.dto.MeetingCreateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingCreateResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingInProgressResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingInviteCodeResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingJoinRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingJoinResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingListResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberArrivalResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberDepartureCreateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberDepartureResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberDepartureUpdateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberLocationUpdateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberNicknameUpdateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberNicknameUpdateResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingMemberPuzzleImageUpdateResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingPreviewRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingPreviewResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingResultResponse;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingRouteSearchRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingRouteSearchResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
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
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResult<MeetingCreateResponse>> createMeeting(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestPart("request") MeetingCreateRequest request,
      @Parameter(description = "약속 퍼즐 이미지") @RequestPart(value = "image", required = false)
          MultipartFile image) {
    return ApiResult.success(
        SuccessCode.CREATED, meetingService.createMeeting(principal.id(), request, image));
  }

  @Operation(summary = "약속방 초대 코드 조회", description = "방장이 자신이 만든 약속방의 초대 코드를 조회한다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.MEETING_NOT_FOUND,
    ErrorCode.AUTH_FORBIDDEN
  })
  @GetMapping("/{meetingId}/invite-code")
  public ResponseEntity<ApiResult<MeetingInviteCodeResponse>> getInviteCode(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long meetingId) {
    return ApiResult.success(meetingService.getInviteCode(principal.id(), meetingId));
  }

  @Operation(summary = "초대 코드로 약속 조회", description = "초대 코드로 약속 정보를 조회한다. 참여하기 전 미리 보기용으로 사용된다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.INVALID_INPUT_VALUE,
    ErrorCode.MEETING_INVITE_CODE_INVALID
  })
  @PostMapping("/preview")
  public ResponseEntity<ApiResult<MeetingPreviewResponse>> previewMeeting(
      @Valid @RequestBody MeetingPreviewRequest request) {
    return ApiResult.success(meetingService.previewMeeting(request));
  }

  @Operation(summary = "초대 코드로 약속 참여", description = "초대 코드로 대기 중인 약속에 참여자로 등록된다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.INVALID_INPUT_VALUE,
    ErrorCode.MEETING_INVITE_CODE_INVALID,
    ErrorCode.MEETING_NOT_JOINABLE,
    ErrorCode.MEETING_MEMBER_ALREADY_JOINED
  })
  @PostMapping(path = "/members", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResult<MeetingJoinResponse>> joinMeeting(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestPart("request") MeetingJoinRequest request,
      @Parameter(description = "약속방 퍼즐 이미지") @RequestPart(value = "image", required = false)
          MultipartFile image) {
    return ApiResult.success(meetingService.joinMeeting(principal.id(), request, image));
  }

  @Operation(summary = "도착 완료", description = "저장된 현재 위치가 목적지 도착 반경 안이면 도착 처리한다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.MEETING_NOT_FOUND,
    ErrorCode.MEETING_MEMBER_NOT_FOUND,
    ErrorCode.MEETING_MEMBER_NOT_ACTIVE,
    ErrorCode.MEETING_ARRIVAL_LOCATION_INVALID
  })
  @PutMapping("/{meetingId}/members/me/arrival")
  public ResponseEntity<ApiResult<MeetingMemberArrivalResponse>> markMemberArrived(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long meetingId) {
    return ApiResult.success(meetingService.markMemberArrived(principal.id(), meetingId));
  }

  @Operation(summary = "약속방 닉네임 수정", description = "인증된 참여자가 해당 약속방에서 사용할 닉네임을 수정한다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.INVALID_INPUT_VALUE,
    ErrorCode.MEETING_NOT_FOUND,
    ErrorCode.MEETING_MEMBER_NOT_FOUND,
    ErrorCode.MEETING_MEMBER_NOT_ACTIVE
  })
  @PatchMapping("/{meetingId}/members/me/nickname")
  public ResponseEntity<ApiResult<MeetingMemberNicknameUpdateResponse>> updateMemberNickname(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long meetingId,
      @Valid @RequestBody MeetingMemberNicknameUpdateRequest request) {
    return ApiResult.success(
        meetingService.updateMemberNickname(principal.id(), meetingId, request));
  }

  @Operation(
      summary = "약속방 참여자 퍼즐 이미지 교체",
      description = "인증된 참여자가 해당 약속방에 등록한 퍼즐 이미지를 새 이미지로 교체한다. 대기 중인 약속에서만 교체할 수 있다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.MEETING_MEMBER_PUZZLE_IMAGE_REQUIRED,
    ErrorCode.MEETING_NOT_WAITING,
    ErrorCode.MEETING_NOT_FOUND,
    ErrorCode.MEETING_MEMBER_NOT_FOUND,
    ErrorCode.MEETING_MEMBER_NOT_ACTIVE
  })
  @PatchMapping(
      path = "/{meetingId}/members/me/puzzle-image",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResult<MeetingMemberPuzzleImageUpdateResponse>> updateMemberPuzzleImage(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long meetingId,
      @Parameter(description = "교체할 약속방 퍼즐 이미지") @RequestPart("image") MultipartFile image) {
    return ApiResult.success(
        meetingService.updateMemberPuzzleImage(principal.id(), meetingId, image));
  }

  @Operation(
      summary = "출발 설정 등록",
      description = "인증된 참여자의 출발지·알림 설정·닉네임과 이동 경로 조회로 고른 경로를 등록하고 출발 처리한다. 약속 당일에만 호출할 수 있다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.INVALID_INPUT_VALUE,
    ErrorCode.MEETING_MAP_ROUTE_NOT_FOUND,
    ErrorCode.MEETING_NOT_FOUND,
    ErrorCode.MEETING_MEMBER_NOT_FOUND,
    ErrorCode.MEETING_NOT_STARTED,
    ErrorCode.MEETING_DEPARTURE_ALREADY_SET,
    ErrorCode.MEETING_MEMBER_NOT_ACTIVE
  })
  @PostMapping("/{meetingId}/members/me/departure")
  public ResponseEntity<ApiResult<MeetingMemberDepartureResponse>> createMemberDeparture(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long meetingId,
      @Parameter(description = "출발 설정 정보") @Valid @RequestBody
          MeetingMemberDepartureCreateRequest request) {
    return ApiResult.success(meetingService.createDeparture(principal.id(), meetingId, request));
  }

  @Operation(summary = "출발 설정 조회", description = "인증된 참여자가 등록한 출발 설정과 이동 경로를 조회한다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.MEETING_NOT_FOUND,
    ErrorCode.MEETING_MEMBER_NOT_FOUND,
    ErrorCode.MEETING_DEPARTURE_NOT_FOUND,
    ErrorCode.MEETING_MEMBER_NOT_ACTIVE
  })
  @GetMapping("/{meetingId}/members/me/departure")
  public ResponseEntity<ApiResult<MeetingMemberDepartureResponse>> getMemberDeparture(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long meetingId) {
    return ApiResult.success(meetingService.getDeparture(principal.id(), meetingId));
  }

  @Operation(
      summary = "출발 설정 수정",
      description = "요청에 넣은 항목만 반영한다. 출발지를 넣으면 선택한 이동 경로도 함께 넣어야 하고 기존 경로를 대체한다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.INVALID_INPUT_VALUE,
    ErrorCode.MEETING_NOT_FOUND,
    ErrorCode.MEETING_MEMBER_NOT_FOUND,
    ErrorCode.MEETING_DEPARTURE_NOT_FOUND,
    ErrorCode.MEETING_MEMBER_NOT_ACTIVE
  })
  @PatchMapping("/{meetingId}/members/me/departure")
  public ResponseEntity<ApiResult<MeetingMemberDepartureResponse>> updateMemberDeparture(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long meetingId,
      @Parameter(description = "수정할 출발 설정 정보") @Valid @RequestBody
          MeetingMemberDepartureUpdateRequest request) {
    return ApiResult.success(meetingService.updateDeparture(principal.id(), meetingId, request));
  }

  @Operation(
      summary = "이동 경로 조회",
      description =
          "출발지에서 약속 장소까지 가는 경로를 이동수단별로 조회한다. 약속 시각에 도착하는 기준으로 계산하고 저장하지 않는다. "
              + "대중교통은 여러 건, 차량과 도보는 한 건을 돌려준다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.INVALID_INPUT_VALUE,
    ErrorCode.MEETING_MAP_ROUTE_NOT_FOUND,
    ErrorCode.MEETING_MAP_TOO_CLOSE,
    ErrorCode.MEETING_NOT_FOUND,
    ErrorCode.MEETING_MEMBER_NOT_FOUND,
    ErrorCode.MEETING_MEMBER_NOT_ACTIVE,
    ErrorCode.MEETING_MAP_UNAVAILABLE
  })
  @PostMapping("/{meetingId}/routes")
  public ResponseEntity<ApiResult<MeetingRouteSearchResponse>> searchMeetingRoutes(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long meetingId,
      @Parameter(description = "출발지 정보") @Valid @RequestBody MeetingRouteSearchRequest request) {
    return ApiResult.success(meetingService.searchRoutes(principal.id(), meetingId, request));
  }

  @Operation(summary = "약속방 나가기", description = "참여자가 약속방에서 나간다. 방장은 나갈 수 없고 약속 삭제를 사용한다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.MEETING_HOST_CANNOT_LEAVE,
    ErrorCode.MEETING_NOT_FOUND,
    ErrorCode.MEETING_MEMBER_NOT_FOUND,
    ErrorCode.MEETING_MEMBER_NOT_ACTIVE
  })
  @DeleteMapping("/{meetingId}/members/me")
  public ResponseEntity<ApiResult<Void>> leaveMeeting(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long meetingId) {
    meetingService.leaveMeeting(principal.id(), meetingId);
    return ApiResult.success(null);
  }

  @Operation(
      summary = "내 약속 목록 조회",
      description =
          "로그인한 사용자가 참여 중인 약속 목록을 조회한다. 상태(waiting, in-progress, completed)를 지정하지 않으면 전체를 조회한다.")
  @ApiErrorCodeExamples({ErrorCode.AUTH_TOKEN_INVALID, ErrorCode.INVALID_INPUT_VALUE})
  @GetMapping
  public ResponseEntity<ApiResult<List<MeetingListResponse>>> getMeetings(
      @AuthenticationPrincipal UserPrincipal principal,
      @Parameter(description = "약속 상태", example = "waiting") @RequestParam(required = false)
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

  @Operation(summary = "참여자 현재 위치 갱신", description = "약속 참여자가 자신의 현재 위치를 갱신한다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.INVALID_INPUT_VALUE,
    ErrorCode.MEETING_NOT_FOUND,
    ErrorCode.AUTH_FORBIDDEN
  })
  @PatchMapping("/{meetingId}/members/location")
  public ResponseEntity<ApiResult<Void>> updateCurrentLocation(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long meetingId,
      @Valid @RequestBody MeetingMemberLocationUpdateRequest request) {
    meetingService.updateCurrentLocation(
        principal.id(), meetingId, request.latitude(), request.longitude());
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

  @Operation(summary = "진행 중인 약속 데이터 조회", description = "진행 중인 약속방의 데이터를 조회한다. 1분 간격으로 polling 한다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.MEETING_NOT_FOUND,
    ErrorCode.AUTH_FORBIDDEN,
    ErrorCode.MEETING_NOT_STARTED
  })
  @GetMapping("/{meetingId}/in-progress")
  public ResponseEntity<ApiResult<MeetingInProgressResponse>> getMeetingInProgress(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long meetingId) {
    return ApiResult.success(meetingService.getMeetingInProgress(principal.id(), meetingId));
  }

  @Operation(summary = "약속 결과 조회", description = "완료된 약속방의 결과(퍼즐 피드, 도착 랭킹, 본인 출발 시각)를 조회한다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.MEETING_NOT_FOUND,
    ErrorCode.AUTH_FORBIDDEN,
    ErrorCode.MEETING_NOT_COMPLETED
  })
  @GetMapping("/{meetingId}/result")
  public ResponseEntity<ApiResult<MeetingResultResponse>> getMeetingResult(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long meetingId) {
    return ApiResult.success(meetingService.getMeetingResult(principal.id(), meetingId));
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
