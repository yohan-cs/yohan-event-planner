package com.yohan.event_planner.dto;

import com.yohan.event_planner.domain.Event;
import jakarta.validation.constraints.Size;

import java.time.ZonedDateTime;

/**
 * Request DTO for partially updating an existing {@link Event}.
 *
 * <p>
 * Used for PATCH-style operations where only non-null fields are applied.
 * Fields are individually validated if present.
 * </p>
 *
 * <p>
 * Time values must include zone information. Internally, they are normalized to UTC
 * and stored along with the original time zone ID.
 * </p>
 */
public record EventUpdateDTO(

        /** Optional new event name. Must be 1–100 characters if present. */
        @Size(min = 1, max = 100, message = "Event name must be between 1 and 100 characters")
        String name,

        /** Optional new start time with time zone. */
        ZonedDateTime startTime,

        /** Optional new end time with time zone. */
        ZonedDateTime endTime,

        /** Optional new event description. Max 500 characters if present. */
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description
) {}
