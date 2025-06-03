package com.yohan.event_planner.service;

import com.yohan.event_planner.business.PasswordBO;
import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.business.handler.UserPatchHandler;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.Role;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.dto.UserResponseDTO;
import com.yohan.event_planner.dto.UserUpdateDTO;
import com.yohan.event_planner.exception.EmailException;
import com.yohan.event_planner.exception.UserNotFoundException;
import com.yohan.event_planner.exception.UserOwnershipException;
import com.yohan.event_planner.exception.UsernameException;
import com.yohan.event_planner.mapper.UserMapper;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import com.yohan.event_planner.util.TestConstants;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
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
    private AuthenticatedUserProvider authenticatedUserProvider;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userBO = mock(UserBO.class);
        userMapper = mock(UserMapper.class);
        userPatchHandler = mock(UserPatchHandler.class);
        passwordBO = mock(PasswordBO.class);
        ownershipValidator = mock(OwnershipValidator.class);
        authenticatedUserProvider = mock(AuthenticatedUserProvider.class);
        userService = new UserServiceImpl(userBO, userMapper, userPatchHandler, passwordBO, ownershipValidator, authenticatedUserProvider);
    }

    @Nested
    class GetUserByIdTests {

        @Test
        void testGetUserByIdSuccess() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            UserResponseDTO expectedDTO = TestUtils.createUserResponseDTO();

            when(userBO.getUserById(TestConstants.USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toResponseDTO(user)).thenReturn(expectedDTO);

            // Act
            UserResponseDTO result = userService.getUserById(TestConstants.USER_ID);

            // Assert
            assertEquals(expectedDTO, result);
            verify(userBO).getUserById(TestConstants.USER_ID);
            verify(userMapper).toResponseDTO(user);
        }

        @Test
        void testGetUserByIdFailure_NotFound() {
            // Arrange
            when(userBO.getUserById(TestConstants.USER_ID)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(UserNotFoundException.class, () -> userService.getUserById(TestConstants.USER_ID));
            verify(userBO).getUserById(TestConstants.USER_ID);
            verify(userMapper, never()).toResponseDTO(any());
        }
    }

    @Nested
    class GetUserByUsernameTests {

        @Test
        void testGetUserByUsername_CaseInsensitive_Success() {
            // Arrange
            String inputUsername = "TestUser";
            String normalizedUsername = inputUsername.toLowerCase();

            User expectedUser = TestUtils.createUserEntityWithId();
            UserResponseDTO expectedDTO = TestUtils.createUserResponseDTO();

            when(userBO.getUserByUsername(normalizedUsername)).thenReturn(Optional.of(expectedUser));
            when(userMapper.toResponseDTO(expectedUser)).thenReturn(expectedDTO);

            // Act
            UserResponseDTO result = userService.getUserByUsername(inputUsername);

            // Assert
            assertEquals(expectedDTO, result);
            verify(userBO).getUserByUsername(normalizedUsername);
            verify(userMapper).toResponseDTO(expectedUser);
        }

        @Test
        void testGetUserByUsername_NotFound_ThrowsException() {
            // Arrange
            String inputUsername = "NonexistentUser";
            String normalizedUsername = inputUsername.toLowerCase();

            when(userBO.getUserByUsername(normalizedUsername)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(UserNotFoundException.class, () -> userService.getUserByUsername(inputUsername));
            verify(userBO).getUserByUsername(normalizedUsername);
            verify(userMapper, never()).toResponseDTO(any());
        }
    }

    @Nested
    class GetUserByEmailTests {

        @Test
        void testGetUserByEmail_CaseInsensitive_Success() {
            // Arrange
            String inputEmail = "USER@Email.Com";
            String normalizedEmail = inputEmail.toLowerCase();

            User expectedUser = TestUtils.createUserEntityWithId();
            UserResponseDTO expectedDTO = TestUtils.createUserResponseDTO();

            when(userBO.getUserByEmail(normalizedEmail)).thenReturn(Optional.of(expectedUser));
            when(userMapper.toResponseDTO(expectedUser)).thenReturn(expectedDTO);

            // Act
            UserResponseDTO result = userService.getUserByEmail(inputEmail);

            // Assert
            assertEquals(expectedDTO, result);
            verify(userBO).getUserByEmail(normalizedEmail);
            verify(userMapper).toResponseDTO(expectedUser);
        }

        @Test
        void testGetUserByEmail_NotFound_ThrowsException() {
            // Arrange
            String inputEmail = "missing@Email.Com";
            String normalizedEmail = inputEmail.toLowerCase();

            when(userBO.getUserByEmail(normalizedEmail)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(UserNotFoundException.class, () -> userService.getUserByEmail(inputEmail));
            verify(userBO).getUserByEmail(normalizedEmail);
            verify(userMapper, never()).toResponseDTO(any());
        }
    }

    @Nested
    class GetUsersByRoleTests {

        @Test
        void testGetUsersByRoleSuccess() {
            // Arrange
            Role role = Role.USER;
            List<User> users = List.of(TestUtils.createUserEntityWithId());
            List<UserResponseDTO> responseDTOs = List.of(TestUtils.createUserResponseDTO());

            when(userBO.getUsersByRole(role)).thenReturn(users);
            when(userMapper.toResponseDTO(users.get(0))).thenReturn(responseDTOs.get(0));

            // Act
            List<UserResponseDTO> result = userService.getUsersByRole(role);

            // Assert
            assertEquals(responseDTOs, result);
            verify(userBO).getUsersByRole(role);
            verify(userMapper).toResponseDTO(users.get(0));
        }

        @Test
        void testGetUsersByRoleEmptyList() {
            // Arrange
            Role role = Role.ADMIN;
            when(userBO.getUsersByRole(role)).thenReturn(List.of());

            // Act
            List<UserResponseDTO> result = userService.getUsersByRole(role);

            // Assert
            assertTrue(result.isEmpty());
            verify(userBO).getUsersByRole(role);
            verify(userMapper, never()).toResponseDTO(any());
        }
    }

    @Nested
    class GetAllUsersTests {

        @Test
        void testGetAllUsersSuccess() {
            // Arrange
            List<User> users = List.of(TestUtils.createUserEntityWithId());
            List<UserResponseDTO> responseDTOs = List.of(TestUtils.createUserResponseDTO());

            when(userBO.getAllUsers()).thenReturn(users);
            when(userMapper.toResponseDTO(users.get(0))).thenReturn(responseDTOs.get(0));

            // Act
            List<UserResponseDTO> result = userService.getAllUsers(0, 10);

            // Assert
            assertEquals(responseDTOs, result);
            verify(userBO).getAllUsers();
            verify(userMapper).toResponseDTO(users.get(0));
        }

        @Test
        void testGetAllUsersEmptyList() {
            // Arrange
            when(userBO.getAllUsers()).thenReturn(List.of());

            // Act
            List<UserResponseDTO> result = userService.getAllUsers(0, 10);

            // Assert
            assertTrue(result.isEmpty());
            verify(userBO).getAllUsers();
            verify(userMapper, never()).toResponseDTO(any());
        }
    }

    @Nested
    class GetCurrentUserTests {

        @Test
        void testGetCurrentUserSuccess() {
            // Arrange
            User currentUser = TestUtils.createUserEntityWithId();
            UserResponseDTO expectedDTO = TestUtils.createUserResponseDTO();

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(userMapper.toResponseDTO(currentUser)).thenReturn(expectedDTO);

            // Act
            UserResponseDTO result = userService.getCurrentUser();

            // Assert
            assertEquals(expectedDTO, result);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(userMapper).toResponseDTO(currentUser);
        }
    }

    @Nested
    class CreateUserTests {

        @Test
        void testCreateUserSuccess() {
            // Arrange
            UserCreateDTO dto = TestUtils.createValidUserCreateDTO();
            User entityToCreate = TestUtils.createUserEntityWithoutId();
            User createdEntity = TestUtils.createUserEntityWithId();
            UserResponseDTO responseDTO = TestUtils.createUserResponseDTO();

            when(userBO.existsByUsername(dto.username())).thenReturn(false);
            when(userBO.existsByEmail(dto.email())).thenReturn(false);
            when(userMapper.toEntity(dto)).thenReturn(entityToCreate);
            when(passwordBO.encryptPassword(dto.password())).thenReturn(TestConstants.VALID_PASSWORD);
            when(userBO.createUser(entityToCreate)).thenReturn(createdEntity);
            when(userMapper.toResponseDTO(createdEntity)).thenReturn(responseDTO);

            // Act
            UserResponseDTO result = userService.createUser(dto);

            // Assert
            assertEquals(responseDTO, result);
            verify(userBO).existsByUsername(dto.username());
            verify(userBO).existsByEmail(dto.email());
            verify(userMapper).toEntity(dto);
            verify(passwordBO).encryptPassword(dto.password());
            verify(userBO).createUser(entityToCreate);
            verify(userMapper).toResponseDTO(createdEntity);
        }

        @Test
        void testCreateUserFailure_UsernameConflict() {
            // Arrange
            UserCreateDTO dto = TestUtils.createValidUserCreateDTO();
            when(userBO.existsByUsername(dto.username())).thenReturn(true);

            // Act + Assert
            assertThrows(UsernameException.class, () -> userService.createUser(dto));

            // Assert
            verify(userBO).existsByUsername(dto.username());
            verify(userBO, never()).existsByEmail(any());
            verify(userMapper, never()).toEntity(any());
            verify(userBO, never()).createUser(any());
        }

        @Test
        void testCreateUserFailure_EmailConflict() {
            // Arrange
            UserCreateDTO dto = TestUtils.createValidUserCreateDTO();
            when(userBO.existsByUsername(dto.username())).thenReturn(false);
            when(userBO.existsByEmail(dto.email())).thenReturn(true);

            // Act + Assert
            assertThrows(EmailException.class, () -> userService.createUser(dto));

            // Assert
            verify(userBO).existsByUsername(dto.username());
            verify(userBO).existsByEmail(dto.email());
            verify(userMapper, never()).toEntity(any());
            verify(userBO, never()).createUser(any());
        }

        @Test
        void testCreateUserFailure_UsernameAndEmailConflict() {
            // Arrange
            UserCreateDTO dto = TestUtils.createValidUserCreateDTO();
            when(userBO.existsByUsername(dto.username())).thenReturn(true);

            // Act + Assert
            assertThrows(UsernameException.class, () -> userService.createUser(dto));

            // Assert
            verify(userBO).existsByUsername(dto.username());
            verify(userBO, never()).existsByEmail(any());
            verify(userMapper, never()).toEntity(any());
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
            UserUpdateDTO dto = new UserUpdateDTO(newUsername, null, null, null, null, updatedTimezone);

            User existingUser = TestUtils.createUserEntityWithId();
            User updatedUser = TestUtils.createUserEntityWithId();
            updatedUser.setUsername(newUsername.toLowerCase());
            updatedUser.setTimezone(updatedTimezone);
            UserResponseDTO responseDTO = TestUtils.createUserResponseDTO();

            when(userBO.getUserById(TestConstants.USER_ID)).thenReturn(Optional.of(existingUser));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(existingUser);
            doNothing().when(ownershipValidator).validateUserOwnership(existingUser.getId(), TestConstants.USER_ID);
            when(userBO.getUserByUsername(newUsername.toLowerCase())).thenReturn(Optional.empty());
            when(userPatchHandler.applyPatch(existingUser, dto)).thenReturn(true);
            when(userBO.updateUser(existingUser)).thenReturn(updatedUser);
            when(userMapper.toResponseDTO(updatedUser)).thenReturn(responseDTO);

            // Act
            UserResponseDTO result = userService.updateUser(TestConstants.USER_ID, dto);

            // Assert
            assertEquals(responseDTO, result);
            verify(userBO).getUserById(TestConstants.USER_ID);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateUserOwnership(existingUser.getId(), TestConstants.USER_ID);
            verify(userBO).getUserByUsername(newUsername.toLowerCase());
            verify(userPatchHandler).applyPatch(existingUser, dto);
            verify(userBO).updateUser(existingUser);
            verify(userMapper).toResponseDTO(updatedUser);
        }

        @Test
        void testUpdateUsernameFailure_DuplicateUsername() {
            // Arrange
            String newUsername = TestConstants.VALID_USERNAME;
            UserUpdateDTO dto = new UserUpdateDTO(newUsername, null, null, null, null, null);

            User existingUser = TestUtils.createUserEntityWithId();
            User updatingUser = TestUtils.createUserEntityWithId(999L);

            when(userBO.getUserById(updatingUser.getId())).thenReturn(Optional.of(updatingUser));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(updatingUser);
            doNothing().when(ownershipValidator).validateUserOwnership(updatingUser.getId(), updatingUser.getId());
            when(userBO.getUserByUsername(newUsername.toLowerCase())).thenReturn(Optional.of(existingUser));

            // Act + Assert
            assertThrows(UsernameException.class, () -> userService.updateUser(updatingUser.getId(), dto));

            // Assert
            verify(userBO).getUserById(updatingUser.getId());
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateUserOwnership(updatingUser.getId(), updatingUser.getId());
            verify(userBO).getUserByUsername(newUsername.toLowerCase());
            verify(userPatchHandler, never()).applyPatch(any(), any());
            verify(userBO, never()).updateUser(any());
            verify(userMapper, never()).toResponseDTO(any());
        }

        @Test
        void testUpdateUserFailure_UnauthorizedAccess() {
            // Arrange
            UserUpdateDTO dto = new UserUpdateDTO("newName", null, null, null, null, null);
            User existingUser = TestUtils.createUserEntityWithId();

            when(userBO.getUserById(TestConstants.USER_ID)).thenReturn(Optional.of(existingUser));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(existingUser);
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, TestConstants.USER_ID))
                    .when(ownershipValidator).validateUserOwnership(existingUser.getId(), TestConstants.USER_ID);

            // Act + Assert
            assertThrows(UserOwnershipException.class, () -> userService.updateUser(TestConstants.USER_ID, dto));

            // Assert
            verify(userBO).getUserById(TestConstants.USER_ID);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateUserOwnership(existingUser.getId(), TestConstants.USER_ID);
            verify(userPatchHandler, never()).applyPatch(any(), any());
            verify(userBO, never()).updateUser(any());
            verify(userMapper, never()).toResponseDTO(any());
        }
    }

    @Nested
    class UpdateCurrentUserTests {

        @Test
        void testUpdateCurrentUserSuccess() {
            // Arrange
            UserUpdateDTO dto = new UserUpdateDTO("newName", null, null, null, null, "Asia/Seoul");
            User currentUser = TestUtils.createUserEntityWithId();
            User updatedUser = TestUtils.createUserEntityWithId();
            updatedUser.setUsername("newname");
            updatedUser.setTimezone("Asia/Seoul");
            UserResponseDTO responseDTO = TestUtils.createUserResponseDTO();

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(userBO.getUserByUsername("newname")).thenReturn(Optional.empty());
            when(userPatchHandler.applyPatch(currentUser, dto)).thenReturn(true);
            when(userBO.updateUser(currentUser)).thenReturn(updatedUser);
            when(userMapper.toResponseDTO(updatedUser)).thenReturn(responseDTO);

            // Act
            UserResponseDTO result = userService.updateCurrentUser(dto);

            // Assert
            assertEquals(responseDTO, result);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(userBO).getUserByUsername("newname");
            verify(userPatchHandler).applyPatch(currentUser, dto);
            verify(userBO).updateUser(currentUser);
            verify(userMapper).toResponseDTO(updatedUser);
        }

        @Test
        void testUpdateCurrentUserFailure_UsernameConflict() {
            // Arrange
            String newUsername = TestConstants.VALID_USERNAME;
            UserUpdateDTO dto = new UserUpdateDTO(newUsername, null, null, null, null, null);
            User currentUser = TestUtils.createUserEntityWithId();
            User existingUser = TestUtils.createUserEntityWithId(999L);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(userBO.getUserByUsername(newUsername.toLowerCase())).thenReturn(Optional.of(existingUser));

            // Act + Assert
            assertThrows(UsernameException.class, () -> userService.updateCurrentUser(dto));
            verify(authenticatedUserProvider).getCurrentUser();
            verify(userBO).getUserByUsername(newUsername.toLowerCase());
            verify(userPatchHandler, never()).applyPatch(any(), any());
            verify(userBO, never()).updateUser(any());
            verify(userMapper, never()).toResponseDTO(any());
        }

        @Test
        void testUpdateCurrentUserFailure_EmailConflict() {
            // Arrange
            String newEmail = TestConstants.VALID_EMAIL;
            UserUpdateDTO dto = new UserUpdateDTO(null, null, newEmail, null, null, null);
            User currentUser = TestUtils.createUserEntityWithId();
            User existingUser = TestUtils.createUserEntityWithId(999L);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(userBO.getUserByEmail(newEmail.toLowerCase())).thenReturn(Optional.of(existingUser));

            // Act + Assert
            assertThrows(EmailException.class, () -> userService.updateCurrentUser(dto));
            verify(authenticatedUserProvider).getCurrentUser();
            verify(userBO).getUserByEmail(newEmail.toLowerCase());
            verify(userPatchHandler, never()).applyPatch(any(), any());
            verify(userBO, never()).updateUser(any());
            verify(userMapper, never()).toResponseDTO(any());
        }

        @Test
        void testUpdateCurrentUserNoOp() {
            // Arrange
            UserUpdateDTO dto = new UserUpdateDTO(null, null, null, null, null, null);
            User currentUser = TestUtils.createUserEntityWithId();
            UserResponseDTO responseDTO = TestUtils.createUserResponseDTO();

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(userPatchHandler.applyPatch(currentUser, dto)).thenReturn(false);
            when(userMapper.toResponseDTO(currentUser)).thenReturn(responseDTO);

            // Act
            UserResponseDTO result = userService.updateCurrentUser(dto);

            // Assert
            assertEquals(responseDTO, result);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(userBO, never()).updateUser(any());
            verify(userMapper).toResponseDTO(currentUser);
        }
    }

    @Nested
    class DeleteUserTests {

        @Test
        void testDeleteUserSuccess() {
            // Arrange
            User existingUser = TestUtils.createUserEntityWithId();

            when(userBO.getUserById(TestConstants.USER_ID)).thenReturn(Optional.of(existingUser));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(existingUser);
            doNothing().when(ownershipValidator).validateUserOwnership(existingUser.getId(), TestConstants.USER_ID);
            doAnswer(invocation -> {
                existingUser.setDeleted(true);
                existingUser.setActive(false);
                return null;
            }).when(userBO).deleteUser(existingUser);

            // Act
            userService.deleteUser(TestConstants.USER_ID);

            // Assert
            verify(userBO).getUserById(TestConstants.USER_ID);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateUserOwnership(existingUser.getId(), TestConstants.USER_ID);
            verify(userBO).deleteUser(existingUser);
            assertTrue(existingUser.isDeleted());
            assertFalse(existingUser.isActive());
        }

        @Test
        void testDeleteUserFailure_UnauthorizedAccess() {
            // Arrange
            User existingUser = TestUtils.createUserEntityWithId();

            when(userBO.getUserById(TestConstants.USER_ID)).thenReturn(Optional.of(existingUser));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(existingUser);
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, TestConstants.USER_ID))
                    .when(ownershipValidator).validateUserOwnership(existingUser.getId(), TestConstants.USER_ID);

            // Act + Assert
            assertThrows(UserOwnershipException.class, () -> userService.deleteUser(TestConstants.USER_ID));

            verify(userBO).getUserById(TestConstants.USER_ID);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateUserOwnership(existingUser.getId(), TestConstants.USER_ID);
            verify(userBO, never()).deleteUser(any());
        }

        @Test
        void testDeleteUserFailure_UserNotFound() {
            // Arrange
            when(userBO.getUserById(TestConstants.USER_ID)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(UserNotFoundException.class, () -> userService.deleteUser(TestConstants.USER_ID));

            verify(userBO).getUserById(TestConstants.USER_ID);
            verify(authenticatedUserProvider, never()).getCurrentUser();
            verify(ownershipValidator, never()).validateUserOwnership(any(), any());
            verify(userBO, never()).deleteUser(any());
        }
    }

    @Nested
    class DeleteCurrentUserTests {

        @Test
        void testDeleteCurrentUserSuccess() {
            // Arrange
            User currentUser = TestUtils.createUserEntityWithId();
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(currentUser);
            doAnswer(invocation -> {
                currentUser.setDeleted(true);
                currentUser.setActive(false);
                return null;
            }).when(userBO).deleteUser(currentUser);

            // Act
            userService.deleteCurrentUser();

            // Assert
            verify(authenticatedUserProvider).getCurrentUser();
            verify(userBO).deleteUser(currentUser);
            assertTrue(currentUser.isDeleted());
            assertFalse(currentUser.isActive());
        }
    }

    @Nested
    class ExistsByUsernameTests {

        @Test
        void testExistsByUsername_ReturnsTrue_EvenWithDifferentCase() {
            // Arrange
            String storedUsername = "storedUsername";
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
            String input = "nonexistent";
            when(userBO.existsByUsername(input)).thenReturn(false);

            // Act
            boolean result = userService.existsByUsername(input);

            // Assert
            assertFalse(result);
            verify(userBO).existsByUsername(input);
        }
    }

    @Nested
    class ExistsByEmailTests {

        @Test
        void testExistsByEmail_ReturnsTrue_EvenWithDifferentCase() {
            // Arrange
            String inputEmail = "TestEmail@Example.com";
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
            when(userBO.existsByEmail(inputEmail.toLowerCase())).thenReturn(false);

            // Act
            boolean result = userService.existsByEmail(inputEmail);

            // Assert
            assertFalse(result);
            verify(userBO).existsByEmail(inputEmail.toLowerCase());
        }
    }

    @Nested
    class CountActiveUsersTests {

        @Test
        void testCountActiveUsersSuccess() {
            // Arrange
            long expectedCount = 37L;
            when(userBO.countActiveUsers()).thenReturn(expectedCount);

            // Act
            long result = userService.countActiveUsers();

            // Assert
            assertEquals(expectedCount, result);
            verify(userBO).countActiveUsers();
        }
    }

    @Nested
    class CountUsersTests {

        @Test
        void testCountUsersSuccess() {
            // Arrange
            long expectedCount = 42L;
            when(userBO.countUsers()).thenReturn(expectedCount);

            // Act
            long result = userService.countUsers();

            // Assert
            assertEquals(expectedCount, result);
            verify(userBO).countUsers();
        }
    }
}