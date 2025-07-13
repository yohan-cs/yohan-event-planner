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
import org.springframework.dao.DataAccessException;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        void testCreateRefreshToken_nullUserId_throwsIllegalArgumentException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> refreshTokenService.createRefreshToken(null));
            
            assertEquals("User ID cannot be null", exception.getMessage());
            verify(refreshTokenRepository, never()).save(any());
        }

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

        @Test
        void testCreateRefreshToken_repositoryException_propagatesException() {
            // Arrange
            Long userId = 1L;
            String rawToken = "raw-token";
            String hashedToken = "hashed-token";
            
            when(jwtUtils.generateRefreshToken()).thenReturn(rawToken);
            when(jwtUtils.hashRefreshToken(rawToken)).thenReturn(hashedToken);
            when(jwtUtils.getRefreshTokenExpirationMs()).thenReturn(86400000L);
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenThrow(new DataAccessException("Database save failed") {});

            // Act & Assert
            assertThrows(DataAccessException.class, 
                () -> refreshTokenService.createRefreshToken(userId));
        }

        @Test
        void testCreateRefreshToken_jwtUtilsException_propagatesException() {
            // Arrange
            Long userId = 1L;
            
            when(jwtUtils.generateRefreshToken())
                .thenThrow(new RuntimeException("Token generation failed"));

            // Act & Assert
            assertThrows(RuntimeException.class, 
                () -> refreshTokenService.createRefreshToken(userId));
        }

        @Test
        void testCreateRefreshToken_verifyExpiryDateCalculation() {
            // Arrange
            Long userId = 1L;
            String rawToken = "raw-token";
            String hashedToken = "hashed-token";
            long expirationMs = 3600000L; // 1 hour
            Instant beforeTest = Instant.now();
            
            when(jwtUtils.generateRefreshToken()).thenReturn(rawToken);
            when(jwtUtils.hashRefreshToken(rawToken)).thenReturn(hashedToken);
            when(jwtUtils.getRefreshTokenExpirationMs()).thenReturn(expirationMs);

            // Act
            String result = refreshTokenService.createRefreshToken(userId);
            Instant afterTest = Instant.now();

            // Assert
            assertEquals(rawToken, result);
            
            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(tokenCaptor.capture());
            
            RefreshToken savedToken = tokenCaptor.getValue();
            Instant expectedMinExpiry = beforeTest.plusMillis(expirationMs);
            Instant expectedMaxExpiry = afterTest.plusMillis(expirationMs);
            
            assertTrue(savedToken.getExpiryDate().isAfter(expectedMinExpiry.minusSeconds(1)), 
                "Expiry date should be after expected minimum");
            assertTrue(savedToken.getExpiryDate().isBefore(expectedMaxExpiry.plusSeconds(1)), 
                "Expiry date should be before expected maximum");
        }

        @Test
        void testCreateRefreshToken_zeroExpirationTime_handlesCorrectly() {
            // Arrange
            Long userId = 1L;
            String rawToken = "raw-token";
            String hashedToken = "hashed-token";
            long zeroExpirationMs = 0L;
            
            when(jwtUtils.generateRefreshToken()).thenReturn(rawToken);
            when(jwtUtils.hashRefreshToken(rawToken)).thenReturn(hashedToken);
            when(jwtUtils.getRefreshTokenExpirationMs()).thenReturn(zeroExpirationMs);

            // Act
            String result = refreshTokenService.createRefreshToken(userId);

            // Assert
            assertEquals(rawToken, result);
            
            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(tokenCaptor.capture());
            
            RefreshToken savedToken = tokenCaptor.getValue();
            // With 0ms expiration, token should be expired immediately or very close to now
            assertTrue(savedToken.getExpiryDate().isBefore(Instant.now().plusSeconds(1)), 
                "Token with 0ms expiration should expire immediately");
        }

        @Test
        void testCreateRefreshToken_multipleCallsForSameUser_createsDistinctTokens() {
            // Arrange
            Long userId = 1L;
            String rawToken1 = "raw-token-1";
            String rawToken2 = "raw-token-2";
            String hashedToken1 = "hashed-token-1";
            String hashedToken2 = "hashed-token-2";
            long expirationMs = 86400000L;
            
            when(jwtUtils.generateRefreshToken())
                .thenReturn(rawToken1)
                .thenReturn(rawToken2);
            when(jwtUtils.hashRefreshToken(rawToken1)).thenReturn(hashedToken1);
            when(jwtUtils.hashRefreshToken(rawToken2)).thenReturn(hashedToken2);
            when(jwtUtils.getRefreshTokenExpirationMs()).thenReturn(expirationMs);

            // Act
            String result1 = refreshTokenService.createRefreshToken(userId);
            String result2 = refreshTokenService.createRefreshToken(userId);

            // Assert
            assertEquals(rawToken1, result1);
            assertEquals(rawToken2, result2);
            assertNotEquals(result1, result2, "Multiple calls should create distinct tokens");
            
            // Verify both tokens were saved
            verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
        }
    }

    @Nested
    class RefreshTokensTests {

        @Test
        void testRefreshTokens_nullToken_throwsIllegalArgumentException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> refreshTokenService.refreshTokens(null));
            
            assertEquals("Refresh token cannot be null or empty", exception.getMessage());
        }

        @Test
        void testRefreshTokens_emptyToken_throwsIllegalArgumentException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> refreshTokenService.refreshTokens("   "));
            
            assertEquals("Refresh token cannot be null or empty", exception.getMessage());
        }

        @Test
        void testRefreshTokens_validToken_returnsNewTokenPair() {
            // Arrange
            String refreshToken = "valid-refresh-token";
            String hashedToken = "hashed-token";
            Long userId = 1L;
            User user = TestUtils.createValidUserEntityWithId();
            RefreshToken tokenEntity = TestUtils.createValidRefreshToken(userId);
            String newAccessToken = "new-access-token";
            String newRefreshToken = "new-refresh-token";

            when(jwtUtils.hashRefreshToken(refreshToken)).thenReturn(hashedToken);
            when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(tokenEntity));
            when(jwtUtils.validateRefreshToken(refreshToken, tokenEntity.getTokenHash())).thenReturn(true);
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
            String hashedToken = "hashed-token";
            
            when(jwtUtils.hashRefreshToken(refreshToken)).thenReturn(hashedToken);
            when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(UnauthorizedException.class, 
                () -> refreshTokenService.refreshTokens(refreshToken));
        }

        @Test
        void testRefreshTokens_tokenExpired_throwsUnauthorizedException() {
            // Arrange
            String refreshToken = "expired-token";
            String hashedToken = "hashed-token";
            Long userId = 1L;
            RefreshToken expiredToken = TestUtils.createExpiredRefreshToken(userId);

            when(jwtUtils.hashRefreshToken(refreshToken)).thenReturn(hashedToken);
            when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(expiredToken));
            when(jwtUtils.validateRefreshToken(refreshToken, expiredToken.getTokenHash())).thenReturn(true);

            // Act & Assert
            assertThrows(UnauthorizedException.class, 
                () -> refreshTokenService.refreshTokens(refreshToken));
        }

        @Test
        void testRefreshTokens_tokenRevoked_throwsUnauthorizedException() {
            // Arrange
            String refreshToken = "revoked-token";
            String hashedToken = "hashed-token";
            Long userId = 1L;
            RefreshToken revokedToken = TestUtils.createRevokedRefreshToken(userId);

            when(jwtUtils.hashRefreshToken(refreshToken)).thenReturn(hashedToken);
            when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(revokedToken));
            when(jwtUtils.validateRefreshToken(refreshToken, revokedToken.getTokenHash())).thenReturn(true);

            // Act & Assert
            assertThrows(UnauthorizedException.class, 
                () -> refreshTokenService.refreshTokens(refreshToken));
        }

        @Test
        void testRefreshTokens_userNotFound_throwsUserNotFoundException() {
            // Arrange
            String refreshToken = "valid-token";
            String hashedToken = "hashed-token";
            Long userId = 1L;
            RefreshToken tokenEntity = TestUtils.createValidRefreshToken(userId);

            when(jwtUtils.hashRefreshToken(refreshToken)).thenReturn(hashedToken);
            when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(tokenEntity));
            when(jwtUtils.validateRefreshToken(refreshToken, tokenEntity.getTokenHash())).thenReturn(true);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(UserNotFoundException.class, 
                () -> refreshTokenService.refreshTokens(refreshToken));

            // Verify token was still revoked even though user lookup failed
            assertTrue(tokenEntity.isRevoked());
        }

        @Test
        void testRefreshTokens_tokenFoundButValidationFails_throwsUnauthorizedException() {
            // Arrange
            String refreshToken = "invalid-token";
            String hashedToken = "hashed-token";
            Long userId = 1L;
            RefreshToken tokenEntity = TestUtils.createValidRefreshToken(userId);

            when(jwtUtils.hashRefreshToken(refreshToken)).thenReturn(hashedToken);
            when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(tokenEntity));
            when(jwtUtils.validateRefreshToken(refreshToken, tokenEntity.getTokenHash())).thenReturn(false);

            // Act & Assert
            assertThrows(UnauthorizedException.class, 
                () -> refreshTokenService.refreshTokens(refreshToken));
            
            // Verify token was not modified since validation failed
            assertFalse(tokenEntity.isRevoked());
        }

        @Test
        void testRefreshTokens_repositoryException_propagatesException() {
            // Arrange
            String refreshToken = "valid-token";
            String hashedToken = "hashed-token";
            
            when(jwtUtils.hashRefreshToken(refreshToken)).thenReturn(hashedToken);
            when(refreshTokenRepository.findByTokenHash(hashedToken))
                .thenThrow(new DataAccessException("Database connection failed") {});

            // Act & Assert
            assertThrows(DataAccessException.class, 
                () -> refreshTokenService.refreshTokens(refreshToken));
        }

        @Test
        void testRefreshTokens_jwtUtilsException_propagatesException() {
            // Arrange
            String refreshToken = "valid-token";
            
            when(jwtUtils.hashRefreshToken(refreshToken))
                .thenThrow(new RuntimeException("Hashing failed"));

            // Act & Assert
            assertThrows(RuntimeException.class, 
                () -> refreshTokenService.refreshTokens(refreshToken));
        }

        @Test
        void testRefreshTokens_userRepositoryException_tokenStillRevoked() {
            // Arrange
            String refreshToken = "valid-token";
            String hashedToken = "hashed-token";
            Long userId = 1L;
            RefreshToken tokenEntity = TestUtils.createValidRefreshToken(userId);

            when(jwtUtils.hashRefreshToken(refreshToken)).thenReturn(hashedToken);
            when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(tokenEntity));
            when(jwtUtils.validateRefreshToken(refreshToken, tokenEntity.getTokenHash())).thenReturn(true);
            when(userRepository.findById(userId))
                .thenThrow(new DataAccessException("User lookup failed") {});

            // Act & Assert
            assertThrows(DataAccessException.class, 
                () -> refreshTokenService.refreshTokens(refreshToken));
            
            // Verify token was still revoked even though user lookup failed
            assertTrue(tokenEntity.isRevoked());
            verify(refreshTokenRepository).save(tokenEntity);
        }
    }

    @Nested
    class RevokeRefreshTokenTests {

        @Test
        void testRevokeRefreshToken_nullToken_doesNotThrow() {
            // Act & Assert - should not throw
            assertDoesNotThrow(() -> refreshTokenService.revokeRefreshToken(null));
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        void testRevokeRefreshToken_emptyToken_doesNotThrow() {
            // Act & Assert - should not throw
            assertDoesNotThrow(() -> refreshTokenService.revokeRefreshToken("  "));
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        void testRevokeRefreshToken_validToken_revokesToken() {
            // Arrange
            String refreshToken = "valid-token";
            String hashedToken = "hashed-token";
            Long userId = 1L;
            RefreshToken tokenEntity = TestUtils.createValidRefreshToken(userId);

            when(jwtUtils.hashRefreshToken(refreshToken)).thenReturn(hashedToken);
            when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(tokenEntity));
            when(jwtUtils.validateRefreshToken(refreshToken, tokenEntity.getTokenHash())).thenReturn(true);

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
            String hashedToken = "hashed-token";
            
            when(jwtUtils.hashRefreshToken(refreshToken)).thenReturn(hashedToken);
            when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.empty());

            // Act & Assert - should not throw
            assertDoesNotThrow(() -> refreshTokenService.revokeRefreshToken(refreshToken));
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        void testRevokeRefreshToken_tokenFoundButValidationFails_doesNotRevoke() {
            // Arrange
            String refreshToken = "invalid-token";
            String hashedToken = "hashed-token";
            Long userId = 1L;
            RefreshToken tokenEntity = TestUtils.createValidRefreshToken(userId);

            when(jwtUtils.hashRefreshToken(refreshToken)).thenReturn(hashedToken);
            when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(tokenEntity));
            when(jwtUtils.validateRefreshToken(refreshToken, tokenEntity.getTokenHash())).thenReturn(false);

            // Act
            refreshTokenService.revokeRefreshToken(refreshToken);

            // Assert - token should not be revoked since validation failed
            assertFalse(tokenEntity.isRevoked());
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        void testRevokeRefreshToken_repositoryException_propagatesException() {
            // Arrange
            String refreshToken = "valid-token";
            String hashedToken = "hashed-token";
            
            when(jwtUtils.hashRefreshToken(refreshToken)).thenReturn(hashedToken);
            when(refreshTokenRepository.findByTokenHash(hashedToken))
                .thenThrow(new DataAccessException("Database lookup failed") {});

            // Act & Assert
            assertThrows(DataAccessException.class, 
                () -> refreshTokenService.revokeRefreshToken(refreshToken));
        }
    }

    @Nested
    class RevokeAllUserTokensTests {

        @Test
        void testRevokeAllUserTokens_nullUserId_throwsIllegalArgumentException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> refreshTokenService.revokeAllUserTokens(null));
            
            assertEquals("User ID cannot be null", exception.getMessage());
            verify(refreshTokenRepository, never()).revokeAllByUserId(any());
        }

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

        @Test
        void testRevokeAllUserTokens_zeroTokens_returnsZero() {
            // Arrange
            Long userId = 1L;
            when(refreshTokenRepository.revokeAllByUserId(userId)).thenReturn(0);

            // Act
            int result = refreshTokenService.revokeAllUserTokens(userId);

            // Assert
            assertEquals(0, result);
            verify(refreshTokenRepository).revokeAllByUserId(userId);
        }

        @Test
        void testRevokeAllUserTokens_repositoryException_propagatesException() {
            // Arrange
            Long userId = 1L;
            when(refreshTokenRepository.revokeAllByUserId(userId))
                .thenThrow(new DataAccessException("Bulk update failed") {});

            // Act & Assert
            assertThrows(DataAccessException.class, 
                () -> refreshTokenService.revokeAllUserTokens(userId));
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

        @Test
        void testCleanupExpiredTokens_noTokensFound_returnsZero() {
            // Arrange
            when(refreshTokenRepository.deleteExpiredTokens(any(Instant.class))).thenReturn(0);

            // Act
            int result = refreshTokenService.cleanupExpiredTokens();

            // Assert
            assertEquals(0, result);
            verify(refreshTokenRepository).deleteExpiredTokens(any(Instant.class));
        }

        @Test
        void testCleanupExpiredTokens_repositoryException_propagatesException() {
            // Arrange
            when(refreshTokenRepository.deleteExpiredTokens(any(Instant.class)))
                .thenThrow(new DataAccessException("Cleanup failed") {});

            // Act & Assert
            assertThrows(DataAccessException.class, 
                () -> refreshTokenService.cleanupExpiredTokens());
        }
    }

    @Nested
    class CleanupRevokedTokensTests {

        @Test
        void testCleanupRevokedTokens_zeroDays_throwsIllegalArgumentException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> refreshTokenService.cleanupRevokedTokens(0));
            
            assertEquals("Days old must be positive", exception.getMessage());
        }

        @Test
        void testCleanupRevokedTokens_negativeDays_throwsIllegalArgumentException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> refreshTokenService.cleanupRevokedTokens(-5));
            
            assertEquals("Days old must be positive", exception.getMessage());
        }

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

        @Test
        void testCleanupRevokedTokens_veryLargeDaysValue_handlesCorrectly() {
            // Arrange
            int veryLargeDays = Integer.MAX_VALUE / (24 * 60 * 60); // Avoid overflow
            when(refreshTokenRepository.deleteRevokedTokensOlderThan(any(Instant.class))).thenReturn(1);

            // Act
            int result = refreshTokenService.cleanupRevokedTokens(veryLargeDays);

            // Assert
            assertEquals(1, result);
            
            ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(refreshTokenRepository).deleteRevokedTokensOlderThan(cutoffCaptor.capture());
            
            Instant cutoffDate = cutoffCaptor.getValue();
            // Should be far in the past
            assertTrue(cutoffDate.isBefore(Instant.now().minusSeconds(365L * 24 * 60 * 60)), 
                "Cutoff with very large days should be far in the past");
        }

        @Test
        void testCleanupRevokedTokens_noTokensFound_returnsZero() {
            // Arrange
            int daysOld = 30;
            when(refreshTokenRepository.deleteRevokedTokensOlderThan(any(Instant.class))).thenReturn(0);

            // Act
            int result = refreshTokenService.cleanupRevokedTokens(daysOld);

            // Assert
            assertEquals(0, result);
            verify(refreshTokenRepository).deleteRevokedTokensOlderThan(any(Instant.class));
        }

        @Test
        void testCleanupRevokedTokens_repositoryException_propagatesException() {
            // Arrange
            int daysOld = 30;
            when(refreshTokenRepository.deleteRevokedTokensOlderThan(any(Instant.class)))
                .thenThrow(new DataAccessException("Cleanup failed") {});

            // Act & Assert
            assertThrows(DataAccessException.class, 
                () -> refreshTokenService.cleanupRevokedTokens(daysOld));
        }
    }

    @Nested
    class FindTokenByRawValueTests {

        @Test
        void testRefreshTokens_usesOptimizedLookup_avoidsLoadingAllTokens() {
            // Arrange
            String refreshToken = "valid-token";
            Long userId = 1L;
            User user = TestUtils.createValidUserEntityWithId();
            RefreshToken tokenEntity = TestUtils.createValidRefreshToken(userId);
            String hashedToken = "hashed-token";
            String newAccessToken = "new-access-token";
            String newRefreshToken = "new-refresh-token";

            when(jwtUtils.hashRefreshToken(refreshToken)).thenReturn(hashedToken);
            when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(tokenEntity));
            when(jwtUtils.validateRefreshToken(refreshToken, tokenEntity.getTokenHash())).thenReturn(true);
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
            
            // Verify optimized lookup was used instead of findAll()
            verify(refreshTokenRepository).findByTokenHash(hashedToken);
            verify(refreshTokenRepository, never()).findAll();
        }

        @Test
        void testRevokeRefreshToken_usesOptimizedLookup_avoidsLoadingAllTokens() {
            // Arrange
            String refreshToken = "valid-token";
            Long userId = 1L;
            RefreshToken tokenEntity = TestUtils.createValidRefreshToken(userId);
            String hashedToken = "hashed-token";

            when(jwtUtils.hashRefreshToken(refreshToken)).thenReturn(hashedToken);
            when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(tokenEntity));
            when(jwtUtils.validateRefreshToken(refreshToken, tokenEntity.getTokenHash())).thenReturn(true);

            // Act
            refreshTokenService.revokeRefreshToken(refreshToken);

            // Assert
            assertTrue(tokenEntity.isRevoked());
            verify(refreshTokenRepository).save(tokenEntity);
            
            // Verify optimized lookup was used instead of findAll()
            verify(refreshTokenRepository).findByTokenHash(hashedToken);
            verify(refreshTokenRepository, never()).findAll();
        }

        @Test
        void testFindTokenByRawValue_hashFoundButValidationFails_returnsEmpty() {
            // Arrange - simulates hash collision where token is found but validation fails
            String refreshToken = "original-token";
            String hashedToken = "colliding-hash";
            Long userId = 1L;
            RefreshToken foundToken = TestUtils.createValidRefreshToken(userId);

            when(jwtUtils.hashRefreshToken(refreshToken)).thenReturn(hashedToken);
            when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(foundToken));
            when(jwtUtils.validateRefreshToken(refreshToken, foundToken.getTokenHash())).thenReturn(false);

            // Act
            assertThrows(UnauthorizedException.class, 
                () -> refreshTokenService.refreshTokens(refreshToken));

            // Assert - token should not be modified since validation failed  
            assertFalse(foundToken.isRevoked());
            verify(refreshTokenRepository, never()).save(foundToken);
        }

        @Test
        void testRefreshTokens_verifyExactSaveSequence_oldTokenRevokedNewTokenCreated() {
            // Arrange
            String refreshToken = "valid-token";
            String hashedToken = "hashed-token";
            Long userId = 1L;
            User user = TestUtils.createValidUserEntityWithId();
            RefreshToken tokenEntity = TestUtils.createValidRefreshToken(userId);
            String newAccessToken = "new-access-token";
            String newRefreshToken = "new-refresh-token";

            when(jwtUtils.hashRefreshToken(refreshToken)).thenReturn(hashedToken);
            when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(tokenEntity));
            when(jwtUtils.validateRefreshToken(refreshToken, tokenEntity.getTokenHash())).thenReturn(true);
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
            
            // Verify exact save sequence: 1) revoke old token, 2) save new token
            ArgumentCaptor<RefreshToken> saveCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository, times(2)).save(saveCaptor.capture());
            
            var savedTokens = saveCaptor.getAllValues();
            
            // First save should be the revoked old token
            RefreshToken firstSaved = savedTokens.get(0);
            assertEquals(tokenEntity.getUserId(), firstSaved.getUserId());
            assertTrue(firstSaved.isRevoked(), "First save should be revoked old token");
            
            // Second save should be the new token
            RefreshToken secondSaved = savedTokens.get(1);
            assertEquals(userId, secondSaved.getUserId());
            assertFalse(secondSaved.isRevoked(), "Second save should be new unrevoked token");
            assertEquals("hashed-new-token", secondSaved.getTokenHash());
        }

        @Test
        void testServiceDoesNotUseUnintendedRepositoryMethods() {
            // This test ensures our service only uses the intended repository methods
            // and doesn't accidentally call methods that would bypass our optimizations
            
            String refreshToken = "test-token";
            String hashedToken = "hashed-token";
            
            when(jwtUtils.hashRefreshToken(refreshToken)).thenReturn(hashedToken);
            when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.empty());
            
            // Act
            assertThrows(UnauthorizedException.class, 
                () -> refreshTokenService.refreshTokens(refreshToken));
            
            // Assert - verify we don't use inefficient methods
            verify(refreshTokenRepository, never()).findAll();
            verify(refreshTokenRepository, never()).existsByTokenHash(any());
            verify(refreshTokenRepository, never()).countActiveTokensByUserId(any(), any());
            verify(refreshTokenRepository, never()).findByUserIdAndIsRevokedFalse(any());
        }
    }
}