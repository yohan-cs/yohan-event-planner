package com.yohan.event_planner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.yohan.event_planner.dto.UserUpdateDTO;
import com.yohan.event_planner.dto.auth.LoginRequestDTO;
import com.yohan.event_planner.dto.auth.RegisterRequestDTO;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class UserFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String token;
    private RegisterRequestDTO registeredUser;

    @BeforeEach
    void setUp() throws Exception {
        // Generate unique suffix for this test instance
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        registeredUser = TestUtils.createValidRegisterPayload(suffix);

        // Register user
        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registeredUser)))
                .andExpect(status().isCreated());

        // Login user
        LoginRequestDTO loginDTO = new LoginRequestDTO(registeredUser.username(), registeredUser.password());

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andReturn();

        token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    @Nested
    class RegisterTests {

        @Test
        void testRegisterSuccess() throws Exception {
            RegisterRequestDTO dto = TestUtils.createValidRegisterPayload(UUID.randomUUID().toString().substring(0, 8));

            mockMvc.perform(post("/auth/register")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());
        }

        // Optionally test 409 username/email conflict here
    }

    @Nested
    class LoginTests {

        @Test
        void testLoginSuccess() throws Exception {
            LoginRequestDTO loginDTO = new LoginRequestDTO(registeredUser.username(), registeredUser.password());

            mockMvc.perform(post("/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.username").value(registeredUser.username()));
        }
    }

    @Nested
    class GetCurrentUserTests {

        @Test
        void testGetCurrentUserProfile() throws Exception {
            mockMvc.perform(get("/users/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(registeredUser.username()))
                    .andExpect(jsonPath("$.email").value(registeredUser.email()));
        }
    }

    @Nested
    class GetUserByUsernameTests {

        @Test
        void testGetUserByUsername() throws Exception {
            mockMvc.perform(get("/users/" + registeredUser.username())
                            .header("Authorization", "Bearer " + token)
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value(registeredUser.username()));
        }
    }

    @Nested
    class PatchCurrentUserTests {

        @Test
        void testUpdateCurrentUserProfile() throws Exception {
            UserUpdateDTO patchDTO = new UserUpdateDTO(null, null, null, null,"newLastName", null);

            mockMvc.perform(patch("/users/me")
                            .header("Authorization", "Bearer " + token)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(patchDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lastName").value("newLastName"));
        }
    }

    @Nested
    class DeleteCurrentUserTests {

        @Test
        void testDeleteCurrentUser() throws Exception {
            mockMvc.perform(delete("/users/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/users/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized()); // or 404 depending on behavior
        }
    }
}
