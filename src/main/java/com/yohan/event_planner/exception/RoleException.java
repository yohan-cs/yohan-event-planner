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
     * @throws NullPointerException if {@code errorCode} is {@code null}
     */
    public RoleException(ErrorCode errorCode, String roleName) {
        super(buildMessage(errorCode, roleName));
        if (errorCode == null) {
            throw new NullPointerException("errorCode cannot be null");
        }
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
        if (errorCode == null) {
            return "Role validation error with null error code for role: " + roleName;
        }
        return switch (errorCode) {
            case DUPLICATE_ROLE -> "Role with name '" + roleName + "' already exists";
            case INVALID_ROLE_NAME -> "The role name '" + roleName + "' is invalid.";
            default -> "An unknown role error occurred";
        };
    }
}
