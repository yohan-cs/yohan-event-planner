package com.yohan.event_planner.dto;

import java.util.List;

/**
 * Data transfer object for comprehensive user event display combining both recurring and individual events.
 * 
 * <p>This DTO provides a unified view of a user's events, aggregating both recurring event templates
 * and individual event instances into a cohesive response structure. It enables comprehensive event
 * management interfaces, dashboard displays, and unified event listing workflows that need to present
 * both event types together for user convenience and completeness.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>Unified Event View</strong>: Combines recurring events and individual events</li>
 *   <li><strong>Comprehensive Coverage</strong>: Complete view of user's event portfolio</li>
 *   <li><strong>Structured Organization</strong>: Separates recurring patterns from individual instances</li>
 *   <li><strong>Dashboard Integration</strong>: Supports comprehensive event dashboard displays</li>
 * </ul>
 * 
 * <h2>Event Type Separation</h2>
 * <p>The DTO maintains clear separation between event types:</p>
 * <ul>
 *   <li><strong>Recurring Events</strong>: Template-based events with recurring patterns</li>
 *   <li><strong>Individual Events</strong>: Standalone events and recurring event instances</li>
 *   <li><strong>Distinct Handling</strong>: Each type handled with appropriate data structures</li>
 *   <li><strong>UI Flexibility</strong>: Enables different UI treatment for different event types</li>
 * </ul>
 * 
 * <h2>Use Cases</h2>
 * <p>This DTO supports various event management use cases:</p>
 * <ul>
 *   <li><strong>Event Dashboard</strong>: Primary data for comprehensive event dashboards</li>
 *   <li><strong>Event Management</strong>: Enable management of all user events</li>
 *   <li><strong>Schedule Overview</strong>: Provide complete schedule overview</li>
 *   <li><strong>Event Planning</strong>: Support comprehensive event planning workflows</li>
 * </ul>
 * 
 * <h2>Data Organization</h2>
 * <p>Events are organized for optimal user experience:</p>
 * <ul>
 *   <li><strong>Type-based Grouping</strong>: Recurring events and individual events separated</li>
 *   <li><strong>Complete Information</strong>: Full event details via response DTOs</li>
 *   <li><strong>Filtering Applied</strong>: Events filtered based on user permissions and preferences</li>
 *   <li><strong>Sorting Available</strong>: Events can be sorted within their respective types</li>
 * </ul>
 * 
 * <h2>Event Filtering</h2>
 * <p>Events included in the response are filtered based on:</p>
 * <ul>
 *   <li><strong>User Ownership</strong>: Only events owned by the requesting user</li>
 *   <li><strong>Privacy Settings</strong>: Respect event privacy and visibility settings</li>
 *   <li><strong>Status Filtering</strong>: Include active events based on request parameters</li>
 *   <li><strong>Time Boundaries</strong>: Optional temporal filtering for relevant events</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Efficient Loading</strong>: Optimized queries for both event types</li>
 *   <li><strong>Lazy Evaluation</strong>: Event details loaded only when needed</li>
 *   <li><strong>Pagination Support</strong>: Large event lists can be paginated</li>
 *   <li><strong>Caching Strategy</strong>: Frequently accessed event lists cached</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This DTO integrates with multiple system components:</p>
 * <ul>
 *   <li><strong>MyEventsController</strong>: Primary source for user event API responses</li>
 *   <li><strong>EventService</strong>: Retrieves individual events and instances</li>
 *   <li><strong>RecurringEventService</strong>: Retrieves recurring event templates</li>
 *   <li><strong>Dashboard Components</strong>: UI components for comprehensive event displays</li>
 * </ul>
 * 
 * <h2>Data Completeness</h2>
 * <ul>
 *   <li><strong>Full Event Data</strong>: Complete event information via response DTOs</li>
 *   <li><strong>Recurring Patterns</strong>: Full recurring event details and patterns</li>
 *   <li><strong>Instance Information</strong>: Complete details for individual event instances</li>
 *   <li><strong>Metadata Inclusion</strong>: All relevant event metadata included</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * <ul>
 *   <li><strong>Event Dashboard</strong>: Primary data structure for user event dashboards</li>
 *   <li><strong>Event Management</strong>: Enable comprehensive event management interfaces</li>
 *   <li><strong>Schedule Review</strong>: Support complete schedule review workflows</li>
 *   <li><strong>Event Organization</strong>: Enable users to organize and categorize events</li>
 * </ul>
 * 
 * <h2>Validation and Constraints</h2>
 * <ul>
 *   <li><strong>Non-null Collections</strong>: Both event lists should never be null</li>
 *   <li><strong>Valid Event Data</strong>: All contained events must be valid</li>
 *   <li><strong>User Consistency</strong>: All events should belong to the same user</li>
 *   <li><strong>Permission Compliance</strong>: All events should be accessible to the user</li>
 * </ul>
 * 
 * <h2>Event Type Distinctions</h2>
 * <p>Understanding the distinction between included event types:</p>
 * <ul>
 *   <li><strong>Recurring Events</strong>: Template events with recurring patterns and rules</li>
 *   <li><strong>Individual Events</strong>: Standalone events and generated recurring instances</li>
 *   <li><strong>Template vs Instance</strong>: Recurring templates vs. actual event occurrences</li>
 *   <li><strong>Management Differences</strong>: Different management operations for each type</li>
 * </ul>
 * 
 * <h2>Important Notes</h2>
 * <ul>
 *   <li><strong>Unified Interface</strong>: Provides single interface for all user events</li>
 *   <li><strong>Type Awareness</strong>: Maintains awareness of different event types</li>
 *   <li><strong>Complete Coverage</strong>: Includes all relevant events for the user</li>
 *   <li><strong>UI Ready</strong>: Structured for direct consumption by UI components</li>
 * </ul>
 * 
 * @param recurringEvents list of recurring event templates owned by the user
 * @param events list of individual events and recurring event instances owned by the user
 * 
 * @see RecurringEventResponseDTO
 * @see EventResponseDTO
 * @see com.yohan.event_planner.controller.MyEventsController
 * @see com.yohan.event_planner.service.MyEventsService
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
public record MyEventsResponseDTO(
        List<RecurringEventResponseDTO> recurringEvents,
        List<EventResponseDTO> events
) {}
