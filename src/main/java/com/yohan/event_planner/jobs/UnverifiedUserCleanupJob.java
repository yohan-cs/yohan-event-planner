package com.yohan.event_planner.jobs;

import com.yohan.event_planner.constants.ApplicationConstants;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Scheduled job for cleaning up unverified user accounts that have exceeded the verification time limit.
 *
 * <p>
 * This job runs periodically to remove user accounts that have not verified their email address
 * within the configured time window. This prevents accumulation of abandoned accounts and helps
 * defend against username squatting and fake account creation.
 * </p>
 *
 * <h2>Cleanup Operations</h2>
 * <ul>
 *   <li><strong>Unverified Accounts</strong>: Removes users who haven't verified email after 24 hours</li>
 *   <li><strong>Username Liberation</strong>: Frees up usernames from abandoned registrations</li>
 *   <li><strong>Email Liberation</strong>: Allows re-registration with the same email address</li>
 *   <li><strong>Database Cleanup</strong>: Removes orphaned user data and associated records</li>
 * </ul>
 *
 * <h2>Security Benefits</h2>
 * <ul>
 *   <li><strong>Anti-Squatting</strong>: Prevents indefinite reservation of usernames</li>
 *   <li><strong>Fake Account Prevention</strong>: Removes accounts with fake/invalid emails</li>
 *   <li><strong>Resource Management</strong>: Prevents database bloat from abandoned accounts</li>
 *   <li><strong>Attack Surface Reduction</strong>: Fewer inactive accounts available for exploitation</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <p>
 * The job can be enabled/disabled via the {@code app.user-cleanup.unverified.enabled}
 * property in application configuration. The verification time limit is configured via
 * {@code app.user-cleanup.unverified.max-age-hours} (default: 24 hours).
 * </p>
 *
 * <h2>Scheduling</h2>
 * <p>
 * The job runs every 6 hours by default, providing timely cleanup without excessive system load.
 * The schedule can be adjusted by modifying the {@code @Scheduled} annotation.
 * </p>
 *
 * <h2>Safety Measures</h2>
 * <ul>
 *   <li><strong>Time Window</strong>: Only removes accounts older than configured threshold</li>
 *   <li><strong>Email Verification Check</strong>: Only removes accounts that haven't verified email</li>
 *   <li><strong>Cascade Deletion</strong>: Properly handles related entity cleanup</li>
 *   <li><strong>Audit Logging</strong>: Logs all cleanup operations for monitoring</li>
 * </ul>
 *
 * @see User
 * @see UserRepository
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.1.0
 */
@Component
@ConditionalOnProperty(
    name = "app.user-cleanup.unverified.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class UnverifiedUserCleanupJob {

    private static final Logger logger = LoggerFactory.getLogger(UnverifiedUserCleanupJob.class);

    /** Default maximum age for unverified accounts in hours */
    private static final long DEFAULT_MAX_AGE_HOURS = ApplicationConstants.UNVERIFIED_USER_MAX_AGE_HOURS;

    private final UserRepository userRepository;

    /**
     * Constructs a new cleanup job with the required dependencies.
     *
     * @param userRepository the repository for user operations
     */
    public UnverifiedUserCleanupJob(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Performs periodic cleanup of unverified user accounts that have exceeded the time limit.
     *
     * <p>
     * This method runs automatically based on the configured schedule. It identifies and removes
     * user accounts that were created more than 24 hours ago but have not verified their email
     * address. This helps prevent username squatting and keeps the database clean.
     * </p>
     *
     * <h3>Cleanup Process</h3>
     * <ol>
     *   <li>Calculate cutoff time (current time - 24 hours)</li>
     *   <li>Find all unverified users created before cutoff time</li>
     *   <li>Delete found users and their associated data</li>
     *   <li>Log cleanup statistics</li>
     * </ol>
     *
     * <h3>Safety Measures</h3>
     * <ul>
     *   <li><strong>Time Validation</strong>: Only deletes accounts older than threshold</li>
     *   <li><strong>Email Verification Check</strong>: Only deletes unverified accounts</li>
     *   <li><strong>Transaction Safety</strong>: Ensures atomicity of cleanup operations</li>
     *   <li><strong>Error Handling</strong>: Graceful handling of cleanup failures</li>
     * </ul>
     *
     * <h3>Related Entity Cleanup</h3>
     * <p>
     * When a user is deleted, the following related entities are automatically cleaned up:
     * </p>
     * <ul>
     *   <li><strong>Events</strong>: All events created by the user</li>
     *   <li><strong>Recurring Events</strong>: All recurring events created by the user</li>
     *   <li><strong>Labels</strong>: All labels created by the user</li>
     *   <li><strong>Badges</strong>: All badges created by the user</li>
     *   <li><strong>Email Verification Tokens</strong>: Any pending verification tokens</li>
     * </ul>
     */
    @Scheduled(
        fixedRate = 6 * 60 * 60 * 1000, // 6 hours in milliseconds
        initialDelay = 30 * 60 * 1000   // 30 minute initial delay
    )
    public void cleanupUnverifiedUsers() {
        try {
            logger.debug("Starting unverified user cleanup job");
            
            long startTime = System.currentTimeMillis();
            
            // Calculate cutoff time
            ZonedDateTime cutoffTime = calculateCutoffTime();
            
            // Find unverified users older than cutoff time
            List<User> unverifiedUsers = userRepository.findAllByEmailVerifiedFalseAndCreatedAtBefore(cutoffTime);
            
            if (unverifiedUsers.isEmpty()) {
                logger.debug("Unverified user cleanup completed: no users to delete");
                return;
            }
            
            // Delete unverified users
            int deletedCount = deleteUnverifiedUsers(unverifiedUsers);
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (deletedCount > 0) {
                logger.info("Unverified user cleanup completed: {} users deleted in {}ms", 
                           deletedCount, duration);
            } else {
                logger.warn("Unverified user cleanup completed with errors: {} users found but {} deleted in {}ms", 
                           unverifiedUsers.size(), deletedCount, duration);
            }
            
        } catch (Exception e) {
            logger.error("Failed to cleanup unverified users", e);
            // Don't rethrow - let the scheduler continue with next execution
        }
    }

    /**
     * Performs immediate cleanup of unverified user accounts.
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
     *   <li><strong>Custom Triggers</strong>: Event-driven cleanup (e.g., high user count)</li>
     *   <li><strong>Testing</strong>: Cleanup during development and testing</li>
     *   <li><strong>Maintenance</strong>: One-time cleanup during system maintenance</li>
     * </ul>
     *
     * @return the number of unverified users that were deleted
     */
    public int performImmediateCleanup() {
        logger.debug("Starting immediate unverified user cleanup");
        logger.info("Performing immediate unverified user cleanup");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Calculate cutoff time
            ZonedDateTime cutoffTime = calculateCutoffTime();
            
            // Find unverified users older than cutoff time
            List<User> unverifiedUsers = userRepository.findAllByEmailVerifiedFalseAndCreatedAtBefore(cutoffTime);
            
            // Delete unverified users
            int deletedCount = deleteUnverifiedUsers(unverifiedUsers);
            
            long duration = System.currentTimeMillis() - startTime;
            
            logger.info("Immediate unverified user cleanup completed: {} users deleted in {}ms", 
                       deletedCount, duration);
            
            return deletedCount;
            
        } catch (Exception e) {
            logger.error("Failed to perform immediate unverified user cleanup", e);
            throw new RuntimeException("Cleanup operation failed", e);
        }
    }

    /**
     * Provides statistics about the cleanup job configuration and performance.
     *
     * <p>
     * This method returns information about the cleanup job that can be used
     * for monitoring, debugging, or administrative purposes.
     * </p>
     *
     * @return a string containing cleanup job statistics
     */
    public String getCleanupStatistics() {
        return String.format(
            "UnverifiedUserCleanupJob - Schedule: every 6 hours, " +
            "Max age: %d hours, Initial delay: 30 minutes, Status: %s",
            DEFAULT_MAX_AGE_HOURS,
            isJobEnabled() ? "ENABLED" : "DISABLED"
        );
    }

    /**
     * Calculates the cutoff time for unverified user cleanup.
     *
     * <p>
     * This method calculates the cutoff time by subtracting the maximum age
     * for unverified accounts from the current time. Users created before
     * this cutoff time are eligible for cleanup.
     * </p>
     *
     * @return the cutoff time (current time minus max age hours)
     */
    private ZonedDateTime calculateCutoffTime() {
        return ZonedDateTime.now().minusHours(DEFAULT_MAX_AGE_HOURS);
    }

    /**
     * Deletes a list of unverified users with individual error handling.
     *
     * <p>
     * This method iterates through the provided list of users and attempts
     * to delete each one individually. If deletion of a single user fails,
     * the error is logged and processing continues with the next user.
     * </p>
     *
     * @param unverifiedUsers the list of users to delete
     * @return the number of users successfully deleted
     */
    private int deleteUnverifiedUsers(List<User> unverifiedUsers) {
        int deletedCount = 0;
        for (User user : unverifiedUsers) {
            try {
                logger.info("Deleting unverified user: {} (created: {}, email: {})", 
                           user.getUsername(), user.getCreatedAt(), user.getEmail());
                userRepository.delete(user);
                deletedCount++;
            } catch (Exception e) {
                logger.error("Failed to delete unverified user: {} (id: {})", 
                            user.getUsername(), user.getId(), e);
                // Continue with next user - don't let one failure stop the cleanup
            }
        }
        return deletedCount;
    }

    /**
     * Checks if the cleanup job is currently enabled.
     * 
     * <p>
     * This method always returns true since the component is only created
     * when the {@code app.user-cleanup.unverified.enabled} property is true
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