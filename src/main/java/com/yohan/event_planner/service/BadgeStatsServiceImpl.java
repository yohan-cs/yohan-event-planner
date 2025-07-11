package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.Badge;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.enums.TimeBucketType;
import com.yohan.event_planner.dto.TimeStatsDTO;
import com.yohan.event_planner.repository.LabelTimeBucketRepository;
import com.yohan.event_planner.time.ClockProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.yohan.event_planner.domain.enums.TimeBucketType.DAY;
import static com.yohan.event_planner.domain.enums.TimeBucketType.MONTH;
import static com.yohan.event_planner.domain.enums.TimeBucketType.WEEK;

/**
 * Implementation of {@link BadgeStatsService} that computes time-based statistics for badges
 * by aggregating data from label time buckets.
 * 
 * <p>This service provides comprehensive time analytics for badges by:</p>
 * <ul>
 *   <li><strong>Multi-label aggregation</strong>: Combines time data from all labels in a badge</li>
 *   <li><strong>Multi-timeframe analysis</strong>: Calculates stats for today, week, month, and all-time</li>
 *   <li><strong>Timezone-aware calculations</strong>: Uses user-specific timezone for accurate day/week boundaries</li>
 *   <li><strong>Efficient querying</strong>: Leverages pre-aggregated time bucket data for performance</li>
 * </ul>
 * 
 * <h2>Time Bucket Integration</h2>
 * <p>The service builds upon the {@link com.yohan.event_planner.domain.LabelTimeBucket} aggregation system:</p>
 * <ul>
 *   <li>DAY buckets: For today's activity (YYYYMMDD format)</li>
 *   <li>WEEK buckets: For weekly activity (ISO week numbers)</li>
 *   <li>MONTH buckets: For monthly activity (calendar months)</li>
 * </ul>
 * 
 * <h2>Timezone Handling</h2>
 * <p>All time calculations respect the user's timezone:</p>
 * <ul>
 *   <li>Uses {@link ClockProvider} to get user-specific clock</li>
 *   <li>Ensures day/week boundaries align with user's local time</li>
 *   <li>Prevents timezone-related calculation errors</li>
 * </ul>
 * 
 * <h2>Week Calculation Details</h2>
 * <p>Week-based statistics use ISO 8601 week standards:</p>
 * <ul>
 *   <li>Weeks start on Monday</li>
 *   <li>Week-based year may differ from calendar year</li>
 *   <li>Ensures consistent weekly reporting across year boundaries</li>
 * </ul>
 * 
 * <h2>Performance Characteristics</h2>
 * <p>The service is optimized for efficient statistics calculation:</p>
 * <ul>
 *   <li>Single repository call per time period</li>
 *   <li>In-memory aggregation of bucket results</li>
 *   <li>No individual event processing required</li>
 *   <li>Scales with number of labels in badge, not number of events</li>
 * </ul>
 * 
 * @see BadgeStatsService
 * @see com.yohan.event_planner.domain.LabelTimeBucket
 * @see TimeStatsDTO
 * @see ClockProvider
 */
@Service
public class BadgeStatsServiceImpl implements BadgeStatsService {

    /** Repository for accessing pre-aggregated label time bucket data. */
    private final LabelTimeBucketRepository bucketRepository;
    
    /** Provider for user-specific clocks to ensure timezone-correct calculations. */
    private final ClockProvider clockProvider;

    /**
     * Creates a new BadgeStatsServiceImpl with the required dependencies.
     * 
     * @param bucketRepository repository for time bucket data access
     * @param clockProvider provider for user-specific timezone clocks
     */
    public BadgeStatsServiceImpl(LabelTimeBucketRepository bucketRepository, ClockProvider clockProvider) {
        this.bucketRepository = bucketRepository;
        this.clockProvider = clockProvider;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Implementation details:</p>
     * <ol>
     *   <li>Extracts all label IDs from the badge</li>
     *   <li>Calculates timezone-aware current date/time using user's clock</li>
     *   <li>Computes bucket identifiers for current and previous periods</li>
     *   <li>Queries time buckets for each relevant time period</li>
     *   <li>Aggregates bucket durations into comprehensive statistics</li>
     * </ol>
     * 
     * <p>Returns zero statistics if the badge contains no labels.</p>
     */
    @Override
    public TimeStatsDTO computeStatsForBadge(Badge badge, Long userId) {
        Set<Long> labelIds = badge.getLabelIds();

        // Early return for empty badges
        if (labelIds.isEmpty()) {
            return new TimeStatsDTO(0, 0, 0, 0, 0, 0);
        }

        // Get the current time in the badge user's timezone using ClockProvider
        ZonedDateTime now = ZonedDateTime.now(clockProvider.getClockForUser(badge.getUser()));
        LocalDate today = now.toLocalDate();

        // Calculate the day value (YYYYMMDD)
        int todayValue = today.getYear() * 10000 + today.getMonthValue() * 100 + today.getDayOfMonth();
        int todayYear = today.getYear();

        // Calculate week and month values
        WeekFields weekFields = WeekFields.ISO;

        LocalDate mondayThisWeek = today.with(weekFields.dayOfWeek(), 1);
        LocalDate mondayLastWeek = mondayThisWeek.minusWeeks(1);

        int thisWeekValue = (int) weekFields.weekOfWeekBasedYear().getFrom(mondayThisWeek);
        int thisWeekYear = (int) weekFields.weekBasedYear().getFrom(mondayThisWeek);

        int lastWeekValue = (int) weekFields.weekOfWeekBasedYear().getFrom(mondayLastWeek);
        int lastWeekYear = (int) weekFields.weekBasedYear().getFrom(mondayLastWeek);

        int thisMonthValue = today.getMonthValue();
        int thisMonthYear = today.getYear();

        int lastMonthValue = today.minusMonths(1).getMonthValue();
        int lastMonthYear = today.minusMonths(1).getYear();

        // Prepare bucket values
        List<Integer> dayBucketValues = List.of(todayValue);
        List<Integer> thisWeekBucketValues = List.of(thisWeekValue);
        List<Integer> lastWeekBucketValues = List.of(lastWeekValue);
        List<Integer> thisMonthBucketValues = List.of(thisMonthValue);
        List<Integer> lastMonthBucketValues = List.of(lastMonthValue);

        // Query buckets
        var dayBuckets = bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                userId, labelIds, DAY, todayYear, dayBucketValues
        );

        var weekBuckets = bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                userId, labelIds, WEEK, thisWeekYear, thisWeekBucketValues
        );

        var lastWeekBuckets = bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                userId, labelIds, WEEK, lastWeekYear, lastWeekBucketValues
        );

        var monthBuckets = bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                userId, labelIds, MONTH, thisMonthYear, thisMonthBucketValues
        );

        var lastMonthBuckets = bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                userId, labelIds, MONTH, lastMonthYear, lastMonthBucketValues
        );

        var allTimeBuckets = bucketRepository.findByUserIdAndLabelIdIn(userId, labelIds);

        // Aggregate
        int minutesToday = dayBuckets.stream().mapToInt(b -> b.getDurationMinutes()).sum();
        int minutesThisWeek = weekBuckets.stream().mapToInt(b -> b.getDurationMinutes()).sum();
        int minutesThisMonth = monthBuckets.stream().mapToInt(b -> b.getDurationMinutes()).sum();
        int minutesLastWeek = lastWeekBuckets.stream().mapToInt(b -> b.getDurationMinutes()).sum();
        int minutesLastMonth = lastMonthBuckets.stream().mapToInt(b -> b.getDurationMinutes()).sum();
        int totalMinutesAllTime = allTimeBuckets.stream().mapToInt(b -> b.getDurationMinutes()).sum();

        return new TimeStatsDTO(
                minutesToday,
                minutesThisWeek,
                minutesThisMonth,
                minutesLastWeek,
                minutesLastMonth,
                totalMinutesAllTime
        );
    }

}

