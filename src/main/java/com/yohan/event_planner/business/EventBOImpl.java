package com.yohan.event_planner.business;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.exception.ConflictException;
import com.yohan.event_planner.exception.InvalidTimeException;
import com.yohan.event_planner.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Concrete implementation of the {@link EventBO} interface.
 *
 * <p>
 * Provides business logic and coordination for managing {@link Event} entities.
 * This implementation delegates persistence operations to the {@link EventRepository},
 * and is responsible for enforcing domain rules such as time validation and conflict detection.
 * </p>
 *
 * <p>
 * Assumes all authorization and ownership checks are handled upstream
 * in the service layer. This class is not defensive and expects well-formed inputs.
 * </p>
 */
@Service
public class EventBOImpl implements EventBO {

    private static final Logger logger = LoggerFactory.getLogger(EventBOImpl.class);
    private final EventRepository eventRepository;

    public EventBOImpl(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Event> getEventById(Long eventId) {
        logger.debug("Fetching event by ID {}", eventId);
        return eventRepository.findById(eventId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Event> getEventsByUser(Long userId) {
        logger.debug("Fetching events for user ID {}", userId);
        return eventRepository.findAllByCreatorId(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Event> getEventsByUserAndDateRange(Long userId, ZonedDateTime start, ZonedDateTime end) {
        logger.debug("Fetching events for user ID {} between {} and {}", userId, start, end);
        return eventRepository.findAllByCreatorIdAndStartTimeBetween(userId, start, end);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Event createEvent(Event event) {
        logger.info("Creating new event '{}'", event.getName());

        validateEventTimes(event);
        checkForConflicts(event);

        Event saved = eventRepository.save(event);
        logger.info("Event created with ID {}", saved.getId());
        return saved;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Event updateEvent(Event event) {
        logger.info("Updating event ID {}", event.getId());

        validateEventTimes(event);
        checkForConflicts(event);

        return eventRepository.save(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteEvent(Long eventId) {
        logger.info("Deleting event ID {}", eventId);
        eventRepository.deleteById(eventId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateEventTimes(Event event) {
        validateStartBeforeEnd(event.getStartTime(), event.getEndTime());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkForConflicts(Event event) {
        Long eventId = event.getId(); // nullable for new events
        validateNoConflicts(event.getCreator().getId(), eventId, event.getStartTime(), event.getEndTime());
    }

    // region: Private Validation Methods

    /**
     * Ensures that a given start time occurs strictly before the end time.
     *
     * @param start the proposed start time (must not be null)
     * @param end   the proposed end time (must not be null)
     * @throws InvalidTimeException if {@code start} is equal to or after {@code end}
     */
    private void validateStartBeforeEnd(ZonedDateTime start, ZonedDateTime end) {
        if (end == null) return;
        if (!start.isBefore(end)) {
            throw new InvalidTimeException(start, end);
        }
    }

    /**
     * Checks whether a proposed time range overlaps with existing events for the same creator.
     * <p>
     * If the event has an end time, it checks for standard overlaps.
     * If the event has no end time (i.e., open-ended), it checks whether the start time overlaps
     * with the end of any other events.
     *
     * @param creatorId      the user ID of the event creator
     * @param excludeEventId (nullable) ID of the event to exclude from the conflict check (used during update)
     * @param newStart       the start time of the event being validated (must not be null)
     * @param newEnd         the end time of the event being validated (may be null for open-ended events)
     * @throws ConflictException if an overlapping event is found
     */
    private void validateNoConflicts(Long creatorId, Long excludeEventId, ZonedDateTime newStart, ZonedDateTime newEnd) {
        Optional<Event> conflict = findConflict(creatorId, excludeEventId, newStart, newEnd);

        conflict.ifPresent(conflictingEvent -> {
            logger.warn("Conflict detected with event ID {} for user ID {}", conflictingEvent.getId(), creatorId);
            throw new ConflictException(conflictingEvent);
        });
    }

    /**
     * Searches for a conflicting event by the same creator.
     *
     * <p>
     * Conflict rules:
     * <ul>
     *     <li><b>Timed Event:</b> Conflicts with any existing event that overlaps the given time range
     *         (<code>startTime &lt; newEnd</code> AND <code>endTime &gt; newStart</code>).</li>
     *     <li><b>Untimed Event:</b> Can conflict with:
     *         <ul>
     *             <li>A timed event that ends after <code>newStart</code>.</li>
     *             <li>Another untimed event that starts at the exact same <code>newStart</code>.</li>
     *         </ul>
     *     </li>
     * </ul>
     * If <code>excludeEventId</code> is provided, that event will be ignored (used for updates).
     * </p>
     *
     * @param creatorId      ID of the event's creator
     * @param excludeEventId ID of the event to exclude from conflict checking (can be null)
     * @param newStart       Proposed start time of the event
     * @param newEnd         Proposed end time (null for untimed events)
     * @return Optional containing the first conflicting event found, if any
     */
    private Optional<Event> findConflict(Long creatorId, Long excludeEventId, ZonedDateTime newStart, ZonedDateTime newEnd) {
        if (newEnd != null) {
            // Timed event: check for overlapping timed events
            return excludeEventId == null
                    ? eventRepository.findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThan(
                    creatorId, newEnd, newStart)
                    : eventRepository.findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                    creatorId, newEnd, newStart, excludeEventId);
        } else {
            // Untimed event: check for timed conflicts first
            Optional<Event> conflict = excludeEventId == null
                    ? eventRepository.findFirstByCreatorIdAndEndTimeGreaterThan(creatorId, newStart)
                    : eventRepository.findFirstByCreatorIdAndEndTimeGreaterThanAndIdNot(creatorId, newStart, excludeEventId);

            if (conflict.isPresent()) {
                return conflict;
            }

            // Then check for untimed conflicts with same start time
            return excludeEventId == null
                    ? eventRepository.findFirstByCreatorIdAndEndTimeIsNullAndStartTimeEquals(creatorId, newStart)
                    : eventRepository.findFirstByCreatorIdAndEndTimeIsNullAndStartTimeEqualsAndIdNot(creatorId, newStart, excludeEventId);
        }
    }

}
