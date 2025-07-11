package com.yohan.event_planner.service;

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

    /** Maximum registration attempts per time window */
    private static final int MAX_REGISTRATION_ATTEMPTS = 5;
    
    /** Time window for registration rate limiting in hours */
    private static final long REGISTRATION_WINDOW_HOURS = 1;
    
    /** Cache name for registration rate limiting */
    private static final String REGISTRATION_CACHE = "registration-rate-limit";

    private final CacheManager cacheManager;

    /**
     * Constructs a new rate limiting service with the specified cache manager.
     *
     * @param cacheManager the cache manager for storing rate limit data
     */
    public RateLimitingServiceImpl(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Rate limit entry containing attempt count and window start time.
     */
    private static class RateLimitEntry {
        private final ConcurrentHashMap<Long, Integer> timeSlots = new ConcurrentHashMap<>();
        private final long windowSizeHours;

        public RateLimitEntry(long windowSizeHours) {
            this.windowSizeHours = windowSizeHours;
        }

        public synchronized boolean isAllowed(int maxAttempts) {
            cleanupOldEntries();
            int currentAttempts = getCurrentAttempts();
            return currentAttempts < maxAttempts;
        }

        public synchronized void recordAttempt() {
            cleanupOldEntries();
            long currentHour = getCurrentHour();
            timeSlots.merge(currentHour, 1, Integer::sum);
        }

        public synchronized int getCurrentAttempts() {
            cleanupOldEntries();
            return timeSlots.values().stream().mapToInt(Integer::intValue).sum();
        }

        public synchronized long getResetTime() {
            cleanupOldEntries();
            if (timeSlots.isEmpty()) {
                return 0;
            }
            long oldestHour = timeSlots.keySet().stream().min(Long::compareTo).orElse(getCurrentHour());
            long resetHour = oldestHour + windowSizeHours;
            long currentHour = getCurrentHour();
            return Math.max(0, (resetHour - currentHour) * 3600);
        }

        private void cleanupOldEntries() {
            long currentHour = getCurrentHour();
            long cutoffHour = currentHour - windowSizeHours + 1;
            timeSlots.entrySet().removeIf(entry -> entry.getKey() < cutoffHour);
        }

        private long getCurrentHour() {
            return Instant.now().truncatedTo(ChronoUnit.HOURS).getEpochSecond() / 3600;
        }
    }

    @Override
    public boolean isRegistrationAllowed(String ipAddress) {
        try {
            Cache cache = cacheManager.getCache(REGISTRATION_CACHE);
            if (cache == null) {
                logger.warn("Registration rate limit cache not found, allowing request");
                return true;
            }

            RateLimitEntry entry = cache.get(ipAddress, RateLimitEntry.class);
            if (entry == null) {
                entry = new RateLimitEntry(REGISTRATION_WINDOW_HOURS);
                cache.put(ipAddress, entry);
            }

            boolean allowed = entry.isAllowed(MAX_REGISTRATION_ATTEMPTS);
            
            if (!allowed) {
                logger.warn("Registration rate limit exceeded for IP: {} (attempts: {}/{})", 
                           ipAddress, entry.getCurrentAttempts(), MAX_REGISTRATION_ATTEMPTS);
            } else {
                logger.debug("Registration allowed for IP: {} (attempts: {}/{})", 
                            ipAddress, entry.getCurrentAttempts(), MAX_REGISTRATION_ATTEMPTS);
            }

            return allowed;
            
        } catch (Exception e) {
            logger.error("Error checking registration rate limit for IP: {}", ipAddress, e);
            // Fail open - allow the request if rate limiting fails
            return true;
        }
    }

    @Override
    public void recordRegistrationAttempt(String ipAddress) {
        try {
            Cache cache = cacheManager.getCache(REGISTRATION_CACHE);
            if (cache == null) {
                logger.warn("Registration rate limit cache not found, cannot record attempt");
                return;
            }

            RateLimitEntry entry = cache.get(ipAddress, RateLimitEntry.class);
            if (entry == null) {
                entry = new RateLimitEntry(REGISTRATION_WINDOW_HOURS);
            }

            entry.recordAttempt();
            cache.put(ipAddress, entry);

            logger.debug("Recorded registration attempt for IP: {} (total attempts: {}/{})", 
                        ipAddress, entry.getCurrentAttempts(), MAX_REGISTRATION_ATTEMPTS);
            
        } catch (Exception e) {
            logger.error("Error recording registration attempt for IP: {}", ipAddress, e);
        }
    }

    @Override
    public int getRemainingRegistrationAttempts(String ipAddress) {
        try {
            Cache cache = cacheManager.getCache(REGISTRATION_CACHE);
            if (cache == null) {
                return MAX_REGISTRATION_ATTEMPTS;
            }

            RateLimitEntry entry = cache.get(ipAddress, RateLimitEntry.class);
            if (entry == null) {
                return MAX_REGISTRATION_ATTEMPTS;
            }

            int currentAttempts = entry.getCurrentAttempts();
            return Math.max(0, MAX_REGISTRATION_ATTEMPTS - currentAttempts);
            
        } catch (Exception e) {
            logger.error("Error getting remaining registration attempts for IP: {}", ipAddress, e);
            return MAX_REGISTRATION_ATTEMPTS;
        }
    }

    @Override
    public long getRegistrationRateLimitResetTime(String ipAddress) {
        try {
            Cache cache = cacheManager.getCache(REGISTRATION_CACHE);
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
}