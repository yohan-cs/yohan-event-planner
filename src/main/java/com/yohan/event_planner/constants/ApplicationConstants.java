package com.yohan.event_planner.constants;

/**
 * Central repository for application-wide constants including magic numbers,
 * string length constraints, and commonly used values.
 * 
 * <p>This class helps maintain consistency and makes configuration changes easier
 * by centralizing hardcoded values that are used across multiple classes.</p>
 */
public final class ApplicationConstants {

    // Prevent instantiation
    private ApplicationConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ================================
    // Database Field Length Constraints
    // ================================
    
    /** Maximum length for short names (usernames, event names, etc.) */
    public static final int SHORT_NAME_MAX_LENGTH = 50;
    
    /** Maximum length for medium text fields (timezone names, etc.) */
    public static final int MEDIUM_TEXT_MAX_LENGTH = 100;
    
    /** Maximum length for standard text fields (descriptions) */
    public static final int STANDARD_TEXT_MAX_LENGTH = 255;
    
    /** Maximum length for standard URLs (media URLs) */
    public static final int URL_MAX_LENGTH = 512;
    
    /** Maximum length for long text fields (bio, notes) */
    public static final int LONG_TEXT_MAX_LENGTH = 1000;
    
    /** Maximum length for very long URLs (profile picture URLs) */
    public static final int VERY_LONG_URL_MAX_LENGTH = 2048;
    
    /** Maximum length for media type fields */
    public static final int MEDIA_TYPE_MAX_LENGTH = 10;

    // ================================
    // JWT and Security Constants
    // ================================
    
    /** JWT Bearer token prefix */
    public static final String JWT_BEARER_PREFIX = "Bearer ";
    
    /** 
     * Default JWT token expiration time in milliseconds (1 hour).
     * Note: Actual JWT expiration is configured via spring.app.jwtExpirationMs property.
     * This constant is provided for reference and fallback purposes.
     */
    public static final long DEFAULT_JWT_EXPIRATION_MS = 3600000L;
    
    /** HMAC-SHA256 algorithm name used for JWT signing and refresh token hashing */
    public static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    // ================================
    // User Management Constants
    // ================================
    
    /** Number of days before a pending user deletion is permanently applied */
    public static final long USER_DELETION_GRACE_PERIOD_DAYS = 30L;

    // ================================
    // Validation Constants
    // ================================
    
    /** Minimum username length */
    public static final int USERNAME_MIN_LENGTH = 3;
    
    /** Maximum username length */
    public static final int USERNAME_MAX_LENGTH = 30;
    
    /** Minimum password length */
    public static final int PASSWORD_MIN_LENGTH = 8;
    
    /** Maximum password length */
    public static final int PASSWORD_MAX_LENGTH = 72;
    
    /** Minimum name length */
    public static final int NAME_MIN_LENGTH = 1;
    
    /** Maximum recap notes length */
    public static final int RECAP_NOTES_MAX_LENGTH = 5000;

    // ================================
    // Rate Limiting Constants
    // ================================
    
    /** Maximum registration attempts per time window */
    public static final int MAX_REGISTRATION_ATTEMPTS = 5;
    
    /** Time window for registration rate limiting in hours */
    public static final long REGISTRATION_WINDOW_HOURS = 1;
    
    /** Cache name for registration rate limiting */
    public static final String REGISTRATION_CACHE = "registration-rate-limit";

    /** Maximum login attempts per time window */
    public static final int MAX_LOGIN_ATTEMPTS = 10;
    
    /** Time window for login rate limiting in hours (15 minutes) */
    public static final double LOGIN_WINDOW_HOURS = 0.25;
    
    /** Cache name for login rate limiting */
    public static final String LOGIN_CACHE = "login-rate-limit";

    /** Maximum password reset requests per time window */
    public static final int MAX_PASSWORD_RESET_ATTEMPTS = 3;
    
    /** Time window for password reset rate limiting in hours */
    public static final long PASSWORD_RESET_WINDOW_HOURS = 1;
    
    /** Cache name for password reset rate limiting */
    public static final String PASSWORD_RESET_CACHE = "password-reset-rate-limit";

    /** Maximum email verification attempts per time window */
    public static final int MAX_EMAIL_VERIFICATION_ATTEMPTS = 5;
    
    /** Time window for email verification rate limiting in hours (30 minutes) */
    public static final double EMAIL_VERIFICATION_WINDOW_HOURS = 0.5;
    
    /** Cache name for email verification rate limiting */
    public static final String EMAIL_VERIFICATION_CACHE = "email-verification-rate-limit";

    // ================================
    // Email Validation Constants
    // ================================
    
    /** Maximum email address length according to RFC 5321 */
    public static final int MAX_EMAIL_LENGTH = 254;
    
    /** Maximum domain name length according to RFC 1035 */
    public static final int MAX_DOMAIN_LENGTH = 253;

    // ================================
    // Date Calculation Constants
    // ================================
    
    /** Multiplier for year component in YYYYMMDD date format */
    public static final int DATE_YEAR_MULTIPLIER = 10000;
    
    /** Multiplier for month component in YYYYMMDD date format */
    public static final int DATE_MONTH_MULTIPLIER = 100;

    // ================================
    // Email Subject Constants
    // ================================
    
    /** Subject line for email verification emails */
    public static final String EMAIL_VERIFICATION_SUBJECT = "Ayoboyo – Verify Your Email Address";
    
    /** Subject line for password reset emails */
    public static final String PASSWORD_RESET_SUBJECT = "Ayoboyo – Reset Your Password";
    
    /** Subject line for welcome emails */
    public static final String WELCOME_EMAIL_SUBJECT = "Welcome to Ayoboyo!";
    
    /** Subject line for password change confirmation emails */
    public static final String PASSWORD_CHANGE_SUBJECT = "Ayoboyo – Your Password Was Changed";

    // ================================
    // Password Reset Constants
    // ================================
    
    /** Minimum delay in milliseconds for timing attack prevention */
    public static final int PASSWORD_RESET_MIN_SIMULATION_DELAY_MS = 100;
    
    /** Maximum delay in milliseconds for timing attack prevention */
    public static final int PASSWORD_RESET_MAX_SIMULATION_DELAY_MS = 300;
    
    /** Default token expiry time in minutes if configuration is invalid */
    public static final int PASSWORD_RESET_DEFAULT_TOKEN_EXPIRY_MINUTES = 30;
    
    /** Length of password reset tokens */
    public static final int PASSWORD_RESET_TOKEN_LENGTH = 64;
    
    /** Character set used for password reset token generation */
    public static final String PASSWORD_RESET_TOKEN_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    
    /** Length of token prefix to show in logs for debugging while maintaining security */
    public static final int PASSWORD_RESET_TOKEN_LOG_PREFIX_LENGTH = 8;

    // ================================
    // Refresh Token Cleanup Constants
    // ================================
    
    /** Number of days to retain revoked refresh tokens for audit purposes */
    public static final int REFRESH_TOKEN_REVOKED_RETENTION_DAYS = 30;

    // ================================
    // Unverified User Cleanup Constants
    // ================================
    
    /** Maximum age in hours for unverified user accounts before cleanup */
    public static final int UNVERIFIED_USER_MAX_AGE_HOURS = 24;

    // ================================
    // Pagination Constants
    // ================================
    
    /** Default page number for pagination (0-based) */
    public static final int DEFAULT_PAGE_NUMBER = 0;
    
    /** Default page size for pagination */
    public static final int DEFAULT_PAGE_SIZE = 10;
    
    /** Maximum allowed page size for pagination */
    public static final int MAX_PAGE_SIZE = 100;
    
    /** Minimum allowed page size for pagination */
    public static final int MIN_PAGE_SIZE = 1;

    // ================================
    // Substring Operations
    // ================================
    
    /** Length of the Bearer prefix for substring operations */
    public static final int JWT_BEARER_PREFIX_LENGTH = 7;

    // ================================
    // GraphQL Controller Constants
    // ================================
    
    /** Return value for successful GraphQL mutations that return Boolean */
    public static final Boolean GRAPHQL_OPERATION_SUCCESS = Boolean.TRUE;
    
    /** Error message for invalid datetime format in GraphQL operations */
    public static final String INVALID_DATETIME_FORMAT_MESSAGE = "Unexpected value type for ZonedDateTime field";
}