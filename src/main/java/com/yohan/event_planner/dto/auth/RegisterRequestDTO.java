package com.yohan.event_planner.dto.auth;

import com.yohan.event_planner.constants.ApplicationConstants;
import com.yohan.event_planner.validation.ValidZoneId;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new user account via public registration.
 *
 * <p>
 * Used by the authentication API (e.g., {@code POST /auth/register}) to accept user-submitted
 * registration data. All fields are validated and required for successful account creation.
 * </p>
 *
 * <p>
 * This DTO exists separately from internal creation models to allow for strict validation and
 * API boundary clarity (e.g., additional terms fields, invitation tokens, etc.).
 * </p>
 */
public record RegisterRequestDTO(

        /** Desired username for the new account. Must be unique and 3–30 characters long. */
        @Pattern(
                regexp = "^[a-z0-9]+(?:[._][a-z0-9]+)*$",
                message = "Username must be lowercase and may contain letters, numbers, periods, or underscores, without leading, trailing, or consecutive special characters"
        )
        @NotBlank(message = "Username is required")
        @Size(min = ApplicationConstants.USERNAME_MIN_LENGTH, max = ApplicationConstants.USERNAME_MAX_LENGTH)
        String username,

        /** Plaintext password for the new account. Must be 8–72 characters long. */
        @NotBlank(message = "Password is required")
        @Size(min = ApplicationConstants.PASSWORD_MIN_LENGTH, max = ApplicationConstants.PASSWORD_MAX_LENGTH)
        String password,

        /** Email address for the user. Must be a valid email format. */
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        /** User's first name. */
        @NotBlank(message = "First name is required")
        @Size(min = ApplicationConstants.NAME_MIN_LENGTH, max = ApplicationConstants.SHORT_NAME_MAX_LENGTH)
        String firstName,

        /** User's last name. */
        @NotBlank(message = "Last name is required")
        @Size(min = ApplicationConstants.NAME_MIN_LENGTH, max = ApplicationConstants.SHORT_NAME_MAX_LENGTH)
        String lastName,

        /** Preferred time zone of the user (e.g., "America/New_York"). */
        @NotBlank(message = "Timezone is required")
        @ValidZoneId(message = "Invalid timezone provided")
        String timezone

) {}
