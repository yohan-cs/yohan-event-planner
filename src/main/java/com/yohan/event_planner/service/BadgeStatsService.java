package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.Badge;
import com.yohan.event_planner.dto.TimeStatsDTO;

public interface BadgeStatsService {
    /**
     * Computes and returns the time-based statistics for the given badge and user.
     *
     * <p>Includes total time spent today, this week, this month, and custom date ranges.</p>
     *
     * @param badge the badge to compute stats for
     * @param userId the user who owns the badge
     * @return a {@link TimeStatsDTO} containing pre-aggregated time statistics
     */
    TimeStatsDTO computeStatsForBadge(Badge badge, Long userId);
}
