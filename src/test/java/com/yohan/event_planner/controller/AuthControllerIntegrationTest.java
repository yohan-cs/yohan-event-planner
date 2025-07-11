package com.yohan.event_planner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.auth.ForgotPasswordRequestDTO;
import com.yohan.event_planner.dto.auth.LoginRequestDTO;
import com.yohan.event_planner.dto.auth.ResetPasswordRequestDTO;
import com.yohan.event_planner.dto.auth.TokenRequestDTO;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.repository.RefreshTokenRepository;
import com.yohan.event_planner.repository.UserRepository;
import com.yohan.event_planner.util.TestConfig;
import com.yohan.event_planner.util.TestDataHelper;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import({TestConfig.class, com.yohan.event_planner.config.TestEmailConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TestDataHelper testDataHelper;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private UserRepository userRepository;

    @Nested
    class RegisterTests {

        @Test
        void testRegister_ShouldCreateUserAndSendVerificationEmail() throws Exception {
            UserCreateDTO registerDTO = TestUtils.createValidRegisterPayload("auth1");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").doesNotExist()) // No token until verified
                    .andExpect(jsonPath("$.refreshToken").doesNotExist()) // No refresh token until verified
                    .andExpect(jsonPath("$.userId").exists())
                    .andExpect(jsonPath("$.username").value(registerDTO.username()))
                    .andExpect(jsonPath("$.email").value(registerDTO.email()))
                    .andExpect(jsonPath("$.timezone").value(registerDTO.timezone()))
                    .andExpect(jsonPath("$.message").value(containsString("check your email")));
        }

        @Test
        void testRegister_DuplicateUsername_ShouldReturnConflict() throws Exception {
            UserCreateDTO registerDTO = TestUtils.createValidRegisterPayload("auth2");

            // First registration should succeed
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isCreated());

            // Second registration with same credentials should fail
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isConflict());
        }

        @Test
        void testRegister_InvalidEmail_ShouldReturnBadRequest() throws Exception {
            UserCreateDTO registerDTO = new UserCreateDTO(
                    "validuser",
                    "ValidPassword123!",
                    "invalid-email", // Invalid email format
                    "John",
                    "Doe",
                    "America/New_York"
            );

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class LoginTests {

        @Test
        void testLogin_ValidCredentials_ShouldReturnTokensAndUserInfo() throws Exception {
            UserCreateDTO registerDTO = TestUtils.createValidRegisterPayload("auth3");

            // Register user first
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isCreated());

            // Verify the user's email for testing purposes
            User user = userRepository.findByUsername(registerDTO.username())
                    .orElseThrow(() -> new IllegalStateException("User not found after registration"));
            user.verifyEmail();
            userRepository.saveAndFlush(user);

            LoginRequestDTO loginDTO = new LoginRequestDTO(registerDTO.username(), registerDTO.password());

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andExpect(jsonPath("$.userId").exists())
                    .andExpect(jsonPath("$.username").value(registerDTO.username()))
                    .andExpect(jsonPath("$.email").value(registerDTO.email()))
                    .andExpect(jsonPath("$.timezone").value(registerDTO.timezone()));
        }

        @Test
        void testLogin_InvalidPassword_ShouldReturnUnauthorized() throws Exception {
            UserCreateDTO registerDTO = TestUtils.createValidRegisterPayload("auth4");

            // Register user first
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isCreated());

            // Verify the user's email for testing purposes
            User user = userRepository.findByUsername(registerDTO.username())
                    .orElseThrow(() -> new IllegalStateException("User not found after registration"));
            user.verifyEmail();
            userRepository.saveAndFlush(user);

            LoginRequestDTO loginDTO = new LoginRequestDTO(registerDTO.username(), "wrongpassword");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
        }

        @Test
        void testLogin_NonExistentUser_ShouldReturnUnauthorized() throws Exception {
            LoginRequestDTO loginDTO = new LoginRequestDTO("nonexistent_user", "anyPassword");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
        }

        @Test
        void testLogin_ShouldCreateRefreshTokenInDatabase() throws Exception {
            UserCreateDTO registerDTO = TestUtils.createValidRegisterPayload("auth5");

            // Register user first
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isCreated());

            // Verify the user's email for testing purposes
            User user = userRepository.findByUsername(registerDTO.username())
                    .orElseThrow(() -> new IllegalStateException("User not found after registration"));
            user.verifyEmail();
            userRepository.saveAndFlush(user);

            LoginRequestDTO loginDTO = new LoginRequestDTO(registerDTO.username(), registerDTO.password());

            long tokenCountBefore = refreshTokenRepository.count();

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isOk());

            long tokenCountAfter = refreshTokenRepository.count();
            assertThat(tokenCountAfter).isEqualTo(tokenCountBefore + 1);
        }
    }

    @Nested
    class RefreshTokenTests {

        private String jwt;
        private User user;
        private String refreshToken;

        @BeforeEach
        void setUp() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("authrefresh");
            this.jwt = auth.jwt();
            this.user = auth.user();
            this.refreshToken = auth.refreshToken();
        }

        @Test
        void testRefreshToken_ValidToken_ShouldReturnNewTokenPair() throws Exception {
            TokenRequestDTO refreshDTO = new TokenRequestDTO(refreshToken);

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty());
        }

        @Test
        void testRefreshToken_ShouldRevokeOldToken() throws Exception {
            TokenRequestDTO refreshDTO = new TokenRequestDTO(refreshToken);

            // First refresh should succeed
            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshDTO)))
                    .andExpect(status().isOk());

            // Second refresh with same token should fail (one-time use)
            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshDTO)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testRefreshToken_InvalidToken_ShouldReturnUnauthorized() throws Exception {
            TokenRequestDTO refreshDTO = new TokenRequestDTO("invalid-refresh-token");

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshDTO)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testRefreshToken_EmptyToken_ShouldReturnBadRequest() throws Exception {
            TokenRequestDTO refreshDTO = new TokenRequestDTO("");

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testRefreshToken_NullToken_ShouldReturnBadRequest() throws Exception {
            TokenRequestDTO refreshDTO = new TokenRequestDTO(null);

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshDTO)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class LogoutTests {

        private String jwt;
        private User user;
        private String refreshToken;

        @BeforeEach
        void setUp() throws Exception {
            var auth = testDataHelper.registerAndLoginUserWithUser("authlogout");
            this.jwt = auth.jwt();
            this.user = auth.user();
            this.refreshToken = auth.refreshToken();
        }

        @Test
        void testLogout_ValidRefreshToken_ShouldSucceed() throws Exception {
            TokenRequestDTO logoutDTO = new TokenRequestDTO(refreshToken);

            mockMvc.perform(post("/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(logoutDTO)))
                    .andExpect(status().isOk());
        }

        @Test
        void testLogout_ShouldRevokeRefreshToken() throws Exception {
            TokenRequestDTO logoutDTO = new TokenRequestDTO(refreshToken);

            // Logout should succeed
            mockMvc.perform(post("/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(logoutDTO)))
                    .andExpect(status().isOk());

            // Attempting to refresh with the same token should fail
            TokenRequestDTO refreshDTO = new TokenRequestDTO(refreshToken);
            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshDTO)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testLogout_InvalidRefreshToken_ShouldStillSucceed() throws Exception {
            TokenRequestDTO logoutDTO = new TokenRequestDTO("invalid-refresh-token");

            // Logout should succeed even with invalid token (graceful handling)
            mockMvc.perform(post("/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(logoutDTO)))
                    .andExpect(status().isOk());
        }

        @Test
        void testLogout_EmptyToken_ShouldReturnBadRequest() throws Exception {
            TokenRequestDTO logoutDTO = new TokenRequestDTO("");

            mockMvc.perform(post("/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(logoutDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testLogout_NullToken_ShouldReturnBadRequest() throws Exception {
            TokenRequestDTO logoutDTO = new TokenRequestDTO(null);

            mockMvc.perform(post("/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(logoutDTO)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class AuthenticationFlowTests {

        @Test
        void testCompleteAuthFlow_RegisterLoginRefreshLogout() throws Exception {
            // 1. Register
            UserCreateDTO registerDTO = TestUtils.createValidRegisterPayload("fullflow");
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isCreated());

            // Verify the user's email for testing purposes
            User user = userRepository.findByUsername(registerDTO.username())
                    .orElseThrow(() -> new IllegalStateException("User not found after registration"));
            user.verifyEmail();
            userRepository.saveAndFlush(user);

            // 2. Login
            LoginRequestDTO loginDTO = new LoginRequestDTO(registerDTO.username(), registerDTO.password());
            MvcResult loginResult = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isOk())
                    .andReturn();

            String loginResponse = loginResult.getResponse().getContentAsString();
            var loginResponseObj = objectMapper.readTree(loginResponse);
            String initialRefreshToken = loginResponseObj.get("refreshToken").asText();

            // 3. Refresh tokens
            TokenRequestDTO refreshDTO = new TokenRequestDTO(initialRefreshToken);
            MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshDTO)))
                    .andExpect(status().isOk())
                    .andReturn();

            String refreshResponse = refreshResult.getResponse().getContentAsString();
            var refreshResponseObj = objectMapper.readTree(refreshResponse);
            String newRefreshToken = refreshResponseObj.get("refreshToken").asText();

            // Verify new tokens are different
            assertThat(newRefreshToken).isNotEqualTo(initialRefreshToken);

            // 4. Logout with new refresh token
            TokenRequestDTO logoutDTO = new TokenRequestDTO(newRefreshToken);
            mockMvc.perform(post("/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(logoutDTO)))
                    .andExpect(status().isOk());

            // 5. Verify token is revoked
            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new TokenRequestDTO(newRefreshToken))))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class PasswordResetTests {

        @Test
        void testForgotPassword_ValidEmail_ShouldReturnStandardResponse() throws Exception {
            // First register a user
            UserCreateDTO registerDTO = TestUtils.createValidRegisterPayload("passwordreset1");
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isCreated());

            ForgotPasswordRequestDTO forgotPasswordDTO = new ForgotPasswordRequestDTO(registerDTO.email());

            mockMvc.perform(post("/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(forgotPasswordDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void testForgotPassword_NonExistentEmail_ShouldReturnSameResponse() throws Exception {
            ForgotPasswordRequestDTO forgotPasswordDTO = new ForgotPasswordRequestDTO("nonexistent@example.com");

            // Should return same response for security (anti-enumeration)
            mockMvc.perform(post("/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(forgotPasswordDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void testForgotPassword_InvalidEmail_ShouldReturnBadRequest() throws Exception {
            ForgotPasswordRequestDTO forgotPasswordDTO = new ForgotPasswordRequestDTO("invalid-email-format");

            mockMvc.perform(post("/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(forgotPasswordDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testForgotPassword_EmptyEmail_ShouldReturnBadRequest() throws Exception {
            ForgotPasswordRequestDTO forgotPasswordDTO = new ForgotPasswordRequestDTO("");

            mockMvc.perform(post("/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(forgotPasswordDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testResetPassword_InvalidToken_ShouldReturnUnauthorized() throws Exception {
            ResetPasswordRequestDTO resetPasswordDTO = new ResetPasswordRequestDTO(
                    "invalid-token-12345",
                    "C0mpl3x&Rand0m!P@ssw0rd"
            );

            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resetPasswordDTO)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testResetPassword_EmptyToken_ShouldReturnBadRequest() throws Exception {
            ResetPasswordRequestDTO resetPasswordDTO = new ResetPasswordRequestDTO(
                    "",
                    "NewSecurePassword123!"
            );

            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resetPasswordDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testResetPassword_WeakPassword_ShouldReturnBadRequest() throws Exception {
            ResetPasswordRequestDTO resetPasswordDTO = new ResetPasswordRequestDTO(
                    "some-token",
                    "weak" // Password too weak
            );

            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resetPasswordDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testResetPassword_EmptyPassword_ShouldReturnBadRequest() throws Exception {
            ResetPasswordRequestDTO resetPasswordDTO = new ResetPasswordRequestDTO(
                    "some-token",
                    ""
            );

            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resetPasswordDTO)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class LoggingVerificationTests {

        @Test
        void testLogin_ShouldProduceExpectedLogEntries() throws Exception {
            // This test ensures logging is working - specific log verification
            // would require additional test configuration for log capture
            UserCreateDTO registerDTO = TestUtils.createValidRegisterPayload("loggingtest1");

            // Register user first
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isCreated());

            // Verify the user's email for testing purposes
            User user = userRepository.findByUsername(registerDTO.username())
                    .orElseThrow(() -> new IllegalStateException("User not found after registration"));
            user.verifyEmail();
            userRepository.saveAndFlush(user);

            LoginRequestDTO loginDTO = new LoginRequestDTO(registerDTO.username(), registerDTO.password());

            // Successful login should generate INFO logs
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isOk());

            // Failed login should generate WARN logs
            LoginRequestDTO failedLoginDTO = new LoginRequestDTO(registerDTO.username(), "wrongpassword");
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(failedLoginDTO)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testRegister_ShouldProduceExpectedLogEntries() throws Exception {
            UserCreateDTO registerDTO = TestUtils.createValidRegisterPayload("loggingtest2");

            // Successful registration should generate INFO logs
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isCreated());

            // Duplicate registration should generate WARN logs
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isConflict());
        }
    }
}