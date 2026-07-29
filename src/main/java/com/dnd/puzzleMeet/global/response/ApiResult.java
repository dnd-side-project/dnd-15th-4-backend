package com.dnd.puzzleMeet.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
@Schema(description = "공통 API 응답 포맷")
public class ApiResult<T> {

  @Schema(description = "HTTP 상태 코드", example = "200")
  private HttpStatus status;

  @Schema(description = "커스텀 코드", example = "API_SUCCESS")
  private String code;

  @Schema(description = "응답 메시지", example = "API_SUCCESS")
  private String message;

  @Schema(description = "응답 데이터", nullable = true)
  private T data;

  private ApiResult(HttpStatus status, String code, String message, T data) {
    this.status = status;
    this.code = code;
    this.message = message;
    this.data = data;
  }

  public static <T> ResponseEntity<ApiResult<T>> success(T data) {
    return success(SuccessCode.OK, data);
  }

  public static <T> ResponseEntity<ApiResult<T>> success(SuccessCode successCode, T data) {
    ApiResult<T> body =
        new ApiResult<>(
            successCode.getHttpStatus(), successCode.getCode(), successCode.getMessage(), data);
    return ResponseEntity.status(successCode.getHttpStatus()).body(body);
  }

  public static <T> ApiResult<T> fail(ErrorCode errorCode) {
    return new ApiResult<>(
        errorCode.getHttpStatus(), errorCode.getCode(), errorCode.getMessage(), null);
  }
}
