package com.yohan.event_planner.exception;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.RecurringEvent;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Exception thrown when a new or updated event conflicts with existing events' time ranges.
 *
 * <p>
 * Supports normal event conflicts and recurring event conflicts.
 * </p>
 *
 * <p>
 * Implements {@link HasErrorCode} to provide a standardized error code.
 * </p>
 */
public class ConflictException extends RuntimeException implements HasErrorCode {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private final ErrorCode errorCode;

    /**
     * Constructs a ConflictException for a normal event conflict.
     *
     * @param event the event that failed to create
     * @param conflictingEventIds the IDs of conflicting events
     */
    public ConflictException(Event event, Set<Long> conflictingEventIds) {
        super(buildMessage(event, conflictingEventIds));
        this.errorCode = ErrorCode.EVENT_CONFLICT;
    }

    /**
     * Constructs a ConflictException for a recurring event conflict.
     *
     * @param recurringEvent the recurring event that failed to create
     * @param conflictingEventIds the IDs of conflicting recurring events
     */
    public ConflictException(RecurringEvent recurringEvent, Set<Long> conflictingEventIds) {
        super(buildMessage(recurringEvent, conflictingEventIds));
        this.errorCode = ErrorCode.RECURRING_EVENT_CONFLICT;
    }

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    // region Message builders

    private static String buildMessage(Event event, Set<Long> conflictingEventIds) {
        return "Event '" + event.getName() + "' (ID: " + event.getId() + ") conflicts with events: " +
                conflictingEventIds;
    }

    private static String buildMessage(RecurringEvent recurringEvent, Set<Long> conflictingEventIds) {
        return "Recurring event '" + recurringEvent.getName() + "' (ID: " + recurringEvent.getId() +
                ") conflicts with recurring events: " + conflictingEventIds;
    }

    // endregion

    private static String formatDateTime(ZonedDateTime dateTime) {
        return dateTime != null
                ? dateTime.format(FORMATTER)
                : "N/A";
    }
}
