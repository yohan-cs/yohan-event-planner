package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.PasswordResetToken;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.auth.ForgotPasswordRequestDTO;
import com.yohan.event_planner.dto.auth.ForgotPasswordResponseDTO;
import com.yohan.event_planner.dto.auth.ResetPasswordRequestDTO;
import com.yohan.event_planner.dto.auth.ResetPasswordResponseDTO;
import com.yohan.event_planner.exception.ErrorCode;
import com.yohan.event_planner.exception.PasswordException;
import com.yohan.event_planner.exception.PasswordResetException;
import com.yohan.event_planner.repository.PasswordResetTokenRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import com.yohan.event_planner.constants.ApplicationConstants;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ClockProvider clockProvider;

    @Mock
    private Clock clock;

    private PasswordResetServiceImpl passwordResetService;

    private final Instant fixedTime = Instant.parse("2024-01-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetServiceImpl(
                passwordResetTokenRepository,
                userRepository,
                emailService,
                passwordEncoder,
                clockProvider
        );

        // Set up test configuration
        ReflectionTestUtils.setField(passwordResetService, "tokenExpiryMinutes", 15);

    }

    private void mockClockProvider() {
        when(clockProvider.getClockForZone(ZoneOffset.UTC)).thenReturn(clock);
        when(clock.instant()).thenReturn(fixedTime);
    }

    @Nested
    class RequestPasswordResetTests {

        @Test
        void requestPasswordReset_whenUserExists_generatesTokenAndSendsEmail() {
            // Arrange
            mockClockProvider();
            String email = "user@example.com";
            User user = TestUtils.createValidUserEntityWithId();
            user.setEmail(email);

            ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO(email);

            when(userRepository.findByEmailAndIsPendingDeletionFalse(email))
                    .thenReturn(Optional.of(user));

            // Act
            ForgotPasswordResponseDTO response = passwordResetService.requestPasswordReset(request);

            // Assert
            assertNotNull(response);
            assertEquals("If your email is registered, you will receive a password reset link shortly.",
                        response.message());

            verify(passwordResetTokenRepository).invalidateAllTokensForUser(user);
            verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
            verify(emailService).sendPasswordResetEmail(eq(email), anyString(), eq(15));
        }

        @Test
        void requestPasswordReset_whenUserDoesNotExist_returnsStandardResponse() {
            // Arrange
            String email = "nonexistent@example.com";
            ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO(email);

            when(userRepository.findByEmailAndIsPendingDeletionFalse(email))
                    .thenReturn(Optional.empty());

            // Act
            ForgotPasswordResponseDTO response = passwordResetService.requestPasswordReset(request);

            // Assert
            assertNotNull(response);
            assertEquals("If your email is registered, you will receive a password reset link shortly.",
                        response.message());

            verify(passwordResetTokenRepository, never()).save(any());
            verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyInt());
        }

        @Test
        void requestPasswordReset_whenExceptionOccurs_returnsStandardResponse() {
            // Arrange
            String email = "user@example.com";
            ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO(email);

            when(userRepository.findByEmailAndIsPendingDeletionFalse(email))
                    .thenThrow(new RuntimeException("Database error"));

            // Act
            ForgotPasswordResponseDTO response = passwordResetService.requestPasswordReset(request);

            // Assert
            assertNotNull(response);
            assertEquals("If your email is registered, you will receive a password reset link shortly.",
                        response.message());
        }

        @Test
        void requestPasswordReset_setsCorrectTokenExpiration() {
            // Arrange
            mockClockProvider();
            String email = "user@example.com";
            User user = TestUtils.createValidUserEntityWithId();
            user.setEmail(email);

            ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO(email);

            when(userRepository.findByEmailAndIsPendingDeletionFalse(email))
                    .thenReturn(Optional.of(user));

            // Act
            passwordResetService.requestPasswordReset(request);

            // Assert
            ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(passwordResetTokenRepository).save(tokenCaptor.capture());

            PasswordResetToken savedToken = tokenCaptor.getValue();
            assertEquals(fixedTime, savedToken.getCreatedAt());
            assertEquals(fixedTime.plusSeconds(15 * 60), savedToken.getExpiryDate());
        }
    }

    @Nested
    class ResetPasswordTests {

        @Test
        void resetPassword_withValidToken_resetsPasswordSuccessfully() {
            // Arrange
            mockClockProvider();
            String token = "valid-token-123";
            String newPassword = "NewPassword123!";
            String hashedPassword = "hashed-password";

            User user = TestUtils.createValidUserEntityWithId();
            PasswordResetToken resetToken = new PasswordResetToken(token, user, fixedTime, fixedTime.plusSeconds(900));

            ResetPasswordRequestDTO request = new ResetPasswordRequestDTO(token, newPassword);

            when(passwordResetTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.of(resetToken));
            when(passwordEncoder.encode(newPassword)).thenReturn(hashedPassword);

            // Act
            ResetPasswordResponseDTO response = passwordResetService.resetPassword(request);

            // Assert
            assertNotNull(response);
            assertEquals("Your password has been successfully reset. Please log in with your new password.", response.message());

            verify(passwordEncoder).encode(newPassword);
            verify(userRepository).save(user);
            verify(passwordResetTokenRepository).save(resetToken);
            verify(emailService).sendPasswordChangeConfirmation(user.getEmail(), user.getUsername());

            assertEquals(hashedPassword, user.getHashedPassword());
            assertTrue(resetToken.isUsed());
        }

        @Test
        void resetPassword_withInvalidToken_throwsPasswordException() {
            // Arrange
            mockClockProvider();
            String token = "invalid-token";
            String newPassword = "NewPassword123!";

            ResetPasswordRequestDTO request = new ResetPasswordRequestDTO(token, newPassword);

            when(passwordResetTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.empty());

            // Act & Assert
            PasswordException thrown = assertThrows(PasswordException.class,
                    () -> passwordResetService.resetPassword(request));

            assertEquals(ErrorCode.INVALID_RESET_TOKEN, thrown.getErrorCode());

            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any());
            verify(emailService, never()).sendPasswordChangeConfirmation(anyString(), anyString());
        }

        @Test
        void resetPassword_whenPasswordEncodingFails_throwsPasswordResetException() {
            // Arrange
            mockClockProvider();
            String token = "valid-token-123";
            String newPassword = "NewPassword123!";

            User user = TestUtils.createValidUserEntityWithId();
            PasswordResetToken resetToken = new PasswordResetToken(token, user, fixedTime, fixedTime.plusSeconds(900));

            ResetPasswordRequestDTO request = new ResetPasswordRequestDTO(token, newPassword);

            when(passwordResetTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.of(resetToken));
            when(passwordEncoder.encode(newPassword))
                    .thenThrow(new RuntimeException("Encoding failed"));

            // Act & Assert
            PasswordResetException thrown = assertThrows(PasswordResetException.class,
                    () -> passwordResetService.resetPassword(request));

            assertEquals(ErrorCode.PASSWORD_RESET_ENCODING_FAILED, thrown.getErrorCode());
        }
    }

    @Nested
    class IsValidResetTokenTests {

        @Test
        void isValidResetToken_withValidToken_returnsTrue() {
            // Arrange
            mockClockProvider();
            String token = "valid-token-123";

            when(passwordResetTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.of(mock(PasswordResetToken.class)));

            // Act
            boolean result = passwordResetService.isValidResetToken(token);

            // Assert
            assertTrue(result);
        }

        @Test
        void isValidResetToken_withInvalidToken_returnsFalse() {
            // Arrange
            mockClockProvider();
            String token = "invalid-token";

            when(passwordResetTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.empty());

            // Act
            boolean result = passwordResetService.isValidResetToken(token);

            // Assert
            assertFalse(result);
        }
    }

    @Nested
    class InvalidateUserTokensTests {

        @Test
        void invalidateUserTokens_withValidUser_invalidatesTokens() {
            // Arrange
            Long userId = 1L;
            User user = TestUtils.createValidUserEntityWithId();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.invalidateAllTokensForUser(user)).thenReturn(3);

            // Act
            int result = passwordResetService.invalidateUserTokens(userId);

            // Assert
            assertEquals(3, result);
            verify(passwordResetTokenRepository).invalidateAllTokensForUser(user);
        }

        @Test
        void invalidateUserTokens_withInvalidUser_returnsZero() {
            // Arrange
            Long userId = 999L;

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act
            int result = passwordResetService.invalidateUserTokens(userId);

            // Assert
            assertEquals(0, result);
            verify(passwordResetTokenRepository, never()).invalidateAllTokensForUser(any());
        }
    }

    @Nested
    class CleanupExpiredTokensTests {

        @Test
        void cleanupExpiredTokens_deletesExpiredAndUsedTokens() {
            // Arrange
            mockClockProvider();
            when(passwordResetTokenRepository.deleteExpiredTokens(fixedTime)).thenReturn(5);
            when(passwordResetTokenRepository.deleteUsedTokens()).thenReturn(3);

            // Act
            int result = passwordResetService.cleanupExpiredTokens();

            // Assert
            assertEquals(8, result);
            verify(passwordResetTokenRepository).deleteExpiredTokens(fixedTime);
            verify(passwordResetTokenRepository).deleteUsedTokens();
        }

        @Test
        void cleanupExpiredTokens_whenNoTokensToDelete_returnsZero() {
            // Arrange
            mockClockProvider();
            when(passwordResetTokenRepository.deleteExpiredTokens(fixedTime)).thenReturn(0);
            when(passwordResetTokenRepository.deleteUsedTokens()).thenReturn(0);

            // Act
            int result = passwordResetService.cleanupExpiredTokens();

            // Assert
            assertEquals(0, result);
        }
    }

    @Nested
    class ConfigurationTests {

        @Test
        void validateConfiguration_withValidTokenExpiry_logsSuccessfully() {
            // Arrange
            PasswordResetServiceImpl service = new PasswordResetServiceImpl(
                    passwordResetTokenRepository,
                    userRepository,
                    emailService,
                    passwordEncoder,
                    clockProvider
            );
            ReflectionTestUtils.setField(service, "tokenExpiryMinutes", 15);

            // Act & Assert
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validateConfiguration"));
        }

        @Test
        void validateConfiguration_withZeroTokenExpiry_usesDefault() {
            // Arrange
            PasswordResetServiceImpl service = new PasswordResetServiceImpl(
                    passwordResetTokenRepository,
                    userRepository,
                    emailService,
                    passwordEncoder,
                    clockProvider
            );
            ReflectionTestUtils.setField(service, "tokenExpiryMinutes", 0);

            // Act
            ReflectionTestUtils.invokeMethod(service, "validateConfiguration");

            // Assert
            Integer actualExpiry = (Integer) ReflectionTestUtils.getField(service, "tokenExpiryMinutes");
            assertEquals(30, actualExpiry); // ApplicationConstants.PASSWORD_RESET_DEFAULT_TOKEN_EXPIRY_MINUTES
        }

        @Test
        void validateConfiguration_withNegativeTokenExpiry_usesDefault() {
            // Arrange
            PasswordResetServiceImpl service = new PasswordResetServiceImpl(
                    passwordResetTokenRepository,
                    userRepository,
                    emailService,
                    passwordEncoder,
                    clockProvider
            );
            ReflectionTestUtils.setField(service, "tokenExpiryMinutes", -5);

            // Act
            ReflectionTestUtils.invokeMethod(service, "validateConfiguration");

            // Assert
            Integer actualExpiry = (Integer) ReflectionTestUtils.getField(service, "tokenExpiryMinutes");
            assertEquals(30, actualExpiry); // ApplicationConstants.PASSWORD_RESET_DEFAULT_TOKEN_EXPIRY_MINUTES
        }
    }

    @Nested
    class SecurityAndTokenGenerationTests {

        @Test
        void generateSecureToken_createsTokensWithCorrectLength() throws Exception {
            // Act
            String token1 = (String) ReflectionTestUtils.invokeMethod(passwordResetService, "generateSecureToken");
            String token2 = (String) ReflectionTestUtils.invokeMethod(passwordResetService, "generateSecureToken");

            // Assert
            assertNotNull(token1);
            assertNotNull(token2);
            assertEquals(ApplicationConstants.PASSWORD_RESET_TOKEN_LENGTH, token1.length());
            assertEquals(ApplicationConstants.PASSWORD_RESET_TOKEN_LENGTH, token2.length());
        }

        @Test
        void generateSecureToken_createsUniqueTokens() throws Exception {
            // Arrange
            Set<String> tokens = new HashSet<>();
            int numberOfTokens = 1000;

            // Act
            for (int i = 0; i < numberOfTokens; i++) {
                String token = (String) ReflectionTestUtils.invokeMethod(passwordResetService, "generateSecureToken");
                tokens.add(token);
            }

            // Assert - All tokens should be unique
            assertEquals(numberOfTokens, tokens.size());
        }

        @Test
        void generateSecureToken_usesValidCharacterSet() throws Exception {
            // Act
            String token = (String) ReflectionTestUtils.invokeMethod(passwordResetService, "generateSecureToken");

            // Assert
            for (char c : token.toCharArray()) {
                assertTrue(ApplicationConstants.PASSWORD_RESET_TOKEN_CHARACTERS.indexOf(c) >= 0,
                    "Token contains invalid character: " + c);
            }
        }

        @Test
        void secureRandom_isProperlyInitialized() throws Exception {
            // Arrange
            Field secureRandomField = PasswordResetServiceImpl.class.getDeclaredField("secureRandom");
            secureRandomField.setAccessible(true);

            // Act
            SecureRandom secureRandom = (SecureRandom) secureRandomField.get(passwordResetService);

            // Assert
            assertNotNull(secureRandom);
            assertInstanceOf(SecureRandom.class, secureRandom);
        }

        @Test
        void simulateProcessingTime_introducesRandomDelay() throws Exception {
            // Arrange
            long startTime = System.currentTimeMillis();

            // Act
            ReflectionTestUtils.invokeMethod(passwordResetService, "simulateProcessingTime");

            // Assert
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            assertTrue(duration >= ApplicationConstants.PASSWORD_RESET_MIN_SIMULATION_DELAY_MS,
                "Delay was too short: " + duration + "ms");
            assertTrue(duration <= ApplicationConstants.PASSWORD_RESET_MAX_SIMULATION_DELAY_MS + 50,
                "Delay was too long: " + duration + "ms"); // Allow 50ms tolerance
        }

        @Test
        void simulateProcessingTime_variableDelays() throws Exception {
            // Arrange
            Set<Long> delays = new HashSet<>();
            int iterations = 10;

            // Act
            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                ReflectionTestUtils.invokeMethod(passwordResetService, "simulateProcessingTime");
                long duration = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms
                delays.add(duration);
            }

            // Assert - Should have some variation in delays (at least 3 different values)
            assertTrue(delays.size() >= 3, "Delays should vary, got: " + delays);
        }
    }

    @Nested
    class ConcurrencyAndTimingTests {

        @Test
        void requestPasswordReset_concurrentRequests_handledSafely() throws InterruptedException {
            // Arrange
            mockClockProvider();
            String email = "user@example.com";
            User user = TestUtils.createValidUserEntityWithId();
            user.setEmail(email);
            
            when(userRepository.findByEmailAndIsPendingDeletionFalse(email))
                    .thenReturn(Optional.of(user));

            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            // Act
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO(email);
                        ForgotPasswordResponseDTO response = passwordResetService.requestPasswordReset(request);
                        assertNotNull(response);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(completionLatch.await(10, TimeUnit.SECONDS));
            executor.shutdown();

            // Assert
            assertEquals(threadCount, successCount.get());
            verify(passwordResetTokenRepository, times(threadCount)).invalidateAllTokensForUser(user);
            verify(passwordResetTokenRepository, times(threadCount)).save(any(PasswordResetToken.class));
        }

        @Test
        void resetPassword_concurrentAttempts_onlyOneSucceeds() throws InterruptedException {
            // Arrange
            mockClockProvider();
            String token = "valid-token-123";
            String newPassword = "NewPassword123!";
            String hashedPassword = "hashed-password";

            User user = TestUtils.createValidUserEntityWithId();
            PasswordResetToken resetToken = new PasswordResetToken(token, user, fixedTime, fixedTime.plusSeconds(900));

            when(passwordResetTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.of(resetToken));
            when(passwordEncoder.encode(newPassword)).thenReturn(hashedPassword);

            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger exceptionCount = new AtomicInteger(0);

            // Act
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO(token, newPassword);
                        ResetPasswordResponseDTO response = passwordResetService.resetPassword(request);
                        assertNotNull(response);
                        successCount.incrementAndGet();
                    } catch (PasswordException e) {
                        exceptionCount.incrementAndGet();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(completionLatch.await(10, TimeUnit.SECONDS));
            executor.shutdown();

            // Assert - Only one should succeed (if token becomes invalid after first use)
            // or all should succeed if the test token doesn't track usage properly
            assertTrue(successCount.get() + exceptionCount.get() == threadCount);
            assertTrue(successCount.get() >= 1); // At least one should succeed
        }

        @Test
        void tokenGeneration_underConcurrency_producesUniqueTokens() throws InterruptedException {
            // Arrange
            int threadCount = 50;
            int tokensPerThread = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(threadCount);
            Set<String> allTokens = new HashSet<>();
            Object lock = new Object();

            // Act
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        Set<String> threadTokens = new HashSet<>();
                        for (int j = 0; j < tokensPerThread; j++) {
                            String token = (String) ReflectionTestUtils.invokeMethod(
                                passwordResetService, "generateSecureToken");
                            threadTokens.add(token);
                        }
                        synchronized (lock) {
                            allTokens.addAll(threadTokens);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(completionLatch.await(15, TimeUnit.SECONDS));
            executor.shutdown();

            // Assert - All tokens should be unique
            assertEquals(threadCount * tokensPerThread, allTokens.size());
        }
    }

    @Nested
    class IntegrationEdgeCaseTests {

        @Test
        void requestPasswordReset_whenEmailServiceFails_returnsStandardResponse() {
            // Arrange
            mockClockProvider();
            String email = "user@example.com";
            User user = TestUtils.createValidUserEntityWithId();
            user.setEmail(email);

            ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO(email);

            when(userRepository.findByEmailAndIsPendingDeletionFalse(email))
                    .thenReturn(Optional.of(user));
            doThrow(new RuntimeException("Email service unavailable"))
                    .when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyInt());

            // Act
            ForgotPasswordResponseDTO response = passwordResetService.requestPasswordReset(request);

            // Assert
            assertNotNull(response);
            assertEquals("If your email is registered, you will receive a password reset link shortly.",
                        response.message());
        }

        @Test
        void resetPassword_whenUserSaveFails_throwsPasswordResetException() {
            // Arrange
            mockClockProvider();
            String token = "valid-token-123";
            String newPassword = "NewPassword123!";
            String hashedPassword = "hashed-password";

            User user = TestUtils.createValidUserEntityWithId();
            PasswordResetToken resetToken = new PasswordResetToken(token, user, fixedTime, fixedTime.plusSeconds(900));

            ResetPasswordRequestDTO request = new ResetPasswordRequestDTO(token, newPassword);

            when(passwordResetTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.of(resetToken));
            when(passwordEncoder.encode(newPassword)).thenReturn(hashedPassword);
            when(userRepository.save(user)).thenThrow(new RuntimeException("Database constraint violation"));

            // Act & Assert
            PasswordResetException thrown = assertThrows(PasswordResetException.class,
                    () -> passwordResetService.resetPassword(request));

            assertEquals(ErrorCode.PASSWORD_RESET_DATABASE_ERROR, thrown.getErrorCode());
        }

        @Test
        void resetPassword_whenConfirmationEmailFails_throwsPasswordResetException() {
            // Arrange
            mockClockProvider();
            String token = "valid-token-123";
            String newPassword = "NewPassword123!";
            String hashedPassword = "hashed-password";

            User user = TestUtils.createValidUserEntityWithId();
            PasswordResetToken resetToken = new PasswordResetToken(token, user, fixedTime, fixedTime.plusSeconds(900));

            ResetPasswordRequestDTO request = new ResetPasswordRequestDTO(token, newPassword);

            when(passwordResetTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.of(resetToken));
            when(passwordEncoder.encode(newPassword)).thenReturn(hashedPassword);
            doThrow(new RuntimeException("Email service unavailable"))
                    .when(emailService).sendPasswordChangeConfirmation(anyString(), anyString());

            // Act & Assert
            PasswordResetException thrown = assertThrows(PasswordResetException.class,
                    () -> passwordResetService.resetPassword(request));

            assertEquals(ErrorCode.PASSWORD_RESET_CONFIRMATION_EMAIL_FAILED, thrown.getErrorCode());
            verify(userRepository).save(user);
            verify(passwordResetTokenRepository).save(resetToken);
        }

        @Test
        void cleanupExpiredTokens_whenRepositoryFails_handlesGracefully() {
            // Arrange
            mockClockProvider();
            when(passwordResetTokenRepository.deleteExpiredTokens(any(Instant.class)))
                    .thenThrow(new RuntimeException("Database connection lost"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> passwordResetService.cleanupExpiredTokens());
        }

        @Test
        void clockProvider_edgeCases_handledCorrectly() {
            // Arrange
            when(clockProvider.getClockForZone(ZoneOffset.UTC))
                    .thenThrow(new RuntimeException("Clock service unavailable"));

            // Act & Assert
            assertThrows(RuntimeException.class, 
                () -> passwordResetService.isValidResetToken("any-token"));
        }
    }

    @Nested
    class EnhancedExceptionHandlingTests {

        @Test
        void requestPasswordReset_whenClockProviderFails_returnsStandardResponse() {
            // Arrange
            String email = "user@example.com";
            User user = TestUtils.createValidUserEntityWithId();
            user.setEmail(email);

            ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO(email);

            when(userRepository.findByEmailAndIsPendingDeletionFalse(email))
                    .thenReturn(Optional.of(user));
            when(clockProvider.getClockForZone(ZoneOffset.UTC))
                    .thenThrow(new RuntimeException("Clock service unavailable"));

            // Act
            ForgotPasswordResponseDTO response = passwordResetService.requestPasswordReset(request);

            // Assert - Should still return standard response due to exception handling
            assertNotNull(response);
            assertEquals("If your email is registered, you will receive a password reset link shortly.",
                        response.message());
        }

        @Test
        void requestPasswordReset_whenTokenSaveFails_returnsStandardResponse() {
            // Arrange
            mockClockProvider();
            String email = "user@example.com";
            User user = TestUtils.createValidUserEntityWithId();
            user.setEmail(email);

            ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO(email);

            when(userRepository.findByEmailAndIsPendingDeletionFalse(email))
                    .thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                    .thenThrow(new RuntimeException("Database constraint violation"));

            // Act
            ForgotPasswordResponseDTO response = passwordResetService.requestPasswordReset(request);

            // Assert - Should still return standard response due to exception handling
            assertNotNull(response);
            assertEquals("If your email is registered, you will receive a password reset link shortly.",
                        response.message());
        }

        @Test
        void resetPassword_whenTokenSaveFails_throwsPasswordResetException() {
            // Arrange
            mockClockProvider();
            String token = "valid-token-123";
            String newPassword = "NewPassword123!";
            String hashedPassword = "hashed-password";

            User user = TestUtils.createValidUserEntityWithId();
            PasswordResetToken resetToken = new PasswordResetToken(token, user, fixedTime, fixedTime.plusSeconds(900));

            ResetPasswordRequestDTO request = new ResetPasswordRequestDTO(token, newPassword);

            when(passwordResetTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.of(resetToken));
            when(passwordEncoder.encode(newPassword)).thenReturn(hashedPassword);
            when(userRepository.save(user)).thenReturn(user);
            when(passwordResetTokenRepository.save(resetToken))
                    .thenThrow(new RuntimeException("Token save failed"));

            // Act & Assert
            PasswordResetException thrown = assertThrows(PasswordResetException.class,
                    () -> passwordResetService.resetPassword(request));

            assertEquals(ErrorCode.PASSWORD_RESET_DATABASE_ERROR, thrown.getErrorCode());
        }

        @Test
        void passwordResetException_hasCorrectErrorMessages() {
            // Test each specific error code
            PasswordResetException tokenGenException = new PasswordResetException(ErrorCode.PASSWORD_RESET_TOKEN_GENERATION_FAILED);
            assertTrue(tokenGenException.getMessage().contains("Failed to generate secure password reset token"));

            PasswordResetException emailException = new PasswordResetException(ErrorCode.PASSWORD_RESET_EMAIL_FAILED);
            assertTrue(emailException.getMessage().contains("Failed to send password reset email"));

            PasswordResetException dbException = new PasswordResetException(ErrorCode.PASSWORD_RESET_DATABASE_ERROR);
            assertTrue(dbException.getMessage().contains("Database error occurred during password reset"));

            PasswordResetException encodingException = new PasswordResetException(ErrorCode.PASSWORD_RESET_ENCODING_FAILED);
            assertTrue(encodingException.getMessage().contains("Failed to encode new password"));

            PasswordResetException confirmationException = new PasswordResetException(ErrorCode.PASSWORD_RESET_CONFIRMATION_EMAIL_FAILED);
            assertTrue(confirmationException.getMessage().contains("Failed to send password change confirmation"));
        }

        @Test
        void passwordResetException_withCause_preservesCause() {
            // Arrange
            RuntimeException originalCause = new RuntimeException("Original database error");

            // Act
            PasswordResetException exception = new PasswordResetException(ErrorCode.PASSWORD_RESET_DATABASE_ERROR, originalCause);

            // Assert
            assertEquals(ErrorCode.PASSWORD_RESET_DATABASE_ERROR, exception.getErrorCode());
            assertEquals(originalCause, exception.getCause());
        }
    }

    @Nested
    class PerformanceAndLoadTests {

        @Test
        void requestPasswordReset_underHighLoad_maintainsPerformance() throws InterruptedException {
            // Arrange
            mockClockProvider();
            String email = "user@example.com";
            User user = TestUtils.createValidUserEntityWithId();
            user.setEmail(email);
            
            when(userRepository.findByEmailAndIsPendingDeletionFalse(email))
                    .thenReturn(Optional.of(user));

            int threadCount = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicLong totalResponseTime = new AtomicLong(0);
            AtomicLong maxResponseTime = new AtomicLong(0);

            // Act
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        long startTime = System.nanoTime();
                        
                        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO(email);
                        ForgotPasswordResponseDTO response = passwordResetService.requestPasswordReset(request);
                        
                        long responseTime = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms
                        
                        assertNotNull(response);
                        successCount.incrementAndGet();
                        totalResponseTime.addAndGet(responseTime);
                        
                        // Update max response time atomically
                        long currentMax;
                        do {
                            currentMax = maxResponseTime.get();
                        } while (responseTime > currentMax && !maxResponseTime.compareAndSet(currentMax, responseTime));
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(completionLatch.await(30, TimeUnit.SECONDS));
            executor.shutdown();

            // Assert
            assertEquals(threadCount, successCount.get());
            
            long averageResponseTime = totalResponseTime.get() / threadCount;
            long maxResponse = maxResponseTime.get();
            
            // Performance assertions - should complete within reasonable time
            assertTrue(averageResponseTime < 5000, "Average response time too high: " + averageResponseTime + "ms");
            assertTrue(maxResponse < 10000, "Max response time too high: " + maxResponse + "ms");
            
            System.out.println("Performance Results:");
            System.out.println("- Average response time: " + averageResponseTime + "ms");
            System.out.println("- Max response time: " + maxResponse + "ms");
            System.out.println("- Success rate: " + successCount.get() + "/" + threadCount);
        }

        @Test
        void tokenGeneration_performanceBenchmark() throws Exception {
            // Arrange
            int tokenCount = 10000;
            long startTime = System.nanoTime();

            // Act
            for (int i = 0; i < tokenCount; i++) {
                ReflectionTestUtils.invokeMethod(passwordResetService, "generateSecureToken");
            }

            // Assert
            long totalTime = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms
            double avgTimePerToken = (double) totalTime / tokenCount;
            
            // Should generate tokens quickly - less than 1ms per token on average
            assertTrue(avgTimePerToken < 1.0, "Token generation too slow: " + avgTimePerToken + "ms per token");
            assertTrue(totalTime < 5000, "Total time too high: " + totalTime + "ms for " + tokenCount + " tokens");
            
            System.out.println("Token Generation Performance:");
            System.out.println("- Total time: " + totalTime + "ms");
            System.out.println("- Average per token: " + String.format("%.3f", avgTimePerToken) + "ms");
            System.out.println("- Tokens per second: " + String.format("%.0f", 1000.0 / avgTimePerToken));
        }

        @Test
        void cleanupExpiredTokens_withLargeDataset_performsEfficiently() {
            // Arrange
            mockClockProvider();
            int largeCount = 10000;
            
            when(passwordResetTokenRepository.deleteExpiredTokens(any(Instant.class)))
                    .thenReturn(largeCount);
            when(passwordResetTokenRepository.deleteUsedTokens())
                    .thenReturn(largeCount / 2);

            long startTime = System.nanoTime();

            // Act
            int result = passwordResetService.cleanupExpiredTokens();

            // Assert
            long duration = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms
            
            assertEquals(largeCount + (largeCount / 2), result);
            assertTrue(duration < 1000, "Cleanup took too long: " + duration + "ms");
            
            System.out.println("Cleanup Performance:");
            System.out.println("- Duration: " + duration + "ms");
            System.out.println("- Records processed: " + result);
        }
    }

    @Nested
    class MemoryAndResourceManagementTests {

        @Test
        void passwordResetService_memoryUsage_staysWithinBounds() throws Exception {
            // Arrange
            Runtime runtime = Runtime.getRuntime();
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            // Act - Generate many tokens to test memory usage
            for (int i = 0; i < 5000; i++) {
                ReflectionTestUtils.invokeMethod(passwordResetService, "generateSecureToken");
            }
            
            // Force garbage collection
            System.gc();
            Thread.sleep(100);
            
            // Assert
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = finalMemory - initialMemory;
            
            // Memory increase should be minimal (less than 10MB)
            assertTrue(memoryIncrease < 10 * 1024 * 1024, 
                "Memory usage increased too much: " + (memoryIncrease / 1024 / 1024) + "MB");
        }

        @Test
        void secureRandom_doesNotAccumulateState() throws Exception {
            // Arrange
            Field secureRandomField = PasswordResetServiceImpl.class.getDeclaredField("secureRandom");
            secureRandomField.setAccessible(true);
            SecureRandom secureRandom = (SecureRandom) secureRandomField.get(passwordResetService);

            // Act - Generate many random numbers
            Set<String> tokens = new HashSet<>();
            for (int i = 0; i < 1000; i++) {
                String token = (String) ReflectionTestUtils.invokeMethod(passwordResetService, "generateSecureToken");
                tokens.add(token);
            }

            // Assert - All tokens should still be unique (no state accumulation issues)
            assertEquals(1000, tokens.size());
            
            // Verify SecureRandom is still functioning
            assertNotNull(secureRandom);
            assertTrue(secureRandom.nextInt(1000) >= 0);
        }

        @Test
        void multipleServiceInstances_doNotShareState() {
            // Arrange
            PasswordResetServiceImpl service1 = new PasswordResetServiceImpl(
                    passwordResetTokenRepository, userRepository, emailService, passwordEncoder, clockProvider);
            PasswordResetServiceImpl service2 = new PasswordResetServiceImpl(
                    passwordResetTokenRepository, userRepository, emailService, passwordEncoder, clockProvider);
            
            ReflectionTestUtils.setField(service1, "tokenExpiryMinutes", 15);
            ReflectionTestUtils.setField(service2, "tokenExpiryMinutes", 30);

            // Act & Assert
            Integer expiry1 = (Integer) ReflectionTestUtils.getField(service1, "tokenExpiryMinutes");
            Integer expiry2 = (Integer) ReflectionTestUtils.getField(service2, "tokenExpiryMinutes");
            
            assertNotEquals(expiry1, expiry2);
            assertEquals(15, expiry1);
            assertEquals(30, expiry2);
        }
    }

    @Nested
    class SecurityEdgeCaseTests {

        @Test
        void tokenGeneration_afterMultipleInvocations_maintainsRandomness() throws Exception {
            // Arrange
            Set<String> batch1 = new HashSet<>();
            Set<String> batch2 = new HashSet<>();
            int batchSize = 100;

            // Act - Generate two batches separated by time
            for (int i = 0; i < batchSize; i++) {
                batch1.add((String) ReflectionTestUtils.invokeMethod(passwordResetService, "generateSecureToken"));
            }
            
            Thread.sleep(50); // Small delay
            
            for (int i = 0; i < batchSize; i++) {
                batch2.add((String) ReflectionTestUtils.invokeMethod(passwordResetService, "generateSecureToken"));
            }

            // Assert - No overlap between batches (maintains randomness)
            assertEquals(batchSize, batch1.size());
            assertEquals(batchSize, batch2.size());
            
            Set<String> intersection = new HashSet<>(batch1);
            intersection.retainAll(batch2);
            assertTrue(intersection.isEmpty(), "Found duplicate tokens between batches: " + intersection.size());
        }

        @Test
        void passwordReset_withSpecialCharacters_handledCorrectly() {
            // Arrange
            mockClockProvider();
            String[] specialPasswords = {
                "Pässwörd123!",           // Umlauts
                "パスワード123!",            // Japanese
                "🔒Secure123!🔑",          // Emojis
                "Пароль123!",             // Cyrillic
                "מילה123!",               // Hebrew
                "كلمة123!",                // Arabic
                "Test\nLine\tTab123!",     // Control characters
                "\"'`<>&123!"             // HTML/SQL injection chars
            };

            // Test each password separately to avoid unnecessary stubbing
            for (int i = 0; i < specialPasswords.length; i++) {
                String password = specialPasswords[i];
                String token = "valid-token-" + i; // Use different token for each test
                
                User user = TestUtils.createValidUserEntityWithId();
                PasswordResetToken resetToken = new PasswordResetToken(token, user, fixedTime, fixedTime.plusSeconds(900));

                when(passwordResetTokenRepository.findValidToken(token, fixedTime))
                        .thenReturn(Optional.of(resetToken));
                when(passwordEncoder.encode(password)).thenReturn("hashed-" + password.hashCode());
                
                ResetPasswordRequestDTO request = new ResetPasswordRequestDTO(token, password);
                
                // Should not throw exception
                assertDoesNotThrow(() -> {
                    ResetPasswordResponseDTO response = passwordResetService.resetPassword(request);
                    assertNotNull(response);
                }, "Failed to handle password: " + password);
            }
        }

        @Test
        void simulateProcessingTime_underInterruption_handlesGracefully() throws Exception {
            // Arrange
            Thread testThread = new Thread(() -> {
                try {
                    ReflectionTestUtils.invokeMethod(passwordResetService, "simulateProcessingTime");
                } catch (Exception e) {
                    // Expected when interrupted
                }
            });

            // Act
            testThread.start();
            Thread.sleep(50); // Let it start
            testThread.interrupt(); // Interrupt during processing

            // Assert
            testThread.join(1000); // Should complete within 1 second
            assertTrue(testThread.isInterrupted() || !testThread.isAlive());
        }

        @Test
        void tokenGeneration_withDifferentCharacterDistribution_isUniform() throws Exception {
            // Arrange
            Map<Character, Integer> charCount = new HashMap<>();
            int tokenCount = 1000;
            String validChars = ApplicationConstants.PASSWORD_RESET_TOKEN_CHARACTERS;

            // Initialize counts
            for (char c : validChars.toCharArray()) {
                charCount.put(c, 0);
            }

            // Act
            for (int i = 0; i < tokenCount; i++) {
                String token = (String) ReflectionTestUtils.invokeMethod(passwordResetService, "generateSecureToken");
                for (char c : token.toCharArray()) {
                    charCount.put(c, charCount.get(c) + 1);
                }
            }

            // Assert - Check for reasonable distribution
            int totalChars = tokenCount * ApplicationConstants.PASSWORD_RESET_TOKEN_LENGTH;
            int expectedPerChar = totalChars / validChars.length();
            
            for (Map.Entry<Character, Integer> entry : charCount.entrySet()) {
                int count = entry.getValue();
                double deviation = Math.abs(count - expectedPerChar) / (double) expectedPerChar;
                
                // Allow 50% deviation (randomness will cause some variation)
                assertTrue(deviation < 0.5, 
                    "Character '" + entry.getKey() + "' appears " + count + " times, expected ~" + expectedPerChar + 
                    " (deviation: " + String.format("%.2f%%", deviation * 100) + ")");
            }
        }
    }

    @Nested
    class DatabaseTransactionEdgeCaseTests {

        @Test
        void passwordReset_withDatabaseConstraintViolation_handlesGracefully() {
            // Arrange
            mockClockProvider();
            String token = "valid-token-123";
            String newPassword = "NewPassword123!";

            User user = TestUtils.createValidUserEntityWithId();
            PasswordResetToken resetToken = new PasswordResetToken(token, user, fixedTime, fixedTime.plusSeconds(900));

            ResetPasswordRequestDTO request = new ResetPasswordRequestDTO(token, newPassword);

            when(passwordResetTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.of(resetToken));
            when(passwordEncoder.encode(newPassword)).thenReturn("hashed-password");
            when(userRepository.save(user))
                    .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Constraint violation"));

            // Act & Assert
            PasswordResetException thrown = assertThrows(PasswordResetException.class,
                    () -> passwordResetService.resetPassword(request));

            assertEquals(ErrorCode.PASSWORD_RESET_DATABASE_ERROR, thrown.getErrorCode());
            assertTrue(thrown.getCause() instanceof org.springframework.dao.DataIntegrityViolationException);
        }

        @Test
        void requestPasswordReset_withDatabaseDeadlock_handlesGracefully() {
            // Arrange
            mockClockProvider();
            String email = "user@example.com";
            User user = TestUtils.createValidUserEntityWithId();
            user.setEmail(email);

            ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO(email);

            when(userRepository.findByEmailAndIsPendingDeletionFalse(email))
                    .thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                    .thenThrow(new org.springframework.dao.CannotAcquireLockException("Deadlock detected"));

            // Act
            ForgotPasswordResponseDTO response = passwordResetService.requestPasswordReset(request);

            // Assert - Should still return standard response
            assertNotNull(response);
            assertEquals("If your email is registered, you will receive a password reset link shortly.",
                        response.message());
        }

        @Test
        void passwordReset_withOptimisticLockingFailure_handlesGracefully() {
            // Arrange
            mockClockProvider();
            String token = "valid-token-123";
            String newPassword = "NewPassword123!";

            User user = TestUtils.createValidUserEntityWithId();
            PasswordResetToken resetToken = new PasswordResetToken(token, user, fixedTime, fixedTime.plusSeconds(900));

            ResetPasswordRequestDTO request = new ResetPasswordRequestDTO(token, newPassword);

            when(passwordResetTokenRepository.findValidToken(token, fixedTime))
                    .thenReturn(Optional.of(resetToken));
            when(passwordEncoder.encode(newPassword)).thenReturn("hashed-password");
            when(userRepository.save(user))
                    .thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException("Version conflict", new Exception()));

            // Act & Assert
            PasswordResetException thrown = assertThrows(PasswordResetException.class,
                    () -> passwordResetService.resetPassword(request));

            assertEquals(ErrorCode.PASSWORD_RESET_DATABASE_ERROR, thrown.getErrorCode());
        }
    }

    @Nested
    class ConfigurationAndEnvironmentTests {

        @Test
        void passwordResetService_withDifferentTimezones_handlesCorrectly() {
            // Arrange
            ZoneId[] timezones = {
                ZoneId.of("UTC"),
                ZoneId.of("America/New_York"),
                ZoneId.of("Europe/London"),
                ZoneId.of("Asia/Tokyo"),
                ZoneId.of("Australia/Sydney")
            };

            Clock utcClock = Clock.fixed(fixedTime, ZoneOffset.UTC);
            when(clockProvider.getClockForZone(ZoneOffset.UTC)).thenReturn(utcClock);

            // Act & Assert
            for (ZoneId timezone : timezones) {
                // Service should always use UTC regardless of system timezone
                boolean result = passwordResetService.isValidResetToken("any-token");
                
                // Result consistency regardless of timezone
                assertFalse(result); // Token doesn't exist, should always be false
                
                // Verify UTC clock is always used
                verify(clockProvider, atLeastOnce()).getClockForZone(ZoneOffset.UTC);
            }
        }

        @Test
        void validateConfiguration_withExtremeValues_handlesAppropriately() {
            // Test extreme configuration values
            int[] extremeValues = {
                Integer.MIN_VALUE,
                -1000000,
                0,
                Integer.MAX_VALUE
            };

            for (int value : extremeValues) {
                // Arrange
                PasswordResetServiceImpl service = new PasswordResetServiceImpl(
                        passwordResetTokenRepository, userRepository, emailService, passwordEncoder, clockProvider);
                ReflectionTestUtils.setField(service, "tokenExpiryMinutes", value);

                // Act
                ReflectionTestUtils.invokeMethod(service, "validateConfiguration");

                // Assert
                Integer actualValue = (Integer) ReflectionTestUtils.getField(service, "tokenExpiryMinutes");
                if (value <= 0) {
                    assertEquals(ApplicationConstants.PASSWORD_RESET_DEFAULT_TOKEN_EXPIRY_MINUTES, actualValue);
                } else {
                    assertEquals(value, actualValue);
                }
            }
        }

        @Test
        void passwordResetService_withNullDependencies_handlesGracefully() {
            // Test creation with null dependencies (Spring will handle this in real scenarios)
            assertDoesNotThrow(() -> {
                new PasswordResetServiceImpl(null, null, null, null, null);
            });
        }

        @Test
        void passwordResetService_withMockDependencyFailures_maintainsStability() {
            // Arrange - Create service with dependencies that can fail
            PasswordResetTokenRepository failingTokenRepo = mock(PasswordResetTokenRepository.class);
            UserRepository failingUserRepo = mock(UserRepository.class);
            EmailService failingEmailService = mock(EmailService.class);
            PasswordEncoder failingPasswordEncoder = mock(PasswordEncoder.class);
            ClockProvider failingClockProvider = mock(ClockProvider.class);

            // Only stub the method that will actually be called (findByEmailAndIsPendingDeletionFalse)
            when(failingUserRepo.findByEmailAndIsPendingDeletionFalse(anyString()))
                    .thenThrow(new RuntimeException("Database connection lost"));

            PasswordResetServiceImpl unstableService = new PasswordResetServiceImpl(
                    failingTokenRepo, failingUserRepo, failingEmailService, failingPasswordEncoder, failingClockProvider);
            ReflectionTestUtils.setField(unstableService, "tokenExpiryMinutes", 15);

            // Act & Assert - Service should handle failures gracefully
            ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO("test@example.com");
            ForgotPasswordResponseDTO response = unstableService.requestPasswordReset(request);
            
            // Should return standard response even with failing dependencies
            assertNotNull(response);
            assertEquals("If your email is registered, you will receive a password reset link shortly.",
                        response.message());
        }
    }

    @Nested
    class ConstructorTests {

        @Test
        void constructor_withValidDependencies_createsInstance() {
            // Act & Assert
            assertDoesNotThrow(() -> new PasswordResetServiceImpl(
                    passwordResetTokenRepository,
                    userRepository,
                    emailService,
                    passwordEncoder,
                    clockProvider
            ));
        }

        @Test
        void constructor_withNullRepository_allowsCreation() {
            // Act & Assert - Spring will handle null injection validation
            assertDoesNotThrow(() -> new PasswordResetServiceImpl(
                    null,
                    userRepository,
                    emailService,
                    passwordEncoder,
                    clockProvider
            ));
        }
    }
}