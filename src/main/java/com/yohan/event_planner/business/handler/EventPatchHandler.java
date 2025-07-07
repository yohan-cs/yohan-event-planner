package com.yohan.event_planner.business.handler;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.exception.InvalidEventStateException;
import com.yohan.event_planner.security.OwnershipValidator;
import com.yohan.event_planner.service.LabelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Objects;

import static com.yohan.event_planner.exception.ErrorCode.EVENT_NOT_CONFIRMED;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_END_TIME;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_NAME;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_START_TIME;

/**
 * Handles partial updates to {@link Event} entities using {@link EventUpdateDTO} payloads.
 * <p>
 * Applies field-by-field patching logic with support for clearing optional values.
 * If an update causes a previously confirmed event to become invalid (e.g. missing name, start, or end time),
 * the event will be automatically marked as unconfirmed.
 * <p>
 * Draft (unconfirmed) events are allowed to have incomplete fields and are not subject to validation.
 */
@Component
public class EventPatchHandler {

    private static final Logger logger = LoggerFactory.getLogger(EventPatchHandler.class);

    private final LabelService labelService;
    private final OwnershipValidator ownershipValidator;

    public EventPatchHandler(LabelService labelService, OwnershipValidator ownershipValidator) {
        this.labelService = labelService;
        this.ownershipValidator = ownershipValidator;
    }

    /**
     * Applies a partial update to an existing {@link Event} based on the given {@link EventUpdateDTO}.
     *
     * <p>If the patch modifies a required field to an invalid state (e.g. null name or time on a confirmed event),
     * the event will be demoted to draft (unconfirmed). Attempts to mark such an event as completed
     * will result in a {@link InvalidEventStateException}.
     *
     * @param existingEvent the event to update
     * @param dto           the update payload
     * @return {@code true} if any field was changed; {@code false} otherwise
     * @throws InvalidEventStateException if the patch attempts to complete an invalid or unconfirmed event
     */
    public boolean applyPatch(Event existingEvent, EventUpdateDTO dto) {
        boolean updated = false;

        // --- Name ---
        if (dto.name() != null && !Objects.equals(dto.name().orElse(null), existingEvent.getName())) {
            existingEvent.setName(dto.name().orElse(null));
            updated = true;
        }

        // --- Start Time ---
        if (dto.startTime() != null) {
            ZonedDateTime newStart = dto.startTime().orElse(null);
            if (!Objects.equals(newStart, existingEvent.getStartTime())) {
                logger.info("Updating start time for event {}: [{} -> {}]",
                        existingEvent.getId(), existingEvent.getStartTime(), newStart);
                existingEvent.setStartTime(newStart);
                updated = true;
            }
        }

        // --- End Time ---
        if (dto.endTime() != null) {
            ZonedDateTime newEnd = dto.endTime().orElse(null);
            if (!Objects.equals(newEnd, existingEvent.getEndTime())) {
                logger.info("Updating end time for event {}: [{} -> {}]",
                        existingEvent.getId(), existingEvent.getEndTime(), newEnd);
                existingEvent.setEndTime(newEnd);
                updated = true;
            }
        }

        // --- Description ---
        if (dto.description() != null && !Objects.equals(dto.description().orElse(null), existingEvent.getDescription())) {
            existingEvent.setDescription(dto.description().orElse(null));
            updated = true;
        }

        // --- Label ---
        if (dto.labelId() != null) {
            Long newLabelId = dto.labelId().orElse(null);
            Long resolvedLabelId = (newLabelId != null)
                    ? newLabelId
                    : existingEvent.getCreator().getUnlabeled().getId();

            Long currentLabelId = existingEvent.getLabel().getId();

            if (!Objects.equals(resolvedLabelId, currentLabelId)) {
                Label newLabel = labelService.getLabelEntityById(resolvedLabelId);
                ownershipValidator.validateLabelOwnership(existingEvent.getCreator().getId(), newLabel);
                existingEvent.setLabel(newLabel);
                updated = true;
            }
        }

        // --- Completion ---
        if (dto.isCompleted() != null && !Objects.equals(dto.isCompleted(), existingEvent.isCompleted())) {
            if (dto.isCompleted()) {
                if (existingEvent.isUnconfirmed()) {
                    throw new InvalidEventStateException(EVENT_NOT_CONFIRMED);
                }
                if (existingEvent.getName() == null) {
                    throw new InvalidEventStateException(MISSING_EVENT_NAME);
                }
                if (existingEvent.getStartTime() == null) {
                    throw new InvalidEventStateException(MISSING_EVENT_START_TIME);
                }
                if (existingEvent.getEndTime() == null) {
                    throw new InvalidEventStateException(MISSING_EVENT_END_TIME);
                }
            }
            existingEvent.setCompleted(dto.isCompleted());
            updated = true;
        }

        return updated;
    }
}