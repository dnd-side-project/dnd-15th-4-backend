package com.dnd.puzzlemeet.domain.meeting.controller;

import com.dnd.puzzlemeet.domain.meeting.dto.ReactionMessageSendRequest;
import com.dnd.puzzlemeet.domain.meeting.dto.ReactionPresetListResponse;
import com.dnd.puzzlemeet.domain.meeting.service.ReactionMessageService;
import com.dnd.puzzlemeet.domain.meeting.service.ReactionPresetService;
import com.dnd.puzzlemeet.global.annotation.ApiErrorCodeExample;
import com.dnd.puzzlemeet.global.annotation.ApiErrorCodeExamples;
import com.dnd.puzzlemeet.global.response.ApiResult;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.response.SuccessCode;
import com.dnd.puzzlemeet.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "퀵 메시지", description = "퀵 메시지 API")
@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
public class ReactionMessageController {

  private final ReactionMessageService reactionMessageService;
  private final ReactionPresetService reactionPresetService;

  @Operation(summary = "퀵메시지 전송", description = "약속 참여자가 프리셋으로 등록된 퀵메시지를 보낸다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.INVALID_INPUT_VALUE,
    ErrorCode.MEETING_NOT_FOUND,
    ErrorCode.AUTH_FORBIDDEN,
    ErrorCode.REACTION_PRESET_NOT_FOUND
  })
  @PostMapping("/{meetingId}/reaction-messages")
  public ResponseEntity<ApiResult<Void>> sendReactionMessage(
      @AuthenticationPrincipal UserPrincipal principal,
      @PathVariable Long meetingId,
      @Valid @RequestBody ReactionMessageSendRequest request) {
    reactionMessageService.sendReactionMessage(principal.id(), meetingId, request);
    return ApiResult.success(SuccessCode.CREATED, null);
  }

  @Operation(summary = "퀵메시지 프리셋 목록 조회", description = "활성화된 퀵메시지 프리셋 목록을 조회한다.")
  @ApiErrorCodeExample(ErrorCode.AUTH_TOKEN_INVALID)
  @GetMapping("/reaction-presets")
  public ResponseEntity<ApiResult<List<ReactionPresetListResponse>>> getReactionPresets() {
    return ApiResult.success(reactionPresetService.getReactionPresets());
  }
}
