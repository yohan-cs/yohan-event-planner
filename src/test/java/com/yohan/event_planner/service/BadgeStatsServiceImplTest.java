package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.enums.TimeBucketType;
import com.yohan.event_planner.repository.LabelTimeBucketRepository;
import com.yohan.event_planner.time.ClockProvider;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static com.yohan.event_planner.util.TestConstants.USER_ID;
import static com.yohan.event_planner.util.TestConstants.VALID_LABEL_ID;
import static com.yohan.event_planner.util.TestConstants.VALID_LABEL_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class BadgeStatsServiceImplTest {

    private LabelTimeBucketRepository bucketRepository;
    private ClockProvider clockProvider;
    private Clock fixedClock;

    private BadgeStatsServiceImpl badgeStatsService;

    @BeforeEach
    void setUp() {
        bucketRepository = mock(LabelTimeBucketRepository.class);
        clockProvider = mock(ClockProvider.class);

        fixedClock = Clock.fixed(Instant.parse("2025-06-16T12:00:00Z"), ZoneId.of("UTC"));

        badgeStatsService = new BadgeStatsServiceImpl(bucketRepository, clockProvider);
    }

    private void mockTimeBuckets(
            int dayMinutes,
            int thisWeekMinutes,
            int lastWeekMinutes,
            int thisMonthMinutes,
            int lastMonthMinutes,
            int totalAllTimeMinutes
    ) {
        when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                anyLong(), anySet(), eq(TimeBucketType.DAY), eq(2025), eq(List.of(20250616))))
                .thenReturn(List.of(TestUtils.createValidDayBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME,2025, 20250616, dayMinutes)));

        when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                anyLong(), anySet(), eq(TimeBucketType.WEEK), eq(2025), eq(List.of(25))))
                .thenReturn(List.of(TestUtils.createValidWeekBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME,2025, 25, thisWeekMinutes)));

        when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                anyLong(), anySet(), eq(TimeBucketType.WEEK), eq(2025), eq(List.of(24))))
                .thenReturn(List.of(TestUtils.createValidWeekBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME,2025, 24, lastWeekMinutes)));

        when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                anyLong(), anySet(), eq(TimeBucketType.MONTH), eq(2025), eq(List.of(6))))
                .thenReturn(List.of(TestUtils.createValidMonthBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME,2025, 6, thisMonthMinutes)));

        when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                anyLong(), anySet(), eq(TimeBucketType.MONTH), eq(2025), eq(List.of(5))))
                .thenReturn(List.of(TestUtils.createValidMonthBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME,2025, 5, lastMonthMinutes)));

        // Independent total minutes (may overlap with above or not)
        when(bucketRepository.findByUserIdAndLabelIdIn(USER_ID, Set.of(VALID_LABEL_ID)))
                .thenReturn(List.of(TestUtils.createValidDayBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME,2025, 99999999, totalAllTimeMinutes)));
    }

    @Nested
    class ComputeStatsForBadgeTests {

        @Test
        void testComputeStatsForBadge_withAllBuckets_returnsCorrectStats() {
            // Arrange
            ZonedDateTime fixedNow = ZonedDateTime.now(fixedClock);

            when(clockProvider.getClockForUser(any())).thenReturn(fixedClock);

            int dayMinutes = 30;
            int thisWeekMinutes = 120;
            int lastWeekMinutes = 90;
            int thisMonthMinutes = 200;
            int lastMonthMinutes = 150;
            int totalAllTimeMinutes = 700;

            mockTimeBuckets(dayMinutes, thisWeekMinutes, lastWeekMinutes, thisMonthMinutes, lastMonthMinutes, totalAllTimeMinutes);

            var user = TestUtils.createValidUserEntityWithId(USER_ID);
            var badge = TestUtils.createValidBadgeWithLabelIds(user, Set.of(VALID_LABEL_ID));

            // Act
            var stats = badgeStatsService.computeStatsForBadge(badge, USER_ID);

            // Assert
            assertEquals(dayMinutes, stats.minutesToday());
            assertEquals(thisWeekMinutes, stats.minutesThisWeek());
            assertEquals(thisMonthMinutes, stats.minutesThisMonth());
            assertEquals(lastWeekMinutes, stats.minutesLastWeek());
            assertEquals(lastMonthMinutes, stats.minutesLastMonth());
            assertEquals(totalAllTimeMinutes, stats.totalMinutesAllTime());
        }

        @Test
        void testComputeStatsForBadge_withLabelsAndCategories_returnsCorrectStats() {
            // Arrange
            ZonedDateTime fixedNow = ZonedDateTime.now(fixedClock);
            when(clockProvider.getClockForUser(any())).thenReturn(fixedClock);

            int dayMinutes = 25;
            int thisWeekMinutes = 100;
            int lastWeekMinutes = 80;
            int thisMonthMinutes = 160;
            int lastMonthMinutes = 140;
            int totalAllTimeMinutes = 550;

            mockTimeBuckets(dayMinutes, thisWeekMinutes, lastWeekMinutes, thisMonthMinutes, lastMonthMinutes, totalAllTimeMinutes);

            var user = TestUtils.createValidUserEntityWithId(USER_ID);

            // VALID_LABEL_ID from labelIds
            var badge = TestUtils.createValidBadgeWithLabelIds(user, Set.of(VALID_LABEL_ID));
            var resolvedLabel = TestUtils.createValidLabelWithId(VALID_LABEL_ID, user); // same ID as direct label

            // Act
            var stats = badgeStatsService.computeStatsForBadge(badge, USER_ID);

            // Assert
            assertEquals(dayMinutes, stats.minutesToday());
            assertEquals(thisWeekMinutes, stats.minutesThisWeek());
            assertEquals(thisMonthMinutes, stats.minutesThisMonth());
            assertEquals(lastWeekMinutes, stats.minutesLastWeek());
            assertEquals(lastMonthMinutes, stats.minutesLastMonth());
            assertEquals(totalAllTimeMinutes, stats.totalMinutesAllTime());
        }

        @Test
        void testComputeStatsForBadge_withNoLabelsOrCategories_returnsEmptyStats() {
            // Arrange
            var user = TestUtils.createValidUserEntityWithId(USER_ID);
            var badge = TestUtils.createEmptyBadge(user, "Empty");

            when(clockProvider.getClockForUser(any())).thenReturn(fixedClock);

            // Act
            var stats = badgeStatsService.computeStatsForBadge(badge, USER_ID);

            // Assert
            assertEquals(TestUtils.createEmptyTimeStatsDTO(), stats);
        }

        @Test
        void testComputeStatsForBadge_withOverlappingLabelIds_doesNotDuplicateMinutes() {
            // Arrange
            ZonedDateTime fixedNow = ZonedDateTime.now(fixedClock);
            when(clockProvider.getClockForUser(any())).thenReturn(fixedClock);

            var user = TestUtils.createValidUserEntityWithId(USER_ID);
            var directLabelId = VALID_LABEL_ID;
            var badge = TestUtils.createValidBadgeWithLabelIds(user, Set.of(directLabelId));

            int totalMinutes = 50;
            mockTimeBuckets(totalMinutes, 0, 0, 0, 0, totalMinutes);

            // Act
            var stats = badgeStatsService.computeStatsForBadge(badge, USER_ID);

            // Assert
            assertEquals(totalMinutes, stats.minutesToday());
            assertEquals(totalMinutes, stats.totalMinutesAllTime());
        }

        @Test
        void testComputeStatsForBadge_withIsoWeekYearOverlap_correctlyHandlesWeekYear() {
            // Arrange
            int thisWeekMinutes = 100;

            // Simulate Jan 1, 2025 which belongs to ISO week 1 of 2025
            Clock janFirstClock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneId.of("UTC"));
            ZonedDateTime fixedNow = ZonedDateTime.now(janFirstClock);
            when(clockProvider.getClockForUser(any())).thenReturn(janFirstClock);

            int isoWeekValue = 1;
            int isoWeekYear = 2025;
            int todayYear = 2025;
            int todayValue = 20250101;
            int thisMonthValue = 1;
            int lastMonthValue = 12;
            int lastMonthYear = 2024;

            var user = TestUtils.createValidUserEntityWithId(USER_ID);
            var badge = TestUtils.createValidBadgeWithLabelIds(user, Set.of(VALID_LABEL_ID));

            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.DAY, todayYear, List.of(todayValue)))
                    .thenReturn(List.of());

            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.WEEK, isoWeekYear, List.of(isoWeekValue)))
                    .thenReturn(List.of(
                            TestUtils.createValidWeekBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME, isoWeekYear, isoWeekValue, thisWeekMinutes)
                    ));

            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.WEEK, isoWeekYear, List.of(isoWeekValue - 1)))
                    .thenReturn(List.of());

            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.MONTH, todayYear, List.of(thisMonthValue)))
                    .thenReturn(List.of());

            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.MONTH, lastMonthYear, List.of(lastMonthValue)))
                    .thenReturn(List.of());

            when(bucketRepository.findByUserIdAndLabelIdIn(USER_ID, Set.of(VALID_LABEL_ID)))
                    .thenReturn(List.of(
                            TestUtils.createValidWeekBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME, isoWeekYear, isoWeekValue, thisWeekMinutes)
                    ));

            // Act
            var stats = badgeStatsService.computeStatsForBadge(badge, USER_ID);

            // Assert
            assertEquals(0, stats.minutesToday());
            assertEquals(thisWeekMinutes, stats.minutesThisWeek());
            assertEquals(0, stats.minutesThisMonth());
            assertEquals(0, stats.minutesLastWeek());
            assertEquals(0, stats.minutesLastMonth());
            assertEquals(thisWeekMinutes, stats.totalMinutesAllTime());
        }

        @Test
        void testComputeStatsForBadge_withDec31Week1OfNextYear_correctlyHandlesWeekYear() {
            // Arrange
            int thisWeekMinutes = 200;

            // Simulate Dec 31, 2018 which is ISO week 1 of 2019
            Clock dec31Clock = Clock.fixed(Instant.parse("2018-12-31T10:00:00Z"), ZoneId.of("UTC"));
            ZonedDateTime fixedNow = ZonedDateTime.now(dec31Clock);
            when(clockProvider.getClockForUser(any())).thenReturn(dec31Clock);

            int isoWeekYear = 2019;
            int isoWeekValue = 1;
            int todayYear = 2018;
            int todayValue = 20181231;
            int thisMonthValue = 12;
            int lastMonthValue = 11;
            int lastMonthYear = 2018;

            var user = TestUtils.createValidUserEntityWithId(USER_ID);
            var badge = TestUtils.createValidBadgeWithLabelIds(user, Set.of(VALID_LABEL_ID));

            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.DAY, todayYear, List.of(todayValue)))
                    .thenReturn(List.of());

            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.WEEK, isoWeekYear, List.of(isoWeekValue)))
                    .thenReturn(List.of(
                            TestUtils.createValidWeekBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME, isoWeekYear, isoWeekValue, thisWeekMinutes)
                    ));

            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.WEEK, isoWeekYear, List.of(isoWeekValue - 1)))
                    .thenReturn(List.of());

            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.MONTH, todayYear, List.of(thisMonthValue)))
                    .thenReturn(List.of());

            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.MONTH, lastMonthYear, List.of(lastMonthValue)))
                    .thenReturn(List.of());

            when(bucketRepository.findByUserIdAndLabelIdIn(USER_ID, Set.of(VALID_LABEL_ID)))
                    .thenReturn(List.of(
                            TestUtils.createValidWeekBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME, isoWeekYear, isoWeekValue, thisWeekMinutes)
                    ));

            // Act
            var stats = badgeStatsService.computeStatsForBadge(badge, USER_ID);

            // Assert
            assertEquals(0, stats.minutesToday());
            assertEquals(thisWeekMinutes, stats.minutesThisWeek());
            assertEquals(0, stats.minutesThisMonth());
            assertEquals(0, stats.minutesLastWeek());
            assertEquals(0, stats.minutesLastMonth());
            assertEquals(thisWeekMinutes, stats.totalMinutesAllTime());
        }

    }

}