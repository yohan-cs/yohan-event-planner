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
        if (!start.isBefore(end)) {
            throw new InvalidTimeException(start, end); // Custom exception with specific details
        }
    }

    /**
     * Checks whether a proposed time range overlaps with existing events for the same creator.
     *
     * @param creatorId      the user ID of the event creator
     * @param excludeEventId (nullable) ID of the event to exclude from the conflict check (used during update)
     * @param newStart       the start time of the event being validated
     * @param newEnd         the end time of the event being validated
     * @throws ConflictException if an overlapping event is found (custom exception for better conflict handling)
     */
    private void validateNoConflicts(Long creatorId, Long excludeEventId, ZonedDateTime newStart, ZonedDateTime newEnd) {
        Optional<Event> conflict;

        if (excludeEventId == null) {
            conflict = eventRepository
                    .findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThan(creatorId, newEnd, newStart);
        } else {
            conflict = eventRepository
                    .findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                            creatorId, newEnd, newStart, excludeEventId
                    );
        }

        conflict.ifPresent(conflictingEvent -> {
            logger.warn("Conflict detected with event ID {} for user ID {}", conflictingEvent.getId(), creatorId);
            throw new ConflictException(conflictingEvent); // Custom exception for conflict detection
        });
    }

    // endregion
}
