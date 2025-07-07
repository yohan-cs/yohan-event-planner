package com.yohan.event_planner.dto;

import com.yohan.event_planner.domain.Event;

import java.time.ZonedDateTime;

/**
 * Request DTO for creating a new {@link Event} via form-based input.
 *
 * <p>
 * This DTO supports both:
 * <ul>
 *     <li><b>Scheduled events</b> — confirmed upon creation if {@code isDraft = false}</li>
 *     <li><b>Unconfirmed drafts</b> — saved as drafts if {@code isDraft = true}, regardless of field completeness</li>
 * </ul>
 * </p>
 *
 * <p>
 * The {@code isDraft} flag determines whether the event is saved as a confirmed scheduled event or a draft.
 * The backend no longer infers this based on which fields are filled. If {@code isDraft = false}, then
 * {@code name}, {@code startTime}, and {@code endTime} must all be non-null, or an error will be thrown.
 * </p>
 *
 * <p>
 * Impromptu events (which require only a start time and no form input) do <b>not</b> use this DTO.
 * </p>
 *
 * <p>
 * Time values (if provided) must include a time zone offset (i.e., be fully formed {@link ZonedDateTime} instances).
 * When persisted, they are normalized to UTC while preserving the original time zone ID for display purposes.
 * </p>
 */
public record EventCreateDTO(

        /**
         * Name or title of the event.
         */
        String name,

        /**
         * Start time of the event, including time zone.
         */
        ZonedDateTime startTime,

        /**
         * End time of the event, including time zone.
         */
        ZonedDateTime endTime,

        /**
         * Optional event description. May be {@code null}.
         */
        String description,

        /**
         * Optional label ID to associate with this event. May be {@code null}.
         */
        Long labelId,

        /**
         * Whether this event should be saved as an unconfirmed draft.
         */
        boolean isDraft
) {}
