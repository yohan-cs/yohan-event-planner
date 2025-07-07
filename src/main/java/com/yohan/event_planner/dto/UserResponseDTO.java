package com.yohan.event_planner.dto;

import com.yohan.event_planner.domain.User;

/**
 * Response Data Transfer Object for exposing public-facing {@link User} information.
 *
 * <p>
 * Returned to clients after user creation, update, or retrieval.
 * Includes only non-sensitive fields and omits internal or security-related data.
 * </p>
 *
 * <p>
 * All fields are non-null and represent user-facing properties.
 * </p>
 */
public record UserResponseDTO(

        /** Public-facing username. */
        String username,

        /** User's email address. */
        String email,

        /** User's first name. */
        String firstName,

        /** User's last name. */
        String lastName,

        /** Preferred time zone (e.g., "America/New_York"). */
        String timezone
) {}
