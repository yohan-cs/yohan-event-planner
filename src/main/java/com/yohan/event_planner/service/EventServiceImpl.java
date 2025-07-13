package com.yohan.event_planner.service;

import com.blazebit.persistence.PagedList;
import com.yohan.event_planner.business.EventBO;
import com.yohan.event_planner.business.RecurringEventBO;
import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.business.handler.EventPatchHandler;
import com.yohan.event_planner.dao.EventDAO;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.TimeFilter;
import com.yohan.event_planner.dto.DayViewDTO;
import com.yohan.event_planner.dto.EventCreateDTO;
import com.yohan.event_planner.dto.EventFilterDTO;
import com.yohan.event_planner.dto.EventChangeContextDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventResponseDTOFactory;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.dto.WeekViewDTO;
import com.yohan.event_planner.exception.ErrorCode;
import com.yohan.event_planner.exception.EventAlreadyConfirmedException;
import com.yohan.event_planner.exception.EventNotFoundException;
import com.yohan.event_planner.exception.EventOwnershipException;
import com.yohan.event_planner.exception.InvalidTimeException;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import com.yohan.event_planner.time.ClockProvider;
import com.yohan.event_planner.time.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link EventService} providing comprehensive event management functionality.
 * 
 * <p>This service serves as the central orchestrator for event operations, coordinating complex 
 * business logic across multiple domains including time management, recurring events, labeling,
 * user authorization, and impromptu event pinning. It provides full CRUD operations with advanced 
 * features like view generation, event confirmation workflows, dashboard pinning capabilities, and 
 * integration with recurring event patterns.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>Event Lifecycle Management</strong>: Create, read, update, delete with state validation</li>
 *   <li><strong>Confirmation Workflows</strong>: Support for draft → confirmed → completed transitions</li>
 *   <li><strong>Time Management</strong>: Advanced timezone handling with UTC storage and local display</li>
 *   <li><strong>Recurring Event Integration</strong>: Bidirectional sync with recurring event patterns</li>
 *   <li><strong>Impromptu Event Pinning</strong>: Automatic pinning and unpinning for dashboard reminders</li>
 * </ul>
 * 
 * <h2>Advanced Time Handling</h2>
 * <p>The service implements sophisticated time management strategies:</p>
 * <ul>
 *   <li><strong>UTC Storage</strong>: All timestamps stored in UTC for consistency</li>
 *   <li><strong>Timezone Preservation</strong>: Original timezone IDs maintained for accurate local display</li>
 *   <li><strong>Duration Calculations</strong>: Automatic duration computation in minutes</li>
 *   <li><strong>Untimed Events</strong>: Support for events without specific end times</li>
 *   <li><strong>Multi-timezone Views</strong>: Generate views adjusted to user's current timezone</li>
 * </ul>
 * 
 * <h2>Event State Management</h2>
 * <p>Events progress through distinct states with enforced transitions:</p>
 * <ul>
 *   <li><strong>Unconfirmed (Draft)</strong>: Initial state, visible only to creator</li>
 *   <li><strong>Confirmed</strong>: Finalized events, visible to authorized users</li>
 *   <li><strong>Completed</strong>: Past events marked as finished</li>
 * </ul>
 * 
 * <h2>Impromptu Event Pinning</h2>
 * <p>Sophisticated pinning system for dashboard reminders:</p>
 * <ul>
 *   <li><strong>Automatic Pinning</strong>: New impromptu events are automatically pinned during creation</li>
 *   <li><strong>Single Pin Constraint</strong>: Only one impromptu event can be pinned per user at a time</li>
 *   <li><strong>Auto-Unpin Safeguards</strong>: Events are automatically unpinned when confirmed, completed, or deleted</li>
 *   <li><strong>Transaction Coordination</strong>: Pinning operations are coordinated with UserBO for data consistency</li>
 *   <li><strong>State Validation</strong>: Pinned events must maintain {@code draft = true && impromptu = true} qualification</li>
 *   <li><strong>Manual Unpinning</strong>: Users can manually unpin events through dedicated service methods</li>
 * </ul>
 * 
 * <h2>View Generation</h2>
 * <p>Provides specialized views for different UI requirements:</p>
 * <ul>
 *   <li><strong>Day View</strong>: Single-day event aggregation with hour-by-hour breakdown</li>
 *   <li><strong>Week View</strong>: Seven-day overview with recurring event expansion</li>
 *   <li><strong>Filtered Views</strong>: Label-based filtering and time range restrictions</li>
 *   <li><strong>Pagination Support</strong>: Cursor-based pagination for infinite scrolling</li>
 * </ul>
 * 
 * <h2>Recurring Event Integration</h2>
 * <p>Seamlessly integrates with recurring event patterns:</p>
 * <ul>
 *   <li><strong>Pattern Expansion</strong>: Generate individual events from recurring patterns</li>
 *   <li><strong>Bulk Updates</strong>: Propagate changes to future instances</li>
 *   <li><strong>Exception Handling</strong>: Support for modified individual occurrences</li>
 *   <li><strong>Conflict Resolution</strong>: Validate timing conflicts across patterns</li>
 * </ul>
 * 
 * <h2>Advanced Features</h2>
 * <ul>
 *   <li><strong>Impromptu Events</strong>: Quick creation for unplanned activities</li>
 *   <li><strong>Bulk Operations</strong>: Efficient deletion of unconfirmed events</li>
 *   <li><strong>Event Filtering</strong>: Complex filtering by time, labels, and completion status</li>
 *   <li><strong>Ownership Validation</strong>: Comprehensive authorization checks</li>
 * </ul>
 * 
 * <h2>Performance Optimizations</h2>
 * <ul>
 *   <li><strong>Blaze-Persistence Integration</strong>: Efficient complex queries via EventDAO</li>
 *   <li><strong>Lazy Loading</strong>: Minimize database hits through strategic loading</li>
 *   <li><strong>Cursor Pagination</strong>: Scalable pagination for large datasets</li>
 *   <li><strong>Batch Operations</strong>: Reduce round trips for bulk updates</li>
 * </ul>
 * 
 * <h2>Security and Authorization</h2>
 * <p>Comprehensive security model ensures data protection:</p>
 * <ul>
 *   <li><strong>Ownership Validation</strong>: Users can only access their own events</li>
 *   <li><strong>State-based Visibility</strong>: Unconfirmed events hidden from non-creators</li>
 *   <li><strong>Label Authorization</strong>: Validate access to associated labels</li>
 *   <li><strong>Operation Restrictions</strong>: Prevent unauthorized state transitions</li>
 * </ul>
 * 
 * <h2>Integration Architecture</h2>
 * <p>This service integrates with multiple system components:</p>
 * <ul>
 *   <li><strong>EventBO</strong>: Business logic delegation for complex operations</li>
 *   <li><strong>LabelService</strong>: Event categorization and organization</li>
 *   <li><strong>RecurringEventBO</strong>: Recurring pattern management</li>
 *   <li><strong>EventDAO</strong>: Advanced query execution via Blaze-Persistence</li>
 *   <li><strong>Time Services</strong>: Timezone conversion and clock management</li>
 * </ul>
 * 
 * <h2>Error Handling</h2>
 * <p>Comprehensive error handling for various scenarios:</p>
 * <ul>
 *   <li><strong>EventNotFoundException</strong>: When requested events don't exist</li>
 *   <li><strong>EventAlreadyConfirmedException</strong>: When attempting invalid state transitions</li>
 *   <li><strong>InvalidTimeException</strong>: For time validation failures</li>
 *   <li><strong>Ownership Violations</strong>: When users access unauthorized events</li>
 * </ul>
 * 
 * <h2>Data Consistency</h2>
 * <p>Maintains consistency across related entities:</p>
 * <ul>
 *   <li><strong>Transactional Boundaries</strong>: Ensure atomic operations</li>
 *   <li><strong>Cascade Management</strong>: Handle related entity updates</li>
 *   <li><strong>Constraint Validation</strong>: Enforce business rules</li>
 *   <li><strong>State Synchronization</strong>: Keep recurring events in sync</li>
 * </ul>
 * 
 * @see EventService
 * @see Event
 * @see EventBO
 * @see EventDAO
 * @see RecurringEventBO
 * @see LabelService
 * @see EventResponseDTOFactory
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
@Service
public class EventServiceImpl implements EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventServiceImpl.class);

    private final EventBO eventBO;
    private final RecurringEventBO recurringEventBO;
    private final UserBO userBO;
    private final LabelService labelService;
    private final EventDAO eventDAO;
    private final EventPatchHandler eventPatchHandler;
    private final EventResponseDTOFactory eventResponseDTOFactory;
    private final OwnershipValidator ownershipValidator;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final ClockProvider clockProvider;

    public EventServiceImpl(
            EventBO eventBO,
            RecurringEventBO recurringEventBO,
            UserBO userBO,
            LabelService labelService,
            EventDAO eventDAO,
            EventPatchHandler eventPatchHandler,
            EventResponseDTOFactory eventResponseDTOFactory,
            OwnershipValidator ownershipValidator,
            AuthenticatedUserProvider authenticatedUserProvider,
            ClockProvider clockProvider
    ) {
        this.eventBO = eventBO;
        this.recurringEventBO = recurringEventBO;
        this.userBO = userBO;
        this.labelService = labelService;
        this.eventDAO = eventDAO;
        this.eventPatchHandler = eventPatchHandler;
        this.eventResponseDTOFactory = eventResponseDTOFactory;
        this.ownershipValidator = ownershipValidator;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.clockProvider = clockProvider;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation enforces ownership validation with different behaviors:</p>
     * <ul>
     *   <li><strong>Unconfirmed events</strong>: Hidden from non-creators by throwing EventNotFoundException</li>
     *   <li><strong>Confirmed events</strong>: Strict ownership validation throws EventOwnershipException</li>
     * </ul>
     *
     * @param eventId the ID of the event to retrieve
     * @return the event as EventResponseDTO
     * @throws EventNotFoundException if event doesn't exist or unconfirmed event accessed by non-creator
     * @throws EventOwnershipException if confirmed event accessed by non-owner
     */
    @Override
    public EventResponseDTO getEventById(Long eventId) {
        User viewer = authenticatedUserProvider.getCurrentUser();
        logger.debug("Retrieving event {} for user {}", eventId, viewer.getId());
        
        Event event = eventBO.getEventById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // For unconfirmed events, hide existence from non-creators by throwing EventNotFoundException
        if (event.isUnconfirmed() && !event.getCreator().getId().equals(viewer.getId())) {
            logger.warn("User {} attempted to access unconfirmed event {} owned by user {}", 
                    viewer.getId(), eventId, event.getCreator().getId());
            throw new EventNotFoundException(eventId);
        }
        
        // For confirmed events, validate ownership properly
        if (!event.isUnconfirmed()) {
            ownershipValidator.validateEventOwnership(viewer.getId(), event);
        }

        logger.info("Successfully retrieved event {} for user {}", eventId, viewer.getId());
        return eventResponseDTOFactory.createFromEvent(event);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>This implementation applies time filter logic to resolve ALL, PAST_ONLY, FUTURE_ONLY, 
     * and CUSTOM time ranges. For CUSTOM filters, validates that start time is not after end time.
     * Uses EventDAO for efficient complex filtering via Blaze-Persistence.</p>
     *
     * @param filter the filtering criteria  
     * @param pageNumber zero-based page number
     * @param pageSize maximum events per page
     * @return paginated confirmed events matching filter criteria
     * @throws InvalidTimeException if CUSTOM filter has start time after end time
     */
    public Page<EventResponseDTO> getConfirmedEventsForCurrentUser(EventFilterDTO filter, int pageNumber, int pageSize) {
        User viewer = authenticatedUserProvider.getCurrentUser();

        ZonedDateTime now = ZonedDateTime.now(clockProvider.getClockForUser(viewer));
        TimeRange timeRange = TimeFilterResolver.resolveTimeRange(filter.timeFilter(), filter.start(), filter.end(), now);

        // For incomplete past event filtering, use 'now' as the reference time
        ZonedDateTime referenceTime = Boolean.FALSE.equals(filter.includeIncompletePastEvents()) ? now : timeRange.end();
        
        EventFilterDTO sanitizedFilter = new EventFilterDTO(
                filter.labelId(),
                TimeFilter.ALL, // resolved meaning - actual times passed to DAO
                timeRange.start(),
                referenceTime, // Use reference time for proper incomplete past event filtering
                filter.sortDescending(),
                filter.includeIncompletePastEvents()
        );

        PagedList<Event> confirmedEvents = eventDAO.findConfirmedEvents(viewer.getId(), sanitizedFilter, pageNumber, pageSize);

        List<EventResponseDTO> dtos = confirmedEvents.stream()
                .map(eventResponseDTOFactory::createFromEvent)
                .toList();

        return new PageImpl<>(dtos, PageRequest.of(pageNumber, pageSize), confirmedEvents.getTotalSize());
    }


    /**
     * {@inheritDoc}
     *
     * <p>This implementation uses cursor-based pagination for efficient scrolling through
     * large event datasets. Delegates to EventBO for the actual data retrieval and converts
     * to DTOs for presentation.</p>
     *
     * @param endTimeCursor cursor position based on event end time
     * @param startTimeCursor cursor position based on event start time
     * @param idCursor cursor position based on event ID for tie-breaking
     * @param limit maximum number of events to return
     * @return list of confirmed events after cursor position
     */
    @Override
    public List<EventResponseDTO> getConfirmedEventsPage(
            ZonedDateTime endTimeCursor,
            ZonedDateTime startTimeCursor,
            Long idCursor,
            int limit
    ) {
        User viewer = authenticatedUserProvider.getCurrentUser();

        List<Event> events = eventBO.getConfirmedEventsPage(
                viewer.getId(),
                endTimeCursor,
                startTimeCursor,
                idCursor,
                limit
        );

        return events.stream()
                .map(eventResponseDTOFactory::createFromEvent)
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation retrieves all draft/unconfirmed events for the current user.
     * Only the event creator can see unconfirmed events, ensuring privacy of drafts.</p>
     *
     * @return list of unconfirmed events owned by current user
     */
    @Override
    @Transactional(readOnly = true)
    public List<EventResponseDTO> getUnconfirmedEventsForCurrentUser() {
        User viewer = authenticatedUserProvider.getCurrentUser();
        logger.debug("Retrieving unconfirmed events for user {}", viewer.getId());

        List<Event> unconfirmedEvents = eventBO.getUnconfirmedEventsForUser(viewer.getId());

        logger.info("Retrieved {} unconfirmed events for user {}", unconfirmedEvents.size(), viewer.getId());
        return unconfirmedEvents.stream()
                .map(eventResponseDTOFactory::createFromEvent)
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation coordinates complex day view generation including:</p>
     * <ul>
     *   <li>Solidification of past recurring events into concrete events</li>
     *   <li>Generation of virtual events for future recurring patterns</li>
     *   <li>Timezone conversion for proper local time display</li>
     *   <li>Delegation to EventBO for business logic execution</li>
     * </ul>
     *
     * @param selectedDate the date to generate view for
     * @return comprehensive day view with confirmed and virtual events
     */
    @Transactional
    public DayViewDTO generateDayView(LocalDate selectedDate) {
        User viewer = authenticatedUserProvider.getCurrentUser();
        DayViewTimeContext timeContext = calculateDayViewTimeContext(viewer, selectedDate);
        
        // Solidify past recurrences if needed
        if (timeContext.shouldSolidifyRecurrences()) {
            eventBO.solidifyRecurrences(viewer.getId(), timeContext.startOfDay(), 
                    timeContext.solidifyEndWindow(), timeContext.userZoneId());
        }

        // Fetch confirmed events
        List<EventResponseDTO> confirmedEvents = fetchConfirmedEventsForDay(viewer, timeContext);

        // Generate virtual events if needed  
        List<EventResponseDTO> virtualEvents = generateVirtualEventsForDay(viewer, timeContext);

        // Delegate business logic to EventBO
        DayViewDTO result = eventBO.generateDayViewData(selectedDate, confirmedEvents, virtualEvents);
        
        logger.debug("Generated DayViewDTO for user {} for date {}", viewer.getId(), selectedDate);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation coordinates complex week view generation including:</p>
     * <ul>
     *   <li>Week boundary calculation (Monday to Sunday)</li>
     *   <li>Solidification of past recurring events within the week</li>
     *   <li>Generation of virtual events for future recurring patterns</li>
     *   <li>Timezone-aware time window calculations</li>
     *   <li>Delegation to EventBO for business logic execution</li>
     * </ul>
     *
     * @param anchorDate any date within the desired week
     * @return comprehensive week view with confirmed and virtual events
     */
    @Transactional
    public WeekViewDTO generateWeekView(LocalDate anchorDate) {
        User viewer = authenticatedUserProvider.getCurrentUser();
        WeekViewTimeContext timeContext = calculateWeekViewTimeContext(viewer, anchorDate);
        
        // Solidify past recurrences if needed
        if (timeContext.shouldSolidifyRecurrences()) {
            eventBO.solidifyRecurrences(viewer.getId(), timeContext.weekStartTime(), 
                    timeContext.solidifyEndWindow(), timeContext.userZoneId());
        }

        // Fetch confirmed events
        List<EventResponseDTO> confirmedEvents = fetchConfirmedEventsForWeek(viewer, timeContext);

        // Generate virtual events if needed
        List<EventResponseDTO> virtualEvents = generateVirtualEventsForWeek(viewer, timeContext);

        // Delegate business logic to EventBO
        WeekViewDTO result = eventBO.generateWeekViewData(viewer.getId(), anchorDate, timeContext.userZoneId(), 
                confirmedEvents, virtualEvents);
        
        logger.debug("Generated WeekViewDTO for user {} for week starting {}", viewer.getId(), timeContext.weekStartDate());
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation handles both confirmed events and drafts based on the DTO flag.
     * Assigns the specified label or defaults to the user's "Unlabeled" category.
     * Delegates business validation and creation to EventBO.</p>
     *
     * @param dto the event creation payload
     * @return the created event as EventResponseDTO
     */
    @Override
    @Transactional
    public EventResponseDTO createEvent(EventCreateDTO dto) {
        User creator = authenticatedUserProvider.getCurrentUser();
        logger.debug("Creating event for user {}: draft={}, startTime={}", 
                creator.getId(), dto.isDraft(), dto.startTime());

        Event event = dto.isDraft()
                ? Event.createUnconfirmedDraft(dto.name(), dto.startTime(), dto.endTime(), creator)
                : Event.createEvent(dto.name(), dto.startTime(), dto.endTime(), creator);

        event.setDescription(dto.description());

        Label label = (dto.labelId() != null)
                ? labelService.getLabelEntityById(dto.labelId())
                : creator.getUnlabeled();

        event.setLabel(label);

        Event saved = eventBO.createEvent(event);
        logger.info("Successfully created event {} for user {}: confirmed={}", 
                saved.getId(), creator.getId(), !saved.isUnconfirmed());
        
        return eventResponseDTOFactory.createFromEvent(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation creates an impromptu event starting at the current time
     * in the user's timezone. The event is automatically assigned to the user's
     * "Unlabeled" category, created in draft status, marked as impromptu, and pinned
     * to the user's dashboard as a reminder.</p>
     * 
     * <p>The pinning operation is performed in the same transaction to ensure atomicity.
     * If the user already has a pinned impromptu event, it will be replaced by this new event.</p>
     *
     * @return the created impromptu event as EventResponseDTO
     */
    @Override
    @Transactional
    public EventResponseDTO createImpromptuEvent() {
        User creator = authenticatedUserProvider.getCurrentUser();
        logger.debug("Creating impromptu event for user {}", creator.getId());

        Clock userClock = clockProvider.getClockForUser(creator);
        ZonedDateTime now = ZonedDateTime.now(userClock);

        Event event = Event.createImpromptuEvent(now, creator);
        event.setLabel(creator.getUnlabeled());

        Event saved = eventBO.createEvent(event);
        
        // Pin the impromptu event for the user
        creator.setPinnedImpromptuEvent(saved);
        userBO.updateUser(creator);
        
        logger.info("Successfully created and pinned impromptu event {} for user {}", saved.getId(), creator.getId());
        
        return eventResponseDTOFactory.createFromEvent(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation validates ownership and current state before confirming.
     * Only unconfirmed (draft) events can be confirmed. Delegates the confirmation
     * logic to EventBO for business rule enforcement.</p>
     *
     * @param eventId the ID of the event to confirm
     * @return the confirmed event as EventResponseDTO
     * @throws EventNotFoundException if event doesn't exist
     * @throws EventOwnershipException if user doesn't own the event
     * @throws EventAlreadyConfirmedException if event is already confirmed
     */
    @Override
    @Transactional
    public EventResponseDTO confirmEvent(Long eventId) {
        User viewer = authenticatedUserProvider.getCurrentUser();
        logger.debug("Confirming event {} for user {}", eventId, viewer.getId());

        Event event = eventBO.getEventById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        ownershipValidator.validateEventOwnership(viewer.getId(), event);

        if (!event.isUnconfirmed()) {
            logger.warn("User {} attempted to confirm already confirmed event {}", 
                    viewer.getId(), eventId);
            throw new EventAlreadyConfirmedException(eventId);
        }

        Event confirmed = eventBO.confirmEvent(event);
        
        // Auto-unpin if this was the user's pinned impromptu event
        if (viewer.getPinnedImpromptuEvent() != null && 
            viewer.getPinnedImpromptuEvent().getId().equals(eventId)) {
            logger.info("Auto-unpinning confirmed event {} for user {}", eventId, viewer.getId());
            viewer.setPinnedImpromptuEvent(null);
            userBO.updateUser(viewer);
        }
        
        logger.info("Successfully confirmed event {} for user {}", eventId, viewer.getId());

        return eventResponseDTOFactory.createFromEvent(confirmed);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation first confirms the event, then validates that the event
     * has already ended before marking it as completed. This is typically used for
     * impromptu events that are being logged after they occurred.</p>
     *
     * @param eventId the ID of the event to confirm and complete
     * @return the confirmed and completed event as EventResponseDTO
     * @throws EventNotFoundException if event doesn't exist
     * @throws EventOwnershipException if user doesn't own the event
     * @throws EventAlreadyConfirmedException if event is already confirmed
     * @throws InvalidTimeException if event hasn't ended yet
     */
    @Override
    @Transactional
    public EventResponseDTO confirmAndCompleteEvent(Long eventId) {
        User viewer = authenticatedUserProvider.getCurrentUser();
        logger.debug("Confirming and completing event {} for user {}", eventId, viewer.getId());

        Event event = eventBO.getEventById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        ownershipValidator.validateEventOwnership(viewer.getId(), event);

        if (!event.isUnconfirmed()) {
            logger.warn("User {} attempted to confirm and complete already confirmed event {}", 
                    viewer.getId(), eventId);
            throw new EventAlreadyConfirmedException(eventId);
        }

        eventBO.confirmEvent(event);

        ZonedDateTime now = ZonedDateTime.now(clockProvider.getClockForUser(viewer));
        if (event.getEndTime().isAfter(now)) {
            logger.warn("User {} attempted to complete future event {} (ends at {})", 
                    viewer.getId(), eventId, event.getEndTime());
            throw new InvalidTimeException(
                    ErrorCode.INVALID_COMPLETION_STATUS,
                    event.getStartTime(),
                    event.getEndTime(),
                    now
            );
        }

        // Auto-unpin if this was the user's pinned impromptu event
        if (viewer.getPinnedImpromptuEvent() != null && 
            viewer.getPinnedImpromptuEvent().getId().equals(eventId)) {
            logger.info("Auto-unpinning confirmed and completed event {} for user {}", eventId, viewer.getId());
            viewer.setPinnedImpromptuEvent(null);
            userBO.updateUser(viewer);
        }

        EventUpdateDTO completionPatch = new EventUpdateDTO(
                null,
                null,
                null,
                null,
                null,
                true
        );

        logger.info("Successfully confirmed and completed event {} for user {}", eventId, viewer.getId());
        return updateEvent(eventId, completionPatch);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation uses EventPatchHandler for sophisticated field updating
     * with skip/clear/update semantics. Creates a snapshot context for tracking changes
     * that require business validation or cascading updates.</p>
     *
     * @param eventId the ID of the event to update
     * @param dto the update payload with optional fields
     * @return the updated event as EventResponseDTO
     * @throws EventNotFoundException if event doesn't exist
     * @throws EventOwnershipException if user doesn't own the event
     */
    @Override
    @Transactional
    public EventResponseDTO updateEvent(Long eventId, EventUpdateDTO dto) {
        User viewer = authenticatedUserProvider.getCurrentUser();
        logger.debug("Updating event {} for user {}", eventId, viewer.getId());

        Event event = eventBO.getEventById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        ownershipValidator.validateEventOwnership(viewer.getId(), event);

        boolean snapshotNeeded = dto.startTime() != null
                || dto.endTime() != null
                || dto.labelId() != null
                || dto.isCompleted() != null;

        EventChangeContextDTO changeContext = snapshotNeeded ? createSnapshotContext(event) : null;

        boolean changed = eventPatchHandler.applyPatch(event, dto);
        Event updated = changed ? eventBO.updateEvent(changeContext, event) : event;

        if (changed) {
            logger.info("Successfully updated event {} for user {}", eventId, viewer.getId());
        } else {
            logger.debug("No changes applied to event {} for user {}", eventId, viewer.getId());
        }

        return eventResponseDTOFactory.createFromEvent(updated);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation validates ownership before deletion and delegates
     * to EventBO for business logic execution. The deletion may cascade to
     * related entities as defined by the business layer.</p>
     *
     * @param eventId the ID of the event to delete
     * @throws EventNotFoundException if event doesn't exist
     * @throws EventOwnershipException if user doesn't own the event
     */
    @Override
    @Transactional
    public void deleteEvent(Long eventId) {
        User viewer = authenticatedUserProvider.getCurrentUser();
        logger.debug("Deleting event {} for user {}", eventId, viewer.getId());
        
        Event existing = eventBO.getEventById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        ownershipValidator.validateEventOwnership(viewer.getId(), existing);

        // Auto-unpin if this was the user's pinned impromptu event
        if (viewer.getPinnedImpromptuEvent() != null && 
            viewer.getPinnedImpromptuEvent().getId().equals(eventId)) {
            logger.info("Auto-unpinning deleted event {} for user {}", eventId, viewer.getId());
            viewer.setPinnedImpromptuEvent(null);
            userBO.updateUser(viewer);
        }

        eventBO.deleteEvent(eventId);
        logger.info("Successfully deleted event {} for user {}", eventId, viewer.getId());
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation performs a bulk delete operation for all unconfirmed
     * events owned by the current user. This is typically used for cleanup operations
     * when a user wants to clear all their drafts.</p>
     */
    @Override
    @Transactional
    public void deleteUnconfirmedEventsForCurrentUser() {
        User viewer = authenticatedUserProvider.getCurrentUser();
        logger.debug("Deleting all unconfirmed events for user {}", viewer.getId());

        eventBO.deleteAllUnconfirmedEventsByUser(viewer.getId());
        logger.info("Successfully deleted all unconfirmed events for user {}", viewer.getId());
    }

    /**
     * Creates a snapshot context for tracking event changes during updates.
     * 
     * <p>This context is used by the business layer to determine what changed
     * and perform appropriate validations or cascading updates.</p>
     *
     * @param event the event to create snapshot context for
     * @return context DTO containing current event state for change tracking
     */
    private EventChangeContextDTO createSnapshotContext(Event event) {
        return new EventChangeContextDTO(
                event.getCreator().getId(),
                event.getLabel().getId(),
                null,
                event.getStartTime(),
                null,
                event.getDurationMinutes(),
                null,
                ZoneId.of(event.getCreator().getTimezone()),
                event.isCompleted(),
                false
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation delegates to EventBO for the complex logic of updating
     * future event instances that were generated from a recurring pattern. Only events
     * with start times in the future are affected.</p>
     *
     * @param recurringEvent the recurring event with updated values
     * @param changedFields set of field names that changed
     * @param userZoneId timezone for time calculations
     * @return number of events updated
     */
    @Override
    @Transactional
    public int updateFutureEventsFromRecurringEvent(RecurringEvent recurringEvent, Set<String> changedFields, ZoneId userZoneId) {
        logger.debug("Updating future events from recurring event {} with changed fields: {}", 
                recurringEvent.getId(), changedFields);
        
        int updatedCount = eventBO.updateFutureEventsFromRecurringEvent(recurringEvent, changedFields, userZoneId);
        
        logger.info("Updated {} future events from recurring event {}", updatedCount, recurringEvent.getId());
        return updatedCount;
    }

    /**
     * Time context record for day view calculations.
     * 
     * @param userZoneId the user's timezone
     * @param nowInUtc current time in UTC
     * @param today today's date in user timezone
     * @param startOfDay start of selected day in UTC
     * @param endOfDay end of selected day in UTC
     * @param solidifyEndWindow end window for solidification in UTC
     * @param selectedDate the selected date for the view
     */
    private record DayViewTimeContext(
            ZoneId userZoneId,
            ZonedDateTime nowInUtc,
            LocalDate today,
            ZonedDateTime startOfDay,
            ZonedDateTime endOfDay,
            ZonedDateTime solidifyEndWindow,
            LocalDate selectedDate
    ) {
        boolean shouldSolidifyRecurrences() {
            return !selectedDate.isAfter(today);
        }
        
        boolean shouldGenerateVirtualEvents() {
            return !selectedDate.isBefore(today);
        }
        
        ZonedDateTime getVirtualsStartWindow() {
            return selectedDate.isEqual(today) ? nowInUtc : startOfDay;
        }
    }

    /**
     * Calculates time context for day view generation.
     *
     * @param viewer the user viewing the day
     * @param selectedDate the date to generate view for
     * @return time context containing all relevant time calculations
     */
    private DayViewTimeContext calculateDayViewTimeContext(User viewer, LocalDate selectedDate) {
        ZoneId userZoneId = ZoneId.of(viewer.getTimezone());
        ZonedDateTime nowInUserzone = ZonedDateTime.now(clockProvider.getClockForUser(viewer));
        ZonedDateTime nowInUtc = nowInUserzone.withZoneSameInstant(ZoneOffset.UTC);
        LocalDate today = nowInUserzone.toLocalDate();

        ZonedDateTime startOfDay = selectedDate.atStartOfDay(userZoneId)
                .withZoneSameInstant(ZoneOffset.UTC);
        ZonedDateTime endOfDay = selectedDate.plusDays(1).atStartOfDay(userZoneId)
                .withZoneSameInstant(ZoneOffset.UTC)
                .minusNanos(1);
                
        ZonedDateTime solidifyEndWindow = selectedDate.isEqual(today) ? nowInUtc : endOfDay;
        
        return new DayViewTimeContext(userZoneId, nowInUtc, today, startOfDay, endOfDay, 
                solidifyEndWindow, selectedDate);
    }

    /**
     * Fetches confirmed events for the day view time range.
     *
     * @param viewer the user viewing the day
     * @param timeContext the time context for the day
     * @return list of confirmed events for the day
     */
    private List<EventResponseDTO> fetchConfirmedEventsForDay(User viewer, DayViewTimeContext timeContext) {
        return eventBO
                .getConfirmedEventsForUserInRange(viewer.getId(), timeContext.startOfDay(), timeContext.endOfDay())
                .stream()
                .map(eventResponseDTOFactory::createFromEvent)
                .toList();
    }

    /**
     * Generates virtual events for the day view if appropriate.
     *
     * @param viewer the user viewing the day
     * @param timeContext the time context for the day
     * @return list of virtual events for the day
     */
    private List<EventResponseDTO> generateVirtualEventsForDay(User viewer, DayViewTimeContext timeContext) {
        List<EventResponseDTO> virtualEvents = new ArrayList<>();
        if (timeContext.shouldGenerateVirtualEvents()) {
            virtualEvents.addAll(
                    recurringEventBO.generateVirtuals(viewer.getId(), timeContext.getVirtualsStartWindow(), 
                            timeContext.endOfDay(), timeContext.userZoneId())
            );
        }
        return virtualEvents;
    }

    /**
     * Time context record for week view calculations.
     * 
     * @param userZoneId the user's timezone
     * @param nowInUtc current time in UTC
     * @param today today's date in user timezone
     * @param weekStartDate start date of the week (Monday)
     * @param weekEndDate end date of the week (Sunday)
     * @param weekStartTime start of week in UTC
     * @param weekEndTime end of week in UTC
     * @param solidifyEndWindow end window for solidification in UTC
     */
    private record WeekViewTimeContext(
            ZoneId userZoneId,
            ZonedDateTime nowInUtc,
            LocalDate today,
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            ZonedDateTime weekStartTime,
            ZonedDateTime weekEndTime,
            ZonedDateTime solidifyEndWindow
    ) {
        boolean shouldSolidifyRecurrences() {
            return !weekStartDate.isAfter(today);
        }
        
        boolean shouldGenerateVirtualEvents() {
            return !weekEndDate.isBefore(today);
        }
        
        ZonedDateTime getVirtualsStartWindow() {
            return weekStartDate.isAfter(today) ? weekStartTime : nowInUtc;
        }
    }

    /**
     * Calculates time context for week view generation.
     *
     * @param viewer the user viewing the week
     * @param anchorDate any date within the desired week
     * @return time context containing all relevant time calculations
     */
    private WeekViewTimeContext calculateWeekViewTimeContext(User viewer, LocalDate anchorDate) {
        ZoneId userZoneId = ZoneId.of(viewer.getTimezone());
        ZonedDateTime nowInUserzone = ZonedDateTime.now(clockProvider.getClockForUser(viewer));
        ZonedDateTime nowInUtc = nowInUserzone.withZoneSameInstant(ZoneOffset.UTC);
        LocalDate today = nowInUserzone.toLocalDate();

        LocalDate weekStartDate = anchorDate.with(DayOfWeek.MONDAY);
        LocalDate weekEndDate = weekStartDate.plusDays(6);

        ZonedDateTime weekStartTime = weekStartDate.atStartOfDay(userZoneId)
                .withZoneSameInstant(ZoneOffset.UTC);
        ZonedDateTime weekEndTime = weekEndDate.plusDays(1).atStartOfDay(userZoneId)
                .withZoneSameInstant(ZoneOffset.UTC)
                .minusNanos(1);
                
        ZonedDateTime solidifyEndWindow = weekEndDate.isBefore(today) ? weekEndTime : nowInUtc;
        
        return new WeekViewTimeContext(userZoneId, nowInUtc, today, weekStartDate, weekEndDate,
                weekStartTime, weekEndTime, solidifyEndWindow);
    }

    /**
     * Fetches confirmed events for the week view time range.
     *
     * @param viewer the user viewing the week
     * @param timeContext the time context for the week
     * @return list of confirmed events for the week
     */
    private List<EventResponseDTO> fetchConfirmedEventsForWeek(User viewer, WeekViewTimeContext timeContext) {
        return eventBO
                .getConfirmedEventsForUserInRange(viewer.getId(), timeContext.weekStartTime(), timeContext.weekEndTime())
                .stream()
                .map(eventResponseDTOFactory::createFromEvent)
                .toList();
    }

    /**
     * Generates virtual events for the week view if appropriate.
     *
     * @param viewer the user viewing the week
     * @param timeContext the time context for the week
     * @return list of virtual events for the week
     */
    private List<EventResponseDTO> generateVirtualEventsForWeek(User viewer, WeekViewTimeContext timeContext) {
        List<EventResponseDTO> virtualEvents = new ArrayList<>();
        if (timeContext.shouldGenerateVirtualEvents()) {
            virtualEvents.addAll(
                    recurringEventBO.generateVirtuals(viewer.getId(), timeContext.getVirtualsStartWindow(), 
                            timeContext.weekEndTime(), timeContext.userZoneId())
            );
        }
        return virtualEvents;
    }

    /**
     * Represents a time range with start and end boundaries.
     * 
     * @param start the start time of the range
     * @param end the end time of the range
     */
    private record TimeRange(ZonedDateTime start, ZonedDateTime end) {}

    /**
     * Utility class for resolving time filter logic into concrete time ranges.
     */
    private static final class TimeFilterResolver {
        
        /**
         * Resolves a time filter into a concrete time range.
         *
         * @param timeFilter the time filter type to resolve
         * @param start the custom start time (used only for CUSTOM filter)
         * @param end the custom end time (used only for CUSTOM filter)
         * @param now the current time for relative filters
         * @return resolved time range with concrete start and end times
         * @throws InvalidTimeException if CUSTOM filter has start time after end time
         */
        static TimeRange resolveTimeRange(TimeFilter timeFilter, ZonedDateTime start, 
                                        ZonedDateTime end, ZonedDateTime now) {
            ZonedDateTime resolvedStart;
            ZonedDateTime resolvedEnd;
            
            switch (timeFilter) {
                case ALL -> {
                    resolvedStart = TimeUtils.FAR_PAST;
                    resolvedEnd = TimeUtils.FAR_FUTURE;
                }
                case PAST_ONLY -> {
                    resolvedStart = TimeUtils.FAR_PAST;
                    resolvedEnd = now;
                }
                case FUTURE_ONLY -> {
                    resolvedStart = now;
                    resolvedEnd = TimeUtils.FAR_FUTURE;
                }
                case CUSTOM -> {
                    resolvedStart = start != null ? start : TimeUtils.FAR_PAST;
                    resolvedEnd = end != null ? end : TimeUtils.FAR_FUTURE;
                    
                    // Validate that start time is not after end time for CUSTOM filter
                    if (resolvedStart.isAfter(resolvedEnd)) {
                        throw new InvalidTimeException(ErrorCode.INVALID_TIME_RANGE, resolvedStart, resolvedEnd);
                    }
                }
                default -> throw new IllegalArgumentException("Unsupported time filter: " + timeFilter);
            }
            
            return new TimeRange(resolvedStart, resolvedEnd);
        }
    }

    @Override
    @Transactional
    public void unpinImpromptuEventForCurrentUser() {
        User currentUser = authenticatedUserProvider.getCurrentUser();
        logger.debug("Unpinning impromptu event for current user {}", currentUser.getId());
        
        currentUser.setPinnedImpromptuEvent(null);
        userBO.updateUser(currentUser);
        
        logger.info("Successfully unpinned impromptu event for current user {}", currentUser.getId());
    }
}
