package com.yohan.event_planner.business;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.Role;
import com.yohan.event_planner.repository.UserRepository;
import com.yohan.event_planner.time.ClockProvider;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static com.yohan.event_planner.util.TestConstants.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class UserBOImplTest {

    private UserRepository userRepository;
    private ClockProvider clockProvider;
    private Clock fixedClock;

    private UserBOImpl userBO;

    @BeforeEach
    void setUp() {
        this.userRepository = mock(UserRepository.class);
        this.clockProvider = mock(ClockProvider.class);

        fixedClock = Clock.fixed(Instant.parse("2025-06-29T12:00:00Z"), ZoneId.of("UTC"));

        userBO = new UserBOImpl(userRepository, clockProvider);
    }

    @Nested
    class GetUserByIdTests {

        @Test
        void testGetUserById_userExists_returnsUser() {
            // Arrange
            User user = TestUtils.createValidUserEntityWithId();

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
            // Mocks
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            // Act
            Optional<User> result = userBO.getUserById(USER_ID);

            // Assert
            assertTrue(result.isEmpty());
            verify(userRepository).findById(USER_ID);
        }
    }

    @Nested
    class GetUserByUsernameTests {

        @Test
        void testGetUserByUsername_userExists_returnsUser() {
            // Arrange
            User user = TestUtils.createValidUserEntityWithId();

            // Mocks
            when(userRepository.findByUsernameAndIsPendingDeletionFalse(user.getUsername())).thenReturn(Optional.of(user));

            // Act
            Optional<User> result = userBO.getUserByUsername(user.getUsername());

            // Assert
            assertTrue(result.isPresent());
            assertEquals(user.getUsername(), result.get().getUsername());
            verify(userRepository).findByUsernameAndIsPendingDeletionFalse(user.getUsername());
        }

        @Test
        void testGetUserByUsername_userNotFound_returnsEmptyOptional() {
            // Arrange
            String username = "nonexistent_user";

            // Mocks
            when(userRepository.findByUsernameAndIsPendingDeletionFalse(username)).thenReturn(Optional.empty());

            // Act
            Optional<User> result = userBO.getUserByUsername(username);

            // Assert
            assertTrue(result.isEmpty());
            verify(userRepository).findByUsernameAndIsPendingDeletionFalse(username);
        }
    }

    @Nested
    class GetUserByEmailTests {

        @Test
        void testGetUserByEmail_userExists_returnsUser() {
            // Arrange
            User user = TestUtils.createValidUserEntityWithId();

            // Mocks
            when(userRepository.findByEmailAndIsPendingDeletionFalse(user.getEmail())).thenReturn(Optional.of(user));

            // Act
            Optional<User> result = userBO.getUserByEmail(user.getEmail());

            // Assert
            assertTrue(result.isPresent());
            assertEquals(user.getEmail(), result.get().getEmail());
            verify(userRepository).findByEmailAndIsPendingDeletionFalse(user.getEmail());
        }

        @Test
        void testGetUserByEmail_userNotFound_returnsEmptyOptional() {
            // Arrange
            String email = "notfound@example.com";

            // Mocks
            when(userRepository.findByEmailAndIsPendingDeletionFalse(email)).thenReturn(Optional.empty());

            // Act
            Optional<User> result = userBO.getUserByEmail(email);

            // Assert
            assertTrue(result.isEmpty());
            verify(userRepository).findByEmailAndIsPendingDeletionFalse(email);
        }
    }

    @Nested
    class GetUsersByRoleTests {

        @Test
        void testGetUsersByRole_usersExist_returnsMatchingUsers() {
            // Arrange
            Role role = Role.USER;
            User user1 = TestUtils.createValidUserEntityWithId();
            User user2 = TestUtils.createValidUserEntityWithId();
            user1.addRole(role);
            user2.addRole(role);
            List<User> users = List.of(user1, user2);

            // Mocks
            when(userRepository.findAllByRolesAndIsPendingDeletionFalse(role)).thenReturn(users);

            // Act
            List<User> result = userBO.getUsersByRole(role);

            // Assert
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(u -> u.getRoles().contains(role)));
            verify(userRepository).findAllByRolesAndIsPendingDeletionFalse(role);
        }

        @Test
        void testGetUsersByRole_noUsersForRole_returnsEmptyList() {
            // Arrange
            Role role = Role.ADMIN;

            // Mocks
            when(userRepository.findAllByRolesAndIsPendingDeletionFalse(role)).thenReturn(List.of());

            // Act
            List<User> result = userBO.getUsersByRole(role);

            // Assert
            assertTrue(result.isEmpty());
            verify(userRepository).findAllByRolesAndIsPendingDeletionFalse(role);
        }
    }

    @Nested
    class GetAllUsersTests {

        @Test
        void testGetAllUsers_multipleUsersExist_returnsAllUsers() {
            // Arrange
            User user1 = TestUtils.createValidUserEntityWithId();
            User user2 = TestUtils.createValidUserEntityWithId();
            List<User> users = List.of(user1, user2);

            // Mocks
            when(userRepository.findAllByIsPendingDeletionFalse()).thenReturn(users);

            // Act
            List<User> result = userBO.getAllUsers();

            // Assert
            assertEquals(2, result.size());
            assertTrue(result.contains(user1));
            assertTrue(result.contains(user2));
            verify(userRepository).findAllByIsPendingDeletionFalse();
        }

        @Test
        void testGetAllUsers_noUsersExist_returnsEmptyList() {
            // Mocks
            when(userRepository.findAllByIsPendingDeletionFalse()).thenReturn(List.of());

            // Act
            List<User> result = userBO.getAllUsers();

            // Assert
            assertTrue(result.isEmpty());
            verify(userRepository).findAllByIsPendingDeletionFalse();
        }
    }

    @Nested
    class CreateUserTests {

        @Test
        void testCreateUser_validUser_savesAndReturnsUserWithId() {
            // Arrange
            User unsavedUser = TestUtils.createValidUserEntityWithId(); // ID is null
            User savedUser = TestUtils.createValidUserEntityWithId(); // ID is set
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
            User updatedUser = TestUtils.createValidUserEntityWithId();
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
    class MarkUserForDeletionTests {

        @Test
        void shouldMarkUserForDeletionWithScheduledDeletionDate30DaysLater() {
            // Arrange
            User user = TestUtils.createValidUserEntityWithId();

            ZonedDateTime fixedNow = ZonedDateTime.now(fixedClock);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            // Act
            userBO.markUserForDeletion(user);

            // Assert
            assertTrue(user.isPendingDeletion());
            assertEquals(fixedNow.plusDays(30), user.getScheduledDeletionDate().orElseThrow());
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
}
