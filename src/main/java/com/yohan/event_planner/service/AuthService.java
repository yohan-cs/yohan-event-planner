package com.yohan.event_planner.service;

import com.yohan.event_planner.dto.auth.LoginRequestDTO;
import com.yohan.event_planner.dto.auth.LoginResponseDTO;
import com.yohan.event_planner.dto.auth.LogoutRequestDTO;
import com.yohan.event_planner.dto.auth.RefreshTokenRequestDTO;
import com.yohan.event_planner.dto.auth.RefreshTokenResponseDTO;
import com.yohan.event_planner.dto.auth.RegisterRequestDTO;

/**
 * Service interface for handling user authentication operations such as login, registration, token refresh, and logout.
 *
 * <p>
 * Provides high-level methods for authenticating users, creating new user accounts,
 * managing refresh tokens, and handling logout operations.
 * Implementations are expected to handle validation, token generation, and conflict checks.
 * </p>
 *
 * @see com.yohan.event_planner.dto.auth.LoginRequestDTO
 * @see com.yohan.event_planner.dto.auth.RegisterRequestDTO
 * @see com.yohan.event_planner.dto.auth.LoginResponseDTO
 * @see com.yohan.event_planner.dto.auth.RefreshTokenRequestDTO
 * @see com.yohan.event_planner.dto.auth.RefreshTokenResponseDTO
 * @see com.yohan.event_planner.dto.auth.LogoutRequestDTO
 */
public interface AuthService {

    /**
     * Authenticates a user using the provided credentials.
     *
     * <p>
     * If authentication is successful, both access and refresh tokens are returned
     * along with user metadata. This method should validate the credentials
     * and reject invalid or disabled accounts.
     * </p>
     *
     * @param request the login request DTO containing username and password
     * @return a {@link LoginResponseDTO} containing JWT access token, refresh token, and user information
     * @throws com.yohan.event_planner.exception.UnauthorizedException if credentials are invalid
     */
    LoginResponseDTO login(LoginRequestDTO request);

    /**
     * Registers a new user account with the provided details.
     *
     * <p>
     * This method performs uniqueness checks and creates the user if validation passes.
     * Implementations should handle password encoding and other setup steps.
     * </p>
     *
     * @param request the registration request DTO containing user details
     * @throws com.yohan.event_planner.exception.ConflictException if the username or email is already taken
     */
    void register(RegisterRequestDTO request);

    /**
     * Refreshes an access token using a valid refresh token.
     *
     * <p>
     * This method validates the refresh token and, if valid, generates a new
     * access token and refresh token pair. The old refresh token is invalidated
     * to implement one-time use security.
     * </p>
     *
     * @param request the refresh token request DTO containing the refresh token
     * @return a {@link RefreshTokenResponseDTO} containing new access and refresh tokens
     * @throws com.yohan.event_planner.exception.UnauthorizedException if the refresh token is invalid or expired
     */
    RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO request);

    /**
     * Logs out a user by invalidating their refresh token.
     *
     * <p>
     * This method revokes the provided refresh token, preventing it from being
     * used to generate new access tokens. The client should discard both
     * access and refresh tokens after logout.
     * </p>
     *
     * @param request the logout request DTO containing the refresh token to revoke
     */
    void logout(LogoutRequestDTO request);
}

