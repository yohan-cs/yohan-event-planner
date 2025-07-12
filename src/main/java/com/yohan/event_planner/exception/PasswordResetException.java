package com.yohan.event_planner.exception;

/**
 * Exception thrown for password reset specific operations including
 * token generation failures, email service issues, database errors,
 * and other password reset workflow problems.
 * 
 * <p>
 * This exception extends {@link PasswordException} to provide more
 * specific error handling for password reset operations while maintaining
 * compatibility with the existing exception hierarchy.
 * </p>
 *
 * @see PasswordException
 * @see ErrorCode
 */
public class PasswordResetException extends PasswordException {

    /**
     * Constructs a new {@code PasswordResetException} with a specific {@link ErrorCode}.
     * 
     * <p>
     * This constructor is used for password reset specific error scenarios
     * such as token generation failures, email service unavailability,
     * database connection issues, or encoding problems.
     * </p>
     *
     * @param errorCode the specific {@link ErrorCode} representing the password reset error
     * @throws NullPointerException if {@code errorCode} is {@code null}
     */
    public PasswordResetException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * Constructs a new {@code PasswordResetException} with a specific {@link ErrorCode}
     * and a detailed message.
     * 
     * <p>
     * This constructor allows for additional context about the specific
     * failure that occurred during the password reset process.
     * </p>
     *
     * @param errorCode the specific {@link ErrorCode} representing the password reset error
     * @param message the detail message explaining the specific failure
     * @throws NullPointerException if {@code errorCode} is {@code null}
     */
    public PasswordResetException(ErrorCode errorCode, String message) {
        super(errorCode);
    }

    /**
     * Constructs a new {@code PasswordResetException} with a specific {@link ErrorCode}
     * and the underlying cause.
     * 
     * <p>
     * This constructor is useful when wrapping lower-level exceptions
     * that occur during password reset operations, such as database
     * connection failures or email service exceptions.
     * </p>
     *
     * @param errorCode the specific {@link ErrorCode} representing the password reset error
     * @param cause the underlying cause of this exception
     * @throws NullPointerException if {@code errorCode} is {@code null}
     */
    public PasswordResetException(ErrorCode errorCode, Throwable cause) {
        super(errorCode);
        initCause(cause);
    }

    /**
     * Builds a detailed error message based on the provided {@link ErrorCode}
     * for password reset specific errors.
     *
     * @param errorCode the error code indicating the password reset failure reason
     * @return the constructed error message
     */
    private static String buildPasswordResetMessage(ErrorCode errorCode) {
        return switch (errorCode) {
            case PASSWORD_RESET_TOKEN_GENERATION_FAILED -> 
                "Failed to generate secure password reset token.";
            case PASSWORD_RESET_EMAIL_FAILED -> 
                "Failed to send password reset email.";
            case PASSWORD_RESET_DATABASE_ERROR -> 
                "Database error occurred during password reset operation.";
            case PASSWORD_RESET_ENCODING_FAILED -> 
                "Failed to encode new password during reset.";
            case PASSWORD_RESET_CONFIRMATION_EMAIL_FAILED -> 
                "Failed to send password change confirmation email.";
            case INVALID_RESET_TOKEN -> 
                "The password reset token is invalid or has expired.";
            case EXPIRED_RESET_TOKEN -> 
                "The password reset token has expired.";
            case USED_RESET_TOKEN -> 
                "The password reset token has already been used.";
            default -> "Password reset error with code: " + errorCode.name();
        };
    }
}