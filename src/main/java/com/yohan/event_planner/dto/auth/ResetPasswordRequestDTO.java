package com.yohan.event_planner.dto.auth;

import com.yohan.event_planner.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for password reset completion requests.
 *
 * <p>
 * This DTO encapsulates the token and new password submitted when a user
 * completes the password reset process. It includes validation to ensure
 * both the token and password meet security requirements.
 * </p>
 *
 * <h2>Validation Rules</h2>
 * <ul>
 *   <li><strong>Token Required</strong>: Reset token cannot be blank or null</li>
 *   <li><strong>Password Length</strong>: New password must meet minimum length requirements</li>
 *   <li><strong>Password Strength</strong>: Should meet application password policy</li>
 * </ul>
 *
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li><strong>Token Validation</strong>: Token will be validated for authenticity and expiry</li>
 *   <li><strong>Single Use</strong>: Token becomes invalid after successful password reset</li>
 *   <li><strong>Password Hashing</strong>: New password will be securely hashed before storage</li>
 *   <li><strong>Session Invalidation</strong>: All user sessions should be invalidated</li>
 * </ul>
 *
 * <h2>Password Policy</h2>
 * <p>
 * The new password must meet the same requirements as initial registration:
 * minimum length, complexity requirements, and should not be a commonly
 * used password.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * {
 *   "token": "abc123def456ghi789...",
 *   "newPassword": "MyNewSecurePassword123!"
 * }
 * }</pre>
 *
 * @param token the password reset token received via email
 * @param newPassword the new password to set for the user account
 *
 * @see com.yohan.event_planner.controller.AuthController
 * @see com.yohan.event_planner.service.PasswordResetService
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
public record ResetPasswordRequestDTO(

        /** The password reset token from the email link. */
        @NotBlank(message = "Reset token is required")
        String token,

        /** The new password to set for the user account. */
        @NotBlank(message = "New password is required")
        @ValidPassword
        String newPassword

) {}