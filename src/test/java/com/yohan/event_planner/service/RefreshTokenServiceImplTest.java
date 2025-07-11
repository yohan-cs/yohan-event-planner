package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.RefreshToken;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.auth.RefreshTokenResponseDTO;
import com.yohan.event_planner.exception.UnauthorizedException;
import com.yohan.event_planner.exception.UserNotFoundException;
import com.yohan.event_planner.repository.RefreshTokenRepository;
import com.yohan.event_planner.repository.UserRepository;
import com.yohan.event_planner.security.CustomUserDetails;
import com.yohan.event_planner.security.JwtUtils;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTokenServiceImplTest {

    private RefreshTokenRepository refreshTokenRepository;
    private UserRepository userRepository;
    private JwtUtils jwtUtils;
    private RefreshTokenServiceImpl refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        userRepository = mock(UserRepository.class);
        jwtUtils = mock(JwtUtils.class);
        refreshTokenService = new RefreshTokenServiceImpl(refreshTokenRepository, userRepository, jwtUtils);
    }

    @Nested
    class CreateRefreshTokenTests {

        @Test
        void testCreateRefreshToken_success_returnsRawToken() {
            // Arrange
            Long userId = 1L;
            String rawToken = "raw-token-uuid";
            String hashedToken = "hashed-token";
            long expirationMs = 86400000L; // 1 day

            when(jwtUtils.generateRefreshToken()).thenReturn(rawToken);
            when(jwtUtils.hashRefreshToken(rawToken)).thenReturn(hashedToken);
            when(jwtUtils.getRefreshTokenExpirationMs()).thenReturn(expirationMs);

            // Act
            String result = refreshTokenService.createRefreshToken(userId);

            // Assert
            assertEquals(rawToken, result);

            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(tokenCaptor.capture());

            RefreshToken savedToken = tokenCaptor.getValue();
            assertEquals(hashedToken, savedToken.getTokenHash());
            assertEquals(userId, savedToken.getUserId());
            assertNotNull(savedToken.getExpiryDate());
            assertFalse(savedToken.isRevoked());
        }
    }

    @Nested
    class RefreshTokensTests {

        @Test
        void testRefreshTokens_validToken_returnsNewTokenPair() {
            // Arrange
            String refreshToken = "valid-refresh-token";
            Long userId = 1L;
            User user = TestUtils.createValidUserEntityWithId();
            RefreshToken tokenEntity = TestUtils.createValidRefreshToken(userId);
            String newAccessToken = "new-access-token";
            String newRefreshToken = "new-refresh-token";

            when(jwtUtils.validateRefreshToken(refreshToken, tokenEntity.getTokenHash())).thenReturn(true);
            when(refreshTokenRepository.findAll()).thenReturn(Arrays.asList(tokenEntity));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(jwtUtils.generateToken(any(CustomUserDetails.class))).thenReturn(newAccessToken);
            when(jwtUtils.generateRefreshToken()).thenReturn(newRefreshToken);
            when(jwtUtils.hashRefreshToken(newRefreshToken)).thenReturn("hashed-new-token");
            when(jwtUtils.getRefreshTokenExpirationMs()).thenReturn(86400000L);

            // Act
            RefreshTokenResponseDTO result = refreshTokenService.refreshTokens(refreshToken);

            // Assert
            assertEquals(newAccessToken, result.accessToken());
            assertEquals(newRefreshToken, result.refreshToken());

            // Verify old token was revoked
            assertTrue(tokenEntity.isRevoked());
            verify(refreshTokenRepository).save(tokenEntity);

            // Verify new refresh token was created
            verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
        }

        @Test
        void testRefreshTokens_tokenNotFound_throwsUnauthorizedException() {
            // Arrange
            String refreshToken = "non-existent-token";
            when(refreshTokenRepository.findAll()).thenReturn(Collections.emptyList());

            // Act & Assert
            assertThrows(UnauthorizedException.class, 
                () -> refreshTokenService.refreshTokens(refreshToken));
        }

        @Test
        void testRefreshTokens_tokenExpired_throwsUnauthorizedException() {
            // Arrange
            String refreshToken = "expired-token";
            Long userId = 1L;
            RefreshToken expiredToken = TestUtils.createExpiredRefreshToken(userId);

            when(jwtUtils.validateRefreshToken(refreshToken, expiredToken.getTokenHash())).thenReturn(true);
            when(refreshTokenRepository.findAll()).thenReturn(Arrays.asList(expiredToken));

            // Act & Assert
            assertThrows(UnauthorizedException.class, 
                () -> refreshTokenService.refreshTokens(refreshToken));
        }

        @Test
        void testRefreshTokens_tokenRevoked_throwsUnauthorizedException() {
            // Arrange
            String refreshToken = "revoked-token";
            Long userId = 1L;
            RefreshToken revokedToken = TestUtils.createRevokedRefreshToken(userId);

            when(jwtUtils.validateRefreshToken(refreshToken, revokedToken.getTokenHash())).thenReturn(true);
            when(refreshTokenRepository.findAll()).thenReturn(Arrays.asList(revokedToken));

            // Act & Assert
            assertThrows(UnauthorizedException.class, 
                () -> refreshTokenService.refreshTokens(refreshToken));
        }

        @Test
        void testRefreshTokens_userNotFound_throwsUserNotFoundException() {
            // Arrange
            String refreshToken = "valid-token";
            Long userId = 1L;
            RefreshToken tokenEntity = TestUtils.createValidRefreshToken(userId);

            when(jwtUtils.validateRefreshToken(refreshToken, tokenEntity.getTokenHash())).thenReturn(true);
            when(refreshTokenRepository.findAll()).thenReturn(Arrays.asList(tokenEntity));
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(UserNotFoundException.class, 
                () -> refreshTokenService.refreshTokens(refreshToken));

            // Verify token was still revoked even though user lookup failed
            assertTrue(tokenEntity.isRevoked());
        }

        @Test
        void testRefreshTokens_multipleTokensInDatabase_findsCorrectOne() {
            // Arrange
            String refreshToken = "correct-token";
            Long userId = 1L;
            User user = TestUtils.createValidUserEntityWithId();
            
            RefreshToken wrongToken = TestUtils.createValidRefreshToken(2L);
            RefreshToken correctToken = TestUtils.createValidRefreshToken(userId);
            String newAccessToken = "new-access-token";
            String newRefreshToken = "new-refresh-token";

            when(jwtUtils.validateRefreshToken(refreshToken, wrongToken.getTokenHash())).thenReturn(false);
            when(jwtUtils.validateRefreshToken(refreshToken, correctToken.getTokenHash())).thenReturn(true);
            when(refreshTokenRepository.findAll()).thenReturn(Arrays.asList(wrongToken, correctToken));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(jwtUtils.generateToken(any(CustomUserDetails.class))).thenReturn(newAccessToken);
            when(jwtUtils.generateRefreshToken()).thenReturn(newRefreshToken);
            when(jwtUtils.hashRefreshToken(newRefreshToken)).thenReturn("hashed-new-token");
            when(jwtUtils.getRefreshTokenExpirationMs()).thenReturn(86400000L);

            // Act
            RefreshTokenResponseDTO result = refreshTokenService.refreshTokens(refreshToken);

            // Assert
            assertEquals(newAccessToken, result.accessToken());
            assertEquals(newRefreshToken, result.refreshToken());

            // Verify correct token was revoked
            assertTrue(correctToken.isRevoked());
            assertFalse(wrongToken.isRevoked());
        }
    }

    @Nested
    class RevokeRefreshTokenTests {

        @Test
        void testRevokeRefreshToken_validToken_revokesToken() {
            // Arrange
            String refreshToken = "valid-token";
            Long userId = 1L;
            RefreshToken tokenEntity = TestUtils.createValidRefreshToken(userId);

            when(jwtUtils.validateRefreshToken(refreshToken, tokenEntity.getTokenHash())).thenReturn(true);
            when(refreshTokenRepository.findAll()).thenReturn(Arrays.asList(tokenEntity));

            // Act
            refreshTokenService.revokeRefreshToken(refreshToken);

            // Assert
            assertTrue(tokenEntity.isRevoked());
            verify(refreshTokenRepository).save(tokenEntity);
        }

        @Test
        void testRevokeRefreshToken_tokenNotFound_doesNotThrow() {
            // Arrange
            String refreshToken = "non-existent-token";
            when(refreshTokenRepository.findAll()).thenReturn(Collections.emptyList());

            // Act & Assert - should not throw
            assertDoesNotThrow(() -> refreshTokenService.revokeRefreshToken(refreshToken));
            verify(refreshTokenRepository, never()).save(any());
        }
    }

    @Nested
    class RevokeAllUserTokensTests {

        @Test
        void testRevokeAllUserTokens_returnsCount() {
            // Arrange
            Long userId = 1L;
            int expectedCount = 3;
            when(refreshTokenRepository.revokeAllByUserId(userId)).thenReturn(expectedCount);

            // Act
            int result = refreshTokenService.revokeAllUserTokens(userId);

            // Assert
            assertEquals(expectedCount, result);
            verify(refreshTokenRepository).revokeAllByUserId(userId);
        }
    }

    @Nested
    class CleanupExpiredTokensTests {

        @Test
        void testCleanupExpiredTokens_returnsCount() {
            // Arrange
            int expectedCount = 5;
            when(refreshTokenRepository.deleteExpiredTokens(any(Instant.class))).thenReturn(expectedCount);

            // Act
            int result = refreshTokenService.cleanupExpiredTokens();

            // Assert
            assertEquals(expectedCount, result);
            verify(refreshTokenRepository).deleteExpiredTokens(any(Instant.class));
        }
    }

    @Nested
    class CleanupRevokedTokensTests {

        @Test
        void testCleanupRevokedTokens_returnsCount() {
            // Arrange
            int daysOld = 30;
            int expectedCount = 2;
            when(refreshTokenRepository.deleteRevokedTokensOlderThan(any(Instant.class))).thenReturn(expectedCount);

            // Act
            int result = refreshTokenService.cleanupRevokedTokens(daysOld);

            // Assert
            assertEquals(expectedCount, result);
            verify(refreshTokenRepository).deleteRevokedTokensOlderThan(any(Instant.class));
        }

        @Test
        void testCleanupRevokedTokens_calculatesCorrectCutoffDate() {
            // Arrange
            int daysOld = 7;
            when(refreshTokenRepository.deleteRevokedTokensOlderThan(any(Instant.class))).thenReturn(1);

            // Act
            refreshTokenService.cleanupRevokedTokens(daysOld);

            // Assert
            ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(refreshTokenRepository).deleteRevokedTokensOlderThan(cutoffCaptor.capture());
            
            Instant cutoffDate = cutoffCaptor.getValue();
            Instant now = Instant.now();
            
            // Verify the cutoff is in the past (should be 7 days ago)
            assertTrue(cutoffDate.isBefore(now), "Cutoff date should be in the past");
            
            // Verify the cutoff is reasonably close to 7 days ago (within 1 minute tolerance)
            long expectedCutoffEpoch = now.minusSeconds(daysOld * 24 * 60 * 60).getEpochSecond();
            long actualCutoffEpoch = cutoffDate.getEpochSecond();
            long differenceSeconds = Math.abs(actualCutoffEpoch - expectedCutoffEpoch);
            
            assertTrue(differenceSeconds < 60, // Within 1 minute tolerance
                String.format("Cutoff should be approximately %d days ago. Difference: %d seconds", daysOld, differenceSeconds));
        }
    }
}