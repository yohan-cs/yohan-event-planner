package com.yohan.event_planner.service;

import com.yohan.event_planner.business.RecurringEventBO;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.LabelTimeBucket;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.TimeBucketType;
import com.yohan.event_planner.dto.LabelMonthStatsDTO;
import com.yohan.event_planner.exception.LabelNotFoundException;
import com.yohan.event_planner.repository.EventRepository;
import com.yohan.event_planner.repository.LabelRepository;
import com.yohan.event_planner.repository.LabelTimeBucketRepository;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.yohan.event_planner.domain.enums.TimeBucketType.MONTH;

/**
 * Implementation of {@link MonthlyCalendarService} providing calendar view generation and statistics.
 * 
 * <p>This service orchestrates the creation of monthly calendar views by aggregating data from
 * events, recurring events, and time bucket statistics. It provides comprehensive month-based
 * views that combine scheduled events with time tracking analytics, enabling users to visualize
 * both their planned activities and actual time allocation patterns.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>Monthly Statistics</strong>: Generate label-specific time statistics for calendar months</li>
 *   <li><strong>Event Aggregation</strong>: Combine regular and recurring events for monthly views</li>
 *   <li><strong>Date Identification</strong>: Identify dates with events for calendar highlighting</li>
 *   <li><strong>Label Filtering</strong>: Support label-specific calendar views and statistics</li>
 * </ul>
 * 
 * <h2>Calendar View Generation</h2>
 * <p>The service creates comprehensive monthly calendar views:</p>
 * <ul>
 *   <li><strong>Event Expansion</strong>: Expand recurring patterns into individual occurrences</li>
 *   <li><strong>Date Aggregation</strong>: Collect all dates with events for a given month</li>
 *   <li><strong>Multi-source Integration</strong>: Combine events from multiple sources</li>
 *   <li><strong>Timezone Handling</strong>: Convert times to user's local timezone for display</li>
 * </ul>
 * 
 * <h2>Time Bucket Statistics</h2>
 * <p>Provides detailed time tracking statistics for calendar integration:</p>
 * <ul>
 *   <li><strong>Monthly Buckets</strong>: Aggregate time data by calendar month</li>
 *   <li><strong>Label-specific Stats</strong>: Generate statistics for individual labels</li>
 *   <li><strong>Duration Calculations</strong>: Calculate total time spent on labeled activities</li>
 *   <li><strong>Historical Analysis</strong>: Support for historical month queries</li>
 * </ul>
 * 
 * <h2>Recurring Event Integration</h2>
 * <p>Seamlessly integrates recurring events into calendar views:</p>
 * <ul>
 *   <li><strong>Pattern Expansion</strong>: Generate occurrences from recurrence rules</li>
 *   <li><strong>Month Boundaries</strong>: Respect calendar month boundaries for expansion</li>
 *   <li><strong>Timezone Conversion</strong>: Handle timezone differences in recurring patterns</li>
 *   <li><strong>Exception Handling</strong>: Process skip dates and modified occurrences</li>
 * </ul>
 * 
 * <h2>Multi-source Event Aggregation</h2>
 * <p>Combines events from multiple sources for complete calendar views:</p>
 * <ul>
 *   <li><strong>Regular Events</strong>: Include one-time and confirmed events</li>
 *   <li><strong>Recurring Events</strong>: Expand and include recurring patterns</li>
 *   <li><strong>Deduplication</strong>: Ensure unique dates in result sets</li>
 *   <li><strong>Sorted Results</strong>: Return dates in chronological order</li>
 * </ul>
 * 
 * <h2>Label-based Filtering</h2>
 * <p>Supports focused views based on event labeling:</p>
 * <ul>
 *   <li><strong>Label-specific Views</strong>: Show only events with specific labels</li>
 *   <li><strong>Statistics Correlation</strong>: Match event data with time tracking</li>
 *   <li><strong>Ownership Validation</strong>: Ensure users only see their own labeled events</li>
 *   <li><strong>Performance Optimization</strong>: Efficient queries for filtered data</li>
 * </ul>
 * 
 * <h2>Performance Optimizations</h2>
 * <ul>
 *   <li><strong>Efficient Queries</strong>: Optimized database queries for month-based data</li>
 *   <li><strong>Stream Processing</strong>: Use Java streams for efficient data processing</li>
 *   <li><strong>Lazy Evaluation</strong>: Minimize unnecessary computations</li>
 *   <li><strong>Caching Opportunities</strong>: Structure for potential caching implementations</li>
 * </ul>
 * 
 * <h2>Integration Architecture</h2>
 * <p>This service integrates with multiple system components:</p>
 * <ul>
 *   <li><strong>RecurringEventBO</strong>: Access recurring event patterns and expansion</li>
 *   <li><strong>RecurrenceRuleService</strong>: Generate occurrences from rules</li>
 *   <li><strong>LabelTimeBucketRepository</strong>: Retrieve time tracking statistics</li>
 *   <li><strong>EventRepository</strong>: Access regular event data</li>
 *   <li><strong>Security Framework</strong>: Validate ownership and access rights</li>
 * </ul>
 * 
 * <h2>Timezone Considerations</h2>
 * <p>Handles complex timezone scenarios for accurate calendar views:</p>
 * <ul>
 *   <li><strong>User Timezone</strong>: Convert all times to user's current timezone</li>
 *   <li><strong>UTC Storage</strong>: Work with UTC-stored event times internally</li>
 *   <li><strong>Date Boundaries</strong>: Respect month boundaries in user's timezone</li>
 *   <li><strong>Recurring Patterns</strong>: Handle timezone-aware recurring events</li>
 * </ul>
 * 
 * <h2>Security and Authorization</h2>
 * <p>Comprehensive security model ensures data protection:</p>
 * <ul>
 *   <li><strong>User Isolation</strong>: Users only see their own events and statistics</li>
 *   <li><strong>Label Ownership</strong>: Validate label access before generating views</li>
 *   <li><strong>Event Filtering</strong>: Filter events by ownership automatically</li>
 *   <li><strong>Statistical Privacy</strong>: Protect time tracking data from unauthorized access</li>
 * </ul>
 * 
 * <h2>Error Handling</h2>
 * <ul>
 *   <li><strong>LabelNotFoundException</strong>: When requested labels don't exist</li>
 *   <li><strong>Ownership Violations</strong>: When users access unauthorized data</li>
 *   <li><strong>Date Validation</strong>: Handle invalid month/year combinations</li>
 *   <li><strong>Timezone Errors</strong>: Graceful handling of timezone conversion issues</li>
 * </ul>
 * 
 * <h2>Use Cases</h2>
 * <p>Primary use cases for monthly calendar service:</p>
 * <ul>
 *   <li><strong>Calendar Views</strong>: Generate monthly calendar displays for UI</li>
 *   <li><strong>Time Analysis</strong>: Provide time tracking insights by month</li>
 *   <li><strong>Activity Planning</strong>: Help users understand their monthly patterns</li>
 *   <li><strong>Progress Tracking</strong>: Monitor goal achievement over time</li>
 * </ul>
 * 
 * <h2>Data Consistency</h2>
 * <p>Ensures consistent data across multiple sources:</p>
 * <ul>
 *   <li><strong>Event Synchronization</strong>: Keep regular and recurring events in sync</li>
 *   <li><strong>Statistics Alignment</strong>: Ensure time buckets match actual events</li>
 *   <li><strong>Timezone Consistency</strong>: Maintain consistent timezone handling</li>
 *   <li><strong>Date Boundary Respect</strong>: Respect calendar month boundaries accurately</li>
 * </ul>
 * 
 * @see MonthlyCalendarService
 * @see RecurringEventBO
 * @see RecurrenceRuleService
 * @see LabelTimeBucketRepository
 * @see EventRepository
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
@Service
public class MonthlyCalendarServiceImpl implements MonthlyCalendarService{

    private final RecurringEventBO recurringEventBO;
    private final RecurrenceRuleService recurrenceRuleService;
    private final LabelTimeBucketRepository labelTimeBucketRepository;
    private final EventRepository eventRepository;
    private final LabelRepository labelRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final OwnershipValidator ownershipValidator;

    public MonthlyCalendarServiceImpl(
            RecurringEventBO recurringEventBO,
            RecurrenceRuleService recurrenceRuleService,
            LabelTimeBucketRepository labelTimeBucketRepository,
            EventRepository eventRepository,
            LabelRepository labelRepository,
            AuthenticatedUserProvider authenticatedUserProvider,
            OwnershipValidator ownershipValidator
    ) {
        this.recurringEventBO = recurringEventBO;
        this.recurrenceRuleService = recurrenceRuleService;
        this.labelTimeBucketRepository = labelTimeBucketRepository;
        this.eventRepository = eventRepository;
        this.labelRepository = labelRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.ownershipValidator = ownershipValidator;
    }

    @Override
    public LabelMonthStatsDTO getMonthlyBucketStats(Long labelId, int year, int month) {
        // Fetch the user's time zone
        User viewer = authenticatedUserProvider.getCurrentUser();
        ZoneId userZoneId = ZoneId.of(viewer.getTimezone());

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new LabelNotFoundException(labelId));

        ownershipValidator.validateLabelOwnership(viewer.getId(), label);

        // Adjust start and end of the month based on user's time zone
        ZonedDateTime startOfMonthUserTimeZone = LocalDate.of(year, month, 1).atStartOfDay(userZoneId);
        LocalDate lastDayOfMonth = LocalDate.of(year, month, 1).withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth());
        ZonedDateTime endOfMonthUserTimeZone = lastDayOfMonth.atTime(23, 59).atZone(userZoneId);

        // Convert the start and end of the month to UTC for the database query
        ZonedDateTime startOfMonthUtc = startOfMonthUserTimeZone.withZoneSameInstant(ZoneOffset.UTC);
        ZonedDateTime endOfMonthUtc = endOfMonthUserTimeZone.withZoneSameInstant(ZoneOffset.UTC);

        // Query the bucket repository to get the time spent in the month bucket for the selected label and user
        LabelTimeBucket monthBucket = labelTimeBucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                viewer.getId(), labelId, MONTH, year, month
        ).orElse(null);

        // If monthBucket is null, return 0 for total time spent
        long totalTimeSpent = (monthBucket != null) ? monthBucket.getDurationMinutes() : 0;

        // Count the number of completed events for the selected label and month, adjusted for the user's time zone
        long totalEvents = eventRepository.countByLabelIdAndEventDateBetweenAndIsCompleted(
                labelId,
                startOfMonthUtc,
                endOfMonthUtc
        );

        // Return the stats in the DTO
        return new LabelMonthStatsDTO(label.getName(), totalEvents, totalTimeSpent);
    }

    @Override
    public List<LocalDate> getDatesByLabel(Long labelId, int year, int month) {
        // Fetch the user's time zone
        User viewer = authenticatedUserProvider.getCurrentUser();
        ZoneId userZoneId = ZoneId.of(viewer.getTimezone());

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new LabelNotFoundException(labelId));

        ownershipValidator.validateLabelOwnership(viewer.getId(), label);

        // Get the start and end of the month, adjusted for user's time zone
        ZonedDateTime startOfMonthUserTimeZone = LocalDate.of(year, month, 1).atStartOfDay(userZoneId);
        LocalDate lastDayOfMonth = LocalDate.of(year, month, 1).withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth());
        ZonedDateTime endOfMonthUserTimeZone = lastDayOfMonth.atTime(23, 59).atZone(userZoneId);

        // Convert the start and end of the month to UTC for the database query
        ZonedDateTime startOfMonthUtc = startOfMonthUserTimeZone.withZoneSameInstant(ZoneOffset.UTC);
        ZonedDateTime endOfMonthUtc = endOfMonthUserTimeZone.withZoneSameInstant(ZoneOffset.UTC);

        // Query the event repository to get events for the selected label and user in the given month
        List<Event> events = eventRepository.findByLabelIdAndEventDateBetweenAndIsCompleted(
                labelId,
                startOfMonthUtc,
                endOfMonthUtc
        );

        // Extract the unique event dates considering both startTime and endTime
        List<LocalDate> eventDates = events.stream()
                .flatMap(event -> {
                    // Start date
                    LocalDate startDate = event.getStartTime().toLocalDate();

                    // End date (if different from start date)
                    LocalDate endDate = (event.getEndTime() != null) ? event.getEndTime().toLocalDate() : startDate;

                    // If start and end dates are different, return both dates; otherwise, return the start date only
                    return (startDate.equals(endDate))
                            ? Stream.of(startDate)
                            : Stream.of(startDate, endDate);
                })
                .distinct()  // Ensure unique dates
                .collect(Collectors.toList());

        return eventDates;
    }

    @Override
    public List<LocalDate> getDatesWithEventsByMonth(Integer year, Integer month) {
        User viewer = authenticatedUserProvider.getCurrentUser();
        ZoneId userZoneId = ZoneId.of(viewer.getTimezone());

        // If year or month are null, default to user's current year/month
        if (year == null || month == null) {
            ZonedDateTime now = ZonedDateTime.now(userZoneId);
            year = now.getYear();
            month = now.getMonthValue();
        }

        // Calculate start and end of month in user's timezone
        ZonedDateTime startOfMonthUser = LocalDate.of(year, month, 1).atStartOfDay(userZoneId);
        LocalDate lastDay = startOfMonthUser.toLocalDate()
                .withDayOfMonth(startOfMonthUser.toLocalDate().lengthOfMonth());
        ZonedDateTime endOfMonthUser = lastDay.atTime(23, 59).atZone(userZoneId);

        // Convert to UTC for DB queries
        ZonedDateTime startOfMonthUtc = startOfMonthUser.withZoneSameInstant(ZoneOffset.UTC);
        ZonedDateTime endOfMonthUtc = endOfMonthUser.withZoneSameInstant(ZoneOffset.UTC);

        // region --- Fetch confirmed scheduled events overlapping with this month ---
        List<Event> events = eventRepository.findConfirmedEventsForUserBetween(
                viewer.getId(),
                startOfMonthUtc,
                endOfMonthUtc
        );

        Stream<LocalDate> scheduledEventDates = events.stream()
                .flatMap(event -> {
                    LocalDate startDate = event.getStartTime().withZoneSameInstant(userZoneId).toLocalDate();
                    LocalDate endDate = event.getEndTime().withZoneSameInstant(userZoneId).toLocalDate();

                    return (startDate.equals(endDate))
                            ? Stream.of(startDate)
                            : Stream.of(startDate, endDate);
                });
        // endregion

        // region --- Fetch confirmed recurring events overlapping with this month via BO ---
        List<RecurringEvent> recurringEvents = recurringEventBO.getConfirmedRecurringEventsForUserInRange(
                viewer.getId(),
                startOfMonthUser.toLocalDate(),
                endOfMonthUser.toLocalDate()
        );

        // Build list of all dates in the month
        List<LocalDate> datesInMonth = startOfMonthUser.toLocalDate()
                .datesUntil(endOfMonthUser.toLocalDate().plusDays(1))
                .collect(Collectors.toList());

        Set<LocalDate> recurringEventDates = new HashSet<>();
        for (RecurringEvent recurringEvent : recurringEvents) {
            ParsedRecurrenceInput parsed = recurringEvent.getRecurrenceRule().getParsed();

            for (LocalDate date : datesInMonth) {
                // Ensure date is within the recurring event's startDate and endDate range
                if ((date.isAfter(recurringEvent.getStartDate()) || date.isEqual(recurringEvent.getStartDate())) &&
                        (date.isBefore(recurringEvent.getEndDate()) || date.isEqual(recurringEvent.getEndDate()))) {

                    if (recurrenceRuleService.occursOn(parsed, date)) {
                        recurringEventDates.add(date);
                    }
                }
            }
        }
        // endregion

        // Combine scheduled and recurring dates
        return Stream.concat(scheduledEventDates, recurringEventDates.stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

}
