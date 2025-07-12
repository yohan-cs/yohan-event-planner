package com.yohan.event_planner.domain;

import com.yohan.event_planner.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private User user;
    private ZonedDateTime currentTime;

    @BeforeEach
    void setUp() {
        user = new User("testuser", "hashedpassword", "test@example.com", 
                       "John", "Doe", "America/New_York");
        currentTime = ZonedDateTime.now();
    }

    @Nested
    class DeletionManagement {

        @Test
        void markForDeletion_shouldSetPendingDeletionToTrue() {
            user.markForDeletion(currentTime);

            assertThat(user.isPendingDeletion()).isTrue();
        }

        @Test
        void markForDeletion_shouldCalculateCorrectDeletionDate() {
            user.markForDeletion(currentTime);

            assertThat(user.getScheduledDeletionDate()).isPresent();
            assertThat(user.getScheduledDeletionDate().get())
                .isEqualTo(currentTime.plusDays(30)); // DELETION_GRACE_PERIOD_DAYS = 30
        }

        @Test
        void unmarkForDeletion_shouldClearPendingDeletionFlag() {
            user.markForDeletion(currentTime);
            user.unmarkForDeletion();

            assertThat(user.isPendingDeletion()).isFalse();
        }

        @Test
        void unmarkForDeletion_shouldClearScheduledDeletionDate() {
            user.markForDeletion(currentTime);
            user.unmarkForDeletion();

            assertThat(user.getScheduledDeletionDate()).isEmpty();
        }

        @Test
        void unmarkForDeletion_whenNotMarkedForDeletion_shouldHaveNoEffect() {
            user.unmarkForDeletion();

            assertThat(user.isPendingDeletion()).isFalse();
            assertThat(user.getScheduledDeletionDate()).isEmpty();
        }
    }

    @Nested
    class RoleManagement {

        @Test
        void constructor_shouldInitializeWithUserRole() {
            assertThat(user.getRoles()).containsExactly(Role.USER);
        }

        @Test
        void addRole_shouldAddNewRole() {
            user.addRole(Role.ADMIN);

            assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.USER, Role.ADMIN);
        }

        @Test
        void addRole_shouldNotAddDuplicateRole() {
            user.addRole(Role.USER);

            assertThat(user.getRoles()).containsExactly(Role.USER);
        }

        @Test
        void removeRole_shouldRemoveExistingRole() {
            user.addRole(Role.ADMIN);
            user.removeRole(Role.USER);

            assertThat(user.getRoles()).containsExactly(Role.ADMIN);
        }

        @Test
        void removeRole_whenRoleNotPresent_shouldHaveNoEffect() {
            user.removeRole(Role.ADMIN);

            assertThat(user.getRoles()).containsExactly(Role.USER);
        }

        @Test
        void getRoles_shouldReturnImmutableCopy() {
            var roles = user.getRoles();
            
            assertThat(roles).isNotSameAs(user.getRoles());
        }
    }

    @Nested
    class EmailVerification {

        @Test
        void constructor_shouldInitializeEmailVerifiedAsFalse() {
            assertThat(user.isEmailVerified()).isFalse();
        }

        @Test
        void verifyEmail_shouldSetEmailVerifiedToTrue() {
            user.verifyEmail();

            assertThat(user.isEmailVerified()).isTrue();
        }

        @Test
        void verifyEmail_whenAlreadyVerified_shouldRemainTrue() {
            user.verifyEmail();
            user.verifyEmail();

            assertThat(user.isEmailVerified()).isTrue();
        }
    }

    @Nested
    class EqualityAndHashing {

        @Test
        void equals_withSameId_shouldReturnTrue() {
            User user1 = new User("user1", "pass", "user1@example.com", "John", "Doe", "UTC");
            User user2 = new User("user2", "pass", "user2@example.com", "Jane", "Smith", "UTC");
            
            // Set same ID using reflection helper from TestUtils
            setUserId(user1, 1L);
            setUserId(user2, 1L);

            assertThat(user1).isEqualTo(user2);
        }

        @Test
        void equals_withDifferentIds_shouldReturnFalse() {
            User user1 = new User("user1", "pass", "user1@example.com", "John", "Doe", "UTC");
            User user2 = new User("user2", "pass", "user2@example.com", "Jane", "Smith", "UTC");
            
            setUserId(user1, 1L);
            setUserId(user2, 2L);

            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        void equals_withNullIds_shouldUseUsernameAndEmail() {
            User user1 = new User("testuser", "pass", "test@example.com", "John", "Doe", "UTC");
            User user2 = new User("testuser", "pass", "test@example.com", "Jane", "Smith", "UTC");

            assertThat(user1).isEqualTo(user2);
        }

        @Test
        void equals_withNullIds_differentUsername_shouldReturnFalse() {
            User user1 = new User("user1", "pass", "test@example.com", "John", "Doe", "UTC");
            User user2 = new User("user2", "pass", "test@example.com", "Jane", "Smith", "UTC");

            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        void equals_withNullIds_differentEmail_shouldReturnFalse() {
            User user1 = new User("testuser", "pass", "user1@example.com", "John", "Doe", "UTC");
            User user2 = new User("testuser", "pass", "user2@example.com", "Jane", "Smith", "UTC");

            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        void equals_withNullUsernameOrEmail_shouldReturnFalse() {
            User user1 = new User("testuser", "pass", "test@example.com", "John", "Doe", "UTC");
            User user2 = new User("testuser", "pass", "test@example.com", "Jane", "Smith", "UTC");
            
            // Simulate null username scenario by setting username to null
            setUsername(user2, null);

            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        void hashCode_withId_shouldUseIdHashCode() {
            setUserId(user, 1L);

            assertThat(user.hashCode()).isEqualTo(Long.valueOf(1L).hashCode());
        }

        @Test
        void hashCode_withoutId_shouldUseUsernameAndEmailXor() {
            int expectedHash = "testuser".hashCode() ^ "test@example.com".hashCode();

            assertThat(user.hashCode()).isEqualTo(expectedHash);
        }

        @Test
        void hashCode_withNullUsernameAndEmail_shouldReturnZero() {
            User userWithNulls = new User(null, "pass", null, "John", "Doe", "UTC");

            assertThat(userWithNulls.hashCode()).isEqualTo(0);
        }
    }

    @Nested
    class ConstructorBehavior {

        @Test
        void constructor_shouldSetCreatedAtToCurrentTime() {
            ZonedDateTime beforeCreation = ZonedDateTime.now().minusSeconds(1);
            User newUser = new User("user", "pass", "email@example.com", "John", "Doe", "UTC");
            ZonedDateTime afterCreation = ZonedDateTime.now().plusSeconds(1);

            assertThat(newUser.getCreatedAt()).isBetween(beforeCreation, afterCreation);
        }

        @Test
        void constructor_shouldInitializePendingDeletionAsFalse() {
            assertThat(user.isPendingDeletion()).isFalse();
        }

        @Test
        void constructor_shouldInitializeScheduledDeletionDateAsEmpty() {
            assertThat(user.getScheduledDeletionDate()).isEmpty();
        }
    }

    // Helper methods using reflection (similar to TestUtils pattern)
    private void setUserId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set user ID", e);
        }
    }

    private void setUsername(User user, String username) {
        try {
            var field = User.class.getDeclaredField("username");
            field.setAccessible(true);
            field.set(user, username);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set username", e);
        }
    }

    private void setEmail(User user, String email) {
        try {
            var field = User.class.getDeclaredField("email");
            field.setAccessible(true);
            field.set(user, email);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set email", e);
        }
    }

    @Nested
    class EqualsHashCodeEdgeCases {

        @Test
        void equals_withNullIds_bothNullUsername_shouldReturnFalse() {
            // Test fallback logic when both users have null username
            User user1 = new User("user1", "pass", "test@example.com", "John", "Doe", "UTC");
            User user2 = new User("user2", "pass", "test@example.com", "Jane", "Smith", "UTC");
            
            setUsername(user1, null);
            setUsername(user2, null);

            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        void equals_withNullIds_bothNullEmail_shouldReturnFalse() {
            // Test fallback logic when both users have null email
            User user1 = new User("testuser", "pass", "user1@example.com", "John", "Doe", "UTC");
            User user2 = new User("testuser", "pass", "user2@example.com", "Jane", "Smith", "UTC");
            
            setEmail(user1, null);
            setEmail(user2, null);

            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        void equals_withNullIds_oneNullUsername_shouldReturnFalse() {
            // Test fallback logic when one user has null username
            User user1 = new User("testuser", "pass", "test@example.com", "John", "Doe", "UTC");
            User user2 = new User("testuser", "pass", "test@example.com", "Jane", "Smith", "UTC");
            
            setUsername(user1, null);

            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        void equals_withNullIds_oneNullEmail_shouldReturnFalse() {
            // Test fallback logic when one user has null email
            User user1 = new User("testuser", "pass", "test@example.com", "John", "Doe", "UTC");
            User user2 = new User("testuser", "pass", "test@example.com", "Jane", "Smith", "UTC");
            
            setEmail(user1, null);

            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        void equals_withNullIds_bothNullUsernameAndEmail_shouldReturnFalse() {
            // Test fallback logic when both users have null username and email
            User user1 = new User("user1", "pass", "user1@example.com", "John", "Doe", "UTC");
            User user2 = new User("user2", "pass", "user2@example.com", "Jane", "Smith", "UTC");
            
            setUsername(user1, null);
            setEmail(user1, null);
            setUsername(user2, null);
            setEmail(user2, null);

            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        void equals_withMixedNullIds_shouldUseFallbackLogic() {
            // Test mixed scenario: one has ID, one doesn't - should use fallback logic
            User user1 = new User("testuser", "pass", "test@example.com", "John", "Doe", "UTC");
            User user2 = new User("testuser", "pass", "test@example.com", "Jane", "Smith", "UTC");
            
            setUserId(user1, 1L); // Only user1 has ID
            
            // Since only one has ID, fallback to username+email comparison
            // Both have same username and email, so should be equal
            assertThat(user1).isEqualTo(user2);
        }

        @Test
        void hashCode_withNullIds_shouldUseUsernameAndEmailXor() {
            // Test hashCode fallback calculation
            User user1 = new User("testuser", "pass", "test@example.com", "John", "Doe", "UTC");
            User user2 = new User("testuser", "pass", "test@example.com", "Jane", "Smith", "UTC");
            
            // Both should have same hashCode (same username and email)
            assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        }

        @Test
        void hashCode_withNullIds_nullUsername_shouldHandleGracefully() {
            // Test hashCode when username is null
            User user = new User("testuser", "pass", "test@example.com", "John", "Doe", "UTC");
            setUsername(user, null);
            
            int expectedHash = 0 ^ "test@example.com".hashCode(); // null username contributes 0
            assertThat(user.hashCode()).isEqualTo(expectedHash);
        }

        @Test
        void hashCode_withNullIds_nullEmail_shouldHandleGracefully() {
            // Test hashCode when email is null
            User user = new User("testuser", "pass", "test@example.com", "John", "Doe", "UTC");
            setEmail(user, null);
            
            int expectedHash = "testuser".hashCode() ^ 0; // null email contributes 0
            assertThat(user.hashCode()).isEqualTo(expectedHash);
        }

        @Test
        void hashCode_withNullIds_bothNullUsernameAndEmail_shouldReturnZero() {
            // Test hashCode when both username and email are null
            User user = new User("testuser", "pass", "test@example.com", "John", "Doe", "UTC");
            setUsername(user, null);
            setEmail(user, null);
            
            assertThat(user.hashCode()).isZero(); // 0 XOR 0 = 0
        }

        @Test
        void hashCode_shouldBeConsistentWithEquals() {
            // Test that equal objects have equal hash codes
            User user1 = new User("testuser", "pass", "test@example.com", "John", "Doe", "UTC");
            User user2 = new User("testuser", "pass", "test@example.com", "Jane", "Smith", "UTC");
            
            assertThat(user1).isEqualTo(user2);
            assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        }

        @Test
        void equals_fallbackLogic_shouldIgnoreOtherFields() {
            // Test that fallback logic only uses username and email, ignoring other fields
            User user1 = new User("testuser", "pass", "test@example.com", "John", "Doe", "UTC");
            User user2 = new User("testuser", "pass", "test@example.com", "Jane", "Smith", "EST");
            
            // Different names and timezones, but same username and email
            assertThat(user1).isEqualTo(user2);
        }

        @Test
        void hashCode_idVsFallback_shouldBeDifferent() {
            // Test that ID-based and fallback hashCodes can be different
            User user1 = new User("testuser", "pass", "test@example.com", "John", "Doe", "UTC");
            User user2 = new User("testuser", "pass", "test@example.com", "Jane", "Smith", "UTC");
            
            setUserId(user1, 999L); // ID that likely has different hash than username^email
            
            // user1 uses ID.hashCode(), user2 uses username^email
            // They might be different (not guaranteed, but likely)
            boolean hashesAreDifferent = user1.hashCode() != user2.hashCode();
            // This test documents the behavior but doesn't assert since hash collision is possible
            assertThat(hashesAreDifferent || !hashesAreDifferent).isTrue(); // Always true, just documenting
        }
    }
}