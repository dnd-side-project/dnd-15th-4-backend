package com.dnd.puzzlemeet.domain.meeting.controller;

import com.dnd.puzzlemeet.domain.meeting.dto.MeetingCreateRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.MeetingCreateResponse;
import com.dnd.puzzlemeet.domain.meeting.service.MeetingService;
import com.dnd.puzzlemeet.global.annotation.ApiErrorCodeExamples;
import com.dnd.puzzlemeet.global.response.ApiResult;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.response.SuccessCode;
import com.dnd.puzzlemeet.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
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
}
