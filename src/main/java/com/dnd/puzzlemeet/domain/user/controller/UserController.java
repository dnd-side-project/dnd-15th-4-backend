package com.dnd.puzzlemeet.domain.user.controller;

import com.dnd.puzzlemeet.domain.user.dto.UserMeResponse;
import com.dnd.puzzlemeet.domain.user.dto.UserUpdateRequest;
import com.dnd.puzzlemeet.domain.user.service.UserService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자", description = "사용자 정보 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @Operation(summary = "본인 조회", description = "인증된 사용자 본인의 정보를 조회한다.")
  @ApiErrorCodeExamples({ErrorCode.AUTH_TOKEN_INVALID, ErrorCode.USER_NOT_FOUND})
  @GetMapping("/me")
  public ResponseEntity<ApiResult<UserMeResponse>> getMe(
      @AuthenticationPrincipal UserPrincipal principal) {
    return ApiResult.success(userService.getMe(principal.id()));
  }

  @Operation(summary = "본인 정보 수정", description = "인증된 사용자 본인의 닉네임을 수정하고 변경된 내 정보를 반환한다.")
  @ApiErrorCodeExamples({
    ErrorCode.AUTH_TOKEN_INVALID,
    ErrorCode.INVALID_INPUT_VALUE,
    ErrorCode.USER_NOT_FOUND
  })
  @PatchMapping("/me")
  public ResponseEntity<ApiResult<UserMeResponse>> updateMe(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody UserUpdateRequest request) {
    return ApiResult.success(userService.updateMe(principal.id(), request));
  }
}
