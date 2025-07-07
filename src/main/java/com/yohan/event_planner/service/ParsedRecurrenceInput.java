package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.enums.RecurrenceFrequency;

import java.time.DayOfWeek;
import java.util.Objects;
import java.util.Set;

public record ParsedRecurrenceInput(
        RecurrenceFrequency frequency,
        Set<DayOfWeek> daysOfWeek,
        Integer ordinal
) {
    public ParsedRecurrenceInput {
        Objects.requireNonNull(frequency, "frequency must not be null");
    }
}