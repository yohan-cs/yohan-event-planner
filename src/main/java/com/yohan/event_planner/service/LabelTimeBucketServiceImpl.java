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
