package com.yohan.event_planner.dto;


import com.yohan.event_planner.domain.enums.RecapMediaType;

/**
 * Response DTO for a single recap media item.
 */
public record RecapMediaResponseDTO(
        Long id,
        String mediaUrl,
        RecapMediaType mediaType,
        Integer durationSeconds, // nullable for images
        Integer mediaOrder
) {}
