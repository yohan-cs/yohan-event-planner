package com.yohan.event_planner.service;

import com.yohan.event_planner.business.PasswordBO;
import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.business.handler.UserPatchHandler;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.dto.UserUpdateDTO;
import com.yohan.event_planner.exception.EmailException;
import com.yohan.event_planner.exception.UserNotFoundException;
import com.yohan.event_planner.exception.UserOwnershipException;
import com.yohan.event_planner.exception.UsernameException;
import com.yohan.event_planner.mapper.UserMapper;
import com.yohan.event_planner.security.OwnershipValidator;
import com.yohan.event_planner.util.TestConstants;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_USER_ACCESS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceImplTest {

    private UserBO userBO;
    private UserMapper userMapper;
    private UserPatchHandler userPatchHandler;
    private PasswordBO passwordBO;
    private OwnershipValidator ownershipValidator;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userBO = mock(UserBO.class);
        userMapper = mock(UserMapper.class);
        userPatchHandler = mock(UserPatchHandler.class);
        passwordBO = mock(PasswordBO.class);
        ownershipValidator = mock(OwnershipValidator.class);
        userService = new UserServiceImpl(userBO, userMapper, userPatchHandler, passwordBO, ownershipValidator);
    }

    @Nested
    class GetUserByUsernameTests {

        @Test
        void testGetUserByUsername_CaseInsensitive() {
            // Arrange
            String inputUsername = "TestUser";

            User expectedUser = TestUtils.createUserEntityWithId();
            when(userBO.getUserByUsername(inputUsername.toLowerCase())).thenReturn(Optional.of(expectedUser));

            // Act
            Optional<User> result = userService.getUserByUsername(inputUsername);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(expectedUser, result.get());
            verify(userBO).getUserByUsername(inputUsername.toLowerCase());
        }

        @Test
        void testGetUserByUsername_NotFound() {
            // Arrange
            String inputUsername = "NonexistentUser";

            when(userBO.getUserByUsername(inputUsername.toLowerCase())).thenReturn(Optional.empty());

            // Act
            Optional<User> result = userService.getUserByUsername(inputUsername);

            // Assert
            assertTrue(result.isEmpty());
            verify(userBO).getUserByUsername(inputUsername.toLowerCase());
        }
    }

    @Nested
    class GetUserByEmailTests {

        @Test
        void testGetUserByEmail_CaseInsensitive() {
            // Arrange
            String inputEmail = "USER@Email.Com";

            User expectedUser = TestUtils.createUserEntityWithId();
            when(userBO.getUserByEmail(inputEmail.toLowerCase())).thenReturn(Optional.of(expectedUser));

            // Act
            Optional<User> result = userService.getUserByEmail(inputEmail);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(expectedUser, result.get());
            verify(userBO).getUserByEmail(inputEmail.toLowerCase());
        }

        @Test
        void testGetUserByEmail_NotFound() {
            // Arrange
            String inputEmail = "missing@Email.Com";

            when(userBO.getUserByEmail(inputEmail.toLowerCase())).thenReturn(Optional.empty());

            // Act
            Optional<User> result = userService.getUserByEmail(inputEmail);

            // Assert
            assertTrue(result.isEmpty());
            verify(userBO).getUserByEmail(inputEmail.toLowerCase());
        }
    }

    @Nested
    class CreateUserTests {

        @Test
        void testCreateUserSuccess() {
            // Arrange
            UserCreateDTO dto = TestUtils.createValidUserCreateDTO();

            User createdUser = TestUtils.createUserEntityWithoutId();

            // Mocks
            when(userBO.existsByUsername(dto.username())).thenReturn(false);
            when(userBO.existsByEmail(dto.email())).thenReturn(false);
            when(userMapper.toEntity(dto)).thenReturn(createdUser);

            // Act
            User result = userService.createUser(dto);

            // Assert
            verify(userBO).existsByUsername(dto.username());
            verify(userBO).existsByEmail(dto.email());
            verify(userBO).createUser(createdUser);
        }

        @Test
        void testCreateUserFailure_UsernameConflict() {
            // Arrange
            UserCreateDTO dto = TestUtils.createValidUserCreateDTO();

            // Mocks
            when(userBO.existsByUsername(dto.username())).thenReturn(true);

            // Act
            assertThrows(UsernameException.class, () -> userService.createUser(dto));

            // Assert
            verify(userBO).existsByUsername(dto.username());
            verify(userBO, never()).existsByEmail(anyString());
            verify(userBO, never()).createUser(any());
        }

        @Test
        void testCreateUserFailure_EmailConflict() {
            // Arrange
            UserCreateDTO dto = TestUtils.createValidUserCreateDTO();

            // Mocks
            when(userBO.existsByUsername(dto.username())).thenReturn(false);
            when(userBO.existsByEmail(dto.email())).thenReturn(true);

            // Act
            assertThrows(EmailException.class, () -> userService.createUser(dto));

            // Assert
            verify(userBO).existsByUsername(dto.username());
            verify(userBO).existsByEmail(dto.email());
            verify(userBO, never()).createUser(any());
        }

        @Test
        void testCreateUserFailure_UsernameAndEmailConflict() {
            // Arrange
            UserCreateDTO dto = TestUtils.createValidUserCreateDTO();

            when(userBO.existsByUsername(dto.username())).thenReturn(true);

            // Act & Assert
            assertThrows(UsernameException.class, () -> userService.createUser(dto));
            verify(userBO).existsByUsername(dto.username());
            verify(userBO, never()).existsByEmail(any());
            verify(userBO, never()).createUser(any());
        }
    }

    @Nested
    class UpdateUserTests {

        @Test
        void testUpdateUsernameTimezoneSuccess() {
            // Arrange
            String newUsername = "newUserName";
            String updatedTimezone = "Asia/Tokyo";

            UserUpdateDTO dto = new UserUpdateDTO(
                    newUsername,
                    null,
                    null,
                    null,
                    null,
                    updatedTimezone
            );

            User existingUser = TestUtils.createUserEntityWithId();
            User updatedUser = TestUtils.createUserEntityWithId();
            updatedUser.setUsername(newUsername.toLowerCase());
            updatedUser.setTimezone(updatedTimezone);

            // Mocks
            when(userBO.getUserById(TestConstants.USER_ID)).thenReturn(Optional.of(existingUser));
            doNothing().when(ownershipValidator).validateUserOwnership(TestConstants.USER_ID);
            when(userBO.getUserByUsername(newUsername)).thenReturn(Optional.empty());
            when(userPatchHandler.applyPatch(existingUser, dto)).thenReturn(true);
            when(userBO.updateUser(existingUser)).thenReturn(updatedUser);

            // Act
            User result = userService.updateUser(TestConstants.USER_ID, dto);

            // Assert
            assertEquals(newUsername.toLowerCase(), result.getUsername());
            assertEquals(updatedTimezone, result.getTimezone());

            verify(userBO).getUserById(TestConstants.USER_ID);
            verify(ownershipValidator).validateUserOwnership(TestConstants.USER_ID);
            verify(userBO).getUserByUsername(newUsername.toLowerCase());
            verify(userPatchHandler).applyPatch(existingUser, dto);
            verify(userBO).updateUser(existingUser);
        }

        @Test
        void testUpdateUsernameFailure_DuplicateUsername() {
            // Arrange
            String newUsername = TestConstants.VALID_USERNAME;

            UserUpdateDTO dto = new UserUpdateDTO(
                    newUsername,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            User existingUser = TestUtils.createUserEntityWithId();
            User updatingUser = TestUtils.createUserEntityWithId(999L);

            // Mocks
            when(userBO.getUserById(updatingUser.getId())).thenReturn(Optional.of(updatingUser));
            doNothing().when(ownershipValidator).validateUserOwnership(updatingUser.getId());
            when(userBO.getUserByUsername(newUsername.toLowerCase())).thenReturn(Optional.of(existingUser));

            // Act + Assert
            assertThrows(UsernameException.class, () -> userService.updateUser(updatingUser.getId(), dto));

            // Verifications
            verify(userBO).getUserById(updatingUser.getId());
            verify(ownershipValidator).validateUserOwnership(updatingUser.getId());
            verify(userBO).getUserByUsername(newUsername.toLowerCase());
            verify(userPatchHandler, never()).applyPatch(any(), any());
            verify(userBO, never()).updateUser(any());
        }

        @Test
        void testUpdateEmailSuccess() {
            // Arrange
            String newEmail = "newEmail@email.com";

            UserUpdateDTO dto = new UserUpdateDTO(
                    null,
                    null,
                    newEmail,
                    null,
                    null,
                    null
            );

            User existingUser = TestUtils.createUserEntityWithId();
            User updatedUser = TestUtils.createUserEntityWithId();
            updatedUser.setEmail(newEmail.toLowerCase());

            // Mocks
            when(userBO.getUserById(TestConstants.USER_ID)).thenReturn(Optional.of(existingUser));
            doNothing().when(ownershipValidator).validateUserOwnership(TestConstants.USER_ID);
            when(userBO.getUserByEmail(newEmail)).thenReturn(Optional.empty());
            when(userPatchHandler.applyPatch(existingUser, dto)).thenReturn(true);
            when(userBO.updateUser(existingUser)).thenReturn(updatedUser);

            // Act
            User result = userService.updateUser(TestConstants.USER_ID, dto);

            // Assert
            assertEquals(newEmail.toLowerCase(), result.getEmail());

            verify(userBO).getUserById(TestConstants.USER_ID);
            verify(ownershipValidator).validateUserOwnership(TestConstants.USER_ID);
            verify(userBO).getUserByEmail(newEmail.toLowerCase());
            verify(userPatchHandler).applyPatch(existingUser, dto);
            verify(userBO).updateUser(existingUser);
        }

        @Test
        void testUpdateEmailFailure_DuplicateEmail() {
            // Arrange
            String newEmail = TestConstants.VALID_EMAIL;

            UserUpdateDTO dto = new UserUpdateDTO(
                    null,
                    null,
                    newEmail,
                    null,
                    null,
                    null
            );

            User existingUser = TestUtils.createUserEntityWithId();
            User updatingUser = TestUtils.createUserEntityWithId(999L);

            // Mocks
            when(userBO.getUserById(updatingUser.getId())).thenReturn(Optional.of(updatingUser));
            doNothing().when(ownershipValidator).validateUserOwnership(updatingUser.getId());
            when(userBO.getUserByEmail(newEmail.toLowerCase())).thenReturn(Optional.of(existingUser));

            // Act + Assert
            assertThrows(EmailException.class, () -> userService.updateUser(updatingUser.getId(), dto));

            // Verifications
            verify(userBO).getUserById(updatingUser.getId());
            verify(ownershipValidator).validateUserOwnership(updatingUser.getId());
            verify(userBO).getUserByEmail(newEmail.toLowerCase());
            verify(userPatchHandler, never()).applyPatch(any(), any());
            verify(userBO, never()).updateUser(any());
        }

        @Test
        void testUpdateUserFailure_UnauthorizedAccess() {
            // Arrange
            UserUpdateDTO dto = new UserUpdateDTO(
                    "newName",
                    null,
                    null,
                    null,
                    null,
                    null);
            User existingUser = TestUtils.createUserEntityWithId();

            when(userBO.getUserById(TestConstants.USER_ID)).thenReturn(Optional.of(existingUser));
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, TestConstants.USER_ID)).when(ownershipValidator)
                    .validateUserOwnership(TestConstants.USER_ID);

            // Act + Assert
            assertThrows(UserOwnershipException.class, () -> userService.updateUser(TestConstants.USER_ID, dto));
            verify(userPatchHandler, never()).applyPatch(any(), any());
            verify(userBO, never()).updateUser(any());
        }

        @Test
        void testUpdateUserNoOp() {
            // Arrange
            UserUpdateDTO dto = new UserUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
            User existingUser = TestUtils.createUserEntityWithId();

            // Mocks
            when(userBO.getUserById(TestConstants.USER_ID)).thenReturn(Optional.of(existingUser));
            doNothing().when(ownershipValidator).validateUserOwnership(TestConstants.USER_ID);
            when(userPatchHandler.applyPatch(existingUser, dto)).thenReturn(false);

            // Act
            User result = userService.updateUser(TestConstants.USER_ID, dto);

            // Assert
            assertEquals(existingUser, result);
            verify(userBO, never()).updateUser(any());
        }
    }

    @Nested
    class DeleteUserTests {

        @Test
        void testDeleteUserSuccess() {
            // Arrange
            User existingUser = TestUtils.createUserEntityWithId();

            // Mocks
            when(userBO.getUserById(TestConstants.USER_ID)).thenReturn(Optional.of(existingUser));
            doNothing().when(ownershipValidator).validateUserOwnership(TestConstants.USER_ID);
            doAnswer(invocation -> {
                existingUser.setDeleted(true);
                existingUser.setActive(false);
                return null;
            }).when(userBO).deleteUser(existingUser);

            // Act
            userService.deleteUser(TestConstants.USER_ID);

            // Assert
            verify(userBO).getUserById(TestConstants.USER_ID);
            verify(ownershipValidator).validateUserOwnership(TestConstants.USER_ID);
            verify(userBO).deleteUser(existingUser);

            // Additional assertions to verify user state was mutated
            assertTrue(existingUser.isDeleted());
            assertFalse(existingUser.isActive());
        }

        @Test
        void testDeleteUserFailure_UnauthorizedAccess() {
            // Arrange
            User existingUser = TestUtils.createUserEntityWithId();

            // Mocks
            when(userBO.getUserById(TestConstants.USER_ID)).thenReturn(Optional.of(existingUser));
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, TestConstants.USER_ID))
                    .when(ownershipValidator).validateUserOwnership(TestConstants.USER_ID);

            // Act + Assert
            assertThrows(UserOwnershipException.class, () -> userService.deleteUser(TestConstants.USER_ID));
            verify(userBO).getUserById(TestConstants.USER_ID);
            verify(ownershipValidator).validateUserOwnership(TestConstants.USER_ID);
            verify(userBO, never()).deleteUser(any());
        }

        @Test
        void testDeleteUserFailure_UserNotFound() {
            // Mocks
            when(userBO.getUserById(TestConstants.USER_ID)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(UserNotFoundException.class, () -> userService.deleteUser(TestConstants.USER_ID));
            verify(userBO).getUserById(TestConstants.USER_ID);
            verify(ownershipValidator, never()).validateUserOwnership(any());
            verify(userBO, never()).deleteUser(any());
        }
    }

    @Nested
    class ExistsByUsernameTests {

        @Test
        void testExistsByUsername_ReturnsTrue_EvenWithDifferentCase() {
            // Arrange
            String storedUsername = "storedUsername";

            // Mocks
            when(userBO.existsByUsername(storedUsername.toLowerCase())).thenReturn(true);

            // Act
            boolean result = userService.existsByUsername(storedUsername);

            // Assert
            assertTrue(result);
            verify(userBO).existsByUsername(storedUsername.toLowerCase());
        }

        @Test
        void testExistsByUsername_ReturnsFalse() {
            // Arrange
            when(userBO.existsByUsername("nonexistent")).thenReturn(false);

            // Act
            boolean result = userService.existsByUsername("nonexistent");

            // Assert
            assertFalse(result);
            verify(userBO).existsByUsername("nonexistent");
        }
    }

    @Nested
    class ExistsByEmailTests {

        @Test
        void testExistsByEmail_ReturnsTrue_EvenWithDifferentCase() {
            // Arrange
            String inputEmail = "TestEmail@Example.com";

            // Mocks
            when(userBO.existsByEmail(inputEmail.toLowerCase())).thenReturn(true);

            // Act
            boolean result = userService.existsByEmail(inputEmail);

            // Assert
            assertTrue(result);
            verify(userBO).existsByEmail(inputEmail.toLowerCase());
        }

        @Test
        void testExistsByEmail_ReturnsFalse() {
            // Arrange
            String inputEmail = "nonexistent@example.com";

            // Mocks
            when(userBO.existsByEmail(inputEmail.toLowerCase())).thenReturn(false);

            // Act
            boolean result = userService.existsByEmail(inputEmail);

            // Assert
            assertFalse(result);
            verify(userBO).existsByEmail(inputEmail.toLowerCase());
        }
    }
}
