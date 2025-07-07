package com.yohan.event_planner.exception;

import static com.yohan.event_planner.exception.ErrorCode.EVENT_ALREADY_CONFIRMED;

/**
 * Thrown when attempting to confirm an event that is already published.
 */
public class EventAlreadyConfirmedException extends RuntimeException implements HasErrorCode {

  private final ErrorCode errorCode;

  public EventAlreadyConfirmedException(Long eventId) {
    super("Event with ID " + eventId + " is already confirmed.");
    this.errorCode = EVENT_ALREADY_CONFIRMED;
  }

  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
