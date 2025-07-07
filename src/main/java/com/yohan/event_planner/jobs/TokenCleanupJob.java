package com.yohan.event_planner.jobs;

import com.yohan.event_planner.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job for cleaning up expired and old revoked refresh tokens.
 *
 * <p>
 * This job runs periodically to maintain database hygiene by removing:
 * <ul>
 *   <li>Expired refresh tokens</li>
 *   <li>Revoked tokens older than 30 days</li>
 * </ul>
 * </p>
 */
@Component
public class TokenCleanupJob {

    private static final Logger logger = LoggerFactory.getLogger(TokenCleanupJob.class);
    private static final int REVOKED_TOKEN_RETENTION_DAYS = 30;

    private final RefreshTokenService refreshTokenService;

    public TokenCleanupJob(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Cleans up expired refresh tokens.
     * Runs every hour at minute 0.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredTokens() {
        logger.debug("Starting cleanup of expired refresh tokens");
        
        try {
            int deletedCount = refreshTokenService.cleanupExpiredTokens();
            logger.info("Cleaned up {} expired refresh tokens", deletedCount);
        } catch (Exception e) {
            logger.error("Error during expired token cleanup", e);
        }
    }

    /**
     * Cleans up old revoked refresh tokens.
     * Runs daily at 2:00 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupRevokedTokens() {
        logger.debug("Starting cleanup of old revoked refresh tokens (older than {} days)", 
                REVOKED_TOKEN_RETENTION_DAYS);
        
        try {
            int deletedCount = refreshTokenService.cleanupRevokedTokens(REVOKED_TOKEN_RETENTION_DAYS);
            logger.info("Cleaned up {} old revoked refresh tokens", deletedCount);
        } catch (Exception e) {
            logger.error("Error during revoked token cleanup", e);
        }
    }
}