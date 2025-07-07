package com.yohan.event_planner.exception;

/**
 * Exception thrown for event recap-related validation errors such as duplicates
 * or other state-related constraints.
 *
 * <p>
 * Implements {@link HasErrorCode} to associate an {@link ErrorCode} with the exception,
 * enabling consistent error handling and messaging.
 * </p>
 */
public class EventRecapException extends RuntimeException implements HasErrorCode {

  private final ErrorCode errorCode;

  /**
   * Constructs a new {@code EventRecapException} with a specific {@link ErrorCode}
   * and the event ID involved in the error.
   *
   * <p>
   * This constructor uses the error code and event ID to generate an appropriate error message.
   * </p>
   *
   * @param errorCode the specific {@link ErrorCode} representing the event recap validation error
   * @param eventId   the ID of the event related to the error condition (may be {@code null})
   * @throws NullPointerException if {@code errorCode} is {@code null}
   */
  public EventRecapException(ErrorCode errorCode, Long eventId) {
    super(buildMessage(errorCode, eventId));
    if (errorCode == null) {
      throw new NullPointerException("errorCode cannot be null");
    }
    this.errorCode = errorCode;
  }

  /**
   * Returns the {@link ErrorCode} associated with this recap exception,
   * indicating the specific type of validation failure.
   *
   * @return the error code for this exception
   */
  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }

  /**
   * Builds a detailed error message based on the provided error code and event context.
   *
   * @param errorCode the {@link ErrorCode} describing the recap error
   * @param eventId   the event ID involved in the error condition, can be {@code null}
   * @return a human-readable message describing the recap error
   */
  private static String buildMessage(ErrorCode errorCode, Long eventId) {
    return switch (errorCode) {
      case DUPLICATE_EVENT_RECAP ->
              "Recap already exists for event with ID " + eventId + ".";
      default ->
              "Event recap validation error with code: " + errorCode.name();
    };
  }
}
