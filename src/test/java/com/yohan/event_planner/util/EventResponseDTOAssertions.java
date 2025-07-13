package com.yohan.event_planner.util;

import com.yohan.event_planner.dto.EventResponseDTO;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Assertion helper for comparing {@link EventResponseDTO} objects in tests.
 */
public class EventResponseDTOAssertions {

    public static void assertEventResponseDTOEquals(EventResponseDTO expected, EventResponseDTO actual) {
        assertNotNull(expected);
        assertNotNull(actual);

        assertEquals(expected.id(), actual.id());
        assertEquals(expected.name(), actual.name());

        assertZonedDateTimeEquals(expected.startTimeUtc(), actual.startTimeUtc());
        assertZonedDateTimeEquals(expected.endTimeUtc(), actual.endTimeUtc());

        assertEquals(expected.durationMinutes(), actual.durationMinutes());
        assertEquals(expected.startTimeZone(), actual.startTimeZone());
        assertEquals(expected.endTimeZone(), actual.endTimeZone());
        assertEquals(expected.description(), actual.description());

        assertEquals(expected.creatorUsername(), actual.creatorUsername());

        // LabelResponseDTO comparison
        if (expected.label() == null) {
            assertNull(actual.label(), "Expected label to be null");
        } else {
            assertNotNull(actual.label(), "Expected label to be non-null");
            assertEquals(expected.label().id(), actual.label().id(), "Label id mismatch");
            assertEquals(expected.label().name(), actual.label().name(), "Label name mismatch");
            assertEquals(expected.label().creatorUsername(), actual.label().creatorUsername(), "Label creatorUsername mismatch");
        }

        assertEquals(expected.isCompleted(), actual.isCompleted());
        assertEquals(expected.unconfirmed(), actual.unconfirmed());

        assertEquals(expected.isVirtual(), actual.isVirtual());
    }

    private static void assertZonedDateTimeEquals(ZonedDateTime expected, ZonedDateTime actual) {
        if (expected == null && actual == null) return;

        assertNotNull(expected, "Expected ZonedDateTime was null");
        assertNotNull(actual, "Actual ZonedDateTime was null");
        assertEquals(expected.toInstant(), actual.toInstant());
    }
}
