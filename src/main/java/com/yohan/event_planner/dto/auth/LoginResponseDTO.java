package com.yohan.event_planner.dto.auth;

/**
 * Response DTO returned upon successful login.
 *
 * <p>
 * Encapsulates the authentication token and basic user information.
 * Used to initialize the client session and local user context.
 * </p>
 */
public record LoginResponseDTO(

        /** JWT access token. */
        String token,

        /** Unique ID of the authenticated user. */
        Long userId,

        /** Authenticated user's username. */
        String username,

        /** Authenticated user's email address. */
        String email,

        /** Authenticated user's preferred time zone. */
        String timezone
) {}
