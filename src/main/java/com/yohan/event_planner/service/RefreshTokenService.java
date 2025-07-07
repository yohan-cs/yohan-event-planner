package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.RefreshToken;
import com.yohan.event_planner.dto.auth.RefreshTokenResponseDTO;

/**
 * Service interface for managing refresh tokens with security best practices.
 *
 * <p>
 * Provides methods for creating, validating, and rotating refresh tokens.
 * Implements token rotation (one-time use) and secure hashing for storage.
 * </p>
 */
public interface RefreshTokenService {

    /**
     * Creates a new refresh token for the specified user.
     * The token is hashed before storage for security.
     *
     * @param userId the ID of the user to create the token for
     * @return the raw (unhashed) refresh token to return to the client
     */
    String createRefreshToken(Long userId);

    /**
     * Validates and rotates a refresh token.
     * This method implements one-time use: the provided token is immediately revoked
     * and a new token pair is generated.
     *
     * @param refreshToken the refresh token to validate and rotate
     * @return a new token pair (access token + refresh token)
     * @throws com.yohan.event_planner.exception.UnauthorizedException if the token is invalid or expired
     */
    RefreshTokenResponseDTO refreshTokens(String refreshToken);

    /**
     * Revokes a specific refresh token.
     * Used during logout to invalidate the token.
     *
     * @param refreshToken the refresh token to revoke
     */
    void revokeRefreshToken(String refreshToken);

    /**
     * Revokes all refresh tokens for a specific user.
     * Used for security purposes (e.g., password change, account compromise).
     *
     * @param userId the ID of the user whose tokens should be revoked
     * @return the number of tokens revoked
     */
    int revokeAllUserTokens(Long userId);

    /**
     * Cleans up expired refresh tokens from the database.
     * Should be called periodically by a scheduled job.
     *
     * @return the number of expired tokens removed
     */
    int cleanupExpiredTokens();

    /**
     * Cleans up revoked tokens older than the specified number of days.
     * Should be called periodically by a scheduled job.
     *
     * @param daysOld the number of days old for revoked tokens to be cleaned up
     * @return the number of old revoked tokens removed
     */
    int cleanupRevokedTokens(int daysOld);
}