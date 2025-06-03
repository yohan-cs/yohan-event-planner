package com.yohan.event_planner.dto.auth;

/**
 * DTO returned upon successful login.
 *
 * <p>Encapsulates the JWT token and essential user information,
 * including username, email, and timezone.</p>
 */
public record LoginResponseDTO(
        String token,
        Long userId,
        String username,
        String email,
        String timezone
) {}
