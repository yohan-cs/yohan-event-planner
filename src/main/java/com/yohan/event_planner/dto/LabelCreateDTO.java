package com.yohan.event_planner.dto;

import com.yohan.event_planner.domain.enums.LabelColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a new label.
 *
 * <p>
 * Used when submitting a new label via {@code POST /labels}.
 * Both the label name and color are required fields.
 * </p>
 *
 */
public record LabelCreateDTO(

        /** Display name of the label. Cannot be blank. */
        @NotBlank(message = "Label name must not be blank")  
        String name,

        /** Color scheme for the label. Must be from the predefined palette. */
        @NotNull(message = "Label color cannot be null")
        LabelColor color
) {}
