package com.yohan.event_planner.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user login.
 *
 * <p>
 * Contains the user's credentials:
 * </p>
 * <ul>
 *   <li>{@code username} – required username for login</li>
 *   <li>{@code password} – required password for login</li>
 * </ul>
 *
 * <p>
 * Fields must be non-blank and are validated before authentication.
 * </p>
 *
 * @param username the user's username (required)
 * @param password the user's password (required)
 */
public record LoginRequestDTO(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {}
