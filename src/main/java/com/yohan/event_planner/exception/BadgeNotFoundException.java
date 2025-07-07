package com.yohan.event_planner.exception;

import static com.yohan.event_planner.exception.ErrorCode.BADGE_NOT_FOUND;

/**
 * Exception thrown when a Badge entity is not found by ID or by name for a given user.
 *
 * Extends {@link ResourceNotFoundException} and implements {@link HasErrorCode}
 * to provide a specific error code.
 *
 * Associates the error with {@link ErrorCode#BADGE_NOT_FOUND}.
 */
public class BadgeNotFoundException extends ResourceNotFoundException implements HasErrorCode {

  private final ErrorCode errorCode;

  /**
   * Constructs a new {@code BadgeNotFoundException} for a missing badge by ID.
   *
   * @param id the badge ID that was not found
   */
  public BadgeNotFoundException(Long id) {
    super("Badge with ID " + id + " not found");
    this.errorCode = BADGE_NOT_FOUND;
  }

  /**
   * Constructs a new {@code BadgeNotFoundException} for a missing badge by name.
   *
   * @param badgeName the name of the badge that was not found
   */
  public BadgeNotFoundException(String badgeName) {
    super("Badge with name '" + badgeName + "' not found");
    this.errorCode = BADGE_NOT_FOUND;
  }

  /**
   * Constructs a new {@code BadgeNotFoundException} with a custom message.
   *
   * @param message the custom error message
   */
  public BadgeNotFoundException(String message, boolean useCustomMessage) {
    super(message);
    this.errorCode = BADGE_NOT_FOUND;
  }

  /**
   * Returns the {@link ErrorCode} associated with this exception.
   *
   * @return the {@code BADGE_NOT_FOUND} error code
   */
  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
