package com.yohan.event_planner.dto;

import com.yohan.event_planner.constants.ApplicationConstants;
import com.yohan.event_planner.domain.Event;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for creating a recap for a completed {@link Event}.
 *
 * <p>
 * Recaps can only be created for events that are marked as {@code isCompleted == true}.
 * Only one recap is allowed per event.
 * </p>
 *
 * <p>
 * The {@code isUnconfirmed} flag determines whether the recap is saved as a draft
 * or as a confirmed post. If {@code isUnconfirmed = true}, the recap is saved as a draft,
 * regardless of field completeness.
 * </p>
 */
public record EventRecapCreateDTO(

        /**
         * The ID of the completed event to attach the recap to.
         */
        @NotNull
        Long eventId,

        /**
         * The user’s written recap or notes.
         */
        @Size(max = ApplicationConstants.RECAP_NOTES_MAX_LENGTH, message = "Recap notes must not exceed " + ApplicationConstants.RECAP_NOTES_MAX_LENGTH + " characters")
        String notes,

        /**
         * Optional title to show on the recap instead of the event name.
         */
        @Size(max = 100, message = "Recap name must not exceed 100 characters")
        String recapName,

        /**
         * Whether to save the recap as a draft instead of publishing it immediately.
         */
        boolean isUnconfirmed,

        @Valid
        List<RecapMediaCreateDTO> media
) {}