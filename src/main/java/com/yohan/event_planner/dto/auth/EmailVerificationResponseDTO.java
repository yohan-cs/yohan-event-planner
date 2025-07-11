package com.yohan.event_planner.dto.auth;

/**
 * Response DTO returned after email verification operations.
 *
 * <p>
 * This DTO provides confirmation of successful email verification and includes
 * information about the next steps for the user. After successful verification,
 * users can proceed to log in with their credentials.
 * </p>
 *
 * <h2>Success Response</h2>
 * <p>
 * When email verification is successful, this response confirms that the user's
 * account has been activated and they can now log in normally.
 * </p>
 *
 * <h2>Usage Pattern</h2>
 * <pre>{@code
 * // Successful verification response
 * {
 *   "success": true,
 *   "message": "Email verified successfully! You can now log in to your account.",
 *   "username": "testuser"
 * }
 * }</pre>
 *
 * @param success indicates whether the verification was successful
 * @param message descriptive message about the verification result
 * @param username the username of the verified account (for convenience)
 *
 * @see EmailVerificationRequestDTO
 * @see com.yohan.event_planner.service.EmailVerificationService
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
public record EmailVerificationResponseDTO(
        
        /** Indicates whether the email verification was successful. */
        boolean success,
        
        /** Descriptive message about the verification result. */
        String message,
        
        /** Username of the verified account for user convenience. */
        String username
) {
    
    /**
     * Creates a successful email verification response.
     *
     * @param username the username of the verified account
     * @return a success response with standard message
     */
    public static EmailVerificationResponseDTO success(String username) {
        return new EmailVerificationResponseDTO(
                true,
                "Email verified successfully! You can now log in to your account.",
                username
        );
    }
    
    /**
     * Creates a failed email verification response.
     *
     * @param message the error message explaining why verification failed
     * @return a failure response with custom message
     */
    public static EmailVerificationResponseDTO failure(String message) {
        return new EmailVerificationResponseDTO(
                false,
                message,
                null
        );
    }
}