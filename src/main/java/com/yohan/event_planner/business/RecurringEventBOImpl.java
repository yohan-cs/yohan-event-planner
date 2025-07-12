package com.yohan.event_planner.business;

import com.yohan.event_planner.domain.RecurrenceRuleVO;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventResponseDTOFactory;
import com.yohan.event_planner.exception.ConflictException;
import com.yohan.event_planner.exception.ErrorCode;
import com.yohan.event_planner.exception.InvalidEventStateException;
import com.yohan.event_planner.exception.InvalidTimeException;
import com.yohan.event_planner.exception.RecurringEventAlreadyConfirmedException;
import com.yohan.event_planner.repository.RecurringEventRepository;
import com.yohan.event_planner.service.ParsedRecurrenceInput;
import com.yohan.event_planner.service.RecurrenceRuleService;
import com.yohan.event_planner.time.ClockProvider;
import com.yohan.event_planner.validation.ConflictValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.yohan.event_planner.exception.ErrorCode.INVALID_EVENT_TIME;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_END_TIME;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_LABEL;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_NAME;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_START_DATE;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_START_TIME;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_RECURRENCE_RULE;

/**
 * Business Object implementation for managing {@link RecurringEvent} entities.
 * 
 * <p><strong>Architectural Role:</strong> This component sits in the Business Object layer,
 * orchestrating domain validation, conflict detection, and business rule enforcement
 * between the Service layer and Repository layer.</p>
 * 
 * <p><strong>Validation Strategy:</strong> Uses a two-phase approach:
 * <ul>
 *   <li><strong>Unconfirmed (Draft) Events:</strong> Minimal validation for flexible data entry</li>
 *   <li><strong>Confirmed Events:</strong> Full validation including field checks, time validation, and conflict detection</li>
 * </ul></p>
 * 
 * <p><strong>Dependencies:</strong>
 * <ul>
 *   <li>{@link ConflictValidator} - Scheduling conflict detection</li>
 *   <li>{@link RecurrenceRuleService} - Pattern parsing and expansion</li>
 *   <li>{@link ClockProvider} - Timezone-aware time operations</li>
 * </ul></p>
 */
@Service
public class RecurringEventBOImpl implements RecurringEventBO {

    private static final Logger logger = LoggerFactory.getLogger(RecurringEventBOImpl.class);

    private final RecurringEventRepository recurringEventRepository;
    private final RecurrenceRuleService recurrenceRuleService;
    private final EventResponseDTOFactory eventResponseDTOFactory;
    private final ClockProvider clockProvider;
    private final ConflictValidator conflictValidator;

    /**
     * Constructs a new RecurringEventBOImpl with the required dependencies.
     * 
     * @param recurringEventRepository repository for recurring event data access
     * @param recurrenceRuleService service for parsing and expanding recurrence patterns
     * @param eventResponseDTOFactory factory for creating event response DTOs
     * @param clockProvider provider for timezone-aware clock operations
     * @param conflictValidator validator for detecting scheduling conflicts
     */
    public RecurringEventBOImpl(
            RecurringEventRepository recurringEventRepository,
            RecurrenceRuleService recurrenceRuleService,
            EventResponseDTOFactory eventResponseDTOFactory,
            ClockProvider clockProvider,
            ConflictValidator conflictValidator
    ) {
        this.recurringEventRepository = recurringEventRepository;
        this.recurrenceRuleService = recurrenceRuleService;
        this.eventResponseDTOFactory = eventResponseDTOFactory;
        this.clockProvider = clockProvider;
        this.conflictValidator = conflictValidator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<RecurringEvent> getRecurringEventById(Long recurringEventId) {
        logger.debug("Fetching recurring event ID {}", recurringEventId);
        return recurringEventRepository.findById(recurringEventId);
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Implementation Details:</strong>
     * Delegates to repository layer with exact parameter mapping to ensure correct date range queries.
     * Critical: Parameter order must match repository method signature exactly.</p>
     * 
     * @param userId the ID of the user whose confirmed recurring events to retrieve
     * @param fromDate the start date of the range (inclusive) - passed as fromDate to repository
     * @param toDate the end date of the range (inclusive) - passed as toDate to repository
     */
    @Override
    public List<RecurringEvent> getConfirmedRecurringEventsForUserInRange(Long userId, LocalDate fromDate, LocalDate toDate) {
        logger.debug("Fetching confirmed recurring events for user ID {} in range {} to {}", userId, fromDate, toDate);
        return recurringEventRepository.findConfirmedRecurringEventsForUserBetween(
                userId, fromDate, toDate
        );
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Implementation Details:</strong>
     * Uses cursor-based pagination to ensure consistent ordering even when new events are added.
     * When no cursor is provided, starts from the most recent events ordered by end date descending, then ID descending.</p>
     */
    @Override
    public List<RecurringEvent> getConfirmedRecurringEventsPage(
            Long userId,
            LocalDate endDateCursor,
            LocalDate startDateCursor,
            LocalTime startTimeCursor,
            LocalTime endTimeCursor,
            Long idCursor,
            int limit
    ) {
        logger.debug("Fetching confirmed recurring events page for user ID {} with limit {} (cursor: {})", 
                    userId, limit, (endDateCursor != null && idCursor != null) ? "provided" : "none");
        
        boolean hasCursor = (endDateCursor != null && idCursor != null);
        PageRequest pageRequest = PageRequest.of(0, limit);
        
        return hasCursor 
            ? recurringEventRepository.findConfirmedByUserIdBeforeCursor(
                userId, endDateCursor, startDateCursor, startTimeCursor, endTimeCursor, idCursor, pageRequest)
            : recurringEventRepository.findTopConfirmedByUserIdOrderByEndDateDescIdDesc(userId, pageRequest);
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Implementation Details:</strong>
     * Delegates to repository layer with exact parameter mapping to ensure correct date range queries.
     * Critical: Parameter order must match repository method signature exactly.</p>
     * 
     * @param userId the ID of the user whose unconfirmed recurring events to retrieve
     * @param fromDate the start date of the range (inclusive) - passed as fromDate to repository
     * @param toDate the end date of the range (inclusive) - passed as toDate to repository
     */
    @Override
    public List<RecurringEvent> getUnconfirmedRecurringEventsForUserInRange(Long userId, LocalDate fromDate, LocalDate toDate) {
        logger.debug("Fetching unconfirmed recurring events by User ID {} in range {} to {}", userId, fromDate, toDate);
        return recurringEventRepository.findUnconfirmedRecurringEventsForUserInRange(userId, fromDate, toDate);
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Implementation Details:</strong>
     * <ul>
     *   <li>Draft events: Minimal validation, saved directly for flexibility</li>
     *   <li>Confirmed events: Full field validation + conflict detection before saving</li>
     * </ul></p>
     */
    @Override
    public RecurringEvent createRecurringEventWithValidation(RecurringEvent recurringEvent) {
        if (recurringEvent.isUnconfirmed()) {
            logger.info("Creating draft recurring event for user ID {}", recurringEvent.getCreator().getId());
            return recurringEventRepository.save(recurringEvent);
        }

        logger.info("Creating confirmed recurring event '{}' for user ID {}", 
                   recurringEvent.getName(), recurringEvent.getCreator().getId());

        try {
            validateRecurringEventFields(recurringEvent);
            conflictValidator.validateNoConflicts(recurringEvent);
            
            RecurringEvent saved = recurringEventRepository.save(recurringEvent);
            logger.info("Successfully created confirmed recurring event ID {} for user ID {}", 
                       saved.getId(), saved.getCreator().getId());
            return saved;
        } catch (ConflictException e) {
            logger.warn("Conflict detected when creating recurring event '{}' for user ID {}: {}",
                       recurringEvent.getName(), recurringEvent.getCreator().getId(), e.getMessage());
            throw e;
        } catch (InvalidEventStateException e) {
            logger.warn("Validation failed when creating recurring event '{}' for user ID {}: {}",
                       recurringEvent.getName(), recurringEvent.getCreator().getId(), e.getErrorCode());
            throw e;
        }
    }


    /**
     * {@inheritDoc}
     * 
     * <p><strong>Implementation Note:</strong>
     * This method only updates the recurring event template. Propagation of changes to
     * existing event instances is handled at the service layer to maintain separation of concerns.</p>
     */
    @Override
    public RecurringEvent updateRecurringEvent(RecurringEvent recurringEvent) {
        logger.info("Updating recurring event ID {} for user ID {}", 
                   recurringEvent.getId(), recurringEvent.getCreator().getId());

        if (recurringEvent.isUnconfirmed()) {
            logger.debug("Updating draft recurring event ID {} without validation", recurringEvent.getId());
            return recurringEventRepository.save(recurringEvent);
        }

        try {
            validateRecurringEventFields(recurringEvent);
            conflictValidator.validateNoConflicts(recurringEvent);
            
            RecurringEvent updated = recurringEventRepository.save(recurringEvent);
            logger.info("Successfully updated confirmed recurring event ID {} for user ID {}", 
                       updated.getId(), updated.getCreator().getId());
            return updated;
        } catch (ConflictException e) {
            logger.warn("Conflict detected when updating recurring event ID {} for user ID {}: {}",
                       recurringEvent.getId(), recurringEvent.getCreator().getId(), e.getMessage());
            throw e;
        } catch (InvalidEventStateException e) {
            logger.warn("Validation failed when updating recurring event ID {} for user ID {}: {}",
                       recurringEvent.getId(), recurringEvent.getCreator().getId(), e.getErrorCode());
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Implementation Details:</strong>
     * <ul>
     *   <li>Validates the event is currently in draft state</li>
     *   <li>Applies full field validation and conflict detection</li>
     *   <li>Sets unconfirmed flag to false and persists the change</li>
     * </ul></p>
     */
    @Override
    public RecurringEvent confirmRecurringEventWithValidation(RecurringEvent recurringEvent) {
        logger.info("Confirming recurring event ID {} for user ID {}", 
                   recurringEvent.getId(), recurringEvent.getCreator().getId());

        if (!recurringEvent.isUnconfirmed()) {
            logger.warn("Attempted to confirm already confirmed recurring event ID {} for user ID {}",
                       recurringEvent.getId(), recurringEvent.getCreator().getId());
            throw new RecurringEventAlreadyConfirmedException(recurringEvent.getId());
        }

        try {
            validateRecurringEventFields(recurringEvent);
            conflictValidator.validateNoConflicts(recurringEvent);

            recurringEvent.setUnconfirmed(false);
            RecurringEvent confirmed = recurringEventRepository.save(recurringEvent);
            logger.info("Successfully confirmed recurring event ID {} for user ID {}", 
                       confirmed.getId(), confirmed.getCreator().getId());
            return confirmed;
        } catch (ConflictException e) {
            logger.warn("Conflict detected when confirming recurring event ID {} for user ID {}: {}",
                       recurringEvent.getId(), recurringEvent.getCreator().getId(), e.getMessage());
            throw e;
        } catch (InvalidEventStateException e) {
            logger.warn("Validation failed when confirming recurring event ID {} for user ID {}: {}",
                       recurringEvent.getId(), recurringEvent.getCreator().getId(), e.getErrorCode());
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Implementation Note:</strong>
     * This method only deletes the recurring event template. Cleanup of associated
     * event instances is handled through database cascade rules or at the service layer.</p>
     */
    @Override
    public void deleteRecurringEvent(Long recurringEventId) {
        logger.info("Deleting recurring event ID {}", recurringEventId);
        recurringEventRepository.deleteById(recurringEventId);
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Implementation Details:</strong>
     * Uses a single repository call for efficient bulk deletion of all draft recurring events
     * for the specified user. Runs within a transaction to ensure atomicity.</p>
     */
    @Override
    @Transactional
    public void deleteAllUnconfirmedRecurringEventsByUser(Long userId) {
        logger.info("Deleting all unconfirmed recurring events for user ID {}", userId);
        recurringEventRepository.deleteByCreatorIdAndUnconfirmedTrue(userId);
        logger.info("Successfully deleted all unconfirmed recurring events for user ID {}", userId);
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Implementation Details:</strong>
     * <ol>
     *   <li>Validates that removing skip days won't create scheduling conflicts</li>
     *   <li>Removes each specified date from the recurring event's skip days set</li>
     *   <li>Persists the updated recurring event within a transaction</li>
     * </ol></p>
     */
    @Override
    @Transactional
    public void removeSkipDaysWithConflictValidation(RecurringEvent recurringEvent, Set<LocalDate> skipDaysToRemove) {
        logger.info("Removing {} skip days from recurring event ID {} for user ID {}", 
                   skipDaysToRemove.size(), recurringEvent.getId(), recurringEvent.getCreator().getId());
        logger.debug("Skip days to remove: {}", skipDaysToRemove);
        
        try {
            conflictValidator.validateNoConflictsForSkipDays(recurringEvent, skipDaysToRemove);
            skipDaysToRemove.forEach(recurringEvent::removeSkipDay);
            recurringEventRepository.save(recurringEvent);
            
            logger.info("Successfully removed {} skip days from recurring event ID {}", 
                       skipDaysToRemove.size(), recurringEvent.getId());
        } catch (ConflictException e) {
            logger.warn("Conflict detected when removing skip days from recurring event ID {} for user ID {}: {}",
                       recurringEvent.getId(), recurringEvent.getCreator().getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * Validates that start times occur before end times for both time and date components.
     * 
     * <p>Ensures temporal consistency by checking:
     * <ul>
     *   <li>Start time is before end time within the same day</li>
     *   <li>Start date is before end date (if end date is specified)</li>
     * </ul></p>
     * 
     * <p><strong>Valid Examples:</strong></p>
     * <pre>{@code
     * // Valid time ranges
     * validateStartBeforeEnd(
     *     LocalTime.of(9, 0),      // 9:00 AM
     *     LocalTime.of(10, 30),    // 10:30 AM
     *     LocalDate.of(2024, 1, 1), // Start date
     *     LocalDate.of(2024, 1, 31) // End date (optional)
     * );
     * 
     * // Single-day recurring event (end date can be null)
     * validateStartBeforeEnd(
     *     LocalTime.of(14, 0),     // 2:00 PM
     *     LocalTime.of(15, 0),     // 3:00 PM
     *     LocalDate.of(2024, 1, 1), // Start date
     *     null                     // No end date
     * );
     * }</pre>
     * 
     * <p><strong>Invalid Examples (will throw InvalidTimeException):</strong></p>
     * <pre>{@code
     * // Start time after end time
     * validateStartBeforeEnd(LocalTime.of(10, 0), LocalTime.of(9, 0), ...); // INVALID
     * 
     * // Start date after end date
     * validateStartBeforeEnd(..., LocalDate.of(2024, 2, 1), LocalDate.of(2024, 1, 1)); // INVALID
     * 
     * // Same times (not strictly before)
     * validateStartBeforeEnd(LocalTime.of(9, 0), LocalTime.of(9, 0), ...); // INVALID
     * }</pre>
     * 
     * @param startTime the start time to validate
     * @param endTime the end time to validate
     * @param startDate the start date to validate
     * @param endDate the end date to validate (can be null for single-day events)
     * @throws InvalidTimeException if start time/date is not before end time/date
     */
    private void validateStartBeforeEnd(
            LocalTime startTime,
            LocalTime endTime,
            LocalDate startDate,
            LocalDate endDate
    ) {
        // For time validation, we need to consider the date span:
        // 1. Same date: startTime must be before endTime (no midnight crossing on same day)
        // 2. Different dates: allow any time combination (including midnight crossing)
        // 3. Same start/end time is valid if spanning multiple days (24h+ events)
        
        boolean sameDate = (endDate == null || startDate.equals(endDate));
        
        if (sameDate && !startTime.isBefore(endTime)) {
            // Same date and start >= end time means zero or negative duration
            throw new InvalidTimeException(INVALID_EVENT_TIME, startTime, endTime);
        }

        // Date validation: end date must be same or after start date
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidTimeException(INVALID_EVENT_TIME, startDate, endDate);
        }
    }

    /**
     * Validates all required fields for a confirmed recurring event.
     * 
     * <p>Performs comprehensive validation including:
     * <ul>
     *   <li>Required field presence (name, times, dates, label, recurrence rule)</li>
     *   <li>Temporal consistency (start before end)</li>
     *   <li>Recurrence rule parsing and summary generation</li>
     * </ul></p>
     * 
     * <p>This validation is only applied to confirmed events. Draft events bypass
     * validation to allow flexible data entry.</p>
     * 
     * @param recurringEvent the event to validate
     * @throws InvalidEventStateException if any required field is missing or invalid
     * @throws InvalidTimeException if temporal constraints are violated
     */
    private void validateRecurringEventFields(RecurringEvent recurringEvent) {
        logger.debug("Validating recurring event fields for ID {}", recurringEvent.getId());
        
        validateRequiredStringField(recurringEvent.getName(), MISSING_EVENT_NAME);
        validateRequiredField(recurringEvent.getStartTime(), MISSING_EVENT_START_TIME);
        validateRequiredField(recurringEvent.getEndTime(), MISSING_EVENT_END_TIME);
        validateRequiredField(recurringEvent.getStartDate(), MISSING_EVENT_START_DATE);
        validateRequiredField(recurringEvent.getLabel(), MISSING_EVENT_LABEL);
        validateRequiredField(recurringEvent.getRecurrenceRule(), MISSING_RECURRENCE_RULE);

        validateStartBeforeEnd(
                recurringEvent.getStartTime(),
                recurringEvent.getEndTime(),
                recurringEvent.getStartDate(),
                recurringEvent.getEndDate()
        );

        // When confirming drafts only
        if (recurringEvent.getRecurrenceRule().getParsed() == null) {
            logger.debug("Parsing recurrence rule for recurring event ID {}", recurringEvent.getId());
            ParsedRecurrenceInput parsed = recurrenceRuleService.parseFromString(
                    recurringEvent.getRecurrenceRule().getSummary());
            String summary = recurrenceRuleService.buildSummary(
                    parsed, recurringEvent.getStartDate(), recurringEvent.getEndDate());
            recurringEvent.setRecurrenceRule(new RecurrenceRuleVO(summary, parsed));
        }
        
        logger.debug("Field validation completed successfully for recurring event ID {}", recurringEvent.getId());
    }

    /**
     * {@inheritDoc}
     * 
     * <p><strong>Implementation Details:</strong>
     * <ol>
     *   <li>Converts the time window to local dates in the user's timezone</li>
     *   <li>Retrieves all confirmed recurring events that could have occurrences in the range</li>
     *   <li>Expands each recurring event's pattern, respecting skip days</li>
     *   <li>Creates virtual event DTOs for future occurrences only (past events are excluded)</li>
     *   <li>Uses the current time in the user's timezone to filter out past events</li>
     * </ol></p>
     */
    @Override
    public List<EventResponseDTO> generateVirtuals(Long userId, ZonedDateTime startTime, ZonedDateTime endTime, ZoneId userZoneId) {
        logger.debug("Generating virtual events for user ID {} in time range {} to {}", userId, startTime, endTime);
        
        List<EventResponseDTO> virtuals = new ArrayList<>();

        // Convert start and end times to LocalDates in the user's timezone for recurrence expansion
        LocalDate fromDate = startTime.withZoneSameInstant(userZoneId).toLocalDate();
        LocalDate toDate = endTime.withZoneSameInstant(userZoneId).toLocalDate();

        List<RecurringEvent> recurrences = getConfirmedRecurringEventsForUserInRange(
                userId, fromDate, toDate
        );
        
        logger.debug("Found {} confirmed recurring events for user ID {} in date range {} to {}", 
                    recurrences.size(), userId, fromDate, toDate);

        ZonedDateTime now = ZonedDateTime.now(clockProvider.getClockForZone(userZoneId));

        for (RecurringEvent recurrence : recurrences) {
            List<LocalDate> occurrenceDates = recurrenceRuleService.expandRecurrence(
                    recurrence.getRecurrenceRule().getParsed(),
                    fromDate,
                    toDate,
                    recurrence.getSkipDays()
            );

            int futureOccurrences = 0;
            for (LocalDate date : occurrenceDates) {
                ZonedDateTime eventEndTime = ZonedDateTime.of(date, recurrence.getEndTime(), userZoneId);

                // Only add virtual events that are in the future and non-null
                if (eventEndTime.isAfter(now)) {
                    EventResponseDTO virtualEventDTO = eventResponseDTOFactory.createFromRecurringEvent(recurrence, date);
                    virtuals.add(virtualEventDTO);
                    futureOccurrences++;
                }
            }
            
            logger.debug("Generated {} future virtual events from recurring event ID {} (total occurrences: {})", 
                        futureOccurrences, recurrence.getId(), occurrenceDates.size());
        }

        logger.debug("Generated {} total virtual events for user ID {}", virtuals.size(), userId);
        return virtuals;
    }

    /**
     * Validates that a required field is not null.
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <pre>{@code
     * // Validate required time fields
     * validateRequiredField(recurringEvent.getStartTime(), MISSING_EVENT_START_TIME);
     * validateRequiredField(recurringEvent.getEndTime(), MISSING_EVENT_END_TIME);
     * 
     * // Validate required date fields
     * validateRequiredField(recurringEvent.getStartDate(), MISSING_EVENT_START_DATE);
     * 
     * // Validate required object references
     * validateRequiredField(recurringEvent.getLabel(), MISSING_EVENT_LABEL);
     * validateRequiredField(recurringEvent.getRecurrenceRule(), MISSING_RECURRENCE_RULE);
     * }</pre>
     * 
     * @param field the field to validate
     * @param errorCode the error code to throw if validation fails
     * @throws InvalidEventStateException if the field is null
     */
    private void validateRequiredField(Object field, ErrorCode errorCode) {
        if (field == null) {
            logger.warn("Required field validation failed: {}", errorCode);
            throw new InvalidEventStateException(errorCode);
        }
    }

    /**
     * Validates that a required string field is not null or blank.
     * 
     * <p>This method performs comprehensive string validation by checking for:</p>
     * <ul>
     *   <li>Null values</li>
     *   <li>Empty strings ("")</li>
     *   <li>Whitespace-only strings ("   ")</li>
     * </ul>
     * 
     * <p><strong>Usage Examples:</strong></p>
     * <pre>{@code
     * // Validate event name (most common use case)
     * validateRequiredStringField(recurringEvent.getName(), MISSING_EVENT_NAME);
     * 
     * // Validate other string fields if they become required
     * validateRequiredStringField(recurringEvent.getDescription(), MISSING_EVENT_DESCRIPTION);
     * }</pre>
     * 
     * <p><strong>Valid and Invalid Examples:</strong></p>
     * <pre>{@code
     * // Valid strings (pass validation)
     * "Daily Meeting"           // Normal event name
     * "Weekly Standup 📅"       // With emoji
     * "a"                       // Single character
     * 
     * // Invalid strings (fail validation)
     * null                      // Null value
     * ""                        // Empty string
     * "   "                     // Whitespace only
     * "\t\n"                    // Tab and newline only
     * }</pre>
     * 
     * @param field the string field to validate
     * @param errorCode the error code to throw if validation fails
     * @throws InvalidEventStateException if the field is null or blank
     */
    private void validateRequiredStringField(String field, ErrorCode errorCode) {
        if (field == null || field.isBlank()) {
            logger.warn("Required string field validation failed: {}", errorCode);
            throw new InvalidEventStateException(errorCode);
        }
    }
}
