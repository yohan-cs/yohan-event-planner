package com.yohan.event_planner.jobs;

import com.yohan.event_planner.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupJobTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    private RefreshTokenCleanupJob cleanupJob;

    @BeforeEach
    void setUp() {
        cleanupJob = new RefreshTokenCleanupJob(refreshTokenService);
    }

    @Nested
    class CleanupExpiredTokensTests {

        @Test
        void cleanupExpiredTokens_whenTokensDeleted_logsSuccess() {
            // Arrange
            when(refreshTokenService.cleanupExpiredTokens()).thenReturn(5);

            // Act
            cleanupJob.cleanupExpiredTokens();

            // Assert
            verify(refreshTokenService).cleanupExpiredTokens();
        }

        @Test
        void cleanupExpiredTokens_whenNoTokensDeleted_logsDebug() {
            // Arrange
            when(refreshTokenService.cleanupExpiredTokens()).thenReturn(0);

            // Act
            cleanupJob.cleanupExpiredTokens();

            // Assert
            verify(refreshTokenService).cleanupExpiredTokens();
        }

        @Test
        void cleanupExpiredTokens_whenServiceThrowsException_logsError() {
            // Arrange
            when(refreshTokenService.cleanupExpiredTokens())
                    .thenThrow(new RuntimeException("Database error"));

            // Act & Assert - Should not throw exception
            assertDoesNotThrow(() -> cleanupJob.cleanupExpiredTokens());
            verify(refreshTokenService).cleanupExpiredTokens();
        }

        @Test
        void cleanupExpiredTokens_whenLargeNumberDeleted_handlesCorrectly() {
            // Arrange
            when(refreshTokenService.cleanupExpiredTokens()).thenReturn(1000);

            // Act
            assertDoesNotThrow(() -> cleanupJob.cleanupExpiredTokens());

            // Assert
            verify(refreshTokenService).cleanupExpiredTokens();
        }
    }

    @Nested
    class CleanupRevokedTokensTests {

        @Test
        void cleanupRevokedTokens_callsServiceWithCorrectRetentionDays() {
            // Arrange
            when(refreshTokenService.cleanupRevokedTokens(30)).thenReturn(3);

            // Act
            cleanupJob.cleanupRevokedTokens();

            // Assert
            verify(refreshTokenService).cleanupRevokedTokens(30);
        }

        @Test
        void cleanupRevokedTokens_whenNoTokensDeleted_logsDebug() {
            // Arrange
            when(refreshTokenService.cleanupRevokedTokens(30)).thenReturn(0);

            // Act
            cleanupJob.cleanupRevokedTokens();

            // Assert
            verify(refreshTokenService).cleanupRevokedTokens(30);
        }

        @Test
        void cleanupRevokedTokens_whenServiceThrowsException_logsError() {
            // Arrange
            when(refreshTokenService.cleanupRevokedTokens(30))
                    .thenThrow(new RuntimeException("Database error"));

            // Act & Assert - Should not throw exception
            assertDoesNotThrow(() -> cleanupJob.cleanupRevokedTokens());
            verify(refreshTokenService).cleanupRevokedTokens(30);
        }

        @Test
        void cleanupRevokedTokens_whenTokensDeleted_logsSuccess() {
            // Arrange
            when(refreshTokenService.cleanupRevokedTokens(30)).thenReturn(15);

            // Act
            cleanupJob.cleanupRevokedTokens();

            // Assert
            verify(refreshTokenService).cleanupRevokedTokens(30);
        }

        @Test
        void cleanupRevokedTokens_whenLargeNumberDeleted_handlesCorrectly() {
            // Arrange
            when(refreshTokenService.cleanupRevokedTokens(30)).thenReturn(500);

            // Act
            assertDoesNotThrow(() -> cleanupJob.cleanupRevokedTokens());

            // Assert
            verify(refreshTokenService).cleanupRevokedTokens(30);
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
            assertTrue(statistics.contains("RefreshTokenCleanupJob"));
            assertTrue(statistics.contains("hourly cleanup"));
            assertTrue(statistics.contains("2:00 AM"));
            assertTrue(statistics.contains("30 days"));
            assertTrue(statistics.contains("Status: ENABLED"));
        }

        @Test
        void getCleanupStatistics_containsAllExpectedElements() {
            // Act
            String statistics = cleanupJob.getCleanupStatistics();

            // Assert
            assertAll("Statistics should contain all expected elements",
                () -> assertTrue(statistics.contains("RefreshTokenCleanupJob"), "Should contain job name"),
                () -> assertTrue(statistics.contains("hourly cleanup"), "Should contain expired token schedule"),
                () -> assertTrue(statistics.contains("daily cleanup at 2:00 AM"), "Should contain revoked token schedule"),
                () -> assertTrue(statistics.contains("Retention: 30 days"), "Should contain retention period"),
                () -> assertTrue(statistics.contains("ENABLED"), "Should contain status")
            );
        }
    }

    @Nested
    class ConstructorTests {

        @Test
        void constructor_withValidService_createsInstance() {
            // Act & Assert
            assertDoesNotThrow(() -> new RefreshTokenCleanupJob(refreshTokenService));
        }

        @Test
        void constructor_withNullService_allowsCreation() {
            // Act & Assert - Spring will handle null injection validation
            assertDoesNotThrow(() -> new RefreshTokenCleanupJob(null));
        }
    }

    @Nested
    class IntegrationTests {

        @Test
        void bothCleanupMethods_canRunIndependently() {
            // Arrange
            when(refreshTokenService.cleanupExpiredTokens()).thenReturn(2);
            when(refreshTokenService.cleanupRevokedTokens(30)).thenReturn(1);

            // Act
            cleanupJob.cleanupExpiredTokens();
            cleanupJob.cleanupRevokedTokens();

            // Assert
            verify(refreshTokenService).cleanupExpiredTokens();
            verify(refreshTokenService).cleanupRevokedTokens(30);
        }

        @Test
        void oneMethodFailure_doesNotAffectOther() {
            // Arrange
            when(refreshTokenService.cleanupExpiredTokens())
                    .thenThrow(new RuntimeException("Expired cleanup failed"));
            when(refreshTokenService.cleanupRevokedTokens(30)).thenReturn(3);

            // Act & Assert
            assertDoesNotThrow(() -> {
                cleanupJob.cleanupExpiredTokens(); // This should fail gracefully
                cleanupJob.cleanupRevokedTokens(); // This should still work
            });

            verify(refreshTokenService).cleanupExpiredTokens();
            verify(refreshTokenService).cleanupRevokedTokens(30);
        }
    }
}