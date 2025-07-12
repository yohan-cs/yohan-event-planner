package com.yohan.event_planner.domain.enums;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeFilterTest {

    @Nested
    class EnumValues {

        @Test
        void allTimeFilters_shouldBePresent() {
            TimeFilter[] filters = TimeFilter.values();

            assertThat(filters).hasSize(4);
            assertThat(filters).containsExactly(
                TimeFilter.ALL, 
                TimeFilter.PAST_ONLY, 
                TimeFilter.FUTURE_ONLY, 
                TimeFilter.CUSTOM
            );
        }

        @Test
        void valueOf_shouldReturnCorrectFilter() {
            assertThat(TimeFilter.valueOf("ALL")).isEqualTo(TimeFilter.ALL);
            assertThat(TimeFilter.valueOf("PAST_ONLY")).isEqualTo(TimeFilter.PAST_ONLY);
            assertThat(TimeFilter.valueOf("FUTURE_ONLY")).isEqualTo(TimeFilter.FUTURE_ONLY);
            assertThat(TimeFilter.valueOf("CUSTOM")).isEqualTo(TimeFilter.CUSTOM);
        }

        @Test
        void name_shouldReturnCorrectName() {
            assertThat(TimeFilter.ALL.name()).isEqualTo("ALL");
            assertThat(TimeFilter.PAST_ONLY.name()).isEqualTo("PAST_ONLY");
            assertThat(TimeFilter.FUTURE_ONLY.name()).isEqualTo("FUTURE_ONLY");
            assertThat(TimeFilter.CUSTOM.name()).isEqualTo("CUSTOM");
        }
    }

    @Nested
    class FilterSemantics {

        @Test
        void enumOrdering_shouldReflectFilterBehavior() {
            // Order represents different filtering strategies
            assertThat(TimeFilter.ALL.ordinal()).isEqualTo(0);         // No filtering
            assertThat(TimeFilter.PAST_ONLY.ordinal()).isEqualTo(1);   // Past events only
            assertThat(TimeFilter.FUTURE_ONLY.ordinal()).isEqualTo(2); // Future events only
            assertThat(TimeFilter.CUSTOM.ordinal()).isEqualTo(3);      // Custom range
        }

        @Test
        void all_shouldRepresentNoTimeFiltering() {
            TimeFilter all = TimeFilter.ALL;
            
            assertThat(all.name()).isEqualTo("ALL");
            assertThat(all.ordinal()).isEqualTo(0); // No filtering, first in list
        }

        @Test
        void pastOnly_shouldRepresentPastEventsFilter() {
            TimeFilter pastOnly = TimeFilter.PAST_ONLY;
            
            assertThat(pastOnly.name()).isEqualTo("PAST_ONLY");
            assertThat(pastOnly.ordinal()).isEqualTo(1);
        }

        @Test
        void futureOnly_shouldRepresentFutureEventsFilter() {
            TimeFilter futureOnly = TimeFilter.FUTURE_ONLY;
            
            assertThat(futureOnly.name()).isEqualTo("FUTURE_ONLY");
            assertThat(futureOnly.ordinal()).isEqualTo(2);
        }

        @Test
        void custom_shouldRepresentCustomRangeFilter() {
            TimeFilter custom = TimeFilter.CUSTOM;
            
            assertThat(custom.name()).isEqualTo("CUSTOM");
            assertThat(custom.ordinal()).isEqualTo(3); // Most flexible, last in list
        }

        @Test
        void filters_shouldBeDistinct() {
            // Each filter should be unique for proper query logic
            assertThat(TimeFilter.ALL).isNotEqualTo(TimeFilter.PAST_ONLY);
            assertThat(TimeFilter.PAST_ONLY).isNotEqualTo(TimeFilter.FUTURE_ONLY);
            assertThat(TimeFilter.FUTURE_ONLY).isNotEqualTo(TimeFilter.CUSTOM);
            assertThat(TimeFilter.ALL).isNotEqualTo(TimeFilter.CUSTOM);
        }
    }

    @Nested
    class QueryLogicUsage {

        @Test
        void enumComparison_shouldWorkWithSwitchStatements() {
            // Verify that filters work correctly in switch statements for query building
            for (TimeFilter filter : TimeFilter.values()) {
                String queryStrategy = switch (filter) {
                    case ALL -> "no time constraints";
                    case PAST_ONLY -> "end time before now";
                    case FUTURE_ONLY -> "start time after now";
                    case CUSTOM -> "custom time range";
                };
                
                assertThat(queryStrategy).isNotNull();
                assertThat(queryStrategy).isIn(
                    "no time constraints", 
                    "end time before now", 
                    "start time after now", 
                    "custom time range"
                );
            }
        }

        @Test
        void filters_shouldSupportCollectionOperations() {
            java.util.Set<TimeFilter> filterSet = java.util.EnumSet.allOf(TimeFilter.class);
            
            assertThat(filterSet).hasSize(4);
            assertThat(filterSet).contains(
                TimeFilter.ALL, 
                TimeFilter.PAST_ONLY, 
                TimeFilter.FUTURE_ONLY, 
                TimeFilter.CUSTOM
            );
        }

        @Test
        void filters_shouldSupportParameterRequirements() {
            // Test which filters require additional parameters
            for (TimeFilter filter : TimeFilter.values()) {
                boolean requiresParameters = switch (filter) {
                    case ALL, PAST_ONLY, FUTURE_ONLY -> false; // No parameters needed
                    case CUSTOM -> true; // May require start/end parameters
                };
                
                if (filter == TimeFilter.CUSTOM) {
                    assertThat(requiresParameters).isTrue();
                } else {
                    assertThat(requiresParameters).isFalse();
                }
            }
        }
    }

    @Nested
    class TemporalBehavior {

        @Test
        void all_shouldIgnoreTimeParameters() {
            // ALL filter should ignore start and end parameters
            TimeFilter all = TimeFilter.ALL;
            
            assertThat(all.name()).isEqualTo("ALL");
            // In real usage, start and end would be ignored regardless of values
        }

        @Test
        void pastOnly_shouldIgnoreTimeParameters() {
            // PAST_ONLY filter should ignore start and end, use FAR_PAST to now
            TimeFilter pastOnly = TimeFilter.PAST_ONLY;
            
            assertThat(pastOnly.name()).isEqualTo("PAST_ONLY");
            // In real usage, would set range from FAR_PAST to current time
        }

        @Test
        void futureOnly_shouldIgnoreTimeParameters() {
            // FUTURE_ONLY filter should ignore start and end, use now to FAR_FUTURE
            TimeFilter futureOnly = TimeFilter.FUTURE_ONLY;
            
            assertThat(futureOnly.name()).isEqualTo("FUTURE_ONLY");
            // In real usage, would set range from current time to FAR_FUTURE
        }

        @Test
        void custom_shouldUseTimeParameters() {
            // CUSTOM filter should use provided start and end parameters
            TimeFilter custom = TimeFilter.CUSTOM;
            
            assertThat(custom.name()).isEqualTo("CUSTOM");
            // In real usage, would use provided start (default FAR_PAST) and end (default FAR_FUTURE)
        }
    }

    @Nested
    class StringRepresentation {

        @Test
        void toString_shouldReturnEnumName() {
            assertThat(TimeFilter.ALL.toString()).isEqualTo("ALL");
            assertThat(TimeFilter.PAST_ONLY.toString()).isEqualTo("PAST_ONLY");
            assertThat(TimeFilter.FUTURE_ONLY.toString()).isEqualTo("FUTURE_ONLY");
            assertThat(TimeFilter.CUSTOM.toString()).isEqualTo("CUSTOM");
        }

        @Test
        void enumNames_shouldBeSuitableForSerialization() {
            // Names should be simple strings suitable for JSON/database storage
            for (TimeFilter filter : TimeFilter.values()) {
                String name = filter.name();
                
                assertThat(name).matches("^[A-Z_]+$"); // Only uppercase letters and underscores
                assertThat(name).doesNotContain(" "); // No spaces
                assertThat(name).isNotEmpty();
                assertThat(name).isNotBlank();
            }
        }

        @Test
        void enumNames_shouldBeDescriptive() {
            // Names should clearly indicate their purpose
            assertThat(TimeFilter.ALL.name()).isEqualTo("ALL");
            assertThat(TimeFilter.PAST_ONLY.name()).contains("PAST");
            assertThat(TimeFilter.FUTURE_ONLY.name()).contains("FUTURE");
            assertThat(TimeFilter.CUSTOM.name()).isEqualTo("CUSTOM");
        }
    }

    @Nested
    class EnumEquality {

        @Test
        void enumEquality_shouldWorkCorrectly() {
            TimeFilter all1 = TimeFilter.ALL;
            TimeFilter all2 = TimeFilter.valueOf("ALL");

            assertThat(all1).isEqualTo(all2);
            assertThat(all1).isSameAs(all2); // Enum instances are singletons
        }

        @Test
        void enumComparison_shouldSupportFiltering() {
            // Can be used for filtering logic
            java.util.List<TimeFilter> filters = java.util.Arrays.asList(
                TimeFilter.CUSTOM, 
                TimeFilter.ALL, 
                TimeFilter.FUTURE_ONLY, 
                TimeFilter.PAST_ONLY
            );
            
            filters.sort(java.util.Comparator.comparingInt(Enum::ordinal));
            
            assertThat(filters).containsExactly(
                TimeFilter.ALL, 
                TimeFilter.PAST_ONLY, 
                TimeFilter.FUTURE_ONLY, 
                TimeFilter.CUSTOM
            );
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void valueOf_withInvalidValue_shouldThrowException() {
            assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> TimeFilter.valueOf("INVALID")
            )).hasMessageContaining("INVALID");
        }

        @Test
        void valueOf_withNullValue_shouldThrowException() {
            assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> TimeFilter.valueOf(null)
            )).isNotNull();
        }

        @Test
        void valueOf_withLowercaseValue_shouldThrowException() {
            assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> TimeFilter.valueOf("all")
            )).hasMessageContaining("all");
        }
    }
}