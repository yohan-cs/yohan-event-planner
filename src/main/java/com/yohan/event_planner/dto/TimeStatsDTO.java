package com.yohan.event_planner.dto;

/**
 * Data transfer object for comprehensive time tracking statistics and productivity analytics.
 * 
 * <p>This DTO provides a complete view of time allocation across various periods, enabling
 * users to track productivity, analyze time usage patterns, and monitor goal progress.
 * It aggregates time data from completed events to provide meaningful insights into
 * time management and activity patterns.</p>
 * 
 * <h2>Core Metrics</h2>
 * <ul>
 *   <li><strong>Current Period Tracking</strong>: Today, this week, this month statistics</li>
 *   <li><strong>Historical Analysis</strong>: Last week, last month comparisons</li>
 *   <li><strong>Lifetime Tracking</strong>: Total time across all recorded activities</li>
 *   <li><strong>Productivity Insights</strong>: Pattern recognition and trend analysis</li>
 * </ul>
 * 
 * <h2>Time Period Definitions</h2>
 * <p>All time periods are calculated in the user's local timezone:</p>
 * <ul>
 *   <li><strong>Today</strong>: Current calendar day (midnight to midnight)</li>
 *   <li><strong>This Week</strong>: Monday to Sunday of the current week</li>
 *   <li><strong>This Month</strong>: First day to last day of the current month</li>
 *   <li><strong>Last Week</strong>: Monday to Sunday of the previous week</li>
 *   <li><strong>Last Month</strong>: First day to last day of the previous month</li>
 *   <li><strong>All Time</strong>: Total time since user account creation</li>
 * </ul>
 * 
 * <h2>Data Sources</h2>
 * <p>Time statistics are derived from:</p>
 * <ul>
 *   <li><strong>Completed Events</strong>: Only confirmed/completed events contribute to stats</li>
 *   <li><strong>Label Time Buckets</strong>: Aggregated time allocations by label</li>
 *   <li><strong>Event Durations</strong>: Actual event durations based on start/end times</li>
 *   <li><strong>Time Zone Adjustments</strong>: Proper timezone handling for accurate calculations</li>
 * </ul>
 * 
 * <h2>Calculation Rules</h2>
 * <ul>
 *   <li><strong>Completion Required</strong>: Only completed events are included in statistics</li>
 *   <li><strong>Duration-Based</strong>: Time calculated from event start and end times</li>
 *   <li><strong>Timezone Aware</strong>: All calculations performed in user's timezone</li>
 *   <li><strong>Minute Precision</strong>: Statistics rounded to nearest minute</li>
 * </ul>
 * 
 * <h2>Analytics Applications</h2>
 * <p>These statistics enable various analytical insights:</p>
 * <ul>
 *   <li><strong>Productivity Tracking</strong>: Monitor daily and weekly productivity patterns</li>
 *   <li><strong>Goal Setting</strong>: Set and track time-based goals</li>
 *   <li><strong>Trend Analysis</strong>: Identify productivity trends over time</li>
 *   <li><strong>Comparative Analysis</strong>: Compare current vs. historical performance</li>
 * </ul>
 * 
 * <h2>Data Integrity</h2>
 * <ul>
 *   <li><strong>Consistent Calculation</strong>: All periods calculated using same methodology</li>
 *   <li><strong>Real-time Updates</strong>: Statistics updated when events are completed</li>
 *   <li><strong>Accurate Aggregation</strong>: Proper handling of overlapping periods</li>
 *   <li><strong>Timezone Consistency</strong>: All time calculations respect user timezone</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Efficient Aggregation</strong>: Database-level aggregation for performance</li>
 *   <li><strong>Caching Strategy</strong>: Statistics cached to reduce computation overhead</li>
 *   <li><strong>Incremental Updates</strong>: Statistics updated incrementally when possible</li>
 *   <li><strong>Query Optimization</strong>: Optimized queries for large datasets</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This DTO integrates with multiple system components:</p>
 * <ul>
 *   <li><strong>LabelTimeBucketService</strong>: Source of time allocation data</li>
 *   <li><strong>EventService</strong>: Event completion triggers statistics updates</li>
 *   <li><strong>Analytics Controllers</strong>: API endpoints for statistics retrieval</li>
 *   <li><strong>Dashboard Components</strong>: UI components for statistics display</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * <ul>
 *   <li><strong>Dashboard Display</strong>: Primary statistics for user dashboards</li>
 *   <li><strong>Progress Tracking</strong>: Monitor progress toward time-based goals</li>
 *   <li><strong>Productivity Analysis</strong>: Analyze productivity patterns and trends</li>
 *   <li><strong>Reporting</strong>: Generate time usage reports and summaries</li>
 * </ul>
 * 
 * <h2>Data Types and Ranges</h2>
 * <ul>
 *   <li><strong>Short-term Metrics</strong>: {@code int} for daily/weekly/monthly statistics</li>
 *   <li><strong>Long-term Metrics</strong>: {@code long} for all-time totals to handle large values</li>
 *   <li><strong>Minute Resolution</strong>: All values represent minutes of activity</li>
 *   <li><strong>Non-negative Values</strong>: All statistics are non-negative integers</li>
 * </ul>
 * 
 * <h2>Validation and Constraints</h2>
 * <ul>
 *   <li><strong>Non-negative Values</strong>: All time statistics must be non-negative</li>
 *   <li><strong>Logical Consistency</strong>: Current period values should be reasonable</li>
 *   <li><strong>Range Validation</strong>: Daily minutes shouldn't exceed 24 hours (1440 minutes)</li>
 *   <li><strong>Temporal Logic</strong>: Historical values should be consistent with current values</li>
 * </ul>
 * 
 * <h2>Important Notes</h2>
 * <ul>
 *   <li><strong>Completion Dependency</strong>: Statistics only include completed events</li>
 *   <li><strong>Real-time Nature</strong>: Statistics reflect current state, not historical snapshots</li>
 *   <li><strong>Timezone Critical</strong>: Proper timezone handling essential for accuracy</li>
 *   <li><strong>Performance Impact</strong>: Statistics calculation may be computationally intensive</li>
 * </ul>
 * 
 * @param today total minutes of completed activities today
 * @param thisWeek total minutes of completed activities this week
 * @param thisMonth total minutes of completed activities this month
 * @param lastWeek total minutes of completed activities last week
 * @param lastMonth total minutes of completed activities last month
 * @param allTime total minutes of all completed activities since account creation
 * 
 * @see com.yohan.event_planner.service.LabelTimeBucketService
 * @see com.yohan.event_planner.domain.LabelTimeBucket
 * @see com.yohan.event_planner.domain.Event
 * @see com.yohan.event_planner.controller.UserToolsController
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
public record TimeStatsDTO(
        int today,
        int thisWeek,
        int thisMonth,
        int lastWeek,
        int lastMonth,
        long allTime
) {}
