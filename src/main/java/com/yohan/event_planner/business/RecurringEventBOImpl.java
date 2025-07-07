package com.yohan.event_planner.business;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.RecurrenceRuleVO;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.exception.InvalidEventStateException;
import com.yohan.event_planner.exception.InvalidTimeException;
import com.yohan.event_planner.repository.RecurringEventRepository;
import com.yohan.event_planner.service.ParsedRecurrenceInput;
import com.yohan.event_planner.service.RecurrenceRuleService;
import com.yohan.event_planner.validation.ConflictValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
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
import static com.yohan.event_planner.exception.ErrorCode.RECURRING_EVENT_ALREADY_CONFIRMED;

@Service
public class RecurringEventBOImpl implements RecurringEventBO {

    private static final Logger logger = LoggerFactory.getLogger(RecurringEventBOImpl.class);

    private final RecurringEventRepository recurringEventRepository;
    private final RecurrenceRuleService recurrenceRuleService;
    private final ConflictValidator conflictValidator;

    public RecurringEventBOImpl(
            RecurringEventRepository recurringEventRepository,
            RecurrenceRuleService recurrenceRuleService,
            ConflictValidator conflictValidator
    ) {
        this.recurringEventRepository = recurringEventRepository;
        this.recurrenceRuleService = recurrenceRuleService;
        this.conflictValidator = conflictValidator;
    }

    @Override
    public Optional<RecurringEvent> getRecurringEventById(Long recurringEventId) {
        logger.debug("Fetching recurring event ID {}", recurringEventId);
        return recurringEventRepository.findById(recurringEventId);
    }

    @Override
    public List<RecurringEvent> getConfirmedRecurringEventsForUserInRange(Long userId, LocalDate fromDate, LocalDate toDate) {
        return recurringEventRepository.findConfirmedRecurringEventsForUserBetween(
                userId, toDate, fromDate
        );
    }

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
        List<RecurringEvent> recurringEvents;

        if (endDateCursor == null || idCursor == null) {
            recurringEvents = recurringEventRepository.findTopConfirmedByUserIdOrderByEndDateDescIdDesc(
                    userId,
                    PageRequest.of(0, limit)
            );
        } else {
            recurringEvents = recurringEventRepository.findConfirmedByUserIdBeforeCursor(
                    userId,
                    endDateCursor,
                    startDateCursor,
                    startTimeCursor,
                    endTimeCursor,
                    idCursor,
                    PageRequest.of(0, limit)
            );
        }

        return recurringEvents;
    }

    @Override
    public List<RecurringEvent> getUnconfirmedRecurringEventsForUserInRange(Long userId, LocalDate fromDate, LocalDate toDate) {
        logger.debug("Fetching unconfirmed recurring events by User ID {} in range {} to {}", userId, fromDate, toDate);
        return recurringEventRepository.findUnconfirmedRecurringEventsForUserInRange(userId, fromDate, toDate);
    }

    @Override
    public RecurringEvent createRecurringEventWithValidation(RecurringEvent recurringEvent) {
        if (recurringEvent.isUnconfirmed()) {
            logger.info("Creating draft recurring event for user ID {}", recurringEvent.getCreator().getId());
            return recurringEventRepository.save(recurringEvent);
        }

        logger.info("Creating scheduled recurring event '{}'", recurringEvent.getName());

        validateRecurringEventFields(recurringEvent);

        conflictValidator.validateNoConflicts(recurringEvent);

        return recurringEventRepository.save(recurringEvent);
    }


    @Override
    public RecurringEvent updateRecurringEvent(RecurringEvent recurringEvent) {
        logger.info("Updating recurring event ID {}", recurringEvent.getId());

        if (recurringEvent.isUnconfirmed()) {
            return recurringEventRepository.save(recurringEvent);
        }

        validateRecurringEventFields(recurringEvent);

        conflictValidator.validateNoConflicts(recurringEvent);

        return recurringEventRepository.save(recurringEvent);
    }

    @Override
    public RecurringEvent confirmRecurringEventWithValidation(RecurringEvent recurringEvent) {
        logger.info("Confirming recurring event ID {}", recurringEvent.getId());

        validateRecurringEventFields(recurringEvent);

        conflictValidator.validateNoConflicts(recurringEvent);

        recurringEvent.setUnconfirmed(false);
        return recurringEventRepository.save(recurringEvent);
    }

    @Override
    public void deleteRecurringEvent(Long recurringEventId) {
        logger.info("Deleting recurring event ID {}", recurringEventId);
        recurringEventRepository.deleteById(recurringEventId);
    }

    @Override
    @Transactional
    public void deleteAllUnconfirmedRecurringEventsByUser(Long userId) {
        recurringEventRepository.deleteByCreatorIdAndUnconfirmedTrue(userId);
    }

    @Override
    @Transactional
    public void removeSkipDaysWithConflictValidation(RecurringEvent recurringEvent, Set<LocalDate> skipDaysToRemove) {
           conflictValidator.validateNoConflictsForSkipDays(recurringEvent, skipDaysToRemove);
           skipDaysToRemove.forEach(recurringEvent::removeSkipDay);
           recurringEventRepository.save(recurringEvent);
    }

    private void validateStartBeforeEnd(
            LocalTime startTime,
            LocalTime endTime,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (!startTime.isBefore(endTime)) {
            throw new InvalidTimeException(INVALID_EVENT_TIME, startTime, endTime);
        }

        if (endDate != null && !startDate.isBefore(endDate)) {
            throw new InvalidTimeException(INVALID_EVENT_TIME, startDate, endDate);
        }
    }

    private void validateRecurringEventFields(RecurringEvent recurringEvent) {
        if (recurringEvent.getName() == null || recurringEvent.getName().isBlank()) {
            throw new InvalidEventStateException(MISSING_EVENT_NAME);
        }
        if (recurringEvent.getStartTime() == null) {
            throw new InvalidEventStateException(MISSING_EVENT_START_TIME);
        }
        if (recurringEvent.getEndTime() == null) {
            throw new InvalidEventStateException(MISSING_EVENT_END_TIME);
        }
        if (recurringEvent.getStartDate() == null) {
            throw new InvalidEventStateException(MISSING_EVENT_START_DATE);
        }
        if (recurringEvent.getLabel() == null) {
            throw new InvalidEventStateException(MISSING_EVENT_LABEL);
        }
        if (recurringEvent.getRecurrenceRule() == null) {
            throw new InvalidEventStateException(MISSING_RECURRENCE_RULE);
        }

        validateStartBeforeEnd(
                recurringEvent.getStartTime(),
                recurringEvent.getEndTime(),
                recurringEvent.getStartDate(),
                recurringEvent.getEndDate()
        );

        // When confirming drafts only
        if (recurringEvent.getRecurrenceRule().getParsed() == null) {
            ParsedRecurrenceInput parsed = recurrenceRuleService.parseFromString(
                    recurringEvent.getRecurrenceRule().getSummary());
            String summary = recurrenceRuleService.buildSummary(
                    parsed, recurringEvent.getStartDate(), recurringEvent.getEndDate());
            recurringEvent.setRecurrenceRule(new RecurrenceRuleVO(summary, parsed));
        }
    }
}
