package com.yohan.event_planner.dto;

import com.yohan.event_planner.constants.ApplicationConstants;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request DTO for updating an existing event recap.
 *
 * <p>
 * The event ID is inferred from the request path. This DTO carries the new notes value
 * and optionally an updated list of media items.
 * </p>
 */
public record EventRecapUpdateDTO(

        /**
         * Updated recap content.
         */
        @Size(max = ApplicationConstants.RECAP_NOTES_MAX_LENGTH, message = "Recap notes must not exceed " + ApplicationConstants.RECAP_NOTES_MAX_LENGTH + " characters")
        String notes,

        /**
         * Updated list of media items.
         * If provided, replaces the existing media list.
         * Null means no change to current media.
         */
        @Valid
        List<RecapMediaCreateDTO> media
) {}
