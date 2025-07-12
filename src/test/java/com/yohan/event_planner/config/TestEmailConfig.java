package com.yohan.event_planner.config;

import com.yohan.event_planner.service.EmailService;
import com.yohan.event_planner.service.EmailDomainValidationService;
import com.yohan.event_planner.service.RateLimitingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration for email services, domain validation, and rate limiting during tests.
 * This configuration provides mock implementations to avoid actual email sending, 
 * allow test domains, and disable rate limiting during tests.
 */
@TestConfiguration
public class TestEmailConfig {

    private static final Logger logger = LoggerFactory.getLogger(TestEmailConfig.class);

    @Bean
    @Primary
    public EmailService mockEmailService() {
        return new EmailService() {
            @Override
            public void sendEmailVerificationEmail(String toEmail, String firstName, String verificationToken) {
                logger.info("Mock email verification email would be sent to: {} with token: {}", toEmail, verificationToken);
                // Mock implementation - don't actually send email
            }

            @Override
            public void sendPasswordResetEmail(String toEmail, String resetToken, int expiryMinutes) {
                logger.info("Mock password reset email would be sent to: {} with token: {}", toEmail, resetToken);
                // Mock implementation - don't actually send email
            }

            @Override
            public void sendWelcomeEmail(String toEmail, String username) {
                logger.info("Mock welcome email would be sent to: {} for user: {}", toEmail, username);
                // Mock implementation - don't actually send email
            }

            @Override
            public void sendPasswordChangeConfirmation(String toEmail, String username) {
                logger.info("Mock password change confirmation would be sent to: {} for user: {}", toEmail, username);
                // Mock implementation - don't actually send email
            }
        };
    }

    @Bean
    @Primary
    public EmailDomainValidationService mockEmailDomainValidationService() {
        return new EmailDomainValidationService() {
            @Override
            public boolean isEmailDomainValid(String emailAddress) {
                // Allow all domains in tests, including example.com and test domains
                logger.debug("Mock email domain validation - allowing all domains for tests: {}", emailAddress);
                return true;
            }

            @Override
            public String extractDomain(String emailAddress) {
                if (emailAddress == null || !emailAddress.contains("@")) {
                    return null;
                }
                return emailAddress.substring(emailAddress.lastIndexOf('@') + 1).toLowerCase();
            }

            @Override
            public boolean isDisposableEmailDomain(String domain) {
                // Never block domains in tests
                return false;
            }

            @Override
            public boolean isSuspiciousDomain(String domain) {
                // Never flag domains as suspicious in tests
                return false;
            }

            @Override
            public String getValidationFailureReason(String emailAddress) {
                // Never fail validation in tests
                return null;
            }
        };
    }

    @Bean
    @Primary
    public RateLimitingService mockRateLimitingService() {
        return new RateLimitingService() {
            @Override
            public boolean isRegistrationAllowed(String ipAddress) {
                // Always allow registration in tests
                logger.debug("Mock rate limiting - allowing registration for IP: {}", ipAddress);
                return true;
            }

            @Override
            public void recordRegistrationAttempt(String ipAddress) {
                // No-op in tests
                logger.debug("Mock rate limiting - recording registration attempt for IP: {}", ipAddress);
            }

            @Override
            public int getRemainingRegistrationAttempts(String ipAddress) {
                // Always return max attempts available in tests
                return 5;
            }

            @Override
            public long getRegistrationRateLimitResetTime(String ipAddress) {
                // No rate limit active in tests
                return 0;
            }

            @Override
            public boolean isLoginAllowed(String ipAddress) {
                // Always allow login in tests
                logger.debug("Mock rate limiting - allowing login for IP: {}", ipAddress);
                return true;
            }

            @Override
            public void recordLoginAttempt(String ipAddress) {
                // No-op in tests
                logger.debug("Mock rate limiting - recording login attempt for IP: {}", ipAddress);
            }

            @Override
            public int getRemainingLoginAttempts(String ipAddress) {
                // Always return max attempts available in tests
                return 10;
            }

            @Override
            public long getLoginRateLimitResetTime(String ipAddress) {
                // No rate limit active in tests
                return 0;
            }

            @Override
            public boolean isPasswordResetAllowed(String ipAddress) {
                // Always allow password reset in tests
                logger.debug("Mock rate limiting - allowing password reset for IP: {}", ipAddress);
                return true;
            }

            @Override
            public void recordPasswordResetAttempt(String ipAddress) {
                // No-op in tests
                logger.debug("Mock rate limiting - recording password reset attempt for IP: {}", ipAddress);
            }

            @Override
            public int getRemainingPasswordResetAttempts(String ipAddress) {
                // Always return max attempts available in tests
                return 3;
            }

            @Override
            public long getPasswordResetRateLimitResetTime(String ipAddress) {
                // No rate limit active in tests
                return 0;
            }

            @Override
            public boolean isEmailVerificationAllowed(String ipAddress) {
                // Always allow email verification in tests
                logger.debug("Mock rate limiting - allowing email verification for IP: {}", ipAddress);
                return true;
            }

            @Override
            public void recordEmailVerificationAttempt(String ipAddress) {
                // No-op in tests
                logger.debug("Mock rate limiting - recording email verification attempt for IP: {}", ipAddress);
            }

            @Override
            public int getRemainingEmailVerificationAttempts(String ipAddress) {
                // Always return max attempts available in tests
                return 5;
            }

            @Override
            public long getEmailVerificationRateLimitResetTime(String ipAddress) {
                // No rate limit active in tests
                return 0;
            }
        };
    }
}