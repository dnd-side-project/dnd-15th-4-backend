package com.dnd.puzzlemeet.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ResponseEntity;

@Schema(description = "공통 에러 응답 포맷")
public record ErrorResult(
    @Schema(description = "커스텀 코드", example = "MEETING_NOT_FOUND") String code,
    @Schema(description = "에러 메시지", example = "존재하지 않는 약속입니다.") String message) {

  public static ResponseEntity<ErrorResult> of(ErrorCode errorCode) {
    ErrorResult body = new ErrorResult(errorCode.getCode(), errorCode.getMessage());
    return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
  }
}
