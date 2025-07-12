package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.RefreshToken;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.auth.RefreshTokenResponseDTO;
import com.yohan.event_planner.exception.UserNotFoundException;
import com.yohan.event_planner.exception.UnauthorizedException;
import com.yohan.event_planner.repository.RefreshTokenRepository;
import com.yohan.event_planner.repository.UserRepository;
import com.yohan.event_planner.security.CustomUserDetails;
import com.yohan.event_planner.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_ACCESS;

/**
 * Implementation of {@link RefreshTokenService} providing secure refresh token management.
 *
 * <p>
 * This implementation follows security best practices including:
 * <ul>
 *   <li>One-time use token rotation to prevent replay attacks</li>
 *   <li>Secure token hashing using cryptographic functions</li>
 *   <li>Immediate revocation of used tokens</li>
 *   <li>Comprehensive audit logging for security monitoring</li>
 * </ul>
 * </p>
 *
 * <h2>Security Architecture Integration</h2>
 * <p>
 * This service integrates with the broader security architecture by collaborating with:
 * </p>
 * <ul>
 *   <li>{@link JwtUtils} - Cryptographic token operations (generation, hashing, validation)</li>
 *   <li>{@link RefreshTokenRepository} - Persistent token storage with optimized queries</li>
 *   <li>{@link UserRepository} - User validation for token refresh operations</li>
 *   <li>{@link com.yohan.event_planner.jobs.TokenCleanupJob} - Automated maintenance of token hygiene</li>
 * </ul>
 *
 * @see RefreshTokenService
 * @see com.yohan.event_planner.jobs.TokenCleanupJob
 */
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository,
                                   UserRepository userRepository,
                                   JwtUtils jwtUtils) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Generates a cryptographically secure token, hashes it for storage, and persists
     * the token entity with the specified expiration time.
     * </p>
     *
     * @param userId the ID of the user to create the token for, must not be null
     * @return the raw (unhashed) refresh token for client storage
     * @throws IllegalArgumentException if userId is null
     */
    @Override
    @Transactional
    public String createRefreshToken(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        logger.debug("Creating refresh token for user ID: {}", userId);
        
        // Generate raw token
        String rawToken = jwtUtils.generateRefreshToken();
        
        // Hash token for storage
        String hashedToken = jwtUtils.hashRefreshToken(rawToken);
        
        // Calculate expiry
        Instant expiryDate = Instant.now().plusMillis(jwtUtils.getRefreshTokenExpirationMs());
        
        // Create and save refresh token entity
        RefreshToken refreshToken = new RefreshToken(hashedToken, userId, expiryDate);
        refreshTokenRepository.save(refreshToken);
        
        logger.debug("Created refresh token for user ID: {}, expires at: {}", userId, expiryDate);
        
        // Return raw token to client
        return rawToken;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Implements secure token rotation by immediately revoking the provided token
     * and generating a new token pair. This prevents token reuse attacks.
     * </p>
     *
     * @param refreshToken the refresh token to validate and rotate, must not be null or empty
     * @return a new token pair containing access and refresh tokens
     * @throws UnauthorizedException if the token is invalid, expired, or revoked
     * @throws UserNotFoundException if the token's associated user is not found
     * @throws IllegalArgumentException if refreshToken is null or empty
     */
    @Override
    @Transactional
    public RefreshTokenResponseDTO refreshTokens(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Refresh token cannot be null or empty");
        }
        
        logger.debug("Processing token refresh request");
        
        // Find token using optimized lookup
        Optional<RefreshToken> tokenEntity = findTokenByRawValue(refreshToken);
        
        if (tokenEntity.isEmpty()) {
            logger.warn("Token refresh failed: invalid or non-existent token provided");
            throw new UnauthorizedException(UNAUTHORIZED_ACCESS);
        }
        
        RefreshToken token = tokenEntity.get();
        
        // Validate token
        if (!token.isValid()) {
            logger.warn("Token refresh failed: expired or revoked token for user ID: {}", token.getUserId());
            throw new UnauthorizedException(UNAUTHORIZED_ACCESS);
        }
        
        // Immediately revoke the used token (one-time use)
        token.setRevoked(true);
        refreshTokenRepository.save(token);
        
        logger.debug("Revoked used refresh token for user ID: {}", token.getUserId());
        
        // Get user details for new token generation
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new UserNotFoundException(token.getUserId()));
        
        // Generate new tokens
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String newAccessToken = jwtUtils.generateToken(userDetails);
        String newRefreshToken = createRefreshToken(user.getId());
        
        logger.info("Successfully refreshed tokens for user ID: {}", user.getId());
        
        return new RefreshTokenResponseDTO(newAccessToken, newRefreshToken);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Safely handles revocation attempts for non-existent tokens without throwing
     * exceptions, making this method idempotent.
     * </p>
     *
     * @param refreshToken the refresh token to revoke, may be null or invalid
     */
    @Override
    @Transactional
    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            logger.warn("Attempted to revoke null or empty refresh token");
            return;
        }
        
        logger.debug("Processing refresh token revocation");
        
        // Find and revoke the token using optimized lookup
        Optional<RefreshToken> tokenEntity = findTokenByRawValue(refreshToken);
        
        if (tokenEntity.isPresent()) {
            RefreshToken token = tokenEntity.get();
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            logger.info("Successfully revoked refresh token for user ID: {}", token.getUserId());
        } else {
            logger.warn("Attempted to revoke non-existent or invalid refresh token");
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Uses bulk update operations for efficient revocation of multiple tokens.
     * Typically used during security events like password changes or account compromise.
     * </p>
     *
     * @param userId the ID of the user whose tokens should be revoked, must not be null
     * @return the number of tokens that were revoked
     * @throws IllegalArgumentException if userId is null
     */
    @Override
    @Transactional
    public int revokeAllUserTokens(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        logger.debug("Revoking all refresh tokens for user ID: {}", userId);
        
        int revokedCount = refreshTokenRepository.revokeAllByUserId(userId);
        
        logger.info("Revoked {} refresh tokens for user ID: {}", revokedCount, userId);
        return revokedCount;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Performs bulk deletion of expired tokens to maintain database hygiene.
     * Should be called periodically by scheduled jobs.
     * </p>
     *
     * @return the number of expired tokens that were removed
     */
    @Override
    @Transactional
    public int cleanupExpiredTokens() {
        logger.debug("Cleaning up expired refresh tokens");
        
        int deletedCount = refreshTokenRepository.deleteExpiredTokens(Instant.now());
        
        if (deletedCount > 0) {
            logger.info("Deleted {} expired refresh tokens", deletedCount);
        } else {
            logger.debug("No expired refresh tokens found for cleanup");
        }
        
        return deletedCount;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Performs bulk deletion of old revoked tokens to prevent database bloat
     * while maintaining audit trails for recent security events.
     * </p>
     *
     * @param daysOld the number of days old for revoked tokens to be cleaned up, must be positive
     * @return the number of old revoked tokens that were removed
     * @throws IllegalArgumentException if daysOld is not positive
     */
    @Override
    @Transactional
    public int cleanupRevokedTokens(int daysOld) {
        if (daysOld <= 0) {
            throw new IllegalArgumentException("Days old must be positive");
        }
        
        logger.debug("Cleaning up revoked refresh tokens older than {} days", daysOld);
        
        Instant cutoffDate = Instant.now().minusSeconds(daysOld * 24L * 60L * 60L);
        int deletedCount = refreshTokenRepository.deleteRevokedTokensOlderThan(cutoffDate);
        
        if (deletedCount > 0) {
            logger.info("Deleted {} old revoked refresh tokens (older than {} days)", deletedCount, daysOld);
        } else {
            logger.debug("No old revoked refresh tokens found for cleanup");
        }
        
        return deletedCount;
    }

    /**
     * Finds a refresh token by validating the provided raw token against stored hashes.
     * 
     * <p>
     * Uses HMAC-SHA256 which produces deterministic hashes, allowing for direct database lookups.
     * This method first hashes the input token and then looks up the corresponding entity.
     * </p>
     *
     * @param refreshToken the raw refresh token to locate
     * @return Optional containing the matching RefreshToken entity, or empty if not found
     */
    private Optional<RefreshToken> findTokenByRawValue(String refreshToken) {
        String hashedToken = jwtUtils.hashRefreshToken(refreshToken);
        return refreshTokenRepository.findByTokenHash(hashedToken)
                .filter(token -> jwtUtils.validateRefreshToken(refreshToken, token.getTokenHash()));
    }
}