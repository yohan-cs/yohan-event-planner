package com.yohan.event_planner.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for refreshing access tokens.
 *
 * <p>
 * Contains the refresh token needed to generate a new access token.
 * The refresh token is validated and then used to issue a new token pair.
 * </p>
 */
public record RefreshTokenRequestDTO(

        /** The refresh token to use for generating new tokens. */
        @NotBlank(message = "Refresh token is required")
        String refreshToken

) {}