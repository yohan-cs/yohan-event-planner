package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.RecurringEventCreateDTO;
import com.yohan.event_planner.dto.RecurringEventResponseDTO;
import com.yohan.event_planner.dto.RecurringEventUpdateDTO;
import com.yohan.event_planner.service.RecurringEventService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Set;

@RestController
@RequestMapping("/recurringevents")
public class RecurringEventController {

    private final RecurringEventService recurringEventService;

    public RecurringEventController(RecurringEventService recurringEventService) {
        this.recurringEventService = recurringEventService;
    }

    // ==============================
    // region Recurring Events
    // ==============================

    /**
     * POST /recurringevents
     * Creates a new recurring event.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringEventResponseDTO createRecurringEvent(@RequestBody @Valid RecurringEventCreateDTO dto) {
        return recurringEventService.createRecurringEvent(dto);
    }

    /**
     * GET /recurringevents/{id}
     * Retrieves a specific recurring event.
     */
    @GetMapping("/{id}")
    public RecurringEventResponseDTO getRecurringEvent(@PathVariable Long id) {
        return recurringEventService.getRecurringEventById(id);
    }

    /**
     * PATCH /recurringevents/{id}
     * Updates an existing recurring event.
     */
    @PatchMapping("/{id}")
    public RecurringEventResponseDTO updateRecurringEvent(@PathVariable Long id, @RequestBody @Valid RecurringEventUpdateDTO dto) {
        return recurringEventService.updateRecurringEvent(id, dto);
    }

    /**
     * DELETE /recurringevents/{id}
     * Deletes a recurring event.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecurringEvent(@PathVariable Long id) {
        recurringEventService.deleteRecurringEvent(id);
    }

    /**
     * POST /recurringevents/{id}/confirm
     * Confirms a recurring event.
     */
    @PostMapping("/{id}/confirm")
    public RecurringEventResponseDTO confirmRecurringEvent(@PathVariable Long id) {
        return recurringEventService.confirmRecurringEvent(id);
    }

    // ==============================
    // region Skip Days Management
    // ==============================

    /**
     * POST /recurringevents/{id}/skipdays
     * Adds skip days to the recurring event.
     */
    @PostMapping("/{id}/skipdays")
    public RecurringEventResponseDTO addSkipDays(@PathVariable Long id, @RequestBody Set<LocalDate> skipDays) {
        return recurringEventService.addSkipDays(id, skipDays);
    }

    /**
     * DELETE /recurringevents/{id}/skipdays
     * Removes skip days from the recurring event.
     */
    @DeleteMapping("/{id}/skipdays")
    public RecurringEventResponseDTO removeSkipDays(@PathVariable Long id, @RequestBody Set<LocalDate> skipDays) {
        return recurringEventService.removeSkipDays(id, skipDays);
    }


    // endregion
}
