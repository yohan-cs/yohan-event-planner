package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.RecurringEventCreateDTO;
import com.yohan.event_planner.dto.RecurringEventResponseDTO;
import com.yohan.event_planner.dto.RecurringEventUpdateDTO;
import com.yohan.event_planner.service.RecurringEventService;
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

/**
 * REST controller for managing recurring event patterns and lifecycle operations.
 * 
 * <p>Provides comprehensive endpoints for creating, reading, updating, and deleting recurring events,
 * as well as specialized operations for confirmation workflows and skip day management.
 * All endpoints require JWT authentication and enforce ownership-based access control.</p>
 * 
 * <h2>Security Context</h2>
 * <p>All operations are performed within the context of the authenticated user.
 * Users can only access and modify their own recurring events unless they have
 * administrative privileges. Authentication is enforced via JWT tokens.</p>
 * 
 * <h2>Architectural Role</h2>
 * <p>This controller serves as the HTTP interface layer, handling:</p>
 * <ul>
 *   <li>Request/response mapping and validation</li>
 *   <li>HTTP status code management</li>
 *   <li>OpenAPI documentation integration</li>
 *   <li>Delegation to service layer for business logic</li>
 * </ul>
 * 
 * <h2>Related Components</h2>
 * <p>Integrates with:</p>
 * <ul>
 *   <li>{@link RecurringEventService} - Primary business logic coordination</li>
 *   <li>{@link com.yohan.event_planner.business.RecurringEventBO} - Entity operations</li>
 *   <li>{@link com.yohan.event_planner.domain.RecurringEvent} - Domain model</li>
 *   <li>Spring Security - Authentication and authorization</li>
 * </ul>
 * 
 * @see RecurringEventService
 * @see com.yohan.event_planner.domain.RecurringEvent
 * @see RecurringEventCreateDTO
 * @see RecurringEventResponseDTO
 * @see RecurringEventUpdateDTO
 */
@Tag(name = "Recurring Events", description = "Recurring event patterns and management")
@RestController
@RequestMapping("/recurringevents")
@SecurityRequirement(name = "Bearer Authentication")
public class RecurringEventController {

    private static final Logger logger = LoggerFactory.getLogger(RecurringEventController.class);
    
    private final RecurringEventService recurringEventService;

    /**
     * Constructs a new RecurringEventController with the required service dependency.
     * 
     * @param recurringEventService the service for handling recurring event business operations
     */
    public RecurringEventController(RecurringEventService recurringEventService) {
        this.recurringEventService = recurringEventService;
    }

    // ==============================
    // region Recurring Events
    // ==============================

    /**
     * Creates a new recurring event with the specified recurrence pattern and configuration.
     * 
     * <p>Supports both draft and confirmed recurring events. Draft events allow flexible data entry
     * while confirmed events undergo full validation and conflict checking. The event can include
     * complex recurrence rules, skip days, and label associations.</p>
     * 
     * <p>The authenticated user becomes the owner of the created recurring event and gains
     * full management privileges over the event and its future occurrences.</p>
     * 
     * @param dto the recurring event creation data including name, schedule, recurrence rule,
     *            and optional label assignment
     * @return the created recurring event with generated ID and processed recurrence information
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if validation fails
     * @throws com.yohan.event_planner.exception.InvalidEventStateException if confirmed event validation fails
     * @throws com.yohan.event_planner.exception.ConflictException if scheduling conflicts detected
     */
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
        logger.debug("Creating recurring event, name: {}, isDraft: {}", dto.name(), dto.isDraft());
        
        RecurringEventResponseDTO result = recurringEventService.createRecurringEvent(dto);
        
        logger.info("Successfully created recurring event with ID: {}, name: {}, status: {}", 
                result.id(), result.name(), result.unconfirmed() ? "draft" : "confirmed");
        
        return result;
    }

    /**
     * Retrieves a recurring event by its unique identifier.
     * 
     * <p>Returns detailed information about the recurring event including its recurrence pattern,
     * skip days, label associations, and metadata. Access is restricted to the event owner
     * unless the user has administrative privileges.</p>
     * 
     * @param id the unique identifier of the recurring event to retrieve
     * @return the complete recurring event information including recurrence details
     * @throws com.yohan.event_planner.exception.RecurringEventNotFoundException if no event exists with the given ID
     * @throws com.yohan.event_planner.exception.RecurringEventOwnershipException if user lacks access rights
     */
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
        logger.debug("Retrieving recurring event with ID: {}", id);
        
        RecurringEventResponseDTO result = recurringEventService.getRecurringEventById(id);
        
        logger.debug("Successfully retrieved recurring event: {} ({})", result.name(), result.id());
        
        return result;
    }

    /**
     * Performs partial updates to an existing recurring event.
     * 
     * <p>Supports updating name, schedule, recurrence rules, description, and label associations.
     * Uses Optional field semantics where null fields are ignored and Optional.empty() clears values.
     * For confirmed recurring events, changes only affect future occurrences while preserving
     * historical event instances.</p>
     * 
     * <p>The update operation validates the new configuration and checks for scheduling conflicts
     * if the recurring event is confirmed. Draft events allow more flexible updates with minimal validation.</p>
     * 
     * @param id the unique identifier of the recurring event to update
     * @param dto the update data containing only the fields to be modified
     * @return the updated recurring event with all modifications applied
     * @throws com.yohan.event_planner.exception.RecurringEventNotFoundException if no event exists with the given ID
     * @throws com.yohan.event_planner.exception.RecurringEventOwnershipException if user lacks update rights
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if validation fails
     * @throws com.yohan.event_planner.exception.InvalidEventStateException if confirmed event validation fails
     * @throws com.yohan.event_planner.exception.ConflictException if scheduling conflicts detected
     */
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
        logger.debug("Updating recurring event with ID: {}", id);
        
        RecurringEventResponseDTO result = recurringEventService.updateRecurringEvent(id, dto);
        
        logger.info("Successfully updated recurring event: {} (ID: {})", result.name(), result.id());
        
        return result;
    }

    /**
     * Permanently deletes a recurring event and all associated future event instances.
     * 
     * <p>This operation completely removes the recurring event pattern and cancels all
     * future occurrences that haven't occurred yet. Past event instances may be preserved
     * depending on system configuration. This action cannot be undone.</p>
     * 
     * <p>Only the event owner or administrators can delete recurring events. All associated
     * skip days, label associations, and metadata are also removed during deletion.</p>
     * 
     * @param id the unique identifier of the recurring event to delete
     * @throws com.yohan.event_planner.exception.RecurringEventNotFoundException if no event exists with the given ID
     * @throws com.yohan.event_planner.exception.RecurringEventOwnershipException if user lacks deletion rights
     */
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
        logger.debug("Deleting recurring event with ID: {}", id);
        
        recurringEventService.deleteRecurringEvent(id);
        
        logger.info("Successfully deleted recurring event with ID: {}", id);
    }

    /**
     * Confirms a draft recurring event, transitioning it to active status.
     * 
     * <p>Confirmation activates a draft recurring event by applying full validation, conflict checking,
     * and making it visible in calendar views and analytics. Once confirmed, the recurring event
     * can generate future event instances and becomes subject to stricter update rules.</p>
     * 
     * <p>The confirmation process validates the recurrence pattern, checks for scheduling conflicts,
     * and ensures all required fields are properly configured. Only draft (unconfirmed) events
     * can be confirmed through this operation.</p>
     * 
     * @param id the unique identifier of the draft recurring event to confirm
     * @return the confirmed recurring event with updated status and validation results
     * @throws com.yohan.event_planner.exception.RecurringEventNotFoundException if no event exists with the given ID
     * @throws com.yohan.event_planner.exception.RecurringEventOwnershipException if user lacks confirmation rights
     * @throws com.yohan.event_planner.exception.RecurringEventAlreadyConfirmedException if the event is already confirmed
     * @throws com.yohan.event_planner.exception.InvalidEventStateException if validation fails during confirmation
     * @throws com.yohan.event_planner.exception.ConflictException if scheduling conflicts are detected
     */
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
        logger.debug("Confirming recurring event with ID: {}", id);
        
        RecurringEventResponseDTO result = recurringEventService.confirmRecurringEvent(id);
        
        logger.info("Successfully confirmed recurring event: {} (ID: {})", result.name(), result.id());
        
        return result;
    }

    // ==============================
    // region Skip Days Management
    // ==============================

    /**
     * Adds specific dates to the skip days collection for a recurring event.
     * 
     * <p>Skip days are dates where the recurrence pattern would normally generate an event
     * occurrence, but should be excluded. This is useful for holiday exclusions, vacation periods,
     * one-time schedule conflicts, or temporary pattern modifications. Skip days can be added
     * retroactively and will affect future event generation.</p>
     * 
     * <p>The provided dates are merged with any existing skip days. Duplicate dates are
     * automatically handled without causing errors. All dates are validated to ensure they
     * fall within the recurring event's active date range.</p>
     * 
     * @param id the unique identifier of the recurring event to modify
     * @param skipDays the set of dates to exclude from the recurrence pattern
     * @return the updated recurring event with the new skip days included
     * @throws com.yohan.event_planner.exception.RecurringEventNotFoundException if no event exists with the given ID
     * @throws com.yohan.event_planner.exception.RecurringEventOwnershipException if user lacks modification rights
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if skip days data is invalid
     */
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
        logger.debug("Adding {} skip days to recurring event with ID: {}", skipDays.size(), id);
        
        RecurringEventResponseDTO result = recurringEventService.addSkipDays(id, skipDays);
        
        logger.info("Successfully added skip days to recurring event: {} (ID: {}), total skip days: {}", 
                result.name(), result.id(), result.skipDays().size());
        
        return result;
    }

    /**
     * Removes specific dates from the skip days collection for a recurring event.
     * 
     * <p>Allows previously excluded dates to be included in the recurrence pattern again.
     * This effectively "un-skips" dates that were previously excluded, allowing the pattern
     * to generate events on those dates in the future. The operation includes conflict validation
     * to ensure that restoring these dates won't create scheduling conflicts.</p>
     * 
     * <p>Only dates that are currently in the skip days collection can be removed. Attempting
     * to remove dates that aren't skipped is safely ignored. Conflict validation ensures that
     * restoring occurrences won't violate scheduling constraints.</p>
     * 
     * @param id the unique identifier of the recurring event to modify
     * @param skipDays the set of dates to remove from the skip days collection
     * @return the updated recurring event with the specified skip days removed
     * @throws com.yohan.event_planner.exception.RecurringEventNotFoundException if no event exists with the given ID
     * @throws com.yohan.event_planner.exception.RecurringEventOwnershipException if user lacks modification rights
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if skip days data is invalid
     * @throws com.yohan.event_planner.exception.ConflictException if removing skip days would create scheduling conflicts
     */
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
        logger.debug("Removing {} skip days from recurring event with ID: {}", skipDays.size(), id);
        
        RecurringEventResponseDTO result = recurringEventService.removeSkipDays(id, skipDays);
        
        logger.info("Successfully removed skip days from recurring event: {} (ID: {}), remaining skip days: {}", 
                result.name(), result.id(), result.skipDays().size());
        
        return result;
    }


    // endregion
}
