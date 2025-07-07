package com.yohan.event_planner.dto;

import java.util.List;

public record MyEventsResponseDTO(
        List<RecurringEventResponseDTO> recurringEvents,
        List<EventResponseDTO> events
) {}
