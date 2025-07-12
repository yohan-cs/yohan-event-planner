package com.yohan.event_planner.validation;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.exception.ConflictException;

import java.time.LocalDate;
import java.util.Set;

/**
 * Validation component responsible for detecting scheduling conflicts between events.
 *
 * <p><strong>Architectural Role:</strong> This component sits in the validation layer
 * and is injected into Business Object (BO) implementations to enforce scheduling
 * business rules. It ensures that new or updated events do not conflict with existing
 * events in terms of time overlap.</p>
 *
 * <p><strong>Conflict Detection Strategy:</strong></p>
 * <ul>
 *   <li><strong>Normal Events:</strong> Checks both regular events and recurring event occurrences
 *       for time overlap using timezone-aware calculations</li>
 *   <li><strong>Recurring Events:</strong> Validates recurrence patterns against existing
 *       recurring events within bounded time windows for performance</li>
 *   <li><strong>Skip Day Management:</strong> Ensures removing skip days doesn't create
 *       conflicts with existing event occurrences</li>
 * </ul>
 *
 * <p><strong>Timezone Handling:</strong> All validations account for user timezones,
 * converting between UTC storage format and user-local time for accurate conflict detection.</p>
 *
 * <p><strong>Performance Considerations:</strong> Uses bounded time windows (typically 31 days)
 * for recurring event validation to prevent excessive computation on infinite or very long
 * recurrence patterns.</p>
 *
 * @see ConflictException
 * @see Event
 * @see RecurringEvent
 */
public interface ConflictValidator {

    /**
     * Validates that the given event does not conflict with any existing events.
     *
     * <p>Performs comprehensive conflict detection including:</p>
     * <ul>
     *   <li>Normal event time overlap checking</li>
     *   <li>Recurring event occurrence validation</li>
     *   <li>Multi-day and overnight event handling</li>
     *   <li>Timezone-aware time calculations</li>
     * </ul>
     *
     * <p>For multi-day events, validates each day segment individually to ensure
     * accurate conflict detection across date boundaries.</p>
     *
     * @param event the event to validate for conflicts, must not be null
     * @throws ConflictException if the event conflicts with existing events
     * @throws IllegalArgumentException if event is null or has invalid time data
     */
    void validateNoConflicts(Event event);

    /**
     * Validates that the given recurring event does not conflict with existing recurring events.
     *
     * <p>Performs sophisticated recurring event conflict detection including:</p>
     * <ul>
     *   <li>Recurrence rule overlap analysis</li>
     *   <li>Infinite vs finite event conflict handling</li>
     *   <li>Bounded time window validation for performance</li>
     *   <li>Self-exclusion during updates</li>
     * </ul>
     *
     * <p>Uses a 31-day maximum window for conflict detection to balance thoroughness
     * with performance, especially for infinite recurring events.</p>
     *
     * @param recurringEvent the recurring event to validate, must not be null
     * @throws ConflictException if the recurring event conflicts with existing events
     * @throws IllegalArgumentException if recurringEvent is null or has invalid recurrence data
     */
    void validateNoConflicts(RecurringEvent recurringEvent);

    /**
     * Validates that removing the specified skip days from a recurring event
     * would not create conflicts with existing events.
     *
     * <p>This method is used when users want to remove skip days from a recurring
     * event, effectively re-enabling those dates. It ensures that re-enabling
     * these dates won't cause the recurring event to conflict with other events
     * that may have been scheduled during the skipped periods.</p>
     *
     * <p>Validation process:</p>
     * <ul>
     *   <li>Identifies overlapping recurring events</li>
     *   <li>Checks if any skip day to remove conflicts with existing occurrences</li>
     *   <li>Applies recurrence rule matching for conflict detection</li>
     *   <li>Excludes self-updates from conflict checking</li>
     * </ul>
     *
     * @param recurringEvent the recurring event being modified, must not be null
     * @param skipDaysToRemove the set of skip days to be removed (re-enabled), must not be null or empty
     * @throws ConflictException if removing any skip day would create conflicts
     * @throws IllegalArgumentException if parameters are null or skipDaysToRemove is empty
     */
    void validateNoConflictsForSkipDays(RecurringEvent recurringEvent, Set<LocalDate> skipDaysToRemove);
}
