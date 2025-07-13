package com.yohan.event_planner.jobs;

import com.yohan.event_planner.constants.ApplicationConstants;
import com.yohan.event_planner.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job for cleaning up expired and old revoked refresh tokens.
 *
 * <p>
 * This job runs periodically to maintain database hygiene and security by removing
 * tokens that are no longer needed. It operates on two categories of tokens with
 * different cleanup strategies to balance security and performance requirements.
 * </p>
 *
 * <h2>Cleanup Operations</h2>
 * <ul>
 *   <li><strong>Expired Tokens</strong>: Removes tokens past their expiration date (hourly)</li>
 *   <li><strong>Old Revoked Tokens</strong>: Removes revoked tokens older than 30 days (daily)</li>
 *   <li><strong>Batch Processing</strong>: Efficient bulk deletion operations</li>
 *   <li><strong>Performance Monitoring</strong>: Logs cleanup statistics</li>
 * </ul>
 *
 * <h2>Security Benefits</h2>
 * <ul>
 *   <li><strong>Attack Surface Reduction</strong>: Fewer tokens available for potential attacks</li>
 *   <li><strong>Data Minimization</strong>: Removes unnecessary authentication data</li>
 *   <li><strong>Audit Trail Balance</strong>: Maintains recent revocation history while cleaning old data</li>
 *   <li><strong>Performance Optimization</strong>: Maintains fast token validation queries</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>
 * The job can be enabled/disabled via the {@code app.refresh-token-cleanup.enabled}
 * property in application configuration. It runs by default but can be disabled
 * in environments where manual cleanup is preferred.
 * </p>
 *
 * <h2>Scheduling Strategy</h2>
 * <ul>
 *   <li><strong>Expired Tokens</strong>: Cleaned up hourly for prompt removal</li>
 *   <li><strong>Revoked Tokens</strong>: Cleaned up daily at 2:00 AM to maintain audit trails</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <p>
 * Cleanup failures are logged but don't affect application functionality. Each cleanup
 * operation is independent and failures in one don't affect the other. The jobs will
 * retry on their next scheduled execution.
 * </p>
 *
 * @see RefreshTokenService#cleanupExpiredTokens()
 * @see RefreshTokenService#cleanupRevokedTokens(int)
 * @see com.yohan.event_planner.domain.RefreshToken
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
@Component
@ConditionalOnProperty(
    name = "app.refresh-token-cleanup.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class RefreshTokenCleanupJob {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);
    private static final int REVOKED_TOKEN_RETENTION_DAYS = ApplicationConstants.REFRESH_TOKEN_REVOKED_RETENTION_DAYS;

    private final RefreshTokenService refreshTokenService;

    /**
     * Constructs a new refresh token cleanup job with the required dependencies.
     *
     * @param refreshTokenService the service for refresh token operations
     */
    public RefreshTokenCleanupJob(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Performs periodic cleanup of expired refresh tokens.
     *
     * <p>
     * This method runs automatically every hour to remove tokens that have passed
     * their expiration date. Expired tokens pose a security risk and consume
     * unnecessary database storage.
     * </p>
     *
     * <h3>Cleanup Process</h3>
     * <ol>
     *   <li>Delegate to service layer for business logic</li>
     *   <li>Log cleanup statistics for monitoring</li>
     *   <li>Handle any cleanup failures gracefully</li>
     * </ol>
     *
     * @see RefreshTokenService#cleanupExpiredTokens()
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredTokens() {
        logger.debug("Starting cleanup of expired refresh tokens");
        
        try {
            long startTime = System.currentTimeMillis();
            int deletedCount = refreshTokenService.cleanupExpiredTokens();
            long duration = System.currentTimeMillis() - startTime;
            
            if (deletedCount > 0) {
                logger.info("Cleaned up {} expired refresh tokens in {}ms", deletedCount, duration);
            } else {
                logger.debug("No expired refresh tokens to cleanup ({}ms)", duration);
            }
        } catch (Exception e) {
            logger.error("Error during expired token cleanup", e);
        }
    }

    /**
     * Performs periodic cleanup of old revoked refresh tokens.
     *
     * <p>
     * This method runs daily at 2:00 AM to remove revoked tokens that are older
     * than the configured retention period (30 days). This balances security
     * (removing old tokens) with audit requirements (keeping recent revocations).
     * </p>
     *
     * <h3>Retention Strategy</h3>
     * <p>
     * Revoked tokens are kept for 30 days to maintain audit trails for security
     * investigations, then removed to prevent database bloat.
     * </p>
     *
     * @see RefreshTokenService#cleanupRevokedTokens(int)
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupRevokedTokens() {
        logger.debug("Starting cleanup of old revoked refresh tokens (older than {} days)", 
                REVOKED_TOKEN_RETENTION_DAYS);
        
        try {
            long startTime = System.currentTimeMillis();
            int deletedCount = refreshTokenService.cleanupRevokedTokens(REVOKED_TOKEN_RETENTION_DAYS);
            long duration = System.currentTimeMillis() - startTime;
            
            if (deletedCount > 0) {
                logger.info("Cleaned up {} old revoked refresh tokens in {}ms", deletedCount, duration);
            } else {
                logger.debug("No old revoked refresh tokens to cleanup ({}ms)", duration);
            }
        } catch (Exception e) {
            logger.error("Error during revoked token cleanup", e);
        }
    }

    /**
     * Provides statistics about the cleanup job configuration and performance.
     *
     * <p>
     * This method returns information about the cleanup job that can be used
     * for monitoring, debugging, or administrative purposes. It includes
     * configuration details about the retention period and scheduling.
     * </p>
     *
     * @return a string containing cleanup job statistics
     */
    public String getCleanupStatistics() {
        return String.format(
            "RefreshTokenCleanupJob - Expired tokens: hourly cleanup, " +
            "Revoked tokens: daily cleanup at 2:00 AM, Retention: %d days, Status: %s",
            REVOKED_TOKEN_RETENTION_DAYS,
            isJobEnabled() ? "ENABLED" : "DISABLED"
        );
    }

    /**
     * Checks if the cleanup job is currently enabled.
     * 
     * <p>
     * This method always returns true since the component is only created
     * when the {@code app.refresh-token-cleanup.enabled} property is true
     * or missing (default behavior).
     * </p>
     *
     * @return true if the cleanup job is enabled, false otherwise
     */
    private boolean isJobEnabled() {
        // Job is enabled if this component is created (due to @ConditionalOnProperty)
        return true;
    }
}