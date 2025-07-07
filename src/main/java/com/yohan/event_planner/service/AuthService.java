package com.yohan.event_planner.service;

import com.yohan.event_planner.dto.auth.LoginRequestDTO;
import com.yohan.event_planner.dto.auth.LoginResponseDTO;
import com.yohan.event_planner.dto.auth.RegisterRequestDTO;

/**
 * Service interface for handling user authentication operations such as login and registration.
 *
 * <p>
 * Provides high-level methods for authenticating users and creating new user accounts.
 * Implementations are expected to handle validation, token generation, and conflict checks.
 * </p>
 *
 * @see com.yohan.event_planner.dto.auth.LoginRequestDTO
 * @see com.yohan.event_planner.dto.auth.RegisterRequestDTO
 * @see com.yohan.event_planner.dto.auth.LoginResponseDTO
 */
public interface AuthService {

    /**
     * Authenticates a user using the provided credentials.
     *
     * <p>
     * If authentication is successful, a signed JWT token and user metadata
     * are returned in the response. This method should validate the credentials
     * and reject invalid or disabled accounts.
     * </p>
     *
     * @param request the login request DTO containing username and password
     * @return a {@link LoginResponseDTO} containing a JWT token and user information
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
}

