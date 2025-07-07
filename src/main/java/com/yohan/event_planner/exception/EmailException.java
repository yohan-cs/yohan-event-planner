package com.yohan.event_planner.exception;


/**
 * Exception thrown for email-related validation errors such as duplicates,
 * invalid format, or invalid length.
 * <p>
 * Implements {@link HasErrorCode} to associate an {@link ErrorCode} with the exception,
 * enabling consistent error reporting.
 * </p>
 */
public class EmailException extends RuntimeException implements HasErrorCode {

    private final ErrorCode errorCode;

    /**
     * Constructs a new {@code EmailException} with a specific {@link ErrorCode}
     * and the email string involved in the error.
     * <p>
     * This constructor uses the error code and email to generate an appropriate error message.
     * </p>
     *
     * @param errorCode the specific {@link ErrorCode} representing the email validation error
     * @param email     the email string related to the error condition (may be {@code null})
     * @throws NullPointerException if {@code errorCode} is {@code null}
     */
    public EmailException(ErrorCode errorCode, String email) {
        super(buildMessage(errorCode, email));
        if (errorCode == null) {
            throw new NullPointerException("errorCode cannot be null");
        }
        this.errorCode = errorCode;
    }

    /**
     * Returns the {@link ErrorCode} associated with this email exception,
     * indicating the specific type of email validation failure.
     *
     * @return the error code for this exception
     */
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Builds a detailed error message based on the provided error code and email context.
     *
     * @param errorCode the {@link ErrorCode} describing the email error
     * @param email     the email string involved in the error condition, can be {@code null}
     * @return a human-readable message describing the email error
     */
    private static String buildMessage(ErrorCode errorCode, String email) {
        return switch (errorCode) {
            case DUPLICATE_EMAIL -> "The email '" + email + "' is already registered.";
            case INVALID_EMAIL_LENGTH -> "The email '" + email + "' does not meet the length requirements.";
            case INVALID_EMAIL_FORMAT -> "The email '" + email + "' has an invalid format.";
            default -> "Email validation error with code: " + errorCode.name();
        };
    }
}
