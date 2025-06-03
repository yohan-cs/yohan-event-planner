package com.yohan.event_planner.security;

import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.exception.UnauthorizedException;
import com.yohan.event_planner.exception.UserNotFoundException;
import com.yohan.event_planner.util.TestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_ACCESS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticatedUserProviderTest {

    private JwtUtils jwtUtils;
    private UserBO userBO;
    private HttpServletRequest request;
    private AuthenticatedUserProvider authenticatedUserProvider;

    @BeforeEach
    void setUp() {
        jwtUtils = mock(JwtUtils.class);
        userBO = mock(UserBO.class);
        request = mock(HttpServletRequest.class);
        authenticatedUserProvider = new AuthenticatedUserProvider(jwtUtils, userBO, request);
    }

    @Nested
    class GetCurrentUserTests {

        @Test
        void testGetCurrentUser_success() {
            // Arrange
            Long userId = 1L;
            String jwtToken = "valid.jwt.token";
            User expectedUser = TestUtils.createUserEntityWithId(userId);

            when(jwtUtils.getJwtFromHeader(request)).thenReturn(jwtToken);
            when(jwtUtils.getUserIdFromJwtToken(jwtToken)).thenReturn(userId);
            when(userBO.getUserById(userId)).thenReturn(Optional.of(expectedUser));

            // Act
            User result = authenticatedUserProvider.getCurrentUser();

            // Assert
            assertEquals(expectedUser, result);
            verify(jwtUtils).getJwtFromHeader(request);
            verify(jwtUtils).getUserIdFromJwtToken(jwtToken);
            verify(userBO).getUserById(userId);
        }

        @Test
        void testGetCurrentUser_missingToken_throwsUnauthorizedException() {
            // Arrange
            when(jwtUtils.getJwtFromHeader(request)).thenThrow(new UnauthorizedException(UNAUTHORIZED_ACCESS));

            // Act + Assert
            assertThrows(UnauthorizedException.class, () -> authenticatedUserProvider.getCurrentUser());
        }

        @Test
        void testGetCurrentUser_invalidToken_throwsUnauthorizedException() {
            // Arrange
            String invalidToken = "invalid.token";
            when(jwtUtils.getJwtFromHeader(request)).thenReturn(invalidToken);
            when(jwtUtils.getUserIdFromJwtToken(invalidToken)).thenThrow(new UnauthorizedException(UNAUTHORIZED_ACCESS));

            // Act + Assert
            assertThrows(UnauthorizedException.class, () -> authenticatedUserProvider.getCurrentUser());
            verify(jwtUtils).getJwtFromHeader(request);
            verify(jwtUtils).getUserIdFromJwtToken(invalidToken);
            verify(userBO, never()).getUserById(any());
        }

        @Test
        void testGetCurrentUser_userNotFound_throwsUserNotFoundException() {
            // Arrange
            String token = "valid.token";
            Long userId = 999L;
            when(jwtUtils.getJwtFromHeader(request)).thenReturn(token);
            when(jwtUtils.getUserIdFromJwtToken(token)).thenReturn(userId);
            when(userBO.getUserById(userId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(UserNotFoundException.class, () -> authenticatedUserProvider.getCurrentUser());
            verify(jwtUtils).getJwtFromHeader(request);
            verify(jwtUtils).getUserIdFromJwtToken(token);
            verify(userBO).getUserById(userId);
        }
    }
}
