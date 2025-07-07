package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.auth.LoginRequestDTO;
import com.yohan.event_planner.dto.auth.LoginResponseDTO;
import com.yohan.event_planner.security.CustomUserDetails;
import com.yohan.event_planner.security.JwtUtils;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private AuthenticationManager authenticationManager;
    private JwtUtils jwtUtils;
    private UserService userService;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        jwtUtils = mock(JwtUtils.class);
        userService = mock(UserService.class);
        authService = new AuthServiceImpl(authenticationManager, jwtUtils, userService);
    }

    @Nested
    class LoginTests {
        @Test
        void testLogin_success_returnsLoginResponseDTO() {
            // Arrange
            LoginRequestDTO loginDTO = new LoginRequestDTO("johnny", "password123");
            User user = TestUtils.createValidUserEntityWithId();
            CustomUserDetails userDetails = new CustomUserDetails(user);
            String expectedToken = "mock-jwt-token";

            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
            when(jwtUtils.generateToken(userDetails)).thenReturn(expectedToken);

            // Act
            LoginResponseDTO result = authService.login(loginDTO);

            // Assert
            assertNotNull(result);
            assertEquals(expectedToken, result.token());
            assertEquals(user.getId(), result.userId());
            assertEquals(user.getUsername(), result.username());
            assertEquals(user.getEmail(), result.email());
            assertEquals(user.getTimezone(), result.timezone());

            ArgumentCaptor<UsernamePasswordAuthenticationToken> captor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(captor.capture());
            UsernamePasswordAuthenticationToken capturedToken = captor.getValue();
            assertEquals(loginDTO.username(), capturedToken.getPrincipal());
            assertEquals(loginDTO.password(), capturedToken.getCredentials());
        }
    }
}
