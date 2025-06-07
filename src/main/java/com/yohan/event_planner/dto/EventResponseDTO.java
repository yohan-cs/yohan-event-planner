package com.yohan.event_planner.dto;

import java.time.ZonedDateTime;

/**
 * Response DTO representing a fully populated view of an event.
 *
 * <p>
 * All time values are returned in UTC. If the original time zone of the event's
 * start or end differs from the creator's profile time zone, that information is
 * included separately to allow clients to re-construct the original local time.
 * </p>
 */
public record EventResponseDTO(

        /** Unique identifier of the event. */
        Long id,

        /** Name or title of the event. */
        String name,

        /** Event start time in UTC. */
        ZonedDateTime startTimeUtc,

        /** Event end time in UTC. May be {@code null} for open-ended events. */
        ZonedDateTime endTimeUtc,

        /** Duration of the event in whole minutes. {@code null} if end time is not provided. */
        Integer durationMinutes,

        /**
         * Original time zone ID used for the event's start time, if different
         * from the creator's time zone. Otherwise {@code null}.
         */
        String startTimeZone,

        /**
         * Original time zone ID used for the event's end time, if different
         * from the creator's time zone. Otherwise {@code null}.
         */
        String endTimeZone,

        /** Optional event description, if provided by the creator. */
        String description,

        /** ID of the user who created the event. */
        Long creatorId,

        /** Username of the user who created the event. */
        String creatorUsername,

        /** Time zone of the user who created the event. */
        String creatorTimezone
) {}
