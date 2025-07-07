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
import java.util.Optional;

import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_ACCESS;

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

    @Override
    @Transactional
    public String createRefreshToken(Long userId) {
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

    @Override
    @Transactional
    public RefreshTokenResponseDTO refreshTokens(String refreshToken) {
        logger.debug("Refreshing tokens with provided refresh token");
        
        // Hash the provided token to find it in database
        String hashedToken = jwtUtils.hashRefreshToken(refreshToken);
        
        // Find all tokens and check each one (since we can't search by hash efficiently)
        Optional<RefreshToken> tokenEntity = refreshTokenRepository.findAll()
                .stream()
                .filter(token -> jwtUtils.validateRefreshToken(refreshToken, token.getTokenHash()))
                .findFirst();
        
        if (tokenEntity.isEmpty()) {
            logger.error("Refresh token not found or invalid");
            throw new UnauthorizedException(UNAUTHORIZED_ACCESS);
        }
        
        RefreshToken token = tokenEntity.get();
        
        // Validate token
        if (!token.isValid()) {
            logger.error("Refresh token is expired or revoked for user ID: {}", token.getUserId());
            throw new UnauthorizedException(UNAUTHORIZED_ACCESS);
        }
        
        // Immediately revoke the used token (one-time use)
        token.setRevoked(true);
        refreshTokenRepository.save(token);
        
        // Get user details for new token generation
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new UserNotFoundException(token.getUserId()));
        
        // Generate new tokens
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String newAccessToken = jwtUtils.generateToken(userDetails);
        String newRefreshToken = createRefreshToken(user.getId());
        
        logger.debug("Successfully refreshed tokens for user ID: {}", user.getId());
        
        return new RefreshTokenResponseDTO(newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String refreshToken) {
        logger.debug("Revoking refresh token");
        
        // Find and revoke the token
        Optional<RefreshToken> tokenEntity = refreshTokenRepository.findAll()
                .stream()
                .filter(token -> jwtUtils.validateRefreshToken(refreshToken, token.getTokenHash()))
                .findFirst();
        
        if (tokenEntity.isPresent()) {
            RefreshToken token = tokenEntity.get();
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            logger.debug("Successfully revoked refresh token for user ID: {}", token.getUserId());
        } else {
            logger.warn("Attempted to revoke non-existent refresh token");
        }
    }

    @Override
    @Transactional
    public int revokeAllUserTokens(Long userId) {
        logger.debug("Revoking all refresh tokens for user ID: {}", userId);
        
        int revokedCount = refreshTokenRepository.revokeAllByUserId(userId);
        
        logger.debug("Revoked {} refresh tokens for user ID: {}", revokedCount, userId);
        return revokedCount;
    }

    @Override
    @Transactional
    public int cleanupExpiredTokens() {
        logger.debug("Cleaning up expired refresh tokens");
        
        int deletedCount = refreshTokenRepository.deleteExpiredTokens(Instant.now());
        
        logger.debug("Deleted {} expired refresh tokens", deletedCount);
        return deletedCount;
    }

    @Override
    @Transactional
    public int cleanupRevokedTokens(int daysOld) {
        logger.debug("Cleaning up revoked refresh tokens older than {} days", daysOld);
        
        Instant cutoffDate = Instant.now().minusSeconds(daysOld * 24 * 60 * 60);
        int deletedCount = refreshTokenRepository.deleteRevokedTokensOlderThan(cutoffDate);
        
        logger.debug("Deleted {} old revoked refresh tokens", deletedCount);
        return deletedCount;
    }
}