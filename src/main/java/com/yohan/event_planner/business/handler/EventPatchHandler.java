package com.yohan.event_planner.business.handler;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.EventUpdateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

/**
 * Applies validated partial updates to an existing {@link Event} entity based on data
 * from a {@link EventUpdateDTO} patch object.
 *
 * <p>
 * This handler is responsible for in-place updates to fields such as name, time range,
 * and description. Fields are only updated if:
 * <ul>
 *   <li>The field in the DTO is non-null</li>
 *   <li>The new value differs from the current value</li>
 * </ul>
 * </p>
 *
 * <p>
 * This component performs <strong>mutation only</strong>; it does <em>not</em> perform validation,
 * conflict detection, or authorization. All such logic is expected to be enforced upstream.
 * </p>
 *
 * <p>
 * Updates are <strong>atomic</strong>: either all validations pass and changes are applied,
 * or nothing is updated.
 * </p>
 *
 * @see EventUpdateDTO
 */
@Component
public class EventPatchHandler {

    private static final Logger logger = LoggerFactory.getLogger(EventPatchHandler.class);

    public EventPatchHandler() {
        // Default constructor
    }

    /**
     * Applies non-null and changed fields from the provided {@link EventUpdateDTO}
     * to the target {@link Event} instance.
     *
     * <p>
     * If either the start or end time is updated, both are reassigned to maintain consistency.
     * </p>
     *
     * @param existingEvent the existing event entity to be modified
     * @param dto           the incoming patch data
     * @param user          the authenticated user performing the update (reserved for future use)
     * @return {@code true} if any fields were modified; otherwise {@code false}
     */
    public boolean applyPatch(Event existingEvent, EventUpdateDTO dto, User user) {
        boolean updated = false;

        if (dto.name() != null && !dto.name().equals(existingEvent.getName())) {
            logger.info("Patching event name: '{}' → '{}'", existingEvent.getName(), dto.name());
            existingEvent.setName(dto.name());
            updated = true;
        }

        boolean startChanged = dto.startTime() != null && !dto.startTime().equals(existingEvent.getStartTime());
        boolean endChanged = dto.endTime() != null && !dto.endTime().equals(existingEvent.getEndTime());

        if (startChanged || endChanged) {
            ZonedDateTime newStart = dto.startTime() != null ? dto.startTime() : existingEvent.getStartTime();
            ZonedDateTime newEnd = dto.endTime() != null ? dto.endTime() : existingEvent.getEndTime();

            logger.info("Patching event time: [{} - {}] → [{} - {}]",
                    existingEvent.getStartTime(), existingEvent.getEndTime(),
                    newStart, newEnd);

            existingEvent.setStartTime(newStart);
            existingEvent.setEndTime(newEnd);
            updated = true;
        }

        if (dto.description() != null && !dto.description().equals(existingEvent.getDescription())) {
            logger.info("Patching event description.");
            existingEvent.setDescription(dto.description());
            updated = true;
        }

        return updated;
    }
}
