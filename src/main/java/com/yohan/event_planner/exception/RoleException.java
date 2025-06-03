package com.yohan.event_planner.exception;

/**
 * Exception thrown for role-related errors such as duplicates or invalid names.
 * Associates each error with a specific {@link ErrorCode} and generates a meaningful error message.
 */
public class RoleException extends RuntimeException implements HasErrorCode {

    private final ErrorCode errorCode;

    /**
     * Constructs a new {@code RoleException} with a specific error code and role name context.
     *
     * @param errorCode the specific {@link ErrorCode} representing the role error
     * @param roleName the role name involved in the error condition
     */
    public RoleException(ErrorCode errorCode, String roleName) {
        super(buildMessage(errorCode, roleName));
        this.errorCode = errorCode;
    }

    /**
     * Returns the {@link ErrorCode} associated with this role exception.
     *
     * @return the error code indicating the specific role-related error
     */
    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Builds a detailed error message based on the provided error code and role name context.
     *
     * @param errorCode the {@link ErrorCode} describing the error
     * @param roleName the role name involved in the error condition
     * @return a human-readable message describing the role error
     */
    private static String buildMessage(ErrorCode errorCode, String roleName) {
        return switch (errorCode) {
            case DUPLICATE_ROLE -> "Role with name '" + roleName + "' already exists";
            case INVALID_ROLE_NAME -> "The role name '" + roleName + "' is invalid.";
            default -> "An unknown role error occurred";
        };
    }
}
