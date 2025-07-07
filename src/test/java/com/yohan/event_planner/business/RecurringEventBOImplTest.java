package com.yohan.event_planner.business;

import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.exception.ConflictException;
import com.yohan.event_planner.exception.InvalidEventStateException;
import com.yohan.event_planner.repository.RecurringEventRepository;
import com.yohan.event_planner.service.RecurrenceRuleService;
import com.yohan.event_planner.util.TestConstants;
import com.yohan.event_planner.util.TestUtils;

import com.yohan.event_planner.validation.ConflictValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.yohan.event_planner.util.TestConstants.VALID_RECURRING_EVENT_ID;
import static com.yohan.event_planner.util.TestUtils.createValidUserEntityWithId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


public class RecurringEventBOImplTest {

    private RecurringEventRepository recurringEventRepository;
    private RecurrenceRuleService recurrenceRuleService;
    private ConflictValidator conflictValidator;
    private Clock fixedClock;
    private User user;

    private RecurringEventBOImpl recurringEventBO;

    @BeforeEach
    void setUp() {
        this.recurringEventRepository = mock(RecurringEventRepository.class);
        this.recurrenceRuleService = mock(RecurrenceRuleService.class);
        this.conflictValidator = mock(ConflictValidator.class);

        user = createValidUserEntityWithId();
        fixedClock = Clock.fixed(Instant.parse("2025-06-29T12:00:00Z"), ZoneId.of("UTC"));

        recurringEventBO = new RecurringEventBOImpl(
                recurringEventRepository,
                recurrenceRuleService,
                conflictValidator
        );
    }

    @Nested
    class GetRecurringEventsByIdTests {


        @Test
        void testRecurringEventFound() {
            // Arrange
            var user = TestUtils.createValidUserEntityWithId();
            var recurringEvent = TestUtils.createValidRecurringEventWithId(user, 100L, fixedClock);
            when(recurringEventRepository.findById(100L)).thenReturn(Optional.of(recurringEvent));

            // Act
            Optional<RecurringEvent> result = recurringEventBO.getRecurringEventById(100L);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(recurringEvent, result.get());
        }

        @Test
        void testRecurringEventNotFound() {
            // Arrange
            when(recurringEventRepository.findById(404L)).thenReturn(Optional.empty());

            // Act
            Optional<RecurringEvent> result = recurringEventBO.getRecurringEventById(404L);

            // Assert
            assertTrue(result.isEmpty());
        }

    }

    @Nested
    class GetConfirmedRecurringEventsForUserInRangeTests {

        @Test
        void returnsRecurringEventsWithinRange() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            LocalDate fromDate = TestConstants.getValidEventStartDate(fixedClock);
            LocalDate toDate = TestConstants.getValidEventEndDate(fixedClock);

            RecurringEvent event1 = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            RecurringEvent event2 = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID + 1, fixedClock);

            List<RecurringEvent> expected = List.of(event1, event2);

            when(recurringEventRepository.findConfirmedRecurringEventsForUserBetween(userId, toDate, fromDate))
                    .thenReturn(expected);

            // Act
            List<RecurringEvent> result = recurringEventBO.getConfirmedRecurringEventsForUserInRange(userId, fromDate, toDate);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.containsAll(expected));

            verify(recurringEventRepository).findConfirmedRecurringEventsForUserBetween(userId, toDate, fromDate);
        }

        @Test
        void returnsEmptyListWhenNoEventsExistInRange() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            LocalDate fromDate = TestConstants.getValidEventStartDate(fixedClock);
            LocalDate toDate = TestConstants.getValidEventEndDate(fixedClock);

            when(recurringEventRepository.findConfirmedRecurringEventsForUserBetween(userId, toDate, fromDate))
                    .thenReturn(Collections.emptyList());

            // Act
            List<RecurringEvent> result = recurringEventBO.getConfirmedRecurringEventsForUserInRange(userId, fromDate, toDate);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(recurringEventRepository).findConfirmedRecurringEventsForUserBetween(userId, toDate, fromDate);
        }

    }

    @Nested
    class GetUnconfirmedRecurringEventsForUserInRangeTests {

        @Test
        void returnsUnconfirmedRecurringEventsWithinRange() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            LocalDate fromDate = TestConstants.getValidEventStartDate(fixedClock);
            LocalDate toDate = TestConstants.getValidEventEndDate(fixedClock);

            RecurringEvent draft1 = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            draft1.setUnconfirmed(true);

            RecurringEvent draft2 = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID + 1, fixedClock);
            draft2.setUnconfirmed(true);

            List<RecurringEvent> expected = List.of(draft1, draft2);

            when(recurringEventRepository.findUnconfirmedRecurringEventsForUserInRange(userId, fromDate, toDate))
                    .thenReturn(expected);

            // Act
            List<RecurringEvent> result = recurringEventBO.getUnconfirmedRecurringEventsForUserInRange(userId, fromDate, toDate);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.containsAll(expected));

            verify(recurringEventRepository).findUnconfirmedRecurringEventsForUserInRange(userId, fromDate, toDate);
        }

        @Test
        void returnsEmptyListWhenNoUnconfirmedEventsExistInRange() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            LocalDate fromDate = TestConstants.getValidEventStartDate(fixedClock);
            LocalDate toDate = TestConstants.getValidEventEndDate(fixedClock);

            when(recurringEventRepository.findUnconfirmedRecurringEventsForUserInRange(userId, fromDate, toDate))
                    .thenReturn(Collections.emptyList());

            // Act
            List<RecurringEvent> result = recurringEventBO.getUnconfirmedRecurringEventsForUserInRange(userId, fromDate, toDate);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(recurringEventRepository).findUnconfirmedRecurringEventsForUserInRange(userId, fromDate, toDate);
        }

    }

    @Nested
    class GetConfirmedRecurringEventsPageTests {

        @Test
        void returnsTopRecurringEventsWhenNoCursorProvided() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            int limit = 10;

            RecurringEvent event1 = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            RecurringEvent event2 = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID + 1, fixedClock);

            List<RecurringEvent> expected = List.of(event1, event2);

            when(recurringEventRepository.findTopConfirmedByUserIdOrderByEndDateDescIdDesc(
                    eq(userId),
                    any()
            )).thenReturn(expected);

            // Act
            List<RecurringEvent> result = recurringEventBO.getConfirmedRecurringEventsPage(
                    userId,
                    null, // no endDateCursor
                    null, // no startDateCursor
                    null, // no startTimeCursor
                    null, // no endTimeCursor
                    null, // no idCursor
                    limit
            );

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.containsAll(expected));

            verify(recurringEventRepository).findTopConfirmedByUserIdOrderByEndDateDescIdDesc(eq(userId), any());
        }

        @Test
        void returnsRecurringEventsBeforeCursorWhenCursorProvided() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            int limit = 5;

            LocalDate endDateCursor = LocalDate.now(fixedClock);
            LocalDate startDateCursor = endDateCursor.minusDays(1);
            LocalTime startTimeCursor = LocalTime.NOON;
            LocalTime endTimeCursor = LocalTime.NOON.plusHours(1);
            Long idCursor = 999L;

            RecurringEvent event1 = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);

            List<RecurringEvent> expected = List.of(event1);

            when(recurringEventRepository.findConfirmedByUserIdBeforeCursor(
                    eq(userId),
                    eq(endDateCursor),
                    eq(startDateCursor),
                    eq(startTimeCursor),
                    eq(endTimeCursor),
                    eq(idCursor),
                    any()
            )).thenReturn(expected);

            // Act
            List<RecurringEvent> result = recurringEventBO.getConfirmedRecurringEventsPage(
                    userId,
                    endDateCursor,
                    startDateCursor,
                    startTimeCursor,
                    endTimeCursor,
                    idCursor,
                    limit
            );

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertTrue(result.containsAll(expected));

            verify(recurringEventRepository).findConfirmedByUserIdBeforeCursor(
                    eq(userId),
                    eq(endDateCursor),
                    eq(startDateCursor),
                    eq(startTimeCursor),
                    eq(endTimeCursor),
                    eq(idCursor),
                    any()
            );
        }

        @Test
        void returnsEmptyListWhenNoEventsFound() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            int limit = 3;

            LocalDate endDateCursor = LocalDate.now(fixedClock);
            LocalDate startDateCursor = endDateCursor.minusDays(2);
            LocalTime startTimeCursor = LocalTime.NOON.minusHours(1);
            LocalTime endTimeCursor = LocalTime.NOON;
            Long idCursor = 888L;

            when(recurringEventRepository.findConfirmedByUserIdBeforeCursor(
                    eq(userId),
                    eq(endDateCursor),
                    eq(startDateCursor),
                    eq(startTimeCursor),
                    eq(endTimeCursor),
                    eq(idCursor),
                    any()
            )).thenReturn(Collections.emptyList());

            // Act
            List<RecurringEvent> result = recurringEventBO.getConfirmedRecurringEventsPage(
                    userId,
                    endDateCursor,
                    startDateCursor,
                    startTimeCursor,
                    endTimeCursor,
                    idCursor,
                    limit
            );

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(recurringEventRepository).findConfirmedByUserIdBeforeCursor(
                    eq(userId),
                    eq(endDateCursor),
                    eq(startDateCursor),
                    eq(startTimeCursor),
                    eq(endTimeCursor),
                    eq(idCursor),
                    any()
            );
        }
    }

    @Nested
    class CreateRecurringEventWithValidationTests {

        @Test
        void createsDraftRecurringEventSuccessfully() {
            // Arrange
            RecurringEvent draft = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            draft.setUnconfirmed(true);

            when(recurringEventRepository.save(draft)).thenReturn(draft);

            // Act
            RecurringEvent result = recurringEventBO.createRecurringEventWithValidation(draft);

            // Assert
            assertNotNull(result);
            assertTrue(result.isUnconfirmed());

            verify(recurringEventRepository).save(draft);
            verifyNoInteractions(conflictValidator);
        }

        @Test
        void createsConfirmedRecurringEventSuccessfully() {
            // Arrange
            RecurringEvent confirmed = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            confirmed.setUnconfirmed(false);

            when(recurringEventRepository.save(confirmed)).thenReturn(confirmed);

            // Act
            RecurringEvent result = recurringEventBO.createRecurringEventWithValidation(confirmed);

            // Assert
            assertNotNull(result);
            assertFalse(result.isUnconfirmed());

            verify(recurringEventRepository).save(confirmed);
            verify(conflictValidator).validateNoConflicts(confirmed);
        }

        @Test
        void throwsExceptionWhenConflictDetectedForConfirmedEvent() {
            // Arrange
            RecurringEvent confirmed = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            confirmed.setUnconfirmed(false);

            doThrow(new ConflictException(confirmed, Set.of(999L)))
                    .when(conflictValidator).validateNoConflicts(confirmed);

            // Act + Assert
            assertThrows(ConflictException.class, () -> recurringEventBO.createRecurringEventWithValidation(confirmed));

            verify(conflictValidator).validateNoConflicts(confirmed);
            verify(recurringEventRepository, never()).save(confirmed);
        }

    }

    @Nested
    class UpdateRecurringEventTests {

        @Test
        void updatesUnconfirmedRecurringEventWithoutValidation() {
            // Arrange
            RecurringEvent draft = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            draft.setUnconfirmed(true);

            when(recurringEventRepository.save(draft)).thenReturn(draft);

            // Act
            RecurringEvent result = recurringEventBO.updateRecurringEvent(draft);

            // Assert
            assertEquals(draft, result);
            verify(recurringEventRepository).save(draft);
            verifyNoInteractions(conflictValidator);
        }

        @Test
        void updatesConfirmedRecurringEventWithValidationAndConflictCheck() {
            // Arrange
            RecurringEvent confirmed = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            confirmed.setUnconfirmed(false);

            when(recurringEventRepository.save(confirmed)).thenReturn(confirmed);

            // Act
            RecurringEvent result = recurringEventBO.updateRecurringEvent(confirmed);

            // Assert
            assertEquals(confirmed, result);
            verify(conflictValidator).validateNoConflicts(confirmed);
            verify(recurringEventRepository).save(confirmed);
        }

        @Test
        void throwsExceptionWhenValidationFailsForConfirmedEvent() {
            // Arrange
            RecurringEvent confirmed = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            confirmed.setUnconfirmed(false);

            // Make it invalid by removing name
            confirmed.setName(null);

            // Act & Assert
            assertThrows(InvalidEventStateException.class, () -> recurringEventBO.updateRecurringEvent(confirmed));

            verifyNoInteractions(conflictValidator);
            verify(recurringEventRepository, never()).save(any());
        }

        @Test
        void throwsConflictExceptionWhenConflictValidatorFails() {
            // Arrange
            RecurringEvent confirmed = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            confirmed.setUnconfirmed(false);

            doThrow(new ConflictException(confirmed, Set.of(999L)))
                    .when(conflictValidator).validateNoConflicts(confirmed);

            // Act & Assert
            assertThrows(ConflictException.class, () -> recurringEventBO.updateRecurringEvent(confirmed));

            verify(conflictValidator).validateNoConflicts(confirmed);
            verify(recurringEventRepository, never()).save(any());
        }

    }

    @Nested
    class ConfirmRecurringEventWithValidationTests {

        @Test
        void confirmsRecurringEventSuccessfully() {
            // Arrange
            RecurringEvent draft = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            draft.setUnconfirmed(true); // initially unconfirmed

            when(recurringEventRepository.save(draft)).thenReturn(draft);

            // Act
            RecurringEvent result = recurringEventBO.confirmRecurringEventWithValidation(draft);

            // Assert
            assertNotNull(result);
            assertFalse(result.isUnconfirmed(), "Recurring event should be marked as confirmed");
            verify(conflictValidator).validateNoConflicts(draft);
            verify(recurringEventRepository).save(draft);
        }

        @Test
        void throwsInvalidEventStateExceptionWhenValidationFails() {
            // Arrange
            RecurringEvent invalid = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            invalid.setUnconfirmed(true);
            invalid.setName(null); // invalid

            // Act & Assert
            assertThrows(InvalidEventStateException.class, () -> recurringEventBO.confirmRecurringEventWithValidation(invalid));

            verifyNoInteractions(conflictValidator);
            verify(recurringEventRepository, never()).save(any());
        }

        @Test
        void throwsConflictExceptionWhenConflictsExist() {
            // Arrange
            RecurringEvent recurringEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            recurringEvent.setUnconfirmed(true);

            doThrow(new ConflictException(recurringEvent, Set.of(999L)))
                    .when(conflictValidator).validateNoConflicts(recurringEvent);

            // Act & Assert
            assertThrows(ConflictException.class, () -> recurringEventBO.confirmRecurringEventWithValidation(recurringEvent));

            verify(conflictValidator).validateNoConflicts(recurringEvent);
            verify(recurringEventRepository, never()).save(any());
        }

    }

    @Nested
    class DeleteRecurringEventTests {

        @Test
        void testDeletesRecurringEventById() {
            // Act
            recurringEventBO.deleteRecurringEvent(123L);

            // Assert
            verify(recurringEventRepository).deleteById(123L);
        }
    }

    @Nested
    class DeleteAllUnconfirmedRecurringEventsByUserTests {

        @Test
        void testDeleteAllUnconfirmedRecurringEventsByUser_Success() {
            // Arrange
            Long userId = 123L;

            // Act
            recurringEventBO.deleteAllUnconfirmedRecurringEventsByUser(userId);

            // Assert
            verify(recurringEventRepository).deleteByCreatorIdAndUnconfirmedTrue(userId);
        }


    }

    @Nested
    class RemoveSkipDaysWithConflictValidationTests {

        @Test
        void removesValidFutureSkipDaysSuccessfully() {
            // Arrange
            RecurringEvent event = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);

            LocalDate futureDate = LocalDate.now(fixedClock).plusDays(1);
            event.addSkipDay(futureDate);

            // Mock recurrence rule to include the date
            when(recurrenceRuleService.occursOn(any(), eq(futureDate))).thenReturn(true);

            // Mock the conflict validation
            doNothing().when(conflictValidator).validateNoConflictsForSkipDays(any(RecurringEvent.class), any(Set.class));

            // Act
            recurringEventBO.removeSkipDaysWithConflictValidation(event, Set.of(futureDate));

            // Assert
            assertFalse(event.getSkipDays().contains(futureDate));  // Verify skip day was removed
            verify(conflictValidator).validateNoConflictsForSkipDays(eq(event), eq(Set.of(futureDate)));  // Verify conflict validation was called
            verify(recurringEventRepository).save(event);  // Verify event was saved
        }

        @Test
        void skipsDatesInRecurrenceRuleAndTriggersConflictValidation() {
            // Arrange
            RecurringEvent event = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);

            LocalDate futureDate = LocalDate.now(fixedClock).plusDays(2);
            event.addSkipDay(futureDate);

            // Act
            recurringEventBO.removeSkipDaysWithConflictValidation(event, Set.of(futureDate));

            // Assert
            assertFalse(event.getSkipDays().contains(futureDate));  // Ensure the skip day was removed
            verify(conflictValidator).validateNoConflictsForSkipDays(any(), any());  // Ensure conflict validation was triggered
            verify(recurringEventRepository).save(event);  // Ensure the event was saved after removing the skip day
        }

        @Test
        void throwsConflictExceptionWhenConflictDetected() {
            // Arrange
            RecurringEvent event = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);

            LocalDate futureDate = LocalDate.now(fixedClock).plusDays(3);
            event.addSkipDay(futureDate);

            // Mock recurrence rule to include the date
            when(recurrenceRuleService.occursOn(any(), eq(futureDate))).thenReturn(true);

            // Mock conflict validator to throw ConflictException
            doThrow(new ConflictException(event, Set.of(999L)))
                    .when(conflictValidator).validateNoConflictsForSkipDays(any(RecurringEvent.class), any(Set.class));

            // Act + Assert
            assertThrows(ConflictException.class, () ->
                    recurringEventBO.removeSkipDaysWithConflictValidation(event, Set.of(futureDate))
            );

            // Ensure skip day is not removed and event is not saved
            assertTrue(event.getSkipDays().contains(futureDate));  // Skip day should still exist
            verify(conflictValidator).validateNoConflictsForSkipDays(any(RecurringEvent.class), any(Set.class)); // Conflict validator should be called
            verify(recurringEventRepository, never()).save(any());  // Event should not be saved
        }

    }

}
