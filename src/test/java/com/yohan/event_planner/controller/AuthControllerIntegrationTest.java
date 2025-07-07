package com.yohan.event_planner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.auth.LoginRequestDTO;
import com.yohan.event_planner.dto.auth.LogoutRequestDTO;
import com.yohan.event_planner.dto.auth.RefreshTokenRequestDTO;
import com.yohan.event_planner.dto.auth.RegisterRequestDTO;
import com.yohan.event_planner.repository.RefreshTokenRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestConfig.class)
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

    @Nested
    class RegisterTests {

        @Test
        void testRegister_ShouldCreateUser() throws Exception {
            RegisterRequestDTO registerDTO = TestUtils.createValidRegisterPayload("auth1");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isCreated());
        }

        @Test
        void testRegister_DuplicateUsername_ShouldReturnConflict() throws Exception {
            RegisterRequestDTO registerDTO = TestUtils.createValidRegisterPayload("auth2");

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
            RegisterRequestDTO registerDTO = new RegisterRequestDTO(
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
            RegisterRequestDTO registerDTO = TestUtils.createValidRegisterPayload("auth3");

            // Register user first
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isCreated());

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
            RegisterRequestDTO registerDTO = TestUtils.createValidRegisterPayload("auth4");

            // Register user first
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isCreated());

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
            RegisterRequestDTO registerDTO = TestUtils.createValidRegisterPayload("auth5");

            // Register user first
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isCreated());

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
            RefreshTokenRequestDTO refreshDTO = new RefreshTokenRequestDTO(refreshToken);

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
            RefreshTokenRequestDTO refreshDTO = new RefreshTokenRequestDTO(refreshToken);

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
            RefreshTokenRequestDTO refreshDTO = new RefreshTokenRequestDTO("invalid-refresh-token");

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshDTO)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testRefreshToken_EmptyToken_ShouldReturnBadRequest() throws Exception {
            RefreshTokenRequestDTO refreshDTO = new RefreshTokenRequestDTO("");

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testRefreshToken_NullToken_ShouldReturnBadRequest() throws Exception {
            RefreshTokenRequestDTO refreshDTO = new RefreshTokenRequestDTO(null);

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
            LogoutRequestDTO logoutDTO = new LogoutRequestDTO(refreshToken);

            mockMvc.perform(post("/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(logoutDTO)))
                    .andExpect(status().isOk());
        }

        @Test
        void testLogout_ShouldRevokeRefreshToken() throws Exception {
            LogoutRequestDTO logoutDTO = new LogoutRequestDTO(refreshToken);

            // Logout should succeed
            mockMvc.perform(post("/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(logoutDTO)))
                    .andExpect(status().isOk());

            // Attempting to refresh with the same token should fail
            RefreshTokenRequestDTO refreshDTO = new RefreshTokenRequestDTO(refreshToken);
            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshDTO)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testLogout_InvalidRefreshToken_ShouldStillSucceed() throws Exception {
            LogoutRequestDTO logoutDTO = new LogoutRequestDTO("invalid-refresh-token");

            // Logout should succeed even with invalid token (graceful handling)
            mockMvc.perform(post("/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(logoutDTO)))
                    .andExpect(status().isOk());
        }

        @Test
        void testLogout_EmptyToken_ShouldReturnBadRequest() throws Exception {
            LogoutRequestDTO logoutDTO = new LogoutRequestDTO("");

            mockMvc.perform(post("/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(logoutDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void testLogout_NullToken_ShouldReturnBadRequest() throws Exception {
            LogoutRequestDTO logoutDTO = new LogoutRequestDTO(null);

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
            RegisterRequestDTO registerDTO = TestUtils.createValidRegisterPayload("fullflow");
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerDTO)))
                    .andExpect(status().isCreated());

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
            RefreshTokenRequestDTO refreshDTO = new RefreshTokenRequestDTO(initialRefreshToken);
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
            LogoutRequestDTO logoutDTO = new LogoutRequestDTO(newRefreshToken);
            mockMvc.perform(post("/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(logoutDTO)))
                    .andExpect(status().isOk());

            // 5. Verify token is revoked
            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshTokenRequestDTO(newRefreshToken))))
                    .andExpect(status().isUnauthorized());
        }
    }
}