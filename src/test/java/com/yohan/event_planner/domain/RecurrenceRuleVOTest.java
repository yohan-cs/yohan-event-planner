package com.yohan.event_planner.domain;

import com.yohan.event_planner.service.ParsedRecurrenceInput;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecurrenceRuleVOTest {

    private static final String DAILY_SUMMARY = "FREQ=DAILY;INTERVAL=1";
    private static final String WEEKLY_SUMMARY = "FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE,FR";
    private static final String MONTHLY_SUMMARY = "FREQ=MONTHLY;INTERVAL=2";
    
    private ParsedRecurrenceInput dailyParsed;
    private ParsedRecurrenceInput weeklyParsed;

    @BeforeEach
    void setUp() {
        dailyParsed = TestUtils.createParsedDailyRecurrenceInput();
        weeklyParsed = TestUtils.createParsedWeeklyRecurrenceInput(
            java.util.Set.of(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.FRIDAY)
        );
    }

    @Nested
    class Construction {

        @Test
        void constructor_shouldSetBothFields() {
            RecurrenceRuleVO rule = new RecurrenceRuleVO(DAILY_SUMMARY, dailyParsed);

            assertThat(rule.getSummary()).isEqualTo(DAILY_SUMMARY);
            assertThat(rule.getParsed()).isEqualTo(dailyParsed);
        }

        @Test
        void constructor_withNullSummary_shouldAllowNull() {
            RecurrenceRuleVO rule = new RecurrenceRuleVO(null, dailyParsed);

            assertThat(rule.getSummary()).isNull();
            assertThat(rule.getParsed()).isEqualTo(dailyParsed);
        }

        @Test
        void constructor_withNullParsed_shouldAllowNull() {
            RecurrenceRuleVO rule = new RecurrenceRuleVO(DAILY_SUMMARY, null);

            assertThat(rule.getSummary()).isEqualTo(DAILY_SUMMARY);
            assertThat(rule.getParsed()).isNull();
        }

        @Test
        void constructor_withBothNull_shouldAllowBothNull() {
            RecurrenceRuleVO rule = new RecurrenceRuleVO(null, null);

            assertThat(rule.getSummary()).isNull();
            assertThat(rule.getParsed()).isNull();
        }

        @Test
        void defaultConstructor_shouldCreateEmptyRule() {
            RecurrenceRuleVO rule = new RecurrenceRuleVO();

            assertThat(rule.getSummary()).isNull();
            assertThat(rule.getParsed()).isNull();
        }
    }

    @Nested
    class ValueObjectEquality {

        @Test
        void equals_withSameSummary_shouldReturnTrue() {
            RecurrenceRuleVO rule1 = new RecurrenceRuleVO(DAILY_SUMMARY, dailyParsed);
            RecurrenceRuleVO rule2 = new RecurrenceRuleVO(DAILY_SUMMARY, weeklyParsed); // Different parsed

            assertThat(rule1).isEqualTo(rule2);
        }

        @Test
        void equals_withDifferentSummary_shouldReturnFalse() {
            RecurrenceRuleVO rule1 = new RecurrenceRuleVO(DAILY_SUMMARY, dailyParsed);
            RecurrenceRuleVO rule2 = new RecurrenceRuleVO(WEEKLY_SUMMARY, dailyParsed); // Same parsed

            assertThat(rule1).isNotEqualTo(rule2);
        }

        @Test
        void equals_withBothNullSummaries_shouldReturnTrue() {
            RecurrenceRuleVO rule1 = new RecurrenceRuleVO(null, dailyParsed);
            RecurrenceRuleVO rule2 = new RecurrenceRuleVO(null, weeklyParsed);

            assertThat(rule1).isEqualTo(rule2);
        }

        @Test
        void equals_withOneNullSummary_shouldReturnFalse() {
            RecurrenceRuleVO rule1 = new RecurrenceRuleVO(DAILY_SUMMARY, dailyParsed);
            RecurrenceRuleVO rule2 = new RecurrenceRuleVO(null, dailyParsed);

            assertThat(rule1).isNotEqualTo(rule2);
        }

        @Test
        void equals_withSelf_shouldReturnTrue() {
            RecurrenceRuleVO rule = new RecurrenceRuleVO(DAILY_SUMMARY, dailyParsed);

            assertThat(rule).isEqualTo(rule);
        }

        @Test
        void equals_withNull_shouldReturnFalse() {
            RecurrenceRuleVO rule = new RecurrenceRuleVO(DAILY_SUMMARY, dailyParsed);

            assertThat(rule).isNotEqualTo(null);
        }

        @Test
        void equals_withDifferentClass_shouldReturnFalse() {
            RecurrenceRuleVO rule = new RecurrenceRuleVO(DAILY_SUMMARY, dailyParsed);

            assertThat(rule).isNotEqualTo("not a recurrence rule");
        }

        @Test
        void equals_shouldIgnoreParsedField() {
            RecurrenceRuleVO rule1 = new RecurrenceRuleVO(DAILY_SUMMARY, dailyParsed);
            RecurrenceRuleVO rule2 = new RecurrenceRuleVO(DAILY_SUMMARY, null);

            // Should be equal because summary is the same, regardless of parsed field
            assertThat(rule1).isEqualTo(rule2);
        }
    }

    @Nested
    class Hashing {

        @Test
        void hashCode_shouldBeBasedOnSummaryOnly() {
            RecurrenceRuleVO rule1 = new RecurrenceRuleVO(DAILY_SUMMARY, dailyParsed);
            RecurrenceRuleVO rule2 = new RecurrenceRuleVO(DAILY_SUMMARY, weeklyParsed);

            assertThat(rule1.hashCode()).isEqualTo(rule2.hashCode());
        }

        @Test
        void hashCode_withDifferentSummaries_shouldReturnDifferentHashes() {
            RecurrenceRuleVO rule1 = new RecurrenceRuleVO(DAILY_SUMMARY, dailyParsed);
            RecurrenceRuleVO rule2 = new RecurrenceRuleVO(WEEKLY_SUMMARY, dailyParsed);

            assertThat(rule1.hashCode()).isNotEqualTo(rule2.hashCode());
        }

        @Test
        void hashCode_withNullSummary_shouldNotThrow() {
            RecurrenceRuleVO rule = new RecurrenceRuleVO(null, dailyParsed);

            assertThat(rule.hashCode()).isNotNull();
        }

        @Test
        void hashCode_shouldBeConsistentWithEquals() {
            RecurrenceRuleVO rule1 = new RecurrenceRuleVO(DAILY_SUMMARY, dailyParsed);
            RecurrenceRuleVO rule2 = new RecurrenceRuleVO(DAILY_SUMMARY, null);

            // If equal, hash codes must be equal
            assertThat(rule1).isEqualTo(rule2);
            assertThat(rule1.hashCode()).isEqualTo(rule2.hashCode());
        }

        @Test
        void hashCode_shouldIgnoreParsedField() {
            RecurrenceRuleVO rule1 = new RecurrenceRuleVO(DAILY_SUMMARY, dailyParsed);
            RecurrenceRuleVO rule2 = new RecurrenceRuleVO(DAILY_SUMMARY, null);

            assertThat(rule1.hashCode()).isEqualTo(rule2.hashCode());
        }
    }

    @Nested
    class PersistenceSemantics {

        @Test
        void transientField_shouldNotAffectEquality() {
            // Simulate post-persistence scenario where parsed field might be null
            RecurrenceRuleVO persistedRule = new RecurrenceRuleVO(DAILY_SUMMARY, null);
            RecurrenceRuleVO inMemoryRule = new RecurrenceRuleVO(DAILY_SUMMARY, dailyParsed);

            assertThat(persistedRule).isEqualTo(inMemoryRule);
            assertThat(persistedRule.hashCode()).isEqualTo(inMemoryRule.hashCode());
        }

        @Test
        void valueObjectSemantics_shouldWork() {
            RecurrenceRuleVO rule1 = new RecurrenceRuleVO(DAILY_SUMMARY, dailyParsed);
            RecurrenceRuleVO rule2 = new RecurrenceRuleVO(DAILY_SUMMARY, dailyParsed);

            // Should be equal as value objects (not reference equality)
            assertThat(rule1).isEqualTo(rule2);
            assertThat(rule1).isNotSameAs(rule2);
        }

        @Test
        void summaryField_shouldBePersistentBasis() {
            RecurrenceRuleVO rule = new RecurrenceRuleVO(DAILY_SUMMARY, dailyParsed);

            // Summary is the authoritative source for persistence
            assertThat(rule.getSummary()).isEqualTo(DAILY_SUMMARY);
            
            // Parsed is transient and may be reconstructed
            assertThat(rule.getParsed()).isEqualTo(dailyParsed);
        }
    }

    @Nested
    class CommonRecurrencePatterns {

        @Test
        void dailyRecurrence_shouldHaveCorrectFields() {
            RecurrenceRuleVO dailyRule = TestUtils.createValidDailyRecurrenceRuleVO(
                java.time.LocalDate.of(2024, 1, 1),
                java.time.LocalDate.of(2024, 12, 31)
            );

            assertThat(dailyRule.getSummary()).isNotNull();
            assertThat(dailyRule.getParsed()).isNotNull();
            assertThat(dailyRule.getParsed().frequency()).isEqualTo(
                com.yohan.event_planner.domain.enums.RecurrenceFrequency.DAILY
            );
        }

        @Test
        void weeklyRecurrence_shouldHaveCorrectFields() {
            RecurrenceRuleVO weeklyRule = TestUtils.createValidWeeklyRecurrenceRuleVO(
                java.util.Set.of(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.FRIDAY),
                java.time.Clock.systemUTC()
            );

            assertThat(weeklyRule.getSummary()).isNotNull();
            assertThat(weeklyRule.getParsed()).isNotNull();
            assertThat(weeklyRule.getParsed().frequency()).isEqualTo(
                com.yohan.event_planner.domain.enums.RecurrenceFrequency.WEEKLY
            );
        }

        @Test
        void monthlyRecurrence_shouldHaveCorrectFields() {
            RecurrenceRuleVO monthlyRule = TestUtils.createValidMonthlyRecurrenceRuleVO(
                2, // Second occurrence
                java.util.Set.of(java.time.DayOfWeek.TUESDAY),
                java.time.Clock.systemUTC()
            );

            assertThat(monthlyRule.getSummary()).isNotNull();
            assertThat(monthlyRule.getParsed()).isNotNull();
            assertThat(monthlyRule.getParsed().frequency()).isEqualTo(
                com.yohan.event_planner.domain.enums.RecurrenceFrequency.MONTHLY
            );
        }

        @Test
        void draftRecurrence_shouldHaveUnspecifiedSummary() {
            RecurrenceRuleVO draftRule = TestUtils.createDraftRecurrenceRuleVO("UNSPECIFIED");

            assertThat(draftRule.getSummary()).isEqualTo("UNSPECIFIED");
            assertThat(draftRule.getParsed()).isNull();
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void emptySummary_shouldBeAllowed() {
            RecurrenceRuleVO rule = new RecurrenceRuleVO("", dailyParsed);

            assertThat(rule.getSummary()).isEmpty();
            assertThat(rule.getParsed()).isEqualTo(dailyParsed);
        }

        @Test
        void whitespaceOnlySummary_shouldBePreserved() {
            String whitespaceSummary = "   ";
            RecurrenceRuleVO rule = new RecurrenceRuleVO(whitespaceSummary, dailyParsed);

            assertThat(rule.getSummary()).isEqualTo(whitespaceSummary);
        }

        @Test
        void longSummary_shouldBeHandled() {
            String longSummary = "FREQ=DAILY;INTERVAL=1;BYMONTHDAY=1,15;BYMONTH=1,3,5,7,9,11;COUNT=100";
            RecurrenceRuleVO rule = new RecurrenceRuleVO(longSummary, dailyParsed);

            assertThat(rule.getSummary()).isEqualTo(longSummary);
        }

        @Test
        void specialCharactersInSummary_shouldBePreserved() {
            String specialSummary = "CUSTOM;@#$%^&*()_+-={}[]|\\:;\"'<>?,.";
            RecurrenceRuleVO rule = new RecurrenceRuleVO(specialSummary, null);

            assertThat(rule.getSummary()).isEqualTo(specialSummary);
        }

        @Test
        void equals_withEmptyAndNullSummaries_shouldReturnFalse() {
            RecurrenceRuleVO emptyRule = new RecurrenceRuleVO("", dailyParsed);
            RecurrenceRuleVO nullRule = new RecurrenceRuleVO(null, dailyParsed);

            assertThat(emptyRule).isNotEqualTo(nullRule);
        }
    }
}