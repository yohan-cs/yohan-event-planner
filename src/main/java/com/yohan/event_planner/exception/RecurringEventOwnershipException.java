package com.yohan.event_planner.exception;

/**
 * Exception thrown when a user attempts to access or modify a recurring event
 * they do not have ownership or permission for.
 * Associates each error with a specific {@link ErrorCode} and generates a meaningful error message.
 */
public class RecurringEventOwnershipException extends RuntimeException implements HasErrorCode {

    private final ErrorCode errorCode;

    /**
     * Constructs a new {@code RecurringEventOwnershipException} with a specific error code and recurring event ID context.
     *
     * @param errorCode the specific {@link ErrorCode} representing the ownership error
     * @param recurringEventId the recurring event ID involved in the ownership violation
     * @throws NullPointerException if {@code errorCode} is {@code null}
     */
    public RecurringEventOwnershipException(ErrorCode errorCode, Long recurringEventId) {
        super(buildMessage(errorCode, recurringEventId));
        if (errorCode == null) {
            throw new NullPointerException("errorCode cannot be null");
        }
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
     * Builds a detailed error message based on the provided error code and recurring event ID context.
     *
     * @param errorCode the {@link ErrorCode} describing the error
     * @param recurringEventId the recurring event ID involved in the ownership violation
     * @return a human-readable message describing the ownership error
     */
    private static String buildMessage(ErrorCode errorCode, Long recurringEventId) {
        if (errorCode == null) {
            return "Recurring event ownership error with null error code for recurring event ID " + recurringEventId;
        }
        return switch (errorCode) {
            case UNAUTHORIZED_RECURRING_EVENT_ACCESS ->
                    "User is not authorized to access or modify recurring event with ID " + recurringEventId;
            default -> "An unknown recurring event ownership error occurred";
        };
    }
}
