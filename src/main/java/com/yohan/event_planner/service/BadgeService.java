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

    BadgeResponseDTO getBadgeById(Long badgeId);

    List<BadgeResponseDTO> getBadgesByUser(Long userId);

    BadgeResponseDTO createBadge(BadgeCreateDTO dto);

    BadgeResponseDTO updateBadge(Long badgeId, BadgeUpdateDTO dto);

    void deleteBadge(Long badgeId);

    void reorderBadges(Long userId, List<Long> orderedBadgeIds);

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
