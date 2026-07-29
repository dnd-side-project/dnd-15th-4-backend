package com.dnd.puzzlemeet.global.exception;

import com.dnd.puzzlemeet.global.response.ApiResult;
import com.dnd.puzzlemeet.global.response.ErrorCode;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  // 비즈니스 로직에서 명시적으로 던진 예외
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiResult<Void>> handleApiException(ApiException e) {
    ErrorCode errorCode = e.getErrorCode();
    log.warn("[ApiException] code={}, message={}", errorCode.getCode(), errorCode.getMessage());
    return createErrorResponse(errorCode);
  }

  // @Valid @RequestBody 검증 실패
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResult<Void>> handleMethodArgumentNotValid(
      MethodArgumentNotValidException e) {
    log.warn("[검증 실패] 요청 바디 검증 실패: {}", extractBindingErrors(e.getBindingResult()));
    return createErrorResponse(ErrorCode.INVALID_INPUT_VALUE);
  }

  // @ModelAttribute 바인딩/검증 실패
  @ExceptionHandler(BindException.class)
  public ResponseEntity<ApiResult<Void>> handleBindException(BindException e) {
    log.warn("[검증 실패] 파라미터 바인딩 검증 실패: {}", extractBindingErrors(e.getBindingResult()));
    return createErrorResponse(ErrorCode.INVALID_INPUT_VALUE);
  }

  // @RequestParam/@PathVariable 등 메서드 파라미터 검증 실패
  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ApiResult<Void>> handleHandlerMethodValidation(
      HandlerMethodValidationException e) {
    log.warn("[검증 실패] 메서드 파라미터 검증 실패: {}", e.getMessage());
    return createErrorResponse(ErrorCode.INVALID_INPUT_VALUE);
  }

  // 요청 값 타입 불일치
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiResult<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
    Class<?> requiredType = e.getRequiredType();
    log.warn(
        "[타입 불일치] name={}, requiredType={}",
        e.getName(),
        requiredType != null ? requiredType.getSimpleName() : "unknown");
    return createErrorResponse(ErrorCode.INVALID_TYPE_VALUE);
  }

  // 필수 요청 파라미터 누락
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiResult<Void>> handleMissingParameter(
      MissingServletRequestParameterException e) {
    log.warn("[파라미터 누락] name={}, type={}", e.getParameterName(), e.getParameterType());
    return createErrorResponse(ErrorCode.MISSING_REQUEST_PARAMETER);
  }

  // 필수 요청 헤더 누락
  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ApiResult<Void>> handleMissingHeader(MissingRequestHeaderException e) {
    log.warn("[헤더 누락] name={}", e.getHeaderName());
    return createErrorResponse(ErrorCode.MISSING_REQUEST_HEADER);
  }

  // 요청 본문 파싱 실패 (JSON 문법 오류 등)
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResult<Void>> handleMessageNotReadable(
      HttpMessageNotReadableException e) {
    log.warn("[본문 파싱 실패] {}", e.getMessage());
    return createErrorResponse(ErrorCode.MALFORMED_REQUEST_BODY);
  }

  // 지원하지 않는 HTTP 메서드
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResult<Void>> handleMethodNotAllowed(
      HttpRequestMethodNotSupportedException e) {
    log.warn("[허용되지 않은 메서드] method={}, supported={}", e.getMethod(), e.getSupportedHttpMethods());
    return createErrorResponse(ErrorCode.METHOD_NOT_ALLOWED);
  }

  // 매핑되는 핸들러/리소스 없음 (404)
  @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
  public ResponseEntity<ApiResult<Void>> handleNotFound(Exception e) {
    log.warn("[리소스 없음] {}", e.getMessage());
    return createErrorResponse(ErrorCode.RESOURCE_NOT_FOUND);
  }

  // 잘못된 인자
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResult<Void>> handleIllegalArgument(IllegalArgumentException e) {
    log.warn("[잘못된 인자] {}", e.getMessage());
    return createErrorResponse(ErrorCode.BAD_REQUEST);
  }

  // 그 외 처리되지 않은 모든 예외
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResult<Void>> handleException(Exception e) {
    log.error("[처리되지 않은 예외] {}", e.getMessage(), e);
    return createErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR);
  }

  private String extractBindingErrors(BindingResult bindingResult) {
    return bindingResult.getFieldErrors().stream()
        .map(error -> "%s (%s)".formatted(error.getField(), error.getDefaultMessage()))
        .collect(Collectors.joining(", "));
  }

  private ResponseEntity<ApiResult<Void>> createErrorResponse(ErrorCode errorCode) {
    return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResult.fail(errorCode));
  }
}
