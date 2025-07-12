package com.yohan.event_planner.exception;

/**
 * Exception thrown for password-related validation errors such as weakness,
 * invalid format, length violations, null values, or duplicate password.
 * <p>
 * Implements {@link HasErrorCode} to associate an {@link ErrorCode} with the exception,
 * enabling consistent error handling and messaging.
 * </p>
 */
public class PasswordException extends RuntimeException implements HasErrorCode {

    private final ErrorCode errorCode;

    /**
     * Constructs a new {@code PasswordException} with a specific {@link ErrorCode}.
     * <p>
     * This constructor does not take the raw password string as a parameter.
     * It uses the error code to generate an appropriate error message.
     * </p>
     *
     * @param errorCode the specific {@link ErrorCode} representing the password validation error
     * @throws NullPointerException if {@code errorCode} is {@code null}
     */
    public PasswordException(ErrorCode errorCode) {
        super(buildMessage(errorCode));
        if (errorCode == null) {
            throw new NullPointerException("errorCode cannot be null");
        }
        this.errorCode = errorCode;
    }

    /**
     * Builds a consistent error message based on the provided {@link ErrorCode}.
     *
     * @param errorCode the error code indicating the password validation failure reason
     * @return the constructed error message
     */
    private static String buildMessage(ErrorCode errorCode) {
        return switch (errorCode) {
            case WEAK_PASSWORD -> "The password does not meet strength requirements.";
            case INVALID_PASSWORD_LENGTH -> "The password length is invalid.";
            case DUPLICATE_PASSWORD -> "The new password must be different from the current password.";
            case INVALID_RESET_TOKEN -> "The password reset token is invalid or has expired.";
            case EXPIRED_RESET_TOKEN -> "The password reset token has expired.";
            case USED_RESET_TOKEN -> "The password reset token has already been used.";
            case PASSWORD_RESET_TOKEN_GENERATION_FAILED -> "Failed to generate secure password reset token.";
            case PASSWORD_RESET_EMAIL_FAILED -> "Failed to send password reset email.";
            case PASSWORD_RESET_DATABASE_ERROR -> "Database error occurred during password reset operation.";
            case PASSWORD_RESET_ENCODING_FAILED -> "Failed to encode new password during reset.";
            case PASSWORD_RESET_CONFIRMATION_EMAIL_FAILED -> "Failed to send password change confirmation email.";
            default -> "Password validation error with code: " + errorCode.name();
        };
    }

    /**
     * Returns the {@link ErrorCode} associated with this password exception,
     * indicating the specific type of password validation failure.
     *
     * @return the error code for this exception
     */
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
