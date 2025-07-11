package com.yohan.event_planner.dto;

import java.time.LocalDate;
import java.util.SortedMap;
import java.util.Set;

/**
 * Data transfer object for recurring event creation results with conflict information and success data.
 * 
 * <p>This DTO encapsulates the complete result of a recurring event creation operation, including
 * the successfully created recurring event and detailed information about any scheduling conflicts
 * that were detected during the creation process. It enables comprehensive conflict resolution
 * workflows and provides complete transparency about the creation outcome.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>Creation Success</strong>: Contains the successfully created recurring event</li>
 *   <li><strong>Conflict Detection</strong>: Detailed mapping of conflicts discovered during creation</li>
 *   <li><strong>Resolution Support</strong>: Enables conflict resolution workflows</li>
 *   <li><strong>Complete Information</strong>: Provides all information needed for next steps</li>
 * </ul>
 * 
 * <h2>Conflict Mapping Structure</h2>
 * <p>The conflict mapping ({@code conflictingDatesToEvent}) provides detailed conflict information:</p>
 * <ul>
 *   <li><strong>Date Keys</strong>: {@link LocalDate} representing specific conflict dates</li>
 *   <li><strong>Event Sets</strong>: {@link Set} of event IDs that conflict on each date</li>
 *   <li><strong>Sorted Order</strong>: {@link SortedMap} ensures chronological conflict presentation</li>
 *   <li><strong>Multiple Conflicts</strong>: Supports multiple conflicting events per date</li>
 * </ul>
 * 
 * <h2>Creation Outcomes</h2>
 * <p>The DTO supports various creation outcome scenarios:</p>
 * 
 * <h3>Successful Creation Without Conflicts</h3>
 * <ul>
 *   <li><strong>Clean Creation</strong>: Recurring event created without any conflicts</li>
 *   <li><strong>Empty Conflicts</strong>: Conflict map is empty</li>
 *   <li><strong>Ready to Use</strong>: Event is immediately available for use</li>
 * </ul>
 * 
 * <h3>Successful Creation With Conflicts</h3>
 * <ul>
 *   <li><strong>Partial Success</strong>: Recurring event created but conflicts detected</li>
 *   <li><strong>Conflict Details</strong>: Detailed conflict information provided</li>
 *   <li><strong>Resolution Required</strong>: User action needed to resolve conflicts</li>
 * </ul>
 * 
 * <h2>Conflict Resolution Workflow</h2>
 * <p>The DTO enables comprehensive conflict resolution:</p>
 * <ul>
 *   <li><strong>Conflict Identification</strong>: Clear identification of all conflict dates</li>
 *   <li><strong>Event Mapping</strong>: Specific events involved in each conflict</li>
 *   <li><strong>Resolution Planning</strong>: Information needed for resolution strategies</li>
 *   <li><strong>User Decision Support</strong>: Data to support user conflict resolution decisions</li>
 * </ul>
 * 
 * <h2>Use Cases</h2>
 * <p>This DTO supports various recurring event creation use cases:</p>
 * <ul>
 *   <li><strong>Interactive Creation</strong>: Support interactive recurring event creation workflows</li>
 *   <li><strong>Conflict Resolution</strong>: Enable comprehensive conflict resolution processes</li>
 *   <li><strong>Creation Confirmation</strong>: Provide creation confirmation with conflict awareness</li>
 *   <li><strong>Batch Processing</strong>: Support batch recurring event creation operations</li>
 * </ul>
 * 
 * <h2>Conflict Types</h2>
 * <p>The conflict detection handles various conflict scenarios:</p>
 * <ul>
 *   <li><strong>Time Overlap</strong>: Events with overlapping time ranges</li>
 *   <li><strong>Exact Timing</strong>: Events with identical start/end times</li>
 *   <li><strong>Cross-label Conflicts</strong>: Conflicts between different event labels</li>
 *   <li><strong>Recurring vs Single</strong>: Conflicts between recurring and single events</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Efficient Conflict Detection</strong>: Optimized algorithms for conflict detection</li>
 *   <li><strong>Sorted Structures</strong>: Chronologically sorted conflict information</li>
 *   <li><strong>Memory Optimization</strong>: Efficient data structures for conflict mapping</li>
 *   <li><strong>Lazy Evaluation</strong>: Conflict details computed only when needed</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This DTO integrates with multiple system components:</p>
 * <ul>
 *   <li><strong>RecurringEventController</strong>: Primary source for creation API responses</li>
 *   <li><strong>RecurringEventService</strong>: Service layer for recurring event creation</li>
 *   <li><strong>ConflictValidator</strong>: Conflict detection and validation logic</li>
 *   <li><strong>Conflict Resolution UI</strong>: UI components for conflict resolution</li>
 * </ul>
 * 
 * <h2>Data Consistency</h2>
 * <ul>
 *   <li><strong>Creation Success</strong>: Saved event represents successfully created event</li>
 *   <li><strong>Conflict Accuracy</strong>: Conflict mapping accurately reflects detected conflicts</li>
 *   <li><strong>Date Consistency</strong>: All dates in conflict map are valid and relevant</li>
 *   <li><strong>Event Validity</strong>: All event IDs in conflicts reference valid events</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * <ul>
 *   <li><strong>Creation Response</strong>: Primary response structure for recurring event creation</li>
 *   <li><strong>Conflict Workflow</strong>: Enable comprehensive conflict resolution workflows</li>
 *   <li><strong>User Feedback</strong>: Provide detailed feedback about creation results</li>
 *   <li><strong>Next Steps</strong>: Guide users through post-creation actions</li>
 * </ul>
 * 
 * <h2>Validation and Constraints</h2>
 * <ul>
 *   <li><strong>Valid Event</strong>: Saved event must be valid and complete</li>
 *   <li><strong>Valid Conflicts</strong>: All conflict dates and event IDs must be valid</li>
 *   <li><strong>Chronological Order</strong>: Conflict dates should be chronologically ordered</li>
 *   <li><strong>Non-null Map</strong>: Conflict map should never be null (use empty map)</li>
 * </ul>
 * 
 * <h2>Conflict Resolution Integration</h2>
 * <p>The DTO integrates with conflict resolution workflows:</p>
 * <ul>
 *   <li><strong>Resolution Input</strong>: Provides input data for ConflictResolutionDTO</li>
 *   <li><strong>Decision Support</strong>: Enables informed conflict resolution decisions</li>
 *   <li><strong>Workflow Continuation</strong>: Supports continued conflict resolution workflows</li>
 *   <li><strong>User Experience</strong>: Provides clear conflict information for user review</li>
 * </ul>
 * 
 * <h2>Important Notes</h2>
 * <ul>
 *   <li><strong>Creation Success</strong>: Event is successfully created regardless of conflicts</li>
 *   <li><strong>Conflict Transparency</strong>: All detected conflicts are transparently reported</li>
 *   <li><strong>Resolution Ready</strong>: Provides all information needed for conflict resolution</li>
 *   <li><strong>User Empowerment</strong>: Empowers users to make informed conflict resolution decisions</li>
 * </ul>
 * 
 * @param savedEvent the successfully created recurring event with complete information
 * @param conflictingDatesToEvent sorted map of conflict dates to sets of conflicting event IDs
 * 
 * @see RecurringEventResponseDTO
 * @see ConflictResolutionDTO
 * @see com.yohan.event_planner.service.RecurringEventService
 * @see com.yohan.event_planner.validation.ConflictValidator
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
public record RecurringEventCreationResultDTO(
        RecurringEventResponseDTO savedEvent,
        SortedMap<LocalDate, Set<Long>> conflictingDatesToEvent
) {}
