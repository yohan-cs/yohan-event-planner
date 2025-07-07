package com.yohan.event_planner.service;

import com.yohan.event_planner.dto.BadgeCreateDTO;
import com.yohan.event_planner.dto.BadgeResponseDTO;
import com.yohan.event_planner.dto.BadgeUpdateDTO;

import java.util.List;
import java.util.Set;

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
