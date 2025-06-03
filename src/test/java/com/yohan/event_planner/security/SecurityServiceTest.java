package com.yohan.event_planner.security;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.exception.UnauthorizedException;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityServiceTest {

    private final SecurityService securityService = new SecurityService();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class GetAuthenticatedUserTests {

        @Test
        void testGetAuthenticatedUser_success() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            CustomUserDetails details = new CustomUserDetails(user);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(details, null)
            );

            // Act
            User result = securityService.getAuthenticatedUser();

            // Assert
            assertEquals(user, result);
        }

        @Test
        void testGetAuthenticatedUser_unauthorized_throws() {
            // Arrange
            SecurityContextHolder.getContext().setAuthentication(null);

            // Act + Assert
            assertThrows(UnauthorizedException.class, () -> securityService.getAuthenticatedUser());
        }

        @Test
        void testGetAuthenticatedUser_invalidPrincipal_throws() {
            // Arrange
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("not-user-details", null)
            );

            // Act + Assert
            assertThrows(UnauthorizedException.class, () -> securityService.getAuthenticatedUser());
        }
    }

    @Nested
    class RequireCurrentUserIdTests {

        @Test
        void testRequireCurrentUserId_success() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(new CustomUserDetails(user), null)
            );

            // Act
            Long result = securityService.requireCurrentUserId();

            // Assert
            assertEquals(user.getId(), result);
        }

        @Test
        void testRequireCurrentUserId_unauthorized_throws() {
            // Arrange
            SecurityContextHolder.getContext().setAuthentication(null);

            // Act + Assert
            assertThrows(UnauthorizedException.class, () -> securityService.requireCurrentUserId());
        }
    }

    @Nested
    class GetCurrentUserIdTests {

        @Test
        void testGetCurrentUserId_authenticated_returnsId() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(new CustomUserDetails(user), null)
            );

            // Act
            Long result = securityService.getCurrentUserId();

            // Assert
            assertEquals(user.getId(), result);
        }

        @Test
        void testGetCurrentUserId_unauthenticated_returnsNull() {
            // Arrange
            SecurityContextHolder.getContext().setAuthentication(null);

            // Act + Assert
            assertNull(securityService.getCurrentUserId());
        }

        @Test
        void testGetCurrentUserId_invalidPrincipal_returnsNull() {
            // Arrange
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("something-else", null)
            );

            // Act + Assert
            assertNull(securityService.getCurrentUserId());
        }
    }
}
