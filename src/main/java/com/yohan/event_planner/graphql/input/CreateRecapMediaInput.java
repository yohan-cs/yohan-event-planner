package com.yohan.event_planner.graphql.input;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

/**
 * Input for creating media items within event recaps.
 * 
 * Media items can be images, videos, or audio files that document an event.
 * They support duration tracking for video/audio content and custom ordering.
 * 
 * @param mediaUrl URL to the media file (required, must be valid URL)
 * @param mediaType Type of media content (required)
 * @param durationSeconds Duration in seconds for video/audio (optional, must be non-negative)
 * @param mediaOrder Display order within the recap (optional)
 */
public record CreateRecapMediaInput(

        @NotBlank(message = "Media URL is required")
        @URL(message = "Invalid URL format")
        String mediaUrl,

        @NotNull(message = "Media type is required")
        String mediaType,

        @Min(value = 0, message = "Duration must be non-negative")
        Integer durationSeconds,

        Integer mediaOrder
) {}