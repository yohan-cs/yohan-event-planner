package com.yohan.event_planner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a new label.
 *
 * <p>
 * Used when submitting a new label via {@code POST /labels}.
 * The label name must not be blank.
 * </p>
 *
 */
public record LabelCreateDTO(

        /** Display name of the label. Cannot be blank. */
        @NotBlank(message = "Label name must not be blank")
        String name
) {}
