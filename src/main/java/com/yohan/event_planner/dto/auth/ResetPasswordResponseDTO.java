package com.yohan.event_planner.dto.auth;

/**
 * Data Transfer Object for password reset completion responses.
 *
 * <p>
 * This DTO provides confirmation that a password reset operation has been
 * completed successfully. It includes a success message and may include
 * additional security information for the user.
 * </p>
 *
 * <h2>Response Information</h2>
 * <ul>
 *   <li><strong>Success Confirmation</strong>: Clear indication that password was changed</li>
 *   <li><strong>Security Notice</strong>: Information about session invalidation</li>
 *   <li><strong>Next Steps</strong>: Guidance for logging in with new password</li>
 * </ul>
 *
 * <h2>Security Features</h2>
 * <ul>
 *   <li><strong>Session Cleanup</strong>: All existing sessions are invalidated</li>
 *   <li><strong>Audit Trail</strong>: Password change is logged for security</li>
 *   <li><strong>Notification</strong>: User receives email confirmation of change</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * {
 *   "message": "Your password has been successfully reset. Please log in with your new password."
 * }
 * }</pre>
 *
 * @param message the confirmation message for the password reset
 *
 * @see ResetPasswordRequestDTO
 * @see com.yohan.event_planner.controller.AuthController
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
public record ResetPasswordResponseDTO(

        /** The confirmation message for successful password reset. */
        String message

) {

    /**
     * Creates a standard success response for password reset completion.
     *
     * <p>
     * This factory method creates a consistent success message that informs
     * the user their password has been changed and provides guidance for
     * logging in with the new credentials.
     * </p>
     *
     * @return a standardized success response for password reset
     */
    public static ResetPasswordResponseDTO success() {
        return new ResetPasswordResponseDTO(
            "Your password has been successfully reset. Please log in with your new password."
        );
    }

    /**
     * Creates an error response for invalid or expired tokens.
     *
     * <p>
     * This factory method creates a response when the provided reset token
     * is invalid, expired, or has already been used. The user will need to
     * request a new password reset.
     * </p>
     *
     * @return a response indicating the token is invalid
     */
    public static ResetPasswordResponseDTO invalidToken() {
        return new ResetPasswordResponseDTO(
            "Invalid or expired reset token. Please request a new password reset."
        );
    }
}