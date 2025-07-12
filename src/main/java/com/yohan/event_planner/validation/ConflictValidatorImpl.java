package com.yohan.event_planner.validation;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.exception.ConflictException;
import com.yohan.event_planner.repository.EventRepository;
import com.yohan.event_planner.repository.RecurringEventRepository;
import com.yohan.event_planner.service.RecurrenceRuleService;
import com.yohan.event_planner.time.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Implementation of {@link ConflictValidator} providing comprehensive event scheduling conflict detection.
 *
 * <p><strong>Architectural Role:</strong> This validation component sits between the Business Object layer
 * and the Repository layer, implementing sophisticated conflict detection algorithms that account for
 * timezone differences, recurring patterns, and performance constraints.</p>
 *
 * <p><strong>Implementation Strategy:</strong></p>
 * <ul>
 *   <li><strong>Normal Events:</strong> Uses timezone-aware overlap detection against both regular events
 *       and recurring event occurrences</li>
 *   <li><strong>Recurring Events:</strong> Implements bounded-window validation (31-day maximum) to balance
 *       thoroughness with performance for complex recurrence patterns</li>
 *   <li><strong>Multi-day Events:</strong> Segments multi-day events by date and validates each segment
 *       individually for accurate conflict detection</li>
 *   <li><strong>Skip Day Management:</strong> Validates that removing skip days won't create conflicts
 *       with existing scheduled events</li>
 * </ul>
 *
 * <p><strong>Performance Optimizations:</strong></p>
 * <ul>
 *   <li>Uses database-optimized queries for initial conflict candidate filtering</li>
 *   <li>Implements early exit patterns to minimize unnecessary processing</li>
 *   <li>Caps recurrence expansion windows to prevent excessive computation</li>
 *   <li>Leverages recurrence rule day-of-week pre-filtering</li>
 * </ul>
 *
 * <p><strong>Timezone Handling:</strong> All conflict detection converts between UTC storage format
 * and user-local timezone to ensure accurate time overlap calculations.</p>
 *
 * @see ConflictValidator
 * @see ConflictException
 */
@Component
public class ConflictValidatorImpl implements ConflictValidator {

    private static final Logger logger = LoggerFactory.getLogger(ConflictValidatorImpl.class);

    private final EventRepository eventRepository;
    private final RecurringEventRepository recurringEventRepository;
    private final RecurrenceRuleService recurrenceRuleService;

    /**
     * Constructs a ConflictValidatorImpl with required dependencies.
     *
     * @param eventRepository repository for accessing regular event data
     * @param recurringEventRepository repository for accessing recurring event data
     * @param recurrenceRuleService service for expanding and evaluating recurrence rules
     */
    public ConflictValidatorImpl(
            EventRepository eventRepository,
            RecurringEventRepository recurringEventRepository,
            RecurrenceRuleService recurrenceRuleService
    ) {
        this.eventRepository = eventRepository;
        this.recurringEventRepository = recurringEventRepository;
        this.recurrenceRuleService = recurrenceRuleService;
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Implementation Details:</strong></p>
     * <ul>
     *   <li>First checks regular event conflicts using UTC time comparison</li>
     *   <li>Then validates against recurring event occurrences in user timezone</li>
     *   <li>Handles single-day events with time-filtered database queries</li>
     *   <li>Handles multi-day events with date-segmented validation</li>
     * </ul>
     */
    @Override
    public void validateNoConflicts(Event event) {
        logger.debug("Starting conflict validation for event: {} (ID: {})", event.getName(), event.getId());
        Set<Long> conflictingEventIds = new HashSet<>();

        // 1. Check normal events (stored in UTC)
        logger.debug("Checking normal event conflicts for user: {} between {} and {}", 
                event.getCreator().getId(), event.getStartTime(), event.getEndTime());
        Set<Long> normalConflicts = eventRepository.findConflictingEventIds(
                event.getCreator(),
                event.getStartTime(),
                event.getEndTime(),
                event.getId() // exclude self for updates, null for new creates
        );
        conflictingEventIds.addAll(normalConflicts);
        logger.debug("Found {} normal event conflicts", normalConflicts.size());

        // 2. Check recurring events
        EventTimezoneConversion conversion = convertToUserTimezone(event);
        logger.debug("Event in user timezone - Start: {} {}, End: {} {}", 
                conversion.startDate, conversion.startTime, conversion.endDate, conversion.endTime);

        if (conversion.startDate.equals(conversion.endDate)) {
            // Single-day event: use time-filtered DB query
            logger.debug("Processing single-day event validation");
            List<RecurringEvent> potentialRecurringConflicts = recurringEventRepository.findPotentialConflictingRecurringEvents(
                    event.getCreator().getId(),
                    conversion.startDate,
                    conversion.startTime,
                    conversion.endTime
            );

            for (RecurringEvent re : potentialRecurringConflicts) {
                List<LocalDate> occurrences = recurrenceRuleService.expandRecurrence(
                        re.getRecurrenceRule().getParsed(),
                        conversion.startDate,
                        conversion.startDate,
                        re.getSkipDays()
                );
                if (!occurrences.isEmpty()) {
                    conflictingEventIds.add(re.getId());
                }
            }

        } else {
            // Multi-day or overnight event: query by date range only, check time overlap in-memory
            logger.debug("Processing multi-day event validation from {} to {}", 
                    conversion.startDate, conversion.endDate);
            List<RecurringEvent> potentialRecurringConflicts = recurringEventRepository.findConfirmedRecurringEventsForUserBetween(
                    event.getCreator().getId(),
                    conversion.startDate,
                    conversion.endDate
            );

            for (RecurringEvent re : potentialRecurringConflicts) {
                for (LocalDate date = conversion.startDate; !date.isAfter(conversion.endDate); date = date.plusDays(1)) {

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

                        if (date.equals(conversion.startDate)) {
                            checkStart = conversion.startTime;
                            checkEnd = LocalTime.MAX;
                        } else if (date.equals(conversion.endDate)) {
                            checkStart = LocalTime.MIN;
                            checkEnd = conversion.endTime;
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
            logger.warn("Event conflict detected for '{}' (ID: {}) with {} conflicting events: {}", 
                    event.getName(), event.getId(), conflictingEventIds.size(), conflictingEventIds);
            throw new ConflictException(event, conflictingEventIds);
        }
        
        logger.info("Event validation successful for '{}' (ID: {}) - no conflicts found", 
                event.getName(), event.getId());
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Implementation Details:</strong></p>
     * <ul>
     *   <li>Uses bounded window validation (31-day maximum) for performance</li>
     *   <li>Implements special handling for infinite vs infinite recurring conflicts</li>
     *   <li>Pre-filters candidates using recurrence rule day-of-week analysis</li>
     *   <li>Excludes self-updates during validation</li>
     * </ul>
     */
    @Override
    public void validateNoConflicts(RecurringEvent recurringEvent) {
        logger.debug("Starting recurring event conflict validation for: {} (ID: {})", 
                recurringEvent.getName(), recurringEvent.getId());
        List<RecurringEvent> candidates = recurringEventRepository.findOverlappingRecurringEvents(
                recurringEvent.getCreator().getId(),
                recurringEvent.getEndTime(),
                recurringEvent.getStartTime(),
                recurringEvent.getStartDate(),
                recurringEvent.getEndDate()
        );
        logger.debug("Found {} recurring event candidates for conflict checking", candidates.size());

        Set<Long> conflictingEventIds = new HashSet<>();

        for (RecurringEvent existing : candidates) {
            // Skip self when updating
            if (recurringEvent.getId() != null && recurringEvent.getId().equals(existing.getId())) {
                continue;
            }

            if (recurringEvent.getEndDate().equals(TimeUtils.FAR_FUTURE_DATE) &&
                    existing.getEndDate().equals(TimeUtils.FAR_FUTURE_DATE)) {
                // If both events are infinite and share the same recurrence rule, they conflict
                logger.debug("Checking infinite vs infinite recurring event conflict with event ID: {}", existing.getId());
                if (hasSharedRecurrenceDays(recurringEvent, existing)) {
                    logger.debug("Infinite recurring events share recurrence days - conflict detected");
                    conflictingEventIds.add(existing.getId());
                }
                continue;
            }


            // Quick pre-check: do their recurrence rules share any days?
            if (!hasSharedRecurrenceDays(recurringEvent, existing)) {
                logger.debug("Skipping event ID {} - no shared recurrence days", existing.getId());
                continue; // no possible conflict
            }

            // Calculate overlap window
            LocalDate overlapStart = Collections.max(List.of(
                    recurringEvent.getStartDate(),
                    existing.getStartDate()
            ));
            LocalDate overlapEnd = calculateEarlierEndDate(recurringEvent, existing);

            if (overlapEnd != null && overlapEnd.isBefore(overlapStart)) {
                continue; // no date overlap
            }

            // Bound window to 31 days max
            if (overlapEnd == null || ChronoUnit.DAYS.between(overlapStart, overlapEnd) > 31) {
                logger.debug("Capping validation window to 31 days starting from {}", overlapStart);
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
            logger.warn("Recurring event conflict detected for '{}' (ID: {}) with {} conflicting events: {}", 
                    recurringEvent.getName(), recurringEvent.getId(), conflictingEventIds.size(), conflictingEventIds);
            throw new ConflictException(recurringEvent, conflictingEventIds);
        }
        
        logger.info("Recurring event validation successful for '{}' (ID: {}) - no conflicts found", 
                recurringEvent.getName(), recurringEvent.getId());
    }

    /**
     * {@inheritDoc}
     *
     * <p><strong>Implementation Details:</strong></p>
     * <ul>
     *   <li>Validates that re-enabling skip days won't create conflicts</li>
     *   <li>Uses recurrence rule occurrence checking for efficient validation</li>
     *   <li>Excludes self-updates from conflict detection</li>
     *   <li>Performs early exit on first conflict found per candidate</li>
     * </ul>
     */
    @Override
    public void validateNoConflictsForSkipDays(RecurringEvent recurringEvent, Set<LocalDate> skipDaysToRemove) {
        logger.debug("Starting skip day conflict validation for recurring event: {} (ID: {}), removing {} skip days", 
                recurringEvent.getName(), recurringEvent.getId(), skipDaysToRemove.size());
        // Fetch existing events that overlap with the recurring event's timeframe
        List<RecurringEvent> candidates = recurringEventRepository.findOverlappingRecurringEvents(
                recurringEvent.getCreator().getId(),
                recurringEvent.getEndTime(),
                recurringEvent.getStartTime(),
                recurringEvent.getStartDate(),
                recurringEvent.getEndDate()
        );
        logger.debug("Found {} candidates for skip day conflict checking", candidates.size());

        Set<Long> conflictingEventIds = new HashSet<>();

        // Iterate through all the potential conflicting events
        for (RecurringEvent existing : candidates) {
            // Skip the current event itself when updating
            if (recurringEvent.getId() != null && recurringEvent.getId().equals(existing.getId())) {
                continue;
            }

            // Quick pre-check: do their recurrence rules share any days?
            if (!hasSharedRecurrenceDays(recurringEvent, existing)) {
                logger.debug("Skipping candidate ID {} - no shared recurrence days", existing.getId());
                continue; // No conflict possible if no shared days
            }

            // For each skip day to remove, check if it conflicts with any occurrence in the existing event
            for (LocalDate skipDay : skipDaysToRemove) {
                // Check if the skip day falls within the recurrence rule of the existing event
                if (recurrenceRuleService.occursOn(existing.getRecurrenceRule().getParsed(), skipDay)) {
                    logger.debug("Skip day conflict found: {} conflicts with existing event ID {}", skipDay, existing.getId());
                    conflictingEventIds.add(existing.getId());
                    break; // Exit early if a conflict is found
                }
            }
        }

        if (!conflictingEventIds.isEmpty()) {
            logger.warn("Skip day conflict detected for recurring event '{}' (ID: {}) with {} conflicting events: {}", 
                    recurringEvent.getName(), recurringEvent.getId(), conflictingEventIds.size(), conflictingEventIds);
            throw new ConflictException(recurringEvent, conflictingEventIds);
        }
        
        logger.info("Skip day validation successful for recurring event '{}' (ID: {}) - no conflicts found", 
                recurringEvent.getName(), recurringEvent.getId());
    }

    /**
     * Determines if two time ranges overlap.
     *
     * <p>Uses inclusive overlap detection - events that touch at boundaries
     * (e.g., one ends at 2:00 PM, another starts at 2:00 PM) are considered
     * overlapping to prevent double-booking.</p>
     *
     * @param start1 start time of first time range
     * @param end1 end time of first time range
     * @param start2 start time of second time range
     * @param end2 end time of second time range
     * @return true if the time ranges overlap, false otherwise
     */
    private boolean timesOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return !start1.isAfter(end2) && !end1.isBefore(start2);
    }

    /**
     * Checks if two recurring events share any common days of the week in their recurrence patterns.
     *
     * <p>This method provides an efficient pre-filter for recurring event conflict detection.
     * If two recurring events don't share any common days of the week, they cannot possibly
     * conflict, allowing early exit from expensive conflict checking algorithms.</p>
     *
     * @param e1 first recurring event to compare
     * @param e2 second recurring event to compare
     * @return true if the events share at least one common day of the week, false otherwise
     */
    private boolean hasSharedRecurrenceDays(RecurringEvent e1, RecurringEvent e2) {
        Set<DayOfWeek> days1 = e1.getRecurrenceRule().getParsed().daysOfWeek();
        Set<DayOfWeek> days2 = e2.getRecurrenceRule().getParsed().daysOfWeek();

        for (DayOfWeek day : days1) {
            if (days2.contains(day)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates the earlier end date between two recurring events for overlap window determination.
     *
     * <p>This method is used to establish the end boundary for conflict detection windows.
     * The overlap window ends when the first event ends, as there can be no conflicts
     * beyond that point.</p>
     *
     * @param newEvent the new recurring event being validated
     * @param existingEvent the existing recurring event to check against
     * @return the earlier end date between the two events
     */
    private LocalDate calculateEarlierEndDate(RecurringEvent newEvent, RecurringEvent existingEvent) {
        // Return the earlier end date between the two events
        return newEvent.getEndDate().isBefore(existingEvent.getEndDate())
                ? newEvent.getEndDate()
                : existingEvent.getEndDate();
    }

    /**
     * Converts an event's UTC times to the creator's local timezone for conflict validation.
     *
     * <p>This conversion is essential for accurate conflict detection as events are stored
     * in UTC but need to be validated in the user's local time context to properly
     * handle timezone-specific scheduling conflicts.</p>
     *
     * @param event the event to convert to user timezone
     * @return timezone conversion object containing local date/time components
     */
    private EventTimezoneConversion convertToUserTimezone(Event event) {
        ZoneId userZone = ZoneId.of(event.getCreator().getTimezone());
        logger.debug("Converting event times to user timezone: {}", userZone);

        return new EventTimezoneConversion(
                event.getStartTime().withZoneSameInstant(userZone).toLocalDate(),
                event.getEndTime().withZoneSameInstant(userZone).toLocalDate(),
                event.getStartTime().withZoneSameInstant(userZone).toLocalTime(),
                event.getEndTime().withZoneSameInstant(userZone).toLocalTime()
        );
    }

    /**
     * Helper record to encapsulate timezone conversion results for events.
     *
     * <p>This record provides a clean way to pass timezone-converted event data
     * between methods without repetitive parameter passing.</p>
     */
    private record EventTimezoneConversion(
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime
    ) {}

}