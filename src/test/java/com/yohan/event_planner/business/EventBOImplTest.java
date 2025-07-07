package com.yohan.event_planner.business;


import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.EventChangeContextDTO;
import com.yohan.event_planner.exception.ConflictException;
import com.yohan.event_planner.exception.InvalidEventStateException;
import com.yohan.event_planner.exception.InvalidTimeException;
import com.yohan.event_planner.repository.EventRepository;
import com.yohan.event_planner.service.LabelTimeBucketService;
import com.yohan.event_planner.service.RecurrenceRuleService;
import com.yohan.event_planner.time.ClockProvider;
import com.yohan.event_planner.util.TestConstants;
import com.yohan.event_planner.util.TestUtils;
import com.yohan.event_planner.validation.ConflictValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.yohan.event_planner.util.TestConstants.EVENT_ID;
import static com.yohan.event_planner.util.TestConstants.USER_ID;
import static com.yohan.event_planner.util.TestConstants.VALID_EVENT_DURATION_MINUTES;
import static com.yohan.event_planner.util.TestConstants.VALID_LABEL_ID;
import static com.yohan.event_planner.util.TestConstants.VALID_TIMEZONE;
import static com.yohan.event_planner.util.TestConstants.getValidEventEndFuture;
import static com.yohan.event_planner.util.TestConstants.getValidEventStartFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class EventBOImplTest {

    private RecurringEventBO recurringEventBO;
    private RecurrenceRuleService recurrenceRuleService;
    private LabelTimeBucketService labelTimeBucketService;
    private EventRepository eventRepository;
    private ConflictValidator conflictValidator;
    private ClockProvider clockProvider;
    private Clock fixedClock;

    private EventBOImpl eventBO;

    @BeforeEach
    void setUp() {
        this.recurringEventBO = mock(RecurringEventBO.class);
        this.recurrenceRuleService = mock(RecurrenceRuleService.class);
        this.labelTimeBucketService = mock(LabelTimeBucketService.class);
        this.eventRepository = mock(EventRepository.class);
        this.conflictValidator = mock(ConflictValidator.class);
        this.clockProvider = mock(ClockProvider.class);

        fixedClock = Clock.fixed(Instant.parse("2025-06-29T12:00:00Z"), ZoneId.of("UTC"));

        eventBO = new EventBOImpl(
                recurringEventBO,
                recurrenceRuleService,
                labelTimeBucketService,
                eventRepository,
                conflictValidator,
                clockProvider
        );
    }

    @Nested
    class GetEventByIdTests {

        @Test
        void testGetEventByIdReturnsEventIfPresent() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidScheduledEventWithId(EVENT_ID, creator, fixedClock);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

            // Act
            Optional<Event> result = eventBO.getEventById(EVENT_ID);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(EVENT_ID, result.get().getId());
            verify(eventRepository).findById(EVENT_ID);
        }

        @Test
        void testGetEventByIdReturnsEmptyIfNotFound() {
            // Arrange
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            // Act
            Optional<Event> result = eventBO.getEventById(EVENT_ID);

            // Assert
            assertTrue(result.isEmpty());
            verify(eventRepository).findById(EVENT_ID);
        }

    }

    @Nested
    class GetConfirmedEventsForUserBetweenTests {

        @Test
        void shouldReturnEventsWithinWindow() {
            // Arrange
            Long userId = USER_ID;
            ZonedDateTime windowStart = getValidEventStartFuture(fixedClock).minusDays(1);
            ZonedDateTime windowEnd = getValidEventEndFuture(fixedClock).plusDays(1);

            User user = TestUtils.createValidUserEntityWithId(userId);
            Event mockEvent = TestUtils.createValidScheduledEventWithId(EVENT_ID, user, fixedClock);
            List<Event> mockEvents = List.of(mockEvent);

            when(eventRepository.findConfirmedEventsForUserBetween(userId, windowStart, windowEnd))
                    .thenReturn(mockEvents);

            // Act
            List<Event> result = eventBO.getConfirmedEventsForUserInRange(userId, windowStart, windowEnd);

            // Assert
            assertEquals(mockEvents, result);
            verify(eventRepository).findConfirmedEventsForUserBetween(userId, windowStart, windowEnd);
        }

        @Test
        void shouldReturnEmptyListWhenNoEventsFound() {
            // Arrange
            Long userId = USER_ID;
            ZonedDateTime windowStart = getValidEventStartFuture(fixedClock);
            ZonedDateTime windowEnd = getValidEventEndFuture(fixedClock);

            when(eventRepository.findConfirmedEventsForUserBetween(userId, windowStart, windowEnd))
                    .thenReturn(List.of());

            // Act
            List<Event> result = eventBO.getConfirmedEventsForUserInRange(userId, windowStart, windowEnd);

            // Assert
            assertTrue(result.isEmpty());
            verify(eventRepository).findConfirmedEventsForUserBetween(userId, windowStart, windowEnd);
        }

    }

    @Nested
    class GetUnconfirmedEventsForUserTests {

        @Test
        void shouldReturnUnconfirmedEventsForUser() {
            // Arrange
            Long userId = 1L;
            Event event1 = mock(Event.class);
            Event event2 = mock(Event.class);
            List<Event> expectedEvents = List.of(event1, event2);

            when(eventRepository.findUnconfirmedEventsForUserSortedByStartTime(userId))
                    .thenReturn(expectedEvents);

            // Act
            List<Event> result = eventBO.getUnconfirmedEventsForUser(userId);

            // Assert
            assertEquals(expectedEvents, result);
            verify(eventRepository).findUnconfirmedEventsForUserSortedByStartTime(userId);
        }

        @Test
        void shouldReturnEmptyListWhenNoUnconfirmedEventsExist() {
            // Arrange
            Long userId = 1L;
            when(eventRepository.findUnconfirmedEventsForUserSortedByStartTime(userId))
                    .thenReturn(List.of());

            // Act
            List<Event> result = eventBO.getUnconfirmedEventsForUser(userId);

            // Assert
            assertTrue(result.isEmpty());
            verify(eventRepository).findUnconfirmedEventsForUserSortedByStartTime(userId);
        }

    }

    @Nested
    class GetConfirmedEventsPageTests {

        @Test
        void shouldReturnTopConfirmedEventsWhenCursorsAreNull() {
            // Arrange
            Long userId = USER_ID;
            ZonedDateTime endTimeCursor = null;
            ZonedDateTime startTimeCursor = null;
            Long idCursor = null;
            int limit = 5;

            Event event1 = TestUtils.createValidScheduledEventWithId(1L, TestUtils.createValidUserEntityWithId(userId), fixedClock);
            Event event2 = TestUtils.createValidScheduledEventWithId(2L, TestUtils.createValidUserEntityWithId(userId), fixedClock);
            List<Event> expectedEvents = List.of(event1, event2);

            when(eventRepository.findTopConfirmedByUserIdOrderByEndTimeDescStartTimeDescIdDesc(
                    eq(userId),
                    any()
            )).thenReturn(expectedEvents);

            // Act
            List<Event> results = eventBO.getConfirmedEventsPage(userId, endTimeCursor, startTimeCursor, idCursor, limit);

            // Assert
            assertEquals(expectedEvents, results);
            verify(eventRepository).findTopConfirmedByUserIdOrderByEndTimeDescStartTimeDescIdDesc(
                    eq(userId),
                    argThat(pageable -> pageable.getPageNumber() == 0 && pageable.getPageSize() == limit)
            );
            verifyNoMoreInteractions(eventRepository);
        }

        @Test
        void shouldReturnEventsBeforeCursorWhenCursorsProvided() {
            // Arrange
            Long userId = USER_ID;
            ZonedDateTime endTimeCursor = ZonedDateTime.now(fixedClock);
            ZonedDateTime startTimeCursor = endTimeCursor.minusHours(1);
            Long idCursor = 10L;
            int limit = 3;

            Event event1 = TestUtils.createValidScheduledEventWithId(5L, TestUtils.createValidUserEntityWithId(userId), fixedClock);
            List<Event> expectedEvents = List.of(event1);

            when(eventRepository.findConfirmedByUserIdBeforeCursor(
                    eq(userId),
                    eq(endTimeCursor),
                    eq(startTimeCursor),
                    eq(idCursor),
                    any()
            )).thenReturn(expectedEvents);

            // Act
            List<Event> results = eventBO.getConfirmedEventsPage(userId, endTimeCursor, startTimeCursor, idCursor, limit);

            // Assert
            assertEquals(expectedEvents, results);
            verify(eventRepository).findConfirmedByUserIdBeforeCursor(
                    eq(userId),
                    eq(endTimeCursor),
                    eq(startTimeCursor),
                    eq(idCursor),
                    argThat(pageable -> pageable.getPageNumber() == 0 && pageable.getPageSize() == limit)
            );
            verifyNoMoreInteractions(eventRepository);
        }

        @Test
        void shouldReturnEmptyListWhenRepositoryReturnsEmpty() {
            // Arrange
            Long userId = USER_ID;
            ZonedDateTime endTimeCursor = ZonedDateTime.now(fixedClock);
            ZonedDateTime startTimeCursor = endTimeCursor.minusHours(2);
            Long idCursor = 100L;
            int limit = 10;

            when(eventRepository.findConfirmedByUserIdBeforeCursor(
                    eq(userId),
                    eq(endTimeCursor),
                    eq(startTimeCursor),
                    eq(idCursor),
                    any()
            )).thenReturn(List.of());

            // Act
            List<Event> results = eventBO.getConfirmedEventsPage(userId, endTimeCursor, startTimeCursor, idCursor, limit);

            // Assert
            assertTrue(results.isEmpty());
            verify(eventRepository).findConfirmedByUserIdBeforeCursor(
                    eq(userId),
                    eq(endTimeCursor),
                    eq(startTimeCursor),
                    eq(idCursor),
                    any()
            );
            verifyNoMoreInteractions(eventRepository);
        }

        @Test
        void shouldThrowWhenLimitIsZero() {
            // Arrange
            Long userId = USER_ID;
            ZonedDateTime endTimeCursor = null;
            ZonedDateTime startTimeCursor = null;
            Long idCursor = null;

            // Act + Assert
            assertThrows(IllegalArgumentException.class, () ->
                    eventBO.getConfirmedEventsPage(userId, endTimeCursor, startTimeCursor, idCursor, 0)
            );
        }
    }

    @Nested
    class CreateEventTests {

        @Test
        void testCreateEventScheduledValid() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidScheduledEventWithId(EVENT_ID, creator, fixedClock);

            // Mock conflict validator to do nothing (no conflicts)
            doNothing().when(conflictValidator).validateNoConflicts(event);

            when(eventRepository.save(event)).thenReturn(event);

            // Act
            Event result = eventBO.createEvent(event);

            // Assert
            assertEquals(event, result);
            verify(conflictValidator).validateNoConflicts(event);
            verify(eventRepository).save(event);
        }

        @Test
        void testCreateEventDraftSkipsValidation() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event draft = TestUtils.createPartialDraftEvent(creator, fixedClock);
            when(eventRepository.save(draft)).thenReturn(draft);

            // Act
            Event result = eventBO.createEvent(draft);

            // Assert
            assertEquals(draft, result);
            verify(eventRepository).save(draft);
        }

        @Test
        void testCreateEventWithInvalidTimeThrowsException() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidScheduledEventWithId(EVENT_ID, creator, fixedClock);
            event.setStartTime(getValidEventEndFuture(fixedClock).plusHours(1)); // start after end

            // Act + Assert
            assertThrows(InvalidTimeException.class, () -> eventBO.createEvent(event));
            verify(eventRepository, never()).save(any());
        }

        @Test
        void testCreateEventWithConflictThrowsException() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event newEvent = TestUtils.createValidScheduledEventWithId(EVENT_ID, creator, fixedClock);

            // Mock conflictValidator to throw ConflictException
            doThrow(new ConflictException(newEvent, Set.of(100L)))
                    .when(conflictValidator).validateNoConflicts(newEvent);

            // Act + Assert
            assertThrows(ConflictException.class, () -> eventBO.createEvent(newEvent));

            // Verify conflict validation was called
            verify(conflictValidator).validateNoConflicts(newEvent);

            // Verify event was not saved due to conflict
            verify(eventRepository, never()).save(any());
        }

    }

    @Nested
    class SolidifyRecurrencesTests {

        @Test
        void shouldCallGetRecurringEventsAndSolidifyEach() {
            // Arrange
            User user = TestUtils.createValidUserEntityWithId();
            ZonedDateTime fixedNow = ZonedDateTime.now(fixedClock);
            ZonedDateTime fromTime = fixedNow.minusDays(3);
            ZoneId userZoneId = ZoneId.of(user.getTimezone());

            RecurringEvent recurrence1 = TestUtils.createValidRecurringEventWithId(user, 1L, fixedClock);
            RecurringEvent recurrence2 = TestUtils.createValidRecurringEventWithId(user, 2L, fixedClock);

            when(recurringEventBO.getConfirmedRecurringEventsForUserInRange(
                    user.getId(),
                    fromTime.toLocalDate(),
                    fixedNow.toLocalDate()
            )).thenReturn(List.of(recurrence1, recurrence2));

            // Act
            eventBO.solidifyRecurrences(user.getId(), fromTime, fixedNow, userZoneId);

            // Assert
            verify(recurringEventBO).getConfirmedRecurringEventsForUserInRange(
                    user.getId(),
                    fromTime.toLocalDate(),
                    fixedNow.toLocalDate()
            );
        }

        @Test
        void shouldDoNothingWhenNoRecurringEventsFound() {
            // Arrange
            User user = TestUtils.createValidUserEntityWithId();
            ZonedDateTime fixedNow = ZonedDateTime.now(fixedClock);
            ZonedDateTime fromTime = fixedNow.minusDays(3);
            ZoneId userZoneId = ZoneId.of(user.getTimezone());

            when(recurringEventBO.getConfirmedRecurringEventsForUserInRange(
                    user.getId(),
                    fromTime.toLocalDate(),
                    fixedNow.toLocalDate()
            )).thenReturn(List.of());

            // Act
            eventBO.solidifyRecurrences(user.getId(), fromTime, fixedNow, userZoneId);

            // Assert
            verify(recurringEventBO).getConfirmedRecurringEventsForUserInRange(
                    user.getId(),
                    fromTime.toLocalDate(),
                    fixedNow.toLocalDate()
            );
        }
    }

    @Nested
    class UpdateEventTests {

        @Test
        void testUpdateEventScheduledValidNoCompletionChange() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidScheduledEventWithId(EVENT_ID, creator, fixedClock);
            EventChangeContextDTO contextDTO = new EventChangeContextDTO(
                    creator.getId(),
                    VALID_LABEL_ID,
                    VALID_LABEL_ID,
                    getValidEventStartFuture(fixedClock),
                    getValidEventStartFuture(fixedClock),
                    VALID_EVENT_DURATION_MINUTES,
                    VALID_EVENT_DURATION_MINUTES,
                    ZoneId.of(VALID_TIMEZONE),
                    false,
                    false
            );

            // Mock conflictValidator to do nothing (no conflicts)
            doNothing().when(conflictValidator).validateNoConflicts(event);

            when(eventRepository.save(event)).thenReturn(event);

            // Act
            Event result = eventBO.updateEvent(contextDTO, event);

            // Assert
            assertEquals(event, result);
            verify(conflictValidator).validateNoConflicts(event);
            verify(labelTimeBucketService, never()).handleEventChange(any());
            verify(eventRepository).save(event);
        }

        @Test
        void testUpdateEventTriggersLabelTimeBucketUpdateOnCompletionChange() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, creator, fixedClock); // isCompleted = true
            when(clockProvider.getClockForUser(creator)).thenReturn(fixedClock);

            EventChangeContextDTO contextDTO = new EventChangeContextDTO(
                    creator.getId(),
                    VALID_LABEL_ID,
                    VALID_LABEL_ID,
                    getValidEventStartFuture(fixedClock),
                    getValidEventStartFuture(fixedClock),
                    VALID_EVENT_DURATION_MINUTES,
                    VALID_EVENT_DURATION_MINUTES,
                    ZoneId.of(VALID_TIMEZONE),
                    false, // wasCompleted
                    true   // isNowCompleted
            );

            // Mock conflictValidator to do nothing (no conflicts)
            doNothing().when(conflictValidator).validateNoConflicts(event);

            when(eventRepository.save(event)).thenReturn(event);

            // Act
            eventBO.updateEvent(contextDTO, event);

            // Assert
            verify(conflictValidator).validateNoConflicts(event);
            verify(labelTimeBucketService).handleEventChange(any(EventChangeContextDTO.class));
            verify(eventRepository).save(event);
        }


        @Test
        void testUpdateEventInvalidTimeThrowsException() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidScheduledEventWithId(EVENT_ID, creator, fixedClock);
            event.setStartTime(getValidEventEndFuture(fixedClock).plusHours(1)); // invalid: start > end

            // Act + Assert
            assertThrows(InvalidTimeException.class, () -> eventBO.updateEvent(null, event));
            verify(eventRepository, never()).save(any());
        }

        @Test
        void testUpdateEventWithConflictThrowsException() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidScheduledEventWithId(EVENT_ID, creator, fixedClock);

            // Mock conflictValidator to throw ConflictException
            doThrow(new ConflictException(event, Set.of(999L)))
                    .when(conflictValidator).validateNoConflicts(event);

            // Act + Assert
            assertThrows(ConflictException.class, () -> eventBO.updateEvent(null, event));

            // Verify conflictValidator was called
            verify(conflictValidator).validateNoConflicts(event);

            // Verify event was not saved due to conflict
            verify(eventRepository, never()).save(any());
        }

        @Test
        void testUpdateEventAlreadyCompletedStillTriggersLabelTimeBucketUpdate() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, creator, fixedClock); // isCompleted = true

            EventChangeContextDTO contextDTO = new EventChangeContextDTO(
                    creator.getId(),
                    VALID_LABEL_ID,
                    VALID_LABEL_ID,
                    getValidEventStartFuture(fixedClock),
                    getValidEventStartFuture(fixedClock),
                    VALID_EVENT_DURATION_MINUTES,
                    VALID_EVENT_DURATION_MINUTES,
                    ZoneId.of(VALID_TIMEZONE),
                    true,  // wasCompleted
                    true   // isNowCompleted
            );

            // Mock conflictValidator to do nothing (no conflicts)
            doNothing().when(conflictValidator).validateNoConflicts(event);

            when(eventRepository.save(event)).thenReturn(event);

            // Act
            eventBO.updateEvent(contextDTO, event);

            // Assert
            verify(conflictValidator).validateNoConflicts(event);
            verify(labelTimeBucketService).handleEventChange(any(EventChangeContextDTO.class));
            verify(eventRepository).save(event);
        }

        @Test
        void testUpdateUnconfirmedDraftSkipsValidationAndSaves() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event draft = TestUtils.createPartialDraftEvent(creator, fixedClock); // unconfirmed = true
            TestUtils.setEventId(draft, EVENT_ID);

            // intentionally make time invalid (should be ignored for drafts)
            draft.setStartTime(getValidEventEndFuture(fixedClock).plusHours(1));

            when(eventRepository.save(draft)).thenReturn(draft);

            // Act
            Event result = eventBO.updateEvent(null, draft);

            // Assert
            assertEquals(draft, result);
            verify(eventRepository).save(draft);
            verify(labelTimeBucketService, never()).handleEventChange(any());
        }

        @Test
        void testUpdateCompletedEventWithNullContextSkipsLabelTimeBucket() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event completed = TestUtils.createValidCompletedEventWithId(EVENT_ID, creator, fixedClock);
            when(clockProvider.getClockForUser(creator)).thenReturn(fixedClock);

            // Mock conflictValidator to do nothing (no conflicts)
            doNothing().when(conflictValidator).validateNoConflicts(completed);

            when(eventRepository.save(completed)).thenReturn(completed);

            // Act
            Event result = eventBO.updateEvent(null, completed);

            // Assert
            assertEquals(completed, result);
            verify(conflictValidator).validateNoConflicts(completed);
            verify(labelTimeBucketService, never()).handleEventChange(any());
            verify(eventRepository).save(completed);
        }

        @Test
        void testUpdateEventNotCompletedWithContextDoesNotTriggerLabelTimeBucket() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidScheduledEventWithId(EVENT_ID, creator, fixedClock); // not completed

            EventChangeContextDTO contextDTO = new EventChangeContextDTO(
                    creator.getId(),
                    VALID_LABEL_ID,
                    VALID_LABEL_ID,
                    getValidEventStartFuture(fixedClock),
                    getValidEventStartFuture(fixedClock),
                    VALID_EVENT_DURATION_MINUTES,
                    VALID_EVENT_DURATION_MINUTES,
                    ZoneId.of(VALID_TIMEZONE),
                    false,
                    false
            );

            // Mock conflictValidator to do nothing (no conflicts)
            doNothing().when(conflictValidator).validateNoConflicts(event);

            when(eventRepository.save(event)).thenReturn(event);

            // Act
            eventBO.updateEvent(contextDTO, event);

            // Assert
            verify(conflictValidator).validateNoConflicts(event);
            verify(labelTimeBucketService, never()).handleEventChange(any());
            verify(eventRepository).save(event);
        }

        @Test
        void testUpdateEventWithLabelChangeTriggersLabelTimeBucket() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, creator, fixedClock); // completed = true
            Long newLabelId = VALID_LABEL_ID;
            Label newLabel = TestUtils.createValidLabelWithId(newLabelId, creator);
            event.setLabel(newLabel); // set different label

            EventChangeContextDTO contextDTO = new EventChangeContextDTO(
                    creator.getId(),
                    VALID_LABEL_ID,
                    newLabelId,
                    getValidEventStartFuture(fixedClock),
                    getValidEventStartFuture(fixedClock),
                    VALID_EVENT_DURATION_MINUTES,
                    VALID_EVENT_DURATION_MINUTES,
                    ZoneId.of(VALID_TIMEZONE),
                    true,
                    true
            );

            // Mock conflictValidator to do nothing (no conflicts)
            doNothing().when(conflictValidator).validateNoConflicts(event);

            when(eventRepository.save(event)).thenReturn(event);

            // Act
            eventBO.updateEvent(contextDTO, event);

            // Assert
            verify(conflictValidator).validateNoConflicts(event);

            ArgumentCaptor<EventChangeContextDTO> captor = ArgumentCaptor.forClass(EventChangeContextDTO.class);
            verify(labelTimeBucketService).handleEventChange(captor.capture());

            EventChangeContextDTO captured = captor.getValue();
            assertEquals(newLabelId, captured.newLabelId());
            assertEquals(VALID_LABEL_ID, captured.oldLabelId());

            verify(eventRepository).save(event);
        }
    }

    @Nested
    class ConfirmEventTests {

        @Test
        void testConfirmEventValid() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event draft = TestUtils.createValidFullDraftEvent(creator, fixedClock); // unconfirmed + all fields valid
            TestUtils.setEventId(draft, EVENT_ID);

            // Mock conflictValidator to do nothing (no conflicts)
            doNothing().when(conflictValidator).validateNoConflicts(draft);

            when(eventRepository.save(draft)).thenReturn(draft);

            // Act
            Event result = eventBO.confirmEvent(draft);

            // Assert
            assertFalse(result.isUnconfirmed());
            verify(conflictValidator).validateNoConflicts(draft);
            verify(eventRepository).save(draft);
        }

        @Test
        void testConfirmEventMissingNameThrowsException() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event draft = TestUtils.createEmptyDraftEvent(creator);
            draft.setStartTime(getValidEventStartFuture(fixedClock));
            draft.setEndTime(getValidEventEndFuture(fixedClock));
            draft.setLabel(TestUtils.createValidLabelWithId(VALID_LABEL_ID, creator));

            // Act + Assert
            assertThrows(InvalidEventStateException.class, () -> eventBO.confirmEvent(draft));
        }

        @Test
        void testConfirmEventMissingStartTimeThrowsException() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event draft = TestUtils.createValidFullDraftEvent(creator, fixedClock);
            draft.setStartTime(null);

            // Act + Assert
            assertThrows(InvalidEventStateException.class, () -> eventBO.confirmEvent(draft));
        }

        @Test
        void testConfirmEventMissingEndTimeThrowsException() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event draft = TestUtils.createValidFullDraftEvent(creator, fixedClock);
            draft.setEndTime(null);

            // Act + Assert
            assertThrows(InvalidEventStateException.class, () -> eventBO.confirmEvent(draft));
        }

        @Test
        void testConfirmEventMissingLabelThrowsException() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event draft = TestUtils.createValidFullDraftEvent(creator, fixedClock);
            draft.setLabel(null);

            // Act + Assert
            assertThrows(InvalidEventStateException.class, () -> eventBO.confirmEvent(draft));
        }

        @Test
        void testConfirmEventInvalidTimeThrowsException() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event draft = TestUtils.createValidFullDraftEvent(creator, fixedClock);
            draft.setStartTime(getValidEventEndFuture(fixedClock).plusHours(1)); // invalid time

            // Act + Assert
            assertThrows(InvalidTimeException.class, () -> eventBO.confirmEvent(draft));
        }

        @Test
        void testConfirmEventWithConflictThrowsException() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event draft = TestUtils.createValidFullDraftEvent(creator, fixedClock);
            TestUtils.setEventId(draft, EVENT_ID);

            // Mock conflictValidator to throw ConflictException
            doThrow(new ConflictException(draft, Set.of(999L)))
                    .when(conflictValidator).validateNoConflicts(draft);

            // Act + Assert
            assertThrows(ConflictException.class, () -> eventBO.confirmEvent(draft));

            // Verify conflict validation was called
            verify(conflictValidator).validateNoConflicts(draft);

            // Verify event was not saved due to conflict
            verify(eventRepository, never()).save(any());
        }

    }

    @Nested
    class DeleteEventTests {
        @Test

        void testDeleteEventDelegatesToRepository() {
            // Act
            eventBO.deleteEvent(EVENT_ID);

            // Assert
            verify(eventRepository).deleteById(EVENT_ID);
        }

    }

    @Nested
    class DeleteAllUnconfirmedEventsByUserTests {

        @Test
        void deleteAllUnconfirmedEventsByUser_callsRepositoryDelete() {
            // Arrange
            Long userId = USER_ID;

            // Act
            eventBO.deleteAllUnconfirmedEventsByUser(userId);

            // Assert
            verify(eventRepository).deleteAllUnconfirmedEventsByUser(userId);
        }

    }

}
