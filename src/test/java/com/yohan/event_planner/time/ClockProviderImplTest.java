package com.yohan.event_planner.time;

import com.yohan.event_planner.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRulesException;

import static org.junit.jupiter.api.Assertions.*;

class ClockProviderImplTest {

    private Clock baseClock;
    private ClockProviderImpl clockProvider;
    private final Instant FIXED_TIME = Instant.parse("2025-07-10T15:30:00Z");

    @BeforeEach
    void setUp() {
        baseClock = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);
        clockProvider = new ClockProviderImpl(baseClock);
    }

    @Test
    void testConstructor_withValidClock() {
        // Act
        ClockProviderImpl provider = new ClockProviderImpl(baseClock);

        // Assert
        assertNotNull(provider, "ClockProviderImpl should be created successfully");
    }

    @Test
    void testGetClockForZone_withUTC() {
        // Act
        Clock utcClock = clockProvider.getClockForZone(ZoneOffset.UTC);

        // Assert
        assertNotNull(utcClock, "UTC clock should not be null");
        assertEquals(ZoneOffset.UTC, utcClock.getZone(), "Clock should be in UTC timezone");
        assertEquals(FIXED_TIME, utcClock.instant(), "Clock should have same time as base clock");
    }

    @Test
    void testGetClockForZone_withTimezone() {
        // Arrange
        ZoneId newYorkZone = ZoneId.of("America/New_York");

        // Act
        Clock newYorkClock = clockProvider.getClockForZone(newYorkZone);

        // Assert
        assertNotNull(newYorkClock, "New York clock should not be null");
        assertEquals(newYorkZone, newYorkClock.getZone(), "Clock should be in New York timezone");
        assertEquals(FIXED_TIME, newYorkClock.instant(), "Clock should have same instant as base clock");
    }

    @Test
    void testGetClockForZone_withNullZone() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            clockProvider.getClockForZone(null);
        }, "Should throw NullPointerException for null zone");
    }

    @Test
    void testGetClockForUser_withValidUser() {
        // Arrange
        User user = createUserWithTimezone("America/New_York");

        // Act
        Clock userClock = clockProvider.getClockForUser(user);

        // Assert
        assertNotNull(userClock, "User clock should not be null");
        assertEquals(ZoneId.of("America/New_York"), userClock.getZone(), 
            "Clock should be in user's timezone");
        assertEquals(FIXED_TIME, userClock.instant(), "Clock should have same instant as base clock");
    }

    @Test
    void testGetClockForUser_withInvalidTimezone() {
        // Arrange
        User user = createUserWithTimezone("Invalid/Timezone");

        // Act & Assert
        assertThrows(ZoneRulesException.class, () -> {
            clockProvider.getClockForUser(user);
        }, "Should throw ZoneRulesException for invalid timezone");
    }

    @Test
    void testGetClockForUser_withNullUser() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            clockProvider.getClockForUser(null);
        }, "Should throw NullPointerException for null user");
    }

    @Test
    void testImplementsClockProviderInterface() {
        // Assert
        assertTrue(clockProvider instanceof ClockProvider, 
            "ClockProviderImpl should implement ClockProvider interface");
    }

    // Helper method to create User with specific timezone
    private User createUserWithTimezone(String timezone) {
        return new User("testuser", "hashedpass", "test@example.com", 
                       "Test", "User", timezone);
    }
}