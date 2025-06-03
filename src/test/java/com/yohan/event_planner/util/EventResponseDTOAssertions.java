package com.yohan.event_planner.util;

import com.yohan.event_planner.dto.EventResponseDTO;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

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

        assertEquals(expected.startTimeZone(), actual.startTimeZone());
        assertEquals(expected.endTimeZone(), actual.endTimeZone());

        assertEquals(expected.description(), actual.description());
        assertEquals(expected.creatorId(), actual.creatorId());
        assertEquals(expected.creatorUsername(), actual.creatorUsername());
        assertEquals(expected.creatorTimezone(), actual.creatorTimezone());
    }

    private static void assertZonedDateTimeEquals(ZonedDateTime expected, ZonedDateTime actual) {
        if (expected == null && actual == null) return;
        assertNotNull(expected);
        assertNotNull(actual);
        assertEquals(expected.toInstant(), actual.toInstant());
    }
}
