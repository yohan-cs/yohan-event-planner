package com.yohan.event_planner.dto;


import com.yohan.event_planner.domain.User;

/**
 * Response Data Transfer Object for exposing public-facing {@link User} information.
 *
 * <p>
 * Returned to clients after user creation, update, or retrieval.
 * Includes only non-sensitive fields and omits internal or security-related data.
 * </p>
 *
 * <p><strong>Included fields:</strong></p>
 * <ul>
 *   <li><code>firstName</code>: user's first name</li>
 *   <li><code>lastName</code>: user's last name</li>
 *   <li><code>username</code>: public-facing username</li>
 *   <li><code>email</code>: user's email address</li>
 *   <li><code>timezone</code>: preferred timezone (e.g., "America/New_York")</li>
 * </ul>
 */
public record UserResponseDTO(
        String firstName,
        String lastName,
        String username,
        String email,
        String timezone
) {}

