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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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

        @Test
        void testPatchUserNameSuccess() {

            // Arrange
            User existingUser = TestUtils.createUserEntityWithId();
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
            User existingUser = TestUtils.createUserEntityWithId();
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
            User existingUser = TestUtils.createUserEntityWithId();
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

        @Test
        void testPatchPasswordSuccess() {
            // Arrange
            User existingUser = TestUtils.createUserEntityWithId();
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
            User existingUser = TestUtils.createUserEntityWithId();
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

        @Test
        void testPatchEmailSuccess() {
            // Arrange
            User existingUser = TestUtils.createUserEntityWithId();
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
            User existingUser = TestUtils.createUserEntityWithId();
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
            User existingUser = TestUtils.createUserEntityWithId();
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

        @Test
        void testPatchFirstNameSuccess() {
            // Arrange
            User existingUser = TestUtils.createUserEntityWithId();
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
            User existingUser = TestUtils.createUserEntityWithId();
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

        @Test
        void testPatchLastNameSuccess() {
            // Arrange
            User existingUser = TestUtils.createUserEntityWithId();
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
            User existingUser = TestUtils.createUserEntityWithId();
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

        @Test
        void testPatchTimezoneSuccess() {
            // Arrange
            User existingUser = TestUtils.createUserEntityWithId();
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
            User existingUser = TestUtils.createUserEntityWithId();
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

        @Test
        void testPatchPartialUpdateFailure_OneValidOneInvalid() {
            // Arrange
            User existingUser = TestUtils.createUserEntityWithId();
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
            User existingUser = TestUtils.createUserEntityWithId();

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
    }
}
