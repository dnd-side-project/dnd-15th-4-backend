package com.dnd.puzzlemeet.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ErrorCode {

  // 400 Bad Request
  BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "요청 파라미터가 올바르지 않습니다."),
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "입력값 검증에 실패했습니다."),
  INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "INVALID_TYPE_VALUE", "요청 값의 타입이 올바르지 않습니다."),
  MISSING_REQUEST_PARAMETER(
      HttpStatus.BAD_REQUEST, "MISSING_REQUEST_PARAMETER", "필수 요청 파라미터가 누락되었습니다."),
  MISSING_REQUEST_HEADER(HttpStatus.BAD_REQUEST, "MISSING_REQUEST_HEADER", "필수 요청 헤더가 누락되었습니다."),
  MALFORMED_REQUEST_BODY(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST_BODY", "요청 본문을 읽을 수 없습니다."),
  MEETING_NOT_WAITING(HttpStatus.BAD_REQUEST, "MEETING_NOT_WAITING", "대기 중인 약속만 수정하거나 삭제할 수 있습니다."),
  MEETING_NOT_JOINABLE(HttpStatus.BAD_REQUEST, "MEETING_NOT_JOINABLE", "대기 중인 약속에만 참여할 수 있습니다."),
  MEETING_MEMBER_ALREADY_JOINED(
      HttpStatus.BAD_REQUEST, "MEETING_MEMBER_ALREADY_JOINED", "이미 참여한 약속입니다."),
  MEETING_NOT_STARTED(HttpStatus.BAD_REQUEST, "MEETING_NOT_STARTED", "아직 시작되지 않은 약속입니다."),
  MEETING_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "MEETING_NOT_COMPLETED", "완료된 약속만 결과를 조회할 수 있습니다."),

  // 401 Unauthorized
  AUTH_KAKAO_UNAUTHORIZED(
      HttpStatus.UNAUTHORIZED, "AUTH_KAKAO_UNAUTHORIZED", "카카오 access token이 유효하지 않습니다."),
  AUTH_KAKAO_CODE_INVALID(
      HttpStatus.UNAUTHORIZED, "AUTH_KAKAO_CODE_INVALID", "카카오 인가 코드가 유효하지 않거나 이미 사용되었습니다."),
  AUTH_KAKAO_LOGIN_CANCELED(
      HttpStatus.UNAUTHORIZED, "AUTH_KAKAO_LOGIN_CANCELED", "카카오 로그인이 취소되었습니다."),
  AUTH_OAUTH_STATE_MISMATCH(
      HttpStatus.UNAUTHORIZED, "AUTH_OAUTH_STATE_MISMATCH", "로그인 요청을 확인할 수 없습니다. 처음부터 다시 시도해 주세요."),
  AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_INVALID", "유효하지 않은 인증 토큰입니다."),
  AUTH_REFRESH_TOKEN_INVALID(
      HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_TOKEN_INVALID", "유효하지 않은 refresh token입니다."),
  AUTH_REFRESH_TOKEN_EXPIRED(
      HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_TOKEN_EXPIRED", "만료된 refresh token입니다."),

  // 403 Forbidden
  AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN", "접근 권한이 없습니다."),
  AUTH_KAKAO_PROFILE_REQUIRED(
      HttpStatus.FORBIDDEN, "AUTH_KAKAO_PROFILE_REQUIRED", "카카오 프로필 정보 제공에 동의해야 합니다."),

  // 404 Not Found
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 사용자입니다."),
  MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND", "존재하지 않는 약속입니다."),
  MEETING_INVITE_CODE_INVALID(
      HttpStatus.NOT_FOUND, "MEETING_INVITE_CODE_INVALID", "유효하지 않은 초대 코드입니다."),
  REACTION_PRESET_NOT_FOUND(
      HttpStatus.NOT_FOUND, "REACTION_PRESET_NOT_FOUND", "존재하지 않거나 비활성화된 리액션 프리셋입니다."),

  // 405 Method Not Allowed
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "허용되지 않은 HTTP 메서드입니다."),

  // 415 Unsupported Media Type
  UNSUPPORTED_MEDIA_TYPE(
      HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 Content-Type입니다."),

  // 500 Internal Server Error
  INTERNAL_SERVER_ERROR(
      HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "알 수 없는 서버 오류가 발생했습니다."),
  S3_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3_UPLOAD_FAILED", "파일 업로드에 실패했습니다."),

  // 502 Bad Gateway
  AUTH_KAKAO_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "AUTH_KAKAO_UNAVAILABLE", "카카오 서버와 통신할 수 없습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
