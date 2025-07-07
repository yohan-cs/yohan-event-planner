package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.EventCreateDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.dto.EventRecapCreateDTO;
import com.yohan.event_planner.dto.EventRecapResponseDTO;
import com.yohan.event_planner.dto.EventRecapUpdateDTO;
import com.yohan.event_planner.service.EventService;
import com.yohan.event_planner.service.EventRecapService;
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

@RestController
@RequestMapping("/events")
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

    /**
     * POST /events
     * Creates a new event.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponseDTO createEvent(@RequestBody @Valid EventCreateDTO dto) {
        return eventService.createEvent(dto);
    }

    /**
     * GET /events/{id}
     * Retrieves a specific event.
     */
    @GetMapping("/{id}")
    public EventResponseDTO getEvent(@PathVariable Long id) {
        return eventService.getEventById(id);
    }

    /**
     * PATCH /events/{id}
     * Updates an existing event.
     */
    @PatchMapping("/{id}")
    public EventResponseDTO updateEvent(@PathVariable Long id, @RequestBody @Valid EventUpdateDTO dto) {
        return eventService.updateEvent(id, dto);
    }

    /**
     * DELETE /events/{id}
     * Deletes an event.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
    }

    /**
     * POST /events/{id}/confirm
     * Confirms an event.
     */
    @PostMapping("/{id}/confirm")
    public EventResponseDTO confirmEvent(@PathVariable Long id) {
        return eventService.confirmEvent(id);
    }

    // ==============================
    // region Event Recaps
    // ==============================

    /**
     * GET /events/{id}/recap
     * Retrieves the recap for a specific event.
     */
    @GetMapping("/{id}/recap")
    public EventRecapResponseDTO getEventRecap(@PathVariable Long id) {
        return eventRecapService.getEventRecap(id);
    }

    /**
     * POST /events/{id}/recap
     * Creates a recap for a specific event.
     */
    @PostMapping("/{id}/recap")
    @ResponseStatus(HttpStatus.CREATED)
    public EventRecapResponseDTO createEventRecap(@PathVariable Long id, @RequestBody @Valid EventRecapCreateDTO dto) {
        return eventRecapService.addEventRecap(dto);
    }

    /**
     * PATCH /events/{id}/recap
     * Updates the recap for a specific event.
     */
    @PatchMapping("/{id}/recap")
    public EventRecapResponseDTO updateEventRecap(@PathVariable Long id, @RequestBody @Valid EventRecapUpdateDTO dto) {
        return eventRecapService.updateEventRecap(id, dto);
    }

    /**
     * DELETE /events/{id}/recap
     * Deletes the recap for a specific event.
     */
    @DeleteMapping("/{id}/recap")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEventRecap(@PathVariable Long id) {
        eventRecapService.deleteEventRecap(id);
    }

    /**
     * POST /events/{id}/recap/confirm
     * Confirms the recap for a specific event.
     */
    @PostMapping("/{id}/recap/confirm")
    public EventRecapResponseDTO confirmEventRecap(@PathVariable Long id) {
        return eventRecapService.confirmEventRecap(id);
    }

    // endregion
}
