package com.yohan.event_planner.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user logout.
 *
 * <p>
 * Contains the refresh token to be revoked during logout.
 * This ensures the refresh token cannot be used to generate new access tokens.
 * </p>
 */
public record LogoutRequestDTO(

        /** The refresh token to revoke during logout. */
        @NotBlank(message = "Refresh token is required")
        String refreshToken

) {}