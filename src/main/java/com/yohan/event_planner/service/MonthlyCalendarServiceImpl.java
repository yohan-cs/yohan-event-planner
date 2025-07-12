package com.yohan.event_planner.service;

import com.yohan.event_planner.business.RecurringEventBO;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.LabelTimeBucket;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.LabelMonthStatsDTO;
import com.yohan.event_planner.exception.ErrorCode;
import com.yohan.event_planner.exception.InvalidCalendarParameterException;
import com.yohan.event_planner.exception.LabelNotFoundException;
import com.yohan.event_planner.repository.EventRepository;
import com.yohan.event_planner.repository.LabelRepository;
import com.yohan.event_planner.repository.LabelTimeBucketRepository;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import com.yohan.event_planner.service.ParsedRecurrenceInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(MonthlyCalendarServiceImpl.class);
    
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

    /**
     * {@inheritDoc}
     * 
     * <p>This implementation retrieves monthly statistics by combining data from label time buckets
     * and completed events. It handles timezone conversion to ensure accurate month boundaries and
     * applies defaults for null year/month parameters using the user's current timezone.</p>
     * 
     * <h3>Implementation Details</h3>
     * <ul>
     *   <li><strong>Timezone Handling</strong>: Converts month boundaries from user timezone to UTC for database queries</li>
     *   <li><strong>Default Values</strong>: Uses current year/month in user's timezone when parameters are null</li>
     *   <li><strong>Security</strong>: Validates label ownership before processing</li>
     *   <li><strong>Statistics Source</strong>: Combines LabelTimeBucket duration with completed event counts</li>
     * </ul>
     * 
     * @throws LabelNotFoundException if the specified label does not exist
     * @throws SecurityException if the user does not own the specified label
     */
    @Override
    public LabelMonthStatsDTO getMonthlyBucketStats(Long labelId, Integer year, Integer month) {
        logger.debug("getMonthlyBucketStats called with labelId={}, year={}, month={}", labelId, year, month);
        
        // Fetch the user's time zone first to get defaults if needed
        User viewer = authenticatedUserProvider.getCurrentUser();
        logger.info("Processing monthly bucket stats for user {} and label {}", viewer.getId(), labelId);
        ZoneId userZoneId = ZoneId.of(viewer.getTimezone());
        
        // Apply defaults and validate parameters
        int[] yearMonth = getYearMonthWithDefaults(year, month, userZoneId);
        year = yearMonth[0];
        month = yearMonth[1];
        
        validateMonthParameter(month, viewer.getId());

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> {
                    logger.warn("Label not found: {} for user: {}", labelId, viewer.getId());
                    return new LabelNotFoundException(labelId);
                });

        ownershipValidator.validateLabelOwnership(viewer.getId(), label);

        // Calculate month boundaries in UTC
        ZonedDateTime[] boundaries = getMonthBoundariesUtc(year, month, userZoneId);
        ZonedDateTime startOfMonthUtc = boundaries[0];
        ZonedDateTime endOfMonthUtc = boundaries[1];

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
        LabelMonthStatsDTO result = new LabelMonthStatsDTO(label.getName(), totalEvents, totalTimeSpent);
        logger.info("Retrieved monthly stats for label {}: {} events, {} minutes", labelId, totalEvents, totalTimeSpent);
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>This implementation retrieves completed events for the specified label within the given month,
     * handling timezone conversion to extract accurate date information. It processes both single-day
     * and multi-day events, ensuring all affected dates are included in the result.</p>
     * 
     * <h3>Implementation Details</h3>
     * <ul>
     *   <li><strong>Event Processing</strong>: Extracts dates from both start and end times of events</li>
     *   <li><strong>Timezone Conversion</strong>: Converts UTC event times to user timezone for accurate date extraction</li>
     *   <li><strong>Multi-day Support</strong>: Includes all dates spanned by events that cross multiple days</li>
     *   <li><strong>Deduplication</strong>: Ensures unique dates in the returned list</li>
     * </ul>
     * 
     * @throws LabelNotFoundException if the specified label does not exist
     * @throws SecurityException if the user does not own the specified label
     */
    @Override
    public List<LocalDate> getDatesByLabel(Long labelId, Integer year, Integer month) {
        logger.debug("getDatesByLabel called with labelId={}, year={}, month={}", labelId, year, month);
        
        // Fetch the user's time zone
        User viewer = authenticatedUserProvider.getCurrentUser();
        logger.debug("Current user: {}", viewer.getId());
        logger.info("Processing event dates for user {} and label {}", viewer.getId(), labelId);
        ZoneId userZoneId = ZoneId.of(viewer.getTimezone());
        
        // Apply defaults and validate parameters
        int[] yearMonth = getYearMonthWithDefaults(year, month, userZoneId);
        year = yearMonth[0];
        month = yearMonth[1];
        
        validateMonthParameter(month, viewer.getId());

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> {
                    logger.warn("Label not found: {} for user: {}", labelId, viewer.getId());
                    return new LabelNotFoundException(labelId);
                });
        logger.debug("Found label: {} owned by user: {}", label.getId(), label.getCreator().getId());

        ownershipValidator.validateLabelOwnership(viewer.getId(), label);
        logger.debug("Ownership validation passed");

        // Calculate month boundaries in UTC
        ZonedDateTime[] boundaries = getMonthBoundariesUtc(year, month, userZoneId);
        ZonedDateTime startOfMonthUtc = boundaries[0];
        ZonedDateTime endOfMonthUtc = boundaries[1];

        // Query the event repository to get events for the selected label and user in the given month
        List<Event> events = eventRepository.findByLabelIdAndEventDateBetweenAndIsCompleted(
                labelId,
                startOfMonthUtc,
                endOfMonthUtc
        );

        // Extract the unique event dates considering both startTime and endTime
        // Convert to user's timezone for proper date extraction
        List<LocalDate> eventDates = events.stream()
                .flatMap(event -> {
                    // Convert UTC times to user's timezone for proper date extraction
                    LocalDate startDate = event.getStartTime().withZoneSameInstant(userZoneId).toLocalDate();
                    LocalDate endDate = (event.getEndTime() != null) ? 
                        event.getEndTime().withZoneSameInstant(userZoneId).toLocalDate() : startDate;

                    // If start and end dates are different, return both dates; otherwise, return the start date only
                    return (startDate.equals(endDate))
                            ? Stream.of(startDate)
                            : Stream.of(startDate, endDate);
                })
                .distinct()  // Ensure unique dates
                .collect(Collectors.toList());

        logger.info("Retrieved {} event dates for label {} in {}/{}", eventDates.size(), labelId, year, month);
        return eventDates;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>This implementation combines dates from both scheduled events and recurring events to provide
     * a comprehensive monthly calendar view. It handles complex timezone scenarios, expands recurring
     * patterns into specific dates, and ensures efficient aggregation of multi-source event data.</p>
     * 
     * <h3>Implementation Details</h3>
     * <ul>
     *   <li><strong>Dual Source Processing</strong>: Combines confirmed scheduled events with recurring event patterns</li>
     *   <li><strong>Recurrence Expansion</strong>: Uses RecurrenceRuleService to determine actual occurrence dates</li>
     *   <li><strong>Multi-day Event Handling</strong>: Includes all dates spanned by events using date ranges</li>
     *   <li><strong>Performance Optimization</strong>: Uses efficient stream processing and set operations</li>
     *   <li><strong>Boundary Validation</strong>: Ensures recurring events respect their start/end date constraints</li>
     * </ul>
     * 
     * <h3>Data Sources</h3>
     * <ul>
     *   <li><strong>EventRepository</strong>: Retrieves confirmed scheduled events within month boundaries</li>
     *   <li><strong>RecurringEventBO</strong>: Provides recurring events that overlap with the specified month</li>
     *   <li><strong>RecurrenceRuleService</strong>: Evaluates recurrence patterns against specific dates</li>
     * </ul>
     */
    @Override
    public List<LocalDate> getDatesWithEventsByMonth(Integer year, Integer month) {
        User viewer = authenticatedUserProvider.getCurrentUser();
        ZoneId userZoneId = ZoneId.of(viewer.getTimezone());

        logger.debug("getDatesWithEventsByMonth called with year={}, month={}", year, month);
        logger.info("Processing monthly calendar for user {} for {}/{}", viewer.getId(), year, month);

        // Apply defaults and validate parameters
        int[] yearMonth = getYearMonthWithDefaults(year, month, userZoneId);
        year = yearMonth[0];
        month = yearMonth[1];
        
        logger.debug("Validating month parameter: {}", month);
        validateMonthParameter(month, viewer.getId());
        logger.debug("Month parameter is valid: {}", month);

        // Calculate month boundaries in UTC
        ZonedDateTime[] boundaries = getMonthBoundariesUtc(year, month, userZoneId);
        ZonedDateTime startOfMonthUtc = boundaries[0];
        ZonedDateTime endOfMonthUtc = boundaries[1];

        // Fetch and process scheduled events
        List<Event> events = eventRepository.findConfirmedEventsForUserBetween(
                viewer.getId(),
                startOfMonthUtc,
                endOfMonthUtc
        );
        Stream<LocalDate> scheduledEventDates = extractScheduledEventDates(events, userZoneId);

        // Fetch and process recurring events
        ZonedDateTime startOfMonthUser = LocalDate.of(year, month, 1).atStartOfDay(userZoneId);
        ZonedDateTime endOfMonthUser = LocalDate.of(year, month, 1)
                .withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth())
                .atTime(23, 59).atZone(userZoneId);
        
        List<RecurringEvent> recurringEvents = recurringEventBO.getConfirmedRecurringEventsForUserInRange(
                viewer.getId(),
                startOfMonthUser.toLocalDate(),
                endOfMonthUser.toLocalDate()
        );

        List<LocalDate> datesInMonth = startOfMonthUser.toLocalDate()
                .datesUntil(endOfMonthUser.toLocalDate().plusDays(1))
                .collect(Collectors.toList());
        
        Set<LocalDate> recurringEventDates = extractRecurringEventDates(recurringEvents, datesInMonth);

        // Combine and return results
        List<LocalDate> combinedDates = Stream.concat(scheduledEventDates, recurringEventDates.stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        
        // Count scheduled dates for logging (need to re-extract since stream was consumed)
        long scheduledCount = extractScheduledEventDates(events, userZoneId).count();
        
        logger.info("Combined calendar view for user {} in {}/{}: {} total dates ({} scheduled, {} recurring)", 
                viewer.getId(), year, month, combinedDates.size(), 
                scheduledCount, recurringEventDates.size());
        
        return combinedDates;
    }

    /**
     * Validates that the month parameter is within the valid range (1-12).
     * 
     * @param month The month value to validate
     * @param userId The user ID for logging context
     * @throws InvalidCalendarParameterException if month is not between 1 and 12
     */
    private void validateMonthParameter(Integer month, Long userId) {
        if (month < 1 || month > 12) {
            logger.warn("Invalid month parameter: {} for user: {}", month, userId);
            throw new InvalidCalendarParameterException(ErrorCode.INVALID_CALENDAR_PARAMETER);
        }
    }

    /**
     * Applies default values for null year and month parameters using the user's current timezone.
     * 
     * @param year The year parameter (may be null)
     * @param month The month parameter (may be null)
     * @param userZoneId The user's timezone for determining current values
     * @return An array containing [year, month] with defaults applied
     */
    private int[] getYearMonthWithDefaults(Integer year, Integer month, ZoneId userZoneId) {
        if (year == null || month == null) {
            logger.debug("Year or month is null, applying defaults");
            ZonedDateTime now = ZonedDateTime.now(userZoneId);
            year = (year != null) ? year : now.getYear();
            month = (month != null) ? month : now.getMonthValue();
            logger.debug("Applied defaults: year={}, month={}", year, month);
        }
        return new int[]{year, month};
    }

    /**
     * Calculates the UTC boundaries for the specified month in the user's timezone.
     * 
     * @param year The year of the month
     * @param month The month (1-12)
     * @param userZoneId The user's timezone
     * @return An array containing [startOfMonthUtc, endOfMonthUtc]
     */
    private ZonedDateTime[] getMonthBoundariesUtc(int year, int month, ZoneId userZoneId) {
        ZonedDateTime startOfMonthUser = LocalDate.of(year, month, 1).atStartOfDay(userZoneId);
        LocalDate lastDay = startOfMonthUser.toLocalDate()
                .withDayOfMonth(startOfMonthUser.toLocalDate().lengthOfMonth());
        ZonedDateTime endOfMonthUser = lastDay.atTime(23, 59).atZone(userZoneId);
        
        return new ZonedDateTime[]{
            startOfMonthUser.withZoneSameInstant(ZoneOffset.UTC),
            endOfMonthUser.withZoneSameInstant(ZoneOffset.UTC)
        };
    }

    /**
     * Extracts unique dates from a list of events, handling timezone conversion and multi-day events.
     * 
     * @param events The list of events to process
     * @param userZoneId The user's timezone for date extraction
     * @return A stream of unique LocalDate objects where events occur
     */
    private Stream<LocalDate> extractScheduledEventDates(List<Event> events, ZoneId userZoneId) {
        return events.stream()
                .flatMap(event -> {
                    LocalDate startDate = event.getStartTime().withZoneSameInstant(userZoneId).toLocalDate();
                    LocalDate endDate = event.getEndTime().withZoneSameInstant(userZoneId).toLocalDate();

                    // Return all dates from start to end (inclusive)
                    return startDate.datesUntil(endDate.plusDays(1));
                });
    }

    /**
     * Extracts dates from recurring events by evaluating recurrence rules against each date in the month.
     * 
     * @param recurringEvents The list of recurring events to process
     * @param datesInMonth All dates within the target month
     * @return A set of unique LocalDate objects where recurring events occur
     */
    private Set<LocalDate> extractRecurringEventDates(List<RecurringEvent> recurringEvents, List<LocalDate> datesInMonth) {
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
        return recurringEventDates;
    }

}
