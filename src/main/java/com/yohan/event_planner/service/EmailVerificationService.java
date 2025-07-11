package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.User;

import java.util.Optional;

/**
 * Service interface for email verification operations in the Event Planner application.
 *
 * <p>
 * This service handles the complete email verification workflow, from initial
 * token generation after registration through verification link processing and
 * account activation. It implements security best practices including token expiration,
 * single-use tokens, and protection against verification abuse.
 * </p>
 *
 * <h2>Email Verification Workflow</h2>
 * <ol>
 *   <li><strong>Token Generation</strong>: Secure token created during user registration</li>
 *   <li><strong>Email Delivery</strong>: Verification link sent to user's email</li>
 *   <li><strong>Link Validation</strong>: User clicks link, token validated</li>
 *   <li><strong>Account Activation</strong>: User account activated and token invalidated</li>
 * </ol>
 *
 * <h2>Security Features</h2>
 * <ul>
 *   <li><strong>Token Expiration</strong>: Tokens expire after configurable time period</li>
 *   <li><strong>Single Use</strong>: Tokens are invalidated after successful verification</li>
 *   <li><strong>Secure Generation</strong>: Uses cryptographically secure random tokens</li>
 *   <li><strong>Rate Limiting</strong>: Prevents abuse of verification functionality</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <ul>
 *   <li><strong>Invalid Tokens</strong>: Expired or non-existent tokens handled gracefully</li>
 *   <li><strong>Email Failures</strong>: SMTP issues logged but don't expose system details</li>
 *   <li><strong>Already Verified</strong>: Duplicate verification attempts handled safely</li>
 * </ul>
 *
 * @see com.yohan.event_planner.domain.EmailVerificationToken
 * @see com.yohan.event_planner.service.EmailService
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
public interface EmailVerificationService {

    /**
     * Generates and sends an email verification token for a newly registered user.
     *
     * <p>
     * This method creates a secure verification token, stores it in the database,
     * and sends it to the user's email address. The token allows the user to
     * verify their email and activate their account.
     * </p>
     *
     * <h3>Process Flow</h3>
     * <ol>
     *   <li>Invalidate any existing verification tokens for the user</li>
     *   <li>Generate a new cryptographically secure token</li>
     *   <li>Store token in database with expiration time</li>
     *   <li>Send verification email with token link</li>
     * </ol>
     *
     * <h3>Security Considerations</h3>
     * <ul>
     *   <li><strong>Token Security</strong>: Uses cryptographically secure random generation</li>
     *   <li><strong>Expiration</strong>: Tokens expire after configurable duration (default: 24 hours)</li>
     *   <li><strong>Uniqueness</strong>: Only one active token per user at any time</li>
     * </ul>
     *
     * @param user the user account that needs email verification
     * @throws com.yohan.event_planner.exception.EmailException if email sending fails
     */
    void generateAndSendVerificationToken(User user);

    /**
     * Verifies an email verification token and activates the user account.
     *
     * <p>
     * This method validates the provided verification token and, if valid, marks
     * the user's email as verified and activates their account. The token is then
     * invalidated to prevent reuse.
     * </p>
     *
     * <h3>Validation Process</h3>
     * <ol>
     *   <li>Verify token exists and is not expired</li>
     *   <li>Confirm token has not been used previously</li>
     *   <li>Mark user's email as verified</li>
     *   <li>Mark token as used and invalidate it</li>
     * </ol>
     *
     * <h3>Security Features</h3>
     * <ul>
     *   <li><strong>Token Validation</strong>: Comprehensive checks for authenticity and expiry</li>
     *   <li><strong>Single Use</strong>: Tokens cannot be reused after verification</li>
     *   <li><strong>Audit Logging</strong>: Verification events logged for security</li>
     * </ul>
     *
     * @param token the verification token from the email link
     * @return the verified User if successful, empty if token is invalid
     * @throws com.yohan.event_planner.exception.EmailException if token is invalid or verification fails
     */
    Optional<User> verifyEmail(String token);

    /**
     * Resends email verification for a user who hasn't verified their account.
     *
     * <p>
     * This method allows users to request a new verification email if they didn't
     * receive the original or if their token has expired. It generates a new token
     * and invalidates any existing ones.
     * </p>
     *
     * <h3>Process Flow</h3>
     * <ol>
     *   <li>Check if user's email is already verified</li>
     *   <li>Invalidate any existing verification tokens</li>
     *   <li>Generate and send new verification token</li>
     * </ol>
     *
     * @param user the user account that needs a new verification email
     * @return true if resend was successful, false if email is already verified
     * @throws com.yohan.event_planner.exception.EmailException if email sending fails
     */
    boolean resendVerificationEmail(User user);

    /**
     * Validates a verification token without consuming it.
     *
     * <p>
     * This method checks if a verification token is valid (exists, not expired,
     * not used) without marking it as used. This can be useful for pre-validation
     * on verification forms.
     * </p>
     *
     * @param token the verification token to validate
     * @return true if the token is valid and can be used for verification
     */
    boolean isValidVerificationToken(String token);

    /**
     * Invalidates all existing email verification tokens for a specific user.
     *
     * <p>
     * This method marks all active verification tokens for a user as used,
     * effectively canceling any pending verification requests. This is useful
     * when a user's email is manually verified by an administrator.
     * </p>
     *
     * @param user the user whose tokens should be invalidated
     * @return the number of tokens that were invalidated
     */
    int invalidateUserVerificationTokens(User user);

    /**
     * Performs cleanup of expired and used email verification tokens.
     *
     * <p>
     * This method removes old verification tokens from the database to maintain
     * performance and reduce storage requirements. It should be called periodically
     * by a scheduled job to keep the token table clean.
     * </p>
     *
     * @return the number of tokens that were cleaned up
     */
    int cleanupExpiredTokens();
}