package com.yohan.event_planner.dto;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.validation.ValidZoneId;
import jakarta.validation.constraints.Email;
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
        @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
        String username,

        /** Optional new plaintext password. Must be 8–72 characters if present. */
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password,

        /** Optional new email address. Must be valid format if present. */
        @Email(message = "Invalid email format")
        String email,

        /** Optional new first name. Must be 1–50 characters if present. */
        @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
        String firstName,

        /** Optional new last name. Must be 1–50 characters if present. */
        @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
        String lastName,

        /** Optional new time zone (IANA ID format, e.g., "America/New_York"). */
        @ValidZoneId(message = "Invalid timezone provided")
        String timezone

) {}
