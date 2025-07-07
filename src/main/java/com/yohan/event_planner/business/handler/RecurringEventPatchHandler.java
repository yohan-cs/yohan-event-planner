package com.yohan.event_planner.business.handler;

import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.RecurrenceRuleVO;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.dto.RecurringEventUpdateDTO;
import com.yohan.event_planner.exception.InvalidEventStateException;
import com.yohan.event_planner.security.OwnershipValidator;
import com.yohan.event_planner.service.LabelService;
import com.yohan.event_planner.service.ParsedRecurrenceInput;
import com.yohan.event_planner.service.RecurrenceRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_END_DATE;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_END_TIME;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_NAME;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_START_DATE;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_START_TIME;

@Component
public class RecurringEventPatchHandler {

    private static final Logger logger = LoggerFactory.getLogger(RecurringEventPatchHandler.class);

    private final LabelService labelService;
    private final RecurrenceRuleService recurrenceRuleService;
    private final OwnershipValidator ownershipValidator;

    public RecurringEventPatchHandler(
            LabelService labelService,
            RecurrenceRuleService recurrenceRuleService,
            OwnershipValidator ownershipValidator
    ) {
        this.labelService = labelService;
        this.recurrenceRuleService = recurrenceRuleService;
        this.ownershipValidator = ownershipValidator;
    }

    /**
     * Applies a partial update to an existing {@link RecurringEvent} based on the given {@link RecurringEventUpdateDTO}.
     *
     * <p>If the patch results in an invalid confirmed recurring event (e.g. missing name or times),
     * it should throw {@link InvalidEventStateException} or you may decide to mark unconfirmed externally.</p>
     *
     * @param existing the existing recurring event to patch
     * @param dto      the update payload with optional fields
     * @return true if any field was changed, false otherwise
     * @throws InvalidEventStateException if a confirmed recurring event is left invalid
     */
    public boolean applyPatch(RecurringEvent existing, RecurringEventUpdateDTO dto) {
        boolean updated = false;

        // --- Name ---
        if (dto.name() != null && !Objects.equals(dto.name().orElse(null), existing.getName())) {
            existing.setName(dto.name().orElse(null));
            updated = true;
        }

        // --- Start Time ---
        if (dto.startTime() != null) {
            LocalTime newStart = dto.startTime().orElse(null);
            if (!Objects.equals(newStart, existing.getStartTime())) {
                logger.info("Updating start time for recurring event {}: [{} -> {}]", existing.getId(), existing.getStartTime(), newStart);
                existing.setStartTime(newStart);
                updated = true;
            }
        }

        // --- End Time ---
        if (dto.endTime() != null) {
            LocalTime newEnd = dto.endTime().orElse(null);
            if (!Objects.equals(newEnd, existing.getEndTime())) {
                logger.info("Updating end time for recurring event {}: [{} -> {}]", existing.getId(), existing.getEndTime(), newEnd);
                existing.setEndTime(newEnd);
                updated = true;
            }
        }

        // --- Start Date ---
        if (dto.startDate() != null) {
            LocalDate newStartDate = dto.startDate().orElse(null);
            if (!Objects.equals(newStartDate, existing.getStartDate())) {
                existing.setStartDate(newStartDate);
                updated = true;
            }
        }

        // --- End Date ---
        if (dto.endDate() != null) {
            LocalDate newEndDate = dto.endDate().orElse(null);
            if (!Objects.equals(newEndDate, existing.getEndDate())) {
                existing.setEndDate(newEndDate);
                updated = true;
            }
        }

        // --- Description ---
        if (dto.description() != null && !Objects.equals(dto.description().orElse(null), existing.getDescription())) {
            existing.setDescription(dto.description().orElse(null));
            updated = true;
        }

        // --- Label ---
        if (dto.labelId() != null) {
            Long newLabelId = dto.labelId().orElse(null);
            Long resolvedLabelId = (newLabelId != null)
                    ? newLabelId
                    : existing.getCreator().getUnlabeled().getId();

            Long currentLabelId = existing.getLabel() != null ? existing.getLabel().getId() : null;

            if (!Objects.equals(resolvedLabelId, currentLabelId)) {
                Label newLabel = labelService.getLabelEntityById(resolvedLabelId);
                ownershipValidator.validateLabelOwnership(existing.getCreator().getId(), newLabel);
                existing.setLabel(newLabel);
                updated = true;
            }
        }

        // --- Recurrence Rule ---
        if (dto.recurrenceRule() != null) {
            String newRule = dto.recurrenceRule().orElse(null);
            RecurrenceRuleVO currentVO = existing.getRecurrenceRule();

            if (!existing.isUnconfirmed()) {
                // Confirmed: Parse and summarize
                ParsedRecurrenceInput newParsed = recurrenceRuleService.parseFromString(newRule);
                String newSummary = recurrenceRuleService.buildSummary(newParsed, existing.getStartDate(), existing.getEndDate());

                if (currentVO == null ||
                        !Objects.equals(newSummary, currentVO.getSummary()) ||
                        !Objects.equals(newParsed, currentVO.getParsed())) {

                    existing.setRecurrenceRule(new RecurrenceRuleVO(newSummary, newParsed));
                    updated = true;
                }
            } else {
                // Unconfirmed: allow clearing by passing `Optional.of(null)`
                if (newRule == null) {
                    if (currentVO != null) {
                        existing.setRecurrenceRule(null);
                        updated = true;
                    }
                } else {
                    // Only update if raw string is different
                    if (currentVO == null || !Objects.equals(newRule, currentVO.getSummary())) {
                        existing.setRecurrenceRule(new RecurrenceRuleVO(newRule, null));
                        updated = true;
                    }
                }
            }
        }

        if (!existing.isUnconfirmed()) {
            if (existing.getName() == null) {
                throw new InvalidEventStateException(MISSING_EVENT_NAME);
            }
            if (existing.getStartTime() == null) {
                throw new InvalidEventStateException(MISSING_EVENT_START_TIME);
            }
            if (existing.getEndTime() == null) {
                throw new InvalidEventStateException(MISSING_EVENT_END_TIME);
            }
            if (existing.getStartDate() == null) {
                throw new InvalidEventStateException(MISSING_EVENT_START_DATE);
            }
            if (existing.getEndDate() == null) {
                throw new InvalidEventStateException(MISSING_EVENT_END_DATE);
            }
        }

        return updated;
    }
}