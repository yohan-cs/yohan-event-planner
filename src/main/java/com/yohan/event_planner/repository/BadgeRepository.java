package com.yohan.event_planner.repository;

import com.yohan.event_planner.domain.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

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
