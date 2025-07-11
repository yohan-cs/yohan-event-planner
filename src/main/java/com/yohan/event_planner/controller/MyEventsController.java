package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.DraftsResponseDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.MyEventsResponseDTO;
import com.yohan.event_planner.dto.RecurringEventResponseDTO;
import com.yohan.event_planner.service.MyEventsService;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "My Events", description = "Personalized event views and draft management")
@RestController
@RequestMapping("/myevents")
@SecurityRequirement(name = "Bearer Authentication")
public class MyEventsController {

    private final MyEventsService myEventsService;
    private final EventService eventService;
    private final RecurringEventService recurringEventService;

    public MyEventsController(
            MyEventsService myEventsService,
            EventService eventService,
            RecurringEventService recurringEventService
    ) {
        this.myEventsService = myEventsService;
        this.eventService = eventService;
        this.recurringEventService = recurringEventService;
    }

    @Operation(
            summary = "Get my events",
            description = "Retrieve confirmed events and recurring events for the current user with optional cursor-based pagination"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Events retrieved successfully",
                    content = @Content(schema = @Schema(implementation = MyEventsResponseDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    @GetMapping
    public MyEventsResponseDTO getMyEvents(
            @Parameter(description = "Cursor for pagination - start time filter")
            @RequestParam(required = false) ZonedDateTime startTimeCursor,
            @Parameter(description = "Cursor for pagination - end time filter")
            @RequestParam(required = false) ZonedDateTime endTimeCursor,
            @Parameter(description = "Maximum number of events to return")
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<EventResponseDTO> events = myEventsService.getEventsPage(
                endTimeCursor,
                startTimeCursor,
                null,
                limit
        );

        List<RecurringEventResponseDTO> recurringEvents = myEventsService.getRecurringEventsPage(
                null, null, null, null, null, limit
        );

        return new MyEventsResponseDTO(recurringEvents, events);
    }

    @Operation(
            summary = "Get my draft events",
            description = "Retrieve all unconfirmed event and recurring event drafts for the current user"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Draft events retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DraftsResponseDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    @GetMapping("/drafts")
    public DraftsResponseDTO getMyDrafts() {
        List<EventResponseDTO> eventDrafts = eventService.getUnconfirmedEventsForCurrentUser();
        List<RecurringEventResponseDTO> recurringEventDrafts = recurringEventService.getUnconfirmedRecurringEventsForCurrentUser();

        return new DraftsResponseDTO(eventDrafts, recurringEventDrafts);
    }

    @Operation(
            summary = "Delete all draft events",
            description = "Permanently delete all unconfirmed event and recurring event drafts for the current user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "All draft events deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    @DeleteMapping("/drafts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllDrafts() {
        eventService.deleteUnconfirmedEventsForCurrentUser();
        recurringEventService.deleteUnconfirmedRecurringEventsForCurrentUser();
    }
}
