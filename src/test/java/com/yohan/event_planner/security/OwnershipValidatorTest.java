package com.yohan.event_planner.security;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.exception.EventOwnershipException;
import com.yohan.event_planner.exception.LabelOwnershipException;
import com.yohan.event_planner.exception.RecurringEventOwnershipException;
import com.yohan.event_planner.exception.UserOwnershipException;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_EVENT_ACCESS;
import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_LABEL_ACCESS;
import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_RECURRING_EVENT_ACCESS;
import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_USER_ACCESS;
import static com.yohan.event_planner.util.TestConstants.EVENT_ID;
import static com.yohan.event_planner.util.TestConstants.OTHER_BADGE_ID;
import static com.yohan.event_planner.util.TestConstants.USER_ID;
import static com.yohan.event_planner.util.TestConstants.USER_ID_OTHER;
import static com.yohan.event_planner.util.TestConstants.VALID_BADGE_ID;
import static com.yohan.event_planner.util.TestConstants.VALID_LABEL_ID;
import static com.yohan.event_planner.util.TestConstants.VALID_RECURRING_EVENT_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OwnershipValidatorTest {

    private OwnershipValidator validator;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        validator = new OwnershipValidator();
        fixedClock = Clock.fixed(Instant.parse("2025-06-29T00:00:00Z"), ZoneOffset.UTC);
    }

    @Nested
    class ValidateEventOwnershipTests {

        @Test
        void testValidateEventOwnership_idsMatch_noExceptionThrown() {
            // Arrange
            Long userId = USER_ID;
            User user = TestUtils.createValidUserEntityWithId(userId);
            Event event = TestUtils.createValidScheduledEventWithId(EVENT_ID, user, fixedClock);

            // Act + Assert
            assertDoesNotThrow(() -> validator.validateEventOwnership(userId, event));
        }

        @Test
        void testValidateEventOwnership_idsMismatch_throwsException() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId(USER_ID_OTHER);
            Event event = TestUtils.createValidScheduledEventWithId(EVENT_ID, creator, fixedClock);

            // Act + Assert
            EventOwnershipException ex = assertThrows(EventOwnershipException.class,
                    () -> validator.validateEventOwnership(USER_ID, event));

            assertEquals(UNAUTHORIZED_EVENT_ACCESS, ex.getErrorCode());
            assertEquals(EVENT_ID, event.getId());
        }
    }

    @Nested
    class ValidateRecurringEventOwnershipTests {

        @Test
        void testValidateRecurringEventOwnership_idsMatch_noExceptionThrown() {
            // Arrange
            Long userId = USER_ID;
            User creator = TestUtils.createValidUserEntityWithId(userId);
            RecurringEvent recurringEvent = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);

            // Act + Assert
            assertDoesNotThrow(() -> validator.validateRecurringEventOwnership(userId, recurringEvent));
        }

        @Test
        void testValidateRecurringEventOwnership_idsMismatch_throwsException() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId(USER_ID_OTHER);
            RecurringEvent recurringEvent = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);

            // Act + Assert
            RecurringEventOwnershipException ex = assertThrows(RecurringEventOwnershipException.class,
                    () -> validator.validateRecurringEventOwnership(USER_ID, recurringEvent));

            // Assert error code and message contains recurring event ID
            assertEquals(UNAUTHORIZED_RECURRING_EVENT_ACCESS, ex.getErrorCode());
            assertTrue(ex.getMessage().contains(String.valueOf(VALID_RECURRING_EVENT_ID)));
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

    @Nested
    class ValidateLabelOwnershipTests {

        @Test
        void testValidateLabelOwnership_idsMatch_noExceptionThrown() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId(USER_ID);
            Label label = TestUtils.createValidLabelWithId(VALID_LABEL_ID, creator);

            // Act + Assert
            assertDoesNotThrow(() -> validator.validateLabelOwnership(USER_ID, label));
        }

        @Test
        void testValidateLabelOwnership_idsMismatch_throwsException() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId(USER_ID_OTHER);
            Label label = TestUtils.createValidLabelWithId(VALID_LABEL_ID, creator);

            // Act
            LabelOwnershipException ex = assertThrows(LabelOwnershipException.class,
                    () -> validator.validateLabelOwnership(USER_ID, label));

            // Assert
            assertEquals(UNAUTHORIZED_LABEL_ACCESS, ex.getErrorCode());
            assertTrue(ex.getMessage().contains(String.valueOf(VALID_LABEL_ID)));
        }
    }


    @Nested
    class ValidateBadgeOwnershipTests {

        @Test
        void testValidateBadgeOwnership_idsMatch_noExceptionThrown() {
            // Arrange
            Long userId = USER_ID;
            User creator = TestUtils.createValidUserEntityWithId(userId);
            var badge = TestUtils.createValidBadgeWithIdAndOwner(VALID_BADGE_ID, creator);

            // Act + Assert
            assertDoesNotThrow(() -> validator.validateBadgeOwnership(userId, badge));
        }

        @Test
        void testValidateBadgeOwnership_idsMismatch_throwsException() {
            // Arrange
            Long currentUserId = USER_ID;
            User creator = TestUtils.createValidUserEntityWithId(USER_ID_OTHER);
            var badge = TestUtils.createValidBadgeWithIdAndOwner(OTHER_BADGE_ID, creator);

            // Act + Assert
            var ex = assertThrows(com.yohan.event_planner.exception.BadgeOwnershipException.class,
                    () -> validator.validateBadgeOwnership(currentUserId, badge));

            assertEquals(com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_BADGE_ACCESS, ex.getErrorCode());
            assertTrue(ex.getMessage().contains(OTHER_BADGE_ID.toString()));
        }
    }

}
