package com.yohan.event_planner.dto.auth;

/**
 * Data Transfer Object for password reset request responses.
 *
 * <p>
 * This DTO provides a consistent response message for password reset requests,
 * regardless of whether the email address exists in the system. This approach
 * prevents email enumeration attacks while still providing useful feedback
 * to legitimate users.
 * </p>
 *
 * <h2>Security Features</h2>
 * <ul>
 *   <li><strong>Anti-Enumeration</strong>: Same response whether email exists or not</li>
 *   <li><strong>User Guidance</strong>: Clear instructions for next steps</li>
 *   <li><strong>Timing Consistency</strong>: Response time should be consistent</li>
 * </ul>
 *
 * <h2>Standard Response</h2>
 * <p>
 * The response message should be generic but helpful, informing the user
 * that if their email is registered, they will receive a reset link.
 * This protects against email enumeration while providing clear guidance.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * {
 *   "message": "If your email is registered, you will receive a password reset link shortly."
 * }
 * }</pre>
 *
 * @param message the response message to display to the user
 *
 * @see ForgotPasswordRequestDTO
 * @see com.yohan.event_planner.controller.AuthController
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
public record ForgotPasswordResponseDTO(

        /** The message explaining the next steps for password reset. */
        String message

) {

    /**
     * Creates a standard response for password reset requests.
     *
     * <p>
     * This factory method creates a consistent response message that doesn't
     * reveal whether the email address exists in the system, helping to prevent
     * email enumeration attacks.
     * </p>
     *
     * @return a standardized response for all password reset requests
     */
    public static ForgotPasswordResponseDTO standard() {
        return new ForgotPasswordResponseDTO(
            "If your email is registered, you will receive a password reset link shortly."
        );
    }

    /**
     * Creates a response for rate-limited requests.
     *
     * <p>
     * This factory method creates a response when the user has exceeded
     * the allowed number of password reset requests within a time window.
     * </p>
     *
     * @return a response indicating rate limiting is in effect
     */
    public static ForgotPasswordResponseDTO rateLimited() {
        return new ForgotPasswordResponseDTO(
            "Too many password reset requests. Please wait before trying again."
        );
    }
}