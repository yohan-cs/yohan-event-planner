package com.yohan.event_planner.controller;

import com.yohan.event_planner.domain.enums.TimeFilter;
import com.yohan.event_planner.dto.EventFilterDTO;
import com.yohan.event_planner.dto.RecurringEventFilterDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.RecurringEventResponseDTO;
import com.yohan.event_planner.service.EventService;
import com.yohan.event_planner.service.RecurringEventService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final EventService eventService;
    private final RecurringEventService recurringEventService;

    public SearchController(EventService eventService, RecurringEventService recurringEventService) {
        this.eventService = eventService;
        this.recurringEventService = recurringEventService;
    }

    // Search for regular events
    @GetMapping("/events")
    public ResponseEntity<Page<EventResponseDTO>> searchEvents(
            @RequestParam(required = false) Long labelId,
            @RequestParam(required = false) TimeFilter timeFilter,
            @RequestParam(required = false) ZonedDateTime start,
            @RequestParam(required = false) ZonedDateTime end,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Boolean sortDescending,
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

    // Search for recurring events
    @GetMapping("/recurringevents")
    public ResponseEntity<Page<RecurringEventResponseDTO>> searchRecurringEvents(
            @RequestParam(required = false) Long labelId,
            @RequestParam(required = false) TimeFilter timeFilter,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize,
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

