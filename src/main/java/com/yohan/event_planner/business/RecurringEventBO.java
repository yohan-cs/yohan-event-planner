package com.yohan.event_planner.business;

import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.RecurringEventResponseDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Business Object (BO) interface for managing {@link RecurringEvent} entities.
 *
 * <p>
 * Encapsulates business-level operations related to recurring event scheduling,
 * including validation, conflict handling, and cascading actions such as
 * instance generation or deletion of future occurrences.
 * </p>
 *
 * <p>
 * This layer assumes that authorization and ownership checks are handled by
 * the service layer. It does not enforce access control.
 * </p>
 */
public interface RecurringEventBO {

    /**
     * Retrieves a recurring event by its unique ID.
     *
     * @param recurringEventId the ID of the recurring event to retrieve
     * @return an {@link Optional} containing the recurring event if found; otherwise empty
     */
    Optional<RecurringEvent> getRecurringEventById(Long recurringEventId);

    List<RecurringEvent> getConfirmedRecurringEventsForUserInRange(Long userId, LocalDate fromDate, LocalDate toDate);

    List<RecurringEvent> getConfirmedRecurringEventsPage(
            Long userId,
            LocalDate endDateCursor,
            LocalDate startDateCursor,
            LocalTime startTimeCursor,
            LocalTime endTimeCursor,
            Long idCursor,
            int limit
    );

    List<RecurringEvent> getUnconfirmedRecurringEventsForUserInRange(Long userId, LocalDate fromDate, LocalDate toDate);


    /**
     * Creates a new recurring event after validating recurrence rules and time bounds.
     *
     * @param recurringEvent the recurring event entity to save
     * @return the saved {@link RecurringEvent}
     */
    RecurringEvent createRecurringEventWithValidation(RecurringEvent recurringEvent);

    /**
     * Updates an existing recurring event, possibly adjusting future scheduled instances.
     *
     * @param updatedEvent the event with updated fields
     * @return the updated {@link RecurringEvent}
     */
    RecurringEvent updateRecurringEvent(RecurringEvent updatedEvent);

    RecurringEvent confirmRecurringEventWithValidation(RecurringEvent recurringEvent);

    /**
     * Deletes a recurring event and cascades any associated cleanup, such as
     * deleting or demoting all future incomplete event instances.
     *
     * @param recurringEventId the ID of the recurring event to delete
     */
    void deleteRecurringEvent(Long recurringEventId);

    void deleteAllUnconfirmedRecurringEventsByUser(Long userId);

    void removeSkipDaysWithConflictValidation(RecurringEvent recurringEvent, Set<LocalDate> skipDaysToRemove);

    /**
     * Generates virtual events from confirmed recurring events within the specified time range.
     * Virtual events represent future occurrences of recurring events that haven't been solidified yet.
     *
     * @param userId the ID of the user whose recurring events to process
     * @param startTime the start of the time window (inclusive)
     * @param endTime the end of the time window (exclusive)
     * @param userZoneId the timezone for date calculations
     * @return a list of virtual events within the time range
     */
    List<EventResponseDTO> generateVirtuals(Long userId, ZonedDateTime startTime, ZonedDateTime endTime, ZoneId userZoneId);
}