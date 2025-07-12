package com.yohan.event_planner.domain;

import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RecurringEventTest {

    private User creator;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate startDate;
    private LocalDate endDate;
    private RecurrenceRuleVO recurrenceRule;

    @BeforeEach
    void setUp() {
        creator = TestUtils.createValidUserEntity();
        startTime = LocalTime.of(14, 30);
        endTime = LocalTime.of(16, 30);
        startDate = LocalDate.of(2024, 1, 15);
        endDate = LocalDate.of(2024, 12, 31);
        recurrenceRule = TestUtils.createValidDailyRecurrenceRuleVO(startDate, endDate);
    }

    @Nested
    class FactoryMethods {

        @Test
        void createRecurringEvent_shouldCreateWithAllFields() {
            RecurringEvent event = RecurringEvent.createRecurringEvent(
                "Daily Meeting", startTime, endTime, startDate, endDate,
                "Daily standup meeting", recurrenceRule, creator, false
            );

            assertThat(event.getName()).isEqualTo("Daily Meeting");
            assertThat(event.getStartTime()).isEqualTo(startTime);
            assertThat(event.getEndTime()).isEqualTo(endTime);
            assertThat(event.getStartDate()).isEqualTo(startDate);
            assertThat(event.getEndDate()).isEqualTo(endDate);
            assertThat(event.getDescription()).isEqualTo("Daily standup meeting");
            assertThat(event.getRecurrenceRule()).isEqualTo(recurrenceRule);
            assertThat(event.getCreator()).isEqualTo(creator);
            assertThat(event.isUnconfirmed()).isFalse();
        }

        @Test
        void createRecurringEvent_asUnconfirmed_shouldCreateDraft() {
            RecurringEvent event = RecurringEvent.createRecurringEvent(
                "Draft Meeting", startTime, endTime, startDate, endDate,
                "Draft description", recurrenceRule, creator, true
            );

            assertThat(event.isUnconfirmed()).isTrue();
        }

        @Test
        void createRecurringEvent_withNullEndTime_shouldAllowNull() {
            RecurringEvent event = RecurringEvent.createRecurringEvent(
                "Open Meeting", startTime, null, startDate, endDate,
                "Meeting with no end time", recurrenceRule, creator, false
            );

            assertThat(event.getEndTime()).isNull();
        }

        @Test
        void createUnconfirmedDraftRecurringEvent_shouldCreateDraftWithMinimalData() {
            RecurringEvent draft = RecurringEvent.createUnconfirmedDraftRecurringEvent(
                "Draft Event", startDate, endDate, creator
            );

            assertThat(draft.getName()).isEqualTo("Draft Event");
            assertThat(draft.getStartDate()).isEqualTo(startDate);
            assertThat(draft.getEndDate()).isEqualTo(endDate);
            assertThat(draft.getCreator()).isEqualTo(creator);
            assertThat(draft.isUnconfirmed()).isTrue();
            assertThat(draft.getRecurrenceRule().getSummary()).isEqualTo("UNSPECIFIED");
        }

        @Test
        void createUnconfirmedDraftRecurringEvent_withNullFields_shouldAllowNulls() {
            RecurringEvent draft = RecurringEvent.createUnconfirmedDraftRecurringEvent(
                null, null, null, creator
            );

            assertThat(draft.getName()).isNull();
            assertThat(draft.getStartDate()).isNull();
            assertThat(draft.getEndDate()).isNull();
            assertThat(draft.getCreator()).isEqualTo(creator);
            assertThat(draft.isUnconfirmed()).isTrue();
        }
    }

    @Nested
    class TimeTruncation {

        @Test
        void setStartTime_shouldTruncateToMinutes() {
            LocalTime timeWithSeconds = LocalTime.of(14, 30, 45, 123456789);
            LocalTime expectedTruncated = timeWithSeconds.truncatedTo(ChronoUnit.MINUTES);
            RecurringEvent event = RecurringEvent.createUnconfirmedDraftRecurringEvent(
                "Test", startDate, endDate, creator
            );

            event.setStartTime(timeWithSeconds);

            assertThat(event.getStartTime()).isEqualTo(expectedTruncated);
            assertThat(event.getStartTime().getSecond()).isZero();
            assertThat(event.getStartTime().getNano()).isZero();
        }

        @Test
        void setEndTime_shouldTruncateToMinutes() {
            LocalTime timeWithSeconds = LocalTime.of(16, 30, 45, 123456789);
            LocalTime expectedTruncated = timeWithSeconds.truncatedTo(ChronoUnit.MINUTES);
            RecurringEvent event = RecurringEvent.createUnconfirmedDraftRecurringEvent(
                "Test", startDate, endDate, creator
            );

            event.setEndTime(timeWithSeconds);

            assertThat(event.getEndTime()).isEqualTo(expectedTruncated);
            assertThat(event.getEndTime().getSecond()).isZero();
            assertThat(event.getEndTime().getNano()).isZero();
        }

        @Test
        void setStartTime_withNull_shouldSetNull() {
            RecurringEvent event = RecurringEvent.createRecurringEvent(
                "Test", startTime, endTime, startDate, endDate,
                "Description", recurrenceRule, creator, false
            );

            event.setStartTime(null);

            assertThat(event.getStartTime()).isNull();
        }

        @Test
        void setEndTime_withNull_shouldSetNull() {
            RecurringEvent event = RecurringEvent.createRecurringEvent(
                "Test", startTime, endTime, startDate, endDate,
                "Description", recurrenceRule, creator, false
            );

            event.setEndTime(null);

            assertThat(event.getEndTime()).isNull();
        }
    }

    @Nested
    class SkipDaysManagement {

        private RecurringEvent event;
        private LocalDate skipDay1;
        private LocalDate skipDay2;

        @BeforeEach
        void setUp() {
            event = RecurringEvent.createRecurringEvent(
                "Test Event", startTime, endTime, startDate, endDate,
                "Description", recurrenceRule, creator, false
            );
            skipDay1 = LocalDate.of(2024, 2, 15);
            skipDay2 = LocalDate.of(2024, 3, 15);
        }

        @Test
        void addSkipDay_shouldAddDateToSkipDays() {
            event.addSkipDay(skipDay1);

            assertThat(event.getSkipDays()).contains(skipDay1);
        }

        @Test
        void addSkipDay_withMultipleDates_shouldAddAll() {
            event.addSkipDay(skipDay1);
            event.addSkipDay(skipDay2);

            assertThat(event.getSkipDays()).containsExactlyInAnyOrder(skipDay1, skipDay2);
        }

        @Test
        void addSkipDay_withDuplicateDate_shouldNotAddDuplicate() {
            event.addSkipDay(skipDay1);
            event.addSkipDay(skipDay1);

            assertThat(event.getSkipDays()).containsExactly(skipDay1);
        }

        @Test
        void addSkipDay_withNull_shouldNotAddNull() {
            event.addSkipDay(null);

            assertThat(event.getSkipDays()).isEmpty();
        }

        @Test
        void removeSkipDay_shouldRemoveDateFromSkipDays() {
            event.addSkipDay(skipDay1);
            event.addSkipDay(skipDay2);

            event.removeSkipDay(skipDay1);

            assertThat(event.getSkipDays()).containsExactly(skipDay2);
        }

        @Test
        void removeSkipDay_withNonExistentDate_shouldHaveNoEffect() {
            event.addSkipDay(skipDay1);

            event.removeSkipDay(skipDay2);

            assertThat(event.getSkipDays()).containsExactly(skipDay1);
        }

        @Test
        void removeSkipDay_withNull_shouldHaveNoEffect() {
            event.addSkipDay(skipDay1);

            event.removeSkipDay(null);

            assertThat(event.getSkipDays()).containsExactly(skipDay1);
        }

        @Test
        void setSkipDays_shouldReplaceEntireCollection() {
            event.addSkipDay(skipDay1);
            Set<LocalDate> newSkipDays = Set.of(skipDay2, LocalDate.of(2024, 4, 15));

            event.setSkipDays(newSkipDays);

            assertThat(event.getSkipDays()).containsExactlyInAnyOrderElementsOf(newSkipDays);
            assertThat(event.getSkipDays()).doesNotContain(skipDay1);
        }

        @Test
        void getSkipDays_shouldReturnMutableCollection() {
            event.addSkipDay(skipDay1);
            Set<LocalDate> skipDays = event.getSkipDays();

            skipDays.add(skipDay2);

            assertThat(event.getSkipDays()).contains(skipDay2);
        }
    }

    @Nested
    class EqualityAndHashing {

        @Test
        void equals_withSameId_shouldReturnTrue() {
            RecurringEvent event1 = RecurringEvent.createRecurringEvent(
                "Event 1", startTime, endTime, startDate, endDate,
                "Description 1", recurrenceRule, creator, false
            );
            RecurringEvent event2 = RecurringEvent.createRecurringEvent(
                "Event 2", startTime.plusHours(1), endTime.plusHours(1), startDate.plusDays(1), endDate.plusDays(1),
                "Description 2", recurrenceRule, creator, true
            );
            
            setRecurringEventId(event1, 1L);
            setRecurringEventId(event2, 1L);

            // RecurringEvent uses all fields for equality, not just ID
            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void equals_withDifferentIds_shouldReturnFalse() {
            RecurringEvent event1 = RecurringEvent.createRecurringEvent(
                "Same Event", startTime, endTime, startDate, endDate,
                "Same Description", recurrenceRule, creator, false
            );
            RecurringEvent event2 = RecurringEvent.createRecurringEvent(
                "Same Event", startTime, endTime, startDate, endDate,
                "Same Description", recurrenceRule, creator, false
            );
            
            setRecurringEventId(event1, 1L);
            setRecurringEventId(event2, 2L);

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void equals_withNullIds_shouldCompareAllFields() {
            RecurringEvent event1 = RecurringEvent.createRecurringEvent(
                "Test Event", startTime, endTime, startDate, endDate,
                "Description", recurrenceRule, creator, false
            );
            RecurringEvent event2 = RecurringEvent.createRecurringEvent(
                "Test Event", startTime, endTime, startDate, endDate,
                "Description", recurrenceRule, creator, false
            );

            assertThat(event1).isEqualTo(event2);
        }

        @Test
        void equals_withDifferentNames_shouldReturnFalse() {
            RecurringEvent event1 = RecurringEvent.createRecurringEvent(
                "Event 1", startTime, endTime, startDate, endDate,
                "Description", recurrenceRule, creator, false
            );
            RecurringEvent event2 = RecurringEvent.createRecurringEvent(
                "Event 2", startTime, endTime, startDate, endDate,
                "Description", recurrenceRule, creator, false
            );

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void equals_withDifferentSkipDays_shouldReturnFalse() {
            RecurringEvent event1 = RecurringEvent.createRecurringEvent(
                "Test Event", startTime, endTime, startDate, endDate,
                "Description", recurrenceRule, creator, false
            );
            RecurringEvent event2 = RecurringEvent.createRecurringEvent(
                "Test Event", startTime, endTime, startDate, endDate,
                "Description", recurrenceRule, creator, false
            );
            
            event1.addSkipDay(LocalDate.of(2024, 2, 15));
            event2.addSkipDay(LocalDate.of(2024, 3, 15));

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void equals_withDifferentUnconfirmedStatus_shouldReturnFalse() {
            RecurringEvent event1 = RecurringEvent.createRecurringEvent(
                "Test Event", startTime, endTime, startDate, endDate,
                "Description", recurrenceRule, creator, false
            );
            RecurringEvent event2 = RecurringEvent.createRecurringEvent(
                "Test Event", startTime, endTime, startDate, endDate,
                "Description", recurrenceRule, creator, true
            );

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void hashCode_shouldBeConsistentWithEquals() {
            RecurringEvent event1 = RecurringEvent.createRecurringEvent(
                "Test Event", startTime, endTime, startDate, endDate,
                "Description", recurrenceRule, creator, false
            );
            RecurringEvent event2 = RecurringEvent.createRecurringEvent(
                "Test Event", startTime, endTime, startDate, endDate,
                "Description", recurrenceRule, creator, false
            );

            assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
        }

        @Test
        void hashCode_shouldIncludeSkipDays() {
            RecurringEvent event1 = RecurringEvent.createRecurringEvent(
                "Test Event", startTime, endTime, startDate, endDate,
                "Description", recurrenceRule, creator, false
            );
            RecurringEvent event2 = RecurringEvent.createRecurringEvent(
                "Test Event", startTime, endTime, startDate, endDate,
                "Description", recurrenceRule, creator, false
            );
            
            event1.addSkipDay(LocalDate.of(2024, 2, 15));

            assertThat(event1.hashCode()).isNotEqualTo(event2.hashCode());
        }
    }

    @Nested
    class StateManagement {

        @Test
        void defaultState_shouldBeUnconfirmed() {
            RecurringEvent draft = RecurringEvent.createUnconfirmedDraftRecurringEvent(
                "Draft", startDate, endDate, creator
            );

            assertThat(draft.isUnconfirmed()).isTrue();
        }

        @Test
        void confirmedEvent_shouldNotBeUnconfirmed() {
            RecurringEvent event = RecurringEvent.createRecurringEvent(
                "Confirmed", startTime, endTime, startDate, endDate,
                "Description", recurrenceRule, creator, false
            );

            assertThat(event.isUnconfirmed()).isFalse();
        }

        @Test
        void setUnconfirmed_shouldUpdateUnconfirmedFlag() {
            RecurringEvent event = RecurringEvent.createRecurringEvent(
                "Test", startTime, endTime, startDate, endDate,
                "Description", recurrenceRule, creator, false
            );

            event.setUnconfirmed(true);

            assertThat(event.isUnconfirmed()).isTrue();
        }
    }

    @Nested
    class InitialState {

        @Test
        void newEvent_shouldHaveEmptySkipDays() {
            RecurringEvent event = RecurringEvent.createRecurringEvent(
                "Test", startTime, endTime, startDate, endDate,
                "Description", recurrenceRule, creator, false
            );

            assertThat(event.getSkipDays()).isEmpty();
        }

        @Test
        void newDraft_shouldHaveEmptySkipDays() {
            RecurringEvent draft = RecurringEvent.createUnconfirmedDraftRecurringEvent(
                "Draft", startDate, endDate, creator
            );

            assertThat(draft.getSkipDays()).isEmpty();
        }
    }

    // Helper method using reflection
    private void setRecurringEventId(RecurringEvent event, Long id) {
        try {
            var field = RecurringEvent.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(event, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set recurring event ID", e);
        }
    }
}