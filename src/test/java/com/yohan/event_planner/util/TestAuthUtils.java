package com.yohan.event_planner.util;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.security.CustomUserDetails;
import com.yohan.event_planner.security.JwtUtils;
import org.springframework.stereotype.Component;

@Component
public class TestAuthUtils {

    private final JwtUtils jwtUtils;

    public TestAuthUtils(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
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
}
