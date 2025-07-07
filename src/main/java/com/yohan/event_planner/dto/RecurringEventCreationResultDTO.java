package com.yohan.event_planner.dto;

import java.time.LocalDate;
import java.util.SortedMap;
import java.util.Set;

public record RecurringEventCreationResultDTO(
        RecurringEventResponseDTO savedEvent,
        SortedMap<LocalDate, Set<Long>> conflictingDatesToEvent
) {}
