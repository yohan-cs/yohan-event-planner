package com.yohan.event_planner.service;

import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.LabelCreateDTO;
import com.yohan.event_planner.dto.LabelResponseDTO;
import com.yohan.event_planner.dto.LabelUpdateDTO;
import com.yohan.event_planner.exception.InvalidLabelAssociationException;
import com.yohan.event_planner.exception.LabelException;
import com.yohan.event_planner.exception.LabelNotFoundException;
import com.yohan.event_planner.exception.LabelOwnershipException;
import com.yohan.event_planner.exception.SystemManagedEntityException;
import com.yohan.event_planner.exception.UserNotFoundException;
import com.yohan.event_planner.mapper.LabelMapper;
import com.yohan.event_planner.repository.LabelRepository;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.dao.DataAccessException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class LabelServiceImplTest {

    @Mock
    private UserBO userBO;
    @Mock
    private LabelRepository labelRepository;
    @Mock
    private LabelMapper labelMapper;
    @Mock
    private OwnershipValidator ownershipValidator;
    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;
    
    @InjectMocks
    private LabelServiceImpl labelService;
    
    @Mock
    private User mockedUser;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = TestUtils.createValidUserEntityWithId();
    }

    @Nested
    class ConstructorTests {

        @Test
        void shouldInitializeAllDependencies() {
            // Arrange
            UserBO mockUserBO = mock(UserBO.class);
            LabelRepository mockRepository = mock(LabelRepository.class);
            LabelMapper mockMapper = mock(LabelMapper.class);
            OwnershipValidator mockValidator = mock(OwnershipValidator.class);
            AuthenticatedUserProvider mockProvider = mock(AuthenticatedUserProvider.class);

            // Act
            LabelServiceImpl service = new LabelServiceImpl(
                mockUserBO, mockRepository, mockMapper, mockValidator, mockProvider
            );

            // Assert - Verify constructor assigns dependencies correctly by testing they work
            assertThat(service).isNotNull();
            // Dependencies will be verified through method calls in other tests
        }
    }

    @Nested
    class GetLabelByIdTests {

        @Test
        void shouldReturnLabelById() {
            // Arrange
            Label label = new Label("Focus", testUser);
            LabelResponseDTO expected = new LabelResponseDTO(1L, "Focus", testUser.getUsername());

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
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

        @Test
        void shouldThrowWhenUserDoesNotOwnLabel() {
            // Arrange
            User otherUser = TestUtils.createValidUserEntityWithId(999L);
            Label label = new Label("Focus", otherUser);
            
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
            when(labelRepository.findById(1L)).thenReturn(Optional.of(label));
            // Mock the ownership validator to throw exception when current user tries to access other user's label
            doThrow(new LabelOwnershipException(1L, testUser.getId()))
                .when(ownershipValidator).validateLabelOwnership(testUser.getId(), label);

            // Act + Assert
            assertThrows(LabelOwnershipException.class, () -> labelService.getLabelById(1L));
            verify(ownershipValidator).validateLabelOwnership(testUser.getId(), label);
        }
    }

    @Nested
    class GetLabelEntityByIdTests {

        @Test
        void shouldReturnLabelEntity() {
            // Arrange
            Label expected = new Label("Focus", testUser);
            when(labelRepository.findById(1L)).thenReturn(Optional.of(expected));

            // Act
            Label result = labelService.getLabelEntityById(1L);

            // Assert
            assertEquals(expected, result);
            verify(labelRepository).findById(1L);
        }

        @Test
        void shouldThrowWhenLabelNotFound() {
            // Arrange
            when(labelRepository.findById(1L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(LabelNotFoundException.class, () -> labelService.getLabelEntityById(1L));
        }
    }

    @Nested
    class GetLabelsByIdsTests {

        @Test
        void shouldReturnExistingLabels() {
            // Arrange
            Set<Long> labelIds = Set.of(1L, 2L);
            Label label1 = TestUtils.createValidLabelWithId(1L, testUser);
            Label label2 = TestUtils.createValidLabelWithId(2L, testUser);
            List<Label> foundLabels = List.of(label1, label2);
            
            when(labelRepository.findAllById(labelIds)).thenReturn(foundLabels);

            // Act
            Set<Label> result = labelService.getLabelsByIds(labelIds);

            // Assert
            assertThat(result)
                .hasSize(2)
                .containsExactlyInAnyOrder(label1, label2);
            verify(labelRepository).findAllById(labelIds);
        }

        @Test
        void shouldReturnEmptySetForNullInput() {
            // Act
            Set<Label> result = labelService.getLabelsByIds(null);

            // Assert
            assertThat(result).isEmpty();
            verifyNoInteractions(labelRepository);
        }

        @Test
        void shouldReturnEmptySetForEmptyInput() {
            // Act
            Set<Label> result = labelService.getLabelsByIds(Set.of());

            // Assert
            assertThat(result).isEmpty();
            verifyNoInteractions(labelRepository);
        }

        @Test
        void shouldIgnoreNonExistentIds() {
            // Arrange
            Set<Long> labelIds = Set.of(1L, 999L);
            Label label1 = TestUtils.createValidLabelWithId(1L, testUser);
            List<Label> foundLabels = List.of(label1); // Only label1 exists
            
            when(labelRepository.findAllById(labelIds)).thenReturn(foundLabels);

            // Act
            Set<Label> result = labelService.getLabelsByIds(labelIds);

            // Assert
            assertThat(result)
                .hasSize(1)
                .containsExactly(label1);
        }

        @Test
        void shouldHandleMixedExistingAndNonExistingIds() {
            // Arrange
            Set<Long> labelIds = Set.of(1L, 2L, 999L, 888L);
            Label label1 = TestUtils.createValidLabelWithId(1L, testUser);
            Label label2 = TestUtils.createValidLabelWithId(2L, testUser);
            List<Label> foundLabels = List.of(label1, label2); // Only 1L and 2L exist
            
            when(labelRepository.findAllById(labelIds)).thenReturn(foundLabels);

            // Act
            Set<Label> result = labelService.getLabelsByIds(labelIds);

            // Assert
            assertThat(result)
                .hasSize(2)
                .containsExactlyInAnyOrder(label1, label2);
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

            // Mock the getUnlabeled() method to return the "Unlabeled" label
            when(mockedUser.getUnlabeled()).thenReturn(unlabeled);
            
            // Mock UserBO to return the mocked user when getUserById is called
            when(userBO.getUserById(userId)).thenReturn(Optional.of(mockedUser));

            // Mock the repository to return labels, including the "Unlabeled" label.
            when(labelRepository.findAllByCreatorIdOrderByNameAsc(userId)).thenReturn(List.of(unlabeled, label1, label2));

            // Mock the labelMapper to return non-null LabelResponseDTO objects
            when(labelMapper.toResponseDTO(label1)).thenReturn(new LabelResponseDTO(1L, "Focus", testUser.getUsername()));
            when(labelMapper.toResponseDTO(label2)).thenReturn(new LabelResponseDTO(2L, "Strength", testUser.getUsername()));

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

        @Test
        void shouldHandleUserWithNoLabels() {
            // Arrange
            Long userId = testUser.getId();
            
            when(userBO.getUserById(userId)).thenReturn(Optional.of(mockedUser));
            when(labelRepository.findAllByCreatorIdOrderByNameAsc(userId)).thenReturn(Collections.emptyList());

            // Act
            List<LabelResponseDTO> result = labelService.getLabelsByUser(userId);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            // Arrange
            Long userId = 999L;
            when(userBO.getUserById(userId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(UserNotFoundException.class, () -> labelService.getLabelsByUser(userId));
        }

        @Test
        void shouldHandleUserWithOnlyUnlabeledLabel() {
            // Arrange
            Long userId = testUser.getId();
            // mockedUser is already a @Mock field
            Label unlabeled = TestUtils.createValidLabelWithId(1L, testUser);
            unlabeled.setName("Unlabeled");
            
            when(mockedUser.getUnlabeled()).thenReturn(unlabeled);
            when(userBO.getUserById(userId)).thenReturn(Optional.of(mockedUser));
            when(labelRepository.findAllByCreatorIdOrderByNameAsc(userId))
                .thenReturn(List.of(unlabeled)); // Only unlabeled label exists

            // Act
            List<LabelResponseDTO> result = labelService.getLabelsByUser(userId);

            // Assert
            assertThat(result).isEmpty(); // Unlabeled should be filtered out
        }

        @Test
        void shouldMaintainAlphabeticalOrderWithMixedCase() {
            // Arrange
            Long userId = testUser.getId();
            // mockedUser is already a @Mock field
            Label unlabeled = TestUtils.createValidLabelWithId(1L, testUser);
            unlabeled.setName("Unlabeled");
            
            Label labelA = TestUtils.createValidLabelWithId(2L, testUser);
            labelA.setName("apple");
            Label labelB = TestUtils.createValidLabelWithId(3L, testUser);
            labelB.setName("Banana");
            Label labelC = TestUtils.createValidLabelWithId(4L, testUser);
            labelC.setName("Cherry");
            
            when(mockedUser.getUnlabeled()).thenReturn(unlabeled);
            when(userBO.getUserById(userId)).thenReturn(Optional.of(mockedUser));
            // Repository returns in the order it would sort alphabetically
            when(labelRepository.findAllByCreatorIdOrderByNameAsc(userId))
                .thenReturn(List.of(labelB, labelC, labelA, unlabeled));

            when(labelMapper.toResponseDTO(labelA)).thenReturn(new LabelResponseDTO(2L, "apple", testUser.getUsername()));
            when(labelMapper.toResponseDTO(labelB)).thenReturn(new LabelResponseDTO(3L, "Banana", testUser.getUsername()));
            when(labelMapper.toResponseDTO(labelC)).thenReturn(new LabelResponseDTO(4L, "Cherry", testUser.getUsername()));

            // Act
            List<LabelResponseDTO> result = labelService.getLabelsByUser(userId);

            // Assert
            assertThat(result)
                .extracting("name")
                .containsExactly("Banana", "Cherry", "apple"); // Verify alphabetical order as returned by repository
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

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
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
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
            when(labelRepository.existsByNameAndCreator(dto.name(), testUser)).thenReturn(true);

            // Act + Assert
            assertThrows(LabelException.class, () -> labelService.createLabel(dto));
        }

        @Test
        void shouldHandleSpecialCharactersInName() {
            // Arrange
            LabelCreateDTO dto = new LabelCreateDTO("Work/Life-Balance (2024)!");
            Label savedLabel = new Label(dto.name(), testUser);
            LabelResponseDTO expected = new LabelResponseDTO(1L, dto.name(), testUser.getUsername());

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
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
        void shouldHandleUnicodeCharacters() {
            // Arrange
            LabelCreateDTO dto = new LabelCreateDTO("学习中文 🎯");
            Label savedLabel = new Label(dto.name(), testUser);
            LabelResponseDTO expected = new LabelResponseDTO(1L, dto.name(), testUser.getUsername());

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
            when(labelRepository.existsByNameAndCreator(dto.name(), testUser)).thenReturn(false);
            when(labelRepository.save(any(Label.class))).thenReturn(savedLabel);
            when(labelMapper.toResponseDTO(savedLabel)).thenReturn(expected);

            // Act
            LabelResponseDTO result = labelService.createLabel(dto);

            // Assert
            assertEquals(expected.name(), result.name());
            verify(labelRepository).save(any(Label.class));
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

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
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

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
            when(labelRepository.findById(1L)).thenReturn(Optional.of(existing));
            doNothing().when(ownershipValidator).validateLabelOwnership(testUser.getId(), existing);
            when(labelRepository.existsByNameAndCreator("NewName", testUser)).thenReturn(true);

            // Act + Assert
            assertThrows(LabelException.class, () -> labelService.updateLabel(1L, dto));
        }

        @Test
        void shouldNotUpdateWhenNameIsUnchanged() {
            // Arrange
            Label existing = new Label("SameName", testUser);
            LabelUpdateDTO dto = new LabelUpdateDTO("SameName");
            LabelResponseDTO expected = new LabelResponseDTO(1L, "SameName", testUser.getUsername());

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
            when(labelRepository.findById(1L)).thenReturn(Optional.of(existing));
            doNothing().when(ownershipValidator).validateLabelOwnership(testUser.getId(), existing);
            when(labelMapper.toResponseDTO(existing)).thenReturn(expected);

            // Act
            LabelResponseDTO result = labelService.updateLabel(1L, dto);

            // Assert
            assertEquals(expected.name(), result.name());
            verify(labelRepository, never()).save(any(Label.class));
        }

        @Test
        void shouldThrowWhenLabelNotFound() {
            // Arrange
            LabelUpdateDTO dto = new LabelUpdateDTO("NewName");
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
            when(labelRepository.findById(1L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(LabelNotFoundException.class, () -> labelService.updateLabel(1L, dto));
        }

        @Test
        void shouldThrowWhenUserDoesNotOwnLabel() {
            // Arrange
            User otherUser = TestUtils.createValidUserEntityWithId(999L);
            Label existing = new Label("Focus", otherUser);
            LabelUpdateDTO dto = new LabelUpdateDTO("NewName");

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
            when(labelRepository.findById(1L)).thenReturn(Optional.of(existing));
            doThrow(new LabelOwnershipException(1L, testUser.getId()))
                .when(ownershipValidator).validateLabelOwnership(testUser.getId(), existing);

            // Act + Assert
            assertThrows(LabelOwnershipException.class, () -> labelService.updateLabel(1L, dto));
        }

        @Test
        void shouldThrowWhenTryingToUpdateSystemLabel() {
            // Arrange
            Label unlabeledLabel = TestUtils.createValidLabelWithId(1L, testUser);
            TestUtils.setUnlabeledLabel(testUser, unlabeledLabel);
            LabelUpdateDTO dto = new LabelUpdateDTO("NewName");

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
            when(labelRepository.findById(1L)).thenReturn(Optional.of(unlabeledLabel));
            doNothing().when(ownershipValidator).validateLabelOwnership(testUser.getId(), unlabeledLabel);

            // Act + Assert
            assertThrows(SystemManagedEntityException.class, () -> labelService.updateLabel(1L, dto));
        }

        @Test
        void shouldHandleNullNameInUpdateDTO() {
            // Arrange
            Label existing = new Label("OriginalName", testUser);
            LabelUpdateDTO dto = new LabelUpdateDTO(null); // null name should be ignored
            LabelResponseDTO expected = new LabelResponseDTO(1L, "OriginalName", testUser.getUsername());

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
            when(labelRepository.findById(1L)).thenReturn(Optional.of(existing));
            doNothing().when(ownershipValidator).validateLabelOwnership(testUser.getId(), existing);
            when(labelMapper.toResponseDTO(existing)).thenReturn(expected);

            // Act
            LabelResponseDTO result = labelService.updateLabel(1L, dto);

            // Assert
            assertEquals("OriginalName", result.name());
            verify(labelRepository, never()).save(any(Label.class)); // No save should occur
        }

        @Test
        void shouldHandleIdenticalNameInUpdateDTO() {
            // Arrange - This tests the specific branch: dto.name() != null but equals current name
            Label existing = new Label("ExactSameName", testUser);
            LabelUpdateDTO dto = new LabelUpdateDTO("ExactSameName"); // Same name as current
            LabelResponseDTO expected = new LabelResponseDTO(1L, "ExactSameName", testUser.getUsername());

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
            when(labelRepository.findById(1L)).thenReturn(Optional.of(existing));
            doNothing().when(ownershipValidator).validateLabelOwnership(testUser.getId(), existing);
            when(labelMapper.toResponseDTO(existing)).thenReturn(expected);

            // Act
            LabelResponseDTO result = labelService.updateLabel(1L, dto);

            // Assert
            assertEquals("ExactSameName", result.name());
            verify(labelRepository, never()).save(any(Label.class)); // No save should occur
            verify(labelRepository, never()).existsByNameAndCreator(any(), any()); // Should skip duplicate check
        }
    }

    @Nested
    class DeleteLabelTests {

        @Test
        void shouldDeleteLabelSuccessfully() {
            // Arrange
            Label label = new Label("Focus", testUser);
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
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
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
            when(labelRepository.findById(1L)).thenReturn(Optional.of(label));
            doNothing().when(ownershipValidator).validateLabelOwnership(testUser.getId(), label);

            // Act + Assert
            assertThrows(SystemManagedEntityException.class, () -> labelService.deleteLabel(1L));
        }

        @Test
        void shouldThrowWhenLabelNotFound() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
            when(labelRepository.findById(1L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(LabelNotFoundException.class, () -> labelService.deleteLabel(1L));
        }

        @Test
        void shouldThrowWhenUserDoesNotOwnLabel() {
            // Arrange
            User otherUser = TestUtils.createValidUserEntityWithId(999L);
            Label label = new Label("Focus", otherUser);
            
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
            when(labelRepository.findById(1L)).thenReturn(Optional.of(label));
            doThrow(new LabelOwnershipException(1L, testUser.getId()))
                .when(ownershipValidator).validateLabelOwnership(testUser.getId(), label);

            // Act + Assert
            assertThrows(LabelOwnershipException.class, () -> labelService.deleteLabel(1L));
            verify(labelRepository, never()).delete(any(Label.class));
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
            assertThrows(InvalidLabelAssociationException.class, () ->
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

        @Test
        void shouldDoNothing_whenInputIsNull() {
            // Act + Assert
            assertDoesNotThrow(() -> labelService.validateExistenceAndOwnership(null, testUser.getId()));
            verifyNoInteractions(labelRepository);
        }

        @Test
        void shouldValidatePartiallyExistingLabels() {
            // Arrange
            Long labelId1 = 1L;
            Long labelId2 = 2L;
            Long labelId3 = 999L; // This one doesn't exist
            Set<Long> labelIds = Set.of(labelId1, labelId2, labelId3);

            Label label1 = TestUtils.createValidLabelWithId(labelId1, testUser);
            Label label2 = TestUtils.createValidLabelWithId(labelId2, testUser);
            // label3 is missing from repository result

            when(labelRepository.findAllById(labelIds)).thenReturn(List.of(label1, label2));

            // Act + Assert
            assertThrows(InvalidLabelAssociationException.class, () ->
                    labelService.validateExistenceAndOwnership(labelIds, testUser.getId())
            );
        }

        @Test
        void shouldValidateMixedOwnershipScenarios() {
            // Arrange
            Long labelId1 = 1L;
            Long labelId2 = 2L;
            Set<Long> labelIds = Set.of(labelId1, labelId2);

            User otherUser = TestUtils.createValidUserEntityWithId(999L);
            Label label1 = TestUtils.createValidLabelWithId(labelId1, testUser); // Owned by testUser
            Label label2 = TestUtils.createValidLabelWithId(labelId2, otherUser); // Owned by otherUser

            when(labelRepository.findAllById(labelIds)).thenReturn(List.of(label1, label2));

            // Act + Assert
            assertThrows(LabelOwnershipException.class, () ->
                    labelService.validateExistenceAndOwnership(labelIds, testUser.getId())
            );
        }

        @Test
        void shouldValidateLargeCollectionEfficiently() {
            // Arrange - Test with larger collection to verify efficiency
            Set<Long> labelIds = Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
            List<Label> labels = labelIds.stream()
                .map(id -> TestUtils.createValidLabelWithId(id, testUser))
                .toList();

            when(labelRepository.findAllById(labelIds)).thenReturn(labels);

            // Act + Assert
            assertDoesNotThrow(() -> labelService.validateExistenceAndOwnership(labelIds, testUser.getId()));
            verify(labelRepository).findAllById(labelIds); // Should only call repository once
        }
    }

    @Nested
    class ErrorHandlingTests {

        @Test
        void shouldHandleRepositoryExceptionInGetLabelById() {
            // Arrange
            when(labelRepository.findById(1L)).thenThrow(new DataAccessException("Database error") {});

            // Act + Assert
            assertThrows(DataAccessException.class, () -> labelService.getLabelById(1L));
        }

        @Test
        void shouldHandleAuthenticationFailureInCreateLabel() {
            // Arrange
            LabelCreateDTO dto = new LabelCreateDTO("Focus");
            when(authenticatedUserProvider.getCurrentUser()).thenThrow(new RuntimeException("Authentication failed"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> labelService.createLabel(dto));
        }

        @Test
        void shouldHandleMapperFailureInGetLabelById() {
            // Arrange
            Label label = new Label("Focus", testUser);
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
            when(labelRepository.findById(1L)).thenReturn(Optional.of(label));
            when(labelMapper.toResponseDTO(label)).thenThrow(new RuntimeException("Mapping failed"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> labelService.getLabelById(1L));
        }

        @Test
        void shouldHandleRepositoryExceptionInCreateLabel() {
            // Arrange
            LabelCreateDTO dto = new LabelCreateDTO("Focus");
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
            when(labelRepository.existsByNameAndCreator(dto.name(), testUser)).thenReturn(false);
            when(labelRepository.save(any(Label.class))).thenThrow(new DataAccessException("Save failed") {});

            // Act + Assert
            assertThrows(DataAccessException.class, () -> labelService.createLabel(dto));
        }

        @Test
        void shouldHandleRepositoryExceptionInUpdateLabel() {
            // Arrange
            Label existing = new Label("OldName", testUser);
            LabelUpdateDTO dto = new LabelUpdateDTO("NewName");
            
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
            when(labelRepository.findById(1L)).thenReturn(Optional.of(existing));
            doNothing().when(ownershipValidator).validateLabelOwnership(testUser.getId(), existing);
            when(labelRepository.existsByNameAndCreator("NewName", testUser)).thenReturn(false);
            when(labelRepository.save(existing)).thenThrow(new DataAccessException("Update failed") {});

            // Act + Assert
            assertThrows(DataAccessException.class, () -> labelService.updateLabel(1L, dto));
        }

        @Test
        void shouldHandleRepositoryExceptionInDeleteLabel() {
            // Arrange
            Label label = new Label("Focus", testUser);
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
            when(labelRepository.findById(1L)).thenReturn(Optional.of(label));
            doNothing().when(ownershipValidator).validateLabelOwnership(testUser.getId(), label);
            doThrow(new DataAccessException("Delete failed") {}).when(labelRepository).delete(label);

            // Act + Assert
            assertThrows(DataAccessException.class, () -> labelService.deleteLabel(1L));
        }
    }

    @Nested
    class TransactionalBehaviorTests {

        @Test
        void shouldHandleTransactionalReadOnlyInGetLabelsByUser() {
            // Arrange
            Long userId = testUser.getId();
            
            when(userBO.getUserById(userId)).thenReturn(Optional.of(mockedUser));
            when(labelRepository.findAllByCreatorIdOrderByNameAsc(userId)).thenReturn(Collections.emptyList());

            // Act & Assert - Should not throw any transaction-related exceptions
            assertDoesNotThrow(() -> labelService.getLabelsByUser(userId));
        }

        @Test
        void shouldRollbackOnUpdateFailure() {
            // Arrange
            Label existing = new Label("OldName", testUser);
            LabelUpdateDTO dto = new LabelUpdateDTO("NewName");
            
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
            when(labelRepository.findById(1L)).thenReturn(Optional.of(existing));
            doNothing().when(ownershipValidator).validateLabelOwnership(testUser.getId(), existing);
            when(labelRepository.existsByNameAndCreator("NewName", testUser)).thenReturn(false);
            // Simulate save failure after name is changed
            when(labelRepository.save(existing)).thenThrow(new DataAccessException("Transaction failed") {});

            // Act + Assert
            assertThrows(DataAccessException.class, () -> labelService.updateLabel(1L, dto));
            // Note: In a real transaction, the name change would be rolled back
        }
    }

    @Nested
    class ConcurrencyTests {

        @Test
        void shouldHandleConcurrentLabelCreation() {
            // Arrange - Simulate race condition where label gets created between existence check and save
            LabelCreateDTO dto = new LabelCreateDTO("Focus");
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
            when(labelRepository.existsByNameAndCreator(dto.name(), testUser)).thenReturn(false);
            when(labelRepository.save(any(Label.class))).thenThrow(new DataAccessException("Unique constraint violation") {});

            // Act + Assert
            assertThrows(DataAccessException.class, () -> labelService.createLabel(dto));
        }

        @Test
        void shouldHandleConcurrentLabelDeletion() {
            // Arrange - Label exists during lookup but gets deleted before our delete attempt
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
            when(labelRepository.findById(1L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(LabelNotFoundException.class, () -> labelService.deleteLabel(1L));
        }
    }
}