package com.yohan.event_planner.dto;

import java.time.ZonedDateTime;

/**
 * Data transfer object for comprehensive event display with timezone-aware time handling and virtual event support.
 * 
 * <p>This DTO provides a complete representation of events for API responses, including both real scheduled
 * events and virtual events generated from recurring patterns. It handles complex timezone scenarios,
 * completion states, and draft status while maintaining compatibility with various UI components
 * and calendar displays throughout the event planning system.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>Complete Event Data</strong>: Full event information for display and management</li>
 *   <li><strong>Timezone Handling</strong>: UTC storage with original timezone preservation</li>
 *   <li><strong>Virtual Event Support</strong>: Supports both real and generated events</li>
 *   <li><strong>State Management</strong>: Handles completion and confirmation states</li>
 * </ul>
 * 
 * <h2>Time Representation</h2>
 * <p>All time values follow a sophisticated timezone handling strategy:</p>
 * 
 * <h3>UTC Storage</h3>
 * <ul>
 *   <li><strong>Consistent Storage</strong>: All times stored and returned in UTC</li>
 *   <li><strong>Database Consistency</strong>: Ensures consistent time handling across the system</li>
 *   <li><strong>Calculation Reliability</strong>: Enables reliable duration and overlap calculations</li>
 *   <li><strong>Global Compatibility</strong>: Compatible with users across different timezones</li>
 * </ul>
 * 
 * <h3>Timezone Preservation</h3>
 * <ul>
 *   <li><strong>Original Timezone</strong>: Preserves original timezone information</li>
 *   <li><strong>Conditional Inclusion</strong>: Timezone included only when different from creator's timezone</li>
 *   <li><strong>Reconstruction Support</strong>: Enables clients to reconstruct original local times</li>
 *   <li><strong>Display Flexibility</strong>: Supports various timezone display preferences</li>
 * </ul>
 * 
 * <h2>Event Types</h2>
 * <p>The DTO supports various event types and states:</p>
 * 
 * <h3>Real Events</h3>
 * <ul>
 *   <li><strong>Confirmed Events</strong>: Events with all required fields (name, startTime, endTime) provided</li>
 *   <li><strong>Unconfirmed Events (Drafts)</strong>: Events that can have any or all fields missing, cannot be confirmed until all required fields are provided</li>
 *   <li><strong>Impromptu Events</strong>: A specific type of unconfirmed event that starts immediately but lacks an end time</li>
 *   <li><strong>Completed Events</strong>: Confirmed events marked as completed</li>
 * </ul>
 * 
 * <h3>Virtual Events</h3>
 * <ul>
 *   <li><strong>Recurring Instances</strong>: Generated instances from recurring event patterns</li>
 *   <li><strong>Template Events</strong>: Virtual events for preview and planning</li>
 *   <li><strong>Pattern Visualization</strong>: Show recurring patterns without actual persistence</li>
 *   <li><strong>Calendar Generation</strong>: Support calendar views with virtual events</li>
 * </ul>
 * 
 * <h2>Duration Handling</h2>
 * <p>Duration information supports various event scenarios:</p>
 * <ul>
 *   <li><strong>Calculated Duration</strong>: Automatic calculation from start and end times</li>
 *   <li><strong>Minute Precision</strong>: Duration tracked in minutes for accuracy</li>
 *   <li><strong>Draft Support</strong>: Null duration for unconfirmed events missing start or end times</li>
 *   <li><strong>Time Tracking</strong>: Integration with time tracking and analytics</li>
 * </ul>
 * 
 * <h2>Label Integration</h2>
 * <ul>
 *   <li><strong>Category Association</strong>: Events associated with user-defined labels</li>
 *   <li><strong>Complete Label Data</strong>: Full label information via LabelResponseDTO</li>
 *   <li><strong>Organization Support</strong>: Enable event organization and categorization</li>
 *   <li><strong>Analytics Integration</strong>: Support label-based analytics and reporting</li>
 * </ul>
 * 
 * <h2>State Management</h2>
 * <p>The DTO manages various event states:</p>
 * 
 * <h3>Completion State</h3>
 * <ul>
 *   <li><strong>Completion Tracking</strong>: Track whether events have been completed</li>
 *   <li><strong>Time Tracking Integration</strong>: Completed events contribute to time statistics</li>
 *   <li><strong>Progress Monitoring</strong>: Support productivity and goal tracking</li>
 *   <li><strong>Analytics Impact</strong>: Completion affects various analytics calculations</li>
 * </ul>
 * 
 * <h3>Confirmation State</h3>
 * <ul>
 *   <li><strong>Draft Management</strong>: Unconfirmed events can have any or all fields missing</li>
 *   <li><strong>Validation Requirements</strong>: Events cannot be confirmed until all required fields are provided</li>
 *   <li><strong>Planning Support</strong>: Support iterative event planning workflows</li>
 *   <li><strong>Review Process</strong>: Enable event review and confirmation processes</li>
 *   <li><strong>UI Differentiation</strong>: Enable different UI treatment for unconfirmed events</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Efficient Serialization</strong>: Optimized JSON serialization for API responses</li>
 *   <li><strong>Minimal Data Transfer</strong>: Conditional timezone fields reduce payload size</li>
 *   <li><strong>Caching Friendly</strong>: Structure supports effective caching strategies</li>
 *   <li><strong>Database Optimization</strong>: Aligns with database structure for efficient queries</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This DTO integrates with multiple system components:</p>
 * <ul>
 *   <li><strong>Event Controllers</strong>: Primary response DTO for event API endpoints</li>
 *   <li><strong>Calendar Services</strong>: Used in calendar view generation</li>
 *   <li><strong>EventResponseDTOFactory</strong>: Factory handles creation from various sources</li>
 *   <li><strong>UI Components</strong>: Direct consumption by event display components</li>
 * </ul>
 * 
 * <h2>Virtual Event Support</h2>
 * <p>Virtual events enable advanced calendar features:</p>
 * <ul>
 *   <li><strong>Recurring Visualization</strong>: Show recurring event instances without persistence</li>
 *   <li><strong>Pattern Preview</strong>: Preview recurring patterns before confirmation</li>
 *   <li><strong>Calendar Population</strong>: Populate calendars with generated instances</li>
 *   <li><strong>Planning Support</strong>: Support planning workflows with virtual events</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * <ul>
 *   <li><strong>API Responses</strong>: Primary structure for event API responses</li>
 *   <li><strong>Calendar Display</strong>: Power calendar views and event lists</li>
 *   <li><strong>Event Management</strong>: Support event editing and management interfaces</li>
 *   <li><strong>Analytics Display</strong>: Provide data for event analytics and reporting</li>
 * </ul>
 * 
 * <h2>Validation and Constraints</h2>
 * <ul>
 *   <li><strong>Confirmed Events</strong>: Must have all required fields (name, startTime, endTime)</li>
 *   <li><strong>Unconfirmed Events</strong>: Can have any combination of fields missing</li>
 *   <li><strong>Time Consistency</strong>: End time should be after start time when both are provided</li>
 *   <li><strong>Duration Logic</strong>: Duration should match calculated time difference when both times are provided</li>
 *   <li><strong>Timezone Validity</strong>: Timezone IDs should be valid when provided</li>
 *   <li><strong>Creator Consistency</strong>: Creator username should be valid and accessible</li>
 * </ul>
 * 
 * <h2>Important Notes</h2>
 * <ul>
 *   <li><strong>UTC Consistency</strong>: All times consistently in UTC for reliability</li>
 *   <li><strong>Timezone Preservation</strong>: Original timezone information preserved for display</li>
 *   <li><strong>Virtual Support</strong>: Seamlessly handles both real and virtual events</li>
 *   <li><strong>State Aware</strong>: Comprehensive state management for various event scenarios</li>
 * </ul>
 * 
 * @see LabelResponseDTO
 * @see com.yohan.event_planner.dto.EventResponseDTOFactory
 * @see com.yohan.event_planner.domain.Event
 * @see com.yohan.event_planner.domain.RecurringEvent
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
public record EventResponseDTO(

        /** Unique identifier of the event. */
        Long id,

        /** Name or title of the event. May be {@code null} for unconfirmed events (drafts). */
        String name,

        /** Event start time in UTC. May be {@code null} for unconfirmed events (drafts). */
        ZonedDateTime startTimeUtc,

        /** Event end time in UTC. May be {@code null} for unconfirmed events (drafts). Required for confirmed events. */
        ZonedDateTime endTimeUtc,

        /** Duration of the event in whole minutes. {@code null} if start or end time is not provided (unconfirmed events). */
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

        /** Username of the user who created the event. */
        String creatorUsername,

        /** Timezone of the user who created the event. */
        String creatorTimezone,

        LabelResponseDTO label,

        boolean isCompleted,

        boolean unconfirmed,

        boolean impromptu,

        boolean isVirtual
) {}
