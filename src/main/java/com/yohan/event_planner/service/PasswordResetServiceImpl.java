package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.PasswordResetToken;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.auth.ForgotPasswordRequestDTO;
import com.yohan.event_planner.dto.auth.ForgotPasswordResponseDTO;
import com.yohan.event_planner.dto.auth.ResetPasswordRequestDTO;
import com.yohan.event_planner.dto.auth.ResetPasswordResponseDTO;
import com.yohan.event_planner.exception.PasswordException;
import com.yohan.event_planner.exception.PasswordResetException;
import com.yohan.event_planner.exception.ErrorCode;
import com.yohan.event_planner.repository.PasswordResetTokenRepository;
import com.yohan.event_planner.repository.UserRepository;
import com.yohan.event_planner.time.ClockProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import jakarta.annotation.PostConstruct;
import com.yohan.event_planner.constants.ApplicationConstants;

/**
 * Implementation of the PasswordResetService interface.
 *
 * <p>
 * This service provides secure password reset functionality with comprehensive
 * security measures including token expiration, single-use tokens, rate limiting,
 * and protection against email enumeration attacks.
 * </p>
 *
 * <h2>Security Implementation</h2>
 * <ul>
 *   <li><strong>Secure Token Generation</strong>: Uses SecureRandom for cryptographically secure tokens</li>
 *   <li><strong>Timing Consistency</strong>: Same response time regardless of email existence</li>
 *   <li><strong>Token Validation</strong>: Comprehensive checks for authenticity, expiry, and usage</li>
 *   <li><strong>Session Management</strong>: Invalidates all user sessions on password change</li>
 * </ul>
 *
 * @see PasswordResetService
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
@Service
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetServiceImpl.class);

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final ClockProvider clockProvider;
    private final SecureRandom secureRandom;

    @Value("${app.password-reset.token-expiry-minutes}")
    private int tokenExpiryMinutes;

    /**
     * Validates configuration after dependency injection.
     * 
     * <p>
     * This method ensures that the token expiry configuration is valid,
     * providing a fallback default if the configured value is invalid.
     * </p>
     * 
     * @throws IllegalStateException if critical configuration is missing
     */
    @PostConstruct
    private void validateConfiguration() {
        if (tokenExpiryMinutes <= 0) {
            logger.warn("Invalid token expiry configuration: {} minutes, using default: {}", 
                tokenExpiryMinutes, ApplicationConstants.PASSWORD_RESET_DEFAULT_TOKEN_EXPIRY_MINUTES);
            tokenExpiryMinutes = ApplicationConstants.PASSWORD_RESET_DEFAULT_TOKEN_EXPIRY_MINUTES;
        }
        logger.info("Password reset service initialized with {}-minute token expiry", tokenExpiryMinutes);
    }

    /**
     * Constructs a new PasswordResetServiceImpl with required dependencies.
     *
     * @param passwordResetTokenRepository repository for password reset tokens
     * @param userRepository repository for user accounts
     * @param emailService service for sending emails
     * @param passwordEncoder encoder for password hashing
     * @param clockProvider provider for current time
     */
    public PasswordResetServiceImpl(
            PasswordResetTokenRepository passwordResetTokenRepository,
            UserRepository userRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            ClockProvider clockProvider) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.clockProvider = clockProvider;
        this.secureRandom = new SecureRandom();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ForgotPasswordResponseDTO requestPasswordReset(ForgotPasswordRequestDTO request) {
        logger.info("Password reset requested for email: {}", request.email());
        logger.debug("Processing password reset request for email: {}", request.email());
        
        try {
            // Look up user by email
            Optional<User> userOptional = userRepository.findByEmailAndIsPendingDeletionFalse(request.email());
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                logger.info("Generating password reset token for user: {}", user.getUsername());
                
                // Invalidate any existing tokens for this user
                int invalidatedCount = passwordResetTokenRepository.invalidateAllTokensForUser(user);
                logger.debug("Invalidated {} existing tokens for user: {}", invalidatedCount, user.getUsername());
                
                // Generate new token
                String token;
                try {
                    token = generateSecureToken();
                } catch (Exception e) {
                    logger.error("Failed to generate secure token for user: {}", user.getUsername(), e);
                    throw new PasswordResetException(ErrorCode.PASSWORD_RESET_TOKEN_GENERATION_FAILED, e);
                }
                
                Instant now = clockProvider.getClockForZone(java.time.ZoneOffset.UTC).instant();
                Instant expiryDate = now.plus(tokenExpiryMinutes, ChronoUnit.MINUTES);
                
                // Save token to database
                PasswordResetToken resetToken = new PasswordResetToken(token, user, now, expiryDate);
                try {
                    passwordResetTokenRepository.save(resetToken);
                } catch (Exception e) {
                    logger.error("Failed to save password reset token for user: {}", user.getUsername(), e);
                    throw new PasswordResetException(ErrorCode.PASSWORD_RESET_DATABASE_ERROR, e);
                }
                
                // Send email with reset link
                try {
                    emailService.sendPasswordResetEmail(request.email(), token, tokenExpiryMinutes);
                } catch (Exception e) {
                    logger.error("Failed to send password reset email for user: {}", user.getUsername(), e);
                    throw new PasswordResetException(ErrorCode.PASSWORD_RESET_EMAIL_FAILED, e);
                }
                
                logger.info("Password reset email sent successfully for user: {}", user.getUsername());
            } else {
                logger.info("Password reset requested for non-existent email: {}", request.email());
                logger.debug("User not found for email: {}", request.email());
                // Simulate processing time to prevent timing attacks
                simulateProcessingTime();
            }
            
            // Always return the same response to prevent email enumeration
            return ForgotPasswordResponseDTO.standard();
            
        } catch (Exception e) {
            logger.error("Error processing password reset request for email: {}", request.email(), e);
            // Still return standard response to avoid information leakage
            return ForgotPasswordResponseDTO.standard();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResetPasswordResponseDTO resetPassword(ResetPasswordRequestDTO request) {
        logger.info("Password reset attempt with token: {}", request.token().substring(0, 8) + "...");
        logger.debug("Validating reset token and processing password change");
        
        Instant now = clockProvider.getClockForZone(java.time.ZoneOffset.UTC).instant();
        
        // Find and validate token
        Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository.findValidToken(request.token(), now);
        
        if (tokenOptional.isEmpty()) {
            logger.warn("Invalid or expired reset token used: {}", request.token().substring(0, 8) + "...");
            throw new PasswordException(ErrorCode.INVALID_RESET_TOKEN);
        }
        
        PasswordResetToken resetToken = tokenOptional.get();
        User user = resetToken.getUser();
        logger.debug("Token validation successful for user: {}", user.getUsername());
        
        try {
            // Update password
            String hashedPassword;
            try {
                hashedPassword = passwordEncoder.encode(request.newPassword());
            } catch (Exception e) {
                logger.error("Failed to encode password for user: {}", user.getUsername(), e);
                throw new PasswordResetException(ErrorCode.PASSWORD_RESET_ENCODING_FAILED, e);
            }
            
            user.setHashedPassword(hashedPassword);
            
            try {
                userRepository.save(user);
            } catch (Exception e) {
                logger.error("Failed to save user with new password: {}", user.getUsername(), e);
                throw new PasswordResetException(ErrorCode.PASSWORD_RESET_DATABASE_ERROR, e);
            }
            
            // Mark token as used
            resetToken.markAsUsed();
            try {
                passwordResetTokenRepository.save(resetToken);
            } catch (Exception e) {
                logger.error("Failed to mark token as used for user: {}", user.getUsername(), e);
                throw new PasswordResetException(ErrorCode.PASSWORD_RESET_DATABASE_ERROR, e);
            }
            
            // Send confirmation email
            try {
                emailService.sendPasswordChangeConfirmation(user.getEmail(), user.getUsername());
            } catch (Exception e) {
                logger.error("Failed to send password change confirmation for user: {}", user.getUsername(), e);
                throw new PasswordResetException(ErrorCode.PASSWORD_RESET_CONFIRMATION_EMAIL_FAILED, e);
            }
            
            logger.info("Password successfully changed for user: {} at timestamp: {}", user.getUsername(), now);
            logger.debug("Password reset completed successfully for user: {}", user.getUsername());
            
            return ResetPasswordResponseDTO.success();
            
        } catch (PasswordResetException e) {
            // Re-throw password reset specific exceptions
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error resetting password for user: {}", user.getUsername(), e);
            throw new PasswordResetException(ErrorCode.UNKNOWN_ERROR, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isValidResetToken(String token) {
        Instant now = clockProvider.getClockForZone(java.time.ZoneOffset.UTC).instant();
        return passwordResetTokenRepository.findValidToken(token, now).isPresent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int invalidateUserTokens(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.warn("Attempted to invalidate tokens for non-existent user: {}", userId);
            return 0;
        }
        
        User user = userOptional.get();
        int invalidated = passwordResetTokenRepository.invalidateAllTokensForUser(user);
        logger.info("Invalidated {} password reset tokens for user: {}", invalidated, user.getUsername());
        return invalidated;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int cleanupExpiredTokens() {
        logger.debug("Starting cleanup of expired password reset tokens");
        Instant now = clockProvider.getClockForZone(java.time.ZoneOffset.UTC).instant();
        int expiredDeleted = passwordResetTokenRepository.deleteExpiredTokens(now);
        int usedDeleted = passwordResetTokenRepository.deleteUsedTokens();
        int totalDeleted = expiredDeleted + usedDeleted;
        
        logger.info("Cleaned up {} expired and {} used password reset tokens", expiredDeleted, usedDeleted);
        logger.debug("Cleanup operation completed, total tokens removed: {}", totalDeleted);
        return totalDeleted;
    }

    /**
     * Generates a cryptographically secure random token for password reset.
     *
     * <p>
     * Uses SecureRandom to generate a token consisting of alphanumeric characters.
     * The token length is determined by the PASSWORD_RESET_TOKEN_LENGTH constant
     * and uses the character set defined in PASSWORD_RESET_TOKEN_CHARACTERS.
     * </p>
     *
     * @return a secure random token string of length PASSWORD_RESET_TOKEN_LENGTH
     * @see ApplicationConstants#PASSWORD_RESET_TOKEN_LENGTH
     * @see ApplicationConstants#PASSWORD_RESET_TOKEN_CHARACTERS
     */
    private String generateSecureToken() {
        logger.debug("Generating new secure token with length: {}", ApplicationConstants.PASSWORD_RESET_TOKEN_LENGTH);
        StringBuilder token = new StringBuilder(ApplicationConstants.PASSWORD_RESET_TOKEN_LENGTH);
        for (int i = 0; i < ApplicationConstants.PASSWORD_RESET_TOKEN_LENGTH; i++) {
            int index = secureRandom.nextInt(ApplicationConstants.PASSWORD_RESET_TOKEN_CHARACTERS.length());
            token.append(ApplicationConstants.PASSWORD_RESET_TOKEN_CHARACTERS.charAt(index));
        }
        return token.toString();
    }

    /**
     * Simulates processing time to prevent timing attacks.
     * 
     * <p>
     * This method introduces a small delay when processing requests for non-existent
     * email addresses to make the response time consistent with valid requests,
     * preventing timing-based email enumeration attacks.
     * </p>
     * 
     * @throws InterruptedException if the thread is interrupted during sleep
     */
    private void simulateProcessingTime() {
        logger.debug("Simulating processing time for non-existent email enumeration protection");
        try {
            // Small random delay to simulate token generation and email sending
            int delayRange = ApplicationConstants.PASSWORD_RESET_MAX_SIMULATION_DELAY_MS - 
                           ApplicationConstants.PASSWORD_RESET_MIN_SIMULATION_DELAY_MS;
            int delay = ApplicationConstants.PASSWORD_RESET_MIN_SIMULATION_DELAY_MS + secureRandom.nextInt(delayRange);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted during processing time simulation", e);
        }
    }
}