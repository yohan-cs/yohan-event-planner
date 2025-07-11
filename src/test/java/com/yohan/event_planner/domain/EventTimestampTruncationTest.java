package com.yohan.event_planner.domain;

import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test class to verify that Event timestamps are truncated to minute precision.
 */
public class EventTimestampTruncationTest {

    private User testUser;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        testUser = TestUtils.createValidUserEntityWithId();
        fixedClock = Clock.fixed(
                ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC).toInstant(),
                ZoneOffset.UTC
        );
    }

    @Test
    void testSetStartTime_ShouldTruncateToMinutes() {
        // Given: A timestamp with seconds and nanoseconds
        ZonedDateTime timeWithSeconds = ZonedDateTime.of(
                2024, 1, 1, 15, 30, 45, 123456789, ZoneOffset.UTC
        );
        
        // When: Setting start time on event
        Event event = new Event();
        event.setStartTime(timeWithSeconds);
        
        // Then: Timestamp should be truncated to minutes
        ZonedDateTime expected = ZonedDateTime.of(
                2024, 1, 1, 15, 30, 0, 0, ZoneOffset.UTC
        );
        assertEquals(expected, event.getStartTime());
    }

    @Test
    void testSetEndTime_ShouldTruncateToMinutes() {
        // Given: A timestamp with seconds and nanoseconds
        ZonedDateTime timeWithSeconds = ZonedDateTime.of(
                2024, 1, 1, 16, 45, 30, 987654321, ZoneOffset.UTC
        );
        
        // When: Setting end time on event
        Event event = new Event();
        event.setEndTime(timeWithSeconds);
        
        // Then: Timestamp should be truncated to minutes
        ZonedDateTime expected = ZonedDateTime.of(
                2024, 1, 1, 16, 45, 0, 0, ZoneOffset.UTC
        );
        assertEquals(expected, event.getEndTime());
    }

    @Test
    void testSetStartTime_NullValue_ShouldHandleGracefully() {
        // When: Setting null start time
        Event event = new Event();
        event.setStartTime(null);
        
        // Then: Should handle null gracefully
        assertNull(event.getStartTime());
        assertNull(event.getStartTimezone());
    }

    @Test
    void testSetEndTime_NullValue_ShouldHandleGracefully() {
        // When: Setting null end time
        Event event = new Event();
        event.setEndTime(null);
        
        // Then: Should handle null gracefully
        assertNull(event.getEndTime());
        assertNull(event.getEndTimezone());
    }

    @Test
    void testEventCreation_ShouldTruncateTimestamps() {
        // Given: Timestamps with seconds and nanoseconds
        ZonedDateTime startWithSeconds = ZonedDateTime.of(
                2024, 1, 1, 10, 15, 30, 123456789, ZoneOffset.UTC
        );
        ZonedDateTime endWithSeconds = ZonedDateTime.of(
                2024, 1, 1, 11, 30, 45, 987654321, ZoneOffset.UTC
        );
        
        // When: Creating event with factory method
        Event event = Event.createEvent("Test Event", startWithSeconds, endWithSeconds, testUser);
        
        // Then: Timestamps should be truncated to minutes
        ZonedDateTime expectedStart = ZonedDateTime.of(
                2024, 1, 1, 10, 15, 0, 0, ZoneOffset.UTC
        );
        ZonedDateTime expectedEnd = ZonedDateTime.of(
                2024, 1, 1, 11, 30, 0, 0, ZoneOffset.UTC
        );
        
        assertEquals(expectedStart, event.getStartTime());
        assertEquals(expectedEnd, event.getEndTime());
    }

    @Test
    void testTimezonePreservation_AfterTruncation() {
        // Given: A timestamp with timezone and seconds
        ZonedDateTime timeWithTimezone = ZonedDateTime.of(
                2024, 1, 1, 15, 30, 45, 123456789, ZoneOffset.of("+05:00")
        );
        
        // When: Setting start time on event
        Event event = new Event();
        event.setStartTime(timeWithTimezone);
        
        // Then: Time should be truncated but timezone should be preserved
        assertEquals("+05:00", event.getStartTimezone());
        
        // And: UTC time should be truncated to minutes
        ZonedDateTime expectedUtc = ZonedDateTime.of(
                2024, 1, 1, 10, 30, 0, 0, ZoneOffset.UTC
        );
        assertEquals(expectedUtc, event.getStartTime());
    }

    @Test
    void testDurationCalculation_AfterTruncation() {
        // Given: Start and end times with seconds that would affect duration
        ZonedDateTime start = ZonedDateTime.of(
                2024, 1, 1, 10, 0, 30, 0, ZoneOffset.UTC
        );
        ZonedDateTime end = ZonedDateTime.of(
                2024, 1, 1, 11, 0, 45, 0, ZoneOffset.UTC
        );
        
        // When: Creating event
        Event event = Event.createEvent("Test Event", start, end, testUser);
        
        // Then: Duration should be calculated from truncated times (exactly 60 minutes)
        assertEquals(60, event.getDurationMinutes());
    }
}