package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.EmailVerificationToken;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.exception.EmailException;
import com.yohan.event_planner.exception.ErrorCode;
import com.yohan.event_planner.repository.EmailVerificationTokenRepository;
import com.yohan.event_planner.repository.UserRepository;
import com.yohan.event_planner.time.ClockProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Implementation of the EmailVerificationService interface.
 *
 * <p>
 * This service provides secure email verification functionality with comprehensive
 * security measures including token expiration, single-use tokens, and protection
 * against verification abuse.
 * </p>
 *
 * <h2>Security Implementation</h2>
 * <ul>
 *   <li><strong>Secure Token Generation</strong>: Uses SecureRandom for cryptographically secure tokens</li>
 *   <li><strong>Token Validation</strong>: Comprehensive checks for authenticity, expiry, and usage</li>
 *   <li><strong>Single Use</strong>: Tokens are invalidated immediately after use</li>
 *   <li><strong>Expiration Control</strong>: Configurable token expiration periods</li>
 * </ul>
 *
 * @see EmailVerificationService
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
@Service
@Transactional
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationServiceImpl.class);
    private static final String TOKEN_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TOKEN_LENGTH = 64;

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ClockProvider clockProvider;
    private final SecureRandom secureRandom;

    @Value("${app.email-verification.token-expiry-hours:24}")
    private int tokenExpiryHours;

    /**
     * Constructs a new EmailVerificationServiceImpl with required dependencies.
     * 
     * <p>
     * Initializes the service with all required repositories and services for email verification
     * operations. The SecureRandom instance is created during construction to ensure
     * cryptographically secure token generation throughout the service lifecycle.
     * </p>
     *
     * @param emailVerificationTokenRepository repository for email verification tokens
     * @param userRepository repository for user accounts  
     * @param emailService service for sending emails
     * @param clockProvider provider for current time (enables testing with fixed time)
     * @throws IllegalArgumentException if any required dependency is null (handled by Spring)
     */
    public EmailVerificationServiceImpl(
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            UserRepository userRepository,
            EmailService emailService,
            ClockProvider clockProvider) {
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.clockProvider = clockProvider;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Validates configuration settings after dependency injection.
     * 
     * <p>
     * Ensures that token expiry configuration is valid and logs warnings
     * for any invalid settings while applying safe defaults.
     * </p>
     */
    @PostConstruct
    private void validateConfiguration() {
        if (tokenExpiryHours <= 0) {
            logger.warn("Invalid token expiry configuration: {} hours, using default: 24", tokenExpiryHours);
            tokenExpiryHours = 24;
        }
        logger.info("Email verification service initialized with {}-hour token expiry", tokenExpiryHours);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void generateAndSendVerificationToken(User user) {
        logger.info("Generating email verification token for user: {}", user.getUsername());
        
        try {
            // Invalidate any existing tokens for this user
            int invalidatedCount = emailVerificationTokenRepository.invalidateAllTokensForUser(user);
            if (invalidatedCount > 0) {
                logger.debug("Invalidated {} existing verification tokens for user: {}", invalidatedCount, user.getUsername());
            }

            // Generate new secure token
            String token = generateSecureToken();
            Instant now = clockProvider.getClockForZone(ZoneOffset.UTC).instant();
            Instant expiryDate = now.plus(tokenExpiryHours, ChronoUnit.HOURS);
            logger.debug("Generated secure verification token with {}-hour expiry for user: {}", 
                tokenExpiryHours, user.getUsername());

            // Create and save the token
            EmailVerificationToken verificationToken = new EmailVerificationToken(token, user, now, expiryDate);
            emailVerificationTokenRepository.save(verificationToken);

            // Send verification email
            emailService.sendEmailVerificationEmail(user.getEmail(), user.getFirstName(), token);
            
            logger.info("Email verification token generated and sent for user: {}", user.getUsername());
        } catch (Exception e) {
            handleVerificationException(e, "generate verification token", user.getUsername());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<User> verifyEmail(String token) {
        logger.info("Processing email verification for token");
        
        try {
            Instant now = clockProvider.getClockForZone(ZoneOffset.UTC).instant();
            logger.debug("Validating verification token for timestamp: {}", now);
            Optional<EmailVerificationToken> tokenOptional = emailVerificationTokenRepository.findValidToken(token, now);
            
            if (tokenOptional.isEmpty()) {
                logger.warn("Invalid or expired verification token used");
                throw new EmailException(ErrorCode.INVALID_VERIFICATION_TOKEN, null);
            }

            EmailVerificationToken verificationToken = tokenOptional.get();
            User user = verificationToken.getUser();

            // Check if email is already verified
            if (user.isEmailVerified()) {
                logger.info("Email already verified for user: {}", user.getUsername());
                return Optional.of(user);
            }

            // Mark email as verified
            user.verifyEmail();
            userRepository.save(user);

            // Mark token as used
            verificationToken.markAsUsed();
            emailVerificationTokenRepository.save(verificationToken);

            logger.info("Email successfully verified for user: {}", user.getUsername());
            return Optional.of(user);
        } catch (EmailException e) {
            throw e;
        } catch (Exception e) {
            handleVerificationException(e, "verify email", "unknown user");
        }
        
        // This should never be reached due to exception handling, but required for compilation
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean resendVerificationEmail(User user) {
        logger.info("Resending verification email for user: {}", user.getUsername());
        
        // Check if email is already verified
        if (user.isEmailVerified()) {
            logger.info("Email already verified for user: {}, skipping resend", user.getUsername());
            return false;
        }

        // Generate and send new verification token
        generateAndSendVerificationToken(user);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidVerificationToken(String token) {
        Instant now = clockProvider.getClockForZone(ZoneOffset.UTC).instant();
        return emailVerificationTokenRepository.findValidToken(token, now).isPresent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int invalidateUserVerificationTokens(User user) {
        logger.info("Invalidating all verification tokens for user: {}", user.getUsername());
        return emailVerificationTokenRepository.invalidateAllTokensForUser(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int cleanupExpiredTokens() {
        logger.info("Starting cleanup of expired email verification tokens");
        
        Instant now = clockProvider.getClockForZone(ZoneOffset.UTC).instant();
        logger.debug("Starting cleanup with current time: {}", now);
        
        int expiredCount = emailVerificationTokenRepository.deleteExpiredTokens(now);
        int usedCount = emailVerificationTokenRepository.deleteUsedTokens();
        
        int totalCleaned = expiredCount + usedCount;
        logger.debug("Cleanup operation details - expired: {}, used: {}, timestamp: {}", 
                   expiredCount, usedCount, now);
        logger.info("Cleanup completed: {} expired tokens, {} used tokens, {} total cleaned", 
                   expiredCount, usedCount, totalCleaned);
        
        return totalCleaned;
    }

    /**
     * Generates a cryptographically secure random token for email verification.
     *
     * @return a secure random token string
     */
    private String generateSecureToken() {
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            int randomIndex = secureRandom.nextInt(TOKEN_CHARACTERS.length());
            token.append(TOKEN_CHARACTERS.charAt(randomIndex));
        }
        return token.toString();
    }

    /**
     * Handles exceptions during verification operations with consistent logging and error conversion.
     * 
     * <p>
     * Provides centralized exception handling for verification operations, ensuring consistent
     * error reporting and logging while preserving original EmailExceptions.
     * </p>
     *
     * @param e the exception that occurred
     * @param operation the operation being performed when the exception occurred
     * @param username the username associated with the operation (for logging)
     * @throws EmailException always throws an EmailException with appropriate error code
     */
    private void handleVerificationException(Exception e, String operation, String username) {
        if (e instanceof EmailException) {
            throw (EmailException) e;
        }
        logger.error("Failed to {} for user: {} - {}", operation, username, e.getMessage());
        if (operation.contains("generate")) {
            throw new EmailException("Failed to send email verification email");
        } else {
            throw new EmailException(ErrorCode.VERIFICATION_FAILED, null);
        }
    }
}