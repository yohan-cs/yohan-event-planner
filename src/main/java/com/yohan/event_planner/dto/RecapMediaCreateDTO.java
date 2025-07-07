package com.yohan.event_planner.dto;


import com.yohan.event_planner.domain.enums.RecapMediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating a recap media item.
 */
public record RecapMediaCreateDTO(

        /**
         * URL of the media file.
         */
        @NotBlank(message = "Media URL is required")
        @Size(max = 512, message = "Media URL must not exceed 512 characters")
        String mediaUrl,

        /**
         * Type of the media (IMAGE or VIDEO).
         */
        @NotNull(message = "Media type is required")
        RecapMediaType mediaType,

        /**
         * Duration in seconds for videos.
         * Can be null for images.
         */
        @PositiveOrZero(message = "Duration must be zero or positive")
        Integer durationSeconds,

        /**
         * Optional explicit ordering for the media item within the recap.
         */
        @PositiveOrZero(message = "Media order must be zero or positive")
        Integer mediaOrder

) {}
