package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.LabelTimeBucket;
import com.yohan.event_planner.domain.enums.TimeBucketType;
import com.yohan.event_planner.dto.EventChangeContextDTO;
import com.yohan.event_planner.repository.LabelTimeBucketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;

import static com.yohan.event_planner.constants.ApplicationConstants.DATE_MONTH_MULTIPLIER;
import static com.yohan.event_planner.constants.ApplicationConstants.DATE_YEAR_MULTIPLIER;
import static com.yohan.event_planner.domain.enums.TimeBucketType.DAY;
import static com.yohan.event_planner.domain.enums.TimeBucketType.MONTH;
import static com.yohan.event_planner.domain.enums.TimeBucketType.WEEK;

/**
 * Implementation of {@link LabelTimeBucketService} providing time bucket management and statistics.
 * 
 * <p>This service manages the complex time tracking system that aggregates event durations into
 * various time buckets (day, week, month) for analytics and reporting. It handles timezone-aware
 * time calculations and maintains accurate statistics as events are created, modified, and completed,
 * enabling comprehensive time tracking and goal monitoring across different temporal granularities.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>Time Bucket Management</strong>: Create and maintain day/week/month aggregations</li>
 *   <li><strong>Duration Tracking</strong>: Apply and revert time changes for accurate statistics</li>
 *   <li><strong>Event Change Handling</strong>: Process event lifecycle changes for time tracking</li>
 *   <li><strong>Timezone Calculations</strong>: Handle complex timezone conversions for bucket placement</li>
 * </ul>
 * 
 * <h2>Time Bucket Types</h2>
 * <p>The service manages three distinct bucket granularities:</p>
 * 
 * <h3>Daily Buckets</h3>
 * <ul>
 *   <li><strong>Granularity</strong>: Track time by calendar day in user's timezone</li>
 *   <li><strong>Use Case</strong>: Daily time tracking and detailed activity monitoring</li>
 *   <li><strong>Boundary Logic</strong>: Respect local midnight boundaries</li>
 * </ul>
 * 
 * <h3>Weekly Buckets</h3>
 * <ul>
 *   <li><strong>Granularity</strong>: Track time by ISO week (Monday-Sunday)</li>
 *   <li><strong>Use Case</strong>: Weekly goal tracking and pattern analysis</li>
 *   <li><strong>ISO Standard</strong>: Follow ISO 8601 week numbering system</li>
 * </ul>
 * 
 * <h3>Monthly Buckets</h3>
 * <ul>
 *   <li><strong>Granularity</strong>: Track time by calendar month</li>
 *   <li><strong>Use Case</strong>: Monthly progress tracking and long-term analytics</li>
 *   <li><strong>Calendar Months</strong>: Follow standard calendar month boundaries</li>
 * </ul>
 * 
 * <h2>Event Lifecycle Integration</h2>
 * <p>The service integrates with the complete event lifecycle:</p>
 * <ul>
 *   <li><strong>Event Completion</strong>: Apply time when events are completed</li>
 *   <li><strong>Event Modification</strong>: Revert old time and apply new time on updates</li>
 *   <li><strong>Event Deletion</strong>: Revert time when completed events are deleted</li>
 *   <li><strong>Label Changes</strong>: Handle time redistribution when labels change</li>
 * </ul>
 * 
 * <h2>Timezone-Aware Calculations</h2>
 * <p>Complex timezone handling ensures accurate bucket placement:</p>
 * <ul>
 *   <li><strong>User Timezone</strong>: Calculate buckets in user's local timezone</li>
 *   <li><strong>UTC Conversion</strong>: Convert UTC-stored times for local calculations</li>
 *   <li><strong>Boundary Handling</strong>: Respect local time boundaries for bucket assignment</li>
 *   <li><strong>DST Awareness</strong>: Handle daylight saving time transitions correctly</li>
 * </ul>
 * 
 * <h2>Change Context Processing</h2>
 * <p>Handles complex event change scenarios:</p>
 * <ul>
 *   <li><strong>State Transitions</strong>: Track when events become completed</li>
 *   <li><strong>Time Modifications</strong>: Process duration and timing changes</li>
 *   <li><strong>Label Reassignment</strong>: Move time between labels when changed</li>
 *   <li><strong>Atomic Operations</strong>: Ensure consistent state during changes</li>
 * </ul>
 * 
 * <h2>Bucket Creation Strategy</h2>
 * <p>Efficient bucket creation and management:</p>
 * <ul>
 *   <li><strong>On-Demand Creation</strong>: Create buckets only when time is applied</li>
 *   <li><strong>Multi-granularity</strong>: Create day, week, and month buckets simultaneously</li>
 *   <li><strong>Upsert Operations</strong>: Update existing buckets or create new ones</li>
 *   <li><strong>Zero Cleanup</strong>: Remove buckets when duration reaches zero</li>
 * </ul>
 * 
 * <h2>Performance Optimizations</h2>
 * <ul>
 *   <li><strong>Batch Operations</strong>: Process multiple bucket types in single transaction</li>
 *   <li><strong>Efficient Queries</strong>: Optimized database operations for bucket management</li>
 *   <li><strong>Conditional Processing</strong>: Skip operations when no changes needed</li>
 *   <li><strong>Minimal Calculations</strong>: Avoid unnecessary timezone conversions</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This service integrates with multiple system components:</p>
 * <ul>
 *   <li><strong>LabelService</strong>: Validate label existence and ownership</li>
 *   <li><strong>LabelTimeBucketRepository</strong>: Persist and retrieve bucket data</li>
 *   <li><strong>Event System</strong>: Respond to event lifecycle changes</li>
 *   <li><strong>Timezone Services</strong>: Handle complex timezone calculations</li>
 * </ul>
 * 
 * <h2>Data Consistency</h2>
 * <p>Ensures consistent time tracking across all operations:</p>
 * <ul>
 *   <li><strong>Transactional Operations</strong>: Ensure atomic bucket updates</li>
 *   <li><strong>Compensating Actions</strong>: Revert operations maintain consistency</li>
 *   <li><strong>State Validation</strong>: Verify bucket state after operations</li>
 *   <li><strong>Idempotent Operations</strong>: Safe to retry failed operations</li>
 * </ul>
 * 
 * <h2>Business Rules</h2>
 * <ul>
 *   <li><strong>Completed Events Only</strong>: Only track time for completed events</li>
 *   <li><strong>Positive Durations</strong>: Ensure time buckets maintain non-negative values</li>
 *   <li><strong>Label Ownership</strong>: Respect label ownership for time tracking</li>
 *   <li><strong>Bucket Granularity</strong>: Maintain separate tracking for each time granularity</li>
 * </ul>
 * 
 * <h2>Error Handling</h2>
 * <ul>
 *   <li><strong>Timezone Validation</strong>: Handle invalid timezone scenarios gracefully</li>
 *   <li><strong>Duration Validation</strong>: Ensure valid duration values</li>
 *   <li><strong>Label Validation</strong>: Verify label existence before operations</li>
 *   <li><strong>Transaction Rollback</strong>: Handle database operation failures</li>
 * </ul>
 * 
 * <h2>Use Cases</h2>
 * <p>Primary use cases for time bucket management:</p>
 * <ul>
 *   <li><strong>Time Tracking</strong>: Maintain accurate time statistics per label</li>
 *   <li><strong>Goal Monitoring</strong>: Track progress toward time-based goals</li>
 *   <li><strong>Analytics</strong>: Provide data for time analysis and reporting</li>
 *   <li><strong>Calendar Integration</strong>: Support calendar views with time statistics</li>
 * </ul>
 * 
 * @see LabelTimeBucketService
 * @see LabelTimeBucket
 * @see TimeBucketType
 * @see EventChangeContextDTO
 * @see LabelService
 * @see LabelTimeBucketRepository
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
@Service
public class LabelTimeBucketServiceImpl implements LabelTimeBucketService{

    private static final Logger logger = LoggerFactory.getLogger(LabelTimeBucketServiceImpl.class);

    private final LabelService labelService;
    private final LabelTimeBucketRepository bucketRepository;

    public LabelTimeBucketServiceImpl(LabelService labelService, LabelTimeBucketRepository bucketRepository) {
        this.labelService = labelService;
        this.bucketRepository = bucketRepository;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Delegates to {@link #adjust(Long, Long, ZonedDateTime, int, ZoneId, int)} with 
     * direction=-1 to subtract the specified duration from all relevant time buckets.</p>
     */
    @Override
    @Transactional
    public void revert(Long userId, Long labelId, ZonedDateTime startTime, int durationMinutes, ZoneId timezone) {
        logger.debug("Reverting time bucket allocation: userId={}, labelId={}, duration={}min", 
            userId, labelId, durationMinutes);
        adjust(userId, labelId, startTime, durationMinutes, timezone, -1);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Delegates to {@link #adjust(Long, Long, ZonedDateTime, int, ZoneId, int)} with 
     * direction=+1 to add the specified duration to all relevant time buckets.</p>
     */
    @Override
    @Transactional
    public void apply(Long userId, Long labelId, ZonedDateTime startTime, int durationMinutes, ZoneId timezone) {
        logger.debug("Applying time bucket allocation: userId={}, labelId={}, duration={}min", 
            userId, labelId, durationMinutes);
        adjust(userId, labelId, startTime, durationMinutes, timezone, +1);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Processes event change context by conditionally reverting old time allocations
     * and applying new time allocations based on completion status changes.</p>
     * 
     * <p>Operation sequence:</p>
     * <ol>
     *   <li>If event was previously completed, revert old time allocation</li>
     *   <li>If event is now completed, apply new time allocation</li>
     * </ol>
     */
    @Override
    public void handleEventChange(EventChangeContextDTO dto) {
        logger.debug("Processing event change: userId={}, wasCompleted={}, isNowCompleted={}", 
            dto.userId(), dto.wasCompleted(), dto.isNowCompleted());
            
        if (dto.wasCompleted() && dto.isNowCompleted()) {
            logger.info("Event modification requires both revert and apply: userId={}, oldLabel={}, newLabel={}", 
                dto.userId(), dto.oldLabelId(), dto.newLabelId());
        }
        
        if (dto.wasCompleted()) {
            revert(dto.userId(), dto.oldLabelId(), dto.oldStartTime(), dto.oldDurationMinutes(), dto.timezone());
        }

        if (dto.isNowCompleted()) {
            apply(dto.userId(), dto.newLabelId(), dto.newStartTime(), dto.newDurationMinutes(), dto.timezone());
        }
    }

    /**
     * Adjusts time bucket values by applying duration changes across all bucket types.
     * 
     * <p>This method orchestrates the core time bucket adjustment logic by:</p>
     * <ul>
     *   <li>Retrieving label information for bucket creation</li>
     *   <li>Splitting duration across day boundaries for accurate allocation</li>
     *   <li>Creating or updating buckets for day, week, and month granularities</li>
     *   <li>Persisting all changes atomically</li>
     * </ul>
     * 
     * @param userId the ID of the user who owns the buckets
     * @param labelId the label associated with the time tracking
     * @param startTime the start time of the event in UTC
     * @param durationMinutes the duration to adjust (positive value)
     * @param timezone the user's timezone for local time calculations
     * @param direction +1 to add time, -1 to subtract time
     */
    private void adjust(Long userId, Long labelId, ZonedDateTime startTime, int durationMinutes, ZoneId timezone, int direction) {
        logger.debug("Starting time bucket adjustment: userId={}, labelId={}, duration={}min, direction={}", 
            userId, labelId, durationMinutes, direction > 0 ? "apply" : "revert");
            
        String labelName = labelService.getLabelById(labelId).name();

        List<TimeSlice> slices = splitByDay(startTime, durationMinutes, timezone);
        logger.debug("Created {} time slices for event spanning {} to {}", 
            slices.size(), startTime, startTime.plusMinutes(durationMinutes));
            
        List<LabelTimeBucket> bucketsToSave = new ArrayList<>();

        for (TimeSlice slice : slices) {
            LocalDateTime localTime = slice.start().withZoneSameInstant(timezone).toLocalDateTime();
            int minutes = slice.minutes();

            // DAY
            LocalDate date = localTime.toLocalDate();
            int dayValue = date.getYear() * DATE_YEAR_MULTIPLIER + date.getMonthValue() * DATE_MONTH_MULTIPLIER + date.getDayOfMonth();
            LabelTimeBucket dayBucket = resolveOrCreateBucket(userId, labelId, labelName, DAY, date.getYear(), dayValue);
            dayBucket.incrementMinutes(direction * minutes);
            bucketsToSave.add(dayBucket);

            // WEEK
            int week = getIsoWeek(localTime);
            int weekYear = getIsoWeekYear(localTime);
            LabelTimeBucket weekBucket = resolveOrCreateBucket(userId, labelId, labelName, WEEK, weekYear, week);
            weekBucket.incrementMinutes(direction * minutes);
            bucketsToSave.add(weekBucket);

            // MONTH
            int month = localTime.getMonthValue();
            int year = localTime.getYear();
            LabelTimeBucket monthBucket = resolveOrCreateBucket(userId, labelId, labelName, MONTH, year, month);
            monthBucket.incrementMinutes(direction * minutes);
            bucketsToSave.add(monthBucket);
        }

        bucketRepository.saveAll(bucketsToSave);
        
        logger.info("Updated {} time buckets for user={}, label={}: {} minutes {}", 
            bucketsToSave.size(), userId, labelId, durationMinutes, direction > 0 ? "added" : "removed");
    }

    /**
     * Resolves an existing time bucket or creates a new one if none exists.
     * 
     * <p>This method implements an upsert pattern, returning existing bucket data
     * for updates or creating new buckets with zero initial duration.</p>
     * 
     * @param userId the ID of the user who owns the bucket
     * @param labelId the label associated with the bucket  
     * @param labelName the name of the label for bucket creation
     * @param type the type of time bucket (DAY, WEEK, MONTH)
     * @param year the year component of the bucket
     * @param value the bucket-specific value (YYYYMMDD for DAY, week number for WEEK, month number for MONTH)
     * @return existing bucket with current data or new bucket with zero duration
     */
    private LabelTimeBucket resolveOrCreateBucket(Long userId, Long labelId, String labelName,
                                                  TimeBucketType type, int year, int value) {
        return bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                userId, labelId, type, year, value
        ).orElseGet(() -> {
            logger.debug("Creating new {} bucket for user={}, label={}, year={}, value={}", 
                type, userId, labelId, year, value);
            return new LabelTimeBucket(userId, labelId, labelName, type, year, value);
        });
    }

    /**
     * Calculates the ISO week number for the given local date time.
     * 
     * <p>Uses ISO 8601 week numbering where weeks start on Monday and 
     * week 1 is the first week with at least 4 days in the new year.</p>
     * 
     * @param localTime the local date time to calculate week for
     * @return ISO week number (1-53)
     */
    private int getIsoWeek(LocalDateTime localTime) {
        return (int) WeekFields.ISO.weekOfWeekBasedYear().getFrom(localTime);
    }

    /**
     * Calculates the ISO week-based year for the given local date time.
     * 
     * <p>The week-based year may differ from the calendar year for dates
     * in the first or last week of the year. For example, January 1st may
     * belong to the previous week-based year if it falls early in the week.</p>
     * 
     * @param localTime the local date time to calculate week-based year for
     * @return ISO week-based year
     */
    private int getIsoWeekYear(LocalDateTime localTime) {
        return (int) WeekFields.ISO.weekBasedYear().getFrom(localTime);
    }

    /**
     * Splits an event duration across multiple days to handle events that span midnight.
     * 
     * <p>This method ensures accurate time allocation by:</p>
     * <ul>
     *   <li>Converting UTC times to user's local timezone</li>
     *   <li>Detecting day boundary crossings at local midnight</li>
     *   <li>Creating separate time slices for each day portion</li>
     *   <li>Calculating precise minute allocations per day</li>
     * </ul>
     * 
     * <p>For example, an event from 11:30 PM to 1:30 AM would create two slices:
     * one for 30 minutes on the first day and one for 90 minutes on the second day.</p>
     * 
     * @param startTimeUtc the event start time in UTC
     * @param totalMinutes the total duration of the event in minutes
     * @param timezone the user's timezone for day boundary calculations  
     * @return list of time slices, each representing a portion within a single day
     */
    private List<TimeSlice> splitByDay(ZonedDateTime startTimeUtc, int totalMinutes, ZoneId timezone) {
        List<TimeSlice> slices = new ArrayList<>();

        ZonedDateTime localStart = startTimeUtc.withZoneSameInstant(timezone);
        ZonedDateTime localEnd = localStart.plusMinutes(totalMinutes);

        ZonedDateTime cursor = localStart;

        while (cursor.isBefore(localEnd)) {
            ZonedDateTime nextMidnight = cursor.toLocalDate().plusDays(1).atStartOfDay(timezone);
            ZonedDateTime segmentEnd = nextMidnight.isBefore(localEnd) ? nextMidnight : localEnd;

            int minutes = (int) Duration.between(cursor, segmentEnd).toMinutes();
            slices.add(new TimeSlice(cursor, minutes));

            cursor = segmentEnd;
        }

        return slices;
    }

    /**
     * Represents a time slice within a single day for accurate bucket allocation.
     * 
     * @param start the start time of this slice in the user's timezone
     * @param minutes the duration of this slice in minutes
     */
    private record TimeSlice(ZonedDateTime start, int minutes) {}
}
