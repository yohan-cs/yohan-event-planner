package com.yohan.event_planner.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.yohan.event_planner.config.jackson.OptionalNullableDeserializer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;

/**
 * DTO for updating an existing recurring event. All fields are optional and may be {@code null}.
 * Only provided fields will be applied during the update.
 *
 * <p>Use {@code Optional.empty()} to explicitly clear a field.</p>
 *
 * @param name               optional updated name
 * @param startTime          optional updated start time
 * @param endTime            optional updated end time
 * @param startDate          optional updated start date of the recurrence
 * @param endDate            optional updated end date of the recurrence
 * @param description        optional updated description
 * @param labelId            optional updated label ID
 * @param recurrenceRule     optional updated recurrence rule (iCalendar format)
 * @param unconfirmed        optional updated draft status (true = draft)
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
        Optional<String> recurrenceRule,
        @JsonDeserialize(using = OptionalNullableDeserializer.class)
        Optional<Boolean> unconfirmed
) {}
