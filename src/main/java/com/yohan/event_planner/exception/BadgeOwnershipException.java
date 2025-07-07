package com.yohan.event_planner.exception;

/**
 * Exception thrown when a user attempts to access or modify a badge
 * they do not have ownership or permission for.
 * Associates each error with a specific {@link ErrorCode} and generates a meaningful error message.
 */
public class BadgeOwnershipException extends RuntimeException implements HasErrorCode {

  private final ErrorCode errorCode;

  /**
   * Constructs a new {@code BadgeOwnershipException} with a specific error code and badge ID context.
   *
   * @param badgeId the badge ID involved in the ownership violation
   * @param userId the ID of the user attempting the unauthorized action
   */
  public BadgeOwnershipException(Long badgeId, Long userId) {
    this(ErrorCode.UNAUTHORIZED_BADGE_ACCESS, badgeId);
  }

  /**
   * Constructs a new {@code BadgeOwnershipException} with the provided error code and badge ID.
   *
   * @param errorCode the specific {@link ErrorCode} representing the ownership error
   * @param badgeId the badge ID involved in the ownership violation
   * @throws NullPointerException if {@code errorCode} is {@code null}
   */
  public BadgeOwnershipException(ErrorCode errorCode, Long badgeId) {
    super(buildMessage(errorCode, badgeId));
    if (errorCode == null) {
      throw new NullPointerException("errorCode cannot be null");
    }
    this.errorCode = errorCode;
  }

  /**
   * Returns the {@link ErrorCode} associated with this ownership exception.
   *
   * @return the error code indicating the specific ownership-related error
   */
  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }

  /**
   * Builds a detailed error message based on the provided error code and badge ID context.
   *
   * @param errorCode the {@link ErrorCode} describing the error
   * @param badgeId the badge ID involved in the ownership violation
   * @return a human-readable message describing the ownership error
   */
  private static String buildMessage(ErrorCode errorCode, Long badgeId) {
    if (errorCode == null) {
      return "Badge ownership error with null error code for badge ID " + badgeId;
    }
    return switch (errorCode) {
      case UNAUTHORIZED_BADGE_ACCESS ->
              "User is not authorized to access or modify badge with ID " + badgeId;
      default -> "An unknown badge ownership error occurred";
    };
  }
}
