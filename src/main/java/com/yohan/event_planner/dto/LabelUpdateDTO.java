package com.yohan.event_planner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Optional;

/**
 * Request DTO for partially updating an existing label.
 *
 * <p>
 * Note: This DTO requires the name field to be present and valid when included in the request.
 * For true optional behavior in PATCH operations, omit the field entirely from the JSON.
 * </p>
 */
public record LabelUpdateDTO(

        /** Label name. When provided, must be non-blank and between 1-100 characters. */
        @NotBlank(message = "Label name must not be blank when provided")
        @Size(min = 1, max = 100, message = "Label name must be between 1 and 100 characters")
        String name
) {}
