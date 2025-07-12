package com.yohan.event_planner.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user login.
 *
 * <p>
 * Used by the authentication API (e.g., {@code POST /auth/login}) to submit user credentials.
 * Fields are validated and must be non-blank.
 * </p>
 */
public record LoginRequestDTO(

        /** Email used for login. Required. */
        @NotBlank(message = "Email is required")
        String email,

        /** Password used for login. Required. */
        @NotBlank(message = "Password is required")
        String password

) {}
