package com.yohan.event_planner.service;

import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.LabelCreateDTO;
import com.yohan.event_planner.dto.LabelResponseDTO;
import com.yohan.event_planner.dto.LabelUpdateDTO;
import com.yohan.event_planner.exception.LabelException;
import com.yohan.event_planner.exception.LabelNotFoundException;
import com.yohan.event_planner.exception.LabelOwnershipException;
import com.yohan.event_planner.exception.SystemManagedEntityException;
import com.yohan.event_planner.mapper.LabelMapper;
import com.yohan.event_planner.repository.LabelRepository;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


public class LabelServiceImplTest {

    private UserBO userBO;
    private LabelRepository labelRepository;
    private LabelMapper labelMapper;
    private OwnershipValidator ownershipValidator;
    private AuthenticatedUserProvider authenticatedUserProvider;
    private LabelServiceImpl labelService;
    private User testUser;

    @BeforeEach
    void setUp() {
        userBO = mock(UserBO.class);
        labelRepository = mock(LabelRepository.class);
        labelMapper = mock(LabelMapper.class);
        ownershipValidator = mock(OwnershipValidator.class);
        authenticatedUserProvider = mock(AuthenticatedUserProvider.class);
        labelService = new LabelServiceImpl(userBO, labelRepository, labelMapper, ownershipValidator, authenticatedUserProvider);
        testUser = TestUtils.createValidUserEntityWithId();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
    }

    @Nested
    class GetLabelByIdTests {

        @Test
        void shouldReturnLabelById() {
            // Arrange
            Label label = new Label("Focus", testUser);
            LabelResponseDTO expected = new LabelResponseDTO(1L, "Focus", testUser.getUsername());

            when(labelRepository.findById(1L)).thenReturn(Optional.of(label));
            doNothing().when(ownershipValidator).validateLabelOwnership(testUser.getId(), label);
            when(labelMapper.toResponseDTO(label)).thenReturn(expected);

            // Act
            LabelResponseDTO result = labelService.getLabelById(1L);

            // Assert
            assertEquals(expected, result);
        }

        @Test
        void shouldThrowWhenLabelNotFound() {
            // Arrange
            when(labelRepository.findById(1L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(LabelNotFoundException.class, () -> labelService.getLabelById(1L));
        }
    }

    @Nested
    class GetLabelsByUserTests {

        @Test
        void shouldReturnAllLabelsForUser() {
            // Arrange
            Label label1 = TestUtils.createValidLabelWithId(1L, testUser);
            label1.setName("Focus");
            Label label2 = TestUtils.createValidLabelWithId(2L, testUser);
            label2.setName("Strength");

            // Create the "Unlabeled" label with ID
            Label unlabeled = TestUtils.createValidLabelWithId(3L, testUser);
            unlabeled.setName("Unlabeled");

            Long userId = testUser.getId();

            // Mock the User object (testUser)
            User mockedUser = mock(User.class);

            // Mock the getUnlabeled() method to return the "Unlabeled" label
            when(mockedUser.getUnlabeled()).thenReturn(unlabeled);

            // Mock UserBO to return the mocked user when getUserById is called
            when(userBO.getUserById(userId)).thenReturn(Optional.of(mockedUser));

            // Mock the repository to return labels, including the "Unlabeled" label.
            when(labelRepository.findAllByCreatorIdOrderByNameAsc(userId)).thenReturn(List.of(unlabeled, label1, label2));

            // Mock the labelMapper to return non-null LabelResponseDTO objects
            when(labelMapper.toResponseDTO(label1)).thenReturn(new LabelResponseDTO(1L, "Focus", testUser.getUsername()));
            when(labelMapper.toResponseDTO(label2)).thenReturn(new LabelResponseDTO(2L, "Strength", testUser.getUsername()));
            when(labelMapper.toResponseDTO(unlabeled)).thenReturn(new LabelResponseDTO(3L, "Unlabeled", testUser.getUsername()));

            // Act
            List<LabelResponseDTO> result = labelService.getLabelsByUser(userId);

            // Assert
            verify(labelRepository).findAllByCreatorIdOrderByNameAsc(userId);  // Ensure the sorted query was called

            // Assert that no labels are null and all labels have a valid name
            assertThat(result)
                    .doesNotContainNull()  // Ensure no null values in result list
                    .extracting("name")
                    .doesNotContainNull()  // Ensure name is not null for any label
                    .containsExactly("Focus", "Strength");  // Ensure correct labels are returned, in alphabetical order
        }

    }

    @Nested
    class CreateLabelTests {

        @Test
        void shouldCreateLabelSuccessfully() {
            // Arrange
            LabelCreateDTO dto = new LabelCreateDTO("Focus");
            Label savedLabel = new Label(dto.name(), testUser);
            LabelResponseDTO expected = new LabelResponseDTO(1L, "Focus", testUser.getUsername());

            when(labelRepository.existsByNameAndCreator(dto.name(), testUser)).thenReturn(false);
            when(labelRepository.save(any(Label.class))).thenReturn(savedLabel);
            when(labelMapper.toResponseDTO(savedLabel)).thenReturn(expected);

            // Act
            LabelResponseDTO result = labelService.createLabel(dto);

            // Assert
            assertEquals(expected.name(), result.name());
            verify(labelRepository).save(any(Label.class));
        }

        @Test
        void shouldThrowWhenLabelAlreadyExists() {
            // Arrange
            LabelCreateDTO dto = new LabelCreateDTO("Focus");
            when(labelRepository.existsByNameAndCreator(dto.name(), testUser)).thenReturn(true);

            // Act + Assert
            assertThrows(LabelException.class, () -> labelService.createLabel(dto));
        }
    }

    @Nested
    class UpdateLabelTests {

        @Test
        void shouldUpdateLabelName() {
            // Arrange
            Label existing = new Label("Focus", testUser);
            LabelUpdateDTO dto = new LabelUpdateDTO("Refocus");
            LabelResponseDTO expected = new LabelResponseDTO(1L, "Refocus", testUser.getUsername());

            when(labelRepository.findById(1L)).thenReturn(Optional.of(existing));
            doNothing().when(ownershipValidator).validateLabelOwnership(testUser.getId(), existing);
            when(labelRepository.existsByNameAndCreator("Refocus", testUser)).thenReturn(false);
            when(labelRepository.save(existing)).thenReturn(existing);
            when(labelMapper.toResponseDTO(existing)).thenReturn(expected);

            // Act
            LabelResponseDTO result = labelService.updateLabel(1L, dto);

            // Assert
            assertEquals(expected.name(), result.name());
        }

        @Test
        void shouldThrowWhenLabelNameAlreadyExists() {
            // Arrange
            Label existing = new Label("OldName", testUser);
            LabelUpdateDTO dto = new LabelUpdateDTO("NewName");

            when(labelRepository.findById(1L)).thenReturn(Optional.of(existing));
            doNothing().when(ownershipValidator).validateLabelOwnership(testUser.getId(), existing);
            when(labelRepository.existsByNameAndCreator("NewName", testUser)).thenReturn(true);

            // Act + Assert
            assertThrows(LabelException.class, () -> labelService.updateLabel(1L, dto));
        }
    }

    @Nested
    class DeleteLabelTests {

        @Test
        void shouldDeleteLabelSuccessfully() {
            // Arrange
            Label label = new Label("Focus", testUser);
            when(labelRepository.findById(1L)).thenReturn(Optional.of(label));
            doNothing().when(ownershipValidator).validateLabelOwnership(testUser.getId(), label);

            // Act
            labelService.deleteLabel(1L);

            // Assert
            verify(labelRepository).delete(label);
        }

        @Test
        void shouldThrowWhenLabelIsUnlabeled() {
            // Arrange
            Label label = TestUtils.createValidLabelWithId(1L, testUser);
            TestUtils.setUnlabeledLabel(testUser, label);
            when(labelRepository.findById(1L)).thenReturn(Optional.of(label));
            doNothing().when(ownershipValidator).validateLabelOwnership(testUser.getId(), label);

            // Act + Assert
            assertThrows(SystemManagedEntityException.class, () -> labelService.deleteLabel(1L));
        }

        @Test
        void shouldThrowWhenLabelNotFound() {
            // Arrange
            when(labelRepository.findById(1L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(LabelNotFoundException.class, () -> labelService.deleteLabel(1L));
        }
    }

    @Nested
    class ValidateExistenceAndOwnershipTests {

        @Test
        void shouldDoNothing_whenAllLabelsExistAndBelongToUser() {
            // Arrange
            Long labelId1 = 1L;
            Long labelId2 = 2L;
            Set<Long> labelIds = Set.of(labelId1, labelId2);

            Label label1 = TestUtils.createValidLabelWithId(labelId1, testUser);
            Label label2 = TestUtils.createValidLabelWithId(labelId2, testUser);

            when(labelRepository.findAllById(labelIds)).thenReturn(List.of(label1, label2));

            // Act + Assert
            assertDoesNotThrow(() -> labelService.validateExistenceAndOwnership(labelIds, testUser.getId()));
            verify(labelRepository).findAllById(labelIds);
        }

        @Test
        void shouldThrowNotFound_whenSomeLabelsDoNotExist() {
            // Arrange
            Long labelId1 = 1L;
            Long labelId2 = 2L;
            Set<Long> labelIds = Set.of(labelId1, labelId2);

            Label label1 = TestUtils.createValidLabelWithId(labelId1, testUser);
            when(labelRepository.findAllById(labelIds)).thenReturn(List.of(label1)); // label2 missing

            // Act + Assert
            assertThrows(LabelNotFoundException.class, () ->
                    labelService.validateExistenceAndOwnership(labelIds, testUser.getId())
            );
        }

        @Test
        void shouldThrowOwnershipException_whenLabelDoesNotBelongToUser() {
            // Arrange
            Long labelId = 1L;
            User otherUser = TestUtils.createValidUserEntityWithId(99L);
            Label label = TestUtils.createValidLabelWithId(labelId, otherUser);

            Set<Long> labelIds = Set.of(labelId);
            when(labelRepository.findAllById(labelIds)).thenReturn(List.of(label));

            // Act + Assert
            assertThrows(LabelOwnershipException.class, () ->
                    labelService.validateExistenceAndOwnership(labelIds, testUser.getId())
            );
        }

        @Test
        void shouldDoNothing_whenInputIsEmpty() {
            // Act + Assert
            assertDoesNotThrow(() -> labelService.validateExistenceAndOwnership(Set.of(), testUser.getId()));
            verifyNoInteractions(labelRepository);
        }
    }
}