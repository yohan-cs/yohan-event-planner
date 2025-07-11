package com.yohan.event_planner.controller;

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
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Tag(name = "Search", description = "Advanced search and filtering capabilities")
@RestController
@RequestMapping("/search")
@SecurityRequirement(name = "Bearer Authentication")
public class SearchController {

    private final EventService eventService;
    private final RecurringEventService recurringEventService;

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
            @Parameter(description = "Page number for pagination")
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Number of events per page")
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "Sort events in descending order")
            @RequestParam(required = false) Boolean sortDescending,
            @Parameter(description = "Include incomplete past events in results")
            @RequestParam(required = false) Boolean includeIncompletePastEvents) {

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
            @Parameter(description = "Page number for pagination")
            @RequestParam(defaultValue = "0") int pageNumber,
            @Parameter(description = "Number of recurring events per page")
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "Sort recurring events in descending order")
            @RequestParam(required = false) Boolean sortDescending) {

        RecurringEventFilterDTO filter = new RecurringEventFilterDTO(
                labelId,
                timeFilter,
                startDate,
                endDate,
                sortDescending
        );

        // Call service to fetch recurring events for current user
        Page<RecurringEventResponseDTO> recurringEvents = recurringEventService.getConfirmedRecurringEventsForCurrentUser(filter, pageNumber, pageSize);
        return ResponseEntity.ok(recurringEvents);
    }
}

