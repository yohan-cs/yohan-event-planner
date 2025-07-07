package com.yohan.event_planner.graphql.input;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

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