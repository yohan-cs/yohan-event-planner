package com.yohan.event_planner.domain;

import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationTokenTest {

    private static final String TOKEN_STRING = "secure-verification-token-123";
    private User user;
    private Clock clock;
    private Instant currentTime;
    private Instant futureExpiry;
    private Instant pastExpiry;

    @BeforeEach
    void setUp() {
        user = TestUtils.createValidUserEntity();
        clock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneOffset.UTC);
        currentTime = clock.instant();
        futureExpiry = currentTime.plusSeconds(86400); // 24 hours later
        pastExpiry = currentTime.minusSeconds(3600); // 1 hour ago
    }

    @Nested
    class Construction {

        @Test
        void constructor_shouldSetAllProperties() {
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, futureExpiry
            );

            assertThat(token.getToken()).isEqualTo(TOKEN_STRING);
            assertThat(token.getUser()).isEqualTo(user);
            assertThat(token.getCreatedAt()).isEqualTo(currentTime);
            assertThat(token.getExpiryDate()).isEqualTo(futureExpiry);
            assertThat(token.isUsed()).isFalse();
        }

        @Test
        void constructor_shouldInitializeUsedAsFalse() {
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, futureExpiry
            );

            assertThat(token.isUsed()).isFalse();
        }

        @Test
        void defaultConstructor_shouldCreateEmptyToken() {
            EmailVerificationToken token = new EmailVerificationToken();

            assertThat(token.getToken()).isNull();
            assertThat(token.getUser()).isNull();
            assertThat(token.getCreatedAt()).isNull();
            assertThat(token.getExpiryDate()).isNull();
            assertThat(token.isUsed()).isFalse();
        }
    }

    @Nested
    class ValidationLogic {

        @Test
        void isValid_withNotUsedAndNotExpired_shouldReturnTrue() {
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, futureExpiry
            );

            assertThat(token.isValid(currentTime)).isTrue();
        }

        @Test
        void isValid_withUsedToken_shouldReturnFalse() {
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, futureExpiry
            );
            token.markAsUsed();

            assertThat(token.isValid(currentTime)).isFalse();
        }

        @Test
        void isValid_withExpiredToken_shouldReturnFalse() {
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, pastExpiry
            );

            assertThat(token.isValid(currentTime)).isFalse();
        }

        @Test
        void isValid_withUsedAndExpiredToken_shouldReturnFalse() {
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, pastExpiry
            );
            token.markAsUsed();

            assertThat(token.isValid(currentTime)).isFalse();
        }

        @Test
        void isValid_withFutureCheckTime_shouldReturnTrue() {
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, futureExpiry
            );
            Instant futureCheckTime = currentTime.plusSeconds(3600); // 1 hour later, but still before expiry

            assertThat(token.isValid(futureCheckTime)).isTrue();
        }

        @Test
        void isValid_withCheckTimeAtExpiry_shouldReturnFalse() {
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, futureExpiry
            );

            // Check time exactly at expiry should return false (not before expiry)
            assertThat(token.isValid(futureExpiry)).isFalse();
        }

        @Test
        void isValid_withCheckTimeJustBeforeExpiry_shouldReturnTrue() {
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, futureExpiry
            );
            Instant justBeforeExpiry = futureExpiry.minusSeconds(1);

            assertThat(token.isValid(justBeforeExpiry)).isTrue();
        }
    }

    @Nested
    class StateManagement {

        @Test
        void markAsUsed_shouldSetUsedToTrue() {
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, futureExpiry
            );

            token.markAsUsed();

            assertThat(token.isUsed()).isTrue();
        }

        @Test
        void markAsUsed_shouldNotAffectExpiry() {
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, futureExpiry
            );

            token.markAsUsed();

            assertThat(token.getExpiryDate()).isEqualTo(futureExpiry);
        }

        @Test
        void markAsUsed_shouldMakeTokenInvalid() {
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, futureExpiry
            );

            // Initially valid
            assertThat(token.isValid(currentTime)).isTrue();

            token.markAsUsed();

            // Now invalid
            assertThat(token.isValid(currentTime)).isFalse();
        }

        @Test
        void setUsed_shouldUpdateUsedFlag() {
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, futureExpiry
            );

            token.setUsed(true);

            assertThat(token.isUsed()).isTrue();
        }

        @Test
        void setUsed_shouldAllowResettingToFalse() {
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, futureExpiry
            );
            token.markAsUsed();

            token.setUsed(false);

            assertThat(token.isUsed()).isFalse();
            assertThat(token.isValid(currentTime)).isTrue();
        }
    }

    @Nested
    class PropertyAccessors {

        @Test
        void setters_shouldUpdateProperties() {
            EmailVerificationToken token = new EmailVerificationToken();
            User newUser = TestUtils.createTestUser("newuser");
            Instant newTime = currentTime.plusSeconds(1000);

            token.setId(1L);
            token.setToken("new-token");
            token.setUser(newUser);
            token.setCreatedAt(currentTime);
            token.setExpiryDate(newTime);
            token.setUsed(true);

            assertThat(token.getId()).isEqualTo(1L);
            assertThat(token.getToken()).isEqualTo("new-token");
            assertThat(token.getUser()).isEqualTo(newUser);
            assertThat(token.getCreatedAt()).isEqualTo(currentTime);
            assertThat(token.getExpiryDate()).isEqualTo(newTime);
            assertThat(token.isUsed()).isTrue();
        }

        @Test
        void setExpiryDate_shouldAffectValidation() {
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, futureExpiry
            );

            // Initially valid
            assertThat(token.isValid(currentTime)).isTrue();

            // Change to past expiry
            token.setExpiryDate(pastExpiry);
            assertThat(token.isValid(currentTime)).isFalse();

            // Change back to future expiry
            token.setExpiryDate(futureExpiry);
            assertThat(token.isValid(currentTime)).isTrue();
        }
    }

    @Nested
    class TimeBoundaryTests {

        @Test
        void isValid_withExpiryJustInFuture_shouldReturnTrue() {
            Instant justInFuture = currentTime.plusMillis(100);
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, justInFuture
            );

            assertThat(token.isValid(currentTime)).isTrue();
        }

        @Test
        void isValid_withExpiryJustInPast_shouldReturnFalse() {
            Instant justInPast = currentTime.minusMillis(100);
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, justInPast
            );

            assertThat(token.isValid(currentTime)).isFalse();
        }

        @Test
        void isValid_withVeryLongExpiry_shouldReturnTrue() {
            Instant veryFutureExpiry = currentTime.plusSeconds(365 * 24 * 3600); // 1 year
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, veryFutureExpiry
            );

            assertThat(token.isValid(currentTime)).isTrue();
        }

        @Test
        void validation_shouldHandleEdgeCases() {
            // Token that expires very soon
            Instant soonExpiry = currentTime.plusMillis(10);
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, soonExpiry
            );

            assertThat(token.isValid(currentTime)).isTrue();
            assertThat(token.isValid(soonExpiry)).isFalse();
        }
    }

    @Nested
    class SecurityBehavior {

        @Test
        void token_onceUsed_shouldRemainUsed() {
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, futureExpiry
            );

            token.markAsUsed();
            boolean firstCheck = token.isUsed();
            boolean secondCheck = token.isUsed();

            assertThat(firstCheck).isTrue();
            assertThat(secondCheck).isTrue();
        }

        @Test
        void multipleMarkAsUsed_shouldNotChangeState() {
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, futureExpiry
            );

            token.markAsUsed();
            token.markAsUsed();
            token.markAsUsed();

            assertThat(token.isUsed()).isTrue();
        }

        @Test
        void validToken_afterUse_shouldBecomeInvalid() {
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, futureExpiry
            );

            // Verify initial state
            assertThat(token.isValid(currentTime)).isTrue();
            assertThat(token.isUsed()).isFalse();

            // Use the token
            token.markAsUsed();

            // Verify final state
            assertThat(token.isValid(currentTime)).isFalse();
            assertThat(token.isUsed()).isTrue();
        }
    }

    @Nested
    class DefaultValues {

        @Test
        void newToken_shouldHaveCorrectDefaults() {
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, futureExpiry
            );

            assertThat(token.getId()).isNull(); // Not persisted yet
            assertThat(token.isUsed()).isFalse(); // Default to unused
        }

        @Test
        void emptyToken_shouldHaveCorrectDefaults() {
            EmailVerificationToken token = new EmailVerificationToken();

            assertThat(token.getId()).isNull();
            assertThat(token.getToken()).isNull();
            assertThat(token.getUser()).isNull();
            assertThat(token.getCreatedAt()).isNull();
            assertThat(token.getExpiryDate()).isNull();
            assertThat(token.isUsed()).isFalse(); // Default to unused
        }
    }

    @Nested
    class LifecycleScenarios {

        @Test
        void normalLifecycle_shouldWorkCorrectly() {
            // 1. Token created
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, futureExpiry
            );
            assertThat(token.isValid(currentTime)).isTrue();
            assertThat(token.isUsed()).isFalse();

            // 2. Token is used for verification
            token.markAsUsed();
            assertThat(token.isValid(currentTime)).isFalse();
            assertThat(token.isUsed()).isTrue();
        }

        @Test
        void expiredToken_shouldNotBeUsable() {
            // Token created but expires in the past
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, pastExpiry
            );

            // Should be invalid due to expiry
            assertThat(token.isValid(currentTime)).isFalse();
            assertThat(token.isUsed()).isFalse();

            // Even marking as used doesn't change validity (already expired)
            token.markAsUsed();
            assertThat(token.isValid(currentTime)).isFalse();
            assertThat(token.isUsed()).isTrue();
        }

        @Test
        void tokenValidation_atDifferentTimes() {
            EmailVerificationToken token = new EmailVerificationToken(
                TOKEN_STRING, user, currentTime, futureExpiry
            );

            // Valid at creation time
            assertThat(token.isValid(currentTime)).isTrue();

            // Valid before expiry
            Instant beforeExpiry = futureExpiry.minusSeconds(1);
            assertThat(token.isValid(beforeExpiry)).isTrue();

            // Invalid at expiry
            assertThat(token.isValid(futureExpiry)).isFalse();

            // Invalid after expiry
            Instant afterExpiry = futureExpiry.plusSeconds(1);
            assertThat(token.isValid(afterExpiry)).isFalse();
        }
    }
}