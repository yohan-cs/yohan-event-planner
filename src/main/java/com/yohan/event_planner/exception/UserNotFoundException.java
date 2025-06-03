package com.yohan.event_planner.exception;

/**
 * Exception thrown when a User entity is not found by either ID, username,
 * or when the authenticated user is missing from the security context.
 *
 * Extends {@link ResourceNotFoundException} and implements {@link HasErrorCode}
 * to provide a specific error code.
 *
 * Associates the error with {@link ErrorCode#USER_NOT_FOUND}.
 */
public class UserNotFoundException extends ResourceNotFoundException implements HasErrorCode {

  private final ErrorCode errorCode;

  /**
   * Constructs a new {@code UserNotFoundException} for a missing user by ID.
   *
   * @param id the user ID that was not found
   */
  public UserNotFoundException(Long id) {
    super("User with ID " + id + " not found");
    this.errorCode = ErrorCode.USER_NOT_FOUND;
  }

  /**
   * Constructs a new {@code UserNotFoundException} for a missing user by username.
   *
   * @param username the username that was not found
   */
  public UserNotFoundException(String username) {
    super("User with username " + username + " not found");
    this.errorCode = ErrorCode.USER_NOT_FOUND;
  }

  /**
   * Constructs a new {@code UserNotFoundException} when the authenticated user
   * is not present in the security context.
   */
  public UserNotFoundException() {
    super("Authenticated user not found");
    this.errorCode = ErrorCode.USER_NOT_FOUND;
  }

  /**
   * Returns the {@link ErrorCode} associated with this exception.
   *
   * @return the {@code USER_NOT_FOUND} error code
   */
  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
