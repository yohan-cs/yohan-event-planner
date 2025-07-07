package com.yohan.event_planner.graphql.input;

import com.yohan.event_planner.domain.enums.RecapMediaType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

public record UpdateRecapMediaInput(

        @NotBlank(message = "Media URL is required")
        @URL(message = "Invalid URL format")
        String mediaUrl,
        @NotNull(message = "Media type is required")
        RecapMediaType mediaType,
        @Min(0)
        Integer durationSeconds
) {}
