package com.yohan.event_planner.dto;

/**
 * Response DTO representing a fully populated view of a label.
 *
 * <p>
 * Includes label metadata along.
 * </p>
 */
public record LabelResponseDTO(

        /** Unique identifier of the label. */
        Long id,

        /** Display name of the label. */
        String name,

        /** Username of the label creator. */
        String creatorUsername
) {}
