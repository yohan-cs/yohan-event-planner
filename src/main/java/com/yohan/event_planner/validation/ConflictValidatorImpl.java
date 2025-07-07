package com.yohan.event_planner.validation;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.exception.ConflictException;
import com.yohan.event_planner.repository.EventRepository;
import com.yohan.event_planner.repository.RecurringEventRepository;
import com.yohan.event_planner.service.RecurrenceRuleService;
import com.yohan.event_planner.time.TimeUtils;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ConflictValidatorImpl implements ConflictValidator {

    private final EventRepository eventRepository;
    private final RecurringEventRepository recurringEventRepository;
    private final RecurrenceRuleService recurrenceRuleService;

    public ConflictValidatorImpl(
            EventRepository eventRepository,
            RecurringEventRepository recurringEventRepository,
            RecurrenceRuleService recurrenceRuleService
    ) {
        this.eventRepository = eventRepository;
        this.recurringEventRepository = recurringEventRepository;
        this.recurrenceRuleService = recurrenceRuleService;
    }

    @Override
    public void validateNoConflicts(Event event) {
        Set<Long> conflictingEventIds = new HashSet<>();

        // 1. Check normal events (stored in UTC)
        Set<Long> normalConflicts = eventRepository.findConflictingEventIds(
                event.getCreator(),
                event.getStartTime(),
                event.getEndTime(),
                event.getId() // exclude self for updates, null for new creates
        );
        conflictingEventIds.addAll(normalConflicts);

        // 2. Check recurring events
        ZoneId userZone = ZoneId.of(event.getCreator().getTimezone());

        LocalDate eventStartDateUserZone = event.getStartTime().withZoneSameInstant(userZone).toLocalDate();
        LocalDate eventEndDateUserZone = event.getEndTime().withZoneSameInstant(userZone).toLocalDate();
        LocalTime eventStartTimeUserZone = event.getStartTime().withZoneSameInstant(userZone).toLocalTime();
        LocalTime eventEndTimeUserZone = event.getEndTime().withZoneSameInstant(userZone).toLocalTime();

        if (eventStartDateUserZone.equals(eventEndDateUserZone)) {
            // Single-day event: use time-filtered DB query
            List<RecurringEvent> potentialRecurringConflicts = recurringEventRepository.findPotentialConflictingRecurringEvents(
                    event.getCreator().getId(),
                    eventStartDateUserZone,
                    eventStartTimeUserZone,
                    eventEndTimeUserZone
            );

            for (RecurringEvent re : potentialRecurringConflicts) {
                List<LocalDate> occurrences = recurrenceRuleService.expandRecurrence(
                        re.getRecurrenceRule().getParsed(),
                        eventStartDateUserZone,
                        eventStartDateUserZone,
                        re.getSkipDays()
                );
                if (!occurrences.isEmpty()) {
                    conflictingEventIds.add(re.getId());
                }
            }

        } else {
            // Multi-day or overnight event: query by date range only, check time overlap in-memory
            List<RecurringEvent> potentialRecurringConflicts = recurringEventRepository.findConfirmedRecurringEventsForUserBetween(
                    event.getCreator().getId(),
                    eventStartDateUserZone,
                    eventEndDateUserZone
            );

            for (RecurringEvent re : potentialRecurringConflicts) {
                for (LocalDate date = eventStartDateUserZone; !date.isAfter(eventEndDateUserZone); date = date.plusDays(1)) {

                    List<LocalDate> occurrences = recurrenceRuleService.expandRecurrence(
                            re.getRecurrenceRule().getParsed(),
                            date,
                            date,
                            re.getSkipDays()
                    );

                    if (!occurrences.isEmpty()) {
                        // Determine effective time window on this date for multi-day event
                        LocalTime checkStart;
                        LocalTime checkEnd;

                        if (date.equals(eventStartDateUserZone)) {
                            checkStart = eventStartTimeUserZone;
                            checkEnd = LocalTime.MAX;
                        } else if (date.equals(eventEndDateUserZone)) {
                            checkStart = LocalTime.MIN;
                            checkEnd = eventEndTimeUserZone;
                        } else {
                            checkStart = LocalTime.MIN;
                            checkEnd = LocalTime.MAX;
                        }

                        if (timesOverlap(checkStart, checkEnd, re.getStartTime(), re.getEndTime())) {
                            conflictingEventIds.add(re.getId());
                        }
                    }
                }
            }
        }

        // 3. Throw if conflicts found
        if (!conflictingEventIds.isEmpty()) {
            throw new ConflictException(event, conflictingEventIds);
        }
    }

    @Override
    public void validateNoConflicts(RecurringEvent recurringEvent) {
        List<RecurringEvent> candidates = recurringEventRepository.findOverlappingRecurringEvents(
                recurringEvent.getCreator().getId(),
                recurringEvent.getEndTime(),
                recurringEvent.getStartTime(),
                recurringEvent.getStartDate(),
                recurringEvent.getEndDate()
        );

        Set<Long> conflictingEventIds = new HashSet<>();

        for (RecurringEvent existing : candidates) {
            // Skip self when updating
            if (recurringEvent.getId() != null && recurringEvent.getId().equals(existing.getId())) {
                continue;
            }

            if (recurringEvent.getEndDate().equals(TimeUtils.FAR_FUTURE_DATE) &&
                    existing.getEndDate().equals(TimeUtils.FAR_FUTURE_DATE)) {
                // If both events are infinite and share the same recurrence rule, they conflict
                if (haveSharedDays(recurringEvent, existing)) {
                    conflictingEventIds.add(existing.getId());
                }
                continue;
            }


            // Quick pre-check: do their recurrence rules share any days?
            if (!haveSharedDays(recurringEvent, existing)) {
                continue; // no possible conflict
            }

            // Calculate overlap window
            LocalDate overlapStart = Collections.max(List.of(
                    recurringEvent.getStartDate(),
                    existing.getStartDate()
            ));
            LocalDate overlapEnd = computeOverlapEnd(recurringEvent, existing);

            if (overlapEnd != null && overlapEnd.isBefore(overlapStart)) {
                continue; // no date overlap
            }

            // Bound window to 31 days max
            if (overlapEnd == null || ChronoUnit.DAYS.between(overlapStart, overlapEnd) > 31) {
                overlapEnd = overlapStart.plusDays(31);
            }

            // Generate recurrences within window
            List<LocalDate> newDates = recurrenceRuleService.expandRecurrence(
                    recurringEvent.getRecurrenceRule().getParsed(),
                    overlapStart,
                    overlapEnd,
                    recurringEvent.getSkipDays()
            );

            List<LocalDate> existingDates = recurrenceRuleService.expandRecurrence(
                    existing.getRecurrenceRule().getParsed(),
                    overlapStart,
                    overlapEnd,
                    existing.getSkipDays()
            );

            // Check for any overlapping dates
            Set<LocalDate> existingDateSet = new HashSet<>(existingDates);
            for (LocalDate date : newDates) {
                if (existingDateSet.contains(date)) {
                    conflictingEventIds.add(existing.getId());
                    break; // one match is sufficient per candidate
                }
            }
        }

        if (!conflictingEventIds.isEmpty()) {
            throw new ConflictException(recurringEvent, conflictingEventIds);
        }
    }

    @Override
    public void validateNoConflictsForSkipDays(RecurringEvent recurringEvent, Set<LocalDate> skipDaysToRemove) {
        // Fetch existing events that overlap with the recurring event's timeframe
        List<RecurringEvent> candidates = recurringEventRepository.findOverlappingRecurringEvents(
                recurringEvent.getCreator().getId(),
                recurringEvent.getEndTime(),
                recurringEvent.getStartTime(),
                recurringEvent.getStartDate(),
                recurringEvent.getEndDate()
        );

        Set<Long> conflictingEventIds = new HashSet<>();

        // Iterate through all the potential conflicting events
        for (RecurringEvent existing : candidates) {
            // Skip the current event itself when updating
            if (recurringEvent.getId() != null && recurringEvent.getId().equals(existing.getId())) {
                continue;
            }

            // Quick pre-check: do their recurrence rules share any days?
            if (!haveSharedDays(recurringEvent, existing)) {
                continue; // No conflict possible if no shared days
            }

            // For each skip day to remove, check if it conflicts with any occurrence in the existing event
            for (LocalDate skipDay : skipDaysToRemove) {
                // Check if the skip day falls within the recurrence rule of the existing event
                if (recurrenceRuleService.occursOn(existing.getRecurrenceRule().getParsed(), skipDay)) {
                    conflictingEventIds.add(existing.getId());
                    break; // Exit early if a conflict is found
                }
            }
        }

        if (!conflictingEventIds.isEmpty()) {
            throw new ConflictException(recurringEvent, conflictingEventIds);
        }
    }

    private boolean timesOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return !start1.isAfter(end2) && !end1.isBefore(start2);
    }

    private boolean haveSharedDays(RecurringEvent e1, RecurringEvent e2) {
        Set<DayOfWeek> days1 = e1.getRecurrenceRule().getParsed().daysOfWeek();
        Set<DayOfWeek> days2 = e2.getRecurrenceRule().getParsed().daysOfWeek();

        for (DayOfWeek day : days1) {
            if (days2.contains(day)) {
                return true;
            }
        }
        return false;
    }

    private LocalDate computeOverlapEnd(RecurringEvent newEvent, RecurringEvent existingEvent) {
        // Return the earlier end date between the two events
        return newEvent.getEndDate().isBefore(existingEvent.getEndDate())
                ? newEvent.getEndDate()
                : existingEvent.getEndDate();
    }

}