package com.yohan.event_planner.domain.enums;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeWindowTest {

    @Nested
    class EnumValues {

        @Test
        void allTimeWindows_shouldBePresent() {
            TimeWindow[] windows = TimeWindow.values();

            assertThat(windows).hasSize(6);
            assertThat(windows).containsExactly(
                TimeWindow.TODAY, 
                TimeWindow.THIS_WEEK, 
                TimeWindow.THIS_MONTH, 
                TimeWindow.LAST_WEEK, 
                TimeWindow.LAST_MONTH, 
                TimeWindow.ALL_TIME
            );
        }

        @Test
        void valueOf_shouldReturnCorrectWindow() {
            assertThat(TimeWindow.valueOf("TODAY")).isEqualTo(TimeWindow.TODAY);
            assertThat(TimeWindow.valueOf("THIS_WEEK")).isEqualTo(TimeWindow.THIS_WEEK);
            assertThat(TimeWindow.valueOf("THIS_MONTH")).isEqualTo(TimeWindow.THIS_MONTH);
            assertThat(TimeWindow.valueOf("LAST_WEEK")).isEqualTo(TimeWindow.LAST_WEEK);
            assertThat(TimeWindow.valueOf("LAST_MONTH")).isEqualTo(TimeWindow.LAST_MONTH);
            assertThat(TimeWindow.valueOf("ALL_TIME")).isEqualTo(TimeWindow.ALL_TIME);
        }

        @Test
        void name_shouldReturnCorrectName() {
            assertThat(TimeWindow.TODAY.name()).isEqualTo("TODAY");
            assertThat(TimeWindow.THIS_WEEK.name()).isEqualTo("THIS_WEEK");
            assertThat(TimeWindow.THIS_MONTH.name()).isEqualTo("THIS_MONTH");
            assertThat(TimeWindow.LAST_WEEK.name()).isEqualTo("LAST_WEEK");
            assertThat(TimeWindow.LAST_MONTH.name()).isEqualTo("LAST_MONTH");
            assertThat(TimeWindow.ALL_TIME.name()).isEqualTo("ALL_TIME");
        }
    }

    @Nested
    class TemporalOrdering {

        @Test
        void enumOrdering_shouldReflectLogicalSequence() {
            // Order should reflect current → past → all time
            assertThat(TimeWindow.TODAY.ordinal()).isEqualTo(0);       // Current day
            assertThat(TimeWindow.THIS_WEEK.ordinal()).isEqualTo(1);   // Current week
            assertThat(TimeWindow.THIS_MONTH.ordinal()).isEqualTo(2);  // Current month
            assertThat(TimeWindow.LAST_WEEK.ordinal()).isEqualTo(3);   // Previous week
            assertThat(TimeWindow.LAST_MONTH.ordinal()).isEqualTo(4);  // Previous month
            assertThat(TimeWindow.ALL_TIME.ordinal()).isEqualTo(5);    // All time
        }

        @Test
        void currentWindows_shouldComeBefore_pastWindows() {
            // Current time windows should have lower ordinals than past windows
            assertThat(TimeWindow.TODAY.ordinal()).isLessThan(TimeWindow.LAST_WEEK.ordinal());
            assertThat(TimeWindow.THIS_WEEK.ordinal()).isLessThan(TimeWindow.LAST_WEEK.ordinal());
            assertThat(TimeWindow.THIS_MONTH.ordinal()).isLessThan(TimeWindow.LAST_MONTH.ordinal());
        }

        @Test
        void allTime_shouldBeLast() {
            // ALL_TIME should have the highest ordinal as it encompasses everything
            for (TimeWindow window : TimeWindow.values()) {
                if (window != TimeWindow.ALL_TIME) {
                    assertThat(window.ordinal()).isLessThan(TimeWindow.ALL_TIME.ordinal());
                }
            }
        }
    }

    @Nested
    class WindowSemantics {

        @Test
        void today_shouldRepresentCurrentDay() {
            TimeWindow today = TimeWindow.TODAY;
            
            assertThat(today.name()).isEqualTo("TODAY");
            assertThat(today.ordinal()).isEqualTo(0); // Most specific current window
        }

        @Test
        void thisWeek_shouldRepresentCurrentWeek() {
            TimeWindow thisWeek = TimeWindow.THIS_WEEK;
            
            assertThat(thisWeek.name()).isEqualTo("THIS_WEEK");
            assertThat(thisWeek.name()).startsWith("THIS_");
        }

        @Test
        void thisMonth_shouldRepresentCurrentMonth() {
            TimeWindow thisMonth = TimeWindow.THIS_MONTH;
            
            assertThat(thisMonth.name()).isEqualTo("THIS_MONTH");
            assertThat(thisMonth.name()).startsWith("THIS_");
        }

        @Test
        void lastWeek_shouldRepresentPreviousWeek() {
            TimeWindow lastWeek = TimeWindow.LAST_WEEK;
            
            assertThat(lastWeek.name()).isEqualTo("LAST_WEEK");
            assertThat(lastWeek.name()).startsWith("LAST_");
        }

        @Test
        void lastMonth_shouldRepresentPreviousMonth() {
            TimeWindow lastMonth = TimeWindow.LAST_MONTH;
            
            assertThat(lastMonth.name()).isEqualTo("LAST_MONTH");
            assertThat(lastMonth.name()).startsWith("LAST_");
        }

        @Test
        void allTime_shouldRepresentUnlimitedWindow() {
            TimeWindow allTime = TimeWindow.ALL_TIME;
            
            assertThat(allTime.name()).isEqualTo("ALL_TIME");
            assertThat(allTime.ordinal()).isEqualTo(TimeWindow.values().length - 1); // Last element
        }

        @Test
        void windows_shouldBeDistinct() {
            // Each window should be unique for proper filtering
            TimeWindow[] windows = TimeWindow.values();
            java.util.Set<TimeWindow> windowSet = java.util.Set.of(windows);
            
            assertThat(windowSet).hasSize(windows.length); // No duplicates
        }
    }

    @Nested
    class UserInterfaceUsage {

        @Test
        void enumComparison_shouldWorkWithSwitchStatements() {
            // Verify that windows work correctly in switch statements for UI display
            for (TimeWindow window : TimeWindow.values()) {
                String displayText = switch (window) {
                    case TODAY -> "Today";
                    case THIS_WEEK -> "This Week";
                    case THIS_MONTH -> "This Month";
                    case LAST_WEEK -> "Last Week";
                    case LAST_MONTH -> "Last Month";
                    case ALL_TIME -> "All Time";
                };
                
                assertThat(displayText).isNotNull();
                assertThat(displayText).isNotEmpty();
                assertThat(displayText).doesNotContain("_"); // User-friendly format
            }
        }

        @Test
        void windows_shouldSupportCollectionOperations() {
            java.util.Set<TimeWindow> windowSet = java.util.EnumSet.allOf(TimeWindow.class);
            
            assertThat(windowSet).hasSize(6);
            assertThat(windowSet).contains(
                TimeWindow.TODAY, 
                TimeWindow.THIS_WEEK, 
                TimeWindow.THIS_MONTH, 
                TimeWindow.LAST_WEEK, 
                TimeWindow.LAST_MONTH, 
                TimeWindow.ALL_TIME
            );
        }

        @Test
        void windows_shouldSupportCategorization() {
            // Test grouping windows by temporal category
            for (TimeWindow window : TimeWindow.values()) {
                String category = switch (window) {
                    case TODAY, THIS_WEEK, THIS_MONTH -> "current";
                    case LAST_WEEK, LAST_MONTH -> "past";
                    case ALL_TIME -> "unlimited";
                };
                
                assertThat(category).isIn("current", "past", "unlimited");
            }
        }
    }

    @Nested
    class AnalyticsIntegration {

        @Test
        void windows_shouldMapToTimeBoundaries() {
            // Test that windows can be used for analytics boundary calculation
            for (TimeWindow window : TimeWindow.values()) {
                String boundaryType = switch (window) {
                    case TODAY -> "daily boundary";
                    case THIS_WEEK, LAST_WEEK -> "weekly boundary";
                    case THIS_MONTH, LAST_MONTH -> "monthly boundary";
                    case ALL_TIME -> "no boundary";
                };
                
                assertThat(boundaryType).contains("boundary");
            }
        }

        @Test
        void currentWindows_shouldHaveThisPrefix() {
            // Current time windows should start with "THIS"
            assertThat(TimeWindow.THIS_WEEK.name()).startsWith("THIS_");
            assertThat(TimeWindow.THIS_MONTH.name()).startsWith("THIS_");
        }

        @Test
        void pastWindows_shouldHaveLastPrefix() {
            // Past time windows should start with "LAST"
            assertThat(TimeWindow.LAST_WEEK.name()).startsWith("LAST_");
            assertThat(TimeWindow.LAST_MONTH.name()).startsWith("LAST_");
        }
    }

    @Nested
    class StringRepresentation {

        @Test
        void toString_shouldReturnEnumName() {
            assertThat(TimeWindow.TODAY.toString()).isEqualTo("TODAY");
            assertThat(TimeWindow.THIS_WEEK.toString()).isEqualTo("THIS_WEEK");
            assertThat(TimeWindow.THIS_MONTH.toString()).isEqualTo("THIS_MONTH");
            assertThat(TimeWindow.LAST_WEEK.toString()).isEqualTo("LAST_WEEK");
            assertThat(TimeWindow.LAST_MONTH.toString()).isEqualTo("LAST_MONTH");
            assertThat(TimeWindow.ALL_TIME.toString()).isEqualTo("ALL_TIME");
        }

        @Test
        void enumNames_shouldBeSuitableForSerialization() {
            // Names should be simple strings suitable for JSON/database storage
            for (TimeWindow window : TimeWindow.values()) {
                String name = window.name();
                
                assertThat(name).matches("^[A-Z_]+$"); // Only uppercase letters and underscores
                assertThat(name).doesNotContain(" "); // No spaces
                assertThat(name).isNotEmpty();
                assertThat(name).isNotBlank();
            }
        }

        @Test
        void enumNames_shouldBeDescriptive() {
            // Names should clearly indicate their temporal scope
            assertThat(TimeWindow.TODAY.name()).contains("TODAY");
            assertThat(TimeWindow.THIS_WEEK.name()).contains("WEEK");
            assertThat(TimeWindow.THIS_MONTH.name()).contains("MONTH");
            assertThat(TimeWindow.LAST_WEEK.name()).contains("WEEK");
            assertThat(TimeWindow.LAST_MONTH.name()).contains("MONTH");
            assertThat(TimeWindow.ALL_TIME.name()).contains("TIME");
        }
    }

    @Nested
    class EnumEquality {

        @Test
        void enumEquality_shouldWorkCorrectly() {
            TimeWindow today1 = TimeWindow.TODAY;
            TimeWindow today2 = TimeWindow.valueOf("TODAY");

            assertThat(today1).isEqualTo(today2);
            assertThat(today1).isSameAs(today2); // Enum instances are singletons
        }

        @Test
        void enumComparison_shouldSupportOrdering() {
            // Can be used for ordering windows by time scope
            java.util.List<TimeWindow> windows = java.util.Arrays.asList(
                TimeWindow.ALL_TIME, 
                TimeWindow.TODAY, 
                TimeWindow.LAST_MONTH, 
                TimeWindow.THIS_WEEK
            );
            
            windows.sort(java.util.Comparator.comparingInt(Enum::ordinal));
            
            assertThat(windows).containsExactly(
                TimeWindow.TODAY, 
                TimeWindow.THIS_WEEK, 
                TimeWindow.LAST_MONTH, 
                TimeWindow.ALL_TIME
            );
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void valueOf_withInvalidValue_shouldThrowException() {
            assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> TimeWindow.valueOf("YESTERDAY")
            )).hasMessageContaining("YESTERDAY");
        }

        @Test
        void valueOf_withNullValue_shouldThrowException() {
            assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> TimeWindow.valueOf(null)
            )).isNotNull();
        }

        @Test
        void valueOf_withLowercaseValue_shouldThrowException() {
            assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> TimeWindow.valueOf("today")
            )).hasMessageContaining("today");
        }
    }
}