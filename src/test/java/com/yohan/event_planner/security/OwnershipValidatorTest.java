package com.yohan.event_planner.security;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.exception.EventOwnershipException;
import com.yohan.event_planner.exception.UserOwnershipException;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_EVENT_ACCESS;
import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_USER_ACCESS;
import static org.junit.jupiter.api.Assertions.*;

public class OwnershipValidatorTest {

    private OwnershipValidator validator;

    @BeforeEach
    void setUp() {
        validator = new OwnershipValidator();
    }

    @Nested
    class ValidateEventOwnershipTests {

        @Test
        void testValidateEventOwnership_idsMatch_noExceptionThrown() {
            // Arrange
            Long userId = 1L;
            User user = TestUtils.createUserEntityWithId(userId);
            Event event = TestUtils.createTimedEventEntityWithId(100L, user);

            // Act + Assert
            assertDoesNotThrow(() -> validator.validateEventOwnership(userId, event));
        }

        @Test
        void testValidateEventOwnership_idsMismatch_throwsException() {
            // Arrange
            Long userId = 1L;
            User eventCreator = TestUtils.createUserEntityWithId(2L);
            Event event = TestUtils.createTimedEventEntityWithId(100L, eventCreator);

            // Act + Assert
            EventOwnershipException ex = assertThrows(EventOwnershipException.class,
                    () -> validator.validateEventOwnership(userId, event));

            assertEquals(UNAUTHORIZED_EVENT_ACCESS, ex.getErrorCode());
            assertEquals(100L, event.getId());
        }
    }

    @Nested
    class ValidateUserOwnershipTests {

        @Test
        void testValidateUserOwnership_idsMatch_noExceptionThrown() {
            // Arrange
            Long userId = 5L;

            // Act + Assert
            assertDoesNotThrow(() -> validator.validateUserOwnership(userId, userId));
        }

        @Test
        void testValidateUserOwnership_idsMismatch_throwsException() {
            // Arrange
            Long currentUserId = 10L;
            Long targetUserId = 20L;

            // Act + Assert
            UserOwnershipException ex = assertThrows(UserOwnershipException.class,
                    () -> validator.validateUserOwnership(currentUserId, targetUserId));

            assertEquals(UNAUTHORIZED_USER_ACCESS, ex.getErrorCode());
            assertTrue(ex.getMessage().contains(String.valueOf(targetUserId)));
        }

    }
}
