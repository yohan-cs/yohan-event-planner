package com.yohan.event_planner.dto;

import java.util.List;

public record UserToolsResponseDTO(
        List<BadgeResponseDTO> badges,
        List<LabelResponseDTO> labels
) {}