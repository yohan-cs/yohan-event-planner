package com.yohan.event_planner.dto;

import jakarta.validation.constraints.NotNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public record EventChangeContextDTO(
        @NotNull Long userId,
        Long oldLabelId,
        Long newLabelId,
        ZonedDateTime oldStartTime,
        ZonedDateTime newStartTime,
        Integer oldDurationMinutes,
        Integer newDurationMinutes,
        @NotNull ZoneId timezone,
        boolean wasCompleted,
        boolean isNowCompleted
) {}