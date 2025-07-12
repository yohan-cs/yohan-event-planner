package com.yohan.event_planner.controller;

import com.yohan.event_planner.dto.LabelMonthStatsDTO;
import com.yohan.event_planner.dto.MonthlyCalendarResponseDTO;
import com.yohan.event_planner.exception.ErrorCode;
import com.yohan.event_planner.exception.InvalidCalendarParameterException;
import com.yohan.event_planner.service.MonthlyCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for calendar views and analytics endpoints.
 * 
 * <p>This controller provides comprehensive monthly calendar functionality by serving as the
 * API gateway for calendar-related operations. It orchestrates monthly calendar view generation,
 * event date retrieval, and label-based analytics through integration with the service layer.
 * All endpoints require JWT authentication and respect user ownership boundaries.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>Monthly Calendar Views</strong>: Generate calendar views for specific months</li>
 *   <li><strong>Event Date Retrieval</strong>: Identify dates with events for calendar highlighting</li>
 *   <li><strong>Label-based Analytics</strong>: Provide time tracking statistics for specific labels</li>
 *   <li><strong>Multi-source Integration</strong>: Aggregate regular and recurring events</li>
 * </ul>
 * 
 * <h2>Architectural Role</h2>
 * <p>As a controller layer component, this class:</p>
 * <ul>
 *   <li><strong>Request Processing</strong>: Handles HTTP requests and parameter validation</li>
 *   <li><strong>Response Formation</strong>: Transforms service results into HTTP responses</li>
 *   <li><strong>Security Integration</strong>: Enforces authentication requirements</li>
 *   <li><strong>API Documentation</strong>: Provides OpenAPI/Swagger documentation</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <ul>
 *   <li><strong>MonthlyCalendarService</strong>: Primary service for calendar operations</li>
 *   <li><strong>Security Framework</strong>: JWT-based authentication and authorization</li>
 *   <li><strong>API Documentation</strong>: OpenAPI/Swagger integration for API docs</li>
 * </ul>
 * 
 * <h2>Security Model</h2>
 * <ul>
 *   <li><strong>Authentication Required</strong>: All endpoints require valid JWT tokens</li>
 *   <li><strong>User Isolation</strong>: Users only access their own calendar data</li>
 *   <li><strong>Label Ownership</strong>: Label-based queries validate ownership</li>
 * </ul>
 * 
 * @see MonthlyCalendarService
 * @see MonthlyCalendarResponseDTO
 * @see LabelMonthStatsDTO
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
@Tag(name = "Calendar", description = "Calendar views and analytics")
@RestController
@RequestMapping("/calendar")
@SecurityRequirement(name = "Bearer Authentication")
public class CalendarController {

    private static final Logger logger = LoggerFactory.getLogger(CalendarController.class);
    
    private final MonthlyCalendarService monthlyCalendarService;

    /**
     * Constructs a new CalendarController with the specified monthly calendar service.
     * 
     * <p>This constructor establishes the primary dependency for calendar operations,
     * enabling the controller to delegate all business logic to the service layer
     * while maintaining proper separation of concerns. The controller serves as an
     * API gateway, handling HTTP concerns and delegating domain logic appropriately.</p>
     * 
     * @param monthlyCalendarService the service for monthly calendar operations and statistics,
     *                              responsible for aggregating events, recurring patterns, and time statistics
     * @throws IllegalArgumentException if monthlyCalendarService is null (handled by Spring)
     */
    public CalendarController(MonthlyCalendarService monthlyCalendarService) {
        this.monthlyCalendarService = monthlyCalendarService;
    }

    /**
     * Retrieves monthly calendar view with optional label filtering and statistics.
     * 
     * <p>This endpoint provides comprehensive monthly calendar data by aggregating events,
     * recurring patterns, and time statistics. When no label filter is applied, returns
     * all event dates for the specified month. When filtered by label, includes both 
     * event dates and detailed monthly statistics for productivity tracking.</p>
     * 
     * <h2>Response Structure</h2>
     * <ul>
     *   <li><strong>Event Dates</strong>: List of ISO 8601 date strings with events</li>
     *   <li><strong>Label Statistics</strong>: Monthly stats when label filter applied</li>
     * </ul>
     * 
     * <h2>Default Parameter Behavior</h2>
     * <ul>
     *   <li><strong>Year</strong>: Defaults to current year if not specified</li>
     *   <li><strong>Month</strong>: Defaults to current month if not specified</li>
     * </ul>
     * 
     * @param labelId optional label ID to filter events and generate statistics
     * @param year optional year (defaults to current year if not specified)  
     * @param month optional month 1-12 (defaults to current month if not specified)
     * @return monthly calendar response with event dates and optional label statistics
     * @throws com.yohan.event_planner.exception.LabelNotFoundException if specified labelId does not exist
     * @throws org.springframework.security.access.AccessDeniedException if user doesn't own the specified label
     */
    @Operation(
            summary = "Get monthly calendar view",
            description = "Retrieve calendar view for a specific month with optional label filtering and time statistics"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", 
                    description = "Monthly calendar view retrieved successfully",
                    content = @Content(schema = @Schema(implementation = MonthlyCalendarResponseDTO.class))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
            @ApiResponse(responseCode = "404", description = "Label not found (if labelId provided)")
    })
    @GetMapping
    public ResponseEntity<MonthlyCalendarResponseDTO> getMonthlyCalendarView(
            @Parameter(description = "Filter by specific label ID (optional). When provided, response includes monthly statistics for the label.")
            @RequestParam(value = "labelId", required = false) Long labelId,
            @Parameter(description = "Year to view (defaults to current year if not specified). Must be a valid 4-digit year.")
            @RequestParam(value = "year", required = false) Integer year,
            @Parameter(description = "Month to view (1-12, defaults to current month if not specified). Must be between 1 and 12.")
            @RequestParam(value = "month", required = false) Integer month
    ) {
        logger.debug("Processing monthly calendar request: labelId={}, year={}, month={}", labelId, year, month);
        long startTime = System.currentTimeMillis();
        
        validateParameters(year, month);
        
        List<String> eventDates;
        LabelMonthStatsDTO stats = null;

        if (labelId == null) {
            logger.debug("Retrieving all events for month without label filtering");
            List<LocalDate> datesWithEvents = monthlyCalendarService.getDatesWithEventsByMonth(year, month);
            eventDates = buildEventDateStrings(datesWithEvents);
        } else {
            logger.debug("Applying label filter labelId={} for monthly calendar view", labelId);
            List<LocalDate> datesWithLabel = monthlyCalendarService.getDatesByLabel(labelId, year, month);
            eventDates = buildEventDateStrings(datesWithLabel);

            stats = monthlyCalendarService.getMonthlyBucketStats(labelId, year, month);
        }

        long processingTime = System.currentTimeMillis() - startTime;
        logger.debug("Monthly calendar view processed in {}ms for labelId={}", processingTime, labelId);
        logger.info("Successfully retrieved monthly calendar view: eventDates={}, hasStats={}, labelId={}", 
                eventDates.size(), stats != null, labelId);
        
        return ResponseEntity.ok(new MonthlyCalendarResponseDTO(eventDates, stats));
    }

    /**
     * Converts a list of LocalDate objects to ISO 8601 date strings for API response.
     * 
     * <p>This method transforms internal date representations into standardized
     * ISO 8601 date strings suitable for JSON API responses. The conversion ensures
     * consistent date formatting across all calendar endpoints and maintains
     * compatibility with frontend calendar components.</p>
     * 
     * <h3>Implementation Details</h3>
     * <ul>
     *   <li><strong>ISO 8601 Compliance</strong>: Uses LocalDate.toString() for standard YYYY-MM-DD format</li>
     *   <li><strong>Stream Processing</strong>: Efficient transformation using Java 8 streams</li>
     *   <li><strong>Null Safety</strong>: Assumes input list is non-null (validated by caller)</li>
     * </ul>
     * 
     * @param dates the list of dates to convert, must not be null but may be empty
     * @return list of ISO 8601 date strings (e.g., "2025-07-10"), never null
     *         but may be empty if input list is empty
     */
    private List<String> buildEventDateStrings(List<LocalDate> dates) {
        return dates.stream()
                .map(LocalDate::toString)
                .collect(Collectors.toList());
    }

    /**
     * Validates year and month parameters for calendar requests.
     * 
     * <p>This method performs basic input validation for calendar parameters,
     * ensuring they fall within acceptable ranges. Validation failures result
     * in appropriate error logging and exception throwing for proper HTTP
     * error response generation.</p>
     * 
     * @param year the year parameter to validate (null values are accepted)
     * @param month the month parameter to validate (null values are accepted)
     * @throws InvalidCalendarParameterException if year is not positive or month is not between 1-12
     */
    private void validateParameters(Integer year, Integer month) {
        if (year != null && year <= 0) {
            logger.warn("Invalid year parameter received: year={}, rejecting request", year);
            throw new InvalidCalendarParameterException(ErrorCode.INVALID_CALENDAR_PARAMETER);
        }
        
        if (month != null && (month < 1 || month > 12)) {
            logger.warn("Invalid month parameter received: month={}, rejecting request", month);
            throw new InvalidCalendarParameterException(ErrorCode.INVALID_CALENDAR_PARAMETER);
        }
    }

}
