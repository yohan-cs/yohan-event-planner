package com.yohan.event_planner.dto;

import java.util.List;

/**
 * DTO representing all draft events and recurring events for a user.
 */
public record DraftsResponseDTO(
        List<EventResponseDTO> eventDrafts,
        List<RecurringEventResponseDTO> recurringEventDrafts
) {
}