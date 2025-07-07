package com.yohan.event_planner.business;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.dto.DayViewDTO;
import com.yohan.event_planner.dto.EventChangeContextDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.WeekViewDTO;

import com.yohan.event_planner.exception.InvalidEventStateException;
import com.yohan.event_planner.exception.InvalidTimeException;
import com.yohan.event_planner.repository.EventRepository;
import com.yohan.event_planner.service.LabelTimeBucketService;
import com.yohan.event_planner.service.RecurrenceRuleService;
import com.yohan.event_planner.time.ClockProvider;
import com.yohan.event_planner.validation.ConflictValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.yohan.event_planner.exception.ErrorCode.EVENT_ALREADY_CONFIRMED;
import static com.yohan.event_planner.exception.ErrorCode.INVALID_COMPLETION_STATUS;
import static com.yohan.event_planner.exception.ErrorCode.INVALID_EVENT_TIME;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_END_TIME;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_LABEL;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_NAME;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_START_TIME;

/**
 * Concrete implementation of the {@link EventBO} interface.
 *
 * <p>
 * Provides business logic and coordination for managing {@link Event} entities.
 * This implementation delegates persistence operations to the {@link EventRepository},
 * and is responsible for enforcing domain rules such as time validation and conflict detection.
 * </p>
 *
 * <p>
 * Assumes all authorization and ownership checks are handled upstream
 * in the service layer. This class is not defensive and expects well-formed inputs.
 * </p>
 */
@Service
public class EventBOImpl implements EventBO {

    private static final Logger logger = LoggerFactory.getLogger(EventBOImpl.class);
    private final RecurringEventBO recurringEventBO;
    private final RecurrenceRuleService recurrenceRuleService;
    private final LabelTimeBucketService labelTimeBucketService;
    private final EventRepository eventRepository;
    private final ConflictValidator conflictValidator;
    private final ClockProvider clockProvider;

    public EventBOImpl(
            RecurringEventBO recurringEventBO,
            RecurrenceRuleService recurrenceRuleService,
            LabelTimeBucketService labelTimeBucketService,
            EventRepository eventRepository,
            ConflictValidator conflictValidator,
            ClockProvider clockProvider)
    {
        this.recurringEventBO = recurringEventBO;
        this.recurrenceRuleService = recurrenceRuleService;
        this.labelTimeBucketService = labelTimeBucketService;
        this.eventRepository = eventRepository;
        this.conflictValidator = conflictValidator;
        this.clockProvider = clockProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Event> getEventById(Long eventId) {
        logger.debug("Fetching event by ID {}", eventId);
        return eventRepository.findById(eventId);
    }

    @Override
    public List<Event> getConfirmedEventsForUserInRange(Long userId, ZonedDateTime windowStart, ZonedDateTime windowEnd) {
        logger.debug("Fetching confirmed events for User ID {} between {} and {}", userId, windowStart, windowEnd);
        return eventRepository.findConfirmedEventsForUserBetween(userId, windowStart, windowEnd);

    }

    @Override
    public List<Event> getConfirmedEventsPage(
            Long userId,
            ZonedDateTime endTimeCursor,
            ZonedDateTime startTimeCursor,
            Long idCursor,
            int limit
    ) {
        if (endTimeCursor == null || startTimeCursor == null || idCursor == null) {
            return eventRepository.findTopConfirmedByUserIdOrderByEndTimeDescStartTimeDescIdDesc(
                    userId,
                    PageRequest.of(0, limit)
            );
        } else {
            return eventRepository.findConfirmedByUserIdBeforeCursor(
                    userId,
                    endTimeCursor,
                    startTimeCursor,
                    idCursor,
                    PageRequest.of(0, limit)
            );
        }
    }


    @Override
    public List<Event> getUnconfirmedEventsForUser(Long userId) {
        logger.debug("Fetching unconfirmed events for User ID {}", userId);
        return eventRepository.findUnconfirmedEventsForUserSortedByStartTime(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Event createEvent(Event event) {
        if (!event.isUnconfirmed()) {
            logger.info("Creating scheduled event '{}'", event.getName());
            validateConfirmedEventFields(event);
            validateStartBeforeEnd(event.getStartTime(), event.getEndTime());
            conflictValidator.validateNoConflicts(event);
        } else {
            logger.info("Creating draft event for user ID {}", event.getCreator().getId());
        }

        return eventRepository.save(event);
    }


    @Override
    public void solidifyRecurrences(
            Long userId,
            ZonedDateTime startTime,
            ZonedDateTime endTime,
            ZoneId userZoneId
    ) {
        List<RecurringEvent> recurrences = recurringEventBO.getConfirmedRecurringEventsForUserInRange(
                userId, startTime.toLocalDate(), endTime.toLocalDate()
        );

        for (RecurringEvent recurrence : recurrences) {
            solidifyVirtualOccurrences(recurrence, startTime, endTime, userZoneId);
        }
    }

    private void solidifyVirtualOccurrences(
            RecurringEvent recurrence,
            ZonedDateTime windowStart,
            ZonedDateTime windowEnd,
            ZoneId userZoneId
    ) {
        List<LocalDate> occurrenceDates = recurrenceRuleService.expandRecurrence(
                recurrence.getRecurrenceRule().getParsed(),
                windowStart.toLocalDate(),
                windowEnd.toLocalDate(),
                recurrence.getSkipDays()
        );

        List<Event> solidifiedEvents = getAllConfirmedEventsAndSolidifiedRecurringInstanceForUserAndRecurringEventBetween(
                recurrence.getCreator().getId(),
                recurrence.getId(),
                windowStart,
                windowEnd
        );

        for (LocalDate date : occurrenceDates) {
            ZonedDateTime startTime = ZonedDateTime.of(date, recurrence.getStartTime(), userZoneId)
                    .withZoneSameInstant(ZoneId.of("UTC"));
            ZonedDateTime endTime = ZonedDateTime.of(date, recurrence.getEndTime(), userZoneId)
                    .withZoneSameInstant(ZoneId.of("UTC"));

            if (!endTime.isBefore(windowEnd)) {
                continue;
            }

            boolean instanceExists = solidifiedEvents.stream()
                    .anyMatch(e -> e.getStartTime().withZoneSameInstant(userZoneId).toLocalDate().equals(date));

            if (instanceExists) {
                continue;
            }

            boolean hasConflict = solidifiedEvents.stream()
                    .anyMatch(e -> e.getStartTime().isBefore(endTime) && e.getEndTime().isAfter(startTime));

            Event event = hasConflict
                    ? Event.createUnconfirmedDraft(recurrence.getName(), startTime, endTime, recurrence.getCreator())
                    : Event.createEvent(recurrence.getName(), startTime, endTime, recurrence.getCreator());

            event.setDescription(recurrence.getDescription());
            event.setLabel((recurrence.getLabel() != null)
                    ? recurrence.getLabel()
                    : recurrence.getCreator().getUnlabeled());
            event.setRecurringEvent(recurrence);

            createEvent(event);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Event updateEvent(EventChangeContextDTO contextDTO, Event event) {
        logger.info("Updating event ID {}", event.getId());

        if (!event.isUnconfirmed()) {
            validateConfirmedEventFields(event);
            validateStartBeforeEnd(event.getStartTime(), event.getEndTime());
            conflictValidator.validateNoConflicts(event);
        }

        boolean wasCompleted = contextDTO != null && contextDTO.wasCompleted();
        boolean isNowCompleted = event.isCompleted();

        if (!wasCompleted && isNowCompleted) {
            ZonedDateTime now = ZonedDateTime.now(clockProvider.getClockForUser(event.getCreator()));


            if (event.getEndTime().isAfter(now)) {
                throw new InvalidTimeException(
                        INVALID_COMPLETION_STATUS,
                        event.getStartTime(),
                        event.getEndTime(),
                        now
                );
            }
        }

        Event saved = eventRepository.save(event);

        if ((contextDTO != null) && (wasCompleted || isNowCompleted)) {
            EventChangeContextDTO context = buildChangeContext(contextDTO, event);
            labelTimeBucketService.handleEventChange(context);
        }

        return saved;
    }

    @Override
    @Transactional
    public Event confirmEvent(Event event) {
        logger.info("Confirming event ID {}", event.getId());

        validateConfirmedEventFields(event);
        validateStartBeforeEnd(event.getStartTime(), event.getEndTime());
        conflictValidator.validateNoConflicts(event);

        event.setUnconfirmed(false);
        return eventRepository.save(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteEvent(Long eventId) {
        logger.info("Deleting event ID {}", eventId);
        eventRepository.deleteById(eventId);
    }

    @Override
    public void deleteAllUnconfirmedEventsByUser(Long userId) {
        logger.info("Deleting all unconfirmed events for  User ID {}", userId);
        eventRepository.deleteAllUnconfirmedEventsByUser(userId);
    }


    // region: Private Methods
    private EventChangeContextDTO buildChangeContext(EventChangeContextDTO contextDTO, Event after) {
        return new EventChangeContextDTO(
                contextDTO.userId(),
                contextDTO.oldLabelId(),
                after.getLabel().getId(),
                contextDTO.oldStartTime(),
                after.getStartTime(),
                contextDTO.oldDurationMinutes(),
                after.getDurationMinutes(),
                contextDTO.timezone(),
                contextDTO.wasCompleted(),
                after.isCompleted()
        );
    }

    // region: Private Validation Methods

    private void validateConfirmedEventFields(Event event) {
        if (event.getName() == null || event.getName().isBlank()) {
            throw new InvalidEventStateException(MISSING_EVENT_NAME);
        }
        if (event.getStartTime() == null) {
            throw new InvalidEventStateException(MISSING_EVENT_START_TIME);
        }
        if (event.getEndTime() == null) {
            throw new InvalidEventStateException(MISSING_EVENT_END_TIME);
        }
        if (event.getLabel() == null) {
            throw new InvalidEventStateException(MISSING_EVENT_LABEL);
        }
    }

    /**
     * Ensures that a given start time occurs strictly before the end time.
     *
     * @param start the proposed start time (must not be null)
     * @param end   the proposed end time (must not be null)
     * @throws InvalidTimeException if {@code start} is equal to or after {@code end}
     */
    private void validateStartBeforeEnd(ZonedDateTime start, ZonedDateTime end) {
        if (!start.isBefore(end)) {
            throw new InvalidTimeException(INVALID_EVENT_TIME, start, end);
        }
    }

    private List<Event> getAllConfirmedEventsAndSolidifiedRecurringInstanceForUserAndRecurringEventBetween(
            Long userId,
            Long recurrenceId,
            ZonedDateTime start,
            ZonedDateTime end
    ) {
        return eventRepository.findConfirmedAndRecurringDraftsByUserAndRecurrenceIdBetween(
                userId, recurrenceId, start, end
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DayViewDTO generateDayViewData(LocalDate selectedDate, List<EventResponseDTO> confirmedEvents, List<EventResponseDTO> virtualEvents) {
        logger.debug("Generating day view data for date {}", selectedDate);
        
        // Combine confirmed and virtual events
        List<EventResponseDTO> allEvents = new ArrayList<>(confirmedEvents);
        allEvents.addAll(virtualEvents);
        
        // Sort events by start time
        allEvents.sort(Comparator.comparing(EventResponseDTO::startTimeUtc));
        
        return new DayViewDTO(selectedDate, allEvents);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WeekViewDTO generateWeekViewData(Long userId, LocalDate anchorDate, ZoneId userZoneId, 
                                          List<EventResponseDTO> confirmedEvents, List<EventResponseDTO> virtualEvents) {
        logger.debug("Generating week view data for user {} for week containing {}", userId, anchorDate);
        
        // Calculate week boundaries (Monday to Sunday)
        LocalDate weekStartDate = anchorDate.with(DayOfWeek.MONDAY);
        
        // Combine confirmed and virtual events
        List<EventResponseDTO> allEvents = new ArrayList<>(confirmedEvents);
        allEvents.addAll(virtualEvents);
        
        // Sort events by start time
        allEvents.sort(Comparator.comparing(EventResponseDTO::startTimeUtc));
        
        // Group events by day
        Map<LocalDate, List<EventResponseDTO>> dayMap = groupEventsByDay(allEvents, userZoneId);
        
        // Create DayViewDTO for each day of the week
        List<DayViewDTO> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStartDate.plusDays(i);
            List<EventResponseDTO> eventsForDay = dayMap.getOrDefault(date, List.of());
            days.add(new DayViewDTO(date, eventsForDay));
        }
        
        return new WeekViewDTO(days);
    }

    /**
     * Groups events by the local dates they span in the given timezone.
     * 
     * <p>
     * Events that span multiple days will appear in the map for each day they occur.
     * This is pure business logic for handling multi-day event distribution.
     * </p>
     *
     * @param events the list of events to group
     * @param userZoneId the timezone to use for local date calculations
     * @return a map where keys are local dates and values are lists of events occurring on that date
     */
    private Map<LocalDate, List<EventResponseDTO>> groupEventsByDay(List<EventResponseDTO> events, ZoneId userZoneId) {
        Map<LocalDate, List<EventResponseDTO>> dayMap = new HashMap<>();

        for (EventResponseDTO event : events) {
            // Convert UTC times into user's local dates for grouping
            LocalDate startDayLocal = event.startTimeUtc().withZoneSameInstant(userZoneId).toLocalDate();
            LocalDate endDayLocal = event.endTimeUtc().withZoneSameInstant(userZoneId).toLocalDate();

            LocalDate cursor = startDayLocal;
            while (!cursor.isAfter(endDayLocal)) {
                dayMap.computeIfAbsent(cursor, d -> new ArrayList<>()).add(event);
                cursor = cursor.plusDays(1);
            }
        }

        return dayMap;
    }
}
