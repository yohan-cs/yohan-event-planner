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
 * Repository interface for managing {@link Event} entities with advanced query capabilities.
 * 
 * <p>This repository provides comprehensive data access functionality for event management,
 * including complex queries for event retrieval, conflict detection, pagination support,
 * and recurring event integration. It extends Spring Data JPA to offer both standard
 * CRUD operations and sophisticated custom queries optimized for the event planning domain.</p>
 * 
 * <h2>Core Query Categories</h2>
 * <ul>
 *   <li><strong>Event State Queries</strong>: Retrieve events by confirmation status</li>
 *   <li><strong>Time-based Queries</strong>: Filter events by date ranges and time windows</li>
 *   <li><strong>Conflict Detection</strong>: Identify scheduling conflicts for time management</li>
 *   <li><strong>Pagination Support</strong>: Cursor-based pagination for large datasets</li>
 *   <li><strong>Label Integration</strong>: Query events by categorization labels</li>
 *   <li><strong>Recurring Event Queries</strong>: Handle recurring event relationships</li>
 * </ul>
 * 
 * <h2>Event State Management</h2>
 * <p>Sophisticated queries for event lifecycle management:</p>
 * <ul>
 *   <li><strong>Confirmed Events</strong>: Retrieve finalized, confirmed events</li>
 *   <li><strong>Unconfirmed Events</strong>: Access draft events for editing</li>
 *   <li><strong>Completed Events</strong>: Filter by completion status for analytics</li>
 *   <li><strong>State Transitions</strong>: Support for event state queries</li>
 * </ul>
 * 
 * <h2>Time Window Queries</h2>
 * <p>Advanced temporal filtering capabilities:</p>
 * <ul>
 *   <li><strong>Date Range Filtering</strong>: Events within specific time windows</li>
 *   <li><strong>Overlap Detection</strong>: Find events that intersect time ranges</li>
 *   <li><strong>Future Event Queries</strong>: Retrieve upcoming events efficiently</li>
 *   <li><strong>Historical Queries</strong>: Access past events for analytics</li>
 * </ul>
 * 
 * <h2>Conflict Detection System</h2>
 * <p>Comprehensive scheduling conflict identification:</p>
 * <ul>
 *   <li><strong>Time Overlap Detection</strong>: Identify overlapping event times</li>
 *   <li><strong>User-scoped Conflicts</strong>: Conflicts within user's events only</li>
 *   <li><strong>Exclusion Support</strong>: Exclude specific events from conflict checks</li>
 *   <li><strong>Efficient Algorithms</strong>: Optimized queries for conflict detection</li>
 * </ul>
 * 
 * <h2>Pagination and Performance</h2>
 * <p>Optimized data access patterns:</p>
 * <ul>
 *   <li><strong>Cursor-based Pagination</strong>: Efficient pagination for large datasets</li>
 *   <li><strong>Multi-field Ordering</strong>: Complex sorting with tie-breaking</li>
 *   <li><strong>Indexed Queries</strong>: Database-optimized query patterns</li>
 *   <li><strong>Minimal Data Transfer</strong>: Efficient result set management</li>
 * </ul>
 * 
 * <h2>Label Integration</h2>
 * <p>Support for event categorization and analytics:</p>
 * <ul>
 *   <li><strong>Label-based Filtering</strong>: Query events by associated labels</li>
 *   <li><strong>Completion Analytics</strong>: Count completed events by label</li>
 *   <li><strong>Time Range Analytics</strong>: Label-specific temporal queries</li>
 *   <li><strong>Statistical Support</strong>: Data for time tracking and analytics</li>
 * </ul>
 * 
 * <h2>Recurring Event Support</h2>
 * <p>Specialized queries for recurring event patterns:</p>
 * <ul>
 *   <li><strong>Pattern Relationships</strong>: Link individual events to recurring patterns</li>
 *   <li><strong>Future Instance Queries</strong>: Retrieve upcoming recurring instances</li>
 *   <li><strong>Draft Inclusion</strong>: Include relevant draft events in results</li>
 *   <li><strong>Bulk Operations</strong>: Efficient operations on recurring series</li>
 * </ul>
 * 
 * <h2>Security Considerations</h2>
 * <p>Data access patterns support security enforcement:</p>
 * <ul>
 *   <li><strong>User Scoping</strong>: All queries naturally scope to user ownership</li>
 *   <li><strong>Authorization Support</strong>: Query patterns support service-layer security</li>
 *   <li><strong>Privacy Protection</strong>: Prevent cross-user data access</li>
 *   <li><strong>State Filtering</strong>: Respect event visibility rules</li>
 * </ul>
 * 
 * <h2>Performance Optimization</h2>
 * <ul>
 *   <li><strong>Index-aware Queries</strong>: Designed for database index utilization</li>
 *   <li><strong>Minimal Joins</strong>: Reduce query complexity where possible</li>
 *   <li><strong>Batch Operations</strong>: Support for bulk data operations</li>
 *   <li><strong>Result Set Optimization</strong>: Efficient data transfer patterns</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This repository integrates with:</p>
 * <ul>
 *   <li><strong>EventService</strong>: Primary service layer integration</li>
 *   <li><strong>EventDAO</strong>: Advanced queries via Blaze-Persistence</li>
 *   <li><strong>RecurringEventService</strong>: Recurring pattern management</li>
 *   <li><strong>Calendar Services</strong>: View generation and aggregation</li>
 * </ul>
 * 
 * <h2>Important Notes</h2>
 * <ul>
 *   <li><strong>Service Layer Security</strong>: Authorization enforced at service layer</li>
 *   <li><strong>UTC Time Storage</strong>: All timestamps stored in UTC</li>
 *   <li><strong>State Consistency</strong>: Queries respect event state invariants</li>
 *   <li><strong>Null Safety</strong>: Proper handling of nullable parameters</li>
 * </ul>
 * 
 * @see Event
 * @see EventService
 * @see EventDAO
 * @see RecurringEvent
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e WHERE e.creator.id = :userId AND e.unconfirmed = true ORDER BY e.startTime DESC")
    List<Event> findUnconfirmedEventsForUserSortedByStartTime(@Param("userId") Long userId);

    @Query("""
    SELECT e FROM Event e
    WHERE e.creator.id = :userId
      AND e.unconfirmed = false
      AND e.endTime >= :windowStart
      AND e.startTime <= :windowEnd
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
        AND e.endTime >= :startDate
        AND e.startTime <= :endDate
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
    AND e.endTime >= :startDate
    AND e.startTime <= :endDate
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
