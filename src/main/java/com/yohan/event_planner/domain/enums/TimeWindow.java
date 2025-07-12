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
    
    /**
     * Current day from midnight to end of day.
     * <p>Filters or aggregates events occurring within today's 24-hour period.</p>
     */
    TODAY,
    
    /**
     * Current week from Monday to Sunday (ISO week).
     * <p>Includes events from the start of the current ISO week to the end of Sunday.</p>
     */
    THIS_WEEK,
    
    /**
     * Current calendar month from first day to last day.
     * <p>Covers all events within the current month boundaries.</p>
     */
    THIS_MONTH,
    
    /**
     * Previous week from Monday to Sunday (ISO week).
     * <p>Includes events from the complete previous ISO week period.</p>
     */
    LAST_WEEK,
    
    /**
     * Previous calendar month from first day to last day.
     * <p>Covers all events within the previous month boundaries.</p>
     */
    LAST_MONTH,
    
    /**
     * All available time without any temporal filtering.
     * <p>Includes events from the earliest recorded time to the latest possible time.</p>
     */
    ALL_TIME
}