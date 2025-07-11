package com.yohan.event_planner.repository;

import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for managing {@link Label} entities with specialized query support.
 * 
 * <p>This repository provides data access functionality for user-defined labels that categorize
 * events within the event planning system. It supports ownership-based queries, uniqueness
 * validation, and sorting operations essential for label management and event categorization.</p>
 * 
 * <h2>Core Query Categories</h2>
 * <ul>
 *   <li><strong>Ownership Queries</strong>: Retrieve labels scoped to specific users</li>
 *   <li><strong>Uniqueness Validation</strong>: Check label name uniqueness per user</li>
 *   <li><strong>Sorted Retrieval</strong>: Alphabetically ordered label collections</li>
 *   <li><strong>Exclusion Filtering</strong>: Query patterns that exclude specific labels</li>
 * </ul>
 * 
 * <h2>User-Scoped Label Management</h2>
 * <p>Labels are inherently user-specific with comprehensive ownership support:</p>
 * <ul>
 *   <li><strong>Creator-based Queries</strong>: All labels scoped to creating user</li>
 *   <li><strong>Privacy Isolation</strong>: No cross-user label access</li>
 *   <li><strong>Ownership Validation</strong>: Support for service-layer security</li>
 *   <li><strong>User Data Integrity</strong>: Maintain proper ownership relationships</li>
 * </ul>
 * 
 * <h2>Uniqueness and Validation</h2>
 * <p>Enforce business rules around label uniqueness:</p>
 * <ul>
 *   <li><strong>Per-User Uniqueness</strong>: Label names unique within user scope</li>
 *   <li><strong>Case-Sensitive Names</strong>: Exact string matching for names</li>
 *   <li><strong>Existence Validation</strong>: Efficient boolean queries for validation</li>
 *   <li><strong>Creation Support</strong>: Enable duplicate prevention at service layer</li>
 * </ul>
 * 
 * <h2>System Label Handling</h2>
 * <p>Special support for system-managed labels:</p>
 * <ul>
 *   <li><strong>Exclusion Patterns</strong>: Filter out system labels (e.g., "Unlabeled")</li>
 *   <li><strong>ID-based Exclusion</strong>: Exclude labels by specific IDs</li>
 *   <li><strong>Flexible Filtering</strong>: Support both inclusive and exclusive queries</li>
 *   <li><strong>Service Layer Support</strong>: Enable system label management</li>
 * </ul>
 * 
 * <h2>Sorting and Organization</h2>
 * <p>Consistent label presentation:</p>
 * <ul>
 *   <li><strong>Alphabetical Sorting</strong>: Labels sorted by name ascending</li>
 *   <li><strong>Consistent Ordering</strong>: Predictable label presentation</li>
 *   <li><strong>User Experience</strong>: Enhance usability through organization</li>
 *   <li><strong>Search Support</strong>: Enable efficient label discovery</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Indexed Queries</strong>: Utilize creator_id and name indexes</li>
 *   <li><strong>Efficient Sorting</strong>: Database-level sorting for performance</li>
 *   <li><strong>Existence Checks</strong>: Boolean queries without data transfer</li>
 *   <li><strong>Minimal Data Transfer</strong>: Optimized result sets</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This repository integrates with:</p>
 * <ul>
 *   <li><strong>LabelService</strong>: Primary service layer integration</li>
 *   <li><strong>EventService</strong>: Event categorization support</li>
 *   <li><strong>BadgeService</strong>: Label grouping for analytics</li>
 *   <li><strong>Time Tracking</strong>: Label-based time bucket queries</li>
 * </ul>
 * 
 * <h2>Query Patterns</h2>
 * <p>Common access patterns supported:</p>
 * <ul>
 *   <li><strong>User Label Lists</strong>: Retrieve all labels for a user</li>
 *   <li><strong>Validation Queries</strong>: Check name availability</li>
 *   <li><strong>Filtered Lists</strong>: Exclude system-managed labels</li>
 *   <li><strong>Sorted Collections</strong>: Organized label presentations</li>
 * </ul>
 * 
 * <h2>Data Consistency</h2>
 * <ul>
 *   <li><strong>Referential Integrity</strong>: Maintain creator relationships</li>
 *   <li><strong>Unique Constraints</strong>: Enforce (creator_id, name) uniqueness</li>
 *   <li><strong>Ownership Consistency</strong>: Reliable user-label associations</li>
 *   <li><strong>State Synchronization</strong>: Consistent label state across operations</li>
 * </ul>
 * 
 * <h2>Important Notes</h2>
 * <ul>
 *   <li><strong>User Scoping</strong>: All queries naturally filter by user ownership</li>
 *   <li><strong>Service Layer Security</strong>: Authorization enforced at service layer</li>
 *   <li><strong>System Labels</strong>: Special handling for system-managed entities</li>
 *   <li><strong>Case Sensitivity</strong>: Label names are case-sensitive</li>
 * </ul>
 * 
 * @see Label
 * @see LabelService
 * @see User
 * @see BadgeService
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
public interface LabelRepository extends JpaRepository<Label, Long> {

    // Method to fetch labels by creator's userId and sort by name in ascending order
    List<Label> findAllByCreatorIdOrderByNameAsc(Long userId);

    // Check if a label exists by name and creator
    boolean existsByNameAndCreator(String name, User creator);

    // Optional: If you want a method that filters out the "Unlabeled" label directly from DB query,
    // you can add a custom query here (though handling in service is fine):
    List<Label> findAllByCreatorIdAndIdNot(Long userId, Long excludedId);
}
