package com.yohan.event_planner.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.dto.auth.LoginRequestDTO;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.repository.UserRepository;
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

@Component
public class TestAuthUtils {

    private final JwtUtils jwtUtils;
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    @Autowired
    public TestAuthUtils(JwtUtils jwtUtils, MockMvc mockMvc, ObjectMapper objectMapper, UserRepository userRepository) {
        this.jwtUtils = jwtUtils;
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
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
     * Returns complete authentication information including refresh token.
     */
    public AuthResult registerAndLoginUser(String usernameSuffix) throws Exception {
        UserCreateDTO registerDTO = createValidRegisterPayload(usernameSuffix);
        return registerAndLogin(registerDTO);
    }

    /**
     * Registers and logs in a new user using a custom UserCreateDTO.
     * Returns complete authentication information including refresh token.
     *
     * @param registerDTO the registration request
     * @return AuthResult with JWT, refresh token, and user ID
     */
    public AuthResult registerAndLogin(UserCreateDTO registerDTO) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());

        // Verify the user's email for testing purposes
        User user = userRepository.findByUsername(registerDTO.username())
                .orElseThrow(() -> new IllegalStateException("User not found after registration"));
        user.verifyEmail();
        userRepository.saveAndFlush(user);

        LoginRequestDTO loginDTO = new LoginRequestDTO(registerDTO.email(), registerDTO.password());

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn();

        MockHttpServletResponse response = loginResult.getResponse();
        String json = response.getContentAsString();
        
        var jsonNode = objectMapper.readTree(json);
        String accessToken = jsonNode.get("token").asText();
        String refreshToken = jsonNode.get("refreshToken").asText();
        Long userId = jsonNode.get("userId").asLong();
        
        return new AuthResult(accessToken, refreshToken, userId);
    }

    public record AuthResult(String jwt, String refreshToken, User user, Long userId) {
        // Constructor for cases where we only have auth tokens
        public AuthResult(String jwt, String refreshToken, Long userId) {
            this(jwt, refreshToken, null, userId);
        }
    }
}
