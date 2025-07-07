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

@Repository
public interface RecurringEventRepository extends JpaRepository<RecurringEvent, Long> {

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
