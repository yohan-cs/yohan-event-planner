package com.yohan.event_planner.domain;

import com.yohan.event_planner.domain.enums.RecapMediaType;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class RecapMediaTest {

    private static final String IMAGE_URL = "https://example.com/image.jpg";
    private static final String VIDEO_URL = "https://example.com/video.mp4";
    private static final String AUDIO_URL = "https://example.com/audio.mp3";
    
    private EventRecap recap;
    private User creator;
    private Event event;

    @BeforeEach
    void setUp() {
        creator = TestUtils.createValidUserEntity();
        event = TestUtils.createValidCompletedEvent(creator, Clock.systemUTC());
        recap = TestUtils.createValidEventRecap(event);
    }

    @Nested
    class Construction {

        @Test
        void constructor_shouldSetAllProperties() {
            RecapMedia media = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);

            assertThat(media.getRecap()).isEqualTo(recap);
            assertThat(media.getMediaUrl()).isEqualTo(IMAGE_URL);
            assertThat(media.getMediaType()).isEqualTo(RecapMediaType.IMAGE);
            assertThat(media.getDurationSeconds()).isNull();
            assertThat(media.getMediaOrder()).isZero();
        }

        @Test
        void constructor_withVideoAndDuration_shouldSetDuration() {
            RecapMedia media = new RecapMedia(recap, VIDEO_URL, RecapMediaType.VIDEO, 120, 1);

            assertThat(media.getRecap()).isEqualTo(recap);
            assertThat(media.getMediaUrl()).isEqualTo(VIDEO_URL);
            assertThat(media.getMediaType()).isEqualTo(RecapMediaType.VIDEO);
            assertThat(media.getDurationSeconds()).isEqualTo(120);
            assertThat(media.getMediaOrder()).isEqualTo(1);
        }

        @Test
        void constructor_withAudio_shouldHandleAudioType() {
            RecapMedia media = new RecapMedia(recap, AUDIO_URL, RecapMediaType.AUDIO, 180, 2);

            assertThat(media.getMediaType()).isEqualTo(RecapMediaType.AUDIO);
            assertThat(media.getDurationSeconds()).isEqualTo(180);
            assertThat(media.getMediaOrder()).isEqualTo(2);
        }

        @Test
        void defaultConstructor_shouldCreateEmptyMedia() {
            RecapMedia media = new RecapMedia();

            assertThat(media.getRecap()).isNull();
            assertThat(media.getMediaUrl()).isNull();
            assertThat(media.getMediaType()).isNull();
            assertThat(media.getDurationSeconds()).isNull();
            assertThat(media.getMediaOrder()).isZero();
        }
    }

    @Nested
    class MediaTypes {

        @Test
        void imageMedia_shouldHaveNullDuration() {
            RecapMedia imageMedia = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);

            assertThat(imageMedia.getMediaType()).isEqualTo(RecapMediaType.IMAGE);
            assertThat(imageMedia.getDurationSeconds()).isNull();
        }

        @Test
        void videoMedia_shouldHaveDuration() {
            RecapMedia videoMedia = new RecapMedia(recap, VIDEO_URL, RecapMediaType.VIDEO, 300, 1);

            assertThat(videoMedia.getMediaType()).isEqualTo(RecapMediaType.VIDEO);
            assertThat(videoMedia.getDurationSeconds()).isEqualTo(300);
        }

        @Test
        void audioMedia_shouldHaveDuration() {
            RecapMedia audioMedia = new RecapMedia(recap, AUDIO_URL, RecapMediaType.AUDIO, 240, 2);

            assertThat(audioMedia.getMediaType()).isEqualTo(RecapMediaType.AUDIO);
            assertThat(audioMedia.getDurationSeconds()).isEqualTo(240);
        }

        @Test
        void videoWithoutDuration_shouldAllowNullDuration() {
            RecapMedia videoMedia = new RecapMedia(recap, VIDEO_URL, RecapMediaType.VIDEO, null, 1);

            assertThat(videoMedia.getMediaType()).isEqualTo(RecapMediaType.VIDEO);
            assertThat(videoMedia.getDurationSeconds()).isNull();
        }
    }

    @Nested
    class OrderingLogic {

        @Test
        void mediaOrder_shouldDetermineSequence() {
            RecapMedia first = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);
            RecapMedia second = new RecapMedia(recap, VIDEO_URL, RecapMediaType.VIDEO, 120, 1);
            RecapMedia third = new RecapMedia(recap, AUDIO_URL, RecapMediaType.AUDIO, 180, 2);

            assertThat(first.getMediaOrder()).isLessThan(second.getMediaOrder());
            assertThat(second.getMediaOrder()).isLessThan(third.getMediaOrder());
        }

        @Test
        void setMediaOrder_shouldUpdateOrder() {
            RecapMedia media = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);

            media.setMediaOrder(5);

            assertThat(media.getMediaOrder()).isEqualTo(5);
        }

        @Test
        void mediaOrder_shouldAllowNegativeValues() {
            RecapMedia media = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, -1);

            assertThat(media.getMediaOrder()).isEqualTo(-1);
        }

        @Test
        void mediaOrder_shouldAllowLargeValues() {
            RecapMedia media = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 1000);

            assertThat(media.getMediaOrder()).isEqualTo(1000);
        }
    }

    @Nested
    class PropertyManagement {

        @Test
        void setMediaUrl_shouldUpdateUrl() {
            RecapMedia media = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);
            String newUrl = "https://example.com/new-image.png";

            media.setMediaUrl(newUrl);

            assertThat(media.getMediaUrl()).isEqualTo(newUrl);
        }

        @Test
        void setMediaType_shouldUpdateType() {
            RecapMedia media = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);

            media.setMediaType(RecapMediaType.VIDEO);

            assertThat(media.getMediaType()).isEqualTo(RecapMediaType.VIDEO);
        }

        @Test
        void setDurationSeconds_shouldUpdateDuration() {
            RecapMedia media = new RecapMedia(recap, VIDEO_URL, RecapMediaType.VIDEO, 120, 1);

            media.setDurationSeconds(180);

            assertThat(media.getDurationSeconds()).isEqualTo(180);
        }

        @Test
        void setDurationSeconds_withNull_shouldClearDuration() {
            RecapMedia media = new RecapMedia(recap, VIDEO_URL, RecapMediaType.VIDEO, 120, 1);

            media.setDurationSeconds(null);

            assertThat(media.getDurationSeconds()).isNull();
        }

        @Test
        void getters_shouldReturnCorrectValues() {
            RecapMedia media = new RecapMedia(recap, VIDEO_URL, RecapMediaType.VIDEO, 300, 2);

            assertThat(media.getRecap()).isEqualTo(recap);
            assertThat(media.getMediaUrl()).isEqualTo(VIDEO_URL);
            assertThat(media.getMediaType()).isEqualTo(RecapMediaType.VIDEO);
            assertThat(media.getDurationSeconds()).isEqualTo(300);
            assertThat(media.getMediaOrder()).isEqualTo(2);
        }
    }

    @Nested
    class EqualityAndHashing {

        @Test
        void equals_withSameId_shouldReturnTrue() {
            RecapMedia media1 = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);
            RecapMedia media2 = new RecapMedia(recap, VIDEO_URL, RecapMediaType.VIDEO, 120, 1);
            
            setRecapMediaId(media1, 1L);
            setRecapMediaId(media2, 1L);

            assertThat(media1).isEqualTo(media2);
        }

        @Test
        void equals_withDifferentIds_shouldReturnFalse() {
            RecapMedia media1 = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);
            RecapMedia media2 = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);
            
            setRecapMediaId(media1, 1L);
            setRecapMediaId(media2, 2L);

            assertThat(media1).isNotEqualTo(media2);
        }

        @Test
        void equals_withNullIds_shouldReturnFalse() {
            RecapMedia media1 = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);
            RecapMedia media2 = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);

            // Without IDs, they should not be equal (ID-only equality)
            assertThat(media1).isNotEqualTo(media2);
        }

        @Test
        void equals_withOneNullId_shouldReturnFalse() {
            RecapMedia media1 = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);
            RecapMedia media2 = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);
            
            setRecapMediaId(media1, 1L);
            // media2 has null ID

            assertThat(media1).isNotEqualTo(media2);
        }

        @Test
        void equals_withSelf_shouldReturnTrue() {
            RecapMedia media = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);

            assertThat(media).isEqualTo(media);
        }

        @Test
        void equals_withNull_shouldReturnFalse() {
            RecapMedia media = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);

            assertThat(media).isNotEqualTo(null);
        }

        @Test
        void equals_withDifferentClass_shouldReturnFalse() {
            RecapMedia media = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);

            assertThat(media).isNotEqualTo("not a recap media");
        }

        @Test
        void hashCode_withId_shouldUseIdHashCode() {
            RecapMedia media = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);
            setRecapMediaId(media, 1L);

            assertThat(media.hashCode()).isEqualTo(Long.valueOf(1L).hashCode());
        }

        @Test
        void hashCode_withNullId_shouldReturnZero() {
            RecapMedia media = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);

            assertThat(media.hashCode()).isZero();
        }

        @Test
        void hashCode_shouldBeConsistentWithEquals() {
            RecapMedia media1 = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);
            RecapMedia media2 = new RecapMedia(recap, VIDEO_URL, RecapMediaType.VIDEO, 120, 1);
            
            setRecapMediaId(media1, 1L);
            setRecapMediaId(media2, 1L);

            assertThat(media1.hashCode()).isEqualTo(media2.hashCode());
        }
    }

    @Nested
    class UrlHandling {

        @Test
        void mediaUrl_shouldAcceptHttpsUrls() {
            RecapMedia media = new RecapMedia(recap, "https://secure.example.com/media.jpg", RecapMediaType.IMAGE, null, 0);

            assertThat(media.getMediaUrl()).startsWith("https://");
        }

        @Test
        void mediaUrl_shouldAcceptHttpUrls() {
            RecapMedia media = new RecapMedia(recap, "http://example.com/media.jpg", RecapMediaType.IMAGE, null, 0);

            assertThat(media.getMediaUrl()).startsWith("http://");
        }

        @Test
        void mediaUrl_shouldAcceptCdnUrls() {
            String cdnUrl = "https://cdn.example.com/uploads/2024/media/12345.mp4";
            RecapMedia media = new RecapMedia(recap, cdnUrl, RecapMediaType.VIDEO, 240, 0);

            assertThat(media.getMediaUrl()).isEqualTo(cdnUrl);
        }

        @Test
        void mediaUrl_shouldAcceptQueryParameters() {
            String urlWithParams = "https://example.com/media.jpg?version=2&quality=high";
            RecapMedia media = new RecapMedia(recap, urlWithParams, RecapMediaType.IMAGE, null, 0);

            assertThat(media.getMediaUrl()).contains("?version=2&quality=high");
        }
    }

    @Nested
    class DurationHandling {

        @Test
        void durationSeconds_shouldAcceptZero() {
            RecapMedia media = new RecapMedia(recap, AUDIO_URL, RecapMediaType.AUDIO, 0, 0);

            assertThat(media.getDurationSeconds()).isZero();
        }

        @Test
        void durationSeconds_shouldAcceptLargeValues() {
            int oneHour = 3600;
            RecapMedia media = new RecapMedia(recap, VIDEO_URL, RecapMediaType.VIDEO, oneHour, 0);

            assertThat(media.getDurationSeconds()).isEqualTo(oneHour);
        }

        @Test
        void durationSeconds_shouldAcceptNull() {
            RecapMedia media = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);

            assertThat(media.getDurationSeconds()).isNull();
        }

        @Test
        void setDurationSeconds_shouldUpdateFromNullToValue() {
            RecapMedia media = new RecapMedia(recap, VIDEO_URL, RecapMediaType.VIDEO, null, 0);

            media.setDurationSeconds(150);

            assertThat(media.getDurationSeconds()).isEqualTo(150);
        }

        @Test
        void setDurationSeconds_shouldUpdateFromValueToNull() {
            RecapMedia media = new RecapMedia(recap, VIDEO_URL, RecapMediaType.VIDEO, 150, 0);

            media.setDurationSeconds(null);

            assertThat(media.getDurationSeconds()).isNull();
        }
    }

    @Nested
    class RecapRelationship {

        @Test
        void getRecap_shouldReturnAssociatedRecap() {
            RecapMedia media = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);

            assertThat(media.getRecap()).isEqualTo(recap);
        }

        @Test
        void recap_shouldNotBeChangeable() {
            // RecapMedia doesn't have setRecap method - relationship is immutable
            RecapMedia media = new RecapMedia(recap, IMAGE_URL, RecapMediaType.IMAGE, null, 0);

            assertThat(media.getRecap()).isEqualTo(recap);
            // Can't change recap after creation - this is by design
        }
    }

    // Helper method using reflection
    private void setRecapMediaId(RecapMedia media, Long id) {
        try {
            var field = RecapMedia.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(media, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set recap media ID", e);
        }
    }
}