package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.DraftsResponseDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.MyEventsResponseDTO;
import com.yohan.event_planner.dto.RecurringEventResponseDTO;
import com.yohan.event_planner.service.MyEventsService;
import com.yohan.event_planner.service.EventService;
import com.yohan.event_planner.service.RecurringEventService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.yohan.event_planner.exception.InvalidCalendarParameterException;
import com.yohan.event_planner.exception.ErrorCode;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * REST controller providing personalized event access endpoints for authenticated users.
 * 
 * <p>This controller serves as the primary interface for the "My Events" dashboard functionality,
 * offering specialized views and operations for user-owned events and drafts. It orchestrates
 * calls to multiple service layers to provide unified responses for complex UI requirements.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>Event Aggregation</strong>: Combines confirmed events and recurring events</li>
 *   <li><strong>Draft Management</strong>: Provides access to unconfirmed event drafts</li>
 *   <li><strong>Bulk Operations</strong>: Supports batch deletion of draft events</li>
 *   <li><strong>Cursor Pagination</strong>: Optimized for infinite scrolling interfaces</li>
 * </ul>
 * 
 * <h2>Security Context</h2>
 * <p>All endpoints require JWT authentication and automatically scope operations to the 
 * current user context. No explicit user ID parameters are needed as user identity is 
 * extracted from the security context.</p>
 * 
 * <h2>API Design Patterns</h2>
 * <ul>
 *   <li><strong>Resource-Based URLs</strong>: RESTful design following /myevents pattern</li>
 *   <li><strong>Consistent Response DTOs</strong>: Standardized response structures</li>
 *   <li><strong>Cursor-Based Pagination</strong>: Performance-optimized pagination</li>
 *   <li><strong>Bulk Operations</strong>: Efficient batch processing for drafts</li>
 * </ul>
 * 
 * <h2>Service Layer Integration</h2>
 * <p>This controller follows a hybrid delegation pattern:</p>
 * <ul>
 *   <li><strong>MyEventsService</strong>: For complex paginated queries of confirmed events</li>
 *   <li><strong>EventService</strong>: For direct CRUD operations on individual events</li>
 *   <li><strong>RecurringEventService</strong>: For direct CRUD operations on recurring events</li>
 * </ul>
 * 
 * @see MyEventsService
 * @see EventService  
 * @see RecurringEventService
 * @see MyEventsResponseDTO
 * @see DraftsResponseDTO
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
@Tag(name = "My Events", description = "Personalized event views and draft management")
@RestController
@RequestMapping("/myevents")
@SecurityRequirement(name = "Bearer Authentication")
public class MyEventsController {

    private static final Logger logger = LoggerFactory.getLogger(MyEventsController.class);

    private final MyEventsService myEventsService;
    private final EventService eventService;
    private final RecurringEventService recurringEventService;

    /**
     * Constructs a new MyEventsController with required service dependencies.
     * 
     * @param myEventsService the service for paginated event queries
     * @param eventService the service for individual event operations
     * @param recurringEventService the service for recurring event operations
     * @throws NullPointerException if any service dependency is null
     */
    public MyEventsController(
            MyEventsService myEventsService,
            EventService eventService,
            RecurringEventService recurringEventService
    ) {
        this.myEventsService = myEventsService;
        this.eventService = eventService;
        this.recurringEventService = recurringEventService;
        logger.debug("MyEventsController initialized with service dependencies");
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
    /**
     * Retrieves a combined view of confirmed events and recurring events for the current user.
     * 
     * <p>This endpoint supports cursor-based pagination optimized for infinite scrolling
     * interfaces. It combines data from both regular events and recurring events to provide
     * a unified dashboard view. Only confirmed events are included in the response.</p>
     * 
     * <h3>Pagination Strategy</h3>
     * <ul>
     *   <li><strong>Events</strong>: Ordered by endTime DESC, startTime DESC, id DESC</li>
     *   <li><strong>Recurring Events</strong>: Complex multi-field ordering for patterns</li>
     *   <li><strong>Cursor-Based</strong>: Avoids expensive offset calculations</li>
     * </ul>
     * 
     * <h3>Response Structure</h3>
     * <p>Returns a unified response containing both event types with separate arrays
     * for regular events and recurring events, enabling flexible UI rendering.</p>
     * 
     * @param startTimeCursor optional cursor for pagination - filters events after this start time
     * @param endTimeCursor optional cursor for pagination - filters events before this end time  
     * @param limit maximum number of events to return per type, defaults to 20, must be positive
     * @return combined response containing confirmed events and recurring events with pagination support
     * @throws org.springframework.security.access.AccessDeniedException if JWT token is missing or invalid
     * @throws com.yohan.event_planner.exception.InvalidCalendarParameterException if limit parameter is invalid (delegated to service layer)
     */
    @GetMapping
    public MyEventsResponseDTO getMyEvents(
            @Parameter(description = "Cursor for pagination - start time filter")
            @RequestParam(required = false) ZonedDateTime startTimeCursor,
            @Parameter(description = "Cursor for pagination - end time filter")
            @RequestParam(required = false) ZonedDateTime endTimeCursor,
            @Parameter(description = "Maximum number of events to return")
            @RequestParam(required = false) String limit
    ) {
        int finalLimit = validateAndGetLimit(limit);
        
        logger.debug("Processing getMyEvents request with startTimeCursor: {}, endTimeCursor: {}, limit: {}", 
                     startTimeCursor, endTimeCursor, finalLimit);
        
        logger.debug("Retrieving events page from MyEventsService");
        List<EventResponseDTO> events = myEventsService.getEventsPage(
                endTimeCursor,
                startTimeCursor,
                null,
                finalLimit
        );

        logger.debug("Retrieving recurring events page from MyEventsService");
        List<RecurringEventResponseDTO> recurringEvents = myEventsService.getRecurringEventsPage(
                null, null, null, null, null, finalLimit
        );

        logger.info("Retrieved {} events and {} recurring events for user", 
                    events.size(), recurringEvents.size());
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
    /**
     * Retrieves all unconfirmed event and recurring event drafts for the current user.
     * 
     * <p>This endpoint provides access to all draft events that have not yet been confirmed
     * by the user. Draft events are typically created during the event creation process
     * but have not been finalized. This supports a "save draft" workflow for complex
     * event creation scenarios.</p>
     * 
     * <h3>Draft Event Characteristics</h3>
     * <ul>
     *   <li><strong>Unconfirmed Status</strong>: Events with confirmed=false</li>
     *   <li><strong>User-Scoped</strong>: Only drafts belonging to the authenticated user</li>
     *   <li><strong>No Pagination</strong>: Returns all drafts (typically small dataset)</li>
     *   <li><strong>Separate Collections</strong>: Regular and recurring events in separate arrays</li>
     * </ul>
     * 
     * <h3>Use Cases</h3>
     * <ul>
     *   <li><strong>Draft Management</strong>: UI for reviewing and managing saved drafts</li>
     *   <li><strong>Resume Editing</strong>: Returning to incomplete event creation flows</li>
     *   <li><strong>Bulk Operations</strong>: Preparing for mass confirmation or deletion</li>
     * </ul>
     * 
     * @return response containing all draft events and recurring events for the current user
     * @throws org.springframework.security.access.AccessDeniedException if JWT token is missing or invalid
     */
    @GetMapping("/drafts")
    public DraftsResponseDTO getMyDrafts() {
        logger.debug("Processing getMyDrafts request for current user");
        
        logger.debug("Retrieving unconfirmed events from EventService");
        List<EventResponseDTO> eventDrafts = eventService.getUnconfirmedEventsForCurrentUser();
        
        logger.debug("Retrieving unconfirmed recurring events from RecurringEventService");
        List<RecurringEventResponseDTO> recurringEventDrafts = recurringEventService.getUnconfirmedRecurringEventsForCurrentUser();

        logger.info("Retrieved {} event drafts and {} recurring event drafts for user", 
                    eventDrafts.size(), recurringEventDrafts.size());
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
    /**
     * Permanently deletes all unconfirmed event and recurring event drafts for the current user.
     * 
     * <p>This endpoint provides a bulk deletion operation for clearing all draft events
     * at once. This is an irreversible operation that permanently removes all unconfirmed
     * events from the database. Only events belonging to the authenticated user are affected.</p>
     * 
     * <h3>Operation Scope</h3>
     * <ul>
     *   <li><strong>User-Scoped</strong>: Only affects drafts belonging to the authenticated user</li>
     *   <li><strong>Draft Events Only</strong>: Only deletes events with confirmed=false</li>
     *   <li><strong>Both Event Types</strong>: Deletes both regular and recurring event drafts</li>
     *   <li><strong>Permanent Deletion</strong>: Hard delete operation, not soft delete</li>
     * </ul>
     * 
     * <h3>Use Cases</h3>
     * <ul>
     *   <li><strong>Cleanup Operations</strong>: Clearing accumulated draft events</li>
     *   <li><strong>Fresh Start</strong>: Resetting the draft state for new planning sessions</li>
     *   <li><strong>Storage Management</strong>: Removing unused draft data</li>
     * </ul>
     * 
     * <h3>Safety Considerations</h3>
     * <p>This operation cannot be undone. Clients should implement appropriate
     * confirmation dialogs before calling this endpoint.</p>
     * 
     * @throws org.springframework.security.access.AccessDeniedException if JWT token is missing or invalid
     */
    @DeleteMapping("/drafts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllDrafts() {
        logger.debug("Processing deleteAllDrafts request for current user");
        
        logger.debug("Deleting unconfirmed events via EventService");
        eventService.deleteUnconfirmedEventsForCurrentUser();
        
        logger.debug("Deleting unconfirmed recurring events via RecurringEventService");
        recurringEventService.deleteUnconfirmedRecurringEventsForCurrentUser();
        
        logger.info("Successfully deleted all draft events for user");
    }

    private int validateAndGetLimit(String limit) {
        logger.debug("validateAndGetLimit called with limit: '{}'", limit);
        
        if (limit == null) {
            logger.debug("limit is null, returning default value 20");
            return 20; // Default value
        }
        
        if (limit.trim().isEmpty()) {
            logger.debug("limit is empty string, throwing exception");
            throw new InvalidCalendarParameterException(ErrorCode.INVALID_PAGINATION_PARAMETER);
        }
        
        try {
            int parsedLimit = Integer.parseInt(limit.trim());
            if (parsedLimit <= 0) {
                logger.debug("limit is <= 0, throwing exception");
                throw new InvalidCalendarParameterException(ErrorCode.INVALID_PAGINATION_PARAMETER);
            }
            logger.debug("limit validated successfully: {}", parsedLimit);
            return parsedLimit;
        } catch (NumberFormatException e) {
            logger.debug("limit is not a valid number, throwing exception");
            throw new InvalidCalendarParameterException(ErrorCode.INVALID_PAGINATION_PARAMETER);
        }
    }
}
