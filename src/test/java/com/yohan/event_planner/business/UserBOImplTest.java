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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Nested
    class EdgeCaseTests {

        @Test
        void testGetUserById_userWithPendingDeletion_stillReturnsUser() {
            // Arrange
            User user = TestUtils.createValidUserEntityWithId();
            user.markForDeletion(ZonedDateTime.now(fixedClock));

            // Mocks - getUserById doesn't filter by pending deletion
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            // Act
            Optional<User> result = userBO.getUserById(user.getId());

            // Assert
            assertTrue(result.isPresent());
            assertTrue(result.get().isPendingDeletion());
            assertEquals(user.getId(), result.get().getId());
            verify(userRepository).findById(user.getId());
        }

        @Test
        void testGetAllUsers_withLargeDataset_returnsAllActiveUsers() {
            // Arrange - simulate larger dataset
            List<User> users = List.of(
                TestUtils.createValidUserEntityWithId(),
                TestUtils.createValidUserEntityWithId(),
                TestUtils.createValidUserEntityWithId(),
                TestUtils.createValidUserEntityWithId(),
                TestUtils.createValidUserEntityWithId()
            );

            // Mocks
            when(userRepository.findAllByIsPendingDeletionFalse()).thenReturn(users);

            // Act
            List<User> result = userBO.getAllUsers();

            // Assert
            assertEquals(5, result.size());
            assertEquals(users, result);
            verify(userRepository).findAllByIsPendingDeletionFalse();
        }

        @Test
        void testGetUsersByRole_withLargeRoleDataset_returnsMatchingUsers() {
            // Arrange
            Role role = Role.USER;
            List<User> users = List.of(
                TestUtils.createValidUserEntityWithId(),
                TestUtils.createValidUserEntityWithId(),
                TestUtils.createValidUserEntityWithId()
            );
            users.forEach(user -> user.addRole(role));

            // Mocks
            when(userRepository.findAllByRolesAndIsPendingDeletionFalse(role)).thenReturn(users);

            // Act
            List<User> result = userBO.getUsersByRole(role);

            // Assert
            assertEquals(3, result.size());
            assertTrue(result.stream().allMatch(u -> u.getRoles().contains(role)));
            verify(userRepository).findAllByRolesAndIsPendingDeletionFalse(role);
        }
    }

    @Nested 
    class BoundaryValueTests {

        @Test
        void testGetUserById_withMinimumValidId_returnsUser() {
            // Arrange
            Long minId = 1L;
            User user = TestUtils.createValidUserEntityWithId();
            when(userRepository.findById(minId)).thenReturn(Optional.of(user));

            // Act
            Optional<User> result = userBO.getUserById(minId);

            // Assert
            assertTrue(result.isPresent());
            verify(userRepository).findById(minId);
        }

        @Test
        void testGetUserById_withVeryLargeId_handlesCorrectly() {
            // Arrange
            Long largeId = Long.MAX_VALUE;
            when(userRepository.findById(largeId)).thenReturn(Optional.empty());

            // Act
            Optional<User> result = userBO.getUserById(largeId);

            // Assert
            assertTrue(result.isEmpty());
            verify(userRepository).findById(largeId);
        }

        @Test
        void testGetUserByUsername_withMinimumLengthUsername_returnsUser() {
            // Arrange
            String shortUsername = "a";
            User user = TestUtils.createValidUserEntityWithId();
            when(userRepository.findByUsernameAndIsPendingDeletionFalse(shortUsername)).thenReturn(Optional.of(user));

            // Act
            Optional<User> result = userBO.getUserByUsername(shortUsername);

            // Assert
            assertTrue(result.isPresent());
            verify(userRepository).findByUsernameAndIsPendingDeletionFalse(shortUsername);
        }

        @Test
        void testGetUserByUsername_withMaximumLengthUsername_handlesCorrectly() {
            // Arrange - Very long username (255 characters)
            String longUsername = "a".repeat(255);
            when(userRepository.findByUsernameAndIsPendingDeletionFalse(longUsername)).thenReturn(Optional.empty());

            // Act
            Optional<User> result = userBO.getUserByUsername(longUsername);

            // Assert
            assertTrue(result.isEmpty());
            verify(userRepository).findByUsernameAndIsPendingDeletionFalse(longUsername);
        }

        @Test
        void testGetUserByEmail_withMinimumValidEmail_returnsUser() {
            // Arrange
            String shortEmail = "a@b.c";
            User user = TestUtils.createValidUserEntityWithId();
            when(userRepository.findByEmailAndIsPendingDeletionFalse(shortEmail)).thenReturn(Optional.of(user));

            // Act
            Optional<User> result = userBO.getUserByEmail(shortEmail);

            // Assert
            assertTrue(result.isPresent());
            verify(userRepository).findByEmailAndIsPendingDeletionFalse(shortEmail);
        }

        @Test
        void testGetUserByEmail_withVeryLongEmail_handlesCorrectly() {
            // Arrange - Very long email (320 characters max according to RFC)
            String longLocalPart = "a".repeat(64);
            String longDomain = "b".repeat(60) + ".example.com";
            String longEmail = longLocalPart + "@" + longDomain;
            when(userRepository.findByEmailAndIsPendingDeletionFalse(longEmail)).thenReturn(Optional.empty());

            // Act
            Optional<User> result = userBO.getUserByEmail(longEmail);

            // Assert
            assertTrue(result.isEmpty());
            verify(userRepository).findByEmailAndIsPendingDeletionFalse(longEmail);
        }

        @Test
        void testGetAllUsers_withExactlyOneUser_returnsCorrectList() {
            // Arrange
            User singleUser = TestUtils.createValidUserEntityWithId();
            List<User> users = List.of(singleUser);
            when(userRepository.findAllByIsPendingDeletionFalse()).thenReturn(users);

            // Act
            List<User> result = userBO.getAllUsers();

            // Assert
            assertEquals(1, result.size());
            assertEquals(singleUser, result.get(0));
            verify(userRepository).findAllByIsPendingDeletionFalse();
        }

        @Test
        void testGetUsersByRole_withSingleUserHavingRole_returnsOneUser() {
            // Arrange
            Role role = Role.ADMIN;
            User user = TestUtils.createValidUserEntityWithId();
            user.addRole(role);
            List<User> users = List.of(user);
            when(userRepository.findAllByRolesAndIsPendingDeletionFalse(role)).thenReturn(users);

            // Act
            List<User> result = userBO.getUsersByRole(role);

            // Assert
            assertEquals(1, result.size());
            assertTrue(result.get(0).getRoles().contains(role));
            verify(userRepository).findAllByRolesAndIsPendingDeletionFalse(role);
        }

        @Test
        void testExistsByUsername_withEmptyStringUsername_returnsFalse() {
            // Arrange
            String emptyUsername = "";
            when(userRepository.existsByUsername(emptyUsername)).thenReturn(false);

            // Act
            boolean result = userBO.existsByUsername(emptyUsername);

            // Assert
            assertFalse(result);
            verify(userRepository).existsByUsername(emptyUsername);
        }

        @Test
        void testExistsByEmail_withEmptyStringEmail_returnsFalse() {
            // Arrange
            String emptyEmail = "";
            when(userRepository.existsByEmail(emptyEmail)).thenReturn(false);

            // Act
            boolean result = userBO.existsByEmail(emptyEmail);

            // Assert
            assertFalse(result);
            verify(userRepository).existsByEmail(emptyEmail);
        }
    }

    @Nested
    class EnhancedExceptionScenarioTests {

        @Test
        void testCreateUser_withRepositoryException_propagatesException() {
            // Arrange
            User user = TestUtils.createValidUserEntityWithId();
            RuntimeException repositoryException = new RuntimeException("Database connection failed");
            when(userRepository.save(user)).thenThrow(repositoryException);

            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> userBO.createUser(user));
            assertEquals("Database connection failed", thrown.getMessage());
            verify(userRepository).save(user);
        }

        @Test
        void testUpdateUser_withRepositoryException_propagatesException() {
            // Arrange
            User user = TestUtils.createValidUserEntityWithId();
            RuntimeException repositoryException = new RuntimeException("Constraint violation");
            when(userRepository.save(user)).thenThrow(repositoryException);

            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> userBO.updateUser(user));
            assertEquals("Constraint violation", thrown.getMessage());
            verify(userRepository).save(user);
        }

        @Test
        void testMarkUserForDeletion_withClockProviderException_propagatesException() {
            // Arrange
            User user = TestUtils.createValidUserEntityWithId();
            RuntimeException clockException = new RuntimeException("Clock provider failed");
            when(clockProvider.getClockForUser(user)).thenThrow(clockException);

            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> userBO.markUserForDeletion(user));
            assertEquals("Clock provider failed", thrown.getMessage());
            verify(clockProvider).getClockForUser(user);
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void testMarkUserForDeletion_withRepositorySaveException_propagatesException() {
            // Arrange
            User user = TestUtils.createValidUserEntityWithId();
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);
            RuntimeException saveException = new RuntimeException("Save operation failed");
            when(userRepository.save(user)).thenThrow(saveException);

            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> userBO.markUserForDeletion(user));
            assertEquals("Save operation failed", thrown.getMessage());
            verify(clockProvider).getClockForUser(user);
            verify(userRepository).save(user);
        }

        @Test
        void testGetUserById_withRepositoryException_propagatesException() {
            // Arrange
            Long userId = 123L;
            RuntimeException repositoryException = new RuntimeException("Database query failed");
            when(userRepository.findById(userId)).thenThrow(repositoryException);

            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> userBO.getUserById(userId));
            assertEquals("Database query failed", thrown.getMessage());
            verify(userRepository).findById(userId);
        }

        @Test
        void testGetUserByUsername_withRepositoryException_propagatesException() {
            // Arrange
            String username = "testuser";
            RuntimeException repositoryException = new RuntimeException("Query timeout");
            when(userRepository.findByUsernameAndIsPendingDeletionFalse(username)).thenThrow(repositoryException);

            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> userBO.getUserByUsername(username));
            assertEquals("Query timeout", thrown.getMessage());
            verify(userRepository).findByUsernameAndIsPendingDeletionFalse(username);
        }

        @Test
        void testGetUserByEmail_withRepositoryException_propagatesException() {
            // Arrange
            String email = "test@example.com";
            RuntimeException repositoryException = new RuntimeException("Index corruption");
            when(userRepository.findByEmailAndIsPendingDeletionFalse(email)).thenThrow(repositoryException);

            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> userBO.getUserByEmail(email));
            assertEquals("Index corruption", thrown.getMessage());
            verify(userRepository).findByEmailAndIsPendingDeletionFalse(email);
        }

        @Test
        void testGetUsersByRole_withRepositoryException_propagatesException() {
            // Arrange
            Role role = Role.USER;
            RuntimeException repositoryException = new RuntimeException("Connection pool exhausted");
            when(userRepository.findAllByRolesAndIsPendingDeletionFalse(role)).thenThrow(repositoryException);

            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> userBO.getUsersByRole(role));
            assertEquals("Connection pool exhausted", thrown.getMessage());
            verify(userRepository).findAllByRolesAndIsPendingDeletionFalse(role);
        }

        @Test
        void testGetAllUsers_withRepositoryException_propagatesException() {
            // Arrange
            RuntimeException repositoryException = new RuntimeException("Memory limit exceeded");
            when(userRepository.findAllByIsPendingDeletionFalse()).thenThrow(repositoryException);

            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> userBO.getAllUsers());
            assertEquals("Memory limit exceeded", thrown.getMessage());
            verify(userRepository).findAllByIsPendingDeletionFalse();
        }

        @Test
        void testExistsByUsername_withRepositoryException_propagatesException() {
            // Arrange
            String username = "testuser";
            RuntimeException repositoryException = new RuntimeException("Database locked");
            when(userRepository.existsByUsername(username)).thenThrow(repositoryException);

            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> userBO.existsByUsername(username));
            assertEquals("Database locked", thrown.getMessage());
            verify(userRepository).existsByUsername(username);
        }

        @Test
        void testExistsByEmail_withRepositoryException_propagatesException() {
            // Arrange
            String email = "test@example.com";
            RuntimeException repositoryException = new RuntimeException("Transaction rollback");
            when(userRepository.existsByEmail(email)).thenThrow(repositoryException);

            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> userBO.existsByEmail(email));
            assertEquals("Transaction rollback", thrown.getMessage());
            verify(userRepository).existsByEmail(email);
        }
    }

    @Nested
    class ValidationEdgeCaseTests {

        @Test
        void testGetUserByUsername_withNullUsername_handlesGracefully() {
            // Arrange
            String nullUsername = null;
            when(userRepository.findByUsernameAndIsPendingDeletionFalse(nullUsername)).thenReturn(Optional.empty());

            // Act
            Optional<User> result = userBO.getUserByUsername(nullUsername);

            // Assert
            assertTrue(result.isEmpty());
            verify(userRepository).findByUsernameAndIsPendingDeletionFalse(nullUsername);
        }

        @Test
        void testGetUserByEmail_withNullEmail_handlesGracefully() {
            // Arrange
            String nullEmail = null;
            when(userRepository.findByEmailAndIsPendingDeletionFalse(nullEmail)).thenReturn(Optional.empty());

            // Act
            Optional<User> result = userBO.getUserByEmail(nullEmail);

            // Assert
            assertTrue(result.isEmpty());
            verify(userRepository).findByEmailAndIsPendingDeletionFalse(nullEmail);
        }

        @Test
        void testGetUsersByRole_withNullRole_handlesGracefully() {
            // Arrange
            Role nullRole = null;
            when(userRepository.findAllByRolesAndIsPendingDeletionFalse(nullRole)).thenReturn(List.of());

            // Act
            List<User> result = userBO.getUsersByRole(nullRole);

            // Assert
            assertTrue(result.isEmpty());
            verify(userRepository).findAllByRolesAndIsPendingDeletionFalse(nullRole);
        }

        @Test
        void testExistsByUsername_withNullUsername_returnsFalse() {
            // Arrange
            String nullUsername = null;
            when(userRepository.existsByUsername(nullUsername)).thenReturn(false);

            // Act
            boolean result = userBO.existsByUsername(nullUsername);

            // Assert
            assertFalse(result);
            verify(userRepository).existsByUsername(nullUsername);
        }

        @Test
        void testExistsByEmail_withNullEmail_returnsFalse() {
            // Arrange
            String nullEmail = null;
            when(userRepository.existsByEmail(nullEmail)).thenReturn(false);

            // Act
            boolean result = userBO.existsByEmail(nullEmail);

            // Assert
            assertFalse(result);
            verify(userRepository).existsByEmail(nullEmail);
        }

        @Test
        void testGetUserByUsername_withSpecialCharacters_handlesCorrectly() {
            // Arrange
            String specialUsername = "user@#$%^&*()_+-=[]{}|;':\",./<>?`~";
            User user = TestUtils.createValidUserEntityWithId();
            when(userRepository.findByUsernameAndIsPendingDeletionFalse(specialUsername)).thenReturn(Optional.of(user));

            // Act
            Optional<User> result = userBO.getUserByUsername(specialUsername);

            // Assert
            assertTrue(result.isPresent());
            verify(userRepository).findByUsernameAndIsPendingDeletionFalse(specialUsername);
        }

        @Test
        void testGetUserByEmail_withInternationalCharacters_handlesCorrectly() {
            // Arrange
            String internationalEmail = "测试@example.com";
            User user = TestUtils.createValidUserEntityWithId();
            when(userRepository.findByEmailAndIsPendingDeletionFalse(internationalEmail)).thenReturn(Optional.of(user));

            // Act
            Optional<User> result = userBO.getUserByEmail(internationalEmail);

            // Assert
            assertTrue(result.isPresent());
            verify(userRepository).findByEmailAndIsPendingDeletionFalse(internationalEmail);
        }

        @Test
        void testCreateUser_withNullUser_propagatesNullPointerException() {
            // Arrange
            User nullUser = null;

            // Act & Assert
            // NPE will be thrown when trying to access user.getUsername() in logging
            NullPointerException thrown = assertThrows(NullPointerException.class, () -> userBO.createUser(nullUser));
            assertNotNull(thrown);
            // Repository should never be called since NPE occurs during logging
            verifyNoInteractions(userRepository);
        }

        @Test
        void testUpdateUser_withNullUser_propagatesNullPointerException() {
            // Arrange
            User nullUser = null;

            // Act & Assert
            // NPE will be thrown when trying to access user.getId() in logging
            NullPointerException thrown = assertThrows(NullPointerException.class, () -> userBO.updateUser(nullUser));
            assertNotNull(thrown);
            // Repository should never be called since NPE occurs during logging
            verifyNoInteractions(userRepository);
        }

        @Test
        void testMarkUserForDeletion_withNullUser_propagatesNullPointerException() {
            // Arrange
            User nullUser = null;

            // Act & Assert
            NullPointerException thrown = assertThrows(NullPointerException.class, () -> userBO.markUserForDeletion(nullUser));
            assertNotNull(thrown);
            verifyNoInteractions(userRepository);
            verifyNoInteractions(clockProvider);
        }

        @Test
        void testGetUserByUsername_withWhitespaceOnlyUsername_handlesCorrectly() {
            // Arrange
            String whitespaceUsername = "   ";
            when(userRepository.findByUsernameAndIsPendingDeletionFalse(whitespaceUsername)).thenReturn(Optional.empty());

            // Act
            Optional<User> result = userBO.getUserByUsername(whitespaceUsername);

            // Assert
            assertTrue(result.isEmpty());
            verify(userRepository).findByUsernameAndIsPendingDeletionFalse(whitespaceUsername);
        }

        @Test
        void testGetUserByEmail_withWhitespaceOnlyEmail_handlesCorrectly() {
            // Arrange
            String whitespaceEmail = "   ";
            when(userRepository.findByEmailAndIsPendingDeletionFalse(whitespaceEmail)).thenReturn(Optional.empty());

            // Act
            Optional<User> result = userBO.getUserByEmail(whitespaceEmail);

            // Assert
            assertTrue(result.isEmpty());
            verify(userRepository).findByEmailAndIsPendingDeletionFalse(whitespaceEmail);
        }
    }
}
