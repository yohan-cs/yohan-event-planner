package com.yohan.event_planner.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record RecurringEventResponseDTO(
        Long id,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        LabelResponseDTO label,
        String recurrenceSummary,
        Set<LocalDate> skipDays,
        String creatorUsername,
        boolean unconfirmed
) {}
