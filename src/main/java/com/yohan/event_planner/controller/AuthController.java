package com.yohan.event_planner.controller;

import com.yohan.event_planner.constants.ApplicationConstants;
import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.dto.auth.EmailVerificationRequestDTO;
import com.yohan.event_planner.dto.auth.EmailVerificationResponseDTO;
import com.yohan.event_planner.dto.auth.ForgotPasswordRequestDTO;
import com.yohan.event_planner.dto.auth.ForgotPasswordResponseDTO;
import com.yohan.event_planner.dto.auth.LoginRequestDTO;
import com.yohan.event_planner.dto.auth.LoginResponseDTO;
import com.yohan.event_planner.dto.auth.RefreshTokenResponseDTO;
import com.yohan.event_planner.dto.auth.RegisterResponseDTO;
import com.yohan.event_planner.dto.auth.ResetPasswordRequestDTO;
import com.yohan.event_planner.dto.auth.ResetPasswordResponseDTO;
import com.yohan.event_planner.dto.auth.ResendVerificationRequestDTO;
import com.yohan.event_planner.dto.auth.ResendVerificationResponseDTO;
import com.yohan.event_planner.dto.auth.TokenRequestDTO;
import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.exception.RateLimitExceededException;
import com.yohan.event_planner.service.AuthService;
import com.yohan.event_planner.service.EmailVerificationService;
import com.yohan.event_planner.service.PasswordResetService;
import com.yohan.event_planner.service.RateLimitingService;
import com.yohan.event_planner.util.IPAddressUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

/**
 * REST controller for user authentication and token management operations.
 *
 * <p>
 * This controller provides endpoints for the complete authentication lifecycle including
 * user registration, login, token refresh, logout, and password reset functionality.
 * All endpoints under {@code /auth/**} are publicly accessible via SecurityConfig exclusion.
 * </p>
 *
 * <h2>Supported Operations</h2>
 * <ul>
 *   <li><strong>Registration</strong>: Create new user accounts with email verification requirement</li>
 *   <li><strong>Login</strong>: Authenticate existing users and issue JWT tokens</li>
 *   <li><strong>Token Refresh</strong>: Generate new access tokens using refresh tokens</li>
 *   <li><strong>Logout</strong>: Revoke refresh tokens and terminate sessions</li>
 *   <li><strong>Password Reset</strong>: Secure password reset via email verification</li>
 *   <li><strong>Email Verification</strong>: Account activation via email token validation</li>
 * </ul>
 *
 * <h2>Security Architecture</h2>
 * <ul>
 *   <li><strong>Rate Limiting</strong>: Comprehensive per-IP rate limiting via RateLimitingService</li>
 *   <li><strong>JWT Security</strong>: Stateless authentication with secure refresh token rotation</li>
 *   <li><strong>Anti-Enumeration</strong>: Consistent responses and timing for security-sensitive operations</li>
 *   <li><strong>Email Security</strong>: Email verification prevents fake account creation and spam</li>
 *   <li><strong>IP Tracking</strong>: Client IP extraction and logging for security monitoring</li>
 * </ul>
 *
 * <h2>Integration Points</h2>
 * <ul>
 *   <li><strong>SecurityConfig</strong>: Public endpoint exclusions for {@code /auth/**} paths</li>
 *   <li><strong>GlobalExceptionHandler</strong>: Centralized error handling for authentication failures</li>
 *   <li><strong>RateLimitingService</strong>: Per-operation rate limiting and abuse prevention</li>
 *   <li><strong>AuthService</strong>: Core authentication logic and token management</li>
 *   <li><strong>PasswordResetService</strong>: Secure password reset workflow management</li>
 *   <li><strong>EmailVerificationService</strong>: Account activation and email verification</li>
 * </ul>
 *
 * <h2>Error Handling Strategy</h2>
 * <p>
 * All authentication endpoints implement consistent error handling patterns:
 * </p>
 * <ul>
 *   <li><strong>Rate Limit Violations</strong>: Throw RateLimitExceededException with retry information</li>
 *   <li><strong>Authentication Failures</strong>: Delegated to GlobalExceptionHandler for consistent responses</li>
 *   <li><strong>Security Events</strong>: Comprehensive logging for security monitoring and audit trails</li>
 *   <li><strong>Anti-Enumeration</strong>: Standard responses prevent user/email enumeration attacks</li>
 * </ul>
 *
 * @see AuthService
 * @see PasswordResetService
 * @see EmailVerificationService
 * @see RateLimitingService
 * @see com.yohan.event_planner.security.SecurityConfig
 * @see com.yohan.event_planner.exception.GlobalExceptionHandler
 * @author Event Planner Development Team
 * @since 2.0.0
 */
@Tag(name = "Authentication", description = "User authentication and token management")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;
    private final UserBO userBO;
    private final RateLimitingService rateLimitingService;

    /**
     * Constructs a new AuthController with required service dependencies.
     *
     * @param authService the authentication service for login, registration, and token operations
     * @param passwordResetService the password reset service for forgot/reset password workflows
     * @param emailVerificationService the email verification service for account activation
     * @param userBO the user business object for user lookup operations
     * @param rateLimitingService the rate limiting service for preventing abuse
     */
    public AuthController(AuthService authService, PasswordResetService passwordResetService,
                         EmailVerificationService emailVerificationService, UserBO userBO,
                         RateLimitingService rateLimitingService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.emailVerificationService = emailVerificationService;
        this.userBO = userBO;
        this.rateLimitingService = rateLimitingService;
    }

    /**
     * Validates rate limiting for authentication operations and throws exception if exceeded.
     * 
     * <p>
     * This method encapsulates the common rate limiting validation pattern used across
     * all authentication endpoints. It checks if the operation is allowed, and if not,
     * logs the violation and throws a RateLimitExceededException with detailed information.
     * </p>
     *
     * @param clientIP the client IP address for rate limiting checks
     * @param operation the operation name for logging and error reporting
     * @param isAllowed function to check if the operation is allowed for this IP
     * @param getRemainingAttempts function to get remaining attempts for error details
     * @param getResetTime function to get reset time for error details
     * @param maxAttempts the maximum allowed attempts for this operation type
     * @throws RateLimitExceededException if the rate limit has been exceeded
     */
    private void validateRateLimit(String clientIP, String operation,
                                 BooleanSupplier isAllowed,
                                 IntSupplier getRemainingAttempts,
                                 LongSupplier getResetTime,
                                 int maxAttempts) {
        if (!isAllowed.getAsBoolean()) {
            int remainingAttempts = getRemainingAttempts.getAsInt();
            long resetTime = getResetTime.getAsLong();
            
            logger.warn("{} rate limit exceeded for IP: {} (remaining: {}, reset in: {}s)", 
                    operation, clientIP, remainingAttempts, resetTime);
            
            throw new RateLimitExceededException(operation, maxAttempts, maxAttempts, resetTime);
        }
    }

    /**
     * Records a rate limiting attempt for the specified operation and client IP.
     * 
     * <p>
     * This method encapsulates the rate limiting recording logic that should be called
     * after every authentication operation attempt, regardless of success or failure.
     * This ensures accurate tracking for rate limiting purposes.
     * </p>
     *
     * @param clientIP the client IP address to record the attempt for
     * @param operation the operation type for determining which rate limiting counter to update
     */
    private void recordRateLimitAttempt(String clientIP, String operation) {
        switch (operation) {
            case "login" -> rateLimitingService.recordLoginAttempt(clientIP);
            case "registration" -> rateLimitingService.recordRegistrationAttempt(clientIP);
            case "password reset", "password reset completion" -> rateLimitingService.recordPasswordResetAttempt(clientIP);
            case "email verification", "email verification resend" -> rateLimitingService.recordEmailVerificationAttempt(clientIP);
            default -> logger.warn("Unknown operation for rate limiting: {}", operation);
        }
    }

    /**
     * Authenticates a user with username and password credentials.
     *
     * <p>
     * This endpoint validates user credentials and, if successful, returns JWT access
     * and refresh tokens for authenticated session management. The authentication process
     * leverages Spring Security's authentication manager to verify credentials against
     * the user database.
     * </p>
     *
     * <h3>Authentication Flow</h3>
     * <ol>
     *   <li>Validate request format and required fields</li>
     *   <li>Authenticate credentials via Spring Security</li>
     *   <li>Generate JWT access token and refresh token</li>
     *   <li>Return tokens with basic user information</li>
     * </ol>
     *
     * @param request the login request containing username and password
     * @return a {@link ResponseEntity} containing {@link LoginResponseDTO} with tokens and user info
     * @throws org.springframework.security.core.AuthenticationException if credentials are invalid
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if request validation fails
     */
    @Operation(
            summary = "Login with username and password",
            description = "Authenticate user credentials and return JWT access token with refresh token for session management"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Successfully authenticated",
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)"),
            @ApiResponse(responseCode = "401", description = "Invalid username or password")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Parameter(description = "User login credentials", required = true)
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletRequest httpRequest) {
        
        // Extract client IP for rate limiting
        String clientIP = IPAddressUtil.getClientIpAddress(httpRequest);
        
        logger.debug("Processing login request from IP: {}", clientIP);
        logger.info("Login attempt for email: {} from IP: {}", request.email(), clientIP);
        
        // Check rate limit before proceeding
        validateRateLimit(clientIP, "login",
                () -> rateLimitingService.isLoginAllowed(clientIP),
                () -> rateLimitingService.getRemainingLoginAttempts(clientIP),
                () -> rateLimitingService.getLoginRateLimitResetTime(clientIP),
                ApplicationConstants.MAX_LOGIN_ATTEMPTS);
        
        try {
            LoginResponseDTO response = authService.login(request);
            logger.info("Authentication event: operation=login, ip={}, userId={}, success=true", 
                    clientIP, response.userId());
            logger.info("Successful login for user: {}, userId: {} from IP: {}", request.email(), response.userId(), clientIP);
            
            // Record the login attempt after successful login
            recordRateLimitAttempt(clientIP, "login");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.info("Authentication event: operation=login, ip={}, email={}, success=false, reason={}", 
                    clientIP, request.email(), e.getClass().getSimpleName());
            logger.warn("Failed login attempt for email: {} from IP: {} - {}", request.email(), clientIP, e.getMessage());
            
            // Record the login attempt even on failure to prevent brute force attacks
            recordRateLimitAttempt(clientIP, "login");
            
            throw e;
        }
    }

    /**
     * Registers a new user account and automatically authenticates them.
     *
     * <p>
     * This endpoint creates a new user account and immediately logs them in,
     * providing a seamless onboarding experience. The user receives authentication
     * tokens upon successful registration, eliminating the need for a separate login step.
     * </p>
     *
     * <h3>Registration Process</h3>
     * <ol>
     *   <li>Validate request data (email format, password strength, etc.)</li>
     *   <li>Check username and email uniqueness</li>
     *   <li>Create user account via {@link AuthService#register}</li>
     *   <li>Automatically authenticate the new user</li>
     *   <li>Generate and return authentication tokens</li>
     * </ol>
     *
     * <h3>Validation Requirements</h3>
     * <ul>
     *   <li><strong>Username</strong>: Must be unique in the system</li>
     *   <li><strong>Email</strong>: Must be valid format and unique</li>
     *   <li><strong>Password</strong>: 8-72 characters with complexity requirements</li>
     * </ul>
     *
     * @param request the user creation data containing username, email, password, and profile info
     * @return a {@link ResponseEntity} with HTTP 201 containing {@link RegisterResponseDTO} with tokens and user info
     * @throws com.yohan.event_planner.exception.ConflictException if username or email already exists
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if request validation fails
     */
    @Operation(
            summary = "Register a new user account with automatic login",
            description = "Create a new user account with username, email, and secure password. Upon successful registration, " +
                         "the user is automatically logged in and receives authentication tokens for immediate access. " +
                         "Username and email must be unique. Password must meet security requirements: " +
                         "8-72 characters with uppercase, lowercase, numbers, and special characters."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201", 
                    description = "User successfully registered and logged in",
                    content = @Content(schema = @Schema(implementation = RegisterResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)"),
            @ApiResponse(responseCode = "409", description = "Username or email already in use")
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(
            @Parameter(description = "User registration data", required = true)
            @Valid @RequestBody UserCreateDTO request,
            HttpServletRequest httpRequest) {
        
        // Extract client IP for rate limiting
        String clientIP = IPAddressUtil.getClientIpAddress(httpRequest);
        
        logger.debug("Processing registration request from IP: {}", clientIP);
        logger.info("Registration attempt for username: {}, email: {} from IP: {}", 
                request.username(), request.email(), clientIP);
        
        // Check rate limit before proceeding
        validateRateLimit(clientIP, "registration",
                () -> rateLimitingService.isRegistrationAllowed(clientIP),
                () -> rateLimitingService.getRemainingRegistrationAttempts(clientIP),
                () -> rateLimitingService.getRegistrationRateLimitResetTime(clientIP),
                ApplicationConstants.MAX_REGISTRATION_ATTEMPTS);
        
        try {
            RegisterResponseDTO response = authService.register(request);
            logger.info("Authentication event: operation=registration, ip={}, userId={}, success=true", 
                    clientIP, response.userId());
            logger.info("Successful registration and auto-login for user: {}, userId: {} from IP: {}", 
                    request.username(), response.userId(), clientIP);
            
            // Record the registration attempt after successful registration
            recordRateLimitAttempt(clientIP, "registration");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            logger.info("Authentication event: operation=registration, ip={}, username={}, success=false, reason={}", 
                    clientIP, request.username(), e.getClass().getSimpleName());
            logger.warn("Failed registration attempt for username: {} from IP: {} - {}", 
                    request.username(), clientIP, e.getMessage());
            
            // Record the registration attempt even on failure to prevent abuse
            recordRateLimitAttempt(clientIP, "registration");
            
            throw e;
        }
    }

    /**
     * Refreshes JWT access tokens using a valid refresh token.
     *
     * <p>
     * This endpoint allows clients to obtain new access tokens without requiring
     * the user to re-authenticate. The refresh token mechanism implements token rotation
     * for enhanced security, where each refresh operation generates both a new access token
     * and a new refresh token, invalidating the previous refresh token.
     * </p>
     *
     * <h3>Token Rotation Security</h3>
     * <ul>
     *   <li><strong>Single Use</strong>: Each refresh token can only be used once</li>
     *   <li><strong>Automatic Rotation</strong>: New refresh token issued with each refresh</li>
     *   <li><strong>Immediate Invalidation</strong>: Previous refresh token becomes invalid</li>
     *   <li><strong>Breach Detection</strong>: Reuse attempts indicate potential token theft</li>
     * </ul>
     *
     * @param request the token refresh request containing the current refresh token
     * @return a {@link ResponseEntity} containing {@link RefreshTokenResponseDTO} with new token pair
     * @throws com.yohan.event_planner.exception.UnauthorizedException if refresh token is invalid or expired
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if request validation fails
     */
    @Operation(
            summary = "Refresh access token using refresh token",
            description = "Generate new access token using valid refresh token. Refresh tokens are rotated for enhanced security."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Successfully refreshed tokens",
                    content = @Content(schema = @Schema(implementation = RefreshTokenResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(
            @Parameter(description = "Refresh token request data", required = true)
            @Valid @RequestBody TokenRequestDTO request) {
        logger.debug("Processing token refresh request");
        try {
            RefreshTokenResponseDTO response = authService.refreshToken(request);
            logger.info("Authentication event: operation=refresh, success=true");
            logger.info("Successful token refresh for user");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.info("Authentication event: operation=refresh, success=false, reason={}", e.getClass().getSimpleName());
            logger.warn("Failed token refresh attempt - {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Logs out a user by revoking their refresh token.
     *
     * <p>
     * This endpoint terminates a user's session by invalidating their refresh token,
     * preventing it from being used for future token generation. The logout operation
     * is designed to be idempotent and gracefully handles both valid and invalid tokens.
     * </p>
     *
     * <h3>Security Considerations</h3>
     * <ul>
     *   <li><strong>Token Revocation</strong>: Permanently invalidates the provided refresh token</li>
     *   <li><strong>Graceful Handling</strong>: Returns success even for invalid tokens to prevent enumeration</li>
     *   <li><strong>Session Cleanup</strong>: Ensures no orphaned refresh tokens remain in the system</li>
     *   <li><strong>Client Responsibility</strong>: Clients should discard both access and refresh tokens locally</li>
     * </ul>
     *
     * <h3>Post-Logout Behavior</h3>
     * <p>
     * After successful logout, the provided refresh token becomes permanently invalid.
     * Any subsequent attempts to use it for token refresh will result in an unauthorized error.
     * Clients should redirect users to the login page and clear all stored authentication state.
     * </p>
     *
     * @param request the logout request containing the refresh token to revoke
     * @return a {@link ResponseEntity} with HTTP 200 status indicating successful logout
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if request validation fails
     */
    @Operation(
            summary = "Logout user and revoke refresh token",
            description = "Revoke refresh token and invalidate user session. This prevents the refresh token from being used for future token generation."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully logged out"),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(description = "Logout request data containing refresh token", required = true)
            @Valid @RequestBody TokenRequestDTO request) {
        logger.debug("Processing logout request");
        try {
            authService.logout(request);
            logger.info("Authentication event: operation=logout, success=true");
            logger.info("Successful logout - refresh token revoked");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.info("Authentication event: operation=logout, success=false, reason={}", e.getClass().getSimpleName());
            logger.warn("Logout attempt failed - {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Initiates the password reset process by sending a secure reset link to the user's email.
     *
     * <p>
     * This endpoint implements a secure password reset workflow that protects against email enumeration
     * attacks by returning the same response regardless of whether the provided email address exists
     * in the system. When a valid email is provided, a time-limited reset token is generated and
     * sent via email.
     * </p>
     *
     * <h3>Security Features</h3>
     * <ul>
     *   <li><strong>Anti-Enumeration</strong>: Consistent response for valid and invalid emails</li>
     *   <li><strong>Token Expiration</strong>: Reset tokens have configurable expiration times</li>
     *   <li><strong>Single Use</strong>: Each reset token can only be used once</li>
     *   <li><strong>Secure Delivery</strong>: Reset links sent via authenticated email service</li>
     * </ul>
     *
     * <h3>Reset Token Properties</h3>
     * <ul>
     *   <li>Cryptographically secure random generation</li>
     *   <li>Limited lifetime (typically 15-60 minutes)</li>there's
     *   <li>Automatically invalidated after successful use</li>
     *   <li>Bound to specific user account for security</li>
     * </ul>
     *
     * @param request the forgot password request containing the user's email address
     * @return a {@link ResponseEntity} containing {@link ForgotPasswordResponseDTO} with standard success message
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if email format validation fails
     * @throws com.yohan.event_planner.exception.EmailException if email sending fails
     */
    @Operation(
            summary = "Request password reset",
            description = "Initiate password reset process by sending a secure reset link to the user's email address. " +
                         "Returns the same response regardless of whether the email exists to prevent email enumeration."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Password reset request processed (standard response for security)",
                    content = @Content(schema = @Schema(implementation = ForgotPasswordResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponseDTO> forgotPassword(
            @Parameter(description = "Forgot password request data", required = true)
            @Valid @RequestBody ForgotPasswordRequestDTO request,
            HttpServletRequest httpRequest) {
        
        // Extract client IP for rate limiting
        String clientIP = IPAddressUtil.getClientIpAddress(httpRequest);
        
        logger.debug("Processing password reset request from IP: {}", clientIP);
        logger.info("Password reset request for email: {} from IP: {}", request.email(), clientIP);
        
        // Check rate limit before proceeding
        validateRateLimit(clientIP, "password reset",
                () -> rateLimitingService.isPasswordResetAllowed(clientIP),
                () -> rateLimitingService.getRemainingPasswordResetAttempts(clientIP),
                () -> rateLimitingService.getPasswordResetRateLimitResetTime(clientIP),
                ApplicationConstants.MAX_PASSWORD_RESET_ATTEMPTS);
        
        try {
            ForgotPasswordResponseDTO response = passwordResetService.requestPasswordReset(request);
            logger.info("Security event: operation=password_reset_request, ip={}, email={}, success=true", 
                    clientIP, request.email());
            logger.info("Password reset request processed for email: {} from IP: {} (standard response for security)", request.email(), clientIP);
            
            // Record the password reset attempt after successful processing
            recordRateLimitAttempt(clientIP, "password reset");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.info("Security event: operation=password_reset_request, ip={}, email={}, success=false, reason={}", 
                    clientIP, request.email(), e.getClass().getSimpleName());
            logger.warn("Password reset request failed for email: {} from IP: {} - {}", request.email(), clientIP, e.getMessage());
            
            // Record the password reset attempt even on failure to prevent abuse
            recordRateLimitAttempt(clientIP, "password reset");
            
            throw e;
        }
    }

    /**
     * Completes the password reset process using a valid reset token and new password.
     *
     * <p>
     * This endpoint finalizes the password reset workflow by validating the provided reset token
     * and updating the user's password. The operation implements multiple security layers including
     * token validation, password strength requirements, and automatic token invalidation upon use.
     * </p>
     *
     * <h3>Validation Process</h3>
     * <ol>
     *   <li><strong>Token Validation</strong>: Verify token exists, is not expired, and not previously used</li>
     *   <li><strong>Password Validation</strong>: Ensure new password meets complexity requirements</li>
     *   <li><strong>User Verification</strong>: Confirm token belongs to an active user account</li>
     *   <li><strong>Password Update</strong>: Hash and store the new password securely</li>
     *   <li><strong>Token Cleanup</strong>: Mark token as used and invalidate it permanently</li>
     * </ol>
     *
     * <h3>Password Requirements</h3>
     * <ul>
     *   <li><strong>Length</strong>: 8-72 characters</li>
     *   <li><strong>Complexity</strong>: Must include uppercase, lowercase, numbers, and special characters</li>
     *   <li><strong>Pattern Validation</strong>: Cannot contain simple patterns or common weak passwords</li>
     *   <li><strong>History Check</strong>: Should not match recent previous passwords</li>
     * </ul>
     *
     * <h3>Security Considerations</h3>
     * <ul>
     *   <li>Reset tokens are single-use and immediately invalidated</li>
     *   <li>Failed attempts are logged for security monitoring</li>
     *   <li>Password validation occurs before token validation to prevent timing attacks</li>
     *   <li>All active sessions for the user are optionally invalidated</li>
     * </ul>
     *
     * @param request the reset password request containing the reset token and new password
     * @return a {@link ResponseEntity} containing {@link ResetPasswordResponseDTO} with success confirmation
     * @throws com.yohan.event_planner.exception.UnauthorizedException if token is invalid, expired, or already used
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if password validation fails
     */
    @Operation(
            summary = "Reset password with token",
            description = "Complete password reset using a valid reset token received via email. " +
                         "The token is single-use and expires after a configurable time period. " +
                         "New password must meet security requirements: 8-72 characters with uppercase, lowercase, numbers, and special characters."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Password reset successful",
                    content = @Content(schema = @Schema(implementation = ResetPasswordResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data or token validation failure"),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, or already used reset token")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<ResetPasswordResponseDTO> resetPassword(
            @Parameter(description = "Reset password request data", required = true)
            @Valid @RequestBody ResetPasswordRequestDTO request,
            HttpServletRequest httpRequest) {
        
        // Extract client IP for rate limiting
        String clientIP = IPAddressUtil.getClientIpAddress(httpRequest);
        
        logger.info("Password reset attempt with token from IP: {}", clientIP);
        
        // Check rate limit before proceeding
        validateRateLimit(clientIP, "password reset completion",
                () -> rateLimitingService.isPasswordResetAllowed(clientIP),
                () -> rateLimitingService.getRemainingPasswordResetAttempts(clientIP),
                () -> rateLimitingService.getPasswordResetRateLimitResetTime(clientIP),
                ApplicationConstants.MAX_PASSWORD_RESET_ATTEMPTS);
        
        try {
            ResetPasswordResponseDTO response = passwordResetService.resetPassword(request);
            logger.info("Successful password reset completed from IP: {}", clientIP);
            
            // Record the password reset attempt after successful completion
            recordRateLimitAttempt(clientIP, "password reset completion");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.warn("Password reset failed from IP: {} - {}", clientIP, e.getMessage());
            
            // Record the password reset attempt even on failure to prevent token brute force
            recordRateLimitAttempt(clientIP, "password reset completion");
            
            throw e;
        }
    }

    /**
     * Verifies a user's email address using a verification token.
     *
     * <p>
     * This endpoint completes the email verification process by validating the provided
     * verification token and activating the user's account. Once verified, users can
     * log in normally and access all application features.
     * </p>
     *
     * <h3>Verification Process</h3>
     * <ol>
     *   <li><strong>Token Validation</strong>: Verify token exists, is not expired, and not previously used</li>
     *   <li><strong>Account Activation</strong>: Mark user's email as verified</li>
     *   <li><strong>Token Cleanup</strong>: Mark token as used and invalidate it permanently</li>
     *   <li><strong>Response Generation</strong>: Return success confirmation with username</li>
     * </ol>
     *
     * <h3>Security Features</h3>
     * <ul>
     *   <li><strong>Single Use</strong>: Tokens can only be used once</li>
     *   <li><strong>Time Limited</strong>: Tokens expire after 24 hours</li>
     *   <li><strong>Secure Generation</strong>: 64-character cryptographically secure tokens</li>
     *   <li><strong>Audit Logging</strong>: Verification events logged for security</li>
     * </ul>
     *
     * @param request the email verification request containing the verification token
     * @return a {@link ResponseEntity} containing {@link EmailVerificationResponseDTO} with verification result
     * @throws com.yohan.event_planner.exception.EmailException if token is invalid, expired, or verification fails
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if request validation fails
     */
    @Operation(
            summary = "Verify email address with verification token",
            description = "Complete email verification using a valid verification token received via email. " +
                         "Successfully verified users can then log in normally."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Email verified successfully",
                    content = @Content(schema = @Schema(implementation = EmailVerificationResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)"),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, or already used verification token")
    })
    @PostMapping("/verify-email")
    public ResponseEntity<EmailVerificationResponseDTO> verifyEmail(
            @Parameter(description = "Email verification request data", required = true)
            @Valid @RequestBody EmailVerificationRequestDTO request,
            HttpServletRequest httpRequest) {
        
        // Extract client IP for rate limiting
        String clientIP = IPAddressUtil.getClientIpAddress(httpRequest);
        
        logger.info("Email verification attempt with token from IP: {}", clientIP);
        
        // Check rate limit before proceeding
        validateRateLimit(clientIP, "email verification",
                () -> rateLimitingService.isEmailVerificationAllowed(clientIP),
                () -> rateLimitingService.getRemainingEmailVerificationAttempts(clientIP),
                () -> rateLimitingService.getEmailVerificationRateLimitResetTime(clientIP),
                ApplicationConstants.MAX_EMAIL_VERIFICATION_ATTEMPTS);
        
        try {
            var verifiedUser = emailVerificationService.verifyEmail(request.token());
            if (verifiedUser.isPresent()) {
                User user = verifiedUser.get();
                logger.info("Email verification successful for user: {} from IP: {}", user.getUsername(), clientIP);
                
                // Record the email verification attempt after successful verification
                recordRateLimitAttempt(clientIP, "email verification");
                
                return ResponseEntity.ok(EmailVerificationResponseDTO.success(user.getUsername()));
            } else {
                logger.warn("Email verification failed from IP: {} - token invalid or already used", clientIP);
                
                // Record the email verification attempt even on failure to prevent token brute force
                recordRateLimitAttempt(clientIP, "email verification");
                
                return ResponseEntity.badRequest()
                        .body(EmailVerificationResponseDTO.failure("Invalid or expired verification token"));
            }
        } catch (Exception e) {
            logger.warn("Email verification failed from IP: {} - {}", clientIP, e.getMessage());
            
            // Record the email verification attempt even on exception to prevent token brute force
            recordRateLimitAttempt(clientIP, "email verification");
            
            throw e;
        }
    }

    /**
     * Resends email verification to a user who hasn't verified their account.
     *
     * <p>
     * This endpoint allows users to request a new verification email if they didn't
     * receive the original email or if their token has expired. For security reasons,
     * the response is the same regardless of whether the email exists in the system.
     * </p>
     *
     * <h3>Process Flow</h3>
     * <ol>
     *   <li><strong>Email Lookup</strong>: Find user account by email address</li>
     *   <li><strong>Verification Check</strong>: Check if email is already verified</li>
     *   <li><strong>Token Generation</strong>: Generate new verification token if needed</li>
     *   <li><strong>Email Delivery</strong>: Send new verification email</li>
     *   <li><strong>Standard Response</strong>: Return consistent response for security</li>
     * </ol>
     *
     * <h3>Security Features</h3>
     * <ul>
     *   <li><strong>Anti-Enumeration</strong>: Same response regardless of email existence</li>
     *   <li><strong>Token Invalidation</strong>: Previous verification tokens are invalidated</li>
     *   <li><strong>Already Verified</strong>: Gracefully handles already verified accounts</li>
     *   <li><strong>Rate Limiting</strong>: Future enhancement for abuse prevention</li>
     * </ul>
     *
     * @param request the resend verification request containing the email address
     * @return a {@link ResponseEntity} containing {@link ResendVerificationResponseDTO} with standard response
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if request validation fails
     */
    @Operation(
            summary = "Resend email verification",
            description = "Request a new verification email for an unverified account. " +
                         "Returns the same response regardless of whether the email exists to prevent enumeration."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Resend request processed (standard response for security)",
                    content = @Content(schema = @Schema(implementation = ResendVerificationResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)")
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<ResendVerificationResponseDTO> resendVerification(
            @Parameter(description = "Resend verification request data", required = true)
            @Valid @RequestBody ResendVerificationRequestDTO request,
            HttpServletRequest httpRequest) {
        
        // Extract client IP for rate limiting
        String clientIP = IPAddressUtil.getClientIpAddress(httpRequest);
        
        logger.info("Resend verification email request for: {} from IP: {}", request.email(), clientIP);
        
        // Check rate limit before proceeding
        validateRateLimit(clientIP, "email verification resend",
                () -> rateLimitingService.isEmailVerificationAllowed(clientIP),
                () -> rateLimitingService.getRemainingEmailVerificationAttempts(clientIP),
                () -> rateLimitingService.getEmailVerificationRateLimitResetTime(clientIP),
                ApplicationConstants.MAX_EMAIL_VERIFICATION_ATTEMPTS);
        
        try {
            var userOptional = userBO.getUserByEmail(request.email());
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                boolean resent = emailVerificationService.resendVerificationEmail(user);
                if (resent) {
                    logger.info("Verification email resent for user: {} from IP: {}", user.getUsername(), clientIP);
                } else {
                    logger.info("Email already verified for user: {} from IP: {}", user.getUsername(), clientIP);
                }
            } else {
                logger.info("No user found with email: {} from IP: {} (standard response for security)", request.email(), clientIP);
            }
            
            // Record the email verification attempt after processing
            recordRateLimitAttempt(clientIP, "email verification resend");
            
            // Always return the same response for security (anti-enumeration)
            return ResponseEntity.ok(ResendVerificationResponseDTO.standard());
        } catch (Exception e) {
            logger.warn("Resend verification email failed for: {} from IP: {} - {}", request.email(), clientIP, e.getMessage());
            
            // Record the email verification attempt even on failure to prevent abuse
            recordRateLimitAttempt(clientIP, "email verification resend");
            
            // Still return standard response even on error to prevent enumeration
            return ResponseEntity.ok(ResendVerificationResponseDTO.standard());
        }
    }
}
