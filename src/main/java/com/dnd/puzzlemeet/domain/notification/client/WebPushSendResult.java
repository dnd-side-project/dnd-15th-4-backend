package com.dnd.puzzlemeet.domain.notification.client;

public record WebPushSendResult(Status status, Integer httpStatus) {

  public static WebPushSendResult success(int httpStatus) {
    return new WebPushSendResult(Status.SUCCESS, httpStatus);
  }

  public static WebPushSendResult expired(int httpStatus) {
    return new WebPushSendResult(Status.EXPIRED, httpStatus);
  }

  public static WebPushSendResult failed(Integer httpStatus) {
    return new WebPushSendResult(Status.FAILED, httpStatus);
  }

  public enum Status {
    SUCCESS,
    EXPIRED,
    FAILED
  }
}
