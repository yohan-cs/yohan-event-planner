package com.yohan.event_planner.exception;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Exception thrown when an attempt is made to add or remove invalid skip days,
 * such as past skip days that cannot be added or removed.
 *
 * <p>Each instance is associated with a specific {@link ErrorCode}
 * indicating the type of violation.</p>
 */
public class InvalidSkipDayException extends RuntimeException implements HasErrorCode {

    private final ErrorCode errorCode;
    private final Set<LocalDate> invalidDates;

    /**
     * Constructs a new {@code InvalidSkipDayException} with the given error code
     * and the set of invalid skip days that caused the exception.
     *
     * @param errorCode the {@link ErrorCode} indicating the nature of the violation
     * @param invalidDates the set of invalid skip days that triggered the exception
     * @throws NullPointerException if {@code errorCode} is {@code null}
     */
    public InvalidSkipDayException(ErrorCode errorCode, Set<LocalDate> invalidDates) {
        super(buildMessage(errorCode, invalidDates));
        if (errorCode == null) {
            throw new NullPointerException("errorCode cannot be null");
        }
        this.errorCode = errorCode;
        this.invalidDates = invalidDates;
    }

    /**
     * Returns the {@link ErrorCode} associated with this exception.
     *
     * @return the error code indicating the specific violation
     */
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the set of invalid skip days that triggered this exception.
     *
     * @return the set of invalid dates
     */
    public Set<LocalDate> getInvalidDates() {
        return invalidDates;
    }

    /**
     * Builds a descriptive error message based on the error code and invalid dates.
     *
     * @param errorCode the {@link ErrorCode} describing the violation
     * @param invalidDates the set of invalid skip days
     * @return a human-readable message explaining the violation
     */
    private static String buildMessage(ErrorCode errorCode, Set<LocalDate> invalidDates) {
        if (errorCode == null) {
            return "Skip day validation error with null error code";
        }
        String dateList = invalidDates != null 
            ? invalidDates.stream()
                .map(date -> date == null ? "null" : date.toString())
                .collect(Collectors.joining(", "))
            : "null";
        return switch (errorCode) {
            case INVALID_SKIP_DAY_REMOVAL ->
                    "Cannot remove past skip days: " + dateList + ". Only today or future skip days can be removed.";
            case INVALID_SKIP_DAY_ADDITION ->
                    "Cannot add past skip days: " + dateList + ". Only today or future skip days can be added.";
            default ->
                    "Invalid skip day operation request.";
        };
    }
}
