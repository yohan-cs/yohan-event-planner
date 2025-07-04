package com.yohan.event_planner.business;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.exception.ConflictException;
import com.yohan.event_planner.exception.EventNotFoundException;
import com.yohan.event_planner.exception.InvalidTimeException;
import com.yohan.event_planner.repository.EventRepository;
import com.yohan.event_planner.util.TestConstants;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.yohan.event_planner.util.TestConstants.EVENT_ID;

import static com.yohan.event_planner.util.TestConstants.VALID_EVENT_END;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EventBOImplTest {

    private EventRepository eventRepository;

    private EventBOImpl eventBO;

    @BeforeEach
    void setUp() {
        this.eventRepository = mock(EventRepository.class);

        eventBO = new EventBOImpl(eventRepository);
    }

    @Nested
    class GetEventByIdTests {

        @Test
        void testGetEventById_eventExists_returnsEvent() {
            // Arrange
            User creator = TestUtils.createUserEntityWithId();
            Event expectedEvent = TestUtils.createTimedEventEntity(creator);

            // Mocks
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(expectedEvent));

            // Act
            Optional<Event> result = eventBO.getEventById(EVENT_ID);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(expectedEvent, result.get());
        }

        @Test
        void testGetEventById_eventNotFound_returnsEmptyOptional() {
            // Mocks
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            // Act
            Optional<Event> result = eventBO.getEventById(EVENT_ID);

            // Assert
            assertFalse(result.isPresent());
            verify(eventRepository).findById(EVENT_ID);
        }
    }

    @Nested
    class GetEventsByUserTests {

        @Test
        void testGetEventsByUser_userHasEvents_returnsListOfEvents() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            Event event1 = TestUtils.createTimedEventEntityWithId(101L, user);
            Event event2 = TestUtils.createTimedEventEntityWithId(102L, user);
            List<Event> expectedEvents = List.of(event1, event2);

            // Mocks
            when(eventRepository.findAllByCreatorId(user.getId())).thenReturn(expectedEvents);

            // Act
            List<Event> result = eventBO.getEventsByUser(user.getId());

            // Assert
            assertEquals(2, result.size());
            assertTrue(result.contains(event1));
            assertTrue(result.contains(event2));
            verify(eventRepository).findAllByCreatorId(user.getId());
        }

        @Test
        void testGetEventsByUser_userHasNoEvents_returnsEmptyList() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();

            // Mocks
            when(eventRepository.findAllByCreatorId(user.getId())).thenReturn(Collections.emptyList());

            // Act
            List<Event> result = eventBO.getEventsByUser(user.getId());

            // Assert
            assertTrue(result.isEmpty());
            verify(eventRepository).findAllByCreatorId(user.getId());
        }

    }

    @Nested
    class GetEventByUserAndDateRangeTests {

        @Test
        void testGetEventByUserAndDateRange_eventsInRange_returnsEvents() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();

            ZonedDateTime rangeStart = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("America/New_York"));
            ZonedDateTime rangeEnd   = ZonedDateTime.of(2024, 1, 2, 0, 0, 0, 0, ZoneId.of("America/New_York"));

            Event event1 = TestUtils.createTimedEventEntityWithId(201L, user); // Assume it's within range
            Event event2 = TestUtils.createTimedEventEntityWithId(202L, user); // Assume also within range

            List<Event> expectedEvents = List.of(event1, event2);

            // Mocks
            when(eventRepository.findAllByCreatorIdAndStartTimeBetween(user.getId(), rangeStart, rangeEnd)).thenReturn(expectedEvents);

            // Act
            List<Event> result = eventBO.getEventsByUserAndDateRange(user.getId(), rangeStart, rangeEnd);

            // Assert
            assertEquals(2, result.size());
            assertTrue(result.contains(event1));
            assertTrue(result.contains(event2));
            verify(eventRepository).findAllByCreatorIdAndStartTimeBetween(user.getId(), rangeStart, rangeEnd);
        }

        @Test
        void testGetEventByUserAndDateRange_noEventsInRange_returnsEmptyList() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();

            ZonedDateTime rangeStart = ZonedDateTime.of(2023, 12, 1, 0, 0, 0, 0, ZoneId.of("America/New_York"));
            ZonedDateTime rangeEnd   = ZonedDateTime.of(2023, 12, 2, 0, 0, 0, 0, ZoneId.of("America/New_York"));

            // Mocks
            when(eventRepository.findAllByCreatorIdAndStartTimeBetween(user.getId(), rangeStart, rangeEnd)).thenReturn(Collections.emptyList());

            // Act
            List<Event> result = eventBO.getEventsByUserAndDateRange(user.getId(), rangeStart, rangeEnd);

            // Assert
            assertTrue(result.isEmpty());
            verify(eventRepository).findAllByCreatorIdAndStartTimeBetween(user.getId(), rangeStart, rangeEnd);
        }

        @Test
        void testGetEventByUserAndDateRange_untimedEventInRange_returnsEvent() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();

            ZonedDateTime rangeStart = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("America/New_York"));
            ZonedDateTime rangeEnd   = ZonedDateTime.of(2024, 1, 10, 0, 0, 0, 0, ZoneId.of("America/New_York"));

            Event untimedEvent = TestUtils.createUntimedEventEntityWithId(301L, user);
            untimedEvent.setStartTime(ZonedDateTime.of(2024, 1, 5, 12, 0, 0, 0, ZoneId.of("America/New_York")));

            List<Event> expectedEvents = List.of(untimedEvent);

            // Mocks
            when(eventRepository.findAllByCreatorIdAndStartTimeBetween(user.getId(), rangeStart, rangeEnd))
                    .thenReturn(expectedEvents);

            // Act
            List<Event> result = eventBO.getEventsByUserAndDateRange(user.getId(), rangeStart, rangeEnd);

            // Assert
            assertEquals(1, result.size());
            assertTrue(result.contains(untimedEvent));
            verify(eventRepository).findAllByCreatorIdAndStartTimeBetween(user.getId(), rangeStart, rangeEnd);
        }

        @Test
        void testGetEventByUserAndDateRange_untimedEventOutOfRange_returnsEmptyList() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();

            ZonedDateTime rangeStart = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("America/New_York"));
            ZonedDateTime rangeEnd   = ZonedDateTime.of(2024, 1, 2, 0, 0, 0, 0, ZoneId.of("America/New_York"));

            Event untimedEvent = TestUtils.createUntimedEventEntityWithId(302L, user);
            untimedEvent.setStartTime(ZonedDateTime.of(2024, 1, 10, 12, 0, 0, 0, ZoneId.of("America/New_York")));

            // Mocks
            when(eventRepository.findAllByCreatorIdAndStartTimeBetween(user.getId(), rangeStart, rangeEnd))
                    .thenReturn(Collections.emptyList());

            // Act
            List<Event> result = eventBO.getEventsByUserAndDateRange(user.getId(), rangeStart, rangeEnd);

            // Assert
            assertTrue(result.isEmpty());
            verify(eventRepository).findAllByCreatorIdAndStartTimeBetween(user.getId(), rangeStart, rangeEnd);
        }
    }

    @Nested
    class CreateEventTests {

        @Test
        void testCreateEvent_validEvent_savesAndReturnsEventWithId() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            Event inputEvent = TestUtils.createTimedEventEntity(user);
            Event savedEvent = TestUtils.createTimedEventEntityWithId(301L, user);

            // Mocks
            when(eventRepository.save(inputEvent)).thenReturn(savedEvent);

            // Act
            Event result = eventBO.createEvent(inputEvent);

            // Assert
            assertNotNull(result.getId());
            assertEquals(savedEvent.getName(), result.getName());
            verify(eventRepository).save(inputEvent);
        }

        @Test
        void testCreateEvent_invalidTime_throwsInvalidTimeException() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            Event invalidEvent = new Event(
                    "Invalid Event",
                    VALID_EVENT_END,
                    user
            );
            invalidEvent.setEndTime(VALID_EVENT_END);

            // Act + Assert
            assertThrows(InvalidTimeException.class, () -> eventBO.createEvent(invalidEvent));
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        void testCreateEvent_conflictingEventExists_throwsConflictException() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            Event newEvent = TestUtils.createTimedEventEntity(user);
            Event conflictingEvent = TestUtils.createTimedEventEntityWithId(999L, user);

            // Mocks
            when(eventRepository.findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThan(
                    eq(user.getId()),
                    eq(newEvent.getEndTime()),
                    eq(newEvent.getStartTime())
            )).thenReturn(Optional.of(conflictingEvent));

            // Act + Assert
            assertThrows(ConflictException.class, () -> eventBO.createEvent(newEvent));
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        void testCreateEvent_validUntimedEvent_savesAndReturnsEventWithId() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            Event inputEvent = TestUtils.createUntimedEventEntity(user);
            Event savedEvent = TestUtils.createUntimedEventEntityWithId(302L, user);

            // Mocks
            when(eventRepository.save(inputEvent)).thenReturn(savedEvent);

            // Act
            Event result = eventBO.createEvent(inputEvent);

            // Assert
            assertNotNull(result.getId());
            assertEquals(savedEvent.getName(), result.getName());
            verify(eventRepository).save(inputEvent);
        }
    }

    @Nested
    class UpdateEventTests {

        @Test
        void testUpdateEvent_validInput_savesAndReturnsEvent() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            Event inputEvent = TestUtils.createTimedEventEntityWithId(103L, user);
            Event savedEvent = TestUtils.createTimedEventEntityWithId(103L, user); // Same but persisted

            // Mocks
            when(eventRepository.findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                    eq(user.getId()), any(), any(), eq(inputEvent.getId())))
                    .thenReturn(Optional.empty());
            when(eventRepository.save(inputEvent)).thenReturn(savedEvent);

            // Act
            Event result = eventBO.updateEvent(inputEvent);

            // Assert
            assertNotNull(result);
            assertEquals(savedEvent.getId(), result.getId());
            verify(eventRepository).save(inputEvent);
        }

        @Test
        void testUpdateEvent_invalidTimes_throwsInvalidTimeException() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            Event invalidEvent = TestUtils.createTimedEventEntityWithId(101L, user);
            invalidEvent.setStartTime(VALID_EVENT_END);   // Start == End
            invalidEvent.setEndTime(VALID_EVENT_END);

            // Act + Assert
            assertThrows(InvalidTimeException.class, () -> eventBO.updateEvent(invalidEvent));
            verify(eventRepository, never()).save(any());
        }

        @Test
        void testUpdateEvent_conflictingEvent_throwsConflictException() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            Event eventToUpdate = TestUtils.createTimedEventEntityWithId(102L, user);

            Event conflict = TestUtils.createTimedEventEntityWithId(999L, user);

            // Mocks
            when(eventRepository.findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                    eq(user.getId()), any(), any(), eq(eventToUpdate.getId())))
                    .thenReturn(Optional.of(conflict));

            // Act + Assert
            assertThrows(ConflictException.class, () -> eventBO.updateEvent(eventToUpdate));
            verify(eventRepository, never()).save(any());
        }

        @Test
        void testUpdateEvent_addsEndTime_savesUpdatedEvent() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            Event inputEvent = TestUtils.createUntimedEventEntityWithId(104L, user);
            inputEvent.setEndTime(VALID_EVENT_END);

            // Recalculate duration manually
            long minutes = java.time.Duration.between(
                    inputEvent.getStartTime().withZoneSameInstant(java.time.ZoneOffset.UTC),
                    VALID_EVENT_END.withZoneSameInstant(java.time.ZoneOffset.UTC)
            ).toMinutes();
            inputEvent.setDurationMinutes((int) minutes);

            Event savedEvent = TestUtils.createTimedEventEntityWithId(104L, user);

            // Mocks
            when(eventRepository.findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                    eq(user.getId()), any(), any(), eq(inputEvent.getId())))
                    .thenReturn(Optional.empty());
            when(eventRepository.save(inputEvent)).thenReturn(savedEvent);

            // Act
            Event result = eventBO.updateEvent(inputEvent);

            // Assert
            assertNotNull(result);
            assertEquals(savedEvent.getEndTime(), result.getEndTime());
            verify(eventRepository).save(inputEvent);
        }

        @Test
        void testUpdateEvent_removesEndTime_savesUpdatedEvent() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            Event inputEvent = TestUtils.createTimedEventEntityWithId(105L, user);
            inputEvent.setEndTime(null);
            inputEvent.setDurationMinutes(null);

            Event savedEvent = TestUtils.createUntimedEventEntityWithId(105L, user);

            // Mocks
            when(eventRepository.findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                    eq(user.getId()), any(), any(), eq(inputEvent.getId())))
                    .thenReturn(Optional.empty());
            when(eventRepository.save(inputEvent)).thenReturn(savedEvent);

            // Act
            Event result = eventBO.updateEvent(inputEvent);

            // Assert
            assertNotNull(result);
            assertNull(result.getEndTime());
            verify(eventRepository).save(inputEvent);
        }
    }

    @Nested
    class DeleteEventTests {

        @Test
        void testDeleteEvent_existingEvent_deletesSuccessfully() {
            // Arrange
            Long eventId = 201L;
            Event event = TestUtils.createTimedEventEntityWithId(eventId, TestUtils.createUserEntityWithId());

            // Mocks
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            // Act
            eventBO.deleteEvent(eventId);

            // Assert
            verify(eventRepository).deleteById(eventId);
        }
    }

    @Nested
    class ValidateEventTimesTests {

        @Test
        void testValidateEventTimes_validTimes_doesNotThrow() {
            // Arrange
            ZonedDateTime validStart = ZonedDateTime.parse("2025-01-01T10:00:00Z");
            ZonedDateTime validEnd = ZonedDateTime.parse("2025-01-01T12:00:00Z");
            Event event = new Event(
                    "Valid Event",
                    validStart,
                    TestUtils.createUserEntityWithId()
            );
            event.setEndTime(validEnd);

            // Act & Assert
            assertDoesNotThrow(() -> eventBO.validateEventTimes(event));
        }

        @Test
        void testValidateEventTimes_invalidTimes_throwsInvalidTimeException() {
            // Arrange
            ZonedDateTime invalidStart = ZonedDateTime.parse("2025-01-01T12:00:00Z");
            ZonedDateTime invalidEnd = ZonedDateTime.parse("2025-01-01T10:00:00Z");

            Event event = new Event(
                    "Invalid Event",
                    invalidStart,
                    TestUtils.createUserEntityWithId()
            );
            event.setEndTime(invalidEnd);

            // Act & Assert
            assertThrows(InvalidTimeException.class, () -> eventBO.validateEventTimes(event));
        }

        @Test
        void testValidateEventTimes_noEndTime_doesNotThrow() {
            // Arrange
            ZonedDateTime start = ZonedDateTime.parse("2025-01-01T08:00:00Z");
            Event event = new Event(
                    "Open-Ended Event",
                    start,
                    TestUtils.createUserEntityWithId()
            );
            // No end time is set

            // Act & Assert
            assertDoesNotThrow(() -> eventBO.validateEventTimes(event));
        }
    }

    @Nested
    class CheckForConflictsTests {

        @Test
        void testCheckForConflicts_noConflict_doesNotThrow() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            Event event = TestUtils.createTimedEventEntity(user);

            when(eventRepository.findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThan(
                    user.getId(), event.getEndTime(), event.getStartTime()))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertDoesNotThrow(() -> eventBO.checkForConflicts(event));
        }

        @Test
        void testCheckForConflicts_conflictExists_throwsConflictException() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            Event event = TestUtils.createTimedEventEntity(user); // No ID = new event
            Event conflicting = TestUtils.createTimedEventEntityWithId(999L, user);

            // Mocks
            when(eventRepository.findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThan(
                    user.getId(), event.getEndTime(), event.getStartTime()))
                    .thenReturn(Optional.of(conflicting));

            // Act & Assert
            assertThrows(ConflictException.class, () -> eventBO.checkForConflicts(event));
        }

        void testCheckForConflicts_adjacentEvents_noConflict() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            Event event = TestUtils.createTimedEventEntity(user); // No ID = new event
            Event adjacentEvent = TestUtils.createTimedEventEntityWithId(999L, user);

            // Set the start and end time of the event to be adjacent
            event.setStartTime(ZonedDateTime.parse("2025-06-05T10:00:00Z"));
            event.setEndTime(ZonedDateTime.parse("2025-06-05T12:00:00Z"));

            adjacentEvent.setStartTime(ZonedDateTime.parse("2025-06-05T12:00:00Z"));
            adjacentEvent.setEndTime(ZonedDateTime.parse("2025-06-05T14:00:00Z"));

            // Mocks
            when(eventRepository.findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                    user.getId(), event.getEndTime(), event.getStartTime(), event.getId()))  // Exclude the event being updated
                    .thenReturn(Optional.empty()); // No conflict, adjacent event shouldn't trigger a conflict

            // Act & Assert
            // Adjacent events should not be considered a conflict, so no exception should be thrown
            assertDoesNotThrow(() -> eventBO.checkForConflicts(event));
        }

        @Test
        void testCheckForConflicts_untimedEvent_noConflict_doesNotThrow() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            Event untimedEvent = TestUtils.createUntimedEventEntity(user);

            // Mocks
            when(eventRepository.findFirstByCreatorIdAndStartTimeLessThanAndEndTimeGreaterThan(
                    any(), any(), any())).thenReturn(Optional.empty());

            // Act & Assert
            assertDoesNotThrow(() -> eventBO.checkForConflicts(untimedEvent));
        }

        @Test
        void testCheckForConflicts_untimedEvent_conflictExists_throwsConflictException() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            Event untimedEvent = TestUtils.createUntimedEventEntity(user);
            Event conflictingEvent = TestUtils.createUntimedEventEntityWithId(999L, user);

            // Mocks
            when(eventRepository.findFirstByCreatorIdAndEndTimeIsNullAndStartTimeEquals(
                    user.getId(), untimedEvent.getStartTime()))
                    .thenReturn(Optional.of(conflictingEvent));

            // Act & Assert
            assertThrows(ConflictException.class, () -> eventBO.checkForConflicts(untimedEvent));
        }

        @Test
        void testCheckForConflicts_multipleUntimedEvents_sameStartTime_throwsConflictException() {
            // Arrange
            User user = TestUtils.createUserEntityWithId();
            Event newUntimedEvent = TestUtils.createUntimedEventEntity(user);
            Event existingUntimedEvent = TestUtils.createUntimedEventEntityWithId(888L, user); // Same start time

            // Mocks
            when(eventRepository.findFirstByCreatorIdAndEndTimeIsNullAndStartTimeEquals(
                    user.getId(), newUntimedEvent.getStartTime()))
                    .thenReturn(Optional.of(existingUntimedEvent));

            // Act & Assert
            assertThrows(ConflictException.class, () -> eventBO.checkForConflicts(newUntimedEvent));
        }
    }
}
