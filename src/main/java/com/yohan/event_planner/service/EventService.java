package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.dto.ConflictResolutionDTO;
import com.yohan.event_planner.dto.DayViewDTO;
import com.yohan.event_planner.dto.EventCreateDTO;
import com.yohan.event_planner.dto.EventFilterDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.dto.WeekViewDTO;
import com.yohan.event_planner.exception.ConflictException;
import com.yohan.event_planner.exception.EventNotFoundException;
import com.yohan.event_planner.exception.EventOwnershipException;
import com.yohan.event_planner.exception.InvalidTimeException;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

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

    Page<EventResponseDTO> getConfirmedEventsForCurrentUser(EventFilterDTO filter, int pageNumber, int pageSize);

    List<EventResponseDTO> getConfirmedEventsPage(
            ZonedDateTime endTimeCursor,
            ZonedDateTime startTimeCursor,
            Long idCursor,
            int limit
    );

    List<EventResponseDTO> getUnconfirmedEventsForCurrentUser();

    DayViewDTO generateDayView(LocalDate selectedDate);

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

    void deleteUnconfirmedEventsForCurrentUser();

}
