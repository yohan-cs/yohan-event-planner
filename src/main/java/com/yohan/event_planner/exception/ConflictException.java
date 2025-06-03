package com.yohan.event_planner.exception;


import com.yohan.event_planner.domain.Event;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Exception thrown when a new or updated event conflicts with an existing event's time range.
 * <p>
 * This exception includes details about the conflicting event, such as its ID, name,
 * and the start and end times, to aid in debugging and user feedback.
 * <p>
 * Implements {@link HasErrorCode} to provide a standardized error code.
 */
public class ConflictException extends RuntimeException implements HasErrorCode {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private final ErrorCode errorCode;

    /**
     * Constructs a new {@code ConflictException} with the provided conflicting event.
     *
     * @param existingEvent the event that causes the scheduling conflict
     */
    public ConflictException(Event existingEvent) {
        super(buildMessage(existingEvent));
        this.errorCode = ErrorCode.EVENT_CONFLICT;
    }

    /**
     * Returns the {@link ErrorCode} associated with this conflict exception.
     *
     * @return the error code indicating a scheduling conflict
     */
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Builds a detailed error message including event ID, name, and time range.
     *
     * @param existingEvent the conflicting event
     * @return a human-readable error message
     */
    private static String buildMessage(Event existingEvent) {
        return "Event conflicts with existing event (ID: " + existingEvent.getId() + "): " +
                existingEvent.getName() + " (" +
                formatDateTime(existingEvent.getStartTime()) + " - " +
                formatDateTime(existingEvent.getEndTime()) + ")";
    }

    private static String formatDateTime(ZonedDateTime dateTime) {
        return dateTime.format(FORMATTER);
    }
}
