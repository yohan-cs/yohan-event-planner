package com.yohan.event_planner.jobs;

import com.yohan.event_planner.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenCleanupJobTest {

    @Mock
    private PasswordResetService passwordResetService;

    private PasswordResetTokenCleanupJob cleanupJob;

    @BeforeEach
    void setUp() {
        cleanupJob = new PasswordResetTokenCleanupJob(passwordResetService);
    }

    @Nested
    class CleanupExpiredTokensTests {

        @Test
        void cleanupExpiredTokens_whenTokensDeleted_logsSuccess() {
            // Arrange
            when(passwordResetService.cleanupExpiredTokens()).thenReturn(5);

            // Act
            cleanupJob.cleanupExpiredTokens();

            // Assert
            verify(passwordResetService).cleanupExpiredTokens();
        }

        @Test
        void cleanupExpiredTokens_whenNoTokensDeleted_logsDebug() {
            // Arrange
            when(passwordResetService.cleanupExpiredTokens()).thenReturn(0);

            // Act
            cleanupJob.cleanupExpiredTokens();

            // Assert
            verify(passwordResetService).cleanupExpiredTokens();
        }

        @Test
        void cleanupExpiredTokens_whenServiceThrowsException_logsError() {
            // Arrange
            when(passwordResetService.cleanupExpiredTokens())
                    .thenThrow(new RuntimeException("Database error"));

            // Act & Assert - Should not throw exception
            assertDoesNotThrow(() -> cleanupJob.cleanupExpiredTokens());
            verify(passwordResetService).cleanupExpiredTokens();
        }
    }

    @Nested
    class PerformImmediateCleanupTests {

        @Test
        void performImmediateCleanup_whenSuccessful_returnsDeletedCount() {
            // Arrange
            when(passwordResetService.cleanupExpiredTokens()).thenReturn(3);

            // Act
            int result = cleanupJob.performImmediateCleanup();

            // Assert
            assertEquals(3, result);
            verify(passwordResetService).cleanupExpiredTokens();
        }

        @Test
        void performImmediateCleanup_whenNoTokensDeleted_returnsZero() {
            // Arrange
            when(passwordResetService.cleanupExpiredTokens()).thenReturn(0);

            // Act
            int result = cleanupJob.performImmediateCleanup();

            // Assert
            assertEquals(0, result);
            verify(passwordResetService).cleanupExpiredTokens();
        }

        @Test
        void performImmediateCleanup_whenServiceThrowsException_throwsRuntimeException() {
            // Arrange
            when(passwordResetService.cleanupExpiredTokens())
                    .thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            RuntimeException thrown = assertThrows(RuntimeException.class, 
                    () -> cleanupJob.performImmediateCleanup());
            
            assertEquals("Cleanup operation failed", thrown.getMessage());
            assertEquals("Database error", thrown.getCause().getMessage());
            verify(passwordResetService).cleanupExpiredTokens();
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
            assertTrue(statistics.contains("PasswordResetTokenCleanupJob"));
            assertTrue(statistics.contains("every 30 minutes"));
            assertTrue(statistics.contains("Initial delay: 5 minutes"));
            assertTrue(statistics.contains("Status: ENABLED"));
        }
    }

    @Nested
    class ConstructorTests {

        @Test
        void constructor_withValidService_createsInstance() {
            // Act & Assert
            assertDoesNotThrow(() -> new PasswordResetTokenCleanupJob(passwordResetService));
        }

        @Test
        void constructor_withNullService_allowsCreation() {
            // Act & Assert - Spring will handle null injection validation
            assertDoesNotThrow(() -> new PasswordResetTokenCleanupJob(null));
        }
    }
}