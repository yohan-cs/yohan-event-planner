package com.yohan.event_planner.exception;

/**
 * Exception thrown when a user attempts to access or modify a label
 * they do not have ownership or permission for.
 * Associates each error with a specific {@link ErrorCode} and generates a meaningful error message.
 */
public class LabelOwnershipException extends RuntimeException implements HasErrorCode {

    private final ErrorCode errorCode;

    /**
     * Constructs a new {@code LabelOwnershipException} for unauthorized label access.
     *
     * @param labelId the label ID involved in the ownership violation
     * @param userId the ID of the user attempting the unauthorized action
     */
    public LabelOwnershipException(Long labelId, Long userId) {
        super(buildMessage(ErrorCode.UNAUTHORIZED_LABEL_ACCESS, labelId));
        this.errorCode = ErrorCode.UNAUTHORIZED_LABEL_ACCESS;
        // userId parameter included for future extensibility but not currently used in message
    }

    /**
     * Constructs a new {@code LabelOwnershipException} with the provided error code and label ID.
     *
     * @param errorCode the specific {@link ErrorCode} representing the ownership error
     * @param labelId the label ID involved in the ownership violation
     * @throws NullPointerException if {@code errorCode} is {@code null}
     */
    public LabelOwnershipException(ErrorCode errorCode, Long labelId) {
        super(buildMessage(errorCode, labelId));
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
     * Builds a detailed error message based on the provided error code and label ID context.
     *
     * @param errorCode the {@link ErrorCode} describing the error
     * @param labelId the label ID involved in the ownership violation
     * @return a human-readable message describing the ownership error
     */
    private static String buildMessage(ErrorCode errorCode, Long labelId) {
        if (errorCode == null) {
            return "Label ownership error with null error code for label ID " + labelId;
        }
        return switch (errorCode) {
            case UNAUTHORIZED_LABEL_ACCESS -> "User is not authorized to access or modify label with ID " + labelId;
            default -> "An unknown label ownership error occurred";
        };
    }
}
