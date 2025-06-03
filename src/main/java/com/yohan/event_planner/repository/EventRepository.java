package com.yohan.event_planner.repository;

import com.yohan.event_planner.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link Event} entities.
 *
 * <p>
 * Extends {@link JpaRepository} to provide standard CRUD operations.
 * Includes custom query methods for retrieving events by creator ID,
 * filtering by date ranges, and detecting scheduling conflicts.
 * </p>
 *
 * <p>
 * All authorization and access control logic should be enforced at the service layer.
 * </p>
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Retrieves all events created by the specified user.
     *
     * @param userId the ID of the user who created the events
     * @return a list of {@link Event} entities (never null; may be empty if none found)
     */
    List<Event> findAllByCreatorId(Long userId);

    /**
     * Retrieves all events created by a user with start times within the given date-time range.
     *
     * @param userId     the ID of the event creator
     * @param startRange the inclusive lower bound of the start time
     * @param endRange   the inclusive upper bound of the start time
     * @return a list of {@link Event} entities (never null; may be empty if none found)
     */
    List<Event> findAllByCreatorIdAndStartTimeBetween(
            Long userId,
            ZonedDateTime startRange,
            ZonedDateTime endRange
    );

    /**
     * Finds the first event created by a user that overlaps with the specified time interval.
     *
     * @param userId the ID of the event creator
     * @param start  the exclusive lower bound of the interval
     * @param end    the exclusive upper bound of the interval
     * @return an {@link Optional} containing a conflicting {@link Event}, or empty if none found
     */
    Optional<Event> findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long userId,
            ZonedDateTime start,
            ZonedDateTime end
    );

    /**
     * Finds the first overlapping event created by a user, excluding the event with the given ID.
     *
     * @param userId         the ID of the event creator
     * @param start          the exclusive lower bound of the interval
     * @param end            the exclusive upper bound of the interval
     * @param excludeEventId the ID of the event to exclude from the conflict check
     * @return an {@link Optional} containing a conflicting {@link Event}, or empty if none found
     */
    Optional<Event> findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
            Long userId,
            ZonedDateTime start,
            ZonedDateTime end,
            Long excludeEventId
    );

    /**
     * Retrieves an event by its unique ID.
     *
     * @param eventId the ID of the event to retrieve
     * @return an {@link Optional} containing the event if found, or empty otherwise
     */
    @Override
    Optional<Event> findById(Long eventId);

    /**
     * Saves the provided {@link Event} entity.
     *
     * @param event the event to persist
     * @param <S>   the specific subtype of {@link Event}
     * @return the saved event
     */
    @Override
    <S extends Event> S save(S event);

    /**
     * Deletes the event with the specified ID.
     *
     * @param eventId the ID of the event to delete
     */
    @Override
    void deleteById(Long eventId);
}
