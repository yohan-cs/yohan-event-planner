package com.yohan.event_planner.dto;

import com.yohan.event_planner.domain.enums.LabelColor;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LabelUpdateDTOTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    class ValidInputTests {

        @Test
        void shouldAcceptValidNameAndColor() {
            // Arrange
            LabelUpdateDTO dto = new LabelUpdateDTO("Updated Name", LabelColor.BLUE);

            // Act
            Set<ConstraintViolation<LabelUpdateDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        void shouldAcceptNameOnly() {
            // Arrange - Color is optional in updates
            LabelUpdateDTO dto = new LabelUpdateDTO("Updated Name", null);

            // Act
            Set<ConstraintViolation<LabelUpdateDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        void shouldAcceptColorOnly() {
            // Arrange - Name is optional in updates
            LabelUpdateDTO dto = new LabelUpdateDTO(null, LabelColor.RED);

            // Act
            Set<ConstraintViolation<LabelUpdateDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        void shouldAcceptAllNullFields() {
            // Arrange - Both fields optional for PATCH operations
            LabelUpdateDTO dto = new LabelUpdateDTO(null, null);

            // Act
            Set<ConstraintViolation<LabelUpdateDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        void shouldAcceptAllValidColors() {
            // Test each LabelColor enum value
            for (LabelColor color : LabelColor.values()) {
                // Arrange
                LabelUpdateDTO dto = new LabelUpdateDTO(null, color);

                // Act
                Set<ConstraintViolation<LabelUpdateDTO>> violations = validator.validate(dto);

                // Assert
                assertThat(violations)
                    .withFailMessage("Color %s should be valid", color)
                    .isEmpty();
            }
        }

        @Test
        void shouldAcceptValidNameLengths() {
            // Test various valid name lengths
            String[] validNames = {
                "A",                    // 1 character
                "Valid Name",           // Normal length
                "A".repeat(100)         // Max length (assuming 100 char limit)
            };

            for (String name : validNames) {
                // Arrange
                LabelUpdateDTO dto = new LabelUpdateDTO(name, LabelColor.GREEN);

                // Act
                Set<ConstraintViolation<LabelUpdateDTO>> violations = validator.validate(dto);

                // Assert
                assertThat(violations)
                    .withFailMessage("Name '%s' should be valid", name)
                    .isEmpty();
            }
        }

        @Test
        void shouldAcceptSpecialCharacters() {
            // Arrange
            LabelUpdateDTO dto = new LabelUpdateDTO("Work/Life-Balance (2024)!", LabelColor.PURPLE);

            // Act
            Set<ConstraintViolation<LabelUpdateDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        void shouldAcceptUnicodeCharacters() {
            // Arrange
            LabelUpdateDTO dto = new LabelUpdateDTO("学习中文 🎯", LabelColor.ORANGE);

            // Act
            Set<ConstraintViolation<LabelUpdateDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    class InvalidInputTests {

        @Test
        void shouldRejectBlankNameWhenProvided() {
            // Arrange - Empty string when name is provided
            LabelUpdateDTO dto = new LabelUpdateDTO("", LabelColor.BLUE);

            // Act
            Set<ConstraintViolation<LabelUpdateDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Label name must be between 1 and 100 characters");
        }

        @Test
        void shouldAllowWhitespaceOnlyNameAtDtoLevel() {
            // Arrange - Whitespace-only string (validated at service layer, not DTO)
            LabelUpdateDTO dto = new LabelUpdateDTO("   ", LabelColor.BLUE);

            // Act
            Set<ConstraintViolation<LabelUpdateDTO>> violations = validator.validate(dto);

            // Assert - DTO validation passes, service layer handles whitespace trimming
            assertThat(violations).isEmpty();
        }

        @Test
        void shouldRejectTooLongName() {
            // Arrange - Name exceeding max length (assuming 100 char limit)
            String tooLongName = "A".repeat(101);
            LabelUpdateDTO dto = new LabelUpdateDTO(tooLongName, LabelColor.GREEN);

            // Act
            Set<ConstraintViolation<LabelUpdateDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Label name must be between 1 and 100 characters");
        }

        @Test
        void shouldRejectNameThatIsTooShort() {
            // This test verifies the min=1 constraint when name is provided
            // Note: Empty string is already caught by @NotBlank, but this tests the @Size constraint
            LabelUpdateDTO dto = new LabelUpdateDTO("", LabelColor.BLUE);

            // Act
            Set<ConstraintViolation<LabelUpdateDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
            // Should have at least the @NotBlank violation, possibly also @Size violation
        }
    }

    @Nested
    class PatchSemanticTests {

        @Test
        void shouldDistinguishBetweenNullAndEmptyForName() {
            // Arrange - null means "don't update", empty/blank means "invalid value"
            LabelUpdateDTO nullNameDto = new LabelUpdateDTO(null, LabelColor.BLUE);
            LabelUpdateDTO emptyNameDto = new LabelUpdateDTO("", LabelColor.BLUE);

            // Act
            Set<ConstraintViolation<LabelUpdateDTO>> nullViolations = validator.validate(nullNameDto);
            Set<ConstraintViolation<LabelUpdateDTO>> emptyViolations = validator.validate(emptyNameDto);

            // Assert
            assertThat(nullViolations).isEmpty(); // null is allowed (means don't update)
            assertThat(emptyViolations).isNotEmpty(); // empty is invalid when provided
        }

        @Test
        void shouldAllowPartialUpdates() {
            // Test various partial update scenarios
            LabelUpdateDTO[] partialUpdates = {
                new LabelUpdateDTO("New Name", null),        // Name only
                new LabelUpdateDTO(null, LabelColor.RED),    // Color only
                new LabelUpdateDTO(null, null),              // No updates (valid for PATCH)
                new LabelUpdateDTO("New Name", LabelColor.BLUE) // Both fields
            };

            for (LabelUpdateDTO dto : partialUpdates) {
                // Act
                Set<ConstraintViolation<LabelUpdateDTO>> violations = validator.validate(dto);

                // Assert
                assertThat(violations)
                    .withFailMessage("Partial update should be valid: name=%s, color=%s", 
                        dto.name(), dto.color())
                    .isEmpty();
            }
        }
    }

    @Nested
    class RecordFunctionalityTests {

        @Test
        void shouldCreateImmutableRecord() {
            // Arrange & Act
            LabelUpdateDTO dto = new LabelUpdateDTO("Test", LabelColor.BLUE);

            // Assert
            assertThat(dto.name()).isEqualTo("Test");
            assertThat(dto.color()).isEqualTo(LabelColor.BLUE);
        }

        @Test
        void shouldImplementEqualsAndHashCode() {
            // Arrange
            LabelUpdateDTO dto1 = new LabelUpdateDTO("Test", LabelColor.BLUE);
            LabelUpdateDTO dto2 = new LabelUpdateDTO("Test", LabelColor.BLUE);
            LabelUpdateDTO dto3 = new LabelUpdateDTO("Different", LabelColor.RED);
            LabelUpdateDTO dto4 = new LabelUpdateDTO(null, null);
            LabelUpdateDTO dto5 = new LabelUpdateDTO(null, null);

            // Assert
            assertThat(dto1).isEqualTo(dto2);
            assertThat(dto1).isNotEqualTo(dto3);
            assertThat(dto4).isEqualTo(dto5); // Both null fields
            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
            assertThat(dto4.hashCode()).isEqualTo(dto5.hashCode());
        }

        @Test
        void shouldImplementToString() {
            // Arrange
            LabelUpdateDTO dto1 = new LabelUpdateDTO("Test", LabelColor.BLUE);
            LabelUpdateDTO dto2 = new LabelUpdateDTO(null, null);

            // Act
            String toString1 = dto1.toString();
            String toString2 = dto2.toString();

            // Assert
            assertThat(toString1).contains("Test");
            assertThat(toString1).contains("BLUE");
            assertThat(toString1).contains("LabelUpdateDTO");
            
            assertThat(toString2).contains("LabelUpdateDTO");
            assertThat(toString2).contains("null"); // Should show null values
        }
    }
}