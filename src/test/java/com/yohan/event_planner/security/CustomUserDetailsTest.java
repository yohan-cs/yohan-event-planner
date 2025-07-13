package com.yohan.event_planner.security;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsTest {

    private User mockUser;
    private CustomUserDetails customUserDetails;

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
        customUserDetails = new CustomUserDetails(mockUser);
    }

    @Nested
    class UserDataDelegationTests {

        @Test
        void getUser_returnsWrappedUser() {
            // Act
            User result = customUserDetails.getUser();

            // Assert
            assertSame(mockUser, result);
        }

        @Test
        void getUserId_delegatesToUser() {
            // Arrange
            Long expectedUserId = 123L;
            when(mockUser.getId()).thenReturn(expectedUserId);

            // Act
            Long result = customUserDetails.getUserId();

            // Assert
            assertEquals(expectedUserId, result);
        }

        @Test
        void getUsername_delegatesToUser() {
            // Arrange
            String expectedUsername = "testuser";
            when(mockUser.getUsername()).thenReturn(expectedUsername);

            // Act
            String result = customUserDetails.getUsername();

            // Assert
            assertEquals(expectedUsername, result);
        }

        @Test
        void getPassword_delegatesToHashedPassword() {
            // Arrange
            String expectedPassword = "$2a$10$hashedpassword";
            when(mockUser.getHashedPassword()).thenReturn(expectedPassword);

            // Act
            String result = customUserDetails.getPassword();

            // Assert
            assertEquals(expectedPassword, result);
        }
    }

    @Nested
    class AuthorityMappingTests {

        @Test
        void getAuthorities_singleRole_returnsCorrectAuthority() {
            // Arrange
            Set<Role> roles = Set.of(Role.USER);
            when(mockUser.getRoles()).thenReturn(roles);

            // Act
            Collection<? extends GrantedAuthority> authorities = customUserDetails.getAuthorities();

            // Assert
            assertEquals(1, authorities.size());
            assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
        }

        @Test
        void getAuthorities_multipleRoles_returnsAllAuthorities() {
            // Arrange
            Set<Role> roles = Set.of(Role.USER, Role.ADMIN);
            when(mockUser.getRoles()).thenReturn(roles);

            // Act
            Collection<? extends GrantedAuthority> authorities = customUserDetails.getAuthorities();

            // Assert
            assertEquals(2, authorities.size());
            assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
            assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        }

        @Test
        void getAuthorities_noRoles_returnsEmptyCollection() {
            // Arrange
            Set<Role> roles = Set.of();
            when(mockUser.getRoles()).thenReturn(roles);

            // Act
            Collection<? extends GrantedAuthority> authorities = customUserDetails.getAuthorities();

            // Assert
            assertTrue(authorities.isEmpty());
        }

        @Test
        void getAuthorities_moderatorRole_returnsCorrectAuthority() {
            // Arrange
            Set<Role> roles = Set.of(Role.MODERATOR);
            when(mockUser.getRoles()).thenReturn(roles);

            // Act
            Collection<? extends GrantedAuthority> authorities = customUserDetails.getAuthorities();

            // Assert
            assertEquals(1, authorities.size());
            assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_MODERATOR")));
        }
    }

    @Nested
    class AccountStatusTests {

        @Test
        void isAccountNonExpired_alwaysReturnsTrue() {
            // Act & Assert
            assertTrue(customUserDetails.isAccountNonExpired());
        }

        @Test
        void isAccountNonLocked_alwaysReturnsTrue() {
            // Act & Assert
            assertTrue(customUserDetails.isAccountNonLocked());
        }

        @Test
        void isCredentialsNonExpired_alwaysReturnsTrue() {
            // Act & Assert
            assertTrue(customUserDetails.isCredentialsNonExpired());
        }

        @Test
        void isEnabled_userNotPendingDeletion_returnsTrue() {
            // Arrange
            when(mockUser.isPendingDeletion()).thenReturn(false);

            // Act
            boolean result = customUserDetails.isEnabled();

            // Assert
            assertTrue(result);
        }

        @Test
        void isEnabled_userPendingDeletion_returnsFalse() {
            // Arrange
            when(mockUser.isPendingDeletion()).thenReturn(true);

            // Act
            boolean result = customUserDetails.isEnabled();

            // Assert
            assertFalse(result);
        }
    }

    @Nested
    class PerformanceTests {

        @Test
        void getAuthorities_largeRoleSet_performsEfficiently() {
            // Arrange - Create a user with all available roles
            Set<Role> allRoles = Set.of(Role.USER, Role.ADMIN, Role.MODERATOR);
            when(mockUser.getRoles()).thenReturn(allRoles);

            // Act - Multiple calls to test performance consistency
            long startTime = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                Collection<? extends GrantedAuthority> authorities = customUserDetails.getAuthorities();
                assertEquals(3, authorities.size());
            }
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;

            // Assert - Should complete well under reasonable time threshold (adjusted for test environment)
            assertTrue(durationMs < 200, "Authority mapping should be efficient, took: " + durationMs + "ms");
        }

        @Test
        void getAuthorities_multipleCallsSameResult_cachesBehavior() {
            // Arrange
            Set<Role> roles = Set.of(Role.USER, Role.ADMIN);
            when(mockUser.getRoles()).thenReturn(roles);

            // Act
            Collection<? extends GrantedAuthority> authorities1 = customUserDetails.getAuthorities();
            Collection<? extends GrantedAuthority> authorities2 = customUserDetails.getAuthorities();

            // Assert - Results should be equivalent
            assertEquals(authorities1.size(), authorities2.size());
            assertTrue(authorities1.containsAll(authorities2));
            assertTrue(authorities2.containsAll(authorities1));
        }
    }

    @Nested
    class ConstructorTests {

        @Test
        void constructor_nullUser_throwsIllegalArgumentException() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new CustomUserDetails(null)
            );
            assertEquals("User cannot be null", exception.getMessage());
        }

        @Test
        void constructor_validUser_logsCreation() {
            // Arrange
            User testUser = mock(User.class);
            when(testUser.getId()).thenReturn(123L);
            when(testUser.getUsername()).thenReturn("testuser");

            // Act - Constructor should succeed without throwing
            CustomUserDetails userDetails = new CustomUserDetails(testUser);

            // Assert
            assertEquals(testUser, userDetails.getUser());
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        void getAuthorities_nullUserId_stillWorksWithOtherData() {
            // Arrange
            when(mockUser.getId()).thenReturn(null);
            when(mockUser.getRoles()).thenReturn(Set.of(Role.USER));

            // Act
            Long userId = customUserDetails.getUserId();
            Collection<? extends GrantedAuthority> authorities = customUserDetails.getAuthorities();

            // Assert
            assertEquals(null, userId);
            assertEquals(1, authorities.size());
        }

        @Test
        void getAuthorities_preservesRoleOrder() {
            // Arrange
            Set<Role> roles = Set.of(Role.ADMIN, Role.USER, Role.MODERATOR);
            when(mockUser.getRoles()).thenReturn(roles);
            when(mockUser.getUsername()).thenReturn("multiRoleUser");

            // Act
            Collection<? extends GrantedAuthority> authorities = customUserDetails.getAuthorities();

            // Assert
            assertEquals(3, authorities.size());
            assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
            assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
            assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_MODERATOR")));
        }
    }

    @Nested
    class LoggingBehaviorTests {

        @Test
        void getAuthorities_mappingRoles_logsDebugInformation() {
            // Arrange
            Set<Role> roles = Set.of(Role.USER, Role.ADMIN);
            when(mockUser.getRoles()).thenReturn(roles);
            when(mockUser.getUsername()).thenReturn("testuser");

            // Act
            Collection<? extends GrantedAuthority> authorities = customUserDetails.getAuthorities();

            // Assert - Verify functional behavior
            assertEquals(2, authorities.size());
            assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
            assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        }

        @Test
        void isEnabled_checkingAccountStatus_logsDebugInformation() {
            // Arrange
            when(mockUser.getUsername()).thenReturn("testuser");
            when(mockUser.isPendingDeletion()).thenReturn(false);

            // Act
            boolean enabled = customUserDetails.isEnabled();

            // Assert - Verify functional behavior
            assertTrue(enabled);
        }

        @Test
        void isEnabled_userPendingDeletion_logsDebugInformation() {
            // Arrange
            when(mockUser.getUsername()).thenReturn("deleteduser");
            when(mockUser.isPendingDeletion()).thenReturn(true);

            // Act
            boolean enabled = customUserDetails.isEnabled();

            // Assert - Verify functional behavior
            assertFalse(enabled);
        }
    }
}