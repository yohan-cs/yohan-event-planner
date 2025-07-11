package com.yohan.event_planner.service;

import com.blazebit.persistence.PagedList;
import com.yohan.event_planner.business.EventBO;
import com.yohan.event_planner.business.RecurringEventBO;
import com.yohan.event_planner.business.handler.RecurringEventPatchHandler;
import com.yohan.event_planner.dao.RecurringEventDAO;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.RecurrenceRuleVO;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.TimeFilter;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventResponseDTOFactory;
import com.yohan.event_planner.dto.LabelResponseDTO;
import com.yohan.event_planner.dto.RecurringEventCreateDTO;
import com.yohan.event_planner.dto.RecurringEventFilterDTO;
import com.yohan.event_planner.dto.RecurringEventResponseDTO;
import com.yohan.event_planner.dto.RecurringEventUpdateDTO;
import com.yohan.event_planner.exception.ErrorCode;
import com.yohan.event_planner.exception.InvalidSkipDayException;
import com.yohan.event_planner.exception.InvalidTimeException;
import com.yohan.event_planner.exception.RecurringEventAlreadyConfirmedException;
import com.yohan.event_planner.exception.RecurringEventNotFoundException;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.yohan.event_planner.exception.ErrorCode.INVALID_SKIP_DAY_REMOVAL;

@Service
public class
RecurringEventServiceImpl implements RecurringEventService {

    private static final Logger logger = LoggerFactory.getLogger(RecurringEventServiceImpl.class);

    private final RecurringEventBO recurringEventBO;
    private final EventBO eventBO;
    private final LabelService labelService;
    private final RecurringEventDAO recurringEventDAO;
    private final RecurringEventPatchHandler recurringEventPatchHandler;
    private final RecurrenceRuleService recurrenceRuleService;
    private final OwnershipValidator ownershipValidator;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final EventResponseDTOFactory eventResponseDTOFactory;
    private final ClockProvider clockProvider;

    public RecurringEventServiceImpl(
            RecurringEventBO recurringEventBO,
            EventBO eventBO,
            LabelService labelService,
            RecurringEventDAO recurringEventDAO,
            RecurringEventPatchHandler recurringEventPatchHandler,
            RecurrenceRuleService recurrenceRuleService,
            OwnershipValidator ownershipValidator,
            AuthenticatedUserProvider authenticatedUserProvider,
            EventResponseDTOFactory eventResponseDTOFactory,
            ClockProvider clockProvider
    ) {
        this.recurringEventBO = recurringEventBO;
        this.eventBO = eventBO;
        this.labelService = labelService;
        this.recurringEventDAO = recurringEventDAO;
        this.recurringEventPatchHandler = recurringEventPatchHandler;
        this.recurrenceRuleService = recurrenceRuleService;
        this.ownershipValidator = ownershipValidator;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.eventResponseDTOFactory = eventResponseDTOFactory;
        this.clockProvider = clockProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringEventResponseDTO getRecurringEventById(Long recurringEventId) {
        User viewer = authenticatedUserProvider.getCurrentUser();
        RecurringEvent recurringEvent = recurringEventBO.getRecurringEventById(recurringEventId)
                .orElseThrow(() -> new RecurringEventNotFoundException(recurringEventId));

        // Hide unconfirmed events unless viewer is the creator
        if (recurringEvent.isUnconfirmed() && !recurringEvent.getCreator().getId().equals(viewer.getId())) {
            throw new RecurringEventNotFoundException(recurringEventId);
        }

        return toRecurringEventResponseDTO(recurringEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecurringEventResponseDTO> getConfirmedRecurringEventsForCurrentUser(RecurringEventFilterDTO filter, int pageNumber, int pageSize) {
        User viewer = authenticatedUserProvider.getCurrentUser();
        LocalDate today = LocalDate.now(clockProvider.getClockForUser(viewer));
        
        LocalDate startDate = filter.startDate();
        LocalDate endDate = filter.endDate();

        switch (filter.timeFilter()) {
            case ALL -> {
                startDate = TimeUtils.FAR_PAST_DATE;
                endDate = TimeUtils.FAR_FUTURE_DATE;
            }
            case PAST_ONLY -> {
                startDate = TimeUtils.FAR_PAST_DATE;
                endDate = today;
            }
            case FUTURE_ONLY -> {
                startDate = today;
                endDate = TimeUtils.FAR_FUTURE_DATE;
            }
            case CUSTOM -> {
                if (startDate == null) startDate = TimeUtils.FAR_PAST_DATE;
                if (endDate == null) endDate = TimeUtils.FAR_FUTURE_DATE;
            }
        }

        // Validate that start date is not after end date for CUSTOM filter
        if (filter.timeFilter() == TimeFilter.CUSTOM && startDate.isAfter(endDate)) {
            throw new InvalidTimeException(ErrorCode.INVALID_TIME_RANGE, startDate, endDate);
        }

        // Pass resolved time range to DAO
        RecurringEventFilterDTO sanitizedFilter = new RecurringEventFilterDTO(
                filter.labelId(),
                filter.timeFilter(), // Keep original for reference, but DAO will use resolved dates
                startDate,
                endDate,
                filter.sortDescending()
        );

        PagedList<RecurringEvent> activeEvents = recurringEventDAO.findConfirmedRecurringEvents(
                viewer.getId(),
                sanitizedFilter,
                pageNumber,
                pageSize
        );

        List<RecurringEventResponseDTO> dtos = activeEvents.stream()
                .map(this::toRecurringEventResponseDTO)
                .toList();

        return new PageImpl<>(dtos, PageRequest.of(pageNumber, pageSize), activeEvents.getTotalSize());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringEventResponseDTO> getConfirmedRecurringEventsPage(
            LocalDate endDateCursor,
            LocalDate startDateCursor,
            LocalTime startTimeCursor,
            LocalTime endTimeCursor,
            Long idCursor,
            int limit
    ) {
        User viewer = authenticatedUserProvider.getCurrentUser();

        List<RecurringEvent> recurringEvents = recurringEventBO.getConfirmedRecurringEventsPage(
                viewer.getId(),
                endDateCursor,
                startDateCursor,
                startTimeCursor,
                endTimeCursor,
                idCursor,
                limit
        );

        return recurringEvents.stream()
                .map(this::toRecurringEventResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringEventResponseDTO> getUnconfirmedRecurringEventsForCurrentUser() {
        User viewer = authenticatedUserProvider.getCurrentUser();
        LocalDate today = LocalDate.now(ZoneId.of(viewer.getTimezone()));

        List<RecurringEvent> drafts = recurringEventBO.getUnconfirmedRecurringEventsForUserInRange(
                viewer.getId(), today, TimeUtils.FAR_FUTURE_DATE
        );

        return drafts.stream()
                .map(this::toRecurringEventResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RecurringEventResponseDTO createRecurringEvent(RecurringEventCreateDTO dto) {
        User creator = authenticatedUserProvider.getCurrentUser();

        RecurrenceRuleVO recurrenceRuleVO = null;

        if (dto.recurrenceRule() != null) {
            if (!dto.isDraft()) {
                ParsedRecurrenceInput parsed = recurrenceRuleService.parseFromString(dto.recurrenceRule());
                String summary = recurrenceRuleService.buildSummary(parsed, dto.startDate(), dto.endDate());
                recurrenceRuleVO = new RecurrenceRuleVO(summary, parsed);
            } else {
                recurrenceRuleVO = new RecurrenceRuleVO(dto.recurrenceRule(), null);
            }
        }

        LocalDate endDate = dto.endDate();
        if (endDate == null) {
            endDate = TimeUtils.FAR_FUTURE_DATE; // Set far future date if null
        }


        RecurringEvent recurringEvent = dto.isDraft()
                ? RecurringEvent.createUnconfirmedDraftRecurringEvent(
                dto.name(),
                dto.startDate(),
                endDate,
                creator
        )
                : RecurringEvent.createRecurringEvent(
                dto.name(),
                dto.startTime(),
                dto.endTime(),
                dto.startDate(),
                endDate,
                dto.description(),
                recurrenceRuleVO,
                creator,
                false // confirmed
        );

        recurringEvent.setDescription(dto.description());

        Label label = (dto.labelId() != null)
                ? labelService.getLabelEntityById(dto.labelId())
                : creator.getUnlabeled();
        recurringEvent.setLabel(label);

        if (dto.skipDays() != null && !dto.skipDays().isEmpty()) {
            recurringEvent.setSkipDays(dto.skipDays());
        }

        RecurringEvent saved = recurringEventBO.createRecurringEventWithValidation(recurringEvent);

        return toRecurringEventResponseDTO(saved);
    }

    @Override
    @Transactional
    public RecurringEventResponseDTO confirmRecurringEvent(Long recurringEventId) {
        User viewer = authenticatedUserProvider.getCurrentUser();

        RecurringEvent recurringEvent = recurringEventBO.getRecurringEventById(recurringEventId)
                .orElseThrow(() -> new RecurringEventNotFoundException(recurringEventId));

        ownershipValidator.validateRecurringEventOwnership(viewer.getId(), recurringEvent);

        if (!recurringEvent.isUnconfirmed()) {
            throw new RecurringEventAlreadyConfirmedException(recurringEventId);
        }

        RecurringEvent confirmed = recurringEventBO.confirmRecurringEventWithValidation(recurringEvent);

        return toRecurringEventResponseDTO(confirmed);
    }

    @Override
    @Transactional
    public RecurringEventResponseDTO updateRecurringEvent(Long recurringEventId, RecurringEventUpdateDTO dto) {
        User viewer = authenticatedUserProvider.getCurrentUser();
        RecurringEvent recurringEvent = recurringEventBO.getRecurringEventById(recurringEventId)
                .orElseThrow(() -> new RecurringEventNotFoundException(recurringEventId));

        ownershipValidator.validateRecurringEventOwnership(viewer.getId(), recurringEvent);

        // Capture the original state to detect what changed
        String originalName = recurringEvent.getName();
        LocalTime originalStartTime = recurringEvent.getStartTime();
        LocalTime originalEndTime = recurringEvent.getEndTime();
        Long originalLabelId = recurringEvent.getLabel() != null ? recurringEvent.getLabel().getId() : null;

        boolean changed = recurringEventPatchHandler.applyPatch(recurringEvent, dto);
        
        if (changed) {
            RecurringEvent updated = recurringEventBO.updateRecurringEvent(recurringEvent);
            
            // If this is a confirmed recurring event, propagate changes to future Event instances
            if (!updated.isUnconfirmed()) {
                Set<String> changedFields = detectChangedFields(
                        originalName, originalStartTime, originalEndTime, originalLabelId, updated);
                
                if (!changedFields.isEmpty()) {
                    ZoneId userZoneId = ZoneId.of(viewer.getTimezone());
                    int updatedEventsCount = eventBO.updateFutureEventsFromRecurringEvent(
                            updated, changedFields, userZoneId);
                    
                    if (updatedEventsCount > 0) {
                        logger.info("Propagated changes from recurring event {} to {} future events", 
                                updated.getId(), updatedEventsCount);
                    }
                }
            }
            
            return toRecurringEventResponseDTO(updated);
        }
        
        return toRecurringEventResponseDTO(recurringEvent);
    }

    private Set<String> detectChangedFields(String originalName, LocalTime originalStartTime, 
                                          LocalTime originalEndTime, Long originalLabelId, 
                                          RecurringEvent updated) {
        Set<String> changedFields = new HashSet<>();
        
        if (!Objects.equals(originalName, updated.getName())) {
            changedFields.add("name");
        }
        
        if (!Objects.equals(originalStartTime, updated.getStartTime())) {
            changedFields.add("startTime");
        }
        
        if (!Objects.equals(originalEndTime, updated.getEndTime())) {
            changedFields.add("endTime");
        }
        
        Long currentLabelId = updated.getLabel() != null ? updated.getLabel().getId() : null;
        if (!Objects.equals(originalLabelId, currentLabelId)) {
            changedFields.add("label");
        }
        
        return changedFields;
    }

    @Override
    @Transactional
    public void deleteRecurringEvent(Long recurringEventId) {
        RecurringEvent existing = recurringEventBO.getRecurringEventById(recurringEventId)
                .orElseThrow(() -> new RecurringEventNotFoundException(recurringEventId));

        User viewer = authenticatedUserProvider.getCurrentUser();
        ownershipValidator.validateRecurringEventOwnership(viewer.getId(), existing);

        recurringEventBO.deleteRecurringEvent(recurringEventId);
    }

    @Override
    @Transactional
    public void deleteUnconfirmedRecurringEventsForCurrentUser() {
        User viewer = authenticatedUserProvider.getCurrentUser();
        recurringEventBO.deleteAllUnconfirmedRecurringEventsByUser(viewer.getId());
    }

    @Override
    @Transactional
    public RecurringEventResponseDTO addSkipDays(Long recurringEventId, Set<LocalDate> skipDaysToAdd) {
        User viewer = authenticatedUserProvider.getCurrentUser();
        RecurringEvent recurringEvent = recurringEventBO.getRecurringEventById(recurringEventId)
                .orElseThrow(() -> new RecurringEventNotFoundException(recurringEventId));

        ownershipValidator.validateRecurringEventOwnership(viewer.getId(), recurringEvent);

        ZoneId creatorZone = ZoneId.of(recurringEvent.getCreator().getTimezone());
        LocalDate today = LocalDate.now(clockProvider.getClockForZone(creatorZone));

        // Identify any invalid dates (null or past dates)
        Set<LocalDate> invalidDates = skipDaysToAdd.stream()
                .filter(date -> date == null || date.isBefore(today))
                .collect(Collectors.toSet());

        if (!invalidDates.isEmpty()) {
            throw new InvalidSkipDayException(ErrorCode.INVALID_SKIP_DAY_ADDITION, invalidDates);
        }

        skipDaysToAdd.forEach(recurringEvent::addSkipDay);

        recurringEventBO.updateRecurringEvent(recurringEvent);

        return this.toRecurringEventResponseDTO(recurringEvent);
    }

    @Override
    @Transactional
    public RecurringEventResponseDTO removeSkipDays(Long recurringEventId, Set<LocalDate> skipDaysToRemove) {
        User viewer = authenticatedUserProvider.getCurrentUser();
        RecurringEvent recurringEvent = recurringEventBO.getRecurringEventById(recurringEventId)
                .orElseThrow(() -> new RecurringEventNotFoundException(recurringEventId));

        ownershipValidator.validateRecurringEventOwnership(viewer.getId(), recurringEvent);

        ZoneId creatorZone = ZoneId.of(recurringEvent.getCreator().getTimezone());
        LocalDate today = LocalDate.now(clockProvider.getClockForZone(creatorZone));

        // Identify invalid dates: null values and past dates
        Set<LocalDate> invalidDates = skipDaysToRemove.stream()
                .filter(date -> date == null || date.isBefore(today))  // Combined check for null and past dates
                .collect(Collectors.toSet());

        if (!invalidDates.isEmpty()) {
            throw new InvalidSkipDayException(INVALID_SKIP_DAY_REMOVAL, invalidDates);
        }

        recurringEventBO.removeSkipDaysWithConflictValidation(recurringEvent, skipDaysToRemove);

        return this.toRecurringEventResponseDTO(recurringEvent);
    }




    /**
     * Private helper to map RecurringEvent entity to RecurringEventResponseDTO.
     */
    private RecurringEventResponseDTO toRecurringEventResponseDTO(RecurringEvent recurringEvent) {
        LocalDate startDate = recurringEvent.getStartDate();
        LocalDate endDate = recurringEvent.getEndDate();

        // Map the Label into LabelResponseDTO
        Label label = recurringEvent.getLabel();
        LabelResponseDTO labelResponseDTO = new LabelResponseDTO(
                label.getId(),
                label.getName(),
                label.getCreator().getUsername()
        );

        // Filter skip days to only include today or future dates
        ZoneId creatorZone = ZoneId.of(recurringEvent.getCreator().getTimezone());
        LocalDate today = LocalDate.now(clockProvider.getClockForZone(creatorZone));
        Set<LocalDate> visibleSkipDays = recurringEvent.getSkipDays().stream()
                .filter(date -> !date.isBefore(today))
                .collect(Collectors.toSet());

        return new RecurringEventResponseDTO(
                recurringEvent.getId(),
                recurringEvent.getName(),
                recurringEvent.getStartTime(),
                recurringEvent.getEndTime(),
                startDate,
                endDate,
                recurringEvent.getDescription(),
                labelResponseDTO,
                recurringEvent.getRecurrenceRule().getSummary(),
                visibleSkipDays,
                recurringEvent.getCreator().getUsername(),
                recurringEvent.isUnconfirmed()
        );
    }
}