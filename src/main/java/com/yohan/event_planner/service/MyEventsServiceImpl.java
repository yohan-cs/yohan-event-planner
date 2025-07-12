package com.yohan.event_planner.service;

import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.RecurringEventResponseDTO;
import com.yohan.event_planner.exception.ErrorCode;
import com.yohan.event_planner.exception.InvalidCalendarParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of {@link MyEventsService} providing cursor-based pagination for user's events.
 * 
 * <p>This service orchestrates the retrieval of confirmed events and recurring events using
 * cursor-based pagination optimized for infinite scrolling interfaces. It delegates to specialized
 * services while providing a unified interface for the "My Events" dashboard functionality.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>Event Pagination</strong>: Cursor-based pagination for regular events</li>
 *   <li><strong>Recurring Event Pagination</strong>: Complex multi-cursor pagination for recurring events</li>
 *   <li><strong>Infinite Scroll Support</strong>: Optimized for horizontal scrolling interfaces</li>
 *   <li><strong>Performance Optimization</strong>: Leverages dedicated service layer methods</li>
 * </ul>
 * 
 * <h2>Pagination Strategy</h2>
 * <p>This service implements cursor-based pagination for optimal performance:</p>
 * 
 * <h3>Regular Events Pagination</h3>
 * <ul>
 *   <li><strong>Ordering</strong>: endTime DESC, startTime DESC, id DESC</li>
 *   <li><strong>Cursor Fields</strong>: endTime, startTime, id</li>
 *   <li><strong>Use Case</strong>: Horizontal scrolling through time-ordered events</li>
 * </ul>
 * 
 * <h3>Recurring Events Pagination</h3>
 * <ul>
 *   <li><strong>Ordering</strong>: endDate DESC, startDate DESC, startTime DESC, endTime DESC, id DESC</li>
 *   <li><strong>Cursor Fields</strong>: endDate, startDate, startTime, endTime, id</li>
 *   <li><strong>Complex Ordering</strong>: Multiple fields to handle recurring patterns</li>
 * </ul>
 * 
 * <h2>Service Orchestration</h2>
 * <p>This service acts as a coordination layer:</p>
 * <ul>
 *   <li><strong>EventService Delegation</strong>: Handles regular event queries</li>
 *   <li><strong>RecurringEventService Delegation</strong>: Handles recurring event queries</li>
 *   <li><strong>Validation Layer</strong>: Ensures pagination parameters are valid</li>
 *   <li><strong>Unified Interface</strong>: Provides consistent API for frontend consumption</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Cursor-Based Pagination</strong>: Avoids expensive offset-based queries</li>
 *   <li><strong>Confirmed Events Only</strong>: Filters to relevant events for better performance</li>
 *   <li><strong>Delegated Queries</strong>: Leverages optimized service layer implementations</li>
 *   <li><strong>Limit Validation</strong>: Prevents unbounded result sets</li>
 * </ul>
 * 
 * <h2>Integration with Frontend</h2>
 * <p>Designed specifically for infinite scrolling interfaces:</p>
 * <ul>
 *   <li><strong>Horizontal Scrolling</strong>: Supports left-to-right event browsing</li>
 *   <li><strong>Cursor Tokens</strong>: Provides cursor values for next page requests</li>
 *   <li><strong>Consistent Ordering</strong>: Ensures predictable result sequences</li>
 *   <li><strong>Responsive Limits</strong>: Configurable page sizes for different devices</li>
 * </ul>
 * 
 * <h2>Error Handling</h2>
 * <ul>
 *   <li><strong>Parameter Validation</strong>: Ensures limit values are positive</li>
 *   <li><strong>Delegation Errors</strong>: Propagates service-specific exceptions</li>
 *   <li><strong>Graceful Degradation</strong>: Handles edge cases in cursor navigation</li>
 * </ul>
 * 
 * <h2>Use Cases</h2>
 * <p>Primary use cases for this service:</p>
 * <ul>
 *   <li><strong>Dashboard Views</strong>: Main "My Events" interface</li>
 *   <li><strong>Quick Browse</strong>: Rapid event browsing functionality</li>
 *   <li><strong>Mobile Interfaces</strong>: Touch-optimized scrolling experiences</li>
 *   <li><strong>Event Discovery</strong>: Browsing through user's event history</li>
 * </ul>
 * 
 * @see MyEventsService
 * @see EventService
 * @see RecurringEventService
 * @see EventResponseDTO
 * @see RecurringEventResponseDTO
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
@Service
public class MyEventsServiceImpl implements MyEventsService {

    private static final Logger logger = LoggerFactory.getLogger(MyEventsServiceImpl.class);

    private final EventService eventService;
    private final RecurringEventService recurringEventService;

    /**
     * Constructs a new MyEventsServiceImpl with required service dependencies.
     * 
     * @param eventService the event service for regular event operations
     * @param recurringEventService the recurring event service for recurring event operations
     * @throws NullPointerException if any service dependency is null
     */
    public MyEventsServiceImpl(
            EventService eventService,
            RecurringEventService recurringEventService
    ) {
        this.eventService = Objects.requireNonNull(eventService, "EventService cannot be null");
        this.recurringEventService = Objects.requireNonNull(recurringEventService, "RecurringEventService cannot be null");
        logger.debug("MyEventsServiceImpl initialized with EventService and RecurringEventService dependencies");
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Validates the limit parameter and delegates to the recurring event service.
     * This method filters to confirmed recurring events only.</p>
     * 
     * @throws InvalidCalendarParameterException if limit is less than or equal to zero
     */
    @Override
    public List<RecurringEventResponseDTO> getRecurringEventsPage(
            LocalDate endDateCursor,
            LocalDate startDateCursor,
            LocalTime startTimeCursor,
            LocalTime endTimeCursor,
            Long idCursor,
            int limit
    ) {
        logger.debug("Retrieving recurring events page with limit: {}, cursors: endDate={}, startDate={}, startTime={}, endTime={}, id={}", 
                     limit, endDateCursor, startDateCursor, startTimeCursor, endTimeCursor, idCursor);
        
        validateLimit(limit);

        logger.debug("Delegating to RecurringEventService for confirmed recurring events pagination");
        List<RecurringEventResponseDTO> result = recurringEventService.getConfirmedRecurringEventsPage(
                endDateCursor,
                startDateCursor,
                startTimeCursor,
                endTimeCursor,
                idCursor,
                limit
        );
        
        logger.info("Retrieved {} recurring events for pagination request", result.size());
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Validates the limit parameter and delegates to the event service.
     * This method filters to confirmed events only.</p>
     * 
     * @throws InvalidCalendarParameterException if limit is less than or equal to zero
     */
    @Override
    public List<EventResponseDTO> getEventsPage(
            ZonedDateTime endTimeCursor,
            ZonedDateTime startTimeCursor,
            Long idCursor,
            int limit
    ) {
        logger.debug("Retrieving events page with limit: {}, cursors: endTime={}, startTime={}, id={}", 
                     limit, endTimeCursor, startTimeCursor, idCursor);
        
        validateLimit(limit);

        logger.debug("Delegating to EventService for confirmed events pagination");
        List<EventResponseDTO> result = eventService.getConfirmedEventsPage(
                endTimeCursor,
                startTimeCursor,
                idCursor,
                limit
        );
        
        logger.info("Retrieved {} events for pagination request", result.size());
        return result;
    }

    /**
     * Validates that the limit parameter is greater than zero.
     * 
     * @param limit the pagination limit to validate
     * @throws InvalidCalendarParameterException if limit is less than or equal to zero
     */
    private void validateLimit(int limit) {
        if (limit <= 0) {
            logger.warn("Invalid pagination limit parameter provided: {}. Limit must be greater than zero", limit);
            throw new InvalidCalendarParameterException(ErrorCode.INVALID_PAGINATION_PARAMETER);
        }
    }

}