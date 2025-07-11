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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

        // Mock clock provider
        Clock fixedClock = Clock.fixed(fixedTime, ZoneOffset.UTC);
        lenient().when(clockProvider.getClockForZone(ZoneOffset.UTC)).thenReturn(fixedClock);
    }

    @Nested
    class GenerateAndSendVerificationTokenTests {

        @Test
        void generateAndSendVerificationToken_ShouldInvalidateExistingTokensAndSendEmail() {
            // Given
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
}