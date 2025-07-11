package com.yohan.event_planner.service;

import com.yohan.event_planner.dto.auth.ForgotPasswordRequestDTO;
import com.yohan.event_planner.dto.auth.ForgotPasswordResponseDTO;
import com.yohan.event_planner.dto.auth.ResetPasswordRequestDTO;
import com.yohan.event_planner.dto.auth.ResetPasswordResponseDTO;

/**
 * Service interface for password reset operations in the Event Planner application.
 *
 * <p>
 * This service handles the complete password reset workflow, from initial
 * forgot password requests through token generation and final password updates.
 * It implements security best practices including token expiration, single-use
 * tokens, and anti-enumeration protections.
 * </p>
 *
 * <h2>Password Reset Workflow</h2>
 * <ol>
 *   <li><strong>Request Reset</strong>: User submits email address</li>
 *   <li><strong>Token Generation</strong>: Secure token created and stored</li>
 *   <li><strong>Email Delivery</strong>: Deep link sent to user's email</li>
 *   <li><strong>Token Validation</strong>: User clicks link, token validated</li>
 *   <li><strong>Password Update</strong>: New password set and token invalidated</li>
 * </ol>
 *
 * <h2>Security Features</h2>
 * <ul>
 *   <li><strong>Token Expiration</strong>: Tokens expire after configurable time period</li>
 *   <li><strong>Single Use</strong>: Tokens are invalidated after successful use</li>
 *   <li><strong>Anti-Enumeration</strong>: Same response regardless of email existence</li>
 *   <li><strong>Rate Limiting</strong>: Prevents abuse of reset functionality</li>
 *   <li><strong>Session Invalidation</strong>: All user sessions cleared on password change</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <ul>
 *   <li><strong>Invalid Tokens</strong>: Expired or non-existent tokens handled gracefully</li>
 *   <li><strong>Email Failures</strong>: SMTP issues logged but don't expose system details</li>
 *   <li><strong>Rate Limiting</strong>: Excessive requests blocked with appropriate messaging</li>
 * </ul>
 *
 * @see com.yohan.event_planner.domain.PasswordResetToken
 * @see com.yohan.event_planner.service.EmailService
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
public interface PasswordResetService {

    /**
     * Initiates a password reset process for the given email address.
     *
     * <p>
     * This method handles forgot password requests by generating a secure reset token
     * and sending it to the user's email address. For security reasons, the response
     * is the same regardless of whether the email exists in the system, preventing
     * email enumeration attacks.
     * </p>
     *
     * <h3>Process Flow</h3>
     * <ol>
     *   <li>Validate email format and check rate limiting</li>
     *   <li>Look up user account by email address</li>
     *   <li>If user exists: generate token, store in database, send email</li>
     *   <li>If user doesn't exist: simulate processing time for security</li>
     *   <li>Return consistent response message</li>
     * </ol>
     *
     * <h3>Security Considerations</h3>
     * <ul>
     *   <li><strong>Timing Attacks</strong>: Response time is consistent regardless of email existence</li>
     *   <li><strong>Rate Limiting</strong>: Maximum requests per email/IP address enforced</li>
     *   <li><strong>Token Security</strong>: Uses cryptographically secure random generation</li>
     *   <li><strong>Email Validation</strong>: Sends only to verified email addresses</li>
     * </ul>
     *
     * @param request the forgot password request containing the email address
     * @return a response indicating the reset process has been initiated
     * @throws com.yohan.event_planner.exception.EmailException if email sending fails
     */
    ForgotPasswordResponseDTO requestPasswordReset(ForgotPasswordRequestDTO request);

    /**
     * Completes a password reset using a valid reset token.
     *
     * <p>
     * This method validates the provided reset token and, if valid, updates the
     * user's password to the new value. The token is then invalidated to prevent
     * reuse, and all existing user sessions are cleared for security.
     * </p>
     *
     * <h3>Validation Process</h3>
     * <ol>
     *   <li>Verify token exists and is not expired</li>
     *   <li>Confirm token has not been used previously</li>
     *   <li>Validate new password meets security requirements</li>
     *   <li>Update password with secure hashing</li>
     *   <li>Mark token as used and invalidate all user sessions</li>
     *   <li>Send confirmation email to user</li>
     * </ol>
     *
     * <h3>Security Features</h3>
     * <ul>
     *   <li><strong>Token Validation</strong>: Comprehensive checks for authenticity and expiry</li>
     *   <li><strong>Password Policy</strong>: New password must meet application requirements</li>
     *   <li><strong>Session Security</strong>: All existing sessions invalidated</li>
     *   <li><strong>Audit Logging</strong>: Password change events logged for security</li>
     *   <li><strong>Email Notification</strong>: User notified of successful password change</li>
     * </ul>
     *
     * @param request the reset password request containing token and new password
     * @return a response confirming the password reset completion
     * @throws com.yohan.event_planner.exception.PasswordException if token is invalid or password doesn't meet requirements
     */
    ResetPasswordResponseDTO resetPassword(ResetPasswordRequestDTO request);

    /**
     * Validates a password reset token without consuming it.
     *
     * <p>
     * This method checks if a password reset token is valid (exists, not expired,
     * not used) without marking it as used. This can be useful for pre-validation
     * on password reset forms before the user submits their new password.
     * </p>
     *
     * @param token the reset token to validate
     * @return true if the token is valid and can be used for password reset
     */
    boolean isValidResetToken(String token);

    /**
     * Invalidates all existing password reset tokens for a specific user.
     *
     * <p>
     * This method marks all active password reset tokens for a user as used,
     * effectively canceling any pending password reset requests. This is useful
     * when a user successfully logs in or changes their password through other means.
     * </p>
     *
     * @param userId the ID of the user whose tokens should be invalidated
     * @return the number of tokens that were invalidated
     */
    int invalidateUserTokens(Long userId);

    /**
     * Performs cleanup of expired and used password reset tokens.
     *
     * <p>
     * This method removes old password reset tokens from the database to maintain
     * performance and reduce storage requirements. It should be called periodically
     * by a scheduled job to keep the token table clean.
     * </p>
     *
     * @return the number of tokens that were cleaned up
     */
    int cleanupExpiredTokens();
}