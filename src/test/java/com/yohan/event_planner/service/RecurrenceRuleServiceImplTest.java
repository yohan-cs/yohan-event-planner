package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.enums.RecurrenceFrequency;
import com.yohan.event_planner.exception.InvalidRecurrenceRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecurrenceRuleServiceImplTest {

    private RecurrenceRuleServiceImpl recurrenceRuleService;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        recurrenceRuleService = new RecurrenceRuleServiceImpl();
        startDate = LocalDate.of(2025, 6, 1); // Changed to LocalDate
        endDate = LocalDate.of(2025, 9, 1);  // Changed to LocalDate
    }

    @Nested
    class ParseFromStringTests {

        @Test
        void parseFromString_dailySuccess() {
            // Arrange
            String input = "DAILY:";

            // Act
            var result = recurrenceRuleService.parseFromString(input);

            // Assert
            assertEquals(RecurrenceFrequency.DAILY, result.frequency());

            Set<DayOfWeek> days = result.daysOfWeek();
            assertNotNull(days);
            assertEquals(7, days.size());

            for (DayOfWeek day : DayOfWeek.values()) {
                assertTrue(days.contains(day));
            }

            assertNull(result.ordinal());
        }

        @Test
        void parseFromString_weeklySuccess() {
            // Arrange
            String input = "WEEKLY:MONDAY,WEDNESDAY,FRIDAY";

            // Act
            var result = recurrenceRuleService.parseFromString(input);

            // Assert
            assertEquals(RecurrenceFrequency.WEEKLY, result.frequency());

            Set<DayOfWeek> days = result.daysOfWeek();
            assertNotNull(days);

            assertTrue(days.contains(DayOfWeek.MONDAY));
            assertTrue(days.contains(DayOfWeek.WEDNESDAY));
            assertTrue(days.contains(DayOfWeek.FRIDAY));

            // Ensure only these days are included
            for (DayOfWeek day : DayOfWeek.values()) {
                if (day != DayOfWeek.MONDAY && day != DayOfWeek.WEDNESDAY && day != DayOfWeek.FRIDAY) {
                    assertFalse(days.contains(day));
                }
            }

            assertNull(result.ordinal());
        }

        @Test
        void parseFromString_monthlySuccess() {
            // Arrange
            String input = "MONTHLY:2:MONDAY,THURSDAY";

            // Act
            var result = recurrenceRuleService.parseFromString(input);

            // Assert
            assertEquals(RecurrenceFrequency.MONTHLY, result.frequency());
            assertEquals(2, result.ordinal());

            Set<DayOfWeek> days = result.daysOfWeek();
            assertNotNull(days);

            assertTrue(days.contains(DayOfWeek.MONDAY));
            assertTrue(days.contains(DayOfWeek.THURSDAY));

            // Ensure only these days are included
            for (DayOfWeek day : DayOfWeek.values()) {
                if (day != DayOfWeek.MONDAY && day != DayOfWeek.THURSDAY) {
                    assertFalse(days.contains(day));
                }
            }
        }

        @Test
        void parseFromString_invalidFormat_throwsException() {
            assertThrows(InvalidRecurrenceRuleException.class, () -> recurrenceRuleService.parseFromString("UNKNOWN"));
            assertThrows(InvalidRecurrenceRuleException.class, () -> recurrenceRuleService.parseFromString("WEEKLY:MONDAY,FRIDAY,INVALIDDAY"));
            assertThrows(InvalidRecurrenceRuleException.class, () -> recurrenceRuleService.parseFromString("MONTHLY:notANumber:MONDAY"));
        }
    }

    @Nested
    class ExpandRecurrenceTests {

        @Test
        void expandRecurrence_dailyOccursEachDay() {
            // Arrange
            Set<DayOfWeek> allDays = EnumSet.allOf(DayOfWeek.class);
            var parsed = new ParsedRecurrenceInput(RecurrenceFrequency.DAILY, allDays, null);

            LocalDate start = LocalDate.of(2025, 6, 29);
            LocalDate end = start.plusDays(4);

            // Act
            var occurrences = recurrenceRuleService.expandRecurrence(parsed, start, end, Set.of());

            // Assert
            assertEquals(5, occurrences.size());
            assertEquals(start, occurrences.get(0));
            assertEquals(end, occurrences.get(4));
        }

        @Test
        void expandRecurrence_weeklyOccursOnlyOnSelectedDays() {
            // Arrange
            Set<DayOfWeek> selectedDays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

            var parsed = new ParsedRecurrenceInput(RecurrenceFrequency.WEEKLY, selectedDays, null);
            var start = LocalDate.of(2025, 6, 2); // Monday, June 2, 2025
            var end = start.plusDays(6);

            // Act
            var occurrences = recurrenceRuleService.expandRecurrence(parsed, start, end, Set.of());

            // Assert
            assertTrue(occurrences.contains(start)); // Monday
            assertTrue(occurrences.contains(start.plusDays(4))); // Friday
            assertEquals(2, occurrences.size());
        }

        @Test
        void expandRecurrence_monthlyOccursOnOrdinalWeekday() {
            // Arrange
            Set<DayOfWeek> days = EnumSet.of(DayOfWeek.TUESDAY);
            int ordinal = 2;

            var parsed = new ParsedRecurrenceInput(RecurrenceFrequency.MONTHLY, days, ordinal);

            // Pick a month where the 2nd Tuesday is known: June 2025
            LocalDate start = LocalDate.of(2025, 6, 1);
            LocalDate end = LocalDate.of(2025, 6, 30);

            // Act
            var occurrences = recurrenceRuleService.expandRecurrence(parsed, start, end, Set.of());

            // Assert
            assertEquals(1, occurrences.size());
            assertEquals(LocalDate.of(2025, 6, 10), occurrences.get(0)); // 2nd Tuesday June 2025
        }

        @Test
        void expandRecurrence_nullParsed_returnsEmptyList() {
            // Act & Assert

            // parsed is null
            assertTrue(recurrenceRuleService.expandRecurrence(null, startDate, endDate, Set.of()).isEmpty());
        }

        @Test
        void expandRecurrence_dailySkipsSpecifiedDays() {
            // Arrange
            Set<DayOfWeek> allDays = EnumSet.allOf(DayOfWeek.class);
            var parsed = new ParsedRecurrenceInput(RecurrenceFrequency.DAILY, allDays, null);

            LocalDate start = LocalDate.of(2025, 6, 1);
            LocalDate end = start.plusDays(4);

            // Skip June 3rd (third day in the range)
            Set<LocalDate> skipDays = Set.of(start.plusDays(2));

            // Act
            var occurrences = recurrenceRuleService.expandRecurrence(parsed, start, end, skipDays);

            // Assert
            assertEquals(4, occurrences.size());
            assertFalse(occurrences.contains(start.plusDays(2)));
            assertEquals(start, occurrences.get(0));
            assertEquals(end, occurrences.get(3));
        }

        @Test
        void expandRecurrence_weeklySkipsSpecifiedDays() {
            // Arrange
            Set<DayOfWeek> selectedDays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
            var parsed = new ParsedRecurrenceInput(RecurrenceFrequency.WEEKLY, selectedDays, null);

            LocalDate start = LocalDate.of(2025, 6, 2); // Monday
            LocalDate end = start.plusDays(6); // Covers Mon-Sun

            // Skip Wednesday
            Set<LocalDate> skipDays = Set.of(start.plusDays(2));

            // Act
            var occurrences = recurrenceRuleService.expandRecurrence(parsed, start, end, skipDays);

            // Assert
            assertTrue(occurrences.contains(start)); // Monday
            assertFalse(occurrences.contains(start.plusDays(2))); // Skipped Wednesday
            assertEquals(1, occurrences.size());
        }

        @Test
        void expandRecurrence_monthlySkipsSpecifiedDays() {
            // Arrange
            Set<DayOfWeek> days = EnumSet.of(DayOfWeek.TUESDAY);
            int ordinal = 2;
            var parsed = new ParsedRecurrenceInput(RecurrenceFrequency.MONTHLY, days, ordinal);

            // June 2025: 2nd Tuesday is June 10
            LocalDate start = LocalDate.of(2025, 6, 1);
            LocalDate end = LocalDate.of(2025, 6, 30);

            // Skip June 10th
            Set<LocalDate> skipDays = Set.of(LocalDate.of(2025, 6, 10));

            // Act
            var occurrences = recurrenceRuleService.expandRecurrence(parsed, start, end, skipDays);

            // Assert
            assertTrue(occurrences.isEmpty()); // Skipped day excluded
        }

    }

    @Nested
    class BuildSummaryTests {

        @Test
        void buildSummary_daily() {
            // Arrange
            Set<DayOfWeek> allDays = EnumSet.allOf(DayOfWeek.class);
            ParsedRecurrenceInput parsed = new ParsedRecurrenceInput(RecurrenceFrequency.DAILY, allDays, null);

            // Act
            String summary = recurrenceRuleService.buildSummary(parsed, startDate, endDate);

            // Assert
            assertEquals("Every day from June 1, 2025 until September 1, 2025", summary);
        }

        @Test
        void buildSummary_weekly_multipleDays() {
            // Arrange
            Set<DayOfWeek> days = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
            ParsedRecurrenceInput parsed = new ParsedRecurrenceInput(RecurrenceFrequency.WEEKLY, days, null);

            // Act
            String summary = recurrenceRuleService.buildSummary(parsed, startDate, endDate);

            // Assert
            assertEquals("Every Monday and Friday from June 1, 2025 until September 1, 2025", summary);
        }

        @Test
        void buildSummary_monthly_withOrdinal() {
            // Arrange
            Set<DayOfWeek> days = EnumSet.of(DayOfWeek.TUESDAY);
            int ordinal = 2;
            ParsedRecurrenceInput parsed = new ParsedRecurrenceInput(RecurrenceFrequency.MONTHLY, days, ordinal);

            // Act
            String summary = recurrenceRuleService.buildSummary(parsed, startDate, endDate);

            // Assert
            assertEquals("Every second Tuesday of the month from June 1, 2025 until September 1, 2025", summary);
        }

        @Test
        void buildSummary_weekly_singleDay() {
            // Arrange
            Set<DayOfWeek> days = EnumSet.of(DayOfWeek.WEDNESDAY);
            ParsedRecurrenceInput parsed = new ParsedRecurrenceInput(RecurrenceFrequency.WEEKLY, days, null);

            // Act
            String summary = recurrenceRuleService.buildSummary(parsed, startDate, endDate);

            // Assert
            assertEquals("Every Wednesday from June 1, 2025 until September 1, 2025", summary);
        }

        @Test
        void buildSummary_monthly_multipleDays() {
            // Arrange
            Set<DayOfWeek> days = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY);
            ParsedRecurrenceInput parsed = new ParsedRecurrenceInput(RecurrenceFrequency.MONTHLY, days, 3);

            // Act
            String summary = recurrenceRuleService.buildSummary(parsed, startDate, endDate);

            // Assert
            assertEquals("Every third Monday and Thursday of the month from June 1, 2025 until September 1, 2025", summary);
        }

        @Test
        void buildSummary_weekly_noDays_selectedProducesEmptyList() {
            // Arrange
            Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class); // all false (empty set)
            ParsedRecurrenceInput parsed = new ParsedRecurrenceInput(RecurrenceFrequency.WEEKLY, days, null);

            // Act
            String summary = recurrenceRuleService.buildSummary(parsed, startDate, endDate);

            // Assert
            assertEquals("Every  from June 1, 2025 until September 1, 2025", summary); // Depending on your formatting, might be empty
        }

    }

    @Nested
    class OccursOnTests {

        @Test
        void occursOn_dailyAlwaysReturnsTrue() {
            // Arrange
            ParsedRecurrenceInput parsed = new ParsedRecurrenceInput(
                    RecurrenceFrequency.DAILY,
                    EnumSet.allOf(DayOfWeek.class),
                    null
            );
            LocalDate anyDate = LocalDate.of(2025, 6, 15);

            // Act
            boolean result = recurrenceRuleService.occursOn(parsed, anyDate);

            // Assert
            assertTrue(result);
        }

        @Test
        void occursOn_weeklyReturnsTrueOnlyOnSelectedDays() {
            // Arrange
            ParsedRecurrenceInput parsed = new ParsedRecurrenceInput(
                    RecurrenceFrequency.WEEKLY,
                    EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                    null
            );

            LocalDate monday = LocalDate.of(2025, 6, 2); // Monday
            LocalDate tuesday = monday.plusDays(1);      // Tuesday

            // Act & Assert
            assertTrue(recurrenceRuleService.occursOn(parsed, monday));
            assertFalse(recurrenceRuleService.occursOn(parsed, tuesday));
        }

        @Test
        void occursOn_monthlyReturnsTrueOnlyForCorrectOrdinal() {
            // Arrange
            ParsedRecurrenceInput parsed = new ParsedRecurrenceInput(
                    RecurrenceFrequency.MONTHLY,
                    EnumSet.of(DayOfWeek.TUESDAY),
                    2 // second Tuesday
            );

            LocalDate secondTuesday = LocalDate.of(2025, 6, 10); // known 2nd Tuesday June 2025
            LocalDate firstTuesday = LocalDate.of(2025, 6, 3);

            // Act & Assert
            assertTrue(recurrenceRuleService.occursOn(parsed, secondTuesday));
            assertFalse(recurrenceRuleService.occursOn(parsed, firstTuesday));
        }

        @Test
        void occursOn_monthlyReturnsFalseIfDayNotIncluded() {
            // Arrange
            ParsedRecurrenceInput parsed = new ParsedRecurrenceInput(
                    RecurrenceFrequency.MONTHLY,
                    EnumSet.of(DayOfWeek.MONDAY),
                    1
            );

            LocalDate firstTuesday = LocalDate.of(2025, 6, 3); // Tuesday

            // Act
            boolean result = recurrenceRuleService.occursOn(parsed, firstTuesday);

            // Assert
            assertFalse(result);
        }

    }
}
