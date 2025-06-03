package com.yohan.event_planner.exception;

/**
 * Exception thrown when a Role entity is not found by either ID or role name.
 *
 * Extends {@link ResourceNotFoundException} and implements {@link HasErrorCode}
 * to provide a specific error code.
 *
 * Associates the error with {@link ErrorCode#ROLE_NOT_FOUND}.
 */
public class RoleNotFoundException extends ResourceNotFoundException implements HasErrorCode {

    private final ErrorCode errorCode;

    /**
     * Constructs a new {@code RoleNotFoundException} for a missing role by ID.
     *
     * @param id the role ID that was not found
     */
    public RoleNotFoundException(Long id) {
        super("Role with ID " + id + " not found");
        this.errorCode = ErrorCode.ROLE_NOT_FOUND;
    }

    /**
     * Constructs a new {@code RoleNotFoundException} for a missing role by name.
     *
     * @param roleName the role name that was not found
     */
    public RoleNotFoundException(String roleName) {
        super("Role with name " + roleName + " not found");
        this.errorCode = ErrorCode.ROLE_NOT_FOUND;
    }

    /**
     * Returns the {@link ErrorCode} associated with this exception.
     *
     * @return the {@code ROLE_NOT_FOUND} error code
     */
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
