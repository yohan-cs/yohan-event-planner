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

/**
 * Handles partial updates to {@link RecurringEvent} entities using {@link RecurringEventUpdateDTO} payloads.
 * 
 * <p>This handler implements the same sophisticated skip vs clear semantics as {@link EventPatchHandler}:
 * <ul>
 *   <li><strong>Skip (null field)</strong>: Field is omitted from DTO → no change to recurring event</li>
 *   <li><strong>Clear (Optional.empty())</strong>: Field contains empty Optional → field set to null</li>
 *   <li><strong>Update (Optional.of(value))</strong>: Field contains value → field updated to value</li>
 * </ul>
 * 
 * <h3>Examples</h3>
 * <pre>
 * // Skip name field - no change
 * RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(null, ...);
 * 
 * // Clear name field - set to null
 * RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(Optional.empty(), ...);
 * 
 * // Update name field - set to "New Name"
 * RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(Optional.of("New Name"), ...);
 * </pre>
 * 
 * <h3>Recurrence Rule Complexity</h3>
 * <p>Recurrence rule handling varies significantly based on confirmation status:
 * <ul>
 *   <li><strong>Confirmed events</strong>: Rules are parsed, validated, and summarized via {@link RecurrenceRuleService}</li>
 *   <li><strong>Unconfirmed (draft) events</strong>: Rules are stored as raw strings without parsing for flexibility</li>
 *   <li><strong>Rule clearing</strong>: Only allowed for unconfirmed events to prevent data loss</li>
 * </ul>
 * 
 * <h3>Architecture Context</h3>
 * <p><strong>Layer Responsibilities:</strong>
 * <ul>
 *   <li><strong>RecurringEventService</strong>: Input validation, authorization, and change propagation to future events</li>
 *   <li><strong>RecurringEventBO</strong>: Business validation, persistence, and complex recurrence operations</li>
 *   <li><strong>This Handler</strong>: Pure field patching logic with confirmation state validation</li>
 *   <li><strong>RecurrenceRuleService</strong>: Parsing and validation of recurrence patterns</li>
 * </ul>
 * 
 * <p><strong>Validation Strategy:</strong> This handler performs only confirmation state validation
 * to prevent invalid confirmed recurring events. All other business validation is delegated to 
 * {@link com.yohan.event_planner.business.RecurringEventBO}.
 * 
 * <p><strong>Label Handling:</strong> When clearing a label (Optional.empty()), the recurring event
 * is automatically assigned to the creator's "Unlabeled" label rather than leaving it null.
 * 
 * <p><strong>Change Propagation:</strong> The service layer detects field changes and propagates
 * updates to future {@link com.yohan.event_planner.domain.Event} instances when confirmed 
 * recurring events are modified.
 * 
 * @since 1.0
 * @author Event Planning System
 */
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
     * <p>This method handles sophisticated field patching with different strategies based on confirmation state:</p>
     * 
     * <h3>Recurrence Rule Processing</h3>
     * <p>The recurrence rule handling varies significantly based on event confirmation status:</p>
     * <ul>
     *   <li><strong>Confirmed Events</strong>: Rules are parsed via {@link RecurrenceRuleService#parseFromString(String)},
     *       validated, and summarized using {@link RecurrenceRuleService#buildSummary(ParsedRecurrenceInput, LocalDate, LocalDate)}.
     *       The resulting {@link RecurrenceRuleVO} contains both the summary and parsed representation.</li>
     *   <li><strong>Unconfirmed Events</strong>: Rules are stored as raw strings in the summary field only,
     *       with parsed field remaining null. This allows flexibility for draft patterns that may not
     *       yet be valid or complete.</li>
     * </ul>
     * 
     * <h3>Field Validation Strategy</h3>
     * <p>The method applies a two-phase validation approach:</p>
     * <ol>
     *   <li><strong>Field Patching</strong>: All fields are updated according to skip/clear/update semantics</li>
     *   <li><strong>Confirmation Validation</strong>: For confirmed events, validates that required fields
     *       (name, startTime, endTime, startDate, endDate) are not null</li>
     * </ol>
     * 
     * <h3>Label Resolution</h3>
     * <p>When clearing a label (Optional.empty()), the event is automatically assigned to the creator's
     * "Unlabeled" label rather than being left without a label. This maintains data integrity and
     * ensures all events remain categorized.</p>
     *
     * @param existing the existing recurring event to patch
     * @param dto      the update payload with optional fields using skip/clear/update semantics
     * @return true if any field was changed, false otherwise
     * @throws InvalidEventStateException if a confirmed recurring event is left invalid after patching
     */
    public boolean applyPatch(RecurringEvent existing, RecurringEventUpdateDTO dto) {
        logger.debug("Applying patch to recurring event ID {}", existing.getId());
        boolean updated = false;

        // --- Name ---
        // Skip if dto.name() is null; otherwise apply clear (empty) or update (value)
        if (dto.name() != null && !Objects.equals(dto.name().orElse(null), existing.getName())) {
            String newName = dto.name().orElse(null);
            logger.info("Updating name for recurring event {}: [{}] -> [{}]", 
                    existing.getId(), existing.getName(), newName);
            existing.setName(newName);
            updated = true;
        }

        // --- Start Time ---
        // Skip if dto.startTime() is null; otherwise apply clear (empty) or update (value)
        if (dto.startTime() != null) {
            LocalTime newStart = dto.startTime().orElse(null);
            if (!Objects.equals(newStart, existing.getStartTime())) {
                logger.info("Updating start time for recurring event {}: [{} -> {}]", existing.getId(), existing.getStartTime(), newStart);
                existing.setStartTime(newStart);
                updated = true;
            }
        }

        // --- End Time ---
        // Skip if dto.endTime() is null; otherwise apply clear (empty) or update (value)
        if (dto.endTime() != null) {
            LocalTime newEnd = dto.endTime().orElse(null);
            if (!Objects.equals(newEnd, existing.getEndTime())) {
                logger.info("Updating end time for recurring event {}: [{} -> {}]", existing.getId(), existing.getEndTime(), newEnd);
                existing.setEndTime(newEnd);
                updated = true;
            }
        }

        // --- Start Date ---
        // Skip if dto.startDate() is null; otherwise apply clear (empty) or update (value)
        if (dto.startDate() != null) {
            LocalDate newStartDate = dto.startDate().orElse(null);
            if (!Objects.equals(newStartDate, existing.getStartDate())) {
                logger.info("Updating start date for recurring event {}: [{}] -> [{}]", 
                        existing.getId(), existing.getStartDate(), newStartDate);
                existing.setStartDate(newStartDate);
                updated = true;
            }
        }

        // --- End Date ---
        // Skip if dto.endDate() is null; otherwise apply clear (empty) or update (value)
        if (dto.endDate() != null) {
            LocalDate newEndDate = dto.endDate().orElse(null);
            if (!Objects.equals(newEndDate, existing.getEndDate())) {
                logger.info("Updating end date for recurring event {}: [{}] -> [{}]", 
                        existing.getId(), existing.getEndDate(), newEndDate);
                existing.setEndDate(newEndDate);
                updated = true;
            }
        }

        // --- Description ---
        // Skip if dto.description() is null; otherwise apply clear (empty) or update (value)
        if (dto.description() != null && !Objects.equals(dto.description().orElse(null), existing.getDescription())) {
            String newDescription = dto.description().orElse(null);
            logger.debug("Updating description for recurring event {}: [{}] -> [{}]", 
                    existing.getId(), 
                    existing.getDescription() != null ? "<has description>" : "<null>",
                    newDescription != null ? "<has description>" : "<null>");
            existing.setDescription(newDescription);
            updated = true;
        }

        // --- Label ---
        // Skip if dto.labelId() is null; otherwise apply clear (empty -> Unlabeled) or update (value)
        if (dto.labelId() != null) {
            logger.debug("Patching label for recurring event {}", existing.getId());
            updated = patchRecurringEventLabel(existing, dto.labelId().orElse(null)) || updated;
        }

        // --- Recurrence Rule ---
        // Skip if dto.recurrenceRule() is null; otherwise apply clear (empty) or update (value)
        // Complex logic: confirmed events parse rules, unconfirmed events store raw strings
        if (dto.recurrenceRule() != null) {
            logger.debug("Patching recurrence rule for recurring event {}", existing.getId());
            updated = patchRecurrenceRule(existing, dto.recurrenceRule().orElse(null)) || updated;
        }

        // --- Confirmation State Validation ---
        // Only confirmed events require all fields to be non-null
        validateConfirmedRecurringEventState(existing);

        if (updated) {
            logger.debug("Recurring event ID {} was modified during patch operation", existing.getId());
        } else {
            logger.debug("No changes made to recurring event ID {} during patch operation", existing.getId());
        }
        return updated;
    }

    /**
     * Validates that a confirmed recurring event has all required fields populated.
     * 
     * <p>Confirmed recurring events must have all required fields populated when confirmed:
     * name, startTime, endTime, startDate, and endDate. Unconfirmed (draft) events
     * are allowed to have null values for flexibility during creation.</p>
     * 
     * @param recurringEvent the recurring event to validate
     * @throws InvalidEventStateException if a confirmed event has missing required fields
     */
    private void validateConfirmedRecurringEventState(RecurringEvent recurringEvent) {
        if (recurringEvent.isUnconfirmed()) {
            logger.debug("Skipping validation for unconfirmed recurring event ID {}", recurringEvent.getId());
            return; // Unconfirmed events can have null fields
        }
        logger.debug("Validating confirmed recurring event ID {} has all required fields", recurringEvent.getId());
        
        if (recurringEvent.getName() == null) {
            logger.warn("Confirmed recurring event ID {} is missing name", recurringEvent.getId());
            throw new InvalidEventStateException(MISSING_EVENT_NAME);
        }
        if (recurringEvent.getStartTime() == null) {
            logger.warn("Confirmed recurring event ID {} is missing start time", recurringEvent.getId());
            throw new InvalidEventStateException(MISSING_EVENT_START_TIME);
        }
        if (recurringEvent.getEndTime() == null) {
            logger.warn("Confirmed recurring event ID {} is missing end time", recurringEvent.getId());
            throw new InvalidEventStateException(MISSING_EVENT_END_TIME);
        }
        if (recurringEvent.getStartDate() == null) {
            logger.warn("Confirmed recurring event ID {} is missing start date", recurringEvent.getId());
            throw new InvalidEventStateException(MISSING_EVENT_START_DATE);
        }
        if (recurringEvent.getEndDate() == null) {
            logger.warn("Confirmed recurring event ID {} is missing end date", recurringEvent.getId());
            throw new InvalidEventStateException(MISSING_EVENT_END_DATE);
        }
        logger.debug("Recurring event ID {} passed validation", recurringEvent.getId());
    }

    /**
     * Patches the label for a recurring event with automatic "Unlabeled" fallback for clear semantics.
     * 
     * <p>Label patching implements the following logic:</p>
     * <ul>
     *   <li><strong>Update</strong>: Non-null labelId assigns the specified label</li>
     *   <li><strong>Clear</strong>: Null labelId automatically assigns the user's "Unlabeled" label</li>
     * </ul>
     * 
     * <p>This ensures that recurring events are never left without a label, maintaining data integrity
     * and consistent categorization. The method validates label ownership before assignment.</p>
     * 
     * @param recurringEvent the recurring event to patch
     * @param requestedLabelId the new label ID (null for clear semantics)
     * @return true if the label was changed, false otherwise
     */
    private boolean patchRecurringEventLabel(RecurringEvent recurringEvent, Long requestedLabelId) {
        // Apply clear semantics: null labelId resolves to user's "Unlabeled" label
        Long targetLabelId = (requestedLabelId != null)
                ? requestedLabelId
                : recurringEvent.getCreator().getUnlabeled().getId();

        Long currentLabelId = recurringEvent.getLabel() != null ? recurringEvent.getLabel().getId() : null;

        if (!Objects.equals(targetLabelId, currentLabelId)) {
            Label targetLabel = labelService.getLabelEntityById(targetLabelId);
            ownershipValidator.validateLabelOwnership(recurringEvent.getCreator().getId(), targetLabel);
            String currentLabelName = recurringEvent.getLabel() != null ? recurringEvent.getLabel().getName() : "<null>";
            logger.info("Updating label for recurring event {}: [{}] -> [{}]", 
                    recurringEvent.getId(), currentLabelName, targetLabel.getName());
            recurringEvent.setLabel(targetLabel);
            return true;
        }
        
        return false;
    }

    /**
     * Patches the recurrence rule for a recurring event with different strategies based on confirmation state.
     * 
     * <p>The patching strategy varies significantly based on event confirmation status:</p>
     * <ul>
     *   <li><strong>Confirmed Events</strong>: Rules are parsed, validated, and summarized</li>
     *   <li><strong>Unconfirmed Events</strong>: Rules are stored as raw strings for flexibility</li>
     * </ul>
     * 
     * @param recurringEvent the recurring event to patch
     * @param newRule the new recurrence rule (null for clear semantics)
     * @return true if the recurrence rule was changed, false otherwise
     */
    private boolean patchRecurrenceRule(RecurringEvent recurringEvent, String newRule) {
        RecurrenceRuleVO currentVO = recurringEvent.getRecurrenceRule();
        
        if (!recurringEvent.isUnconfirmed()) {
            // Confirmed events: Parse and validate the rule, then summarize
            logger.debug("Processing recurrence rule for confirmed recurring event ID {}", recurringEvent.getId());
            ParsedRecurrenceInput newParsed = recurrenceRuleService.parseFromString(newRule);
            String newSummary = recurrenceRuleService.buildSummary(newParsed, recurringEvent.getStartDate(), recurringEvent.getEndDate());

            if (currentVO == null ||
                    !Objects.equals(newSummary, currentVO.getSummary()) ||
                    !Objects.equals(newParsed, currentVO.getParsed())) {

                logger.info("Updated recurrence rule for confirmed recurring event {}: [{}] -> [{}]", 
                        recurringEvent.getId(), 
                        currentVO != null ? currentVO.getSummary() : "<null>", 
                        newSummary);
                recurringEvent.setRecurrenceRule(new RecurrenceRuleVO(newSummary, newParsed));
                return true;
            }
        } else {
            // Unconfirmed events: Store raw string only (parsed = null)
            logger.debug("Processing recurrence rule for unconfirmed recurring event ID {}", recurringEvent.getId());
            if (newRule == null) {
                // Clear semantics: Optional.empty() -> null rule
                if (currentVO != null) {
                    logger.info("Clearing recurrence rule for unconfirmed recurring event {}", recurringEvent.getId());
                    recurringEvent.setRecurrenceRule(null);
                    return true;
                }
            } else {
                // Update semantics: Optional.of(value) -> raw string in summary
                if (currentVO == null || !Objects.equals(newRule, currentVO.getSummary())) {
                    logger.info("Updated recurrence rule for unconfirmed recurring event {}: [{}] -> [{}]", 
                            recurringEvent.getId(), 
                            currentVO != null ? currentVO.getSummary() : "<null>", 
                            newRule);
                    recurringEvent.setRecurrenceRule(new RecurrenceRuleVO(newRule, null));
                    return true;
                }
            }
        }
        
        return false;
    }
}