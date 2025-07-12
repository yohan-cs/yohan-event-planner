package com.yohan.event_planner.domain;

import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventRecapTest {

    private User creator;
    private User otherUser;
    private Event event;
    private Clock clock;

    @BeforeEach
    void setUp() {
        creator = TestUtils.createValidUserEntity();
        otherUser = TestUtils.createTestUser("otheruser");
        clock = Clock.systemUTC();
        event = TestUtils.createValidCompletedEvent(creator, clock);
    }

    @Nested
    class FactoryMethods {

        @Test
        void createConfirmedRecap_shouldCreateConfirmedRecap() {
            EventRecap recap = EventRecap.createConfirmedRecap(
                event, creator, "Great event!", "Event Recap"
            );

            assertThat(recap.getEvent()).isEqualTo(event);
            assertThat(recap.getCreator()).isEqualTo(creator);
            assertThat(recap.getNotes()).isEqualTo("Great event!");
            assertThat(recap.getRecapName()).isEqualTo("Event Recap");
            assertThat(recap.isUnconfirmed()).isFalse();
        }

        @Test
        void createUnconfirmedRecap_shouldCreateUnconfirmedRecap() {
            EventRecap recap = EventRecap.createUnconfirmedRecap(
                event, creator, "Draft notes", "Draft Recap"
            );

            assertThat(recap.getEvent()).isEqualTo(event);
            assertThat(recap.getCreator()).isEqualTo(creator);
            assertThat(recap.getNotes()).isEqualTo("Draft notes");
            assertThat(recap.getRecapName()).isEqualTo("Draft Recap");
            assertThat(recap.isUnconfirmed()).isTrue();
        }

        @Test
        void createConfirmedRecap_withNullFields_shouldAllowNulls() {
            EventRecap recap = EventRecap.createConfirmedRecap(
                event, creator, null, null
            );

            assertThat(recap.getNotes()).isNull();
            assertThat(recap.getRecapName()).isNull();
            assertThat(recap.isUnconfirmed()).isFalse();
        }

        @Test
        void createUnconfirmedRecap_withNullFields_shouldAllowNulls() {
            EventRecap recap = EventRecap.createUnconfirmedRecap(
                event, creator, null, null
            );

            assertThat(recap.getNotes()).isNull();
            assertThat(recap.getRecapName()).isNull();
            assertThat(recap.isUnconfirmed()).isTrue();
        }
    }

    @Nested
    class OwnershipValidation {

        @Test
        void createConfirmedRecap_withMatchingCreator_shouldSucceed() {
            EventRecap recap = EventRecap.createConfirmedRecap(
                event, creator, "Valid recap", "Valid"
            );

            assertThat(recap.getCreator()).isEqualTo(creator);
            assertThat(recap.getEvent().getCreator()).isEqualTo(creator);
        }

        @Test
        void createConfirmedRecap_withMismatchedCreator_shouldThrowException() {
            assertThatThrownBy(() -> EventRecap.createConfirmedRecap(
                event, otherUser, "Invalid recap", "Invalid"
            ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Recap creator must match event creator.");
        }

        @Test
        void createUnconfirmedRecap_withMatchingCreator_shouldSucceed() {
            EventRecap recap = EventRecap.createUnconfirmedRecap(
                event, creator, "Valid draft", "Valid Draft"
            );

            assertThat(recap.getCreator()).isEqualTo(creator);
            assertThat(recap.getEvent().getCreator()).isEqualTo(creator);
        }

        @Test
        void createUnconfirmedRecap_withMismatchedCreator_shouldThrowException() {
            assertThatThrownBy(() -> EventRecap.createUnconfirmedRecap(
                event, otherUser, "Invalid draft", "Invalid Draft"
            ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Recap creator must match event creator.");
        }

        @Test
        void ownershipValidation_shouldPreventRecapForDifferentUser() {
            User anotherUser = TestUtils.createTestUser("thirduser");
            Event otherUserEvent = TestUtils.createValidCompletedEvent(anotherUser, clock);

            assertThatThrownBy(() -> EventRecap.createConfirmedRecap(
                otherUserEvent, creator, "Unauthorized recap", "Unauthorized"
            ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Recap creator must match event creator.");
        }
    }

    @Nested
    class StateManagement {

        @Test
        void defaultState_shouldHaveEmptyMediaList() {
            EventRecap recap = EventRecap.createConfirmedRecap(
                event, creator, "Notes", "Name"
            );

            assertThat(recap.getMedia()).isEmpty();
        }

        @Test
        void setUnconfirmed_shouldUpdateUnconfirmedFlag() {
            EventRecap recap = EventRecap.createConfirmedRecap(
                event, creator, "Notes", "Name"
            );

            recap.setUnconfirmed(true);

            assertThat(recap.isUnconfirmed()).isTrue();
        }

        @Test
        void setUnconfirmed_onDraft_shouldUpdateFlag() {
            EventRecap recap = EventRecap.createUnconfirmedRecap(
                event, creator, "Draft", "Draft Name"
            );

            recap.setUnconfirmed(false);

            assertThat(recap.isUnconfirmed()).isFalse();
        }
    }

    @Nested
    class ContentManagement {

        @Test
        void setNotes_shouldUpdateNotes() {
            EventRecap recap = EventRecap.createConfirmedRecap(
                event, creator, "Original notes", "Name"
            );

            recap.setNotes("Updated notes");

            assertThat(recap.getNotes()).isEqualTo("Updated notes");
        }

        @Test
        void setNotes_withNull_shouldSetNull() {
            EventRecap recap = EventRecap.createConfirmedRecap(
                event, creator, "Original notes", "Name"
            );

            recap.setNotes(null);

            assertThat(recap.getNotes()).isNull();
        }

        @Test
        void setRecapName_shouldUpdateRecapName() {
            EventRecap recap = EventRecap.createConfirmedRecap(
                event, creator, "Notes", "Original Name"
            );

            recap.setRecapName("Updated Name");

            assertThat(recap.getRecapName()).isEqualTo("Updated Name");
        }

        @Test
        void setRecapName_withNull_shouldSetNull() {
            EventRecap recap = EventRecap.createConfirmedRecap(
                event, creator, "Notes", "Original Name"
            );

            recap.setRecapName(null);

            assertThat(recap.getRecapName()).isNull();
        }
    }

    @Nested
    class MediaManagement {

        @Test
        void setMedia_shouldReplaceMediaList() {
            EventRecap recap = EventRecap.createConfirmedRecap(
                event, creator, "Notes", "Name"
            );
            List<RecapMedia> mediaList = new ArrayList<>();
            RecapMedia media1 = TestUtils.createValidImageRecapMedia(recap);
            RecapMedia media2 = TestUtils.createValidVideoRecapMedia(recap);
            mediaList.add(media1);
            mediaList.add(media2);

            recap.setMedia(mediaList);

            assertThat(recap.getMedia()).hasSize(2);
            assertThat(recap.getMedia()).containsExactly(media1, media2);
        }

        @Test
        void setMedia_withEmptyList_shouldClearMedia() {
            EventRecap recap = EventRecap.createConfirmedRecap(
                event, creator, "Notes", "Name"
            );
            List<RecapMedia> mediaList = new ArrayList<>();
            mediaList.add(TestUtils.createValidImageRecapMedia(recap));
            recap.setMedia(mediaList);

            recap.setMedia(new ArrayList<>());

            assertThat(recap.getMedia()).isEmpty();
        }

        @Test
        void getMedia_shouldReturnMutableList() {
            EventRecap recap = EventRecap.createConfirmedRecap(
                event, creator, "Notes", "Name"
            );
            List<RecapMedia> mediaList = recap.getMedia();

            mediaList.add(TestUtils.createValidImageRecapMedia(recap));

            assertThat(recap.getMedia()).hasSize(1);
        }
    }

    @Nested
    class EqualityAndHashing {

        @Test
        void equals_withSameId_shouldReturnTrue() {
            EventRecap recap1 = EventRecap.createConfirmedRecap(
                event, creator, "Notes 1", "Name 1"
            );
            EventRecap recap2 = EventRecap.createConfirmedRecap(
                event, creator, "Notes 2", "Name 2"
            );
            
            setEventRecapId(recap1, 1L);
            setEventRecapId(recap2, 1L);

            assertThat(recap1).isEqualTo(recap2);
        }

        @Test
        void equals_withDifferentIds_shouldReturnFalse() {
            EventRecap recap1 = EventRecap.createConfirmedRecap(
                event, creator, "Same Notes", "Same Name"
            );
            EventRecap recap2 = EventRecap.createConfirmedRecap(
                event, creator, "Same Notes", "Same Name"
            );
            
            setEventRecapId(recap1, 1L);
            setEventRecapId(recap2, 2L);

            assertThat(recap1).isNotEqualTo(recap2);
        }

        @Test
        void equals_withNullIds_shouldReturnFalse() {
            EventRecap recap1 = EventRecap.createConfirmedRecap(
                event, creator, "Notes", "Name"
            );
            EventRecap recap2 = EventRecap.createConfirmedRecap(
                event, creator, "Notes", "Name"
            );

            // Without IDs, they should not be equal (ID-only equality)
            assertThat(recap1).isNotEqualTo(recap2);
        }

        @Test
        void equals_withNullId_shouldOnlyEqualSelf() {
            EventRecap recap = EventRecap.createConfirmedRecap(
                event, creator, "Notes", "Name"
            );

            assertThat(recap).isEqualTo(recap);
        }

        @Test
        void hashCode_withId_shouldUseIdHashCode() {
            EventRecap recap = EventRecap.createConfirmedRecap(
                event, creator, "Notes", "Name"
            );
            setEventRecapId(recap, 1L);

            assertThat(recap.hashCode()).isEqualTo(Long.valueOf(1L).hashCode());
        }

        @Test
        void hashCode_withNullId_shouldReturnZero() {
            EventRecap recap = EventRecap.createConfirmedRecap(
                event, creator, "Notes", "Name"
            );

            assertThat(recap.hashCode()).isZero();
        }

        @Test
        void hashCode_shouldBeConsistentWithEquals() {
            EventRecap recap1 = EventRecap.createConfirmedRecap(
                event, creator, "Notes", "Name"
            );
            EventRecap recap2 = EventRecap.createConfirmedRecap(
                event, creator, "Notes", "Name"
            );
            
            setEventRecapId(recap1, 1L);
            setEventRecapId(recap2, 1L);

            assertThat(recap1.hashCode()).isEqualTo(recap2.hashCode());
        }
    }

    @Nested
    class InitialState {

        @Test
        void newConfirmedRecap_shouldHaveCorrectInitialState() {
            EventRecap recap = EventRecap.createConfirmedRecap(
                event, creator, "Notes", "Name"
            );

            assertThat(recap.getId()).isNull(); // Not persisted yet
            assertThat(recap.getEvent()).isEqualTo(event);
            assertThat(recap.getCreator()).isEqualTo(creator);
            assertThat(recap.getNotes()).isEqualTo("Notes");
            assertThat(recap.getRecapName()).isEqualTo("Name");
            assertThat(recap.isUnconfirmed()).isFalse();
            assertThat(recap.getMedia()).isEmpty();
        }

        @Test
        void newUnconfirmedRecap_shouldHaveCorrectInitialState() {
            EventRecap recap = EventRecap.createUnconfirmedRecap(
                event, creator, "Draft", "Draft Name"
            );

            assertThat(recap.getId()).isNull(); // Not persisted yet
            assertThat(recap.getEvent()).isEqualTo(event);
            assertThat(recap.getCreator()).isEqualTo(creator);
            assertThat(recap.getNotes()).isEqualTo("Draft");
            assertThat(recap.getRecapName()).isEqualTo("Draft Name");
            assertThat(recap.isUnconfirmed()).isTrue();
            assertThat(recap.getMedia()).isEmpty();
        }
    }

    // Helper method using reflection
    private void setEventRecapId(EventRecap recap, Long id) {
        try {
            var field = EventRecap.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(recap, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set event recap ID", e);
        }
    }
}