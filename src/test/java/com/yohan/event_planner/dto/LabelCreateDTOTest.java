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

class LabelCreateDTOTest {

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
            LabelCreateDTO dto = new LabelCreateDTO("Work Tasks", LabelColor.BLUE);

            // Act
            Set<ConstraintViolation<LabelCreateDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        void shouldAcceptAllValidColors() {
            // Test each LabelColor enum value
            for (LabelColor color : LabelColor.values()) {
                // Arrange
                String labelName = "Test-" + color.name();
                LabelCreateDTO dto = new LabelCreateDTO(labelName, color);

                // Act
                Set<ConstraintViolation<LabelCreateDTO>> violations = validator.validate(dto);

                // Assert
                assertThat(violations)
                    .withFailMessage("Color %s should be valid", color)
                    .isEmpty();
            }
        }

        @Test
        void shouldAcceptSingleCharacterName() {
            // Arrange
            LabelCreateDTO dto = new LabelCreateDTO("A", LabelColor.RED);

            // Act
            Set<ConstraintViolation<LabelCreateDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        void shouldAcceptLongValidName() {
            // Arrange
            String longName = "A".repeat(100); // Assuming max length validation exists
            LabelCreateDTO dto = new LabelCreateDTO(longName, LabelColor.GREEN);

            // Act
            Set<ConstraintViolation<LabelCreateDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        void shouldAcceptNamesWithSpecialCharacters() {
            // Arrange
            LabelCreateDTO dto = new LabelCreateDTO("Work/Life-Balance (2024)!", LabelColor.PURPLE);

            // Act
            Set<ConstraintViolation<LabelCreateDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        void shouldAcceptUnicodeCharacters() {
            // Arrange
            LabelCreateDTO dto = new LabelCreateDTO("学习中文 🎯", LabelColor.ORANGE);

            // Act
            Set<ConstraintViolation<LabelCreateDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    class InvalidInputTests {

        @Test
        void shouldRejectNullName() {
            // Arrange
            LabelCreateDTO dto = new LabelCreateDTO(null, LabelColor.BLUE);

            // Act
            Set<ConstraintViolation<LabelCreateDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Label name must not be blank");
        }

        @Test
        void shouldRejectBlankName() {
            // Arrange
            LabelCreateDTO dto = new LabelCreateDTO("", LabelColor.BLUE);

            // Act
            Set<ConstraintViolation<LabelCreateDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Label name must not be blank");
        }

        @Test
        void shouldRejectWhitespaceOnlyName() {
            // Arrange
            LabelCreateDTO dto = new LabelCreateDTO("   ", LabelColor.BLUE);

            // Act
            Set<ConstraintViolation<LabelCreateDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Label name must not be blank");
        }

        @Test
        void shouldRejectNullColor() {
            // Arrange
            LabelCreateDTO dto = new LabelCreateDTO("Valid Name", null);

            // Act
            Set<ConstraintViolation<LabelCreateDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Label color cannot be null");
        }

        @Test
        void shouldRejectBothNullNameAndColor() {
            // Arrange
            LabelCreateDTO dto = new LabelCreateDTO(null, null);

            // Act
            Set<ConstraintViolation<LabelCreateDTO>> violations = validator.validate(dto);

            // Assert
            assertThat(violations).hasSize(2);
            // Should have one violation for name and one for color
            assertThat(violations.stream().map(ConstraintViolation::getMessage))
                .containsExactlyInAnyOrder("Label name must not be blank", "Label color cannot be null");
            Set<String> messages = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
            assertThat(messages).containsExactlyInAnyOrder(
                "Label name must not be blank",
                "Label color cannot be null"
            );
        }
    }

    @Nested
    class RecordFunctionalityTests {

        @Test
        void shouldCreateImmutableRecord() {
            // Arrange & Act
            LabelCreateDTO dto = new LabelCreateDTO("Test", LabelColor.BLUE);

            // Assert
            assertThat(dto.name()).isEqualTo("Test");
            assertThat(dto.color()).isEqualTo(LabelColor.BLUE);
        }

        @Test
        void shouldImplementEqualsAndHashCode() {
            // Arrange
            LabelCreateDTO dto1 = new LabelCreateDTO("Test", LabelColor.BLUE);
            LabelCreateDTO dto2 = new LabelCreateDTO("Test", LabelColor.BLUE);
            LabelCreateDTO dto3 = new LabelCreateDTO("Different", LabelColor.RED);

            // Assert
            assertThat(dto1).isEqualTo(dto2);
            assertThat(dto1).isNotEqualTo(dto3);
            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
            assertThat(dto1.hashCode()).isNotEqualTo(dto3.hashCode());
        }

        @Test
        void shouldImplementToString() {
            // Arrange
            LabelCreateDTO dto = new LabelCreateDTO("Test", LabelColor.BLUE);

            // Act
            String toString = dto.toString();

            // Assert
            assertThat(toString).contains("Test");
            assertThat(toString).contains("BLUE");
            assertThat(toString).contains("LabelCreateDTO");
        }
    }
}