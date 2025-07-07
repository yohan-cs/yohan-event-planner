package com.yohan.event_planner.exception;

import static com.yohan.event_planner.exception.ErrorCode.EVENT_RECAP_NOT_FOUND;

/**
 * Exception thrown when an {@link com.yohan.event_planner.domain.EventRecap}
 * is not found for a given event ID.
 *
 * <p>
 * Extends {@link ResourceNotFoundException} and implements {@link HasErrorCode}
 * to provide a specific error code.
 * </p>
 *
 * Associates the error with {@link ErrorCode#EVENT_RECAP_NOT_FOUND}.
 */
public class EventRecapNotFoundException extends ResourceNotFoundException implements HasErrorCode {

    private final ErrorCode errorCode;

    /**
     * Constructs a new {@code EventRecapNotFoundException} for a missing recap by event ID.
     *
     * @param eventId the ID of the event whose recap was not found
     */
    public EventRecapNotFoundException(Long eventId) {
        super("Recap for event with ID " + eventId + " not found");
        this.errorCode = EVENT_RECAP_NOT_FOUND;
    }

    /**
     * Constructs a new {@code EventRecapNotFoundException} with a custom message.
     *
     * @param message the custom error message
     */
    public EventRecapNotFoundException(String message, boolean useCustomMessage) {
        super(message);
        this.errorCode = EVENT_RECAP_NOT_FOUND;
    }

    /**
     * Returns the {@link ErrorCode} associated with this exception.
     *
     * @return the {@code EVENT_RECAP_NOT_FOUND} error code
     */
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
