package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.RecurringEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface RecurrenceRuleService {

    ParsedRecurrenceInput parseFromString(String rule);

    List<LocalDate> expandRecurrence(ParsedRecurrenceInput input, LocalDate startInclusive, LocalDate endInclusive, Set<LocalDate> skipDays);

    String buildSummary(ParsedRecurrenceInput parsedRecurrence, LocalDate startDate, LocalDate endDate);

    boolean occursOn(ParsedRecurrenceInput parsed, LocalDate date);
}
