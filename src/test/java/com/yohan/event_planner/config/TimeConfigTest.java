package com.yohan.event_planner.config;

import com.yohan.event_planner.time.ClockProvider;
import com.yohan.event_planner.time.ClockProviderImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeConfigTest {

    private TimeConfig timeConfig;

    @BeforeEach
    void setUp() {
        timeConfig = new TimeConfig();
    }

    @Nested
    class ClockBeanTests {

        @Test
        void testClock_returnsSystemUTCClock() {
            // Act
            Clock clock = timeConfig.clock();

            // Assert
            assertNotNull(clock, "Clock bean should not be null");
            assertEquals(ZoneOffset.UTC, clock.getZone(), "Clock should be configured for UTC timezone");
        }

        @Test
        void testClock_providesCurrentTime() {
            // Arrange
            Instant beforeTest = Instant.now();

            // Act
            Clock clock = timeConfig.clock();
            Instant clockTime = clock.instant();

            // Assert
            Instant afterTest = Instant.now();
            
            assertTrue(clockTime.isAfter(beforeTest.minusSeconds(1)), 
                "Clock time should be approximately current time (after beforeTest-1s)");
            assertTrue(clockTime.isBefore(afterTest.plusSeconds(1)), 
                "Clock time should be approximately current time (before afterTest+1s)");
        }

        @Test
        void testClock_isSystemClock() {
            // Act
            Clock clock = timeConfig.clock();

            // Assert
            // Verify it's actually a system clock by checking type and behavior
            assertTrue(clock.toString().contains("SystemClock"), 
                "Should be a system clock implementation");
        }

        @Test
        void testClock_multipleCallsReturnSameSystemClock() {
            // Act
            Clock clock1 = timeConfig.clock();
            Clock clock2 = timeConfig.clock();

            // Assert
            // Clock.systemUTC() returns the same singleton instance
            assertSame(clock1, clock2, 
                "Multiple calls should return same system Clock instance (singleton behavior)");
            assertEquals(clock1.getZone(), clock2.getZone(), 
                "All Clock instances should have same UTC zone");
        }
    }

    @Nested
    class ClockProviderBeanTests {

        @Test
        void testClockProvider_returnsClockProviderImpl() {
            // Arrange
            Clock mockClock = Clock.fixed(Instant.parse("2025-07-10T15:30:00Z"), ZoneOffset.UTC);

            // Act
            ClockProvider clockProvider = timeConfig.clockProvider(mockClock);

            // Assert
            assertNotNull(clockProvider, "ClockProvider bean should not be null");
            assertInstanceOf(ClockProviderImpl.class, clockProvider, 
                "Should return ClockProviderImpl instance");
        }

        @Test
        void testClockProvider_wrapsProvidedClock() {
            // Arrange
            Instant fixedTime = Instant.parse("2025-07-10T15:30:00Z");
            Clock fixedClock = Clock.fixed(fixedTime, ZoneOffset.UTC);

            // Act
            ClockProvider clockProvider = timeConfig.clockProvider(fixedClock);
            Clock utcClock = clockProvider.getClockForZone(ZoneOffset.UTC);

            // Assert
            assertEquals(fixedTime, utcClock.instant(), 
                "ClockProvider should wrap the provided Clock correctly");
        }

        @Test
        void testClockProvider_withNullClock_handlesGracefully() {
            // Act & Assert
            assertDoesNotThrow(() -> {
                ClockProvider clockProvider = timeConfig.clockProvider(null);
                assertNotNull(clockProvider, "ClockProvider should be created even with null Clock");
            });
        }
    }

    @Nested
    class IntegrationTests {

        @Test
        void testClockAndClockProvider_integration() {
            // Arrange
            Clock systemClock = timeConfig.clock();

            // Act
            ClockProvider clockProvider = timeConfig.clockProvider(systemClock);

            // Assert
            assertNotNull(clockProvider, "ClockProvider should be created successfully");
            
            // Test that ClockProvider uses the system clock
            Clock utcClock = clockProvider.getClockForZone(ZoneOffset.UTC);
            Instant now1 = systemClock.instant();
            Instant now2 = utcClock.instant();
            
            // Should be very close in time (within 1 second)
            long timeDifferenceMs = Math.abs(now1.toEpochMilli() - now2.toEpochMilli());
            assertTrue(timeDifferenceMs < 1000, 
                "System clock and ClockProvider should provide similar times");
        }

        @Test
        void testConfiguration_clockProviderBeansAreIndependent() {
            // Act
            Clock systemClock = timeConfig.clock();
            ClockProvider provider1 = timeConfig.clockProvider(systemClock);
            ClockProvider provider2 = timeConfig.clockProvider(systemClock);

            // Assert
            // Clock.systemUTC() returns same instance, but ClockProvider instances are independent
            assertNotSame(provider1, provider2, "ClockProvider beans should be independent instances");
            
            // Both providers should work with the same system clock
            Clock clock1 = provider1.getClockForZone(ZoneOffset.UTC);
            Clock clock2 = provider2.getClockForZone(ZoneOffset.UTC);
            assertEquals(clock1.getZone(), clock2.getZone(), "Both providers should work correctly");
        }
    }

    @Nested
    class ConfigurationAnnotationTests {

        @Test
        void testClass_hasConfigurationAnnotation() {
            // Assert
            assertTrue(TimeConfig.class.isAnnotationPresent(
                org.springframework.context.annotation.Configuration.class),
                "TimeConfig should have @Configuration annotation");
        }

        @Test
        void testClockMethod_hasBeanAnnotation() throws NoSuchMethodException {
            // Act
            var clockMethod = TimeConfig.class.getMethod("clock");

            // Assert
            assertTrue(clockMethod.isAnnotationPresent(
                org.springframework.context.annotation.Bean.class),
                "clock() method should have @Bean annotation");
        }

        @Test
        void testClockProviderMethod_hasBeanAnnotation() throws NoSuchMethodException {
            // Act
            var clockProviderMethod = TimeConfig.class.getMethod("clockProvider", Clock.class);

            // Assert
            assertTrue(clockProviderMethod.isAnnotationPresent(
                org.springframework.context.annotation.Bean.class),
                "clockProvider() method should have @Bean annotation");
        }
    }

    @Nested
    class DocumentationValidationTests {

        @Test
        void testClockConfiguration_matchesDocumentation() {
            // Act
            Clock clock = timeConfig.clock();

            // Assert - Verify documented behavior
            assertEquals(ZoneOffset.UTC, clock.getZone(), 
                "Clock should be UTC as documented");
            assertNotNull(clock.instant(), 
                "Clock should provide current time as documented");
        }

        @Test
        void testClockProvider_supportsTestingAsDocumented() {
            // Arrange - Create a fixed clock for testing as mentioned in documentation
            Instant fixedTime = Instant.parse("2025-01-01T00:00:00Z");
            Clock fixedClock = Clock.fixed(fixedTime, ZoneOffset.UTC);

            // Act
            ClockProvider clockProvider = timeConfig.clockProvider(fixedClock);

            // Assert - Verify testing support as documented
            Clock testClock = clockProvider.getClockForZone(ZoneOffset.UTC);
            assertEquals(fixedTime, testClock.instant(), 
                "ClockProvider should support fixed time for testing as documented");
        }

        @Test
        void testTimeStrategy_followsUTCCentricApproach() {
            // Act
            Clock clock = timeConfig.clock();

            // Assert - Verify UTC-centric strategy as documented
            assertEquals(ZoneOffset.UTC, clock.getZone(), 
                "Should follow UTC-centric approach as documented");
            
            // Verify time precision is sufficient for event scheduling
            Instant time1 = clock.instant();
            Instant time2 = clock.instant();
            assertTrue(time2.isAfter(time1) || time2.equals(time1), 
                "Clock should provide sufficient precision for event scheduling");
        }
    }
}