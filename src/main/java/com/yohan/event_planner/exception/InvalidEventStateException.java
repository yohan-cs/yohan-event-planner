package com.yohan.event_planner.exception;

/**
 * Exception thrown when an event is in an invalid state for a given operation.
 *
 * <p>This includes structural or logical issues unrelated to time constraints,
 * such as missing required fields (e.g. name, label, start time, end time, etc.) during confirmation.</p>
 *
 * <p>Each instance is associated with a specific {@link ErrorCode} indicating
 * the type of state violation.</p>
 */
public class InvalidEventStateException extends RuntimeException implements HasErrorCode {

  private final ErrorCode errorCode;

  /**
   * Constructs a new {@code InvalidEventStateException} with the given error code.
   *
   * @param errorCode the {@link ErrorCode} indicating the nature of the invalid event state
   * @throws NullPointerException if {@code errorCode} is {@code null}
   */
  public InvalidEventStateException(ErrorCode errorCode) {
    super(buildMessage(errorCode));
    if (errorCode == null) {
      throw new NullPointerException("errorCode cannot be null");
    }
    this.errorCode = errorCode;
  }

  /**
   * Returns the {@link ErrorCode} associated with this invalid event state.
   *
   * @return the error code indicating the specific state violation
   */
  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }

  /**
   * Builds a descriptive error message based on the error code.
   *
   * @param errorCode the {@link ErrorCode} describing the invalid event state
   * @return a human-readable message explaining the violation
   */
  private static String buildMessage(ErrorCode errorCode) {
    if (errorCode == null) {
      return "Event state validation error with null error code";
    }
    return switch (errorCode) {
      case MISSING_EVENT_NAME -> "Event cannot be confirmed without a name.";
      case MISSING_EVENT_START_TIME -> "Event cannot be confirmed without a start time.";
      case MISSING_EVENT_END_TIME -> "Event cannot be confirmed without an end time.";
      case MISSING_EVENT_START_DATE -> "Event cannot be confirmed without a start date.";
      case MISSING_EVENT_END_DATE -> "Event cannot be confirmed without an end date.";
      case MISSING_EVENT_LABEL -> "Event cannot be confirmed without a label.";
      case MISSING_RECURRENCE_RULE -> "Event cannot be confirmed without a recurrence rule.";
      case RECAP_ON_INCOMPLETE_EVENT -> "Cannot add a recap to an event that has not been completed.";
      default -> "Event is in an invalid state for this operation.";
    };
  }
}