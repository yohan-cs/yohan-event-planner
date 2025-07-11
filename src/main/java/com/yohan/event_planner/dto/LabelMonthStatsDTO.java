package com.yohan.event_planner.dto;

/**
 * Data transfer object for monthly statistics and analytics for a specific label category.
 * 
 * <p>This DTO provides comprehensive monthly analytics for a specific label, including
 * event count metrics and time tracking statistics. It enables monthly productivity analysis,
 * trend monitoring, and label-based performance tracking within the event planning system,
 * supporting both analytical dashboards and monthly review workflows.</p>
 * 
 * <h2>Core Metrics</h2>
 * <ul>
 *   <li><strong>Event Count Metrics</strong>: Total number of completed events for the label</li>
 *   <li><strong>Time Tracking Metrics</strong>: Total time invested in label activities</li>
 *   <li><strong>Monthly Scope</strong>: All metrics scoped to a specific calendar month</li>
 *   <li><strong>Label-Specific Analysis</strong>: Analytics focused on a single label category</li>
 * </ul>
 * 
 * <h2>Time Tracking</h2>
 * <p>Time statistics provide detailed productivity insights:</p>
 * <ul>
 *   <li><strong>Minute Precision</strong>: Time tracked in minutes for precise analysis</li>
 *   <li><strong>Completed Events Only</strong>: Only completed events contribute to time statistics</li>
 *   <li><strong>Duration-Based</strong>: Time calculated from actual event durations</li>
 *   <li><strong>Monthly Aggregation</strong>: All time values aggregated for the month</li>
 * </ul>
 * 
 * <h2>Event Counting</h2>
 * <p>Event metrics provide activity analysis:</p>
 * <ul>
 *   <li><strong>Completion Required</strong>: Only completed events included in counts</li>
 *   <li><strong>Label Association</strong>: Events must be associated with the specific label</li>
 *   <li><strong>Month Boundaries</strong>: Events counted within the specified month</li>
 *   <li><strong>Activity Volume</strong>: Measure of activity volume for the label</li>
 * </ul>
 * 
 * <h2>Use Cases</h2>
 * <p>This DTO supports various analytical use cases:</p>
 * <ul>
 *   <li><strong>Monthly Review</strong>: Review monthly productivity by label category</li>
 *   <li><strong>Goal Tracking</strong>: Track progress toward label-specific goals</li>\n *   <li><strong>Time Analysis</strong>: Analyze time allocation across different labels</li>
 *   <li><strong>Productivity Metrics</strong>: Measure productivity and efficiency by category</li>
 * </ul>
 * 
 * <h2>Analytics Applications</h2>\n * <ul>\n *   <li><strong>Trend Analysis</strong>: Compare monthly statistics across time periods</li>\n *   <li><strong>Category Performance</strong>: Evaluate performance by event category</li>\n *   <li><strong>Time Allocation</strong>: Understand time distribution across labels</li>\n *   <li><strong>Productivity Planning</strong>: Plan future activities based on historical data</li>\n * </ul>
 * 
 * <h2>Data Sources</h2>
 * <p>Statistics are derived from multiple data sources:</p>
 * <ul>
 *   <li><strong>Event Records</strong>: Completed events with label associations</li>
 *   <li><strong>Time Buckets</strong>: Label time bucket aggregations</li>
 *   <li><strong>Duration Calculations</strong>: Event duration calculations and summations</li>
 *   <li><strong>Month Filtering</strong>: Temporal filtering for monthly scope</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Aggregated Data</strong>: Pre-aggregated statistics for performance</li>
 *   <li><strong>Efficient Queries</strong>: Optimized database queries for monthly data</li>
 *   <li><strong>Caching Strategy</strong>: Monthly statistics cached for frequent access</li>
 *   <li><strong>Incremental Updates</strong>: Statistics updated incrementally when possible</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This DTO integrates with multiple system components:</p>
 * <ul>
 *   <li><strong>MonthlyCalendarService</strong>: Primary source for monthly analytics</li>
 *   <li><strong>LabelTimeBucketService</strong>: Source of time tracking statistics</li>
 *   <li><strong>EventService</strong>: Source of event count metrics</li>
 *   <li><strong>Analytics Controllers</strong>: API endpoints for monthly analytics</li>
 * </ul>
 * 
 * <h2>Data Consistency</h2>
 * <ul>
 *   <li><strong>Label Consistency</strong>: All metrics relate to the specified label</li>
 *   <li><strong>Month Alignment</strong>: All data aligned to the same calendar month</li>
 *   <li><strong>Completion Filter</strong>: Consistent filtering for completed events only</li>
 *   <li><strong>Real-time Updates</strong>: Statistics reflect current state of completed events</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * <ul>
 *   <li><strong>Monthly Dashboard</strong>: Primary data for monthly analytics dashboards</li>
 *   <li><strong>Calendar Integration</strong>: Integrated with monthly calendar displays</li>
 *   <li><strong>Reporting</strong>: Generate monthly reports and summaries</li>
 *   <li><strong>Goal Tracking</strong>: Monitor progress toward monthly goals</li>
 * </ul>
 * 
 * <h2>Validation and Constraints</h2>
 * <ul>
 *   <li><strong>Non-negative Values</strong>: Event counts and time values must be non-negative</li>
 *   <li><strong>Valid Label Name</strong>: Label name must be valid and non-empty</li>
 *   <li><strong>Logical Consistency</strong>: Time and event metrics should be logically consistent</li>
 *   <li><strong>Month Scope</strong>: All metrics must be scoped to the specified month</li>
 * </ul>
 * 
 * <h2>Important Notes</h2>
 * <ul>
 *   <li><strong>Completion Dependent</strong>: All statistics based on completed events only</li>
 *   <li><strong>Label Specific</strong>: Statistics focused on a single label category</li>
 *   <li><strong>Monthly Scope</strong>: All data scoped to a specific calendar month</li>
 *   <li><strong>Productivity Focus</strong>: Designed for productivity and performance analysis</li>
 * </ul>
 * 
 * @param labelName the name of the label for which statistics are provided
 * @param totalEvents total number of completed events for the label in the month
 * @param totalTimeSpent total time spent on events with this label in minutes
 * 
 * @see com.yohan.event_planner.service.MonthlyCalendarService
 * @see com.yohan.event_planner.service.LabelTimeBucketService
 * @see com.yohan.event_planner.domain.Label
 * @see com.yohan.event_planner.domain.LabelTimeBucket
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
public record LabelMonthStatsDTO(
        String labelName,       // The name of the label
        long totalEvents,       // Total number of completed events for the label
        long totalTimeSpent     // Total time spent on events (in minutes)

) {}
