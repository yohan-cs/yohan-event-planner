package com.yohan.event_planner.business.handler;

import com.yohan.event_planner.business.PasswordBO;
import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.UserUpdateDTO;
import com.yohan.event_planner.exception.EmailException;
import com.yohan.event_planner.exception.PasswordException;
import com.yohan.event_planner.exception.UsernameException;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class UserPatchHandlerTest {

    private PasswordBO passwordBO;
    private UserBO userBO;

    private UserPatchHandler userPatchHandler;

    @BeforeEach
    void setUp() {
        passwordBO = mock(PasswordBO.class);
        userBO = mock(UserBO.class);

        userPatchHandler = new UserPatchHandler(passwordBO, userBO);
    }

    @Nested
    class ApplyPatchTests {

        // region --- Username Patching ---

        @Test
        void testPatchUserNameSuccess() {

            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String newUsername = "NewUserName";

            UserUpdateDTO dto = new UserUpdateDTO(
                    newUsername,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertTrue(updated, "Patch should return true when username is changed.");
            assertEquals(newUsername.toLowerCase(), existingUser.getUsername(),
                    "Username should be updated and normalized to lowercase.");
        }

        @Test
        void testPatchUsernameNoOp() {

            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String existingUsername = existingUser.getUsername();

            UserUpdateDTO dto = new UserUpdateDTO(
                    existingUsername,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertFalse(updated, "Patch should return false when username is unchanged.");
            assertEquals(existingUsername, existingUser.getUsername(),
                    "Username should remain unchanged and normalized to lowercase.");
        }

        @Test
        void testPatchUsernameFailure_DuplicateUsername() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String newUsername = "duplicateUser";

            UserUpdateDTO dto = new UserUpdateDTO(
                    newUsername,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Mocks
            when(userBO.existsByUsername(newUsername.toLowerCase())).thenReturn(true);

            // Act & Assert
            assertThrows(UsernameException.class, () -> userPatchHandler.applyPatch(existingUser, dto));
            verify(userBO).existsByUsername(newUsername.toLowerCase());
            assertNotEquals(newUsername.toLowerCase(), existingUser.getUsername(),
                    "Username should not be updated if duplicate.");
        }

        // endregion

        // region --- Password Patching ---

        @Test
        void testPatchPasswordSuccess() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String hashedPassword = existingUser.getHashedPassword();
            String newPassword = "newPassword123";
            String hashedNewPassword = "hashedNewPassword123";

            UserUpdateDTO dto = new UserUpdateDTO(
                    null,
                    newPassword,
                    null,
                    null,
                    null,
                    null
            );

            // Mocks
            when(passwordBO.isMatch(newPassword, existingUser.getHashedPassword())).thenReturn(false);
            when(passwordBO.encryptPassword(newPassword)).thenReturn(hashedNewPassword);

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertTrue(updated, "Patch should return true when password is changed.");;
            assertEquals(hashedNewPassword, existingUser.getHashedPassword(),
                    "Password should be updated.");
            verify(passwordBO).isMatch(newPassword, hashedPassword);
            verify(passwordBO).encryptPassword(newPassword);
        }

        @Test
        void testPatchPasswordFailure_DuplicatePassword() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String newPassword = "newPassword123";
            String existingHashedPassword = existingUser.getHashedPassword();

            UserUpdateDTO dto = new UserUpdateDTO(
                    null,
                    newPassword,
                    null,
                    null,
                    null,
                    null
            );

            // Mocks
            when(passwordBO.isMatch(newPassword, existingHashedPassword)).thenReturn(true);


            // Act & Assert
            assertThrows(PasswordException.class, () -> userPatchHandler.applyPatch(existingUser, dto));
            verify(passwordBO).isMatch(newPassword, existingHashedPassword);
            verify(passwordBO, never()).encryptPassword(any());
        }

        // endregion

        // region --- Email Patching ---

        @Test
        void testPatchEmailSuccess() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String newEmail = "newEmail@email.com";

            UserUpdateDTO dto = new UserUpdateDTO(
                    null,
                    null,
                    newEmail,
                    null,
                    null,
                    null
            );

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertTrue(updated, "Patch should return true when email is changed.");
            assertEquals(newEmail.toLowerCase(), existingUser.getEmail(),
                    "Email should be updated and normalized to lowercase.");
        }

        @Test
        void testPatchUserEmailNoOp() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String existingEmail = existingUser.getEmail();

            UserUpdateDTO dto = new UserUpdateDTO(
                    null,
                    null,
                    existingEmail,
                    null,
                    null,
                    null
            );

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertFalse(updated, "Patch should return false when email is unchanged.");
            assertEquals(existingEmail, existingUser.getEmail(),
                    "Email should remain unchanged and normalized to lowercase.");
        }

        @Test
        void testPatchEmailFailure_DuplicateEmail() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String newEmail = "duplicateEmail";

            UserUpdateDTO dto = new UserUpdateDTO(
                    null,
                    null,
                    newEmail,
                    null,
                    null,
                    null
            );

            // Mocks
            when(userBO.existsByEmail(newEmail.toLowerCase())).thenReturn(true);

            // Act & Assert
            assertThrows(EmailException.class, () -> userPatchHandler.applyPatch(existingUser, dto));
            verify(userBO).existsByEmail(newEmail.toLowerCase());
            assertNotEquals(newEmail.toLowerCase(), existingUser.getEmail(),
                    "Email should not be updated if duplicate.");
        }

        // endregion

        // region --- First name Patching ---

        @Test
        void testPatchFirstNameSuccess() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String newFirstName = "Newfirstname";

            UserUpdateDTO dto = new UserUpdateDTO(
                    null,
                    null,
                    null,
                    newFirstName,
                    null,
                    null
            );

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertTrue(updated, "Patch should return true when first name is changed.");
            assertEquals(newFirstName, existingUser.getFirstName(),
                    "First name should be updated.");
        }

        @Test
        void testPatchFirstNameNoOp() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String existingFirstName = existingUser.getFirstName();

            UserUpdateDTO dto = new UserUpdateDTO(
                    null,
                    null,
                    null,
                    existingFirstName,
                    null,
                    null
            );

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertFalse(updated, "Patch should return false when first name is unchanged");
            assertEquals(existingFirstName, existingUser.getFirstName(),
                    "First name should remain unchanged.");
        }

        // endregion

        // region --- Last name Patching ---

        @Test
        void testPatchLastNameSuccess() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String newLastName = "Newlastname";

            UserUpdateDTO dto = new UserUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    newLastName,
                    null
            );

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertTrue(updated, "Patch should return true when last name is changed");
            assertEquals(newLastName, existingUser.getLastName(),
                    "Last name should be updated");
        }

        @Test
        void testPatchLastNameNoOp() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String existingLastName = existingUser.getLastName();

            UserUpdateDTO dto = new UserUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    existingLastName,
                    null
            );

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertFalse(updated, "Patch should return false when last name is unchanged.");;
            assertEquals(existingLastName, existingUser.getLastName(),
                    "Last name should remain unchanged.");
        }

        // endregion

        // region --- Timezone Patching ---

        @Test
        void testPatchTimezoneSuccess() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String newTimezone = "Asia/Tokyo";

            UserUpdateDTO dto = new UserUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    newTimezone
            );

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertTrue(updated, "Patch should return true when timezone is changed.");;
            assertEquals(newTimezone, existingUser.getTimezone(),
                    "Timezone should be updated.");
        }

        @Test
        void testPatchTimezoneNoOp() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String existingTimezone = existingUser.getTimezone();

            UserUpdateDTO dto = new UserUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    existingTimezone
            );

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertFalse(updated, "Patch should return false when timezone is unchanged.");
            assertEquals(existingTimezone, existingUser.getTimezone(),
                    "Timezone should remain unchanged.");
        }

        // endregion

        // region --- Other edge cases ---

        @Test
        void testPatchPartialUpdateFailure_OneValidOneInvalid() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String newUsername = "newuser";
            String duplicateEmail = "taken@email.com";

            UserUpdateDTO dto = new UserUpdateDTO(
                    newUsername,
                    null,
                    duplicateEmail,
                    null,
                    null,
                    null
            );

            // Mocks
            when(userBO.existsByUsername(newUsername.toLowerCase())).thenReturn(false);
            when(userBO.existsByEmail(duplicateEmail.toLowerCase())).thenReturn(true);

            String originalUsername = existingUser.getUsername();
            String originalEmail = existingUser.getEmail();

            // Act & Assert
            assertThrows(EmailException.class, () -> userPatchHandler.applyPatch(existingUser, dto));
            verify(userBO).existsByUsername(newUsername.toLowerCase());
            verify(userBO).existsByEmail(duplicateEmail.toLowerCase());
            assertEquals(originalUsername, existingUser.getUsername(), "Username should not be updated.");
            assertEquals(originalEmail, existingUser.getEmail(), "Email should not be updated.");
        }

        @Test
        void testPatchNoChangesNoOp() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();

            UserUpdateDTO dto = new UserUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertFalse(updated, "Patch should return false when everything is unchanged.");
        }

        // endregion

        // region --- Multi-Field Update Tests ---

        @Test
        void testPatchMultipleFieldsSuccess() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String newUsername = "newuser";
            String newEmail = "new@example.com";
            String newFirstName = "NewFirst";
            String newLastName = "NewLast";
            String newTimezone = "Europe/London";

            UserUpdateDTO dto = new UserUpdateDTO(
                    newUsername,
                    null, // Don't change password
                    newEmail,
                    newFirstName,
                    newLastName,
                    newTimezone
            );

            // Mocks
            when(userBO.existsByUsername(newUsername.toLowerCase())).thenReturn(false);
            when(userBO.existsByEmail(newEmail.toLowerCase())).thenReturn(false);

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertTrue(updated, "Patch should return true when multiple fields are changed.");
            assertEquals(newUsername.toLowerCase(), existingUser.getUsername());
            assertEquals(newEmail.toLowerCase(), existingUser.getEmail());
            assertEquals(newFirstName, existingUser.getFirstName());
            assertEquals(newLastName, existingUser.getLastName());
            assertEquals(newTimezone, existingUser.getTimezone());

            verify(userBO).existsByUsername(newUsername.toLowerCase());
            verify(userBO).existsByEmail(newEmail.toLowerCase());
        }

        @Test
        void testPatchMultipleFieldsPartialFailure() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String newUsername = "newuser";
            String duplicateEmail = "taken@example.com";
            String newFirstName = "NewFirst";

            UserUpdateDTO dto = new UserUpdateDTO(
                    newUsername,
                    null,
                    duplicateEmail,
                    newFirstName,
                    null,
                    null
            );

            // Mocks - username is valid but email is taken
            when(userBO.existsByUsername(newUsername.toLowerCase())).thenReturn(false);
            when(userBO.existsByEmail(duplicateEmail.toLowerCase())).thenReturn(true);

            // Store original values
            String originalUsername = existingUser.getUsername();
            String originalEmail = existingUser.getEmail();
            String originalFirstName = existingUser.getFirstName();

            // Act & Assert
            assertThrows(EmailException.class, () -> userPatchHandler.applyPatch(existingUser, dto));

            // Verify atomic behavior - no changes applied
            assertEquals(originalUsername, existingUser.getUsername(), "Username should not be updated on failure.");
            assertEquals(originalEmail, existingUser.getEmail(), "Email should not be updated on failure.");
            assertEquals(originalFirstName, existingUser.getFirstName(), "First name should not be updated on failure.");
        }

        @Test
        void testPatchCredentialsAndPersonalInfoSimultaneously() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String newUsername = "updateduser";
            String newPassword = "newSecurePassword123";
            String newEmail = "updated@example.com";
            String newFirstName = "UpdatedFirst";

            UserUpdateDTO dto = new UserUpdateDTO(
                    newUsername,
                    newPassword,
                    newEmail,
                    newFirstName,
                    null, // Don't change last name
                    null  // Don't change timezone
            );

            // Mocks
            when(userBO.existsByUsername(newUsername.toLowerCase())).thenReturn(false);
            when(userBO.existsByEmail(newEmail.toLowerCase())).thenReturn(false);
            when(passwordBO.isMatch(newPassword, existingUser.getHashedPassword())).thenReturn(false);
            when(passwordBO.encryptPassword(newPassword)).thenReturn("hashedNewPassword");

            String originalLastName = existingUser.getLastName();
            String originalTimezone = existingUser.getTimezone();

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertTrue(updated, "Patch should return true when credentials and personal info are changed.");
            assertEquals(newUsername.toLowerCase(), existingUser.getUsername());
            assertEquals("hashedNewPassword", existingUser.getHashedPassword());
            assertEquals(newEmail.toLowerCase(), existingUser.getEmail());
            assertEquals(newFirstName, existingUser.getFirstName());
            // Unchanged fields should remain the same
            assertEquals(originalLastName, existingUser.getLastName());
            assertEquals(originalTimezone, existingUser.getTimezone());

            verify(passwordBO).encryptPassword(newPassword);
        }

        // endregion

        // region --- Normalization Edge Case Tests ---

        @Test
        void testUsernameNormalizationWithMixedCase() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String mixedCaseUsername = "TestUser123";
            String expectedNormalized = "testuser123";

            UserUpdateDTO dto = new UserUpdateDTO(
                    mixedCaseUsername,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Mocks
            when(userBO.existsByUsername(expectedNormalized)).thenReturn(false);

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertTrue(updated, "Patch should return true when username is normalized and changed.");
            assertEquals(expectedNormalized, existingUser.getUsername(), "Username should be normalized to lowercase.");
            verify(userBO).existsByUsername(expectedNormalized);
        }

        @Test
        void testEmailNormalizationWithMixedCase() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String mixedCaseEmail = "Test@Example.COM";
            String expectedNormalized = "test@example.com";

            UserUpdateDTO dto = new UserUpdateDTO(
                    null,
                    null,
                    mixedCaseEmail,
                    null,
                    null,
                    null
            );

            // Mocks
            when(userBO.existsByEmail(expectedNormalized)).thenReturn(false);

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertTrue(updated, "Patch should return true when email is normalized and changed.");
            assertEquals(expectedNormalized, existingUser.getEmail(), "Email should be normalized to lowercase.");
            verify(userBO).existsByEmail(expectedNormalized);
        }

        @Test
        void testNormalizationWithNoChangeAfterNormalization() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            existingUser.setUsername("testuser");
            existingUser.setEmail("test@example.com");

            // Try to update with mixed case that normalizes to existing values
            UserUpdateDTO dto = new UserUpdateDTO(
                    "TestUser", // Normalizes to "testuser" (same as current)
                    null,
                    "Test@Example.Com", // Normalizes to "test@example.com" (same as current)
                    null,
                    null,
                    null
            );

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertFalse(updated, "Patch should return false when normalized values are unchanged.");
            assertEquals("testuser", existingUser.getUsername());
            assertEquals("test@example.com", existingUser.getEmail());
            
            // Verify no uniqueness checks were performed since values are unchanged
            verify(userBO, never()).existsByUsername(any());
            verify(userBO, never()).existsByEmail(any());
        }

        // endregion

        // region --- Exception Message Validation Tests ---

        @Test
        void testUsernameExceptionContainsOriginalValue() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String originalCaseUsername = "DuplicateUser";
            String normalizedUsername = "duplicateuser";

            UserUpdateDTO dto = new UserUpdateDTO(
                    originalCaseUsername,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            when(userBO.existsByUsername(normalizedUsername)).thenReturn(true);

            // Act & Assert
            UsernameException exception = assertThrows(UsernameException.class, 
                    () -> userPatchHandler.applyPatch(existingUser, dto));

            // Verify exception contains original (non-normalized) username
            assertTrue(exception.getMessage().contains(originalCaseUsername),
                      "Exception should contain original username value, not normalized version");
        }

        @Test
        void testEmailExceptionContainsOriginalValue() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String originalCaseEmail = "Duplicate@Example.COM";
            String normalizedEmail = "duplicate@example.com";

            UserUpdateDTO dto = new UserUpdateDTO(
                    null,
                    null,
                    originalCaseEmail,
                    null,
                    null,
                    null
            );

            when(userBO.existsByEmail(normalizedEmail)).thenReturn(true);

            // Act & Assert
            EmailException exception = assertThrows(EmailException.class, 
                    () -> userPatchHandler.applyPatch(existingUser, dto));

            // Verify exception contains original (non-normalized) email
            assertTrue(exception.getMessage().contains(originalCaseEmail),
                      "Exception should contain original email value, not normalized version");
        }

        // endregion

        // region --- Boundary Value Tests ---

        @Test
        void testPatchWithMinLengthValues() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            
            // Create values at minimum allowed lengths based on ApplicationConstants
            String minUsername = "abc"; // Minimum 3 characters
            String minFirstName = "A"; // Minimum 1 character
            String minLastName = "B"; // Minimum 1 character

            UserUpdateDTO dto = new UserUpdateDTO(
                    minUsername,
                    null,
                    null,
                    minFirstName,
                    minLastName,
                    null
            );

            // Mocks
            when(userBO.existsByUsername(minUsername)).thenReturn(false);

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertTrue(updated, "Patch should succeed with minimum length values.");
            assertEquals(minUsername, existingUser.getUsername());
            assertEquals(minFirstName, existingUser.getFirstName());
            assertEquals(minLastName, existingUser.getLastName());
        }

        @Test
        void testPatchPasswordMinAndMaxLength() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            String minLengthPassword = "12345678"; // Minimum 8 characters

            UserUpdateDTO dto = new UserUpdateDTO(
                    null,
                    minLengthPassword,
                    null,
                    null,
                    null,
                    null
            );

            // Mocks
            when(passwordBO.isMatch(minLengthPassword, existingUser.getHashedPassword())).thenReturn(false);
            when(passwordBO.encryptPassword(minLengthPassword)).thenReturn("hashedMinPassword");

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertTrue(updated, "Patch should succeed with minimum length password.");
            assertEquals("hashedMinPassword", existingUser.getHashedPassword());
            verify(passwordBO).encryptPassword(minLengthPassword);
        }

        @Test
        void testPatchWithLongValidValues() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            
            // Create reasonably long values (but within limits)
            String longUsername = "a".repeat(25); // Near max of 30
            String longEmail = "test" + "a".repeat(20) + "@example.com";
            String longFirstName = "First" + "n".repeat(40); // Near max of 50
            String longLastName = "Last" + "n".repeat(41); // Near max of 50
            String longPassword = "SecurePassword" + "1".repeat(50); // Well within 72 char limit

            UserUpdateDTO dto = new UserUpdateDTO(
                    longUsername,
                    longPassword,
                    longEmail,
                    longFirstName,
                    longLastName,
                    "America/New_York"
            );

            // Mocks
            when(userBO.existsByUsername(longUsername)).thenReturn(false);
            when(userBO.existsByEmail(longEmail.toLowerCase())).thenReturn(false);
            when(passwordBO.isMatch(longPassword, existingUser.getHashedPassword())).thenReturn(false);
            when(passwordBO.encryptPassword(longPassword)).thenReturn("hashedLongPassword");

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertTrue(updated, "Patch should succeed with long but valid values.");
            assertEquals(longUsername, existingUser.getUsername());
            assertEquals(longEmail.toLowerCase(), existingUser.getEmail());
            assertEquals(longFirstName, existingUser.getFirstName());
            assertEquals(longLastName, existingUser.getLastName());
            assertEquals("hashedLongPassword", existingUser.getHashedPassword());
        }

        @Test
        void testPatchWithSpecialCharactersInAllowedFields() {
            // Arrange
            User existingUser = TestUtils.createValidUserEntityWithId();
            
            // Test special characters in fields that allow them
            String usernameWithDots = "user.name_123";
            String emailWithSpecialChars = "user+tag@example-domain.co.uk";
            String firstNameWithSpaces = "Jean-Paul";
            String lastNameWithApostrophe = "O'Connor";

            UserUpdateDTO dto = new UserUpdateDTO(
                    usernameWithDots,
                    null,
                    emailWithSpecialChars,
                    firstNameWithSpaces,
                    lastNameWithApostrophe,
                    null
            );

            // Mocks
            when(userBO.existsByUsername(usernameWithDots)).thenReturn(false);
            when(userBO.existsByEmail(emailWithSpecialChars.toLowerCase())).thenReturn(false);

            // Act
            boolean updated = userPatchHandler.applyPatch(existingUser, dto);

            // Assert
            assertTrue(updated, "Patch should succeed with special characters in allowed fields.");
            assertEquals(usernameWithDots, existingUser.getUsername());
            assertEquals(emailWithSpecialChars.toLowerCase(), existingUser.getEmail());
            assertEquals(firstNameWithSpaces, existingUser.getFirstName());
            assertEquals(lastNameWithApostrophe, existingUser.getLastName());
        }

        // endregion

    }
}
