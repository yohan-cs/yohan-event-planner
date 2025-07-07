package com.yohan.event_planner.dto;

public record TimeStatsDTO(
        int minutesToday,
        int minutesThisWeek,
        int minutesThisMonth,
        int minutesLastWeek,
        int minutesLastMonth,
        long totalMinutesAllTime
) {}
