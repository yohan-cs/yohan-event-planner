package com.yohan.event_planner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record BadgeCreateDTO(

        @NotBlank(message = "Badge name must not be blank")
        @Size(max = 255, message = "Badge name must not exceed 255 characters")
        String name,
        Set<Long> labelIds
) {}
