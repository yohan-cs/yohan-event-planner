package com.yohan.event_planner.domain.enums;

/**
 * Enumeration of time bucket types used for aggregating and storing time statistics.
 * 
 * <p>This enum defines the granularity at which time data is bucketed and stored for
 * analytics and reporting purposes. Each bucket type represents a different temporal
 * aggregation strategy used by the {@link com.yohan.event_planner.domain.LabelTimeBucket} entity.</p>
 * 
 * <h2>Bucketing Strategies</h2>
 * <p>Each bucket type uses specific rules for time period calculation:</p>
 * <ul>
 *   <li><strong>DAY</strong>: Calendar days for detailed daily tracking</li>
 *   <li><strong>WEEK</strong>: ISO week numbering for consistent weekly aggregation</li>
 *   <li><strong>MONTH</strong>: Calendar months for monthly reporting</li>
 * </ul>
 * 
 * <h2>Storage Considerations</h2>
 * <p>Different bucket types have different storage and query characteristics:</p>
 * <ul>
 *   <li><strong>Higher granularity</strong> (DAY): More records, detailed analysis</li>
 *   <li><strong>Lower granularity</strong> (MONTH): Fewer records, summary analysis</li>
 *   <li><strong>Balance</strong> (WEEK): Moderate detail with reasonable storage</li>
 * </ul>
 * 
 * @see com.yohan.event_planner.domain.LabelTimeBucket
 * @see com.yohan.event_planner.domain.enums.TimeWindow
 */
public enum TimeBucketType {
    
    /**
     * Daily time buckets for detailed day-by-day time tracking.
     * <p>Provides the highest granularity for precise daily analytics
     * and detailed time usage patterns.</p>
     */
    DAY,
    
    /**
     * Weekly time buckets using ISO week numbering (ISO 8601).
     * <p>Weeks start on Monday and provide consistent weekly aggregation
     * across year boundaries. Uses ISO week-based years which may differ
     * from calendar years.</p>
     */
    WEEK,
    
    /**
     * Monthly time buckets based on calendar months.
     * <p>Provides summary-level aggregation for monthly reporting
     * and long-term trend analysis.</p>
     */
    MONTH
}
