package com.yohan.event_planner.service;

/**
 * Service interface for rate limiting operations across various endpoints.
 *
 * <p>
 * This service provides rate limiting functionality to prevent abuse and protect
 * the application from excessive requests. It supports various rate limiting strategies
 * including per-IP, per-user, and per-operation rate limiting.
 * </p>
 *
 * <h2>Rate Limiting Strategies</h2>
 * <ul>
 *   <li><strong>Registration Limiting</strong>: Prevent rapid account creation</li>
 *   <li><strong>Login Limiting</strong>: Prevent brute force attacks</li>
 *   <li><strong>Email Limiting</strong>: Prevent email spam and abuse</li>
 *   <li><strong>API Limiting</strong>: General API endpoint protection</li>
 * </ul>
 *
 * <h2>Implementation Features</h2>
 * <ul>
 *   <li><strong>Sliding Window</strong>: Time-based rate limiting windows</li>
 *   <li><strong>Per-IP Tracking</strong>: Individual limits per client IP</li>
 *   <li><strong>Configurable Limits</strong>: Adjustable rate limits per operation</li>
 *   <li><strong>Automatic Cleanup</strong>: Expired rate limit data is automatically removed</li>
 * </ul>
 *
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.1.0
 */
public interface RateLimitingService {

    /**
     * Checks if a registration attempt from the given IP address is allowed.
     *
     * <p>
     * This method implements rate limiting for user registration to prevent:
     * </p>
     * <ul>
     *   <li>Username squatting through rapid account creation</li>
     *   <li>Fake account creation with invalid emails</li>
     *   <li>Resource exhaustion through spam registrations</li>
     *   <li>Database pollution with abandoned accounts</li>
     * </ul>
     *
     * <h3>Rate Limiting Policy</h3>
     * <ul>
     *   <li><strong>Default Limit</strong>: 5 registration attempts per hour per IP</li>
     *   <li><strong>Time Window</strong>: 1 hour sliding window</li>
     *   <li><strong>Reset Behavior</strong>: Automatic reset after time window expires</li>
     * </ul>
     *
     * @param ipAddress the IP address of the client attempting registration
     * @return true if the registration attempt is allowed, false if rate limit exceeded
     */
    boolean isRegistrationAllowed(String ipAddress);

    /**
     * Records a registration attempt for the given IP address.
     *
     * <p>
     * This method should be called after a registration attempt (successful or failed)
     * to update the rate limiting counters. This ensures accurate tracking of
     * registration attempts for rate limiting purposes.
     * </p>
     *
     * @param ipAddress the IP address of the client that attempted registration
     */
    void recordRegistrationAttempt(String ipAddress);

    /**
     * Gets the number of remaining registration attempts for the given IP address.
     *
     * <p>
     * This method can be used to provide feedback to clients about their
     * remaining attempts before rate limiting kicks in. This improves user
     * experience by providing transparency about rate limits.
     * </p>
     *
     * @param ipAddress the IP address to check
     * @return the number of registration attempts remaining in the current window
     */
    int getRemainingRegistrationAttempts(String ipAddress);

    /**
     * Gets the time in seconds until the rate limit window resets for registration.
     *
     * <p>
     * This method provides information about when the rate limit will reset,
     * allowing clients to know when they can retry registration attempts.
     * </p>
     *
     * @param ipAddress the IP address to check
     * @return the number of seconds until the rate limit resets, or 0 if no limit is active
     */
    long getRegistrationRateLimitResetTime(String ipAddress);
}