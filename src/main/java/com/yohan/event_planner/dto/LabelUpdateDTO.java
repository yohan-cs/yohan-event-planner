package com.yohan.event_planner.dto;

import com.yohan.event_planner.domain.enums.LabelColor;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for partially updating an existing label.
 *
 * <p>
 * Note: Fields are optional in PATCH operations. Only include fields you want to update.
 * When name is provided, it must be valid. When color is provided, it must be from the 
 * predefined palette.
 * </p>
 */
public record LabelUpdateDTO(

        /** Label name. When provided, must be between 1-100 characters. */
        @Size(min = 1, max = 100, message = "Label name must be between 1 and 100 characters")
        String name,

        /** Label color. When provided, must be from the predefined palette. */
        LabelColor color
) {}
