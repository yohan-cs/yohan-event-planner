package com.yohan.event_planner.service;

import com.yohan.event_planner.dto.EventChangeContextDTO;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public interface LabelTimeBucketService {

    /**
     * Reverts the impact of a previously completed event from the relevant label time buckets.
     *
     * @param userId           the ID of the user who owns the event
     * @param labelId          the label associated with the event
     * @param startTime        the original start time of the event (in UTC)
     * @param durationMinutes  the duration of the event in minutes
     * @param timezone         the user's timezone to determine local week/month
     */
    void revert(Long userId, Long labelId, ZonedDateTime startTime, int durationMinutes, ZoneId timezone);

    /**
     * Applies the impact of a newly completed event to the relevant label time buckets.
     *
     * @param userId           the ID of the user who owns the event
     * @param labelId          the label associated with the event
     * @param startTime        the new start time of the event (in UTC)
     * @param durationMinutes  the duration of the event in minutes
     * @param timezone         the user's timezone to determine local week/month
     */
    void apply(Long userId, Long labelId, ZonedDateTime startTime, int durationMinutes, ZoneId timezone);

    /**
     * Applies label time bucket adjustments based on a completed event change.
     *
     * <p>Contract assumptions:
     * <ul>
     *   <li>{@code userId} and {@code timezone} must not be null</li>
     *   <li>If {@code wasCompleted} is {@code true}, then {@code oldLabelId}, {@code oldStartTime},
     *       and {@code oldDurationMinutes} must all be non-null</li>
     *   <li>If {@code isNowCompleted} is {@code true}, then {@code newLabelId}, {@code newStartTime},
     *       and {@code newDurationMinutes} must all be non-null</li>
     * </ul>
     *
     * <p>This method performs no internal null checks and assumes all inputs are valid.
     */
    void handleEventChange(EventChangeContextDTO dto);
}
