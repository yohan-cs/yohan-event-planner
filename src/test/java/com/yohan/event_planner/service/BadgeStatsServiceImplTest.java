package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.enums.TimeBucketType;
import com.yohan.event_planner.repository.LabelTimeBucketRepository;
import com.yohan.event_planner.time.ClockProvider;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.yohan.event_planner.util.TestConstants.USER_ID;
import static com.yohan.event_planner.util.TestConstants.VALID_LABEL_ID;
import static com.yohan.event_planner.util.TestConstants.VALID_LABEL_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BadgeStatsServiceImplTest {

    @Mock
    private LabelTimeBucketRepository bucketRepository;
    @Mock
    private ClockProvider clockProvider;
    private Clock fixedClock;

    @InjectMocks
    private BadgeStatsServiceImpl badgeStatsService;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2025-06-16T12:00:00Z"), ZoneId.of("UTC"));
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
            assertEquals(dayMinutes, stats.today());
            assertEquals(thisWeekMinutes, stats.thisWeek());
            assertEquals(thisMonthMinutes, stats.thisMonth());
            assertEquals(lastWeekMinutes, stats.lastWeek());
            assertEquals(lastMonthMinutes, stats.lastMonth());
            assertEquals(totalAllTimeMinutes, stats.allTime());
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
            assertEquals(dayMinutes, stats.today());
            assertEquals(thisWeekMinutes, stats.thisWeek());
            assertEquals(thisMonthMinutes, stats.thisMonth());
            assertEquals(lastWeekMinutes, stats.lastWeek());
            assertEquals(lastMonthMinutes, stats.lastMonth());
            assertEquals(totalAllTimeMinutes, stats.allTime());
        }

        @Test
        void testComputeStatsForBadge_withNoLabelsOrCategories_returnsEmptyStats() {
            // Arrange
            var user = TestUtils.createValidUserEntityWithId(USER_ID);
            var badge = TestUtils.createEmptyBadge(user, "Empty");

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
            assertEquals(totalMinutes, stats.today());
            assertEquals(totalMinutes, stats.allTime());
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
            int lastWeekValue = 52; // Last week of 2024 (week 52)
            int lastWeekYear = 2024;
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

            // Last week query - week 52 of 2024
            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.WEEK, lastWeekYear, List.of(lastWeekValue)))
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
            assertEquals(0, stats.today());
            assertEquals(thisWeekMinutes, stats.thisWeek());
            assertEquals(0, stats.thisMonth());
            assertEquals(0, stats.lastWeek());
            assertEquals(0, stats.lastMonth());
            assertEquals(thisWeekMinutes, stats.allTime());
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
            int lastWeekValue = 52; // Last week of 2018 (week 52)
            int lastWeekYear = 2018;
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

            // Last week query - week 52 of 2018  
            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.WEEK, lastWeekYear, List.of(lastWeekValue)))
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
            assertEquals(0, stats.today());
            assertEquals(thisWeekMinutes, stats.thisWeek());
            assertEquals(0, stats.thisMonth());
            assertEquals(0, stats.lastWeek());
            assertEquals(0, stats.lastMonth());
            assertEquals(thisWeekMinutes, stats.allTime());
        }

        @Test
        void testComputeStatsForBadge_withDifferentTimezones() {
            // Arrange - Test with PST timezone
            Clock pstClock = Clock.fixed(Instant.parse("2025-06-16T19:00:00Z"), ZoneId.of("America/Los_Angeles"));
            when(clockProvider.getClockForUser(any())).thenReturn(pstClock);

            var user = TestUtils.createValidUserEntityWithId(USER_ID);
            var badge = TestUtils.createValidBadgeWithLabelIds(user, Set.of(VALID_LABEL_ID));

            int dayMinutes = 45;
            mockTimeBuckets(dayMinutes, 0, 0, 0, 0, dayMinutes);

            // Act
            var stats = badgeStatsService.computeStatsForBadge(badge, USER_ID);

            // Assert
            assertEquals(dayMinutes, stats.today());
            assertEquals(dayMinutes, stats.allTime());
        }

        @Test
        void testComputeStatsForBadge_withManyLabels() {
            // Arrange - Badge with multiple labels
            when(clockProvider.getClockForUser(any())).thenReturn(fixedClock);

            var user = TestUtils.createValidUserEntityWithId(USER_ID);
            Long secondLabelId = VALID_LABEL_ID + 1;
            Long thirdLabelId = VALID_LABEL_ID + 2;
            var badge = TestUtils.createValidBadgeWithLabelIds(user, Set.of(VALID_LABEL_ID, secondLabelId, thirdLabelId));

            // Mock buckets for multiple labels
            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    anyLong(), anySet(), eq(TimeBucketType.DAY), eq(2025), eq(List.of(20250616))))
                    .thenReturn(List.of(
                            TestUtils.createValidDayBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME, 2025, 20250616, 30),
                            TestUtils.createValidDayBucket(USER_ID, secondLabelId, "Label2", 2025, 20250616, 20),
                            TestUtils.createValidDayBucket(USER_ID, thirdLabelId, "Label3", 2025, 20250616, 10)
                    ));

            // Mock other time periods as empty
            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    anyLong(), anySet(), eq(TimeBucketType.WEEK), anyInt(), anyList()))
                    .thenReturn(List.of());
            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    anyLong(), anySet(), eq(TimeBucketType.MONTH), anyInt(), anyList()))
                    .thenReturn(List.of());
            when(bucketRepository.findByUserIdAndLabelIdIn(anyLong(), anySet()))
                    .thenReturn(List.of(
                            TestUtils.createValidDayBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME, 2025, 20250616, 30),
                            TestUtils.createValidDayBucket(USER_ID, secondLabelId, "Label2", 2025, 20250616, 20),
                            TestUtils.createValidDayBucket(USER_ID, thirdLabelId, "Label3", 2025, 20250616, 10)
                    ));

            // Act
            var stats = badgeStatsService.computeStatsForBadge(badge, USER_ID);

            // Assert - Should aggregate across all labels
            assertEquals(60, stats.today()); // 30 + 20 + 10
            assertEquals(60, stats.allTime());
        }

        @Test
        void testComputeStatsForBadge_withZeroDurationBuckets() {
            // Arrange
            when(clockProvider.getClockForUser(any())).thenReturn(fixedClock);

            var user = TestUtils.createValidUserEntityWithId(USER_ID);
            var badge = TestUtils.createValidBadgeWithLabelIds(user, Set.of(VALID_LABEL_ID));

            // All buckets have zero duration
            mockTimeBuckets(0, 0, 0, 0, 0, 0);

            // Act
            var stats = badgeStatsService.computeStatsForBadge(badge, USER_ID);

            // Assert
            assertEquals(0, stats.today());
            assertEquals(0, stats.thisWeek());
            assertEquals(0, stats.thisMonth());
            assertEquals(0, stats.lastWeek());
            assertEquals(0, stats.lastMonth());
            assertEquals(0, stats.allTime());
        }

        @Test
        void testComputeStatsForBadge_withMaxIntegerValues() {
            // Arrange
            when(clockProvider.getClockForUser(any())).thenReturn(fixedClock);

            var user = TestUtils.createValidUserEntityWithId(USER_ID);
            var badge = TestUtils.createValidBadgeWithLabelIds(user, Set.of(VALID_LABEL_ID));

            // Test with large values approaching integer limits
            int maxTestValue = Integer.MAX_VALUE / 10; // Safe value to avoid overflow in calculations
            mockTimeBuckets(maxTestValue, maxTestValue, maxTestValue, maxTestValue, maxTestValue, maxTestValue);

            // Act
            var stats = badgeStatsService.computeStatsForBadge(badge, USER_ID);

            // Assert
            assertEquals(maxTestValue, stats.today());
            assertEquals(maxTestValue, stats.thisWeek());
            assertEquals(maxTestValue, stats.thisMonth());
            assertEquals(maxTestValue, stats.lastWeek());
            assertEquals(maxTestValue, stats.lastMonth());
            assertEquals(maxTestValue, stats.allTime());
        }

        @Test
        void testComputeStatsForBadge_verifiesCorrectRepositoryQueries() {
            // Arrange
            when(clockProvider.getClockForUser(any())).thenReturn(fixedClock);

            var user = TestUtils.createValidUserEntityWithId(USER_ID);
            var badge = TestUtils.createValidBadgeWithLabelIds(user, Set.of(VALID_LABEL_ID));

            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    anyLong(), anySet(), any(), anyInt(), anyList()))
                    .thenReturn(List.of());
            when(bucketRepository.findByUserIdAndLabelIdIn(anyLong(), anySet()))
                    .thenReturn(List.of());

            // Act
            badgeStatsService.computeStatsForBadge(badge, USER_ID);

            // Assert - Verify exact repository calls with calculated values
            verify(bucketRepository).findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    eq(USER_ID), eq(Set.of(VALID_LABEL_ID)), eq(TimeBucketType.DAY), eq(2025), eq(List.of(20250616)));
            verify(bucketRepository).findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    eq(USER_ID), eq(Set.of(VALID_LABEL_ID)), eq(TimeBucketType.WEEK), eq(2025), eq(List.of(25)));
            verify(bucketRepository).findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    eq(USER_ID), eq(Set.of(VALID_LABEL_ID)), eq(TimeBucketType.WEEK), eq(2025), eq(List.of(24)));
            verify(bucketRepository).findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    eq(USER_ID), eq(Set.of(VALID_LABEL_ID)), eq(TimeBucketType.MONTH), eq(2025), eq(List.of(6)));
            verify(bucketRepository).findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    eq(USER_ID), eq(Set.of(VALID_LABEL_ID)), eq(TimeBucketType.MONTH), eq(2025), eq(List.of(5)));
            verify(bucketRepository).findByUserIdAndLabelIdIn(eq(USER_ID), eq(Set.of(VALID_LABEL_ID)));
        }

        @Test
        void testComputeStatsForBadge_withLastWeekInPreviousYear() {
            // Arrange - First week of 2025, last week is week 52 of 2024
            Clock earlyJanClock = Clock.fixed(Instant.parse("2025-01-06T10:00:00Z"), ZoneId.of("UTC"));
            when(clockProvider.getClockForUser(any())).thenReturn(earlyJanClock);

            var user = TestUtils.createValidUserEntityWithId(USER_ID);
            var badge = TestUtils.createValidBadgeWithLabelIds(user, Set.of(VALID_LABEL_ID));

            int thisWeekMinutes = 60;
            int lastWeekMinutes = 40;

            // Current week: Week 2 of 2025
            // Last week: Week 1 of 2025
            // Note: Jan 6, 2025 is Monday of week 2
            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.DAY, 2025, List.of(20250106)))
                    .thenReturn(List.of());

            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.WEEK, 2025, List.of(2)))
                    .thenReturn(List.of(
                            TestUtils.createValidWeekBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME, 2025, 2, thisWeekMinutes)
                    ));

            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.WEEK, 2025, List.of(1)))
                    .thenReturn(List.of(
                            TestUtils.createValidWeekBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME, 2025, 1, lastWeekMinutes)
                    ));

            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.MONTH, 2025, List.of(1)))
                    .thenReturn(List.of());

            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.MONTH, 2024, List.of(12)))
                    .thenReturn(List.of());

            when(bucketRepository.findByUserIdAndLabelIdIn(USER_ID, Set.of(VALID_LABEL_ID)))
                    .thenReturn(List.of(
                            TestUtils.createValidWeekBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME, 2025, 2, thisWeekMinutes),
                            TestUtils.createValidWeekBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME, 2025, 1, lastWeekMinutes)
                    ));

            // Act
            var stats = badgeStatsService.computeStatsForBadge(badge, USER_ID);

            // Assert
            assertEquals(0, stats.today());
            assertEquals(thisWeekMinutes, stats.thisWeek());
            assertEquals(0, stats.thisMonth());
            assertEquals(lastWeekMinutes, stats.lastWeek());
            assertEquals(0, stats.lastMonth());
            assertEquals(thisWeekMinutes + lastWeekMinutes, stats.allTime());
        }

        @Test
        void testComputeStatsForBadge_withLastMonthInPreviousYear() {
            // Arrange - January 2025, last month is December 2024
            Clock janClock = Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), ZoneId.of("UTC"));
            when(clockProvider.getClockForUser(any())).thenReturn(janClock);

            var user = TestUtils.createValidUserEntityWithId(USER_ID);
            var badge = TestUtils.createValidBadgeWithLabelIds(user, Set.of(VALID_LABEL_ID));

            int thisMonthMinutes = 80;
            int lastMonthMinutes = 120;

            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.DAY, 2025, List.of(20250115)))
                    .thenReturn(List.of());

            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.WEEK, 2025, List.of(3)))
                    .thenReturn(List.of());

            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.WEEK, 2025, List.of(2)))
                    .thenReturn(List.of());

            // This month: January 2025 (month = 1)
            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.MONTH, 2025, List.of(1)))
                    .thenReturn(List.of(
                            TestUtils.createValidMonthBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME, 2025, 1, thisMonthMinutes)
                    ));

            // Last month: December 2024 (month = 12, year = 2024)
            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    USER_ID, Set.of(VALID_LABEL_ID), TimeBucketType.MONTH, 2024, List.of(12)))
                    .thenReturn(List.of(
                            TestUtils.createValidMonthBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME, 2024, 12, lastMonthMinutes)
                    ));

            when(bucketRepository.findByUserIdAndLabelIdIn(USER_ID, Set.of(VALID_LABEL_ID)))
                    .thenReturn(List.of(
                            TestUtils.createValidMonthBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME, 2025, 1, thisMonthMinutes),
                            TestUtils.createValidMonthBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME, 2024, 12, lastMonthMinutes)
                    ));

            // Act
            var stats = badgeStatsService.computeStatsForBadge(badge, USER_ID);

            // Assert
            assertEquals(0, stats.today());
            assertEquals(0, stats.thisWeek());
            assertEquals(thisMonthMinutes, stats.thisMonth());
            assertEquals(0, stats.lastWeek());
            assertEquals(lastMonthMinutes, stats.lastMonth());
            assertEquals(thisMonthMinutes + lastMonthMinutes, stats.allTime());
        }

        @Test
        void testComputeStatsForBadge_withRepositoryException() {
            // Arrange
            when(clockProvider.getClockForUser(any())).thenReturn(fixedClock);

            var user = TestUtils.createValidUserEntityWithId(USER_ID);
            var badge = TestUtils.createValidBadgeWithLabelIds(user, Set.of(VALID_LABEL_ID));

            // Mock repository to throw exception on first call
            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    anyLong(), anySet(), eq(TimeBucketType.DAY), anyInt(), anyList()))
                    .thenThrow(new RuntimeException("Database connection error"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> {
                badgeStatsService.computeStatsForBadge(badge, USER_ID);
            });
        }

        @Test
        void testComputeStatsForBadge_withNullRepositoryResults() {
            // Arrange
            when(clockProvider.getClockForUser(any())).thenReturn(fixedClock);

            var user = TestUtils.createValidUserEntityWithId(USER_ID);
            var badge = TestUtils.createValidBadgeWithLabelIds(user, Set.of(VALID_LABEL_ID));

            // Mock repository to return null instead of empty list
            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    anyLong(), anySet(), any(), anyInt(), anyList()))
                    .thenReturn(null);
            when(bucketRepository.findByUserIdAndLabelIdIn(anyLong(), anySet()))
                    .thenReturn(null);

            // Act & Assert - Should throw NullPointerException when trying to stream null
            assertThrows(NullPointerException.class, () -> {
                badgeStatsService.computeStatsForBadge(badge, USER_ID);
            });
        }

        @Test
        void testComputeStatsForBadge_withClockProviderException() {
            // Arrange
            var user = TestUtils.createValidUserEntityWithId(USER_ID);
            var badge = TestUtils.createValidBadgeWithLabelIds(user, Set.of(VALID_LABEL_ID));

            // Mock ClockProvider to throw exception
            when(clockProvider.getClockForUser(any())).thenThrow(new RuntimeException("Clock service unavailable"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> {
                badgeStatsService.computeStatsForBadge(badge, USER_ID);
            });
        }

        @Test
        void testComputeStatsForBadge_withDuplicateBuckets() {
            // Arrange
            when(clockProvider.getClockForUser(any())).thenReturn(fixedClock);

            var user = TestUtils.createValidUserEntityWithId(USER_ID);
            var badge = TestUtils.createValidBadgeWithLabelIds(user, Set.of(VALID_LABEL_ID));

            // Mock repository to return duplicate buckets (shouldn't happen but defensive test)
            var duplicateBucket = TestUtils.createValidDayBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME, 2025, 20250616, 30);
            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    anyLong(), anySet(), eq(TimeBucketType.DAY), eq(2025), eq(List.of(20250616))))
                    .thenReturn(List.of(duplicateBucket, duplicateBucket)); // Same bucket twice

            // Mock other queries as empty
            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    anyLong(), anySet(), eq(TimeBucketType.WEEK), anyInt(), anyList()))
                    .thenReturn(List.of());
            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    anyLong(), anySet(), eq(TimeBucketType.MONTH), anyInt(), anyList()))
                    .thenReturn(List.of());
            when(bucketRepository.findByUserIdAndLabelIdIn(anyLong(), anySet()))
                    .thenReturn(List.of(duplicateBucket, duplicateBucket));

            // Act
            var stats = badgeStatsService.computeStatsForBadge(badge, USER_ID);

            // Assert - Should sum both buckets (60 = 30 + 30)
            assertEquals(60, stats.today());
            assertEquals(60, stats.allTime());
        }

        @Test
        void testComputeStatsForBadge_withNegativeDurations() {
            // Arrange
            when(clockProvider.getClockForUser(any())).thenReturn(fixedClock);

            var user = TestUtils.createValidUserEntityWithId(USER_ID);
            var badge = TestUtils.createValidBadgeWithLabelIds(user, Set.of(VALID_LABEL_ID));

            // Create buckets with negative durations (data corruption scenario)
            var negativeBucket = TestUtils.createValidDayBucket(USER_ID, VALID_LABEL_ID, VALID_LABEL_NAME, 2025, 20250616, -50);
            var positiveBucket = TestUtils.createValidDayBucket(USER_ID, VALID_LABEL_ID + 1, "Label2", 2025, 20250616, 30);

            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    anyLong(), anySet(), eq(TimeBucketType.DAY), eq(2025), eq(List.of(20250616))))
                    .thenReturn(List.of(negativeBucket, positiveBucket));

            // Mock other queries as empty
            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    anyLong(), anySet(), eq(TimeBucketType.WEEK), anyInt(), anyList()))
                    .thenReturn(List.of());
            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    anyLong(), anySet(), eq(TimeBucketType.MONTH), anyInt(), anyList()))
                    .thenReturn(List.of());
            when(bucketRepository.findByUserIdAndLabelIdIn(anyLong(), anySet()))
                    .thenReturn(List.of(negativeBucket, positiveBucket));

            // Act
            var stats = badgeStatsService.computeStatsForBadge(badge, USER_ID);

            // Assert - Should sum all values including negative (-50 + 30 = -20)
            assertEquals(-20, stats.today());
            assertEquals(-20, stats.allTime());
        }

        @Test
        void testComputeStatsForBadge_withMaximumLabelCount() {
            // Arrange
            when(clockProvider.getClockForUser(any())).thenReturn(fixedClock);

            var user = TestUtils.createValidUserEntityWithId(USER_ID);
            
            // Create badge with 100 labels
            Set<Long> manyLabelIds = new HashSet<>();
            List<com.yohan.event_planner.domain.LabelTimeBucket> manyBuckets = new ArrayList<>();
            
            for (int i = 0; i < 100; i++) {
                Long labelId = VALID_LABEL_ID + i;
                manyLabelIds.add(labelId);
                manyBuckets.add(TestUtils.createValidDayBucket(USER_ID, labelId, "Label" + i, 2025, 20250616, 1));
            }
            
            var badge = TestUtils.createValidBadgeWithLabelIds(user, manyLabelIds);

            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    eq(USER_ID), eq(manyLabelIds), eq(TimeBucketType.DAY), eq(2025), eq(List.of(20250616))))
                    .thenReturn(manyBuckets);

            // Mock other queries as empty
            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    eq(USER_ID), eq(manyLabelIds), eq(TimeBucketType.WEEK), anyInt(), anyList()))
                    .thenReturn(List.of());
            when(bucketRepository.findByUserIdAndLabelIdInAndBucketTypeAndBucketYearAndBucketValueIn(
                    eq(USER_ID), eq(manyLabelIds), eq(TimeBucketType.MONTH), anyInt(), anyList()))
                    .thenReturn(List.of());
            when(bucketRepository.findByUserIdAndLabelIdIn(eq(USER_ID), eq(manyLabelIds)))
                    .thenReturn(manyBuckets);

            // Act
            var stats = badgeStatsService.computeStatsForBadge(badge, USER_ID);

            // Assert - Should aggregate all 100 labels (100 * 1 minute each)
            assertEquals(100, stats.today());
            assertEquals(100, stats.allTime());
        }

    }

}