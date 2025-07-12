package com.yohan.event_planner.domain;

import com.yohan.event_planner.domain.enums.TimeBucketType;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LabelTimeBucketTest {

    private static final Long USER_ID = 1L;
    private static final Long LABEL_ID = 10L;
    private static final String LABEL_NAME = "Work Tasks";
    private static final int BUCKET_YEAR = 2024;
    private static final int BUCKET_VALUE = 15; // Week 15 or March

    private LabelTimeBucket bucket;

    @BeforeEach
    void setUp() {
        bucket = new LabelTimeBucket(
            USER_ID, LABEL_ID, LABEL_NAME, 
            TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
        );
    }

    @Nested
    class Construction {

        @Test
        void constructor_shouldSetAllProperties() {
            LabelTimeBucket newBucket = new LabelTimeBucket(
                USER_ID, LABEL_ID, LABEL_NAME,
                TimeBucketType.MONTH, 2024, 3
            );

            assertThat(newBucket.getUserId()).isEqualTo(USER_ID);
            assertThat(newBucket.getLabelId()).isEqualTo(LABEL_ID);
            assertThat(newBucket.getLabelName()).isEqualTo(LABEL_NAME);
            assertThat(newBucket.getBucketType()).isEqualTo(TimeBucketType.MONTH);
            assertThat(newBucket.getBucketYear()).isEqualTo(2024);
            assertThat(newBucket.getBucketValue()).isEqualTo(3);
            assertThat(newBucket.getDurationMinutes()).isZero();
        }

        @Test
        void constructor_shouldInitializeDurationToZero() {
            assertThat(bucket.getDurationMinutes()).isZero();
        }

        @Test
        void defaultConstructor_shouldCreateEmptyBucket() {
            LabelTimeBucket emptyBucket = new LabelTimeBucket();

            assertThat(emptyBucket.getUserId()).isNull();
            assertThat(emptyBucket.getLabelId()).isNull();
            assertThat(emptyBucket.getLabelName()).isNull();
            assertThat(emptyBucket.getBucketType()).isNull();
            assertThat(emptyBucket.getBucketYear()).isZero();
            assertThat(emptyBucket.getBucketValue()).isZero();
            assertThat(emptyBucket.getDurationMinutes()).isZero();
        }
    }

    @Nested
    class TimeAccumulation {

        @Test
        void incrementMinutes_shouldAddToExistingDuration() {
            bucket.incrementMinutes(30);

            assertThat(bucket.getDurationMinutes()).isEqualTo(30);
        }

        @Test
        void incrementMinutes_multipleIncrements_shouldAccumulate() {
            bucket.incrementMinutes(15);
            bucket.incrementMinutes(25);
            bucket.incrementMinutes(10);

            assertThat(bucket.getDurationMinutes()).isEqualTo(50);
        }

        @Test
        void incrementMinutes_withZero_shouldNotChangeDuration() {
            bucket.setDurationMinutes(100);

            bucket.incrementMinutes(0);

            assertThat(bucket.getDurationMinutes()).isEqualTo(100);
        }

        @Test
        void incrementMinutes_withNegativeValue_shouldSubtract() {
            bucket.setDurationMinutes(100);

            bucket.incrementMinutes(-30);

            assertThat(bucket.getDurationMinutes()).isEqualTo(70);
        }

        @Test
        void incrementMinutes_fromInitialZero_shouldSetCorrectValue() {
            bucket.incrementMinutes(45);

            assertThat(bucket.getDurationMinutes()).isEqualTo(45);
        }

        @Test
        void setDurationMinutes_shouldDirectlySetDuration() {
            bucket.setDurationMinutes(200);

            assertThat(bucket.getDurationMinutes()).isEqualTo(200);
        }
    }

    @Nested
    class NaturalKeyEquality {

        @Test
        void equals_withSameNaturalKey_shouldReturnTrue() {
            LabelTimeBucket bucket1 = new LabelTimeBucket(
                USER_ID, LABEL_ID, LABEL_NAME,
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );
            LabelTimeBucket bucket2 = new LabelTimeBucket(
                USER_ID, LABEL_ID, "Different Label Name", // Different label name
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );

            assertThat(bucket1).isEqualTo(bucket2);
        }

        @Test
        void equals_withDifferentUserId_shouldReturnFalse() {
            LabelTimeBucket bucket1 = new LabelTimeBucket(
                USER_ID, LABEL_ID, LABEL_NAME,
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );
            LabelTimeBucket bucket2 = new LabelTimeBucket(
                USER_ID + 1, LABEL_ID, LABEL_NAME,
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );

            assertThat(bucket1).isNotEqualTo(bucket2);
        }

        @Test
        void equals_withDifferentLabelId_shouldReturnFalse() {
            LabelTimeBucket bucket1 = new LabelTimeBucket(
                USER_ID, LABEL_ID, LABEL_NAME,
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );
            LabelTimeBucket bucket2 = new LabelTimeBucket(
                USER_ID, LABEL_ID + 1, LABEL_NAME,
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );

            assertThat(bucket1).isNotEqualTo(bucket2);
        }

        @Test
        void equals_withDifferentBucketType_shouldReturnFalse() {
            LabelTimeBucket bucket1 = new LabelTimeBucket(
                USER_ID, LABEL_ID, LABEL_NAME,
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );
            LabelTimeBucket bucket2 = new LabelTimeBucket(
                USER_ID, LABEL_ID, LABEL_NAME,
                TimeBucketType.MONTH, BUCKET_YEAR, BUCKET_VALUE
            );

            assertThat(bucket1).isNotEqualTo(bucket2);
        }

        @Test
        void equals_withDifferentBucketYear_shouldReturnFalse() {
            LabelTimeBucket bucket1 = new LabelTimeBucket(
                USER_ID, LABEL_ID, LABEL_NAME,
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );
            LabelTimeBucket bucket2 = new LabelTimeBucket(
                USER_ID, LABEL_ID, LABEL_NAME,
                TimeBucketType.WEEK, BUCKET_YEAR + 1, BUCKET_VALUE
            );

            assertThat(bucket1).isNotEqualTo(bucket2);
        }

        @Test
        void equals_withDifferentBucketValue_shouldReturnFalse() {
            LabelTimeBucket bucket1 = new LabelTimeBucket(
                USER_ID, LABEL_ID, LABEL_NAME,
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );
            LabelTimeBucket bucket2 = new LabelTimeBucket(
                USER_ID, LABEL_ID, LABEL_NAME,
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE + 1
            );

            assertThat(bucket1).isNotEqualTo(bucket2);
        }

        @Test
        void equals_withDifferentDuration_shouldReturnTrue() {
            LabelTimeBucket bucket1 = new LabelTimeBucket(
                USER_ID, LABEL_ID, LABEL_NAME,
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );
            LabelTimeBucket bucket2 = new LabelTimeBucket(
                USER_ID, LABEL_ID, LABEL_NAME,
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );
            
            bucket1.setDurationMinutes(100);
            bucket2.setDurationMinutes(200);

            // Duration should not affect equality (natural key only)
            assertThat(bucket1).isEqualTo(bucket2);
        }

        @Test
        void equals_withSelf_shouldReturnTrue() {
            assertThat(bucket).isEqualTo(bucket);
        }

        @Test
        void equals_withNull_shouldReturnFalse() {
            assertThat(bucket).isNotEqualTo(null);
        }

        @Test
        void equals_withDifferentClass_shouldReturnFalse() {
            assertThat(bucket).isNotEqualTo("not a time bucket");
        }
    }

    @Nested
    class Hashing {

        @Test
        void hashCode_shouldBeConsistentWithEquals() {
            LabelTimeBucket bucket1 = new LabelTimeBucket(
                USER_ID, LABEL_ID, LABEL_NAME,
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );
            LabelTimeBucket bucket2 = new LabelTimeBucket(
                USER_ID, LABEL_ID, "Different Name",
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );

            assertThat(bucket1.hashCode()).isEqualTo(bucket2.hashCode());
        }

        @Test
        void hashCode_shouldNotIncludeDuration() {
            LabelTimeBucket bucket1 = new LabelTimeBucket(
                USER_ID, LABEL_ID, LABEL_NAME,
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );
            LabelTimeBucket bucket2 = new LabelTimeBucket(
                USER_ID, LABEL_ID, LABEL_NAME,
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );
            
            bucket1.setDurationMinutes(100);
            bucket2.setDurationMinutes(200);

            assertThat(bucket1.hashCode()).isEqualTo(bucket2.hashCode());
        }

        @Test
        void hashCode_withDifferentNaturalKey_shouldReturnDifferentHash() {
            LabelTimeBucket bucket1 = new LabelTimeBucket(
                USER_ID, LABEL_ID, LABEL_NAME,
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );
            LabelTimeBucket bucket2 = new LabelTimeBucket(
                USER_ID, LABEL_ID + 1, LABEL_NAME,
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );

            assertThat(bucket1.hashCode()).isNotEqualTo(bucket2.hashCode());
        }
    }

    @Nested
    class PropertySetters {

        @Test
        void setUserId_shouldUpdateUserId() {
            bucket.setUserId(999L);

            assertThat(bucket.getUserId()).isEqualTo(999L);
        }

        @Test
        void setLabelId_shouldUpdateLabelId() {
            bucket.setLabelId(999L);

            assertThat(bucket.getLabelId()).isEqualTo(999L);
        }

        @Test
        void setBucketType_shouldUpdateBucketType() {
            bucket.setBucketType(TimeBucketType.MONTH);

            assertThat(bucket.getBucketType()).isEqualTo(TimeBucketType.MONTH);
        }

        @Test
        void setBucketYear_shouldUpdateBucketYear() {
            bucket.setBucketYear(2025);

            assertThat(bucket.getBucketYear()).isEqualTo(2025);
        }

        @Test
        void setBucketValue_shouldUpdateBucketValue() {
            bucket.setBucketValue(52);

            assertThat(bucket.getBucketValue()).isEqualTo(52);
        }

        @Test
        void setDurationMinutes_shouldUpdateDuration() {
            bucket.setDurationMinutes(300);

            assertThat(bucket.getDurationMinutes()).isEqualTo(300);
        }
    }

    @Nested
    class TimeBucketTypes {

        @Test
        void weekBucket_shouldHaveCorrectProperties() {
            LabelTimeBucket weekBucket = TestUtils.createValidWeekBucket(
                USER_ID, LABEL_ID, LABEL_NAME, 2024, 15, 120
            );

            assertThat(weekBucket.getBucketType()).isEqualTo(TimeBucketType.WEEK);
            assertThat(weekBucket.getBucketYear()).isEqualTo(2024);
            assertThat(weekBucket.getBucketValue()).isEqualTo(15); // Week 15
            assertThat(weekBucket.getDurationMinutes()).isEqualTo(120);
        }

        @Test
        void monthBucket_shouldHaveCorrectProperties() {
            LabelTimeBucket monthBucket = TestUtils.createValidMonthBucket(
                USER_ID, LABEL_ID, LABEL_NAME, 2024, 3, 480
            );

            assertThat(monthBucket.getBucketType()).isEqualTo(TimeBucketType.MONTH);
            assertThat(monthBucket.getBucketYear()).isEqualTo(2024);
            assertThat(monthBucket.getBucketValue()).isEqualTo(3); // March
            assertThat(monthBucket.getDurationMinutes()).isEqualTo(480);
        }

        @Test
        void dayBucket_shouldHaveCorrectProperties() {
            LabelTimeBucket dayBucket = TestUtils.createValidDayBucket(
                USER_ID, LABEL_ID, LABEL_NAME, 2024, 75, 60
            );

            assertThat(dayBucket.getBucketType()).isEqualTo(TimeBucketType.DAY);
            assertThat(dayBucket.getBucketYear()).isEqualTo(2024);
            assertThat(dayBucket.getBucketValue()).isEqualTo(75); // Day 75 of year
            assertThat(dayBucket.getDurationMinutes()).isEqualTo(60);
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void incrementMinutes_withLargeValue_shouldWork() {
            bucket.incrementMinutes(Integer.MAX_VALUE - 1000);

            assertThat(bucket.getDurationMinutes()).isEqualTo(Integer.MAX_VALUE - 1000);
        }

        @Test
        void incrementMinutes_multipleSmallIncrements_shouldAccumulate() {
            for (int i = 0; i < 100; i++) {
                bucket.incrementMinutes(1);
            }

            assertThat(bucket.getDurationMinutes()).isEqualTo(100);
        }

        @Test
        void equals_withNullFields_shouldHandleGracefully() {
            LabelTimeBucket bucket1 = new LabelTimeBucket();
            LabelTimeBucket bucket2 = new LabelTimeBucket();

            assertThat(bucket1).isEqualTo(bucket2);
        }

        @Test
        void hashCode_withNullFields_shouldNotThrow() {
            LabelTimeBucket bucketWithNulls = new LabelTimeBucket();

            assertThat(bucketWithNulls.hashCode()).isNotNull();
        }
    }

    @Nested
    class LabelNameSnapshot {

        @Test
        void labelName_shouldBeIncludedInConstruction() {
            String customLabelName = "Custom Work Label";
            LabelTimeBucket bucket = new LabelTimeBucket(
                USER_ID, LABEL_ID, customLabelName,
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );

            assertThat(bucket.getLabelName()).isEqualTo(customLabelName);
        }

        @Test
        void labelName_shouldNotAffectEquality() {
            LabelTimeBucket bucket1 = new LabelTimeBucket(
                USER_ID, LABEL_ID, "Original Name",
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );
            LabelTimeBucket bucket2 = new LabelTimeBucket(
                USER_ID, LABEL_ID, "Changed Name",
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );

            // Label name should not affect equality (only natural key)
            assertThat(bucket1).isEqualTo(bucket2);
        }

        @Test
        void labelName_shouldNotAffectHashCode() {
            LabelTimeBucket bucket1 = new LabelTimeBucket(
                USER_ID, LABEL_ID, "Original Name",
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );
            LabelTimeBucket bucket2 = new LabelTimeBucket(
                USER_ID, LABEL_ID, "Changed Name",
                TimeBucketType.WEEK, BUCKET_YEAR, BUCKET_VALUE
            );

            assertThat(bucket1.hashCode()).isEqualTo(bucket2.hashCode());
        }
    }
}