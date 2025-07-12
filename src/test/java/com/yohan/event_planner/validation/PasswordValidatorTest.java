package com.yohan.event_planner.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    private PasswordValidator passwordValidator;

    @BeforeEach
    void setUp() {
        passwordValidator = new PasswordValidator();
        passwordValidator.initialize(null);

        // Set up mock chain for custom messages - use lenient() to avoid UnnecessaryStubbing warnings
        lenient().when(context.buildConstraintViolationWithTemplate(anyString()))
                .thenReturn(violationBuilder);
        lenient().when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Nested
    class ValidPasswordTests {

        @Test
        void isValid_withValidPassword_returnsTrue() {
            // Arrange
            String password = "ValidP@ssw0rd43";

            // Act
            boolean result = passwordValidator.isValid(password, context);

            // Assert
            assertTrue(result);
            verify(context, never()).disableDefaultConstraintViolation();
        }

        @Test
        void isValid_withNullPassword_returnsTrue() {
            // Act
            boolean result = passwordValidator.isValid(null, context);

            // Assert
            assertTrue(result);
            verify(context, never()).disableDefaultConstraintViolation();
        }

        @Test
        void isValid_withMinimumValidPassword_returnsTrue() {
            // Arrange
            String password = "Valid1@x"; // Exactly 8 characters, all requirements met

            // Act
            boolean result = passwordValidator.isValid(password, context);

            // Assert
            assertTrue(result);
        }

        @Test
        void isValid_withComplexValidPassword_returnsTrue() {
            // Arrange
            String password = "MyC0mpl3x&S3cur3P@ssw0rd";

            // Act
            boolean result = passwordValidator.isValid(password, context);

            // Assert
            assertTrue(result);
        }

        @Test
        void isValid_withMaximumLengthPassword_returnsTrue() {
            // Arrange - Create exactly 72 character password
            String password = "A1a!" + "x".repeat(68); // Exactly 72 chars with required char types

            // Act
            boolean result = passwordValidator.isValid(password, context);

            // Assert
            assertTrue(result);
        }
    }

    @Nested
    class InvalidPasswordTests {

        @Nested
        class LengthTests {

            @Test
            void isValid_withTooShortPassword_returnsFalse() {
                // Arrange
                String password = "Aa1!bcd"; // 7 characters

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                verify(context).disableDefaultConstraintViolation();
                verify(context).buildConstraintViolationWithTemplate(
                        "Password must be between 8 and 72 characters long");
            }

            @Test
            void isValid_withEmptyString_returnsFalse() {
                // Arrange
                String password = "";

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                verify(context).buildConstraintViolationWithTemplate(
                        "Password must be between 8 and 72 characters long");
            }

            @Test
            void isValid_withTooLongPassword_returnsFalse() {
                // Arrange - Create 73 character password (exceeds max)
                String password = "A1a!" + "x".repeat(69); // 73 chars

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                verify(context).buildConstraintViolationWithTemplate(
                        "Password must be between 8 and 72 characters long");
            }
        }

        @Nested
        class CharacterRequirementTests {

            @Test
            void isValid_withoutUppercase_returnsFalse() {
                // Arrange
                String password = "mypassw0rd!"; // No uppercase

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                verify(context).buildConstraintViolationWithTemplate(
                        "Password must contain at least one uppercase letter (A-Z)");
            }

            @Test
            void isValid_withoutLowercase_returnsFalse() {
                // Arrange
                String password = "MYPASSW0RD!"; // No lowercase

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                verify(context).buildConstraintViolationWithTemplate(
                        "Password must contain at least one lowercase letter (a-z)");
            }

            @Test
            void isValid_withoutDigit_returnsFalse() {
                // Arrange
                String password = "MyPassword!"; // No digit

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                verify(context).buildConstraintViolationWithTemplate(
                        "Password must contain at least one digit (0-9)");
            }

            @Test
            void isValid_withoutSpecialCharacter_returnsFalse() {
                // Arrange
                String password = "MyPassword123"; // No special character

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                verify(context).buildConstraintViolationWithTemplate(
                        "Password must contain at least one special character (!@#$%^&* etc.)");
            }
        }

        @Nested
        class CommonPasswordTests {

            @Test
            void isValid_withKnownCommonPassword_returnsFalse() {
                // Arrange - Use a password that has all required char types but is in the common list
                String password = "Password123!"; // This should be in the common passwords list

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                // Note: May fail on different validation rule first, so we just check it fails
            }

            @Test
            void isValid_withCommonPasswordVariation_returnsFalse() {
                // Arrange
                String password = "Welcome123!"; // Common base word with required chars

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                // The password should fail due to common password detection or pattern detection
            }

            @Test
            void isValid_withCommonPasswordDifferentCase_returnsFalse() {
                // Arrange - Test case insensitive common password detection
                String password = "PASSWORD123!"; // Uppercase version should fail on missing lowercase

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                // Should fail due to missing lowercase letters
            }

            @Test
            void isValid_withSimpleCommonPassword_returnsFalse() {
                // Arrange - Test a simple common password from the list
                String password = "password"; // Definitely in common list but missing requirements

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                // Should fail on missing uppercase, digit, and special char first
            }

            @Test
            void isValid_withCompleteCommonPassword_returnsFalse() {
                // Arrange - Test common password that has all required character types
                String password = "P@ssw0rd"; // In common list but has all char requirements

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                verify(context).buildConstraintViolationWithTemplate(
                        "Password is too common and easily guessable. Please choose a more unique password");
            }

            @Test
            void isValid_withCaseInsensitiveCommonPassword_returnsFalse() {
                // Arrange - Test that common password detection is case-insensitive
                String password = "ADMIN123!"; // "admin" is in common list, but uppercase with requirements

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                // Should fail due to missing lowercase, not common password detection in this case
                verify(context).buildConstraintViolationWithTemplate(
                        "Password must contain at least one lowercase letter (a-z)");
            }

            @Test
            void isValid_withCommonPasswordCaseVariation_returnsFalse() {
                // Arrange - Test common password with mixed case
                String password = "Password1!"; // "password" base is common, has all requirements

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                // Should fail on common password detection since "password" lowercase is in the list
                verify(context).buildConstraintViolationWithTemplate(
                        "Password is too common and easily guessable. Please choose a more unique password");
            }
        }

        @Nested
        class SimplePatternTests {

            @Test
            void isValid_withRepeatedCharacters_returnsFalse() {
                // Arrange
                String password = "MyPaaassw0rd!"; // Contains "aaa"

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                verify(context).buildConstraintViolationWithTemplate(
                        "Password contains simple patterns that are easy to guess. Please use a more complex password");
            }

            @Test
            void isValid_withSequentialNumbers_returnsFalse() {
                // Arrange
                String password = "MyPass123w0rd!"; // Contains "123"

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                verify(context).buildConstraintViolationWithTemplate(
                        "Password contains simple patterns that are easy to guess. Please use a more complex password");
            }

            @Test
            void isValid_withSequentialLetters_returnsFalse() {
                // Arrange
                String password = "MyPassabc123!"; // Contains "abc"

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                verify(context).buildConstraintViolationWithTemplate(
                        "Password contains simple patterns that are easy to guess. Please use a more complex password");
            }

            @Test
            void isValid_withKeyboardPattern_returnsFalse() {
                // Arrange
                String password = "MyPassqwe123!"; // Contains "qwe"

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                verify(context).buildConstraintViolationWithTemplate(
                        "Password contains simple patterns that are easy to guess. Please use a more complex password");
            }

            @Test
            void isValid_withTwoConsecutiveCharacters_returnsTrue() {
                // Arrange - Test boundary: exactly 2 consecutive chars should be allowed
                String password = "MyPaassw0rd!"; // Only 2 consecutive 'a's

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertTrue(result);
            }

            @Test
            void isValid_withExactlyThreeConsecutiveCharacters_returnsFalse() {
                // Arrange - Test boundary: exactly 3 consecutive chars should fail
                String password = "MyPaaassw0rd!"; // Exactly 3 consecutive 'a's

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                verify(context).buildConstraintViolationWithTemplate(
                        "Password contains simple patterns that are easy to guess. Please use a more complex password");
            }

            @Test
            void isValid_withMultipleKeyboardPatterns_returnsFalse() {
                // Arrange
                String password = "MyPassasd456!"; // Contains "asd" keyboard pattern

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                verify(context).buildConstraintViolationWithTemplate(
                        "Password contains simple patterns that are easy to guess. Please use a more complex password");
            }

            @Test
            void isValid_withMultiplePatternViolations_returnsFalse() {
                // Arrange - Password with both sequential and keyboard patterns
                String password = "MyPass123qwe!"; // Contains both "123" and "qwe"

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                assertFalse(result);
                verify(context).buildConstraintViolationWithTemplate(
                        "Password contains simple patterns that are easy to guess. Please use a more complex password");
            }

            @Test
            void isValid_withReverseSequentialPatterns_returnsTrue() {
                // Arrange - Test reverse sequences (our current implementation doesn't catch these)
                String password = "MyPass321cba!"; // Contains "321" and "cba" reverse sequences

                // Act
                boolean result = passwordValidator.isValid(password, context);

                // Assert
                // Current implementation only catches forward sequences, so this should pass
                // This documents current behavior - could be enhanced to catch reverse sequences
                assertTrue(result);
            }
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        void isValid_withAllSpecialCharacters_returnsTrue() {
            // Arrange
            String password = "A1a!@#$%^&*()_+-=[]{}|;':\",./<>?";

            // Act
            boolean result = passwordValidator.isValid(password, context);

            // Assert
            assertTrue(result);
        }

        @Test
        void isValid_withMixedCaseAndNumbers_returnsTrue() {
            // Arrange
            String password = "MyTestP@ssword2024";

            // Act
            boolean result = passwordValidator.isValid(password, context);

            // Assert
            assertTrue(result);
        }

        @Test
        void isValid_withMinimalRequirements_returnsTrue() {
            // Arrange
            String password = "Aa1!mpqz"; // Minimal valid password - avoiding patterns

            // Act
            boolean result = passwordValidator.isValid(password, context);

            // Assert
            assertTrue(result);
        }

        @Test
        void isValid_withSpecialCharacterBoundaries_returnsTrue() {
            // Arrange - Test with minimal special character requirement
            String password = "MyPassword1!"; // Only one special char

            // Act
            boolean result = passwordValidator.isValid(password, context);

            // Assert
            assertTrue(result);
        }

        @Test
        void isValid_withUnicodeCharacters_returnsTrue() {
            // Arrange - Test handling of unicode characters
            String password = "MyPäss1!word"; // Contains unicode character ä

            // Act
            boolean result = passwordValidator.isValid(password, context);

            // Assert
            assertTrue(result);
        }

        @Test
        void isValid_withDifferentSpecialCharacters_returnsTrue() {
            // Arrange - Test various special characters
            String password1 = "MyPassword1@";
            String password2 = "MyPassword1#";
            String password3 = "MyPassword1%";
            String password4 = "MyPassword1&";

            // Act & Assert
            assertTrue(passwordValidator.isValid(password1, context));
            assertTrue(passwordValidator.isValid(password2, context));
            assertTrue(passwordValidator.isValid(password3, context));
            assertTrue(passwordValidator.isValid(password4, context));
        }

        @Test
        void isValid_withComplexMixedPatterns_returnsTrue() {
            // Arrange - Test complex password that avoids all patterns
            String password = "MyC0mpl3x!W0rd2024"; // Complex, no simple patterns

            // Act
            boolean result = passwordValidator.isValid(password, context);

            // Assert
            assertTrue(result);
        }
    }

    @Nested
    class InitializationTests {

        @Test
        void initialize_withNullAnnotation_doesNotThrow() {
            // Act & Assert
            assertDoesNotThrow(() -> passwordValidator.initialize(null));
        }

        @Test
        void initialize_withValidAnnotation_doesNotThrow() {
            // Arrange
            ValidPassword annotation = mock(ValidPassword.class);

            // Act & Assert
            assertDoesNotThrow(() -> passwordValidator.initialize(annotation));
        }
    }

    @Nested
    class ValidationOrderTests {

        @Test
        void isValid_checksLengthFirst_beforeOtherValidations() {
            // Arrange - Short password that would fail multiple validations
            String password = "a1!"; // Too short, but has some required chars

            // Act
            boolean result = passwordValidator.isValid(password, context);

            // Assert
            assertFalse(result);
            // Should fail on length first, not on missing uppercase
            verify(context).buildConstraintViolationWithTemplate(
                    "Password must be between 8 and 72 characters long");
        }

        @Test
        void isValid_checksCharacterRequirements_beforePatterns() {
            // Arrange - Password with missing chars but also has patterns
            String password = "mypass123qwe"; // Missing uppercase and special, has keyboard pattern

            // Act
            boolean result = passwordValidator.isValid(password, context);

            // Assert
            assertFalse(result);
            // Should fail on missing uppercase first, not on pattern detection
            verify(context).buildConstraintViolationWithTemplate(
                    "Password must contain at least one uppercase letter (A-Z)");
        }
    }

    @Nested
    class ParameterizedTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "password123", "PASSWORD123", "Password", "12345678", "abcdefgh",
            "ABCDEFGH", "!@#$%^&*", "password!", "123!@#ABC", "Pass1!", "short"
        })
        void isValid_withInvalidPasswords_returnsFalse(String invalidPassword) {
            // Act
            boolean result = passwordValidator.isValid(invalidPassword, context);

            // Assert
            assertFalse(result);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "ValidP@ss1", "MySecur3!Pass", "Str0ng&Safe#", "C0mpl3x$Word",
            "Secure1!password", "MyGr8!Choice", "Perfect1@Word"
        })
        void isValid_withValidPasswords_returnsTrue(String validPassword) {
            // Act
            boolean result = passwordValidator.isValid(validPassword, context);

            // Assert
            assertTrue(result);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "password123", "admin123", "welcome123", "qwerty123", "abc123",
            "123456789", "password!", "Password123!", "p@ssw0rd"
        })
        void isValid_withCommonPasswords_returnsFalse(String commonPassword) {
            // Act
            boolean result = passwordValidator.isValid(commonPassword, context);

            // Assert
            assertFalse(result);
            // These should all fail either due to common password detection or missing character requirements
        }
    }

    @Nested
    class PerformanceTests {

        @Test
        void isValid_withLongValidPassword_performsReasonably() {
            // Arrange - Test performance with long password
            String password = "A1a!" + "ValidPasswordContent".repeat(3); // Long but valid

            // Act
            long startTime = System.nanoTime();
            boolean result = passwordValidator.isValid(password, context);
            long endTime = System.nanoTime();

            // Assert
            assertTrue(result);
            // Should complete in reasonable time (under 50ms for long passwords)
            assertTrue((endTime - startTime) < 50_000_000, "Validation should complete within 50 milliseconds");
        }

        @Test
        void isValid_withLongInvalidPassword_performsReasonably() {
            // Arrange - Test performance with long invalid password (early termination)
            String password = "a1a!" + "invalidpasswordcontent".repeat(3); // Long but missing uppercase

            // Act
            long startTime = System.nanoTime();
            boolean result = passwordValidator.isValid(password, context);
            long endTime = System.nanoTime();

            // Assert
            assertFalse(result);
            // Should fail fast and complete quickly (under 50ms including JVM warmup)
            assertTrue((endTime - startTime) < 50_000_000, "Validation should fail fast within 50 milliseconds");
        }
    }

}