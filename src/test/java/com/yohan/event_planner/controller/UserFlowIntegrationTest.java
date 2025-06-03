package com.yohan.event_planner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.UserUpdateDTO;
import com.yohan.event_planner.dto.auth.RegisterRequestDTO;
import com.yohan.event_planner.repository.UserRepository;
import com.yohan.event_planner.security.JwtUtils;
import com.yohan.event_planner.util.TestAuthUtils;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class UserFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtils jwtUtils;
    private TestAuthUtils testAuthUtils;

    @BeforeEach
    void setUp() {
        testAuthUtils = new TestAuthUtils(jwtUtils, mockMvc, objectMapper);
    }

    @Test
    void testUserLifecycleFlow() throws Exception {
        // Register + Login
        String suffix = "flow1";
        RegisterRequestDTO registerDTO = TestUtils.createValidRegisterPayload(suffix);
        String jwt = testAuthUtils.registerAndLoginUser(suffix);

        // GET /users/me
        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(registerDTO.username()))
                .andExpect(jsonPath("$.email").value(registerDTO.email()))
                .andExpect(jsonPath("$.timezone").value(registerDTO.timezone()));

        // PATCH /users/me
        UserUpdateDTO updateDTO = new UserUpdateDTO(
                null,
                null,
                null,
                "UpdatedName",
                null,
                null
        );

        mockMvc.perform(patch("/users/me")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("UpdatedName"));

        mockMvc.perform(patch("/users/me")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("UpdatedName"));

        // Verify DB reflects the change
        Optional<User> updatedUserOpt = userRepository.findByUsername(registerDTO.username());
        assertThat(updatedUserOpt).isPresent();
        assertThat(updatedUserOpt.get().getFirstName()).isEqualTo("UpdatedName");

        // DELETE /users/me
        mockMvc.perform(delete("/users/me")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());

        // Confirm soft deletion
        Optional<User> deletedUser = userRepository.findByUsername(registerDTO.username());
        assertThat(deletedUser).isPresent();
        assertThat(deletedUser.get().isDeleted()).isTrue();
    }
}
