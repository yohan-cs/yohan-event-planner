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

    /**
     * Checks if a login attempt from the given IP address is allowed.
     *
     * <p>
     * This method implements rate limiting for user login to prevent:
     * </p>
     * <ul>
     *   <li>Brute force attacks against user credentials</li>
     *   <li>Credential stuffing attacks using known breaches</li>
     *   <li>Account enumeration through repeated login attempts</li>
     *   <li>Distributed password attacks across multiple accounts</li>
     * </ul>
     *
     * <h3>Rate Limiting Policy</h3>
     * <ul>
     *   <li><strong>Default Limit</strong>: 10 login attempts per 15 minutes per IP</li>
     *   <li><strong>Time Window</strong>: 15 minute sliding window</li>
     *   <li><strong>Reset Behavior</strong>: Automatic reset after time window expires</li>
     * </ul>
     *
     * @param ipAddress the IP address of the client attempting login
     * @return true if the login attempt is allowed, false if rate limit exceeded
     */
    boolean isLoginAllowed(String ipAddress);

    /**
     * Records a login attempt for the given IP address.
     *
     * <p>
     * This method should be called after a login attempt (successful or failed)
     * to update the rate limiting counters. This ensures accurate tracking of
     * login attempts for rate limiting purposes.
     * </p>
     *
     * @param ipAddress the IP address of the client that attempted login
     */
    void recordLoginAttempt(String ipAddress);

    /**
     * Gets the number of remaining login attempts for the given IP address.
     *
     * <p>
     * This method can be used to provide feedback to clients about their
     * remaining attempts before rate limiting kicks in.
     * </p>
     *
     * @param ipAddress the IP address to check
     * @return the number of login attempts remaining in the current window
     */
    int getRemainingLoginAttempts(String ipAddress);

    /**
     * Gets the time in seconds until the rate limit window resets for login.
     *
     * @param ipAddress the IP address to check
     * @return the number of seconds until the rate limit resets, or 0 if no limit is active
     */
    long getLoginRateLimitResetTime(String ipAddress);

    /**
     * Checks if a password reset request from the given IP address is allowed.
     *
     * <p>
     * This method implements rate limiting for password reset requests to prevent:
     * </p>
     * <ul>
     *   <li>Email flooding through repeated reset requests</li>
     *   <li>Password reset token generation abuse</li>
     *   <li>Account harassment through forced password resets</li>
     *   <li>Email service resource exhaustion</li>
     * </ul>
     *
     * <h3>Rate Limiting Policy</h3>
     * <ul>
     *   <li><strong>Default Limit</strong>: 3 password reset requests per hour per IP</li>
     *   <li><strong>Time Window</strong>: 1 hour sliding window</li>
     *   <li><strong>Reset Behavior</strong>: Automatic reset after time window expires</li>
     * </ul>
     *
     * @param ipAddress the IP address of the client requesting password reset
     * @return true if the password reset request is allowed, false if rate limit exceeded
     */
    boolean isPasswordResetAllowed(String ipAddress);

    /**
     * Records a password reset request for the given IP address.
     *
     * @param ipAddress the IP address of the client that requested password reset
     */
    void recordPasswordResetAttempt(String ipAddress);

    /**
     * Gets the number of remaining password reset requests for the given IP address.
     *
     * @param ipAddress the IP address to check
     * @return the number of password reset requests remaining in the current window
     */
    int getRemainingPasswordResetAttempts(String ipAddress);

    /**
     * Gets the time in seconds until the rate limit window resets for password reset.
     *
     * @param ipAddress the IP address to check
     * @return the number of seconds until the rate limit resets, or 0 if no limit is active
     */
    long getPasswordResetRateLimitResetTime(String ipAddress);

    /**
     * Checks if an email verification attempt from the given IP address is allowed.
     *
     * <p>
     * This method implements rate limiting for email verification to prevent:
     * </p>
     * <ul>
     *   <li>Brute force attacks against verification tokens</li>
     *   <li>Email verification token enumeration</li>
     *   <li>Repeated verification email sending abuse</li>
     *   <li>Account activation bypass attempts</li>
     * </ul>
     *
     * <h3>Rate Limiting Policy</h3>
     * <ul>
     *   <li><strong>Default Limit</strong>: 5 email verification attempts per 30 minutes per IP</li>
     *   <li><strong>Time Window</strong>: 30 minute sliding window</li>
     *   <li><strong>Reset Behavior</strong>: Automatic reset after time window expires</li>
     * </ul>
     *
     * @param ipAddress the IP address of the client attempting email verification
     * @return true if the email verification attempt is allowed, false if rate limit exceeded
     */
    boolean isEmailVerificationAllowed(String ipAddress);

    /**
     * Records an email verification attempt for the given IP address.
     *
     * @param ipAddress the IP address of the client that attempted email verification
     */
    void recordEmailVerificationAttempt(String ipAddress);

    /**
     * Gets the number of remaining email verification attempts for the given IP address.
     *
     * @param ipAddress the IP address to check
     * @return the number of email verification attempts remaining in the current window
     */
    int getRemainingEmailVerificationAttempts(String ipAddress);

    /**
     * Gets the time in seconds until the rate limit window resets for email verification.
     *
     * @param ipAddress the IP address to check
     * @return the number of seconds until the rate limit resets, or 0 if no limit is active
     */
    long getEmailVerificationRateLimitResetTime(String ipAddress);
}