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
}