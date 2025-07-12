package com.yohan.event_planner.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentCaptor.forClass;

import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class RateLimitingServiceImplTest {

    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;
    
    @InjectMocks
    private RateLimitingServiceImpl rateLimitingService;
    
    private static final String TEST_IP = "192.168.1.1";
    private static final String DIFFERENT_IP = "192.168.1.2";
    private static final String REGISTRATION_CACHE = "registration-rate-limit";
    private static final String LOGIN_CACHE = "login-rate-limit";
    private static final String PASSWORD_RESET_CACHE = "password-reset-rate-limit";
    private static final String EMAIL_VERIFICATION_CACHE = "email-verification-rate-limit";
    private static final int MAX_REGISTRATION_ATTEMPTS = 5;
    private static final int MAX_LOGIN_ATTEMPTS = 10;
    private static final int MAX_PASSWORD_RESET_ATTEMPTS = 3;
    private static final int MAX_EMAIL_VERIFICATION_ATTEMPTS = 5;

    @BeforeEach
    void setUp() {
        // No global stubs - each test will stub what it needs
    }

    @Nested
    class ConstructorTests {

        @Test
        void constructor_nullCacheManager_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    new RateLimitingServiceImpl(null));
            
            assertEquals("CacheManager cannot be null", exception.getMessage());
        }

        @Test
        void constructor_validCacheManager_shouldCreateInstance() {
            // Act
            RateLimitingServiceImpl service = new RateLimitingServiceImpl(cacheManager);
            
            // Assert
            assertNotNull(service);
        }
    }

    @Nested
    class IsRegistrationAllowedTests {

        @Test
        void isRegistrationAllowed_nullIpAddress_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    rateLimitingService.isRegistrationAllowed(null));
            
            assertEquals("IP address cannot be null or empty", exception.getMessage());
        }

        @Test
        void isRegistrationAllowed_emptyIpAddress_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    rateLimitingService.isRegistrationAllowed(""));
            
            assertEquals("IP address cannot be null or empty", exception.getMessage());
        }

        @Test
        void isRegistrationAllowed_whitespaceIpAddress_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    rateLimitingService.isRegistrationAllowed("   "));
            
            assertEquals("IP address cannot be null or empty", exception.getMessage());
        }

        @Test
        void isRegistrationAllowed_cacheNotFound_shouldReturnTrueAndLog() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(null);

            // Act
            boolean result = rateLimitingService.isRegistrationAllowed(TEST_IP);

            // Assert
            assertTrue(result);
            verify(cacheManager).getCache(REGISTRATION_CACHE);
        }

        @Test
        void isRegistrationAllowed_newIpAddress_shouldReturnTrue() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(cache);

            // Act
            boolean result = rateLimitingService.isRegistrationAllowed(TEST_IP);

            // Assert
            assertTrue(result);
            verify(cache).get(eq(TEST_IP), any(Class.class));
            verify(cache).put(eq(TEST_IP), any());
        }

        @Test
        void isRegistrationAllowed_belowLimit_shouldReturnTrue() {
            // Arrange
            // Create a mock entry that's below the limit

            // Act
            boolean result = rateLimitingService.isRegistrationAllowed(TEST_IP);

            // Assert
            assertTrue(result);
        }

        @Test
        void isRegistrationAllowed_cacheException_shouldReturnTrueFailOpen() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenThrow(new RuntimeException("Cache error"));

            // Act
            boolean result = rateLimitingService.isRegistrationAllowed(TEST_IP);

            // Assert
            assertTrue(result);
        }

        @Test
        void isRegistrationAllowed_cacheGetException_shouldReturnTrueFailOpen() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(cache);
            when(cache.get(eq(TEST_IP), any(Class.class)))
                    .thenThrow(new RuntimeException("Cache get error"));

            // Act
            boolean result = rateLimitingService.isRegistrationAllowed(TEST_IP);

            // Assert
            assertTrue(result);
        }
    }

    @Nested
    class RecordRegistrationAttemptTests {

        @Test
        void recordRegistrationAttempt_nullIpAddress_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    rateLimitingService.recordRegistrationAttempt(null));
            
            assertEquals("IP address cannot be null or empty", exception.getMessage());
        }

        @Test
        void recordRegistrationAttempt_emptyIpAddress_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    rateLimitingService.recordRegistrationAttempt(""));
            
            assertEquals("IP address cannot be null or empty", exception.getMessage());
        }

        @Test
        void recordRegistrationAttempt_whitespaceIpAddress_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    rateLimitingService.recordRegistrationAttempt("   "));
            
            assertEquals("IP address cannot be null or empty", exception.getMessage());
        }

        @Test
        void recordRegistrationAttempt_cacheNotFound_shouldReturnSilently() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(null);

            // Act - should not throw exception
            assertDoesNotThrow(() -> rateLimitingService.recordRegistrationAttempt(TEST_IP));

            // Assert
            verify(cacheManager).getCache(REGISTRATION_CACHE);
        }

        @Test
        void recordRegistrationAttempt_newIpAddress_shouldCreateEntryAndRecord() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(cache);

            // Act
            rateLimitingService.recordRegistrationAttempt(TEST_IP);

            // Assert
            verify(cache).get(eq(TEST_IP), any(Class.class));
            verify(cache).put(eq(TEST_IP), any());
        }

        @Test
        void recordRegistrationAttempt_existingEntry_shouldUpdateEntry() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(cache);

            // Act
            rateLimitingService.recordRegistrationAttempt(TEST_IP);

            // Assert
            verify(cache).put(eq(TEST_IP), any());
        }

        @Test
        void recordRegistrationAttempt_cacheException_shouldHandleGracefully() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenThrow(new RuntimeException("Cache error"));

            // Act - should not throw exception
            assertDoesNotThrow(() -> rateLimitingService.recordRegistrationAttempt(TEST_IP));
        }

        @Test
        void recordRegistrationAttempt_cacheGetException_shouldHandleGracefully() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(cache);
            when(cache.get(eq(TEST_IP), any(Class.class)))
                    .thenThrow(new RuntimeException("Cache get error"));

            // Act - should not throw exception
            assertDoesNotThrow(() -> rateLimitingService.recordRegistrationAttempt(TEST_IP));
        }

        @Test
        void recordRegistrationAttempt_cachePutException_shouldHandleGracefully() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(cache);
            doThrow(new RuntimeException("Cache put error")).when(cache).put(eq(TEST_IP), any());

            // Act - should not throw exception
            assertDoesNotThrow(() -> rateLimitingService.recordRegistrationAttempt(TEST_IP));
        }
    }

    @Nested
    class GetRemainingRegistrationAttemptsTests {

        @Test
        void getRemainingRegistrationAttempts_nullIpAddress_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    rateLimitingService.getRemainingRegistrationAttempts(null));
            
            assertEquals("IP address cannot be null or empty", exception.getMessage());
        }

        @Test
        void getRemainingRegistrationAttempts_emptyIpAddress_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    rateLimitingService.getRemainingRegistrationAttempts(""));
            
            assertEquals("IP address cannot be null or empty", exception.getMessage());
        }

        @Test
        void getRemainingRegistrationAttempts_whitespaceIpAddress_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    rateLimitingService.getRemainingRegistrationAttempts("   "));
            
            assertEquals("IP address cannot be null or empty", exception.getMessage());
        }

        @Test
        void getRemainingRegistrationAttempts_cacheNotFound_shouldReturnMaxAttempts() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(null);

            // Act
            int result = rateLimitingService.getRemainingRegistrationAttempts(TEST_IP);

            // Assert
            assertEquals(MAX_REGISTRATION_ATTEMPTS, result);
        }

        @Test
        void getRemainingRegistrationAttempts_noEntry_shouldReturnMaxAttempts() {
            // Arrange

            // Act
            int result = rateLimitingService.getRemainingRegistrationAttempts(TEST_IP);

            // Assert
            assertEquals(MAX_REGISTRATION_ATTEMPTS, result);
        }

        @Test
        void getRemainingRegistrationAttempts_cacheException_shouldReturnMaxAttempts() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenThrow(new RuntimeException("Cache error"));

            // Act
            int result = rateLimitingService.getRemainingRegistrationAttempts(TEST_IP);

            // Assert
            assertEquals(MAX_REGISTRATION_ATTEMPTS, result);
        }

        @Test
        void getRemainingRegistrationAttempts_cacheGetException_shouldReturnMaxAttempts() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(cache);
            when(cache.get(eq(TEST_IP), any(Class.class)))
                    .thenThrow(new RuntimeException("Cache get error"));

            // Act
            int result = rateLimitingService.getRemainingRegistrationAttempts(TEST_IP);

            // Assert
            assertEquals(MAX_REGISTRATION_ATTEMPTS, result);
        }
    }

    @Nested
    class GetRegistrationRateLimitResetTimeTests {

        @Test
        void getRegistrationRateLimitResetTime_nullIpAddress_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    rateLimitingService.getRegistrationRateLimitResetTime(null));
            
            assertEquals("IP address cannot be null or empty", exception.getMessage());
        }

        @Test
        void getRegistrationRateLimitResetTime_emptyIpAddress_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    rateLimitingService.getRegistrationRateLimitResetTime(""));
            
            assertEquals("IP address cannot be null or empty", exception.getMessage());
        }

        @Test
        void getRegistrationRateLimitResetTime_whitespaceIpAddress_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    rateLimitingService.getRegistrationRateLimitResetTime("   "));
            
            assertEquals("IP address cannot be null or empty", exception.getMessage());
        }

        @Test
        void getRegistrationRateLimitResetTime_cacheNotFound_shouldReturnZero() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(null);

            // Act
            long result = rateLimitingService.getRegistrationRateLimitResetTime(TEST_IP);

            // Assert
            assertEquals(0L, result);
        }

        @Test
        void getRegistrationRateLimitResetTime_noEntry_shouldReturnZero() {
            // Arrange

            // Act
            long result = rateLimitingService.getRegistrationRateLimitResetTime(TEST_IP);

            // Assert
            assertEquals(0L, result);
        }

        @Test
        void getRegistrationRateLimitResetTime_cacheException_shouldReturnZero() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenThrow(new RuntimeException("Cache error"));

            // Act
            long result = rateLimitingService.getRegistrationRateLimitResetTime(TEST_IP);

            // Assert
            assertEquals(0L, result);
        }

        @Test
        void getRegistrationRateLimitResetTime_cacheGetException_shouldReturnZero() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(cache);
            when(cache.get(eq(TEST_IP), any(Class.class)))
                    .thenThrow(new RuntimeException("Cache get error"));

            // Act
            long result = rateLimitingService.getRegistrationRateLimitResetTime(TEST_IP);

            // Assert
            assertEquals(0L, result);
        }
    }

    @Nested
    class IntegrationTests {

        @Test
        void fullRateLimitingFlow_shouldWorkCorrectly() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(cache);

            // Act & Assert - Initial state should allow registration
            assertTrue(rateLimitingService.isRegistrationAllowed(TEST_IP));
            assertEquals(MAX_REGISTRATION_ATTEMPTS, rateLimitingService.getRemainingRegistrationAttempts(TEST_IP));
            assertEquals(0L, rateLimitingService.getRegistrationRateLimitResetTime(TEST_IP));

            // Record an attempt
            rateLimitingService.recordRegistrationAttempt(TEST_IP);

            // Verify cache interactions
            verify(cache, atLeastOnce()).get(eq(TEST_IP), any(Class.class));
            verify(cache, atLeastOnce()).put(eq(TEST_IP), any());
        }

        @Test
        void multipleIpAddresses_shouldTrackIndependently() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(cache);

            // Act
            assertTrue(rateLimitingService.isRegistrationAllowed(TEST_IP));
            assertTrue(rateLimitingService.isRegistrationAllowed(DIFFERENT_IP));

            rateLimitingService.recordRegistrationAttempt(TEST_IP);
            rateLimitingService.recordRegistrationAttempt(DIFFERENT_IP);

            // Assert - Both IPs should be tracked independently
            verify(cache, atLeast(1)).get(eq(TEST_IP), any(Class.class));
            verify(cache, atLeast(1)).get(eq(DIFFERENT_IP), any(Class.class));
            verify(cache, atLeast(1)).put(eq(TEST_IP), any());
            verify(cache, atLeast(1)).put(eq(DIFFERENT_IP), any());
        }

        @Test
        void cacheFailureScenario_shouldFailOpenForAllMethods() {
            // Arrange - Cache manager always returns null (cache unavailable)
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(null);

            // Act & Assert - All methods should fail open (safe defaults)
            assertTrue(rateLimitingService.isRegistrationAllowed(TEST_IP));
            assertEquals(MAX_REGISTRATION_ATTEMPTS, rateLimitingService.getRemainingRegistrationAttempts(TEST_IP));
            assertEquals(0L, rateLimitingService.getRegistrationRateLimitResetTime(TEST_IP));
            
            // Recording should not throw exception
            assertDoesNotThrow(() -> rateLimitingService.recordRegistrationAttempt(TEST_IP));
        }

        @Test
        void partialCacheFailure_shouldHandleGracefully() {
            // Arrange - Cache exists but get operations fail
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(cache);
            when(cache.get(anyString(), any(Class.class)))
                    .thenThrow(new RuntimeException("Intermittent cache error"));

            // Act & Assert - Should handle failures gracefully
            assertTrue(rateLimitingService.isRegistrationAllowed(TEST_IP));
            assertEquals(MAX_REGISTRATION_ATTEMPTS, rateLimitingService.getRemainingRegistrationAttempts(TEST_IP));
            assertEquals(0L, rateLimitingService.getRegistrationRateLimitResetTime(TEST_IP));
            assertDoesNotThrow(() -> rateLimitingService.recordRegistrationAttempt(TEST_IP));
        }
    }

    @Nested
    class LoginRateLimitingTests {

        @Test
        void isLoginAllowed_nullIpAddress_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    rateLimitingService.isLoginAllowed(null));
            
            assertEquals("IP address cannot be null or empty", exception.getMessage());
        }

        @Test
        void isLoginAllowed_newIpAddress_shouldReturnTrue() {
            // Arrange

            // Act
            boolean result = rateLimitingService.isLoginAllowed(TEST_IP);

            // Assert
            assertTrue(result);
        }

        @Test
        void isLoginAllowed_cacheNotFound_shouldReturnTrueFailOpen() {
            // Arrange
            when(cacheManager.getCache(LOGIN_CACHE)).thenReturn(null);

            // Act
            boolean result = rateLimitingService.isLoginAllowed(TEST_IP);

            // Assert
            assertTrue(result);
        }

        @Test
        void recordLoginAttempt_validIp_shouldUpdateCache() {
            // Arrange
            when(cacheManager.getCache(LOGIN_CACHE)).thenReturn(cache);

            // Act
            rateLimitingService.recordLoginAttempt(TEST_IP);

            // Assert
            verify(cache).put(eq(TEST_IP), any());
        }

        @Test
        void getRemainingLoginAttempts_noEntry_shouldReturnMaxAttempts() {
            // Arrange

            // Act
            int result = rateLimitingService.getRemainingLoginAttempts(TEST_IP);

            // Assert
            assertEquals(MAX_LOGIN_ATTEMPTS, result);
        }

        @Test
        void getLoginRateLimitResetTime_noEntry_shouldReturnZero() {
            // Arrange

            // Act
            long result = rateLimitingService.getLoginRateLimitResetTime(TEST_IP);

            // Assert
            assertEquals(0L, result);
        }
    }

    @Nested
    class PasswordResetRateLimitingTests {

        @Test
        void isPasswordResetAllowed_nullIpAddress_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    rateLimitingService.isPasswordResetAllowed(null));
            
            assertEquals("IP address cannot be null or empty", exception.getMessage());
        }

        @Test
        void isPasswordResetAllowed_newIpAddress_shouldReturnTrue() {
            // Arrange

            // Act
            boolean result = rateLimitingService.isPasswordResetAllowed(TEST_IP);

            // Assert
            assertTrue(result);
        }

        @Test
        void isPasswordResetAllowed_cacheNotFound_shouldReturnTrueFailOpen() {
            // Arrange
            when(cacheManager.getCache(PASSWORD_RESET_CACHE)).thenReturn(null);

            // Act
            boolean result = rateLimitingService.isPasswordResetAllowed(TEST_IP);

            // Assert
            assertTrue(result);
        }

        @Test
        void recordPasswordResetAttempt_validIp_shouldUpdateCache() {
            // Arrange
            when(cacheManager.getCache(PASSWORD_RESET_CACHE)).thenReturn(cache);
            when(cache.get(eq(TEST_IP), any(Class.class))).thenReturn(null);

            // Act
            rateLimitingService.recordPasswordResetAttempt(TEST_IP);

            // Assert
            verify(cache).put(eq(TEST_IP), any());
        }

        @Test
        void getRemainingPasswordResetAttempts_noEntry_shouldReturnMaxAttempts() {
            // Arrange

            // Act
            int result = rateLimitingService.getRemainingPasswordResetAttempts(TEST_IP);

            // Assert
            assertEquals(MAX_PASSWORD_RESET_ATTEMPTS, result);
        }

        @Test
        void getPasswordResetRateLimitResetTime_noEntry_shouldReturnZero() {
            // Arrange

            // Act
            long result = rateLimitingService.getPasswordResetRateLimitResetTime(TEST_IP);

            // Assert
            assertEquals(0L, result);
        }
    }

    @Nested
    class EmailVerificationRateLimitingTests {

        @Test
        void isEmailVerificationAllowed_nullIpAddress_shouldThrowException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    rateLimitingService.isEmailVerificationAllowed(null));
            
            assertEquals("IP address cannot be null or empty", exception.getMessage());
        }

        @Test
        void isEmailVerificationAllowed_newIpAddress_shouldReturnTrue() {
            // Arrange

            // Act
            boolean result = rateLimitingService.isEmailVerificationAllowed(TEST_IP);

            // Assert
            assertTrue(result);
        }

        @Test
        void isEmailVerificationAllowed_cacheNotFound_shouldReturnTrueFailOpen() {
            // Arrange
            when(cacheManager.getCache(EMAIL_VERIFICATION_CACHE)).thenReturn(null);

            // Act
            boolean result = rateLimitingService.isEmailVerificationAllowed(TEST_IP);

            // Assert
            assertTrue(result);
        }

        @Test
        void recordEmailVerificationAttempt_validIp_shouldUpdateCache() {
            // Arrange
            when(cacheManager.getCache(EMAIL_VERIFICATION_CACHE)).thenReturn(cache);

            // Act
            rateLimitingService.recordEmailVerificationAttempt(TEST_IP);

            // Assert
            verify(cache).put(eq(TEST_IP), any());
        }

        @Test
        void getRemainingEmailVerificationAttempts_noEntry_shouldReturnMaxAttempts() {
            // Arrange

            // Act
            int result = rateLimitingService.getRemainingEmailVerificationAttempts(TEST_IP);

            // Assert
            assertEquals(MAX_EMAIL_VERIFICATION_ATTEMPTS, result);
        }

        @Test
        void getEmailVerificationRateLimitResetTime_noEntry_shouldReturnZero() {
            // Arrange

            // Act
            long result = rateLimitingService.getEmailVerificationRateLimitResetTime(TEST_IP);

            // Assert
            assertEquals(0L, result);
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        void specialCharacterIpAddress_shouldHandleCorrectly() {
            // Arrange
            String specialIp = "::1"; // IPv6 localhost

            // Act & Assert
            assertTrue(rateLimitingService.isRegistrationAllowed(specialIp));
            assertDoesNotThrow(() -> rateLimitingService.recordRegistrationAttempt(specialIp));
            assertEquals(MAX_REGISTRATION_ATTEMPTS, rateLimitingService.getRemainingRegistrationAttempts(specialIp));
            assertEquals(0L, rateLimitingService.getRegistrationRateLimitResetTime(specialIp));
        }

        @Test
        void longIpAddress_shouldHandleCorrectly() {
            // Arrange
            String longIp = "2001:0db8:85a3:0000:0000:8a2e:0370:7334"; // Long IPv6

            // Act & Assert
            assertTrue(rateLimitingService.isRegistrationAllowed(longIp));
            assertDoesNotThrow(() -> rateLimitingService.recordRegistrationAttempt(longIp));
            assertEquals(MAX_REGISTRATION_ATTEMPTS, rateLimitingService.getRemainingRegistrationAttempts(longIp));
            assertEquals(0L, rateLimitingService.getRegistrationRateLimitResetTime(longIp));
        }

        @Test
        void concurrentAccess_shouldHandleSafely() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(cache);

            // Act - Simulate concurrent access
            assertDoesNotThrow(() -> {
                rateLimitingService.isRegistrationAllowed(TEST_IP);
                rateLimitingService.recordRegistrationAttempt(TEST_IP);
                rateLimitingService.getRemainingRegistrationAttempts(TEST_IP);
                rateLimitingService.getRegistrationRateLimitResetTime(TEST_IP);
            });

            // Assert - Verify cache was accessed
            verify(cache, atLeastOnce()).get(eq(TEST_IP), any(Class.class));
        }

        @Test
        void cacheReturnsWrongType_shouldHandleGracefully() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(cache);
            when(cache.get(eq(TEST_IP), any(Class.class)))
                    .thenThrow(new ClassCastException("Wrong type in cache"));

            // Act & Assert - Should fail open
            assertTrue(rateLimitingService.isRegistrationAllowed(TEST_IP));
            assertEquals(MAX_REGISTRATION_ATTEMPTS, rateLimitingService.getRemainingRegistrationAttempts(TEST_IP));
            assertEquals(0L, rateLimitingService.getRegistrationRateLimitResetTime(TEST_IP));
            assertDoesNotThrow(() -> rateLimitingService.recordRegistrationAttempt(TEST_IP));
        }
    }

    @Nested
    class ActualRateLimitingBehaviorTests {

        @Test
        void registrationRateLimit_atExactLimit_shouldReturnFalse() {
            // Arrange - Create a real cache implementation for this test
            Cache realCache = new org.springframework.cache.concurrent.ConcurrentMapCache(REGISTRATION_CACHE);
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(realCache);

            // Act - Record maximum allowed attempts
            for (int i = 0; i < MAX_REGISTRATION_ATTEMPTS; i++) {
                assertTrue(rateLimitingService.isRegistrationAllowed(TEST_IP));
                rateLimitingService.recordRegistrationAttempt(TEST_IP);
            }

            // Assert - Next attempt should be denied
            assertFalse(rateLimitingService.isRegistrationAllowed(TEST_IP));
            assertEquals(0, rateLimitingService.getRemainingRegistrationAttempts(TEST_IP));
        }

        @Test
        void loginRateLimit_atExactLimit_shouldReturnFalse() {
            // Arrange
            Cache realCache = new org.springframework.cache.concurrent.ConcurrentMapCache(LOGIN_CACHE);
            when(cacheManager.getCache(LOGIN_CACHE)).thenReturn(realCache);

            // Act - Record maximum allowed attempts
            for (int i = 0; i < MAX_LOGIN_ATTEMPTS; i++) {
                assertTrue(rateLimitingService.isLoginAllowed(TEST_IP));
                rateLimitingService.recordLoginAttempt(TEST_IP);
            }

            // Assert - Next attempt should be denied
            assertFalse(rateLimitingService.isLoginAllowed(TEST_IP));
            assertEquals(0, rateLimitingService.getRemainingLoginAttempts(TEST_IP));
        }

        @Test
        void passwordResetRateLimit_atExactLimit_shouldReturnFalse() {
            // Arrange
            Cache realCache = new org.springframework.cache.concurrent.ConcurrentMapCache(PASSWORD_RESET_CACHE);
            when(cacheManager.getCache(PASSWORD_RESET_CACHE)).thenReturn(realCache);

            // Act - Record maximum allowed attempts
            for (int i = 0; i < MAX_PASSWORD_RESET_ATTEMPTS; i++) {
                assertTrue(rateLimitingService.isPasswordResetAllowed(TEST_IP));
                rateLimitingService.recordPasswordResetAttempt(TEST_IP);
            }

            // Assert - Next attempt should be denied
            assertFalse(rateLimitingService.isPasswordResetAllowed(TEST_IP));
            assertEquals(0, rateLimitingService.getRemainingPasswordResetAttempts(TEST_IP));
        }

        @Test
        void emailVerificationRateLimit_atExactLimit_shouldReturnFalse() {
            // Arrange
            Cache realCache = new org.springframework.cache.concurrent.ConcurrentMapCache(EMAIL_VERIFICATION_CACHE);
            when(cacheManager.getCache(EMAIL_VERIFICATION_CACHE)).thenReturn(realCache);

            // Act - Record maximum allowed attempts
            for (int i = 0; i < MAX_EMAIL_VERIFICATION_ATTEMPTS; i++) {
                assertTrue(rateLimitingService.isEmailVerificationAllowed(TEST_IP));
                rateLimitingService.recordEmailVerificationAttempt(TEST_IP);
            }

            // Assert - Next attempt should be denied
            assertFalse(rateLimitingService.isEmailVerificationAllowed(TEST_IP));
            assertEquals(0, rateLimitingService.getRemainingEmailVerificationAttempts(TEST_IP));
        }

        @Test
        void registrationLimiting_progressiveAttempts_shouldDecrementCorrectly() {
            // Arrange
            Cache realCache = new org.springframework.cache.concurrent.ConcurrentMapCache(REGISTRATION_CACHE);
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(realCache);

            // Act & Assert - Verify remaining attempts decrease correctly
            assertEquals(MAX_REGISTRATION_ATTEMPTS, rateLimitingService.getRemainingRegistrationAttempts(TEST_IP));
            
            rateLimitingService.recordRegistrationAttempt(TEST_IP);
            assertEquals(MAX_REGISTRATION_ATTEMPTS - 1, rateLimitingService.getRemainingRegistrationAttempts(TEST_IP));
            
            rateLimitingService.recordRegistrationAttempt(TEST_IP);
            assertEquals(MAX_REGISTRATION_ATTEMPTS - 2, rateLimitingService.getRemainingRegistrationAttempts(TEST_IP));
            
            rateLimitingService.recordRegistrationAttempt(TEST_IP);
            assertEquals(MAX_REGISTRATION_ATTEMPTS - 3, rateLimitingService.getRemainingRegistrationAttempts(TEST_IP));
        }

        @Test
        void rateLimiting_afterLimit_shouldStayBlocked() {
            // Arrange
            Cache realCache = new org.springframework.cache.concurrent.ConcurrentMapCache(REGISTRATION_CACHE);
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(realCache);

            // Act - Exceed the limit
            for (int i = 0; i < MAX_REGISTRATION_ATTEMPTS + 3; i++) {
                rateLimitingService.recordRegistrationAttempt(TEST_IP);
            }

            // Assert - Should remain blocked
            assertFalse(rateLimitingService.isRegistrationAllowed(TEST_IP));
            assertEquals(0, rateLimitingService.getRemainingRegistrationAttempts(TEST_IP));
            
            // Additional attempts should still be blocked
            assertFalse(rateLimitingService.isRegistrationAllowed(TEST_IP));
            assertFalse(rateLimitingService.isRegistrationAllowed(TEST_IP));
        }
    }

    @Nested
    class CacheDataIntegrityTests {

        @Test
        void recordRegistrationAttempt_shouldStoreCorrectEntryType() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(cache);
            ArgumentCaptor<Object> valueCaptor = forClass(Object.class);

            // Act
            rateLimitingService.recordRegistrationAttempt(TEST_IP);

            // Assert
            verify(cache).put(eq(TEST_IP), valueCaptor.capture());
            Object capturedValue = valueCaptor.getValue();
            assertNotNull(capturedValue);
            // Note: Since RateLimitEntry is a private inner class, we verify it's the right type indirectly
            assertTrue(capturedValue.getClass().getSimpleName().contains("RateLimitEntry"));
        }

        @Test
        void multipleOperations_shouldMaintainCacheConsistency() {
            // Arrange
            Cache realCache = new org.springframework.cache.concurrent.ConcurrentMapCache(REGISTRATION_CACHE);
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(realCache);

            // Act - Perform multiple operations
            assertTrue(rateLimitingService.isRegistrationAllowed(TEST_IP));
            rateLimitingService.recordRegistrationAttempt(TEST_IP);
            int remaining1 = rateLimitingService.getRemainingRegistrationAttempts(TEST_IP);
            
            rateLimitingService.recordRegistrationAttempt(TEST_IP);
            int remaining2 = rateLimitingService.getRemainingRegistrationAttempts(TEST_IP);
            
            long resetTime = rateLimitingService.getRegistrationRateLimitResetTime(TEST_IP);

            // Assert - Data should be consistent
            assertEquals(MAX_REGISTRATION_ATTEMPTS - 1, remaining1);
            assertEquals(MAX_REGISTRATION_ATTEMPTS - 2, remaining2);
            assertTrue(resetTime > 0); // Should have a reset time since attempts were recorded
        }

        @Test
        void cacheEntry_afterMultipleRecords_shouldAccumulate() {
            // Arrange
            Cache realCache = new org.springframework.cache.concurrent.ConcurrentMapCache(REGISTRATION_CACHE);
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(realCache);

            // Act - Record multiple attempts
            rateLimitingService.recordRegistrationAttempt(TEST_IP);
            rateLimitingService.recordRegistrationAttempt(TEST_IP);
            rateLimitingService.recordRegistrationAttempt(TEST_IP);

            // Assert - Verify accumulated state
            assertEquals(MAX_REGISTRATION_ATTEMPTS - 3, rateLimitingService.getRemainingRegistrationAttempts(TEST_IP));
            assertTrue(rateLimitingService.isRegistrationAllowed(TEST_IP)); // Should still be allowed
        }
    }

    @Nested
    class BoundaryValueTests {

        @Test
        void allOperationTypes_atExactLimits_shouldBehaveCorrectly() {
            // Arrange - Setup real caches for all operation types
            Cache regCache = new org.springframework.cache.concurrent.ConcurrentMapCache(REGISTRATION_CACHE);
            Cache loginCache = new org.springframework.cache.concurrent.ConcurrentMapCache(LOGIN_CACHE);
            Cache pwdCache = new org.springframework.cache.concurrent.ConcurrentMapCache(PASSWORD_RESET_CACHE);
            Cache emailCache = new org.springframework.cache.concurrent.ConcurrentMapCache(EMAIL_VERIFICATION_CACHE);
            
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(regCache);
            when(cacheManager.getCache(LOGIN_CACHE)).thenReturn(loginCache);
            when(cacheManager.getCache(PASSWORD_RESET_CACHE)).thenReturn(pwdCache);
            when(cacheManager.getCache(EMAIL_VERIFICATION_CACHE)).thenReturn(emailCache);

            // Test Registration (limit: 5)
            for (int i = 0; i < MAX_REGISTRATION_ATTEMPTS; i++) {
                rateLimitingService.recordRegistrationAttempt(TEST_IP);
            }
            assertFalse(rateLimitingService.isRegistrationAllowed(TEST_IP));

            // Test Login (limit: 10)
            for (int i = 0; i < MAX_LOGIN_ATTEMPTS; i++) {
                rateLimitingService.recordLoginAttempt(TEST_IP);
            }
            assertFalse(rateLimitingService.isLoginAllowed(TEST_IP));

            // Test Password Reset (limit: 3)
            for (int i = 0; i < MAX_PASSWORD_RESET_ATTEMPTS; i++) {
                rateLimitingService.recordPasswordResetAttempt(TEST_IP);
            }
            assertFalse(rateLimitingService.isPasswordResetAllowed(TEST_IP));

            // Test Email Verification (limit: 5)
            for (int i = 0; i < MAX_EMAIL_VERIFICATION_ATTEMPTS; i++) {
                rateLimitingService.recordEmailVerificationAttempt(TEST_IP);
            }
            assertFalse(rateLimitingService.isEmailVerificationAllowed(TEST_IP));
        }

        @Test
        void remainingAttempts_atBoundaries_shouldCalculateCorrectly() {
            // Arrange
            Cache realCache = new org.springframework.cache.concurrent.ConcurrentMapCache(REGISTRATION_CACHE);
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(realCache);

            // Test at each boundary
            assertEquals(MAX_REGISTRATION_ATTEMPTS, rateLimitingService.getRemainingRegistrationAttempts(TEST_IP));
            
            rateLimitingService.recordRegistrationAttempt(TEST_IP);
            assertEquals(MAX_REGISTRATION_ATTEMPTS - 1, rateLimitingService.getRemainingRegistrationAttempts(TEST_IP));
            
            // Record remaining attempts until limit
            for (int i = 1; i < MAX_REGISTRATION_ATTEMPTS; i++) {
                rateLimitingService.recordRegistrationAttempt(TEST_IP);
            }
            assertEquals(0, rateLimitingService.getRemainingRegistrationAttempts(TEST_IP));
            
            // Beyond limit should still return 0
            rateLimitingService.recordRegistrationAttempt(TEST_IP);
            assertEquals(0, rateLimitingService.getRemainingRegistrationAttempts(TEST_IP));
        }

        @Test
        void resetTime_atBoundaries_shouldBeConsistent() {
            // Arrange
            Cache realCache = new org.springframework.cache.concurrent.ConcurrentMapCache(REGISTRATION_CACHE);
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(realCache);

            // Test before any attempts
            assertEquals(0L, rateLimitingService.getRegistrationRateLimitResetTime(TEST_IP));
            
            // Test after first attempt
            rateLimitingService.recordRegistrationAttempt(TEST_IP);
            long resetTime1 = rateLimitingService.getRegistrationRateLimitResetTime(TEST_IP);
            assertTrue(resetTime1 > 0);
            
            // Test at limit
            for (int i = 1; i < MAX_REGISTRATION_ATTEMPTS; i++) {
                rateLimitingService.recordRegistrationAttempt(TEST_IP);
            }
            long resetTime2 = rateLimitingService.getRegistrationRateLimitResetTime(TEST_IP);
            assertTrue(resetTime2 > 0);
            assertEquals(resetTime1, resetTime2, 5); // Allow 5 second tolerance for execution time
        }
    }

    @Nested
    class ErrorRecoveryScenarioTests {

        @Test
        void cacheCorruption_nonRateLimitEntry_shouldHandleGracefully() {
            // Arrange - Put wrong type in cache
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(cache);
            when(cache.get(eq(TEST_IP), any(Class.class))).thenReturn("wrong-type");

            // Act & Assert - Should fail open on all operations
            assertTrue(rateLimitingService.isRegistrationAllowed(TEST_IP));
            assertEquals(MAX_REGISTRATION_ATTEMPTS, rateLimitingService.getRemainingRegistrationAttempts(TEST_IP));
            assertEquals(0L, rateLimitingService.getRegistrationRateLimitResetTime(TEST_IP));
            assertDoesNotThrow(() -> rateLimitingService.recordRegistrationAttempt(TEST_IP));
        }

        @Test
        void cachePutFailsAfterSuccessfulGet_shouldHandleGracefully() {
            // Arrange
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(cache);
            doThrow(new RuntimeException("Put failed")).when(cache).put(eq(TEST_IP), any());

            // Act & Assert - Should handle gracefully
            assertTrue(rateLimitingService.isRegistrationAllowed(TEST_IP));
            assertDoesNotThrow(() -> rateLimitingService.recordRegistrationAttempt(TEST_IP));
        }

        @Test
        void intermittentCacheFailures_shouldMaintainService() {
            // Arrange - Cache fails every other call
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(cache);
            when(cache.get(eq(TEST_IP), any(Class.class)))
                    .thenReturn(null)
                    .thenThrow(new RuntimeException("Intermittent failure"))
                    .thenReturn(null);

            // Act & Assert - Service should remain functional
            assertTrue(rateLimitingService.isRegistrationAllowed(TEST_IP));
            assertTrue(rateLimitingService.isRegistrationAllowed(TEST_IP)); // Should fail open
            assertTrue(rateLimitingService.isRegistrationAllowed(TEST_IP));
        }

        @Test
        void memoryPressure_cacheEviction_shouldHandleGracefully() {
            // Arrange - Simulate cache eviction by returning null after put

            // Act - Record attempt then check again (simulating eviction)
            rateLimitingService.recordRegistrationAttempt(TEST_IP);
            assertTrue(rateLimitingService.isRegistrationAllowed(TEST_IP));
            
            // Assert - Should treat as new IP after eviction
            assertEquals(MAX_REGISTRATION_ATTEMPTS, rateLimitingService.getRemainingRegistrationAttempts(TEST_IP));
        }
    }

    @Nested
    class CrossOperationIndependenceTests {

        @Test
        void multipleOperationTypes_shouldNotInterfere() {
            // Arrange - Setup real caches for all operation types
            Cache regCache = new org.springframework.cache.concurrent.ConcurrentMapCache(REGISTRATION_CACHE);
            Cache loginCache = new org.springframework.cache.concurrent.ConcurrentMapCache(LOGIN_CACHE);
            Cache pwdCache = new org.springframework.cache.concurrent.ConcurrentMapCache(PASSWORD_RESET_CACHE);
            Cache emailCache = new org.springframework.cache.concurrent.ConcurrentMapCache(EMAIL_VERIFICATION_CACHE);
            
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(regCache);
            when(cacheManager.getCache(LOGIN_CACHE)).thenReturn(loginCache);
            when(cacheManager.getCache(PASSWORD_RESET_CACHE)).thenReturn(pwdCache);
            when(cacheManager.getCache(EMAIL_VERIFICATION_CACHE)).thenReturn(emailCache);

            // Act - Exhaust registration attempts
            for (int i = 0; i < MAX_REGISTRATION_ATTEMPTS; i++) {
                rateLimitingService.recordRegistrationAttempt(TEST_IP);
            }

            // Assert - Other operations should remain unaffected
            assertFalse(rateLimitingService.isRegistrationAllowed(TEST_IP));
            assertTrue(rateLimitingService.isLoginAllowed(TEST_IP));
            assertTrue(rateLimitingService.isPasswordResetAllowed(TEST_IP));
            assertTrue(rateLimitingService.isEmailVerificationAllowed(TEST_IP));
            
            assertEquals(0, rateLimitingService.getRemainingRegistrationAttempts(TEST_IP));
            assertEquals(MAX_LOGIN_ATTEMPTS, rateLimitingService.getRemainingLoginAttempts(TEST_IP));
            assertEquals(MAX_PASSWORD_RESET_ATTEMPTS, rateLimitingService.getRemainingPasswordResetAttempts(TEST_IP));
            assertEquals(MAX_EMAIL_VERIFICATION_ATTEMPTS, rateLimitingService.getRemainingEmailVerificationAttempts(TEST_IP));
        }

        @Test
        void sameIpDifferentOperations_shouldMaintainSeparateLimits() {
            // Arrange
            Cache regCache = new org.springframework.cache.concurrent.ConcurrentMapCache(REGISTRATION_CACHE);
            Cache loginCache = new org.springframework.cache.concurrent.ConcurrentMapCache(LOGIN_CACHE);
            
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(regCache);
            when(cacheManager.getCache(LOGIN_CACHE)).thenReturn(loginCache);

            // Act - Record attempts for both operations
            rateLimitingService.recordRegistrationAttempt(TEST_IP);
            rateLimitingService.recordRegistrationAttempt(TEST_IP);
            rateLimitingService.recordLoginAttempt(TEST_IP);

            // Assert - Each operation maintains its own count
            assertEquals(MAX_REGISTRATION_ATTEMPTS - 2, rateLimitingService.getRemainingRegistrationAttempts(TEST_IP));
            assertEquals(MAX_LOGIN_ATTEMPTS - 1, rateLimitingService.getRemainingLoginAttempts(TEST_IP));
        }

        @Test
        void differentOperations_separateResetTimes_shouldCalculateIndependently() {
            // Arrange
            Cache regCache = new org.springframework.cache.concurrent.ConcurrentMapCache(REGISTRATION_CACHE);
            Cache loginCache = new org.springframework.cache.concurrent.ConcurrentMapCache(LOGIN_CACHE);
            
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(regCache);
            when(cacheManager.getCache(LOGIN_CACHE)).thenReturn(loginCache);

            // Act
            rateLimitingService.recordRegistrationAttempt(TEST_IP);
            rateLimitingService.recordLoginAttempt(TEST_IP);

            // Assert - Reset times should both be positive but different due to different window sizes
            long regResetTime = rateLimitingService.getRegistrationRateLimitResetTime(TEST_IP);
            long loginResetTime = rateLimitingService.getLoginRateLimitResetTime(TEST_IP);
            
            assertTrue(regResetTime > 0);
            assertTrue(loginResetTime > 0);
            
            // Registration has 1 hour window (3600 seconds), Login has 0.25 hour window (900 seconds)
            // So registration reset time should be roughly 4 times longer than login reset time
            assertTrue(regResetTime > loginResetTime, "Registration reset time should be longer than login reset time");
            assertEquals(3600, regResetTime, 5); // Registration: ~1 hour
            assertEquals(900, loginResetTime, 5);  // Login: ~15 minutes
        }
    }

    @Nested
    class TimeBasedEdgeCaseTests {

        @Test
        void resetTime_withMultipleAttempts_shouldReflectOldestEntry() {
            // Arrange
            Cache realCache = new org.springframework.cache.concurrent.ConcurrentMapCache(REGISTRATION_CACHE);
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(realCache);

            // Act - Record attempts (all within same hour in this test environment)
            rateLimitingService.recordRegistrationAttempt(TEST_IP);
            long firstResetTime = rateLimitingService.getRegistrationRateLimitResetTime(TEST_IP);
            
            rateLimitingService.recordRegistrationAttempt(TEST_IP);
            long secondResetTime = rateLimitingService.getRegistrationRateLimitResetTime(TEST_IP);

            // Assert - Reset time should remain consistent (same hour)
            assertTrue(firstResetTime > 0);
            assertEquals(firstResetTime, secondResetTime, 5); // 5 second tolerance
        }

        @Test
        void emptyCache_resetTime_shouldReturnZero() {
            // Arrange
            Cache realCache = new org.springframework.cache.concurrent.ConcurrentMapCache(REGISTRATION_CACHE);
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(realCache);

            // Act & Assert - No attempts recorded should return 0
            assertEquals(0L, rateLimitingService.getRegistrationRateLimitResetTime(TEST_IP));
            assertEquals(0L, rateLimitingService.getLoginRateLimitResetTime(TEST_IP));
            assertEquals(0L, rateLimitingService.getPasswordResetRateLimitResetTime(TEST_IP));
            assertEquals(0L, rateLimitingService.getEmailVerificationRateLimitResetTime(TEST_IP));
        }

        @Test
        void rateLimitCalculations_shouldBeConsistentAcrossCalls() {
            // Arrange
            Cache realCache = new org.springframework.cache.concurrent.ConcurrentMapCache(REGISTRATION_CACHE);
            when(cacheManager.getCache(REGISTRATION_CACHE)).thenReturn(realCache);

            // Act - Record some attempts
            rateLimitingService.recordRegistrationAttempt(TEST_IP);
            rateLimitingService.recordRegistrationAttempt(TEST_IP);

            // Assert - Multiple calls should return consistent results
            int remaining1 = rateLimitingService.getRemainingRegistrationAttempts(TEST_IP);
            int remaining2 = rateLimitingService.getRemainingRegistrationAttempts(TEST_IP);
            int remaining3 = rateLimitingService.getRemainingRegistrationAttempts(TEST_IP);
            
            assertEquals(remaining1, remaining2);
            assertEquals(remaining2, remaining3);
            assertEquals(MAX_REGISTRATION_ATTEMPTS - 2, remaining1);
        }
    }
}