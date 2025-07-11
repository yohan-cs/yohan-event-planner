package com.yohan.event_planner.dto;

import java.util.List;

/**
 * Data transfer object for monthly calendar view responses with event dates and label statistics.
 * 
 * <p>This DTO provides a comprehensive monthly calendar response that combines event occurrence
 * data with label-based statistics for the specified month. It enables efficient monthly calendar
 * displays with integrated analytics, supporting both calendar visualization and productivity
 * tracking within a single response structure.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>Monthly Event Overview</strong>: List of dates containing events in the month</li>
 *   <li><strong>Label Statistics</strong>: Monthly statistics for specific label categories</li>
 *   <li><strong>Calendar Integration</strong>: Data optimized for monthly calendar displays</li>
 *   <li><strong>Analytics Support</strong>: Integrated productivity and time tracking statistics</li>
 * </ul>
 * 
 * <h2>Event Date Representation</h2>
 * <p>Event dates are provided as ISO 8601 date strings:</p>
 * <ul>
 *   <li><strong>ISO 8601 Format</strong>: Dates in "YYYY-MM-DD" format (e.g., "2025-07-05")</li>
 *   <li><strong>Date-only Precision</strong>: Day-level precision for calendar display</li>
 *   <li><strong>Timezone Independent</strong>: Date strings avoid timezone complexities</li>
 *   <li><strong>UI Compatibility</strong>: Format compatible with calendar UI components</li>
 * </ul>
 * 
 * <h2>Use Cases</h2>
 * <p>This DTO supports various monthly calendar and analytics use cases:</p>
 * <ul>
 *   <li><strong>Monthly Calendar View</strong>: Display monthly calendar with event indicators</li>
 *   <li><strong>Event Overview</strong>: Quick overview of event distribution across the month</li>
 *   <li><strong>Productivity Dashboard</strong>: Monthly productivity tracking and statistics</li>
 *   <li><strong>Label Analytics</strong>: Analyze monthly activity for specific labels</li>
 * </ul>
 * 
 * <h2>Calendar Display Integration</h2>
 * <ul>
 *   <li><strong>Event Indicators</strong>: Event dates used to display calendar indicators</li>
 *   <li><strong>Quick Navigation</strong>: Enable navigation to days with events</li>
 *   <li><strong>Visual Density</strong>: Show event density across the month</li>
 *   <li><strong>Interactive Elements</strong>: Support interactive calendar features</li>
 * </ul>
 * 
 * <h2>Label Statistics Integration</h2>
 * <p>Monthly statistics provide analytical insights:</p>
 * <ul>
 *   <li><strong>Time Tracking</strong>: Total time spent on label activities</li>
 *   <li><strong>Event Counting</strong>: Number of completed events for the label</li>
 *   <li><strong>Productivity Metrics</strong>: Monthly productivity analysis for the label</li>
 *   <li><strong>Trend Analysis</strong>: Support for monthly trend analysis</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Efficient Queries</strong>: Optimized database queries for monthly data</li>
 *   <li><strong>Minimal Data Transfer</strong>: Compact representation for fast transfer</li>
 *   <li><strong>Caching Strategy</strong>: Monthly data cached for frequent access</li>
 *   <li><strong>Aggregated Statistics</strong>: Pre-aggregated statistics for performance</li>
 * </ul>
 * 
 * <h2>Data Filtering</h2>
 * <p>Data included in the response is filtered based on:</p>
 * <ul>
 *   <li><strong>Month Boundaries</strong>: Only events within the specified month</li>
 *   <li><strong>User Permissions</strong>: Only events the user can access</li>
 *   <li><strong>Label Context</strong>: Statistics filtered by label when specified</li>
 *   <li><strong>Completion Status</strong>: Statistics based on completed events only</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This DTO integrates with multiple system components:</p>
 * <ul>
 *   <li><strong>CalendarController</strong>: Primary source for monthly calendar API responses</li>
 *   <li><strong>MonthlyCalendarService</strong>: Service layer for monthly data aggregation</li>
 *   <li><strong>LabelTimeBucketService</strong>: Source of label-based time statistics</li>
 *   <li><strong>Calendar UI Components</strong>: UI components for monthly calendar displays</li>
 * </ul>
 * 
 * <h2>Data Consistency</h2>
 * <ul>
 *   <li><strong>Month Alignment</strong>: Event dates and statistics aligned to same month</li>
 *   <li><strong>Label Consistency</strong>: Statistics match the label filter applied</li>
 *   <li><strong>Timezone Handling</strong>: Consistent timezone handling for dates and statistics</li>
 *   <li><strong>Real-time Data</strong>: Statistics reflect current state of completed events</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * <ul>
 *   <li><strong>Monthly Calendar</strong>: Primary data structure for monthly calendar views</li>
 *   <li><strong>Dashboard Display</strong>: Enable monthly productivity dashboard displays</li>
 *   <li><strong>Analytics View</strong>: Support monthly analytics and reporting</li>
 *   <li><strong>Planning Interface</strong>: Enable monthly planning and review workflows</li>
 * </ul>
 * 
 * <h2>Date String Format</h2>
 * <p>Event dates follow strict formatting conventions:</p>
 * <ul>
 *   <li><strong>ISO 8601 Compliance</strong>: Full compliance with ISO 8601 date format</li>
 *   <li><strong>Zero Padding</strong>: Months and days zero-padded (e.g., "2025-07-05")</li>
 *   <li><strong>4-digit Years</strong>: Full 4-digit year representation</li>
 *   <li><strong>Consistent Format</strong>: Same format used throughout the system</li>
 * </ul>
 * 
 * <h2>Validation and Constraints</h2>
 * <ul>
 *   <li><strong>Valid Dates</strong>: All event dates must be valid and within the month</li>
 *   <li><strong>Non-null Collections</strong>: Event dates list should never be null</li>
 *   <li><strong>Month Consistency</strong>: All dates should be within the same month</li>
 *   <li><strong>Statistics Validation</strong>: Label statistics must be consistent and valid</li>
 * </ul>
 * 
 * <h2>Important Notes</h2>
 * <ul>
 *   <li><strong>Compact Format</strong>: Optimized for minimal data transfer</li>
 *   <li><strong>Calendar Focused</strong>: Specifically designed for calendar display needs</li>
 *   <li><strong>Analytics Integration</strong>: Combines calendar and analytics data efficiently</li>
 *   <li><strong>Month Scoped</strong>: All data scoped to a specific calendar month</li>
 * </ul>
 * 
 * @param eventDates list of ISO 8601 date strings representing dates with events (e.g., ["2025-07-05", "2025-07-12"])
 * @param bucketMonthStats monthly statistics for the selected label, providing time and event analytics
 * 
 * @see LabelMonthStatsDTO
 * @see com.yohan.event_planner.controller.CalendarController
 * @see com.yohan.event_planner.service.MonthlyCalendarService
 * @see com.yohan.event_planner.service.LabelTimeBucketService
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
public record MonthlyCalendarResponseDTO(
        List<String> eventDates,  // List of event dates (e.g., ["2025-07-05", "2025-07-12"])
        LabelMonthStatsDTO bucketMonthStats  // Monthly stats for the selected label
) {}
