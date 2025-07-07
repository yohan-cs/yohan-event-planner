package com.yohan.event_planner.exception;

/**
 * Exception thrown when a recurrence rule is invalid or incomplete.
 *
 * <p>This includes issues like missing frequency, unsupported combinations (e.g. DAILY with dayOfWeek),
 * or invalid monthly ordinals.</p>
 *
 * <p>Each instance is associated with a specific {@link ErrorCode} indicating
 * the type of recurrence rule violation.</p>
 */
public class InvalidRecurrenceRuleException extends RuntimeException implements HasErrorCode {

    private final ErrorCode errorCode;

    /**
     * Constructs a new {@code InvalidRecurrenceRuleException} with the given error code.
     *
     * @param errorCode the {@link ErrorCode} indicating the nature of the recurrence rule violation
     * @throws NullPointerException if {@code errorCode} is {@code null}
     */
    public InvalidRecurrenceRuleException(ErrorCode errorCode) {
        super(buildMessage(errorCode));
        if (errorCode == null) {
            throw new NullPointerException("errorCode cannot be null");
        }
        this.errorCode = errorCode;
    }

    /**
     * Returns the {@link ErrorCode} associated with this invalid recurrence rule.
     *
     * @return the error code indicating the specific rule violation
     */
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Builds a descriptive error message based on the error code.
     *
     * @param errorCode the {@link ErrorCode} describing the rule violation
     * @return a human-readable message explaining the violation
     */
    private static String buildMessage(ErrorCode errorCode) {
        if (errorCode == null) {
            return "Recurrence rule validation error with null error code";
        }
        return switch (errorCode) {
            case MISSING_RECURRENCE_FREQUENCY -> "Recurrence rule must specify a frequency.";
            case WEEKLY_MISSING_DAYS -> "Weekly recurrence must specify at least one day.";
            case WEEKLY_INVALID_DAY -> "The recurrence rule contains an invalid day of the week.";
            case MONTHLY_MISSING_ORDINAL_OR_DAY -> "Monthly recurrence must include both ordinal and day(s).";
            case MONTHLY_INVALID_ORDINAL -> "The recurrence rule contains an invalid ordinal value (must be between 1 and 4).";
            case MONTHLY_INVALID_DAY -> "The recurrence rule contains an invalid day for monthly recurrence.";
            case UNSUPPORTED_RECURRENCE_COMBINATION -> "The recurrence rule combination is not supported.";
            default -> "Recurrence rule is invalid or incomplete.";
        };
    }
}
