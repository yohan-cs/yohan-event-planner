package com.yohan.event_planner.exception;

import static com.yohan.event_planner.exception.ErrorCode.RECURRING_EVENT_NOT_FOUND;

/**
 * Exception thrown when a RecurringEvent entity is not found by its ID.
 *
 * Extends {@link ResourceNotFoundException} to indicate a missing resource,
 * and implements {@link HasErrorCode} to provide a specific error code.
 *
 * Associates the error with {@link ErrorCode#RECURRING_EVENT_NOT_FOUND}.
 */
public class RecurringEventNotFoundException extends ResourceNotFoundException implements HasErrorCode {

  private final ErrorCode errorCode;

  /**
   * Constructs a new {@code RecurringEventNotFoundException} for a missing RecurringEvent by its ID.
   *
   * @param id the ID of the RecurringEvent that was not found
   */
  public RecurringEventNotFoundException(Long id) {
    super("Recurring event with ID " + id + " not found");
    this.errorCode = RECURRING_EVENT_NOT_FOUND;
  }

  /**
   * Returns the {@link ErrorCode} associated with this exception.
   *
   * @return the {@code RECURRING_EVENT_NOT_FOUND} error code
   */
  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
