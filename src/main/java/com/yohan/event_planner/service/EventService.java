package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.dto.DayViewDTO;
import com.yohan.event_planner.dto.EventCreateDTO;
import com.yohan.event_planner.dto.EventFilterDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.dto.WeekViewDTO;
import com.yohan.event_planner.exception.ConflictException;
import com.yohan.event_planner.exception.EventNotFoundException;
import com.yohan.event_planner.exception.EventOwnershipException;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

/**
 * Service interface for managing event-related operations.
 *
 * <p>
 * Provides functionality for creating, updating, deleting, confirming,
 * and retrieving events. Abstracts business rules and delegates data access
 * to the appropriate layers.
 * </p>
 */
public interface EventService {

    /**
     * Retrieves an event by its unique ID.
     *
     * @param eventId the ID of the event to retrieve
     * @return the corresponding {@link EventResponseDTO}
     * @throws EventNotFoundException if no event exists with the given ID
     */
    EventResponseDTO getEventById(Long eventId);

    /**
     * Retrieves a paginated list of confirmed events for the current user with filtering.
     *
     * @param filter the filtering criteria including time range, labels, and sort options
     * @param pageNumber zero-based page number to retrieve
     * @param pageSize maximum number of events to return per page
     * @return paginated list of confirmed events matching the filter criteria
     * @throws UnauthorizedException if the user is not authenticated
     */
    Page<EventResponseDTO> getConfirmedEventsForCurrentUser(EventFilterDTO filter, int pageNumber, int pageSize);

    /**
     * Retrieves confirmed events using cursor-based pagination for infinite scrolling.
     *
     * @param endTimeCursor cursor position based on event end time for pagination
     * @param startTimeCursor cursor position based on event start time for pagination
     * @param idCursor cursor position based on event ID for tie-breaking
     * @param limit maximum number of events to return
     * @return list of confirmed events after the cursor position
     * @throws UnauthorizedException if the user is not authenticated
     */
    List<EventResponseDTO> getConfirmedEventsPage(
            ZonedDateTime endTimeCursor,
            ZonedDateTime startTimeCursor,
            Long idCursor,
            int limit
    );

    /**
     * Retrieves all unconfirmed (draft) events for the current user.
     *
     * @return list of unconfirmed events owned by the current user
     * @throws UnauthorizedException if the user is not authenticated
     */
    List<EventResponseDTO> getUnconfirmedEventsForCurrentUser();

    /**
     * Generates a comprehensive day view with events and time slots for the specified date.
     *
     * @param selectedDate the date to generate the view for (in user's timezone)
     * @return day view containing confirmed events, virtual recurring events, and time slots
     * @throws UnauthorizedException if the user is not authenticated
     */
    DayViewDTO generateDayView(LocalDate selectedDate);

    /**
     * Generates a comprehensive week view anchored on the specified date.
     *
     * @param anchorDate any date within the desired week (week starts on Monday)
     * @return week view containing confirmed events and virtual recurring events for the full week
     * @throws UnauthorizedException if the user is not authenticated
     */
    WeekViewDTO generateWeekView(LocalDate anchorDate);

    /**
     * Creates a new scheduled event using the provided data transfer object.
     *
     * <p>
     * The event will be associated with the currently authenticated user
     * and immediately validated for time and conflict rules.
     * </p>
     *
     * @param dto the event creation payload
     * @return the created {@link EventResponseDTO}
     * @throws UnauthorizedException if the user is not authenticated
     * @throws ConflictException if the event overlaps with an existing one
     */
    EventResponseDTO createEvent(EventCreateDTO dto);

    /**
     * Creates an impromptu event starting at the current time with default settings.
     *
     * @return the created impromptu event as EventResponseDTO
     * @throws UnauthorizedException if the user is not authenticated
     */
    EventResponseDTO createImpromptuEvent();

    /**
     * Confirms a previously saved draft or impromptu event, finalizing its timeslot
     * and making it eligible for timeline aggregation and conflict checking.
     *
     * <p>
     * If the event is already confirmed (i.e. not a draft), this will throw an exception.
     * </p>
     *
     * @param eventId the ID of the event to confirm
     * @return the confirmed {@link EventResponseDTO}
     * @throws EventNotFoundException if the event does not exist
     * @throws EventOwnershipException if the user does not own the event
     * @throws EventAlreadyConfirmedException if the event is already confirmed
     */
    EventResponseDTO confirmEvent(Long eventId);

    /**
     * Confirms and marks the event as completed in a single operation.
     *
     * <p>
     * Intended for confirming events that have already ended or been completed
     * at the time of confirmation (e.g., impromptu events).
     * </p>
     *
     * @param eventId the ID of the event to confirm and complete
     * @return the updated {@link EventResponseDTO}
     * @throws EventNotFoundException if the event does not exist
     * @throws EventOwnershipException if the user does not own the event
     * @throws EventAlreadyConfirmedException if the event is already confirmed
     */
    EventResponseDTO confirmAndCompleteEvent(Long eventId);

    /**
     * Applies partial updates to an existing event.
     *
     * <p>
     * Only non-null fields in the update DTO are applied. The current user must be the event creator.
     * </p>
     *
     * @param eventId the ID of the event to update
     * @param dto     the update payload
     * @return the updated {@link EventResponseDTO}
     * @throws EventNotFoundException if the event does not exist
     * @throws EventOwnershipException if the user is not authorized to update the event
     */
    EventResponseDTO updateEvent(Long eventId, EventUpdateDTO dto);

    /**
     * Deletes an existing event.
     *
     * <p>
     * The event must exist and be owned by the currently authenticated user.
     * </p>
     *
     * @param eventId the ID of the event to delete
     * @throws EventNotFoundException if the event does not exist
     * @throws EventOwnershipException if the user is not authorized to delete the event
     */
    void deleteEvent(Long eventId);

    /**
     * Deletes all unconfirmed (draft) events for the current user in a single operation.
     *
     * @throws UnauthorizedException if the user is not authenticated
     */
    void deleteUnconfirmedEventsForCurrentUser();

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
