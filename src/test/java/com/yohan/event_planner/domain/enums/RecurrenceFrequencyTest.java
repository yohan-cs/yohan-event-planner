package com.yohan.event_planner.domain.enums;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecurrenceFrequencyTest {

    @Nested
    class EnumValues {

        @Test
        void allFrequencies_shouldBePresent() {
            RecurrenceFrequency[] frequencies = RecurrenceFrequency.values();

            assertThat(frequencies).hasSize(3);
            assertThat(frequencies).containsExactly(
                RecurrenceFrequency.DAILY, 
                RecurrenceFrequency.WEEKLY, 
                RecurrenceFrequency.MONTHLY
            );
        }

        @Test
        void valueOf_shouldReturnCorrectFrequency() {
            assertThat(RecurrenceFrequency.valueOf("DAILY")).isEqualTo(RecurrenceFrequency.DAILY);
            assertThat(RecurrenceFrequency.valueOf("WEEKLY")).isEqualTo(RecurrenceFrequency.WEEKLY);
            assertThat(RecurrenceFrequency.valueOf("MONTHLY")).isEqualTo(RecurrenceFrequency.MONTHLY);
        }

        @Test
        void name_shouldReturnCorrectName() {
            assertThat(RecurrenceFrequency.DAILY.name()).isEqualTo("DAILY");
            assertThat(RecurrenceFrequency.WEEKLY.name()).isEqualTo("WEEKLY");
            assertThat(RecurrenceFrequency.MONTHLY.name()).isEqualTo("MONTHLY");
        }
    }

    @Nested
    class TemporalOrdering {

        @Test
        void enumOrdering_shouldReflectFrequency() {
            // Order should be from most frequent to least frequent
            assertThat(RecurrenceFrequency.DAILY.ordinal()).isEqualTo(0);    // Most frequent
            assertThat(RecurrenceFrequency.WEEKLY.ordinal()).isEqualTo(1);   // Medium frequency
            assertThat(RecurrenceFrequency.MONTHLY.ordinal()).isEqualTo(2);  // Least frequent
        }

        @Test
        void frequencyHierarchy_shouldBeLogical() {
            // DAILY is more frequent than WEEKLY, which is more frequent than MONTHLY
            assertThat(RecurrenceFrequency.DAILY.ordinal()).isLessThan(RecurrenceFrequency.WEEKLY.ordinal());
            assertThat(RecurrenceFrequency.WEEKLY.ordinal()).isLessThan(RecurrenceFrequency.MONTHLY.ordinal());
        }
    }

    @Nested
    class BusinessSemantics {

        @Test
        void daily_shouldRepresentHighestFrequency() {
            RecurrenceFrequency daily = RecurrenceFrequency.DAILY;
            
            assertThat(daily.name()).isEqualTo("DAILY");
            assertThat(daily.ordinal()).isEqualTo(0); // Highest frequency, lowest ordinal
        }

        @Test
        void weekly_shouldRepresentMediumFrequency() {
            RecurrenceFrequency weekly = RecurrenceFrequency.WEEKLY;
            
            assertThat(weekly.name()).isEqualTo("WEEKLY");
            assertThat(weekly.ordinal()).isEqualTo(1); // Medium frequency
        }

        @Test
        void monthly_shouldRepresentLowestFrequency() {
            RecurrenceFrequency monthly = RecurrenceFrequency.MONTHLY;
            
            assertThat(monthly.name()).isEqualTo("MONTHLY");
            assertThat(monthly.ordinal()).isEqualTo(2); // Lowest frequency, highest ordinal
        }

        @Test
        void frequencies_shouldBeDistinct() {
            // Each frequency should be unique for proper pattern matching
            assertThat(RecurrenceFrequency.DAILY).isNotEqualTo(RecurrenceFrequency.WEEKLY);
            assertThat(RecurrenceFrequency.WEEKLY).isNotEqualTo(RecurrenceFrequency.MONTHLY);
            assertThat(RecurrenceFrequency.DAILY).isNotEqualTo(RecurrenceFrequency.MONTHLY);
        }
    }

    @Nested
    class RecurrencePatternUsage {

        @Test
        void enumComparison_shouldWorkWithSwitchStatements() {
            // Verify that frequencies work correctly in switch statements for interval calculation
            for (RecurrenceFrequency frequency : RecurrenceFrequency.values()) {
                String intervalDescription = switch (frequency) {
                    case DAILY -> "every day";
                    case WEEKLY -> "every week";
                    case MONTHLY -> "every month";
                };
                
                assertThat(intervalDescription).isNotNull();
                assertThat(intervalDescription).isIn("every day", "every week", "every month");
            }
        }

        @Test
        void frequencies_shouldSupportCollectionOperations() {
            java.util.Set<RecurrenceFrequency> frequencySet = java.util.EnumSet.allOf(RecurrenceFrequency.class);
            
            assertThat(frequencySet).hasSize(3);
            assertThat(frequencySet).contains(
                RecurrenceFrequency.DAILY, 
                RecurrenceFrequency.WEEKLY, 
                RecurrenceFrequency.MONTHLY
            );
        }

        @Test
        void frequencies_shouldWorkInRRuleContext() {
            // Test that frequencies can be used for RRule-style operations
            for (RecurrenceFrequency frequency : RecurrenceFrequency.values()) {
                String rrulePrefix = switch (frequency) {
                    case DAILY -> "FREQ=DAILY";
                    case WEEKLY -> "FREQ=WEEKLY";
                    case MONTHLY -> "FREQ=MONTHLY";
                };
                
                assertThat(rrulePrefix).startsWith("FREQ=");
                assertThat(rrulePrefix).contains(frequency.name());
            }
        }
    }

    @Nested
    class StringRepresentation {

        @Test
        void toString_shouldReturnEnumName() {
            assertThat(RecurrenceFrequency.DAILY.toString()).isEqualTo("DAILY");
            assertThat(RecurrenceFrequency.WEEKLY.toString()).isEqualTo("WEEKLY");
            assertThat(RecurrenceFrequency.MONTHLY.toString()).isEqualTo("MONTHLY");
        }

        @Test
        void enumNames_shouldBeSuitableForSerialization() {
            // Names should be simple strings suitable for JSON/database storage
            for (RecurrenceFrequency frequency : RecurrenceFrequency.values()) {
                String name = frequency.name();
                
                assertThat(name).matches("^[A-Z]+$"); // Only uppercase letters
                assertThat(name).doesNotContain(" "); // No spaces
                assertThat(name).doesNotContain("_"); // No underscores in these particular enums
                assertThat(name).isNotEmpty();
                assertThat(name).isNotBlank();
                assertThat(name).endsWith("LY"); // All end with "LY" (DAILY, WEEKLY, MONTHLY)
            }
        }
    }

    @Nested
    class EnumEquality {

        @Test
        void enumEquality_shouldWorkCorrectly() {
            RecurrenceFrequency daily1 = RecurrenceFrequency.DAILY;
            RecurrenceFrequency daily2 = RecurrenceFrequency.valueOf("DAILY");

            assertThat(daily1).isEqualTo(daily2);
            assertThat(daily1).isSameAs(daily2); // Enum instances are singletons
        }

        @Test
        void enumComparison_shouldSupportNaturalOrdering() {
            // Can be used for sorting or comparison if needed
            java.util.List<RecurrenceFrequency> frequencies = java.util.Arrays.asList(
                RecurrenceFrequency.MONTHLY, 
                RecurrenceFrequency.DAILY, 
                RecurrenceFrequency.WEEKLY
            );
            
            frequencies.sort(java.util.Comparator.comparingInt(Enum::ordinal));
            
            assertThat(frequencies).containsExactly(
                RecurrenceFrequency.DAILY, 
                RecurrenceFrequency.WEEKLY, 
                RecurrenceFrequency.MONTHLY
            );
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void valueOf_withInvalidValue_shouldThrowException() {
            assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> RecurrenceFrequency.valueOf("YEARLY")
            )).hasMessageContaining("YEARLY");
        }

        @Test
        void valueOf_withNullValue_shouldThrowException() {
            assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> RecurrenceFrequency.valueOf(null)
            )).isNotNull();
        }

        @Test
        void valueOf_withLowercaseValue_shouldThrowException() {
            assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> RecurrenceFrequency.valueOf("daily")
            )).hasMessageContaining("daily");
        }
    }
}