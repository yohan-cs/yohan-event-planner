package com.yohan.event_planner.exception;

import java.util.Set;

import static com.yohan.event_planner.exception.ErrorCode.INVALID_LABEL_ASSOCIATION;

/**
 * Exception thrown when invalid label IDs are provided for badge or other entity associations.
 * This is used for validation errors where label IDs in request body are invalid (non-existent or not owned).
 *
 * This differs from {@link LabelNotFoundException} which is used when fetching specific labels directly.
 * This exception indicates a validation error in request input and should result in 400 Bad Request.
 */
public class InvalidLabelAssociationException extends RuntimeException implements HasErrorCode {

    private final ErrorCode errorCode;

    /**
     * Constructs a new exception for invalid label associations.
     *
     * @param invalidIds the set of label IDs that are invalid
     */
    public InvalidLabelAssociationException(Set<Long> invalidIds) {
        super("Invalid label IDs provided: " + invalidIds + ". Labels must exist and be owned by the requesting user.");
        this.errorCode = INVALID_LABEL_ASSOCIATION;
    }

    /**
     * Constructs a new exception with a custom message.
     *
     * @param message the custom error message
     */
    public InvalidLabelAssociationException(String message) {
        super(message);
        this.errorCode = INVALID_LABEL_ASSOCIATION;
    }

    /**
     * Returns the error code associated with this exception.
     *
     * @return the INVALID_LABEL_ASSOCIATION error code
     */
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}