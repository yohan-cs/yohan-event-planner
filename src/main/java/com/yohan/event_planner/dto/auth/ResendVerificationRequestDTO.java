package com.yohan.event_planner.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for resending email verification emails.
 *
 * <p>
 * This DTO is used when a user requests a new verification email, typically
 * because they didn't receive the original email or the token has expired.
 * The system will generate a new verification token and send it to the user's
 * registered email address.
 * </p>
 *
 * <h2>Validation Rules</h2>
 * <ul>
 *   <li><strong>Email</strong>: Must be a valid email format</li>
 *   <li><strong>Length</strong>: Email must be between 1 and 254 characters</li>
 *   <li><strong>Registration</strong>: Email must belong to a registered user</li>
 * </ul>
 *
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li><strong>Anti-Enumeration</strong>: Same response regardless of email existence</li>
 *   <li><strong>Rate Limiting</strong>: Prevents abuse through frequency limits</li>
 *   <li><strong>Token Invalidation</strong>: Previous tokens are invalidated</li>
 * </ul>
 *
 * @param email the email address to resend verification to
 *
 * @see ResendVerificationResponseDTO
 * @see com.yohan.event_planner.service.EmailVerificationService
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
public record ResendVerificationRequestDTO(
        
        /**
         * The email address to resend the verification email to.
         * Must be a valid email format and belong to a registered user account.
         */
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        @Size(max = 254, message = "Email must be less than 255 characters")
        String email
) {}