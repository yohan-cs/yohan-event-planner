package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.dto.auth.LoginRequestDTO;
import com.yohan.event_planner.dto.auth.LoginResponseDTO;
import com.yohan.event_planner.dto.auth.LogoutRequestDTO;
import com.yohan.event_planner.dto.auth.RefreshTokenRequestDTO;
import com.yohan.event_planner.dto.auth.RefreshTokenResponseDTO;
import com.yohan.event_planner.dto.auth.RegisterRequestDTO;
import com.yohan.event_planner.security.CustomUserDetails;
import com.yohan.event_planner.security.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Implementation of the {@link AuthService} interface.
 * Handles user authentication and registration.
 *
 * <p>
 * This class is responsible for verifying user credentials, generating JWT tokens,
 * and delegating new user creation to the {@link UserService}. It does not duplicate
 * any validation or uniqueness checks — those are delegated to the service layer.
 * </p>
 *
 * <p>
 * The {@link JwtUtils} is used for generating signed tokens, and
 * {@link AuthenticationManager} is used to validate user credentials via Spring Security.
 * </p>
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    /**
     * Constructs a new {@code AuthServiceImpl} with the required dependencies.
     *
     * @param authenticationManager the authentication manager to verify credentials
     * @param jwtUtils              the utility class for generating JWT tokens
     * @param userService           the user service for managing registration logic
     * @param refreshTokenService   the refresh token service for token management
     */
    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtUtils jwtUtils,
                           UserService userService,
                           RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Authenticates a user and generates both access and refresh tokens.
     *
     * @param request the login request containing username and password
     * @return a {@link LoginResponseDTO} containing the tokens and basic user info
     * @throws org.springframework.security.core.AuthenticationException if credentials are invalid
     */
    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String accessToken = jwtUtils.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(userDetails.getUserId());
        User user = userDetails.getUser();

        return new LoginResponseDTO(
                accessToken,
                refreshToken,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getTimezone()
        );
    }

    /**
     * Registers a new user by delegating to {@link UserService#createUser}.
     *
     * <p>
     * Validation logic such as duplicate username/email checks is handled
     * within the {@code UserService}. Any exceptions related to conflicts
     * will be thrown from there.
     * </p>
     *
     * @param request the registration request with user details
     */
    @Override
    public void register(RegisterRequestDTO request) {
        UserCreateDTO createDTO = new UserCreateDTO(
                request.username(),
                request.password(),
                request.email(),
                request.firstName(),
                request.lastName(),
                request.timezone()
        );
        userService.createUser(createDTO);
    }

    /**
     * Refreshes tokens using a valid refresh token.
     *
     * @param request the refresh token request containing the refresh token
     * @return a {@link RefreshTokenResponseDTO} containing new access and refresh tokens
     * @throws com.yohan.event_planner.exception.UnauthorizedException if the refresh token is invalid
     */
    @Override
    public RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        return refreshTokenService.refreshTokens(request.refreshToken());
    }

    /**
     * Logs out a user by revoking their refresh token.
     *
     * @param request the logout request containing the refresh token to revoke
     */
    @Override
    public void logout(LogoutRequestDTO request) {
        refreshTokenService.revokeRefreshToken(request.refreshToken());
    }
}
