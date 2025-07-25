package com.yohan.event_planner.repository;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.RecurringEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Repository interface for managing {@link RecurringEvent} entities with pattern-based queries.
 * 
 * <p>This repository provides specialized data access functionality for recurring event patterns,
 * supporting complex temporal queries, state management, and cursor-based pagination. It enables
 * efficient retrieval of recurring patterns for expansion, calendar generation, and analytics
 * within the event planning system.</p>
 * 
 * <h2>Core Capabilities</h2>
 * <ul>
 *   <li><strong>Pattern State Queries</strong>: Retrieve patterns by confirmation status</li>
 *   <li><strong>Temporal Range Queries</strong>: Find patterns intersecting date ranges</li>
 *   <li><strong>Cursor Pagination</strong>: Efficient pagination for large pattern collections</li>
 *   <li><strong>User-Scoped Access</strong>: All queries filtered by pattern ownership</li>
 * </ul>
 * 
 * <h2>Recurring Pattern Management</h2>
 * <p>Comprehensive support for recurring event patterns:</p>
 * <ul>
 *   <li><strong>Pattern Lifecycle</strong>: Draft, confirmed, and active pattern states</li>
 *   <li><strong>Date Range Filtering</strong>: Patterns intersecting specific time windows</li>
 *   <li><strong>Ownership Isolation</strong>: User-specific pattern collections</li>
 *   <li><strong>State Transitions</strong>: Support for pattern confirmation workflows</li>
 * </ul>
 * 
 * <h2>Performance Optimization</h2>
 * <ul>
 *   <li><strong>Indexed Queries</strong>: Utilize creator_id and date range indexes</li>
 *   <li><strong>Cursor Pagination</strong>: Efficient pagination without offset costs</li>
 *   <li><strong>Range Queries</strong>: Optimized date range intersection logic</li>
 *   <li><strong>State Filtering</strong>: Efficient confirmation status queries</li>
 * </ul>
 * 
 * @see RecurringEvent
 * @see RecurringEventService
 * @see Event
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
@Repository
public interface RecurringEventRepository extends JpaRepository<RecurringEvent, Long> {

    /**
     * Finds unconfirmed (draft) recurring events for a user that overlap with the specified date range.
     * 
     * <p><strong>Range Logic:</strong> Returns events where the event's date range intersects 
     * with the query range. An event overlaps if:
     * <ul>
     *   <li>Event starts before or on the query end date: {@code startDate <= toDate}</li>
     *   <li>Event ends after or on the query start date: {@code endDate >= fromDate}</li>
     * </ul></p>
     * 
     * <p><strong>Parameter Order:</strong> Critical - parameters must be passed in exact order:
     * userId, fromDate, toDate</p>
     * 
     * @param userId the ID of the user whose draft recurring events to retrieve
     * @param fromDate the start date of the query range (inclusive)
     * @param toDate the end date of the query range (inclusive) 
     * @return list of unconfirmed recurring events overlapping the date range
     */
    @Query("""
    SELECT r FROM RecurringEvent r
    WHERE r.creator.id = :userId
      AND r.unconfirmed = true
      AND r.startDate <= :toDate
      AND r.endDate >= :fromDate
""")
    List<RecurringEvent> findUnconfirmedRecurringEventsForUserInRange(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    /**
     * Finds confirmed recurring events for a user that overlap with the specified date range.
     * 
     * <p><strong>Range Logic:</strong> Returns events where the event's date range intersects 
     * with the query range. An event overlaps if:
     * <ul>
     *   <li>Event starts before or on the query end date: {@code startDate <= toDate}</li>
     *   <li>Event ends after or on the query start date: {@code endDate >= fromDate}</li>
     * </ul></p>
     * 
     * <p><strong>Parameter Order:</strong> Critical - parameters must be passed in exact order:
     * userId, fromDate, toDate</p>
     * 
     * @param userId the ID of the user whose recurring events to retrieve
     * @param fromDate the start date of the query range (inclusive)
     * @param toDate the end date of the query range (inclusive) 
     * @return list of confirmed recurring events overlapping the date range
     */
    @Query("""
    SELECT r FROM RecurringEvent r
    WHERE r.creator.id = :userId
      AND r.unconfirmed = false
      AND r.startDate <= :toDate
      AND r.endDate >= :fromDate
""")
    List<RecurringEvent> findConfirmedRecurringEventsForUserBetween(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("""
        SELECT r FROM RecurringEvent r
        WHERE r.creator.id = :userId
          AND r.unconfirmed = false
        ORDER BY r.endDate DESC, r.id DESC
    """)
    List<RecurringEvent> findTopConfirmedByUserIdOrderByEndDateDescIdDesc(
            Long userId,
            Pageable pageable
    );

    @Query("""
    SELECT r FROM RecurringEvent r
    WHERE r.creator.id = :userId
      AND r.unconfirmed = false
      AND (
          r.endDate < :endDateCursor
          OR (r.endDate = :endDateCursor AND r.startDate < :startDateCursor)
          OR (r.endDate = :endDateCursor AND r.startDate = :startDateCursor AND r.startTime < :startTimeCursor)
          OR (r.endDate = :endDateCursor AND r.startDate = :startDateCursor AND r.startTime = :startTimeCursor AND r.endTime < :endTimeCursor)
          OR (r.endDate = :endDateCursor AND r.startDate = :startDateCursor AND r.startTime = :startTimeCursor AND r.endTime = :endTimeCursor AND r.id < :idCursor)
      )
    ORDER BY r.endDate DESC, r.startDate DESC, r.startTime DESC, r.endTime DESC, r.id DESC
""")
    List<RecurringEvent> findConfirmedByUserIdBeforeCursor(
            @Param("userId") Long userId,
            @Param("endDateCursor") LocalDate endDateCursor,
            @Param("startDateCursor") LocalDate startDateCursor,
            @Param("startTimeCursor") LocalTime startTimeCursor,
            @Param("endTimeCursor") LocalTime endTimeCursor,
            @Param("idCursor") Long idCursor,
            Pageable pageable
    );

    @Query("""
    SELECT r FROM RecurringEvent r
    WHERE r.creator.id = :userId
      AND r.unconfirmed = false
      AND r.startDate <= :date
      AND r.endDate >= :date
      AND r.startTime <= :endTime
      AND r.endTime >= :startTime
""")
    List<RecurringEvent> findPotentialConflictingRecurringEvents(
            @Param("userId") Long userId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    @Query("SELECT e FROM RecurringEvent e WHERE e.creator.id = :creatorId AND (" +
            // Check for overlap in startTime and endTime within the same date range
            "(e.startDate = :newEventStartDate AND e.startTime BETWEEN :startTime AND :endTime) OR " +
            "(e.endDate = :newEventEndDate AND e.endTime BETWEEN :startTime AND :endTime) OR " +
            // Check for overlap based on startDate and endDate range
            "(e.startDate BETWEEN :newEventStartDate AND :newEventEndDate) OR " +
            "(e.endDate BETWEEN :newEventStartDate AND :newEventEndDate))")
    List<RecurringEvent> findOverlappingRecurringEvents(
            @Param("creatorId") Long creatorId,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("newEventStartDate") LocalDate newEventStartDate,
            @Param("newEventEndDate") LocalDate newEventEndDate
    );

    void deleteByCreatorIdAndUnconfirmedTrue(Long creatorId);
}
