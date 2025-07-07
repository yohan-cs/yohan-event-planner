package com.yohan.event_planner.domain.enums;

/**
 * Represents a user-facing time window for filtering or computing stats.
 *
 * <p>
 * This enum is distinct from {@link TimeBucketType}, which represents storage granularity.
 * {@code TimeWindow} is used for filtering or aggregating user activity in specific temporal ranges.
 * </p>
 */
public enum TimeWindow {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    LAST_WEEK,
    LAST_MONTH,
    ALL_TIME
}