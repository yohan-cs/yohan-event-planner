package com.yohan.event_planner.exception;

/**
 * Exception thrown when a client exceeds the configured rate limits.
 *
 * <p>
 * This exception is thrown when a client makes too many requests within
 * a configured time window, indicating potential abuse or automated behavior.
 * It provides information about the rate limit violation and when the client
 * can retry their request.
 * </p>
 *
 * <h2>Common Scenarios</h2>
 * <ul>
 *   <li><strong>Registration Abuse</strong>: Too many registration attempts from same IP</li>
 *   <li><strong>Login Brute Force</strong>: Excessive login attempts</li>
 *   <li><strong>Email Spam</strong>: Too many email operations</li>
 *   <li><strong>API Abuse</strong>: General API endpoint overuse</li>
 * </ul>
 *
 * <h2>Client Response</h2>
 * <p>
 * When this exception is thrown, clients should receive:
 * </p>
 * <ul>
 *   <li><strong>HTTP 429</strong>: Too Many Requests status code</li>
 *   <li><strong>Retry-After Header</strong>: When the client can retry</li>
 *   <li><strong>Error Message</strong>: Human-readable description</li>
 *   <li><strong>Rate Limit Info</strong>: Current limit and window information</li>
 * </ul>
 *
 * @see org.springframework.web.bind.annotation.ExceptionHandler
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.1.0
 */
public class RateLimitExceededException extends RuntimeException {

    /** The number of seconds until the rate limit resets */
    private final long retryAfterSeconds;
    
    /** The maximum number of attempts allowed in the time window */
    private final int maxAttempts;
    
    /** The current number of attempts made */
    private final int currentAttempts;
    
    /** The operation that was rate limited */
    private final String operation;

    /**
     * Constructs a new rate limit exceeded exception with detailed information.
     *
     * @param operation the operation that was rate limited (e.g., "registration", "login")
     * @param maxAttempts the maximum number of attempts allowed
     * @param currentAttempts the current number of attempts made
     * @param retryAfterSeconds the number of seconds until the rate limit resets
     */
    public RateLimitExceededException(String operation, int maxAttempts, int currentAttempts, long retryAfterSeconds) {
        super(String.format("Rate limit exceeded for %s: %d/%d attempts used. Try again in %d seconds.", 
                           operation, currentAttempts, maxAttempts, retryAfterSeconds));
        this.operation = operation;
        this.maxAttempts = maxAttempts;
        this.currentAttempts = currentAttempts;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * Constructs a new rate limit exceeded exception with a custom message.
     *
     * @param operation the operation that was rate limited
     * @param maxAttempts the maximum number of attempts allowed
     * @param currentAttempts the current number of attempts made
     * @param retryAfterSeconds the number of seconds until the rate limit resets
     * @param message a custom error message
     */
    public RateLimitExceededException(String operation, int maxAttempts, int currentAttempts, 
                                    long retryAfterSeconds, String message) {
        super(message);
        this.operation = operation;
        this.maxAttempts = maxAttempts;
        this.currentAttempts = currentAttempts;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * Gets the number of seconds until the rate limit resets.
     *
     * @return the retry-after time in seconds
     */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    /**
     * Gets the maximum number of attempts allowed in the time window.
     *
     * @return the maximum attempts allowed
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Gets the current number of attempts made.
     *
     * @return the current attempt count
     */
    public int getCurrentAttempts() {
        return currentAttempts;
    }

    /**
     * Gets the operation that was rate limited.
     *
     * @return the operation name
     */
    public String getOperation() {
        return operation;
    }
}