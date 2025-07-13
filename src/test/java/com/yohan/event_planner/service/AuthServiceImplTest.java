package com.yohan.event_planner.service;

import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.dto.UserResponseDTO;
import com.yohan.event_planner.dto.auth.LoginRequestDTO;
import com.yohan.event_planner.dto.auth.LoginResponseDTO;
import com.yohan.event_planner.dto.auth.RefreshTokenResponseDTO;
import com.yohan.event_planner.dto.auth.RegisterResponseDTO;
import com.yohan.event_planner.dto.auth.TokenRequestDTO;
import com.yohan.event_planner.security.CustomUserDetails;
import com.yohan.event_planner.security.JwtUtils;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private UserService userService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private UserBO userBO;
    @Mock
    private EmailDomainValidationService emailDomainValidationService;
    @Mock
    private Authentication authentication;
    @Mock 
    private UserResponseDTO userResponseDTO;
    
    @InjectMocks
    private AuthServiceImpl authService;


    @Nested
    class LoginTests {
        @Test
        void testLogin_verifiedUser_returnsLoginResponseDTO() {
            // Arrange
            LoginRequestDTO loginDTO = new LoginRequestDTO("johnny@example.com", "password123");
            User user = TestUtils.createValidUserEntityWithId();
            user.verifyEmail(); // Verify email for successful login
            CustomUserDetails userDetails = new CustomUserDetails(user);
            String expectedAccessToken = "mock-jwt-token";
            String expectedRefreshToken = "mock-refresh-token";

            Authentication auth = authentication;
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
            assertEquals(loginDTO.email(), capturedToken.getPrincipal());
            assertEquals(loginDTO.password(), capturedToken.getCredentials());
        }

        @Test
        void testLogin_unverifiedUser_shouldThrowEmailException() {
            // Arrange
            LoginRequestDTO loginDTO = new LoginRequestDTO("johnny@example.com", "password123");
            User user = TestUtils.createValidUserEntityWithId();
            // Don't verify email to test email verification requirement
            CustomUserDetails userDetails = new CustomUserDetails(user);

            Authentication auth = authentication;
            when(auth.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

            // Act & Assert
            assertThrows(com.yohan.event_planner.exception.EmailException.class, () ->
                    authService.login(loginDTO));

            ArgumentCaptor<UsernamePasswordAuthenticationToken> captor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(captor.capture());
            UsernamePasswordAuthenticationToken capturedToken = captor.getValue();
            assertEquals(loginDTO.email(), capturedToken.getPrincipal());
            assertEquals(loginDTO.password(), capturedToken.getCredentials());
            
            // Verify that JWT and refresh token generation methods were never called
            verify(jwtUtils, never()).generateToken(any());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        void testLogin_authenticationFailure_shouldPropagateException() {
            // Arrange
            LoginRequestDTO loginDTO = new LoginRequestDTO("invalid@example.com", "wrongpassword");
            
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // Act & Assert
            assertThrows(BadCredentialsException.class, () ->
                    authService.login(loginDTO));

            ArgumentCaptor<UsernamePasswordAuthenticationToken> captor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authenticationManager).authenticate(captor.capture());
            UsernamePasswordAuthenticationToken capturedToken = captor.getValue();
            assertEquals(loginDTO.email(), capturedToken.getPrincipal());
            assertEquals(loginDTO.password(), capturedToken.getCredentials());
            
            // Verify that JWT and refresh token generation methods were never called
            verify(jwtUtils, never()).generateToken(any());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        void testLogin_jwtGenerationFailure_shouldPropagateException() {
            // Arrange
            LoginRequestDTO loginDTO = new LoginRequestDTO("johnny@example.com", "password123");
            User user = TestUtils.createValidUserEntityWithId();
            user.verifyEmail(); // Verify email for successful authentication
            CustomUserDetails userDetails = new CustomUserDetails(user);

            Authentication auth = authentication;
            when(auth.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
            when(jwtUtils.generateToken(userDetails)).thenThrow(new RuntimeException("JWT generation failed"));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    authService.login(loginDTO));
            
            assertEquals("JWT generation failed", exception.getMessage());

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtils).generateToken(userDetails);
            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        void testLogin_refreshTokenCreationFailure_shouldPropagateException() {
            // Arrange
            LoginRequestDTO loginDTO = new LoginRequestDTO("johnny@example.com", "password123");
            User user = TestUtils.createValidUserEntityWithId();
            user.verifyEmail(); // Verify email for successful authentication
            CustomUserDetails userDetails = new CustomUserDetails(user);
            String expectedAccessToken = "mock-jwt-token";

            Authentication auth = authentication;
            when(auth.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
            when(jwtUtils.generateToken(userDetails)).thenReturn(expectedAccessToken);
            when(refreshTokenService.createRefreshToken(user.getId())).thenThrow(new RuntimeException("Refresh token creation failed"));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    authService.login(loginDTO));
            
            assertEquals("Refresh token creation failed", exception.getMessage());

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtils).generateToken(userDetails);
            verify(refreshTokenService).createRefreshToken(user.getId());
        }

        @Test
        void testLogin_accountDisabled_shouldPropagateException() {
            // Arrange
            LoginRequestDTO loginDTO = new LoginRequestDTO("disabled@example.com", "password123");
            
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new DisabledException("User account is disabled"));

            // Act & Assert
            DisabledException exception = assertThrows(DisabledException.class, () ->
                    authService.login(loginDTO));
            
            assertEquals("User account is disabled", exception.getMessage());

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtils, never()).generateToken(any());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        void testLogin_accountLocked_shouldPropagateException() {
            // Arrange
            LoginRequestDTO loginDTO = new LoginRequestDTO("locked@example.com", "password123");
            
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new LockedException("User account is locked"));

            // Act & Assert
            LockedException exception = assertThrows(LockedException.class, () ->
                    authService.login(loginDTO));
            
            assertEquals("User account is locked", exception.getMessage());

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtils, never()).generateToken(any());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        void testLogin_nullRequest_shouldThrowException() {
            // Act & Assert
            assertThrows(NullPointerException.class, () ->
                    authService.login(null));

            verify(authenticationManager, never()).authenticate(any());
            verify(jwtUtils, never()).generateToken(any());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        void testLogin_authenticationReturnsNullPrincipal_shouldThrowException() {
            // Arrange
            LoginRequestDTO loginDTO = new LoginRequestDTO("johnny@example.com", "password123");

            Authentication auth = authentication;
            when(auth.getPrincipal()).thenReturn(null);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

            // Act & Assert
            assertThrows(NullPointerException.class, () ->
                    authService.login(loginDTO));

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
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
            // userResponseDTO is already a @Mock field
            
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
            // userResponseDTO is already a @Mock field
            
            when(emailDomainValidationService.isEmailDomainValid(registerDTO.email())).thenReturn(true);
            when(userService.createUser(registerDTO)).thenReturn(userResponseDTO);
            when(userBO.getUserByUsername(registerDTO.username())).thenReturn(Optional.empty());

            // When & Then
            IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                    authService.register(registerDTO));
            
            assertEquals("User not found after creation: usertestuser", exception.getMessage());
            
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
            // userResponseDTO is already a @Mock field
            
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

        @Test
        void register_UserServiceThrowsUsernameException_ShouldPropagate() {
            // Given
            UserCreateDTO registerDTO = TestUtils.createValidRegisterPayload("testuser");
            
            when(emailDomainValidationService.isEmailDomainValid(registerDTO.email())).thenReturn(true);
            when(userService.createUser(registerDTO))
                    .thenThrow(new com.yohan.event_planner.exception.UsernameException(com.yohan.event_planner.exception.ErrorCode.DUPLICATE_USERNAME, "usertestuser"));

            // When & Then
            com.yohan.event_planner.exception.UsernameException exception = assertThrows(
                    com.yohan.event_planner.exception.UsernameException.class, () ->
                    authService.register(registerDTO));
            
            assertTrue(exception.getMessage().contains("usertestuser"));
            assertEquals(com.yohan.event_planner.exception.ErrorCode.DUPLICATE_USERNAME, exception.getErrorCode());
            
            verify(emailDomainValidationService).isEmailDomainValid(registerDTO.email());
            verify(userService).createUser(registerDTO);
            verify(userBO, never()).getUserByUsername(any());
            verify(emailVerificationService, never()).generateAndSendVerificationToken(any());
        }

        @Test
        void register_UserServiceThrowsEmailException_ShouldPropagate() {
            // Given
            UserCreateDTO registerDTO = TestUtils.createValidRegisterPayload("testuser");
            
            when(emailDomainValidationService.isEmailDomainValid(registerDTO.email())).thenReturn(true);
            when(userService.createUser(registerDTO))
                    .thenThrow(new com.yohan.event_planner.exception.EmailException(com.yohan.event_planner.exception.ErrorCode.DUPLICATE_EMAIL, "usertestuser@example.com"));

            // When & Then
            com.yohan.event_planner.exception.EmailException exception = assertThrows(
                    com.yohan.event_planner.exception.EmailException.class, () ->
                    authService.register(registerDTO));
            
            assertTrue(exception.getMessage().contains("usertestuser@example.com"));
            assertEquals(com.yohan.event_planner.exception.ErrorCode.DUPLICATE_EMAIL, exception.getErrorCode());
            
            verify(emailDomainValidationService).isEmailDomainValid(registerDTO.email());
            verify(userService).createUser(registerDTO);
            verify(userBO, never()).getUserByUsername(any());
            verify(emailVerificationService, never()).generateAndSendVerificationToken(any());
        }

        @Test
        void register_UserServiceThrowsValidationException_ShouldNotCallEmailService() {
            // Given
            UserCreateDTO registerDTO = TestUtils.createValidRegisterPayload("testuser");
            
            when(emailDomainValidationService.isEmailDomainValid(registerDTO.email())).thenReturn(true);
            when(userService.createUser(registerDTO))
                    .thenThrow(new IllegalArgumentException("Invalid user data"));

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    authService.register(registerDTO));
            
            assertEquals("Invalid user data", exception.getMessage());
            
            verify(emailDomainValidationService).isEmailDomainValid(registerDTO.email());
            verify(userService).createUser(registerDTO);
            verify(userBO, never()).getUserByUsername(any());
            verify(emailVerificationService, never()).generateAndSendVerificationToken(any());
        }

        @Test
        void register_NullRequest_ShouldThrowException() {
            // When & Then
            assertThrows(NullPointerException.class, () ->
                    authService.register(null));

            verify(emailDomainValidationService, never()).isEmailDomainValid(any());
            verify(userService, never()).createUser(any());
            verify(userBO, never()).getUserByUsername(any());
            verify(emailVerificationService, never()).generateAndSendVerificationToken(any());
        }
    }

    @Nested
    class RefreshTokenTests {
        
        @Test
        void refreshToken_ValidToken_ShouldReturnNewTokens() {
            // Given
            TokenRequestDTO tokenRequest = new TokenRequestDTO("valid-refresh-token");
            RefreshTokenResponseDTO expectedResponse = new RefreshTokenResponseDTO(
                    "new-access-token", 
                    "new-refresh-token"
            );
            
            when(refreshTokenService.refreshTokens(tokenRequest.token())).thenReturn(expectedResponse);

            // When
            RefreshTokenResponseDTO result = authService.refreshToken(tokenRequest);

            // Then
            assertNotNull(result);
            assertEquals(expectedResponse.accessToken(), result.accessToken());
            assertEquals(expectedResponse.refreshToken(), result.refreshToken());
            
            verify(refreshTokenService).refreshTokens(tokenRequest.token());
        }

        @Test
        void refreshToken_InvalidToken_ShouldPropagateException() {
            // Given
            TokenRequestDTO tokenRequest = new TokenRequestDTO("invalid-refresh-token");
            
            when(refreshTokenService.refreshTokens(tokenRequest.token()))
                    .thenThrow(new com.yohan.event_planner.exception.UnauthorizedException(com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_ACCESS));

            // When & Then
            assertThrows(com.yohan.event_planner.exception.UnauthorizedException.class, () ->
                    authService.refreshToken(tokenRequest));
            
            verify(refreshTokenService).refreshTokens(tokenRequest.token());
        }

        @Test
        void refreshToken_NullRequest_ShouldThrowException() {
            // When & Then
            assertThrows(NullPointerException.class, () ->
                    authService.refreshToken(null));

            verify(refreshTokenService, never()).refreshTokens(any());
        }

        @Test
        void refreshToken_EmptyToken_ShouldPropagateException() {
            // Given
            TokenRequestDTO tokenRequest = new TokenRequestDTO("");
            
            when(refreshTokenService.refreshTokens(""))
                    .thenThrow(new com.yohan.event_planner.exception.UnauthorizedException(com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_ACCESS));

            // When & Then
            assertThrows(com.yohan.event_planner.exception.UnauthorizedException.class, () ->
                    authService.refreshToken(tokenRequest));
            
            verify(refreshTokenService).refreshTokens("");
        }
    }

    @Nested
    class LogoutTests {
        
        @Test
        void logout_ValidToken_ShouldRevokeToken() {
            // Given
            TokenRequestDTO tokenRequest = new TokenRequestDTO("valid-refresh-token");

            // When
            authService.logout(tokenRequest);

            // Then
            verify(refreshTokenService).revokeRefreshToken(tokenRequest.token());
        }

        @Test
        void logout_InvalidToken_ShouldPropagateException() {
            // Given
            TokenRequestDTO tokenRequest = new TokenRequestDTO("invalid-refresh-token");
            
            doThrow(new com.yohan.event_planner.exception.UnauthorizedException(com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_ACCESS))
                    .when(refreshTokenService).revokeRefreshToken(tokenRequest.token());

            // When & Then
            assertThrows(com.yohan.event_planner.exception.UnauthorizedException.class, () ->
                    authService.logout(tokenRequest));
            
            verify(refreshTokenService).revokeRefreshToken(tokenRequest.token());
        }

        @Test
        void logout_NullRequest_ShouldThrowException() {
            // When & Then
            assertThrows(NullPointerException.class, () ->
                    authService.logout(null));

            verify(refreshTokenService, never()).revokeRefreshToken(any());
        }

        @Test
        void logout_EmptyToken_ShouldPropagateException() {
            // Given
            TokenRequestDTO tokenRequest = new TokenRequestDTO("");
            
            doThrow(new com.yohan.event_planner.exception.UnauthorizedException(com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_ACCESS))
                    .when(refreshTokenService).revokeRefreshToken("");

            // When & Then
            assertThrows(com.yohan.event_planner.exception.UnauthorizedException.class, () ->
                    authService.logout(tokenRequest));
            
            verify(refreshTokenService).revokeRefreshToken("");
        }
    }
}
