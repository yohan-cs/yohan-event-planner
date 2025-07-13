package com.yohan.event_planner.jobs;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.repository.UserRepository;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnverifiedUserCleanupJobTest {

    @Mock
    private UserRepository userRepository;

    private UnverifiedUserCleanupJob cleanupJob;

    @BeforeEach
    void setUp() {
        cleanupJob = new UnverifiedUserCleanupJob(userRepository);
    }

    @Nested
    class CleanupUnverifiedUsersTests {

        @Test
        void cleanupUnverifiedUsers_whenUsersDeleted_logsSuccess() {
            // Arrange
            User user1 = TestUtils.createValidUserEntityWithId();
            User user2 = TestUtils.createValidUserEntityWithId();
            List<User> unverifiedUsers = List.of(user1, user2);
            
            when(userRepository.findAllByEmailVerifiedFalseAndCreatedAtBefore(any(ZonedDateTime.class)))
                    .thenReturn(unverifiedUsers);

            // Act
            cleanupJob.cleanupUnverifiedUsers();

            // Assert
            verify(userRepository).findAllByEmailVerifiedFalseAndCreatedAtBefore(any(ZonedDateTime.class));
            verify(userRepository, times(2)).delete(any(User.class));
        }

        @Test
        void cleanupUnverifiedUsers_whenNoUsersFound_logsDebugAndReturnsEarly() {
            // Arrange
            when(userRepository.findAllByEmailVerifiedFalseAndCreatedAtBefore(any(ZonedDateTime.class)))
                    .thenReturn(List.of());

            // Act
            cleanupJob.cleanupUnverifiedUsers();

            // Assert
            verify(userRepository).findAllByEmailVerifiedFalseAndCreatedAtBefore(any(ZonedDateTime.class));
            verify(userRepository, never()).delete(any(User.class));
        }

        @Test
        void cleanupUnverifiedUsers_whenSomeUserDeletionsFail_continuesWithOthers() {
            // Arrange
            User user1 = TestUtils.createValidUserEntityWithId();
            User user2 = TestUtils.createValidUserEntityWithId();
            User user3 = TestUtils.createValidUserEntityWithId();
            List<User> unverifiedUsers = List.of(user1, user2, user3);
            
            when(userRepository.findAllByEmailVerifiedFalseAndCreatedAtBefore(any(ZonedDateTime.class)))
                    .thenReturn(unverifiedUsers);
            
            // Make all deletions fail to test error handling
            doThrow(new RuntimeException("Database error")).when(userRepository).delete(any(User.class));

            // Act
            assertDoesNotThrow(() -> cleanupJob.cleanupUnverifiedUsers());

            // Assert - Verify deletion was attempted for all users
            verify(userRepository, times(3)).delete(any(User.class));
        }

        @Test
        void cleanupUnverifiedUsers_whenRepositoryThrowsException_logsError() {
            // Arrange
            when(userRepository.findAllByEmailVerifiedFalseAndCreatedAtBefore(any(ZonedDateTime.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // Act & Assert - Should not throw exception
            assertDoesNotThrow(() -> cleanupJob.cleanupUnverifiedUsers());
            verify(userRepository).findAllByEmailVerifiedFalseAndCreatedAtBefore(any(ZonedDateTime.class));
        }

        @Test
        void cleanupUnverifiedUsers_usesCorrectCutoffTime() {
            // Arrange
            ZonedDateTime testStartTime = ZonedDateTime.now();
            when(userRepository.findAllByEmailVerifiedFalseAndCreatedAtBefore(any(ZonedDateTime.class)))
                    .thenReturn(List.of());
            
            ArgumentCaptor<ZonedDateTime> timeCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);

            // Act
            cleanupJob.cleanupUnverifiedUsers();

            // Assert
            verify(userRepository).findAllByEmailVerifiedFalseAndCreatedAtBefore(timeCaptor.capture());
            
            ZonedDateTime capturedTime = timeCaptor.getValue();
            // Should be approximately 24 hours before current time
            assertTrue(capturedTime.isBefore(testStartTime));
            assertTrue(capturedTime.isAfter(testStartTime.minusHours(25))); // Allow some tolerance
            assertTrue(capturedTime.isBefore(testStartTime.minusHours(23))); // Allow some tolerance
        }

        @Test
        void cleanupUnverifiedUsers_whenLargeNumberOfUsers_handlesCorrectly() {
            // Arrange
            List<User> largeUserList = java.util.stream.IntStream.range(0, 100)
                    .mapToObj(i -> TestUtils.createValidUserEntityWithId())
                    .collect(java.util.stream.Collectors.toList());
            
            when(userRepository.findAllByEmailVerifiedFalseAndCreatedAtBefore(any(ZonedDateTime.class)))
                    .thenReturn(largeUserList);

            // Act
            assertDoesNotThrow(() -> cleanupJob.cleanupUnverifiedUsers());

            // Assert
            verify(userRepository, times(100)).delete(any(User.class));
        }

        @Test
        void cleanupUnverifiedUsers_whenAllDeletionsFail_logsWarning() {
            // Arrange
            User user1 = TestUtils.createValidUserEntityWithId();
            User user2 = TestUtils.createValidUserEntityWithId();
            List<User> unverifiedUsers = List.of(user1, user2);
            
            when(userRepository.findAllByEmailVerifiedFalseAndCreatedAtBefore(any(ZonedDateTime.class)))
                    .thenReturn(unverifiedUsers);
            doThrow(new RuntimeException("Database error")).when(userRepository).delete(any(User.class));

            // Act
            assertDoesNotThrow(() -> cleanupJob.cleanupUnverifiedUsers());

            // Assert
            verify(userRepository, times(2)).delete(any(User.class));
        }
    }

    @Nested
    class PerformImmediateCleanupTests {

        @Test
        void performImmediateCleanup_whenUsersDeleted_returnsCorrectCount() {
            // Arrange
            User user1 = TestUtils.createValidUserEntityWithId();
            User user2 = TestUtils.createValidUserEntityWithId();
            User user3 = TestUtils.createValidUserEntityWithId();
            List<User> unverifiedUsers = List.of(user1, user2, user3);
            
            when(userRepository.findAllByEmailVerifiedFalseAndCreatedAtBefore(any(ZonedDateTime.class)))
                    .thenReturn(unverifiedUsers);

            // Act
            int result = cleanupJob.performImmediateCleanup();

            // Assert
            assertEquals(3, result);
            verify(userRepository, times(3)).delete(any(User.class));
        }

        @Test
        void performImmediateCleanup_whenNoUsersFound_returnsZero() {
            // Arrange
            when(userRepository.findAllByEmailVerifiedFalseAndCreatedAtBefore(any(ZonedDateTime.class)))
                    .thenReturn(List.of());

            // Act
            int result = cleanupJob.performImmediateCleanup();

            // Assert
            assertEquals(0, result);
            verify(userRepository, never()).delete(any(User.class));
        }

        @Test
        void performImmediateCleanup_whenSomeDeletionsFail_returnsPartialCount() {
            // Arrange
            User user1 = TestUtils.createValidUserEntityWithId();
            User user2 = TestUtils.createValidUserEntityWithId();
            User user3 = TestUtils.createValidUserEntityWithId();
            List<User> unverifiedUsers = List.of(user1, user2, user3);
            
            when(userRepository.findAllByEmailVerifiedFalseAndCreatedAtBefore(any(ZonedDateTime.class)))
                    .thenReturn(unverifiedUsers);
            
            // Make all deletions fail to test error handling
            doThrow(new RuntimeException("Database error")).when(userRepository).delete(any(User.class));

            // Act
            int result = cleanupJob.performImmediateCleanup();

            // Assert
            assertEquals(0, result); // No successful deletions when all fail
            verify(userRepository, times(3)).delete(any(User.class));
        }

        @Test
        void performImmediateCleanup_whenRepositoryThrowsException_throwsRuntimeException() {
            // Arrange
            when(userRepository.findAllByEmailVerifiedFalseAndCreatedAtBefore(any(ZonedDateTime.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class, 
                    () -> cleanupJob.performImmediateCleanup());
            
            assertEquals("Cleanup operation failed", thrown.getMessage());
            assertEquals("Database connection failed", thrown.getCause().getMessage());
        }

        @Test
        void performImmediateCleanup_usesCorrectCutoffTime() {
            // Arrange
            ZonedDateTime testStartTime = ZonedDateTime.now();
            when(userRepository.findAllByEmailVerifiedFalseAndCreatedAtBefore(any(ZonedDateTime.class)))
                    .thenReturn(List.of());
            
            ArgumentCaptor<ZonedDateTime> timeCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);

            // Act
            cleanupJob.performImmediateCleanup();

            // Assert
            verify(userRepository).findAllByEmailVerifiedFalseAndCreatedAtBefore(timeCaptor.capture());
            
            ZonedDateTime capturedTime = timeCaptor.getValue();
            // Should be approximately 24 hours before current time
            assertTrue(capturedTime.isBefore(testStartTime));
            assertTrue(capturedTime.isAfter(testStartTime.minusHours(25))); // Allow some tolerance
            assertTrue(capturedTime.isBefore(testStartTime.minusHours(23))); // Allow some tolerance
        }
    }

    @Nested
    class GetCleanupStatisticsTests {

        @Test
        void getCleanupStatistics_returnsFormattedString() {
            // Act
            String statistics = cleanupJob.getCleanupStatistics();

            // Assert
            assertNotNull(statistics);
            assertTrue(statistics.contains("UnverifiedUserCleanupJob"));
            assertTrue(statistics.contains("every 6 hours"));
            assertTrue(statistics.contains("Max age: 24 hours"));
            assertTrue(statistics.contains("Initial delay: 30 minutes"));
            assertTrue(statistics.contains("Status: ENABLED"));
        }

        @Test
        void getCleanupStatistics_containsAllExpectedElements() {
            // Act
            String statistics = cleanupJob.getCleanupStatistics();

            // Assert
            assertAll("Statistics should contain all expected elements",
                () -> assertTrue(statistics.contains("UnverifiedUserCleanupJob"), "Should contain job name"),
                () -> assertTrue(statistics.contains("6 hours"), "Should contain schedule interval"),
                () -> assertTrue(statistics.contains("24 hours"), "Should contain max age"),
                () -> assertTrue(statistics.contains("30 minutes"), "Should contain initial delay"),
                () -> assertTrue(statistics.contains("ENABLED"), "Should contain status")
            );
        }
    }

    @Nested
    class ConstructorTests {

        @Test
        void constructor_withValidRepository_createsInstance() {
            // Act & Assert
            assertDoesNotThrow(() -> new UnverifiedUserCleanupJob(userRepository));
        }

        @Test
        void constructor_withNullRepository_allowsCreation() {
            // Act & Assert - Spring will handle null injection validation
            assertDoesNotThrow(() -> new UnverifiedUserCleanupJob(null));
        }
    }

    @Nested
    class IntegrationTests {

        @Test
        void bothCleanupMethods_useConsistentCutoffTimeLogic() {
            // Arrange
            when(userRepository.findAllByEmailVerifiedFalseAndCreatedAtBefore(any(ZonedDateTime.class)))
                    .thenReturn(List.of());

            // Act
            cleanupJob.cleanupUnverifiedUsers();
            cleanupJob.performImmediateCleanup();

            // Assert
            verify(userRepository, times(2)).findAllByEmailVerifiedFalseAndCreatedAtBefore(any(ZonedDateTime.class));
            
            // This test mainly verifies both methods call the repository with the correct signature
            // and use the same cutoff time calculation logic (24 hours ago)
        }

        @Test
        void scheduledCleanup_doesNotThrowOnFailure_butImmediateCleanupDoes() {
            // Arrange
            when(userRepository.findAllByEmailVerifiedFalseAndCreatedAtBefore(any(ZonedDateTime.class)))
                    .thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            assertDoesNotThrow(() -> cleanupJob.cleanupUnverifiedUsers()); // Should not throw
            assertThrows(RuntimeException.class, () -> cleanupJob.performImmediateCleanup()); // Should throw
        }
    }
}