package com.yohan.event_planner.dto;

import com.yohan.event_planner.domain.Event;
import jakarta.validation.constraints.Size;

import java.time.ZonedDateTime;

/**
 * Data Transfer Object for partially updating {@link Event} entities.
 *
 * <p>
 * Designed for PATCH-style updates, this DTO allows partial modification of event details.
 * Only non-null fields will be considered during patch operations.
 * </p>
 *
 * <p>
 * Validation constraints (applied only if fields are present):
 * <ul>
 *   <li><b>{@code name}</b>: 1–100 characters</li>
 *   <li><b>{@code description}</b>: max 500 characters</li>
 * </ul>
 * </p>
 *
 * <p>
 * Start and end times must be provided as {@link java.time.ZonedDateTime} with timezone info.
 * Internal storage will normalize to UTC and persist original timezones separately.
 * </p>
 *
 * @param name        optional new event name
 * @param startTime   optional new start time (with timezone)
 * @param endTime     optional new end time (with timezone)
 * @param description optional new event description
 */
public record EventUpdateDTO(

        @Size(min = 1, max = 100, message = "Event name must be between 1 and 100 characters")
        String name,

        ZonedDateTime startTime,

        ZonedDateTime endTime,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description
) {
}
