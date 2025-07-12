package com.yohan.event_planner.domain;

import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    private static final Long USER_ID = 1L;
    private static final String TOKEN_HASH = "hashed-token";
    private Clock clock;
    private Instant futureExpiry;
    private Instant pastExpiry;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneOffset.UTC);
        futureExpiry = clock.instant().plusSeconds(3600); // 1 hour from now
        pastExpiry = clock.instant().minusSeconds(3600); // 1 hour ago
    }

    @Nested
    class Construction {

        @Test
        void constructor_shouldSetPropertiesCorrectly() {
            RefreshToken token = new RefreshToken(TOKEN_HASH, USER_ID, futureExpiry);

            assertThat(token.getTokenHash()).isEqualTo(TOKEN_HASH);
            assertThat(token.getUserId()).isEqualTo(USER_ID);
            assertThat(token.getExpiryDate()).isEqualTo(futureExpiry);
        }

        @Test
        void constructor_shouldInitializeDefaultValues() {
            RefreshToken token = new RefreshToken(TOKEN_HASH, USER_ID, futureExpiry);

            assertThat(token.isRevoked()).isFalse();
            assertThat(token.getId()).isNull(); // Not persisted yet
            assertThat(token.getCreatedAt()).isNull(); // Set by @CreationTimestamp
        }

        @Test
        void defaultConstructor_shouldCreateEmptyToken() {
            RefreshToken token = new RefreshToken();

            assertThat(token.getTokenHash()).isNull();
            assertThat(token.getUserId()).isNull();
            assertThat(token.getExpiryDate()).isNull();
            assertThat(token.isRevoked()).isFalse();
        }
    }

    @Nested
    class ExpirationLogic {

        @Test
        void isExpired_withFutureExpiryDate_shouldReturnFalse() {
            // Create token with future expiry that's safely in the future
            Instant farFutureExpiry = Instant.now().plusSeconds(86400); // 24 hours
            RefreshToken token = new RefreshToken(TOKEN_HASH, USER_ID, farFutureExpiry);

            assertThat(token.isExpired()).isFalse();
        }

        @Test
        void isExpired_withPastExpiryDate_shouldReturnTrue() {
            RefreshToken token = TestUtils.createExpiredRefreshToken(USER_ID, clock);

            assertThat(token.isExpired()).isTrue();
        }

        @Test
        void isExpired_withExactCurrentTime_shouldReturnTrue() {
            // Create token that expires exactly now
            RefreshToken token = new RefreshToken(TOKEN_HASH, USER_ID, Instant.now());
            
            // Since isExpired() uses Instant.now() and we can't control it precisely,
            // we need to wait a tiny bit to ensure it's actually past
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            assertThat(token.isExpired()).isTrue();
        }

        @Test
        void isExpired_shouldUseCurrentTime() {
            // This test verifies that isExpired() uses Instant.now() internally
            Instant nowPlus1Hour = Instant.now().plusSeconds(3600);
            RefreshToken token = new RefreshToken(TOKEN_HASH, USER_ID, nowPlus1Hour);

            assertThat(token.isExpired()).isFalse();
        }
    }

    @Nested
    class ValidationLogic {

        @Test
        void isValid_withNotRevokedAndNotExpired_shouldReturnTrue() {
            // Create token with future expiry that's safely in the future
            Instant farFutureExpiry = Instant.now().plusSeconds(86400); // 24 hours
            RefreshToken token = new RefreshToken(TOKEN_HASH, USER_ID, farFutureExpiry);

            assertThat(token.isValid()).isTrue();
        }

        @Test
        void isValid_withRevokedToken_shouldReturnFalse() {
            RefreshToken token = TestUtils.createRevokedRefreshToken(USER_ID, clock);

            assertThat(token.isValid()).isFalse();
        }

        @Test
        void isValid_withExpiredToken_shouldReturnFalse() {
            RefreshToken token = TestUtils.createExpiredRefreshToken(USER_ID, clock);

            assertThat(token.isValid()).isFalse();
        }

        @Test
        void isValid_withRevokedAndExpiredToken_shouldReturnFalse() {
            RefreshToken token = new RefreshToken(TOKEN_HASH, USER_ID, pastExpiry);
            token.setRevoked(true);

            assertThat(token.isValid()).isFalse();
        }

        @Test
        void isValid_shouldCombineRevokedAndExpiredChecks() {
            Instant farFutureExpiry = Instant.now().plusSeconds(86400); // 24 hours
            RefreshToken token = new RefreshToken(TOKEN_HASH, USER_ID, farFutureExpiry);
            
            // Initially valid
            assertThat(token.isValid()).isTrue();
            
            // Revoke it
            token.setRevoked(true);
            assertThat(token.isValid()).isFalse();
            
            // Un-revoke but make it expired
            token.setRevoked(false);
            token.setExpiryDate(pastExpiry);
            assertThat(token.isValid()).isFalse();
        }
    }

    @Nested
    class StateManagement {

        @Test
        void setRevoked_shouldUpdateRevokedStatus() {
            RefreshToken token = TestUtils.createValidRefreshToken(USER_ID);

            token.setRevoked(true);

            assertThat(token.isRevoked()).isTrue();
        }

        @Test
        void setRevoked_shouldNotAffectExpiry() {
            RefreshToken token = TestUtils.createValidRefreshToken(USER_ID, clock);
            Instant originalExpiry = token.getExpiryDate();

            token.setRevoked(true);

            assertThat(token.getExpiryDate()).isEqualTo(originalExpiry);
        }

        @Test
        void setExpiryDate_shouldUpdateExpiryDate() {
            RefreshToken token = TestUtils.createValidRefreshToken(USER_ID);
            Instant newExpiry = Instant.now().plusSeconds(7200); // 2 hours

            token.setExpiryDate(newExpiry);

            assertThat(token.getExpiryDate()).isEqualTo(newExpiry);
        }

        @Test
        void setExpiryDate_shouldNotAffectRevokedStatus() {
            RefreshToken token = TestUtils.createRevokedRefreshToken(USER_ID);

            token.setExpiryDate(futureExpiry);

            assertThat(token.isRevoked()).isTrue();
        }
    }

    @Nested
    class EqualityAndHashing {

        @Test
        void equals_withSameId_shouldReturnTrue() {
            RefreshToken token1 = TestUtils.createValidRefreshToken(USER_ID);
            RefreshToken token2 = TestUtils.createValidRefreshToken(USER_ID + 1);
            
            setRefreshTokenId(token1, 1L);
            setRefreshTokenId(token2, 1L);

            assertThat(token1).isEqualTo(token2);
        }

        @Test
        void equals_withDifferentIds_shouldReturnFalse() {
            RefreshToken token1 = TestUtils.createValidRefreshToken(USER_ID);
            RefreshToken token2 = TestUtils.createValidRefreshToken(USER_ID);
            
            setRefreshTokenId(token1, 1L);
            setRefreshTokenId(token2, 2L);

            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        void equals_withNullIds_shouldReturnFalse() {
            Instant futureExpiry1 = Instant.now().plusSeconds(3600);
            Instant futureExpiry2 = Instant.now().plusSeconds(7200); // Different expiry
            RefreshToken token1 = new RefreshToken(TOKEN_HASH, USER_ID, futureExpiry1);
            RefreshToken token2 = new RefreshToken(TOKEN_HASH, USER_ID, futureExpiry2);

            // Without IDs, they should not be equal (ID-only equality)
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        void equals_withSelf_shouldReturnTrue() {
            RefreshToken token = TestUtils.createValidRefreshToken(USER_ID);

            assertThat(token).isEqualTo(token);
        }

        @Test
        void equals_withNull_shouldReturnFalse() {
            RefreshToken token = TestUtils.createValidRefreshToken(USER_ID);

            assertThat(token).isNotEqualTo(null);
        }

        @Test
        void equals_withDifferentClass_shouldReturnFalse() {
            RefreshToken token = TestUtils.createValidRefreshToken(USER_ID);

            assertThat(token).isNotEqualTo("not a refresh token");
        }

        @Test
        void hashCode_withId_shouldUseIdHashCode() {
            RefreshToken token = TestUtils.createValidRefreshToken(USER_ID);
            setRefreshTokenId(token, 1L);

            assertThat(token.hashCode()).isEqualTo(Long.valueOf(1L).hashCode());
        }

        @Test
        void hashCode_withNullId_shouldReturnZero() {
            Instant futureExpiry = Instant.now().plusSeconds(3600);
            RefreshToken token = new RefreshToken(TOKEN_HASH, USER_ID, futureExpiry);

            assertThat(token.hashCode()).isZero();
        }

        @Test
        void hashCode_shouldBeConsistentWithEquals() {
            RefreshToken token1 = TestUtils.createValidRefreshToken(USER_ID);
            RefreshToken token2 = TestUtils.createValidRefreshToken(USER_ID + 1);
            
            setRefreshTokenId(token1, 1L);
            setRefreshTokenId(token2, 1L);

            assertThat(token1.hashCode()).isEqualTo(token2.hashCode());
        }
    }

    @Nested
    class TimeBoundaryTests {

        @Test
        void isExpired_withExpiryJustInFuture_shouldReturnFalse() {
            Instant justInFuture = Instant.now().plusMillis(100);
            RefreshToken token = new RefreshToken(TOKEN_HASH, USER_ID, justInFuture);

            assertThat(token.isExpired()).isFalse();
        }

        @Test
        void isExpired_withExpiryJustInPast_shouldReturnTrue() {
            Instant justInPast = Instant.now().minusMillis(100);
            RefreshToken token = new RefreshToken(TOKEN_HASH, USER_ID, justInPast);

            assertThat(token.isExpired()).isTrue();
        }

        @Test
        void validation_shouldHandleEdgeCases() {
            // Token that's valid but expires very soon
            Instant soonExpiry = Instant.now().plusMillis(10);
            RefreshToken token = new RefreshToken(TOKEN_HASH, USER_ID, soonExpiry);

            assertThat(token.isValid()).isTrue();
            
            // Wait for expiry
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            assertThat(token.isValid()).isFalse();
        }
    }

    @Nested
    class PropertyAccessors {

        @Test
        void setters_shouldUpdateProperties() {
            RefreshToken token = new RefreshToken();
            User user = TestUtils.createValidUserEntity();
            Instant now = Instant.now();

            token.setId(1L);
            token.setTokenHash("new-hash");
            token.setUserId(999L);
            token.setExpiryDate(futureExpiry);
            token.setCreatedAt(now);
            token.setRevoked(true);
            token.setUser(user);

            assertThat(token.getId()).isEqualTo(1L);
            assertThat(token.getTokenHash()).isEqualTo("new-hash");
            assertThat(token.getUserId()).isEqualTo(999L);
            assertThat(token.getExpiryDate()).isEqualTo(futureExpiry);
            assertThat(token.getCreatedAt()).isEqualTo(now);
            assertThat(token.isRevoked()).isTrue();
            assertThat(token.getUser()).isEqualTo(user);
        }
    }

    // Helper method using reflection
    private void setRefreshTokenId(RefreshToken token, Long id) {
        try {
            var field = RefreshToken.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(token, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set refresh token ID", e);
        }
    }
}