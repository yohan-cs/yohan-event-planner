package com.yohan.event_planner.domain.enums;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeBucketTypeTest {

    @Nested
    class EnumValues {

        @Test
        void allBucketTypes_shouldBePresent() {
            TimeBucketType[] types = TimeBucketType.values();

            assertThat(types).hasSize(3);
            assertThat(types).containsExactly(
                TimeBucketType.DAY, 
                TimeBucketType.WEEK, 
                TimeBucketType.MONTH
            );
        }

        @Test
        void valueOf_shouldReturnCorrectType() {
            assertThat(TimeBucketType.valueOf("DAY")).isEqualTo(TimeBucketType.DAY);
            assertThat(TimeBucketType.valueOf("WEEK")).isEqualTo(TimeBucketType.WEEK);
            assertThat(TimeBucketType.valueOf("MONTH")).isEqualTo(TimeBucketType.MONTH);
        }

        @Test
        void name_shouldReturnCorrectName() {
            assertThat(TimeBucketType.DAY.name()).isEqualTo("DAY");
            assertThat(TimeBucketType.WEEK.name()).isEqualTo("WEEK");
            assertThat(TimeBucketType.MONTH.name()).isEqualTo("MONTH");
        }
    }

    @Nested
    class GranularityOrdering {

        @Test
        void enumOrdering_shouldReflectGranularity() {
            // Order should be from highest granularity to lowest granularity
            assertThat(TimeBucketType.DAY.ordinal()).isEqualTo(0);    // Highest granularity
            assertThat(TimeBucketType.WEEK.ordinal()).isEqualTo(1);   // Medium granularity
            assertThat(TimeBucketType.MONTH.ordinal()).isEqualTo(2);  // Lowest granularity
        }

        @Test
        void granularityHierarchy_shouldBeLogical() {
            // DAY has higher granularity than WEEK, which has higher granularity than MONTH
            assertThat(TimeBucketType.DAY.ordinal()).isLessThan(TimeBucketType.WEEK.ordinal());
            assertThat(TimeBucketType.WEEK.ordinal()).isLessThan(TimeBucketType.MONTH.ordinal());
        }
    }

    @Nested
    class BusinessSemantics {

        @Test
        void day_shouldRepresentHighestGranularity() {
            TimeBucketType day = TimeBucketType.DAY;
            
            assertThat(day.name()).isEqualTo("DAY");
            assertThat(day.ordinal()).isEqualTo(0); // Highest granularity, lowest ordinal
        }

        @Test
        void week_shouldRepresentMediumGranularity() {
            TimeBucketType week = TimeBucketType.WEEK;
            
            assertThat(week.name()).isEqualTo("WEEK");
            assertThat(week.ordinal()).isEqualTo(1); // Medium granularity
        }

        @Test
        void month_shouldRepresentLowestGranularity() {
            TimeBucketType month = TimeBucketType.MONTH;
            
            assertThat(month.name()).isEqualTo("MONTH");
            assertThat(month.ordinal()).isEqualTo(2); // Lowest granularity, highest ordinal
        }

        @Test
        void bucketTypes_shouldBeDistinct() {
            // Each bucket type should be unique for proper aggregation strategy
            assertThat(TimeBucketType.DAY).isNotEqualTo(TimeBucketType.WEEK);
            assertThat(TimeBucketType.WEEK).isNotEqualTo(TimeBucketType.MONTH);
            assertThat(TimeBucketType.DAY).isNotEqualTo(TimeBucketType.MONTH);
        }
    }

    @Nested
    class AnalyticsUsage {

        @Test
        void enumComparison_shouldWorkWithSwitchStatements() {
            // Verify that bucket types work correctly in switch statements for aggregation logic
            for (TimeBucketType type : TimeBucketType.values()) {
                String aggregationStrategy = switch (type) {
                    case DAY -> "daily aggregation";
                    case WEEK -> "weekly aggregation";
                    case MONTH -> "monthly aggregation";
                };
                
                assertThat(aggregationStrategy).isNotNull();
                assertThat(aggregationStrategy).isIn("daily aggregation", "weekly aggregation", "monthly aggregation");
            }
        }

        @Test
        void bucketTypes_shouldSupportCollectionOperations() {
            java.util.Set<TimeBucketType> bucketSet = java.util.EnumSet.allOf(TimeBucketType.class);
            
            assertThat(bucketSet).hasSize(3);
            assertThat(bucketSet).contains(
                TimeBucketType.DAY, 
                TimeBucketType.WEEK, 
                TimeBucketType.MONTH
            );
        }

        @Test
        void bucketTypes_shouldSupportStorageEfficiencyAnalysis() {
            // Test that bucket types can be ordered by storage efficiency (records generated)
            for (TimeBucketType type : TimeBucketType.values()) {
                String storageProfile = switch (type) {
                    case DAY -> "high volume, detailed";
                    case WEEK -> "moderate volume, balanced";
                    case MONTH -> "low volume, summary";
                };
                
                assertThat(storageProfile).contains("volume");
            }
        }
    }

    @Nested
    class TimeAggregationSemantics {

        @Test
        void day_shouldImplyDailyBoundaries() {
            // DAY bucket type should be used for calendar day boundaries
            TimeBucketType day = TimeBucketType.DAY;
            
            assertThat(day.name()).isEqualTo("DAY");
            // In real usage, this would aggregate by calendar day (midnight to midnight)
        }

        @Test
        void week_shouldImplyISOWeekBoundaries() {
            // WEEK bucket type should use ISO week numbering (Monday start)
            TimeBucketType week = TimeBucketType.WEEK;
            
            assertThat(week.name()).isEqualTo("WEEK");
            // In real usage, this would aggregate by ISO week (Monday to Sunday)
        }

        @Test
        void month_shouldImplyCalendarMonthBoundaries() {
            // MONTH bucket type should use calendar month boundaries
            TimeBucketType month = TimeBucketType.MONTH;
            
            assertThat(month.name()).isEqualTo("MONTH");
            // In real usage, this would aggregate by calendar month (1st to last day)
        }
    }

    @Nested
    class StringRepresentation {

        @Test
        void toString_shouldReturnEnumName() {
            assertThat(TimeBucketType.DAY.toString()).isEqualTo("DAY");
            assertThat(TimeBucketType.WEEK.toString()).isEqualTo("WEEK");
            assertThat(TimeBucketType.MONTH.toString()).isEqualTo("MONTH");
        }

        @Test
        void enumNames_shouldBeSuitableForSerialization() {
            // Names should be simple strings suitable for JSON/database storage
            for (TimeBucketType type : TimeBucketType.values()) {
                String name = type.name();
                
                assertThat(name).matches("^[A-Z]+$"); // Only uppercase letters
                assertThat(name).doesNotContain(" "); // No spaces
                assertThat(name).doesNotContain("_"); // No underscores
                assertThat(name).isNotEmpty();
                assertThat(name).isNotBlank();
                assertThat(name).hasSizeLessThanOrEqualTo(10); // Reasonable length for database storage
            }
        }
    }

    @Nested
    class EnumEquality {

        @Test
        void enumEquality_shouldWorkCorrectly() {
            TimeBucketType day1 = TimeBucketType.DAY;
            TimeBucketType day2 = TimeBucketType.valueOf("DAY");

            assertThat(day1).isEqualTo(day2);
            assertThat(day1).isSameAs(day2); // Enum instances are singletons
        }

        @Test
        void enumComparison_shouldSupportOrdering() {
            // Can be used for sorting by granularity if needed
            java.util.List<TimeBucketType> types = java.util.Arrays.asList(
                TimeBucketType.MONTH, 
                TimeBucketType.DAY, 
                TimeBucketType.WEEK
            );
            
            types.sort(java.util.Comparator.comparingInt(Enum::ordinal));
            
            assertThat(types).containsExactly(
                TimeBucketType.DAY, 
                TimeBucketType.WEEK, 
                TimeBucketType.MONTH
            );
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void valueOf_withInvalidValue_shouldThrowException() {
            assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> TimeBucketType.valueOf("YEAR")
            )).hasMessageContaining("YEAR");
        }

        @Test
        void valueOf_withNullValue_shouldThrowException() {
            assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> TimeBucketType.valueOf(null)
            )).isNotNull();
        }

        @Test
        void valueOf_withLowercaseValue_shouldThrowException() {
            assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> TimeBucketType.valueOf("day")
            )).hasMessageContaining("day");
        }
    }
}