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
}