package com.yohan.event_planner.exception;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Exception thrown when event start and end times are invalid,
 * specifically when the start time is not before the end time.
 * Associates the error with {@link ErrorCode#INVALID_EVENT_TIME}.
 */
public class InvalidTimeException extends RuntimeException implements HasErrorCode {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private final ErrorCode errorCode;

    /**
     * Constructs a new {@code InvalidTimeException} with the provided start and end times.
     *
     * @param startTime the event start time
     * @param endTime   the event end time
     */
    public InvalidTimeException(ZonedDateTime startTime, ZonedDateTime endTime) {
        super(buildMessage(startTime, endTime));
        this.errorCode = ErrorCode.INVALID_EVENT_TIME;
    }

    /**
     * Returns the {@link ErrorCode} associated with this time validation exception.
     *
     * @return the error code indicating the invalid time condition
     */
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Builds a detailed error message indicating the invalid time range.
     *
     * @param startTime the invalid start time
     * @param endTime   the invalid end time
     * @return a human-readable error message
     */
    private static String buildMessage(ZonedDateTime startTime, ZonedDateTime endTime) {
        return "Invalid event time: Start time " + formatDateTime(startTime) +
                " must be before end time " + formatDateTime(endTime) + ".";
    }

    /**
     * Formats a ZonedDateTime into a human-readable string with pattern yyyy-MM-dd HH:mm:ss z.
     *
     * @param dateTime the date and time to format
     * @return formatted date-time string
     */
    private static String formatDateTime(ZonedDateTime dateTime) {
        return dateTime.format(FORMATTER);
    }
}
