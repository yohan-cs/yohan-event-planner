package com.yohan.event_planner.dto;

import jakarta.validation.constraints.Size;

import java.util.Optional;

/**
 * Request DTO for partially updating an existing label.
 *
 * <p>
 * Supports PATCH-style operations where only non-null fields are applied.
 * </p>
 */
public record LabelUpdateDTO(

        /** Optional new label name. */
        @Size(min = 1, max = 100, message = "Label name must be between 1 and 100 characters")
        String name
) {}
