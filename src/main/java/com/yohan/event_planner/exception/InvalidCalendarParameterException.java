package com.yohan.event_planner.exception;

/**
 * Exception thrown for calendar-related parameter validation errors such as invalid
 * month values, year values, pagination parameters, or other date-related parameters.
 * <p>
 * Implements {@link HasErrorCode} to associate an {@link ErrorCode} with the exception,
 * enabling consistent error handling and messaging. This exception results in a 
 * 400 Bad Request HTTP status when handled by the global exception handler.
 * </p>
 * 
 * <h2>Common Use Cases</h2>
 * <ul>
 *   <li><strong>Month Validation</strong>: Values outside 1-12 range</li>
 *   <li><strong>Year Validation</strong>: Non-positive year values</li>
 *   <li><strong>Pagination Parameters</strong>: Invalid limit or offset values</li>
 *   <li><strong>Date Range Parameters</strong>: Malformed or inconsistent date parameters</li>
 * </ul>
 */
public class InvalidCalendarParameterException extends RuntimeException implements HasErrorCode {

    private final ErrorCode errorCode;

    /**
     * Constructs a new {@code InvalidCalendarParameterException} with a specific {@link ErrorCode}.
     * <p>
     * This constructor uses the error code to generate an appropriate error message
     * based on the type of calendar parameter validation failure.
     * </p>
     *
     * @param errorCode the specific {@link ErrorCode} representing the calendar parameter validation error
     * @throws NullPointerException if {@code errorCode} is {@code null}
     */
    public InvalidCalendarParameterException(ErrorCode errorCode) {
        super(buildMessage(errorCode));
        if (errorCode == null) {
            throw new NullPointerException("errorCode cannot be null");
        }
        this.errorCode = errorCode;
    }

    /**
     * Returns the {@link ErrorCode} associated with this calendar parameter exception,
     * indicating the specific type of parameter validation failure.
     *
     * @return the error code for this exception
     */
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Builds a consistent error message based on the provided {@link ErrorCode}.
     *
     * @param errorCode the error code indicating the calendar parameter validation failure reason
     * @return the constructed error message
     */
    private static String buildMessage(ErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_CALENDAR_PARAMETER -> "The calendar parameter is invalid.";
            case INVALID_PAGINATION_PARAMETER -> "The pagination parameter is invalid.";
            default -> "Calendar parameter validation error with code: " + errorCode.name();
        };
    }
}