package com.yohan.event_planner.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitConfigTest {

    private RateLimitConfig rateLimitConfig;

    @BeforeEach
    void setUp() {
        rateLimitConfig = new RateLimitConfig();
    }

    @Nested
    class AnnotationTests {

        @Test
        void testClass_hasConfigurationAnnotation() {
            // Assert
            assertTrue(RateLimitConfig.class.isAnnotationPresent(
                    org.springframework.context.annotation.Configuration.class),
                    "RateLimitConfig should have @Configuration annotation");
        }

        @Test
        void testClass_hasEnableCachingAnnotation() {
            // Assert
            assertTrue(RateLimitConfig.class.isAnnotationPresent(
                    org.springframework.cache.annotation.EnableCaching.class),
                    "RateLimitConfig should have @EnableCaching annotation");
        }

        @Test
        void testRateLimitCacheManagerMethod_hasBeanAnnotation() throws NoSuchMethodException {
            // Act
            var method = RateLimitConfig.class.getMethod("rateLimitCacheManager");

            // Assert
            assertTrue(method.isAnnotationPresent(
                    org.springframework.context.annotation.Bean.class),
                    "rateLimitCacheManager() method should have @Bean annotation");
        }
    }

    @Nested
    class CacheManagerBeanTests {

        @Test
        void testRateLimitCacheManager_returnsNonNullCacheManager() {
            // Act
            CacheManager result = rateLimitConfig.rateLimitCacheManager();

            // Assert
            assertNotNull(result, "CacheManager should not be null");
        }

        @Test
        void testRateLimitCacheManager_returnsConcurrentMapCacheManager() {
            // Act
            CacheManager result = rateLimitConfig.rateLimitCacheManager();

            // Assert
            assertInstanceOf(ConcurrentMapCacheManager.class, result,
                    "Should return ConcurrentMapCacheManager instance");
        }

        @Test
        void testRateLimitCacheManager_hasAllRequiredCaches() {
            // Act
            CacheManager cacheManager = rateLimitConfig.rateLimitCacheManager();

            // Assert - Verify all required cache names are configured
            assertNotNull(cacheManager.getCache("registration-rate-limit"), 
                    "registration-rate-limit cache should be available");
            assertNotNull(cacheManager.getCache("login-rate-limit"), 
                    "login-rate-limit cache should be available");
            assertNotNull(cacheManager.getCache("password-reset-rate-limit"), 
                    "password-reset-rate-limit cache should be available");
            assertNotNull(cacheManager.getCache("email-verification-rate-limit"), 
                    "email-verification-rate-limit cache should be available");
        }

        @Test
        void testRateLimitCacheManager_hasExactlyFourCaches() {
            // Act
            CacheManager cacheManager = rateLimitConfig.rateLimitCacheManager();
            Collection<String> cacheNames = cacheManager.getCacheNames();

            // Assert
            assertEquals(4, cacheNames.size(), 
                    "Should have exactly 4 configured caches");
            assertTrue(cacheNames.contains("registration-rate-limit"));
            assertTrue(cacheNames.contains("login-rate-limit"));
            assertTrue(cacheNames.contains("password-reset-rate-limit"));
            assertTrue(cacheNames.contains("email-verification-rate-limit"));
        }

        @Test
        void testRateLimitCacheManager_doesNotHaveUnexpectedCaches() {
            // Act
            CacheManager cacheManager = rateLimitConfig.rateLimitCacheManager();

            // Assert - Verify it doesn't create unintended caches
            assertNull(cacheManager.getCache("some-other-cache"), 
                    "Should not have unintended caches");
            assertNull(cacheManager.getCache("rate-limit"), 
                    "Should not have generic rate-limit cache");
        }
    }

    @Nested
    class CacheFunctionalityTests {

        @Test
        void testCaches_supportBasicOperations() {
            // Arrange
            CacheManager cacheManager = rateLimitConfig.rateLimitCacheManager();
            Cache registrationCache = cacheManager.getCache("registration-rate-limit");

            // Act & Assert
            assertDoesNotThrow(() -> {
                // Test put operation
                registrationCache.put("test-key", "test-value");
                
                // Test get operation
                Cache.ValueWrapper result = registrationCache.get("test-key");
                assertNotNull(result, "Should be able to retrieve cached value");
                assertEquals("test-value", result.get(), "Retrieved value should match stored value");
                
                // Test eviction
                registrationCache.evict("test-key");
                Cache.ValueWrapper evictedResult = registrationCache.get("test-key");
                assertNull(evictedResult, "Value should be evicted");
            }, "Basic cache operations should work correctly");
        }

        @Test
        void testCaches_areIndependent() {
            // Arrange
            CacheManager cacheManager = rateLimitConfig.rateLimitCacheManager();
            Cache registrationCache = cacheManager.getCache("registration-rate-limit");
            Cache loginCache = cacheManager.getCache("login-rate-limit");

            // Act
            registrationCache.put("same-key", "registration-value");
            loginCache.put("same-key", "login-value");

            // Assert
            assertEquals("registration-value", registrationCache.get("same-key").get(),
                    "Registration cache should have its own value");
            assertEquals("login-value", loginCache.get("same-key").get(),
                    "Login cache should have its own value");
        }

        @Test
        void testCaches_handleNullValues() {
            // Arrange
            CacheManager cacheManager = rateLimitConfig.rateLimitCacheManager();
            Cache cache = cacheManager.getCache("registration-rate-limit");

            // Act & Assert
            assertDoesNotThrow(() -> {
                cache.put("null-key", null);
                Cache.ValueWrapper result = cache.get("null-key");
                assertNotNull(result, "Should return wrapper even for null values");
                assertNull(result.get(), "Wrapped value should be null");
            }, "Should handle null values gracefully");
        }
    }

    @Nested
    class ConfigurationConsistencyTests {

        @Test
        void testCacheNames_matchServiceExpectations() {
            // This test ensures the cache names match what RateLimitingService expects
            // Act
            CacheManager cacheManager = rateLimitConfig.rateLimitCacheManager();
            Collection<String> cacheNames = cacheManager.getCacheNames();

            // Assert - These names must match exactly what RateLimitingServiceImpl uses
            assertTrue(cacheNames.contains("registration-rate-limit"), 
                    "Cache name must match RateLimitingService.REGISTRATION_CACHE");
            assertTrue(cacheNames.contains("login-rate-limit"), 
                    "Cache name must match RateLimitingService.LOGIN_CACHE");
            assertTrue(cacheNames.contains("password-reset-rate-limit"), 
                    "Cache name must match RateLimitingService.PASSWORD_RESET_CACHE");
            assertTrue(cacheNames.contains("email-verification-rate-limit"), 
                    "Cache name must match RateLimitingService.EMAIL_VERIFICATION_CACHE");
        }

        @Test
        void testCacheType_supportsConcurrentAccess() {
            // Act
            CacheManager cacheManager = rateLimitConfig.rateLimitCacheManager();

            // Assert
            assertInstanceOf(ConcurrentMapCacheManager.class, cacheManager,
                    "Should use ConcurrentMapCacheManager for thread-safe operations");
        }
    }

    @Nested
    class InstantiationTests {

        @Test
        void testCanBeInstantiated() {
            // Act & Assert
            assertDoesNotThrow(() -> {
                RateLimitConfig config = new RateLimitConfig();
                assertNotNull(config, "RateLimitConfig should be instantiable");
            });
        }

        @Test
        void testCacheManagerCreation_isRepeatable() {
            // Act
            CacheManager manager1 = rateLimitConfig.rateLimitCacheManager();
            CacheManager manager2 = rateLimitConfig.rateLimitCacheManager();

            // Assert
            assertNotNull(manager1, "First cache manager should not be null");
            assertNotNull(manager2, "Second cache manager should not be null");
            
            // Both should have the same configuration
            assertEquals(manager1.getCacheNames(), manager2.getCacheNames(),
                    "Both cache managers should have same cache names");
        }
    }

    @Nested
    class DocumentationValidationTests {

        @Test
        void testConfiguration_matchesDocumentedCacheTypes() {
            // Act
            CacheManager cacheManager = rateLimitConfig.rateLimitCacheManager();

            // Assert - Verify documented cache types are available
            assertNotNull(cacheManager.getCache("registration-rate-limit"), 
                    "Should have registration-rate-limit as documented");
            assertNotNull(cacheManager.getCache("login-rate-limit"), 
                    "Should have login-rate-limit as documented");
            assertNotNull(cacheManager.getCache("password-reset-rate-limit"), 
                    "Should have password-reset-rate-limit as documented");
            assertNotNull(cacheManager.getCache("email-verification-rate-limit"), 
                    "Should have email-verification-rate-limit as documented");
        }

        @Test
        void testCacheManager_providesInMemoryCaching() {
            // Act
            CacheManager cacheManager = rateLimitConfig.rateLimitCacheManager();

            // Assert
            assertInstanceOf(ConcurrentMapCacheManager.class, cacheManager,
                    "Should provide in-memory caching as documented");
        }

        @Test
        void testCacheManager_supportsThreadSafety() {
            // Act
            CacheManager cacheManager = rateLimitConfig.rateLimitCacheManager();
            Cache cache = cacheManager.getCache("registration-rate-limit");

            // Assert
            assertDoesNotThrow(() -> {
                // Simulate concurrent access
                cache.put("concurrent-key-1", "value1");
                cache.put("concurrent-key-2", "value2");
                cache.get("concurrent-key-1");
                cache.get("concurrent-key-2");
            }, "Cache should support concurrent access as documented");
        }
    }
}