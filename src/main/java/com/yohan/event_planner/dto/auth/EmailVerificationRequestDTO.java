package com.yohan.event_planner.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for email verification operations.
 *
 * <p>
 * This DTO is used when a user clicks on the verification link in their email
 * to verify their email address and activate their account. The token is extracted
 * from the verification link and sent to the backend for validation.
 * </p>
 *
 * <h2>Validation Rules</h2>
 * <ul>
 *   <li><strong>Token</strong>: Must be present and non-empty</li>
 *   <li><strong>Length</strong>: Expected to be 64 characters (cryptographically secure)</li>
 * </ul>
 *
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li><strong>Single Use</strong>: Each token can only be used once</li>
 *   <li><strong>Expiration</strong>: Tokens expire after 24 hours by default</li>
 *   <li><strong>Secure Generation</strong>: Tokens are cryptographically secure</li>
 * </ul>
 *
 * @param token the email verification token from the verification link
 *
 * @see EmailVerificationResponseDTO
 * @see com.yohan.event_planner.service.EmailVerificationService
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
public record EmailVerificationRequestDTO(
        
        /**
         * The email verification token received from the verification email.
         * This should be a 64-character alphanumeric string.
         */
        @NotBlank(message = "Verification token is required")
        @Size(min = 1, max = 128, message = "Verification token must be between 1 and 128 characters")
        String token
) {}