package com.yohan.event_planner.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.yohan.event_planner.config.jackson.OptionalNullableDeserializer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/**
 * DTO for updating an existing recurring event. All fields are optional and may be {@code null}.
 * Only provided fields will be applied during the update.
 *
 * <p>This DTO implements skip vs clear semantics for Optional fields:
 * <ul>
 *   <li><strong>Skip (null field)</strong>: Field is omitted from DTO → no change to recurring event</li>
 *   <li><strong>Clear (Optional.empty())</strong>: Field contains empty Optional → field set to null</li>
 *   <li><strong>Update (Optional.of(value))</strong>: Field contains value → field updated to value</li>
 * </ul>
 *
 * <p><strong>Note:</strong> Confirmation of recurring events is handled via a separate 
 * {@code confirmRecurringEvent()} endpoint, not through this update DTO. This maintains 
 * clear separation between field updates and state transitions.</p>
 *
 * @param name               optional updated name
 * @param startTime          optional updated start time
 * @param endTime            optional updated end time
 * @param startDate          optional updated start date of the recurrence
 * @param endDate            optional updated end date of the recurrence
 * @param description        optional updated description
 * @param labelId            optional updated label ID
 * @param recurrenceRule     optional updated recurrence rule (iCalendar format)
 */
public record RecurringEventUpdateDTO(
        @JsonDeserialize(using = OptionalNullableDeserializer.class)
        Optional<String> name,
        @JsonDeserialize(using = OptionalNullableDeserializer.class)
        Optional<LocalTime> startTime,
        @JsonDeserialize(using = OptionalNullableDeserializer.class)
        Optional<LocalTime> endTime,
        @JsonDeserialize(using = OptionalNullableDeserializer.class)
        Optional<LocalDate> startDate,
        @JsonDeserialize(using = OptionalNullableDeserializer.class)
        Optional<LocalDate> endDate,
        @JsonDeserialize(using = OptionalNullableDeserializer.class)
        Optional<String> description,
        @JsonDeserialize(using = OptionalNullableDeserializer.class)
        Optional<Long> labelId,
        @JsonDeserialize(using = OptionalNullableDeserializer.class)
        Optional<String> recurrenceRule
) {}
