package com.yohan.event_planner.exception;

/**
 * Exception thrown when an Event entity is not found by its ID.
 *
 * Extends {@link ResourceNotFoundException} to indicate a missing resource,
 * and implements {@link HasErrorCode} to provide a specific error code.
 *
 * Associates the error with {@link ErrorCode#EVENT_NOT_FOUND}.
 */
public class EventNotFoundException extends ResourceNotFoundException implements HasErrorCode {

    private final ErrorCode errorCode;

    /**
     * Constructs a new {@code EventNotFoundException} for a missing Event by its ID.
     *
     * @param id the ID of the Event that was not found
     */
    public EventNotFoundException(Long id) {
        super("Event with ID " + id + " not found");
        this.errorCode = ErrorCode.EVENT_NOT_FOUND;
    }

    /**
     * Returns the {@link ErrorCode} associated with this exception.
     *
     * @return the {@code EVENT_NOT_FOUND} error code
     */
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
