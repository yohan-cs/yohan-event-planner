package com.yohan.event_planner.dto;

import com.yohan.event_planner.domain.enums.RecapMediaType;

/**
 * Request DTO for updating an existing recap media item.
 */
public record RecapMediaUpdateDTO(
        String mediaUrl,
        RecapMediaType mediaType,
        Integer durationSeconds // nullable for images
) {}
