package com.yohan.event_planner.dto;

import java.time.LocalDate;
import java.util.Map;

/**
 * Data transfer object for resolving scheduling conflicts when creating recurring events.
 * 
 * <p>This DTO enables users to specify precise resolution strategies for each conflicting date
 * when creating recurring events that overlap with existing events. It provides granular control
 * over conflict resolution, allowing users to decide which events should be skipped on specific
 * dates to prevent scheduling conflicts.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>Conflict Resolution Mapping</strong>: Maps conflicting dates to skip decisions</li>
 *   <li><strong>Granular Control</strong>: Individual resolution for each conflicting date</li>
 *   <li><strong>Event Identification</strong>: Clear identification of new and existing events</li>
 *   <li><strong>Skip Logic</strong>: Determines which event instance should be skipped</li>
 * </ul>
 * 
 * <h2>Resolution Strategy</h2>
 * <p>The resolution map ({@code resolutions}) uses the following format:</p>
 * <ul>
 *   <li><strong>Key</strong>: {@link LocalDate} representing the conflicting date</li>
 *   <li><strong>Value</strong>: {@link Long} ID of the event that should skip that date</li>
 *   <li><strong>New Event Skip</strong>: Use {@code newEventId} to skip the new recurring event</li>
 *   <li><strong>Existing Event Skip</strong>: Use existing event ID to skip the existing event</li>
 * </ul>
 * 
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Conflict resolution example
 * Map<LocalDate, Long> resolutions = Map.of(
 *     LocalDate.of(2024, 7, 1), 999L,  // Skip new event on July 1st
 *     LocalDate.of(2024, 7, 3), 101L   // Skip existing event (ID 101) on July 3rd
 * );
 * 
 * ConflictResolutionDTO resolution = new ConflictResolutionDTO(999L, resolutions);
 * }</pre>
 * 
 * <h2>Conflict Types</h2>
 * <p>The DTO supports resolution for various conflict scenarios:</p>
 * <ul>
 *   <li><strong>New vs. Existing</strong>: New recurring event conflicts with existing events</li>
 *   <li><strong>Time Overlap</strong>: Events with overlapping time ranges</li>
 *   <li><strong>Multi-instance Conflicts</strong>: Multiple instances of the same recurring event</li>
 *   <li><strong>Cross-label Conflicts</strong>: Events from different labels conflicting</li>
 * </ul>
 * 
 * <h2>Resolution Outcomes</h2>
 * <p>Based on the resolution map, the system will:</p>
 * <ul>
 *   <li><strong>Skip New Event</strong>: Create skip entries for new recurring event instances</li>
 *   <li><strong>Skip Existing Event</strong>: Create skip entries for existing conflicting events</li>
 *   <li><strong>Maintain Schedule</strong>: Preserve the overall recurring schedule integrity</li>
 *   <li><strong>Update Metadata</strong>: Track skip reasons and conflict resolution history</li>
 * </ul>
 * 
 * <h2>Validation Rules</h2>
 * <ul>
 *   <li><strong>Event ID Validation</strong>: All event IDs must reference valid existing events</li>
 *   <li><strong>Date Consistency</strong>: Conflict dates must correspond to actual conflicts</li>
 *   <li><strong>Resolution Completeness</strong>: All provided conflicts must have resolutions</li>
 *   <li><strong>User Ownership</strong>: Users can only resolve conflicts for their own events</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This DTO integrates with multiple system components:</p>
 * <ul>
 *   <li><strong>RecurringEventService</strong>: Primary consumer for conflict resolution</li>
 *   <li><strong>ConflictValidator</strong>: Validates resolution decisions</li>
 *   <li><strong>EventService</strong>: Applies skip logic to conflicting events</li>
 *   <li><strong>REST Controllers</strong>: API parameter binding for conflict resolution</li>
 * </ul>
 * 
 * <h2>Business Rules</h2>
 * <ul>
 *   <li><strong>User Choice</strong>: Users have complete control over conflict resolution</li>
 *   <li><strong>Atomic Operations</strong>: All resolutions applied as a single transaction</li>
 *   <li><strong>Audit Trail</strong>: Conflict resolution decisions are logged</li>
 *   <li><strong>Reversibility</strong>: Skip decisions can be modified after creation</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Efficient Processing</strong>: Batch processing of resolution decisions</li>
 *   <li><strong>Database Optimization</strong>: Minimize database queries during resolution</li>
 *   <li><strong>Memory Usage</strong>: Efficient map structure for large conflict sets</li>
 *   <li><strong>Validation Caching</strong>: Cache event validations during processing</li>
 * </ul>
 * 
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li><strong>Ownership Validation</strong>: Ensure users can only resolve their own conflicts</li>
 *   <li><strong>Event Access Control</strong>: Validate access to all referenced events</li>
 *   <li><strong>Input Sanitization</strong>: Validate all event IDs and dates</li>
 *   <li><strong>Authorization</strong>: Verify user permissions for conflict resolution</li>
 * </ul>
 * 
 * <h2>Error Handling</h2>
 * <ul>
 *   <li><strong>Invalid Event IDs</strong>: Clear error messages for non-existent events</li>
 *   <li><strong>Ownership Violations</strong>: Proper error handling for unauthorized access</li>
 *   <li><strong>Inconsistent Resolutions</strong>: Validation of resolution logic</li>
 *   <li><strong>Partial Failures</strong>: Rollback support for failed resolutions</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * <ul>
 *   <li><strong>Recurring Event Creation</strong>: Resolve conflicts during event creation</li>
 *   <li><strong>Schedule Optimization</strong>: Optimize schedules by resolving conflicts</li>
 *   <li><strong>Batch Operations</strong>: Handle multiple conflicts in a single operation</li>
 *   <li><strong>User Experience</strong>: Provide clear conflict resolution interfaces</li>
 * </ul>
 * 
 * <h2>Important Notes</h2>
 * <ul>
 *   <li><strong>User Decision</strong>: All conflict resolution decisions are user-driven</li>
 *   <li><strong>Flexible Resolution</strong>: Supports various resolution strategies</li>
 *   <li><strong>Audit Support</strong>: Maintains history of conflict resolution decisions</li>
 *   <li><strong>Schedule Integrity</strong>: Preserves overall schedule consistency</li>
 * </ul>
 * 
 * @param newEventId the ID of the new recurring event being created
 * @param resolutions a map of conflicting dates to the ID of the event that should skip that date
 * 
 * @see com.yohan.event_planner.service.RecurringEventService
 * @see com.yohan.event_planner.validation.ConflictValidator
 * @see com.yohan.event_planner.domain.RecurringEvent
 * @see com.yohan.event_planner.domain.Event
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
public record ConflictResolutionDTO(
        Long newEventId,
        Map<LocalDate, Long> resolutions
) {
}