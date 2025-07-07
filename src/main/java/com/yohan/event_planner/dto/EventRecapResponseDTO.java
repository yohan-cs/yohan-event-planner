package com.yohan.event_planner.dto;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Response DTO representing a user's recap for a completed event.
 *
 * <p>
 * Recap content includes event details, the creator's username,
 * optional freeform notes, and associated media items.
 * </p>
 */
public record EventRecapResponseDTO(

        /**
         * Unique ID of the recap.
         */
        Long id,

        /**
         * Name of the event this recap belongs to.
         */
        String eventName,

        /**
         * Username of the user who created the recap.
         */
        String username,

        /**
         * Start time of the event in the viewer's local timezone.
         */
        ZonedDateTime date,

        /**
         * Duration of the event in minutes.
         */
        int durationMinutes,

        /**
         * Name of the label (e.g. "Jiu Jitsu", "Work").
         * May be null if the event is unlabeled.
         */
        String labelName,

        /**
         * Freeform recap notes.
         * Can be null for muted recaps.
         */
        String notes,

        /**
         * List of media items associated with this recap.
         * Ordered as stored in the database.
         */
        List<RecapMediaResponseDTO> media,

        /**
         * Whether this recap is unconfirmed (draft).
         */
        boolean unconfirmed
) {}
