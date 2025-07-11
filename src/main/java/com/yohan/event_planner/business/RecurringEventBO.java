package com.yohan.event_planner.business;

import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.dto.EventResponseDTO;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
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

    /**
     * Retrieves all confirmed recurring events for a user within the specified date range.
     * 
     * <p>Only returns recurring events that have been confirmed (not in draft state).
     * The date range is inclusive of both start and end dates.</p>
     *
     * @param userId the ID of the user whose recurring events to retrieve
     * @param fromDate the start date of the range (inclusive)
     * @param toDate the end date of the range (inclusive)
     * @return a list of confirmed recurring events within the date range
     */
    List<RecurringEvent> getConfirmedRecurringEventsForUserInRange(Long userId, LocalDate fromDate, LocalDate toDate);

    /**
     * Retrieves a paginated list of confirmed recurring events for a user using cursor-based pagination.
     * 
     * <p>If cursor parameters are null, returns the first page ordered by end date descending, then ID descending.
     * If cursor parameters are provided, returns events before the cursor position.</p>
     *
     * @param userId the ID of the user whose recurring events to retrieve
     * @param endDateCursor the end date cursor for pagination (null for first page)
     * @param startDateCursor the start date cursor for pagination (null for first page)
     * @param startTimeCursor the start time cursor for pagination (null for first page)
     * @param endTimeCursor the end time cursor for pagination (null for first page)
     * @param idCursor the ID cursor for pagination (null for first page)
     * @param limit the maximum number of events to return
     * @return a list of confirmed recurring events for the specified page
     */
    List<RecurringEvent> getConfirmedRecurringEventsPage(
            Long userId,
            LocalDate endDateCursor,
            LocalDate startDateCursor,
            LocalTime startTimeCursor,
            LocalTime endTimeCursor,
            Long idCursor,
            int limit
    );

    /**
     * Retrieves all unconfirmed (draft) recurring events for a user within the specified date range.
     * 
     * <p>Only returns recurring events that are in draft state (unconfirmed).
     * The date range is inclusive of both start and end dates.</p>
     *
     * @param userId the ID of the user whose draft recurring events to retrieve
     * @param fromDate the start date of the range (inclusive)
     * @param toDate the end date of the range (inclusive)
     * @return a list of unconfirmed recurring events within the date range
     */
    List<RecurringEvent> getUnconfirmedRecurringEventsForUserInRange(Long userId, LocalDate fromDate, LocalDate toDate);


    /**
     * Creates a new recurring event after validating recurrence rules and time bounds.
     * 
     * <p>For confirmed events, performs full validation including field validation, 
     * time consistency checks, and conflict detection. For unconfirmed (draft) events,
     * performs minimal validation to allow flexible data entry.</p>
     *
     * @param recurringEvent the recurring event entity to save
     * @return the saved {@link RecurringEvent}
     * @throws com.yohan.event_planner.exception.InvalidEventStateException if validation fails for confirmed events
     * @throws com.yohan.event_planner.exception.ConflictException if scheduling conflicts are detected
     */
    RecurringEvent createRecurringEventWithValidation(RecurringEvent recurringEvent);

    /**
     * Updates an existing recurring event with validation and conflict checking.
     * 
     * <p>For confirmed events, performs full validation including field validation, 
     * time consistency checks, and conflict detection. For unconfirmed (draft) events,
     * performs minimal validation. Note: This method does not propagate changes to
     * existing event instances - that is handled at the service layer.</p>
     *
     * @param updatedEvent the event with updated fields
     * @return the updated {@link RecurringEvent}
     * @throws com.yohan.event_planner.exception.InvalidEventStateException if validation fails for confirmed events
     * @throws com.yohan.event_planner.exception.ConflictException if scheduling conflicts are detected
     */
    RecurringEvent updateRecurringEvent(RecurringEvent updatedEvent);

    /**
     * Confirms a draft recurring event by applying full validation and conflict checking.
     * 
     * <p>Transitions an unconfirmed (draft) recurring event to confirmed status after
     * validating all required fields, time consistency, and checking for scheduling conflicts.
     * The recurrence rule is parsed and validated during this process.</p>
     *
     * @param recurringEvent the draft recurring event to confirm
     * @return the confirmed {@link RecurringEvent}
     * @throws com.yohan.event_planner.exception.RecurringEventAlreadyConfirmedException if the recurring event is already confirmed
     * @throws com.yohan.event_planner.exception.InvalidEventStateException if validation fails
     * @throws com.yohan.event_planner.exception.ConflictException if scheduling conflicts are detected
     */
    RecurringEvent confirmRecurringEventWithValidation(RecurringEvent recurringEvent);

    /**
     * Deletes a recurring event and cascades any associated cleanup, such as
     * deleting or demoting all future incomplete event instances.
     *
     * @param recurringEventId the ID of the recurring event to delete
     */
    void deleteRecurringEvent(Long recurringEventId);

    /**
     * Deletes all unconfirmed (draft) recurring events for a specific user.
     * 
     * <p>This is typically used during user cleanup operations to remove
     * all draft recurring events that haven't been confirmed yet.</p>
     *
     * @param userId the ID of the user whose draft recurring events should be deleted
     */
    void deleteAllUnconfirmedRecurringEventsByUser(Long userId);

    /**
     * Removes specified skip days from a recurring event after validating no conflicts would occur.
     * 
     * <p>Skip days are dates where a recurring event occurrence is intentionally skipped.
     * This method removes the specified dates from the skip days set, effectively
     * un-skipping those occurrences. Conflict validation ensures that restoring these
     * occurrences won't create scheduling conflicts.</p>
     *
     * @param recurringEvent the recurring event to modify
     * @param skipDaysToRemove the set of dates to remove from the skip days
     * @throws com.yohan.event_planner.exception.ConflictException if removing skip days would create conflicts
     */
    void removeSkipDaysWithConflictValidation(RecurringEvent recurringEvent, Set<LocalDate> skipDaysToRemove);

    /**
     * Generates virtual events from confirmed recurring events within the specified time range.
     * Virtual events represent future occurrences of recurring events that haven't been solidified yet.
     *
     * <p><strong>Key Behaviors:</strong></p>
     * <ul>
     *   <li>Only processes confirmed (non-draft) recurring events</li>
     *   <li>Filters out past events - only returns future occurrences</li>
     *   <li>Respects skip days - excludes dates marked as skipped</li>
     *   <li>Uses user timezone for date calculations and filtering</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <pre>{@code
     * // Generate virtual events for a specific day
     * ZoneId userZone = ZoneId.of("America/New_York");
     * LocalDate today = LocalDate.now();
     * ZonedDateTime startOfDay = today.atStartOfDay(userZone).withZoneSameInstant(ZoneOffset.UTC);
     * ZonedDateTime endOfDay = today.plusDays(1).atStartOfDay(userZone).withZoneSameInstant(ZoneOffset.UTC);
     * List<EventResponseDTO> virtualEvents = generateVirtuals(userId, startOfDay, endOfDay, userZone);
     * 
     * // Generate virtual events for a week view
     * LocalDate weekStart = today.with(DayOfWeek.MONDAY);
     * ZonedDateTime weekStartTime = weekStart.atStartOfDay(userZone).withZoneSameInstant(ZoneOffset.UTC);
     * ZonedDateTime weekEndTime = weekStart.plusDays(7).atStartOfDay(userZone).withZoneSameInstant(ZoneOffset.UTC);
     * List<EventResponseDTO> weeklyVirtuals = generateVirtuals(userId, weekStartTime, weekEndTime, userZone);
     * }</pre>
     *
     * @param userId the ID of the user whose recurring events to process
     * @param startTime the start of the time window (inclusive)
     * @param endTime the end of the time window (exclusive)
     * @param userZoneId the timezone for date calculations
     * @return a list of virtual events within the time range
     */
    List<EventResponseDTO> generateVirtuals(Long userId, ZonedDateTime startTime, ZonedDateTime endTime, ZoneId userZoneId);
}