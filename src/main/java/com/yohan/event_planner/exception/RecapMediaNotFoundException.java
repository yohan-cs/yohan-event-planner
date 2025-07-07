package com.yohan.event_planner.exception;

/**
 * Exception thrown when a RecapMedia entity is not found by its ID.
 *
 * Extends {@link ResourceNotFoundException} to indicate a missing resource,
 * and implements {@link HasErrorCode} to provide a specific error code.
 *
 * Associates the error with {@link ErrorCode#RECAP_MEDIA_NOT_FOUND}.
 */
public class RecapMediaNotFoundException extends ResourceNotFoundException implements HasErrorCode {

  private final ErrorCode errorCode;

  /**
   * Constructs a new {@code RecapMediaNotFoundException} for a missing RecapMedia by its ID.
   *
   * @param id the ID of the RecapMedia that was not found
   */
  public RecapMediaNotFoundException(Long id) {
    super("Recap media with ID " + id + " not found");
    this.errorCode = ErrorCode.RECAP_MEDIA_NOT_FOUND;
  }

  /**
   * Returns the {@link ErrorCode} associated with this exception.
   *
   * @return the {@code RECAP_MEDIA_NOT_FOUND} error code
   */
  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
