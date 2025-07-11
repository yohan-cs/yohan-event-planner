package com.yohan.event_planner.dto;

import com.yohan.event_planner.domain.enums.TimeFilter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * Data transfer object for filtering recurring events with comprehensive query criteria and temporal controls.
 * 
 * <p>This DTO provides sophisticated filtering capabilities specifically designed for recurring event queries,
 * supporting label-based categorization, temporal filtering using date ranges, and flexible sorting options.
 * It enables complex recurring event discovery and display within the event planning system while maintaining
 * performance and usability for recurring event management workflows.</p>
 * 
 * <h2>Core Filtering Capabilities</h2>
 * <ul>
 *   <li><strong>Label-based Filtering</strong>: Filter recurring events by specific categorization labels</li>
 *   <li><strong>Date Range Filtering</strong>: Temporal filtering using LocalDate boundaries</li>
 *   <li><strong>Time Filter Strategies</strong>: Flexible temporal filtering with multiple strategies</li>
 *   <li><strong>Sorting Control</strong>: Configurable sort order for result presentation</li>
 * </ul>
 * 
 * <h2>Time Filter Strategies</h2>
 * <p>The {@link TimeFilter} enumeration provides flexible temporal filtering for recurring events:</p>
 * 
 * <h3>ALL</h3>
 * <ul>
 *   <li><strong>Behavior</strong>: No time filtering applied to recurring events</li>
 *   <li><strong>Parameters</strong>: {@code startDate} and {@code endDate} are ignored</li>
 *   <li><strong>Use Case</strong>: Retrieve all recurring events regardless of timing</li>
 * </ul>
 * 
 * <h3>PAST_ONLY</h3>
 * <ul>
 *   <li><strong>Behavior</strong>: Filter for recurring events that have started in the past</li>
 *   <li><strong>Parameters</strong>: {@code endDate} defaults to current date, {@code startDate} ignored</li>
 *   <li><strong>Use Case</strong>: Historical recurring event analysis</li>
 * </ul>
 * 
 * <h3>FUTURE_ONLY</h3>
 * <ul>
 *   <li><strong>Behavior</strong>: Filter for upcoming or active recurring events</li>
 *   <li><strong>Parameters</strong>: {@code startDate} defaults to current date, {@code endDate} ignored</li>
 *   <li><strong>Use Case</strong>: Planning view and upcoming recurring activity management</li>
 * </ul>
 * 
 * <h3>CUSTOM</h3>
 * <ul>
 *   <li><strong>Behavior</strong>: Apply exact {@code startDate} and {@code endDate} boundaries</li>
 *   <li><strong>Parameters</strong>: Both {@code startDate} and {@code endDate} honored when provided</li>
 *   <li><strong>Defaults</strong>: {@code FAR_PAST} and {@code FAR_FUTURE} when null</li>
 *   <li><strong>Use Case</strong>: Precise date range queries for recurring events</li>
 * </ul>
 * 
 * <h2>Date Range Handling</h2>
 * <p>Recurring event filtering uses {@link LocalDate} for date boundaries:</p>
 * <ul>
 *   <li><strong>Timezone Independence</strong>: LocalDate avoids timezone complexities</li>
 *   <li><strong>Recurring Patterns</strong>: Aligns with recurring event scheduling logic</li>
 *   <li><strong>Date Precision</strong>: Day-level precision for recurring event filtering</li>
 *   <li><strong>Pattern Matching</strong>: Supports filtering based on recurring patterns</li>
 * </ul>
 * 
 * <h2>Label Integration</h2>
 * <ul>
 *   <li><strong>Optional Filtering</strong>: {@code labelId} is optional for broader queries</li>
 *   <li><strong>Ownership Validation</strong>: Label access validated at service layer</li>
 *   <li><strong>Category Filtering</strong>: Filter recurring events by category/label</li>
 *   <li><strong>Performance Optimization</strong>: Label-based queries use database indexes</li>
 * </ul>
 * 
 * <h2>Recurring Event Specifics</h2>
 * <p>This filter is optimized for recurring event characteristics:</p>
 * <ul>
 *   <li><strong>Pattern-based Filtering</strong>: Filter based on recurring event patterns</li>
 *   <li><strong>Instance Generation</strong>: Consider recurring instances within date ranges</li>
 *   <li><strong>Rule Processing</strong>: Handle RRULE-based recurring patterns</li>
 *   <li><strong>Exception Handling</strong>: Account for recurring event exceptions and modifications</li>
 * </ul>
 * 
 * <h2>Sorting and Presentation</h2>
 * <ul>
 *   <li><strong>Flexible Ordering</strong>: {@code sortDescending} controls temporal sorting</li>
 *   <li><strong>Creation Time Sorting</strong>: Sort by recurring event creation time</li>
 *   <li><strong>Start Date Sorting</strong>: Sort by recurring event start date</li>
 *   <li><strong>UI Integration</strong>: Enable different presentation modes for recurring events</li>
 * </ul>
 * 
 * <h2>Validation and Constraints</h2>
 * <ul>
 *   <li><strong>Required Fields</strong>: {@code timeFilter} is mandatory for proper filtering</li>
 *   <li><strong>Positive IDs</strong>: {@code labelId} must be positive when provided</li>
 *   <li><strong>Date Logic</strong>: Custom date ranges validated at service layer</li>
 *   <li><strong>Null Handling</strong>: Proper defaults for optional parameters</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This DTO integrates with multiple system components:</p>
 * <ul>
 *   <li><strong>RecurringEventService</strong>: Primary filtering for recurring event queries</li>
 *   <li><strong>RecurringEventDAO</strong>: Advanced query execution for recurring events</li>
 *   <li><strong>REST Controllers</strong>: API parameter binding and validation</li>
 *   <li><strong>Calendar Services</strong>: Integration with calendar view generation</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Index Optimization</strong>: Leverages database indexes for efficient filtering</li>
 *   <li><strong>Query Optimization</strong>: Optimized queries for recurring event patterns</li>
 *   <li><strong>Date Range Optimization</strong>: Efficient date range queries</li>
 *   <li><strong>Pattern Processing</strong>: Optimized recurring pattern evaluation</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * <ul>
 *   <li><strong>Recurring Event Dashboard</strong>: Primary data for recurring event displays</li>
 *   <li><strong>Calendar Views</strong>: Filter recurring events for calendar displays</li>
 *   <li><strong>Schedule Management</strong>: Manage recurring event schedules</li>
 *   <li><strong>Pattern Analysis</strong>: Analyze recurring event patterns and usage</li>
 * </ul>
 * 
 * <h2>Recurring Event Considerations</h2>
 * <ul>
 *   <li><strong>Pattern Complexity</strong>: Handle complex recurring patterns efficiently</li>
 *   <li><strong>Instance Generation</strong>: Consider generated instances in filtering</li>
 *   <li><strong>Exception Handling</strong>: Account for recurring event exceptions</li>
 *   <li><strong>Rule Processing</strong>: Proper handling of RRULE specifications</li>
 * </ul>
 * 
 * <h2>Important Notes</h2>
 * <ul>
 *   <li><strong>Recurring Focus</strong>: Specifically designed for recurring event queries</li>
 *   <li><strong>Pattern Aware</strong>: Considers recurring patterns in filtering logic</li>
 *   <li><strong>Performance Critical</strong>: Recurring event queries can be computationally intensive</li>
 *   <li><strong>Date Precision</strong>: Uses LocalDate for day-level precision</li>
 * </ul>
 * 
 * @param labelId optional label ID to filter recurring events by specific category
 * @param timeFilter required time filtering strategy for temporal bounds
 * @param startDate optional start date for custom time filtering
 * @param endDate optional end date for custom time filtering
 * @param sortDescending optional flag to control sorting order (true for descending)
 * 
 * @see TimeFilter
 * @see com.yohan.event_planner.service.RecurringEventService
 * @see com.yohan.event_planner.domain.RecurringEvent
 * @see com.yohan.event_planner.domain.RecurrenceRuleVO
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
public record RecurringEventFilterDTO(

        @Positive
        Long labelId,

        @NotNull
        TimeFilter timeFilter,

        LocalDate startDate,

        LocalDate endDate,

        Boolean sortDescending

) {}