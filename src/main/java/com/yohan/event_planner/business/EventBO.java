package com.yohan.event_planner.business;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.DayViewDTO;
import com.yohan.event_planner.dto.EventChangeContextDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.WeekViewDTO;
import com.yohan.event_planner.exception.ConflictException;
import com.yohan.event_planner.exception.EventNotFoundException;
import com.yohan.event_planner.exception.EventOwnershipException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
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

    List<Event> getConfirmedEventsForUserInRange(Long userId, ZonedDateTime windowStart, ZonedDateTime windowEnd);

    List<Event> getConfirmedEventsPage(
            Long userId,
            ZonedDateTime endTimeCursor,
            ZonedDateTime startTimeCursor,
            Long idCursor,
            int limit
    );

    List<Event> getUnconfirmedEventsForUser(Long userId);

    /**
     * Creates a new scheduled event after validating time bounds and checking for conflicts.
     *
     * <p>
     * This is typically used when the event is created with a start and end time
     * and should be validated immediately. It is not used for impromptu (open-ended)
     * event creation, which bypasses immediate validation.
     * </p>
     *
     * @param event the event to create (must include start time, end time, and creator)
     * @return the saved {@link Event}
     * @throws IllegalArgumentException if time validation fails
     * @throws ConflictException if the event conflicts with an existing event
     */
    Event createEvent(Event event);

    void solidifyRecurrences(
            Long userId,
            ZonedDateTime startTime,
            ZonedDateTime endTime,
            ZoneId userZoneId
    );

    /**
     * Applies updates to an existing event, validating time bounds and checking for conflicts.
     *
     * <p>
     * This method also handles time bucket updates when completion status or label changes.
     * The context DTO is used to detect what changed and update downstream systems accordingly.
     * </p>
     *
     * @param contextDTO context about the original event (for bucket/statistics updates)
     * @param event      the updated event to persist
     * @return the updated {@link Event}
     * @throws IllegalArgumentException if time validation fails
     * @throws ConflictException if the updated event conflicts with another event
     */
    Event updateEvent(EventChangeContextDTO contextDTO, Event event);

    /**
     * Confirms an open-ended (impromptu) event by performing conflict validation and
     * persisting the updated state.
     *
     * <p>
     * This method is only used to transition an event from an unconfirmed state
     * (e.g. draft = true) to confirmed (draft = false). If the event is already
     * confirmed, the caller is expected to short-circuit or raise an exception.
     * </p>
     *
     * @param event the event to confirm
     * @return the confirmed and saved {@link Event}
     * @throws ConflictException if the confirmed time range overlaps another event
     */
    Event confirmEvent(Event event);

    /**
     * Deletes an event by its ID.
     *
     * @param eventId the ID of the event to delete
     * @throws EventNotFoundException if no event exists for the given ID
     * @throws EventOwnershipException if the current user does not own the event
     */
    void deleteEvent(Long eventId);

    void deleteAllUnconfirmedEventsByUser(Long userId);

    /**
     * Generates a day view containing events for the specified date.
     * 
     * <p>
     * This method encapsulates the business logic for:
     * - Event combination and sorting for a single day
     * - Proper event ordering by start time
     * </p>
     *
     * @param selectedDate the date to generate the view for
     * @param confirmedEvents the confirmed events for the day
     * @param virtualEvents the virtual events to include
     * @return a DayViewDTO containing sorted events for the day
     */
    DayViewDTO generateDayViewData(LocalDate selectedDate, List<EventResponseDTO> confirmedEvents, List<EventResponseDTO> virtualEvents);

    /**
     * Generates a week view containing events for the week containing the anchor date.
     * 
     * <p>
     * This method encapsulates the business logic for:
     * - Calculating week boundaries (Monday to Sunday)
     * - Converting timezone boundaries from user zone to UTC
     * - Deciding when to solidify past recurrences vs generate virtual events
     * - Grouping events by day within the week
     * </p>
     *
     * @param userId the user ID to generate the view for
     * @param anchorDate any date within the desired week
     * @param userZoneId the user's timezone for boundary calculations
     * @param confirmedEvents the confirmed events for the week period
     * @param virtualEvents the virtual events to include
     * @return a WeekViewDTO containing events grouped by day
     */
    WeekViewDTO generateWeekViewData(Long userId, LocalDate anchorDate, ZoneId userZoneId, 
                                   List<EventResponseDTO> confirmedEvents, List<EventResponseDTO> virtualEvents);

    /**
     * Updates future Event instances that were created from a RecurringEvent.
     * Only updates events that have a start time in the future.
     *
     * @param recurringEvent the RecurringEvent containing the updated values
     * @param changedFields the set of fields that changed (name, startTime, endTime, label)
     * @param userZoneId the timezone to use for time calculations
     * @return the number of events updated
     */
    int updateFutureEventsFromRecurringEvent(RecurringEvent recurringEvent, Set<String> changedFields, ZoneId userZoneId);

}
