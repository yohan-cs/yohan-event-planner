package com.yohan.event_planner.validation;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.RecurringEvent;

import java.time.LocalDate;
import java.util.Set;

public interface ConflictValidator {

    void validateNoConflicts(Event event);

    void validateNoConflicts(RecurringEvent recurringEvent);

    void validateNoConflictsForSkipDays(RecurringEvent recurringEvent, Set<LocalDate> skipDaysToRemove);
}
