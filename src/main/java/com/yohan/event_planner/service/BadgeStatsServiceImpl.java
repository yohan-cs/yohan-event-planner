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

@Service
public class BadgeStatsServiceImpl implements BadgeStatsService {

    private final LabelTimeBucketRepository bucketRepository;
    private final ClockProvider clockProvider;

    public BadgeStatsServiceImpl(LabelTimeBucketRepository bucketRepository, ClockProvider clockProvider) {
        this.bucketRepository = bucketRepository;
        this.clockProvider = clockProvider;
    }

    @Override
    public TimeStatsDTO computeStatsForBadge(Badge badge, Long userId) {
        Set<Long> labelIds = badge.getLabelIds();

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

