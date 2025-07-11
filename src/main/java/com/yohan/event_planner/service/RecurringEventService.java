package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.EventChangeContextDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.RecurringEventCreateDTO;
import com.yohan.event_planner.dto.RecurringEventCreationResultDTO;
import com.yohan.event_planner.dto.RecurringEventFilterDTO;
import com.yohan.event_planner.dto.RecurringEventResponseDTO;
import com.yohan.event_planner.dto.RecurringEventUpdateDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

/**
 * Service interface for managing recurring events and their complex lifecycle operations.
 * 
 * <p>This service handles all aspects of recurring event management, including:</p>
 * <ul>
 *   <li><strong>CRUD operations</strong>: Create, read, update, and delete recurring events</li>
 *   <li><strong>Recurrence pattern management</strong>: Handle complex recurrence rules and skip days</li>
 *   <li><strong>Future event coordination</strong>: Manage individual events generated from recurring patterns</li>
 *   <li><strong>Confirmation workflows</strong>: Support draft-to-confirmed state transitions</li>
 *   <li><strong>Skip day management</strong>: Allow exclusion of specific dates from recurrence patterns</li>
 * </ul>
 * 
 * <h2>Recurring Event Lifecycle</h2>
 * <p>Recurring events follow a sophisticated lifecycle:</p>
 * <ol>
 *   <li><strong>Draft Creation</strong>: Initial creation in unconfirmed state</li>
 *   <li><strong>Pattern Definition</strong>: Setting recurrence rules and date ranges</li>
 *   <li><strong>Confirmation</strong>: Activating the pattern for event generation</li>
 *   <li><strong>Event Generation</strong>: Creating individual events from the pattern</li>
 *   <li><strong>Ongoing Management</strong>: Updates, skip days, and deletions</li>
 * </ol>
 * 
 * <h2>Future Event Management</h2>
 * <p>The service maintains complex relationships with generated events:</p>
 * <ul>
 *   <li><strong>Future-only updates</strong>: Changes only affect future occurrences</li>
 *   <li><strong>Past event preservation</strong>: Historical events remain unchanged</li>
 *   <li><strong>Skip day application</strong>: Retroactive and prospective exclusions</li>
 *   <li><strong>Cascading operations</strong>: Coordinate changes across related events</li>
 * </ul>
 * 
 * <h2>Access Control and Security</h2>
 * <p>Access patterns follow strict ownership rules:</p>
 * <ul>
 *   <li><strong>Owner access</strong>: Users can manage their own recurring events</li>
 *   <li><strong>Admin privileges</strong>: Administrators may access any recurring event</li>
 *   <li><strong>Read restrictions</strong>: Standard users cannot access others' events</li>
 *   <li><strong>Operation validation</strong>: All modifications require ownership verification</li>
 * </ul>
 * 
 * <h2>Query and Filtering</h2>
 * <p>The service supports sophisticated querying capabilities:</p>
 * <ul>
 *   <li><strong>Filter-based queries</strong>: Label, date range, and time-based filtering</li>
 *   <li><strong>Pagination support</strong>: Cursor and page-based navigation</li>
 *   <li><strong>State filtering</strong>: Separate confirmed and draft event queries</li>
 *   <li><strong>Performance optimization</strong>: Efficient queries for large datasets</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>The service integrates with multiple system components:</p>
 * <ul>
 *   <li><strong>Event Service</strong>: For generating and managing individual events</li>
 *   <li><strong>Recurrence Rule Service</strong>: For parsing and expanding recurrence patterns</li>
 *   <li><strong>Label System</strong>: For categorization and analytics</li>
 *   <li><strong>Time Bucket Service</strong>: For time tracking and statistics</li>
 * </ul>
 * 
 * @see com.yohan.event_planner.domain.RecurringEvent
 * @see RecurringEventCreateDTO
 * @see RecurringEventResponseDTO
 * @see RecurrenceRuleService
 * @see EventService
 */
public interface RecurringEventService {

    /**
     * Retrieves a recurring event by ID for the given user.
     *
     * <p>Admins may access others' events, but regular users can only access their own.</p>
     *
     * @param recurringEventId the ID of the recurring event
     * @return the requested recurring event
     */
    RecurringEventResponseDTO getRecurringEventById(Long recurringEventId);

    /**
     * Retrieves confirmed recurring events for the current user with filtering and pagination.
     * 
     * <p>Supports comprehensive filtering by label, date ranges, and time periods.
     * Only returns confirmed (non-draft) recurring events owned by the current user.</p>
     * 
     * @param filter the filter criteria for narrowing results
     * @param pageNumber the page number for pagination (0-based)
     * @param pageSize the number of results per page
     * @return a page of matching confirmed recurring events
     */
    Page<RecurringEventResponseDTO> getConfirmedRecurringEventsForCurrentUser(RecurringEventFilterDTO filter, int pageNumber, int pageSize);

    /**
     * Retrieves confirmed recurring events using cursor-based pagination.
     * 
     * <p>Provides efficient pagination for large datasets using multiple cursor fields.
     * Supports filtering by date ranges and time windows for precise result sets.</p>
     * 
     * @param endDateCursor cursor for end date pagination
     * @param startDateCursor cursor for start date pagination  
     * @param startTimeCursor cursor for start time pagination
     * @param endTimeCursor cursor for end time pagination
     * @param idCursor cursor for ID-based pagination
     * @param limit maximum number of results to return
     * @return ordered list of confirmed recurring events
     */
    List<RecurringEventResponseDTO> getConfirmedRecurringEventsPage(
            LocalDate endDateCursor,
            LocalDate startDateCursor,
            LocalTime startTimeCursor,
            LocalTime endTimeCursor,
            Long idCursor,
            int limit
    );


    /**
     * Returns all unconfirmed (draft) recurring events owned by the user.
     *
     * <p>This is a self-only operation.</p>
     *
     * @return list of unconfirmed recurring events
     */
    List<RecurringEventResponseDTO> getUnconfirmedRecurringEventsForCurrentUser();

    /**
     * Creates a new recurring event in draft or confirmed state, depending on input.
     *
     * @param dto the creation DTO containing recurrence details
     * @return the created recurring event
     */
    RecurringEventResponseDTO createRecurringEvent(RecurringEventCreateDTO dto);


    /**
     * Confirms a draft recurring event, activating its recurrence pattern.
     * 
     * <p>Transitions a recurring event from draft to confirmed state, which:
     * <ul>
     *   <li>Validates the recurrence pattern and date range</li>
     *   <li>Makes the recurring event visible in standard queries</li>
     *   <li>Enables future event generation from the pattern</li>
     *   <li>Includes the event in analytics and time tracking</li>
     * </ul>
     * 
     * @param recurringEventId the ID of the recurring event to confirm
     * @return the confirmed recurring event
     * @throws RecurringEventNotFoundException if the recurring event doesn't exist
     * @throws RecurringEventAlreadyConfirmedException if already confirmed
     */
    RecurringEventResponseDTO confirmRecurringEvent(Long recurringEventId);

    /**
     * Updates an existing recurring event.
     *
     * <p>If the event is confirmed, only future occurrences will be updated. Past instances remain unchanged.</p>
     *
     * @param recurringEventId the ID of the recurring event to update
     * @param dto the update DTO with the new values
     * @return the updated recurring event
     */
    RecurringEventResponseDTO updateRecurringEvent(Long recurringEventId, RecurringEventUpdateDTO dto);

    /**
     * Deletes a recurring event and all future associated events.
     *
     * <p>Past instances are not affected unless explicitly deleted elsewhere.</p>
     *
     * @param recurringEventId the ID of the recurring event to delete
     */
    void deleteRecurringEvent(Long recurringEventId);
    /**
     * Deletes all unconfirmed (draft) recurring events for the current user.
     * 
     * <p>Provides a bulk cleanup operation for removing all draft recurring events.
     * This is commonly used for clearing incomplete or experimental patterns.
     * Only affects unconfirmed events - confirmed recurring events are not touched.</p>
     */
    void deleteUnconfirmedRecurringEventsForCurrentUser();

    /**
     * Adds specific dates to the skip days collection for a recurring event.
     * 
     * <p>Skip days are dates where the recurrence pattern would normally create
     * an event, but should be excluded. This is useful for:
     * <ul>
     *   <li>Holiday exclusions</li>
     *   <li>Vacation periods</li>
     *   <li>One-time schedule conflicts</li>
     *   <li>Temporary pattern modifications</li>
     * </ul>
     * 
     * @param recurringEventId the ID of the recurring event to modify
     * @param skipDaysToAdd the set of dates to exclude from the pattern
     * @return the updated recurring event with new skip days
     * @throws RecurringEventNotFoundException if the recurring event doesn't exist
     */
    RecurringEventResponseDTO addSkipDays(Long recurringEventId, Set<LocalDate> skipDaysToAdd);
    
    /**
     * Removes specific dates from the skip days collection for a recurring event.
     * 
     * <p>Allows previously excluded dates to be included in the recurrence pattern again.
     * This effectively "un-skips" dates that were previously excluded, allowing
     * the pattern to generate events on those dates in the future.</p>
     * 
     * @param recurringEventId the ID of the recurring event to modify
     * @param skipDaysToRemove the set of dates to include back in the pattern
     * @return the updated recurring event with removed skip days
     * @throws RecurringEventNotFoundException if the recurring event doesn't exist
     */
    RecurringEventResponseDTO removeSkipDays(Long recurringEventId, Set<LocalDate> skipDaysToRemove);
}
