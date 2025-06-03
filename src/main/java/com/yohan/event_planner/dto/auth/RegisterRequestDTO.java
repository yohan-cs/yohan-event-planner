package com.yohan.event_planner.dto.auth;

import com.yohan.event_planner.validation.ValidZoneId;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO representing the registration request payload for creating a new user account.
 *
 * <p>
 * This class is used exclusively in the public authentication API layer (e.g., POST /auth/register).
 * It contains the essential user input fields needed for account creation, along with validation constraints
 * to ensure proper formatting and completeness of the data submitted by the client.
 * </p>
 *
 * <p>
 * While this DTO may resemble {@code UserCreateDTO}, it is intentionally separated to clearly define
 * the boundary between external request formats and internal service logic. This allows for flexibility
 * in how registration data is handled (e.g., stricter validation, additional fields like TOS agreement, etc.).
 * </p>
 */
public record RegisterRequestDTO(

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
