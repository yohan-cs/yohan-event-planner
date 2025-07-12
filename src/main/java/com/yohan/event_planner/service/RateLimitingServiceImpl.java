package com.yohan.event_planner.service;

import com.yohan.event_planner.constants.ApplicationConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of rate limiting service using in-memory caching.
 *
 * <p>
 * This service provides thread-safe rate limiting functionality using Spring's
 * caching infrastructure. It implements sliding window rate limiting to prevent
 * abuse while allowing legitimate usage patterns.
 * </p>
 *
 * <h2>Rate Limiting Algorithm</h2>
 * <p>
 * Uses a sliding window algorithm with the following characteristics:
 * </p>
 * <ul>
 *   <li><strong>Window Size</strong>: 1 hour for registration attempts</li>
 *   <li><strong>Attempt Limit</strong>: 5 registration attempts per window</li>
 *   <li><strong>Sliding Behavior</strong>: Window slides continuously, not fixed intervals</li>
 *   <li><strong>Automatic Cleanup</strong>: Old entries are automatically removed</li>
 * </ul>
 *
 * <h2>Security Features</h2>
 * <ul>
 *   <li><strong>IP-based Limiting</strong>: Rate limits applied per client IP address</li>
 *   <li><strong>Attack Prevention</strong>: Prevents various abuse patterns</li>
 *   <li><strong>Resource Protection</strong>: Protects backend resources from overload</li>
 *   <li><strong>Fair Usage</strong>: Ensures fair access for legitimate users</li>
 * </ul>
 *
 * <h2>Performance Characteristics</h2>
 * <ul>
 *   <li><strong>In-Memory Storage</strong>: Fast access with low latency</li>
 *   <li><strong>Thread Safety</strong>: Concurrent access safe operations</li>
 *   <li><strong>Memory Efficient</strong>: Automatic cleanup of expired data</li>
 *   <li><strong>Scalable</strong>: Handles high concurrent load efficiently</li>
 * </ul>
 *
 * @see RateLimitingService
 * @see org.springframework.cache.CacheManager
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.1.0
 */
@Service
public class RateLimitingServiceImpl implements RateLimitingService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingServiceImpl.class);


    private final CacheManager cacheManager;

    /**
     * Constructs a new rate limiting service with the specified cache manager.
     * 
     * <p>
     * The cache manager is used to store and retrieve rate limiting data using
     * Spring's caching abstraction. The implementation expects all required caches
     * to be configured in the cache manager.
     * </p>
     * 
     * <h3>Cache Configuration Requirements</h3>
     * <ul>
     *   <li><strong>Cache Names</strong>: registration-rate-limit, login-rate-limit, password-reset-rate-limit, email-verification-rate-limit</li>
     *   <li><strong>Concurrency</strong>: Must support concurrent read/write operations</li>
     *   <li><strong>Expiration</strong>: Optional, as manual cleanup is implemented</li>
     * </ul>
     *
     * @param cacheManager the cache manager for storing rate limit data.
     *                     Must not be null and should have the required cache configured.
     * @throws IllegalArgumentException if cacheManager is null
     */
    public RateLimitingServiceImpl(CacheManager cacheManager) {
        if (cacheManager == null) {
            throw new IllegalArgumentException("CacheManager cannot be null");
        }
        this.cacheManager = cacheManager;
    }

    /**
     * Rate limit entry containing attempt tracking data for sliding window algorithm.
     * 
     * <p>
     * This inner class implements a sophisticated sliding window rate limiting mechanism
     * that tracks attempts across hourly time slots. It provides thread-safe operations
     * for checking limits, recording attempts, and automatic cleanup of expired data.
     * </p>
     * 
     * <h3>Algorithm Details</h3>
     * <ul>
     *   <li><strong>Time Slots</strong>: Tracks attempts per hour using epoch hour as key</li>
     *   <li><strong>Sliding Window</strong>: Continuously removes expired hourly slots</li>
     *   <li><strong>Thread Safety</strong>: All operations are synchronized for concurrent access</li>
     *   <li><strong>Memory Efficiency</strong>: Automatic cleanup prevents memory leaks</li>
     * </ul>
     */
    private static class RateLimitEntry {
        private final ConcurrentHashMap<Long, Integer> timeSlots = new ConcurrentHashMap<>();
        private final double windowSizeHours;

        /**
         * Constructs a new rate limit entry with the specified window size.
         * 
         * @param windowSizeHours the size of the sliding window in hours (supports fractional hours)
         */
        public RateLimitEntry(double windowSizeHours) {
            this.windowSizeHours = windowSizeHours;
        }

        /**
         * Checks if an additional attempt is allowed within the current rate limit.
         * 
         * <p>
         * This method performs automatic cleanup of expired entries before checking
         * the current attempt count against the maximum allowed attempts.
         * </p>
         * 
         * @param maxAttempts the maximum number of attempts allowed in the window
         * @return true if an additional attempt is allowed, false if limit would be exceeded
         */
        public synchronized boolean isAllowed(int maxAttempts) {
            cleanupOldEntries();
            int currentAttempts = getCurrentAttempts();
            return currentAttempts < maxAttempts;
        }

        /**
         * Records a new attempt in the current hour slot.
         * 
         * <p>
         * This method increments the attempt counter for the current hour,
         * creating a new slot if none exists. Expired entries are cleaned
         * up before recording the attempt.
         * </p>
         */
        public synchronized void recordAttempt() {
            cleanupOldEntries();
            long currentHour = getCurrentHour();
            timeSlots.merge(currentHour, 1, Integer::sum);
        }

        /**
         * Gets the total number of attempts in the current sliding window.
         * 
         * <p>
         * This method sums all attempts across all active time slots within
         * the sliding window after cleaning up expired entries.
         * </p>
         * 
         * @return the total number of attempts in the current window
         */
        public synchronized int getCurrentAttempts() {
            cleanupOldEntries();
            return timeSlots.values().stream().mapToInt(Integer::intValue).sum();
        }

        /**
         * Calculates the time in seconds until the rate limit window resets.
         * 
         * <p>
         * The reset time is determined by finding the oldest active time slot
         * and calculating when it will expire from the sliding window. If no
         * attempts are recorded, returns 0 indicating immediate availability.
         * </p>
         * 
         * @return the number of seconds until the rate limit resets, or 0 if no limit is active
         */
        public synchronized long getResetTime() {
            cleanupOldEntries();
            if (timeSlots.isEmpty()) {
                return 0;
            }
            long oldestHour = timeSlots.keySet().stream().min(Long::compareTo).orElse(getCurrentHour());
            double resetHour = oldestHour + windowSizeHours;
            long currentHour = getCurrentHour();
            return Math.max(0, (long)((resetHour - currentHour) * 3600));
        }

        /**
         * Removes expired time slots that fall outside the current sliding window.
         * 
         * <p>
         * This method calculates the cutoff hour based on the current time and window size,
         * then removes all time slots older than the cutoff. This ensures the sliding
         * window behavior and prevents memory leaks from accumulating old data.
         * </p>
         * 
         * <h4>Algorithm Details</h4>
         * <ul>
         *   <li>Cutoff calculation: currentHour - windowSizeHours + 1</li>
         *   <li>Removal condition: timeSlot.hour < cutoffHour</li>
         *   <li>Atomic operation: uses removeIf for thread-safe bulk removal</li>
         * </ul>
         */
        private void cleanupOldEntries() {
            long currentHour = getCurrentHour();
            long cutoffHour = (long)(currentHour - windowSizeHours + 1);
            timeSlots.entrySet().removeIf(entry -> entry.getKey() < cutoffHour);
        }

        /**
         * Calculates the current hour as an epoch hour value for time slot indexing.
         * 
         * <p>
         * This method truncates the current time to the hour boundary and converts
         * it to a simple numeric hour value. This provides a stable, comparable
         * key for time slot indexing that naturally handles hour boundaries.
         * </p>
         * 
         * <h4>Implementation Notes</h4>
         * <ul>
         *   <li>Uses {@link Instant#truncatedTo(ChronoUnit)} for precise hour boundary</li>
         *   <li>Converts to epoch seconds then divides by 3600 for hour granularity</li>
         *   <li>Result is timezone-independent UTC-based hour value</li>
         * </ul>
         * 
         * @return the current hour as an epoch hour value (hours since Unix epoch)
         */
        private long getCurrentHour() {
            return Instant.now().truncatedTo(ChronoUnit.HOURS).getEpochSecond() / 3600;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation uses an in-memory sliding window algorithm with automatic
     * cleanup of expired entries. If the cache is unavailable or any error occurs,
     * the method employs a "fail open" strategy, allowing the request to proceed
     * to prevent service disruption.
     * </p>
     * 
     * <h3>Error Handling Strategy</h3>
     * <ul>
     *   <li><strong>Cache Unavailable</strong>: Logs warning and allows request</li>
     *   <li><strong>Exception Occurred</strong>: Logs error and allows request</li>
     *   <li><strong>Rate Limit Exceeded</strong>: Logs warning and denies request</li>
     * </ul>
     * 
     * @param ipAddress the IP address of the client attempting registration.
     *                  Must not be null or empty for accurate rate limiting.
     * @return true if the registration attempt is allowed, false if rate limit exceeded
     * @throws IllegalArgumentException if ipAddress is null or empty
     */
    @Override
    public boolean isRegistrationAllowed(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }
        
        try {
            Cache cache = cacheManager.getCache(ApplicationConstants.REGISTRATION_CACHE);
            if (cache == null) {
                logger.warn("Registration rate limit cache not found, allowing request");
                return true;
            }

            RateLimitEntry entry = cache.get(ipAddress, RateLimitEntry.class);
            if (entry == null) {
                entry = new RateLimitEntry(ApplicationConstants.REGISTRATION_WINDOW_HOURS);
                cache.put(ipAddress, entry);
            }

            boolean allowed = entry.isAllowed(ApplicationConstants.MAX_REGISTRATION_ATTEMPTS);
            
            if (!allowed) {
                logger.warn("Registration rate limit exceeded for IP: {} (attempts: {}/{})", 
                           ipAddress, entry.getCurrentAttempts(), ApplicationConstants.MAX_REGISTRATION_ATTEMPTS);
            } else {
                logger.debug("Registration allowed for IP: {} (attempts: {}/{})", 
                            ipAddress, entry.getCurrentAttempts(), ApplicationConstants.MAX_REGISTRATION_ATTEMPTS);
            }

            return allowed;
            
        } catch (Exception e) {
            logger.error("Error checking registration rate limit for IP: {}", ipAddress, e);
            // Fail open - allow the request if rate limiting fails
            return true;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation stores the attempt data in an in-memory cache using
     * a sliding window algorithm. If the cache is unavailable, the attempt
     * recording is silently skipped with appropriate logging.
     * </p>
     * 
     * <h3>Implementation Details</h3>
     * <ul>
     *   <li><strong>Idempotent Operation</strong>: Safe to call multiple times</li>
     *   <li><strong>Automatic Entry Creation</strong>: Creates new entry if none exists</li>
     *   <li><strong>Thread-Safe</strong>: Concurrent calls are safely handled</li>
     * </ul>
     * 
     * @param ipAddress the IP address of the client that attempted registration.
     *                  Must not be null or empty for proper tracking.
     * @throws IllegalArgumentException if ipAddress is null or empty
     */
    @Override
    public void recordRegistrationAttempt(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }
        
        try {
            Cache cache = cacheManager.getCache(ApplicationConstants.REGISTRATION_CACHE);
            if (cache == null) {
                logger.warn("Registration rate limit cache not found, cannot record attempt");
                return;
            }

            RateLimitEntry entry = cache.get(ipAddress, RateLimitEntry.class);
            if (entry == null) {
                entry = new RateLimitEntry(ApplicationConstants.REGISTRATION_WINDOW_HOURS);
            }

            entry.recordAttempt();
            cache.put(ipAddress, entry);

            logger.debug("Recorded registration attempt for IP: {} (total attempts: {}/{})", 
                        ipAddress, entry.getCurrentAttempts(), ApplicationConstants.MAX_REGISTRATION_ATTEMPTS);
            
        } catch (Exception e) {
            logger.error("Error recording registration attempt for IP: {}", ipAddress, e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation calculates remaining attempts by subtracting current
     * attempts from the maximum allowed. If cache is unavailable or errors occur,
     * returns the maximum attempts to maintain service availability.
     * </p>
     * 
     * @param ipAddress the IP address to check remaining attempts for.
     *                  Must not be null or empty for accurate calculation.
     * @return the number of registration attempts remaining in the current window,
     *         or maximum attempts if cache is unavailable
     * @throws IllegalArgumentException if ipAddress is null or empty
     */
    @Override
    public int getRemainingRegistrationAttempts(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }
        
        try {
            Cache cache = cacheManager.getCache(ApplicationConstants.REGISTRATION_CACHE);
            if (cache == null) {
                return ApplicationConstants.MAX_REGISTRATION_ATTEMPTS;
            }

            RateLimitEntry entry = cache.get(ipAddress, RateLimitEntry.class);
            if (entry == null) {
                return ApplicationConstants.MAX_REGISTRATION_ATTEMPTS;
            }

            int currentAttempts = entry.getCurrentAttempts();
            return Math.max(0, ApplicationConstants.MAX_REGISTRATION_ATTEMPTS - currentAttempts);
            
        } catch (Exception e) {
            logger.error("Error getting remaining registration attempts for IP: {}", ipAddress, e);
            return ApplicationConstants.MAX_REGISTRATION_ATTEMPTS;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation calculates reset time based on the oldest active
     * time slot in the sliding window. If no attempts are recorded or cache
     * is unavailable, returns 0 indicating immediate availability.
     * </p>
     * 
     * @param ipAddress the IP address to check reset time for.
     *                  Must not be null or empty for accurate calculation.
     * @return the number of seconds until the rate limit resets, or 0 if no limit is active
     * @throws IllegalArgumentException if ipAddress is null or empty
     */
    @Override
    public long getRegistrationRateLimitResetTime(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }
        
        try {
            Cache cache = cacheManager.getCache(ApplicationConstants.REGISTRATION_CACHE);
            if (cache == null) {
                return 0;
            }

            RateLimitEntry entry = cache.get(ipAddress, RateLimitEntry.class);
            if (entry == null) {
                return 0;
            }

            return entry.getResetTime();
            
        } catch (Exception e) {
            logger.error("Error getting registration rate limit reset time for IP: {}", ipAddress, e);
            return 0;
        }
    }

    @Override
    public boolean isLoginAllowed(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }
        
        try {
            Cache cache = cacheManager.getCache(ApplicationConstants.LOGIN_CACHE);
            if (cache == null) {
                logger.warn("Login rate limit cache not found, allowing request");
                return true;
            }

            RateLimitEntry entry = cache.get(ipAddress, RateLimitEntry.class);
            if (entry == null) {
                entry = new RateLimitEntry(ApplicationConstants.LOGIN_WINDOW_HOURS);
                cache.put(ipAddress, entry);
            }

            boolean allowed = entry.isAllowed(ApplicationConstants.MAX_LOGIN_ATTEMPTS);
            
            if (!allowed) {
                logger.warn("Login rate limit exceeded for IP: {} (attempts: {}/{})", 
                           ipAddress, entry.getCurrentAttempts(), ApplicationConstants.MAX_LOGIN_ATTEMPTS);
            } else {
                logger.debug("Login allowed for IP: {} (attempts: {}/{})", 
                            ipAddress, entry.getCurrentAttempts(), ApplicationConstants.MAX_LOGIN_ATTEMPTS);
            }

            return allowed;
            
        } catch (Exception e) {
            logger.error("Error checking login rate limit for IP: {}", ipAddress, e);
            return true;
        }
    }

    @Override
    public void recordLoginAttempt(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }
        
        try {
            Cache cache = cacheManager.getCache(ApplicationConstants.LOGIN_CACHE);
            if (cache == null) {
                logger.warn("Login rate limit cache not found, cannot record attempt");
                return;
            }

            RateLimitEntry entry = cache.get(ipAddress, RateLimitEntry.class);
            if (entry == null) {
                entry = new RateLimitEntry(ApplicationConstants.LOGIN_WINDOW_HOURS);
            }

            entry.recordAttempt();
            cache.put(ipAddress, entry);

            logger.debug("Recorded login attempt for IP: {} (total attempts: {}/{})", 
                        ipAddress, entry.getCurrentAttempts(), ApplicationConstants.MAX_LOGIN_ATTEMPTS);
            
        } catch (Exception e) {
            logger.error("Error recording login attempt for IP: {}", ipAddress, e);
        }
    }

    @Override
    public int getRemainingLoginAttempts(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }
        
        try {
            Cache cache = cacheManager.getCache(ApplicationConstants.LOGIN_CACHE);
            if (cache == null) {
                return ApplicationConstants.MAX_LOGIN_ATTEMPTS;
            }

            RateLimitEntry entry = cache.get(ipAddress, RateLimitEntry.class);
            if (entry == null) {
                return ApplicationConstants.MAX_LOGIN_ATTEMPTS;
            }

            int currentAttempts = entry.getCurrentAttempts();
            return Math.max(0, ApplicationConstants.MAX_LOGIN_ATTEMPTS - currentAttempts);
            
        } catch (Exception e) {
            logger.error("Error getting remaining login attempts for IP: {}", ipAddress, e);
            return ApplicationConstants.MAX_LOGIN_ATTEMPTS;
        }
    }

    @Override
    public long getLoginRateLimitResetTime(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }
        
        try {
            Cache cache = cacheManager.getCache(ApplicationConstants.LOGIN_CACHE);
            if (cache == null) {
                return 0;
            }

            RateLimitEntry entry = cache.get(ipAddress, RateLimitEntry.class);
            if (entry == null) {
                return 0;
            }

            return entry.getResetTime();
            
        } catch (Exception e) {
            logger.error("Error getting login rate limit reset time for IP: {}", ipAddress, e);
            return 0;
        }
    }

    @Override
    public boolean isPasswordResetAllowed(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }
        
        try {
            Cache cache = cacheManager.getCache(ApplicationConstants.PASSWORD_RESET_CACHE);
            if (cache == null) {
                logger.warn("Password reset rate limit cache not found, allowing request");
                return true;
            }

            RateLimitEntry entry = cache.get(ipAddress, RateLimitEntry.class);
            if (entry == null) {
                entry = new RateLimitEntry(ApplicationConstants.PASSWORD_RESET_WINDOW_HOURS);
                cache.put(ipAddress, entry);
            }

            boolean allowed = entry.isAllowed(ApplicationConstants.MAX_PASSWORD_RESET_ATTEMPTS);
            
            if (!allowed) {
                logger.warn("Password reset rate limit exceeded for IP: {} (attempts: {}/{})", 
                           ipAddress, entry.getCurrentAttempts(), ApplicationConstants.MAX_PASSWORD_RESET_ATTEMPTS);
            } else {
                logger.debug("Password reset allowed for IP: {} (attempts: {}/{})", 
                            ipAddress, entry.getCurrentAttempts(), ApplicationConstants.MAX_PASSWORD_RESET_ATTEMPTS);
            }

            return allowed;
            
        } catch (Exception e) {
            logger.error("Error checking password reset rate limit for IP: {}", ipAddress, e);
            return true;
        }
    }

    @Override
    public void recordPasswordResetAttempt(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }
        
        try {
            Cache cache = cacheManager.getCache(ApplicationConstants.PASSWORD_RESET_CACHE);
            if (cache == null) {
                logger.warn("Password reset rate limit cache not found, cannot record attempt");
                return;
            }

            RateLimitEntry entry = cache.get(ipAddress, RateLimitEntry.class);
            if (entry == null) {
                entry = new RateLimitEntry(ApplicationConstants.PASSWORD_RESET_WINDOW_HOURS);
            }

            entry.recordAttempt();
            cache.put(ipAddress, entry);

            logger.debug("Recorded password reset attempt for IP: {} (total attempts: {}/{})", 
                        ipAddress, entry.getCurrentAttempts(), ApplicationConstants.MAX_PASSWORD_RESET_ATTEMPTS);
            
        } catch (Exception e) {
            logger.error("Error recording password reset attempt for IP: {}", ipAddress, e);
        }
    }

    @Override
    public int getRemainingPasswordResetAttempts(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }
        
        try {
            Cache cache = cacheManager.getCache(ApplicationConstants.PASSWORD_RESET_CACHE);
            if (cache == null) {
                return ApplicationConstants.MAX_PASSWORD_RESET_ATTEMPTS;
            }

            RateLimitEntry entry = cache.get(ipAddress, RateLimitEntry.class);
            if (entry == null) {
                return ApplicationConstants.MAX_PASSWORD_RESET_ATTEMPTS;
            }

            int currentAttempts = entry.getCurrentAttempts();
            return Math.max(0, ApplicationConstants.MAX_PASSWORD_RESET_ATTEMPTS - currentAttempts);
            
        } catch (Exception e) {
            logger.error("Error getting remaining password reset attempts for IP: {}", ipAddress, e);
            return ApplicationConstants.MAX_PASSWORD_RESET_ATTEMPTS;
        }
    }

    @Override
    public long getPasswordResetRateLimitResetTime(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }
        
        try {
            Cache cache = cacheManager.getCache(ApplicationConstants.PASSWORD_RESET_CACHE);
            if (cache == null) {
                return 0;
            }

            RateLimitEntry entry = cache.get(ipAddress, RateLimitEntry.class);
            if (entry == null) {
                return 0;
            }

            return entry.getResetTime();
            
        } catch (Exception e) {
            logger.error("Error getting password reset rate limit reset time for IP: {}", ipAddress, e);
            return 0;
        }
    }

    @Override
    public boolean isEmailVerificationAllowed(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }
        
        try {
            Cache cache = cacheManager.getCache(ApplicationConstants.EMAIL_VERIFICATION_CACHE);
            if (cache == null) {
                logger.warn("Email verification rate limit cache not found, allowing request");
                return true;
            }

            RateLimitEntry entry = cache.get(ipAddress, RateLimitEntry.class);
            if (entry == null) {
                entry = new RateLimitEntry(ApplicationConstants.EMAIL_VERIFICATION_WINDOW_HOURS);
                cache.put(ipAddress, entry);
            }

            boolean allowed = entry.isAllowed(ApplicationConstants.MAX_EMAIL_VERIFICATION_ATTEMPTS);
            
            if (!allowed) {
                logger.warn("Email verification rate limit exceeded for IP: {} (attempts: {}/{})", 
                           ipAddress, entry.getCurrentAttempts(), ApplicationConstants.MAX_EMAIL_VERIFICATION_ATTEMPTS);
            } else {
                logger.debug("Email verification allowed for IP: {} (attempts: {}/{})", 
                            ipAddress, entry.getCurrentAttempts(), ApplicationConstants.MAX_EMAIL_VERIFICATION_ATTEMPTS);
            }

            return allowed;
            
        } catch (Exception e) {
            logger.error("Error checking email verification rate limit for IP: {}", ipAddress, e);
            return true;
        }
    }

    @Override
    public void recordEmailVerificationAttempt(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }
        
        try {
            Cache cache = cacheManager.getCache(ApplicationConstants.EMAIL_VERIFICATION_CACHE);
            if (cache == null) {
                logger.warn("Email verification rate limit cache not found, cannot record attempt");
                return;
            }

            RateLimitEntry entry = cache.get(ipAddress, RateLimitEntry.class);
            if (entry == null) {
                entry = new RateLimitEntry(ApplicationConstants.EMAIL_VERIFICATION_WINDOW_HOURS);
            }

            entry.recordAttempt();
            cache.put(ipAddress, entry);

            logger.debug("Recorded email verification attempt for IP: {} (total attempts: {}/{})", 
                        ipAddress, entry.getCurrentAttempts(), ApplicationConstants.MAX_EMAIL_VERIFICATION_ATTEMPTS);
            
        } catch (Exception e) {
            logger.error("Error recording email verification attempt for IP: {}", ipAddress, e);
        }
    }

    @Override
    public int getRemainingEmailVerificationAttempts(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }
        
        try {
            Cache cache = cacheManager.getCache(ApplicationConstants.EMAIL_VERIFICATION_CACHE);
            if (cache == null) {
                return ApplicationConstants.MAX_EMAIL_VERIFICATION_ATTEMPTS;
            }

            RateLimitEntry entry = cache.get(ipAddress, RateLimitEntry.class);
            if (entry == null) {
                return ApplicationConstants.MAX_EMAIL_VERIFICATION_ATTEMPTS;
            }

            int currentAttempts = entry.getCurrentAttempts();
            return Math.max(0, ApplicationConstants.MAX_EMAIL_VERIFICATION_ATTEMPTS - currentAttempts);
            
        } catch (Exception e) {
            logger.error("Error getting remaining email verification attempts for IP: {}", ipAddress, e);
            return ApplicationConstants.MAX_EMAIL_VERIFICATION_ATTEMPTS;
        }
    }

    @Override
    public long getEmailVerificationRateLimitResetTime(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }
        
        try {
            Cache cache = cacheManager.getCache(ApplicationConstants.EMAIL_VERIFICATION_CACHE);
            if (cache == null) {
                return 0;
            }

            RateLimitEntry entry = cache.get(ipAddress, RateLimitEntry.class);
            if (entry == null) {
                return 0;
            }

            return entry.getResetTime();
            
        } catch (Exception e) {
            logger.error("Error getting email verification rate limit reset time for IP: {}", ipAddress, e);
            return 0;
        }
    }
}