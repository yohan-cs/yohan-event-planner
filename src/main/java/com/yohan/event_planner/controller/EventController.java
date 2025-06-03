package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.EventCreateDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.exception.ConflictException;
import com.yohan.event_planner.exception.EventNotFoundException;
import com.yohan.event_planner.exception.UnauthorizedException;
import com.yohan.event_planner.service.EventService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * REST controller for managing {@link com.yohan.event_planner.domain.Event} resources.
 *
 * <p>
 * Exposes endpoints to create, retrieve, update, and delete events.
 * Supports both current user–based queries and public user event retrieval by ID.
 * </p>
 *
 * <p>
 * Authorization is enforced internally via the service layer. The controller does not
 * perform any direct authentication or token parsing.
 * </p>
 */
@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * Retrieves an event by its unique ID.
     *
     * @param eventId the ID of the event
     * @return the corresponding {@link EventResponseDTO}
     * @throws EventNotFoundException if the event with the given ID doesn't exist
     */
    @GetMapping("/{eventId}")
    public EventResponseDTO getEventById(@PathVariable Long eventId) {
        return eventService.getEventById(eventId);
    }

    /**
     * Retrieves all events created by a specific user.
     *
     * @param userId the ID of the user
     * @return a list of {@link EventResponseDTO}
     * @throws EventNotFoundException if no events are found for the given user
     */
    @GetMapping("/users/{userId}")
    public List<EventResponseDTO> getEventsByUser(@PathVariable Long userId) {
        return eventService.getEventsByUser(userId);
    }

    /**
     * Retrieves events created by a specific user within a given date range.
     *
     * @param userId the ID of the user
     * @param start  the start time (inclusive)
     * @param end    the end time (inclusive)
     * @return a list of {@link EventResponseDTO} created by the specified user within the given range
     * @throws EventNotFoundException if no events are found for the given user within the range
     */
    @GetMapping(value = "/users/{userId}", params = {"start", "end"})
    public List<EventResponseDTO> getEventsByUserAndDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime end
    ) {
        return eventService.getEventsByUserAndDateRange(userId, start, end);
    }

    /**
     * Retrieves all events created by the currently authenticated user.
     * Authentication is handled by the service layer.
     *
     * @return a list of {@link EventResponseDTO} belonging to the current user
     * @throws UnauthorizedException if the user is not authenticated
     */
    @GetMapping("/me")
    public List<EventResponseDTO> getMyEvents() {
        return eventService.getEventsByCurrentUser();
    }

    /**
     * Retrieves events created by the currently authenticated user within a given date range.
     * Authentication is handled by the service layer.
     *
     * @param start the start time (inclusive)
     * @param end   the end time (inclusive)
     * @return a list of {@link EventResponseDTO} in the given date range
     * @throws UnauthorizedException if the user is not authenticated
     */
    @GetMapping(value = "/me", params = {"start", "end"})
    public List<EventResponseDTO> getMyEventsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime end
    ) {
        return eventService.getEventsByCurrentUserAndDateRange(start, end);
    }

    /**
     * Creates a new event for the currently authenticated user.
     * Authentication is handled by the service layer.
     *
     * @param dto the request body containing event creation data
     * @return the created {@link EventResponseDTO}
     * @throws UnauthorizedException if the user is not authenticated
     * @throws ConflictException if there is a conflict with the event's time or schedule
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponseDTO createEvent(@Valid @RequestBody EventCreateDTO dto) {
        return eventService.createEvent(dto);
    }

    /**
     * Updates an existing event with the specified ID using partial update semantics.
     * Only the creator of the event is authorized to perform this update.
     * Authorization is enforced by the service layer.
     *
     * @param id  the ID of the event to update
     * @param dto the request body containing fields to update
     * @return the updated {@link EventResponseDTO}
     * @throws UnauthorizedException if the user is not authorized to update the event
     * @throws EventNotFoundException if the event with the given ID does not exist
     * @throws ConflictException if there is a conflict with the event's time or schedule
     */
    @PatchMapping("/{id}")
    public EventResponseDTO updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventUpdateDTO dto
    ) {
        return eventService.updateEvent(id, dto);
    }

    /**
     * Deletes the specified event, if the authenticated user is its owner.
     * Authorization is enforced by the service layer.
     *
     * @param id the ID of the event to delete
     * @throws UnauthorizedException if the user is not authorized to delete the event
     * @throws EventNotFoundException if the event with the given ID does not exist
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
    }
}
