package com.yohan.event_planner.dto;


import com.yohan.event_planner.domain.enums.TimeFilter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record RecurringEventFilterDTO(

        @Positive
        Long labelId,

        @NotNull
        TimeFilter timeFilter,

        LocalDate startDate,

        LocalDate endDate,

        Boolean sortDescending

) {}