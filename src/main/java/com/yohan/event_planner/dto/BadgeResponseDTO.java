package com.yohan.event_planner.dto;

import java.util.Set;

public record BadgeResponseDTO(
        Long id,
        String name,
        int sortOrder,
        TimeStatsDTO timeStats,
        Set<BadgeLabelDTO> labels
) {}