package com.yohan.event_planner.service;

import com.yohan.event_planner.constants.ApplicationConstants;
import com.yohan.event_planner.exception.EmailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Implementation of the EmailService interface for sending transactional emails.
 *
 * <p>
 * This service handles all email operations for the Event Planner application,
 * including password reset emails, welcome messages, and security notifications.
 * It uses Spring's JavaMailSender with SMTP configuration for reliable email delivery.
 * </p>
 *
 * <h2>Email Features</h2>
 * <ul>
 *   <li><strong>HTML Email Support</strong>: Rich formatting for better user experience</li>
 *   <li><strong>Template-based Content</strong>: Consistent branding and messaging</li>
 *   <li><strong>Security Best Practices</strong>: Secure token handling and warnings</li>
 *   <li><strong>Error Handling</strong>: Comprehensive logging and exception management</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>
 * Email settings are configured through application properties, including
 * SMTP server details, sender information, and password reset parameters.
 * This allows for easy environment-specific configuration.
 * </p>
 *
 * @see EmailService
 * @see JavaMailSender
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    
    @Value("${app.password-reset.from-email}")
    private String fromEmail;
    
    @Value("${app.password-reset.from-name}")
    private String fromName;
    
    @Value("${app.password-reset.deep-link-base}")
    private String deepLinkBase;
    
    @Value("${app.email-verification.web-link-base:https://localhost:3000/verify-email}")
    private String verificationLinkBase;

    /**
     * Constructs an EmailService with the required dependencies.
     * 
     * <p>
     * Initializes the service with Spring's JavaMailSender for SMTP operations.
     * Configuration properties are injected via @Value annotations and validated
     * at startup to ensure all required email settings are present.
     * </p>
     *
     * @param mailSender the JavaMailSender for sending emails
     * @throws IllegalArgumentException if mailSender is null (handled by Spring)
     */
    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        logger.debug("Email service initialized with configuration - fromEmail configured: {}, deepLinkBase configured: {}", 
            fromEmail != null, deepLinkBase != null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendEmailVerificationEmail(String toEmail, String firstName, String verificationToken) {
        try {
            logger.info("Sending email verification email to: {}", toEmail);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(ApplicationConstants.EMAIL_VERIFICATION_SUBJECT);
            
            String verificationLink = verificationLinkBase + "?token=" + verificationToken;
            logger.debug("Generating email verification content for user: {}", firstName);
            String htmlContent = createEmailVerificationContent(firstName, verificationLink);
            
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("Email verification email sent successfully to: {}", toEmail);
            
        } catch (MessagingException e) {
            logger.error("Failed to send email verification email to: {}", toEmail, e);
            throw new EmailException("Failed to send email verification email", e);
        } catch (Exception e) {
            logger.error("Unexpected error sending email verification email to: {}", toEmail, e);
            throw new EmailException("Unexpected error sending email verification email", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken, int expiryMinutes) {
        try {
            logger.info("Sending password reset email to: {}", toEmail);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(ApplicationConstants.PASSWORD_RESET_SUBJECT);
            
            String resetLink = deepLinkBase + "?token=" + resetToken;
            logger.debug("Generating password reset content for {} minute expiry", expiryMinutes);
            String htmlContent = createPasswordResetEmailContent(resetLink, expiryMinutes);
            
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("Password reset email sent successfully to: {}", toEmail);
            
        } catch (MessagingException e) {
            logger.error("Failed to send password reset email to: {}", toEmail, e);
            throw new EmailException("Failed to send password reset email", e);
        } catch (Exception e) {
            logger.error("Unexpected error sending password reset email to: {}", toEmail, e);
            throw new EmailException("Unexpected error sending password reset email", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendWelcomeEmail(String toEmail, String username) {
        try {
            logger.info("Sending welcome email to: {}", toEmail);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(ApplicationConstants.WELCOME_EMAIL_SUBJECT);
            
            logger.debug("Generating welcome email content for user: {}", username);
            String htmlContent = createWelcomeEmailContent(username);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("Welcome email sent successfully to: {}", toEmail);
            
        } catch (MessagingException e) {
            logger.error("Failed to send welcome email to: {}", toEmail, e);
            throw new EmailException("Failed to send welcome email", e);
        } catch (Exception e) {
            logger.error("Unexpected error sending welcome email to: {}", toEmail, e);
            throw new EmailException("Unexpected error sending welcome email", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendPasswordChangeConfirmation(String toEmail, String username) {
        try {
            logger.info("Sending password change confirmation to: {}", toEmail);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(ApplicationConstants.PASSWORD_CHANGE_SUBJECT);
            
            logger.debug("Generating password change confirmation content for user: {}", username);
            String htmlContent = createPasswordChangeConfirmationContent(username);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("Password change confirmation sent successfully to: {}", toEmail);
            
        } catch (MessagingException e) {
            logger.error("Failed to send password change confirmation to: {}", toEmail, e);
            throw new EmailException("Failed to send password change confirmation", e);
        } catch (Exception e) {
            logger.error("Unexpected error sending password change confirmation to: {}", toEmail, e);
            throw new EmailException("Unexpected error sending password change confirmation", e);
        }
    }

    /**
     * Creates the HTML content for email verification emails.
     *
     * @param firstName the user's first name for personalization
     * @param verificationLink the web link containing the verification token
     * @return formatted HTML email content
     */
    private String createEmailVerificationContent(String firstName, String verificationLink) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Verify Your Email Address</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #28a745; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #28a745; 
                             color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .info { background-color: #d1ecf1; border-left: 4px solid #bee5eb; padding: 10px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to Ayoboyo!</h1>
                    </div>
                    <div class="content">
                        <h2>Hi %s,</h2>
                        <p>Thank you for registering with Event Planner! We're excited to have you on board.</p>
                        
                        <p>To complete your registration and activate your account, please verify your email address by clicking the button below:</p>
                        
                        <a href="%s" class="button">Verify Email Address</a>
                        
                        <div class="info">
                            <strong>ℹ️ What happens after verification:</strong>
                            <ul>
                                <li>Your account will be activated</li>
                                <li>You'll be able to sign in and start planning events</li>
                                <li>You'll receive important notifications about your events</li>
                            </ul>
                        </div>
                        
                        <p>If the button above doesn't work, you can copy and paste this link into your browser:</p>
                        <p style="word-break: break-all; background-color: #f8f9fa; padding: 10px; border-radius: 4px;">%s</p>
                        
                        <p><strong>Important:</strong> This verification link will expire in 24 hours. If you don't verify your email within this time, you'll need to register again.</p>
                        
                        <p>If you didn't create an account with us, please ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from Event Planner. Please do not reply to this email.</p>
                        <p>If you need help, contact our support team.</p>
                    </div>
                </div>
            </body>
            </html>
            """, firstName, verificationLink, verificationLink);
    }

    /**
     * Creates the HTML content for password reset emails.
     *
     * @param resetLink the deep link containing the reset token
     * @param expiryMinutes the number of minutes until token expires
     * @return formatted HTML email content
     */
    private String createPasswordResetEmailContent(String resetLink, int expiryMinutes) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Reset Your Password</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #007bff; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #007bff; 
                             color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .warning { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 10px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Reset Your Password</h1>
                    </div>
                    <div class="content">
                        <h2>Password Reset Request</h2>
                        <p>We received a request to reset your password for your Event Planner account.</p>
                        
                        <p>To reset your password, tap the button below or copy the link into your app:</p>
                        
                        <a href="%s" class="button">Reset Password</a>
                        
                        <p><strong>Link:</strong> %s</p>
                        
                        <div class="warning">
                            <strong>⚠️ Security Notice:</strong>
                            <ul>
                                <li>This link will expire in <strong>%d minutes</strong></li>
                                <li>This link can only be used once</li>
                                <li>If you didn't request this reset, please ignore this email</li>
                                <li>Never share this link with anyone</li>
                            </ul>
                        </div>
                        
                        <p>If you're having trouble with the button above, copy and paste the link into your Event Planner app.</p>
                        
                        <p>For security reasons, this link will automatically expire in %d minutes.</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from Event Planner. Please do not reply to this email.</p>
                        <p>If you need help, contact our support team.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(resetLink, resetLink, expiryMinutes, expiryMinutes);
    }

    /**
     * Creates the HTML content for welcome emails.
     *
     * @param username the user's display name
     * @return formatted HTML email content
     */
    private String createWelcomeEmailContent(String username) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Welcome to Event Planner</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #28a745; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to Event Planner!</h1>
                    </div>
                    <div class="content">
                        <h2>Hi %s,</h2>
                        <p>Welcome to Event Planner! Your account has been successfully created and you're ready to start organizing your events.</p>
                        
                        <h3>Getting Started:</h3>
                        <ul>
                            <li>Create your first event</li>
                            <li>Set up recurring events for regular activities</li>
                            <li>Organize events with labels and badges</li>
                            <li>Track your time and productivity</li>
                        </ul>
                        
                        <p>If you have any questions or need help getting started, don't hesitate to reach out to our support team.</p>
                        
                        <p>Happy planning!</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from Event Planner.</p>
                        <p>© 2024 Event Planner. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username);
    }

    /**
     * Creates the HTML content for password change confirmation emails.
     *
     * @param username the user's display name
     * @return formatted HTML email content
     */
    private String createPasswordChangeConfirmationContent(String username) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Password Changed Successfully</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #28a745; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .warning { background-color: #f8d7da; border-left: 4px solid #dc3545; padding: 10px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Password Changed Successfully</h1>
                    </div>
                    <div class="content">
                        <h2>Hi %s,</h2>
                        <p>This email confirms that your password has been successfully changed for your Event Planner account.</p>
                        
                        <p><strong>When:</strong> Just now</p>
                        <p><strong>Account:</strong> %s</p>
                        
                        <div class="warning">
                            <strong>⚠️ Didn't make this change?</strong>
                            <p>If you didn't change your password, please contact our support team immediately. Your account security may be compromised.</p>
                        </div>
                        
                        <p>For your security, you've been logged out of all devices. Please log in again with your new password.</p>
                        
                        <p>Thank you for keeping your account secure!</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated security notification from Event Planner.</p>
                        <p>© 2024 Event Planner. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, username);
    }
}