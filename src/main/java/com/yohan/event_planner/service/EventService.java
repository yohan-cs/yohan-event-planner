package com.yohan.event_planner.service;

import com.yohan.event_planner.dto.EventCreateDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.exception.ConflictException;
import com.yohan.event_planner.exception.EventNotFoundException;
import com.yohan.event_planner.exception.EventOwnershipException;
import com.yohan.event_planner.exception.UnauthorizedException;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Service interface for managing event-related operations.
 *
 * <p>
 * Defines operations for creating, updating, deleting, and retrieving events.
 * Acts as the primary orchestrator for event workflows and abstracts underlying business logic.
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
     * Retrieves all events created by the specified user.
     *
     * @param userId the ID of the user whose events to fetch
     * @return a list of {@link EventResponseDTO}s created by the user; may be empty
     */
    List<EventResponseDTO> getEventsByUser(Long userId);

    /**
     * Retrieves all events created by the specified user within a given date-time range.
     *
     * @param userId the ID of the user whose events to fetch
     * @param start  the inclusive lower bound of the date-time range
     * @param end    the exclusive upper bound of the date-time range
     * @return a list of {@link EventResponseDTO}s matching the criteria; may be empty
     */
    List<EventResponseDTO> getEventsByUserAndDateRange(Long userId, ZonedDateTime start, ZonedDateTime end);

    /**
     * Retrieves all events created by the currently authenticated user.
     *
     * <p>
     * Authentication is resolved internally using the current security context.
     * </p>
     *
     * @return a list of {@link EventResponseDTO}s for the current user; may be empty
     * @throws UnauthorizedException if the user is not authenticated
     */
    List<EventResponseDTO> getEventsByCurrentUser();

    /**
     * Retrieves events created by the currently authenticated user within a date-time range.
     *
     * <p>
     * Authentication is resolved internally using the current security context.
     * </p>
     *
     * @param start the inclusive lower bound of the date-time range
     * @param end   the exclusive upper bound of the date-time range
     * @return a list of {@link EventResponseDTO}s matching the criteria; may be empty
     * @throws UnauthorizedException if the user is not authenticated
     */
    List<EventResponseDTO> getEventsByCurrentUserAndDateRange(ZonedDateTime start, ZonedDateTime end);

    /**
     * Creates a new event using the provided data transfer object.
     *
     * <p>
     * The event will be associated with the currently authenticated user as the creator.
     * </p>
     *
     * @param dto the event creation payload
     * @return the created {@link EventResponseDTO}
     * @throws UnauthorizedException if the user is not authenticated
     * @throws ConflictException if the event overlaps with an existing one
     */
    EventResponseDTO createEvent(EventCreateDTO dto);

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
}
