package com.yohan.event_planner.security;

import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.Role;
import com.yohan.event_planner.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserBO userBO;

    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new UserDetailsServiceImpl(userBO);
    }

    @Nested
    class LoadUserByUsernameTests {

        @Test
        void loadUserByUsername_existingUser_returnsCustomUserDetails() {
            // Arrange
            String email = "test@example.com";
            User mockUser = createMockUser(1L, "testuser", email);
            when(userBO.getUserByEmail(email)).thenReturn(Optional.of(mockUser));

            // Act
            UserDetails result = userDetailsService.loadUserByUsername(email);

            // Assert
            assertInstanceOf(CustomUserDetails.class, result);
            CustomUserDetails customUserDetails = (CustomUserDetails) result;
            assertSame(mockUser, customUserDetails.getUser());
            assertEquals("testuser", result.getUsername());
            verify(userBO).getUserByEmail(email);
        }

        @Test
        void loadUserByUsername_userNotFound_throwsUsernameNotFoundException() {
            // Arrange
            String email = "nonexistent@example.com";
            when(userBO.getUserByEmail(email)).thenReturn(Optional.empty());

            // Act & Assert
            UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(email)
            );

            assertEquals("User with email nonexistent@example.com not found", exception.getMessage());
            verify(userBO).getUserByEmail(email);
        }

        @Test
        void loadUserByUsername_emailAsUsername_usesEmailForLookup() {
            // Arrange
            String email = "user@domain.org";
            User mockUser = mock(User.class);
            when(userBO.getUserByEmail(email)).thenReturn(Optional.of(mockUser));

            // Act
            UserDetails result = userDetailsService.loadUserByUsername(email);

            // Assert
            assertInstanceOf(CustomUserDetails.class, result);
            verify(userBO).getUserByEmail(email);
        }

        @Test
        void loadUserByUsername_userWithMultipleRoles_preservesAllRoles() {
            // Arrange
            String email = "admin@example.com";
            User mockUser = mock(User.class);
            when(mockUser.getRoles()).thenReturn(Set.of(Role.USER, Role.ADMIN));
            when(userBO.getUserByEmail(email)).thenReturn(Optional.of(mockUser));

            // Act
            UserDetails result = userDetailsService.loadUserByUsername(email);

            // Assert
            CustomUserDetails customUserDetails = (CustomUserDetails) result;
            assertEquals(2, result.getAuthorities().size());
            assertTrue(result.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
            assertTrue(result.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
        }

        @Test
        void loadUserByUsername_nullEmail_throwsUsernameNotFoundException() {
            // Arrange
            when(userBO.getUserByEmail(null)).thenReturn(Optional.empty());

            // Act & Assert
            UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(null)
            );

            assertEquals("User with email null not found", exception.getMessage());
        }

        @Test
        void loadUserByUsername_emptyEmail_throwsUsernameNotFoundException() {
            // Arrange
            String email = "";
            when(userBO.getUserByEmail(email)).thenReturn(Optional.empty());

            // Act & Assert
            UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(email)
            );

            assertEquals("User with email  not found", exception.getMessage());
        }
    }

    @Nested
    class LoadUserByUserIdTests {

        @Test
        void loadUserByUserId_existingUser_returnsCustomUserDetails() {
            // Arrange
            Long userId = 123L;
            User mockUser = createMockUserForId(userId);
            when(mockUser.getId()).thenReturn(userId);
            when(userBO.getUserById(userId)).thenReturn(Optional.of(mockUser));

            // Act
            UserDetails result = userDetailsService.loadUserByUserId(userId);

            // Assert
            assertInstanceOf(CustomUserDetails.class, result);
            CustomUserDetails customUserDetails = (CustomUserDetails) result;
            assertSame(mockUser, customUserDetails.getUser());
            assertEquals(userId, customUserDetails.getUserId());
            verify(userBO).getUserById(userId);
        }

        @Test
        void loadUserByUserId_userNotFound_throwsUserNotFoundException() {
            // Arrange
            Long userId = 999L;
            when(userBO.getUserById(userId)).thenReturn(Optional.empty());

            // Act & Assert
            UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userDetailsService.loadUserByUserId(userId)
            );

            // UserNotFoundException constructor creates the message
            assertTrue(exception.getMessage().contains("999"));
            verify(userBO).getUserById(userId);
        }

        @Test
        void loadUserByUserId_userWithNoRoles_returnsUserDetailsWithEmptyAuthorities() {
            // Arrange
            Long userId = 456L;
            User mockUser = createMockUserForId(userId);
            when(mockUser.getRoles()).thenReturn(Set.of());
            when(userBO.getUserById(userId)).thenReturn(Optional.of(mockUser));

            // Act
            UserDetails result = userDetailsService.loadUserByUserId(userId);

            // Assert
            CustomUserDetails customUserDetails = (CustomUserDetails) result;
            assertTrue(result.getAuthorities().isEmpty());
        }

        @Test
        void loadUserByUserId_nullUserId_throwsUserNotFoundException() {
            // Arrange
            when(userBO.getUserById(null)).thenReturn(Optional.empty());

            // Act & Assert
            UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userDetailsService.loadUserByUserId(null)
            );

            assertTrue(exception.getMessage().contains("null"));
        }

        @Test
        void loadUserByUserId_userWithSingleRole_returnsCorrectAuthorities() {
            // Arrange
            Long userId = 789L;
            User mockUser = createMockUserForId(userId);
            when(mockUser.getRoles()).thenReturn(Set.of(Role.MODERATOR));
            when(userBO.getUserById(userId)).thenReturn(Optional.of(mockUser));

            // Act
            UserDetails result = userDetailsService.loadUserByUserId(userId);

            // Assert
            assertEquals(1, result.getAuthorities().size());
            assertTrue(result.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_MODERATOR")));
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        void loadUserByUsername_userBOThrowsException_propagatesException() {
            // Arrange
            String email = "error@example.com";
            when(userBO.getUserByEmail(email)).thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userDetailsService.loadUserByUsername(email)
            );

            assertEquals("Database error", exception.getMessage());
        }

        @Test
        void loadUserByUserId_userBOThrowsException_propagatesException() {
            // Arrange
            Long userId = 100L;
            when(userBO.getUserById(userId)).thenThrow(new RuntimeException("Database connection failed"));

            // Act & Assert
            RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userDetailsService.loadUserByUserId(userId)
            );

            assertEquals("Database connection failed", exception.getMessage());
        }

        @Test
        void loadUserByUsername_specialCharactersInEmail_handlesCorrectly() {
            // Arrange
            String email = "user+test@example-domain.co.uk";
            User mockUser = mock(User.class);
            when(userBO.getUserByEmail(email)).thenReturn(Optional.of(mockUser));

            // Act
            UserDetails result = userDetailsService.loadUserByUsername(email);

            // Assert
            assertInstanceOf(CustomUserDetails.class, result);
            verify(userBO).getUserByEmail(email);
        }
    }

    private User createMockUser(Long id, String username, String email) {
        User mockUser = mock(User.class);
        when(mockUser.getUsername()).thenReturn(username);
        return mockUser;
    }

    private User createMockUserWithRoles(Long id, String username, String email, Set<Role> roles) {
        User mockUser = mock(User.class);
        when(mockUser.getRoles()).thenReturn(roles);
        return mockUser;
    }

    private User createMockUserForId(Long id) {
        User mockUser = mock(User.class);
        return mockUser;
    }
}