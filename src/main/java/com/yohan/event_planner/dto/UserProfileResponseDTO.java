package com.yohan.event_planner.dto;

import java.util.List;

public record UserProfileResponseDTO(
        boolean isSelf,
        UserHeaderResponseDTO header,
        List<BadgeResponseDTO> badges
) {
}