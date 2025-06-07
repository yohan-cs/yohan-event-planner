package com.yohan.event_planner.service;

import com.yohan.event_planner.business.EventBO;
import com.yohan.event_planner.business.handler.EventPatchHandler;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.EventCreateDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.exception.EventNotFoundException;
import com.yohan.event_planner.exception.EventOwnershipException;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import com.yohan.event_planner.util.EventResponseDTOAssertions;
import com.yohan.event_planner.util.TestConstants;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_EVENT_ACCESS;
import static com.yohan.event_planner.util.TestConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EventServiceImplTest {

    private EventBO eventBO;
    private EventPatchHandler eventPatchHandler;
    private OwnershipValidator ownershipValidator;
    private AuthenticatedUserProvider authenticatedUserProvider;

    private EventServiceImpl eventService;

    @BeforeEach
    void setUp() {
        eventBO = mock(EventBO.class);
        eventPatchHandler = mock(EventPatchHandler.class);
        ownershipValidator = mock(OwnershipValidator.class);
        authenticatedUserProvider = mock(AuthenticatedUserProvider.class);
        eventService = new EventServiceImpl(eventBO, eventPatchHandler, ownershipValidator, authenticatedUserProvider);
    }

    @Nested
    class GetEventByIdTests {

        @Test
        void testGetEventById_eventExists_returnsDTO() {
            // Arrange
            User creator = TestUtils.createUserEntityWithId(1L);
            creator.setTimezone("UTC");

            Event event = TestUtils.createTimedEventEntityWithId(100L, creator);
            event.setDescription(TestConstants.VALID_EVENT_DESCRIPTION);

            EventResponseDTO expectedDto = TestUtils.createEventResponseDTO(event);

            // Mocks
            when(eventBO.getEventById(100L)).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(creator);

            // Act
            EventResponseDTO result = eventService.getEventById(100L);

            // Assert
            assertEquals(expectedDto, result);
        }



        @Test
        void testGetEventById_eventNotFound_throwsException() {
            // Arrange
            Long eventId = 100L;
            when(eventBO.getEventById(eventId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(EventNotFoundException.class, () -> eventService.getEventById(eventId));
            verify(eventBO).getEventById(eventId);
        }
    }

    @Nested
    class GetEventsByUserTests {
        @Test
        void testGetEventsByUser_returnsEmptyList() {
            // Arrange
            Long userId = 1L;
            User viewer = TestUtils.createUserEntityWithId(userId);

            // Mocks
            when(eventBO.getEventsByUser(userId)).thenReturn(List.of());
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);

            // Act
            List<EventResponseDTO> result = eventService.getEventsByUser(userId);

            // Assert
            assertTrue(result.isEmpty());
            verify(eventBO).getEventsByUser(userId);
            verify(authenticatedUserProvider).getCurrentUser();
        }

        @Test
        void testGetEventsByUser_returnsListOfEvents() {
            // Arrange
            Long userId = 1L;
            User viewer = TestUtils.createUserEntityWithId(userId);
            viewer.setTimezone("UTC");

            Event event1 = TestUtils.createTimedEventEntityWithId(100L, viewer);
            event1.setDescription(TestConstants.VALID_EVENT_DESCRIPTION);
            Event event2 = TestUtils.createTimedEventEntityWithId(101L, viewer);
            event2.setDescription("Another event");

            List<Event> eventList = List.of(event1, event2);
            List<EventResponseDTO> expected = eventList.stream()
                    .map(TestUtils::createEventResponseDTO)
                    .toList();

            // Mocks
            when(eventBO.getEventsByUser(userId)).thenReturn(eventList);
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);

            // Act
            List<EventResponseDTO> result = eventService.getEventsByUser(userId);

            // Assert
            assertEquals(expected, result);
            verify(eventBO).getEventsByUser(userId);
            verify(authenticatedUserProvider).getCurrentUser();
        }
    }

    @Nested
    class GetEventByUserAndDateRangeTests {

        @Test
        void testGetEventsByUserAndDateRange_returnsEmptyList() {
            // Arrange
            Long userId = 1L;
            ZonedDateTime start = VALID_EVENT_START.minusDays(10);
            ZonedDateTime end = VALID_EVENT_END.minusDays(5);
            User viewer = TestUtils.createUserEntityWithId(userId);

            when(eventBO.getEventsByUserAndDateRange(userId, start, end)).thenReturn(List.of());
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);

            // Act
            List<EventResponseDTO> result = eventService.getEventsByUserAndDateRange(userId, start, end);

            // Assert
            assertTrue(result.isEmpty());
            verify(eventBO).getEventsByUserAndDateRange(userId, start, end);
            verify(authenticatedUserProvider).getCurrentUser();
        }

        @Test
        void testGetEventsByUserAndDateRange_returnsListOfEvents() {
            // Arrange
            Long userId = 1L;
            User viewer = TestUtils.createUserEntityWithId(userId);
            viewer.setTimezone("UTC");

            ZonedDateTime start = VALID_EVENT_START.minusHours(1);
            ZonedDateTime end = VALID_EVENT_END.plusHours(2);

            Event event1 = TestUtils.createTimedEventEntityWithId(200L, viewer);
            event1.setDescription(TestConstants.VALID_EVENT_DESCRIPTION);

            Event event2 = TestUtils.createTimedEventEntityWithId(201L, viewer);
            event2.setDescription("Another event in range");

            List<Event> eventList = List.of(event1, event2);
            List<EventResponseDTO> expected = eventList.stream()
                    .map(TestUtils::createEventResponseDTO)
                    .collect(Collectors.toList());

            when(eventBO.getEventsByUserAndDateRange(userId, start, end)).thenReturn(eventList);
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);

            // Act
            List<EventResponseDTO> result = eventService.getEventsByUserAndDateRange(userId, start, end);

            // Assert
            assertEquals(expected, result);
            verify(eventBO).getEventsByUserAndDateRange(userId, start, end);
            verify(authenticatedUserProvider).getCurrentUser();
        }

        @Test
        void testGetEventsByUserAndDateRange_includesUntimedEvent() {
            // Arrange
            Long userId = 1L;
            User viewer = TestUtils.createUserEntityWithId(userId);
            viewer.setTimezone("UTC");

            ZonedDateTime start = VALID_EVENT_START.minusHours(1);
            ZonedDateTime end = VALID_EVENT_START.plusHours(1);

            Event untimedEvent = TestUtils.createUntimedEventEntityWithId(200L, viewer);
            untimedEvent.setDescription("Untimed Event");

            List<Event> eventList = List.of(untimedEvent);
            List<EventResponseDTO> expected = eventList.stream()
                    .map(TestUtils::createEventResponseDTO)
                    .toList();

            when(eventBO.getEventsByUserAndDateRange(userId, start, end)).thenReturn(eventList);
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);

            // Act
            List<EventResponseDTO> result = eventService.getEventsByUserAndDateRange(userId, start, end);

            // Assert
            assertEquals(expected, result);
        }
    }

    @Nested
    class GetEventsByCurrentUserTests {

        @Test
        void testGetEventsByCurrentUser_returnsEmptyList() {
            // Arrange
            User currentUser = TestUtils.createUserEntityWithId(1L);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(eventBO.getEventsByUser(currentUser.getId())).thenReturn(List.of());

            // Act
            List<EventResponseDTO> result = eventService.getEventsByCurrentUser();

            // Assert
            assertTrue(result.isEmpty());
            verify(authenticatedUserProvider).getCurrentUser();
            verify(eventBO).getEventsByUser(currentUser.getId());
        }

        @Test
        void testGetEventsByCurrentUser_returnsList() {
            // Arrange
            User currentUser = TestUtils.createUserEntityWithId(1L);
            currentUser.setTimezone("UTC");

            Event e1 = TestUtils.createTimedEventEntityWithId(100L, currentUser);
            e1.setDescription("Desc A");
            Event e2 = TestUtils.createTimedEventEntityWithId(101L, currentUser);
            e2.setDescription("Desc B");

            List<Event> events = List.of(e1, e2);
            List<EventResponseDTO> expected = events.stream()
                    .map(TestUtils::createEventResponseDTO)
                    .toList();

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(currentUser);
            when(eventBO.getEventsByUser(currentUser.getId())).thenReturn(events);

            // Act
            List<EventResponseDTO> result = eventService.getEventsByCurrentUser();

            // Assert
            assertEquals(expected, result);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(eventBO).getEventsByUser(currentUser.getId());
        }
    }

    @Nested
    class GetEventsByCurrentUserAndDateRangeTests {

        @Test
        void testGetEventsByCurrentUserAndDateRange_returnsEmptyList() {
            // Arrange
            User user = TestUtils.createUserEntityWithId(1L);
            ZonedDateTime start = VALID_EVENT_START.minusDays(1);
            ZonedDateTime end = VALID_EVENT_END.plusDays(1);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(eventBO.getEventsByUserAndDateRange(user.getId(), start, end)).thenReturn(List.of());

            // Act
            List<EventResponseDTO> result = eventService.getEventsByCurrentUserAndDateRange(start, end);

            // Assert
            assertTrue(result.isEmpty());
            verify(authenticatedUserProvider).getCurrentUser();
            verify(eventBO).getEventsByUserAndDateRange(user.getId(), start, end);
        }

        @Test
        void testGetEventsByCurrentUserAndDateRange_returnsList() {
            // Arrange
            User user = TestUtils.createUserEntityWithId(1L);
            user.setTimezone("UTC");

            ZonedDateTime start = VALID_EVENT_START;
            ZonedDateTime end = VALID_EVENT_END;

            Event e1 = TestUtils.createTimedEventEntityWithId(200L, user);
            e1.setDescription("Event A");
            Event e2 = TestUtils.createTimedEventEntityWithId(201L, user);
            e2.setDescription("Event B");

            List<Event> events = List.of(e1, e2);
            List<EventResponseDTO> expected = events.stream()
                    .map(TestUtils::createEventResponseDTO)
                    .toList();

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(eventBO.getEventsByUserAndDateRange(user.getId(), start, end)).thenReturn(events);

            // Act
            List<EventResponseDTO> result = eventService.getEventsByCurrentUserAndDateRange(start, end);

            // Assert
            assertEquals(expected, result);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(eventBO).getEventsByUserAndDateRange(user.getId(), start, end);
        }

        @Test
        void testGetEventsByCurrentUserAndDateRange_includesUntimedEvent() {
            // Arrange
            User user = TestUtils.createUserEntityWithId(1L);
            user.setTimezone("UTC");

            ZonedDateTime start = VALID_EVENT_START.minusHours(1);
            ZonedDateTime end = VALID_EVENT_START.plusHours(1);

            Event untimedEvent = TestUtils.createUntimedEventEntityWithId(300L, user);
            untimedEvent.setDescription("Untimed Event");

            List<Event> events = List.of(untimedEvent);
            List<EventResponseDTO> expected = events.stream()
                    .map(TestUtils::createEventResponseDTO)
                    .toList();

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(eventBO.getEventsByUserAndDateRange(user.getId(), start, end)).thenReturn(events);

            // Act
            List<EventResponseDTO> result = eventService.getEventsByCurrentUserAndDateRange(start, end);

            // Assert
            assertEquals(expected, result);
        }

    }

    @Nested
    class CreateEventTests {

        @Test
        void testCreateEvent_success() {
            // Arrange
            User creator = TestUtils.createUserEntityWithId(1L);
            creator.setTimezone("UTC");

            EventCreateDTO dto = TestUtils.createTimedEventCreateDTO();

            Event eventToCreate = new Event(
                    dto.name(),
                    dto.startTime(),
                    creator
            );
            eventToCreate.setEndTime(dto.endTime());
            eventToCreate.setDescription(dto.description());

            Event createdEvent = TestUtils.createTimedEventEntityWithId(100L, creator);
            createdEvent.setName(dto.name());
            createdEvent.setStartTime(VALID_EVENT_START);
            createdEvent.setEndTime(VALID_EVENT_END);
            createdEvent.setDescription(dto.description());

            EventResponseDTO expected = TestUtils.createEventResponseDTO(createdEvent);

            // Mocks
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(creator);
            when(eventBO.createEvent(any(Event.class))).thenReturn(createdEvent);

            // Act
            EventResponseDTO result = eventService.createEvent(dto);

            // Assert
            assertEquals(expected, result);
            verify(authenticatedUserProvider).getCurrentUser();

            ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
            verify(eventBO).createEvent(captor.capture());

            Event captured = captor.getValue();
            EventResponseDTOAssertions.assertEventResponseDTOEquals(expected, result);
        }

        @Test
        void testCreateEvent_differentTimezones_includesOriginalZones() {
            // Arrange
            User creator = TestUtils.createUserEntityWithId(1L);
            creator.setTimezone("America/New_York"); // Creator's profile timezone

            EventCreateDTO dto = TestUtils.createTimedEventCreateDTO();

            // Use times in UTC zone (which differs from creator's zone)
            ZonedDateTime utcStart = ZonedDateTime.of(2024, 1, 1, 7, 0, 0, 0, ZoneId.of("UTC"));
            ZonedDateTime utcEnd = ZonedDateTime.of(2024, 1, 1, 8, 0, 0, 0, ZoneId.of("UTC"));
            int duration = (int) java.time.Duration.between(utcStart, utcEnd).toMinutes();

            Event createdEvent = TestUtils.createTimedEventEntityWithId(100L, creator);
            createdEvent.setName(dto.name());
            createdEvent.setDescription(dto.description());

            // Set start/end times with UTC zone → this sets startTimezone/endTimezone internally
            createdEvent.setStartTime(utcStart);
            createdEvent.setEndTime(utcEnd);

            // Build expected DTO
            EventResponseDTO expected = new EventResponseDTO(
                    createdEvent.getId(),
                    createdEvent.getName(),
                    createdEvent.getStartTime(),
                    createdEvent.getEndTime(),
                    duration,
                    utcStart.getZone().getId(),// startTimeZone included because ≠ creator timezone
                    utcEnd.getZone().getId(), // endTimeZone included because ≠ creator timezone
                    createdEvent.getDescription(),
                    creator.getId(),
                    creator.getUsername(),
                    creator.getTimezone()
            );

            // Mocks
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(creator);
            when(eventBO.createEvent(any(Event.class))).thenReturn(createdEvent);

            // Act
            EventResponseDTO result = eventService.createEvent(dto);

            // Assert
            EventResponseDTOAssertions.assertEventResponseDTOEquals(expected, result);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(eventBO).createEvent(any(Event.class));
        }

        @Test
        void testCreateEvent_untimedEvent_success() {
            // Arrange
            User creator = TestUtils.createUserEntityWithId(1L);
            creator.setTimezone("UTC");

            EventCreateDTO dto = TestUtils.createUntimedEventCreateDTO();

            Event eventToCreate = new Event(dto.name(), dto.startTime(), creator);
            eventToCreate.setDescription(dto.description());

            Event createdEvent = TestUtils.createUntimedEventEntityWithId(101L, creator);
            createdEvent.setName(dto.name());
            createdEvent.setStartTime(dto.startTime());
            createdEvent.setEndTime(null);
            createdEvent.setDescription(dto.description());

            EventResponseDTO expected = TestUtils.createEventResponseDTO(createdEvent);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(creator);
            when(eventBO.createEvent(any(Event.class))).thenReturn(createdEvent);

            // Act
            EventResponseDTO result = eventService.createEvent(dto);

            // Assert
            EventResponseDTOAssertions.assertEventResponseDTOEquals(expected, result);
        }
    }


    @Nested
    class UpdateEventTests {

        @Test
        void testUpdateEvent_success_changesPersisted() {
            // Arrange
            Long eventId = 100L;
            User user = TestUtils.createUserEntityWithId(1L);
            user.setTimezone("UTC");

            Event originalEvent = TestUtils.createTimedEventEntityWithId(eventId, user);
            originalEvent.setDescription("Old description");

            ZonedDateTime newStart = VALID_EVENT_START.plusHours(1);
            ZonedDateTime newEnd = VALID_EVENT_END.plusHours(1);
            String newDescription = "New description";

            EventUpdateDTO updateDTO = new EventUpdateDTO(
                    "Updated Name",
                    newStart,
                    Optional.of(newEnd),
                    Optional.of(newDescription)
            );

            Event updatedEvent = TestUtils.createTimedEventEntityWithId(eventId, user);
            updatedEvent.setName(updateDTO.name());
            updatedEvent.setStartTime(updateDTO.startTime());
            updatedEvent.setEndTime(newEnd);
            updatedEvent.setDescription(newDescription);

            EventResponseDTO expected = TestUtils.createEventResponseDTO(updatedEvent);

            // Mocks
            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(originalEvent));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            doNothing().when(ownershipValidator).validateEventOwnership(user.getId(), originalEvent);
            when(eventPatchHandler.applyPatch(originalEvent, updateDTO, user)).then(invocation -> {
                originalEvent.setName(updateDTO.name());
                originalEvent.setStartTime(updateDTO.startTime());
                originalEvent.setEndTime(updateDTO.endTime().orElse(null));
                originalEvent.setDescription(updateDTO.description().orElse(null));
                return true;
            });
            when(eventBO.updateEvent(originalEvent)).thenReturn(updatedEvent);

            // Act
            EventResponseDTO result = eventService.updateEvent(eventId, updateDTO);

            // Assert
            EventResponseDTOAssertions.assertEventResponseDTOEquals(expected, result);
            verify(eventBO).getEventById(eventId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(user.getId(), originalEvent);
            verify(eventPatchHandler).applyPatch(originalEvent, updateDTO, user);
            verify(eventBO).updateEvent(originalEvent);
        }

        @Test
        void testUpdateEvent_eventNotFound_throwsException() {
            // Arrange
            Long eventId = 999L;
            EventUpdateDTO dto = new EventUpdateDTO(
                    "Name",
                    VALID_EVENT_START,
                    Optional.of(VALID_EVENT_END),
                    Optional.of("Desc")
            );

            // Mocks
            when(eventBO.getEventById(eventId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(EventNotFoundException.class, () -> eventService.updateEvent(eventId, dto));

            verify(eventBO).getEventById(eventId);
            verifyNoMoreInteractions(eventBO, ownershipValidator, eventPatchHandler);
        }

        @Test
        void testUpdateEvent_ownershipValidationFails_throwsException() {
            // Arrange
            Long eventId = 100L;
            User user = TestUtils.createUserEntityWithId(1L);
            EventUpdateDTO dto = new EventUpdateDTO(
                    "Name",
                    VALID_EVENT_START,
                    Optional.of(VALID_EVENT_END),
                    Optional.of("Desc")
            );
            Event event = TestUtils.createTimedEventEntityWithId(eventId, TestUtils.createUserEntityWithId(2L)); // different creator

            // Mocks
            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            doThrow(new EventOwnershipException(UNAUTHORIZED_EVENT_ACCESS, eventId))
                    .when(ownershipValidator).validateEventOwnership(user.getId(), event);

            // Act + Assert
            assertThrows(EventOwnershipException.class, () -> eventService.updateEvent(eventId, dto));

            verify(eventBO).getEventById(eventId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(user.getId(), event);
            verifyNoMoreInteractions(eventBO, eventPatchHandler);
        }

        @Test
        void testUpdateEvent_noChanges_returnsOriginalEvent() {
            // Arrange
            Long eventId = 100L;
            User user = TestUtils.createUserEntityWithId(1L);
            user.setTimezone("UTC");

            Event event = TestUtils.createTimedEventEntityWithId(eventId, user);
            event.setDescription("Unchanged description");

            EventUpdateDTO dto = new EventUpdateDTO(null, null, null, null);
            EventResponseDTO expected = TestUtils.createEventResponseDTO(event);

            // Mocks
            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            doNothing().when(ownershipValidator).validateEventOwnership(user.getId(), event);
            when(eventPatchHandler.applyPatch(event, dto, user)).thenReturn(false);

            // Act
            EventResponseDTO result = eventService.updateEvent(eventId, dto);

            // Assert
            EventResponseDTOAssertions.assertEventResponseDTOEquals(expected, result);
            verify(eventBO).getEventById(eventId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(user.getId(), event);
            verify(eventPatchHandler).applyPatch(event, dto, user);
            verifyNoMoreInteractions(eventBO);
        }

        @Test
        void testUpdateEvent_noEndTimeToHasEndTime_success() {
            Long eventId = 200L;
            User user = TestUtils.createUserEntityWithId(1L);
            user.setTimezone("UTC");

            Event original = TestUtils.createUntimedEventEntityWithId(eventId, user);
            ZonedDateTime newEnd = VALID_EVENT_END;

            EventUpdateDTO dto = new EventUpdateDTO(null, null, Optional.of(newEnd), Optional.empty());

            Event updated = TestUtils.createUntimedEventEntityWithId(eventId, user);
            updated.setEndTime(newEnd);

            EventResponseDTO expected = TestUtils.createEventResponseDTO(updated);

            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(original));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            doNothing().when(ownershipValidator).validateEventOwnership(user.getId(), original);
            when(eventPatchHandler.applyPatch(original, dto, user)).then(invocation -> {
                original.setEndTime(newEnd);
                return true;
            });
            when(eventBO.updateEvent(original)).thenReturn(updated);

            EventResponseDTO result = eventService.updateEvent(eventId, dto);

            EventResponseDTOAssertions.assertEventResponseDTOEquals(expected, result);
        }

        @Test
        void testUpdateEvent_hasEndTimeToNoEndTime_success() {
            Long eventId = 201L;
            User user = TestUtils.createUserEntityWithId(1L);
            user.setTimezone("UTC");

            Event original = TestUtils.createTimedEventEntityWithId(eventId, user);

            EventUpdateDTO dto = new EventUpdateDTO(null, null, Optional.empty(), Optional.empty());

            Event updated = TestUtils.createUntimedEventEntityWithId(eventId, user);

            EventResponseDTO expected = TestUtils.createEventResponseDTO(updated);

            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(original));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            doNothing().when(ownershipValidator).validateEventOwnership(user.getId(), original);
            when(eventPatchHandler.applyPatch(original, dto, user)).then(invocation -> {
                original.setEndTime(null);
                return true;
            });
            when(eventBO.updateEvent(original)).thenReturn(updated);

            EventResponseDTO result = eventService.updateEvent(eventId, dto);

            EventResponseDTOAssertions.assertEventResponseDTOEquals(expected, result);
        }

        @Test
        void testUpdateEvent_noEndTimeToNoEndTime_success_noChange() {
            Long eventId = 202L;
            User user = TestUtils.createUserEntityWithId(1L);
            user.setTimezone("UTC");

            Event original = TestUtils.createUntimedEventEntityWithId(eventId, user);
            EventResponseDTO expected = TestUtils.createEventResponseDTO(original);
            EventUpdateDTO dto = new EventUpdateDTO(null, null, Optional.empty(), Optional.empty());

            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(original));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            doNothing().when(ownershipValidator).validateEventOwnership(user.getId(), original);
            when(eventPatchHandler.applyPatch(original, dto, user)).thenReturn(false);

            EventResponseDTO result = eventService.updateEvent(eventId, dto);

            EventResponseDTOAssertions.assertEventResponseDTOEquals(expected, result);
        }

    }

    @Nested
    class DeleteEventTests {

        @Test
        void testDeleteEvent_success() {
            // Arrange
            Long eventId = 100L;
            User user = TestUtils.createUserEntityWithId(1L);
            Event event = TestUtils.createTimedEventEntityWithId(eventId, user);

            // Mocks
            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            doNothing().when(ownershipValidator).validateEventOwnership(user.getId(), event);

            // Act
            eventService.deleteEvent(eventId);

            // Assert
            verify(eventBO).getEventById(eventId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(user.getId(), event);
            verify(eventBO).deleteEvent(eventId);
        }

        @Test
        void testDeleteEvent_unauthorized_throwsException() {
            // Arrange
            Long eventId = 100L;
            User user = TestUtils.createUserEntityWithId(1L);
            Event event = TestUtils.createTimedEventEntityWithId(eventId, user);

            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            doThrow(new EventOwnershipException(UNAUTHORIZED_EVENT_ACCESS, eventId))
                    .when(ownershipValidator).validateEventOwnership(user.getId(), event);

            // Act + Assert
            assertThrows(EventOwnershipException.class, () -> eventService.deleteEvent(eventId));

            verify(eventBO).getEventById(eventId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(user.getId(), event);
            verifyNoMoreInteractions(eventBO);
        }

        @Test
        void testDeleteEvent_eventNotFound_throwsException() {
            // Arrange
            Long eventId = 404L;

            // Mocks
            when(eventBO.getEventById(eventId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(EventNotFoundException.class, () -> eventService.deleteEvent(eventId));

            verify(eventBO).getEventById(eventId);
            verifyNoMoreInteractions(eventBO, authenticatedUserProvider, ownershipValidator);
        }
    }

}
