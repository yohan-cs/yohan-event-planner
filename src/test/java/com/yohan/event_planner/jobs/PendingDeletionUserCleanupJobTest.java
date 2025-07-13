package com.yohan.event_planner.jobs;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.repository.UserRepository;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentCaptor.forClass;

class PendingDeletionUserCleanupJobTest {

    private UserRepository userRepository;
    private PendingDeletionUserCleanupJob pendingDeletionUserCleanupJob;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        pendingDeletionUserCleanupJob = new PendingDeletionUserCleanupJob(userRepository);
    }

    @Nested
    class DeleteExpiredUsersTests {

        @Test
        void testDeleteExpiredUsers_deletesEligibleUsers() {
            // Arrange
            User user1 = TestUtils.createValidUserEntityWithId();
            User user2 = TestUtils.createValidUserEntityWithId();
            List<User> expiredUsers = List.of(user1, user2);

            when(userRepository.findAllByIsPendingDeletionTrueAndScheduledDeletionDateBefore(any(ZonedDateTime.class)))
                    .thenReturn(expiredUsers);

            // Act
            pendingDeletionUserCleanupJob.deleteExpiredUsers();

            // Assert
            verify(userRepository).deleteAll(expiredUsers);
        }

        @Test
        void testDeleteExpiredUsers_doesNothingIfNoUsers() {
            // Arrange
            when(userRepository.findAllByIsPendingDeletionTrueAndScheduledDeletionDateBefore(any(ZonedDateTime.class)))
                    .thenReturn(List.of());

            // Act
            pendingDeletionUserCleanupJob.deleteExpiredUsers();

            // Assert
            verify(userRepository, never()).deleteAll(any());
        }

        @Test
        void testDeleteExpiredUsers_whenRepositoryThrowsException_logsErrorAndRethrows() {
            // Arrange
            when(userRepository.findAllByIsPendingDeletionTrueAndScheduledDeletionDateBefore(any(ZonedDateTime.class)))
                    .thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class, 
                    () -> pendingDeletionUserCleanupJob.deleteExpiredUsers());
            
            assertEquals("Database error", thrown.getMessage());
            verify(userRepository).findAllByIsPendingDeletionTrueAndScheduledDeletionDateBefore(any(ZonedDateTime.class));
        }

        @Test
        void testDeleteExpiredUsers_usesCorrectCutoffTime() {
            // Arrange
            ZonedDateTime testStartTime = ZonedDateTime.now(java.time.ZoneOffset.UTC);
            when(userRepository.findAllByIsPendingDeletionTrueAndScheduledDeletionDateBefore(any(ZonedDateTime.class)))
                    .thenReturn(List.of());
            
            ArgumentCaptor<ZonedDateTime> timeCaptor = forClass(ZonedDateTime.class);

            // Act
            pendingDeletionUserCleanupJob.deleteExpiredUsers();

            // Assert
            verify(userRepository).findAllByIsPendingDeletionTrueAndScheduledDeletionDateBefore(timeCaptor.capture());
            
            ZonedDateTime capturedTime = timeCaptor.getValue();
            assertTrue(capturedTime.isAfter(testStartTime.minusMinutes(1)), 
                "Captured time should be close to current time");
            assertTrue(capturedTime.isBefore(testStartTime.plusMinutes(1)), 
                "Captured time should be close to current time");
        }

        @Test
        void testDeleteExpiredUsers_whenLargeNumberOfUsers_handlesCorrectly() {
            // Arrange
            List<User> largeUserList = java.util.stream.IntStream.range(0, 100)
                    .mapToObj(i -> TestUtils.createValidUserEntityWithId())
                    .collect(java.util.stream.Collectors.toList());
            
            when(userRepository.findAllByIsPendingDeletionTrueAndScheduledDeletionDateBefore(any(ZonedDateTime.class)))
                    .thenReturn(largeUserList);

            // Act
            assertDoesNotThrow(() -> pendingDeletionUserCleanupJob.deleteExpiredUsers());

            // Assert
            verify(userRepository).deleteAll(largeUserList);
        }
    }

    @Nested
    class GetCleanupStatisticsTests {

        @Test
        void getCleanupStatistics_returnsFormattedString() {
            // Act
            String statistics = pendingDeletionUserCleanupJob.getCleanupStatistics();

            // Assert
            assertNotNull(statistics);
            assertTrue(statistics.contains("PendingDeletionUserCleanupJob"));
            assertTrue(statistics.contains("daily at 3:00 AM UTC"));
            assertTrue(statistics.contains("Grace period: 30 days"));
            assertTrue(statistics.contains("Status: ENABLED"));
        }

        @Test
        void getCleanupStatistics_containsAllExpectedElements() {
            // Act
            String statistics = pendingDeletionUserCleanupJob.getCleanupStatistics();

            // Assert
            assertAll("Statistics should contain all expected elements",
                () -> assertTrue(statistics.contains("PendingDeletionUserCleanupJob"), "Should contain job name"),
                () -> assertTrue(statistics.contains("3:00 AM UTC"), "Should contain schedule time"),
                () -> assertTrue(statistics.contains("30 days"), "Should contain grace period"),
                () -> assertTrue(statistics.contains("ENABLED"), "Should contain status")
            );
        }
    }

    @Nested
    class ConstructorTests {

        @Test
        void constructor_withValidRepository_createsInstance() {
            // Act & Assert
            assertDoesNotThrow(() -> new PendingDeletionUserCleanupJob(userRepository));
        }

        @Test
        void constructor_withNullRepository_allowsCreation() {
            // Act & Assert - Spring will handle null injection validation
            assertDoesNotThrow(() -> new PendingDeletionUserCleanupJob(null));
        }
    }

}