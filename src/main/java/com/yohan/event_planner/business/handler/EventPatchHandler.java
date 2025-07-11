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
 * 
 * <p>This handler implements a sophisticated skip vs clear semantic for Optional fields:
 * <ul>
 *   <li><strong>Skip (null field)</strong>: Field is omitted from DTO → no change to event</li>
 *   <li><strong>Clear (Optional.empty())</strong>: Field contains empty Optional → field set to null</li>
 *   <li><strong>Update (Optional.of(value))</strong>: Field contains value → field updated to value</li>
 * </ul>
 * 
 * <h3>Examples</h3>
 * <pre>
 * // Skip name field - no change
 * EventUpdateDTO dto = new EventUpdateDTO(null, ...);
 * 
 * // Clear name field - set to null
 * EventUpdateDTO dto = new EventUpdateDTO(Optional.empty(), ...);
 * 
 * // Update name field - set to "New Name"
 * EventUpdateDTO dto = new EventUpdateDTO(Optional.of("New Name"), ...);
 * </pre>
 * 
 * <h3>Architecture Context</h3>
 * <p><strong>Layer Responsibilities:</strong>
 * <ul>
 *   <li><strong>EventService</strong>: Input validation, authorization, and business orchestration</li>
 *   <li><strong>EventBO</strong>: Business validation, persistence, and conflict detection</li>
 *   <li><strong>This Handler</strong>: Pure field patching logic with completion state validation</li>
 * </ul>
 * 
 * <p><strong>Validation Strategy:</strong> This handler performs only completion validation
 * to prevent invalid completed events. All other business validation is delegated to 
 * {@link com.yohan.event_planner.business.EventBO}.
 * 
 * <p><strong>Label Handling:</strong> When clearing a label (Optional.empty()), the event
 * is automatically assigned to the creator's "Unlabeled" label rather than leaving it null.
 * 
 * <p><strong>State Management:</strong> Draft (unconfirmed) events are allowed to have 
 * incomplete fields and are not subject to completion validation.
 * 
 * @since 1.0
 * @author Event Planning System
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
     * <p>This method implements field-by-field patching using the skip vs clear semantics:
     * <ul>
     *   <li><strong>Skip</strong>: When a DTO field is null, the corresponding event field is left unchanged</li>
     *   <li><strong>Clear</strong>: When a DTO field contains {@code Optional.empty()}, the event field is set to null</li>
     *   <li><strong>Update</strong>: When a DTO field contains a value, the event field is updated to that value</li>
     * </ul>
     * 
 * <p>This method handles sophisticated field patching with different strategies based on completion state:</p>
     * 
     * <h3>Field Validation Strategy</h3>
     * <p>The method applies a two-phase validation approach:</p>
     * <ol>
     *   <li><strong>Field Patching</strong>: All fields are updated according to skip/clear/update semantics</li>
     *   <li><strong>Completion Validation</strong>: For events being marked as completed, validates that required fields
     *       (name, startTime, endTime) are not null and event is confirmed</li>
     * </ol>
     * 
     * <h3>Label Resolution</h3>
     * <p>When clearing a label (Optional.empty()), the event is automatically assigned to the creator's
     * "Unlabeled" label rather than being left without a label. This maintains data integrity and
     * ensures all events remain categorized.</p>
     * 
     * <h3>Return Value Semantics</h3>
     * <p>Returns {@code true} if any field was actually modified, {@code false} if no changes were made.
     * This includes cases where:
     * <ul>
     *   <li>All DTO fields are null (skip all)</li>
     *   <li>All DTO fields contain the same values as the existing event</li>
     *   <li>Mixed scenarios where some fields are updated but result in the same values</li>
     * </ul>
     * 
 * @param existingEvent the existing event to patch
     * @param dto the update payload with optional fields using skip/clear/update semantics
     * @return true if any field was changed, false otherwise
     * @throws InvalidEventStateException if attempting to complete an invalid or unconfirmed event
     */
    public boolean applyPatch(Event existingEvent, EventUpdateDTO dto) {
        logger.debug("Applying patch to event ID {}", existingEvent.getId());
        boolean updated = false;

        // --- Name ---
        // Skip: dto.name() == null → no change
        // Clear: dto.name() == Optional.empty() → set to null  
        // Update: dto.name() == Optional.of(value) → set to value
        if (dto.name() != null && !Objects.equals(dto.name().orElse(null), existingEvent.getName())) {
            String newName = dto.name().orElse(null);
            logger.info("Updating name for event {}: [{}] -> [{}]", 
                    existingEvent.getId(), existingEvent.getName(), newName);
            existingEvent.setName(newName);
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
        // Same skip/clear/update semantics as other Optional fields
        if (dto.description() != null && !Objects.equals(dto.description().orElse(null), existingEvent.getDescription())) {
            String newDescription = dto.description().orElse(null);
            logger.debug("Updating description for event {}: [{}] -> [{}]", 
                    existingEvent.getId(), 
                    existingEvent.getDescription() != null ? "<has description>" : "<null>",
                    newDescription != null ? "<has description>" : "<null>");
            existingEvent.setDescription(newDescription);
            updated = true;
        }

        // --- Label ---
        // Skip: dto.labelId() == null → no change
        // Clear: dto.labelId() == Optional.empty() → assign to "Unlabeled" label (not null)
        // Update: dto.labelId() == Optional.of(id) → assign to specified label
        if (dto.labelId() != null) {
            Long newLabelId = dto.labelId().orElse(null);
            Long targetLabelId = resolveTargetLabelId(newLabelId, existingEvent);

            Long currentLabelId = existingEvent.getLabel().getId();

            if (!Objects.equals(targetLabelId, currentLabelId)) {
                Label newLabel = labelService.getLabelEntityById(targetLabelId);
                ownershipValidator.validateLabelOwnership(existingEvent.getCreator().getId(), newLabel);
                String currentLabelName = existingEvent.getLabel() != null ? existingEvent.getLabel().getName() : "null";
                String newLabelName = newLabel != null ? newLabel.getName() : "null";
                logger.info("Updating label for event {}: [{}] -> [{}]", 
                        existingEvent.getId(), currentLabelName, newLabelName);
                existingEvent.setLabel(newLabel);
                updated = true;
            }
        }

        // --- Completion ---
        // Boolean field - no Optional wrapper, so only skip (null) vs update (true/false)
        if (dto.isCompleted() != null && !Objects.equals(dto.isCompleted(), existingEvent.isCompleted())) {
            if (dto.isCompleted()) {
                validateEventCanBeCompleted(existingEvent);
            }
            logger.info("Updating completion status for event {}: [{}] -> [{}]", 
                    existingEvent.getId(), existingEvent.isCompleted(), dto.isCompleted());
            existingEvent.setCompleted(dto.isCompleted());
            updated = true;
        }

        if (updated) {
            logger.debug("Event ID {} was modified during patch operation", existingEvent.getId());
        } else {
            logger.debug("No changes made to event ID {} during patch operation", existingEvent.getId());
        }
        return updated;
    }

    /**
     * Resolves the target label ID for an event update.
     * 
     * <p>When clearing a label (newLabelId is null), the event is assigned to the 
     * creator's "Unlabeled" label rather than being left with a null label.
     * 
     * @param newLabelId the label ID from the DTO (null when clearing)
     * @param event the event being updated
     * @return the resolved label ID to assign
     */
    private Long resolveTargetLabelId(Long newLabelId, Event event) {
        return (newLabelId != null) 
                ? newLabelId 
                : event.getCreator().getUnlabeled().getId();
    }

    /**
     * Validates that an event can be marked as completed.
     * 
     * <p>Confirmed events must have all required fields populated when being marked as completed:
     * name, startTime, and endTime. Additionally, only confirmed events can be marked as completed.</p>
     * 
     * @param event the event to validate for completion
     * @throws InvalidEventStateException if the event cannot be completed
     */
    private void validateEventCanBeCompleted(Event event) {
        logger.debug("Validating event ID {} can be completed", event.getId());
        if (event.isUnconfirmed()) {
            logger.warn("Attempted to complete unconfirmed event ID {}", event.getId());
            throw new InvalidEventStateException(EVENT_NOT_CONFIRMED);
        }
        if (event.getName() == null) {
            logger.warn("Cannot complete event ID {}: missing name", event.getId());
            throw new InvalidEventStateException(MISSING_EVENT_NAME);
        }
        if (event.getStartTime() == null) {
            logger.warn("Cannot complete event ID {}: missing start time", event.getId());
            throw new InvalidEventStateException(MISSING_EVENT_START_TIME);
        }
        if (event.getEndTime() == null) {
            logger.warn("Cannot complete event ID {}: missing end time", event.getId());
            throw new InvalidEventStateException(MISSING_EVENT_END_TIME);
        }
        logger.debug("Event ID {} passed completion validation", event.getId());
    }
}