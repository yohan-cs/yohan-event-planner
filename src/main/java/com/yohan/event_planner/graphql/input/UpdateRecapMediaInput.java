package com.yohan.event_planner.graphql.input;

import com.yohan.event_planner.domain.enums.RecapMediaType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

/**
 * Input for updating existing recap media properties.
 * 
 * Allows modification of media URL, type, and duration for existing media items.
 * All fields are required when provided to ensure data integrity.
 * 
 * @param mediaUrl Updated URL to the media file (required if provided, must be valid URL)
 * @param mediaType Updated media type (required if provided)
 * @param durationSeconds Updated duration in seconds (optional, must be non-negative if provided)
 */
public record UpdateRecapMediaInput(

        @NotBlank(message = "Media URL is required")
        @URL(message = "Invalid URL format")
        String mediaUrl,
        @NotNull(message = "Media type is required")
        RecapMediaType mediaType,
        @Min(0)
        Integer durationSeconds
) {}
