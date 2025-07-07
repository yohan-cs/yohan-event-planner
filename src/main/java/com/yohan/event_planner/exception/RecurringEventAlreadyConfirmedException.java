package com.yohan.event_planner.exception;

import static com.yohan.event_planner.exception.ErrorCode.RECURRING_EVENT_ALREADY_CONFIRMED;

/**
 * Thrown when attempting to confirm a recurring event that is already published.
 */
public class RecurringEventAlreadyConfirmedException extends RuntimeException implements HasErrorCode {

  private final ErrorCode errorCode;

  public RecurringEventAlreadyConfirmedException(Long recurringEventId) {
    super("Recurring event with ID " + recurringEventId + " is already confirmed.");
    this.errorCode = RECURRING_EVENT_ALREADY_CONFIRMED;
  }

  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}