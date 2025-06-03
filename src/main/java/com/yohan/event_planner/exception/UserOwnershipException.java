package com.yohan.event_planner.exception;

/**
 * Exception thrown when a user attempts to access or modify another user
 * they do not have ownership or permission for.
 * Associates each error with a specific {@link ErrorCode} and generates a meaningful error message.
 */
public class UserOwnershipException extends RuntimeException implements HasErrorCode {

    private final ErrorCode errorCode;

    /**
     * Constructs a new {@code UserOwnershipException} with a specific error code and user ID context.
     *
     * @param errorCode the specific {@link ErrorCode} representing the ownership error
     * @param userId    the user ID involved in the ownership violation
     */
    public UserOwnershipException(ErrorCode errorCode, Long userId) {
        super(buildMessage(errorCode, userId));
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
     * Builds a detailed error message based on the provided error code and user ID context.
     *
     * @param errorCode the {@link ErrorCode} describing the error
     * @param userId    the user ID involved in the ownership violation
     * @return a human-readable message describing the ownership error
     */
    private static String buildMessage(ErrorCode errorCode, Long userId) {
        return switch (errorCode) {
            case UNAUTHORIZED_USER_ACCESS -> "User is not authorized to access or modify user with ID " + userId;
            default -> "An unknown user ownership error occurred";
        };
    }
}
