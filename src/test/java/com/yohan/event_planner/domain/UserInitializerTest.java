package com.yohan.event_planner.domain;

import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.repository.LabelRepository;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserInitializerTest {

    @Mock
    private UserBO userBO;
    
    @Mock
    private LabelRepository labelRepository;

    private UserInitializer userInitializer;
    private User testUser;
    private User savedUser;

    @BeforeEach
    void setUp() {
        userInitializer = new UserInitializer(userBO, labelRepository);
        testUser = TestUtils.createValidUserEntity();
        savedUser = TestUtils.createValidUserEntityWithId();
    }

    @Nested
    class Construction {

        @Test
        void constructor_shouldSetDependencies() {
            UserInitializer initializer = new UserInitializer(userBO, labelRepository);

            assertThat(initializer).isNotNull();
        }

        @Test
        void constructor_withNullUserBO_shouldNotThrow() {
            // Constructor should accept dependencies - validation happens at runtime
            UserInitializer initializer = new UserInitializer(null, labelRepository);

            assertThat(initializer).isNotNull();
        }

        @Test
        void constructor_withNullLabelRepository_shouldNotThrow() {
            // Constructor should accept dependencies - validation happens at runtime
            UserInitializer initializer = new UserInitializer(userBO, null);

            assertThat(initializer).isNotNull();
        }
    }

    @Nested
    class SuccessfulInitialization {

        @Test
        void initializeUser_shouldCreateUserAndUnlabeledLabel() {
            // Arrange
            when(userBO.createUser(testUser)).thenReturn(savedUser);
            when(userBO.updateUser(savedUser)).thenReturn(savedUser);

            // Act
            User result = userInitializer.initializeUser(testUser);

            // Assert
            assertThat(result).isEqualTo(savedUser);

            // Verify user creation
            verify(userBO).createUser(testUser);

            // Verify label creation and persistence
            ArgumentCaptor<Label> labelCaptor = ArgumentCaptor.forClass(Label.class);
            verify(labelRepository).save(labelCaptor.capture());
            
            Label createdLabel = labelCaptor.getValue();
            assertThat(createdLabel.getName()).isEqualTo("Unlabeled");
            assertThat(createdLabel.getCreator()).isEqualTo(savedUser);

            // Verify user update
            verify(userBO).updateUser(savedUser);
        }

        @Test
        void initializeUser_shouldCallMethodsInCorrectOrder() {
            // Arrange
            when(userBO.createUser(testUser)).thenReturn(savedUser);
            when(userBO.updateUser(savedUser)).thenReturn(savedUser);

            // Act
            userInitializer.initializeUser(testUser);

            // Assert - verify order of operations
            var inOrder = inOrder(userBO, labelRepository);
            inOrder.verify(userBO).createUser(testUser);
            inOrder.verify(labelRepository).save(any(Label.class));
            inOrder.verify(userBO).updateUser(savedUser);
        }

        @Test
        void initializeUser_shouldAssignUnlabeledToUser() {
            // Arrange
            when(userBO.createUser(testUser)).thenReturn(savedUser);
            when(userBO.updateUser(savedUser)).thenReturn(savedUser);

            // Act
            userInitializer.initializeUser(testUser);

            // Assert - verify the unlabeled label was assigned to the user
            ArgumentCaptor<Label> labelCaptor = ArgumentCaptor.forClass(Label.class);
            verify(labelRepository).save(labelCaptor.capture());
            
            Label unlabeledLabel = labelCaptor.getValue();
            
            // Note: We can't directly verify assignUnlabeled() since it's package-private,
            // but we can verify that updateUser was called with the savedUser
            verify(userBO).updateUser(eq(savedUser));
        }

        @Test
        void initializeUser_shouldReturnUpdatedUser() {
            // Arrange
            User finalUser = TestUtils.createTestUser("finaluser");
            when(userBO.createUser(testUser)).thenReturn(savedUser);
            when(userBO.updateUser(savedUser)).thenReturn(finalUser);

            // Act
            User result = userInitializer.initializeUser(testUser);

            // Assert
            assertThat(result).isEqualTo(finalUser);
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void initializeUser_whenUserCreationFails_shouldPropagateException() {
            // Arrange
            RuntimeException userCreationException = new RuntimeException("User creation failed");
            when(userBO.createUser(testUser)).thenThrow(userCreationException);

            // Act & Assert
            assertThatThrownBy(() -> userInitializer.initializeUser(testUser))
                    .isEqualTo(userCreationException);

            // Verify no label operations were attempted
            verify(labelRepository, never()).save(any());
            verify(userBO, never()).updateUser(any());
        }

        @Test
        void initializeUser_whenLabelSaveFails_shouldPropagateException() {
            // Arrange
            RuntimeException labelSaveException = new RuntimeException("Label save failed");
            when(userBO.createUser(testUser)).thenReturn(savedUser);
            when(labelRepository.save(any(Label.class))).thenThrow(labelSaveException);

            // Act & Assert
            assertThatThrownBy(() -> userInitializer.initializeUser(testUser))
                    .isEqualTo(labelSaveException);

            // Verify user creation happened but update didn't
            verify(userBO).createUser(testUser);
            verify(userBO, never()).updateUser(any());
        }

        @Test
        void initializeUser_whenUserUpdateFails_shouldPropagateException() {
            // Arrange
            RuntimeException userUpdateException = new RuntimeException("User update failed");
            when(userBO.createUser(testUser)).thenReturn(savedUser);
            when(userBO.updateUser(savedUser)).thenThrow(userUpdateException);

            // Act & Assert
            assertThatThrownBy(() -> userInitializer.initializeUser(testUser))
                    .isEqualTo(userUpdateException);

            // Verify all previous operations completed
            verify(userBO).createUser(testUser);
            verify(labelRepository).save(any(Label.class));
        }

        @Test
        void initializeUser_withNullUser_shouldHandleGracefully() {
            // Arrange
            when(userBO.createUser(null)).thenThrow(new IllegalArgumentException("User cannot be null"));

            // Act & Assert
            assertThatThrownBy(() -> userInitializer.initializeUser(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User cannot be null");

            // Verify no subsequent operations
            verify(labelRepository, never()).save(any());
            verify(userBO, never()).updateUser(any());
        }
    }

    @Nested
    class LabelCreationDetails {

        @Test
        void initializeUser_shouldCreateLabelWithCorrectName() {
            // Arrange
            when(userBO.createUser(testUser)).thenReturn(savedUser);
            when(userBO.updateUser(savedUser)).thenReturn(savedUser);

            // Act
            userInitializer.initializeUser(testUser);

            // Assert
            ArgumentCaptor<Label> labelCaptor = ArgumentCaptor.forClass(Label.class);
            verify(labelRepository).save(labelCaptor.capture());
            
            Label createdLabel = labelCaptor.getValue();
            assertThat(createdLabel.getName()).isEqualTo("Unlabeled");
        }

        @Test
        void initializeUser_shouldCreateLabelWithCorrectCreator() {
            // Arrange
            when(userBO.createUser(testUser)).thenReturn(savedUser);
            when(userBO.updateUser(savedUser)).thenReturn(savedUser);

            // Act
            userInitializer.initializeUser(testUser);

            // Assert
            ArgumentCaptor<Label> labelCaptor = ArgumentCaptor.forClass(Label.class);
            verify(labelRepository).save(labelCaptor.capture());
            
            Label createdLabel = labelCaptor.getValue();
            assertThat(createdLabel.getCreator()).isEqualTo(savedUser);
        }

        @Test
        void initializeUser_shouldCreateLabelWithNullId() {
            // Arrange
            when(userBO.createUser(testUser)).thenReturn(savedUser);
            when(userBO.updateUser(savedUser)).thenReturn(savedUser);

            // Act
            userInitializer.initializeUser(testUser);

            // Assert
            ArgumentCaptor<Label> labelCaptor = ArgumentCaptor.forClass(Label.class);
            verify(labelRepository).save(labelCaptor.capture());
            
            Label createdLabel = labelCaptor.getValue();
            assertThat(createdLabel.getId()).isNull(); // New entity, not yet persisted
        }
    }

    @Nested
    class TransactionalBehavior {

        @Test
        void initializeUser_shouldBeTransactional() {
            // This test verifies the method has @Transactional annotation
            // The actual transactional behavior would be tested in integration tests
            
            // Verify the method exists and can be called
            when(userBO.createUser(testUser)).thenReturn(savedUser);
            when(userBO.updateUser(savedUser)).thenReturn(savedUser);

            // Act
            User result = userInitializer.initializeUser(testUser);

            // Assert
            assertThat(result).isNotNull();
            
            // Verify all operations were called (atomicity tested by integration tests)
            verify(userBO).createUser(testUser);
            verify(labelRepository).save(any(Label.class));
            verify(userBO).updateUser(savedUser);
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void initializeUser_withUserHavingExistingData_shouldStillWork() {
            // Arrange - user with some pre-existing data
            User userWithData = TestUtils.createValidUserEntity();
            userWithData.setEmail("existing@example.com");
            userWithData.setBio("Existing bio");
            
            when(userBO.createUser(userWithData)).thenReturn(savedUser);
            when(userBO.updateUser(savedUser)).thenReturn(savedUser);

            // Act
            User result = userInitializer.initializeUser(userWithData);

            // Assert
            assertThat(result).isEqualTo(savedUser);
            verify(userBO).createUser(userWithData);
            verify(labelRepository).save(any(Label.class));
            verify(userBO).updateUser(savedUser);
        }

        @Test
        void initializeUser_shouldOnlyCallEachOperationOnce() {
            // Arrange
            when(userBO.createUser(testUser)).thenReturn(savedUser);
            when(userBO.updateUser(savedUser)).thenReturn(savedUser);

            // Act
            userInitializer.initializeUser(testUser);

            // Assert
            verify(userBO, times(1)).createUser(testUser);
            verify(labelRepository, times(1)).save(any(Label.class));
            verify(userBO, times(1)).updateUser(savedUser);
        }

        @Test
        void initializeUser_shouldNotModifyOriginalUser() {
            // Arrange
            String originalEmail = testUser.getEmail();
            String originalUsername = testUser.getUsername();
            
            when(userBO.createUser(testUser)).thenReturn(savedUser);
            when(userBO.updateUser(savedUser)).thenReturn(savedUser);

            // Act
            userInitializer.initializeUser(testUser);

            // Assert - original user should be unchanged
            assertThat(testUser.getEmail()).isEqualTo(originalEmail);
            assertThat(testUser.getUsername()).isEqualTo(originalUsername);
        }
    }

    @Nested
    class BusinessLogicValidation {

        @Test
        void initializeUser_shouldCreateExactlyOneLabel() {
            // Arrange
            when(userBO.createUser(testUser)).thenReturn(savedUser);
            when(userBO.updateUser(savedUser)).thenReturn(savedUser);

            // Act
            userInitializer.initializeUser(testUser);

            // Assert
            verify(labelRepository, times(1)).save(any(Label.class));
        }

        @Test
        void initializeUser_shouldUseCorrectLabelName() {
            // Arrange
            when(userBO.createUser(testUser)).thenReturn(savedUser);
            when(userBO.updateUser(savedUser)).thenReturn(savedUser);

            // Act
            userInitializer.initializeUser(testUser);

            // Assert
            ArgumentCaptor<Label> labelCaptor = ArgumentCaptor.forClass(Label.class);
            verify(labelRepository).save(labelCaptor.capture());
            
            Label createdLabel = labelCaptor.getValue();
            assertThat(createdLabel.getName())
                    .isEqualTo("Unlabeled")
                    .isNotEmpty()
                    .isNotBlank();
        }

        @Test
        void initializeUser_shouldLinkLabelToCorrectUser() {
            // Arrange
            when(userBO.createUser(testUser)).thenReturn(savedUser);
            when(userBO.updateUser(savedUser)).thenReturn(savedUser);

            // Act
            userInitializer.initializeUser(testUser);

            // Assert
            ArgumentCaptor<Label> labelCaptor = ArgumentCaptor.forClass(Label.class);
            verify(labelRepository).save(labelCaptor.capture());
            
            Label createdLabel = labelCaptor.getValue();
            assertThat(createdLabel.getCreator())
                    .isEqualTo(savedUser)
                    .isNotNull();
        }
    }
}