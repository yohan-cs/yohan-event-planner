package com.yohan.event_planner.exception;

/**
 * Enum representing specific error codes for application exceptions.
 *
 * Each enum constant corresponds to a unique error situation
 * that can be used to standardize error handling and client communication.
 */
public enum ErrorCode {
    // Username related errors
    DUPLICATE_USERNAME,
    INVALID_USERNAME_LENGTH,
    INVALID_USERNAME_PROFANITY,
    UNAUTHORIZED_USER_ACCESS,
    NULL_USERNAME,

    // Password related errors
    INVALID_PASSWORD_LENGTH,
    DUPLICATE_PASSWORD,
    WEAK_PASSWORD,
    NULL_PASSWORD,

    // Email related errors
    DUPLICATE_EMAIL,
    INVALID_EMAIL_LENGTH,
    INVALID_EMAIL_FORMAT,
    NULL_EMAIL,

    // Resource related errors
    USER_NOT_FOUND,
    EVENT_NOT_FOUND,
    ROLE_NOT_FOUND,

    // Event related errors
    EVENT_CONFLICT,
    INVALID_EVENT_TIME,
    UNAUTHORIZED_EVENT_ACCESS,

    // Role related errors
    DUPLICATE_ROLE,
    INVALID_ROLE_NAME,

    // Credentials errors
    INVALID_CREDENTIALS,

    // JWT Authentication/Authorization errors
    UNAUTHORIZED_ACCESS,  // 401 Unauthorized
    ACCESS_DENIED,        // 403 Forbidden

    // Generic errors
    VALIDATION_FAILED,
    UNKNOWN_ERROR
}

