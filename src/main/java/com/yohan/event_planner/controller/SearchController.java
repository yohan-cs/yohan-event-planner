package com.yohan.event_planner.controller;

import com.yohan.event_planner.constants.ApplicationConstants;
import com.yohan.event_planner.domain.enums.TimeFilter;
import com.yohan.event_planner.dto.EventFilterDTO;
import com.yohan.event_planner.dto.RecurringEventFilterDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.RecurringEventResponseDTO;
import com.yohan.event_planner.service.EventService;
import com.yohan.event_planner.service.RecurringEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * REST controller providing advanced search and filtering capabilities for events and recurring events.
 * 
 * <p>This controller serves as the primary entry point for complex event discovery operations,
 * supporting sophisticated filtering by labels, time ranges, completion status, and pagination.
 * All operations require JWT authentication and automatically scope results to the current user.</p>
 * 
 * <h2>Supported Operations</h2>
 * <ul>
 *   <li><strong>Event Search</strong>: Advanced filtering for confirmed events with time-based constraints</li>
 *   <li><strong>Recurring Event Search</strong>: Pattern-aware filtering for recurring event templates</li>
 * </ul>
 * 
 * <h2>Security Model</h2>
 * <ul>
 *   <li><strong>Authentication Required</strong>: All endpoints require valid JWT token</li>
 *   <li><strong>User Scoping</strong>: Results automatically filtered to current user's events</li>
 *   <li><strong>Ownership Validation</strong>: Label access validated at service layer</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <ul>
 *   <li><strong>EventService</strong>: Delegates event filtering and retrieval operations</li>
 *   <li><strong>RecurringEventService</strong>: Handles recurring event pattern queries</li>
 *   <li><strong>Filter DTOs</strong>: Uses sophisticated filter objects for complex query criteria</li>
 * </ul>
 * 
 * @see EventService
 * @see RecurringEventService
 * @see EventFilterDTO
 * @see RecurringEventFilterDTO
 */
@Tag(name = "Search", description = "Advanced search and filtering capabilities")
@RestController
@RequestMapping("/search")
@SecurityRequirement(name = "Bearer Authentication")
@Validated
public class SearchController {

    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    private final EventService eventService;
    private final RecurringEventService recurringEventService;

    /**
     * Constructs a new SearchController with the required service dependencies.
     * 
     * @param eventService the service for event operations
     * @param recurringEventService the service for recurring event operations
     */
    public SearchController(EventService eventService, RecurringEventService recurringEventService) {
        this.eventService = eventService;
        this.recurringEventService = recurringEventService;
    }

    @Operation(
            summary = "Search for events",
            description = "Search and filter confirmed events for the current user with advanced filtering options including label, time range, and completion status"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Events retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "404", description = "Label not found (if labelId provided)")
    })
    @GetMapping("/events")
    public ResponseEntity<Page<EventResponseDTO>> searchEvents(
            @Parameter(description = "Filter by specific label ID")
            @RequestParam(required = false) Long labelId,
            @Parameter(description = "Time-based filter (past, present, future, etc.)")
            @RequestParam(required = false) TimeFilter timeFilter,
            @Parameter(description = "Start date/time filter")
            @RequestParam(required = false) ZonedDateTime start,
            @Parameter(description = "End date/time filter")
            @RequestParam(required = false) ZonedDateTime end,
            @Parameter(description = "Page number for pagination (0-based)")
            @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
            @Parameter(description = "Number of events per page (1-100)")
            @RequestParam(defaultValue = "10") @Min(ApplicationConstants.MIN_PAGE_SIZE) @Max(ApplicationConstants.MAX_PAGE_SIZE) int pageSize,
            @Parameter(description = "Sort events in descending order")
            @RequestParam(required = false) Boolean sortDescending,
            @Parameter(description = "Include incomplete past events in results")
            @RequestParam(required = false) Boolean includeIncompletePastEvents) {

        logger.debug("Searching events for user with filter: timeFilter={}, labelId={}, pageNumber={}, pageSize={}", 
                timeFilter, labelId, pageNumber, pageSize);

        EventFilterDTO filter = new EventFilterDTO(
                labelId,
                timeFilter,
                start,
                end,
                sortDescending,
                includeIncompletePastEvents
        );

        // Call service to fetch events for current user
        Page<EventResponseDTO> events = eventService.getConfirmedEventsForCurrentUser(filter, pageNumber, pageSize);
        
        logger.info("Successfully retrieved {} events for user search", events.getTotalElements());
        return ResponseEntity.ok(events);
    }

    @Operation(
            summary = "Search for recurring events",
            description = "Search and filter confirmed recurring events for the current user with advanced filtering options including label and date range"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Recurring events retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "404", description = "Label not found (if labelId provided)")
    })
    @GetMapping("/recurringevents")
    public ResponseEntity<Page<RecurringEventResponseDTO>> searchRecurringEvents(
            @Parameter(description = "Filter by specific label ID")
            @RequestParam(required = false) Long labelId,
            @Parameter(description = "Time-based filter (past, present, future, etc.)")
            @RequestParam(required = false) TimeFilter timeFilter,
            @Parameter(description = "Start date filter")
            @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "End date filter")
            @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "Page number for pagination (0-based)")
            @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
            @Parameter(description = "Number of recurring events per page (1-100)")
            @RequestParam(defaultValue = "10") @Min(ApplicationConstants.MIN_PAGE_SIZE) @Max(ApplicationConstants.MAX_PAGE_SIZE) int pageSize,
            @Parameter(description = "Sort recurring events in descending order")
            @RequestParam(required = false) Boolean sortDescending) {

        logger.debug("Searching recurring events for user with filter: timeFilter={}, labelId={}, pageNumber={}, pageSize={}", 
                timeFilter, labelId, pageNumber, pageSize);

        RecurringEventFilterDTO filter = new RecurringEventFilterDTO(
                labelId,
                timeFilter,
                startDate,
                endDate,
                sortDescending
        );

        // Call service to fetch recurring events for current user
        Page<RecurringEventResponseDTO> recurringEvents = recurringEventService.getConfirmedRecurringEventsForCurrentUser(filter, pageNumber, pageSize);
        
        logger.info("Successfully retrieved {} recurring events for user search", recurringEvents.getTotalElements());
        return ResponseEntity.ok(recurringEvents);
    }
}

