package com.yohan.event_planner.exception;

/**
 * Exception thrown when a user attempts to access or perform an action they are not authorized for.
 * Associates each error with a specific {@link ErrorCode} and generates a meaningful error message.
 */
public class UnauthorizedException extends RuntimeException implements HasErrorCode {

  private final ErrorCode errorCode;

  /**
   * Constructs a new {@code UnauthorizedException} with a specific error code and user context.
   *
   * @param errorCode the specific {@link ErrorCode} representing the authorization error
   */
  public UnauthorizedException(ErrorCode errorCode) {
    super(buildMessage(errorCode));
    this.errorCode = errorCode;
  }

  /**
   * Returns the {@link ErrorCode} associated with this unauthorized exception.
   *
   * @return the error code indicating the specific authorization-related error
   */
  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }

  /**
   * Builds a detailed error message based on the provided error code and user context.
   *
   * @param errorCode the {@link ErrorCode} describing the error
   * @return a human-readable message describing the authorization error
   */
  private static String buildMessage(ErrorCode errorCode) {
    return switch (errorCode) {
      case UNAUTHORIZED_ACCESS -> "User not authenticated";
      default -> "An unknown authorization error occurred";
    };
  }
}
