package com.yohan.event_planner.jobs;

import com.yohan.event_planner.service.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job for cleaning up expired and used password reset tokens.
 *
 * <p>
 * This job runs periodically to remove password reset tokens that are no longer
 * valid due to expiration or successful usage. Regular cleanup prevents database
 * bloat and maintains optimal query performance for token validation operations.
 * </p>
 *
 * <h2>Cleanup Operations</h2>
 * <ul>
 *   <li><strong>Expired Tokens</strong>: Removes tokens past their expiration date</li>
 *   <li><strong>Used Tokens</strong>: Removes tokens that have been successfully used</li>
 *   <li><strong>Batch Processing</strong>: Efficient bulk deletion operations</li>
 *   <li><strong>Performance Monitoring</strong>: Logs cleanup statistics</li>
 * </ul>
 *
 * <h2>Security Benefits</h2>
 * <ul>
 *   <li><strong>Attack Surface Reduction</strong>: Fewer tokens available for potential attacks</li>
 *   <li><strong>Data Minimization</strong>: Removes unnecessary sensitive data</li>
 *   <li><strong>Performance Optimization</strong>: Maintains fast token validation queries</li>
 *   <li><strong>Storage Efficiency</strong>: Prevents database bloat</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>
 * The job can be enabled/disabled via the {@code app.password-reset.cleanup.enabled}
 * property in application configuration. It runs by default but can be disabled
 * in environments where manual cleanup is preferred.
 * </p>
 *
 * <h2>Scheduling</h2>
 * <p>
 * The job runs every 30 minutes by default, which provides a good balance between
 * cleanup frequency and system resource usage. The schedule can be adjusted by
 * modifying the {@code @Scheduled} annotation.
 * </p>
 *
 * <h2>Error Handling</h2>
 * <p>
 * Cleanup failures are logged but don't affect application functionality. The job
 * will retry on the next scheduled execution. Critical errors are logged at ERROR
 * level for monitoring system alerts.
 * </p>
 *
 * @see PasswordResetService
 * @see org.springframework.scheduling.annotation.Scheduled
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
@Component
@ConditionalOnProperty(
    name = "app.password-reset.cleanup.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class PasswordResetTokenCleanupJob {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetTokenCleanupJob.class);

    private final PasswordResetService passwordResetService;

    /**
     * Constructs a new cleanup job with the required dependencies.
     *
     * @param passwordResetService the service for password reset operations
     */
    public PasswordResetTokenCleanupJob(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    /**
     * Performs periodic cleanup of expired and used password reset tokens.
     *
     * <p>
     * This method runs automatically based on the configured schedule. It removes
     * tokens that are no longer valid and logs cleanup statistics for monitoring.
     * The operation is designed to be efficient and non-blocking.
     * </p>
     *
     * <h3>Cleanup Process</h3>
     * <ol>
     *   <li>Identify expired tokens (past their expiration date)</li>
     *   <li>Identify used tokens (successfully used for password reset)</li>
     *   <li>Perform bulk deletion operations</li>
     *   <li>Log cleanup statistics</li>
     * </ol>
     *
     * <h3>Performance Considerations</h3>
     * <ul>
     *   <li><strong>Batch Operations</strong>: Uses efficient bulk delete queries</li>
     *   <li><strong>Index Utilization</strong>: Leverages database indexes for performance</li>
     *   <li><strong>Transaction Management</strong>: Ensures data consistency</li>
     *   <li><strong>Minimal Locking</strong>: Designed to minimize database locks</li>
     * </ul>
     *
     * <h3>Monitoring</h3>
     * <p>
     * The job logs cleanup statistics at INFO level for normal operations and
     * ERROR level for failures. Monitor these logs to ensure the cleanup job
     * is running correctly and to track token usage patterns.
     * </p>
     *
     * <h3>Error Recovery</h3>
     * <p>
     * If cleanup fails, the job logs the error and continues normal operation.
     * The next scheduled execution will retry the cleanup. This ensures that
     * temporary issues don't accumulate into larger problems.
     * </p>
     *
     * @see org.springframework.scheduling.annotation.Scheduled
     * @see PasswordResetService#cleanupExpiredTokens()
     */
    @Scheduled(
        fixedRate = 30 * 60 * 1000, // 30 minutes in milliseconds
        initialDelay = 5 * 60 * 1000 // 5 minute initial delay
    )
    public void cleanupExpiredTokens() {
        try {
            logger.debug("Starting password reset token cleanup job");
            
            long startTime = System.currentTimeMillis();
            int tokensDeleted = passwordResetService.cleanupExpiredTokens();
            long duration = System.currentTimeMillis() - startTime;
            
            if (tokensDeleted > 0) {
                logger.info("Password reset token cleanup completed: {} tokens deleted in {}ms", 
                           tokensDeleted, duration);
            } else {
                logger.debug("Password reset token cleanup completed: no tokens to delete ({}ms)", duration);
            }
            
        } catch (Exception e) {
            logger.error("Failed to cleanup password reset tokens", e);
            // Don't rethrow - let the scheduler continue with next execution
        }
    }

    /**
     * Performs immediate cleanup of expired and used password reset tokens.
     *
     * <p>
     * This method can be called manually to trigger cleanup outside of the
     * regular schedule. It's useful for administrative operations or when
     * implementing custom cleanup triggers.
     * </p>
     *
     * <h3>Use Cases</h3>
     * <ul>
     *   <li><strong>Manual Cleanup</strong>: Administrative cleanup operations</li>
     *   <li><strong>Custom Triggers</strong>: Event-driven cleanup (e.g., high token count)</li>
     *   <li><strong>Testing</strong>: Cleanup during development and testing</li>
     *   <li><strong>Maintenance</strong>: One-time cleanup during system maintenance</li>
     * </ul>
     *
     * @return the number of tokens that were deleted
     */
    public int performImmediateCleanup() {
        logger.info("Performing immediate password reset token cleanup");
        
        try {
            long startTime = System.currentTimeMillis();
            int tokensDeleted = passwordResetService.cleanupExpiredTokens();
            long duration = System.currentTimeMillis() - startTime;
            
            logger.info("Immediate password reset token cleanup completed: {} tokens deleted in {}ms", 
                       tokensDeleted, duration);
            
            return tokensDeleted;
            
        } catch (Exception e) {
            logger.error("Failed to perform immediate password reset token cleanup", e);
            throw new RuntimeException("Cleanup operation failed", e);
        }
    }

    /**
     * Provides statistics about the cleanup job configuration and performance.
     *
     * <p>
     * This method returns information about the cleanup job that can be used
     * for monitoring, debugging, or administrative purposes. It includes
     * configuration details and runtime statistics.
     * </p>
     *
     * @return a string containing cleanup job statistics
     */
    public String getCleanupStatistics() {
        return String.format(
            "PasswordResetTokenCleanupJob - Schedule: every 30 minutes, " +
            "Initial delay: 5 minutes, Status: %s",
            isJobEnabled() ? "ENABLED" : "DISABLED"
        );
    }

    /**
     * Checks if the cleanup job is currently enabled.
     *
     * @return true if the cleanup job is enabled, false otherwise
     */
    private boolean isJobEnabled() {
        // Job is enabled if this component is created (due to @ConditionalOnProperty)
        return true;
    }
}