package com.yohan.event_planner.business;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.Role;
import com.yohan.event_planner.repository.UserRepository;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserBOImplTest {

    private UserRepository userRepository;

    private UserBOImpl userBO;

    @BeforeEach
    void setUp() {
        this.userRepository = mock(UserRepository.class);

        userBO = new UserBOImpl(userRepository);
    }

    @Nested
    class GetUserByIdTests {

        @Test
        void testGetUserById_userExists_returnsUser() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();

            // Mocks
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            // Act
            Optional<User> result = userBO.getUserById(user.getId());

            // Assert
            assertTrue(result.isPresent());
            assertEquals(user.getId(), result.get().getId());
            verify(userRepository).findById(user.getId());
        }

        @Test
        void testGetUserById_userNotFound_returnsEmptyOptional() {
            // Arrange
            Long userId = 123L;

            // Mocks
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act
            Optional<User> result = userBO.getUserById(userId);

            // Assert
            assertTrue(result.isEmpty());
            verify(userRepository).findById(userId);
        }
    }

    @Nested
    class GetUserByUsernameTests {

        @Test
        void testGetUserByUsername_userExists_returnsUser() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();

            // Mocks
            when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

            // Act
            Optional<User> result = userBO.getUserByUsername(user.getUsername());

            // Assert
            assertTrue(result.isPresent());
            assertEquals(user.getUsername(), result.get().getUsername());
            verify(userRepository).findByUsername(user.getUsername());
        }

        @Test
        void testGetUserByUsername_userNotFound_returnsEmptyOptional() {
            // Arrange
            String username = "nonexistent_user";

            // Mocks
            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            // Act
            Optional<User> result = userBO.getUserByUsername(username);

            // Assert
            assertTrue(result.isEmpty());
            verify(userRepository).findByUsername(username);
        }
    }

    @Nested
    class GetUserByEmailTests {

        @Test
        void testGetUserByEmail_userExists_returnsUser() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();

            // Mocks
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            // Act
            Optional<User> result = userBO.getUserByEmail(user.getEmail());

            // Assert
            assertTrue(result.isPresent());
            assertEquals(user.getEmail(), result.get().getEmail());
            verify(userRepository).findByEmail(user.getEmail());
        }

        @Test
        void testGetUserByEmail_userNotFound_returnsEmptyOptional() {
            // Arrange
            String email = "notfound@example.com";

            // Mocks
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            // Act
            Optional<User> result = userBO.getUserByEmail(email);

            // Assert
            assertTrue(result.isEmpty());
            verify(userRepository).findByEmail(email);
        }
    }

    @Nested
    class GetUserByRoleTests {

        @Test
        void testGetUsersByRole_usersExist_returnsMatchingUsers() {
            // Arrange
            Role role = Role.USER;
            User user1 = TestUtils.createUserEntityWithId();
            User user2 = TestUtils.createUserEntityWithId();
            user1.addRole(role);
            user2.addRole(role);
            List<User> users = List.of(user1, user2);

            // Mocks
            when(userRepository.findAllByRoles(role)).thenReturn(users);

            // Act
            List<User> result = userBO.getUsersByRole(role);

            // Assert
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(u -> u.getRoles().contains(role)));
            verify(userRepository).findAllByRoles(role);
        }

        @Test
        void testGetUsersByRole_noUsersForRole_returnsEmptyList() {
            // Arrange
            Role role = Role.ADMIN;

            // Mocks
            when(userRepository.findAllByRoles(role)).thenReturn(List.of());

            // Act
            List<User> result = userBO.getUsersByRole(role);

            // Assert
            assertTrue(result.isEmpty());
            verify(userRepository).findAllByRoles(role);
        }
    }

    @Nested
    class GetAllUsersTests {

        @Test
        void testGetAllUsers_multipleUsersExist_returnsAllUsers() {
            // Arrange
            User user1 = TestUtils.createUserEntityWithId();
            User user2 = TestUtils.createUserEntityWithId();
            List<User> users = List.of(user1, user2);

            // Mocks
            when(userRepository.findAll()).thenReturn(users);

            // Act
            List<User> result = userBO.getAllUsers();

            // Assert
            assertEquals(2, result.size());
            assertTrue(result.contains(user1));
            assertTrue(result.contains(user2));
            verify(userRepository).findAll();
        }

        @Test
        void testGetAllUsers_noUsersExist_returnsEmptyList() {
            // Mocks
            when(userRepository.findAll()).thenReturn(List.of());

            // Act
            List<User> result = userBO.getAllUsers();

            // Assert
            assertTrue(result.isEmpty());
            verify(userRepository).findAll();
        }
    }

    @Nested
    class CreateUserTests {

        @Test
        void testCreateUser_validUser_savesAndReturnsUserWithId() {
            // Arrange
            User unsavedUser = TestUtils.createUserEntityWithoutId(); // ID is null
            User savedUser = TestUtils.createUserEntityWithId(); // ID is set
            when(userRepository.save(unsavedUser)).thenReturn(savedUser);

            // Act
            User result = userBO.createUser(unsavedUser);

            // Assert
            assertNotNull(result.getId());
            assertEquals(savedUser.getUsername(), result.getUsername());
            verify(userRepository).save(unsavedUser);
        }
    }

    @Nested
    class UpdateUserTests {

        @Test
        void testUpdateUser_validUser_updatesAndReturnsUser() {
            // Arrange
            User updatedUser = TestUtils.createUserEntityWithId();
            when(userRepository.save(updatedUser)).thenReturn(updatedUser);

            // Act
            User result = userBO.updateUser(updatedUser);

            // Assert
            assertEquals(updatedUser.getId(), result.getId());
            assertEquals(updatedUser.getEmail(), result.getEmail());
            verify(userRepository).save(updatedUser);
        }
    }

    @Nested
    class DeleteUserTests {

        @Test
        void testDeleteUser_marksUserAsDeletedAndInactiveAndSaves() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            assertFalse(user.isDeleted()); // sanity check
            assertTrue(user.isActive());   // sanity check

            // Mocks
            when(userRepository.save(user)).thenReturn(user);

            // Act
            userBO.deleteUser(user);

            // Assert
            assertTrue(user.isDeleted());
            assertFalse(user.isActive());
            verify(userRepository).save(user);
        }
    }

    @Nested
    class ExistsByUsernameTests {

        @Test
        void testExistsByUsername_userExists_returnsTrue() {
            // Arrange
            String username = "existing_user";
            when(userRepository.existsByUsername(username)).thenReturn(true);

            // Act
            boolean result = userBO.existsByUsername(username);

            // Assert
            assertTrue(result);
            verify(userRepository).existsByUsername(username);
        }

        @Test
        void testExistsByUsername_userDoesNotExist_returnsFalse() {
            // Arrange
            String username = "nonexistent_user";
            when(userRepository.existsByUsername(username)).thenReturn(false);

            // Act
            boolean result = userBO.existsByUsername(username);

            // Assert
            assertFalse(result);
            verify(userRepository).existsByUsername(username);
        }
    }

    @Nested
    class ExistsByEmailTests {

        @Test
        void testExistsByEmail_userExists_returnsTrue() {
            // Arrange
            String email = "exists@example.com";
            when(userRepository.existsByEmail(email)).thenReturn(true);

            // Act
            boolean result = userBO.existsByEmail(email);

            // Assert
            assertTrue(result);
            verify(userRepository).existsByEmail(email);
        }

        @Test
        void testExistsByEmail_userDoesNotExist_returnsFalse() {
            // Arrange
            String email = "notfound@example.com";
            when(userRepository.existsByEmail(email)).thenReturn(false);

            // Act
            boolean result = userBO.existsByEmail(email);

            // Assert
            assertFalse(result);
            verify(userRepository).existsByEmail(email);
        }
    }

    @Nested
    class CountActiveUsersTests {

        @Test
        void testCountActiveUsers_excludesDeletedUsers() {
            // Mocks
            when(userRepository.countByDeletedFalse()).thenReturn(1L);

            // Act
            long count = userBO.countActiveUsers();

            // Assert
            assertEquals(1L, count);
            verify(userRepository).countByDeletedFalse();
        }
    }

    @Nested
    class CountUsersTests {

        @Test
        void testCountUsers_returnsCorrectCount() {
            // Arrange
            long expectedCount = 42L;
            when(userRepository.count()).thenReturn(expectedCount);

            // Act
            long result = userBO.countUsers();

            // Assert
            assertEquals(expectedCount, result);
            verify(userRepository).count();
        }
    }
}
