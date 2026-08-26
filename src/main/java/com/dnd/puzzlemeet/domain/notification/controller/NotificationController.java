package com.dnd.puzzlemeet.domain.notification.controller;

import com.dnd.puzzlemeet.domain.notification.dto.PushSubscriptionCreateRequest;
import com.dnd.puzzlemeet.domain.notification.dto.PushSubscriptionDeleteRequest;
import com.dnd.puzzlemeet.domain.notification.dto.PushVapidPublicKeyResponse;
import com.dnd.puzzlemeet.domain.notification.service.PushSubscriptionService;
import com.dnd.puzzlemeet.global.annotation.ApiErrorCodeExample;
import com.dnd.puzzlemeet.global.annotation.ApiErrorCodeExamples;
import com.dnd.puzzlemeet.global.response.ApiResult;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import com.dnd.puzzlemeet.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "브라우저 알림", description = "WebPush 브라우저 알림 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final PushSubscriptionService pushSubscriptionService;

  @Operation(summary = "VAPID 공개키 조회", description = "브라우저 푸시 구독에 사용할 VAPID 공개키를 조회한다.")
  @ApiErrorCodeExample(ErrorCode.AUTH_TOKEN_INVALID)
  @GetMapping("/vapid-public-key")
  public ResponseEntity<ApiResult<PushVapidPublicKeyResponse>> getVapidPublicKey() {
    return ApiResult.success(pushSubscriptionService.getVapidPublicKey());
  }

  @Operation(summary = "브라우저 푸시 구독 등록", description = "현재 브라우저의 푸시 구독을 등록하거나 갱신한다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.INVALID_INPUT_VALUE,
    ErrorCode.USER_NOT_FOUND
  })
  @PostMapping("/push-subscriptions")
  public ResponseEntity<ApiResult<Void>> upsertPushSubscription(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody PushSubscriptionCreateRequest request) {
    pushSubscriptionService.upsert(principal.id(), request);
    return ApiResult.success(null);
  }

  @Operation(summary = "브라우저 푸시 구독 해지", description = "현재 사용자의 브라우저 푸시 구독을 멱등적으로 해지한다.")
  @ApiErrorCodeExamples({ErrorCode.AUTH_TOKEN_INVALID, ErrorCode.INVALID_INPUT_VALUE})
  @DeleteMapping("/push-subscriptions")
  public ResponseEntity<ApiResult<Void>> deletePushSubscription(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody PushSubscriptionDeleteRequest request) {
    pushSubscriptionService.delete(principal.id(), request);
    return ApiResult.success(null);
  }
}
