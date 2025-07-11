package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.EventCreateDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.dto.EventRecapCreateDTO;
import com.yohan.event_planner.dto.EventRecapResponseDTO;
import com.yohan.event_planner.dto.EventRecapUpdateDTO;
import com.yohan.event_planner.service.EventService;
import com.yohan.event_planner.service.EventRecapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Events", description = "Core event management operations")
@RestController
@RequestMapping("/events")
@SecurityRequirement(name = "Bearer Authentication")
public class EventController {

    private final EventService eventService;
    private final EventRecapService eventRecapService;

    public EventController(EventService eventService, EventRecapService eventRecapService) {
        this.eventService = eventService;
        this.eventRecapService = eventRecapService;
    }

    // ==============================
    // region Events
    // ==============================

    @Operation(
            summary = "Create a new event",
            description = "Create a new event with scheduling details. Events can be scheduled, impromptu, or untimed. Supports multi-timezone scheduling."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201", 
                    description = "Event created successfully",
                    content = @Content(schema = @Schema(implementation = EventResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponseDTO createEvent(
            @Parameter(description = "Event creation data", required = true)
            @RequestBody @Valid EventCreateDTO dto) {
        return eventService.createEvent(dto);
    }

    @Operation(
            summary = "Get event by ID",
            description = "Retrieve detailed information about a specific event including scheduling, labels, and completion status"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Event retrieved successfully",
                    content = @Content(schema = @Schema(implementation = EventResponseDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the event owner"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @GetMapping("/{id}")
    public EventResponseDTO getEvent(
            @Parameter(description = "Event ID", required = true)
            @PathVariable Long id) {
        return eventService.getEventById(id);
    }

    @Operation(
            summary = "Update an existing event",
            description = "Perform partial updates to an event including scheduling changes, label assignments, and completion status"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Event updated successfully",
                    content = @Content(schema = @Schema(implementation = EventResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the event owner"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @PatchMapping("/{id}")
    public EventResponseDTO updateEvent(
            @Parameter(description = "Event ID", required = true)
            @PathVariable Long id, 
            @Parameter(description = "Event update data", required = true)
            @RequestBody @Valid EventUpdateDTO dto) {
        return eventService.updateEvent(id, dto);
    }

    @Operation(
            summary = "Delete an event",
            description = "Permanently delete an event and all associated data including recaps and media"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Event deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the event owner"),
            @ApiResponse(responseCode = "404", description = "Event not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(
            @Parameter(description = "Event ID", required = true)
            @PathVariable Long id) {
        eventService.deleteEvent(id);
    }

    @Operation(
            summary = "Confirm an event",
            description = "Convert a draft event to confirmed status, making it visible in calendar views and analytics"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Event confirmed successfully",
                    content = @Content(schema = @Schema(implementation = EventResponseDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the event owner"),
            @ApiResponse(responseCode = "404", description = "Event not found"),
            @ApiResponse(responseCode = "409", description = "Event already confirmed")
    })
    @PostMapping("/{id}/confirm")
    public EventResponseDTO confirmEvent(
            @Parameter(description = "Event ID", required = true)
            @PathVariable Long id) {
        return eventService.confirmEvent(id);
    }

    // ==============================
    // region Event Recaps
    // ==============================

    @Operation(
            summary = "Get event recap",
            description = "Retrieve the recap information for a specific event including notes and media attachments"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Event recap retrieved successfully",
                    content = @Content(schema = @Schema(implementation = EventRecapResponseDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the event owner"),
            @ApiResponse(responseCode = "404", description = "Event or recap not found")
    })
    @GetMapping("/{id}/recap")
    public EventRecapResponseDTO getEventRecap(
            @Parameter(description = "Event ID", required = true)
            @PathVariable Long id) {
        return eventRecapService.getEventRecap(id);
    }

    @Operation(
            summary = "Create event recap",
            description = "Create a recap for a specific event with notes and media attachments"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201", 
                    description = "Event recap created successfully",
                    content = @Content(schema = @Schema(implementation = EventRecapResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the event owner"),
            @ApiResponse(responseCode = "404", description = "Event not found"),
            @ApiResponse(responseCode = "409", description = "Event recap already exists")
    })
    @PostMapping("/{id}/recap")
    @ResponseStatus(HttpStatus.CREATED)
    public EventRecapResponseDTO createEventRecap(
            @Parameter(description = "Event ID", required = true)
            @PathVariable Long id, 
            @Parameter(description = "Event recap creation data", required = true)
            @RequestBody @Valid EventRecapCreateDTO dto) {
        return eventRecapService.addEventRecap(dto);
    }

    @Operation(
            summary = "Update event recap",
            description = "Update the recap for a specific event including notes and media attachments"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Event recap updated successfully",
                    content = @Content(schema = @Schema(implementation = EventRecapResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the event owner"),
            @ApiResponse(responseCode = "404", description = "Event or recap not found")
    })
    @PatchMapping("/{id}/recap")
    public EventRecapResponseDTO updateEventRecap(
            @Parameter(description = "Event ID", required = true)
            @PathVariable Long id, 
            @Parameter(description = "Event recap update data", required = true)
            @RequestBody @Valid EventRecapUpdateDTO dto) {
        return eventRecapService.updateEventRecap(id, dto);
    }

    @Operation(
            summary = "Delete event recap",
            description = "Delete the recap for a specific event including all associated media attachments"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Event recap deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the event owner"),
            @ApiResponse(responseCode = "404", description = "Event or recap not found")
    })
    @DeleteMapping("/{id}/recap")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEventRecap(
            @Parameter(description = "Event ID", required = true)
            @PathVariable Long id) {
        eventRecapService.deleteEventRecap(id);
    }

    @Operation(
            summary = "Confirm event recap",
            description = "Convert a draft event recap to confirmed status, making it visible in analytics and reports"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Event recap confirmed successfully",
                    content = @Content(schema = @Schema(implementation = EventRecapResponseDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the event owner"),
            @ApiResponse(responseCode = "404", description = "Event or recap not found"),
            @ApiResponse(responseCode = "409", description = "Event recap already confirmed")
    })
    @PostMapping("/{id}/recap/confirm")
    public EventRecapResponseDTO confirmEventRecap(
            @Parameter(description = "Event ID", required = true)
            @PathVariable Long id) {
        return eventRecapService.confirmEventRecap(id);
    }

    // endregion
}
