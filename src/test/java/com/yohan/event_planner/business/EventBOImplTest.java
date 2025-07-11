package com.yohan.event_planner.business;

import com.yohan.event_planner.business.handler.EventPatchHandler;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.DayViewDTO;
import com.yohan.event_planner.dto.EventChangeContextDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.LabelResponseDTO;
import com.yohan.event_planner.dto.WeekViewDTO;
import com.yohan.event_planner.exception.ConflictException;
import com.yohan.event_planner.exception.EventAlreadyConfirmedException;
import com.yohan.event_planner.exception.InvalidEventStateException;
import com.yohan.event_planner.exception.InvalidTimeException;
import com.yohan.event_planner.repository.EventRepository;
import com.yohan.event_planner.service.LabelTimeBucketService;
import com.yohan.event_planner.service.RecurrenceRuleService;
import com.yohan.event_planner.time.ClockProvider;
import com.yohan.event_planner.util.TestUtils;
import com.yohan.event_planner.validation.ConflictValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

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
import static org.mockito.Mockito.verifyNoInteractions;
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
                mock(EventPatchHandler.class),
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

        @Test
        void testConfirmEventThrowsExceptionWhenEventAlreadyConfirmed() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            Event alreadyConfirmed = TestUtils.createValidFullDraftEvent(creator, fixedClock);
            alreadyConfirmed.setUnconfirmed(false); // already confirmed
            TestUtils.setEventId(alreadyConfirmed, EVENT_ID);

            // Act + Assert
            assertThrows(EventAlreadyConfirmedException.class, () -> eventBO.confirmEvent(alreadyConfirmed));

            // Should not proceed to validation or saving
            verifyNoInteractions(conflictValidator);
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

    @Nested
    class ViewGenerationTimezoneTests {

        @Test
        void testDayViewGenerationWithTimezoneEdgeCases() {
            // Arrange
            LocalDate selectedDate = LocalDate.of(2025, 6, 29);
            User creator = TestUtils.createValidUserEntityWithId();
            Label label = TestUtils.createValidLabelWithId(VALID_LABEL_ID, creator);
            LabelResponseDTO labelDto = TestUtils.createLabelResponseDTO(label);
            
            // Create events that span timezone boundaries
            List<EventResponseDTO> confirmedEvents = List.of(
                new EventResponseDTO(
                    1L, "Event 1", 
                    ZonedDateTime.of(2025, 6, 28, 23, 30, 0, 0, ZoneId.of("UTC")), // 11:30 PM UTC
                    ZonedDateTime.of(2025, 6, 29, 1, 0, 0, 0, ZoneId.of("UTC")),  // 1:00 AM UTC next day
                    90, // durationMinutes
                    null, null, // timezone fields
                    "Event crossing midnight", 
                    creator.getUsername(), creator.getTimezone(),
                    labelDto, false, false, false
                ),
                new EventResponseDTO(
                    2L, "Event 2",
                    ZonedDateTime.of(2025, 6, 29, 10, 0, 0, 0, ZoneId.of("UTC")),
                    ZonedDateTime.of(2025, 6, 29, 11, 0, 0, 0, ZoneId.of("UTC")),
                    60, // durationMinutes
                    null, null, // timezone fields
                    "Morning event", 
                    creator.getUsername(), creator.getTimezone(),
                    labelDto, false, false, false
                )
            );
            
            List<EventResponseDTO> virtualEvents = List.of(
                new EventResponseDTO(
                    3L, "Virtual Event",
                    ZonedDateTime.of(2025, 6, 29, 22, 0, 0, 0, ZoneId.of("UTC")),
                    ZonedDateTime.of(2025, 6, 30, 0, 30, 0, 0, ZoneId.of("UTC")), // Crosses midnight
                    150, // durationMinutes
                    null, null, // timezone fields
                    "Virtual event crossing midnight", 
                    creator.getUsername(), creator.getTimezone(),
                    labelDto, false, false, true
                )
            );

            // Act
            DayViewDTO result = eventBO.generateDayViewData(selectedDate, confirmedEvents, virtualEvents);

            // Assert
            assertEquals(selectedDate, result.date());
            assertEquals(3, result.events().size());
            
            // Verify events are sorted by start time
            List<EventResponseDTO> sortedEvents = result.events();
            assertTrue(sortedEvents.get(0).startTimeUtc().isBefore(sortedEvents.get(1).startTimeUtc()));
            assertTrue(sortedEvents.get(1).startTimeUtc().isBefore(sortedEvents.get(2).startTimeUtc()));
        }

        @Test
        void testWeekViewGenerationWithDifferentTimezones() {
            // Arrange
            Long userId = USER_ID;
            LocalDate anchorDate = LocalDate.of(2025, 6, 25); // Wednesday - anchor within the week
            ZoneId userZoneId = ZoneId.of("America/New_York"); // UTC-4 in summer
            User creator = TestUtils.createValidUserEntityWithId();
            Label label = TestUtils.createValidLabelWithId(VALID_LABEL_ID, creator);
            LabelResponseDTO labelDto = TestUtils.createLabelResponseDTO(label);
            
            // Create events spanning different days in different timezones within the same week
            List<EventResponseDTO> confirmedEvents = List.of(
                // Event that's Saturday night in NYC (within the week June 23-29)
                new EventResponseDTO(
                    1L, "Late Night Event",
                    ZonedDateTime.of(2025, 6, 29, 3, 0, 0, 0, ZoneId.of("UTC")),    // 11 PM Saturday NYC
                    ZonedDateTime.of(2025, 6, 29, 4, 0, 0, 0, ZoneId.of("UTC")),    // 12 AM Sunday NYC
                    60, // durationMinutes
                    "America/New_York", "America/New_York", // timezone fields
                    "Late night event", 
                    creator.getUsername(), creator.getTimezone(),
                    labelDto, false, false, false
                ),
                // Event on Wednesday in NYC
                new EventResponseDTO(
                    2L, "Wednesday Event",
                    ZonedDateTime.of(2025, 6, 25, 18, 0, 0, 0, ZoneId.of("UTC")),   // 2 PM Wednesday NYC
                    ZonedDateTime.of(2025, 6, 25, 19, 0, 0, 0, ZoneId.of("UTC")),   // 3 PM Wednesday NYC
                    60, // durationMinutes
                    "America/New_York", "America/New_York", // timezone fields
                    "Wednesday event", 
                    creator.getUsername(), creator.getTimezone(),
                    labelDto, false, false, false
                )
            );
            
            List<EventResponseDTO> virtualEvents = List.of(
                new EventResponseDTO(
                    3L, "Virtual Friday Event",
                    ZonedDateTime.of(2025, 6, 27, 17, 0, 0, 0, ZoneId.of("UTC")),   // 1 PM Friday NYC
                    ZonedDateTime.of(2025, 6, 27, 18, 0, 0, 0, ZoneId.of("UTC")),   // 2 PM Friday NYC
                    60, // durationMinutes
                    null, null, // timezone fields
                    "Virtual Friday event", 
                    creator.getUsername(), creator.getTimezone(),
                    labelDto, false, false, true
                )
            );

            // Act
            WeekViewDTO result = eventBO.generateWeekViewData(userId, anchorDate, userZoneId, confirmedEvents, virtualEvents);

            // Assert
            assertEquals(7, result.days().size());
            
            // Verify week starts on Monday and ends on Sunday
            assertEquals(LocalDate.of(2025, 6, 23), result.days().get(0).date()); // Monday
            assertEquals(LocalDate.of(2025, 6, 29), result.days().get(6).date()); // Sunday
            
            // Verify events are distributed correctly across days in user timezone
            Map<LocalDate, List<EventResponseDTO>> dayEvents = result.days().stream()
                .collect(java.util.stream.Collectors.toMap(
                    DayViewDTO::date,
                    DayViewDTO::events
                ));
            
            // Wednesday should have the Wednesday event
            List<EventResponseDTO> wednesdayEvents = dayEvents.getOrDefault(LocalDate.of(2025, 6, 25), List.of());
            assertEquals(1, wednesdayEvents.size());
            assertEquals("Wednesday Event", wednesdayEvents.get(0).name());
            
            // Friday should have the virtual event
            List<EventResponseDTO> fridayEvents = dayEvents.getOrDefault(LocalDate.of(2025, 6, 27), List.of());
            assertEquals(1, fridayEvents.size());
            assertEquals("Virtual Friday Event", fridayEvents.get(0).name());
            
            // Sunday should have the late night event (starts Saturday 11 PM NYC, which is Sunday 3 AM UTC)
            List<EventResponseDTO> sundayEvents = dayEvents.getOrDefault(LocalDate.of(2025, 6, 29), List.of());
            assertEquals(1, sundayEvents.size());
            assertEquals("Late Night Event", sundayEvents.get(0).name());
        }

        @Test
        void testWeekViewWithEmptyEventsAcrossTimezones() {
            // Arrange
            Long userId = USER_ID;
            LocalDate anchorDate = LocalDate.of(2025, 6, 29);
            ZoneId userZoneId = ZoneId.of("Pacific/Auckland"); // UTC+12
            
            // Act
            WeekViewDTO result = eventBO.generateWeekViewData(userId, anchorDate, userZoneId, List.of(), List.of());

            // Assert
            assertEquals(7, result.days().size());
            
            // All days should have empty event lists
            for (DayViewDTO day : result.days()) {
                assertTrue(day.events().isEmpty());
            }
        }
    }

    @Nested
    class MultiDayEventHandlingTests {

        @Test
        void testGroupEventsByDayWithSingleDayEvents() {
            // Arrange
            ZoneId userZoneId = ZoneId.of("America/Los_Angeles");
            User creator = TestUtils.createValidUserEntityWithId();
            Label label = TestUtils.createValidLabelWithId(VALID_LABEL_ID, creator);
            LabelResponseDTO labelDto = TestUtils.createLabelResponseDTO(label);
            
            List<EventResponseDTO> events = List.of(
                new EventResponseDTO(
                    1L, "Morning Event",
                    ZonedDateTime.of(2025, 6, 29, 16, 0, 0, 0, ZoneId.of("UTC")), // 9 AM PDT
                    ZonedDateTime.of(2025, 6, 29, 17, 0, 0, 0, ZoneId.of("UTC")), // 10 AM PDT
                    60, // durationMinutes
                    "America/Los_Angeles", "America/Los_Angeles", // timezone fields
                    "Morning event", 
                    creator.getUsername(), creator.getTimezone(),
                    labelDto, false, false, false
                ),
                new EventResponseDTO(
                    2L, "Afternoon Event",
                    ZonedDateTime.of(2025, 6, 29, 20, 0, 0, 0, ZoneId.of("UTC")), // 1 PM PDT
                    ZonedDateTime.of(2025, 6, 29, 21, 0, 0, 0, ZoneId.of("UTC")), // 2 PM PDT
                    60, // durationMinutes
                    "America/Los_Angeles", "America/Los_Angeles", // timezone fields
                    "Afternoon event", 
                    creator.getUsername(), creator.getTimezone(),
                    labelDto, false, false, false
                )
            );

            // Act
            WeekViewDTO weekView = eventBO.generateWeekViewData(USER_ID, LocalDate.of(2025, 6, 29), userZoneId, events, List.of());
            
            // Assert - Check that single-day events appear only on their respective days
            Map<LocalDate, List<EventResponseDTO>> dayEvents = weekView.days().stream()
                .collect(java.util.stream.Collectors.toMap(
                    DayViewDTO::date,
                    DayViewDTO::events
                ));
            
            // June 29, 2025 should have both events
            List<EventResponseDTO> june29Events = dayEvents.get(LocalDate.of(2025, 6, 29));
            assertEquals(2, june29Events.size());
            
            // Other days should be empty
            for (DayViewDTO day : weekView.days()) {
                if (!day.date().equals(LocalDate.of(2025, 6, 29))) {
                    assertTrue(day.events().isEmpty(), "Day " + day.date() + " should have no events");
                }
            }
        }

        @Test
        void testGroupEventsByDayWithMultiDayEvents() {
            // Arrange
            ZoneId userZoneId = ZoneId.of("Europe/London");
            User creator = TestUtils.createValidUserEntityWithId();
            Label label = TestUtils.createValidLabelWithId(VALID_LABEL_ID, creator);
            LabelResponseDTO labelDto = TestUtils.createLabelResponseDTO(label);
            
            List<EventResponseDTO> events = List.of(
                // Event spanning 3 days
                new EventResponseDTO(
                    1L, "Conference",
                    ZonedDateTime.of(2025, 6, 27, 8, 0, 0, 0, ZoneId.of("UTC")),  // Friday 8 AM UTC
                    ZonedDateTime.of(2025, 6, 29, 17, 0, 0, 0, ZoneId.of("UTC")), // Sunday 5 PM UTC
                    3300, // durationMinutes (55 hours)
                    "Europe/London", "Europe/London", // timezone fields
                    "Three-day conference", 
                    creator.getUsername(), creator.getTimezone(),
                    labelDto, false, false, false
                ),
                // Event spanning 2 days
                new EventResponseDTO(
                    2L, "Workshop",
                    ZonedDateTime.of(2025, 6, 28, 14, 0, 0, 0, ZoneId.of("UTC")), // Saturday 2 PM UTC
                    ZonedDateTime.of(2025, 6, 29, 10, 0, 0, 0, ZoneId.of("UTC")), // Sunday 10 AM UTC
                    1200, // durationMinutes (20 hours)
                    "Europe/London", "Europe/London", // timezone fields
                    "Two-day workshop", 
                    creator.getUsername(), creator.getTimezone(),
                    labelDto, false, false, false
                )
            );

            // Act
            WeekViewDTO weekView = eventBO.generateWeekViewData(USER_ID, LocalDate.of(2025, 6, 29), userZoneId, events, List.of());
            
            // Assert
            Map<LocalDate, List<EventResponseDTO>> dayEvents = weekView.days().stream()
                .collect(java.util.stream.Collectors.toMap(
                    DayViewDTO::date,
                    DayViewDTO::events
                ));
            
            // Friday (June 27) should have the conference event
            List<EventResponseDTO> fridayEvents = dayEvents.get(LocalDate.of(2025, 6, 27));
            assertEquals(1, fridayEvents.size());
            assertEquals("Conference", fridayEvents.get(0).name());
            
            // Saturday (June 28) should have both events
            List<EventResponseDTO> saturdayEvents = dayEvents.get(LocalDate.of(2025, 6, 28));
            assertEquals(2, saturdayEvents.size());
            
            // Sunday (June 29) should have both events
            List<EventResponseDTO> sundayEvents = dayEvents.get(LocalDate.of(2025, 6, 29));
            assertEquals(2, sundayEvents.size());
        }

        @Test
        void testGroupEventsByDayWithMidnightCrossingEvents() {
            // Arrange
            ZoneId userZoneId = ZoneId.of("Asia/Tokyo"); // UTC+9
            User creator = TestUtils.createValidUserEntityWithId();
            Label label = TestUtils.createValidLabelWithId(VALID_LABEL_ID, creator);
            LabelResponseDTO labelDto = TestUtils.createLabelResponseDTO(label);
            
            // Use anchor date June 26 (Thursday) to test within the week June 23-29
            LocalDate anchorDate = LocalDate.of(2025, 6, 26);
            
            List<EventResponseDTO> events = List.of(
                // Event that crosses midnight in Tokyo timezone (Friday night to Saturday morning)
                new EventResponseDTO(
                    1L, "Late Night Event",
                    ZonedDateTime.of(2025, 6, 27, 14, 30, 0, 0, ZoneId.of("UTC")), // 11:30 PM Friday JST
                    ZonedDateTime.of(2025, 6, 27, 16, 30, 0, 0, ZoneId.of("UTC")), // 1:30 AM Saturday JST
                    120, // durationMinutes
                    "Asia/Tokyo", "Asia/Tokyo", // timezone fields
                    "Late night event", 
                    creator.getUsername(), creator.getTimezone(),
                    labelDto, false, false, false
                ),
                // Event that starts and ends on same day in Tokyo (Thursday)
                new EventResponseDTO(
                    2L, "Same Day Event",
                    ZonedDateTime.of(2025, 6, 26, 1, 0, 0, 0, ZoneId.of("UTC")),   // 10:00 AM Thursday JST
                    ZonedDateTime.of(2025, 6, 26, 3, 0, 0, 0, ZoneId.of("UTC")),   // 12:00 PM Thursday JST
                    120, // durationMinutes
                    "Asia/Tokyo", "Asia/Tokyo", // timezone fields
                    "Same day event", 
                    creator.getUsername(), creator.getTimezone(),
                    labelDto, false, false, false
                )
            );

            // Act
            WeekViewDTO weekView = eventBO.generateWeekViewData(USER_ID, anchorDate, userZoneId, events, List.of());
            
            // Assert
            assertEquals(7, weekView.days().size());
            
            Map<LocalDate, List<EventResponseDTO>> dayEvents = weekView.days().stream()
                .collect(java.util.stream.Collectors.toMap(
                    DayViewDTO::date,
                    DayViewDTO::events
                ));
            
            // Thursday (June 26) should have the Same Day Event
            List<EventResponseDTO> thursdayEvents = dayEvents.getOrDefault(LocalDate.of(2025, 6, 26), List.of());
            assertEquals(1, thursdayEvents.size());
            assertEquals("Same Day Event", thursdayEvents.get(0).name());
            
            // Friday (June 27) should have the Late Night Event (starts on Friday night JST)
            List<EventResponseDTO> fridayEvents = dayEvents.getOrDefault(LocalDate.of(2025, 6, 27), List.of());
            assertEquals(1, fridayEvents.size());
            assertEquals("Late Night Event", fridayEvents.get(0).name());
            
            // Saturday (June 28) should also have the Late Night Event (ends on Saturday morning JST)
            List<EventResponseDTO> saturdayEvents = dayEvents.getOrDefault(LocalDate.of(2025, 6, 28), List.of());
            assertEquals(1, saturdayEvents.size());
            assertEquals("Late Night Event", saturdayEvents.get(0).name());
        }
    }

    @Nested
    class SolidifyRecurrencesPerformanceTests {

        @Test
        void testSolidifyRecurrencesWithLargeDataset() {
            // Arrange
            User user = TestUtils.createValidUserEntityWithId();
            ZoneId userZoneId = ZoneId.of(user.getTimezone());
            ZonedDateTime startTime = ZonedDateTime.now(fixedClock).minusDays(30);
            ZonedDateTime endTime = ZonedDateTime.now(fixedClock);
            
            // Create 50 recurring events (simulating a power user)
            List<RecurringEvent> largeRecurringEventList = IntStream.range(0, 50)
                .mapToObj(i -> TestUtils.createValidRecurringEventWithId(user, (long) i, fixedClock))
                .toList();
            
            when(recurringEventBO.getConfirmedRecurringEventsForUserInRange(
                    user.getId(),
                    startTime.toLocalDate(),
                    endTime.toLocalDate()
            )).thenReturn(largeRecurringEventList);
            
            // Mock recurrence rule service to return 10 occurrences per recurring event
            for (RecurringEvent recurrence : largeRecurringEventList) {
                List<LocalDate> occurrences = IntStream.range(0, 10)
                    .mapToObj(i -> startTime.toLocalDate().plusDays(i * 3))
                    .toList();
                
                when(recurrenceRuleService.expandRecurrence(
                    recurrence.getRecurrenceRule().getParsed(),
                    startTime.toLocalDate(),
                    endTime.toLocalDate(),
                    recurrence.getSkipDays()
                )).thenReturn(occurrences);
            }
            
            // Mock repository to return empty list (no existing events)
            when(eventRepository.findConfirmedAndRecurringDraftsByUserAndRecurrenceIdBetween(
                any(), any(), any(), any()
            )).thenReturn(List.of());
            
            // Mock event repository save to return the event
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act - Measure performance
            long startTimeMs = System.currentTimeMillis();
            eventBO.solidifyRecurrences(user.getId(), startTime, endTime, userZoneId);
            long endTimeMs = System.currentTimeMillis();
            long executionTime = endTimeMs - startTimeMs;

            // Assert
            // Verify the method completed within reasonable time (should be < 1 second for this dataset)
            assertTrue(executionTime < 1000, "Solidify recurrences took too long: " + executionTime + "ms");
            
            // Verify that recurring events were fetched
            verify(recurringEventBO).getConfirmedRecurringEventsForUserInRange(
                user.getId(),
                startTime.toLocalDate(),
                endTime.toLocalDate()
            );
            
            // Verify that recurrence rules were expanded for each recurring event
            verify(recurrenceRuleService, org.mockito.Mockito.times(50)).expandRecurrence(any(), any(), any(), any());
            
            // Verify that events were saved (50 recurring events × 10 occurrences each = 500 events)
            verify(eventRepository, org.mockito.Mockito.times(500)).save(any(Event.class));
        }

        @Test
        void testSolidifyRecurrencesWithExistingEventsPerformance() {
            // Arrange
            User user = TestUtils.createValidUserEntityWithId();
            ZoneId userZoneId = ZoneId.of(user.getTimezone());
            ZonedDateTime startTime = ZonedDateTime.now(fixedClock).minusDays(10);
            ZonedDateTime endTime = ZonedDateTime.now(fixedClock);
            
            // Create 10 recurring events
            List<RecurringEvent> recurringEvents = IntStream.range(0, 10)
                .mapToObj(i -> TestUtils.createValidRecurringEventWithId(user, (long) i, fixedClock))
                .toList();
            
            when(recurringEventBO.getConfirmedRecurringEventsForUserInRange(
                    user.getId(),
                    startTime.toLocalDate(),
                    endTime.toLocalDate()
            )).thenReturn(recurringEvents);
            
            // Mock recurrence rule service to return 5 occurrences per recurring event
            for (RecurringEvent recurrence : recurringEvents) {
                List<LocalDate> occurrences = IntStream.range(0, 5)
                    .mapToObj(i -> startTime.toLocalDate().plusDays(i * 2))
                    .toList();
                
                when(recurrenceRuleService.expandRecurrence(
                    recurrence.getRecurrenceRule().getParsed(),
                    startTime.toLocalDate(),
                    endTime.toLocalDate(),
                    recurrence.getSkipDays()
                )).thenReturn(occurrences);
            }
            
            // Mock repository to return existing events for some dates (simulating partial solidification)
            when(eventRepository.findConfirmedAndRecurringDraftsByUserAndRecurrenceIdBetween(
                any(), any(), any(), any()
            )).thenReturn(IntStream.range(0, 3)
                .mapToObj(i -> TestUtils.createValidScheduledEventWithId((long) i, user, fixedClock))
                .toList());
            
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act - Measure performance
            long startTimeMs = System.currentTimeMillis();
            eventBO.solidifyRecurrences(user.getId(), startTime, endTime, userZoneId);
            long endTimeMs = System.currentTimeMillis();
            long executionTime = endTimeMs - startTimeMs;

            // Assert
            // Should complete quickly even with existing event checks
            assertTrue(executionTime < 500, "Solidify recurrences with existing events took too long: " + executionTime + "ms");
            
            // Verify repository interactions
            verify(eventRepository, org.mockito.Mockito.times(10)).findConfirmedAndRecurringDraftsByUserAndRecurrenceIdBetween(
                any(), any(), any(), any()
            );
            
            // Should save fewer events due to existing events (some dates already have events)
            verify(eventRepository, org.mockito.Mockito.atLeast(1)).save(any(Event.class));
            verify(eventRepository, org.mockito.Mockito.atMost(50)).save(any(Event.class));
        }

        @Test
        void testSolidifyRecurrencesWithNoRecurringEvents() {
            // Arrange
            User user = TestUtils.createValidUserEntityWithId();
            ZoneId userZoneId = ZoneId.of(user.getTimezone());
            ZonedDateTime startTime = ZonedDateTime.now(fixedClock).minusDays(7);
            ZonedDateTime endTime = ZonedDateTime.now(fixedClock);
            
            when(recurringEventBO.getConfirmedRecurringEventsForUserInRange(
                    user.getId(),
                    startTime.toLocalDate(),
                    endTime.toLocalDate()
            )).thenReturn(List.of());

            // Act - Measure performance
            long startTimeMs = System.currentTimeMillis();
            eventBO.solidifyRecurrences(user.getId(), startTime, endTime, userZoneId);
            long endTimeMs = System.currentTimeMillis();
            long executionTime = endTimeMs - startTimeMs;

            // Assert
            // Should complete very quickly with no recurring events
            assertTrue(executionTime < 50, "Solidify recurrences with no events took too long: " + executionTime + "ms");
            
            // Verify minimal repository interactions
            verify(recurringEventBO).getConfirmedRecurringEventsForUserInRange(
                user.getId(),
                startTime.toLocalDate(),
                endTime.toLocalDate()
            );
            
            // Should not interact with other services
            verify(recurrenceRuleService, org.mockito.Mockito.never()).expandRecurrence(any(), any(), any(), any());
            verify(eventRepository, org.mockito.Mockito.never()).save(any(Event.class));
        }
    }

}
