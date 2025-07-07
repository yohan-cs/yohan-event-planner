package com.yohan.event_planner.exception;

import java.util.Set;

import static com.yohan.event_planner.exception.ErrorCode.LABEL_NOT_FOUND;

/**
 * Exception thrown when a Label entity is not found by ID or by name for a given user.
 *
 * Extends {@link ResourceNotFoundException} and implements {@link HasErrorCode}
 * to provide a specific error code.
 *
 * Associates the error with {@link ErrorCode#LABEL_NOT_FOUND}.
 */
public class LabelNotFoundException extends ResourceNotFoundException implements HasErrorCode {

  private final ErrorCode errorCode;

  /**
   * Constructs a new {@code LabelNotFoundException} for a missing label by ID.
   *
   * @param id the label ID that was not found
   */
  public LabelNotFoundException(Long id) {
    super("Label with ID " + id + " not found");
    this.errorCode = LABEL_NOT_FOUND;
  }

  /**
   * Constructs a new {@code LabelNotFoundException} for a missing label by name.
   *
   * @param labelName the name of the label that was not found
   */
  public LabelNotFoundException(String labelName) {
    super("Label with name '" + labelName + "' not found");
    this.errorCode = LABEL_NOT_FOUND;
  }

  /**
   * Constructs a new {@code LabelNotFoundException} for multiple missing label IDs.
   *
   * @param ids the set of label IDs that were not found
   */
  public LabelNotFoundException(Set<Long> ids) {
    super("Labels with IDs " + ids + " not found");
    this.errorCode = LABEL_NOT_FOUND;
  }

  /**
   * Constructs a new {@code LabelNotFoundException} with a custom message.
   *
   * @param message the custom error message
   */
  public LabelNotFoundException(String message, boolean useCustomMessage) {
    super(message);
    this.errorCode = LABEL_NOT_FOUND;
  }

  /**
   * Returns the {@link ErrorCode} associated with this exception.
   *
   * @return the {@code LABEL_NOT_FOUND} error code
   */
  @Override
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
