package com.yohan.event_planner.dto;

/**
 * DTO representing monthly statistics for a specific label.
 *
 * @param labelName The name of the label.
 * @param totalEvents Total number of completed events for the label in the month.
 * @param totalTimeSpent Total time spent on events with this label (in minutes).
 */
public record LabelMonthStatsDTO(
        String labelName,       // The name of the label
        long totalEvents,       // Total number of completed events for the label
        long totalTimeSpent     // Total time spent on events (in minutes)

) {}
