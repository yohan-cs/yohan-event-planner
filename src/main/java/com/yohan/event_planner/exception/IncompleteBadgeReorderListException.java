package com.yohan.event_planner.exception;

/**
 * Exception thrown when a badge reorder operation is missing required badges
 * or includes an incomplete list of the user's badges.
 *
 * <p>This ensures clients provide a full and valid reorder list when reordering badges.</p>
 *
 * <p>Each instance is associated with {@link ErrorCode#INCOMPLETE_BADGE_REORDER_LIST}
 * to allow consistent error handling.</p>
 */
public class IncompleteBadgeReorderListException extends RuntimeException implements HasErrorCode {

    private final ErrorCode errorCode;

    /**
     * Constructs a new {@code IncompleteReorderListException} using the standard error code.
     */
    public IncompleteBadgeReorderListException() {
        super(buildMessage());
        this.errorCode = ErrorCode.INCOMPLETE_BADGE_REORDER_LIST;
    }

    /**
     * Returns the {@link ErrorCode} associated with this exception.
     *
     * @return the error code indicating the specific reorder list violation
     */
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Builds a descriptive error message for this exception.
     *
     * @return a human-readable message explaining the reorder list violation
     */
    private static String buildMessage() {
        return "Reorder list must include all badges owned by the user.";
    }
}