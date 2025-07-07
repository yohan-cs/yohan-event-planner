package com.yohan.event_planner.service;

import com.blazebit.persistence.PagedList;
import com.yohan.event_planner.business.EventBO;
import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.business.handler.EventPatchHandler;
import com.yohan.event_planner.dao.EventDAO;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.TimeFilter;
import com.yohan.event_planner.dto.ConflictResolutionDTO;
import com.yohan.event_planner.dto.DayViewDTO;
import com.yohan.event_planner.dto.EventCreateDTO;
import com.yohan.event_planner.dto.EventFilterDTO;
import com.yohan.event_planner.dto.EventChangeContextDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventResponseDTOFactory;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.dto.WeekViewDTO;
import com.yohan.event_planner.exception.ConflictException;
import com.yohan.event_planner.exception.ErrorCode;
import com.yohan.event_planner.exception.EventAlreadyConfirmedException;
import com.yohan.event_planner.exception.EventNotFoundException;
import com.yohan.event_planner.exception.EventOwnershipException;
import com.yohan.event_planner.exception.InvalidTimeException;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import com.yohan.event_planner.time.ClockProvider;
import com.yohan.event_planner.time.TimeUtils;
import com.yohan.event_planner.validation.ConflictValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Concrete implementation of the {@link EventService} interface.
 *
 * <p>
 * Coordinates validation, business logic, and DTO conversion for managing {@link Event} entities.
 * This implementation enforces ownership checks via {@link OwnershipValidator} and delegates
 * domain logic to {@link EventBO}.
 * </p>
 *
 * <h2>Time Handling</h2>
 * <ul>
 *     <li>All start and end times are stored internally in UTC.</li>
 *     <li>Original time zone IDs are stored separately to support accurate local time display.</li>
 *     <li>Time zone adjustments for response objects are based on the viewer's profile time zone.</li>
 * </ul>
 *
 * <h2>Duration Handling</h2>
 * <ul>
 *     <li>{@code durationMinutes} is calculated during event creation if {@code endTime} is provided.</li>
 *     <li>During update operations, {@code durationMinutes} is updated or cleared via the patch handler.</li>
 *     <li>Duration is expressed in whole minutes and stored explicitly in the {@link Event} entity.</li>
 * </ul>
 *
 * <p>
 * This class is not responsible for persistence or direct repository access.
 * It delegates those concerns to the {@link EventBO}.
 * </p>
 */
@Service
public class EventServiceImpl implements EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventServiceImpl.class);

    private final EventBO eventBO;
    private final UserBO userBO;
    private final RecurringEventService recurringEventService;
    private final LabelService labelService;
    private final EventDAO eventDAO;
    private final EventPatchHandler eventPatchHandler;
    private final EventResponseDTOFactory eventResponseDTOFactory;
    private final OwnershipValidator ownershipValidator;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final ClockProvider clockProvider;

    public EventServiceImpl(
            EventBO eventBO,
            UserBO userBO,
            RecurringEventService recurringEventService,
            LabelService labelService,
            EventDAO eventDAO,
            EventPatchHandler eventPatchHandler,
            EventResponseDTOFactory eventResponseDTOFactory,
            OwnershipValidator ownershipValidator,
            AuthenticatedUserProvider authenticatedUserProvider,
            ClockProvider clockProvider
    ) {
        this.eventBO = eventBO;
        this.userBO = userBO;
        this.recurringEventService = recurringEventService;
        this.labelService = labelService;
        this.eventDAO = eventDAO;
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
                    recurringEventService.generateVirtuals(viewer.getId(), virtualsStartWindow, endOfDay, userZoneId)
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
                    recurringEventService.generateVirtuals(viewer.getId(), virtualsStartWindow, weekEndTime, userZoneId)
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
}
