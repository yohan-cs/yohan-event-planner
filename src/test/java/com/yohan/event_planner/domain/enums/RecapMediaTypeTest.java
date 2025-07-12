package com.yohan.event_planner.domain.enums;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecapMediaTypeTest {

    @Nested
    class EnumValues {

        @Test
        void allMediaTypes_shouldBePresent() {
            RecapMediaType[] types = RecapMediaType.values();

            assertThat(types).hasSize(3);
            assertThat(types).containsExactly(RecapMediaType.IMAGE, RecapMediaType.VIDEO, RecapMediaType.AUDIO);
        }

        @Test
        void valueOf_shouldReturnCorrectType() {
            assertThat(RecapMediaType.valueOf("IMAGE")).isEqualTo(RecapMediaType.IMAGE);
            assertThat(RecapMediaType.valueOf("VIDEO")).isEqualTo(RecapMediaType.VIDEO);
            assertThat(RecapMediaType.valueOf("AUDIO")).isEqualTo(RecapMediaType.AUDIO);
        }

        @Test
        void name_shouldReturnCorrectName() {
            assertThat(RecapMediaType.IMAGE.name()).isEqualTo("IMAGE");
            assertThat(RecapMediaType.VIDEO.name()).isEqualTo("VIDEO");
            assertThat(RecapMediaType.AUDIO.name()).isEqualTo("AUDIO");
        }
    }

    @Nested
    class BusinessSemantics {

        @Test
        void enumOrdering_shouldBeConsistent() {
            // Verify the order is IMAGE, VIDEO, AUDIO
            assertThat(RecapMediaType.IMAGE.ordinal()).isEqualTo(0);
            assertThat(RecapMediaType.VIDEO.ordinal()).isEqualTo(1);
            assertThat(RecapMediaType.AUDIO.ordinal()).isEqualTo(2);
        }

        @Test
        void mediaTypes_shouldRepresentDistinctCategories() {
            // Each media type should be unique for proper categorization
            assertThat(RecapMediaType.IMAGE).isNotEqualTo(RecapMediaType.VIDEO);
            assertThat(RecapMediaType.VIDEO).isNotEqualTo(RecapMediaType.AUDIO);
            assertThat(RecapMediaType.IMAGE).isNotEqualTo(RecapMediaType.AUDIO);
        }

        @Test
        void mediaTypes_shouldSupportCollectionOperations() {
            java.util.Set<RecapMediaType> mediaSet = java.util.EnumSet.allOf(RecapMediaType.class);
            
            assertThat(mediaSet).hasSize(3);
            assertThat(mediaSet).contains(RecapMediaType.IMAGE, RecapMediaType.VIDEO, RecapMediaType.AUDIO);
        }
    }

    @Nested
    class StringRepresentation {

        @Test
        void toString_shouldReturnEnumName() {
            assertThat(RecapMediaType.IMAGE.toString()).isEqualTo("IMAGE");
            assertThat(RecapMediaType.VIDEO.toString()).isEqualTo("VIDEO");
            assertThat(RecapMediaType.AUDIO.toString()).isEqualTo("AUDIO");
        }

        @Test
        void enumNames_shouldBeSuitableForSerialization() {
            // Names should be simple strings suitable for JSON/database storage
            for (RecapMediaType type : RecapMediaType.values()) {
                String name = type.name();
                
                assertThat(name).matches("^[A-Z]+$"); // Only uppercase letters
                assertThat(name).doesNotContain(" "); // No spaces
                assertThat(name).doesNotContain("_"); // No underscores in these particular enums
                assertThat(name).isNotEmpty();
                assertThat(name).isNotBlank();
            }
        }
    }

    @Nested
    class EnumEquality {

        @Test
        void enumEquality_shouldWorkCorrectly() {
            RecapMediaType image1 = RecapMediaType.IMAGE;
            RecapMediaType image2 = RecapMediaType.valueOf("IMAGE");

            assertThat(image1).isEqualTo(image2);
            assertThat(image1).isSameAs(image2); // Enum instances are singletons
        }

        @Test
        void enumComparison_shouldWorkWithSwitchStatements() {
            // Verify that media types work correctly in switch statements
            for (RecapMediaType type : RecapMediaType.values()) {
                String category = switch (type) {
                    case IMAGE -> "visual";
                    case VIDEO -> "multimedia";
                    case AUDIO -> "sound";
                };
                
                assertThat(category).isNotNull();
                assertThat(category).isIn("visual", "multimedia", "sound");
            }
        }
    }

    @Nested
    class MediaTypeCharacteristics {

        @Test
        void image_shouldRepresentStaticVisualContent() {
            // IMAGE is intended for static visual content
            RecapMediaType image = RecapMediaType.IMAGE;
            
            assertThat(image.name()).isEqualTo("IMAGE");
            assertThat(image.ordinal()).isEqualTo(0); // First in the list
        }

        @Test
        void video_shouldRepresentMultimediaContent() {
            // VIDEO represents content with both visual and audio components
            RecapMediaType video = RecapMediaType.VIDEO;
            
            assertThat(video.name()).isEqualTo("VIDEO");
            assertThat(video.ordinal()).isEqualTo(1); // Second in the list
        }

        @Test
        void audio_shouldRepresentSoundContent() {
            // AUDIO represents audio-only content
            RecapMediaType audio = RecapMediaType.AUDIO;
            
            assertThat(audio.name()).isEqualTo("AUDIO");
            assertThat(audio.ordinal()).isEqualTo(2); // Third in the list
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void valueOf_withInvalidValue_shouldThrowException() {
            assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> RecapMediaType.valueOf("INVALID")
            )).hasMessageContaining("INVALID");
        }

        @Test
        void valueOf_withNullValue_shouldThrowException() {
            assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> RecapMediaType.valueOf(null)
            )).isNotNull();
        }

        @Test
        void valueOf_withLowercaseValue_shouldThrowException() {
            assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> RecapMediaType.valueOf("image")
            )).hasMessageContaining("image");
        }
    }
}