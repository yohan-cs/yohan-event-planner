package com.yohan.event_planner.exception;

import static com.yohan.event_planner.exception.ErrorCode.EVENT_RECAP_ALREADY_CONFIRMED;

/**
 * Thrown when attempting to confirm an event recap that is already confirmed.
 */
public class EventRecapAlreadyConfirmedException extends RuntimeException implements HasErrorCode {

  private final ErrorCode errorCode;

  public EventRecapAlreadyConfirmedException(Long eventId) {
    super("Recap for event with ID " + eventId + " is already confirmed.");
    this.errorCode = EVENT_RECAP_ALREADY_CONFIRMED;
  }

  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
