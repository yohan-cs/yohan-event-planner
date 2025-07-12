package com.yohan.event_planner.service;

import com.yohan.event_planner.dto.LabelMonthStatsDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for generating monthly calendar views and statistics.
 * 
 * <p>This service provides comprehensive monthly calendar functionality by aggregating events,
 * recurring patterns, and time statistics into unified views. It supports both label-specific
 * and global monthly views, enabling users to visualize their activities and time allocation
 * patterns across calendar months with detailed analytics integration.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>Monthly Statistics</strong>: Generate aggregated time statistics for calendar months</li>
 *   <li><strong>Event Aggregation</strong>: Combine regular and recurring events for monthly views</li>
 *   <li><strong>Date Identification</strong>: Identify specific dates with events for calendar highlighting</li>
 *   <li><strong>Label Integration</strong>: Support label-specific filtering and analytics</li>
 * </ul>
 * 
 * <h2>Calendar View Generation</h2>
 * <p>Comprehensive monthly calendar creation:</p>
 * <ul>
 *   <li><strong>Multi-source Integration</strong>: Combine events from regular and recurring sources</li>
 *   <li><strong>Time Bucket Integration</strong>: Include time statistics for enhanced views</li>
 *   <li><strong>Timezone Awareness</strong>: Handle user-specific timezone conversions</li>
 *   <li><strong>Historical Support</strong>: Generate views for past, present, and future months</li>
 * </ul>
 * 
 * <h2>Analytics Integration</h2>
 * <p>Rich statistical data for informed decision making:</p>
 * <ul>
 *   <li><strong>Label Statistics</strong>: Time tracking data organized by labels</li>
 *   <li><strong>Monthly Aggregation</strong>: Summarized data for entire calendar months</li>
 *   <li><strong>Goal Tracking</strong>: Support for progress monitoring and analysis</li>
 *   <li><strong>Trend Analysis</strong>: Historical data for pattern recognition</li>
 * </ul>
 * 
 * <h2>Filtering and Customization</h2>
 * <p>Flexible view customization options:</p>
 * <ul>
 *   <li><strong>Label-based Filtering</strong>: Focus on specific event categories</li>
 *   <li><strong>Date Range Specification</strong>: Precise month and year targeting</li>
 *   <li><strong>User-scoped Data</strong>: Automatic filtering by user ownership</li>
 *   <li><strong>Privacy Respect</strong>: Honor user privacy and visibility settings</li>
 * </ul>
 * 
 * @see MonthlyCalendarServiceImpl
 * @see LabelMonthStatsDTO
 * @see com.yohan.event_planner.domain.LabelTimeBucket
 * @see RecurringEventService
 */
public interface MonthlyCalendarService {

    /**
     * Retrieves the aggregated stats for a specific label in a given month.
     *
     * @param labelId The ID of the label to filter events by.
     * @param year The year for the selected month.
     * @param month The month to fetch stats for.
     * @return A DTO containing the stats for the label in the specified month.
     */
    LabelMonthStatsDTO getMonthlyBucketStats(Long labelId, Integer year, Integer month);

    /**
     * Retrieves the dates for the selected label in a given month where the user has events.
     *
     * @param labelId The ID of the label to filter events by.
     * @param year The year for the selected month.
     * @param month The month to fetch the event dates for.
     * @return A list of LocalDate objects in the month where the user has events with the selected label.
     */
    List<LocalDate> getDatesByLabel(Long labelId, Integer year, Integer month);

    /**
     * Retrieves all dates in a given month where the authenticated user has any events.
     * 
     * <p>This method combines dates from both regular scheduled events and recurring events
     * to provide a comprehensive view of all dates with user activity in the specified month.
     * It handles timezone conversion to ensure accurate date boundaries and supports both
     * confirmed scheduled events and expanded recurring event occurrences.</p>
     * 
     * <h3>Event Sources</h3>
     * <ul>
     *   <li><strong>Scheduled Events</strong>: Confirmed one-time and modified events</li>
     *   <li><strong>Recurring Events</strong>: Expanded occurrences from recurrence patterns</li>
     * </ul>
     * 
     * <h3>Date Handling</h3>
     * <ul>
     *   <li><strong>Multi-day Events</strong>: Includes all dates from start to end (inclusive)</li>
     *   <li><strong>Timezone Conversion</strong>: Converts UTC event times to user's timezone for accurate date extraction</li>
     *   <li><strong>Month Boundaries</strong>: Respects calendar month boundaries in user's timezone</li>
     *   <li><strong>Deduplication</strong>: Returns unique dates in chronological order</li>
     * </ul>
     * 
     * <h3>Default Behavior</h3>
     * <p>When year or month parameters are null, defaults to the current year/month
     * in the authenticated user's timezone.</p>
     * 
     * @param year The year for the selected month (null defaults to current year)
     * @param month The month to fetch event dates for (null defaults to current month)
     * @return A list of LocalDate objects representing all dates in the month where the user has events,
     *         sorted in chronological order with duplicates removed
     * @throws InvalidCalendarParameterException if month is not between 1 and 12
     * @see #getDatesByLabel(Long, Integer, Integer)
     * @see RecurringEventBO#getConfirmedRecurringEventsForUserInRange(Long, LocalDate, LocalDate)
     */
    List<LocalDate> getDatesWithEventsByMonth(Integer year, Integer month);

}