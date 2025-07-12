package com.yohan.event_planner.exception;

/**
 * Represents a structured error response sent back to the client
 * when an exception is thrown within the application.
 *
 * This record holds:
 * - HTTP status code
 * - Human-readable error message
 * - Optional error code for machine-readable error identification
 * - Timestamp of when the error occurred
 * - Request path where the error occurred
 */
public record ErrorResponse(int status, String message, String errorCode, long timestamp, String path) {

    // Convenience constructor for backward compatibility
    public ErrorResponse(int status, String message, long timestamp) {
        this(status, message, null, timestamp, null);
    }
    
    // Constructor with path but no error code
    public ErrorResponse(int status, String message, long timestamp, String path) {
        this(status, message, null, timestamp, path);
    }
}