package com.yohan.event_planner.dto;

import com.yohan.event_planner.domain.Event;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;

/**
 * Request DTO for creating a new {@link Event}.
 *
 * <p>
 * Used in the event creation API (e.g., {@code POST /events}) to submit new event details.
 * All fields are required except for the optional description.
 * </p>
 *
 * <p>
 * Time values must include a time zone offset (i.e., be fully formed {@link ZonedDateTime} instances).
 * When persisted, they are normalized to UTC while preserving the original time zone IDs separately.
 * </p>
 */
public record EventCreateDTO(

        /** Name or title of the event. Cannot be blank. */
        @NotBlank(message = "Event name must not be blank")
        String name,

        /** Start time of the event, including time zone. Cannot be {@code null}. */
        @NotNull(message = "Start time must be provided")
        ZonedDateTime startTime,

        /** End time of the event, including time zone. Cannot be {@code null}. */
        @NotNull(message = "End time must be provided")
        ZonedDateTime endTime,

        /** Optional event description. May be {@code null}. */
        String description
) {}
