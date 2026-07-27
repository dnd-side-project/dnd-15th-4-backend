package com.dnd.PuzzleMeet.global.exception;

import com.dnd.PuzzleMeet.global.response.ErrorCode;
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
