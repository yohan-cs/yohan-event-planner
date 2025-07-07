package com.yohan.event_planner.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record RecurringEventCreateDTO(
        String name,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        Long labelId,
        String recurrenceRule,
        Set<LocalDate> skipDays,
        boolean isDraft
) {}
