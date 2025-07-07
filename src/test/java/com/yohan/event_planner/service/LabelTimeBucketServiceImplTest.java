package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.LabelTimeBucket;
import com.yohan.event_planner.dto.EventChangeContextDTO;
import com.yohan.event_planner.dto.LabelResponseDTO;
import com.yohan.event_planner.repository.LabelTimeBucketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


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
                            "testuser"
                    ));

            // Act
            bucketService.handleEventChange(context);

            // Assert
            verify(bucketRepository, times(1)).saveAll(any());
            verify(bucketRepository, never()).deleteAll(any());
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
                        "testuser"
                ));

        // Act
        bucketService.handleEventChange(context);

        // Assert
        verify(bucketRepository).saveAll(any());
        verify(bucketRepository, never()).deleteAll(any());
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
                        "testuser"
                ));

        // Act
        bucketService.handleEventChange(context);

        // Assert
        verify(bucketRepository, times(2)).saveAll(any()); // revert + apply
        verify(bucketRepository, never()).deleteAll(any());
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
}
