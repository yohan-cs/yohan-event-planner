package com.yohan.event_planner.service;

import com.yohan.event_planner.dto.DayViewDTO;
import com.yohan.event_planner.dto.LabelMonthStatsDTO;

import java.time.LocalDate;
import java.util.List;

public interface MonthlyCalendarService {

    /**
     * Retrieves the aggregated stats for a specific label in a given month.
     *
     * @param labelId The ID of the label to filter events by.
     * @param year The year for the selected month.
     * @param month The month to fetch stats for.
     * @return A DTO containing the stats for the label in the specified month.
     */
    LabelMonthStatsDTO getMonthlyBucketStats(Long labelId, int year, int month);

    /**
     * Retrieves the dates for the selected label in a given month where the user has events.
     *
     * @param labelId The ID of the label to filter events by.
     * @param year The year for the selected month.
     * @param month The month to fetch the event dates for.
     * @return A list of LocalDate objects in the month where the user has events with the selected label.
     */
    List<LocalDate> getDatesByLabel(Long labelId, int year, int month);

    List<LocalDate> getDatesWithEventsByMonth(Integer year, Integer month);

}