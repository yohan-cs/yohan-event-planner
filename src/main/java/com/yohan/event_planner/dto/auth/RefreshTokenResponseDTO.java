package com.yohan.event_planner.dto.auth;

/**
 * Response DTO returned upon successful token refresh.
 *
 * <p>
 * Contains both a new access token and a new refresh token.
 * The old refresh token is invalidated when this response is issued.
 * </p>
 */
public record RefreshTokenResponseDTO(

        /** New JWT access token. */
        String accessToken,

        /** New opaque refresh token. */
        String refreshToken

) {}