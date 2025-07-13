package com.yohan.event_planner.dto;

import com.yohan.event_planner.domain.enums.LabelColor;

/**
 * Response DTO representing a fully populated view of a label.
 *
 * <p>
 * Includes label metadata with color information for visual identification.
 * </p>
 */
public record LabelResponseDTO(

        /** Unique identifier of the label. */
        Long id,

        /** Display name of the label. */
        String name,

        /** Color scheme of the label for visual identification. */
        LabelColor color,

        /** Username of the label creator. */
        String creatorUsername
) {}
