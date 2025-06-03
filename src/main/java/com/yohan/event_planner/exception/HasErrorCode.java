package com.yohan.event_planner.exception;

/**
 * Interface for exceptions that are associated with a specific {@link ErrorCode}.
 * <p>
 * This allows centralized exception handlers (like {@code GlobalExceptionHandler})
 * to extract and propagate structured error codes along with the response,
 * enabling clients to perform more granular error handling.
 */
public interface HasErrorCode {

    /**
     * Retrieves the {@link ErrorCode} associated with the exception.
     *
     * @return the error code representing the specific reason for the exception
     */
    ErrorCode getErrorCode();
}
