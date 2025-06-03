package com.yohan.event_planner.dto;

import com.yohan.event_planner.domain.Event;

import java.time.ZonedDateTime;

/**
 * Response DTO for exposing public-facing {@link Event} information.
 *
 * <p>
 * This DTO is returned to clients and includes all relevant event fields such as:
 * </p>
 * <ul>
 *   <li>{@code id} – unique event identifier</li>
 *   <li>{@code name} – event name</li>
 *   <li>{@code startTime}, {@code endTime} – event times in original timezones</li>
 *   <li>{@code startTimezone}, {@code endTimezone} – original timezone IDs</li>
 *   <li>{@code description} – optional event description</li>
 * </ul>
 *
 * <p>
 * Timestamps are returned in the original zone (not UTC) for client display.
 * </p>
 *
 * @param id             unique identifier of the event
 * @param name           name of the event
 * @param startTime      event start time with original timezone
 * @param endTime        event end time with original timezone
 * @param startTimezone  ID of the start time's original timezone
 * @param endTimezone    ID of the end time's original timezone
 * @param description    optional event description
 */
public record EventResponseDTO(
        Long id,
        String name,
        ZonedDateTime startTime,
        ZonedDateTime endTime,
        String startTimezone,
        String endTimezone,
        String description
) {}
