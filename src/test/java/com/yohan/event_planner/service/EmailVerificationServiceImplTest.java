package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.EmailVerificationToken;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.exception.EmailException;
import com.yohan.event_planner.exception.ErrorCode;
import com.yohan.event_planner.repository.EmailVerificationTokenRepository;
import com.yohan.event_planner.repository.UserRepository;
import com.yohan.event_planner.time.ClockProvider;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private ClockProvider clockProvider;

    private EmailVerificationServiceImpl emailVerificationService;

    private final Instant fixedTime = Instant.parse("2024-01-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        emailVerificationService = new EmailVerificationServiceImpl(
                emailVerificationTokenRepository,
                userRepository,
                emailService,
                clockProvider
        );

        // Set up test configuration - 24 hours expiry
        ReflectionTestUtils.setField(emailVerificationService, "tokenExpiryHours", 24);
    }

    private void mockClockProvider() {
        Clock fixedClock = Clock.fixed(fixedTime, ZoneOffset.UTC);
        when(clockProvider.getClockForZone(ZoneOffset.UTC)).thenReturn(fixedClock);
    }

    @Nested
    class GenerateAndSendVerificationTokenTests {

        @Test
        void generateAndSendVerificationToken_ShouldInvalidateExistingTokensAndSendEmail() {
            // Given
            mockClockProvider();
            User user = TestUtils.createTestUser("testuser");
            when(emailVerificationTokenRepository.invalidateAllTokensForUser(user)).thenReturn(2);

            // When
            emailVerificationService.generateAndSendVerificationToken(user);

            // Then
            verify(emailVerificationTokenRepository).invalidateAllTokensForUser(user);
            
            ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
            verify(emailVerificationTokenRepository).save(tokenCaptor.capture());
            
            EmailVerificationToken savedToken = tokenCaptor.getValue();
            assertNotNull(savedToken.getToken());
            assertEquals(64, savedToken.getToken().length()); // TOKEN_LENGTH
            assertEquals(user, savedToken.getUser());
            assertEquals(fixedTime, savedToken.getCreatedAt());
            assertEquals(fixedTime.plusSeconds(24 * 3600), savedToken.getExpiryDate()); // 24 hours later
            assertFalse(savedToken.isUsed());

            verify(emailService).sendEmailVerificationEmail(
                    eq(user.getEmail()),
                    eq(user.getFirstName()),
                    eq(savedToken.getToken())
            );
        }

        @Test
        void generateAndSendVerificationToken_EmailServiceThrowsException_ShouldThrowEmailException() {
            // Given
            mockClockProvider();
            User user = TestUtils.createTestUser("testuser");
            doThrow(new RuntimeException("SMTP error")).when(emailService)
                    .sendEmailVerificationEmail(anyString(), anyString(), anyString());

            // When & Then
            EmailException exception = assertThrows(EmailException.class, () ->
                    emailVerificationService.generateAndSendVerificationToken(user));
            
            assertEquals(ErrorCode.EMAIL_SEND_FAILED, exception.getErrorCode());
        }

        @Test
        void generateAndSendVerificationToken_NoExistingTokens_ShouldNotLogInvalidation() {
            // Given
            mockClockProvider();
            User user = TestUtils.createTestUser("testuser");
            when(emailVerificationTokenRepository.invalidateAllTokensForUser(user)).thenReturn(0);

            // When
            emailVerificationService.generateAndSendVerificationToken(user);

            // Then
            verify(emailVerificationTokenRepository).invalidateAllTokensForUser(user);
            verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
            verify(emailService).sendEmailVerificationEmail(anyString(), anyString(), anyString());
        }
    }

    @Nested
    class VerifyEmailTests {

        @Test
        void verifyEmail_ValidToken_ShouldMarkEmailAsVerifiedAndInvalidateToken() {
            // Given
            mockClockProvider();
            String token = "valid-token-123";
            User user = TestUtils.createTestUser("testuser");
            EmailVerificationToken verificationToken = new EmailVerificationToken(
                    token, user, fixedTime, fixedTime.plusSeconds(24 * 3600));
            
            when(emailVerificationTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.of(verificationToken));

            // When
            Optional<User> result = emailVerificationService.verifyEmail(token);

            // Then
            assertTrue(result.isPresent());
            assertEquals(user, result.get());
            assertTrue(user.isEmailVerified());
            assertTrue(verificationToken.isUsed());
            
            verify(userRepository).save(user);
            verify(emailVerificationTokenRepository).save(verificationToken);
        }

        @Test
        void verifyEmail_InvalidToken_ShouldThrowEmailException() {
            // Given
            mockClockProvider();
            String token = "invalid-token";
            when(emailVerificationTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.empty());

            // When & Then
            EmailException exception = assertThrows(EmailException.class, () ->
                    emailVerificationService.verifyEmail(token));
            
            assertEquals(ErrorCode.INVALID_VERIFICATION_TOKEN, exception.getErrorCode());
        }

        @Test
        void verifyEmail_AlreadyVerifiedUser_ShouldReturnTrueWithoutUpdating() {
            // Given
            mockClockProvider();
            String token = "valid-token-123";
            User user = TestUtils.createTestUser("testuser");
            user.verifyEmail(); // Already verified
            
            EmailVerificationToken verificationToken = new EmailVerificationToken(
                    token, user, fixedTime, fixedTime.plusSeconds(24 * 3600));
            
            when(emailVerificationTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.of(verificationToken));

            // When
            Optional<User> result = emailVerificationService.verifyEmail(token);

            // Then
            assertTrue(result.isPresent());
            assertEquals(user, result.get());
            verify(userRepository, never()).save(user);
            verify(emailVerificationTokenRepository, never()).save(verificationToken);
        }

        @Test
        void verifyEmail_UnexpectedError_ShouldThrowEmailException() {
            // Given
            mockClockProvider();
            String token = "valid-token-123";
            User user = TestUtils.createTestUser("testuser");
            EmailVerificationToken verificationToken = new EmailVerificationToken(
                    token, user, fixedTime, fixedTime.plusSeconds(24 * 3600));
            
            when(emailVerificationTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.of(verificationToken));
            when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

            // When & Then
            EmailException exception = assertThrows(EmailException.class, () ->
                    emailVerificationService.verifyEmail(token));
            
            assertEquals(ErrorCode.VERIFICATION_FAILED, exception.getErrorCode());
        }
    }

    @Nested
    class ResendVerificationEmailTests {

        @Test
        void resendVerificationEmail_UnverifiedUser_ShouldGenerateNewToken() {
            // Given
            mockClockProvider();
            User user = TestUtils.createTestUser("testuser");
            // User is not verified by default

            // When
            boolean result = emailVerificationService.resendVerificationEmail(user);

            // Then
            assertTrue(result);
            verify(emailVerificationTokenRepository).invalidateAllTokensForUser(user);
            verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
            verify(emailService).sendEmailVerificationEmail(anyString(), anyString(), anyString());
        }

        @Test
        void resendVerificationEmail_AlreadyVerifiedUser_ShouldReturnFalse() {
            // Given
            User user = TestUtils.createTestUser("testuser");
            user.verifyEmail(); // Already verified

            // When
            boolean result = emailVerificationService.resendVerificationEmail(user);

            // Then
            assertFalse(result);
            verify(emailVerificationTokenRepository, never()).invalidateAllTokensForUser(any());
            verify(emailVerificationTokenRepository, never()).save(any());
            verify(emailService, never()).sendEmailVerificationEmail(anyString(), anyString(), anyString());
        }
    }

    @Nested
    class UtilityMethodTests {

        @Test
        void isValidVerificationToken_ValidToken_ShouldReturnTrue() {
            // Given
            mockClockProvider();
            String token = "valid-token";
            when(emailVerificationTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.of(mock(EmailVerificationToken.class)));

            // When
            boolean result = emailVerificationService.isValidVerificationToken(token);

            // Then
            assertTrue(result);
        }

        @Test
        void isValidVerificationToken_InvalidToken_ShouldReturnFalse() {
            // Given
            mockClockProvider();
            String token = "invalid-token";
            when(emailVerificationTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.empty());

            // When
            boolean result = emailVerificationService.isValidVerificationToken(token);

            // Then
            assertFalse(result);
        }

        @Test
        void invalidateUserVerificationTokens_ShouldCallRepository() {
            // Given
            User user = TestUtils.createTestUser("testuser");
            when(emailVerificationTokenRepository.invalidateAllTokensForUser(user)).thenReturn(3);

            // When
            int result = emailVerificationService.invalidateUserVerificationTokens(user);

            // Then
            assertEquals(3, result);
            verify(emailVerificationTokenRepository).invalidateAllTokensForUser(user);
        }

        @Test
        void cleanupExpiredTokens_ShouldCallBothDeleteMethods() {
            // Given
            mockClockProvider();
            when(emailVerificationTokenRepository.deleteExpiredTokens(fixedTime)).thenReturn(5);
            when(emailVerificationTokenRepository.deleteUsedTokens()).thenReturn(3);

            // When
            int result = emailVerificationService.cleanupExpiredTokens();

            // Then
            assertEquals(8, result); // 5 + 3
            verify(emailVerificationTokenRepository).deleteExpiredTokens(fixedTime);
            verify(emailVerificationTokenRepository).deleteUsedTokens();
        }
    }

    @Nested
    class ConfigurationValidationTests {

        @Test
        void emailVerificationService_withZeroExpiryHours_usesDefaultExpiry() {
            // Arrange
            EmailVerificationServiceImpl testService = new EmailVerificationServiceImpl(
                    emailVerificationTokenRepository, userRepository, emailService, clockProvider);
            ReflectionTestUtils.setField(testService, "tokenExpiryHours", 0);
            
            // Simulate @PostConstruct call
            ReflectionTestUtils.invokeMethod(testService, "validateConfiguration");
            
            // Assert
            int actualExpiry = (Integer) ReflectionTestUtils.getField(testService, "tokenExpiryHours");
            assertEquals(24, actualExpiry);
        }

        @Test
        void emailVerificationService_withNegativeExpiryHours_usesDefaultExpiry() {
            // Arrange
            EmailVerificationServiceImpl testService = new EmailVerificationServiceImpl(
                    emailVerificationTokenRepository, userRepository, emailService, clockProvider);
            ReflectionTestUtils.setField(testService, "tokenExpiryHours", -5);
            
            // Simulate @PostConstruct call
            ReflectionTestUtils.invokeMethod(testService, "validateConfiguration");
            
            // Assert
            int actualExpiry = (Integer) ReflectionTestUtils.getField(testService, "tokenExpiryHours");
            assertEquals(24, actualExpiry);
        }

        @Test
        void emailVerificationService_withValidExpiryHours_keepsConfiguredValue() {
            // Arrange
            EmailVerificationServiceImpl testService = new EmailVerificationServiceImpl(
                    emailVerificationTokenRepository, userRepository, emailService, clockProvider);
            ReflectionTestUtils.setField(testService, "tokenExpiryHours", 48);
            
            // Simulate @PostConstruct call
            ReflectionTestUtils.invokeMethod(testService, "validateConfiguration");
            
            // Assert
            int actualExpiry = (Integer) ReflectionTestUtils.getField(testService, "tokenExpiryHours");
            assertEquals(48, actualExpiry);
        }
    }

    @Nested
    class TokenSecurityTests {

        @Test
        void generateSecureToken_generatesUniqueTokens() {
            // Arrange
            Set<String> tokens = new HashSet<>();
            int tokenCount = 1000;

            // Act - Generate multiple tokens
            for (int i = 0; i < tokenCount; i++) {
                String token = ReflectionTestUtils.invokeMethod(emailVerificationService, "generateSecureToken");
                assertNotNull(token);
                assertFalse(tokens.contains(token), "Generated duplicate token: " + token);
                tokens.add(token);
            }

            // Assert
            assertEquals(tokenCount, tokens.size());
        }

        @Test
        void generateSecureToken_meetsSecurityRequirements() {
            // Act
            String token = ReflectionTestUtils.invokeMethod(emailVerificationService, "generateSecureToken");

            // Assert
            assertNotNull(token);
            assertEquals(64, token.length()); // TOKEN_LENGTH
            assertTrue(token.matches("[A-Za-z0-9]+"), "Token should only contain alphanumeric characters");
        }
    }

    @Nested
    class ConcurrencyTests {

        @Test
        void generateAndSendVerificationToken_concurrentCalls_handlesCorrectly() throws InterruptedException {
            // Arrange
            mockClockProvider();
            User user = TestUtils.createTestUser("testuser");
            int threadCount = 10;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            when(emailVerificationTokenRepository.invalidateAllTokensForUser(user)).thenReturn(0);

            // Act
            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        emailVerificationService.generateAndSendVerificationToken(user);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            // Assert
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Concurrent operations should complete within 5 seconds");
            assertEquals(threadCount, successCount.get() + failureCount.get());
            // At least some operations should succeed
            assertTrue(successCount.get() > 0, "At least some concurrent operations should succeed");
        }

        @Test
        void verifyEmail_concurrentCalls_handlesCorrectly() throws InterruptedException {
            // Arrange
            mockClockProvider();
            String token = "test-token";
            User user = TestUtils.createTestUser("testuser");
            EmailVerificationToken verificationToken = new EmailVerificationToken(
                    token, user, fixedTime, fixedTime.plusSeconds(24 * 3600));
            
            when(emailVerificationTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.of(verificationToken));

            int threadCount = 5;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            // Act
            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        Optional<User> result = emailVerificationService.verifyEmail(token);
                        if (result.isPresent()) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // Expected for concurrent access to same token
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            // Assert
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Concurrent verification should complete within 5 seconds");
            // At least one verification should succeed
            assertTrue(successCount.get() >= 1, "At least one verification should succeed");
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        void generateAndSendVerificationToken_withNullUser_throwsException() {
            // Act & Assert
            assertThrows(NullPointerException.class, () -> 
                emailVerificationService.generateAndSendVerificationToken(null));
        }

        @Test
        void verifyEmail_withNullToken_throwsEmailException() {
            // Act & Assert
            mockClockProvider();
            EmailException exception = assertThrows(EmailException.class, () ->
                emailVerificationService.verifyEmail(null));
            assertEquals(ErrorCode.INVALID_VERIFICATION_TOKEN, exception.getErrorCode());
        }

        @Test
        void verifyEmail_withEmptyToken_throwsEmailException() {
            // Arrange
            mockClockProvider();
            when(emailVerificationTokenRepository.findValidToken("", fixedTime))
                .thenReturn(Optional.empty());

            // Act & Assert
            EmailException exception = assertThrows(EmailException.class, () ->
                emailVerificationService.verifyEmail(""));
            assertEquals(ErrorCode.INVALID_VERIFICATION_TOKEN, exception.getErrorCode());
        }

        @Test
        void verifyEmail_withVeryLongToken_handlesCorrectly() {
            // Arrange
            mockClockProvider();
            String longToken = "a".repeat(1000);
            when(emailVerificationTokenRepository.findValidToken(longToken, fixedTime))
                .thenReturn(Optional.empty());

            // Act & Assert
            EmailException exception = assertThrows(EmailException.class, () ->
                emailVerificationService.verifyEmail(longToken));
            assertEquals(ErrorCode.INVALID_VERIFICATION_TOKEN, exception.getErrorCode());
        }

        @Test
        void verifyEmail_withSpecialCharacters_handlesCorrectly() {
            // Arrange
            mockClockProvider();
            String specialToken = "token-with-special!@#$%^&*()chars";
            when(emailVerificationTokenRepository.findValidToken(specialToken, fixedTime))
                .thenReturn(Optional.empty());

            // Act & Assert
            EmailException exception = assertThrows(EmailException.class, () ->
                emailVerificationService.verifyEmail(specialToken));
            assertEquals(ErrorCode.INVALID_VERIFICATION_TOKEN, exception.getErrorCode());
        }

        @Test
        void isValidVerificationToken_withNullToken_returnsFalse() {
            // Arrange
            mockClockProvider();
            when(emailVerificationTokenRepository.findValidToken(null, fixedTime))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertFalse(emailVerificationService.isValidVerificationToken(null));
        }

        @Test
        void isValidVerificationToken_withEmptyToken_returnsFalse() {
            // Arrange
            mockClockProvider();
            when(emailVerificationTokenRepository.findValidToken("", fixedTime))
                .thenReturn(Optional.empty());

            // Act & Assert
            assertFalse(emailVerificationService.isValidVerificationToken(""));
        }

        @Test
        void resendVerificationEmail_withNullUser_throwsException() {
            // Act & Assert
            assertThrows(NullPointerException.class, () ->
                emailVerificationService.resendVerificationEmail(null));
        }

        @Test
        void invalidateUserVerificationTokens_withNullUser_throwsException() {
            // Act & Assert
            assertThrows(Exception.class, () ->
                emailVerificationService.invalidateUserVerificationTokens(null));
        }
    }

    @Nested
    class RepositoryErrorTests {

        @Test
        void generateAndSendVerificationToken_repositoryThrowsException_propagatesEmailException() {
            // Arrange
            User user = TestUtils.createTestUser("testuser");
            when(emailVerificationTokenRepository.invalidateAllTokensForUser(user))
                .thenThrow(new RuntimeException("Database connection failed"));

            // Act & Assert
            EmailException exception = assertThrows(EmailException.class, () ->
                emailVerificationService.generateAndSendVerificationToken(user));
            assertEquals(ErrorCode.EMAIL_SEND_FAILED, exception.getErrorCode());
        }

        @Test
        void verifyEmail_userRepositoryThrowsException_propagatesEmailException() {
            // Arrange
            mockClockProvider();
            String token = "valid-token-123";
            User user = TestUtils.createTestUser("testuser");
            EmailVerificationToken verificationToken = new EmailVerificationToken(
                    token, user, fixedTime, fixedTime.plusSeconds(24 * 3600));
            
            when(emailVerificationTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.of(verificationToken));
            when(userRepository.save(any(User.class)))
                    .thenThrow(new RuntimeException("Database save failed"));

            // Act & Assert
            EmailException exception = assertThrows(EmailException.class, () ->
                emailVerificationService.verifyEmail(token));
            assertEquals(ErrorCode.VERIFICATION_FAILED, exception.getErrorCode());
        }

        @Test
        void verifyEmail_tokenRepositoryThrowsException_propagatesEmailException() {
            // Arrange
            mockClockProvider();
            String token = "valid-token-123";
            User user = TestUtils.createTestUser("testuser");
            EmailVerificationToken verificationToken = new EmailVerificationToken(
                    token, user, fixedTime, fixedTime.plusSeconds(24 * 3600));
            
            when(emailVerificationTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.of(verificationToken));
            when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
                    .thenThrow(new RuntimeException("Token save failed"));

            // Act & Assert
            EmailException exception = assertThrows(EmailException.class, () ->
                emailVerificationService.verifyEmail(token));
            assertEquals(ErrorCode.VERIFICATION_FAILED, exception.getErrorCode());
        }

        @Test
        void cleanupExpiredTokens_repositoryThrowsException_handlesGracefully() {
            // Arrange
            mockClockProvider();
            when(emailVerificationTokenRepository.deleteExpiredTokens(any(Instant.class)))
                    .thenThrow(new RuntimeException("Database cleanup failed"));

            // Act & Assert
            assertThrows(RuntimeException.class, () ->
                emailVerificationService.cleanupExpiredTokens());
        }
    }

    @Nested
    class TimeBoundaryTests {

        @Test
        void verifyEmail_tokenExpiresAtExactMoment_handlesCorrectly() {
            // Arrange
            mockClockProvider();
            String token = "expiring-token";
            User user = TestUtils.createTestUser("testuser");
            // Token expires exactly at fixedTime
            EmailVerificationToken verificationToken = new EmailVerificationToken(
                    token, user, fixedTime.minusSeconds(24 * 3600), fixedTime);
            
            when(emailVerificationTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.empty()); // Repository should not return expired token

            // Act & Assert
            EmailException exception = assertThrows(EmailException.class, () ->
                emailVerificationService.verifyEmail(token));
            assertEquals(ErrorCode.INVALID_VERIFICATION_TOKEN, exception.getErrorCode());
        }

        @Test
        void generateAndSendVerificationToken_withMaximumExpiryHours_handlesCorrectly() {
            // Arrange
            mockClockProvider();
            User user = TestUtils.createTestUser("testuser");
            EmailVerificationServiceImpl testService = new EmailVerificationServiceImpl(
                    emailVerificationTokenRepository, userRepository, emailService, clockProvider);
            ReflectionTestUtils.setField(testService, "tokenExpiryHours", Integer.MAX_VALUE);
            
            when(emailVerificationTokenRepository.invalidateAllTokensForUser(user)).thenReturn(0);

            // Act & Assert - Service should handle large expiry gracefully
            assertDoesNotThrow(() -> testService.generateAndSendVerificationToken(user));
            
            // Verify token was created and saved
            ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
            verify(emailVerificationTokenRepository).save(tokenCaptor.capture());
            
            EmailVerificationToken savedToken = tokenCaptor.getValue();
            assertNotNull(savedToken);
            // Verify the expiry date is very far in the future (but doesn't overflow)
            assertTrue(savedToken.getExpiryDate().isAfter(fixedTime.plusSeconds(365L * 24 * 3600))); // At least 1 year in future
        }
    }

    @Nested
    class PerformanceTests {

        @Test
        void generateSecureToken_performanceTest_completesInReasonableTime() {
            // Arrange
            int tokenCount = 10000;
            long startTime = System.currentTimeMillis();

            // Act
            for (int i = 0; i < tokenCount; i++) {
                String token = ReflectionTestUtils.invokeMethod(emailVerificationService, "generateSecureToken");
                assertNotNull(token);
            }

            // Assert
            long duration = System.currentTimeMillis() - startTime;
            assertTrue(duration < 5000, "Generating " + tokenCount + " tokens should complete within 5 seconds, took: " + duration + "ms");
        }

        @Test
        void cleanupExpiredTokens_withLargeDataset_completesInReasonableTime() {
            // Arrange
            mockClockProvider();
            when(emailVerificationTokenRepository.deleteExpiredTokens(any(Instant.class)))
                    .thenReturn(50000); // Simulate large cleanup
            when(emailVerificationTokenRepository.deleteUsedTokens())
                    .thenReturn(30000);

            long startTime = System.currentTimeMillis();

            // Act
            int result = emailVerificationService.cleanupExpiredTokens();

            // Assert
            long duration = System.currentTimeMillis() - startTime;
            assertEquals(80000, result);
            assertTrue(duration < 1000, "Cleanup should complete within 1 second, took: " + duration + "ms");
        }
    }

    @Nested
    class PostConstructTests {

        @Test
        void validateConfiguration_manuallyInvoked_worksCorrectly() {
            // Arrange
            EmailVerificationServiceImpl testService = new EmailVerificationServiceImpl(
                    emailVerificationTokenRepository, userRepository, emailService, clockProvider);
            
            // Set a valid configuration value
            ReflectionTestUtils.setField(testService, "tokenExpiryHours", 48);
            
            // Act - Manually invoke @PostConstruct method (since Spring container isn't managing this)
            ReflectionTestUtils.invokeMethod(testService, "validateConfiguration");
            
            // Assert - Configuration should remain unchanged for valid values
            int actualExpiry = (Integer) ReflectionTestUtils.getField(testService, "tokenExpiryHours");
            assertEquals(48, actualExpiry);
        }

        @Test
        void validateConfiguration_withDefaultSpringValue_usesConfiguredDefault() {
            // Arrange
            EmailVerificationServiceImpl testService = new EmailVerificationServiceImpl(
                    emailVerificationTokenRepository, userRepository, emailService, clockProvider);
            
            // Spring's @Value annotation would set this to 24 (the default), but we need to simulate it
            ReflectionTestUtils.setField(testService, "tokenExpiryHours", 24);
            
            // Act
            ReflectionTestUtils.invokeMethod(testService, "validateConfiguration");
            
            // Assert
            int actualExpiry = (Integer) ReflectionTestUtils.getField(testService, "tokenExpiryHours");
            assertEquals(24, actualExpiry);
        }
    }

    @Nested
    class IntegrationStyleTests {

        @Test
        void completeVerificationWorkflow_endToEnd_worksCorrectly() {
            // Arrange
            mockClockProvider();
            User user = TestUtils.createTestUser("integrationuser");
            when(emailVerificationTokenRepository.invalidateAllTokensForUser(user)).thenReturn(1);
            
            // Capture the generated token
            ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
            
            // Step 1: Generate and send verification token
            emailVerificationService.generateAndSendVerificationToken(user);
            
            verify(emailVerificationTokenRepository).save(tokenCaptor.capture());
            EmailVerificationToken generatedToken = tokenCaptor.getValue();
            String tokenValue = generatedToken.getToken();
            
            // Step 2: Verify the token can be validated
            when(emailVerificationTokenRepository.findValidToken(tokenValue, fixedTime))
                    .thenReturn(Optional.of(generatedToken));
            
            assertTrue(emailVerificationService.isValidVerificationToken(tokenValue));
            
            // Step 3: Use token for verification
            Optional<User> verifiedUser = emailVerificationService.verifyEmail(tokenValue);
            
            // Assert complete workflow
            assertTrue(verifiedUser.isPresent());
            assertEquals(user, verifiedUser.get());
            assertTrue(user.isEmailVerified());
            assertTrue(generatedToken.isUsed());
            
            // Step 4: Verify token can't be reused
            when(emailVerificationTokenRepository.findValidToken(tokenValue, fixedTime))
                    .thenReturn(Optional.empty()); // Used tokens should not be found
            
            assertFalse(emailVerificationService.isValidVerificationToken(tokenValue));
        }

        @Test
        void resendWorkflow_replacesPreviousToken_worksCorrectly() {
            // Arrange
            mockClockProvider();
            User user = TestUtils.createTestUser("resenduser");
            
            // First send
            when(emailVerificationTokenRepository.invalidateAllTokensForUser(user)).thenReturn(0);
            emailVerificationService.generateAndSendVerificationToken(user);
            
            // Resend - should invalidate previous tokens
            when(emailVerificationTokenRepository.invalidateAllTokensForUser(user)).thenReturn(1);
            boolean resendResult = emailVerificationService.resendVerificationEmail(user);
            
            // Assert
            assertTrue(resendResult);
            verify(emailVerificationTokenRepository, times(2)).invalidateAllTokensForUser(user);
            verify(emailVerificationTokenRepository, times(2)).save(any(EmailVerificationToken.class));
            verify(emailService, times(2)).sendEmailVerificationEmail(anyString(), anyString(), anyString());
        }
    }

    @Nested
    class ExceptionHandlingTests {

        @Test
        void handleVerificationException_withEmailException_rethrowsOriginal() {
            // Arrange
            mockClockProvider();
            User user = TestUtils.createTestUser("testuser");
            EmailException originalException = new EmailException(ErrorCode.EMAIL_SEND_FAILED, "test@example.com");
            doThrow(originalException).when(emailService)
                    .sendEmailVerificationEmail(anyString(), anyString(), anyString());

            // Act & Assert
            EmailException thrown = assertThrows(EmailException.class, () ->
                    emailVerificationService.generateAndSendVerificationToken(user));
            
            assertEquals(ErrorCode.EMAIL_SEND_FAILED, thrown.getErrorCode());
            assertEquals(originalException, thrown);
        }

        @Test
        void handleVerificationException_withRuntimeException_wrapsInEmailException() {
            // Arrange
            mockClockProvider();
            User user = TestUtils.createTestUser("testuser");
            RuntimeException originalException = new RuntimeException("Database connection failed");
            doThrow(originalException).when(emailService)
                    .sendEmailVerificationEmail(anyString(), anyString(), anyString());

            // Act & Assert
            EmailException thrown = assertThrows(EmailException.class, () ->
                    emailVerificationService.generateAndSendVerificationToken(user));
            
            assertEquals(ErrorCode.EMAIL_SEND_FAILED, thrown.getErrorCode());
            assertEquals("Failed to send email verification email", thrown.getMessage());
        }
    }
}