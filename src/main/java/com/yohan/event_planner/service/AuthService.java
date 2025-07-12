package com.yohan.event_planner.service;

import com.yohan.event_planner.dto.UserCreateDTO;
import com.yohan.event_planner.dto.auth.LoginRequestDTO;
import com.yohan.event_planner.dto.auth.LoginResponseDTO;
import com.yohan.event_planner.dto.auth.RefreshTokenResponseDTO;
import com.yohan.event_planner.dto.auth.RegisterResponseDTO;
import com.yohan.event_planner.dto.auth.TokenRequestDTO;

/**
 * Service interface for handling user authentication operations such as login, registration, token refresh, and logout.
 *
 * <p>
 * Provides high-level methods for authenticating users, creating new user accounts,
 * managing refresh tokens, and handling logout operations.
 * Implementations are expected to handle validation, token generation, and conflict checks.
 * </p>
 *
 * <h2>Security Architecture</h2>
 * <p>
 * This service implements a multi-layered security approach designed to prevent various
 * attack vectors while maintaining usability:
 * </p>
 * <ul>
 *   <li><strong>Authentication Security</strong>: Credential validation with Spring Security integration</li>
 *   <li><strong>Email Verification</strong>: Mandatory email verification prevents fake account creation</li>
 *   <li><strong>Token Security</strong>: JWT access tokens with secure refresh token rotation</li>
 *   <li><strong>Domain Validation</strong>: Email domain filtering blocks disposable email services</li>
 * </ul>
 *
 * <h2>Threat Model & Attack Prevention</h2>
 * <p>
 * The authentication system is designed to mitigate the following attack vectors:
 * </p>
 * <ul>
 *   <li><strong>Credential Stuffing</strong>: Rate limiting and account lockout mechanisms</li>
 *   <li><strong>Fake Account Creation</strong>: Email verification and domain validation</li>
 *   <li><strong>Token Theft</strong>: Short-lived access tokens with secure refresh rotation</li>
 *   <li><strong>Session Hijacking</strong>: Stateless JWT tokens with proper validation</li>
 *   <li><strong>Email Enumeration</strong>: Consistent response timing and error messages</li>
 *   <li><strong>Disposable Email Abuse</strong>: Domain blacklisting and validation</li>
 * </ul>
 *
 * <h2>Security Controls</h2>
 * <ul>
 *   <li><strong>Multi-Factor Verification</strong>: Email verification required for activation</li>
 *   <li><strong>Token Expiration</strong>: Time-limited access tokens with rotation</li>
 *   <li><strong>Secure Logout</strong>: Token invalidation prevents reuse</li>
 *   <li><strong>Audit Logging</strong>: Security events logged for monitoring</li>
 * </ul>
 *
 * @see com.yohan.event_planner.dto.auth.LoginRequestDTO
 * @see com.yohan.event_planner.dto.UserCreateDTO
 * @see com.yohan.event_planner.dto.auth.LoginResponseDTO
 * @see com.yohan.event_planner.dto.auth.RegisterResponseDTO
 * @see com.yohan.event_planner.dto.auth.TokenRequestDTO
 * @see com.yohan.event_planner.dto.auth.RefreshTokenResponseDTO
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
     * <h3>Security Measures</h3>
     * <ul>
     *   <li><strong>Credential Validation</strong>: Delegates to Spring Security for secure password verification</li>
     *   <li><strong>Email Verification Check</strong>: Prevents login for unverified email addresses</li>
     *   <li><strong>Account Status Validation</strong>: Rejects disabled, locked, or expired accounts</li>
     *   <li><strong>Token Generation</strong>: Creates secure JWT access tokens and refresh tokens</li>
     *   <li><strong>Audit Logging</strong>: Logs authentication attempts for security monitoring</li>
     * </ul>
     *
     * <h3>Attack Prevention</h3>
     * <ul>
     *   <li><strong>Brute Force Protection</strong>: Rate limiting prevents credential stuffing attacks</li>
     *   <li><strong>Timing Attack Mitigation</strong>: Consistent response times prevent user enumeration</li>
     *   <li><strong>Token Security</strong>: Short-lived access tokens limit exposure window</li>
     * </ul>
     *
     * @param request the login request DTO containing username and password
     * @return a {@link LoginResponseDTO} containing JWT access token, refresh token, and user information
     * @throws com.yohan.event_planner.exception.UnauthorizedException if credentials are invalid
     * @throws com.yohan.event_planner.exception.EmailException if email is not verified
     */
    LoginResponseDTO login(LoginRequestDTO request);

    /**
     * Registers a new user account with comprehensive security validation.
     *
     * <p>
     * <strong>Security Note:</strong> This implementation differs from the typical pattern
     * of immediate authentication. Instead, it requires email verification before login
     * to enhance security and prevent abuse.
     * </p>
     *
     * <h3>Security Measures</h3>
     * <ul>
     *   <li><strong>Email Domain Validation</strong>: Blocks disposable email services and invalid domains</li>
     *   <li><strong>Uniqueness Validation</strong>: Prevents duplicate usernames and email addresses</li>
     *   <li><strong>Email Verification Required</strong>: No authentication tokens until email is verified</li>
     *   <li><strong>Input Sanitization</strong>: Validates and sanitizes all user input</li>
     *   <li><strong>Password Security</strong>: Enforces strong password policies</li>
     * </ul>
     *
     * <h3>Attack Prevention</h3>
     * <ul>
     *   <li><strong>Fake Account Creation</strong>: Email verification prevents throwaway accounts</li>
     *   <li><strong>Domain Abuse</strong>: Blacklists known disposable email providers</li>
     *   <li><strong>Resource Exhaustion</strong>: Rate limiting prevents spam registrations</li>
     *   <li><strong>Data Pollution</strong>: Verification requirement ensures legitimate users</li>
     * </ul>
     *
     * <h3>Registration Flow</h3>
     * <ol>
     *   <li>Validate email domain against security policies</li>
     *   <li>Check username and email uniqueness</li>
     *   <li>Create user account in pending verification state</li>
     *   <li>Send email verification token</li>
     *   <li>Return registration confirmation (no tokens)</li>
     * </ol>
     *
     * @param request the user creation DTO containing user details
     * @return a {@link RegisterResponseDTO} with user information and verification instructions
     * @throws com.yohan.event_planner.exception.ConflictException if the username or email is already taken
     * @throws com.yohan.event_planner.exception.EmailException if email domain is invalid or blocked
     */
    RegisterResponseDTO register(UserCreateDTO request);

    /**
     * Refreshes an access token using a valid refresh token.
     *
     * <p>
     * This method validates the refresh token and, if valid, generates a new
     * access token and refresh token pair. The old refresh token is invalidated
     * to implement one-time use security.
     * </p>
     *
     * <h3>Security Measures</h3>
     * <ul>
     *   <li><strong>Token Rotation</strong>: Issues new refresh token and invalidates the old one</li>
     *   <li><strong>Expiration Validation</strong>: Checks token expiration and validity</li>
     *   <li><strong>One-Time Use</strong>: Refresh tokens can only be used once</li>
     *   <li><strong>User Validation</strong>: Ensures associated user is still active and verified</li>
     * </ul>
     *
     * <h3>Attack Prevention</h3>
     * <ul>
     *   <li><strong>Token Theft Mitigation</strong>: Rotation limits damage from stolen tokens</li>
     *   <li><strong>Replay Attack Prevention</strong>: One-time use prevents token reuse</li>
     *   <li><strong>Session Hijacking Protection</strong>: Short-lived access tokens limit exposure</li>
     * </ul>
     *
     * @param request the token request DTO containing the refresh token
     * @return a {@link RefreshTokenResponseDTO} containing new access and refresh tokens
     * @throws com.yohan.event_planner.exception.UnauthorizedException if the refresh token is invalid or expired
     */
    RefreshTokenResponseDTO refreshToken(TokenRequestDTO request);

    /**
     * Logs out a user by invalidating their refresh token.
     *
     * <p>
     * This method revokes the provided refresh token, preventing it from being
     * used to generate new access tokens. The client should discard both
     * access and refresh tokens after logout.
     * </p>
     *
     * <h3>Security Measures</h3>
     * <ul>
     *   <li><strong>Token Revocation</strong>: Immediately invalidates the refresh token</li>
     *   <li><strong>Session Termination</strong>: Prevents further access with existing tokens</li>
     *   <li><strong>Audit Logging</strong>: Records logout events for security monitoring</li>
     * </ul>
     *
     * <h3>Attack Prevention</h3>
     * <ul>
     *   <li><strong>Stolen Token Mitigation</strong>: Immediate revocation prevents unauthorized use</li>
     *   <li><strong>Account Takeover Protection</strong>: Users can force logout from all devices</li>
     * </ul>
     *
     * <h3>Client Responsibilities</h3>
     * <p>
     * After successful logout, clients must:
     * </p>
     * <ul>
     *   <li>Clear all stored authentication tokens</li>
     *   <li>Redirect to login page or public area</li>
     *   <li>Clear any cached user data</li>
     * </ul>
     *
     * @param request the token request DTO containing the refresh token to revoke
     */
    void logout(TokenRequestDTO request);
}

