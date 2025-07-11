package com.yohan.event_planner.service;

import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.dto.UserResponseDTO;
import com.yohan.event_planner.dto.auth.LoginRequestDTO;
import com.yohan.event_planner.dto.auth.LoginResponseDTO;
import com.yohan.event_planner.dto.auth.RegisterResponseDTO;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private AuthenticationManager authenticationManager;
    private JwtUtils jwtUtils;
    private UserService userService;
    private RefreshTokenService refreshTokenService;
    private EmailVerificationService emailVerificationService;
    private UserBO userBO;
    private EmailDomainValidationService emailDomainValidationService;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        jwtUtils = mock(JwtUtils.class);
        userService = mock(UserService.class);
        refreshTokenService = mock(RefreshTokenService.class);
        emailVerificationService = mock(EmailVerificationService.class);
        userBO = mock(UserBO.class);
        emailDomainValidationService = mock(EmailDomainValidationService.class);
        authService = new AuthServiceImpl(authenticationManager, jwtUtils, userService, 
                                        refreshTokenService, emailVerificationService, userBO,
                                        emailDomainValidationService);
    }

    @Nested
    class LoginTests {
        @Test
        void testLogin_verifiedUser_returnsLoginResponseDTO() {
            // Arrange
            LoginRequestDTO loginDTO = new LoginRequestDTO("johnny", "password123");
            User user = TestUtils.createValidUserEntityWithId();
            user.verifyEmail(); // Verify email for successful login
            CustomUserDetails userDetails = new CustomUserDetails(user);
            String expectedAccessToken = "mock-jwt-token";
            String expectedRefreshToken = "mock-refresh-token";

            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
            when(jwtUtils.generateToken(userDetails)).thenReturn(expectedAccessToken);
            when(refreshTokenService.createRefreshToken(user.getId())).thenReturn(expectedRefreshToken);

            // Act
            LoginResponseDTO result = authService.login(loginDTO);

            // Assert
            assertNotNull(result);
            assertEquals(expectedAccessToken, result.token());
            assertEquals(expectedRefreshToken, result.refreshToken());
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

        @Test
        void testLogin_unverifiedUser_shouldThrowEmailException() {
            // Arrange
            LoginRequestDTO loginDTO = new LoginRequestDTO("johnny", "password123");
            User user = TestUtils.createValidUserEntityWithId();
            // Don't verify email to test email verification requirement
            CustomUserDetails userDetails = new CustomUserDetails(user);

            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

            // Act & Assert
            assertThrows(com.yohan.event_planner.exception.EmailException.class, () ->
                    authService.login(loginDTO));

            ArgumentCaptor<UsernamePasswordAuthenticationToken> captor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(captor.capture());
            UsernamePasswordAuthenticationToken capturedToken = captor.getValue();
            assertEquals(loginDTO.username(), capturedToken.getPrincipal());
            assertEquals(loginDTO.password(), capturedToken.getCredentials());
            
            // Verify that JWT and refresh token generation methods were never called
            verify(jwtUtils, never()).generateToken(any());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }
    }

    @Nested
    class RegisterTests {
        
        @Test
        void register_ShouldCreateUserAndSendVerificationEmail() {
            // Given
            UserCreateDTO registerDTO = TestUtils.createValidRegisterPayload("testuser");
            User createdUser = TestUtils.createTestUser("testuser");
            UserResponseDTO userResponseDTO = mock(UserResponseDTO.class);
            
            when(emailDomainValidationService.isEmailDomainValid(registerDTO.email())).thenReturn(true);
            when(userService.createUser(registerDTO)).thenReturn(userResponseDTO);
            when(userBO.getUserByUsername(registerDTO.username())).thenReturn(Optional.of(createdUser));

            // When
            RegisterResponseDTO result = authService.register(registerDTO);

            // Then
            assertNotNull(result);
            assertNull(result.token()); // No token until verified
            assertNull(result.refreshToken()); // No refresh token until verified
            assertEquals(createdUser.getId(), result.userId());
            assertEquals(createdUser.getUsername(), result.username());
            assertEquals(createdUser.getEmail(), result.email());
            assertEquals(createdUser.getTimezone(), result.timezone());
            assertTrue(result.message().contains("check your email"));

            verify(emailDomainValidationService).isEmailDomainValid(registerDTO.email());
            verify(userService).createUser(registerDTO);
            verify(userBO).getUserByUsername(registerDTO.username());
            verify(emailVerificationService).generateAndSendVerificationToken(createdUser);
        }

        @Test
        void register_UserNotFoundAfterCreation_ShouldThrowException() {
            // Given
            UserCreateDTO registerDTO = TestUtils.createValidRegisterPayload("testuser");
            UserResponseDTO userResponseDTO = mock(UserResponseDTO.class);
            
            when(emailDomainValidationService.isEmailDomainValid(registerDTO.email())).thenReturn(true);
            when(userService.createUser(registerDTO)).thenReturn(userResponseDTO);
            when(userBO.getUserByUsername(registerDTO.username())).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    authService.register(registerDTO));
            
            assertEquals("User not found after creation", exception.getMessage());
            
            verify(emailDomainValidationService).isEmailDomainValid(registerDTO.email());
            verify(userService).createUser(registerDTO);
            verify(userBO).getUserByUsername(registerDTO.username());
            verify(emailVerificationService, never()).generateAndSendVerificationToken(any());
        }

        @Test
        void register_EmailVerificationServiceThrowsException_ShouldPropagate() {
            // Given
            UserCreateDTO registerDTO = TestUtils.createValidRegisterPayload("testuser");
            User createdUser = TestUtils.createTestUser("testuser");
            UserResponseDTO userResponseDTO = mock(UserResponseDTO.class);
            
            when(emailDomainValidationService.isEmailDomainValid(registerDTO.email())).thenReturn(true);
            when(userService.createUser(registerDTO)).thenReturn(userResponseDTO);
            when(userBO.getUserByUsername(registerDTO.username())).thenReturn(Optional.of(createdUser));
            doThrow(new RuntimeException("Email service error"))
                    .when(emailVerificationService).generateAndSendVerificationToken(createdUser);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    authService.register(registerDTO));
            
            assertEquals("Email service error", exception.getMessage());
            
            verify(emailDomainValidationService).isEmailDomainValid(registerDTO.email());
            verify(userService).createUser(registerDTO);
            verify(userBO).getUserByUsername(registerDTO.username());
            verify(emailVerificationService).generateAndSendVerificationToken(createdUser);
        }

        @Test
        void register_InvalidEmailDomain_ShouldThrowEmailException() {
            // Given
            UserCreateDTO registerDTO = TestUtils.createValidRegisterPayload("testuser");
            String failureReason = "Disposable email domains are not allowed: tempmail.org";
            
            when(emailDomainValidationService.isEmailDomainValid(registerDTO.email())).thenReturn(false);
            when(emailDomainValidationService.getValidationFailureReason(registerDTO.email())).thenReturn(failureReason);

            // When & Then
            com.yohan.event_planner.exception.EmailException exception = assertThrows(
                    com.yohan.event_planner.exception.EmailException.class, () ->
                    authService.register(registerDTO));
            
            assertEquals(failureReason, exception.getMessage());
            assertEquals(com.yohan.event_planner.exception.ErrorCode.INVALID_EMAIL_DOMAIN, exception.getErrorCode());
            
            verify(emailDomainValidationService).isEmailDomainValid(registerDTO.email());
            verify(emailDomainValidationService).getValidationFailureReason(registerDTO.email());
            verify(userService, never()).createUser(any());
            verify(userBO, never()).getUserByUsername(any());
            verify(emailVerificationService, never()).generateAndSendVerificationToken(any());
        }

        @Test
        void register_InvalidEmailDomainWithNullReason_ShouldThrowEmailException() {
            // Given
            UserCreateDTO registerDTO = TestUtils.createValidRegisterPayload("testuser");
            
            when(emailDomainValidationService.isEmailDomainValid(registerDTO.email())).thenReturn(false);
            when(emailDomainValidationService.getValidationFailureReason(registerDTO.email())).thenReturn(null);

            // When & Then
            com.yohan.event_planner.exception.EmailException exception = assertThrows(
                    com.yohan.event_planner.exception.EmailException.class, () ->
                    authService.register(registerDTO));
            
            assertEquals("Email domain not allowed for registration", exception.getMessage());
            assertEquals(com.yohan.event_planner.exception.ErrorCode.INVALID_EMAIL_DOMAIN, exception.getErrorCode());
            
            verify(emailDomainValidationService).isEmailDomainValid(registerDTO.email());
            verify(emailDomainValidationService).getValidationFailureReason(registerDTO.email());
            verify(userService, never()).createUser(any());
            verify(userBO, never()).getUserByUsername(any());
            verify(emailVerificationService, never()).generateAndSendVerificationToken(any());
        }
    }
}
