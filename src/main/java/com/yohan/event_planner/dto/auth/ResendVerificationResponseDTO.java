package com.yohan.event_planner.dto.auth;

/**
 * Response DTO returned after resend verification email operations.
 *
 * <p>
 * This DTO provides confirmation that a verification email resend request has
 * been processed. For security reasons, the response is the same regardless of
 * whether the email address exists in the system, preventing email enumeration attacks.
 * </p>
 *
 * <h2>Security Response Pattern</h2>
 * <p>
 * The response message is intentionally generic to prevent attackers from
 * determining which email addresses are registered in the system. The same
 * success message is returned whether the email exists or not.
 * </p>
 *
 * <h2>Usage Pattern</h2>
 * <pre>{@code
 * // Standard response (same for existing and non-existing emails)
 * {
 *   "message": "If an account with that email exists, a new verification email has been sent."
 * }
 * }</pre>
 *
 * @param message the response message about the resend operation
 *
 * @see ResendVerificationRequestDTO
 * @see com.yohan.event_planner.service.EmailVerificationService
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
public record ResendVerificationResponseDTO(
        
        /** Message about the resend verification email operation. */
        String message
) {
    
    /**
     * Creates a standard resend verification response.
     * 
     * <p>
     * This method returns the same response regardless of whether the email
     * exists in the system, following security best practices to prevent
     * email enumeration attacks.
     * </p>
     *
     * @return a standard response message for resend operations
     */
    public static ResendVerificationResponseDTO standard() {
        return new ResendVerificationResponseDTO(
                "If an account with that email exists, a new verification email has been sent."
        );
    }
    
    /**
     * Creates a rate-limited response for too many resend requests.
     *
     * @return a response indicating rate limiting is in effect
     */
    public static ResendVerificationResponseDTO rateLimited() {
        return new ResendVerificationResponseDTO(
                "Too many requests. Please wait before requesting another verification email."
        );
    }
}