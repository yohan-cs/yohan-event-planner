package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.RecurringEventCreateDTO;
import com.yohan.event_planner.dto.RecurringEventResponseDTO;
import com.yohan.event_planner.dto.RecurringEventUpdateDTO;
import com.yohan.event_planner.service.RecurringEventService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Set;

@Tag(name = "Recurring Events", description = "Recurring event patterns and management")
@RestController
@RequestMapping("/recurringevents")
@SecurityRequirement(name = "Bearer Authentication")
public class RecurringEventController {

    private final RecurringEventService recurringEventService;

    public RecurringEventController(RecurringEventService recurringEventService) {
        this.recurringEventService = recurringEventService;
    }

    // ==============================
    // region Recurring Events
    // ==============================

    @Operation(
            summary = "Create a new recurring event",
            description = "Create a new recurring event with complex recurrence patterns including RRule support and infinite recurrence"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201", 
                    description = "Recurring event created successfully",
                    content = @Content(schema = @Schema(implementation = RecurringEventResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringEventResponseDTO createRecurringEvent(
            @Parameter(description = "Recurring event creation data", required = true)
            @RequestBody @Valid RecurringEventCreateDTO dto) {
        return recurringEventService.createRecurringEvent(dto);
    }

    @Operation(
            summary = "Get recurring event by ID",
            description = "Retrieve detailed information about a specific recurring event including recurrence rules and skip days"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Recurring event retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RecurringEventResponseDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the recurring event owner"),
            @ApiResponse(responseCode = "404", description = "Recurring event not found")
    })
    @GetMapping("/{id}")
    public RecurringEventResponseDTO getRecurringEvent(
            @Parameter(description = "Recurring event ID", required = true)
            @PathVariable Long id) {
        return recurringEventService.getRecurringEventById(id);
    }

    @Operation(
            summary = "Update an existing recurring event",
            description = "Perform partial updates to a recurring event including recurrence rules, scheduling, and labels"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Recurring event updated successfully",
                    content = @Content(schema = @Schema(implementation = RecurringEventResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid input data (validation failure)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the recurring event owner"),
            @ApiResponse(responseCode = "404", description = "Recurring event not found")
    })
    @PatchMapping("/{id}")
    public RecurringEventResponseDTO updateRecurringEvent(
            @Parameter(description = "Recurring event ID", required = true)
            @PathVariable Long id, 
            @Parameter(description = "Recurring event update data", required = true)
            @RequestBody @Valid RecurringEventUpdateDTO dto) {
        return recurringEventService.updateRecurringEvent(id, dto);
    }

    @Operation(
            summary = "Delete a recurring event",
            description = "Permanently delete a recurring event and all associated data"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Recurring event deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the recurring event owner"),
            @ApiResponse(responseCode = "404", description = "Recurring event not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecurringEvent(
            @Parameter(description = "Recurring event ID", required = true)
            @PathVariable Long id) {
        recurringEventService.deleteRecurringEvent(id);
    }

    @Operation(
            summary = "Confirm a recurring event",
            description = "Convert a draft recurring event to confirmed status, making it visible in calendar views and analytics"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Recurring event confirmed successfully",
                    content = @Content(schema = @Schema(implementation = RecurringEventResponseDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the recurring event owner"),
            @ApiResponse(responseCode = "404", description = "Recurring event not found"),
            @ApiResponse(responseCode = "409", description = "Recurring event already confirmed")
    })
    @PostMapping("/{id}/confirm")
    public RecurringEventResponseDTO confirmRecurringEvent(
            @Parameter(description = "Recurring event ID", required = true)
            @PathVariable Long id) {
        return recurringEventService.confirmRecurringEvent(id);
    }

    // ==============================
    // region Skip Days Management
    // ==============================

    @Operation(
            summary = "Add skip days to recurring event",
            description = "Add specific dates to skip in the recurring event pattern"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Skip days added successfully",
                    content = @Content(schema = @Schema(implementation = RecurringEventResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid skip days data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the recurring event owner"),
            @ApiResponse(responseCode = "404", description = "Recurring event not found")
    })
    @PostMapping("/{id}/skipdays")
    public RecurringEventResponseDTO addSkipDays(
            @Parameter(description = "Recurring event ID", required = true)
            @PathVariable Long id, 
            @Parameter(description = "Set of dates to skip", required = true)
            @RequestBody Set<LocalDate> skipDays) {
        return recurringEventService.addSkipDays(id, skipDays);
    }

    @Operation(
            summary = "Remove skip days from recurring event",
            description = "Remove specific skip dates from the recurring event pattern, allowing events to occur on those dates again"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Skip days removed successfully",
                    content = @Content(schema = @Schema(implementation = RecurringEventResponseDTO.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid skip days data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not the recurring event owner"),
            @ApiResponse(responseCode = "404", description = "Recurring event not found")
    })
    @DeleteMapping("/{id}/skipdays")
    public RecurringEventResponseDTO removeSkipDays(
            @Parameter(description = "Recurring event ID", required = true)
            @PathVariable Long id, 
            @Parameter(description = "Set of skip dates to remove", required = true)
            @RequestBody Set<LocalDate> skipDays) {
        return recurringEventService.removeSkipDays(id, skipDays);
    }


    // endregion
}
