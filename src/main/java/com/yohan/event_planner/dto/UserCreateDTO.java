package com.yohan.event_planner.dto;

import com.yohan.event_planner.constants.ApplicationConstants;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.validation.ValidPassword;
import com.yohan.event_planner.validation.ValidZoneId;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new {@link User} account.
 *
 * <p>
 * Used internally to pass validated user creation data to the service layer.
 * Includes all required fields with validation annotations to enforce formatting and completeness.
 * </p>
 *
 * <h2>Password Security Requirements</h2>
 * <ul>
 *   <li><strong>Length</strong>: 8-72 characters</li>
 *   <li><strong>Uppercase Letters</strong>: At least 1 uppercase letter (A-Z)</li>
 *   <li><strong>Lowercase Letters</strong>: At least 1 lowercase letter (a-z)</li>
 *   <li><strong>Numbers</strong>: At least 1 digit (0-9)</li>
 *   <li><strong>Special Characters</strong>: At least 1 special character (!@#$%^&*)</li>
 *   <li><strong>Pattern Protection</strong>: No common passwords or simple patterns</li>
 * </ul>
 *
 * <h2>Valid Password Examples</h2>
 * <ul>
 *   <li>{@code MySecureP@ssw0rd}</li>
 *   <li>{@code C0mpl3x!Password}</li>
 *   <li>{@code Str0ng&S@fe123}</li>
 * </ul>
 */
public record UserCreateDTO(

        /** Desired username for the new account. Must be 3–30 characters. */
        @Pattern(
                regexp = "^[a-z0-9]+(?:[._][a-z0-9]+)*$",
                message = "Username must be lowercase and may contain letters, numbers, periods, or underscores, without leading, trailing, or consecutive special characters"
        )
        @NotBlank(message = "Username is required")
        @Size(min = ApplicationConstants.USERNAME_MIN_LENGTH, max = ApplicationConstants.USERNAME_MAX_LENGTH)
        String username,

        /** Plaintext password for the account. Must meet security requirements. */
        @NotBlank(message = "Password is required")
        @ValidPassword
        String password,

        /** User's email address. Must be a valid email format. */
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        /** User's first name. Required, 1–50 characters. */
        @NotBlank(message = "First name is required")
        @Size(min = ApplicationConstants.NAME_MIN_LENGTH, max = ApplicationConstants.SHORT_NAME_MAX_LENGTH)
        String firstName,

        /** User's last name. Required, 1–50 characters. */
        @NotBlank(message = "Last name is required")
        @Size(min = ApplicationConstants.NAME_MIN_LENGTH, max = ApplicationConstants.SHORT_NAME_MAX_LENGTH)
        String lastName,

        /** Preferred IANA timezone ID (e.g., "America/New_York"). */
        @NotBlank(message = "Timezone is required")
        @ValidZoneId(message = "Invalid timezone provided")
        String timezone

) {}
