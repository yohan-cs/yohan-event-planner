package com.yohan.event_planner.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * Data Transfer Object for creating recurring events with comprehensive schedule management.
 *
 * <p>This DTO enables the creation of recurring event patterns that automatically generate
 * virtual events based on specified recurrence rules. It supports complex scheduling scenarios
 * including indefinite recurrence, skip days, and draft states for iterative planning.</p>
 *
 * <h2>Recurring Event Fundamentals</h2>
 * <p>Recurring events work as templates that define:</p>
 * <ul>
 *   <li><strong>Schedule Pattern</strong>: When and how often events should occur</li>
 *   <li><strong>Time Boundaries</strong>: Daily start/end times for generated events</li>
 *   <li><strong>Date Range</strong>: Period during which the pattern is active</li>
 *   <li><strong>Exception Handling</strong>: Days to skip within the pattern</li>
 * </ul>
 *
 * <h2>Recurrence Rule Format</h2>
 * <p>The {@code recurrenceRule} follows RFC 5545 (iCalendar) RRULE syntax:</p>
 * <pre>{@code
 * // Daily recurrence
 * "FREQ=DAILY"
 * 
 * // Weekly on specific days
 * "FREQ=WEEKLY;BYDAY=MO,WE,FR"
 * 
 * // Monthly on specific day of month
 * "FREQ=MONTHLY;BYMONTHDAY=15"
 * 
 * // Every weekday
 * "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
 * 
 * // With count limit
 * "FREQ=DAILY;COUNT=10"
 * }</pre>
 *
 * <h2>Time Handling</h2>
 * <p>All times are interpreted in the creator's timezone:</p>
 * <ul>
 *   <li><strong>Local Times</strong>: startTime and endTime represent local daily schedule</li>
 *   <li><strong>UTC Storage</strong>: System converts to UTC using creator's timezone</li>
 *   <li><strong>Virtual Events</strong>: Generated events inherit time zone handling</li>
 *   <li><strong>Duration Calculation</strong>: Automatic duration computation in minutes</li>
 * </ul>
 *
 * <h2>Skip Days Management</h2>
 * <p>The {@code skipDays} set provides exception handling:</p>
 * <ul>
 *   <li><strong>Specific Dates</strong>: Exclude particular days from the pattern</li>
 *   <li><strong>Holidays</strong>: Skip work patterns on holiday dates</li>
 *   <li><strong>Vacation Periods</strong>: Temporary pattern interruptions</li>
 *   <li><strong>Flexible Exceptions</strong>: Add/remove skip days after creation</li>
 * </ul>
 *
 * <h2>Draft vs Confirmed States</h2>
 * <p>The {@code isDraft} flag controls visibility and behavior:</p>
 * <ul>
 *   <li><strong>Draft Mode</strong>: Pattern visible only to creator, allows experimentation</li>
 *   <li><strong>Confirmed Mode</strong>: Pattern active and generates visible virtual events</li>
 *   <li><strong>Transition</strong>: Drafts can be confirmed through separate endpoints</li>
 *   <li><strong>Planning</strong>: Drafts support iterative schedule development</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Daily morning workout
 * RecurringEventCreateDTO workout = new RecurringEventCreateDTO(
 *     "Morning Workout",
 *     LocalTime.of(7, 0),     // 7:00 AM
 *     LocalTime.of(8, 0),     // 8:00 AM
 *     LocalDate.of(2025, 1, 1),    // Start Jan 1st
 *     LocalDate.of(2025, 12, 31),  // End Dec 31st
 *     "Daily cardio and strength training",
 *     101L,                   // "Fitness" label ID
 *     "FREQ=DAILY",           // Every day
 *     Set.of(LocalDate.of(2025, 12, 25)), // Skip Christmas
 *     false                   // Confirmed pattern
 * );
 * 
 * // Weekly team meeting
 * RecurringEventCreateDTO meeting = new RecurringEventCreateDTO(
 *     "Team Standup",
 *     LocalTime.of(9, 0),     // 9:00 AM
 *     LocalTime.of(9, 30),    // 9:30 AM
 *     LocalDate.now(),        // Start today
 *     null,                   // Indefinite duration
 *     "Daily team synchronization",
 *     202L,                   // "Work" label ID
 *     "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", // Weekdays only
 *     Set.of(),               // No skip days initially
 *     true                    // Draft for review
 * );
 * }</pre>
 *
 * <h2>Validation and Processing</h2>
 * <p>The system validates and processes the recurring event:</p>
 * <ul>
 *   <li><strong>Time Validation</strong>: startTime must be before endTime</li>
 *   <li><strong>Date Validation</strong>: startDate must be before endDate (if provided)</li>
 *   <li><strong>Label Ownership</strong>: labelId must belong to the authenticated user</li>
 *   <li><strong>RRULE Parsing</strong>: recurrenceRule must be valid RFC 5545 format</li>
 *   <li><strong>Skip Day Validation</strong>: skipDays must fall within the date range</li>
 * </ul>
 *
 * @param name display name for the recurring event pattern
 * @param startTime daily start time in creator's timezone
 * @param endTime daily end time in creator's timezone
 * @param startDate first date the pattern becomes active
 * @param endDate last date the pattern is active (null for indefinite)
 * @param description optional detailed description of the recurring activity
 * @param labelId ID of the label to associate with generated events
 * @param recurrenceRule RFC 5545 RRULE string defining the recurrence pattern
 * @param skipDays set of specific dates to exclude from the pattern
 * @param isDraft true for draft patterns (creator-only), false for active patterns
 */
public record RecurringEventCreateDTO(
        String name,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        Long labelId,
        String recurrenceRule,
        Set<LocalDate> skipDays,
        boolean isDraft
) {}
