package com.yohan.event_planner.domain;

import com.yohan.event_planner.domain.enums.LabelColor;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LabelTest {

    private static final String LABEL_NAME = "Work Tasks";
    private User creator;
    private User otherUser;

    @BeforeEach
    void setUp() {
        creator = TestUtils.createValidUserEntity();
        otherUser = TestUtils.createTestUser("otheruser");
    }

    @Nested
    class Construction {

        @Test
        void constructor_shouldSetNameAndCreator() {
            Label label = new Label(LABEL_NAME, LabelColor.RED, creator);

            assertThat(label.getName()).isEqualTo(LABEL_NAME);
            assertThat(label.getColor()).isEqualTo(LabelColor.RED);
            assertThat(label.getCreator()).isEqualTo(creator);
        }

        @Test
        void constructor_withNullName_shouldAllowNull() {
            Label label = new Label(null, LabelColor.BLUE, creator);

            assertThat(label.getName()).isNull();
            assertThat(label.getColor()).isEqualTo(LabelColor.BLUE);
            assertThat(label.getCreator()).isEqualTo(creator);
        }

        @Test
        void constructor_withNullCreator_shouldAllowNull() {
            Label label = new Label(LABEL_NAME, LabelColor.GREEN, null);

            assertThat(label.getName()).isEqualTo(LABEL_NAME);
            assertThat(label.getColor()).isEqualTo(LabelColor.GREEN);
            assertThat(label.getCreator()).isNull();
        }

        @Test
        void defaultConstructor_shouldCreateEmptyLabel() {
            Label label = new Label();

            assertThat(label.getName()).isNull();
            assertThat(label.getColor()).isNull();
            assertThat(label.getCreator()).isNull();
            assertThat(label.getId()).isNull();
        }

        @Test
        void newLabel_shouldHaveNullId() {
            Label label = new Label(LABEL_NAME, LabelColor.PURPLE, creator);

            assertThat(label.getId()).isNull(); // Not persisted yet
        }
    }

    @Nested
    class PropertyManagement {

        @Test
        void setName_shouldUpdateName() {
            Label label = new Label(LABEL_NAME, LabelColor.ORANGE, creator);
            String newName = "Updated Label Name";

            label.setName(newName);

            assertThat(label.getName()).isEqualTo(newName);
        }

        @Test
        void setName_withNull_shouldSetNull() {
            Label label = new Label(LABEL_NAME, LabelColor.YELLOW, creator);

            label.setName(null);

            assertThat(label.getName()).isNull();
        }

        @Test
        void setCreator_shouldUpdateCreator() {
            Label label = new Label(LABEL_NAME, LabelColor.TEAL, creator);

            label.setCreator(otherUser);

            assertThat(label.getCreator()).isEqualTo(otherUser);
        }

        @Test
        void setCreator_withNull_shouldSetNull() {
            Label label = new Label(LABEL_NAME, LabelColor.PINK, creator);

            label.setCreator(null);

            assertThat(label.getCreator()).isNull();
        }
    }

    @Nested
    class EqualityAndHashing {

        @Test
        void equals_withSameId_shouldReturnTrue() {
            Label label1 = new Label("Label 1", LabelColor.RED, creator);
            Label label2 = new Label("Label 2", LabelColor.BLUE, otherUser);
            
            setLabelId(label1, 1L);
            setLabelId(label2, 1L);

            assertThat(label1).isEqualTo(label2);
        }

        @Test
        void equals_withDifferentIds_shouldReturnFalse() {
            Label label1 = new Label(LABEL_NAME, LabelColor.GREEN, creator);
            Label label2 = new Label(LABEL_NAME, LabelColor.GREEN, creator);
            
            setLabelId(label1, 1L);
            setLabelId(label2, 2L);

            assertThat(label1).isNotEqualTo(label2);
        }

        @Test
        void equals_withNullIds_shouldReturnFalse() {
            Label label1 = new Label(LABEL_NAME, LabelColor.PURPLE, creator);
            Label label2 = new Label(LABEL_NAME, LabelColor.PURPLE, creator);

            // Without IDs, they should not be equal (ID-only equality)
            assertThat(label1).isNotEqualTo(label2);
        }

        @Test
        void equals_withOneNullId_shouldReturnFalse() {
            Label label1 = new Label(LABEL_NAME, LabelColor.ORANGE, creator);
            Label label2 = new Label(LABEL_NAME, LabelColor.ORANGE, creator);
            
            setLabelId(label1, 1L);
            // label2 has null ID

            assertThat(label1).isNotEqualTo(label2);
        }

        @Test
        void equals_withSameNameDifferentCreators_shouldNotMatterForEquality() {
            Label label1 = new Label(LABEL_NAME, LabelColor.TEAL, creator);
            Label label2 = new Label(LABEL_NAME, LabelColor.TEAL, otherUser);
            
            setLabelId(label1, 1L);
            setLabelId(label2, 1L);

            // Should be equal because ID is the same, regardless of name/creator
            assertThat(label1).isEqualTo(label2);
        }

        @Test
        void equals_withSelf_shouldReturnTrue() {
            Label label = new Label(LABEL_NAME, LabelColor.YELLOW, creator);

            assertThat(label).isEqualTo(label);
        }

        @Test
        void equals_withNull_shouldReturnFalse() {
            Label label = new Label(LABEL_NAME, LabelColor.GRAY, creator);

            assertThat(label).isNotEqualTo(null);
        }

        @Test
        void equals_withDifferentClass_shouldReturnFalse() {
            Label label = new Label(LABEL_NAME, LabelColor.BLUE, creator);

            assertThat(label).isNotEqualTo("not a label");
        }

        @Test
        void hashCode_withId_shouldUseIdHashCode() {
            Label label = new Label(LABEL_NAME, LabelColor.BLUE, creator);
            setLabelId(label, 1L);

            assertThat(label.hashCode()).isEqualTo(Long.valueOf(1L).hashCode());
        }

        @Test
        void hashCode_withNullId_shouldReturnZero() {
            Label label = new Label(LABEL_NAME, LabelColor.BLUE, creator);

            assertThat(label.hashCode()).isZero();
        }

        @Test
        void hashCode_shouldBeConsistentWithEquals() {
            Label label1 = new Label("Label 1", LabelColor.RED, creator);
            Label label2 = new Label("Label 2", LabelColor.GREEN, otherUser);
            
            setLabelId(label1, 1L);
            setLabelId(label2, 1L);

            assertThat(label1.hashCode()).isEqualTo(label2.hashCode());
        }

        @Test
        void hashCode_shouldIgnoreNameAndCreator() {
            Label label1 = new Label("Label 1", LabelColor.RED, creator);
            Label label2 = new Label("Label 2", LabelColor.GREEN, otherUser);
            
            setLabelId(label1, 1L);
            setLabelId(label2, 1L);

            // Same ID should result in same hash code regardless of other fields
            assertThat(label1.hashCode()).isEqualTo(label2.hashCode());
        }
    }

    @Nested
    class EntitySemantics {

        @Test
        void idBasedEquality_shouldWorkForCollections() {
            Label label1 = new Label(LABEL_NAME, LabelColor.BLUE, creator);
            Label label2 = new Label(LABEL_NAME, LabelColor.BLUE, creator);
            
            setLabelId(label1, 1L);
            setLabelId(label2, 1L);

            // Should be considered the same entity for Set operations
            java.util.Set<Label> labelSet = new java.util.HashSet<>();
            labelSet.add(label1);
            labelSet.add(label2);

            assertThat(labelSet).hasSize(1); // Only one element due to ID equality
        }

        @Test
        void differentIds_shouldBeTreatedAsDifferentEntities() {
            Label label1 = new Label(LABEL_NAME, LabelColor.BLUE, creator);
            Label label2 = new Label(LABEL_NAME, LabelColor.BLUE, creator);
            
            setLabelId(label1, 1L);
            setLabelId(label2, 2L);

            java.util.Set<Label> labelSet = new java.util.HashSet<>();
            labelSet.add(label1);
            labelSet.add(label2);

            assertThat(labelSet).hasSize(2); // Two different entities
        }

        @Test
        void transientEntities_shouldNotBeEqual() {
            Label label1 = new Label(LABEL_NAME, LabelColor.BLUE, creator);
            Label label2 = new Label(LABEL_NAME, LabelColor.BLUE, creator);

            // Both have null IDs (transient entities)
            java.util.Set<Label> labelSet = new java.util.HashSet<>();
            labelSet.add(label1);
            labelSet.add(label2);

            assertThat(labelSet).hasSize(2); // Treated as different entities
        }
    }

    @Nested
    class UniquenessConstraintSemantics {

        @Test
        void sameNameDifferentUsers_shouldBeAllowed() {
            Label label1 = new Label(LABEL_NAME, LabelColor.BLUE, creator);
            Label label2 = new Label(LABEL_NAME, LabelColor.TEAL, otherUser);

            assertThat(label1.getName()).isEqualTo(label2.getName());
            assertThat(label1.getCreator()).isNotEqualTo(label2.getCreator());
        }

        @Test
        void differentNamesSameUser_shouldBeAllowed() {
            Label label1 = new Label("Work", LabelColor.ORANGE, creator);
            Label label2 = new Label("Personal", LabelColor.PURPLE, creator);

            assertThat(label1.getName()).isNotEqualTo(label2.getName());
            assertThat(label1.getCreator()).isEqualTo(label2.getCreator());
        }

        @Test
        void caseSensitiveNames_shouldBeTreatedAsDifferent() {
            Label label1 = new Label("work", LabelColor.YELLOW, creator);
            Label label2 = new Label("Work", LabelColor.PINK, creator);

            // Names are case-sensitive, so these should be different
            assertThat(label1.getName()).isNotEqualTo(label2.getName());
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void emptyName_shouldBeAllowed() {
            Label label = new Label("", LabelColor.TEAL, creator);

            assertThat(label.getName()).isEmpty();
        }

        @Test
        void whitespaceOnlyName_shouldBePreserved() {
            String whitespaceName = "   ";
            Label label = new Label(whitespaceName, LabelColor.GRAY, creator);

            assertThat(label.getName()).isEqualTo(whitespaceName);
        }

        @Test
        void longName_shouldBeAllowed() {
            String longName = "This is a very long label name that might be used for detailed categorization";
            Label label = new Label(longName, LabelColor.RED, creator);

            assertThat(label.getName()).isEqualTo(longName);
        }

        @Test
        void specialCharactersInName_shouldBePreserved() {
            String specialName = "Work@Home-2024_#1";
            Label label = new Label(specialName, LabelColor.ORANGE, creator);

            assertThat(label.getName()).isEqualTo(specialName);
        }

        @Test
        void unicodeCharactersInName_shouldBeSupported() {
            String unicodeName = "工作 🏢 Travail";
            Label label = new Label(unicodeName, LabelColor.PURPLE, creator);

            assertThat(label.getName()).isEqualTo(unicodeName);
        }
    }

    // Helper method using reflection
    private void setLabelId(Label label, Long id) {
        try {
            var field = Label.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(label, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set label ID", e);
        }
    }
}