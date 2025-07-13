package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.EventCreateDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.dto.EventRecapCreateDTO;
import com.yohan.event_planner.dto.EventRecapResponseDTO;
import com.yohan.event_planner.dto.EventRecapUpdateDTO;
import com.yohan.event_planner.service.EventService;
import com.yohan.event_planner.service.EventRecapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for core event management operations.
 * 
 * <p>This controller provides comprehensive event lifecycle management by serving as the
 * API gateway for event-related operations. It orchestrates event creation, updates, deletion,
 * and state management through integration with the service layer, while also handling 
 * event recap functionality for completed events. All endpoints require JWT authentication 
 * and respect user ownership boundaries.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>Event CRUD Operations</strong>: Complete lifecycle management of events</li>
 *   <li><strong>Event State Management</strong>: Draft/confirmed transitions and completion</li>
 *   <li><strong>Event Recap Management</strong>: Post-event documentation and media</li>
 *   <li><strong>Ownership Validation</strong>: Ensures users only access their own events</li>
 * </ul>
 * 
 * <h2>Architectural Role</h2>
 * <p>As a controller layer component, this class:</p>
 * <ul>
 *   <li><strong>Request Processing</strong>: Handles HTTP requests and parameter validation</li>
 *   <li><strong>Response Formation</strong>: Transforms service results into HTTP responses</li>
 *   <li><strong>Security Integration</strong>: Enforces authentication and ownership validation</li>
 *   <li><strong>API Documentation</strong>: Provides OpenAPI/Swagger documentation</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <ul>
 *   <li><strong>EventService</strong>: Primary service for event operations and state management</li>
 *   <li><strong>EventRecapService</strong>: Specialized service for post-event recap functionality</li>
 *   <li><strong>Security Framework</strong>: JWT-based authentication and authorization</li>
 *   <li><strong>Validation Framework</strong>: Jakarta Bean Validation for request validation</li>
 * </ul>
 * 
 * <h2>Security Model</h2>
 * <ul>
 *   <li><strong>Authentication Required</strong>: All endpoints require valid JWT tokens</li>
 *   <li><strong>User Isolation</strong>: Users only access their own events and recaps</li>
 *   <li><strong>Ownership Validation</strong>: Service layer enforces ownership for all operations</li>
 * </ul>
 * 
 * @see EventService
 * @see EventRecapService
 * @see EventResponseDTO
 * @see EventRecapResponseDTO
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
@Tag(name = "Events", description = "Core event management operations")
@RestController
@RequestMapping("/events")
@SecurityRequirement(name = "Bearer Authentication")
@Validated
public class EventController {

    private static final Logger logger = LoggerFactory.getLogger(EventController.class);
    
    private final EventService eventService;
    private final EventRecapService eventRecapService;

    /**
     * Constructs a new EventController with the specified event and recap services.
     * 
     * <p>This constructor establishes the primary dependencies for event and recap operations,
     * enabling the controller to delegate all business logic to the service layer while 
     * maintaining proper separation of concerns. The controller serves as an API gateway,
     * handling HTTP concerns and delegating domain logic appropriately.</p>
     * 
     * @param eventService the service for core event operations including CRUD, state management,
     *                    and ownership validation
     * @param eventRecapService the service for event recap operations including creation, updates,
     *                         and confirmation of post-event documentation
     * @throws IllegalArgumentException if either service is null (handled by Spring)
     */
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
        logger.info("Creating new event: {} (draft: {})", dto.name(), dto.isDraft());
        EventResponseDTO response = eventService.createEvent(dto);
        logger.info("Successfully created event with ID: {}", response.id());
        return response;
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
            @PathVariable @Min(1) Long id) {
        logger.debug("Retrieving event ID: {} for user", id);
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
            @PathVariable @Min(1) Long id, 
            @Parameter(description = "Event update data", required = true)
            @RequestBody @Valid EventUpdateDTO dto) {
        logger.debug("Updating event ID: {} for user", id);
        EventResponseDTO response = eventService.updateEvent(id, dto);
        logger.info("Successfully updated event ID: {}", id);
        return response;
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
            @PathVariable @Min(1) Long id) {
        logger.info("Deleting event ID: {}", id);
        eventService.deleteEvent(id);
        logger.info("Successfully deleted event ID: {}", id);
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
            @PathVariable @Min(1) Long id) {
        logger.info("Confirming event ID: {}", id);
        EventResponseDTO response = eventService.confirmEvent(id);
        logger.info("Successfully confirmed event ID: {}", id);
        return response;
    }

    @Operation(
            summary = "Create impromptu event",
            description = "Create a new impromptu event with current timestamp. The event is automatically pinned for the user as a dashboard reminder."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201", 
                    description = "Impromptu event created successfully and automatically pinned",
                    content = @Content(schema = @Schema(implementation = EventResponseDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    @PostMapping("/impromptu")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponseDTO createImpromptuEvent() {
        logger.info("Creating impromptu event");
        EventResponseDTO response = eventService.createImpromptuEvent();
        logger.info("Successfully created impromptu event with ID: {}", response.id());
        return response;
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
            @PathVariable @Min(1) Long id) {
        logger.debug("Retrieving recap for event ID: {}", id);
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
            @PathVariable @Min(1) Long id, 
            @Parameter(description = "Event recap creation data", required = true)
            @RequestBody @Valid EventRecapCreateDTO dto) {
        logger.info("Creating recap for event ID: {} (draft: {})", id, dto.isUnconfirmed());
        EventRecapResponseDTO response = eventRecapService.addEventRecap(dto);
        logger.info("Successfully created recap for event ID: {}", id);
        return response;
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
            @PathVariable @Min(1) Long id, 
            @Parameter(description = "Event recap update data", required = true)
            @RequestBody @Valid EventRecapUpdateDTO dto) {
        logger.debug("Updating recap for event ID: {}", id);
        EventRecapResponseDTO response = eventRecapService.updateEventRecap(id, dto);
        logger.info("Successfully updated recap for event ID: {}", id);
        return response;
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
            @PathVariable @Min(1) Long id) {
        logger.info("Deleting recap for event ID: {}", id);
        eventRecapService.deleteEventRecap(id);
        logger.info("Successfully deleted recap for event ID: {}", id);
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
            @PathVariable @Min(1) Long id) {
        logger.info("Confirming recap for event ID: {}", id);
        EventRecapResponseDTO response = eventRecapService.confirmEventRecap(id);
        logger.info("Successfully confirmed recap for event ID: {}", id);
        return response;
    }

    // endregion
}
