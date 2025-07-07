package com.yohan.event_planner.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record BadgeCreateDTO(

        @NotBlank(message = "Badge name must not be blank")
        String name,
        Set<Long> labelIds
) {}
