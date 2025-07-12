package com.yohan.event_planner.domain;

import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BadgeTest {

    private User user;
    private Badge badge;

    @BeforeEach
    void setUp() {
        user = TestUtils.createValidUserEntity();
        badge = new Badge("Test Badge", user, 0);
    }

    @Nested
    class Construction {

        @Test
        void constructor_shouldSetPropertiesCorrectly() {
            Badge newBadge = new Badge("My Badge", user, 5);

            assertThat(newBadge.getName()).isEqualTo("My Badge");
            assertThat(newBadge.getUser()).isEqualTo(user);
            assertThat(newBadge.getSortOrder()).isEqualTo(5);
        }

        @Test
        void constructor_shouldInitializeEmptyCollections() {
            Badge newBadge = new Badge("Empty Badge", user, 0);

            assertThat(newBadge.getLabelIds()).isEmpty();
            assertThat(newBadge.getLabelOrder()).isEmpty();
        }

        @Test
        void defaultConstructor_shouldCreateEmptyBadge() {
            Badge emptyBadge = new Badge();

            assertThat(emptyBadge.getName()).isNull();
            assertThat(emptyBadge.getUser()).isNull();
            assertThat(emptyBadge.getSortOrder()).isZero();
            assertThat(emptyBadge.getLabelIds()).isEmpty();
            assertThat(emptyBadge.getLabelOrder()).isEmpty();
        }
    }

    @Nested
    class LabelCollectionManagement {

        @Test
        void addLabelIds_shouldAddNewLabelsToSets() {
            Set<Long> labelsToAdd = Set.of(1L, 2L, 3L);

            badge.addLabelIds(labelsToAdd);

            assertThat(badge.getLabelIds()).containsExactlyInAnyOrderElementsOf(labelsToAdd);
        }

        @Test
        void addLabelIds_shouldMaintainOrderInList() {
            Set<Long> labelsToAdd = Set.of(3L, 1L, 2L); // Set has unpredictable order

            badge.addLabelIds(labelsToAdd);

            // Should maintain the order they were added (iteration order of the set)
            assertThat(badge.getLabelOrder()).hasSize(3);
            assertThat(badge.getLabelOrder()).containsExactlyInAnyOrderElementsOf(labelsToAdd);
        }

        @Test
        void addLabelIds_shouldNotAddDuplicates() {
            Set<Long> initialLabels = Set.of(1L, 2L);
            Set<Long> labelsWithDuplicates = Set.of(2L, 3L, 4L);

            badge.addLabelIds(initialLabels);
            badge.addLabelIds(labelsWithDuplicates);

            assertThat(badge.getLabelIds()).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
            assertThat(badge.getLabelOrder()).hasSize(4);
            assertThat(badge.getLabelOrder()).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
        }

        @Test
        void addLabelIds_withEmptySet_shouldHaveNoEffect() {
            Set<Long> initialLabels = Set.of(1L, 2L);
            badge.addLabelIds(initialLabels);

            badge.addLabelIds(Set.of());

            assertThat(badge.getLabelIds()).containsExactlyInAnyOrder(1L, 2L);
            assertThat(badge.getLabelOrder()).hasSize(2);
        }

        @Test
        void removeLabelIds_shouldRemoveExistingLabels() {
            Set<Long> initialLabels = Set.of(1L, 2L, 3L, 4L);
            Set<Long> labelsToRemove = Set.of(2L, 4L);

            badge.addLabelIds(initialLabels);
            badge.removeLabelIds(labelsToRemove);

            assertThat(badge.getLabelIds()).containsExactlyInAnyOrder(1L, 3L);
            assertThat(badge.getLabelOrder()).containsExactlyInAnyOrder(1L, 3L);
        }

        @Test
        void removeLabelIds_shouldNotAffectNonExistentLabels() {
            Set<Long> initialLabels = Set.of(1L, 2L);
            Set<Long> labelsToRemove = Set.of(3L, 4L, 5L);

            badge.addLabelIds(initialLabels);
            badge.removeLabelIds(labelsToRemove);

            assertThat(badge.getLabelIds()).containsExactlyInAnyOrder(1L, 2L);
            assertThat(badge.getLabelOrder()).hasSize(2);
        }

        @Test
        void removeLabelIds_withMixOfExistingAndNonExisting_shouldRemoveOnlyExisting() {
            Set<Long> initialLabels = Set.of(1L, 2L, 3L);
            Set<Long> labelsToRemove = Set.of(2L, 4L, 5L); // Only 2L exists

            badge.addLabelIds(initialLabels);
            badge.removeLabelIds(labelsToRemove);

            assertThat(badge.getLabelIds()).containsExactlyInAnyOrder(1L, 3L);
            assertThat(badge.getLabelOrder()).containsExactlyInAnyOrder(1L, 3L);
        }

        @Test
        void removeLabelIds_withEmptySet_shouldHaveNoEffect() {
            Set<Long> initialLabels = Set.of(1L, 2L);
            badge.addLabelIds(initialLabels);

            badge.removeLabelIds(Set.of());

            assertThat(badge.getLabelIds()).containsExactlyInAnyOrder(1L, 2L);
            assertThat(badge.getLabelOrder()).hasSize(2);
        }
    }

    @Nested
    class DualCollectionConsistency {

        @Test
        void collectionsShouldRemainConsistent_afterMultipleOperations() {
            // Add some initial labels
            badge.addLabelIds(Set.of(1L, 2L, 3L));
            
            // Remove some labels
            badge.removeLabelIds(Set.of(2L));
            
            // Add more labels
            badge.addLabelIds(Set.of(4L, 5L));
            
            // Remove again
            badge.removeLabelIds(Set.of(1L, 5L));

            // Verify consistency
            assertThat(badge.getLabelIds()).containsExactlyInAnyOrder(3L, 4L);
            assertThat(badge.getLabelOrder()).containsExactlyInAnyOrder(3L, 4L);
            assertThat(badge.getLabelIds()).hasSize(badge.getLabelOrder().size());
        }

        @Test
        void labelOrderShouldNotContainDuplicates() {
            Set<Long> labels = Set.of(1L, 2L, 3L);

            badge.addLabelIds(labels);
            badge.addLabelIds(labels); // Try to add same labels again

            assertThat(badge.getLabelOrder()).containsExactlyInAnyOrder(1L, 2L, 3L);
            assertThat(badge.getLabelOrder()).hasSize(3); // No duplicates
        }

        @Test
        void setAndListShouldContainSameElements() {
            Set<Long> labels = Set.of(1L, 2L, 3L, 4L, 5L);

            badge.addLabelIds(labels);

            // Both collections should contain exactly the same elements
            assertThat(badge.getLabelIds()).containsExactlyInAnyOrderElementsOf(badge.getLabelOrder());
        }

        @Test
        void emptyBadge_shouldHaveEmptyCollections() {
            assertThat(badge.getLabelIds()).isEmpty();
            assertThat(badge.getLabelOrder()).isEmpty();
        }
    }

    @Nested
    class OrderPreservation {

        @Test
        void setLabelOrder_shouldUpdateOrderList() {
            badge.addLabelIds(Set.of(1L, 2L, 3L));
            List<Long> newOrder = List.of(3L, 1L, 2L);

            badge.setLabelOrder(newOrder);

            assertThat(badge.getLabelOrder()).containsExactly(3L, 1L, 2L);
        }

        @Test
        void setLabelOrder_shouldNotAffectLabelIds() {
            Set<Long> originalIds = Set.of(1L, 2L, 3L);
            badge.addLabelIds(originalIds);
            List<Long> newOrder = List.of(3L, 1L, 2L);

            badge.setLabelOrder(newOrder);

            assertThat(badge.getLabelIds()).containsExactlyInAnyOrderElementsOf(originalIds);
        }

        @Test
        void setLabelOrder_withEmptyList_shouldClearOrder() {
            badge.addLabelIds(Set.of(1L, 2L, 3L));

            badge.setLabelOrder(new ArrayList<>());

            assertThat(badge.getLabelOrder()).isEmpty();
            // LabelIds should remain unchanged
            assertThat(badge.getLabelIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
        }
    }

    @Nested
    class PropertyManagement {

        @Test
        void setName_shouldUpdateName() {
            badge.setName("Updated Badge Name");

            assertThat(badge.getName()).isEqualTo("Updated Badge Name");
        }

        @Test
        void setSortOrder_shouldUpdateSortOrder() {
            badge.setSortOrder(10);

            assertThat(badge.getSortOrder()).isEqualTo(10);
        }

        @Test
        void getLabelIds_shouldReturnMutableSet() {
            badge.addLabelIds(Set.of(1L, 2L));
            Set<Long> labelIds = badge.getLabelIds();

            // Should be able to modify the returned set
            labelIds.add(3L);

            assertThat(badge.getLabelIds()).contains(3L);
        }

        @Test
        void getLabelOrder_shouldReturnMutableList() {
            badge.addLabelIds(Set.of(1L, 2L));
            List<Long> labelOrder = badge.getLabelOrder();

            // Should be able to modify the returned list
            labelOrder.add(3L);

            assertThat(badge.getLabelOrder()).contains(3L);
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void addLabelIds_withSingleElement_shouldWork() {
            badge.addLabelIds(Set.of(1L));

            assertThat(badge.getLabelIds()).containsExactly(1L);
            assertThat(badge.getLabelOrder()).containsExactly(1L);
        }

        @Test
        void removeLabelIds_withSingleElement_shouldWork() {
            badge.addLabelIds(Set.of(1L, 2L));

            badge.removeLabelIds(Set.of(1L));

            assertThat(badge.getLabelIds()).containsExactly(2L);
            assertThat(badge.getLabelOrder()).containsExactly(2L);
        }

        @Test
        void removeLabelIds_removingAllLabels_shouldLeaveEmptyCollections() {
            Set<Long> allLabels = Set.of(1L, 2L, 3L);
            badge.addLabelIds(allLabels);

            badge.removeLabelIds(allLabels);

            assertThat(badge.getLabelIds()).isEmpty();
            assertThat(badge.getLabelOrder()).isEmpty();
        }
    }
}