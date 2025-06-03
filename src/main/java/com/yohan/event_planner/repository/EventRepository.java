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
 * </p>
 *
 * <p>
 * Custom query methods enable retrieval of events by creator ID,
 * filtering by date ranges, and conflict detection within time intervals.
 * </p>
 *
 * <p>
 * Note that authorization and access control should be enforced at the service layer.
 * </p>
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Retrieves all events created by a specific user.
     *
     * @param userId the ID of the user who created the events
     * @return a list of {@link Event} entities created by the given user
     */
    List<Event> findAllByCreatorId(Long userId);

    /**
     * Retrieves all events created by a specific user with start times between the given range.
     *
     * @param userId     the ID of the user who created the events
     * @param startRange inclusive start of the date-time range
     * @param endRange   inclusive end of the date-time range
     * @return a list of {@link Event} entities matching the criteria
     */
    List<Event> findAllByCreatorIdAndStartTimeBetween(
            Long userId,
            ZonedDateTime startRange,
            ZonedDateTime endRange
    );

    /**
     * Finds the first event created by a user that overlaps with a given time interval.
     *
     * @param userId the ID of the event creator
     * @param start  exclusive lower bound of the interval
     * @param end    exclusive upper bound of the interval
     * @return an {@link Optional} containing a conflicting {@link Event} if found
     */
    Optional<Event> findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long userId,
            ZonedDateTime start,
            ZonedDateTime end
    );

    /**
     * Finds the first event created by a user that overlaps with a given time interval,
     * excluding a specific event ID.
     *
     * @param userId         the ID of the event creator
     * @param start          exclusive lower bound of the interval
     * @param end            exclusive upper bound of the interval
     * @param excludeEventId the ID of the event to exclude from the check
     * @return an {@link Optional} containing a conflicting {@link Event} if found
     */
    Optional<Event> findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
            Long userId,
            ZonedDateTime start,
            ZonedDateTime end,
            Long excludeEventId
    );

    @Override
    Optional<Event> findById(Long eventId);

    @Override
    <S extends Event> S save(S event);

    @Override
    void deleteById(Long eventId);
}
