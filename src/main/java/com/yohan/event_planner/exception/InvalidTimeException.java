package com.yohan.event_planner.exception;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Exception thrown when an event's time configuration is invalid.
 *
 * <p>This exception applies to a variety of invalid time-related conditions, including:</p>
 * <ul>
 *   <li>Start time is not before end time ({@link ErrorCode#INVALID_TIME_RANGE})</li>
 *   <li>Start and end time combination is invalid for the given context ({@link ErrorCode#INVALID_EVENT_TIME})</li>
 *   <li>Attempting to mark an event as complete before it has started or without an end time ({@link ErrorCode#INVALID_COMPLETION_STATUS})</li>
 * </ul>
 *
 * <p>Supports ZonedDateTime, LocalDate, and LocalTime for flexible use across both
 * single and recurring event logic. All formatted consistently in UTC or ISO style for readability.</p>
 */
public class InvalidTimeException extends RuntimeException implements HasErrorCode {

    private static final DateTimeFormatter ZONED_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final ErrorCode errorCode;

    public InvalidTimeException(ErrorCode errorCode, ZonedDateTime startTime, ZonedDateTime endTime) {
        this(errorCode, startTime, endTime, ZonedDateTime.now(ZoneOffset.UTC)); // fallback for legacy usage
    }

    public InvalidTimeException(ErrorCode errorCode, ZonedDateTime startTime, ZonedDateTime endTime, ZonedDateTime now) {
        super(buildZonedMessage(errorCode, startTime, endTime, now));
        this.errorCode = requireNonNull(errorCode);
    }

    public InvalidTimeException(ErrorCode errorCode, LocalDate startDate, LocalDate endDate) {
        super(buildDateMessage(errorCode, startDate, endDate));
        this.errorCode = requireNonNull(errorCode);
    }

    public InvalidTimeException(ErrorCode errorCode, LocalTime startTime, LocalTime endTime) {
        super(buildTimeMessage(errorCode, startTime, endTime));
        this.errorCode = requireNonNull(errorCode);
    }

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    private static ErrorCode requireNonNull(ErrorCode code) {
        if (code == null) {
            throw new NullPointerException("errorCode cannot be null");
        }
        return code;
    }

    private static String buildZonedMessage(ErrorCode code, ZonedDateTime start, ZonedDateTime end, ZonedDateTime now) {
        return switch (code) {
            case INVALID_TIME_RANGE, INVALID_EVENT_TIME ->
                    "Invalid event time: Start time " + format(start) + " must be before end time " + format(end) + ".";
            case INVALID_COMPLETION_STATUS ->
                    "Invalid completion status for event. Reason: " + formatCompletionReason(start, end, now);
            case INVALID_COMPLETION_FOR_DRAFT_EVENT ->
                    "Invalid completion status: Draft events cannot be marked as completed.";
            default ->
                    "Invalid time configuration (" + code.name() + "): start=" + format(start) + ", end=" + format(end);
        };
    }

    private static String buildDateMessage(ErrorCode code, LocalDate start, LocalDate end) {
        return switch (code) {
            case INVALID_EVENT_TIME, INVALID_TIME_RANGE ->
                    "Invalid date range: Start date " + format(start) + " must be before end date " + format(end) + ".";
            default ->
                    "Invalid date configuration (" + code.name() + "): start=" + format(start) + ", end=" + format(end);
        };
    }

    private static String buildTimeMessage(ErrorCode code, LocalTime start, LocalTime end) {
        return switch (code) {
            case INVALID_EVENT_TIME, INVALID_TIME_RANGE ->
                    "Invalid time range: Start time " + format(start) + " must be before end time " + format(end) + ".";
            default ->
                    "Invalid time configuration (" + code.name() + "): start=" + format(start) + ", end=" + format(end);
        };
    }

    private static String format(ZonedDateTime dt) {
        return dt != null ? dt.withZoneSameInstant(ZoneOffset.UTC).format(ZONED_FORMATTER) : "null";
    }

    private static String format(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "null";
    }

    private static String format(LocalTime time) {
        return time != null ? time.format(TIME_FORMATTER) : "null";
    }

    private static String formatCompletionReason(ZonedDateTime start, ZonedDateTime end, ZonedDateTime now) {
        if (start != null && start.isAfter(now)) {
            return "Start time " + format(start) + " is in the future.";
        } else if (end == null) {
            return "End time is missing.";
        } else if (end.isAfter(now)) {
            return "End time " + format(end) + " is in the future.";
        } else {
            return "Completion not allowed due to invalid event timing.";
        }
    }
}
