package com.yohan.event_planner.validation;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.exception.ConflictException;
import com.yohan.event_planner.exception.InvalidTimeException;
import com.yohan.event_planner.repository.EventRepository;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Component responsible for validating business rules related to {@link Event} creation and updates.
 * <p>
 * Performs temporal validations such as ensuring start times precede end times, and
 * enforces conflict detection against existing events by the same creator.
 * <p>
 * Assumes that basic input validations (e.g., non-null constraints) are handled
 * by DTO or controller layers before invocation.
 * </p>
 */
@Component
public class EventValidator {

    private final EventRepository eventRepository;

    /**
     * Constructs an {@code EventValidator} with the given {@link EventRepository}.
     *
     * @param eventRepository the repository used to query existing events for conflicts
     */
    public EventValidator(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Validates that the provided start time occurs strictly before the end time.
     *
     * @param start the proposed event start time, must not be null
     * @param end   the proposed event end time, must not be null
     * @throws InvalidTimeException if {@code start} is equal to or after {@code end}
     */
    public void validateStartBeforeEnd(ZonedDateTime start, ZonedDateTime end) {
        if (!start.isBefore(end)) {
            throw new InvalidTimeException(start, end);
        }
    }

    /**
     * Validates that a new or updated event does not conflict with existing events
     * created by the same user.
     * <p>
     * A conflict occurs if any existing event's time range overlaps with the
     * proposed new event's time range.
     * </p>
     *
     * @param creatorId      the ID of the user who owns the event, must not be null
     * @param excludeEventId the ID of the event to exclude from conflict check (useful for updates), may be {@code null}
     * @param newStart       the proposed event start time, must not be null
     * @param newEnd         the proposed event end time, must not be null
     * @throws ConflictException if an overlapping event is found in the repository
     */
    public void validateNoConflicts(Long creatorId, Long excludeEventId, ZonedDateTime newStart, ZonedDateTime newEnd) {
        Optional<Event> conflictingEvent;

        if (excludeEventId == null) {
            conflictingEvent = eventRepository
                    .findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThan(creatorId, newEnd, newStart);
        } else {
            conflictingEvent = eventRepository
                    .findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(creatorId, newEnd, newStart, excludeEventId);
        }

        if (conflictingEvent.isPresent()) {
            throw new ConflictException(conflictingEvent.get());
        }
    }
}
