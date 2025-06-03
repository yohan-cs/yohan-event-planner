package com.yohan.event_planner.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.dto.auth.LoginRequestDTO;
import com.yohan.event_planner.dto.auth.RegisterRequestDTO;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.security.CustomUserDetails;
import com.yohan.event_planner.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.yohan.event_planner.util.TestUtils.createValidRegisterPayload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class TestAuthUtils {

    private final JwtUtils jwtUtils;
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public TestAuthUtils(JwtUtils jwtUtils, MockMvc mockMvc, ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    /**
     * Generates a valid JWT token for the given user.
     *
     * @param user the authenticated user
     * @return a JWT token string
     */
    public String generateToken(User user) {
        CustomUserDetails customUserDetails = new CustomUserDetails(user);
        return jwtUtils.generateToken(customUserDetails);
    }

    /**
     * Registers and logs in a new user using HTTP endpoints.
     * Returns the JWT token for authenticated requests.
     *
     * @param usernameSuffix a unique suffix to prevent username/email collisions
     * @return a valid JWT token for the created user
     */
    public String registerAndLoginUser(String usernameSuffix) throws Exception {
        RegisterRequestDTO registerDTO = createValidRegisterPayload(usernameSuffix);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());

        LoginRequestDTO loginDTO = new LoginRequestDTO(registerDTO.username(), registerDTO.password());

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn();

        MockHttpServletResponse response = loginResult.getResponse();
        String json = response.getContentAsString();

        return objectMapper.readTree(json).get("token").asText();
    }
}
