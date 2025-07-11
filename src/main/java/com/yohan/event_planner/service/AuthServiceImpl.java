package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.dto.auth.LoginRequestDTO;
import com.yohan.event_planner.dto.auth.LoginResponseDTO;
import com.yohan.event_planner.dto.auth.RefreshTokenResponseDTO;
import com.yohan.event_planner.dto.auth.RegisterResponseDTO;
import com.yohan.event_planner.dto.auth.TokenRequestDTO;
import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.exception.EmailException;
import com.yohan.event_planner.exception.ErrorCode;
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
    private final EmailVerificationService emailVerificationService;
    private final UserBO userBO;
    private final EmailDomainValidationService emailDomainValidationService;

    /**
     * Constructs a new {@code AuthServiceImpl} with the required dependencies.
     *
     * @param authenticationManager the authentication manager to verify credentials
     * @param jwtUtils              the utility class for generating JWT tokens
     * @param userService           the user service for managing registration logic
     * @param refreshTokenService   the refresh token service for token management
     * @param emailVerificationService the email verification service for account activation
     * @param userBO                the user business object for user lookup operations
     * @param emailDomainValidationService the email domain validation service for blocking fake emails
     */
    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtUtils jwtUtils,
                           UserService userService,
                           RefreshTokenService refreshTokenService,
                           EmailVerificationService emailVerificationService,
                           UserBO userBO,
                           EmailDomainValidationService emailDomainValidationService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.emailVerificationService = emailVerificationService;
        this.userBO = userBO;
        this.emailDomainValidationService = emailDomainValidationService;
    }

    /**
     * Authenticates a user and generates both access and refresh tokens.
     *
     * <p>
     * This method validates user credentials and checks that the user's email
     * has been verified before allowing login. If the email is not verified,
     * an {@link EmailException} is thrown to prevent login.
     * </p>
     *
     * @param request the login request containing username and password
     * @return a {@link LoginResponseDTO} containing the tokens and basic user info
     * @throws org.springframework.security.core.AuthenticationException if credentials are invalid
     * @throws EmailException if the user's email has not been verified
     */
    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        // Check if the user's email has been verified
        if (!user.isEmailVerified()) {
            throw new EmailException(ErrorCode.EMAIL_NOT_VERIFIED, user.getEmail());
        }

        String accessToken = jwtUtils.generateToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(userDetails.getUserId());

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
     * Registers a new user and sends email verification.
     *
     * <p>
     * This method creates a new user account and sends an email verification link.
     * Users must verify their email address before they can log in. This approach
     * ensures that all registered users have valid, accessible email addresses.
     * </p>
     *
     * <p>
     * The registration portion delegates to {@link UserService#createUser} for
     * validation and user creation, while the email verification portion generates
     * a secure token and sends it via email.
     * </p>
     *
     * @param request the user creation DTO with user details
     * @return a {@link RegisterResponseDTO} containing user information and verification instructions
     */
    @Override
    public RegisterResponseDTO register(UserCreateDTO request) {
        // Validate email domain before proceeding with registration
        if (!emailDomainValidationService.isEmailDomainValid(request.email())) {
            String failureReason = emailDomainValidationService.getValidationFailureReason(request.email());
            throw new EmailException(
                    failureReason != null ? failureReason : "Email domain not allowed for registration",
                    ErrorCode.INVALID_EMAIL_DOMAIN);
        }

        // First, register the user using the existing registration logic
        userService.createUser(request);

        // Get the created user through the UserBO
        User user = userBO.getUserByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("User not found after creation"));

        // Generate and send email verification token
        emailVerificationService.generateAndSendVerificationToken(user);

        return new RegisterResponseDTO(
                null, // No token until email is verified
                null, // No refresh token until email is verified
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getTimezone(),
                "Registration successful! Please check your email to verify your account before logging in."
        );
    }

    /**
     * Refreshes tokens using a valid refresh token.
     *
     * @param request the token request containing the refresh token
     * @return a {@link RefreshTokenResponseDTO} containing new access and refresh tokens
     * @throws com.yohan.event_planner.exception.UnauthorizedException if the refresh token is invalid
     */
    @Override
    public RefreshTokenResponseDTO refreshToken(TokenRequestDTO request) {
        return refreshTokenService.refreshTokens(request.token());
    }

    /**
     * Logs out a user by revoking their refresh token.
     *
     * @param request the token request containing the refresh token to revoke
     */
    @Override
    public void logout(TokenRequestDTO request) {
        refreshTokenService.revokeRefreshToken(request.token());
    }
}
