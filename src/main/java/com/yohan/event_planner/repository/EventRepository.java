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

    List<Event> findAllByCreatorId(Long userId);

    List<Event> findAllByCreatorIdAndStartTimeBetween(
            Long userId,
            ZonedDateTime startRange,
            ZonedDateTime endRange
    );

    Optional<Event> findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long userId,
            ZonedDateTime start,
            ZonedDateTime end
    );

    Optional<Event> findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
            Long userId,
            ZonedDateTime start,
            ZonedDateTime end,
            Long excludeEventId
    );

    Optional<Event> findFirstByCreatorIdAndEndTimeGreaterThan(
            Long userId,
            ZonedDateTime start
    );

    Optional<Event> findFirstByCreatorIdAndEndTimeGreaterThanAndIdNot(
            Long userId,
            ZonedDateTime start,
            Long excludeEventId
    );

    /**
     * Finds the first untimed event (with null endTime) that has the exact same start time.
     * Used to detect conflicts between two open-ended events.
     *
     * @param userId the ID of the event creator
     * @param start  the start time of the new event
     * @return an {@link Optional} containing a conflicting {@link Event}, or empty if none found
     */
    Optional<Event> findFirstByCreatorIdAndEndTimeIsNullAndStartTimeEquals(
            Long userId,
            ZonedDateTime start
    );

    /**
     * Same as {@link #findFirstByCreatorIdAndEndTimeIsNullAndStartTimeEquals} but excludes a given event ID.
     *
     * @param userId         the ID of the event creator
     * @param start          the start time of the new event
     * @param excludeEventId the ID of the event to exclude from the conflict check
     * @return an {@link Optional} containing a conflicting {@link Event}, or empty if none found
     */
    Optional<Event> findFirstByCreatorIdAndEndTimeIsNullAndStartTimeEqualsAndIdNot(
            Long userId,
            ZonedDateTime start,
            Long excludeEventId
    );

    @Override
    Optional<Event> findById(Long eventId);

    @Override
    <S extends Event> S save(S event);

    @Override
    void deleteById(Long eventId);
}
