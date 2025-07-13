package com.yohan.event_planner.dto;

import java.util.List;

/**
 * Data transfer object for comprehensive user profile display with context-aware information.
 * 
 * <p>This DTO provides a complete user profile view that adapts based on the viewing context,
 * distinguishing between self-view and external view scenarios. It combines user header information
 * with badge achievements and pinned impromptu events to create a comprehensive profile display that 
 * respects privacy settings and provides appropriate information based on the relationship between 
 * viewer and profile owner.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>Context-Aware Display</strong>: Adapts content based on self vs. external viewing</li>
 *   <li><strong>Complete Profile</strong>: Combines header information with achievement badges</li>
 *   <li><strong>Privacy Respect</strong>: Respects privacy settings and viewing permissions</li>
 *   <li><strong>Achievement Showcase</strong>: Displays user achievements and accomplishments</li>
 *   <li><strong>Pinned Event Display</strong>: Shows pinned impromptu events as dashboard reminders for profile owners</li>
 * </ul>
 * 
 * <h2>Viewing Context</h2>
 * <p>The {@code isSelf} flag determines the viewing context and available information:</p>
 * 
 * <h3>Self-View (isSelf = true)</h3>
 * <ul>
 *   <li><strong>Full Information</strong>: Complete access to all profile information</li>
 *   <li><strong>Private Details</strong>: Access to private profile elements</li>
 *   <li><strong>Management Controls</strong>: Enable profile editing and management features</li>
 *   <li><strong>Complete Badge List</strong>: All earned badges, including private ones</li>
 *   <li><strong>Pinned Events</strong>: Access to currently pinned impromptu events for dashboard reminders</li>
 * </ul>
 * 
 * <h3>External View (isSelf = false)</h3>
 * <ul>
 *   <li><strong>Public Information</strong>: Only publicly available profile information</li>
 *   <li><strong>Privacy Filtered</strong>: Private details filtered based on settings</li>
 *   <li><strong>View-only Mode</strong>: No management controls available</li>
 *   <li><strong>Public Badges</strong>: Only publicly visible badges displayed</li>
 *   <li><strong>No Pinned Events</strong>: Pinned events are never visible to external viewers</li>
 * </ul>
 * 
 * <h2>Profile Components</h2>
 * <p>The profile combines multiple information components:</p>
 * <ul>
 *   <li><strong>Header Information</strong>: Core user details via UserHeaderResponseDTO</li>
 *   <li><strong>Achievement Badges</strong>: User accomplishments and milestones</li>
 *   <li><strong>Pinned Events</strong>: Currently pinned impromptu events for dashboard reminders (owner-only)</li>
 *   <li><strong>Context Metadata</strong>: Viewing context and permission information</li>
 *   <li><strong>Privacy Controls</strong>: Appropriate filtering based on privacy settings</li>
 * </ul>
 * 
 * <h2>Pinned Impromptu Events</h2>
 * <p>The pinned event functionality provides dashboard reminders for profile owners:</p>
 * <ul>
 *   <li><strong>Owner-Only Visibility</strong>: Pinned events are only visible when {@code isSelf = true}</li>
 *   <li><strong>Qualification Filtering</strong>: Only events with {@code draft = true && impromptu = true} are shown</li>
 *   <li><strong>Automatic Cleanup</strong>: Invalid pinned events are automatically removed during retrieval</li>
 *   <li><strong>Null Safety</strong>: Field is null when no qualifying pinned event exists or for external viewers</li>
 *   <li><strong>Real-time Validation</strong>: Pinned event status is validated on every profile request</li>
 * </ul>
 * 
 * <h2>Use Cases</h2>
 * <p>This DTO supports various profile display use cases:</p>
 * <ul>
 *   <li><strong>User Profile Page</strong>: Complete profile display for users</li>
 *   <li><strong>Profile Management</strong>: Enable profile editing when viewing own profile</li>
 *   <li><strong>Public Profiles</strong>: Display public profile information to other users</li>
 *   <li><strong>Achievement Display</strong>: Showcase user achievements and badges</li>
 * </ul>
 * 
 * <h2>Privacy and Security</h2>
 * <ul>
 *   <li><strong>Context-Aware Filtering</strong>: Information filtered based on viewing context</li>
 *   <li><strong>Privacy Settings</strong>: Respect user privacy preferences</li>
 *   <li><strong>Permission Checks</strong>: Ensure viewer has appropriate permissions</li>
 *   <li><strong>Data Minimization</strong>: Only include necessary information for context</li>
 * </ul>
 * 
 * <h2>Badge Integration</h2>
 * <p>Badges provide achievement and accomplishment information:</p>
 * <ul>
 *   <li><strong>Achievement Display</strong>: Visual representation of user accomplishments</li>
 *   <li><strong>Progress Tracking</strong>: Show user progress and milestones</li>
 *   <li><strong>Privacy Filtering</strong>: Filter badges based on privacy settings</li>
 *   <li><strong>Motivational Elements</strong>: Encourage user engagement and achievement</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Efficient Loading</strong>: Optimized queries for profile components</li>
 *   <li><strong>Lazy Badge Loading</strong>: Badges loaded only when needed</li>
 *   <li><strong>Caching Strategy</strong>: Profile data cached for frequent access</li>
 *   <li><strong>Context-Aware Queries</strong>: Query optimization based on viewing context</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This DTO integrates with multiple system components:</p>
 * <ul>
 *   <li><strong>UserProfileController</strong>: Primary source for profile API responses</li>
 *   <li><strong>UserService</strong>: Retrieves user header and profile information</li>
 *   <li><strong>BadgeService</strong>: Retrieves user achievement badges</li>
 *   <li><strong>Profile UI Components</strong>: UI components for profile displays</li>
 * </ul>
 * 
 * <h2>Data Consistency</h2>
 * <ul>
 *   <li><strong>User Coherence</strong>: All components represent the same user</li>
 *   <li><strong>Privacy Consistency</strong>: Consistent privacy filtering across components</li>
 *   <li><strong>Context Alignment</strong>: All data aligned with viewing context</li>
 *   <li><strong>Real-time Updates</strong>: Profile reflects current user state</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * <ul>
 *   <li><strong>Profile Display</strong>: Primary data structure for user profile pages</li>
 *   <li><strong>Profile Management</strong>: Enable profile editing and management</li>
 *   <li><strong>Social Features</strong>: Support social profile viewing features</li>
 *   <li><strong>Achievement Tracking</strong>: Display and track user achievements</li>
 * </ul>
 * 
 * <h2>Validation and Constraints</h2>
 * <ul>
 *   <li><strong>Valid Header</strong>: UserHeaderResponseDTO must be valid and complete</li>
 *   <li><strong>Non-null Collections</strong>: Badges list should never be null</li>
 *   <li><strong>Context Consistency</strong>: isSelf flag must accurately reflect viewing context</li>
 *   <li><strong>Privacy Compliance</strong>: All data must comply with privacy settings</li>
 * </ul>
 * 
 * <h2>GraphQL Integration</h2>
 * <p>This DTO is designed for GraphQL profile queries:</p>
 * <ul>
 *   <li><strong>Field Selection</strong>: Supports GraphQL field selection</li>
 *   <li><strong>Nested Queries</strong>: Handles nested header and badge queries</li>
 *   <li><strong>Context Resolvers</strong>: GraphQL resolvers handle context determination</li>
 *   <li><strong>Privacy Resolvers</strong>: GraphQL resolvers apply privacy filtering</li>
 * </ul>
 * 
 * <h2>Important Notes</h2>
 * <ul>
 *   <li><strong>Context Critical</strong>: Viewing context determines available information</li>
 *   <li><strong>Privacy First</strong>: Privacy considerations built into the design</li>
 *   <li><strong>Achievement Focus</strong>: Emphasizes user achievements and progress</li>
 *   <li><strong>UI Ready</strong>: Structured for direct consumption by UI components</li>
 * </ul>
 * 
 * @param isSelf flag indicating whether the viewer is viewing their own profile
 * @param header user header information including display name and profile details
 * @param badges list of achievement badges earned by the user, filtered by privacy settings
 * @param pinnedImpromptuEvent currently pinned impromptu event (only visible to profile owner when qualified)
 * 
 * @see UserHeaderResponseDTO
 * @see BadgeResponseDTO
 * @see EventResponseDTO
 * @see com.yohan.event_planner.controller.UserProfileGraphQLController
 * @see com.yohan.event_planner.service.BadgeService
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
public record UserProfileResponseDTO(
        boolean isSelf,
        UserHeaderResponseDTO header,
        List<BadgeResponseDTO> badges,
        EventResponseDTO pinnedImpromptuEvent
) {
}