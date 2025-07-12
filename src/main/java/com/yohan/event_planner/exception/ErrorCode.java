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

    // Password related errors
    INVALID_PASSWORD_LENGTH,
    DUPLICATE_PASSWORD,
    WEAK_PASSWORD,
    INVALID_RESET_TOKEN,
    EXPIRED_RESET_TOKEN,
    USED_RESET_TOKEN,
    INSUFFICIENT_PASSWORD_STRENGTH,
    COMMON_PASSWORD,
    SIMPLE_PASSWORD_PATTERN,
    
    // Password reset specific errors
    PASSWORD_RESET_TOKEN_GENERATION_FAILED,
    PASSWORD_RESET_EMAIL_FAILED,
    PASSWORD_RESET_DATABASE_ERROR,
    PASSWORD_RESET_ENCODING_FAILED,
    PASSWORD_RESET_CONFIRMATION_EMAIL_FAILED,

    // Email related errors
    DUPLICATE_EMAIL,
    INVALID_EMAIL_LENGTH,
    INVALID_EMAIL_FORMAT,
    INVALID_EMAIL_DOMAIN,
    EMAIL_SEND_FAILED,
    EMAIL_SENDING_FAILED,
    EMAIL_NOT_VERIFIED,
    
    // Email verification related errors
    INVALID_VERIFICATION_TOKEN,
    EXPIRED_VERIFICATION_TOKEN,
    USED_VERIFICATION_TOKEN,
    VERIFICATION_FAILED,

    // Resource related errors
    USER_NOT_FOUND,
    EVENT_NOT_FOUND,
    ROLE_NOT_FOUND,
    LABEL_NOT_FOUND,
    INVALID_LABEL_ASSOCIATION,
    BADGE_NOT_FOUND,
    EVENT_RECAP_NOT_FOUND,
    RECAP_MEDIA_NOT_FOUND,

    // Event related errors
    EVENT_CONFLICT,
    INVALID_EVENT_TIME,
    UNAUTHORIZED_EVENT_ACCESS,
    EVENT_ALREADY_CONFIRMED,

    // Recurring event related errors
    RECURRING_EVENT_CONFLICT,
    RECURRING_EVENT_HEAVY_CONFLICT,
    UNAUTHORIZED_RECURRING_EVENT_ACCESS,
    RECURRING_EVENT_ALREADY_CONFIRMED,

    // Recurring event validation errors
    MISSING_RECURRENCE_FREQUENCY,
    WEEKLY_MISSING_DAYS,
    WEEKLY_INVALID_DAY,
    MONTHLY_MISSING_ORDINAL_OR_DAY,
    MONTHLY_INVALID_ORDINAL,
    MONTHLY_INVALID_DAY,
    UNSUPPORTED_RECURRENCE_COMBINATION,
    RECURRING_EVENT_NOT_FOUND,
    INVALID_SKIP_DAY_REMOVAL,
    INVALID_SKIP_DAY_ADDITION,
    INVALID_RECURRENCE_INTERVAL,
    INVALID_RECURRENCE_RULE,

    // Event state validation errors
    MISSING_EVENT_NAME,
    MISSING_EVENT_START_TIME,
    MISSING_EVENT_END_TIME,
    MISSING_EVENT_LABEL,
    MISSING_RECURRENCE_RULE,
    EVENT_NOT_CONFIRMED,
    MISSING_EVENT_START_DATE,
    MISSING_EVENT_END_DATE,
    RECAP_ON_INCOMPLETE_EVENT,

    // Role related errors
    DUPLICATE_ROLE,
    INVALID_ROLE_NAME,

    // Label related errors
    DUPLICATE_LABEL,
    UNAUTHORIZED_LABEL_ACCESS,

    // EventRecap related errors
    DUPLICATE_EVENT_RECAP,
    EVENT_RECAP_ALREADY_CONFIRMED,

    // RecapMedia related errors
    INVALID_MEDIA_TYPE,
    INCOMPLETE_RECAP_MEDIA_REORDER_LIST,

    // Badge related errors
    DUPLICATE_BADGE,
    UNAUTHORIZED_BADGE_ACCESS,
    INCOMPLETE_BADGE_REORDER_LIST,
    INCOMPLETE_BADGE_LABEL_REORDER_LIST,

    // Time related errors
    INVALID_TIME_RANGE,
    INVALID_COMPLETION_STATUS,
    INVALID_COMPLETION_FOR_DRAFT_EVENT,


    // Credentials errors
    INVALID_CREDENTIALS,

    // JWT Authentication/Authorization errors
    UNAUTHORIZED_ACCESS,  // 401 Unauthorized
    ACCESS_DENIED,        // 403 Forbidden
    
    // Rate limiting errors
    RATE_LIMIT_EXCEEDED,  // 429 Too Many Requests

    // Request structure / validation errors
    NULL_FIELD_NOT_ALLOWED,
    INVALID_CALENDAR_PARAMETER,
    INVALID_PAGINATION_PARAMETER,

    // System Managed errors,
    SYSTEM_MANAGED_LABEL,

    // Generic errors,
    UNKNOWN_ERROR
}

