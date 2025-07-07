package com.yohan.event_planner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.yohan.event_planner.config.jackson.OptionalNullableDeserializer;
import com.yohan.event_planner.domain.Event;

import jakarta.validation.constraints.Size;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Request DTO for partially updating an existing {@link Event}.
 *
 * <p>
 * Used for PATCH-style operations where fields may be added, modified, or cleared.
 * Only non-null fields are processed during patching.
 * </p>
 *
 * <h2>Clearing vs Skipping</h2>
 * <ul>
 *     <li><b>Omit a field</b> to leave it unchanged (absent field maps to null).</li>
 *     <li><b>Pass explicit {@code null} inside the {@code Optional}</b> to clear it.</li>
 * </ul>
 *
 * <h2>Time Handling</h2>
 * <ul>
 *     <li>All time values must include a time zone.</li>
 *     <li>They are stored in UTC and accompanied by the original time zone ID.</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventUpdateDTO(

        /**
         * Optional new event name.
         * <p>Use {@code Optional.empty()} to clear it. Omit to leave unchanged.</p>
         */
        @JsonDeserialize(using = OptionalNullableDeserializer.class)
        Optional<@Size(min = 1, max = 100, message = "Event name must be between 1 and 100 characters") String> name,

        /**
         * Optional new start time.
         * <p>Use {@code Optional.empty()} to clear it. Omit to leave unchanged.</p>
         */
        @JsonDeserialize(using = OptionalNullableDeserializer.class)
        Optional<ZonedDateTime> startTime,

        /**
         * Optional new end time.
         * <p>Use {@code Optional.empty()} to clear it. Omit to leave unchanged.</p>
         */
        @JsonDeserialize(using = OptionalNullableDeserializer.class)
        Optional<ZonedDateTime> endTime,

        /**
         * Optional new description.
         * <p>Use {@code Optional.empty()} to clear it. Omit to leave unchanged.</p>
         */
        @JsonDeserialize(using = OptionalNullableDeserializer.class)
        Optional<@Size(max = 500, message = "Description must not exceed 500 characters") String> description,

        /**
         * Optional new label ID to assign.
         * <p>Use {@code Optional.empty()} to reset to the creator's "Unlabeled" label. Omit to leave unchanged.</p>
         */
        @JsonDeserialize(using = OptionalNullableDeserializer.class)
        Optional<Long> labelId,

        /**
         * Whether to mark the event as completed or not.
         * <p>If {@code null}, the completion status remains unchanged.</p>
         */
        Boolean isCompleted
) {}
