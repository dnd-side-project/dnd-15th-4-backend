package com.dnd.puzzleMeet.global.exception;

import com.dnd.puzzleMeet.global.response.ErrorCode;
import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

  private final ErrorCode errorCode;

  public ApiException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public static ApiException of(ErrorCode errorCode) {
    return new ApiException(errorCode);
  }
}
