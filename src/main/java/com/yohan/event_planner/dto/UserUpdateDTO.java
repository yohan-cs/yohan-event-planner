package com.yohan.event_planner.dto;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.validation.ValidZoneId;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for partially updating a {@link User}.
 *
 * <p>
 * Designed for PATCH-style updates, this DTO allows clients to supply only the fields
 * they intend to change. All fields are optional, and validation is applied individually if present.
 * </p>
 *
 * <p><strong>Field validation (if present):</strong></p>
 * <ul>
 *   <li><code>username</code>: 3–30 characters</li>
 *   <li><code>password</code>: 8–72 characters (raw, will be hashed)</li>
 *   <li><code>email</code>: must be a valid format</li>
 *   <li><code>firstName</code>: 1–50 characters</li>
 *   <li><code>lastName</code>: 1–50 characters</li>
 *   <li><code>timezone</code>: must be a valid IANA Zone ID ({@link ValidZoneId})</li>
 * </ul>
 *
 * <p><strong>Example usage:</strong></p>
 * <pre>{@code
 * // Change username and timezone only
 * UserUpdateDTO patch = new UserUpdateDTO("newName", null, null, null, null, "America/New_York");
 * }</pre>
 *
 * @param username  new username (optional)
 * @param password  new plain-text password (optional)
 * @param email     new email address (optional)
 * @param firstName new first name (optional)
 * @param lastName  new last name (optional)
 * @param timezone  new timezone ID (optional)
 */
public record UserUpdateDTO(

        @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
        String username,

        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password,

        @Email(message = "Invalid email format")
        String email,

        @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
        String firstName,

        @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
        String lastName,

        @ValidZoneId(message = "Invalid timezone provided")
        String timezone
) {}