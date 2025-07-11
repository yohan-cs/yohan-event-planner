package com.yohan.event_planner.dto;

import jakarta.validation.constraints.NotNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Data transfer object capturing comprehensive event change context for time tracking and analytics.
 * 
 * <p>This DTO encapsulates the complete before-and-after state of event modifications, enabling
 * precise time bucket adjustments, analytics tracking, and audit capabilities. It supports
 * complex scenarios involving label changes, time modifications, and completion state transitions
 * essential for accurate time tracking within the event planning system.</p>
 * 
 * <h2>Core Change Tracking</h2>
 * <ul>
 *   <li><strong>Label Transitions</strong>: Track changes between event categorization labels</li>
 *   <li><strong>Time Modifications</strong>: Capture start time and duration adjustments</li>
 *   <li><strong>Completion State</strong>: Monitor transitions between completed and incomplete states</li>
 *   <li><strong>User Context</strong>: Maintain user ownership and timezone information</li>
 * </ul>
 * 
 * <h2>Time Tracking Integration</h2>
 * <p>Essential for maintaining accurate time bucket statistics:</p>
 * <ul>
 *   <li><strong>Time Bucket Adjustments</strong>: Revert old time allocations and apply new ones</li>
 *   <li><strong>Label Redistribution</strong>: Move time between labels when events are recategorized</li>
 *   <li><strong>Duration Changes</strong>: Adjust time tracking when event durations change</li>
 *   <li><strong>Completion Tracking</strong>: Apply time tracking only for completed events</li>
 * </ul>
 * 
 * <h2>Change Scenarios</h2>
 * <p>The DTO supports various event modification scenarios:</p>
 * 
 * <h3>Label Changes</h3>
 * <ul>
 *   <li><strong>Recategorization</strong>: Events moved between different labels</li>
 *   <li><strong>Time Redistribution</strong>: Adjust time bucket allocations across labels</li>
 *   <li><strong>Analytics Impact</strong>: Maintain accurate label-based statistics</li>
 * </ul>
 * 
 * <h3>Time Modifications</h3>
 * <ul>
 *   <li><strong>Start Time Changes</strong>: Adjust event scheduling and bucket placement</li>
 *   <li><strong>Duration Changes</strong>: Modify event length and time allocations</li>
 *   <li><strong>Timezone Awareness</strong>: Handle time changes across different timezones</li>
 * </ul>
 * 
 * <h3>Completion State Transitions</h3>
 * <ul>
 *   <li><strong>Completion</strong>: Events transitioning from incomplete to completed</li>
 *   <li><strong>Reversion</strong>: Events changing from completed back to incomplete</li>
 *   <li><strong>Time Tracking</strong>: Apply or remove time tracking based on completion</li>
 * </ul>
 * 
 * <h2>Timezone Handling</h2>
 * <ul>
 *   <li><strong>User Timezone</strong>: Capture timezone context for accurate bucket placement</li>
 *   <li><strong>Time Calculations</strong>: Ensure proper timezone-aware time bucket assignments</li>
 *   <li><strong>DST Handling</strong>: Account for daylight saving time transitions</li>
 *   <li><strong>Cross-timezone Changes</strong>: Handle events modified across timezone boundaries</li>
 * </ul>
 * 
 * <h2>Analytics and Audit Support</h2>
 * <ul>
 *   <li><strong>Change Tracking</strong>: Complete audit trail of event modifications</li>
 *   <li><strong>Time Analytics</strong>: Precise time allocation adjustments</li>
 *   <li><strong>Label Analytics</strong>: Track label usage and redistribution patterns</li>
 *   <li><strong>Productivity Metrics</strong>: Monitor completion state changes over time</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This DTO integrates with multiple system components:</p>
 * <ul>
 *   <li><strong>LabelTimeBucketService</strong>: Primary consumer for time tracking adjustments</li>
 *   <li><strong>EventService</strong>: Generates change context during event updates</li>
 *   <li><strong>Analytics Services</strong>: Provides data for change pattern analysis</li>
 *   <li><strong>Audit Systems</strong>: Enables comprehensive change tracking</li>
 * </ul>
 * 
 * <h2>Data Consistency</h2>
 * <ul>
 *   <li><strong>Atomic Changes</strong>: Ensure all related time bucket adjustments are atomic</li>
 *   <li><strong>Compensating Actions</strong>: Revert old allocations before applying new ones</li>
 *   <li><strong>State Validation</strong>: Verify completion state consistency</li>
 *   <li><strong>Timezone Consistency</strong>: Maintain accurate timezone-aware calculations</li>
 * </ul>
 * 
 * <h2>Validation and Constraints</h2>
 * <ul>
 *   <li><strong>Required Fields</strong>: userId and timezone are mandatory for proper processing</li>
 *   <li><strong>State Consistency</strong>: Ensure logical consistency between old and new values</li>
 *   <li><strong>Null Handling</strong>: Proper handling of optional change fields</li>
 *   <li><strong>Time Validation</strong>: Ensure valid time ranges and durations</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Efficient Processing</strong>: Minimize database operations for change processing</li>
 *   <li><strong>Batch Operations</strong>: Group related time bucket adjustments</li>
 *   <li><strong>Change Detection</strong>: Optimize processing based on actual changes</li>
 *   <li><strong>Timezone Calculations</strong>: Cache timezone conversions where possible</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * <ul>
 *   <li><strong>Event Updates</strong>: Track changes during event modification operations</li>
 *   <li><strong>Completion Workflows</strong>: Handle event completion and reversion</li>
 *   <li><strong>Label Management</strong>: Support event recategorization operations</li>
 *   <li><strong>Time Tracking</strong>: Maintain accurate time allocation statistics</li>
 * </ul>
 * 
 * <h2>Important Notes</h2>
 * <ul>
 *   <li><strong>Complete Context</strong>: Captures both before and after states for all relevant fields</li>
 *   <li><strong>Timezone Critical</strong>: Timezone information is essential for accurate time calculations</li>
 *   <li><strong>Completion Focused</strong>: Time tracking only applied to completed events</li>
 *   <li><strong>Change Granularity</strong>: Supports fine-grained change tracking and processing</li>
 * </ul>
 * 
 * @see com.yohan.event_planner.service.LabelTimeBucketService
 * @see com.yohan.event_planner.service.EventService
 * @see com.yohan.event_planner.domain.Event
 * @see com.yohan.event_planner.domain.LabelTimeBucket
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
public record EventChangeContextDTO(
        @NotNull Long userId,
        Long oldLabelId,
        Long newLabelId,
        ZonedDateTime oldStartTime,
        ZonedDateTime newStartTime,
        Integer oldDurationMinutes,
        Integer newDurationMinutes,
        @NotNull ZoneId timezone,
        boolean wasCompleted,
        boolean isNowCompleted
) {}