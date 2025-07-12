package com.yohan.event_planner.service;

import com.yohan.event_planner.dto.BadgeCreateDTO;
import com.yohan.event_planner.dto.BadgeResponseDTO;
import com.yohan.event_planner.dto.BadgeUpdateDTO;

import java.util.List;
import java.util.Set;

/**
 * Service interface for managing user-defined badges that group labels for enhanced analytics.
 * 
 * <p>Badges serve as higher-level organizational units that aggregate multiple labels to provide
 * comprehensive time tracking and goal monitoring capabilities. This service supports full CRUD
 * operations with advanced features including ordering management and statistical integration.</p>
 * 
 * <h2>Core Operations</h2>
 * <ul>
 *   <li><strong>CRUD Operations</strong>: Complete lifecycle management for badge entities</li>
 *   <li><strong>Label Grouping</strong>: Associate and manage multiple labels within badges</li>
 *   <li><strong>Ordering Systems</strong>: Maintain display order for badges and their labels</li>
 *   <li><strong>Analytics Integration</strong>: Support for aggregated time statistics</li>
 * </ul>
 * 
 * <h2>Badge Architecture</h2>
 * <p>Badges implement a sophisticated organization system:</p>
 * <ul>
 *   <li><strong>Multi-Label Collections</strong>: Group related labels for comprehensive tracking</li>
 *   <li><strong>Hierarchical Organization</strong>: Badges contain ordered collections of labels</li>
 *   <li><strong>Flexible Composition</strong>: Dynamic label association and management</li>
 *   <li><strong>Statistical Aggregation</strong>: Combined time tracking across all contained labels</li>
 * </ul>
 * 
 * <h2>Ordering and Display</h2>
 * <p>Comprehensive ordering management for optimal user experience:</p>
 * <ul>
 *   <li><strong>Badge Ordering</strong>: Control display sequence of user's badges</li>
 *   <li><strong>Label Ordering</strong>: Manage label order within each badge</li>
 *   <li><strong>Flexible Reordering</strong>: Support complete reordering operations</li>
 *   <li><strong>Consistency Validation</strong>: Ensure reorder operations include all entities</li>
 * </ul>
 * 
 * <h2>Security and Ownership</h2>
 * <p>Robust security model ensures data protection:</p>
 * <ul>
 *   <li><strong>User Isolation</strong>: Badges are scoped to individual users</li>
 *   <li><strong>Ownership Validation</strong>: Comprehensive ownership checks</li>
 *   <li><strong>Label Authorization</strong>: Ensure associated labels are owned by same user</li>
 *   <li><strong>Operation Security</strong>: Validate permissions for all operations</li>
 * </ul>
 * 
 * @see BadgeServiceImpl
 * @see com.yohan.event_planner.domain.Badge
 * @see com.yohan.event_planner.domain.Label
 * @see BadgeStatsService
 */
public interface BadgeService {

    /**
     * Retrieves a badge by its unique identifier with computed statistics and resolved labels.
     * 
     * <p>This method fetches the complete badge information including time statistics
     * aggregated across all associated labels and resolves label details for display.
     * The operation validates that the current authenticated user owns the requested badge.</p>
     * 
     * @param badgeId the unique identifier of the badge to retrieve
     * @return complete badge information with statistics and resolved labels
     * @throws BadgeNotFoundException if no badge exists with the specified ID
     * @throws BadgeOwnershipException if the current user doesn't own the badge
     */
    BadgeResponseDTO getBadgeById(Long badgeId);

    /**
     * Retrieves all badges belonging to a specific user, ordered by their sort order.
     * 
     * <p>Returns badges with computed statistics and resolved label information,
     * sorted according to the user's preferred display order. Each badge includes
     * aggregated time statistics computed across all its associated labels.</p>
     * 
     * @param userId the ID of the user whose badges to retrieve
     * @return list of badges with statistics, ordered by sort order (empty if none exist)
     */
    List<BadgeResponseDTO> getBadgesByUser(Long userId);

    /**
     * Creates a new badge with the specified properties and optional label associations.
     * 
     * <p>The new badge is assigned the next available sort order based on the user's
     * existing badges. If label IDs are provided, the method validates that all
     * specified labels are owned by the current authenticated user before creating
     * the associations.</p>
     * 
     * @param dto the badge creation data including name and optional label IDs
     * @return the created badge with computed statistics and resolved labels
     * @throws IllegalArgumentException if label validation fails or required data is invalid
     */
    BadgeResponseDTO createBadge(BadgeCreateDTO dto);

    /**
     * Updates an existing badge's properties.
     * 
     * <p>Currently supports updating the badge name. The operation validates ownership
     * before allowing updates and returns the badge with current statistics and
     * resolved label information.</p>
     * 
     * @param badgeId the ID of the badge to update
     * @param dto the update data containing modified properties
     * @return the updated badge with current statistics and resolved labels
     * @throws BadgeNotFoundException if no badge exists with the specified ID
     * @throws BadgeOwnershipException if the current user doesn't own the badge
     */
    BadgeResponseDTO updateBadge(Long badgeId, BadgeUpdateDTO dto);

    /**
     * Permanently deletes a badge after validating ownership.
     * 
     * <p>This operation removes the badge and all its associations but does not
     * affect the associated labels themselves. The deletion is permanent and
     * cannot be undone.</p>
     * 
     * @param badgeId the ID of the badge to delete
     * @throws BadgeNotFoundException if no badge exists with the specified ID
     * @throws BadgeOwnershipException if the current user doesn't own the badge
     */
    void deleteBadge(Long badgeId);

    /**
     * Reorders all badges for a user according to the provided sequence.
     * 
     * <p>This operation requires that ALL user-owned badges are included in the
     * reorder list to ensure consistency and prevent partial reordering. The position
     * in the list determines the new sort order, with index 0 being the first position.
     * Missing any user-owned badge or including badges not owned by the user will
     * result in an exception.</p>
     * 
     * @param userId the ID of the user whose badges to reorder
     * @param orderedBadgeIds complete list of badge IDs in desired order
     * @throws IncompleteBadgeReorderListException if not all user badges are included
     * @throws BadgeNotFoundException if any badge ID doesn't exist
     * @throws BadgeOwnershipException if any badge isn't owned by the user
     */
    void reorderBadges(Long userId, List<Long> orderedBadgeIds);

    /**
     * Reorders labels within a specific badge according to the provided sequence.
     * 
     * <p>This operation requires that ALL labels currently associated with the badge
     * are included in the reorder list to maintain consistency. The position in the
     * list determines the new display order within the badge. Missing any currently
     * associated label or including labels not associated with the badge will result
     * in an exception.</p>
     * 
     * @param badgeId the ID of the badge whose labels to reorder
     * @param labelOrder complete list of label IDs in desired order within the badge
     * @throws BadgeNotFoundException if the badge doesn't exist
     * @throws BadgeOwnershipException if the current user doesn't own the badge
     * @throws IncompleteBadgeLabelReorderListException if not all badge labels are included
     */
    void reorderBadgeLabels(Long badgeId, List<Long> labelOrder);


    /**
     * Validates that each badge ID exists and is owned by the specified user.
     *
     * <p>
     * This method is optimized to run in a single query using batch retrieval.
     * Throws {@code BadgeNotFoundException} or {@code BadgeOwnershipException} if invalid.
     * </p>
     *
     * @param badgeIds the set of badge IDs to validate
     * @param userId the ID of the authenticated user
     */
    void validateExistenceAndOwnership(Set<Long> badgeIds, Long userId);
}
