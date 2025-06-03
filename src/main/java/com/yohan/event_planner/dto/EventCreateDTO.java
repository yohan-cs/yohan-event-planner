package com.yohan.event_planner.dto;


import com.yohan.event_planner.domain.Event;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.ZonedDateTime;

/**
 * Data Transfer Object for creating a new {@link Event}.
 *
 * <p>
 * This DTO provides all required fields for event creation,
 * including the name, description, start and end times, and their original timezones.
 * </p>
 *
 * <p>
 * Time values are expected to be provided as {@link ZonedDateTime} and will be
 * normalized to UTC when persisted.
 * </p>
 */
public record EventCreateDTO(

        @NotBlank(message = "Event name must not be blank")
        String name,

        String description,

        @NotNull(message = "Start time must be provided")
        ZonedDateTime startTime,

        @NotNull(message = "End time must be provided")
        ZonedDateTime endTime
) {}
