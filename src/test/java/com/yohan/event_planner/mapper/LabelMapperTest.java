package com.yohan.event_planner.mapper;

import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.LabelColor;
import com.yohan.event_planner.dto.LabelCreateDTO;
import com.yohan.event_planner.dto.LabelResponseDTO;
import com.yohan.event_planner.dto.LabelUpdateDTO;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class LabelMapperTest {

    private LabelMapper mapper;
    private User testUser;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(LabelMapper.class);
        testUser = TestUtils.createValidUserEntity();
    }

    @Nested
    class ToEntityTests {

        @Test
        void shouldMapCreateDTOToEntity() {
            // Arrange
            LabelCreateDTO dto = new LabelCreateDTO("Focus", LabelColor.BLUE);

            // Act
            Label result = mapper.toEntity(dto);

            // Assert
            assertThat(result.getName()).isEqualTo("Focus");
            assertThat(result.getColor()).isEqualTo(LabelColor.BLUE);
            assertThat(result.getCreator()).isNull(); // Creator must be set manually
            assertThat(result.getId()).isNull(); // ID not set for new entities
        }

        @Test
        void shouldMapAllColorValues() {
            // Test each LabelColor enum value
            for (LabelColor color : LabelColor.values()) {
                // Arrange
                String labelName = "Test-" + color.name();
                LabelCreateDTO dto = new LabelCreateDTO(labelName, color);

                // Act
                Label result = mapper.toEntity(dto);

                // Assert
                assertThat(result.getName()).isEqualTo(labelName);
                assertThat(result.getColor()).isEqualTo(color);
            }
        }
    }

    @Nested
    class ToResponseDTOTests {

        @Test
        void shouldMapEntityToResponseDTO() {
            // Arrange
            Label label = new Label("Focus", LabelColor.RED, testUser);
            TestUtils.setLabelId(label, 123L);

            // Act
            LabelResponseDTO result = mapper.toResponseDTO(label);

            // Assert
            assertThat(result.id()).isEqualTo(123L);
            assertThat(result.name()).isEqualTo("Focus");
            assertThat(result.color()).isEqualTo(LabelColor.RED);
            assertThat(result.creatorUsername()).isEqualTo(testUser.getUsername());
        }

        @Test
        void shouldMapAllColorValuesInResponse() {
            // Test each LabelColor enum value in response mapping
            for (LabelColor color : LabelColor.values()) {
                // Arrange
                String labelName = "Test-" + color.name();
                Label label = new Label(labelName, color, testUser);
                TestUtils.setLabelId(label, 1L);

                // Act
                LabelResponseDTO result = mapper.toResponseDTO(label);

                // Assert
                assertThat(result.name()).isEqualTo(labelName);
                assertThat(result.color()).isEqualTo(color);
                assertThat(result.creatorUsername()).isEqualTo(testUser.getUsername());
            }
        }

        @Test
        void shouldHandleNullColorGracefully() {
            // Arrange - test edge case with null color (shouldn't happen in practice)
            Label label = new Label();
            label.setName("Test");
            label.setCreator(testUser);
            label.setColor(null);
            TestUtils.setLabelId(label, 1L);

            // Act
            LabelResponseDTO result = mapper.toResponseDTO(label);

            // Assert
            assertThat(result.name()).isEqualTo("Test");
            assertThat(result.color()).isNull();
            assertThat(result.creatorUsername()).isEqualTo(testUser.getUsername());
        }
    }

    @Nested
    class UpdateEntityFromDtoTests {

        @Test
        void shouldUpdateEntityColorWhenProvided() {
            // Arrange
            Label entity = new Label("Test", LabelColor.RED, testUser);
            LabelUpdateDTO updateDto = new LabelUpdateDTO(null, LabelColor.BLUE);

            // Act
            mapper.updateEntityFromDto(updateDto, entity);

            // Assert
            assertThat(entity.getName()).isEqualTo("Test"); // Name unchanged
            assertThat(entity.getColor()).isEqualTo(LabelColor.BLUE); // Color updated
        }

        @Test
        void shouldUpdateEntityNameWhenProvided() {
            // Arrange
            Label entity = new Label("OldName", LabelColor.RED, testUser);
            LabelUpdateDTO updateDto = new LabelUpdateDTO("NewName", null);

            // Act
            mapper.updateEntityFromDto(updateDto, entity);

            // Assert
            assertThat(entity.getName()).isEqualTo("NewName"); // Name updated
            assertThat(entity.getColor()).isEqualTo(LabelColor.RED); // Color unchanged
        }

        @Test
        void shouldUpdateBothNameAndColor() {
            // Arrange
            Label entity = new Label("OldName", LabelColor.RED, testUser);
            LabelUpdateDTO updateDto = new LabelUpdateDTO("NewName", LabelColor.GREEN);

            // Act
            mapper.updateEntityFromDto(updateDto, entity);

            // Assert
            assertThat(entity.getName()).isEqualTo("NewName");
            assertThat(entity.getColor()).isEqualTo(LabelColor.GREEN);
        }

        @Test
        void shouldIgnoreNullColorInUpdate() {
            // Arrange
            Label entity = new Label("Test", LabelColor.ORANGE, testUser);
            LabelUpdateDTO updateDto = new LabelUpdateDTO("NewName", null);

            // Act
            mapper.updateEntityFromDto(updateDto, entity);

            // Assert
            assertThat(entity.getName()).isEqualTo("NewName");
            assertThat(entity.getColor()).isEqualTo(LabelColor.ORANGE); // Color unchanged
        }

        @Test
        void shouldIgnoreNullNameInUpdate() {
            // Arrange
            Label entity = new Label("OriginalName", LabelColor.ORANGE, testUser);
            LabelUpdateDTO updateDto = new LabelUpdateDTO(null, LabelColor.PURPLE);

            // Act
            mapper.updateEntityFromDto(updateDto, entity);

            // Assert
            assertThat(entity.getName()).isEqualTo("OriginalName"); // Name unchanged
            assertThat(entity.getColor()).isEqualTo(LabelColor.PURPLE); // Color updated
        }

        @Test
        void shouldIgnoreAllNullFieldsInUpdate() {
            // Arrange
            Label entity = new Label("OriginalName", LabelColor.YELLOW, testUser);
            LabelUpdateDTO updateDto = new LabelUpdateDTO(null, null);

            // Act
            mapper.updateEntityFromDto(updateDto, entity);

            // Assert
            assertThat(entity.getName()).isEqualTo("OriginalName"); // Name unchanged
            assertThat(entity.getColor()).isEqualTo(LabelColor.YELLOW); // Color unchanged
        }

        @Test
        void shouldUpdateWithAllColorValues() {
            // Test updating to each LabelColor enum value
            for (LabelColor color : LabelColor.values()) {
                // Arrange
                Label entity = new Label("Test", LabelColor.GRAY, testUser);
                LabelUpdateDTO updateDto = new LabelUpdateDTO(null, color);

                // Act
                mapper.updateEntityFromDto(updateDto, entity);

                // Assert
                assertThat(entity.getColor()).isEqualTo(color);
                assertThat(entity.getName()).isEqualTo("Test"); // Name unchanged
            }
        }
    }
}