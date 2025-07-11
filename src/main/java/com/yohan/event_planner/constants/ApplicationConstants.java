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
    // Substring Operations
    // ================================
    
    /** Length of the Bearer prefix for substring operations */
    public static final int JWT_BEARER_PREFIX_LENGTH = 7;
}