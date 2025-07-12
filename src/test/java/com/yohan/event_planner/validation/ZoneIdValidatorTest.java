package com.yohan.event_planner.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ZoneIdValidator}.
 * 
 * <p>
 * Tests timezone validation logic including valid IANA timezone IDs,
 * invalid timezone strings, null/blank handling, and edge cases.
 * Verifies that the validator correctly delegates timezone validation
 * to {@link java.time.ZoneId#of(String)}.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ZoneIdValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    private ZoneIdValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ZoneIdValidator();
        validator.initialize(null);
    }

    @Nested
    class ValidTimezoneTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "UTC",
            "GMT",
            "Z",
            "America/New_York",
            "America/Los_Angeles",
            "Europe/London",
            "Europe/Paris",
            "Asia/Tokyo",
            "Asia/Shanghai",
            "Australia/Sydney",
            "Australia/Melbourne",
            "US/Pacific",
            "US/Eastern",
            "Canada/Eastern",
            "Brazil/East",
            "Africa/Cairo",
            "Pacific/Auckland",
            "Indian/Mauritius",
            "Atlantic/Azores",
            "Europe/Berlin",
            "Asia/Kolkata",
            "America/Chicago",
            "America/Denver",
            "GMT+0",
            "GMT-5",
            "GMT+9",
            "Etc/UTC",
            "Etc/GMT+5",
            "Etc/GMT-3"
        })
        void isValid_withValidTimezones_returnsTrue(String timezone) {
            // Act
            boolean result = validator.isValid(timezone, context);

            // Assert
            assertTrue(result, "Expected timezone '" + timezone + "' to be valid");
        }

        @Test
        void isValid_withNullTimezone_returnsTrue() {
            // Act
            boolean result = validator.isValid(null, context);

            // Assert
            assertTrue(result, "Null timezone should be considered valid (handled by @NotBlank)");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n", " \t \n "})
        void isValid_withBlankTimezone_returnsTrue(String timezone) {
            // Act
            boolean result = validator.isValid(timezone, context);

            // Assert
            assertTrue(result, "Blank timezone should be considered valid (handled by @NotBlank)");
        }
    }

    @Nested
    class InvalidTimezoneTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "Invalid/Timezone",
            "NotA/ValidZone",
            "XYZ",
            "123",
            "America/NonExistent",
            "Europe/FakeCity",
            "Asia/NotReal",
            "GMT+25:00",
            "UTC+100",
            "Random String",
            "America/New York",  // Space instead of underscore
            "america/new_york",  // Wrong case
            "AMERICA/NEW_YORK",  // Wrong case
            "US/Random",
            "Canada/NotReal",
            "Europe/",
            "America/",
            "/NewYork",
            "America//NewYork",
            "GMT++5",
            "GMT--3",
            "GMTT+5",
            "Invalid",
            "Test123",
            "SpecialChars!@#",
            "Very/Long/Invalid/Timezone/Name/That/Should/Not/Exist"
        })
        void isValid_withInvalidTimezones_returnsFalse(String timezone) {
            // Act
            boolean result = validator.isValid(timezone, context);

            // Assert
            assertFalse(result, "Expected timezone '" + timezone + "' to be invalid");
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        void isValid_withEmptyString_returnsTrue() {
            // Act
            boolean result = validator.isValid("", context);

            // Assert
            assertTrue(result, "Empty string should be considered valid (handled by @NotBlank)");
        }

        @Test
        void isValid_withWhitespaceOnly_returnsTrue() {
            // Act
            boolean result = validator.isValid("    ", context);

            // Assert
            assertTrue(result, "Whitespace-only string should be considered valid (handled by @NotBlank)");
        }

        @Test
        void isValid_withVeryLongInvalidString_returnsFalse() {
            // Arrange
            String veryLongInvalidTimezone = "A".repeat(1000) + "/InvalidTimezone";

            // Act
            boolean result = validator.isValid(veryLongInvalidTimezone, context);

            // Assert
            assertFalse(result, "Very long invalid timezone should return false");
        }

        @Test
        void isValid_withSpecialCharacters_returnsFalse() {
            // Act
            boolean result = validator.isValid("America/New@York", context);

            // Assert
            assertFalse(result, "Timezone with special characters should be invalid");
        }

        @Test
        void isValid_withUnicodeCharacters_returnsFalse() {
            // Act
            boolean result = validator.isValid("Améric@/Põrto", context);

            // Assert
            assertFalse(result, "Timezone with unicode characters should be invalid");
        }
    }

    @Nested
    class InitializationTests {

        @Test
        void initialize_withNullAnnotation_doesNotThrow() {
            // Act & Assert
            assertDoesNotThrow(() -> validator.initialize(null));
        }

        @Test
        void initialize_canBeCalledMultipleTimes() {
            // Act & Assert
            assertDoesNotThrow(() -> {
                validator.initialize(null);
                validator.initialize(null);
                validator.initialize(null);
            });
        }
    }

    @Nested
    class BehaviorConsistencyTests {

        @Test
        void isValid_multipleCallsWithSameInput_returnsConsistentResults() {
            // Arrange
            String timezone = "America/New_York";

            // Act
            boolean result1 = validator.isValid(timezone, context);
            boolean result2 = validator.isValid(timezone, context);
            boolean result3 = validator.isValid(timezone, context);

            // Assert
            assertTrue(result1);
            assertEquals(result1, result2);
            assertEquals(result2, result3);
        }

        @Test
        void isValid_validAfterInvalid_returnsCorrectResults() {
            // Act & Assert
            assertFalse(validator.isValid("Invalid/Zone", context));
            assertTrue(validator.isValid("UTC", context));
            assertFalse(validator.isValid("Another/Invalid", context));
            assertTrue(validator.isValid("America/New_York", context));
        }
    }
}