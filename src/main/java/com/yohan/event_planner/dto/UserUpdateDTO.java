package com.yohan.event_planner.dto;

import com.yohan.event_planner.constants.ApplicationConstants;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.validation.ValidZoneId;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for partially updating a {@link User} account.
 *
 * <p>
 * Used for PATCH-style operations where only non-null fields are applied.
 * All fields are optional and validated individually if present.
 * </p>
 */
public record UserUpdateDTO(

        /** Optional new username. Must be 3–30 characters if present. */
        @Pattern(
                regexp = "^[a-zA-Z0-9]+(?:[._][a-zA-Z0-9]+)*$",
                message = "Username may contain letters, numbers, periods, or underscores, without leading, trailing, or consecutive special characters"
        )
        @Size(min = ApplicationConstants.USERNAME_MIN_LENGTH, max = ApplicationConstants.USERNAME_MAX_LENGTH, message = "Username must be between " + ApplicationConstants.USERNAME_MIN_LENGTH + " and " + ApplicationConstants.USERNAME_MAX_LENGTH + " characters")
        String username,

        /** Optional new plaintext password. Must be 8–72 characters if present. */
        @Size(min = ApplicationConstants.PASSWORD_MIN_LENGTH, max = ApplicationConstants.PASSWORD_MAX_LENGTH, message = "Password must be between " + ApplicationConstants.PASSWORD_MIN_LENGTH + " and " + ApplicationConstants.PASSWORD_MAX_LENGTH + " characters")
        String password,

        /** Optional new email address. Must be valid format if present. */
        @Email(message = "Invalid email format")
        String email,

        /** Optional new first name. Must be 1–50 characters if present. */
        @Size(min = ApplicationConstants.NAME_MIN_LENGTH, max = ApplicationConstants.SHORT_NAME_MAX_LENGTH, message = "First name must be between " + ApplicationConstants.NAME_MIN_LENGTH + " and " + ApplicationConstants.SHORT_NAME_MAX_LENGTH + " characters")
        String firstName,

        /** Optional new last name. Must be 1–50 characters if present. */
        @Size(min = ApplicationConstants.NAME_MIN_LENGTH, max = ApplicationConstants.SHORT_NAME_MAX_LENGTH, message = "Last name must be between " + ApplicationConstants.NAME_MIN_LENGTH + " and " + ApplicationConstants.SHORT_NAME_MAX_LENGTH + " characters")
        String lastName,

        /** Optional new time zone (IANA ID format, e.g., "America/New_York"). */
        @ValidZoneId(message = "Invalid timezone provided")
        String timezone
) {}
