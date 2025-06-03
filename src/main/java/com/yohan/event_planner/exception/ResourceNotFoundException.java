package com.yohan.event_planner.exception;

/**
 * Generic exception thrown when a requested resource
 * cannot be found in the system.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a ResourceNotFoundException with the specified detail message.
     *
     * @param message the detail message explaining which resource was not found
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
