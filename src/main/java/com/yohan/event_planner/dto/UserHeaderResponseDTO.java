package com.yohan.event_planner.dto;

/**
 * Response DTO for returning public-facing profile information on the /username page.
 *
 * <p>This DTO is returned by both profile fetch and update operations,
 * and exposes only non-sensitive, frontend-relevant fields.</p>
 */
public record UserHeaderResponseDTO(

        /** Public username used for profile URLs and display. */
        String username,

        /** User's first name. */
        String firstName,

        /** User's last name. */
        String lastName,

        /** Optional bio or tagline provided by the user. */
        String bio,

        /** URL to the user's profile picture (nullable or placeholder). */
        String profilePictureUrl
) {}
