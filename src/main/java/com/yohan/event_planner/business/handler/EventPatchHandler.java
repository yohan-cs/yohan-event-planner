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
 *   <li>The new value differs from the current value, or is explicitly cleared (e.g. {@code Optional.empty()})</li>
 * </ul>
 * </p>
 *
 * <h2>Time and Duration Handling</h2>
 * <ul>
 *     <li>If either the start or end time is updated, both are reassigned to maintain consistency.</li>
 *     <li>If {@code endTime} is explicitly cleared (via {@code Optional.empty()}), the event is made open-ended.</li>
 *     <li>{@code durationMinutes} is automatically recalculated or cleared based on {@code endTime} changes.</li>
 *     <li>All time values are normalized to UTC when stored.</li>
 * </ul>
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
     * If either {@code startTime} or {@code endTime} is updated (or explicitly cleared),
     * the event's time range is reassigned. If {@code endTime} is present, the duration
     * is recalculated in whole minutes. If it is cleared, {@code durationMinutes} is also removed.
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

        boolean startChanged = dto.startTime() != null &&
                !dto.startTime().equals(existingEvent.getStartTime());

        boolean endChanged = dto.endTime() != null && (
                (dto.endTime().isEmpty() && existingEvent.getEndTime() != null) ||
                        (dto.endTime().isPresent() && !dto.endTime().get().equals(existingEvent.getEndTime()))
        );

        if (startChanged || endChanged) {
            ZonedDateTime newStart = dto.startTime() != null
                    ? dto.startTime()
                    : existingEvent.getStartTime();

            logger.info("Patching event time: [{} - {}] → [{} - {}]",
                    existingEvent.getStartTime(),
                    existingEvent.getEndTime(),
                    newStart,
                    (dto.endTime() != null && dto.endTime().isPresent()) ? dto.endTime().get() : null
            );

            if (dto.endTime() != null && dto.endTime().isPresent()) {
                ZonedDateTime newEnd = dto.endTime().get();
                existingEvent.setStartTime(newStart);
                existingEvent.setEndTime(newEnd);

                long minutes = java.time.Duration.between(
                        newStart.withZoneSameInstant(java.time.ZoneOffset.UTC),
                        newEnd.withZoneSameInstant(java.time.ZoneOffset.UTC)
                ).toMinutes();
                existingEvent.setDurationMinutes((int) minutes);

            } else if (dto.endTime() != null && dto.endTime().isEmpty()) {
                existingEvent.setStartTime(newStart);
                existingEvent.setEndTime(null); // also clears durationMinutes via setter
            } else {
                existingEvent.setStartTime(newStart);
            }

            updated = true;
        }

        if (dto.description() != null) {
            if (dto.description().isEmpty()) {
                if (existingEvent.getDescription() != null) {
                    logger.info("Clearing event description.");
                    existingEvent.setDescription(null);
                    updated = true;
                }
            } else {
                String newDescription = dto.description().get();
                if (!newDescription.equals(existingEvent.getDescription())) {
                    logger.info("Patching event description.");
                    existingEvent.setDescription(newDescription);
                    updated = true;
                }
            }
        }

        return updated;
    }

}
