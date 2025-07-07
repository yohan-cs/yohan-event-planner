package com.yohan.event_planner.exception;

/**
 * Exception thrown for username-related validation errors such as duplicates,
 * invalid length, or null values.
 * <p>
 * Implements {@link HasErrorCode} to associate an {@link ErrorCode} with the exception,
 * enabling consistent error handling and messaging.
 * </p>
 */
public class UsernameException extends RuntimeException implements HasErrorCode {

    private final ErrorCode errorCode;

    /**
     * Constructs a new {@code UsernameException} with a specific {@link ErrorCode}
     * and the username involved in the error.
     * <p>
     * This constructor uses the error code and username to generate an appropriate error message.
     * </p>
     *
     * @param errorCode the specific {@link ErrorCode} representing the username validation error
     * @param username  the username related to the error condition (may be {@code null})
     * @throws NullPointerException if {@code errorCode} is {@code null}
     */
    public UsernameException(ErrorCode errorCode, String username) {
        super(buildMessage(errorCode, username));
        if (errorCode == null) {
            throw new NullPointerException("errorCode cannot be null");
        }
        this.errorCode = errorCode;
    }

    /**
     * Returns the {@link ErrorCode} associated with this username exception,
     * indicating the specific type of username validation failure.
     *
     * @return the error code for this exception
     */
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Builds a detailed error message based on the provided error code and username context.
     *
     * @param errorCode the {@link ErrorCode} describing the username error
     * @param username  the username involved in the error condition, can be {@code null}
     * @return a human-readable message describing the username error
     */
    private static String buildMessage(ErrorCode errorCode, String username) {
        return switch (errorCode) {
            case DUPLICATE_USERNAME -> "User with username '" + username + "' already exists.";
            case INVALID_USERNAME_LENGTH -> "The username '" + username + "' does not meet the length requirements.";
            default -> "Username validation error with code: " + errorCode.name();
        };
    }
}
