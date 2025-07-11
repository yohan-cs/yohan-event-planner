package com.yohan.event_planner.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for password reset requests.
 *
 * <p>
 * This DTO encapsulates the email address submitted when a user requests
 * a password reset. It includes validation to ensure the email format is
 * correct and that the field is not empty.
 * </p>
 *
 * <h2>Validation Rules</h2>
 * <ul>
 *   <li><strong>Email Format</strong>: Must be a valid email address format</li>
 *   <li><strong>Required Field</strong>: Email cannot be blank or null</li>
 * </ul>
 *
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li><strong>Email Enumeration</strong>: System should not reveal whether email exists</li>
 *   <li><strong>Rate Limiting</strong>: Should be implemented at controller level</li>
 *   <li><strong>Input Sanitization</strong>: Email is validated and sanitized</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * {
 *   "email": "user@example.com"
 * }
 * }</pre>
 *
 * @param email the email address to send password reset link to
 *
 * @see com.yohan.event_planner.controller.AuthController
 * @see com.yohan.event_planner.service.PasswordResetService
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 2.0.0
 */
public record ForgotPasswordRequestDTO(

        /** The email address where the password reset link should be sent. */
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email

) {}