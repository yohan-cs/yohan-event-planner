package com.yohan.event_planner.business;

import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventResponseDTOFactory;
import com.yohan.event_planner.exception.ConflictException;
import com.yohan.event_planner.exception.InvalidEventStateException;
import com.yohan.event_planner.exception.InvalidTimeException;
import com.yohan.event_planner.exception.RecurringEventAlreadyConfirmedException;
import com.yohan.event_planner.repository.RecurringEventRepository;
import com.yohan.event_planner.service.RecurrenceRuleService;
import com.yohan.event_planner.time.ClockProvider;
import com.yohan.event_planner.util.TestConstants;
import com.yohan.event_planner.util.TestUtils;

import com.yohan.event_planner.validation.ConflictValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.yohan.event_planner.util.TestConstants.VALID_RECURRING_EVENT_ID;
import static com.yohan.event_planner.util.TestUtils.createValidUserEntityWithId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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


@ExtendWith(MockitoExtension.class)
public class RecurringEventBOImplTest {

    @Mock
    private RecurringEventRepository recurringEventRepository;
    @Mock
    private RecurrenceRuleService recurrenceRuleService;
    @Mock
    private ConflictValidator conflictValidator;
    @Mock
    private EventResponseDTOFactory eventResponseDTOFactory;
    @Mock
    private ClockProvider clockProvider;
    
    private Clock fixedClock;
    private User user;

    @InjectMocks
    private RecurringEventBOImpl recurringEventBO;

    @BeforeEach
    void setUp() {
        user = createValidUserEntityWithId();
        fixedClock = Clock.fixed(Instant.parse("2025-06-29T12:00:00Z"), ZoneId.of("UTC"));
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

            when(recurringEventRepository.findConfirmedRecurringEventsForUserBetween(userId, fromDate, toDate))
                    .thenReturn(expected);

            // Act
            List<RecurringEvent> result = recurringEventBO.getConfirmedRecurringEventsForUserInRange(userId, fromDate, toDate);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.containsAll(expected));

            verify(recurringEventRepository).findConfirmedRecurringEventsForUserBetween(userId, fromDate, toDate);
        }

        @Test
        void returnsEmptyListWhenNoEventsExistInRange() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            LocalDate fromDate = TestConstants.getValidEventStartDate(fixedClock);
            LocalDate toDate = TestConstants.getValidEventEndDate(fixedClock);

            when(recurringEventRepository.findConfirmedRecurringEventsForUserBetween(userId, fromDate, toDate))
                    .thenReturn(Collections.emptyList());

            // Act
            List<RecurringEvent> result = recurringEventBO.getConfirmedRecurringEventsForUserInRange(userId, fromDate, toDate);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(recurringEventRepository).findConfirmedRecurringEventsForUserBetween(userId, fromDate, toDate);
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

        @Test
        void throwsExceptionWhenCreatingConfirmedEventWithBlankName() {
            // Arrange
            RecurringEvent confirmed = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            confirmed.setUnconfirmed(false);
            confirmed.setName("   "); // blank name

            // Act + Assert
            assertThrows(InvalidEventStateException.class, () -> recurringEventBO.createRecurringEventWithValidation(confirmed));

            verifyNoInteractions(conflictValidator);
            verify(recurringEventRepository, never()).save(confirmed);
        }

        @Test
        void throwsExceptionWhenCreatingConfirmedEventWithMissingTime() {
            // Arrange
            RecurringEvent confirmed = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            confirmed.setUnconfirmed(false);
            confirmed.setStartTime(null); // missing start time

            // Act + Assert
            assertThrows(InvalidEventStateException.class, () -> recurringEventBO.createRecurringEventWithValidation(confirmed));

            verifyNoInteractions(conflictValidator);
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
        void throwsExceptionWhenNameIsBlank() {
            // Arrange
            RecurringEvent confirmed = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            confirmed.setUnconfirmed(false);
            confirmed.setName("   "); // blank, not null

            // Act & Assert
            assertThrows(InvalidEventStateException.class, () -> recurringEventBO.updateRecurringEvent(confirmed));

            verifyNoInteractions(conflictValidator);
            verify(recurringEventRepository, never()).save(any());
        }

        @Test
        void throwsExceptionWhenNameIsEmpty() {
            // Arrange
            RecurringEvent confirmed = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            confirmed.setUnconfirmed(false);
            confirmed.setName(""); // empty string

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

        @Test
        void throwsExceptionWhenRecurringEventAlreadyConfirmed() {
            // Arrange
            RecurringEvent alreadyConfirmed = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            alreadyConfirmed.setUnconfirmed(false); // already confirmed

            // Act & Assert
            assertThrows(RecurringEventAlreadyConfirmedException.class, 
                () -> recurringEventBO.confirmRecurringEventWithValidation(alreadyConfirmed));

            // Should not proceed to validation or saving
            verifyNoInteractions(conflictValidator);
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

    @Nested
    class GenerateVirtualsTests {

        @Test
        void generatesVirtualEventsSuccessfully() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            ZoneId userZoneId = ZoneId.of(TestConstants.VALID_TIMEZONE);

            ZonedDateTime startTime = TestConstants.getValidEventStartFuture(fixedClock);
            ZonedDateTime endTime = startTime.plusDays(7);

            LocalDate fromDate = startTime.withZoneSameInstant(userZoneId).toLocalDate();
            LocalDate toDate = endTime.withZoneSameInstant(userZoneId).toLocalDate();

            RecurringEvent recurringEvent = TestUtils.createValidRecurringEventWithId(
                    TestUtils.createValidUserEntityWithId(), VALID_RECURRING_EVENT_ID, fixedClock
            );

            when(recurringEventRepository.findConfirmedRecurringEventsForUserBetween(userId, fromDate, toDate))
                    .thenReturn(List.of(recurringEvent));

            List<LocalDate> occurrenceDates = List.of(fromDate.plusDays(1), fromDate.plusDays(2));
            when(recurrenceRuleService.expandRecurrence(any(), eq(fromDate), eq(toDate), any()))
                    .thenReturn(occurrenceDates);

            EventResponseDTO dto1 = mock(EventResponseDTO.class);
            EventResponseDTO dto2 = mock(EventResponseDTO.class);
            EventResponseDTOFactory eventResponseDTOFactory = mock(EventResponseDTOFactory.class);
            when(eventResponseDTOFactory.createFromRecurringEvent(recurringEvent, occurrenceDates.get(0)))
                    .thenReturn(dto1);
            when(eventResponseDTOFactory.createFromRecurringEvent(recurringEvent, occurrenceDates.get(1)))
                    .thenReturn(dto2);

            ClockProvider clockProvider = mock(ClockProvider.class);
            when(clockProvider.getClockForZone(userZoneId)).thenReturn(fixedClock);

            // Create BO with mocked dependencies
            RecurringEventBOImpl boWithMocks = new RecurringEventBOImpl(
                    recurringEventRepository,
                    recurrenceRuleService,
                    eventResponseDTOFactory,
                    clockProvider,
                    conflictValidator
            );

            // Act
            List<EventResponseDTO> result = boWithMocks.generateVirtuals(userId, startTime, endTime, userZoneId);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.contains(dto1));
            assertTrue(result.contains(dto2));

            verify(recurringEventRepository).findConfirmedRecurringEventsForUserBetween(userId, fromDate, toDate);
            verify(recurrenceRuleService).expandRecurrence(any(), eq(fromDate), eq(toDate), any());
            verify(eventResponseDTOFactory).createFromRecurringEvent(recurringEvent, occurrenceDates.get(0));
            verify(eventResponseDTOFactory).createFromRecurringEvent(recurringEvent, occurrenceDates.get(1));
        }

        @Test
        void returnsEmptyListWhenNoRecurrences() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            ZonedDateTime startTime = TestConstants.getValidEventStartFuture(fixedClock);
            ZonedDateTime endTime = startTime.plusDays(1);
            ZoneId userZoneId = ZoneId.of(TestConstants.VALID_TIMEZONE);

            ClockProvider clockProvider = mock(ClockProvider.class);
            when(clockProvider.getClockForZone(userZoneId)).thenReturn(fixedClock);
            when(recurringEventRepository.findConfirmedRecurringEventsForUserBetween(any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            EventResponseDTOFactory eventResponseDTOFactory = mock(EventResponseDTOFactory.class);

            // Create BO with mocked dependencies
            RecurringEventBOImpl boWithMocks = new RecurringEventBOImpl(
                    recurringEventRepository,
                    recurrenceRuleService,
                    eventResponseDTOFactory,
                    clockProvider,
                    conflictValidator
            );

            // Act
            List<EventResponseDTO> result = boWithMocks.generateVirtuals(userId, startTime, endTime, userZoneId);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(recurringEventRepository).findConfirmedRecurringEventsForUserBetween(any(), any(), any());
            verify(clockProvider).getClockForZone(userZoneId);
            verifyNoInteractions(recurrenceRuleService, eventResponseDTOFactory);
        }

        @Test
        void skipsEventsThatAreInThePast() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            ZoneId userZoneId = ZoneId.of(TestConstants.VALID_TIMEZONE);
            ZonedDateTime startTime = TestConstants.getValidEventStartFuture(fixedClock);
            ZonedDateTime endTime = startTime.plusDays(1);

            LocalDate fromDate = startTime.withZoneSameInstant(userZoneId).toLocalDate();
            LocalDate toDate = endTime.withZoneSameInstant(userZoneId).toLocalDate();

            RecurringEvent recurringEvent = TestUtils.createValidRecurringEventWithId(
                    TestUtils.createValidUserEntityWithId(), VALID_RECURRING_EVENT_ID, fixedClock
            );

            when(recurringEventRepository.findConfirmedRecurringEventsForUserBetween(userId, fromDate, toDate))
                    .thenReturn(List.of(recurringEvent));

            // Past date
            List<LocalDate> occurrenceDates = List.of(LocalDate.now(fixedClock).minusDays(1));
            when(recurrenceRuleService.expandRecurrence(any(), eq(fromDate), eq(toDate), any()))
                    .thenReturn(occurrenceDates);

            ClockProvider clockProvider = mock(ClockProvider.class);
            when(clockProvider.getClockForZone(userZoneId)).thenReturn(fixedClock);

            EventResponseDTOFactory eventResponseDTOFactory = mock(EventResponseDTOFactory.class);

            // Create BO with mocked dependencies
            RecurringEventBOImpl boWithMocks = new RecurringEventBOImpl(
                    recurringEventRepository,
                    recurrenceRuleService,
                    eventResponseDTOFactory,
                    clockProvider,
                    conflictValidator
            );

            // Act
            List<EventResponseDTO> result = boWithMocks.generateVirtuals(userId, startTime, endTime, userZoneId);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(recurringEventRepository).findConfirmedRecurringEventsForUserBetween(userId, fromDate, toDate);
            verify(recurrenceRuleService).expandRecurrence(any(), eq(fromDate), eq(toDate), any());
            verifyNoInteractions(eventResponseDTOFactory);
        }

        @Test
        void doesNotIncludePastOccurrencesInVirtuals() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            ZonedDateTime startTime = TestConstants.getValidEventStartPast(fixedClock);
            ZonedDateTime endTime = startTime.plusDays(1);
            ZoneId userZoneId = ZoneId.of(TestConstants.VALID_TIMEZONE);

            User creator = TestUtils.createValidUserEntityWithId();
            RecurringEvent recurrence = TestUtils.createValidRecurringEventWithId(
                    creator,
                    VALID_RECURRING_EVENT_ID,
                    fixedClock
            );

            // Set recurrence endTime to early morning to guarantee it's before 'now'
            recurrence.setEndTime(LocalTime.of(1, 0)); // 1 AM

            ClockProvider clockProvider = mock(ClockProvider.class);
            when(clockProvider.getClockForZone(userZoneId)).thenReturn(fixedClock);
            when(recurringEventRepository.findConfirmedRecurringEventsForUserBetween(any(), any(), any()))
                    .thenReturn(List.of(recurrence));

            // Mock occurrence date as yesterday relative to fixedClock
            LocalDate yesterday = LocalDate.now(fixedClock).minusDays(1);
            when(recurrenceRuleService.expandRecurrence(any(), any(), any(), any()))
                    .thenReturn(List.of(yesterday));

            EventResponseDTOFactory eventResponseDTOFactory = mock(EventResponseDTOFactory.class);

            // Create BO with mocked dependencies
            RecurringEventBOImpl boWithMocks = new RecurringEventBOImpl(
                    recurringEventRepository,
                    recurrenceRuleService,
                    eventResponseDTOFactory,
                    clockProvider,
                    conflictValidator
            );

            // Act
            List<EventResponseDTO> result = boWithMocks.generateVirtuals(userId, startTime, endTime, userZoneId);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty(), "No virtuals should be generated for past occurrences.");

            verify(recurrenceRuleService).expandRecurrence(any(), any(), any(), any());
            verifyNoInteractions(eventResponseDTOFactory);
        }

        @Test
        void returnsOnlyFutureOccurrencesWhenMixed() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            ZonedDateTime startTime = TestConstants.getValidEventStartPast(fixedClock);
            ZonedDateTime endTime = TestConstants.getValidEventEndFuture(fixedClock);
            ZoneId userZoneId = ZoneId.of(TestConstants.VALID_TIMEZONE);

            User creator = TestUtils.createValidUserEntityWithId();
            RecurringEvent recurrence = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);
            recurrence.setEndTime(LocalTime.NOON);

            // Mock clock to fixed date at midnight
            LocalDate fixedDate = LocalDate.of(2025, 6, 29);
            ZonedDateTime mockNow = fixedDate.atStartOfDay(userZoneId);
            ClockProvider clockProvider = mock(ClockProvider.class);
            when(clockProvider.getClockForZone(userZoneId)).thenReturn(
                    Clock.fixed(mockNow.toInstant(), userZoneId)
            );

            when(recurringEventRepository.findConfirmedRecurringEventsForUserBetween(any(), any(), any()))
                    .thenReturn(List.of(recurrence));

            LocalDate pastDate = fixedDate.minusDays(1);
            LocalDate futureDate = fixedDate.plusDays(1);

            when(recurrenceRuleService.expandRecurrence(any(), any(), any(), any()))
                    .thenReturn(List.of(pastDate, futureDate));

            EventResponseDTO futureVirtual = mock(EventResponseDTO.class);
            EventResponseDTOFactory eventResponseDTOFactory = mock(EventResponseDTOFactory.class);
            when(eventResponseDTOFactory.createFromRecurringEvent(recurrence, futureDate)).thenReturn(futureVirtual);

            // Create BO with mocked dependencies
            RecurringEventBOImpl boWithMocks = new RecurringEventBOImpl(
                    recurringEventRepository,
                    recurrenceRuleService,
                    eventResponseDTOFactory,
                    clockProvider,
                    conflictValidator
            );

            // Act
            List<EventResponseDTO> result = boWithMocks.generateVirtuals(userId, startTime, endTime, userZoneId);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size(), "Should return only the future occurrence.");
            assertTrue(result.contains(futureVirtual));

            verify(eventResponseDTOFactory).createFromRecurringEvent(recurrence, futureDate);
        }

        @Test
        void handlesLargeNumberOfRecurrencesPerformance() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            ZonedDateTime startTime = TestConstants.getValidEventStartFuture(fixedClock);
            ZonedDateTime endTime = startTime.plusDays(30); // 30-day window
            ZoneId userZoneId = ZoneId.of(TestConstants.VALID_TIMEZONE);

            // Create multiple recurring events to simulate heavy load
            List<RecurringEvent> manyRecurrences = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                RecurringEvent recurrence = TestUtils.createValidRecurringEventWithId(
                        TestUtils.createValidUserEntityWithId(), VALID_RECURRING_EVENT_ID + i, fixedClock
                );
                manyRecurrences.add(recurrence);
            }

            ClockProvider clockProvider = mock(ClockProvider.class);
            when(clockProvider.getClockForZone(userZoneId)).thenReturn(fixedClock);
            when(recurringEventRepository.findConfirmedRecurringEventsForUserBetween(any(), any(), any()))
                    .thenReturn(manyRecurrences);

            // Mock multiple occurrences per recurrence (daily for 30 days = 30 occurrences each)
            List<LocalDate> manyOccurrences = new ArrayList<>();
            LocalDate fromDate = startTime.withZoneSameInstant(userZoneId).toLocalDate();
            for (int i = 0; i < 30; i++) {
                manyOccurrences.add(fromDate.plusDays(i));
            }
            when(recurrenceRuleService.expandRecurrence(any(), any(), any(), any()))
                    .thenReturn(manyOccurrences);

            EventResponseDTOFactory eventResponseDTOFactory = mock(EventResponseDTOFactory.class);
            when(eventResponseDTOFactory.createFromRecurringEvent(any(), any()))
                    .thenReturn(mock(EventResponseDTO.class));

            // Create BO with mocked dependencies
            RecurringEventBOImpl boWithMocks = new RecurringEventBOImpl(
                    recurringEventRepository,
                    recurrenceRuleService,
                    eventResponseDTOFactory,
                    clockProvider,
                    conflictValidator
            );

            // Act - Measure performance of generating many virtual events
            long startTimeMillis = System.currentTimeMillis();
            List<EventResponseDTO> result = boWithMocks.generateVirtuals(userId, startTime, endTime, userZoneId);
            long durationMillis = System.currentTimeMillis() - startTimeMillis;

            // Assert - Should handle large datasets efficiently
            assertNotNull(result);
            assertEquals(1500, result.size()); // 50 recurrences × 30 occurrences = 1500 virtual events
            assertTrue(durationMillis < 1000, "Virtual event generation should complete within 1 second for large datasets");

            // Verify all mocks were called appropriately
            verify(recurringEventRepository).findConfirmedRecurringEventsForUserBetween(any(), any(), any());
            verify(recurrenceRuleService, org.mockito.Mockito.times(50)).expandRecurrence(any(), any(), any(), any());
            verify(eventResponseDTOFactory, org.mockito.Mockito.times(1500)).createFromRecurringEvent(any(), any());
        }

    }

    @Nested
    class BoundaryValueTests {

        @Test
        void handlesMinimumPaginationLimit() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            int minLimit = 1;
            when(recurringEventRepository.findTopConfirmedByUserIdOrderByEndDateDescIdDesc(any(), any()))
                    .thenReturn(Collections.emptyList());

            // Act
            List<RecurringEvent> result = recurringEventBO.getConfirmedRecurringEventsPage(
                    userId, null, null, null, null, null, minLimit);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(recurringEventRepository).findTopConfirmedByUserIdOrderByEndDateDescIdDesc(any(), any());
        }

        @Test
        void handlesLargePaginationLimit() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            int largeLimit = 1000;
            when(recurringEventRepository.findTopConfirmedByUserIdOrderByEndDateDescIdDesc(any(), any()))
                    .thenReturn(Collections.emptyList());

            // Act
            List<RecurringEvent> result = recurringEventBO.getConfirmedRecurringEventsPage(
                    userId, null, null, null, null, null, largeLimit);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(recurringEventRepository).findTopConfirmedByUserIdOrderByEndDateDescIdDesc(any(), any());
        }

        @Test
        void handlesEmptySkipDaysSet() {
            // Arrange
            RecurringEvent event = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            Set<LocalDate> emptySkipDays = Set.of();
            when(recurringEventRepository.save(event)).thenReturn(event);

            // Act
            recurringEventBO.removeSkipDaysWithConflictValidation(event, emptySkipDays);

            // Assert
            verify(conflictValidator).validateNoConflictsForSkipDays(event, emptySkipDays);
            verify(recurringEventRepository).save(event);
        }

        @Test
        void handlesLargeSkipDaysSet() {
            // Arrange
            RecurringEvent event = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            Set<LocalDate> largeSkipDaysSet = Set.of(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 15),
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 2, 15),
                LocalDate.of(2024, 3, 1)
            );
            when(recurringEventRepository.save(event)).thenReturn(event);

            // Act
            recurringEventBO.removeSkipDaysWithConflictValidation(event, largeSkipDaysSet);

            // Assert
            verify(conflictValidator).validateNoConflictsForSkipDays(event, largeSkipDaysSet);
            verify(recurringEventRepository).save(event);
            // Verify all skip days were removed
            largeSkipDaysSet.forEach(date -> 
                assertFalse(event.getSkipDays().contains(date), "Skip day " + date + " should have been removed"));
        }

        @Test
        void handlesVeryLongDateRangeQuery() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            LocalDate veryOldDate = LocalDate.of(1990, 1, 1);
            LocalDate veryFutureDate = LocalDate.of(2050, 12, 31);
            when(recurringEventRepository.findConfirmedRecurringEventsForUserBetween(any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            // Act
            List<RecurringEvent> result = recurringEventBO.getConfirmedRecurringEventsForUserInRange(
                    userId, veryOldDate, veryFutureDate);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(recurringEventRepository).findConfirmedRecurringEventsForUserBetween(
                    userId, veryOldDate, veryFutureDate);
        }

        @Test
        void handlesSameDayDateRange() {
            // Arrange
            Long userId = TestConstants.USER_ID;
            LocalDate sameDate = LocalDate.of(2024, 6, 15);
            when(recurringEventRepository.findConfirmedRecurringEventsForUserBetween(any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            // Act
            List<RecurringEvent> result = recurringEventBO.getConfirmedRecurringEventsForUserInRange(
                    userId, sameDate, sameDate);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(recurringEventRepository).findConfirmedRecurringEventsForUserBetween(
                    userId, sameDate, sameDate);
        }

        @Test
        void handlesEventWithMaximumLength() {
            // Arrange
            RecurringEvent longEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            longEvent.setUnconfirmed(false);
            // Set times for maximum 24-hour duration
            longEvent.setStartTime(LocalTime.of(0, 0));  // Midnight
            longEvent.setEndTime(LocalTime.of(23, 59)); // 11:59 PM
            when(recurringEventRepository.save(longEvent)).thenReturn(longEvent);

            // Act
            RecurringEvent result = recurringEventBO.createRecurringEventWithValidation(longEvent);

            // Assert
            assertNotNull(result);
            assertFalse(result.isUnconfirmed());
            verify(conflictValidator).validateNoConflicts(longEvent);
            verify(recurringEventRepository).save(longEvent);
        }

        @Test
        void handlesEventWithMinimumDuration() {
            // Arrange
            RecurringEvent shortEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            shortEvent.setUnconfirmed(false);
            // Set times for minimal 1-minute duration
            shortEvent.setStartTime(LocalTime.of(12, 0));  // Noon
            shortEvent.setEndTime(LocalTime.of(12, 1));   // 12:01 PM
            when(recurringEventRepository.save(shortEvent)).thenReturn(shortEvent);

            // Act
            RecurringEvent result = recurringEventBO.createRecurringEventWithValidation(shortEvent);

            // Assert
            assertNotNull(result);
            assertFalse(result.isUnconfirmed());
            verify(conflictValidator).validateNoConflicts(shortEvent);
            verify(recurringEventRepository).save(shortEvent);
        }

        @Test
        void handlesRecurringEventWithVeryLongName() {
            // Arrange
            RecurringEvent longNameEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            longNameEvent.setUnconfirmed(false);
            String veryLongName = "A".repeat(255); // Maximum typical database field length
            longNameEvent.setName(veryLongName);
            when(recurringEventRepository.save(longNameEvent)).thenReturn(longNameEvent);

            // Act
            RecurringEvent result = recurringEventBO.createRecurringEventWithValidation(longNameEvent);

            // Assert
            assertNotNull(result);
            assertEquals(veryLongName, result.getName());
            verify(recurringEventRepository).save(longNameEvent);
        }

        @Test
        void handlesRecurringEventWithSingleCharacterName() {
            // Arrange
            RecurringEvent shortNameEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            shortNameEvent.setUnconfirmed(false);
            shortNameEvent.setName("A"); // Minimum valid name
            when(recurringEventRepository.save(shortNameEvent)).thenReturn(shortNameEvent);

            // Act
            RecurringEvent result = recurringEventBO.createRecurringEventWithValidation(shortNameEvent);

            // Assert
            assertNotNull(result);
            assertEquals("A", result.getName());
            verify(recurringEventRepository).save(shortNameEvent);
        }
    }

    @Nested
    class EnhancedExceptionScenarioTests {

        @Test
        void throwsInvalidEventStateExceptionWhenMissingMultipleFields() {
            // Arrange
            RecurringEvent incompleteEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            incompleteEvent.setUnconfirmed(false);
            // Make multiple fields invalid
            incompleteEvent.setName(null);
            incompleteEvent.setStartTime(null);
            incompleteEvent.setLabel(null);

            // Act & Assert
            InvalidEventStateException exception = assertThrows(InvalidEventStateException.class, () -> 
                recurringEventBO.createRecurringEventWithValidation(incompleteEvent));

            // Should fail on the first validation error (name)
            assertNotNull(exception.getErrorCode());
            verify(recurringEventRepository, never()).save(any());
            verifyNoInteractions(conflictValidator);
        }

        @Test
        void throwsInvalidTimeExceptionWhenStartTimeAfterEndTime() {
            // Arrange
            RecurringEvent invalidTimeEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            invalidTimeEvent.setUnconfirmed(false);
            // Set same date to ensure same-day validation applies
            LocalDate sameDate = LocalDate.of(2025, 6, 27);
            invalidTimeEvent.setStartDate(sameDate);
            invalidTimeEvent.setEndDate(sameDate);
            invalidTimeEvent.setStartTime(LocalTime.of(15, 0)); // 3 PM
            invalidTimeEvent.setEndTime(LocalTime.of(14, 0));   // 2 PM - before start time

            // Act & Assert
            assertThrows(InvalidTimeException.class, () -> 
                recurringEventBO.createRecurringEventWithValidation(invalidTimeEvent));

            verify(recurringEventRepository, never()).save(any());
            verifyNoInteractions(conflictValidator);
        }

        @Test
        void throwsInvalidTimeExceptionWhenStartDateAfterEndDate() {
            // Arrange
            RecurringEvent invalidDateEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            invalidDateEvent.setUnconfirmed(false);
            invalidDateEvent.setStartDate(LocalDate.of(2024, 12, 31));
            invalidDateEvent.setEndDate(LocalDate.of(2024, 1, 1)); // End before start

            // Act & Assert
            assertThrows(InvalidTimeException.class, () -> 
                recurringEventBO.createRecurringEventWithValidation(invalidDateEvent));

            verify(recurringEventRepository, never()).save(any());
            verifyNoInteractions(conflictValidator);
        }

        @Test
        void throwsInvalidTimeExceptionWhenStartTimeEqualsEndTime() {
            // Arrange
            RecurringEvent sameTimeEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            sameTimeEvent.setUnconfirmed(false);
            // Set same date to ensure same-day validation applies
            LocalDate sameDate = LocalDate.of(2025, 6, 27);
            sameTimeEvent.setStartDate(sameDate);
            sameTimeEvent.setEndDate(sameDate);
            LocalTime sameTime = LocalTime.of(14, 30);
            sameTimeEvent.setStartTime(sameTime);
            sameTimeEvent.setEndTime(sameTime); // Same time not allowed on same day

            // Act & Assert
            assertThrows(InvalidTimeException.class, () -> 
                recurringEventBO.createRecurringEventWithValidation(sameTimeEvent));

            verify(recurringEventRepository, never()).save(any());
            verifyNoInteractions(conflictValidator);
        }

        @Test
        void handlesConflictExceptionFromConflictValidatorDuringUpdate() {
            // Arrange
            RecurringEvent conflictingEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            conflictingEvent.setUnconfirmed(false);
            
            ConflictException conflictException = new ConflictException(conflictingEvent, Set.of(999L, 888L));
            doThrow(conflictException).when(conflictValidator).validateNoConflicts(conflictingEvent);

            // Act & Assert
            ConflictException thrownException = assertThrows(ConflictException.class, () -> 
                recurringEventBO.updateRecurringEvent(conflictingEvent));

            assertEquals(conflictException, thrownException);
            verify(conflictValidator).validateNoConflicts(conflictingEvent);
            verify(recurringEventRepository, never()).save(any());
        }

        @Test
        void handlesConflictExceptionFromConflictValidatorDuringConfirmation() {
            // Arrange
            RecurringEvent draftEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            draftEvent.setUnconfirmed(true);
            
            ConflictException conflictException = new ConflictException(draftEvent, Set.of(777L));
            doThrow(conflictException).when(conflictValidator).validateNoConflicts(draftEvent);

            // Act & Assert
            ConflictException thrownException = assertThrows(ConflictException.class, () -> 
                recurringEventBO.confirmRecurringEventWithValidation(draftEvent));

            assertEquals(conflictException, thrownException);
            verify(conflictValidator).validateNoConflicts(draftEvent);
            verify(recurringEventRepository, never()).save(any());
            // Event should still be unconfirmed
            assertTrue(draftEvent.isUnconfirmed());
        }

        @Test
        void handlesConflictExceptionFromSkipDaysValidation() {
            // Arrange
            RecurringEvent event = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            Set<LocalDate> skipDaysToRemove = Set.of(LocalDate.of(2024, 6, 15));
            
            ConflictException conflictException = new ConflictException(event, Set.of(555L));
            doThrow(conflictException).when(conflictValidator).validateNoConflictsForSkipDays(event, skipDaysToRemove);

            // Act & Assert
            ConflictException thrownException = assertThrows(ConflictException.class, () -> 
                recurringEventBO.removeSkipDaysWithConflictValidation(event, skipDaysToRemove));

            assertEquals(conflictException, thrownException);
            verify(conflictValidator).validateNoConflictsForSkipDays(event, skipDaysToRemove);
            verify(recurringEventRepository, never()).save(any());
        }

        @Test
        void throwsRecurringEventAlreadyConfirmedExceptionWhenConfirmingConfirmedEvent() {
            // Arrange
            RecurringEvent alreadyConfirmedEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            alreadyConfirmedEvent.setUnconfirmed(false); // Already confirmed

            // Act & Assert
            RecurringEventAlreadyConfirmedException exception = assertThrows(RecurringEventAlreadyConfirmedException.class, () -> 
                recurringEventBO.confirmRecurringEventWithValidation(alreadyConfirmedEvent));

            assertNotNull(exception);
            verifyNoInteractions(conflictValidator);
            verify(recurringEventRepository, never()).save(any());
        }

        @Test
        void preservesOriginalExceptionMessageAndType() {
            // Arrange
            RecurringEvent invalidEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            invalidEvent.setUnconfirmed(false);
            invalidEvent.setName("   "); // Blank name

            // Act & Assert
            InvalidEventStateException exception = assertThrows(InvalidEventStateException.class, () -> 
                recurringEventBO.createRecurringEventWithValidation(invalidEvent));

            // Verify the original exception details are preserved
            assertNotNull(exception.getErrorCode());
            assertTrue(exception.getMessage().contains("MISSING_EVENT_NAME") || 
                      exception.getMessage().contains("name"));
        }

        @Test
        void handlesMultipleValidationErrorsInSequence() {
            // Arrange
            RecurringEvent eventWithMultipleIssues = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            eventWithMultipleIssues.setUnconfirmed(false);

            // Test each validation error independently
            
            // 1. Missing name
            eventWithMultipleIssues.setName(null);
            assertThrows(InvalidEventStateException.class, () -> 
                recurringEventBO.createRecurringEventWithValidation(eventWithMultipleIssues));
            
            // 2. Fix name, break start time
            eventWithMultipleIssues.setName("Valid Name");
            eventWithMultipleIssues.setStartTime(null);
            assertThrows(InvalidEventStateException.class, () -> 
                recurringEventBO.createRecurringEventWithValidation(eventWithMultipleIssues));
            
            // 3. Fix start time, break end time
            eventWithMultipleIssues.setStartTime(LocalTime.of(9, 0));
            eventWithMultipleIssues.setEndTime(null);
            assertThrows(InvalidEventStateException.class, () -> 
                recurringEventBO.createRecurringEventWithValidation(eventWithMultipleIssues));

            // Verify no saves occurred during validation failures
            verify(recurringEventRepository, never()).save(any());
        }

        @Test
        void allowsValidationToPassAfterFixingAllIssues() {
            // Arrange
            RecurringEvent eventToFix = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            eventToFix.setUnconfirmed(false);
            eventToFix.setName(null); // Start with invalid state

            // Verify it fails initially
            assertThrows(InvalidEventStateException.class, () -> 
                recurringEventBO.createRecurringEventWithValidation(eventToFix));

            // Fix the issue
            eventToFix.setName("Fixed Name");
            when(recurringEventRepository.save(eventToFix)).thenReturn(eventToFix);

            // Act - should now succeed
            RecurringEvent result = recurringEventBO.createRecurringEventWithValidation(eventToFix);

            // Assert
            assertNotNull(result);
            assertEquals("Fixed Name", result.getName());
            verify(conflictValidator).validateNoConflicts(eventToFix);
            verify(recurringEventRepository).save(eventToFix);
        }
    }

    @Nested
    class ValidationEdgeCaseTests {

        @Test
        void handlesEventNameWithOnlyWhitespace() {
            // Arrange
            RecurringEvent whitespaceNameEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            whitespaceNameEvent.setUnconfirmed(false);
            whitespaceNameEvent.setName("   \t\n   "); // Only whitespace characters

            // Act & Assert
            InvalidEventStateException exception = assertThrows(InvalidEventStateException.class, () -> 
                recurringEventBO.createRecurringEventWithValidation(whitespaceNameEvent));

            assertNotNull(exception.getErrorCode());
            verify(recurringEventRepository, never()).save(any());
        }

        @Test
        void handlesEventNameWithUnicodeCharacters() {
            // Arrange
            RecurringEvent unicodeNameEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            unicodeNameEvent.setUnconfirmed(false);
            unicodeNameEvent.setName("会议 📅 Réunion 🔔 Встреча"); // Unicode characters
            when(recurringEventRepository.save(unicodeNameEvent)).thenReturn(unicodeNameEvent);

            // Act
            RecurringEvent result = recurringEventBO.createRecurringEventWithValidation(unicodeNameEvent);

            // Assert
            assertNotNull(result);
            assertEquals("会议 📅 Réunion 🔔 Встреча", result.getName());
            verify(recurringEventRepository).save(unicodeNameEvent);
        }

        @Test
        void handlesEventNameWithSpecialCharacters() {
            // Arrange
            RecurringEvent specialCharEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            specialCharEvent.setUnconfirmed(false);
            specialCharEvent.setName("Event@Home#2024&More!"); // Special characters
            when(recurringEventRepository.save(specialCharEvent)).thenReturn(specialCharEvent);

            // Act
            RecurringEvent result = recurringEventBO.createRecurringEventWithValidation(specialCharEvent);

            // Assert
            assertNotNull(result);
            assertEquals("Event@Home#2024&More!", result.getName());
            verify(recurringEventRepository).save(specialCharEvent);
        }

        @Test
        void handlesTimesAtMidnightBoundary() {
            // Arrange
            RecurringEvent midnightEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            midnightEvent.setUnconfirmed(false);
            midnightEvent.setStartTime(LocalTime.of(23, 58)); // 11:58 PM
            midnightEvent.setEndTime(LocalTime.of(23, 59)); // 11:59 PM (1 minute duration)
            when(recurringEventRepository.save(midnightEvent)).thenReturn(midnightEvent);

            // Act
            RecurringEvent result = recurringEventBO.createRecurringEventWithValidation(midnightEvent);

            // Assert
            assertNotNull(result);
            assertEquals(LocalTime.of(23, 58), result.getStartTime());
            assertEquals(LocalTime.of(23, 59), result.getEndTime());
            verify(recurringEventRepository).save(midnightEvent);
        }

        @Test
        void handlesDateAtYearBoundary() {
            // Arrange
            RecurringEvent yearBoundaryEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            yearBoundaryEvent.setUnconfirmed(false);
            yearBoundaryEvent.setStartDate(LocalDate.of(2023, 12, 31)); // Dec 31
            yearBoundaryEvent.setEndDate(LocalDate.of(2024, 1, 1));     // Jan 1 next year
            when(recurringEventRepository.save(yearBoundaryEvent)).thenReturn(yearBoundaryEvent);

            // Act
            RecurringEvent result = recurringEventBO.createRecurringEventWithValidation(yearBoundaryEvent);

            // Assert
            assertNotNull(result);
            assertEquals(LocalDate.of(2023, 12, 31), result.getStartDate());
            assertEquals(LocalDate.of(2024, 1, 1), result.getEndDate());
            verify(recurringEventRepository).save(yearBoundaryEvent);
        }

        @Test
        void handlesLeapYearDate() {
            // Arrange
            RecurringEvent leapYearEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            leapYearEvent.setUnconfirmed(false);
            leapYearEvent.setStartDate(LocalDate.of(2024, 2, 29)); // Leap day
            leapYearEvent.setEndDate(LocalDate.of(2024, 3, 1));    // Day after leap day
            when(recurringEventRepository.save(leapYearEvent)).thenReturn(leapYearEvent);

            // Act
            RecurringEvent result = recurringEventBO.createRecurringEventWithValidation(leapYearEvent);

            // Assert
            assertNotNull(result);
            assertEquals(LocalDate.of(2024, 2, 29), result.getStartDate());
            assertEquals(LocalDate.of(2024, 3, 1), result.getEndDate());
            verify(recurringEventRepository).save(leapYearEvent);
        }

        @Test
        void allowsNullEndDateForInfiniteRecurrence() {
            // Arrange
            RecurringEvent infiniteEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            infiniteEvent.setUnconfirmed(false);
            infiniteEvent.setEndDate(null); // Infinite recurrence
            when(recurringEventRepository.save(infiniteEvent)).thenReturn(infiniteEvent);

            // Act
            RecurringEvent result = recurringEventBO.createRecurringEventWithValidation(infiniteEvent);

            // Assert
            assertNotNull(result);
            assertNull(result.getEndDate());
            verify(recurringEventRepository).save(infiniteEvent);
        }

        @Test
        void handlesDraftEventWithMissingRequiredFields() {
            // Arrange
            RecurringEvent draftEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            draftEvent.setUnconfirmed(true); // Draft mode
            // Make it invalid but it should still pass because it's a draft
            draftEvent.setName(null);
            draftEvent.setStartTime(null);
            draftEvent.setEndTime(null);
            when(recurringEventRepository.save(draftEvent)).thenReturn(draftEvent);

            // Act
            RecurringEvent result = recurringEventBO.createRecurringEventWithValidation(draftEvent);

            // Assert
            assertNotNull(result);
            assertTrue(result.isUnconfirmed());
            verify(recurringEventRepository).save(draftEvent);
            // Should not perform validation for drafts
            verifyNoInteractions(conflictValidator);
        }

        @Test
        void handlesEventWithAllOptionalFieldsNull() {
            // Arrange
            RecurringEvent minimalEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);
            minimalEvent.setUnconfirmed(false);
            // Keep only required fields, set optional ones to null
            minimalEvent.setDescription(null);
            minimalEvent.setEndDate(null); // Optional for infinite recurrence
            when(recurringEventRepository.save(minimalEvent)).thenReturn(minimalEvent);

            // Act
            RecurringEvent result = recurringEventBO.createRecurringEventWithValidation(minimalEvent);

            // Assert
            assertNotNull(result);
            assertNull(result.getDescription());
            assertNull(result.getEndDate());
            verify(recurringEventRepository).save(minimalEvent);
        }
    }

}
