package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.EventCreateDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.exception.ConflictException;
import com.yohan.event_planner.exception.EventNotFoundException;
import com.yohan.event_planner.exception.UnauthorizedException;
import com.yohan.event_planner.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@SecurityRequirement(name = "Bearer Authentication")
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
    @Operation(summary = "Retrieve an event by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event found"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
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
    @Operation(summary = "Retrieve all events created by a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Events retrieved"),
            @ApiResponse(responseCode = "404", description = "No events found for user")
    })
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
    @Operation(summary = "Retrieve events by user and date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Events retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid date/time format in request parameters"),
            @ApiResponse(responseCode = "404", description = "No events found in date range")
    })
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
    @Operation(summary = "Retrieve events created by the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Events retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
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
    @Operation(summary = "Retrieve current user's events within a date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Events retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid date/time format in request parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
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
    @Operation(summary = "Create a new event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Event created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "409", description = "Conflict with existing event")
    })
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
    @Operation(summary = "Update an existing event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Event not found"),
            @ApiResponse(responseCode = "409", description = "Conflict with existing event")
    })
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
    @Operation(summary = "Delete an event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Event deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
    }
}
