package com.yohan.event_planner.dto;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.validation.ValidZoneId;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new {@link User} account.
 *
 * <p>
 * Used internally to pass validated user creation data to the service layer.
 * Includes all required fields with validation annotations to enforce formatting and completeness.
 * </p>
 */
public record UserCreateDTO(

        /** Desired username for the new account. Must be 3–30 characters. */
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 30)
        String username,

        /** Plaintext password for the account. Must be 8–72 characters. */
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72)
        String password,

        /** User's email address. Must be a valid email format. */
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        /** User's first name. Required, 1–50 characters. */
        @NotBlank(message = "First name is required")
        @Size(min = 1, max = 50)
        String firstName,

        /** User's last name. Required, 1–50 characters. */
        @NotBlank(message = "Last name is required")
        @Size(min = 1, max = 50)
        String lastName,

        /** Preferred IANA timezone ID (e.g., "America/New_York"). */
        @NotBlank(message = "Timezone is required")
        @ValidZoneId(message = "Invalid timezone provided")
        String timezone

) {}
