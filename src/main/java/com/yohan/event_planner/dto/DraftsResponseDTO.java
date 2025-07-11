package com.yohan.event_planner.dto;

import java.util.List;

/**
 * Data transfer object for comprehensive draft management providing access to all unconfirmed user events.
 * 
 * <p>This DTO provides a unified view of all draft content created by a user, including both
 * individual event drafts and recurring event drafts. It enables comprehensive draft management
 * interfaces where users can review, edit, and confirm their unfinalized event planning content
 * before committing to their schedules.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>Draft Aggregation</strong>: Combines all types of draft events in one response</li>
 *   <li><strong>Comprehensive Coverage</strong>: Complete view of user's unconfirmed events</li>
 *   <li><strong>Type Separation</strong>: Maintains distinction between individual and recurring drafts</li>
 *   <li><strong>Management Support</strong>: Enables comprehensive draft management workflows</li>
 * </ul>
 * 
 * <h2>Draft Categories</h2>
 * <p>The DTO organizes drafts into two primary categories:</p>
 * 
 * <h3>Individual Event Drafts</h3>
 * <ul>
 *   <li><strong>Unconfirmed Events</strong>: Individual events marked as unconfirmed/draft</li>
 *   <li><strong>Scheduled Drafts</strong>: Events with specific times but not yet confirmed</li>
 *   <li><strong>Unscheduled Drafts</strong>: Event ideas without confirmed timing</li>
 *   <li><strong>Template Events</strong>: Events used as templates for future scheduling</li>
 * </ul>
 * 
 * <h3>Recurring Event Drafts</h3>
 * <ul>
 *   <li><strong>Unconfirmed Patterns</strong>: Recurring event templates not yet activated</li>
 *   <li><strong>Pattern Experiments</strong>: Recurring patterns being tested or refined</li>
 *   <li><strong>Scheduled Recurrence</strong>: Recurring events with patterns but not confirmed</li>
 *   <li><strong>Rule Drafts</strong>: Complex recurrence rules being developed</li>
 * </ul>
 * 
 * <h2>Use Cases</h2>
 * <p>This DTO supports various draft management use cases:</p>
 * <ul>
 *   <li><strong>Draft Dashboard</strong>: Comprehensive view of all unconfirmed events</li>
 *   <li><strong>Planning Workflows</strong>: Support iterative event planning processes</li>
 *   <li><strong>Review Process</strong>: Enable review and confirmation of draft events</li>
 *   <li><strong>Content Organization</strong>: Organize and categorize draft content</li>
 * </ul>
 * 
 * <h2>Draft States</h2>
 * <p>Drafts can exist in various states:</p>
 * <ul>
 *   <li><strong>Unconfirmed Status</strong>: Events marked with unconfirmed flag</li>
 *   <li><strong>Incomplete Data</strong>: Events with missing required information</li>
 *   <li><strong>Pending Review</strong>: Events awaiting user confirmation</li>
 *   <li><strong>Template Status</strong>: Events used as templates for future creation</li>
 * </ul>
 * 
 * <h2>Management Operations</h2>
 * <p>Drafts enable various management operations:</p>
 * <ul>
 *   <li><strong>Confirmation</strong>: Convert drafts to confirmed events</li>
 *   <li><strong>Editing</strong>: Modify draft content before confirmation</li>
 *   <li><strong>Deletion</strong>: Remove unwanted draft content</li>
 *   <li><strong>Duplication</strong>: Create new events based on draft templates</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Efficient Loading</strong>: Optimized queries for draft content</li>
 *   <li><strong>Lazy Evaluation</strong>: Draft details loaded only when needed</li>
 *   <li><strong>Filtering Support</strong>: Efficient filtering of draft vs. confirmed content</li>
 *   <li><strong>Batch Operations</strong>: Support for batch draft management operations</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This DTO integrates with multiple system components:</p>
 * <ul>
 *   <li><strong>Draft Management UI</strong>: Primary data for draft management interfaces</li>
 *   <li><strong>EventService</strong>: Retrieves individual event drafts</li>
 *   <li><strong>RecurringEventService</strong>: Retrieves recurring event drafts</li>
 *   <li><strong>Planning Workflows</strong>: Supports iterative planning processes</li>
 * </ul>
 * 
 * <h2>Data Consistency</h2>
 * <ul>
 *   <li><strong>User Ownership</strong>: All drafts belong to the requesting user</li>
 *   <li><strong>Draft Status</strong>: All events maintain proper draft/unconfirmed status</li>
 *   <li><strong>Complete Information</strong>: Full event details available for management</li>
 *   <li><strong>Type Separation</strong>: Clear separation between individual and recurring drafts</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * <ul>
 *   <li><strong>Draft Dashboard</strong>: Primary data structure for draft management dashboards</li>
 *   <li><strong>Planning Interface</strong>: Enable iterative event planning workflows</li>
 *   <li><strong>Review Process</strong>: Support draft review and confirmation processes</li>
 *   <li><strong>Content Management</strong>: Organize and manage unconfirmed content</li>
 * </ul>
 * 
 * <h2>Validation and Constraints</h2>
 * <ul>
 *   <li><strong>Non-null Collections</strong>: Both draft lists should never be null</li>
 *   <li><strong>Draft Status</strong>: All events should have appropriate draft/unconfirmed status</li>
 *   <li><strong>User Consistency</strong>: All drafts should belong to the same user</li>
 *   <li><strong>Valid Content</strong>: All draft content should be valid and manageable</li>
 * </ul>
 * 
 * <h2>Planning Workflow Support</h2>
 * <p>Drafts support iterative planning workflows:</p>
 * <ul>
 *   <li><strong>Incremental Planning</strong>: Build events incrementally over time</li>
 *   <li><strong>Template Reuse</strong>: Reuse draft templates for similar events</li>
 *   <li><strong>Experimental Scheduling</strong>: Test scheduling ideas before confirmation</li>
 *   <li><strong>Collaborative Planning</strong>: Share and review draft content</li>
 * </ul>
 * 
 * <h2>Important Notes</h2>
 * <ul>
 *   <li><strong>Draft Focus</strong>: Specifically designed for unconfirmed content management</li>
 *   <li><strong>Unified Interface</strong>: Provides single interface for all draft types</li>
 *   <li><strong>Management Ready</strong>: Structured for comprehensive draft management</li>
 *   <li><strong>Planning Support</strong>: Enables flexible event planning workflows</li>
 * </ul>
 * 
 * @param eventDrafts list of individual event drafts and unconfirmed events
 * @param recurringEventDrafts list of recurring event drafts and unconfirmed recurring patterns
 * 
 * @see EventResponseDTO
 * @see RecurringEventResponseDTO
 * @see com.yohan.event_planner.service.EventService
 * @see com.yohan.event_planner.service.RecurringEventService
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
public record DraftsResponseDTO(
        List<EventResponseDTO> eventDrafts,
        List<RecurringEventResponseDTO> recurringEventDrafts
) {
}