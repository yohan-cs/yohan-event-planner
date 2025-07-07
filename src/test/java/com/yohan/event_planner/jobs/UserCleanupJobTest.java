package com.yohan.event_planner.jobs;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.repository.UserRepository;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserCleanupJobTest {

    private UserRepository userRepository;
    private UserCleanupJob userCleanupJob;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userCleanupJob = new UserCleanupJob(userRepository);
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
            userCleanupJob.deleteExpiredUsers();

            // Assert
            verify(userRepository).deleteAll(expiredUsers);
        }

        @Test
        void testDeleteExpiredUsers_doesNothingIfNoUsers() {
            // Arrange
            when(userRepository.findAllByIsPendingDeletionTrueAndScheduledDeletionDateBefore(any(ZonedDateTime.class)))
                    .thenReturn(List.of());

            // Act
            userCleanupJob.deleteExpiredUsers();

            // Assert
            verify(userRepository, never()).deleteAll(any());
        }
    }

}
