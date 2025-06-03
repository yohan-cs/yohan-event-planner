package com.yohan.event_planner.exception;

/**
 * Exception thrown when a user attempts to access or modify an event
 * they do not have ownership or permission for.
 * Associates each error with a specific {@link ErrorCode} and generates a meaningful error message.
 */
public class EventOwnershipException extends RuntimeException implements HasErrorCode {

    private final ErrorCode errorCode;

    /**
     * Constructs a new {@code EventOwnershipException} with a specific error code and event ID context.
     *
     * @param errorCode the specific {@link ErrorCode} representing the ownership error
     * @param eventId the event ID involved in the ownership violation
     */
    public EventOwnershipException(ErrorCode errorCode, Long eventId) {
        super(buildMessage(errorCode, eventId));
        this.errorCode = errorCode;
    }

    /**
     * Returns the {@link ErrorCode} associated with this ownership exception.
     *
     * @return the error code indicating the specific ownership-related error
     */
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Builds a detailed error message based on the provided error code and event ID context.
     *
     * @param errorCode the {@link ErrorCode} describing the error
     * @param eventId the event ID involved in the ownership violation
     * @return a human-readable message describing the ownership error
     */
    private static String buildMessage(ErrorCode errorCode, Long eventId) {
        return switch (errorCode) {
            case UNAUTHORIZED_EVENT_ACCESS -> "User is not authorized to access or modify event with ID " + eventId;
            default -> "An unknown event ownership error occurred";
        };
    }
}
