package com.yohan.event_planner.repository;

import com.yohan.event_planner.domain.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link Badge} entities with ordering and aggregation support.
 * 
 * <p>This repository provides specialized data access functionality for user-defined badges that
 * group multiple labels for enhanced analytics and time tracking. It supports user-scoped queries,
 * sophisticated ordering management, and aggregation operations essential for badge-based
 * organization and goal tracking within the event planning system.</p>
 * 
 * <h2>Core Query Categories</h2>
 * <ul>
 *   <li><strong>User-Scoped Queries</strong>: Retrieve badges owned by specific users</li>
 *   <li><strong>Ordering Management</strong>: Support display order manipulation and queries</li>
 *   <li><strong>Aggregation Queries</strong>: Support for badge statistics and analytics</li>
 *   <li><strong>Validation Queries</strong>: Enable badge existence and ownership verification</li>
 * </ul>
 * 
 * <h2>User-Scoped Badge Management</h2>
 * <p>Badges are inherently user-specific with comprehensive ownership support:</p>
 * <ul>
 *   <li><strong>Owner-based Queries</strong>: All badges scoped to owning user</li>
 *   <li><strong>Privacy Isolation</strong>: Complete separation between user badge collections</li>
 *   <li><strong>Ownership Validation</strong>: Support for service-layer security enforcement</li>
 *   <li><strong>Data Integrity</strong>: Maintain proper user-badge relationships</li>
 * </ul>
 * 
 * <h2>Display Order Management</h2>
 * <p>Sophisticated ordering system for optimal user experience:</p>
 * <ul>
 *   <li><strong>Sort Order Queries</strong>: Retrieve badges in user-defined display order</li>
 *   <li><strong>Order Calculation</strong>: Support for determining next available positions</li>
 *   <li><strong>Reordering Support</strong>: Enable complete badge reorganization</li>
 *   <li><strong>Consistent Presentation</strong>: Ensure predictable badge display</li>
 * </ul>
 * 
 * <h2>Badge Aggregation Support</h2>
 * <p>Enable statistical analysis and reporting:</p>
 * <ul>
 *   <li><strong>Count Queries</strong>: Badge collection statistics per user</li>
 *   <li><strong>Order Statistics</strong>: Maximum sort order for positioning</li>
 *   <li><strong>Collection Queries</strong>: Complete badge sets for operations</li>
 *   <li><strong>Analytics Support</strong>: Data foundations for badge statistics</li>
 * </ul>
 * 
 * <h2>Label Relationship Support</h2>
 * <p>Badge-label associations through repository patterns:</p>
 * <ul>
 *   <li><strong>Multi-Label Badges</strong>: Support badges containing multiple labels</li>
 *   <li><strong>Relationship Integrity</strong>: Maintain badge-label associations</li>
 *   <li><strong>Cascade Operations</strong>: Handle badge deletion with label cleanup</li>
 *   <li><strong>Association Queries</strong>: Support label-badge relationship queries</li>
 * </ul>
 * 
 * <h2>Performance Optimization</h2>
 * <ul>
 *   <li><strong>Indexed Queries</strong>: Utilize user_id and sortOrder indexes</li>
 *   <li><strong>Efficient Sorting</strong>: Database-level ordering for performance</li>
 *   <li><strong>Aggregation Functions</strong>: Use database aggregation for statistics</li>
 *   <li><strong>Minimal Data Transfer</strong>: Optimized result sets for specific operations</li>
 * </ul>
 * 
 * <h2>Ordering Operations</h2>
 * <p>Support for complex badge reordering scenarios:</p>
 * <ul>
 *   <li><strong>Position Calculation</strong>: Determine optimal sort order values</li>
 *   <li><strong>Reorder Validation</strong>: Enable complete collection reordering</li>
 *   <li><strong>Gap Management</strong>: Handle sort order gaps and compaction</li>
 *   <li><strong>Consistency Maintenance</strong>: Ensure ordering integrity</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This repository integrates with:</p>
 * <ul>
 *   <li><strong>BadgeService</strong>: Primary service layer integration</li>
 *   <li><strong>BadgeStatsService</strong>: Analytics and statistics generation</li>
 *   <li><strong>LabelService</strong>: Label association management</li>
 *   <li><strong>Time Tracking</strong>: Badge-based time aggregation support</li>
 * </ul>
 * 
 * <h2>Query Patterns</h2>
 * <p>Common access patterns supported:</p>
 * <ul>
 *   <li><strong>Ordered Badge Lists</strong>: User badge collections in display order</li>
 *   <li><strong>Unordered Collections</strong>: Complete badge sets for operations</li>
 *   <li><strong>Position Queries</strong>: Sort order calculations and validations</li>
 *   <li><strong>Existence Validation</strong>: Badge ownership and availability checks</li>
 * </ul>
 * 
 * <h2>Data Consistency</h2>
 * <ul>
 *   <li><strong>Referential Integrity</strong>: Maintain user-badge relationships</li>
 *   <li><strong>Order Consistency</strong>: Reliable sort order management</li>
 *   <li><strong>Label Associations</strong>: Consistent badge-label relationships</li>
 *   <li><strong>State Synchronization</strong>: Coordinate badge state across operations</li>
 * </ul>
 * 
 * <h2>Security Considerations</h2>
 * <p>Repository design supports comprehensive security:</p>
 * <ul>
 *   <li><strong>User Scoping</strong>: All queries naturally filter by user ownership</li>
 *   <li><strong>Privacy Protection</strong>: Prevent cross-user badge access</li>
 *   <li><strong>Service Layer Security</strong>: Authorization enforced at service layer</li>
 *   <li><strong>Data Isolation</strong>: Complete separation of user badge data</li>
 * </ul>
 * 
 * <h2>Important Notes</h2>
 * <ul>
 *   <li><strong>User Ownership</strong>: All badges are scoped to individual users</li>
 *   <li><strong>Sort Order Management</strong>: Maintain consistent ordering across operations</li>
 *   <li><strong>Label Integration</strong>: Consider badge-label relationships in operations</li>
 *   <li><strong>Performance Impact</strong>: Ordering queries should utilize appropriate indexes</li>
 * </ul>
 * 
 * @see Badge
 * @see BadgeService
 * @see BadgeStatsService
 * @see Label
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
public interface BadgeRepository extends JpaRepository<Badge, Long> {

    /**
     * Finds all badges for a user, ordered by sortOrder ascending.
     *
     * @param userId the ID of the badge owner
     * @return a list of badges in the saved display order
     */
    List<Badge> findByUserIdOrderBySortOrderAsc(Long userId);

    /**
     * Finds all badges for a user without ordering.
     * Used in reorder operations for ID validation and mapping.
     *
     * @param userId the badge owner
     * @return list of all badges
     */
    List<Badge> findByUserId(Long userId);

    /**
     * Finds the current maximum sortOrder value for a user's badges.
     * Used to place a new badge at the end.
     *
     * @param userId the badge owner
     * @return the max sortOrder, or empty if the user has no badges
     */
    @Query("SELECT MAX(b.sortOrder) FROM Badge b WHERE b.user.id = :userId")
    Optional<Integer> findMaxSortOrderByUserId(Long userId);
}
