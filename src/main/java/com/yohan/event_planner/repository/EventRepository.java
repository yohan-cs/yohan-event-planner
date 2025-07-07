package com.yohan.event_planner.repository;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    @Query("SELECT e FROM Event e WHERE e.creator.id = :userId AND e.unconfirmed = true ORDER BY e.startTime DESC")
    List<Event> findUnconfirmedEventsForUserSortedByStartTime(@Param("userId") Long userId);

    @Query("""
    SELECT e FROM Event e
    WHERE e.creator.id = :userId
      AND e.unconfirmed = false
      AND e.endTime > :windowStart
      AND e.startTime < :windowEnd
""")
    List<Event> findConfirmedEventsForUserBetween(
            @Param("userId") Long userId,
            @Param("windowStart") ZonedDateTime windowStart,
            @Param("windowEnd") ZonedDateTime windowEnd
    );

    @Query("""
    SELECT e FROM Event e
    WHERE e.creator.id = :userId
      AND e.endTime > :start
      AND e.startTime < :end
      AND (
           e.unconfirmed = false
        OR (e.unconfirmed = true AND e.recurringEvent.id = :recurrenceId)
      )
""")
    List<Event> findConfirmedAndRecurringDraftsByUserAndRecurrenceIdBetween(
            @Param("userId") Long userId,
            @Param("recurrenceId") Long recurrenceId,
            @Param("start") ZonedDateTime start,
            @Param("end") ZonedDateTime end
    );

    @Query("""
    SELECT e FROM Event e
    WHERE e.creator.id = :userId
    AND e.unconfirmed = false
    ORDER BY e.endTime DESC, e.startTime DESC, e.id DESC
""")
    List<Event> findTopConfirmedByUserIdOrderByEndTimeDescStartTimeDescIdDesc(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
    SELECT e FROM Event e
    WHERE e.creator.id = :userId
    AND e.unconfirmed = false
    AND (
        e.endTime < :endTimeCursor
        OR (e.endTime = :endTimeCursor AND e.startTime < :startTimeCursor)
        OR (e.endTime = :endTimeCursor AND e.startTime = :startTimeCursor AND e.id < :idCursor)
    )
    ORDER BY e.endTime DESC, e.startTime DESC, e.id DESC
""")
    List<Event> findConfirmedByUserIdBeforeCursor(
            @Param("userId") Long userId,
            @Param("endTimeCursor") ZonedDateTime endTimeCursor,
            @Param("startTimeCursor") ZonedDateTime startTimeCursor,
            @Param("idCursor") Long idCursor,
            Pageable pageable
    );

    @Query("""
            SELECT e.id
            FROM Event e
            WHERE e.creator = :user
              AND e.unconfirmed = false
              AND (:excludeEventId IS NULL OR e.id <> :excludeEventId)
              AND (
                   (e.startTime <= :end AND e.endTime >= :start)
                 )
            """)
    Set<Long> findConflictingEventIds(@Param("user") User user,
                                      @Param("start") ZonedDateTime start,
                                      @Param("end") ZonedDateTime end,
                                      @Param("excludeEventId") Long excludeEventId);

    @Query("""
        SELECT e FROM Event e
        WHERE e.label.id = :labelId
        AND e.startTime >= :startDate
        AND e.endTime <= :endDate
        AND e.isCompleted = true
    """)
    List<Event> findByLabelIdAndEventDateBetweenAndIsCompleted(
            @Param("labelId") Long labelId,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate
    );

    @Query("""
    SELECT COUNT(e) FROM Event e
    WHERE e.label.id = :labelId
    AND e.startTime >= :startDate
    AND e.endTime <= :endDate
    AND e.isCompleted = true
""")
    long countByLabelIdAndEventDateBetweenAndIsCompleted(
            @Param("labelId") Long labelId,
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate
    );

    @Override
    Optional<Event> findById(Long eventId);

    @Override
    <S extends Event> S save(S event);

    @Override
    void deleteById(Long eventId);

    @Modifying
    @Query("DELETE FROM Event e WHERE e.creator.id = :userId AND e.unconfirmed = true")
    void deleteAllUnconfirmedEventsByUser(@Param("userId") Long userId);

    @Query("""
    SELECT e FROM Event e
    WHERE e.recurringEvent.id = :recurringEventId
    AND e.startTime > :currentTime
    ORDER BY e.startTime ASC
    """)
    List<Event> findFutureEventsByRecurringEventId(
            @Param("recurringEventId") Long recurringEventId,
            @Param("currentTime") ZonedDateTime currentTime
    );
}
