package com.yohan.event_planner.exception;

/**
 * Exception thrown when a user attempts to modify or delete a system-managed entity,
 * such as the default "Unlabeled" label.
 *
 * <p>This exception is used to enforce immutability rules on internally-managed
 * fallback entities that are not meant to be altered by users.</p>
 *
 * <p>Each instance is associated with a specific {@link ErrorCode}, which determines
 * the type of protected entity being accessed.</p>
 */
public class SystemManagedEntityException extends RuntimeException implements HasErrorCode {

  private final ErrorCode errorCode;

  /**
   * Constructs a new {@code SystemManagedEntityException} with the given error code.
   *
   * @param errorCode the {@link ErrorCode} indicating which type of system-managed entity was affected
   * @throws NullPointerException if {@code errorCode} is {@code null}
   */
  public SystemManagedEntityException(ErrorCode errorCode) {
    super(buildMessage(errorCode));
    if (errorCode == null) {
      throw new NullPointerException("errorCode cannot be null");
    }
    this.errorCode = errorCode;
  }

  /**
   * Returns the {@link ErrorCode} associated with this system-managed entity exception.
   *
   * @return the error code indicating the specific protected entity
   */
  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }

  /**
   * Builds a descriptive error message based on the error code.
   *
   * @param errorCode the {@link ErrorCode} describing the system-managed restriction
   * @return a human-readable message explaining the restriction
   */
  private static String buildMessage(ErrorCode errorCode) {
    if (errorCode == null) {
      return "System-managed entity validation error with null error code";
    }
    return switch (errorCode) {
      case SYSTEM_MANAGED_LABEL -> "Cannot modify or delete the user's default 'Unlabeled' label.";
      default -> "Attempted to modify or delete a system-managed entity.";
    };
  }
}
