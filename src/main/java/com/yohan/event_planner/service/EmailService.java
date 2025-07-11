package com.yohan.event_planner.service;

/**
 * Service interface for email operations in the Event Planner application.
 *
 * <p>
 * This interface defines the contract for email-related functionality, including
 * password reset emails, account notifications, and other transactional emails.
 * Implementations should handle SMTP configuration, email templating, and
 * error handling for email delivery.
 * </p>
 *
 * <h2>Email Types Supported</h2>
 * <ul>
 *   <li><strong>Email Verification</strong>: Account activation emails for new users</li>
 *   <li><strong>Password Reset</strong>: Secure token-based password recovery emails</li>
 *   <li><strong>Account Notifications</strong>: Welcome emails, account changes, etc.</li>
 *   <li><strong>System Alerts</strong>: Security notifications and system updates</li>
 * </ul>
 *
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li><strong>Token Security</strong>: Secure handling of sensitive reset tokens</li>
 *   <li><strong>Rate Limiting</strong>: Prevention of email spam and abuse</li>
 *   <li><strong>Content Validation</strong>: Sanitization of email content</li>
 *   <li><strong>Delivery Tracking</strong>: Monitoring of email delivery success</li>
 * </ul>
 *
 * @see EmailServiceImpl
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
public interface EmailService {

    /**
     * Sends an email verification email to the specified email address.
     *
     * <p>
     * This method generates and sends an email verification email containing a secure
     * verification link that allows the user to activate their account. The email includes
     * instructions and information about the verification process.
     * </p>
     *
     * <h3>Email Content</h3>
     * <ul>
     *   <li><strong>Subject</strong>: Clear indication of email verification request</li>
     *   <li><strong>Verification Link</strong>: Secure token embedded in web URL</li>
     *   <li><strong>Expiry Notice</strong>: Information about token expiration time</li>
     *   <li><strong>Account Info</strong>: Confirmation of registration details</li>
     * </ul>
     *
     * <h3>Verification Link Format</h3>
     * <p>
     * The generated verification link follows the format configured in application properties:
     * {@code https://app.example.com/verify-email?token=SECURE_TOKEN_HERE}
     * </p>
     *
     * @param toEmail the recipient's email address
     * @param firstName the user's first name for personalization
     * @param verificationToken the secure email verification token
     * @throws com.yohan.event_planner.exception.EmailException if email sending fails
     */
    void sendEmailVerificationEmail(String toEmail, String firstName, String verificationToken);

    /**
     * Sends a password reset email to the specified email address.
     *
     * <p>
     * This method generates and sends a password reset email containing a secure
     * deep link that allows the user to reset their password. The email includes
     * instructions and security warnings about the token's limited lifespan.
     * </p>
     *
     * <h3>Email Content</h3>
     * <ul>
     *   <li><strong>Subject</strong>: Clear indication of password reset request</li>
     *   <li><strong>Deep Link</strong>: Secure token embedded in app-specific URL</li>
     *   <li><strong>Expiry Notice</strong>: Information about token expiration time</li>
     *   <li><strong>Security Warning</strong>: Advice if user didn't request reset</li>
     * </ul>
     *
     * <h3>Deep Link Format</h3>
     * <p>
     * The generated deep link follows the format configured in application properties:
     * {@code myapp://reset-password?token=SECURE_TOKEN_HERE}
     * </p>
     *
     * @param toEmail the recipient's email address
     * @param resetToken the secure password reset token
     * @param expiryMinutes the number of minutes until token expires
     * @throws com.yohan.event_planner.exception.EmailException if email sending fails
     */
    void sendPasswordResetEmail(String toEmail, String resetToken, int expiryMinutes);

    /**
     * Sends a welcome email to newly registered users.
     *
     * <p>
     * This method sends a welcome email to users who have successfully registered
     * for the Event Planner application. The email includes getting started
     * information and links to helpful resources.
     * </p>
     *
     * @param toEmail the recipient's email address
     * @param username the user's display name
     * @throws com.yohan.event_planner.exception.EmailException if email sending fails
     */
    void sendWelcomeEmail(String toEmail, String username);

    /**
     * Sends a password change confirmation email.
     *
     * <p>
     * This method sends a confirmation email after a user successfully changes
     * their password, either through the reset process or account settings.
     * This provides security notification of the password change.
     * </p>
     *
     * @param toEmail the recipient's email address
     * @param username the user's display name
     * @throws com.yohan.event_planner.exception.EmailException if email sending fails
     */
    void sendPasswordChangeConfirmation(String toEmail, String username);
}