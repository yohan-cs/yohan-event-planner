package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.DraftsResponseDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.MyEventsResponseDTO;
import com.yohan.event_planner.dto.RecurringEventResponseDTO;
import com.yohan.event_planner.service.MyEventsService;
import com.yohan.event_planner.service.EventService;
import com.yohan.event_planner.service.RecurringEventService;

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

@RestController
@RequestMapping("/myevents")
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

    /**
     * GET /myevents
     * Returns confirmed events and recurring events in separate lists for the current user.
     */
    @GetMapping
    public MyEventsResponseDTO getMyEvents(
            @RequestParam(required = false) ZonedDateTime startTimeCursor,
            @RequestParam(required = false) ZonedDateTime endTimeCursor,
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

    /**
     * GET /myevents/drafts
     * Returns all event and recurring event drafts for the current user, separated.
     */
    @GetMapping("/drafts")
    public DraftsResponseDTO getMyDrafts() {
        List<EventResponseDTO> eventDrafts = eventService.getUnconfirmedEventsForCurrentUser();
        List<RecurringEventResponseDTO> recurringEventDrafts = recurringEventService.getUnconfirmedRecurringEventsForCurrentUser();

        return new DraftsResponseDTO(eventDrafts, recurringEventDrafts);
    }

    /**
     * DELETE /myevents/drafts
     * Deletes all event and recurring event drafts for the current user.
     */
    @DeleteMapping("/drafts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllDrafts() {
        eventService.deleteUnconfirmedEventsForCurrentUser();
        recurringEventService.deleteUnconfirmedRecurringEventsForCurrentUser();
    }
}
