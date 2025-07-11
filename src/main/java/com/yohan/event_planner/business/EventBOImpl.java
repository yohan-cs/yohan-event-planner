package com.yohan.event_planner.business;

import com.yohan.event_planner.business.handler.EventPatchHandler;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.dto.DayViewDTO;
import com.yohan.event_planner.dto.EventChangeContextDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.dto.WeekViewDTO;

import com.yohan.event_planner.exception.EventAlreadyConfirmedException;
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
import java.util.Set;

import static com.yohan.event_planner.exception.ErrorCode.INVALID_COMPLETION_STATUS;
import static com.yohan.event_planner.exception.ErrorCode.INVALID_EVENT_TIME;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_END_TIME;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_LABEL;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_NAME;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_START_TIME;

/**
 * Business Object implementation for managing {@link Event} entities.
 * 
 * <p><strong>Architectural Role:</strong> This component sits in the Business Object layer,
 * orchestrating domain validation, conflict detection, and business rule enforcement
 * between the Service layer and Repository layer. It handles both individual events
 * and recurring event solidification.</p>
 * 
 * <p><strong>Validation Strategy:</strong> Uses a two-phase approach:
 * <ul>
 *   <li><strong>Unconfirmed (Draft) Events:</strong> Minimal validation for flexible data entry</li>
 *   <li><strong>Confirmed Events:</strong> Full validation including field checks, time validation, and conflict detection</li>
 * </ul></p>
 * 
 * <p><strong>Key Responsibilities:</strong>
 * <ul>
 *   <li>Event CRUD operations with domain validation</li>
 *   <li>Recurring event solidification (virtual → physical instances)</li>
 *   <li>Calendar view data generation (day/week views)</li>
 *   <li>Future event propagation from recurring event changes</li>
 *   <li>Completion status validation and time bucket integration</li>
 * </ul></p>
 * 
 * <p><strong>Dependencies:</strong>
 * <ul>
 *   <li>{@link ConflictValidator} - Scheduling conflict detection</li>
 *   <li>{@link RecurringEventBO} - Recurring event operations</li>
 *   <li>{@link EventPatchHandler} - Event field updates</li>
 *   <li>{@link LabelTimeBucketService} - Time tracking integration</li>
 *   <li>{@link ClockProvider} - Timezone-aware time operations</li>
 * </ul></p>
 * 
 * <p><strong>Authorization:</strong> Assumes all authorization and ownership checks are handled
 * upstream in the service layer. This class focuses on business logic and domain validation.</p>
 */
@Service
public class EventBOImpl implements EventBO {

    private static final Logger logger = LoggerFactory.getLogger(EventBOImpl.class);
    private static final ZoneId UTC = ZoneId.of("UTC");
    private final RecurringEventBO recurringEventBO;
    private final RecurrenceRuleService recurrenceRuleService;
    private final LabelTimeBucketService labelTimeBucketService;
    private final EventRepository eventRepository;
    private final EventPatchHandler eventPatchHandler;
    private final ConflictValidator conflictValidator;
    private final ClockProvider clockProvider;

    public EventBOImpl(
            RecurringEventBO recurringEventBO,
            RecurrenceRuleService recurrenceRuleService,
            LabelTimeBucketService labelTimeBucketService,
            EventRepository eventRepository,
            EventPatchHandler eventPatchHandler,
            ConflictValidator conflictValidator,
            ClockProvider clockProvider)
    {
        this.recurringEventBO = recurringEventBO;
        this.recurrenceRuleService = recurrenceRuleService;
        this.labelTimeBucketService = labelTimeBucketService;
        this.eventRepository = eventRepository;
        this.eventPatchHandler = eventPatchHandler;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Event> getConfirmedEventsForUserInRange(Long userId, ZonedDateTime windowStart, ZonedDateTime windowEnd) {
        logger.debug("Fetching confirmed events for User ID {} between {} and {}", userId, windowStart, windowEnd);
        return eventRepository.findConfirmedEventsForUserBetween(userId, windowStart, windowEnd);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Event> getConfirmedEventsPage(
            Long userId,
            ZonedDateTime endTimeCursor,
            ZonedDateTime startTimeCursor,
            Long idCursor,
            int limit
    ) {
        if (endTimeCursor == null || startTimeCursor == null || idCursor == null) {
            logger.debug("Using initial pagination for user {} with limit {}", userId, limit);
            return eventRepository.findTopConfirmedByUserIdOrderByEndTimeDescStartTimeDescIdDesc(
                    userId,
                    PageRequest.of(0, limit)
            );
        } else {
            logger.debug("Using cursor-based pagination for user {} with cursors: endTime={}, startTime={}, id={}", 
                    userId, endTimeCursor, startTimeCursor, idCursor);
            return eventRepository.findConfirmedByUserIdBeforeCursor(
                    userId,
                    endTimeCursor,
                    startTimeCursor,
                    idCursor,
                    PageRequest.of(0, limit)
            );
        }
    }


    /**
     * {@inheritDoc}
     */
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


    /**
     * {@inheritDoc}
     */
    @Override
    public void solidifyRecurrences(
            Long userId,
            ZonedDateTime startTime,
            ZonedDateTime endTime,
            ZoneId userZoneId
    ) {
        logger.info("Solidifying recurrences for user {} between {} and {}", userId, startTime, endTime);
        List<RecurringEvent> recurrences = recurringEventBO.getConfirmedRecurringEventsForUserInRange(
                userId, startTime.toLocalDate(), endTime.toLocalDate()
        );
        
        if (recurrences.isEmpty()) {
            logger.debug("No recurring events found for user {} in specified time range", userId);
            return;
        }
        
        logger.debug("Found {} recurring events to solidify for user {}", recurrences.size(), userId);
        for (RecurringEvent recurrence : recurrences) {
            solidifyVirtualOccurrences(recurrence, startTime, endTime, userZoneId);
        }
        logger.info("Completed solidification for user {}", userId);
    }

    /**
     * Solidifies virtual occurrences of a recurring event within the specified time window.
     *
     * <p>
     * This method expands the recurrence rule to generate individual event instances,
     * checking for existing events and conflicts. Events are created as confirmed if no
     * conflicts exist, or as unconfirmed drafts if conflicts are detected.
     * </p>
     *
     * @param recurrence the recurring event to solidify occurrences for
     * @param windowStart the start of the time window for solidification
     * @param windowEnd the end of the time window for solidification
     * @param userZoneId the user's timezone for date calculations
     */
    private void solidifyVirtualOccurrences(
            RecurringEvent recurrence,
            ZonedDateTime windowStart,
            ZonedDateTime windowEnd,
            ZoneId userZoneId
    ) {
        logger.debug("Solidifying virtual occurrences for recurring event {} '{}'", 
                recurrence.getId(), recurrence.getName());
        List<LocalDate> occurrenceDates = recurrenceRuleService.expandRecurrence(
                recurrence.getRecurrenceRule().getParsed(),
                windowStart.toLocalDate(),
                windowEnd.toLocalDate(),
                recurrence.getSkipDays()
        );
        
        if (occurrenceDates.isEmpty()) {
            logger.debug("No occurrence dates found for recurring event {}", recurrence.getId());
            return;
        }
        
        logger.debug("Found {} potential occurrence dates for recurring event {}", 
                occurrenceDates.size(), recurrence.getId());

        List<Event> solidifiedEvents = getAllConfirmedEventsAndSolidifiedRecurringInstanceForUserAndRecurringEventBetween(
                recurrence.getCreator().getId(),
                recurrence.getId(),
                windowStart,
                windowEnd
        );

        for (LocalDate date : occurrenceDates) {
            ZonedDateTime startTime = ZonedDateTime.of(date, recurrence.getStartTime(), userZoneId)
                    .withZoneSameInstant(UTC);
            ZonedDateTime endTime = ZonedDateTime.of(date, recurrence.getEndTime(), userZoneId)
                    .withZoneSameInstant(UTC);

            if (!endTime.isBefore(windowEnd)) {
                continue;
            }

            boolean instanceExists = solidifiedEvents.stream()
                    .anyMatch(e -> e.getStartTime().withZoneSameInstant(userZoneId).toLocalDate().equals(date));

            if (instanceExists) {
                logger.debug("Event instance already exists for recurring event {} on date {}", 
                        recurrence.getId(), date);
                continue;
            }

            boolean hasConflict = solidifiedEvents.stream()
                    .anyMatch(e -> e.getStartTime().isBefore(endTime) && e.getEndTime().isAfter(startTime));

            Event event = hasConflict
                    ? Event.createUnconfirmedDraft(recurrence.getName(), startTime, endTime, recurrence.getCreator())
                    : Event.createEvent(recurrence.getName(), startTime, endTime, recurrence.getCreator());
            
            if (hasConflict) {
                logger.debug("Creating unconfirmed draft due to conflict for recurring event {} on {}", 
                        recurrence.getId(), date);
            } else {
                logger.debug("Creating confirmed event for recurring event {} on {}", 
                        recurrence.getId(), date);
            }

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

        validateCompletionStatusChange(event, wasCompleted, isNowCompleted);

        Event saved = eventRepository.save(event);

        if ((contextDTO != null) && (wasCompleted || isNowCompleted)) {
            EventChangeContextDTO context = buildChangeContext(contextDTO, event);
            labelTimeBucketService.handleEventChange(context);
        }

        return saved;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Event confirmEvent(Event event) {
        logger.info("Confirming event ID {}", event.getId());

        if (!event.isUnconfirmed()) {
            throw new EventAlreadyConfirmedException(event.getId());
        }

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

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteAllUnconfirmedEventsByUser(Long userId) {
        logger.info("Deleting all unconfirmed events for  User ID {}", userId);
        eventRepository.deleteAllUnconfirmedEventsByUser(userId);
    }


    // region: Private Methods
    
    /**
     * Validates that completion status changes are allowed based on event timing.
     *
     * <p>
     * Events can only be marked as completed if their end time has already passed.
     * This prevents users from marking future events as completed.
     * </p>
     *
     * @param event the event being updated
     * @param wasCompleted the previous completion status
     * @param isNowCompleted the new completion status
     * @throws InvalidTimeException if attempting to complete a future event
     */
    private void validateCompletionStatusChange(Event event, boolean wasCompleted, boolean isNowCompleted) {
        if (!wasCompleted && isNowCompleted) {
            logger.debug("Validating completion status change for event {}", event.getId());
            ZonedDateTime now = ZonedDateTime.now(clockProvider.getClockForUser(event.getCreator()));
            if (event.getEndTime().isAfter(now)) {
                logger.warn("Cannot complete future event {}: ends at {} but current time is {}", 
                        event.getId(), event.getEndTime(), now);
                throw new InvalidTimeException(
                        INVALID_COMPLETION_STATUS,
                        event.getStartTime(),
                        event.getEndTime(),
                        now
                );
            }
            logger.debug("Event {} completion validation passed", event.getId());
        }
    }
    
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

    /**
     * Validates all required fields for a confirmed event.
     * 
     * <p>Performs comprehensive validation including:
     * <ul>
     *   <li>Required field presence (name, times, label)</li>
     *   <li>String field non-blank validation</li>
     * </ul></p>
     * 
     * <p>This validation is only applied to confirmed events. Draft events bypass
     * validation to allow flexible data entry.</p>
     * 
     * @param event the event to validate
     * @throws InvalidEventStateException if any required field is missing or invalid
     */
    private void validateConfirmedEventFields(Event event) {
        logger.debug("Validating confirmed event fields for event {}", event.getId());
        validateRequiredStringField(event.getName(), MISSING_EVENT_NAME);
        validateRequiredField(event.getStartTime(), MISSING_EVENT_START_TIME);
        validateRequiredField(event.getEndTime(), MISSING_EVENT_END_TIME);
        validateRequiredField(event.getLabel(), MISSING_EVENT_LABEL);
        logger.debug("Event {} field validation passed", event.getId());
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
            logger.warn("Invalid time range: start {} is not before end {}", start, end);
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

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int updateFutureEventsFromRecurringEvent(RecurringEvent recurringEvent, Set<String> changedFields, ZoneId userZoneId) {
        if (changedFields.isEmpty()) {
            return 0;
        }

        logger.info("Updating future events for recurring event {} with changed fields: {}", 
                    recurringEvent.getId(), changedFields);

        ZonedDateTime currentTime = ZonedDateTime.now(clockProvider.getClockForZone(userZoneId));
        List<Event> futureEvents = eventRepository.findFutureEventsByRecurringEventId(
                recurringEvent.getId(), currentTime);

        if (futureEvents.isEmpty()) {
            logger.info("No future events found for recurring event {}", recurringEvent.getId());
            return 0;
        }

        int updatedCount = 0;
        for (Event event : futureEvents) {
            LocalDate occurrenceDate = event.getStartTime().withZoneSameInstant(userZoneId).toLocalDate();
            
            // Create EventUpdateDTO with only the changed fields
            EventUpdateDTO updateDTO = createEventUpdateDTOFromRecurringEvent(
                    recurringEvent, changedFields, occurrenceDate, userZoneId);
            
            boolean wasUpdated = eventPatchHandler.applyPatch(event, updateDTO);
            
            if (wasUpdated) {
                eventRepository.save(event);
                updatedCount++;
                logger.debug("Updated event {} from recurring event {}", event.getId(), recurringEvent.getId());
            }
        }

        logger.info("Updated {} future events for recurring event {}", updatedCount, recurringEvent.getId());
        return updatedCount;
    }

    /**
     * Creates an EventUpdateDTO from a RecurringEvent for updating individual event instances.
     *
     * <p>
     * This method constructs a partial update DTO containing only the fields that have changed
     * in the recurring event. The DTO is used to apply consistent updates to all future
     * event instances created from the recurring event.
     * </p>
     *
     * @param recurringEvent the recurring event with updated values
     * @param changedFields the set of field names that have changed
     * @param occurrenceDate the specific date for this event occurrence
     * @param userZoneId the user's timezone for time calculations
     * @return an EventUpdateDTO containing only the changed fields
     */
    private EventUpdateDTO createEventUpdateDTOFromRecurringEvent(
            RecurringEvent recurringEvent, 
            Set<String> changedFields, 
            LocalDate occurrenceDate, 
            ZoneId userZoneId) {
        
        Optional<String> name = changedFields.contains("name") 
                ? Optional.of(recurringEvent.getName()) 
                : null;
        
        Optional<ZonedDateTime> startTime = changedFields.contains("startTime")
                ? Optional.of(ZonedDateTime.of(occurrenceDate, recurringEvent.getStartTime(), userZoneId)
                        .withZoneSameInstant(UTC))
                : null;
        
        Optional<ZonedDateTime> endTime = changedFields.contains("endTime")
                ? Optional.of(ZonedDateTime.of(occurrenceDate, recurringEvent.getEndTime(), userZoneId)
                        .withZoneSameInstant(UTC))
                : null;
        
        Optional<Long> labelId = changedFields.contains("label")
                ? Optional.of(recurringEvent.getLabel().getId())
                : null;
        
        return new EventUpdateDTO(name, startTime, endTime, null, labelId, null);
    }

    /**
     * Validates that a required field is not null.
     * 
     * @param field the field to validate
     * @param errorCode the error code to throw if validation fails
     * @throws InvalidEventStateException if the field is null
     */
    private void validateRequiredField(Object field, com.yohan.event_planner.exception.ErrorCode errorCode) {
        if (field == null) {
            logger.warn("Required field validation failed: {}", errorCode);
            throw new InvalidEventStateException(errorCode);
        }
    }

    /**
     * Validates that a required string field is not null or blank.
     * 
     * @param field the string field to validate
     * @param errorCode the error code to throw if validation fails
     * @throws InvalidEventStateException if the field is null or blank
     */
    private void validateRequiredStringField(String field, com.yohan.event_planner.exception.ErrorCode errorCode) {
        if (field == null || field.isBlank()) {
            logger.warn("Required string field validation failed: {}", errorCode);
            throw new InvalidEventStateException(errorCode);
        }
    }
}
