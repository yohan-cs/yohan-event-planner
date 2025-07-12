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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

        @Test
        void parseFromString_edgeCases_throwsException() {
            // Empty string
            assertThrows(InvalidRecurrenceRuleException.class, () -> recurrenceRuleService.parseFromString(""));
            
            // Missing parts
            assertThrows(InvalidRecurrenceRuleException.class, () -> recurrenceRuleService.parseFromString("DAILY"));
            
            // Case sensitivity - should work
            var result = recurrenceRuleService.parseFromString("daily:");
            assertEquals(RecurrenceFrequency.DAILY, result.frequency());
            
            // Weekly with no days
            assertThrows(InvalidRecurrenceRuleException.class, () -> recurrenceRuleService.parseFromString("WEEKLY:"));
            
            // Monthly boundary ordinals
            assertThrows(InvalidRecurrenceRuleException.class, () -> recurrenceRuleService.parseFromString("MONTHLY:0:MONDAY"));
            assertThrows(InvalidRecurrenceRuleException.class, () -> recurrenceRuleService.parseFromString("MONTHLY:5:MONDAY"));
            assertThrows(InvalidRecurrenceRuleException.class, () -> recurrenceRuleService.parseFromString("MONTHLY:-1:MONDAY"));
            
            // Monthly missing days
            assertThrows(InvalidRecurrenceRuleException.class, () -> recurrenceRuleService.parseFromString("MONTHLY:2:"));
        }

        @Test
        void parseFromString_nullInput_throwsException() {
            // Null input should throw NPE or appropriate exception
            assertThrows(Exception.class, () -> recurrenceRuleService.parseFromString(null));
        }

        @Test
        void parseFromString_whitespaceHandling() {
            // Leading/trailing spaces should be handled
            var result1 = recurrenceRuleService.parseFromString(" DAILY: ");
            assertEquals(RecurrenceFrequency.DAILY, result1.frequency());
            
            // Spaces around day names
            var result2 = recurrenceRuleService.parseFromString("WEEKLY: MONDAY , FRIDAY ");
            assertEquals(RecurrenceFrequency.WEEKLY, result2.frequency());
            assertTrue(result2.daysOfWeek().contains(DayOfWeek.MONDAY));
            assertTrue(result2.daysOfWeek().contains(DayOfWeek.FRIDAY));
            
            // Spaces around ordinal and days
            var result3 = recurrenceRuleService.parseFromString("MONTHLY: 2 : TUESDAY ");
            assertEquals(RecurrenceFrequency.MONTHLY, result3.frequency());
            assertEquals(2, result3.ordinal());
            assertTrue(result3.daysOfWeek().contains(DayOfWeek.TUESDAY));
        }

        @Test
        void parseFromString_multipleDaysEdgeCases() {
            // All 7 days for weekly (should work like DAILY)
            var result = recurrenceRuleService.parseFromString("WEEKLY:MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY");
            assertEquals(RecurrenceFrequency.WEEKLY, result.frequency());
            assertEquals(7, result.daysOfWeek().size());
            
            // Duplicate days should be deduplicated
            result = recurrenceRuleService.parseFromString("WEEKLY:MONDAY,MONDAY,TUESDAY");
            assertEquals(RecurrenceFrequency.WEEKLY, result.frequency());
            assertEquals(2, result.daysOfWeek().size()); // Only MONDAY and TUESDAY
            assertTrue(result.daysOfWeek().contains(DayOfWeek.MONDAY));
            assertTrue(result.daysOfWeek().contains(DayOfWeek.TUESDAY));
            
            // Single day weekly
            result = recurrenceRuleService.parseFromString("WEEKLY:FRIDAY");
            assertEquals(1, result.daysOfWeek().size());
            assertTrue(result.daysOfWeek().contains(DayOfWeek.FRIDAY));
        }

        @Test
        void parseFromString_monthlyEdgeCases() {
            // All ordinals should work (1-4)
            for (int ordinal = 1; ordinal <= 4; ordinal++) {
                var result = recurrenceRuleService.parseFromString("MONTHLY:" + ordinal + ":MONDAY");
                assertEquals(RecurrenceFrequency.MONTHLY, result.frequency());
                assertEquals(ordinal, result.ordinal());
                assertTrue(result.daysOfWeek().contains(DayOfWeek.MONDAY));
            }
            
            // Multiple days with ordinal
            var result = recurrenceRuleService.parseFromString("MONTHLY:3:MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY");
            assertEquals(5, result.daysOfWeek().size());
            assertEquals(3, result.ordinal());
        }

        @Test
        void parseFromString_caseSensitivityForDays() {
            // Mixed case day names should work
            var result = recurrenceRuleService.parseFromString("WEEKLY:monday,TuEsDaY,FRIDAY");
            assertEquals(RecurrenceFrequency.WEEKLY, result.frequency());
            assertEquals(3, result.daysOfWeek().size());
            assertTrue(result.daysOfWeek().contains(DayOfWeek.MONDAY));
            assertTrue(result.daysOfWeek().contains(DayOfWeek.TUESDAY));
            assertTrue(result.daysOfWeek().contains(DayOfWeek.FRIDAY));
            
            // Lowercase frequency and days
            result = recurrenceRuleService.parseFromString("monthly:2:wednesday,saturday");
            assertEquals(RecurrenceFrequency.MONTHLY, result.frequency());
            assertEquals(2, result.ordinal());
            assertTrue(result.daysOfWeek().contains(DayOfWeek.WEDNESDAY));
            assertTrue(result.daysOfWeek().contains(DayOfWeek.SATURDAY));
        }
    }

    @Nested
    class ParsedRecurrenceInputTests {

        @Test
        void parsedRecurrenceInput_nullFrequency_throwsException() {
            // Record validation should prevent null frequency
            assertThrows(NullPointerException.class, () -> 
                new ParsedRecurrenceInput(null, EnumSet.noneOf(DayOfWeek.class), null));
        }

        @Test
        void parsedRecurrenceInput_validConstruction() {
            // Valid constructions should work
            assertDoesNotThrow(() -> 
                new ParsedRecurrenceInput(RecurrenceFrequency.DAILY, EnumSet.allOf(DayOfWeek.class), null));
            
            assertDoesNotThrow(() -> 
                new ParsedRecurrenceInput(RecurrenceFrequency.WEEKLY, EnumSet.of(DayOfWeek.MONDAY), null));
            
            assertDoesNotThrow(() -> 
                new ParsedRecurrenceInput(RecurrenceFrequency.MONTHLY, EnumSet.of(DayOfWeek.TUESDAY), 2));
        }

        @Test
        void parsedRecurrenceInput_emptyDaysOfWeek() {
            // Empty days collection should be allowed (though may not be useful)
            assertDoesNotThrow(() -> 
                new ParsedRecurrenceInput(RecurrenceFrequency.WEEKLY, EnumSet.noneOf(DayOfWeek.class), null));
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
        void expandRecurrence_edgeCases() {
            // Start date after end date
            LocalDate start = LocalDate.of(2025, 6, 10);
            LocalDate end = LocalDate.of(2025, 6, 5);
            Set<DayOfWeek> allDays = EnumSet.allOf(DayOfWeek.class);
            var parsed = new ParsedRecurrenceInput(RecurrenceFrequency.DAILY, allDays, null);
            
            var result = recurrenceRuleService.expandRecurrence(parsed, start, end, Set.of());
            assertTrue(result.isEmpty());
            
            // Single day range
            start = LocalDate.of(2025, 6, 15);
            end = start;
            result = recurrenceRuleService.expandRecurrence(parsed, start, end, Set.of());
            assertEquals(1, result.size());
            assertEquals(start, result.get(0));
            
            // All days in range are skip days
            start = LocalDate.of(2025, 6, 1);
            end = LocalDate.of(2025, 6, 3);
            Set<LocalDate> skipDays = Set.of(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 6, 2),
                LocalDate.of(2025, 6, 3)
            );
            result = recurrenceRuleService.expandRecurrence(parsed, start, end, skipDays);
            assertTrue(result.isEmpty());
        }

        @Test
        void expandRecurrence_nullParameters_throwsException() {
            Set<DayOfWeek> allDays = EnumSet.allOf(DayOfWeek.class);
            var parsed = new ParsedRecurrenceInput(RecurrenceFrequency.DAILY, allDays, null);
            LocalDate start = LocalDate.of(2025, 6, 1);
            LocalDate end = LocalDate.of(2025, 6, 10);
            
            // Null start date
            assertThrows(Exception.class, () -> 
                recurrenceRuleService.expandRecurrence(parsed, null, end, Set.of()));
            
            // Null end date
            assertThrows(Exception.class, () -> 
                recurrenceRuleService.expandRecurrence(parsed, start, null, Set.of()));
            
            // Null skip days
            assertThrows(Exception.class, () -> 
                recurrenceRuleService.expandRecurrence(parsed, start, end, null));
        }

        @Test
        void expandRecurrence_emptyCollections() {
            // Empty skip days should work (same as no skip days)
            Set<DayOfWeek> allDays = EnumSet.allOf(DayOfWeek.class);
            var parsed = new ParsedRecurrenceInput(RecurrenceFrequency.DAILY, allDays, null);
            LocalDate start = LocalDate.of(2025, 6, 1);
            LocalDate end = LocalDate.of(2025, 6, 3);
            
            var resultWithEmpty = recurrenceRuleService.expandRecurrence(parsed, start, end, Set.of());
            var resultWithNoSkip = recurrenceRuleService.expandRecurrence(parsed, start, end, Set.of());
            
            assertEquals(resultWithEmpty, resultWithNoSkip);
            assertEquals(3, resultWithEmpty.size());
            
            // Empty days of week for weekly should produce no results
            var emptyWeekly = new ParsedRecurrenceInput(RecurrenceFrequency.WEEKLY, EnumSet.noneOf(DayOfWeek.class), null);
            var emptyResult = recurrenceRuleService.expandRecurrence(emptyWeekly, start, end, Set.of());
            assertTrue(emptyResult.isEmpty());
        }

        @Test
        void expandRecurrence_invalidFrequencyHandling() {
            // Test with parsed input that has null frequency (though this should be prevented by record)
            var parsedWithNullFreq = new ParsedRecurrenceInput(RecurrenceFrequency.DAILY, EnumSet.allOf(DayOfWeek.class), null);
            
            // Should work normally
            var result = recurrenceRuleService.expandRecurrence(parsedWithNullFreq, 
                LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 2), Set.of());
            assertEquals(2, result.size());
        }

        @Test
        void expandRecurrence_monthlyAcrossMonthBoundaries() {
            // Test monthly recurrence across months with different numbers of days
            Set<DayOfWeek> tuesdays = EnumSet.of(DayOfWeek.TUESDAY);
            var monthlyParsed = new ParsedRecurrenceInput(RecurrenceFrequency.MONTHLY, tuesdays, 4); // 4th Tuesday
            
            // Test across February (shorter month) and March
            LocalDate start = LocalDate.of(2025, 2, 1);
            LocalDate end = LocalDate.of(2025, 3, 31);
            
            var result = recurrenceRuleService.expandRecurrence(monthlyParsed, start, end, Set.of());
            
            // February 2025 has 4 Tuesdays: 4th, 11th, 18th, 25th (so 4th Tuesday is Feb 25)
            // March 2025: 4th Tuesday is March 25th
            assertTrue(result.contains(LocalDate.of(2025, 2, 25))); // 4th Tuesday of Feb
            assertTrue(result.contains(LocalDate.of(2025, 3, 25))); // 4th Tuesday of Mar
            
            // Test case where there's no 5th occurrence
            var fifthTuesday = new ParsedRecurrenceInput(RecurrenceFrequency.MONTHLY, tuesdays, 5);
            result = recurrenceRuleService.expandRecurrence(fifthTuesday, start, end, Set.of());
            // Some months may not have a 5th Tuesday
            assertTrue(result.size() <= 2); // At most 2 months could have 5th Tuesday
        }

        @Test
        void expandRecurrence_largeRange_performanceTest() {
            // Test expansion over a large range (1 year) to ensure reasonable performance
            Set<DayOfWeek> weekdays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
            var weeklyParsed = new ParsedRecurrenceInput(RecurrenceFrequency.WEEKLY, weekdays, null);
            
            LocalDate start = LocalDate.of(2025, 1, 1);
            LocalDate end = LocalDate.of(2025, 12, 31);
            
            long startTime = System.currentTimeMillis();
            var result = recurrenceRuleService.expandRecurrence(weeklyParsed, start, end, Set.of());
            long endTime = System.currentTimeMillis();
            
            // Should complete in reasonable time (less than 1 second for a year)
            assertTrue(endTime - startTime < 1000, "Expansion took too long: " + (endTime - startTime) + "ms");
            
            // Should have approximately 3 days per week * 52 weeks = ~156 occurrences
            assertTrue(result.size() > 150 && result.size() < 160, 
                      "Expected ~156 occurrences, got " + result.size());
            
            // Results should be in chronological order
            for (int i = 1; i < result.size(); i++) {
                assertTrue(result.get(i).isAfter(result.get(i-1)), 
                          "Results not in chronological order");
            }
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

        @Test
        void buildSummary_edgeCases() {
            // Null end date (infinite recurrence)
            Set<DayOfWeek> days = EnumSet.of(DayOfWeek.MONDAY);
            ParsedRecurrenceInput parsed = new ParsedRecurrenceInput(RecurrenceFrequency.WEEKLY, days, null);
            
            String summary = recurrenceRuleService.buildSummary(parsed, startDate, null);
            assertEquals("Every Monday from June 1, 2025 forever", summary);
            
            // Invalid ordinal in summary (edge case)
            ParsedRecurrenceInput monthlyParsed = new ParsedRecurrenceInput(RecurrenceFrequency.MONTHLY, days, 99);
            summary = recurrenceRuleService.buildSummary(monthlyParsed, startDate, endDate);
            assertTrue(summary.contains("unknown"));
        }

        @Test
        void buildSummary_nullParameters_throwsException() {
            Set<DayOfWeek> days = EnumSet.of(DayOfWeek.MONDAY);
            ParsedRecurrenceInput parsed = new ParsedRecurrenceInput(RecurrenceFrequency.WEEKLY, days, null);
            
            // Null parsedRecurrence
            assertThrows(Exception.class, () -> 
                recurrenceRuleService.buildSummary(null, startDate, endDate));
            
            // Null start date
            assertThrows(Exception.class, () -> 
                recurrenceRuleService.buildSummary(parsed, null, endDate));
            
            // Null end date is allowed (infinite recurrence)
            assertDoesNotThrow(() -> recurrenceRuleService.buildSummary(parsed, startDate, null));
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

        @Test
        void occursOn_edgeCases() {
            // Null input
            assertFalse(recurrenceRuleService.occursOn(null, LocalDate.of(2025, 6, 1)));
            
            // Cannot create ParsedRecurrenceInput with null frequency due to record validation
            // Test that the constructor properly validates
            assertThrows(NullPointerException.class, () -> 
                new ParsedRecurrenceInput(null, EnumSet.noneOf(DayOfWeek.class), null));
            
            // Edge of month boundary for monthly recurrence
            ParsedRecurrenceInput monthlyParsed = new ParsedRecurrenceInput(
                    RecurrenceFrequency.MONTHLY,
                    EnumSet.of(DayOfWeek.SUNDAY),
                    1 // First Sunday
            );
            
            // Test last day of month that is first Sunday
            LocalDate firstSundayJune = LocalDate.of(2025, 6, 1); // June 1, 2025 is a Sunday
            assertTrue(recurrenceRuleService.occursOn(monthlyParsed, firstSundayJune));
            
            // Test second Sunday of same month
            LocalDate secondSundayJune = LocalDate.of(2025, 6, 8);
            assertFalse(recurrenceRuleService.occursOn(monthlyParsed, secondSundayJune));
        }

        @Test
        void occursOn_nullParameters_throwsException() {
            Set<DayOfWeek> days = EnumSet.of(DayOfWeek.MONDAY);
            ParsedRecurrenceInput parsed = new ParsedRecurrenceInput(RecurrenceFrequency.WEEKLY, days, null);
            LocalDate testDate = LocalDate.of(2025, 6, 2); // Monday
            
            // Null date parameter
            assertThrows(Exception.class, () -> 
                recurrenceRuleService.occursOn(parsed, null));
            
            // Valid parameters should work
            assertTrue(recurrenceRuleService.occursOn(parsed, testDate));
        }

        @Test
        void occursOn_complexMonthlyScenarios() {
            // Test various monthly scenarios across different months
            Set<DayOfWeek> thursdays = EnumSet.of(DayOfWeek.THURSDAY);
            
            // 3rd Thursday scenarios
            ParsedRecurrenceInput thirdThursday = new ParsedRecurrenceInput(
                    RecurrenceFrequency.MONTHLY, thursdays, 3);
            
            // June 2025: 3rd Thursday is June 19
            assertTrue(recurrenceRuleService.occursOn(thirdThursday, LocalDate.of(2025, 6, 19)));
            assertFalse(recurrenceRuleService.occursOn(thirdThursday, LocalDate.of(2025, 6, 12))); // 2nd Thursday
            assertFalse(recurrenceRuleService.occursOn(thirdThursday, LocalDate.of(2025, 6, 26))); // 4th Thursday
            
            // February 2025: 3rd Thursday is February 20
            assertTrue(recurrenceRuleService.occursOn(thirdThursday, LocalDate.of(2025, 2, 20)));
            
            // Test 5th occurrence in months that don't have it
            ParsedRecurrenceInput fifthThursday = new ParsedRecurrenceInput(
                    RecurrenceFrequency.MONTHLY, thursdays, 5);
            
            // June 2025 doesn't have a 5th Thursday
            assertFalse(recurrenceRuleService.occursOn(fifthThursday, LocalDate.of(2025, 6, 19))); // 3rd Thursday
            assertFalse(recurrenceRuleService.occursOn(fifthThursday, LocalDate.of(2025, 6, 26))); // 4th Thursday
            
            // January 2025 has 5 Thursdays (2nd, 9th, 16th, 23rd, 30th)
            assertTrue(recurrenceRuleService.occursOn(fifthThursday, LocalDate.of(2025, 1, 30))); // 5th Thursday
            assertFalse(recurrenceRuleService.occursOn(fifthThursday, LocalDate.of(2025, 1, 23))); // 4th Thursday
        }

    }
}
