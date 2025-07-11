package com.yohan.event_planner.domain;

import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class PasswordResetTokenTest {

    private User testUser;
    private Instant baseTime;
    private Instant futureTime;
    private Instant pastTime;

    @BeforeEach
    void setUp() {
        testUser = TestUtils.createValidUserEntityWithId();
        baseTime = Instant.parse("2024-01-15T10:00:00Z");
        futureTime = baseTime.plus(30, ChronoUnit.MINUTES);
        pastTime = baseTime.minus(30, ChronoUnit.MINUTES);
    }

    @Nested
    class ConstructorTests {

        @Test
        void defaultConstructor_createsEmptyToken() {
            // Act
            PasswordResetToken token = new PasswordResetToken();

            // Assert
            assertNull(token.getId());
            assertNull(token.getToken());
            assertNull(token.getUser());
            assertNull(token.getCreatedAt());
            assertNull(token.getExpiryDate());
            assertFalse(token.isUsed());
        }

        @Test
        void parameterizedConstructor_setsAllFields() {
            // Arrange
            String tokenString = "test-token-123";

            // Act
            PasswordResetToken token = new PasswordResetToken(tokenString, testUser, baseTime, futureTime);

            // Assert
            assertEquals(tokenString, token.getToken());
            assertEquals(testUser, token.getUser());
            assertEquals(baseTime, token.getCreatedAt());
            assertEquals(futureTime, token.getExpiryDate());
            assertFalse(token.isUsed());
            assertNull(token.getId()); // ID is set by JPA
        }

        @Test
        void parameterizedConstructor_withNullValues_acceptsNulls() {
            // Act
            PasswordResetToken token = new PasswordResetToken(null, null, null, null);

            // Assert
            assertNull(token.getToken());
            assertNull(token.getUser());
            assertNull(token.getCreatedAt());
            assertNull(token.getExpiryDate());
            assertFalse(token.isUsed());
        }
    }

    @Nested
    class IsValidTests {

        @Test
        void isValid_withUnusedAndNotExpiredToken_returnsTrue() {
            // Arrange
            PasswordResetToken token = new PasswordResetToken("token", testUser, pastTime, futureTime);
            Instant currentTime = baseTime;

            // Act
            boolean result = token.isValid(currentTime);

            // Assert
            assertTrue(result);
        }

        @Test
        void isValid_withUsedToken_returnsFalse() {
            // Arrange
            PasswordResetToken token = new PasswordResetToken("token", testUser, pastTime, futureTime);
            token.markAsUsed();
            Instant currentTime = baseTime;

            // Act
            boolean result = token.isValid(currentTime);

            // Assert
            assertFalse(result);
        }

        @Test
        void isValid_withExpiredToken_returnsFalse() {
            // Arrange
            PasswordResetToken token = new PasswordResetToken("token", testUser, pastTime, baseTime);
            Instant currentTime = futureTime; // Current time is after expiry

            // Act
            boolean result = token.isValid(currentTime);

            // Assert
            assertFalse(result);
        }

        @Test
        void isValid_withUsedAndExpiredToken_returnsFalse() {
            // Arrange
            PasswordResetToken token = new PasswordResetToken("token", testUser, pastTime, baseTime);
            token.markAsUsed();
            Instant currentTime = futureTime; // Current time is after expiry

            // Act
            boolean result = token.isValid(currentTime);

            // Assert
            assertFalse(result);
        }

        @Test
        void isValid_atExactExpiryTime_returnsFalse() {
            // Arrange
            PasswordResetToken token = new PasswordResetToken("token", testUser, pastTime, baseTime);
            Instant currentTime = baseTime; // Exactly at expiry time

            // Act
            boolean result = token.isValid(currentTime);

            // Assert
            assertFalse(result);
        }

        @Test
        void isValid_justBeforeExpiry_returnsTrue() {
            // Arrange
            PasswordResetToken token = new PasswordResetToken("token", testUser, pastTime, futureTime);
            Instant currentTime = futureTime.minus(1, ChronoUnit.SECONDS); // Just before expiry

            // Act
            boolean result = token.isValid(currentTime);

            // Assert
            assertTrue(result);
        }

        @Test
        void isValid_justAfterExpiry_returnsFalse() {
            // Arrange
            PasswordResetToken token = new PasswordResetToken("token", testUser, pastTime, baseTime);
            Instant currentTime = baseTime.plus(1, ChronoUnit.SECONDS); // Just after expiry

            // Act
            boolean result = token.isValid(currentTime);

            // Assert
            assertFalse(result);
        }
    }

    @Nested
    class MarkAsUsedTests {

        @Test
        void markAsUsed_setsUsedToTrue() {
            // Arrange
            PasswordResetToken token = new PasswordResetToken("token", testUser, pastTime, futureTime);
            assertFalse(token.isUsed());

            // Act
            token.markAsUsed();

            // Assert
            assertTrue(token.isUsed());
        }

        @Test
        void markAsUsed_whenAlreadyUsed_remainsTrue() {
            // Arrange
            PasswordResetToken token = new PasswordResetToken("token", testUser, pastTime, futureTime);
            token.markAsUsed();
            assertTrue(token.isUsed());

            // Act
            token.markAsUsed();

            // Assert
            assertTrue(token.isUsed());
        }

        @Test
        void markAsUsed_invalidatesToken() {
            // Arrange
            PasswordResetToken token = new PasswordResetToken("token", testUser, pastTime, futureTime);
            assertTrue(token.isValid(baseTime));

            // Act
            token.markAsUsed();

            // Assert
            assertFalse(token.isValid(baseTime));
        }
    }

    @Nested
    class GettersAndSettersTests {

        @Test
        void gettersAndSetters_workCorrectly() {
            // Arrange
            PasswordResetToken token = new PasswordResetToken();
            Long id = 123L;
            String tokenString = "test-token";

            // Act & Assert
            token.setId(id);
            assertEquals(id, token.getId());

            token.setToken(tokenString);
            assertEquals(tokenString, token.getToken());

            token.setUser(testUser);
            assertEquals(testUser, token.getUser());

            token.setCreatedAt(baseTime);
            assertEquals(baseTime, token.getCreatedAt());

            token.setExpiryDate(futureTime);
            assertEquals(futureTime, token.getExpiryDate());

            token.setUsed(true);
            assertTrue(token.isUsed());

            token.setUsed(false);
            assertFalse(token.isUsed());
        }

        @Test
        void setters_acceptNullValues() {
            // Arrange
            PasswordResetToken token = new PasswordResetToken("token", testUser, baseTime, futureTime);

            // Act & Assert
            assertDoesNotThrow(() -> {
                token.setId(null);
                token.setToken(null);
                token.setUser(null);
                token.setCreatedAt(null);
                token.setExpiryDate(null);
            });

            assertNull(token.getId());
            assertNull(token.getToken());
            assertNull(token.getUser());
            assertNull(token.getCreatedAt());
            assertNull(token.getExpiryDate());
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        void isValid_withNullExpiryDate_throwsNullPointerException() {
            // Arrange
            PasswordResetToken token = new PasswordResetToken();
            token.setExpiryDate(null);

            // Act & Assert
            assertThrows(NullPointerException.class, () -> token.isValid(baseTime));
        }

        @Test
        void isValid_withNullCurrentTime_throwsNullPointerException() {
            // Arrange
            PasswordResetToken token = new PasswordResetToken("token", testUser, pastTime, futureTime);

            // Act & Assert
            assertThrows(NullPointerException.class, () -> token.isValid(null));
        }

        @Test
        void token_withVeryLongString_handlesCorrectly() {
            // Arrange
            String longToken = "a".repeat(128); // Maximum allowed length
            PasswordResetToken token = new PasswordResetToken(longToken, testUser, baseTime, futureTime);

            // Act & Assert
            assertEquals(longToken, token.getToken());
            assertTrue(token.isValid(baseTime));
        }

        @Test
        void token_withEmptyString_handlesCorrectly() {
            // Arrange
            String emptyToken = "";
            PasswordResetToken token = new PasswordResetToken(emptyToken, testUser, baseTime, futureTime);

            // Act & Assert
            assertEquals(emptyToken, token.getToken());
            assertTrue(token.isValid(baseTime));
        }

        @Test
        void createdAt_afterExpiryDate_stillValidatesCorrectly() {
            // Arrange - Created after expiry (unusual but possible due to clock skew)
            PasswordResetToken token = new PasswordResetToken("token", testUser, futureTime, baseTime);

            // Act & Assert
            assertFalse(token.isValid(baseTime)); // Should be invalid due to expiry
            assertTrue(token.isValid(pastTime)); // Should be valid before expiry
        }
    }
}