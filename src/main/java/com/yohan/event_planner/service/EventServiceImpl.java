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
import com.yohan.event_planner.repository.EventRepository;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link EventService} providing comprehensive event management functionality.
 * 
 * <p>This service serves as the central orchestrator for event operations, coordinating complex 
 * business logic across multiple domains including time management, recurring events, labeling,
 * and user authorization. It provides full CRUD operations with advanced features like view 
 * generation, event confirmation workflows, and integration with recurring event patterns.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>Event Lifecycle Management</strong>: Create, read, update, delete with state validation</li>
 *   <li><strong>Confirmation Workflows</strong>: Support for draft → confirmed → completed transitions</li>
 *   <li><strong>Time Management</strong>: Advanced timezone handling with UTC storage and local display</li>
 *   <li><strong>Recurring Event Integration</strong>: Bidirectional sync with recurring event patterns</li>
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
    private final UserBO userBO;
    private final RecurringEventBO recurringEventBO;
    private final LabelService labelService;
    private final EventDAO eventDAO;
    private final EventRepository eventRepository;
    private final EventPatchHandler eventPatchHandler;
    private final EventResponseDTOFactory eventResponseDTOFactory;
    private final OwnershipValidator ownershipValidator;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final ClockProvider clockProvider;

    public EventServiceImpl(
            EventBO eventBO,
            UserBO userBO,
            RecurringEventBO recurringEventBO,
            LabelService labelService,
            EventDAO eventDAO,
            EventRepository eventRepository,
            EventPatchHandler eventPatchHandler,
            EventResponseDTOFactory eventResponseDTOFactory,
            OwnershipValidator ownershipValidator,
            AuthenticatedUserProvider authenticatedUserProvider,
            ClockProvider clockProvider
    ) {
        this.eventBO = eventBO;
        this.userBO = userBO;
        this.recurringEventBO = recurringEventBO;
        this.labelService = labelService;
        this.eventDAO = eventDAO;
        this.eventRepository = eventRepository;
        this.eventPatchHandler = eventPatchHandler;
        this.eventResponseDTOFactory = eventResponseDTOFactory;
        this.ownershipValidator = ownershipValidator;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.clockProvider = clockProvider;
    }

    @Override
    public EventResponseDTO getEventById(Long eventId) {
        User viewer = authenticatedUserProvider.getCurrentUser();
        Event event = eventBO.getEventById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // Hide unconfirmed events unless viewer is the creator
        if (event.isUnconfirmed() && !event.getCreator().getId().equals(viewer.getId())) {
            throw new EventNotFoundException(eventId);
        }

        return eventResponseDTOFactory.createFromEvent(event);
    }

    public Page<EventResponseDTO> getConfirmedEventsForCurrentUser(EventFilterDTO filter, int pageNumber, int pageSize) {
        User viewer = authenticatedUserProvider.getCurrentUser();

        ZonedDateTime now = ZonedDateTime.now(clockProvider.getClockForUser(viewer));
        ZonedDateTime start = filter.start();
        ZonedDateTime end = filter.end();

        switch (filter.timeFilter()) {
            case ALL -> {
                start = TimeUtils.FAR_PAST;
                end = TimeUtils.FAR_FUTURE;
            }
            case PAST_ONLY -> {
                start = TimeUtils.FAR_PAST;
                end = now;
            }
            case FUTURE_ONLY -> {
                start = now;
                end = TimeUtils.FAR_FUTURE;
            }
            case CUSTOM -> {
                if (start == null) start = TimeUtils.FAR_PAST;
                if (end == null) end = TimeUtils.FAR_FUTURE;
            }
        }

        // Validate that start time is not after end time for CUSTOM filter
        if (filter.timeFilter() == TimeFilter.CUSTOM && start.isAfter(end)) {
            throw new InvalidTimeException(ErrorCode.INVALID_TIME_RANGE, start, end);
        }

        EventFilterDTO sanitizedFilter = new EventFilterDTO(
                filter.labelId(),
                TimeFilter.ALL, // resolved meaning
                start,
                end,
                filter.sortDescending(),
                filter.includeIncompletePastEvents()
        );

        PagedList<Event> confirmedEvents = eventDAO.findConfirmedEvents(viewer.getId(), sanitizedFilter, pageNumber, pageSize);

        List<EventResponseDTO> dtos = confirmedEvents.stream()
                .map(eventResponseDTOFactory::createFromEvent)
                .toList();

        return new PageImpl<>(dtos, PageRequest.of(pageNumber, pageSize), confirmedEvents.getTotalSize());
    }


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
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponseDTO> getUnconfirmedEventsForCurrentUser() {
        User viewer = authenticatedUserProvider.getCurrentUser();

        List<Event> unconfirmedEvents = eventBO.getUnconfirmedEventsForUser(viewer.getId());

        return unconfirmedEvents.stream()
                .map(eventResponseDTOFactory::createFromEvent)
                .toList();
    }

    @Transactional
    public DayViewDTO generateDayView(LocalDate selectedDate) {
        User viewer = authenticatedUserProvider.getCurrentUser();
        ZoneId userZoneId = ZoneId.of(viewer.getTimezone());

        ZonedDateTime nowInUserzone = ZonedDateTime.now(clockProvider.getClockForUser(viewer));
        ZonedDateTime nowInUtc = nowInUserzone.withZoneSameInstant(ZoneOffset.UTC);
        LocalDate today = nowInUserzone.toLocalDate();

        ZonedDateTime startOfDay = selectedDate.atStartOfDay(userZoneId)
                .withZoneSameInstant(ZoneOffset.UTC);
        ZonedDateTime endOfDay = selectedDate.plusDays(1).atStartOfDay(userZoneId)
                .withZoneSameInstant(ZoneOffset.UTC)
                .minusNanos(1);

        // Solidify past recurrences
        if (!selectedDate.isAfter(today)) {
            ZonedDateTime solidifyEndWindow = selectedDate.isEqual(today) ? nowInUtc : endOfDay;
            eventBO.solidifyRecurrences(viewer.getId(), startOfDay, solidifyEndWindow, userZoneId);
        }

        // Fetch confirmed events
        List<EventResponseDTO> confirmedEvents = eventBO
                .getConfirmedEventsForUserInRange(viewer.getId(), startOfDay, endOfDay)
                .stream()
                .map(eventResponseDTOFactory::createFromEvent)
                .toList();

        // Generate virtual events if the selected date is today or in the future
        List<EventResponseDTO> virtualEvents = new ArrayList<>();
        if (!selectedDate.isBefore(today)) {
            ZonedDateTime virtualsStartWindow = selectedDate.isEqual(today) ? nowInUtc : startOfDay;
            virtualEvents.addAll(
                    recurringEventBO.generateVirtuals(viewer.getId(), virtualsStartWindow, endOfDay, userZoneId)
            );
        }

        // Delegate business logic to EventBO
        DayViewDTO result = eventBO.generateDayViewData(selectedDate, confirmedEvents, virtualEvents);
        
        logger.debug("Generated DayViewDTO for user {} for date {}", viewer.getId(), selectedDate);
        return result;
    }

    @Transactional
    public WeekViewDTO generateWeekView(LocalDate anchorDate) {
        User viewer = authenticatedUserProvider.getCurrentUser();
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

        // Solidify past recurrences
        if (!weekStartDate.isAfter(today)) {
            ZonedDateTime solidifyEndWindow = weekEndDate.isBefore(today)
                    ? weekEndTime
                    : nowInUtc;

            eventBO.solidifyRecurrences(viewer.getId(), weekStartTime, solidifyEndWindow, userZoneId);
        }

        // Fetch confirmed events
        List<EventResponseDTO> confirmedEvents = eventBO
                .getConfirmedEventsForUserInRange(viewer.getId(), weekStartTime, weekEndTime).stream()
                .map(eventResponseDTOFactory::createFromEvent)
                .toList();

        // Generate virtual events for future recurring events in the week
        List<EventResponseDTO> virtualEvents = new ArrayList<>();
        if (!weekEndDate.isBefore(today)) {
            ZonedDateTime virtualsStartWindow = weekStartDate.isAfter(today)
                    ? weekStartTime
                    : nowInUtc;

            virtualEvents.addAll(
                    recurringEventBO.generateVirtuals(viewer.getId(), virtualsStartWindow, weekEndTime, userZoneId)
            );
        }

        // Delegate business logic to EventBO
        WeekViewDTO result = eventBO.generateWeekViewData(viewer.getId(), anchorDate, userZoneId, confirmedEvents, virtualEvents);
        
        logger.debug("Generated WeekViewDTO for user {} for week starting {}", viewer.getId(), weekStartDate);
        return result;
    }

    @Override
    @Transactional
    public EventResponseDTO createEvent(EventCreateDTO dto) {
        User creator = authenticatedUserProvider.getCurrentUser();

        Event event = dto.isDraft()
                ? Event.createUnconfirmedDraft(dto.name(), dto.startTime(), dto.endTime(), creator)
                : Event.createEvent(dto.name(), dto.startTime(), dto.endTime(), creator);

        event.setDescription(dto.description());

        Label label = (dto.labelId() != null)
                ? labelService.getLabelEntityById(dto.labelId())
                : creator.getUnlabeled();

        event.setLabel(label);

        Event saved = eventBO.createEvent(event);
        return eventResponseDTOFactory.createFromEvent(saved);
    }

    @Override
    @Transactional
    public EventResponseDTO createImpromptuEvent() {
        User creator = authenticatedUserProvider.getCurrentUser();

        Clock userClock = clockProvider.getClockForUser(creator);
        ZonedDateTime now = ZonedDateTime.now(userClock);

        Event event = Event.createImpromptuEvent(now, creator);
        event.setLabel(creator.getUnlabeled());

        return eventResponseDTOFactory.createFromEvent(eventBO.createEvent(event));
    }

    @Override
    @Transactional
    public EventResponseDTO confirmEvent(Long eventId) {
        User viewer = authenticatedUserProvider.getCurrentUser();

        Event event = eventBO.getEventById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        ownershipValidator.validateEventOwnership(viewer.getId(), event);

        if (!event.isUnconfirmed()) {
            throw new EventAlreadyConfirmedException(eventId);
        }

        Event confirmed = eventBO.confirmEvent(event);

        return eventResponseDTOFactory.createFromEvent(confirmed);
    }

    @Override
    @Transactional
    public EventResponseDTO confirmAndCompleteEvent(Long eventId) {
        User viewer = authenticatedUserProvider.getCurrentUser();

        Event event = eventBO.getEventById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        ownershipValidator.validateEventOwnership(viewer.getId(), event);

        if (!event.isUnconfirmed()) {
            throw new EventAlreadyConfirmedException(eventId);
        }

        eventBO.confirmEvent(event);

        ZonedDateTime now = ZonedDateTime.now(clockProvider.getClockForUser(viewer));
        if (event.getEndTime().isAfter(now)) {
            throw new InvalidTimeException(
                    ErrorCode.INVALID_COMPLETION_STATUS,
                    event.getStartTime(),
                    event.getEndTime(),
                    now
            );
        }

        EventUpdateDTO completionPatch = new EventUpdateDTO(
                null,
                null,
                null,
                null,
                null,
                true
        );

        return updateEvent(eventId, completionPatch);
    }

    @Override
    @Transactional
    public EventResponseDTO updateEvent(Long eventId, EventUpdateDTO dto) {
        User viewer = authenticatedUserProvider.getCurrentUser();

        Event event = eventBO.getEventById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        ownershipValidator.validateEventOwnership(viewer.getId(), event);

        boolean snapshotNeeded = dto.startTime() != null
                || dto.endTime() != null
                || dto.labelId() != null
                || dto.isCompleted() != null;

        EventChangeContextDTO contextDTO = snapshotNeeded ? createSnapshotContext(event) : null;

        boolean changed = eventPatchHandler.applyPatch(event, dto);
        Event updated = changed ? eventBO.updateEvent(contextDTO, event) : event;

        return eventResponseDTOFactory.createFromEvent(updated);
    }

    @Override
    @Transactional
    public void deleteEvent(Long eventId) {
        Event existing = eventBO.getEventById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        User viewer = authenticatedUserProvider.getCurrentUser();
        ownershipValidator.validateEventOwnership(viewer.getId(), existing);

        eventBO.deleteEvent(eventId);
    }

    @Override
    @Transactional
    public void deleteUnconfirmedEventsForCurrentUser() {
        User viewer = authenticatedUserProvider.getCurrentUser();

        eventBO.deleteAllUnconfirmedEventsByUser(viewer.getId());
    }

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

    @Override
    @Transactional
    public int updateFutureEventsFromRecurringEvent(RecurringEvent recurringEvent, Set<String> changedFields, ZoneId userZoneId) {
        return eventBO.updateFutureEventsFromRecurringEvent(recurringEvent, changedFields, userZoneId);
    }
}
