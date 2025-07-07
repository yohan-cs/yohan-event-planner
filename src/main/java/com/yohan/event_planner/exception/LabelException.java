package com.yohan.event_planner.exception;

/**
 * Exception thrown for label-related validation errors such as duplicates,
 * invalid names, or null values.
 * <p>
 * Implements {@link HasErrorCode} to associate an {@link ErrorCode} with the exception,
 * enabling consistent error handling and messaging.
 * </p>
 */
public class LabelException extends RuntimeException implements HasErrorCode {

    private final ErrorCode errorCode;

    /**
     * Constructs a new {@code LabelException} with a specific {@link ErrorCode}
     * and the label name involved in the error.
     * <p>
     * This constructor uses the error code and label name to generate an appropriate error message.
     * </p>
     *
     * @param errorCode the specific {@link ErrorCode} representing the label validation error
     * @param labelName the label name related to the error condition (may be {@code null})
     * @throws NullPointerException if {@code errorCode} is {@code null}
     */
    public LabelException(ErrorCode errorCode, String labelName) {
        super(buildMessage(errorCode, labelName));
        if (errorCode == null) {
            throw new NullPointerException("errorCode cannot be null");
        }
        this.errorCode = errorCode;
    }

    /**
     * Returns the {@link ErrorCode} associated with this label exception,
     * indicating the specific type of label validation failure.
     *
     * @return the error code for this exception
     */
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Builds a detailed error message based on the provided error code and label context.
     *
     * @param errorCode the {@link ErrorCode} describing the label error
     * @param labelName the label name involved in the error condition, can be {@code null}
     * @return a human-readable message describing the label error
     */
    private static String buildMessage(ErrorCode errorCode, String labelName) {
        return switch (errorCode) {
            case DUPLICATE_LABEL -> "Label with name '" + labelName + "' already exists.";
            default -> "Label validation error with code: " + errorCode.name();
        };
    }
}
