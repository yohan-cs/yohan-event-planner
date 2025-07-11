package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.PasswordResetToken;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.auth.ForgotPasswordRequestDTO;
import com.yohan.event_planner.dto.auth.ForgotPasswordResponseDTO;
import com.yohan.event_planner.dto.auth.ResetPasswordRequestDTO;
import com.yohan.event_planner.dto.auth.ResetPasswordResponseDTO;
import com.yohan.event_planner.exception.ErrorCode;
import com.yohan.event_planner.exception.PasswordException;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

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

        // Mock clock provider - use lenient since not all tests use it
        lenient().when(clockProvider.getClockForZone(ZoneOffset.UTC)).thenReturn(clock);
        lenient().when(clock.instant()).thenReturn(fixedTime);
    }

    @Nested
    class RequestPasswordResetTests {

        @Test
        void requestPasswordReset_whenUserExists_generatesTokenAndSendsEmail() {
            // Arrange
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
        void resetPassword_whenPasswordEncodingFails_throwsPasswordException() {
            // Arrange
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
            PasswordException thrown = assertThrows(PasswordException.class,
                    () -> passwordResetService.resetPassword(request));

            assertEquals(ErrorCode.UNKNOWN_ERROR, thrown.getErrorCode());
        }
    }

    @Nested
    class IsValidResetTokenTests {

        @Test
        void isValidResetToken_withValidToken_returnsTrue() {
            // Arrange
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
            when(passwordResetTokenRepository.deleteExpiredTokens(fixedTime)).thenReturn(0);
            when(passwordResetTokenRepository.deleteUsedTokens()).thenReturn(0);

            // Act
            int result = passwordResetService.cleanupExpiredTokens();

            // Assert
            assertEquals(0, result);
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