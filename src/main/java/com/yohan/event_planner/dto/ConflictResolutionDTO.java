package com.yohan.event_planner.dto;

import java.time.LocalDate;
import java.util.Map;

/**
 * DTO representing the user's resolution decisions for each conflicting date.
 * For each date, the user specifies the ID of the event that should skip that date.
 *
 * Example: { "2024-07-01" -> 999L, "2024-07-03" -> 101L }
 * where 999L is the new event ID (the event being created),
 * and 101L is an existing conflicting event ID.
 *
 * @param newEventId the ID of the new recurring event being created
 * @param resolutions a map of conflicting dates to the ID of the event that should skip that date
 */
public record ConflictResolutionDTO(
        Long newEventId,
        Map<LocalDate, Long> resolutions
) {
}