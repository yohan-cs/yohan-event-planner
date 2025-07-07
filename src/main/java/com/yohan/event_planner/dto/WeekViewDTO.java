package com.yohan.event_planner.dto;

import java.util.List;

public record WeekViewDTO(
        List<DayViewDTO> days
) {}
