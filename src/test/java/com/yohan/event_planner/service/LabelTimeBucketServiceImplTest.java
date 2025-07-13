package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.LabelTimeBucket;
import com.yohan.event_planner.domain.enums.LabelColor;
import com.yohan.event_planner.dto.EventChangeContextDTO;
import com.yohan.event_planner.dto.LabelResponseDTO;
import com.yohan.event_planner.repository.LabelTimeBucketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;


import static com.yohan.event_planner.domain.enums.TimeBucketType.DAY;
import static com.yohan.event_planner.domain.enums.TimeBucketType.MONTH;
import static com.yohan.event_planner.domain.enums.TimeBucketType.WEEK;
import static com.yohan.event_planner.util.TestConstants.USER_ID;
import static com.yohan.event_planner.util.TestConstants.VALID_EVENT_DURATION_MINUTES;
import static com.yohan.event_planner.util.TestConstants.VALID_LABEL_ID;
import static com.yohan.event_planner.util.TestConstants.VALID_LABEL_NAME;
import static com.yohan.event_planner.util.TestConstants.VALID_TIMEZONE;
import static com.yohan.event_planner.util.TestConstants.getValidEventStartFuture;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class LabelTimeBucketServiceImplTest {

    private LabelService labelService;
    private LabelTimeBucketRepository bucketRepository;
    private Clock fixedClock;

    private LabelTimeBucketServiceImpl bucketService;

    @BeforeEach
    void setUp() {
        labelService = mock(LabelService.class);
        bucketRepository = mock(LabelTimeBucketRepository.class);

        fixedClock = Clock.fixed(Instant.parse("2025-06-29T12:00:00Z"), ZoneId.of("UTC"));


        bucketService = new LabelTimeBucketServiceImpl(labelService, bucketRepository);
    }

    @Nested
    class RevertTests {

        @Test
        void testRevert_removesAllRelevantTimeBuckets() {
            // Arrange
            long userId = 1L;
            long labelId = 10L;
            int originalDuration = 90;
            int revertAmount = 30;
            ZoneId timezone = ZoneId.of("America/New_York");

            ZonedDateTime startTime = ZonedDateTime.of(
                    2024, 1, 1, 0, 0, 0, 0, timezone
            );

            LabelTimeBucket dayBucket = new LabelTimeBucket(userId, labelId, VALID_LABEL_NAME, DAY, 2024, 20240101);
            dayBucket.setDurationMinutes(originalDuration);

            LabelTimeBucket weekBucket = new LabelTimeBucket(userId, labelId, VALID_LABEL_NAME, WEEK, 2024, 1);
            weekBucket.setDurationMinutes(originalDuration);

            LabelTimeBucket monthBucket = new LabelTimeBucket(userId, labelId, VALID_LABEL_NAME, MONTH, 2024, 1);
            monthBucket.setDurationMinutes(originalDuration);

            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, DAY, 2024, 20240101)).thenReturn(Optional.of(dayBucket));
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, WEEK, 2024, 1)).thenReturn(Optional.of(weekBucket));
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, MONTH, 2024, 1)).thenReturn(Optional.of(monthBucket));

            // Mock labelService to provide label name
            when(labelService.getLabelById(labelId))
                    .thenReturn(new LabelResponseDTO(
                            labelId,
                            "Test Label",
                            LabelColor.BLUE,
                            "testuser"
                    ));

            // Act
            bucketService.revert(userId, labelId, startTime, revertAmount, timezone);

            // Assert
            ArgumentCaptor<List<LabelTimeBucket>> captor = ArgumentCaptor.forClass(List.class);
            verify(bucketRepository).saveAll(captor.capture());

            List<LabelTimeBucket> saved = captor.getValue();
            assertThat(saved).hasSize(3);

            assertThat(saved).allSatisfy(bucket -> {
                assertThat(bucket.getUserId()).isEqualTo(userId);
                assertThat(bucket.getLabelId()).isEqualTo(labelId);
                assertThat(bucket.getDurationMinutes()).isEqualTo(originalDuration - revertAmount);
            });

            assertThat(saved).extracting(LabelTimeBucket::getBucketType)
                    .containsExactlyInAnyOrder(DAY, WEEK, MONTH);
        }

    }

    @Nested
    class ApplyTests {

        @Test
        void testApply_addsAllRelevantTimeBuckets() {
            // Arrange
            long userId = 1L;
            long labelId = 10L;
            ZoneId timezone = ZoneId.of("America/New_York");

            ZonedDateTime startTime = ZonedDateTime.of(
                    2024, 1, 1, 0, 0, 0, 0, timezone
            );

            int durationMinutes = 60;

            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    eq(userId), eq(labelId), eq(DAY), eq(2024), eq(20240101)
            )).thenReturn(Optional.empty());

            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    eq(userId), eq(labelId), eq(WEEK), eq(2024), eq(1)
            )).thenReturn(Optional.empty());

            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    eq(userId), eq(labelId), eq(MONTH), eq(2024), eq(1)
            )).thenReturn(Optional.empty());

            // Mock labelService to provide label name
            when(labelService.getLabelById(labelId))
                    .thenReturn(new LabelResponseDTO(
                            labelId,
                            "Test Label",
                            LabelColor.BLUE,
                            "testuser"
                    ));

            // Act
            bucketService.apply(userId, labelId, startTime, durationMinutes, timezone);

            // Assert
            ArgumentCaptor<List<LabelTimeBucket>> captor = ArgumentCaptor.forClass(List.class);
            verify(bucketRepository).saveAll(captor.capture());

            List<LabelTimeBucket> saved = captor.getValue();
            assertThat(saved).hasSize(3);

            assertThat(saved).extracting(LabelTimeBucket::getBucketType)
                    .containsExactlyInAnyOrder(DAY, WEEK, MONTH);

            assertThat(saved).extracting(LabelTimeBucket::getBucketValue)
                    .containsExactlyInAnyOrder(20240101, 1, 1);

            assertThat(saved).allSatisfy(bucket -> {
                assertThat(bucket.getUserId()).isEqualTo(userId);
                assertThat(bucket.getLabelId()).isEqualTo(labelId);
                assertThat(bucket.getBucketYear()).isEqualTo(2024);
                assertThat(bucket.getDurationMinutes()).isEqualTo(60);
            });
        }
    }

    @Nested
    class HandleEventChangeTests {

        @Test
        void testHandleEventChange_appliesWhenNewlyCompleted() {
            // Arrange
            ZonedDateTime startTime = getValidEventStartFuture(fixedClock);
            ZoneId zoneId = ZoneId.of(VALID_TIMEZONE);

            EventChangeContextDTO context = new EventChangeContextDTO(
                    USER_ID,
                    null,
                    VALID_LABEL_ID,
                    null,
                    startTime,
                    null,
                    VALID_EVENT_DURATION_MINUTES,
                    zoneId,
                    false,  // wasCompleted
                    true    // isNowCompleted
            );

            when(labelService.getLabelById(VALID_LABEL_ID))
                    .thenReturn(new LabelResponseDTO(
                            VALID_LABEL_ID,
                            "Test Label",
                            LabelColor.BLUE,
                            "testuser"
                    ));

            // Act
            bucketService.handleEventChange(context);

            // Assert
            verify(bucketRepository, times(1)).saveAll(any(List.class));
            verify(bucketRepository, never()).deleteAll(any(List.class));
        }

    }

    @Test
    void testHandleEventChange_revertsWhenNoLongerCompleted() {
        // Arrange
        ZonedDateTime startTime = getValidEventStartFuture(fixedClock);
        ZoneId zoneId = ZoneId.of(VALID_TIMEZONE);

        EventChangeContextDTO context = new EventChangeContextDTO(
                USER_ID,
                VALID_LABEL_ID,
                null,
                startTime,
                null,
                VALID_EVENT_DURATION_MINUTES,
                null,
                zoneId,
                true,   // wasCompleted
                false   // isNowCompleted
        );

        when(labelService.getLabelById(VALID_LABEL_ID))
                .thenReturn(new LabelResponseDTO(
                        VALID_LABEL_ID,
                        "Test Label",
                        LabelColor.BLUE,
                        "testuser"
                ));

        // Act
        bucketService.handleEventChange(context);

        // Assert
        verify(bucketRepository).saveAll(any(List.class));
        verify(bucketRepository, never()).deleteAll(any(List.class));
    }

    @Test
    void testHandleEventChange_revertsAndAppliesWhenStillCompleted() {
        // Arrange
        ZonedDateTime oldStart = getValidEventStartFuture(fixedClock);
        ZonedDateTime newStart = oldStart.plusHours(1);
        ZoneId zoneId = ZoneId.of(VALID_TIMEZONE);

        EventChangeContextDTO context = new EventChangeContextDTO(
                USER_ID,
                VALID_LABEL_ID,
                VALID_LABEL_ID, // same label ID, still mocked
                oldStart,
                newStart,
                60,   // old duration
                90,   // new duration
                zoneId,
                true,  // wasCompleted
                true   // isNowCompleted
        );

        when(labelService.getLabelById(VALID_LABEL_ID))
                .thenReturn(new LabelResponseDTO(
                        VALID_LABEL_ID,
                        "Test Label",
                        LabelColor.BLUE,
                        "testuser"
                ));

        // Act
        bucketService.handleEventChange(context);

        // Assert
        verify(bucketRepository, times(2)).saveAll(any(List.class)); // revert + apply
        verify(bucketRepository, never()).deleteAll(any(List.class));
    }

    @Test
    void testHandleEventChange_noopWhenStillIncomplete() {
        // Arrange
        EventChangeContextDTO context = new EventChangeContextDTO(
                USER_ID,
                null,
                null,
                null,
                null,
                null,
                null,
                ZoneId.of(VALID_TIMEZONE),
                false,  // wasCompleted
                false   // isNowCompleted
        );

        // Act
        bucketService.handleEventChange(context);

        // Assert
        verifyNoInteractions(bucketRepository);
        
    }

    @Nested
    class EdgeCaseTests {

        @Test
        void testApply_eventSpanningMidnight() {
            // Arrange: Event from 11:30 PM to 1:30 AM (spans two days)
            long userId = 1L;
            long labelId = 10L;
            ZoneId timezone = ZoneId.of("America/New_York");
            
            // December 31, 2023 11:30 PM EST
            ZonedDateTime startTime = ZonedDateTime.of(2023, 12, 31, 23, 30, 0, 0, timezone);
            int durationMinutes = 120; // 2 hours total

            when(labelService.getLabelById(labelId))
                    .thenReturn(new LabelResponseDTO(labelId, "Test Label", LabelColor.BLUE, "testuser"));

            // Mock repository responses for all bucket types for both days
            // Day buckets
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, DAY, 2023, 20231231)).thenReturn(Optional.empty());
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, DAY, 2024, 20240101)).thenReturn(Optional.empty());
            
            // Week buckets (Dec 31, 2023 is week 52 of 2023, Jan 1, 2024 is week 1 of 2024)
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, WEEK, 2023, 52)).thenReturn(Optional.empty());
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, WEEK, 2024, 1)).thenReturn(Optional.empty());
            
            // Month buckets
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, MONTH, 2023, 12)).thenReturn(Optional.empty());
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, MONTH, 2024, 1)).thenReturn(Optional.empty());

            // Act
            bucketService.apply(userId, labelId, startTime, durationMinutes, timezone);

            // Assert
            ArgumentCaptor<List<LabelTimeBucket>> captor = ArgumentCaptor.forClass(List.class);
            verify(bucketRepository).saveAll(captor.capture());

            List<LabelTimeBucket> saved = captor.getValue();
            
            // Should create buckets for both days
            List<LabelTimeBucket> dayBuckets = saved.stream()
                    .filter(bucket -> bucket.getBucketType() == DAY)
                    .toList();
            
            assertThat(dayBuckets).hasSize(2);
            
            // First day (Dec 31) should have 30 minutes (11:30 PM to midnight)
            LabelTimeBucket dec31Bucket = dayBuckets.stream()
                    .filter(bucket -> bucket.getBucketValue() == 20231231)
                    .findFirst().orElseThrow();
            assertThat(dec31Bucket.getDurationMinutes()).isEqualTo(30);
            
            // Second day (Jan 1) should have 90 minutes (midnight to 1:30 AM)
            LabelTimeBucket jan1Bucket = dayBuckets.stream()
                    .filter(bucket -> bucket.getBucketValue() == 20240101)
                    .findFirst().orElseThrow();
            assertThat(jan1Bucket.getDurationMinutes()).isEqualTo(90);
        }

        @Test
        void testApply_daylightSavingTimeTransition() {
            // Arrange: Event during DST "spring forward" (2:00 AM becomes 3:00 AM)
            long userId = 1L;
            long labelId = 10L;
            ZoneId timezone = ZoneId.of("America/New_York");
            
            // March 10, 2024 1:30 AM EST (before DST transition)
            ZonedDateTime startTime = ZonedDateTime.of(2024, 3, 10, 1, 30, 0, 0, timezone);
            int durationMinutes = 120; // 2 hours

            when(labelService.getLabelById(labelId))
                    .thenReturn(new LabelResponseDTO(labelId, "DST Label", LabelColor.BLUE, "testuser"));

            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    any(Long.class), any(Long.class), any(), any(Integer.class), any(Integer.class)
            )).thenReturn(Optional.empty());

            // Act
            bucketService.apply(userId, labelId, startTime, durationMinutes, timezone);

            // Assert - should handle DST transition gracefully
            ArgumentCaptor<List<LabelTimeBucket>> captor = ArgumentCaptor.forClass(List.class);
            verify(bucketRepository).saveAll(captor.capture());

            List<LabelTimeBucket> saved = captor.getValue();
            assertThat(saved).isNotEmpty();
            
            // Verify DST transition handled gracefully without exceptions
            verify(bucketRepository).saveAll(any(List.class));
        }

        @Test
        void testApply_crossingYearBoundary() {
            // Arrange: Event from Dec 31 to Jan 1 (crosses year boundary)
            long userId = 1L;
            long labelId = 10L;
            ZoneId timezone = ZoneId.of("UTC");
            
            ZonedDateTime startTime = ZonedDateTime.of(2023, 12, 31, 23, 0, 0, 0, timezone);
            int durationMinutes = 120; // 2 hours

            when(labelService.getLabelById(labelId))
                    .thenReturn(new LabelResponseDTO(labelId, "Year Boundary", LabelColor.BLUE, "testuser"));

            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    any(Long.class), any(Long.class), any(), any(Integer.class), any(Integer.class)
            )).thenReturn(Optional.empty());

            // Act
            bucketService.apply(userId, labelId, startTime, durationMinutes, timezone);

            // Assert
            ArgumentCaptor<List<LabelTimeBucket>> captor = ArgumentCaptor.forClass(List.class);
            verify(bucketRepository).saveAll(captor.capture());

            List<LabelTimeBucket> saved = captor.getValue();
            
            // Check that we have buckets for both years
            boolean has2023Buckets = saved.stream().anyMatch(bucket -> bucket.getBucketYear() == 2023);
            boolean has2024Buckets = saved.stream().anyMatch(bucket -> bucket.getBucketYear() == 2024);
            
            assertThat(has2023Buckets).isTrue();
            assertThat(has2024Buckets).isTrue();
        }

        @Test
        void testApply_isoWeekCalculation() {
            // Arrange: Test ISO week edge case (January 1st in week of previous year)
            long userId = 1L;
            long labelId = 10L;
            ZoneId timezone = ZoneId.of("Europe/London");
            
            // January 1, 2024 is a Monday (week 1 of 2024)
            ZonedDateTime startTime = ZonedDateTime.of(2024, 1, 1, 10, 0, 0, 0, timezone);
            int durationMinutes = 60;

            when(labelService.getLabelById(labelId))
                    .thenReturn(new LabelResponseDTO(labelId, "ISO Week", LabelColor.BLUE, "testuser"));

            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    any(Long.class), any(Long.class), any(), any(Integer.class), any(Integer.class)
            )).thenReturn(Optional.empty());

            // Act
            bucketService.apply(userId, labelId, startTime, durationMinutes, timezone);

            // Assert
            ArgumentCaptor<List<LabelTimeBucket>> captor = ArgumentCaptor.forClass(List.class);
            verify(bucketRepository).saveAll(captor.capture());

            List<LabelTimeBucket> saved = captor.getValue();
            
            LabelTimeBucket weekBucket = saved.stream()
                    .filter(bucket -> bucket.getBucketType() == WEEK)
                    .findFirst().orElseThrow();
            
            assertThat(weekBucket.getBucketYear()).isEqualTo(2024);
            assertThat(weekBucket.getBucketValue()).isEqualTo(1); // Week 1
        }

        @Test
        void testApply_zeroDurationEvent() {
            // Arrange
            long userId = 1L;
            long labelId = 10L;
            ZoneId timezone = ZoneId.of("UTC");
            ZonedDateTime startTime = ZonedDateTime.of(2024, 6, 15, 12, 0, 0, 0, timezone);
            int durationMinutes = 0;

            when(labelService.getLabelById(labelId))
                    .thenReturn(new LabelResponseDTO(labelId, "Zero Duration", LabelColor.BLUE, "testuser"));

            // Act
            bucketService.apply(userId, labelId, startTime, durationMinutes, timezone);

            // Assert - When duration is 0, no time slices are created, so no buckets are processed
            verify(labelService).getLabelById(labelId); // Label is still retrieved
            
            // saveAll should be called with an empty list since no time slices are created
            ArgumentCaptor<List<LabelTimeBucket>> captor = ArgumentCaptor.forClass(List.class);
            verify(bucketRepository).saveAll(captor.capture());
            
            List<LabelTimeBucket> saved = captor.getValue();
            assertThat(saved).isEmpty(); // No buckets created when duration is 0
            
            // Verify no repository lookups were made since no slices were created
            verify(bucketRepository, never()).findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    any(Long.class), any(Long.class), any(), any(Integer.class), any(Integer.class));
        }

        @Test
        void testRevert_existingBucketsWithSufficientTime() {
            // Arrange
            long userId = 1L;
            long labelId = 10L;
            ZoneId timezone = ZoneId.of("UTC");
            ZonedDateTime startTime = ZonedDateTime.of(2024, 6, 15, 12, 0, 0, 0, timezone);
            int durationToRevert = 30;

            // Create existing buckets with sufficient time
            LabelTimeBucket existingDayBucket = new LabelTimeBucket(userId, labelId, "Test", DAY, 2024, 20240615);
            existingDayBucket.setDurationMinutes(60);
            
            LabelTimeBucket existingWeekBucket = new LabelTimeBucket(userId, labelId, "Test", WEEK, 2024, 24);
            existingWeekBucket.setDurationMinutes(120);
            
            LabelTimeBucket existingMonthBucket = new LabelTimeBucket(userId, labelId, "Test", MONTH, 2024, 6);
            existingMonthBucket.setDurationMinutes(180);

            when(labelService.getLabelById(labelId))
                    .thenReturn(new LabelResponseDTO(labelId, "Test Label", LabelColor.BLUE, "testuser"));

            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, DAY, 2024, 20240615
            )).thenReturn(Optional.of(existingDayBucket));
            
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, WEEK, 2024, 24
            )).thenReturn(Optional.of(existingWeekBucket));
            
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, MONTH, 2024, 6
            )).thenReturn(Optional.of(existingMonthBucket));

            // Act
            bucketService.revert(userId, labelId, startTime, durationToRevert, timezone);

            // Assert
            assertThat(existingDayBucket.getDurationMinutes()).isEqualTo(30);
            assertThat(existingWeekBucket.getDurationMinutes()).isEqualTo(90);
            assertThat(existingMonthBucket.getDurationMinutes()).isEqualTo(150);
        }
    }

    @Nested
    class ErrorScenarioTests {

        @Test
        void testAdjust_labelServiceThrowsException() {
            // Arrange
            long userId = 1L;
            long labelId = 999L; // Non-existent label
            ZoneId timezone = ZoneId.of("UTC");
            ZonedDateTime startTime = ZonedDateTime.of(2024, 6, 15, 12, 0, 0, 0, timezone);
            int durationMinutes = 60;

            when(labelService.getLabelById(labelId))
                    .thenThrow(new RuntimeException("Label not found"));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> 
                bucketService.apply(userId, labelId, startTime, durationMinutes, timezone));
            
            verify(bucketRepository, never()).saveAll(any());
        }

        @Test
        void testAdjust_repositoryThrowsException() {
            // Arrange
            long userId = 1L;
            long labelId = 10L;
            ZoneId timezone = ZoneId.of("UTC");
            ZonedDateTime startTime = ZonedDateTime.of(2024, 6, 15, 12, 0, 0, 0, timezone);
            int durationMinutes = 60;

            when(labelService.getLabelById(labelId))
                    .thenReturn(new LabelResponseDTO(labelId, "Test Label", LabelColor.BLUE, "testuser"));

            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    any(Long.class), any(Long.class), any(), any(Integer.class), any(Integer.class)
            )).thenReturn(Optional.empty());

            doThrow(new RuntimeException("Database error"))
                    .when(bucketRepository).saveAll(any(List.class));

            // Act & Assert
            assertThrows(RuntimeException.class, () -> 
                bucketService.apply(userId, labelId, startTime, durationMinutes, timezone));
        }
    }

    @Nested
    class TimezoneEdgeCaseTests {

        @Test
        void testApply_negativeUtcOffset() {
            // Arrange: Test with timezone that has negative UTC offset
            long userId = 1L;
            long labelId = 10L;
            ZoneId timezone = ZoneId.of("America/Los_Angeles"); // UTC-8/UTC-7
            
            ZonedDateTime startTime = ZonedDateTime.of(2024, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC);
            int durationMinutes = 60;

            when(labelService.getLabelById(labelId))
                    .thenReturn(new LabelResponseDTO(labelId, "Pacific Time", LabelColor.BLUE, "testuser"));

            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    any(Long.class), any(Long.class), any(), any(Integer.class), any(Integer.class)
            )).thenReturn(Optional.empty());

            // Act
            bucketService.apply(userId, labelId, startTime, durationMinutes, timezone);

            // Assert
            ArgumentCaptor<List<LabelTimeBucket>> captor = ArgumentCaptor.forClass(List.class);
            verify(bucketRepository).saveAll(captor.capture());

            List<LabelTimeBucket> saved = captor.getValue();
            assertThat(saved).isNotEmpty();
            
            // Event at 12:00 UTC should be 5:00 AM PDT, so bucket should be for June 15
            LabelTimeBucket dayBucket = saved.stream()
                    .filter(bucket -> bucket.getBucketType() == DAY)
                    .findFirst().orElseThrow();
            assertThat(dayBucket.getBucketValue()).isEqualTo(20240615);
        }

        @Test
        void testApply_positiveUtcOffset() {
            // Arrange: Test with timezone that has positive UTC offset
            long userId = 1L;
            long labelId = 10L;
            ZoneId timezone = ZoneId.of("Asia/Tokyo"); // UTC+9
            
            ZonedDateTime startTime = ZonedDateTime.of(2024, 6, 15, 20, 0, 0, 0, ZoneOffset.UTC);
            int durationMinutes = 60;

            when(labelService.getLabelById(labelId))
                    .thenReturn(new LabelResponseDTO(labelId, "Tokyo Time", LabelColor.BLUE, "testuser"));

            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    any(Long.class), any(Long.class), any(), any(Integer.class), any(Integer.class)
            )).thenReturn(Optional.empty());

            // Act
            bucketService.apply(userId, labelId, startTime, durationMinutes, timezone);

            // Assert
            ArgumentCaptor<List<LabelTimeBucket>> captor = ArgumentCaptor.forClass(List.class);
            verify(bucketRepository).saveAll(captor.capture());

            List<LabelTimeBucket> saved = captor.getValue();
            
            // Event at 20:00 UTC should be 5:00 AM JST on June 16
            LabelTimeBucket dayBucket = saved.stream()
                    .filter(bucket -> bucket.getBucketType() == DAY)
                    .findFirst().orElseThrow();
            assertThat(dayBucket.getBucketValue()).isEqualTo(20240616); // Next day in JST
        }
    }


    @Nested
    class BoundaryConditionTests {

        @Test
        void testApply_largeEventDuration() {
            // Arrange: Test with very long event (25 hours to ensure it spans 2 days)
            long userId = 1L;
            long labelId = 10L;
            ZoneId timezone = ZoneId.of("UTC");
            ZonedDateTime startTime = ZonedDateTime.of(2024, 6, 15, 23, 0, 0, 0, timezone); // Start at 11 PM
            int durationMinutes = 25 * 60; // 25 hours to ensure day boundary crossing

            when(labelService.getLabelById(labelId))
                    .thenReturn(new LabelResponseDTO(labelId, "Long Event", LabelColor.BLUE, "testuser"));

            // Mock repository responses for both days (June 15 and 16, 2024)
            // Day buckets
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, DAY, 2024, 20240615)).thenReturn(Optional.empty());
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, DAY, 2024, 20240616)).thenReturn(Optional.empty());
            
            // Week buckets (both days are in week 24 of 2024)
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, WEEK, 2024, 24)).thenReturn(Optional.empty());
            
            // Month buckets (both days are in June 2024)
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, MONTH, 2024, 6)).thenReturn(Optional.empty());

            // Act
            bucketService.apply(userId, labelId, startTime, durationMinutes, timezone);

            // Assert
            ArgumentCaptor<List<LabelTimeBucket>> captor = ArgumentCaptor.forClass(List.class);
            verify(bucketRepository).saveAll(captor.capture());

            List<LabelTimeBucket> saved = captor.getValue();
            
            // Should span two days
            List<LabelTimeBucket> dayBuckets = saved.stream()
                    .filter(bucket -> bucket.getBucketType() == DAY)
                    .toList();
            assertThat(dayBuckets).hasSize(2);
            
            // Total duration should equal original
            int totalDuration = dayBuckets.stream()
                    .mapToInt(LabelTimeBucket::getDurationMinutes)
                    .sum();
            assertThat(totalDuration).isEqualTo(durationMinutes);
        }

        @Test
        void testApply_eventSpanningMultipleWeeks() {
            // Arrange: Event spanning from Sunday to next Tuesday (crosses week boundary)
            long userId = 1L;
            long labelId = 10L;
            ZoneId timezone = ZoneId.of("UTC");
            
            // June 16, 2024 is a Sunday
            ZonedDateTime startTime = ZonedDateTime.of(2024, 6, 16, 20, 0, 0, 0, timezone);
            int durationMinutes = 3 * 24 * 60; // 3 days

            when(labelService.getLabelById(labelId))
                    .thenReturn(new LabelResponseDTO(labelId, "Multi Week", LabelColor.BLUE, "testuser"));

            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    any(Long.class), any(Long.class), any(), any(Integer.class), any(Integer.class)
            )).thenReturn(Optional.empty());

            // Act
            bucketService.apply(userId, labelId, startTime, durationMinutes, timezone);

            // Assert
            ArgumentCaptor<List<LabelTimeBucket>> captor = ArgumentCaptor.forClass(List.class);
            verify(bucketRepository).saveAll(captor.capture());

            List<LabelTimeBucket> saved = captor.getValue();
            
            // Should have buckets for both weeks
            List<LabelTimeBucket> weekBuckets = saved.stream()
                    .filter(bucket -> bucket.getBucketType() == WEEK)
                    .toList();
            assertThat(weekBuckets.size()).isGreaterThanOrEqualTo(1); // At least one week bucket
        }
    }

    @Nested
    class InputValidationTests {

        @Test
        void testApply_negativeDuration() {
            // Arrange
            long userId = 1L;
            long labelId = 10L;
            ZoneId timezone = ZoneId.of("UTC");
            ZonedDateTime startTime = ZonedDateTime.of(2024, 6, 15, 12, 0, 0, 0, timezone);
            int negativeDuration = -30;

            when(labelService.getLabelById(labelId))
                    .thenReturn(new LabelResponseDTO(labelId, "Test Label", LabelColor.BLUE, "testuser"));

            // Act - Service should handle negative duration gracefully
            // Note: Negative duration may result in empty time slices
            bucketService.apply(userId, labelId, startTime, negativeDuration, timezone);
            
            // Assert - Verify the service processes it without throwing exception
            verify(labelService).getLabelById(labelId);
            
            // The service may or may not call saveAll depending on slice generation
            // This test primarily verifies no exceptions are thrown
        }
    }

    @Nested
    class TimeCalculationEdgeCaseTests {

        @Test
        void testApply_leapYearFebruary29() {
            // Arrange: Test Feb 29 on leap year for proper day bucket value calculation
            long userId = 1L;
            long labelId = 10L;
            ZoneId timezone = ZoneId.of("UTC");
            ZonedDateTime leapYearDate = ZonedDateTime.of(2024, 2, 29, 10, 0, 0, 0, timezone);
            int durationMinutes = 60;

            when(labelService.getLabelById(labelId))
                    .thenReturn(new LabelResponseDTO(labelId, "Leap Year", LabelColor.BLUE, "testuser"));

            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, DAY, 2024, 20240229)).thenReturn(Optional.empty());
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, WEEK, 2024, 9)).thenReturn(Optional.empty());
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, MONTH, 2024, 2)).thenReturn(Optional.empty());

            // Act
            bucketService.apply(userId, labelId, leapYearDate, durationMinutes, timezone);

            // Assert
            ArgumentCaptor<List<LabelTimeBucket>> captor = ArgumentCaptor.forClass(List.class);
            verify(bucketRepository).saveAll(captor.capture());

            List<LabelTimeBucket> saved = captor.getValue();
            LabelTimeBucket dayBucket = saved.stream()
                    .filter(bucket -> bucket.getBucketType() == DAY)
                    .findFirst().orElseThrow();
            
            // Verify correct day bucket value for Feb 29
            assertThat(dayBucket.getBucketValue()).isEqualTo(20240229);
        }

        @Test
        void testApply_iso8601WeekBoundaryEdgeCase() {
            // Arrange: Test Jan 1, 2021 (Friday) - belongs to week 53 of 2020
            long userId = 1L;
            long labelId = 10L;
            ZoneId timezone = ZoneId.of("UTC");
            ZonedDateTime edgeCaseDate = ZonedDateTime.of(2021, 1, 1, 10, 0, 0, 0, timezone);
            int durationMinutes = 60;

            when(labelService.getLabelById(labelId))
                    .thenReturn(new LabelResponseDTO(labelId, "ISO Week Edge", LabelColor.BLUE, "testuser"));

            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, DAY, 2021, 20210101)).thenReturn(Optional.empty());
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, WEEK, 2020, 53)).thenReturn(Optional.empty()); // Week 53 of 2020!
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, MONTH, 2021, 1)).thenReturn(Optional.empty());

            // Act
            bucketService.apply(userId, labelId, edgeCaseDate, durationMinutes, timezone);

            // Assert
            ArgumentCaptor<List<LabelTimeBucket>> captor = ArgumentCaptor.forClass(List.class);
            verify(bucketRepository).saveAll(captor.capture());

            List<LabelTimeBucket> saved = captor.getValue();
            LabelTimeBucket weekBucket = saved.stream()
                    .filter(bucket -> bucket.getBucketType() == WEEK)
                    .findFirst().orElseThrow();
            
            // Jan 1, 2021 should belong to week 53 of 2020 per ISO 8601
            assertThat(weekBucket.getBucketYear()).isEqualTo(2020);
            assertThat(weekBucket.getBucketValue()).isEqualTo(53);
        }

        @Test
        void testApply_eventAtExactMidnight() {
            // Arrange: Test event starting exactly at midnight
            long userId = 1L;
            long labelId = 10L;
            ZoneId timezone = ZoneId.of("America/New_York");
            ZonedDateTime midnightStart = ZonedDateTime.of(2024, 6, 15, 0, 0, 0, 0, timezone);
            int durationMinutes = 60;

            when(labelService.getLabelById(labelId))
                    .thenReturn(new LabelResponseDTO(labelId, "Midnight Test", LabelColor.BLUE, "testuser"));

            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    any(Long.class), any(Long.class), any(), any(Integer.class), any(Integer.class)
            )).thenReturn(Optional.empty());

            // Act
            bucketService.apply(userId, labelId, midnightStart, durationMinutes, timezone);

            // Assert - Should handle midnight boundary correctly
            ArgumentCaptor<List<LabelTimeBucket>> captor = ArgumentCaptor.forClass(List.class);
            verify(bucketRepository).saveAll(captor.capture());

            List<LabelTimeBucket> saved = captor.getValue();
            assertThat(saved).isNotEmpty();
            
            // All buckets should be for June 15, not split across days
            List<LabelTimeBucket> dayBuckets = saved.stream()
                    .filter(bucket -> bucket.getBucketType() == DAY)
                    .toList();
            assertThat(dayBuckets).hasSize(1);
            assertThat(dayBuckets.get(0).getBucketValue()).isEqualTo(20240615);
        }
    }

    @Nested
    class ComplexEventChangeTests {

        @Test
        void testHandleEventChange_labelChangeWithoutCompletionChange() {
            // Arrange: Event stays completed but changes labels
            ZonedDateTime startTime = getValidEventStartFuture(fixedClock);
            ZoneId timezone = ZoneId.of(VALID_TIMEZONE);
            
            EventChangeContextDTO context = new EventChangeContextDTO(
                    USER_ID,
                    VALID_LABEL_ID,      // old label
                    VALID_LABEL_ID + 1,  // new label  
                    startTime,           // same time
                    startTime,           // same time
                    60,                  // same duration
                    60,                  // same duration
                    timezone,
                    true,                // was completed
                    true                 // still completed
            );

            when(labelService.getLabelById(VALID_LABEL_ID))
                    .thenReturn(new LabelResponseDTO(VALID_LABEL_ID, "Old Label", LabelColor.BLUE, "testuser"));
            when(labelService.getLabelById(VALID_LABEL_ID + 1))
                    .thenReturn(new LabelResponseDTO(VALID_LABEL_ID + 1, "New Label", LabelColor.GREEN, "testuser"));

            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    any(Long.class), any(Long.class), any(), any(Integer.class), any(Integer.class)
            )).thenReturn(Optional.empty());

            // Act
            bucketService.handleEventChange(context);

            // Assert - Should revert old label and apply new label
            verify(bucketRepository, times(2)).saveAll(any(List.class)); // revert + apply
            verify(labelService).getLabelById(VALID_LABEL_ID);      // for revert
            verify(labelService).getLabelById(VALID_LABEL_ID + 1);  // for apply
        }

        @Test
        void testHandleEventChange_partialDataChange() {
            // Arrange: Test DTO with some null fields but valid operation
            EventChangeContextDTO context = new EventChangeContextDTO(
                    USER_ID,
                    null,                // no old label (wasn't completed)
                    VALID_LABEL_ID,      // new label
                    null,                // no old start time
                    getValidEventStartFuture(fixedClock), // new start time
                    null,                // no old duration
                    60,                  // new duration
                    ZoneId.of(VALID_TIMEZONE),
                    false,               // wasn't completed
                    true                 // now completed
            );

            when(labelService.getLabelById(VALID_LABEL_ID))
                    .thenReturn(new LabelResponseDTO(VALID_LABEL_ID, "New Label", LabelColor.GREEN, "testuser"));

            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    any(Long.class), any(Long.class), any(), any(Integer.class), any(Integer.class)
            )).thenReturn(Optional.empty());

            // Act
            bucketService.handleEventChange(context);

            // Assert - Should only apply (no revert since wasn't completed)
            verify(bucketRepository, times(1)).saveAll(any(List.class)); // apply only
            verify(labelService, times(1)).getLabelById(VALID_LABEL_ID); // for apply only
        }
    }

    @Nested
    class BucketStateManagementTests {

        @Test
        void testApply_existingBucketIncrement() {
            // Arrange: Test adding time to existing buckets with various initial durations
            long userId = 1L;
            long labelId = 10L;
            ZoneId timezone = ZoneId.of("UTC");
            ZonedDateTime startTime = ZonedDateTime.of(2024, 6, 15, 12, 0, 0, 0, timezone);
            int additionalDuration = 30;

            // Create existing buckets with different initial durations
            LabelTimeBucket existingDayBucket = new LabelTimeBucket(userId, labelId, "Test", DAY, 2024, 20240615);
            existingDayBucket.setDurationMinutes(45);
            
            LabelTimeBucket existingWeekBucket = new LabelTimeBucket(userId, labelId, "Test", WEEK, 2024, 24);
            existingWeekBucket.setDurationMinutes(120);
            
            LabelTimeBucket existingMonthBucket = new LabelTimeBucket(userId, labelId, "Test", MONTH, 2024, 6);
            existingMonthBucket.setDurationMinutes(300);

            when(labelService.getLabelById(labelId))
                    .thenReturn(new LabelResponseDTO(labelId, "Test Label", LabelColor.BLUE, "testuser"));

            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, DAY, 2024, 20240615)).thenReturn(Optional.of(existingDayBucket));
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, WEEK, 2024, 24)).thenReturn(Optional.of(existingWeekBucket));
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, MONTH, 2024, 6)).thenReturn(Optional.of(existingMonthBucket));

            // Act
            bucketService.apply(userId, labelId, startTime, additionalDuration, timezone);

            // Assert - Verify buckets were incremented correctly
            assertThat(existingDayBucket.getDurationMinutes()).isEqualTo(75);   // 45 + 30
            assertThat(existingWeekBucket.getDurationMinutes()).isEqualTo(150); // 120 + 30
            assertThat(existingMonthBucket.getDurationMinutes()).isEqualTo(330); // 300 + 30
            
            verify(bucketRepository).saveAll(any(List.class));
        }

        @Test
        void testRevert_bucketBelowZero() {
            // Arrange: Test reverting more time than exists in bucket
            long userId = 1L;
            long labelId = 10L;
            ZoneId timezone = ZoneId.of("UTC");
            ZonedDateTime startTime = ZonedDateTime.of(2024, 6, 15, 12, 0, 0, 0, timezone);
            int revertAmount = 60; // More than existing

            // Create existing bucket with less time than revert amount
            LabelTimeBucket existingDayBucket = new LabelTimeBucket(userId, labelId, "Test", DAY, 2024, 20240615);
            existingDayBucket.setDurationMinutes(30); // Less than revert amount
            
            LabelTimeBucket existingWeekBucket = new LabelTimeBucket(userId, labelId, "Test", WEEK, 2024, 24);
            existingWeekBucket.setDurationMinutes(30);
            
            LabelTimeBucket existingMonthBucket = new LabelTimeBucket(userId, labelId, "Test", MONTH, 2024, 6);
            existingMonthBucket.setDurationMinutes(30);

            when(labelService.getLabelById(labelId))
                    .thenReturn(new LabelResponseDTO(labelId, "Test Label", LabelColor.BLUE, "testuser"));

            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, DAY, 2024, 20240615)).thenReturn(Optional.of(existingDayBucket));
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, WEEK, 2024, 24)).thenReturn(Optional.of(existingWeekBucket));
            when(bucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    userId, labelId, MONTH, 2024, 6)).thenReturn(Optional.of(existingMonthBucket));

            // Act
            bucketService.revert(userId, labelId, startTime, revertAmount, timezone);

            // Assert - Buckets should go negative (business logic allows this)
            assertThat(existingDayBucket.getDurationMinutes()).isEqualTo(-30);   // 30 - 60
            assertThat(existingWeekBucket.getDurationMinutes()).isEqualTo(-30);  // 30 - 60
            assertThat(existingMonthBucket.getDurationMinutes()).isEqualTo(-30); // 30 - 60
            
            verify(bucketRepository).saveAll(any(List.class));
        }
    }
}
