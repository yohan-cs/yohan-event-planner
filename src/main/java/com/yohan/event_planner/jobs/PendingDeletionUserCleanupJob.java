package com.yohan.event_planner.jobs;

import com.yohan.event_planner.constants.ApplicationConstants;
import com.yohan.event_planner.repository.UserRepository;
import com.yohan.event_planner.domain.User;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Scheduled job for cleaning up users that have exceeded their pending deletion grace period.
 *
 * <p>
 * This job runs periodically to permanently delete user accounts that were marked for deletion
 * and have passed the configured grace period. It ensures that users who requested account
 * deletion are properly removed from the system after the required waiting period.
 * </p>
 *
 * <h2>Cleanup Operations</h2>
 * <ul>
 *   <li><strong>Grace Period Enforcement</strong>: Deletes users after the configured waiting period</li>
 *   <li><strong>Permanent Deletion</strong>: Completely removes user records and associated data</li>
 *   <li><strong>Data Consistency</strong>: Ensures transactional deletion of all user-related data</li>
 *   <li><strong>Performance Monitoring</strong>: Logs deletion statistics for monitoring</li>
 * </ul>
 *
 * <h2>Security and Privacy Benefits</h2>
 * <ul>
 *   <li><strong>Data Minimization</strong>: Removes personal data as requested by users</li>
 *   <li><strong>Compliance Support</strong>: Helps meet data protection regulations</li>
 *   <li><strong>Storage Efficiency</strong>: Prevents accumulation of deleted account data</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>
 * The job can be enabled/disabled via the {@code app.user-cleanup.pending-deletion.enabled}
 * property in application configuration. It runs by default but can be disabled
 * in environments where manual cleanup is preferred.
 * </p>
 *
 * <h2>Scheduling</h2>
 * <p>
 * The job runs daily at 3:00 AM UTC, providing timely cleanup without impacting
 * normal business hours. The schedule ensures that deletion requests are processed
 * promptly after the grace period expires.
 * </p>
 *
 * <h2>Transaction Management</h2>
 * <p>
 * All deletion operations are performed within a transaction to ensure data consistency.
 * If the deletion process fails, the transaction is rolled back and the job will retry
 * on the next scheduled execution.
 * </p>
 *
 * <h2>Error Handling</h2>
 * <p>
 * Cleanup failures are logged but cause transaction rollback for data consistency. The job
 * will retry on the next scheduled execution. Critical errors are logged at ERROR
 * level for monitoring system alerts.
 * </p>
 *
 * @see User#markForDeletion(ZonedDateTime)
 * @see UserRepository#findAllByIsPendingDeletionTrueAndScheduledDeletionDateBefore(ZonedDateTime)
 * @see ApplicationConstants#USER_DELETION_GRACE_PERIOD_DAYS
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
@Component
@ConditionalOnProperty(
    name = "app.user-cleanup.pending-deletion.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class PendingDeletionUserCleanupJob {

    private static final Logger logger = LoggerFactory.getLogger(PendingDeletionUserCleanupJob.class);
    private final UserRepository userRepository;

    /**
     * Constructs a new pending deletion cleanup job with the required dependencies.
     *
     * @param userRepository the repository for user data access operations
     */
    public PendingDeletionUserCleanupJob(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Performs periodic cleanup of users whose deletion grace period has expired.
     *
     * <p>
     * This method runs automatically based on the configured cron schedule. It identifies
     * users marked for deletion whose grace period has passed and permanently removes
     * them from the system. The operation is transactional to ensure data consistency.
     * </p>
     *
     * <h3>Cleanup Process</h3>
     * <ol>
     *   <li>Query for users with deletion scheduled before current time</li>
     *   <li>Perform batch deletion of eligible users</li>
     *   <li>Log deletion statistics for monitoring</li>
     * </ol>
     *
     * <h3>Transaction Behavior</h3>
     * <p>
     * The entire cleanup operation runs within a single transaction. If any part
     * of the deletion process fails, all changes are rolled back and the job will
     * retry on the next scheduled execution.
     * </p>
     *
     * @see org.springframework.scheduling.annotation.Scheduled
     * @see User#markForDeletion(ZonedDateTime)
     * @see ApplicationConstants#USER_DELETION_GRACE_PERIOD_DAYS
     */
    @Scheduled(cron = "0 0 3 * * *") // runs every day at 3am UTC
    @Transactional
    public void deleteExpiredUsers() {
        logger.debug("Starting pending deletion user cleanup job");
        
        try {
            long startTime = System.currentTimeMillis();
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            List<User> toDelete = userRepository.findAllByIsPendingDeletionTrueAndScheduledDeletionDateBefore(now);
            
            if (!toDelete.isEmpty()) {
                logger.info("Deleting {} expired users with deletion dates before {}", toDelete.size(), now);
                userRepository.deleteAll(toDelete);
                
                long duration = System.currentTimeMillis() - startTime;
                logger.info("Pending deletion user cleanup completed: {} users deleted in {}ms", 
                           toDelete.size(), duration);
            } else {
                long duration = System.currentTimeMillis() - startTime;
                logger.debug("Pending deletion user cleanup completed: no users to delete ({}ms)", duration);
            }
            
        } catch (Exception e) {
            logger.error("Failed to cleanup pending deletion users", e);
            // Re-throw to trigger transaction rollback
            throw e;
        }
    }

    /**
     * Provides statistics about the cleanup job configuration and performance.
     *
     * <p>
     * This method returns information about the cleanup job that can be used
     * for monitoring, debugging, or administrative purposes. It includes
     * configuration details about the grace period and scheduling.
     * </p>
     *
     * @return a string containing cleanup job statistics
     */
    public String getCleanupStatistics() {
        return String.format(
            "PendingDeletionUserCleanupJob - Schedule: daily at 3:00 AM UTC, " +
            "Grace period: %d days, Status: %s",
            ApplicationConstants.USER_DELETION_GRACE_PERIOD_DAYS,
            isJobEnabled() ? "ENABLED" : "DISABLED"
        );
    }

    /**
     * Checks if the cleanup job is currently enabled.
     * 
     * <p>
     * This method always returns true since the component is only created
     * when the {@code app.user-cleanup.pending-deletion.enabled} property is true
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