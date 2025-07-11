package com.yohan.event_planner.business;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.dto.DayViewDTO;
import com.yohan.event_planner.dto.EventChangeContextDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.WeekViewDTO;
import com.yohan.event_planner.exception.ConflictException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Business Object (BO) interface for managing {@link Event} entities.
 *
 * <p>
 * Encapsulates business-level operations related to event scheduling,
 * including validation, conflict detection, and persistence delegation.
 * </p>
 *
 * <p>
 * This layer assumes that authorization and ownership checks are handled by
 * the service layer. It does not enforce access control.
 * </p>
 */
public interface EventBO {

    /**
     * Retrieves an event by its unique ID.
     *
     * @param eventId the ID of the event to retrieve
     * @return an {@link Optional} containing the event if found; otherwise empty
     */
    Optional<Event> getEventById(Long eventId);

    /**
     * Retrieves all confirmed events for a user within a specified time range.
     *
     * <p>
     * This method finds events that overlap with the given time window, including events
     * that start before the window but end within it, or start within the window but end after it.
     * Only confirmed (non-draft) events are returned.
     * </p>
     *
     * @param userId the ID of the user whose events to retrieve
     * @param windowStart the start of the time window (inclusive)
     * @param windowEnd the end of the time window (exclusive)
     * @return a list of confirmed events overlapping the time range, ordered by start time
     */
    List<Event> getConfirmedEventsForUserInRange(Long userId, ZonedDateTime windowStart, ZonedDateTime windowEnd);

    /**
     * Retrieves a paginated list of confirmed events for a user using cursor-based pagination.
     *
     * <p>
     * This method supports efficient pagination for large event lists by using cursor-based
     * pagination instead of offset-based pagination. Events are ordered by end time descending,
     * then start time descending, then ID descending.
     * </p>
     *
     * <p>
     * When all cursor parameters are null, returns the first page. When cursor parameters
     * are provided, returns events that come before the cursor position in the sort order.
     * </p>
     *
     * @param userId the ID of the user whose events to retrieve
     * @param endTimeCursor the end time cursor for pagination (null for first page)
     * @param startTimeCursor the start time cursor for pagination (null for first page)
     * @param idCursor the ID cursor for pagination (null for first page)
     * @param limit the maximum number of events to return
     * @return a list of confirmed events, ordered by end time desc, start time desc, ID desc
     * @throws IllegalArgumentException if limit is less than or equal to 0
     */
    List<Event> getConfirmedEventsPage(
            Long userId,
            ZonedDateTime endTimeCursor,
            ZonedDateTime startTimeCursor,
            Long idCursor,
            int limit
    );

    /**
     * Retrieves all unconfirmed (draft) events for a user.
     *
     * <p>
     * Unconfirmed events are those marked as drafts and may have incomplete information.
     * These events are typically used for planning purposes and are not yet finalized.
     * Results are ordered by start time ascending.
     * </p>
     *
     * @param userId the ID of the user whose unconfirmed events to retrieve
     * @return a list of unconfirmed events, ordered by start time ascending
     */
    List<Event> getUnconfirmedEventsForUser(Long userId);

    /**
     * Creates a new event with conditional validation based on confirmation status.
     *
     * <p>
     * This method handles both confirmed and unconfirmed (draft) event creation:
     * <ul>
     *   <li><strong>Confirmed events</strong>: Validates required fields, time bounds, and checks for conflicts</li>
     *   <li><strong>Unconfirmed events</strong>: Skips validation to allow flexible draft creation</li>
     * </ul>
     * </p>
     *
     * <p>
     * Draft events can have missing or incomplete information and are typically used
     * for planning purposes before being confirmed with complete details.
     * </p>
     *
     * @param event the event to create (creator is required; other fields validated only if confirmed)
     * @return the saved {@link Event}
     * @throws InvalidEventStateException if a confirmed event has missing required fields
     * @throws InvalidTimeException if a confirmed event has invalid time bounds
     * @throws ConflictException if a confirmed event conflicts with an existing event
     */
    Event createEvent(Event event);

    /**
     * Converts virtual recurring event instances into concrete Event entities within a time range.
     *
     * <p>
     * This method processes all confirmed recurring events for a user and creates individual
     * Event instances for occurrences that fall within the specified time window. This is
     * typically used to "solidify" past recurring events for historical tracking and analytics.
     * </p>
     *
     * <p>
     * For each recurring event occurrence:
     * <ul>
     *   <li>If no conflict exists, creates a confirmed Event</li>
     *   <li>If a scheduling conflict is detected, creates an unconfirmed draft Event</li>
     *   <li>Skips creation if an Event already exists for that date</li>
     * </ul>
     * </p>
     *
     * @param userId the ID of the user whose recurring events to solidify
     * @param startTime the start of the solidification window
     * @param endTime the end of the solidification window
     * @param userZoneId the user's timezone for date calculations
     */
    void solidifyRecurrences(
            Long userId,
            ZonedDateTime startTime,
            ZonedDateTime endTime,
            ZoneId userZoneId
    );

    /**
     * Applies updates to an existing event with conditional validation and side effects.
     *
     * <p>
     * This method handles both confirmed and unconfirmed event updates:
     * <ul>
     *   <li><strong>Confirmed events</strong>: Validates required fields, time bounds, and checks for conflicts</li>
     *   <li><strong>Unconfirmed events</strong>: Skips business validation to allow flexible editing</li>
     * </ul>
     * </p>
     *
     * <p>
     * Additional processing includes:
     * <ul>
     *   <li><strong>Completion validation</strong>: Ensures only past events can be marked as completed</li>
     *   <li><strong>Time bucket updates</strong>: Updates analytics when completion status changes</li>
     *   <li><strong>Context tracking</strong>: Uses context DTO to detect changes for downstream systems</li>
     * </ul>
     * </p>
     *
     * @param contextDTO context about the original event state (for analytics and change detection)
     * @param event the updated event to persist
     * @return the updated {@link Event}
     * @throws InvalidEventStateException if a confirmed event has missing required fields
     * @throws InvalidTimeException if time validation fails or attempting to complete a future event
     * @throws ConflictException if a confirmed event conflicts with another event
     */
    Event updateEvent(EventChangeContextDTO contextDTO, Event event);

    /**
     * Confirms an unconfirmed (draft) event by performing full validation and updating status.
     *
     * <p>
     * This method transitions an event from unconfirmed (draft) status to confirmed status
     * by performing the same validation that would occur during confirmed event creation:
     * <ul>
     *   <li>Validates all required fields are present (name, start time, end time, label)</li>
     *   <li>Validates time bounds (start time before end time)</li>
     *   <li>Checks for scheduling conflicts with other events</li>
     *   <li>Sets the event's unconfirmed flag to false</li>
     * </ul>
     * </p>
     *
     * <p>
     * This is typically used for completing draft events or confirming impromptu events
     * that were initially created without full validation.
     * </p>
     *
     * @param event the unconfirmed event to confirm (must have all required fields)
     * @return the confirmed and saved {@link Event}
     * @throws EventAlreadyConfirmedException if the event is already confirmed
     * @throws InvalidEventStateException if the event has missing required fields
     * @throws InvalidTimeException if the event has invalid time bounds
     * @throws ConflictException if the confirmed time range overlaps another event
     */
    Event confirmEvent(Event event);

    /**
     * Deletes an event by its ID.
     *
     * <p>
     * This method performs direct deletion without additional validation.
     * Authorization and ownership checks are expected to be handled by the service layer
     * before calling this method.
     * </p>
     *
     * @param eventId the ID of the event to delete
     */
    void deleteEvent(Long eventId);

    /**
     * Deletes all unconfirmed (draft) events for a specific user.
     *
     * <p>
     * This method removes all events marked as unconfirmed/draft for the given user.
     * This is typically used for cleanup operations, such as when a user wants to
     * clear all their draft events or during account management operations.
     * </p>
     *
     * <p>
     * Only unconfirmed events are affected; confirmed events remain unchanged.
     * </p>
     *
     * @param userId the ID of the user whose unconfirmed events to delete
     */
    void deleteAllUnconfirmedEventsByUser(Long userId);

    /**
     * Generates a day view by combining and sorting events for a specific date.
     * 
     * <p>
     * This method provides pure business logic for day view generation:
     * <ul>
     *   <li>Combines confirmed and virtual events into a single list</li>
     *   <li>Sorts all events by start time (UTC) in ascending order</li>
     *   <li>Returns a structured DTO for UI consumption</li>
     * </ul>
     * </p>
     *
     * <p>
     * The method does not perform data retrieval or timezone calculations - it operates
     * on pre-filtered event lists provided by the caller.
     * </p>
     *
     * @param selectedDate the date this view represents
     * @param confirmedEvents the confirmed events to include (pre-filtered for the date)
     * @param virtualEvents the virtual events to include (pre-filtered for the date)
     * @return a DayViewDTO containing all events sorted by start time
     */
    DayViewDTO generateDayViewData(LocalDate selectedDate, List<EventResponseDTO> confirmedEvents, List<EventResponseDTO> virtualEvents);

    /**
     * Generates a week view by organizing events into day-by-day structure.
     * 
     * <p>
     * This method provides business logic for week view generation:
     * <ul>
     *   <li>Calculates week boundaries (Monday to Sunday) from the anchor date</li>
     *   <li>Combines confirmed and virtual events into a single list</li>
     *   <li>Sorts all events by start time (UTC) in ascending order</li>
     *   <li>Groups events by local date in the user's timezone</li>
     *   <li>Handles multi-day events by including them in each day they span</li>
     *   <li>Creates DayViewDTO objects for each day of the week</li>
     * </ul>
     * </p>
     *
     * <p>
     * The method does not perform data retrieval - it operates on pre-filtered
     * event lists provided by the caller. Multi-day events appear in multiple
     * days based on their local date span in the user's timezone.
     * </p>
     *
     * @param userId the user ID (used for logging purposes)
     * @param anchorDate any date within the desired week
     * @param userZoneId the user's timezone for local date calculations
     * @param confirmedEvents the confirmed events to include (pre-filtered for the week)
     * @param virtualEvents the virtual events to include (pre-filtered for the week)
     * @return a WeekViewDTO containing 7 DayViewDTO objects with events grouped by day
     */
    WeekViewDTO generateWeekViewData(Long userId, LocalDate anchorDate, ZoneId userZoneId, 
                                   List<EventResponseDTO> confirmedEvents, List<EventResponseDTO> virtualEvents);

    /**
     * Propagates changes from a RecurringEvent to all future Event instances.
     *
     * <p>
     * This method updates all future Event instances that were created from the given
     * RecurringEvent, applying only the fields that have changed. The process includes:
     * <ul>
     *   <li>Short-circuits if no fields have changed (returns 0)</li>
     *   <li>Queries for all future events linked to the recurring event</li>
     *   <li>For each future event, creates an EventUpdateDTO with only changed fields</li>
     *   <li>Applies the patch using EventPatchHandler</li>
     *   <li>Saves the event if any changes were actually made</li>
     *   <li>Returns the count of events that were actually modified</li>
     * </ul>
     * </p>
     *
     * <p>
     * "Future" events are determined by comparing their start time to the current time
     * in the user's timezone. Only events that start in the future are updated.
     * </p>
     *
     * @param recurringEvent the RecurringEvent containing the updated values
     * @param changedFields the set of field names that changed (name, startTime, endTime, label)
     * @param userZoneId the timezone to use for determining "future" and time calculations
     * @return the number of events that were actually modified (not just processed)
     */
    int updateFutureEventsFromRecurringEvent(RecurringEvent recurringEvent, Set<String> changedFields, ZoneId userZoneId);

}
