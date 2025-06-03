package com.yohan.event_planner.dto;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.validation.ValidZoneId;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


/**
 * Data Transfer Object for creating a new {@link User}.
 *
 * <p>
 * Includes all required fields with validation annotations
 * to enforce input constraints before persistence.
 * </p>
 *
 * <p><strong>Validation rules:</strong></p>
 * <ul>
 *   <li><code>username</code>: required, 3–30 characters</li>
 *   <li><code>password</code>: required, 8–72 characters</li>
 *   <li><code>email</code>: required, must be valid email format</li>
 *   <li><code>firstName</code>: required, 1–50 characters</li>
 *   <li><code>lastName</code>: required, 1–50 characters</li>
 *   <li><code>timezone</code>: required, must be a valid Zone ID ({@link ValidZoneId})</li>
 * </ul>
 *
 * @param username  Desired username for the account
 * @param password  Plain-text password (hashed during processing)
 * @param email     User's email address
 * @param firstName User's first name
 * @param lastName  User's last name
 * @param timezone  User's IANA timezone ID (e.g., "America/New_York")
 */
public record UserCreateDTO(

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 30)
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72)
        String password,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "First name is required")
        @Size(min = 1, max = 50)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 1, max = 50)
        String lastName,

        @NotBlank(message = "Timezone is required")
        @ValidZoneId(message = "Invalid timezone provided")
        String timezone
) {}
