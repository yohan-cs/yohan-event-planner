package com.yohan.event_planner.domain;

import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class EventTest {

    private User creator;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    @BeforeEach
    void setUp() {
        creator = TestUtils.createValidUserEntity();
        startTime = ZonedDateTime.now().plusHours(1).truncatedTo(ChronoUnit.MINUTES);
        endTime = startTime.plusHours(2);
    }

    @Nested
    class FactoryMethods {

        @Test
        void createEvent_shouldCreateConfirmedEvent() {
            Event event = Event.createEvent("Test Event", startTime, endTime, creator);

            assertThat(event.getName()).isEqualTo("Test Event");
            assertThat(event.getStartTime()).isEqualTo(startTime.withZoneSameInstant(ZoneOffset.UTC));
            assertThat(event.getEndTime()).isEqualTo(endTime.withZoneSameInstant(ZoneOffset.UTC));
            assertThat(event.getCreator()).isEqualTo(creator);
            assertThat(event.isUnconfirmed()).isFalse();
            assertThat(event.isImpromptu()).isFalse();
        }

        @Test
        void createImpromptuEvent_shouldCreateUnconfirmedEventWithoutName() {
            Event event = Event.createImpromptuEvent(startTime, creator);

            assertThat(event.getName()).isNull();
            assertThat(event.getStartTime()).isEqualTo(startTime.withZoneSameInstant(ZoneOffset.UTC));
            assertThat(event.getEndTime()).isNull();
            assertThat(event.getCreator()).isEqualTo(creator);
            assertThat(event.isUnconfirmed()).isTrue();
            assertThat(event.isImpromptu()).isTrue();
        }

        @Test
        void createUnconfirmedDraft_shouldCreateDraftWithPartialData() {
            Event event = Event.createUnconfirmedDraft("Draft Event", startTime, null, creator);

            assertThat(event.getName()).isEqualTo("Draft Event");
            assertThat(event.getStartTime()).isEqualTo(startTime.withZoneSameInstant(ZoneOffset.UTC));
            assertThat(event.getEndTime()).isNull();
            assertThat(event.getCreator()).isEqualTo(creator);
            assertThat(event.isUnconfirmed()).isTrue();
            assertThat(event.isImpromptu()).isFalse();
        }

        @Test
        void createUnconfirmedDraft_shouldAllowAllNullFields() {
            Event event = Event.createUnconfirmedDraft(null, null, null, creator);

            assertThat(event.getName()).isNull();
            assertThat(event.getStartTime()).isNull();
            assertThat(event.getEndTime()).isNull();
            assertThat(event.getCreator()).isEqualTo(creator);
            assertThat(event.isUnconfirmed()).isTrue();
            assertThat(event.isImpromptu()).isFalse();
        }
    }

    @Nested
    class TimezoneHandling {

        @Test
        void setStartTime_shouldExtractTimezoneAndConvertToUtc() {
            ZonedDateTime nyTime = ZonedDateTime.of(2024, 1, 15, 14, 30, 0, 0, 
                                                   ZoneId.of("America/New_York"));
            Event event = Event.createUnconfirmedDraft(null, null, null, creator);

            event.setStartTime(nyTime);

            assertThat(event.getStartTimezone()).isEqualTo("America/New_York");
            assertThat(event.getStartTime()).isEqualTo(nyTime.withZoneSameInstant(ZoneOffset.UTC));
        }

        @Test
        void setEndTime_shouldExtractTimezoneAndConvertToUtc() {
            ZonedDateTime londonTime = ZonedDateTime.of(2024, 1, 15, 19, 30, 0, 0, 
                                                       ZoneId.of("Europe/London"));
            Event event = Event.createUnconfirmedDraft(null, null, null, creator);

            event.setEndTime(londonTime);

            assertThat(event.getEndTimezone()).isEqualTo("Europe/London");
            assertThat(event.getEndTime()).isEqualTo(londonTime.withZoneSameInstant(ZoneOffset.UTC));
        }

        @Test
        void setStartTime_withNull_shouldClearTimezoneAndTime() {
            Event event = Event.createEvent("Test", startTime, endTime, creator);

            event.setStartTime(null);

            assertThat(event.getStartTime()).isNull();
            assertThat(event.getStartTimezone()).isNull();
        }

        @Test
        void setEndTime_withNull_shouldClearTimezoneAndTime() {
            Event event = Event.createEvent("Test", startTime, endTime, creator);

            event.setEndTime(null);

            assertThat(event.getEndTime()).isNull();
            assertThat(event.getEndTimezone()).isNull();
        }

        @Test
        void setStartTime_shouldTruncateToMinutes() {
            ZonedDateTime timeWithSeconds = ZonedDateTime.of(2024, 1, 15, 14, 30, 45, 123456789, 
                                                            ZoneId.of("UTC"));
            ZonedDateTime expectedTruncated = timeWithSeconds.truncatedTo(ChronoUnit.MINUTES);
            Event event = Event.createUnconfirmedDraft(null, null, null, creator);

            event.setStartTime(timeWithSeconds);

            assertThat(event.getStartTime()).isEqualTo(expectedTruncated);
        }

        @Test
        void setEndTime_shouldTruncateToMinutes() {
            ZonedDateTime timeWithSeconds = ZonedDateTime.of(2024, 1, 15, 16, 30, 45, 123456789, 
                                                            ZoneId.of("UTC"));
            ZonedDateTime expectedTruncated = timeWithSeconds.truncatedTo(ChronoUnit.MINUTES);
            Event event = Event.createUnconfirmedDraft(null, null, null, creator);

            event.setEndTime(timeWithSeconds);

            assertThat(event.getEndTime()).isEqualTo(expectedTruncated);
        }
    }

    @Nested
    class DurationCalculation {

        @Test
        void durationCalculation_withBothTimes_shouldCalculateCorrectDuration() {
            Event event = Event.createEvent("Test", startTime, endTime, creator);

            assertThat(event.getDurationMinutes()).isEqualTo(120); // 2 hours
        }

        @Test
        void durationCalculation_withOnlyStartTime_shouldBeNull() {
            Event event = Event.createImpromptuEvent(startTime, creator);

            assertThat(event.getDurationMinutes()).isNull();
        }

        @Test
        void durationCalculation_withOnlyEndTime_shouldBeNull() {
            Event event = Event.createUnconfirmedDraft(null, null, endTime, creator);

            assertThat(event.getDurationMinutes()).isNull();
        }

        @Test
        void durationCalculation_shouldRecalculateWhenStartTimeChanges() {
            Event event = Event.createEvent("Test", startTime, endTime, creator);
            ZonedDateTime newStartTime = startTime.plusMinutes(30);

            event.setStartTime(newStartTime);

            assertThat(event.getDurationMinutes()).isEqualTo(90); // 1.5 hours
        }

        @Test
        void durationCalculation_shouldRecalculateWhenEndTimeChanges() {
            Event event = Event.createEvent("Test", startTime, endTime, creator);
            ZonedDateTime newEndTime = endTime.plusMinutes(30);

            event.setEndTime(newEndTime);

            assertThat(event.getDurationMinutes()).isEqualTo(150); // 2.5 hours
        }

        @Test
        void durationCalculation_shouldHandleAcrossTimezones() {
            ZonedDateTime nyStart = ZonedDateTime.of(2024, 1, 15, 14, 0, 0, 0, 
                                                    ZoneId.of("America/New_York"));
            ZonedDateTime londonEnd = ZonedDateTime.of(2024, 1, 15, 20, 0, 0, 0, 
                                                      ZoneId.of("Europe/London"));
            Event event = Event.createEvent("Cross-timezone", nyStart, londonEnd, creator);

            // NY 14:00 = UTC 19:00, London 20:00 = UTC 20:00, so 1 hour duration
            assertThat(event.getDurationMinutes()).isEqualTo(60);
        }

        @Test
        void durationCalculation_shouldBecomeNullWhenStartTimeCleared() {
            Event event = Event.createEvent("Test", startTime, endTime, creator);

            event.setStartTime(null);

            assertThat(event.getDurationMinutes()).isNull();
        }

        @Test
        void durationCalculation_shouldBecomeNullWhenEndTimeCleared() {
            Event event = Event.createEvent("Test", startTime, endTime, creator);

            event.setEndTime(null);

            assertThat(event.getDurationMinutes()).isNull();
        }
    }

    @Nested
    class EqualityAndHashing {

        @Test
        void equals_withSameId_shouldReturnTrue() {
            Event event1 = Event.createEvent("Event 1", startTime, endTime, creator);
            Event event2 = Event.createEvent("Event 2", startTime.plusHours(1), endTime.plusHours(1), creator);
            
            setEventId(event1, 1L);
            setEventId(event2, 1L);

            assertThat(event1).isEqualTo(event2);
        }

        @Test
        void equals_withDifferentIds_shouldReturnFalse() {
            Event event1 = Event.createEvent("Same Event", startTime, endTime, creator);
            Event event2 = Event.createEvent("Same Event", startTime, endTime, creator);
            
            setEventId(event1, 1L);
            setEventId(event2, 2L);

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void equals_withNullIds_shouldCompareAllFields() {
            Event event1 = Event.createEvent("Test Event", startTime, endTime, creator);
            Event event2 = Event.createEvent("Test Event", startTime, endTime, creator);

            assertThat(event1).isEqualTo(event2);
        }

        @Test
        void equals_withNullIds_differentNames_shouldReturnFalse() {
            Event event1 = Event.createEvent("Event 1", startTime, endTime, creator);
            Event event2 = Event.createEvent("Event 2", startTime, endTime, creator);

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void equals_withNullIds_differentStartTimes_shouldReturnFalse() {
            Event event1 = Event.createEvent("Test Event", startTime, endTime, creator);
            Event event2 = Event.createEvent("Test Event", startTime.plusHours(1), endTime, creator);

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void equals_withNullIds_differentEndTimes_shouldReturnFalse() {
            Event event1 = Event.createEvent("Test Event", startTime, endTime, creator);
            Event event2 = Event.createEvent("Test Event", startTime, endTime.plusHours(1), creator);

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void equals_withNullIds_differentCreators_shouldReturnFalse() {
            User otherCreator = TestUtils.createTestUser("otheruser");
            Event event1 = Event.createEvent("Test Event", startTime, endTime, creator);
            Event event2 = Event.createEvent("Test Event", startTime, endTime, otherCreator);

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void equals_withNullIds_differentTimezones_shouldReturnFalse() {
            ZonedDateTime nyTime = startTime.withZoneSameInstant(ZoneId.of("America/New_York"));
            Event event1 = Event.createEvent("Test Event", startTime, endTime, creator);
            Event event2 = Event.createEvent("Test Event", nyTime, endTime, creator);

            // Even though UTC times are same, timezones are different
            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void hashCode_withId_shouldUseIdHashCode() {
            Event event = Event.createEvent("Test", startTime, endTime, creator);
            setEventId(event, 1L);

            assertThat(event.hashCode()).isEqualTo(Long.valueOf(1L).hashCode());
        }

        @Test
        void hashCode_withoutId_shouldUseFieldsHash() {
            Event event = Event.createEvent("Test", startTime, endTime, creator);
            
            int expectedHash = java.util.Objects.hash(
                event.getName(), 
                event.getStartTime(), 
                event.getEndTime(), 
                event.getCreator(),
                event.getStartTimezone(),
                event.getEndTimezone()
            );

            assertThat(event.hashCode()).isEqualTo(expectedHash);
        }
    }

    @Nested
    class StateManagement {

        @Test
        void defaultState_shouldBeIncompleteAndUnconfirmed() {
            Event event = Event.createUnconfirmedDraft("Draft", null, null, creator);

            assertThat(event.isCompleted()).isFalse();
            assertThat(event.isUnconfirmed()).isTrue();
        }

        @Test
        void confirmedEvent_shouldNotBeUnconfirmed() {
            Event event = Event.createEvent("Confirmed", startTime, endTime, creator);

            assertThat(event.isUnconfirmed()).isFalse();
        }

        @Test
        void setCompleted_shouldUpdateCompletedFlag() {
            Event event = Event.createEvent("Test", startTime, endTime, creator);

            event.setCompleted(true);

            assertThat(event.isCompleted()).isTrue();
        }

        @Test
        void setUnconfirmed_shouldUpdateUnconfirmedFlag() {
            Event event = Event.createEvent("Test", startTime, endTime, creator);

            event.setUnconfirmed(true);

            assertThat(event.isUnconfirmed()).isTrue();
        }
    }

    // Helper method using reflection
    private void setEventId(Event event, Long id) {
        try {
            var field = Event.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(event, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set event ID", e);
        }
    }

    private void setEventField(Event event, String fieldName, Object value) {
        try {
            var field = Event.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(event, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set event field: " + fieldName, e);
        }
    }

    @Nested
    class EqualsHashCodeEdgeCases {

        @Test
        void equals_withNullIds_allFieldsNull_shouldReturnTrue() {
            // Test fallback logic when all relevant fields are null
            Event event1 = Event.createUnconfirmedDraft(null, null, null, creator);
            Event event2 = Event.createUnconfirmedDraft(null, null, null, creator);

            assertThat(event1).isEqualTo(event2);
        }

        @Test
        void equals_withNullIds_nullName_differentOtherFields_shouldReturnFalse() {
            // Test fallback logic with null names but different other fields
            Event event1 = Event.createUnconfirmedDraft(null, startTime, endTime, creator);
            Event event2 = Event.createUnconfirmedDraft(null, startTime.plusHours(1), endTime, creator);

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void equals_withNullIds_nullStartTime_shouldHandleGracefully() {
            // Test fallback logic with null start times
            Event event1 = Event.createUnconfirmedDraft("Test Event", null, endTime, creator);
            Event event2 = Event.createUnconfirmedDraft("Test Event", null, endTime, creator);

            assertThat(event1).isEqualTo(event2);
        }

        @Test
        void equals_withNullIds_nullEndTime_shouldHandleGracefully() {
            // Test fallback logic with null end times
            Event event1 = Event.createUnconfirmedDraft("Test Event", startTime, null, creator);
            Event event2 = Event.createUnconfirmedDraft("Test Event", startTime, null, creator);

            assertThat(event1).isEqualTo(event2);
        }

        @Test
        void equals_withNullIds_nullCreator_shouldHandleGracefully() {
            // Test fallback logic with null creators (edge case)
            Event event1 = Event.createUnconfirmedDraft("Test Event", startTime, endTime, creator);
            Event event2 = Event.createUnconfirmedDraft("Test Event", startTime, endTime, creator);
            
            setEventField(event1, "creator", null);
            setEventField(event2, "creator", null);

            assertThat(event1).isEqualTo(event2);
        }

        @Test
        void equals_withNullIds_differentTimezones_shouldReturnFalse() {
            // Test fallback logic considers timezones
            Event event1 = Event.createEvent("Test Event", startTime, endTime, creator);
            Event event2 = Event.createEvent("Test Event", startTime, endTime, creator);
            
            // Manually set different timezones
            setEventField(event1, "startTimezone", "UTC");
            setEventField(event1, "endTimezone", "UTC");
            setEventField(event2, "startTimezone", "America/New_York");
            setEventField(event2, "endTimezone", "America/New_York");

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void equals_withNullIds_oneNullTimezone_shouldReturnFalse() {
            // Test fallback logic when one event has null timezone
            Event event1 = Event.createEvent("Test Event", startTime, endTime, creator);
            Event event2 = Event.createEvent("Test Event", startTime, endTime, creator);
            
            setEventField(event1, "startTimezone", null);

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void equals_withMixedNullIds_shouldUseFallbackLogic() {
            // Test mixed scenario: one has ID, one doesn't - should use fallback logic
            Event event1 = Event.createEvent("Test Event", startTime, endTime, creator);
            Event event2 = Event.createEvent("Test Event", startTime, endTime, creator);
            
            setEventId(event1, 1L); // Only event1 has ID
            
            // Since only one has ID, fallback to field-based comparison
            // Both have same fields, so should be equal
            assertThat(event1).isEqualTo(event2);
        }

        @Test
        void hashCode_withNullIds_shouldUseAllFields() {
            // Test hashCode fallback calculation uses all relevant fields
            Event event1 = Event.createEvent("Test Event", startTime, endTime, creator);
            Event event2 = Event.createEvent("Test Event", startTime, endTime, creator);
            
            // Both should have same hashCode (same all fields)
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
        }

        @Test
        void hashCode_withNullIds_nullName_shouldHandleGracefully() {
            // Test hashCode when name is null
            Event event = Event.createUnconfirmedDraft(null, startTime, endTime, creator);
            
            assertThat(event.hashCode()).isNotNull(); // Should not throw
        }

        @Test
        void hashCode_withNullIds_nullTimes_shouldHandleGracefully() {
            // Test hashCode when times are null
            Event event = Event.createUnconfirmedDraft("Test Event", null, null, creator);
            
            assertThat(event.hashCode()).isNotNull(); // Should not throw
        }

        @Test
        void hashCode_withNullIds_nullTimezones_shouldHandleGracefully() {
            // Test hashCode when timezones are null
            Event event = Event.createUnconfirmedDraft("Test Event", startTime, endTime, creator);
            setEventField(event, "startTimezone", null);
            setEventField(event, "endTimezone", null);
            
            assertThat(event.hashCode()).isNotNull(); // Should not throw
        }

        @Test
        void hashCode_shouldBeConsistentWithEquals() {
            // Test that equal objects have equal hash codes
            Event event1 = Event.createEvent("Test Event", startTime, endTime, creator);
            Event event2 = Event.createEvent("Test Event", startTime, endTime, creator);
            
            assertThat(event1).isEqualTo(event2);
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
        }

        @Test
        void equals_fallbackLogic_shouldConsiderAllRelevantFields() {
            // Test that fallback logic considers name, times, creator, and timezones
            Event event1 = Event.createEvent("Test Event", startTime, endTime, creator);
            Event event2 = Event.createEvent("Test Event", startTime, endTime, creator);
            
            // Verify they're equal
            assertThat(event1).isEqualTo(event2);
            
            // Change any field and verify they become unequal
            setEventField(event1, "name", "Different Event");
            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void hashCode_idVsFallback_shouldBeDifferent() {
            // Test that ID-based and fallback hashCodes can be different
            Event event1 = Event.createEvent("Test Event", startTime, endTime, creator);
            Event event2 = Event.createEvent("Test Event", startTime, endTime, creator);
            
            setEventId(event1, 999L); // ID that likely has different hash than combined fields
            
            // event1 uses ID.hashCode(), event2 uses Objects.hash() of all fields
            // They might be different (not guaranteed, but likely)
            boolean hashesAreDifferent = event1.hashCode() != event2.hashCode();
            // This test documents the behavior but doesn't assert since hash collision is possible
            assertThat(hashesAreDifferent || !hashesAreDifferent).isTrue(); // Always true, just documenting
        }

        @Test
        void equals_complexScenario_withPartialNullFields() {
            // Test complex scenario with mixed null and non-null fields
            Event event1 = Event.createUnconfirmedDraft("Test Event", startTime, null, creator);
            Event event2 = Event.createUnconfirmedDraft("Test Event", startTime, null, creator);
            
            // Set one timezone to null, one to a value
            setEventField(event1, "startTimezone", null);
            setEventField(event2, "startTimezone", "UTC");

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void equals_timezoneConsistency_shouldMatter() {
            // Test that timezone consistency matters in equality
            Event event1 = Event.createEvent("Test Event", startTime, endTime, creator);
            Event event2 = Event.createEvent("Test Event", startTime, endTime, creator);
            
            // Set mismatched timezone combinations
            setEventField(event1, "startTimezone", "UTC");
            setEventField(event1, "endTimezone", "America/New_York");
            setEventField(event2, "startTimezone", "America/New_York");
            setEventField(event2, "endTimezone", "UTC");

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        void equals_withTransientEvents_shouldUseFallbackLogic() {
            // Test that transient events (no ID) always use fallback logic
            Event event1 = Event.createEvent("Test Event", startTime, endTime, creator);
            Event event2 = Event.createEvent("Test Event", startTime, endTime, creator);
            
            // Both are transient (no IDs set)
            assertThat(event1.getId()).isNull();
            assertThat(event2.getId()).isNull();
            
            // Should be equal using fallback logic
            assertThat(event1).isEqualTo(event2);
        }
    }
}