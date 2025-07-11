package com.yohan.event_planner.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for rate limiting functionality using Spring Cache.
 *
 * <p>
 * This configuration provides caching infrastructure for implementing rate limiting
 * across various endpoints. It uses in-memory caching for simplicity and performance,
 * suitable for single-instance deployments or when using sticky sessions.
 * </p>
 *
 * <h2>Rate Limiting Strategy</h2>
 * <ul>
 *   <li><strong>In-Memory Cache</strong>: Fast access with automatic cleanup</li>
 *   <li><strong>Per-IP Tracking</strong>: Rate limits applied per client IP address</li>
 *   <li><strong>Time Windows</strong>: Configurable time windows for rate limiting</li>
 *   <li><strong>Automatic Expiry</strong>: Cached rate limit data expires automatically</li>
 * </ul>
 *
 * <h2>Supported Cache Types</h2>
 * <ul>
 *   <li><strong>registration-rate-limit</strong>: For registration attempt limiting</li>
 *   <li><strong>login-rate-limit</strong>: For login attempt limiting (future use)</li>
 *   <li><strong>email-rate-limit</strong>: For email operation limiting (future use)</li>
 * </ul>
 *
 * <h2>Configuration Properties</h2>
 * <p>
 * Rate limiting behavior can be customized via application properties:
 * </p>
 * <ul>
 *   <li>{@code app.rate-limit.registration.max-attempts}: Maximum registration attempts per window</li>
 *   <li>{@code app.rate-limit.registration.window-seconds}: Time window in seconds</li>
 * </ul>
 *
 * <h2>Production Considerations</h2>
 * <p>
 * For production deployments with multiple instances, consider:
 * </p>
 * <ul>
 *   <li><strong>Redis Cache</strong>: Shared cache across multiple instances</li>
 *   <li><strong>Database Rate Limiting</strong>: Persistent rate limit tracking</li>
 *   <li><strong>Load Balancer</strong>: Rate limiting at infrastructure level</li>
 *   <li><strong>CDN Rate Limiting</strong>: Edge-based rate limiting for global traffic</li>
 * </ul>
 *
 * @see com.yohan.event_planner.service.RateLimitingService
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.1.0
 */
@Configuration
@EnableCaching
public class RateLimitConfig {

    /**
     * Creates a cache manager for rate limiting operations.
     *
     * <p>
     * This cache manager uses concurrent maps for thread-safe in-memory caching.
     * Each cache has automatic cleanup and supports configurable TTL (time-to-live)
     * for rate limiting windows.
     * </p>
     *
     * @return a configured cache manager for rate limiting
     */
    @Bean
    public CacheManager rateLimitCacheManager() {
        return new ConcurrentMapCacheManager(
            "registration-rate-limit",
            "login-rate-limit", 
            "email-rate-limit"
        );
    }
}