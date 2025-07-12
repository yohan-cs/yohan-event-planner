package com.yohan.event_planner.dto;

import com.yohan.event_planner.domain.enums.TimeFilter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.ZonedDateTime;

/**
 * Data transfer object for filtering events with comprehensive query criteria and privacy controls.
 * 
 * <p>This DTO provides sophisticated filtering capabilities for event queries, supporting both
 * self-service and public event searches with privacy-aware field handling. It enables complex
 * temporal filtering, label-based categorization, and completion state management essential
 * for flexible event discovery and display within the event planning system.</p>
 * 
 * <h2>Core Filtering Capabilities</h2>
 * <ul>
 *   <li><strong>Label-based Filtering</strong>: Filter events by specific categorization labels</li>
 *   <li><strong>Temporal Filtering</strong>: Advanced time-based filtering with multiple strategies</li>
 *   <li><strong>Completion State Filtering</strong>: Include or exclude incomplete past events</li>
 *   <li><strong>Sorting Control</strong>: Configurable sort order for result presentation</li>
 * </ul>
 * 
 * <h2>Time Filter Strategies</h2>
 * <p>The {@link TimeFilter} enumeration provides flexible temporal filtering:</p>
 * 
 * <h3>ALL</h3>
 * <ul>
 *   <li><strong>Behavior</strong>: No time filtering applied</li>
 *   <li><strong>Parameters</strong>: {@code start} and {@code end} are ignored</li>
 *   <li><strong>Use Case</strong>: Retrieve all events regardless of timing</li>
 * </ul>
 * 
 * <h3>PAST_ONLY</h3>
 * <ul>
 *   <li><strong>Behavior</strong>: Filter for events that have already occurred</li>
 *   <li><strong>Parameters</strong>: {@code end} defaults to current time, {@code start} ignored</li>
 *   <li><strong>Use Case</strong>: Historical event analysis and completed activity review</li>
 * </ul>
 * 
 * <h3>FUTURE_ONLY</h3>
 * <ul>
 *   <li><strong>Behavior</strong>: Filter for upcoming events</li>
 *   <li><strong>Parameters</strong>: {@code start} defaults to current time, {@code end} ignored</li>
 *   <li><strong>Use Case</strong>: Planning view and upcoming activity management</li>
 * </ul>
 * 
 * <h3>CUSTOM</h3>
 * <ul>
 *   <li><strong>Behavior</strong>: Apply exact {@code start} and {@code end} timestamps</li>
 *   <li><strong>Parameters</strong>: Both {@code start} and {@code end} honored when provided</li>
 *   <li><strong>Defaults</strong>: {@code FAR_PAST} and {@code FAR_FUTURE} when null</li>
 *   <li><strong>Use Case</strong>: Precise date range queries and calendar views</li>
 * </ul>
 * 
 * <h2>Privacy and Access Control</h2>
 * <p>The filter supports different behaviors based on query context:</p>
 * <ul>
 *   <li><strong>Self-Service Queries</strong>: Full access to all filter options</li>
 *   <li><strong>Public Queries</strong>: Limited filtering with privacy-safe defaults</li>
 *   <li><strong>Context Determination</strong>: Query context resolved at service layer</li>
 *   <li><strong>Field Sanitization</strong>: Automatic privacy-based field filtering</li>
 * </ul>
 * 
 * <h2>Label Integration</h2>
 * <ul>
 *   <li><strong>Optional Filtering</strong>: {@code labelId} is optional for broader queries</li>
 *   <li><strong>Ownership Validation</strong>: Label access validated at service layer</li>
 *   <li><strong>Privacy Respect</strong>: Public queries may have label restrictions</li>
 *   <li><strong>Performance Optimization</strong>: Label-based queries use database indexes</li>
 * </ul>
 * 
 * <h2>Completion State Management</h2>
 * <p>The {@code includeIncompletePastEvents} field provides completion filtering:</p>
 * <ul>
 *   <li><strong>Private Queries</strong>: Full control over incomplete event inclusion</li>
 *   <li><strong>Public Queries</strong>: Defaults to {@code false} for privacy</li>
 *   <li><strong>Analytics Support</strong>: Enable analysis of incomplete vs completed events</li>
 *   <li><strong>Goal Tracking</strong>: Support for productivity and completion metrics</li>
 * </ul>
 * 
 * <h2>Sorting and Presentation</h2>
 * <ul>
 *   <li><strong>Flexible Ordering</strong>: {@code sortDescending} controls time-based sorting</li>
 *   <li><strong>Chronological Views</strong>: Support both forward and reverse chronological order</li>
 *   <li><strong>UI Integration</strong>: Enable different presentation modes</li>
 *   <li><strong>Performance Optimization</strong>: Sorting leverages database indexes</li>
 * </ul>
 * 
 * <h2>Validation and Constraints</h2>
 * <ul>
 *   <li><strong>Required Fields</strong>: {@code timeFilter} is mandatory for proper filtering</li>
 *   <li><strong>Positive IDs</strong>: {@code labelId} must be positive when provided</li>
 *   <li><strong>Time Validation</strong>: Custom time ranges validated at service layer</li>
 *   <li><strong>Null Handling</strong>: Proper defaults for optional parameters</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This DTO integrates with multiple system components:</p>
 * <ul>
 *   <li><strong>EventService</strong>: Primary filtering for event queries</li>
 *   <li><strong>EventDAO</strong>: Advanced query execution via Blaze-Persistence</li>
 *   <li><strong>REST Controllers</strong>: API parameter binding and validation</li>
 *   <li><strong>Security Framework</strong>: Privacy-aware filtering based on user context</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * <ul>
 *   <li><strong>Dashboard Queries</strong>: Flexible event retrieval for user dashboards</li>
 *   <li><strong>Calendar Views</strong>: Time-based filtering for calendar displays</li>
 *   <li><strong>Analytics Queries</strong>: Completion state analysis and reporting</li>
 *   <li><strong>Search Operations</strong>: Label-based event discovery</li>
 * </ul>
 * 
 * <h2>Important Notes</h2>
 * <ul>
 *   <li><strong>User Context</strong>: Target user determined by route path, not DTO content</li>
 *   <li><strong>Service Resolution</strong>: TimeFilter enum resolved to actual start/end times by service layer</li>
 *   <li><strong>DAO Integration</strong>: Pre-resolved start/end times passed to DAO for pure data access</li>
 *   <li><strong>Performance Impact</strong>: Complex filters may require query optimization</li>
 *   <li><strong>Privacy Enforcement</strong>: Public query restrictions applied automatically</li>
 * </ul>
 * 
 * @see TimeFilter
 * @see com.yohan.event_planner.service.EventService
 * @see com.yohan.event_planner.dao.EventDAO
 * @see com.yohan.event_planner.domain.Event
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */

public record EventFilterDTO(

        @Positive
        Long labelId,

        @NotNull
        TimeFilter timeFilter,

        ZonedDateTime start,

        ZonedDateTime end,

        Boolean sortDescending,

        Boolean includeIncompletePastEvents

) {}
