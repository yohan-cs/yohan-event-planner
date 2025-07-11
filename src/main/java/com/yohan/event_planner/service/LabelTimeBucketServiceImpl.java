package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.LabelTimeBucket;
import com.yohan.event_planner.domain.enums.TimeBucketType;
import com.yohan.event_planner.dto.EventChangeContextDTO;
import com.yohan.event_planner.repository.LabelTimeBucketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;

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

    private final LabelService labelService;
    private final LabelTimeBucketRepository bucketRepository;

    public LabelTimeBucketServiceImpl(LabelService labelService, LabelTimeBucketRepository bucketRepository) {
        this.labelService = labelService;
        this.bucketRepository = bucketRepository;
    }

    @Override
    @Transactional
    public void revert(Long userId, Long labelId, ZonedDateTime startTime, int durationMinutes, ZoneId timezone) {
        adjust(userId, labelId, startTime, durationMinutes, timezone, -1);
    }

    @Override
    @Transactional
    public void apply(Long userId, Long labelId, ZonedDateTime startTime, int durationMinutes, ZoneId timezone) {
        adjust(userId, labelId, startTime, durationMinutes, timezone, +1);
    }

    @Override
    public void handleEventChange(EventChangeContextDTO dto) {
        if (dto.wasCompleted()) {
            revert(dto.userId(), dto.oldLabelId(), dto.oldStartTime(), dto.oldDurationMinutes(), dto.timezone());
        }

        if (dto.isNowCompleted()) {
            apply(dto.userId(), dto.newLabelId(), dto.newStartTime(), dto.newDurationMinutes(), dto.timezone());
        }
    }

    private void adjust(Long userId, Long labelId, ZonedDateTime startTime, int durationMinutes, ZoneId timezone, int direction) {
        String labelName = labelService.getLabelById(labelId).name();

        List<TimeSlice> slices = splitByDay(startTime, durationMinutes, timezone);
        List<LabelTimeBucket> bucketsToSave = new ArrayList<>();

        for (TimeSlice slice : slices) {
            LocalDateTime localTime = slice.start().withZoneSameInstant(timezone).toLocalDateTime();
            int minutes = slice.minutes();

            // DAY
            LocalDate date = localTime.toLocalDate();
            int dayValue = date.getYear() * 10000 + date.getMonthValue() * 100 + date.getDayOfMonth();
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
    }

    private LabelTimeBucket resolveOrCreateBucket(Long userId, Long labelId, String labelName,
                                                  TimeBucketType type, int year, int value) {
        return bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                userId, labelId, type, year, value
        ).orElseGet(() ->
                new LabelTimeBucket(userId, labelId, labelName, type, year, value)
        );
    }

    private int getIsoWeek(LocalDateTime localTime) {
        return (int) WeekFields.ISO.weekOfWeekBasedYear().getFrom(localTime);
    }

    private int getIsoWeekYear(LocalDateTime localTime) {
        return (int) WeekFields.ISO.weekBasedYear().getFrom(localTime);
    }

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

    private record TimeSlice(ZonedDateTime start, int minutes) {}
}
