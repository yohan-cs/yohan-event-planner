package com.yohan.event_planner.dto;

import java.util.List;

public record MonthlyCalendarResponseDTO(
        List<String> eventDates,  // List of event dates (e.g., ["2025-07-05", "2025-07-12"])
        LabelMonthStatsDTO bucketMonthStats  // Monthly stats for the selected label
) {}
