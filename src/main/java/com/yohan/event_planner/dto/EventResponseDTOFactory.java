package com.yohan.event_planner.dto;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Factory for building EventResponseDTOs for both real and virtual events.
 */
@Component
public class EventResponseDTOFactory {

    public EventResponseDTO createFromEvent(Event event) {
        String creatorZone = event.getCreator().getTimezone();
        String startZone = event.getStartTimezone();
        String endZone = event.getEndTimezone();

        String startTimeZone = !creatorZone.equals(startZone) ? startZone : null;
        String endTimeZone = !creatorZone.equals(endZone) ? endZone : null;

        Label label = event.getLabel(); // never null

        LabelResponseDTO labelDto = new LabelResponseDTO(
                label.getId(),
                label.getName(),
                label.getCreator() != null ? label.getCreator().getUsername() : null
        );

        return new EventResponseDTO(
                event.getId(),
                event.getName(),
                event.getStartTime(),
                event.getEndTime(),
                event.getDurationMinutes(),
                startTimeZone,
                endTimeZone,
                event.getDescription(),
                event.getCreator().getUsername(),
                event.getCreator().getTimezone(),
                labelDto,
                event.isCompleted(),
                event.isUnconfirmed(),
                false
        );
    }

    public EventResponseDTO createFromRecurringEvent(RecurringEvent recurringEvent, LocalDate occurrenceDate) {
        ZoneId zone = ZoneId.of(recurringEvent.getCreator().getTimezone());

        ZonedDateTime startTimeUtc = ZonedDateTime.of(occurrenceDate, recurringEvent.getStartTime(), zone)
                .withZoneSameInstant(ZoneId.of("UTC"));
        ZonedDateTime endTimeUtc = ZonedDateTime.of(occurrenceDate, recurringEvent.getEndTime(), zone)
                .withZoneSameInstant(ZoneId.of("UTC"));

        int durationMinutes = (int) java.time.Duration.between(startTimeUtc, endTimeUtc).toMinutes();

        Label label = recurringEvent.getLabel(); // never null

        LabelResponseDTO labelDto = new LabelResponseDTO(
                label.getId(),
                label.getName(),
                label.getCreator() != null ? label.getCreator().getUsername() : null
        );

        return new EventResponseDTO(
                null,
                recurringEvent.getName(),
                startTimeUtc,
                endTimeUtc,
                durationMinutes,
                null,
                null,
                recurringEvent.getDescription(),
                recurringEvent.getCreator().getUsername(),
                recurringEvent.getCreator().getTimezone(),
                labelDto,
                false,
                false,
                true
        );
    }
}