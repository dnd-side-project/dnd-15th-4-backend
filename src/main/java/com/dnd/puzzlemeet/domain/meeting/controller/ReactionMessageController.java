package com.dnd.puzzlemeet.domain.meeting.controller;

import com.dnd.puzzlemeet.domain.meeting.dto.ReactionMessageSendRequest;
import com.dnd.puzzlemeet.domain.meeting.service.ReactionMessageService;
import com.dnd.puzzlemeet.global.annotation.ApiErrorCodeExamples;
import com.dnd.puzzlemeet.global.response.ApiResult;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.response.SuccessCode;
import com.dnd.puzzlemeet.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
}
