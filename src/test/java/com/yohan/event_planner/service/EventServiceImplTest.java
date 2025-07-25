package com.yohan.event_planner.service;

import com.blazebit.persistence.PagedList;
import com.yohan.event_planner.business.EventBO;
import com.yohan.event_planner.business.RecurringEventBO;
import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.business.handler.EventPatchHandler;
import com.yohan.event_planner.dao.EventDAO;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.TimeFilter;
import com.yohan.event_planner.dto.DayViewDTO;
import com.yohan.event_planner.dto.EventCreateDTO;
import com.yohan.event_planner.dto.EventFilterDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventResponseDTOFactory;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.dto.WeekViewDTO;
import com.yohan.event_planner.exception.ErrorCode;
import com.yohan.event_planner.exception.EventAlreadyConfirmedException;
import com.yohan.event_planner.exception.EventNotFoundException;
import com.yohan.event_planner.exception.InvalidTimeException;
import com.yohan.event_planner.exception.UserOwnershipException;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import com.yohan.event_planner.time.ClockProvider;
import com.yohan.event_planner.time.TimeUtils;
import com.yohan.event_planner.util.EventResponseDTOAssertions;
import com.yohan.event_planner.util.TestConstants;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;


import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_USER_ACCESS;

import static com.yohan.event_planner.util.TestConstants.getValidEventEndFuture;
import static com.yohan.event_planner.util.TestConstants.getValidEventStartFuture;
import static com.yohan.event_planner.util.TestUtils.createEventResponseDTO;
import static com.yohan.event_planner.util.TestUtils.createValidUserEntityWithId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class EventServiceImplTest {

    @Mock
    private EventBO eventBO;
    @Mock
    private UserBO userBO;
    @Mock
    private RecurringEventBO recurringEventBO;
    @Mock
    private LabelService labelService;
    @Mock
    private EventDAO eventDAO;
    @Mock
    private EventPatchHandler eventPatchHandler;
    @Mock
    private EventResponseDTOFactory eventResponseDTOFactory;
    @Mock
    private OwnershipValidator ownershipValidator;
    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;
    @Mock
    private ClockProvider clockProvider;
    
    private User user;
    private ZoneId userZoneId;
    private Clock fixedClock;
    private ZonedDateTime nowInUserZone;
    private ZonedDateTime nowInUtc;

    @InjectMocks
    private EventServiceImpl eventService;

    @BeforeEach
    void setUp() {
        // Creating a user
        user = createValidUserEntityWithId();  // This should return a user with a valid timezone string
        userZoneId = ZoneId.of(user.getTimezone());

        // Time
        nowInUserZone = ZonedDateTime.of(2025, 6, 27, 10, 0, 0, 0, userZoneId);
        nowInUtc = nowInUserZone.withZoneSameInstant(ZoneOffset.UTC);
        fixedClock = Clock.fixed(nowInUserZone.toInstant(), userZoneId);
    }

    @Nested
    class GetEventByIdTests {

        @Test
        void testGetEventById_SuccessForConfirmedEvent() {
            // Arrange
            Event event = TestUtils.createValidScheduledEventWithId(TestConstants.EVENT_ID, user, fixedClock);
            EventResponseDTO expectedDto = createEventResponseDTO(event);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(eventBO.getEventById(TestConstants.EVENT_ID)).thenReturn(Optional.of(event));
            when(eventResponseDTOFactory.createFromEvent(event)).thenReturn(expectedDto);

            // Act
            EventResponseDTO result = eventService.getEventById(TestConstants.EVENT_ID);

            // Assert
            EventResponseDTOAssertions.assertEventResponseDTOEquals(result, expectedDto);
        }

        @Test
        void testGetEventById_FailsWhenNotFound() {
            // Arrange
            User viewer = createValidUserEntityWithId();
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(TestConstants.EVENT_ID)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(EventNotFoundException.class, () -> eventService.getEventById(TestConstants.EVENT_ID));
        }

        @Test
        void testGetEventById_FailsIfUnconfirmedAndViewerNotCreator() {
            // Arrange
            User creator = createValidUserEntityWithId(2L); // Different user than viewer
            User viewer = createValidUserEntityWithId(1L);
            Event draft = TestUtils.createEmptyDraftEvent(creator);
            TestUtils.setEventId(draft, TestConstants.EVENT_ID);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(TestConstants.EVENT_ID)).thenReturn(Optional.of(draft));

            // Act + Assert
            assertThrows(EventNotFoundException.class, () -> eventService.getEventById(TestConstants.EVENT_ID));
        }

    }

    @Nested
    class GetConfirmedEventsForCurrentTests {

        @Test
        void testGetConfirmedEventsForCurrentUser_Success() {
            // Arrange
            Event event = TestUtils.createValidScheduledEventWithId(TestConstants.EVENT_ID, user, fixedClock);
            EventResponseDTO expectedDto = createEventResponseDTO(event);

            // Define the filter
            EventFilterDTO filter = new EventFilterDTO(
                    TestConstants.VALID_LABEL_ID,
                    TimeFilter.ALL, // TimeFilter is ALL here to test the resolved behavior
                    null, // startTime and endTime will be calculated by the service layer
                    null,
                    true,
                    true
            );

            int pageNumber = 0;
            int pageSize = 10;

            // Mock the current user
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            // Mock the clock provider
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            // Mock the paged events response from DAO
            PagedList<Event> pagedEvents = mock(PagedList.class);
            when(pagedEvents.stream()).thenReturn(Stream.of(event));
            when(pagedEvents.getTotalSize()).thenReturn(1L);
            when(eventDAO.findConfirmedEvents(eq(user.getId()), any(EventFilterDTO.class), eq(pageNumber), eq(pageSize)))
                    .thenReturn(pagedEvents);

            // Mock the conversion of Event to EventResponseDTO
            when(eventResponseDTOFactory.createFromEvent(event)).thenReturn(expectedDto);

            // Act
            Page<EventResponseDTO> results = eventService.getConfirmedEventsForCurrentUser(filter, pageNumber, pageSize);

            // Assert
            assertEquals(1, results.getTotalElements());
            EventResponseDTOAssertions.assertEventResponseDTOEquals(results.getContent().get(0), expectedDto);
        }


    }

    @Nested
    class GetConfirmedEventsPageTests {

        @Test
        void shouldReturnConfirmedEventsPage_WhenFirstPageRequested() {
            // Arrange
            ZonedDateTime endTimeCursor = null;
            ZonedDateTime startTimeCursor = null;
            Long idCursor = null;
            int limit = 10;

            Event event = TestUtils.createValidScheduledEventWithId(TestConstants.EVENT_ID, user, fixedClock);
            EventResponseDTO expectedDto = createEventResponseDTO(event);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(eventBO.getConfirmedEventsPage(user.getId(), endTimeCursor, startTimeCursor, idCursor, limit))
                    .thenReturn(List.of(event));
            when(eventResponseDTOFactory.createFromEvent(event)).thenReturn(expectedDto);

            // Act
            List<EventResponseDTO> results = eventService.getConfirmedEventsPage(endTimeCursor, startTimeCursor, idCursor, limit);

            // Assert
            assertEquals(1, results.size());
            EventResponseDTOAssertions.assertEventResponseDTOEquals(results.get(0), expectedDto);

            verify(authenticatedUserProvider).getCurrentUser();
            verify(eventBO).getConfirmedEventsPage(user.getId(), endTimeCursor, startTimeCursor, idCursor, limit);
            verify(eventResponseDTOFactory).createFromEvent(event);
        }

        @Test
        void shouldReturnEmptyList_WhenNoEventsFound() {
            // Arrange
            ZonedDateTime endTimeCursor = ZonedDateTime.now(fixedClock);
            ZonedDateTime startTimeCursor = ZonedDateTime.now(fixedClock).minusHours(1);
            Long idCursor = 999L;
            int limit = 10;

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(eventBO.getConfirmedEventsPage(user.getId(), endTimeCursor, startTimeCursor, idCursor, limit))
                    .thenReturn(Collections.emptyList());

            // Act
            List<EventResponseDTO> results = eventService.getConfirmedEventsPage(endTimeCursor, startTimeCursor, idCursor, limit);

            // Assert
            assertNotNull(results);
            assertTrue(results.isEmpty());

            verify(authenticatedUserProvider).getCurrentUser();
            verify(eventBO).getConfirmedEventsPage(user.getId(), endTimeCursor, startTimeCursor, idCursor, limit);
            verifyNoInteractions(eventResponseDTOFactory);
        }

        @Test
        void shouldReturnMultipleEventsPage_WhenEventsExist() {
            // Arrange
            ZonedDateTime endTimeCursor = ZonedDateTime.now(fixedClock);
            ZonedDateTime startTimeCursor = ZonedDateTime.now(fixedClock).minusHours(1);
            Long idCursor = 999L;
            int limit = 10;

            Event event1 = TestUtils.createValidScheduledEventWithId(1L, user, fixedClock);
            Event event2 = TestUtils.createValidScheduledEventWithId(2L, user, fixedClock);

            EventResponseDTO dto1 = createEventResponseDTO(event1);
            EventResponseDTO dto2 = createEventResponseDTO(event2);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(eventBO.getConfirmedEventsPage(user.getId(), endTimeCursor, startTimeCursor, idCursor, limit))
                    .thenReturn(List.of(event1, event2));
            when(eventResponseDTOFactory.createFromEvent(event1)).thenReturn(dto1);
            when(eventResponseDTOFactory.createFromEvent(event2)).thenReturn(dto2);

            // Act
            List<EventResponseDTO> results = eventService.getConfirmedEventsPage(endTimeCursor, startTimeCursor, idCursor, limit);

            // Assert
            assertEquals(2, results.size());
            EventResponseDTOAssertions.assertEventResponseDTOEquals(results.get(0), dto1);
            EventResponseDTOAssertions.assertEventResponseDTOEquals(results.get(1), dto2);

            verify(authenticatedUserProvider).getCurrentUser();
            verify(eventBO).getConfirmedEventsPage(user.getId(), endTimeCursor, startTimeCursor, idCursor, limit);
            verify(eventResponseDTOFactory).createFromEvent(event1);
            verify(eventResponseDTOFactory).createFromEvent(event2);
        }

        @Test
        void shouldHandleLargePaginationLimits() {
            // Arrange - Test boundary conditions for pagination
            User viewer = createValidUserEntityWithId();
            int largeLimit = 1000; // Large but reasonable limit
            
            List<Event> manyEvents = new ArrayList<>();
            for (int i = 0; i < largeLimit; i++) {
                Event event = TestUtils.createValidScheduledEventWithId((long) i, viewer, fixedClock);
                manyEvents.add(event);
            }

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getConfirmedEventsPage(viewer.getId(), null, null, null, largeLimit))
                    .thenReturn(manyEvents);
            
            // Mock DTO conversion for each event
            for (Event event : manyEvents) {
                EventResponseDTO dto = createEventResponseDTO(event);
                when(eventResponseDTOFactory.createFromEvent(event)).thenReturn(dto);
            }

            // Act
            List<EventResponseDTO> result = eventService.getConfirmedEventsPage(null, null, null, largeLimit);

            // Assert
            assertEquals(largeLimit, result.size());
            verify(eventBO).getConfirmedEventsPage(viewer.getId(), null, null, null, largeLimit);
        }

        @Test
        void shouldHandleMinimalPaginationLimit() {
            // Arrange - Test with limit of 1
            User viewer = createValidUserEntityWithId();
            Event singleEvent = TestUtils.createValidScheduledEventWithId(1L, viewer, fixedClock);
            EventResponseDTO dto = createEventResponseDTO(singleEvent);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getConfirmedEventsPage(viewer.getId(), null, null, null, 1))
                    .thenReturn(List.of(singleEvent));
            when(eventResponseDTOFactory.createFromEvent(singleEvent)).thenReturn(dto);

            // Act
            List<EventResponseDTO> result = eventService.getConfirmedEventsPage(null, null, null, 1);

            // Assert
            assertEquals(1, result.size());
            verify(eventBO).getConfirmedEventsPage(viewer.getId(), null, null, null, 1);
        }

        @Test
        void shouldHandleCursorBoundaryConditions() {
            // Arrange - Test with extreme cursor values
            User viewer = createValidUserEntityWithId();
            ZonedDateTime extremePastTime = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            ZonedDateTime extremeFutureTime = ZonedDateTime.of(2100, 12, 31, 23, 59, 59, 999_999_999, ZoneOffset.UTC);
            Long extremeId = Long.MAX_VALUE;

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getConfirmedEventsPage(viewer.getId(), extremePastTime, extremeFutureTime, extremeId, 10))
                    .thenReturn(Collections.emptyList());

            // Act
            List<EventResponseDTO> result = eventService.getConfirmedEventsPage(
                    extremePastTime, extremeFutureTime, extremeId, 10);

            // Assert
            assertEquals(0, result.size());
            verify(eventBO).getConfirmedEventsPage(viewer.getId(), extremePastTime, extremeFutureTime, extremeId, 10);
        }

    }

    @Nested
    class GetUnconfirmedEventForCurrentUserTests {

        @Test
        void testGetUnconfirmedEventsForCurrentUser_Success() {
            // Arrange
            User user = createValidUserEntityWithId();
            Event draft = TestUtils.createPartialDraftEvent(user, fixedClock);
            TestUtils.setEventId(draft, TestConstants.EVENT_ID);
            EventResponseDTO expectedDto = createEventResponseDTO(draft);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(eventBO.getUnconfirmedEventsForUser(eq(user.getId())))
                    .thenReturn(List.of(draft));
            when(eventResponseDTOFactory.createFromEvent(draft)).thenReturn(expectedDto);

            // Act
            List<EventResponseDTO> results = eventService.getUnconfirmedEventsForCurrentUser();

            // Assert
            assertEquals(1, results.size());
            EventResponseDTOAssertions.assertEventResponseDTOEquals(results.get(0), expectedDto);

            // Verify
            verify(authenticatedUserProvider).getCurrentUser();
            verify(eventBO).getUnconfirmedEventsForUser(user.getId());
            verify(eventResponseDTOFactory).createFromEvent(draft);
        }

        @Test
        void testGetUnconfirmedEventsForCurrentUser_ReturnsMultipleEvents() {
            // Arrange
            User user = createValidUserEntityWithId();
            Event draft1 = TestUtils.createPartialDraftEvent(user, fixedClock);
            Event draft2 = TestUtils.createValidFullDraftEvent(user, fixedClock);
            TestUtils.setEventId(draft1, 1L);
            TestUtils.setEventId(draft2, 2L);
            EventResponseDTO dto1 = createEventResponseDTO(draft1);
            EventResponseDTO dto2 = createEventResponseDTO(draft2);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(eventBO.getUnconfirmedEventsForUser(eq(user.getId())))
                    .thenReturn(List.of(draft1, draft2));
            when(eventResponseDTOFactory.createFromEvent(draft1)).thenReturn(dto1);
            when(eventResponseDTOFactory.createFromEvent(draft2)).thenReturn(dto2);

            // Act
            List<EventResponseDTO> results = eventService.getUnconfirmedEventsForCurrentUser();

            // Assert
            assertEquals(2, results.size());
            EventResponseDTOAssertions.assertEventResponseDTOEquals(results.get(0), dto1);
            EventResponseDTOAssertions.assertEventResponseDTOEquals(results.get(1), dto2);

            // Verify
            verify(authenticatedUserProvider).getCurrentUser();
            verify(eventBO).getUnconfirmedEventsForUser(user.getId());
            verify(eventResponseDTOFactory).createFromEvent(draft1);
            verify(eventResponseDTOFactory).createFromEvent(draft2);
        }
    }

    @Nested
    class GenerateDayViewTests {

        @Test
        void shouldReturnEmptyEventsForFutureDateWithNoEvents() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            LocalDate selectedDate = nowInUserZone.toLocalDate().plusDays(5);
            ZonedDateTime startOfDay = selectedDate.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime endOfDay = selectedDate.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);

            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), startOfDay, endOfDay)).thenReturn(Collections.emptyList());
            
            // Mock the new EventBO method
            DayViewDTO expectedDayView = new DayViewDTO(selectedDate, Collections.emptyList());
            when(eventBO.generateDayViewData(eq(selectedDate), eq(Collections.emptyList()), eq(Collections.emptyList())))
                    .thenReturn(expectedDayView);

            // Act
            DayViewDTO dayViewDTO = eventService.generateDayView(selectedDate);

            // Assert
            assertNotNull(dayViewDTO, "DayViewDTO should not be null.");
            assertEquals(0, dayViewDTO.events().size(), "The events list should be empty for a future date.");
            verify(eventBO, never()).solidifyRecurrences(user.getId(), startOfDay, nowInUtc, userZoneId);
        }

        @Test
        void shouldReturnEventsForFutureDateWithEvents() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            LocalDate selectedDate = nowInUserZone.toLocalDate().plusDays(5);
            ZonedDateTime startOfDay = selectedDate.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime endOfDay = selectedDate.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);

            Event event1 = TestUtils.createValidScheduledEvent(user, fixedClock);
            User user1 = TestUtils.createValidUserEntity();
            Event event2 = TestUtils.createValidScheduledEvent(user1, fixedClock);

            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), startOfDay, endOfDay)).thenReturn(Arrays.asList(event1, event2));
            when(eventResponseDTOFactory.createFromEvent(event1)).thenReturn(createEventResponseDTO(event1));
            when(eventResponseDTOFactory.createFromEvent(event2)).thenReturn(createEventResponseDTO(event2));

            // Mock the EventBO method that creates the DayViewDTO
            List<EventResponseDTO> confirmedEventDTOs = Arrays.asList(createEventResponseDTO(event1), createEventResponseDTO(event2));
            DayViewDTO expectedDayView = new DayViewDTO(selectedDate, confirmedEventDTOs);
            when(eventBO.generateDayViewData(eq(selectedDate), eq(confirmedEventDTOs), eq(Collections.emptyList())))
                    .thenReturn(expectedDayView);

            // Act
            DayViewDTO dayViewDTO = eventService.generateDayView(selectedDate);

            // Assert
            assertNotNull(dayViewDTO, "DayViewDTO should not be null.");
            assertEquals(2, dayViewDTO.events().size(), "The events list should contain 2 events.");
            verify(eventBO, never()).solidifyRecurrences(user.getId(), startOfDay, nowInUtc, userZoneId);
        }

        @Test
        void shouldReturnOnlyVirtualsForToday() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            LocalDate today = nowInUserZone.toLocalDate();
            ZonedDateTime nowInUtc = nowInUserZone.withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime startOfDay = today.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime endOfDay = today.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);

            // Prepare virtual DTO
            EventResponseDTO virtualDto = TestUtils.createValidScheduledEventResponseDTO(fixedClock);

            // Mock confirmed events as empty
            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), startOfDay, endOfDay))
                    .thenReturn(List.of());

            // Mock generateVirtuals to return the virtualDto
            when(recurringEventBO.generateVirtuals(user.getId(), nowInUtc, endOfDay, userZoneId))
                    .thenReturn(List.of(virtualDto));

            // Mock the EventBO method that creates the DayViewDTO
            List<EventResponseDTO> virtualEvents = List.of(virtualDto);
            DayViewDTO expectedDayView = new DayViewDTO(today, virtualEvents);
            when(eventBO.generateDayViewData(eq(today), eq(Collections.emptyList()), eq(virtualEvents)))
                    .thenReturn(expectedDayView);

            // Act
            DayViewDTO dayViewDTO = eventService.generateDayView(today);

            // Verify generateVirtuals was called correctly
            verify(recurringEventBO).generateVirtuals(user.getId(), nowInUtc, endOfDay, userZoneId);

            // Assert: The list of events should contain only the virtual events
            assertNotNull(dayViewDTO, "DayViewDTO should not be null.");
            assertEquals(1, dayViewDTO.events().size(), "The events list should contain only virtual events");
            assertTrue(dayViewDTO.events().get(0) instanceof EventResponseDTO, "The event should be a virtual EventResponseDTO");
        }

        @Test
        void shouldReturnSolidifiedAndVirtualEventsForDayView() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            LocalDate selectedDate = nowInUserZone.toLocalDate().plusDays(5);
            ZonedDateTime startOfDay = selectedDate.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime endOfDay = selectedDate.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);

            // Mock solidified event
            Event solidifiedEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            EventResponseDTO solidifiedEventDTO = TestUtils.createValidScheduledEventResponseDTO(fixedClock);

            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), startOfDay, endOfDay))
                    .thenReturn(Collections.singletonList(solidifiedEvent));
            when(eventResponseDTOFactory.createFromEvent(solidifiedEvent)).thenReturn(solidifiedEventDTO);

            // Mock virtual event
            EventResponseDTO virtualEventDTO = TestUtils.createValidScheduledEventResponseDTO(fixedClock);
            when(recurringEventBO.generateVirtuals(user.getId(), startOfDay, endOfDay, userZoneId))
                    .thenReturn(Collections.singletonList(virtualEventDTO));

            // Mock the EventBO method that creates the DayViewDTO
            List<EventResponseDTO> confirmedEvents = Collections.singletonList(solidifiedEventDTO);
            List<EventResponseDTO> virtualEvents = Collections.singletonList(virtualEventDTO);
            List<EventResponseDTO> combinedEvents = Arrays.asList(solidifiedEventDTO, virtualEventDTO);
            DayViewDTO expectedDayView = new DayViewDTO(selectedDate, combinedEvents);
            when(eventBO.generateDayViewData(eq(selectedDate), eq(confirmedEvents), eq(virtualEvents)))
                    .thenReturn(expectedDayView);

            // Act
            DayViewDTO dayViewDTO = eventService.generateDayView(selectedDate);

            // Assert
            assertNotNull(dayViewDTO, "DayViewDTO should not be null.");
            assertEquals(2, dayViewDTO.events().size(), "The events list should contain both solidified and virtual events.");
            assertTrue(dayViewDTO.events().contains(solidifiedEventDTO), "Should contain solidified event DTO.");
            assertTrue(dayViewDTO.events().contains(virtualEventDTO), "Should contain virtual event DTO.");

            // Verify calls
            verify(eventBO).getConfirmedEventsForUserInRange(user.getId(), startOfDay, endOfDay);
            verify(recurringEventBO).generateVirtuals(user.getId(), startOfDay, endOfDay, userZoneId);
            verify(eventResponseDTOFactory).createFromEvent(solidifiedEvent);
        }

        @Test
        void shouldReturnEmptyEventsForPastDateWithNoEvents() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            LocalDate selectedDate = nowInUserZone.toLocalDate().minusDays(5);  // Past date
            ZonedDateTime startOfDay = selectedDate.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime endOfDay = selectedDate.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);

            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), startOfDay, endOfDay)).thenReturn(Collections.emptyList());

            // Mock the EventBO method that creates the DayViewDTO
            DayViewDTO expectedDayView = new DayViewDTO(selectedDate, Collections.emptyList());
            when(eventBO.generateDayViewData(eq(selectedDate), eq(Collections.emptyList()), eq(Collections.emptyList())))
                    .thenReturn(expectedDayView);

            // Act
            DayViewDTO dayViewDTO = eventService.generateDayView(selectedDate);

            // Assert
            assertNotNull(dayViewDTO, "DayViewDTO should not be null.");
            assertEquals(0, dayViewDTO.events().size(), "The events list should be empty for a past date.");
        }

        @Test
        void shouldReturnSolidifiedEventsForFutureDate() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            LocalDate selectedDate = nowInUserZone.toLocalDate().plusDays(7);  // Future date
            ZonedDateTime startOfDay = selectedDate.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime endOfDay = selectedDate.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);

            Event event1 = TestUtils.createValidScheduledEvent(user, fixedClock);

            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), startOfDay, endOfDay))
                    .thenReturn(Collections.singletonList(event1));
            when(eventResponseDTOFactory.createFromEvent(event1)).thenReturn(createEventResponseDTO(event1));

            // Mock the EventBO method that creates the DayViewDTO
            List<EventResponseDTO> confirmedEvents = Collections.singletonList(createEventResponseDTO(event1));
            DayViewDTO expectedDayView = new DayViewDTO(selectedDate, confirmedEvents);
            when(eventBO.generateDayViewData(eq(selectedDate), eq(confirmedEvents), eq(Collections.emptyList())))
                    .thenReturn(expectedDayView);

            // Act
            DayViewDTO dayViewDTO = eventService.generateDayView(selectedDate);

            // Assert
            assertNotNull(dayViewDTO, "DayViewDTO should not be null.");
            assertEquals(1, dayViewDTO.events().size(), "The events list should contain 1 solidified event.");
        }

        @Test
        void shouldReturnMixedEventsForSameDay() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            LocalDate selectedDate = nowInUserZone.toLocalDate().plusDays(7);  // Future date
            ZonedDateTime startOfDay = selectedDate.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime endOfDay = selectedDate.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);

            // Mock solidified event
            Event solidifiedEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            EventResponseDTO solidifiedEventDTO = TestUtils.createValidScheduledEventResponseDTO(fixedClock);

            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), startOfDay, endOfDay))
                    .thenReturn(Collections.singletonList(solidifiedEvent));
            when(eventResponseDTOFactory.createFromEvent(solidifiedEvent)).thenReturn(solidifiedEventDTO);

            // Mock virtual event
            EventResponseDTO virtualEventDTO = TestUtils.createValidScheduledEventResponseDTO(fixedClock);
            when(recurringEventBO.generateVirtuals(user.getId(), startOfDay, endOfDay, userZoneId))
                    .thenReturn(Collections.singletonList(virtualEventDTO));

            // Mock the EventBO method that creates the DayViewDTO
            List<EventResponseDTO> confirmedEvents = Collections.singletonList(solidifiedEventDTO);
            List<EventResponseDTO> virtualEvents = Collections.singletonList(virtualEventDTO);
            List<EventResponseDTO> combinedEvents = Arrays.asList(solidifiedEventDTO, virtualEventDTO);
            DayViewDTO expectedDayView = new DayViewDTO(selectedDate, combinedEvents);
            when(eventBO.generateDayViewData(eq(selectedDate), eq(confirmedEvents), eq(virtualEvents)))
                    .thenReturn(expectedDayView);

            // Act
            DayViewDTO dayViewDTO = eventService.generateDayView(selectedDate);

            // Assert
            assertNotNull(dayViewDTO, "DayViewDTO should not be null.");
            assertEquals(2, dayViewDTO.events().size(), "The events list should contain both solidified and virtual events.");
            assertTrue(dayViewDTO.events().contains(solidifiedEventDTO), "The events should include the solidified EventResponseDTO.");
            assertTrue(dayViewDTO.events().contains(virtualEventDTO), "The events should include the virtual EventResponseDTO.");

            // Verify interactions
            verify(eventBO).getConfirmedEventsForUserInRange(user.getId(), startOfDay, endOfDay);
            verify(eventResponseDTOFactory).createFromEvent(solidifiedEvent);
            verify(recurringEventBO).generateVirtuals(user.getId(), startOfDay, endOfDay, userZoneId);
        }

        @Test
        void shouldHandleMultipleRecurringEventsOnSameDay() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            LocalDate selectedDate = nowInUserZone.toLocalDate().plusDays(5);  // Future date
            ZonedDateTime startOfDay = selectedDate.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime endOfDay = selectedDate.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);

            // Mock virtual events
            EventResponseDTO virtualEventDTO1 = TestUtils.createValidScheduledEventResponseDTO(fixedClock);
            EventResponseDTO virtualEventDTO2 = TestUtils.createValidScheduledEventResponseDTO(fixedClock);

            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), startOfDay, endOfDay))
                    .thenReturn(Collections.emptyList());

            when(recurringEventBO.generateVirtuals(user.getId(), startOfDay, endOfDay, userZoneId))
                    .thenReturn(Arrays.asList(virtualEventDTO1, virtualEventDTO2));

            // Mock the EventBO method that creates the DayViewDTO
            List<EventResponseDTO> virtualEvents = Arrays.asList(virtualEventDTO1, virtualEventDTO2);
            DayViewDTO expectedDayView = new DayViewDTO(selectedDate, virtualEvents);
            when(eventBO.generateDayViewData(eq(selectedDate), eq(Collections.emptyList()), eq(virtualEvents)))
                    .thenReturn(expectedDayView);

            // Act
            DayViewDTO dayViewDTO = eventService.generateDayView(selectedDate);

            // Assert
            assertNotNull(dayViewDTO, "DayViewDTO should not be null.");
            assertEquals(2, dayViewDTO.events().size(), "The events list should contain 2 virtual events.");
            assertTrue(dayViewDTO.events().contains(virtualEventDTO1), "Should contain virtualEventDTO1.");
            assertTrue(dayViewDTO.events().contains(virtualEventDTO2), "Should contain virtualEventDTO2.");

            // Verify interactions
            verify(eventBO).getConfirmedEventsForUserInRange(user.getId(), startOfDay, endOfDay);
            verify(recurringEventBO).generateVirtuals(user.getId(), startOfDay, endOfDay, userZoneId);
        }

        @Test
        void shouldSolidifyPastVirtualEventsForSelectedDay() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            // Set the selected day to a past date (e.g., 1 day ago)
            LocalDate selectedDate = nowInUserZone.toLocalDate().minusDays(1);  // Past day
            ZonedDateTime startOfDay = selectedDate.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime endOfDay = selectedDate.plusDays(1).atStartOfDay(userZoneId)
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .minusNanos(1);

            // Mock confirmed event for the past date
            Event event = mock(Event.class);
            EventResponseDTO eventDTO = TestUtils.createValidScheduledEventResponseDTO(fixedClock);

            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), startOfDay, endOfDay))
                    .thenReturn(Collections.singletonList(event));
            when(eventResponseDTOFactory.createFromEvent(event)).thenReturn(eventDTO);

            // Mock solidification of past recurrences
            doNothing().when(eventBO).solidifyRecurrences(eq(user.getId()), eq(startOfDay), eq(endOfDay), eq(userZoneId));

            // Mock the EventBO method that creates the DayViewDTO
            List<EventResponseDTO> confirmedEvents = Collections.singletonList(eventDTO);
            DayViewDTO expectedDayView = new DayViewDTO(selectedDate, confirmedEvents);
            when(eventBO.generateDayViewData(eq(selectedDate), eq(confirmedEvents), eq(Collections.emptyList())))
                    .thenReturn(expectedDayView);

            // Act
            DayViewDTO dayViewDTO = eventService.generateDayView(selectedDate);

            // Assert
            assertNotNull(dayViewDTO, "DayViewDTO should not be null.");
            assertEquals(1, dayViewDTO.events().size(), "The events list should contain 1 event (solidified).");
            assertTrue(dayViewDTO.events().contains(eventDTO), "The event should be the confirmed solidified EventResponseDTO.");

            // Verify solidifyRecurrences was called for the past day with correct end window
            verify(eventBO).solidifyRecurrences(user.getId(), startOfDay, endOfDay, userZoneId);

            // Verify virtuals were not generated for past dates
            verifyNoInteractions(recurringEventBO);
        }

        @Test
        void shouldReturnEmptyEventsForFutureDateWithMultipleVirtualEvents() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            LocalDate selectedDate = nowInUserZone.toLocalDate().plusDays(7);  // Future date
            ZonedDateTime startOfDay = selectedDate.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime endOfDay = selectedDate.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);

            // Mock that no confirmed events exist for the selected date
            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), startOfDay, endOfDay))
                    .thenReturn(Collections.emptyList());

            // Mock virtuals generation returning empty list
            when(recurringEventBO.generateVirtuals(user.getId(), startOfDay, endOfDay, userZoneId))
                    .thenReturn(Collections.emptyList());

            // Mock the EventBO method that creates the DayViewDTO
            DayViewDTO expectedDayView = new DayViewDTO(selectedDate, Collections.emptyList());
            when(eventBO.generateDayViewData(eq(selectedDate), eq(Collections.emptyList()), eq(Collections.emptyList())))
                    .thenReturn(expectedDayView);

            // Act
            DayViewDTO dayViewDTO = eventService.generateDayView(selectedDate);

            // Assert
            assertNotNull(dayViewDTO, "DayViewDTO should not be null.");
            assertEquals(0, dayViewDTO.events().size(), "The events list should be empty for a future date with no confirmed or virtual events.");

            // Verify interactions
            verify(eventBO).getConfirmedEventsForUserInRange(user.getId(), startOfDay, endOfDay);
            verify(recurringEventBO).generateVirtuals(user.getId(), startOfDay, endOfDay, userZoneId);
            verify(eventBO, never()).solidifyRecurrences(anyLong(), any(), any(), any());
        }

        @Test
        void shouldNotSolidifyVirtualEventsOutsideSelectedDay() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            LocalDate selectedDate = nowInUserZone.toLocalDate();  // Today's date
            ZonedDateTime nowInUtc = nowInUserZone.withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime startOfDay = selectedDate.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime endOfDay = selectedDate.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);

            // Mock confirmed events as empty
            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), startOfDay, endOfDay))
                    .thenReturn(Collections.emptyList());

            // Mock virtuals generation to return an empty list (no virtuals in scope for test)
            when(recurringEventBO.generateVirtuals(user.getId(), nowInUtc, endOfDay, userZoneId))
                    .thenReturn(Collections.emptyList());

            // Mock the EventBO method that creates the DayViewDTO
            DayViewDTO expectedDayView = new DayViewDTO(selectedDate, Collections.emptyList());
            when(eventBO.generateDayViewData(eq(selectedDate), eq(Collections.emptyList()), eq(Collections.emptyList())))
                    .thenReturn(expectedDayView);

            // Act
            DayViewDTO dayViewDTO = eventService.generateDayView(selectedDate);

            // Assert
            assertNotNull(dayViewDTO, "DayViewDTO should not be null.");
            assertEquals(0, dayViewDTO.events().size(), "The events list should be empty.");

            // Verify solidify was called for today up to now
            verify(eventBO).solidifyRecurrences(user.getId(), startOfDay, nowInUtc, userZoneId);

            // Verify virtuals generation was called with correct parameters
            verify(recurringEventBO).generateVirtuals(user.getId(), nowInUtc, endOfDay, userZoneId);
        }

        @Test
        void shouldReturnSolidifiedAndVirtualEventsForToday() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            LocalDate today = nowInUserZone.toLocalDate();
            ZonedDateTime nowInUtc = nowInUserZone.withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime startOfDay = today.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime endOfDay = today.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);

            // Mock confirmed event
            Event confirmedEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            EventResponseDTO confirmedEventDTO = TestUtils.createValidScheduledEventResponseDTO(fixedClock);

            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), startOfDay, endOfDay))
                    .thenReturn(Collections.singletonList(confirmedEvent));
            when(eventResponseDTOFactory.createFromEvent(confirmedEvent)).thenReturn(confirmedEventDTO);

            // Mock virtual event
            EventResponseDTO virtualEventDTO = TestUtils.createValidScheduledEventResponseDTO(fixedClock);
            when(recurringEventBO.generateVirtuals(user.getId(), nowInUtc, endOfDay, userZoneId))
                    .thenReturn(Collections.singletonList(virtualEventDTO));

            // Mock the EventBO method that creates the DayViewDTO
            List<EventResponseDTO> confirmedEvents = Collections.singletonList(confirmedEventDTO);
            List<EventResponseDTO> virtualEvents = Collections.singletonList(virtualEventDTO);
            List<EventResponseDTO> combinedEvents = Arrays.asList(confirmedEventDTO, virtualEventDTO);
            DayViewDTO expectedDayView = new DayViewDTO(today, combinedEvents);
            when(eventBO.generateDayViewData(eq(today), eq(confirmedEvents), eq(virtualEvents)))
                    .thenReturn(expectedDayView);

            // Act
            DayViewDTO dayViewDTO = eventService.generateDayView(today);

            // Assert
            assertNotNull(dayViewDTO, "DayViewDTO should not be null.");
            assertEquals(2, dayViewDTO.events().size(), "Should return both confirmed and virtual events.");
            assertTrue(dayViewDTO.events().contains(confirmedEventDTO), "Should contain confirmed event DTO.");
            assertTrue(dayViewDTO.events().contains(virtualEventDTO), "Should contain virtual event DTO.");

            // Verify solidify
            verify(eventBO).solidifyRecurrences(user.getId(), startOfDay, nowInUtc, userZoneId);
        }

        @Test
        void shouldReturnEmptyEventsForTodayWithNoEvents() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            LocalDate today = nowInUserZone.toLocalDate();
            ZonedDateTime nowInUtc = nowInUserZone.withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime startOfDay = today.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime endOfDay = today.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);

            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), startOfDay, endOfDay))
                    .thenReturn(Collections.emptyList());
            when(recurringEventBO.generateVirtuals(user.getId(), nowInUtc, endOfDay, userZoneId))
                    .thenReturn(Collections.emptyList());

            // Mock the EventBO method that creates the DayViewDTO
            DayViewDTO expectedDayView = new DayViewDTO(today, Collections.emptyList());
            when(eventBO.generateDayViewData(eq(today), eq(Collections.emptyList()), eq(Collections.emptyList())))
                    .thenReturn(expectedDayView);

            // Act
            DayViewDTO dayViewDTO = eventService.generateDayView(today);

            // Assert
            assertNotNull(dayViewDTO, "DayViewDTO should not be null.");
            assertEquals(0, dayViewDTO.events().size(), "The events list should be empty.");

            // Verify solidify
            verify(eventBO).solidifyRecurrences(user.getId(), startOfDay, nowInUtc, userZoneId);
            verify(recurringEventBO).generateVirtuals(user.getId(), nowInUtc, endOfDay, userZoneId);
        }

    }


    @Nested
    class GenerateWeekViewTests {

        @Test
        void shouldReturnEmptyWeekViewForPastWeekWithNoEvents() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            // Select a past week anchor date (e.g. last week Monday)
            LocalDate anchorDate = nowInUserZone.toLocalDate().minusWeeks(1).with(DayOfWeek.MONDAY);
            LocalDate weekStartDate = anchorDate;
            LocalDate weekEndDate = weekStartDate.plusDays(6);

            ZonedDateTime weekStartTime = weekStartDate.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime weekEndTime = weekEndDate.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);

            // Mock confirmed events as empty
            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), weekStartTime, weekEndTime))
                    .thenReturn(Collections.emptyList());

            // Mock the EventBO method that creates the WeekViewDTO
            List<DayViewDTO> emptyDays = Collections.nCopies(7, new DayViewDTO(weekStartDate, Collections.emptyList()));
            WeekViewDTO expectedWeekView = new WeekViewDTO(emptyDays);
            when(eventBO.generateWeekViewData(eq(user.getId()), eq(anchorDate), eq(userZoneId), eq(Collections.emptyList()), eq(Collections.emptyList())))
                    .thenReturn(expectedWeekView);

            // Act
            WeekViewDTO weekViewDTO = eventService.generateWeekView(anchorDate);

            // Assert
            assertNotNull(weekViewDTO, "WeekViewDTO should not be null.");
            assertEquals(7, weekViewDTO.days().size(), "Should contain 7 days.");
            weekViewDTO.days().forEach(day -> assertTrue(day.events().isEmpty(), "Each day should have no events."));

            // Verify solidifyRecurrences was called for the entire week
            verify(eventBO).solidifyRecurrences(user.getId(), weekStartTime, weekEndTime, userZoneId);

            // Verify no virtuals generated for past week
            verifyNoInteractions(recurringEventBO);
        }

        @Test
        void shouldReturnConfirmedEventsForPastWeek() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            // Select a past week anchor date (e.g. last week Monday)
            LocalDate anchorDate = nowInUserZone.toLocalDate().minusWeeks(1).with(DayOfWeek.MONDAY);
            LocalDate weekStartDate = anchorDate;
            LocalDate weekEndDate = weekStartDate.plusDays(6);

            ZonedDateTime weekStartTime = weekStartDate.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime weekEndTime = weekEndDate.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);

            // Create distinct start times for Monday and Wednesday
            ZonedDateTime mondayStart = weekStartDate.atStartOfDay(userZoneId).withHour(9).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime wednesdayStart = weekStartDate.plusDays(2).atStartOfDay(userZoneId).withHour(14).withZoneSameInstant(ZoneOffset.UTC);

            // Mock confirmed events (1 event on Monday, 1 on Wednesday) with unique times
            Event eventMonday = TestUtils.createValidScheduledEvent(user, fixedClock);
            eventMonday.setStartTime(mondayStart);
            eventMonday.setEndTime(mondayStart.plusHours(1));

            Event eventWednesday = TestUtils.createValidScheduledEvent(user, fixedClock);
            eventWednesday.setStartTime(wednesdayStart);
            eventWednesday.setEndTime(wednesdayStart.plusHours(1));

            EventResponseDTO eventMondayDTO = createEventResponseDTO(eventMonday);
            EventResponseDTO eventWednesdayDTO = createEventResponseDTO(eventWednesday);

            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), weekStartTime, weekEndTime))
                    .thenReturn(Arrays.asList(eventMonday, eventWednesday));

            when(eventResponseDTOFactory.createFromEvent(eventMonday)).thenReturn(eventMondayDTO);
            when(eventResponseDTOFactory.createFromEvent(eventWednesday)).thenReturn(eventWednesdayDTO);

            // Mock the EventBO method that creates the WeekViewDTO
            List<EventResponseDTO> confirmedEvents = Arrays.asList(eventMondayDTO, eventWednesdayDTO);
            // Create days with Monday having eventMonday and Wednesday having eventWednesday
            List<DayViewDTO> weekDays = Arrays.asList(
                new DayViewDTO(weekStartDate, Arrays.asList(eventMondayDTO)), // Monday
                new DayViewDTO(weekStartDate.plusDays(1), Collections.emptyList()), // Tuesday
                new DayViewDTO(weekStartDate.plusDays(2), Arrays.asList(eventWednesdayDTO)), // Wednesday
                new DayViewDTO(weekStartDate.plusDays(3), Collections.emptyList()), // Thursday
                new DayViewDTO(weekStartDate.plusDays(4), Collections.emptyList()), // Friday
                new DayViewDTO(weekStartDate.plusDays(5), Collections.emptyList()), // Saturday
                new DayViewDTO(weekStartDate.plusDays(6), Collections.emptyList())  // Sunday
            );
            WeekViewDTO expectedWeekView = new WeekViewDTO(weekDays);
            when(eventBO.generateWeekViewData(eq(user.getId()), eq(anchorDate), eq(userZoneId), eq(confirmedEvents), eq(Collections.emptyList())))
                    .thenReturn(expectedWeekView);

            // Act
            WeekViewDTO weekViewDTO = eventService.generateWeekView(anchorDate);

            // Assert
            assertNotNull(weekViewDTO, "WeekViewDTO should not be null.");
            assertEquals(7, weekViewDTO.days().size(), "Should contain 7 days.");

            boolean mondayHasEvent = weekViewDTO.days().stream()
                    .filter(day -> day.date().getDayOfWeek() == DayOfWeek.MONDAY)
                    .anyMatch(day -> day.events().contains(eventMondayDTO));

            boolean wednesdayHasEvent = weekViewDTO.days().stream()
                    .filter(day -> day.date().getDayOfWeek() == DayOfWeek.WEDNESDAY)
                    .anyMatch(day -> day.events().contains(eventWednesdayDTO));

            assertTrue(mondayHasEvent, "Monday should contain the confirmed event.");
            assertTrue(wednesdayHasEvent, "Wednesday should contain the confirmed event.");

            // Verify solidifyRecurrences was called for the entire week
            verify(eventBO).solidifyRecurrences(user.getId(), weekStartTime, weekEndTime, userZoneId);

            // Verify no virtuals generated for past week
            verifyNoInteractions(recurringEventBO);
        }

        @Test
        void shouldReturnConfirmedEventsForCurrentWeekWithoutVirtuals() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            LocalDate today = nowInUserZone.toLocalDate();
            LocalDate anchorDate = today.with(DayOfWeek.MONDAY);
            LocalDate weekStartDate = anchorDate;
            LocalDate weekEndDate = weekStartDate.plusDays(6);

            ZonedDateTime nowInUtc = nowInUserZone.withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime weekStartTime = weekStartDate.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime weekEndTime = weekEndDate.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);

            // Mock confirmed events (1 event on Tuesday)
            ZonedDateTime tuesdayStart = weekStartDate.plusDays(1).atStartOfDay(userZoneId).withHour(10).withZoneSameInstant(ZoneOffset.UTC);

            Event eventTuesday = TestUtils.createValidScheduledEvent(user, fixedClock);
            eventTuesday.setStartTime(tuesdayStart);
            eventTuesday.setEndTime(tuesdayStart.plusHours(1));

            EventResponseDTO eventTuesdayDTO = createEventResponseDTO(eventTuesday);

            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), weekStartTime, weekEndTime))
                    .thenReturn(Collections.singletonList(eventTuesday));

            when(eventResponseDTOFactory.createFromEvent(eventTuesday)).thenReturn(eventTuesdayDTO);

            // Mock generateVirtuals to return empty
            when(recurringEventBO.generateVirtuals(user.getId(), nowInUtc, weekEndTime, userZoneId))
                    .thenReturn(Collections.emptyList());

            // Mock the EventBO method that creates the WeekViewDTO
            List<EventResponseDTO> confirmedEvents = Collections.singletonList(eventTuesdayDTO);
            // Create days with Tuesday having the event
            List<DayViewDTO> weekDays = Arrays.asList(
                new DayViewDTO(weekStartDate, Collections.emptyList()), // Monday
                new DayViewDTO(weekStartDate.plusDays(1), Arrays.asList(eventTuesdayDTO)), // Tuesday
                new DayViewDTO(weekStartDate.plusDays(2), Collections.emptyList()), // Wednesday
                new DayViewDTO(weekStartDate.plusDays(3), Collections.emptyList()), // Thursday
                new DayViewDTO(weekStartDate.plusDays(4), Collections.emptyList()), // Friday
                new DayViewDTO(weekStartDate.plusDays(5), Collections.emptyList()), // Saturday
                new DayViewDTO(weekStartDate.plusDays(6), Collections.emptyList())  // Sunday
            );
            WeekViewDTO expectedWeekView = new WeekViewDTO(weekDays);
            when(eventBO.generateWeekViewData(eq(user.getId()), eq(anchorDate), eq(userZoneId), eq(confirmedEvents), eq(Collections.emptyList())))
                    .thenReturn(expectedWeekView);

            // Act
            WeekViewDTO weekViewDTO = eventService.generateWeekView(anchorDate);

            // Assert
            assertNotNull(weekViewDTO, "WeekViewDTO should not be null.");
            assertEquals(7, weekViewDTO.days().size(), "Should contain 7 days.");

            boolean tuesdayHasEvent = weekViewDTO.days().stream()
                    .filter(day -> day.date().getDayOfWeek() == DayOfWeek.TUESDAY)
                    .anyMatch(day -> day.events().contains(eventTuesdayDTO));

            assertTrue(tuesdayHasEvent, "Tuesday should contain the confirmed event.");

            // Verify solidifyRecurrences was called up to now
            verify(eventBO).solidifyRecurrences(user.getId(), weekStartTime, nowInUtc, userZoneId);

            // Verify virtuals generated with nowInUtc as start window
            verify(recurringEventBO).generateVirtuals(user.getId(), nowInUtc, weekEndTime, userZoneId);
        }

        @Test
        void shouldReturnVirtualEventsForCurrentWeekWithoutConfirmedEvents() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            LocalDate today = nowInUserZone.toLocalDate();
            LocalDate anchorDate = today.with(DayOfWeek.MONDAY);
            LocalDate weekStartDate = anchorDate;
            LocalDate weekEndDate = weekStartDate.plusDays(6);

            ZonedDateTime nowInUtc = nowInUserZone.withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime weekStartTime = weekStartDate.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime weekEndTime = weekEndDate.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);

            // Mock confirmed events as empty
            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), weekStartTime, weekEndTime))
                    .thenReturn(Collections.emptyList());

            // Mock virtual event DTO
            EventResponseDTO virtualEventDTO = TestUtils.createValidScheduledEventResponseDTO(fixedClock);

            when(recurringEventBO.generateVirtuals(user.getId(), nowInUtc, weekEndTime, userZoneId))
                    .thenReturn(Collections.singletonList(virtualEventDTO));

            // Mock the EventBO method that creates the WeekViewDTO
            List<EventResponseDTO> virtualEvents = Collections.singletonList(virtualEventDTO);
            // Create days with one day having the virtual event (let's say Monday)
            List<DayViewDTO> weekDays = Arrays.asList(
                new DayViewDTO(weekStartDate, Arrays.asList(virtualEventDTO)), // Monday
                new DayViewDTO(weekStartDate.plusDays(1), Collections.emptyList()), // Tuesday
                new DayViewDTO(weekStartDate.plusDays(2), Collections.emptyList()), // Wednesday
                new DayViewDTO(weekStartDate.plusDays(3), Collections.emptyList()), // Thursday
                new DayViewDTO(weekStartDate.plusDays(4), Collections.emptyList()), // Friday
                new DayViewDTO(weekStartDate.plusDays(5), Collections.emptyList()), // Saturday
                new DayViewDTO(weekStartDate.plusDays(6), Collections.emptyList())  // Sunday
            );
            WeekViewDTO expectedWeekView = new WeekViewDTO(weekDays);
            when(eventBO.generateWeekViewData(eq(user.getId()), eq(anchorDate), eq(userZoneId), eq(Collections.emptyList()), eq(virtualEvents)))
                    .thenReturn(expectedWeekView);

            // Act
            WeekViewDTO weekViewDTO = eventService.generateWeekView(anchorDate);

            // Assert
            assertNotNull(weekViewDTO, "WeekViewDTO should not be null.");
            assertEquals(7, weekViewDTO.days().size(), "Should contain 7 days.");

            boolean anyDayHasVirtualEvent = weekViewDTO.days().stream()
                    .anyMatch(day -> day.events().contains(virtualEventDTO));

            assertTrue(anyDayHasVirtualEvent, "At least one day should contain the virtual event.");

            // Verify solidifyRecurrences was called up to now
            verify(eventBO).solidifyRecurrences(user.getId(), weekStartTime, nowInUtc, userZoneId);

            // Verify virtuals generated with nowInUtc as start window
            verify(recurringEventBO).generateVirtuals(user.getId(), nowInUtc, weekEndTime, userZoneId);
        }

        @Test
        void shouldReturnConfirmedAndVirtualEventsForCurrentWeek() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            LocalDate today = nowInUserZone.toLocalDate();
            LocalDate anchorDate = today.with(DayOfWeek.MONDAY);
            LocalDate weekStartDate = anchorDate;
            LocalDate weekEndDate = weekStartDate.plusDays(6);

            ZonedDateTime nowInUtc = nowInUserZone.withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime weekStartTime = weekStartDate.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime weekEndTime = weekEndDate.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);

            // Mock confirmed event (Thursday)
            ZonedDateTime thursdayStart = weekStartDate.plusDays(3).atStartOfDay(userZoneId).withHour(11).withZoneSameInstant(ZoneOffset.UTC);

            Event confirmedEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            confirmedEvent.setStartTime(thursdayStart);
            confirmedEvent.setEndTime(thursdayStart.plusHours(1));

            EventResponseDTO confirmedEventDTO = createEventResponseDTO(confirmedEvent);

            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), weekStartTime, weekEndTime))
                    .thenReturn(Collections.singletonList(confirmedEvent));

            when(eventResponseDTOFactory.createFromEvent(confirmedEvent)).thenReturn(confirmedEventDTO);

            // Mock virtual event DTO
            EventResponseDTO virtualEventDTO = TestUtils.createValidScheduledEventResponseDTO(fixedClock);

            when(recurringEventBO.generateVirtuals(user.getId(), nowInUtc, weekEndTime, userZoneId))
                    .thenReturn(Collections.singletonList(virtualEventDTO));

            // Mock the EventBO method that creates the WeekViewDTO
            List<EventResponseDTO> confirmedEvents = Collections.singletonList(confirmedEventDTO);
            List<EventResponseDTO> virtualEvents = Collections.singletonList(virtualEventDTO);
            // Create days with Thursday having confirmed event and another day having virtual event
            List<DayViewDTO> weekDays = Arrays.asList(
                new DayViewDTO(weekStartDate, Arrays.asList(virtualEventDTO)), // Monday - virtual event
                new DayViewDTO(weekStartDate.plusDays(1), Collections.emptyList()), // Tuesday
                new DayViewDTO(weekStartDate.plusDays(2), Collections.emptyList()), // Wednesday
                new DayViewDTO(weekStartDate.plusDays(3), Arrays.asList(confirmedEventDTO)), // Thursday - confirmed event
                new DayViewDTO(weekStartDate.plusDays(4), Collections.emptyList()), // Friday
                new DayViewDTO(weekStartDate.plusDays(5), Collections.emptyList()), // Saturday
                new DayViewDTO(weekStartDate.plusDays(6), Collections.emptyList())  // Sunday
            );
            WeekViewDTO expectedWeekView = new WeekViewDTO(weekDays);
            when(eventBO.generateWeekViewData(eq(user.getId()), eq(anchorDate), eq(userZoneId), eq(confirmedEvents), eq(virtualEvents)))
                    .thenReturn(expectedWeekView);

            // Act
            WeekViewDTO weekViewDTO = eventService.generateWeekView(anchorDate);

            // Assert
            assertNotNull(weekViewDTO, "WeekViewDTO should not be null.");
            assertEquals(7, weekViewDTO.days().size(), "Should contain 7 days.");

            boolean thursdayHasConfirmedEvent = weekViewDTO.days().stream()
                    .filter(day -> day.date().getDayOfWeek() == DayOfWeek.THURSDAY)
                    .anyMatch(day -> day.events().contains(confirmedEventDTO));

            boolean anyDayHasVirtualEvent = weekViewDTO.days().stream()
                    .anyMatch(day -> day.events().contains(virtualEventDTO));

            assertTrue(thursdayHasConfirmedEvent, "Thursday should contain the confirmed event.");
            assertTrue(anyDayHasVirtualEvent, "At least one day should contain the virtual event.");

            // Verify solidifyRecurrences was called up to now
            verify(eventBO).solidifyRecurrences(user.getId(), weekStartTime, nowInUtc, userZoneId);

            // Verify virtuals generated with nowInUtc as start window
            verify(recurringEventBO).generateVirtuals(user.getId(), nowInUtc, weekEndTime, userZoneId);
        }

        @Test
        void shouldReturnEmptyWeekViewForFutureWeekWithNoEvents() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            // Select a future week anchor date (e.g. next week Monday)
            LocalDate anchorDate = nowInUserZone.toLocalDate().plusWeeks(1).with(DayOfWeek.MONDAY);
            LocalDate weekStartDate = anchorDate;
            LocalDate weekEndDate = weekStartDate.plusDays(6);

            ZonedDateTime weekStartTime = weekStartDate.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime weekEndTime = weekEndDate.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);

            // Mock confirmed events as empty
            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), weekStartTime, weekEndTime))
                    .thenReturn(Collections.emptyList());

            // Mock generateVirtuals to return empty
            when(recurringEventBO.generateVirtuals(user.getId(), weekStartTime, weekEndTime, userZoneId))
                    .thenReturn(Collections.emptyList());

            // Mock the EventBO method that creates the WeekViewDTO
            List<DayViewDTO> emptyWeekDays = Arrays.asList(
                new DayViewDTO(weekStartDate, Collections.emptyList()), // Monday
                new DayViewDTO(weekStartDate.plusDays(1), Collections.emptyList()), // Tuesday
                new DayViewDTO(weekStartDate.plusDays(2), Collections.emptyList()), // Wednesday
                new DayViewDTO(weekStartDate.plusDays(3), Collections.emptyList()), // Thursday
                new DayViewDTO(weekStartDate.plusDays(4), Collections.emptyList()), // Friday
                new DayViewDTO(weekStartDate.plusDays(5), Collections.emptyList()), // Saturday
                new DayViewDTO(weekStartDate.plusDays(6), Collections.emptyList())  // Sunday
            );
            WeekViewDTO expectedWeekView = new WeekViewDTO(emptyWeekDays);
            when(eventBO.generateWeekViewData(eq(user.getId()), eq(anchorDate), eq(userZoneId), eq(Collections.emptyList()), eq(Collections.emptyList())))
                    .thenReturn(expectedWeekView);

            // Act
            WeekViewDTO weekViewDTO = eventService.generateWeekView(anchorDate);

            // Assert
            assertNotNull(weekViewDTO, "WeekViewDTO should not be null.");
            assertEquals(7, weekViewDTO.days().size(), "Should contain 7 days.");
            weekViewDTO.days().forEach(day -> assertTrue(day.events().isEmpty(), "Each day should have no events."));

            // Verify solidifyRecurrences was NOT called for future week
            verify(eventBO, never()).solidifyRecurrences(anyLong(), any(), any(), any());

            // Verify virtuals generated with weekStartTime as start window
            verify(recurringEventBO).generateVirtuals(user.getId(), weekStartTime, weekEndTime, userZoneId);
        }

        @Test
        void shouldReturnVirtualEventsForFutureWeekWithoutConfirmedEvents() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            // Select a future week anchor date (e.g. next week Monday)
            LocalDate anchorDate = nowInUserZone.toLocalDate().plusWeeks(1).with(DayOfWeek.MONDAY);
            LocalDate weekStartDate = anchorDate;
            LocalDate weekEndDate = weekStartDate.plusDays(6);

            ZonedDateTime weekStartTime = weekStartDate.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime weekEndTime = weekEndDate.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);

            // Mock confirmed events as empty
            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), weekStartTime, weekEndTime))
                    .thenReturn(Collections.emptyList());

            // Explicitly set virtual event's start time within the future week (e.g. Tuesday 9 AM)
            ZonedDateTime tuesdayStart = weekStartDate.plusDays(1).atStartOfDay(userZoneId).withHour(9).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime tuesdayEnd = tuesdayStart.plusHours(1);

            EventResponseDTO virtualEventDTO = new EventResponseDTO(
                    null,
                    "Virtual Event",
                    tuesdayStart,
                    tuesdayEnd,
                    60,
                    null,
                    null,
                    "desc",
                    user.getUsername(),
                    user.getTimezone(),
                    null,
                    false,
                    false,
                    false,
                    true
            );

            // Mock generateVirtuals to return the virtual event DTO
            when(recurringEventBO.generateVirtuals(user.getId(), weekStartTime, weekEndTime, userZoneId))
                    .thenReturn(Collections.singletonList(virtualEventDTO));

            // Mock the EventBO method that creates the WeekViewDTO
            List<EventResponseDTO> virtualEvents = Collections.singletonList(virtualEventDTO);
            // Create days with Tuesday having the virtual event
            List<DayViewDTO> weekDays = Arrays.asList(
                new DayViewDTO(weekStartDate, Collections.emptyList()), // Monday
                new DayViewDTO(weekStartDate.plusDays(1), Arrays.asList(virtualEventDTO)), // Tuesday - virtual event
                new DayViewDTO(weekStartDate.plusDays(2), Collections.emptyList()), // Wednesday
                new DayViewDTO(weekStartDate.plusDays(3), Collections.emptyList()), // Thursday
                new DayViewDTO(weekStartDate.plusDays(4), Collections.emptyList()), // Friday
                new DayViewDTO(weekStartDate.plusDays(5), Collections.emptyList()), // Saturday
                new DayViewDTO(weekStartDate.plusDays(6), Collections.emptyList())  // Sunday
            );
            WeekViewDTO expectedWeekView = new WeekViewDTO(weekDays);
            when(eventBO.generateWeekViewData(eq(user.getId()), eq(anchorDate), eq(userZoneId), eq(Collections.emptyList()), eq(virtualEvents)))
                    .thenReturn(expectedWeekView);

            // Act
            WeekViewDTO weekViewDTO = eventService.generateWeekView(anchorDate);

            // Assert
            assertNotNull(weekViewDTO, "WeekViewDTO should not be null.");
            assertEquals(7, weekViewDTO.days().size(), "Should contain 7 days.");

            boolean anyDayHasVirtualEvent = weekViewDTO.days().stream()
                    .flatMap(day -> day.events().stream())
                    .anyMatch(event -> event.name().equals(virtualEventDTO.name()));

            assertTrue(anyDayHasVirtualEvent, "At least one day should contain the virtual event.");

            // Verify solidifyRecurrences was NOT called for future week
            verify(eventBO, never()).solidifyRecurrences(anyLong(), any(), any(), any());

            // Verify virtuals generated with weekStartTime as start window
            verify(recurringEventBO).generateVirtuals(user.getId(), weekStartTime, weekEndTime, userZoneId);
        }

        @Test
        void shouldHandleDSTTransitionWeekCorrectly() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            // Use existing fixedClock (e.g. June 26, 2025) where DST week is in the past
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            // Set user's timezone to one with DST (e.g., America/New_York)
            ZoneId dstZoneId = ZoneId.of("America/New_York");

            // Anchor date during DST transition week (March 10, 2025)
            LocalDate anchorDate = LocalDate.of(2025, 3, 10);
            LocalDate weekStartDate = anchorDate;
            LocalDate weekEndDate = weekStartDate.plusDays(6);

            ZonedDateTime weekStartTime = weekStartDate.atStartOfDay(dstZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime weekEndTime = weekEndDate.plusDays(1).atStartOfDay(dstZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);

            // Mock confirmed events before and after DST transition (Saturday and Sunday)
            ZonedDateTime saturdayStart = weekStartDate.plusDays(5).atStartOfDay(dstZoneId).withHour(9).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime sundayStart = weekStartDate.plusDays(6).atStartOfDay(dstZoneId).withHour(9).withZoneSameInstant(ZoneOffset.UTC);

            Event saturdayEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            saturdayEvent.setStartTime(saturdayStart);
            saturdayEvent.setEndTime(saturdayStart.plusHours(1));

            Event sundayEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            sundayEvent.setStartTime(sundayStart);
            sundayEvent.setEndTime(sundayStart.plusHours(1));

            EventResponseDTO saturdayDTO = createEventResponseDTO(saturdayEvent);
            EventResponseDTO sundayDTO = createEventResponseDTO(sundayEvent);

            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), weekStartTime, weekEndTime))
                    .thenReturn(Arrays.asList(saturdayEvent, sundayEvent));

            when(eventResponseDTOFactory.createFromEvent(saturdayEvent)).thenReturn(saturdayDTO);
            when(eventResponseDTOFactory.createFromEvent(sundayEvent)).thenReturn(sundayDTO);

            // Mock the EventBO method that creates the WeekViewDTO
            List<EventResponseDTO> confirmedEvents = Arrays.asList(saturdayDTO, sundayDTO);
            // Create days with Saturday and Sunday having events
            List<DayViewDTO> weekDays = Arrays.asList(
                new DayViewDTO(weekStartDate, Collections.emptyList()), // Monday
                new DayViewDTO(weekStartDate.plusDays(1), Collections.emptyList()), // Tuesday
                new DayViewDTO(weekStartDate.plusDays(2), Collections.emptyList()), // Wednesday
                new DayViewDTO(weekStartDate.plusDays(3), Collections.emptyList()), // Thursday
                new DayViewDTO(weekStartDate.plusDays(4), Collections.emptyList()), // Friday
                new DayViewDTO(weekStartDate.plusDays(5), Arrays.asList(saturdayDTO)), // Saturday - confirmed event
                new DayViewDTO(weekStartDate.plusDays(6), Arrays.asList(sundayDTO))  // Sunday - confirmed event
            );
            WeekViewDTO expectedWeekView = new WeekViewDTO(weekDays);
            when(eventBO.generateWeekViewData(eq(user.getId()), eq(anchorDate), eq(dstZoneId), eq(confirmedEvents), eq(Collections.emptyList())))
                    .thenReturn(expectedWeekView);

            // Act
            WeekViewDTO weekViewDTO = eventService.generateWeekView(anchorDate);

            // Assert
            assertNotNull(weekViewDTO, "WeekViewDTO should not be null.");
            assertEquals(7, weekViewDTO.days().size(), "Should contain 7 days.");

            boolean saturdayHasEvent = weekViewDTO.days().stream()
                    .filter(day -> day.date().getDayOfWeek() == DayOfWeek.SATURDAY)
                    .anyMatch(day -> day.events().contains(saturdayDTO));

            boolean sundayHasEvent = weekViewDTO.days().stream()
                    .filter(day -> day.date().getDayOfWeek() == DayOfWeek.SUNDAY)
                    .anyMatch(day -> day.events().contains(sundayDTO));

            assertTrue(saturdayHasEvent, "Saturday should contain the confirmed event before DST transition.");
            assertTrue(sundayHasEvent, "Sunday should contain the confirmed event after DST transition.");

            // Verify solidifyRecurrences was called for the entire week (since it's in the past)
            verify(eventBO).solidifyRecurrences(user.getId(), weekStartTime, weekEndTime, dstZoneId);

            // Verify virtuals were NOT called for past week
            verifyNoInteractions(recurringEventBO);
        }

        @Test
        void shouldHandleDifferentUserTimezones() {
            // Arrange - User in Tokyo timezone
            ZoneId tokyoZone = ZoneId.of("Asia/Tokyo");
            User tokyoUser = createValidUserEntityWithId();
            tokyoUser.setTimezone("Asia/Tokyo");
            
            Clock tokyoClock = Clock.fixed(nowInUserZone.toInstant(), tokyoZone);
            LocalDate anchorDate = nowInUserZone.toLocalDate();

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(tokyoUser);
            when(clockProvider.getClockForUser(tokyoUser)).thenReturn(tokyoClock);
            
            LocalDate weekStartDate = anchorDate.with(DayOfWeek.MONDAY);
            LocalDate weekEndDate = weekStartDate.plusDays(6);
            
            ZonedDateTime weekStartTime = weekStartDate.atStartOfDay(tokyoZone).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime weekEndTime = weekEndDate.plusDays(1).atStartOfDay(tokyoZone).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);
            
            when(eventBO.getConfirmedEventsForUserInRange(tokyoUser.getId(), weekStartTime, weekEndTime))
                    .thenReturn(Collections.emptyList());
            when(recurringEventBO.generateVirtuals(eq(tokyoUser.getId()), any(), eq(weekEndTime), eq(tokyoZone)))
                    .thenReturn(Collections.emptyList());
            
            List<DayViewDTO> emptyDays = Collections.nCopies(7, new DayViewDTO(weekStartDate, Collections.emptyList()));
            WeekViewDTO expectedWeekView = new WeekViewDTO(emptyDays);
            when(eventBO.generateWeekViewData(eq(tokyoUser.getId()), eq(anchorDate), eq(tokyoZone), any(), any()))
                    .thenReturn(expectedWeekView);

            // Act
            WeekViewDTO result = eventService.generateWeekView(anchorDate);

            // Assert
            assertNotNull(result);
            // Verify that Tokyo timezone was used for all calculations
            verify(eventBO).getConfirmedEventsForUserInRange(tokyoUser.getId(), weekStartTime, weekEndTime);
            verify(eventBO).generateWeekViewData(tokyoUser.getId(), anchorDate, tokyoZone, Collections.emptyList(), Collections.emptyList());
        }

        @Test
        void shouldHandleWeekBoundariesAcrossMonths() {
            // Arrange - Week that spans across months (e.g., end of January to beginning of February)
            LocalDate endOfMonth = LocalDate.of(2024, 1, 31); // Last day of January
            LocalDate anchorDate = endOfMonth.with(DayOfWeek.WEDNESDAY); // Mid-week anchor
            
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);
            
            LocalDate weekStartDate = anchorDate.with(DayOfWeek.MONDAY); // Should be January 29
            LocalDate weekEndDate = weekStartDate.plusDays(6); // Should be February 4
            
            ZonedDateTime weekStartTime = weekStartDate.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime weekEndTime = weekEndDate.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);
            
            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), weekStartTime, weekEndTime))
                    .thenReturn(Collections.emptyList());
            
            List<DayViewDTO> emptyDays = Collections.nCopies(7, new DayViewDTO(weekStartDate, Collections.emptyList()));
            WeekViewDTO expectedWeekView = new WeekViewDTO(emptyDays);
            when(eventBO.generateWeekViewData(eq(user.getId()), eq(anchorDate), eq(userZoneId), any(), any()))
                    .thenReturn(expectedWeekView);

            // Act
            WeekViewDTO result = eventService.generateWeekView(anchorDate);

            // Assert
            assertNotNull(result);
            // Verify correct week boundaries were calculated
            verify(eventBO).getConfirmedEventsForUserInRange(user.getId(), weekStartTime, weekEndTime);
            assertTrue(weekStartDate.getMonthValue() != weekEndDate.getMonthValue(), 
                    "Week should span across different months");
        }

        @Test
        void shouldHandleWeekBoundariesAcrossYears() {
            // Arrange - Create a week that actually spans across years
            // Let's test the behavior when we have a week that crosses year boundaries
            // Since January 1, 2024 was a Monday, any day in that week won't span years
            // We need a date where the Monday is in December 2023
            LocalDate anchorDate = LocalDate.of(2023, 12, 31); // Sunday, Dec 31, 2023
            
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);
            
            // Calculate what the service will actually compute
            LocalDate weekStartDate = anchorDate.with(DayOfWeek.MONDAY); // Should be December 25, 2023
            LocalDate weekEndDate = weekStartDate.plusDays(6); // Should be December 31, 2023
            
            // This still won't span years! The issue is that .with(DayOfWeek.MONDAY) gives us the Monday
            // of the SAME week, not necessarily a previous week
            // Let's manually construct a scenario that works
            weekStartDate = LocalDate.of(2023, 12, 25); // Monday, December 25, 2023
            weekEndDate = LocalDate.of(2023, 12, 31);   // Sunday, December 31, 2023
            
            // Still doesn't span years. Let's create a week that actually does
            // Week of Dec 26, 2022 to Jan 1, 2023 (Monday to Sunday)
            anchorDate = LocalDate.of(2022, 12, 28); // Wednesday
            weekStartDate = LocalDate.of(2022, 12, 26); // Manually set Monday
            weekEndDate = LocalDate.of(2023, 1, 1);     // Manually set Sunday (next year!)
            
            ZonedDateTime weekStartTime = weekStartDate.atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC);
            ZonedDateTime weekEndTime = weekEndDate.plusDays(1).atStartOfDay(userZoneId).withZoneSameInstant(ZoneOffset.UTC).minusNanos(1);
            
            when(eventBO.getConfirmedEventsForUserInRange(user.getId(), weekStartTime, weekEndTime))
                    .thenReturn(Collections.emptyList());
            
            List<DayViewDTO> emptyDays = Collections.nCopies(7, new DayViewDTO(weekStartDate, Collections.emptyList()));
            WeekViewDTO expectedWeekView = new WeekViewDTO(emptyDays);
            when(eventBO.generateWeekViewData(eq(user.getId()), eq(anchorDate), eq(userZoneId), any(), any()))
                    .thenReturn(expectedWeekView);

            // Act
            WeekViewDTO result = eventService.generateWeekView(anchorDate);

            // Assert
            assertNotNull(result);
            // Verify correct week boundaries were calculated - we manually control the expected calls
            verify(eventBO).getConfirmedEventsForUserInRange(user.getId(), weekStartTime, weekEndTime);
            
            // Verify this actually spans years
            assertTrue(weekStartDate.getYear() != weekEndDate.getYear(), 
                    "Week should span across different years: start=" + weekStartDate + ", end=" + weekEndDate);
        }

    }

    @Nested
    class CreateEventTests {

        @Test
        void shouldCreateConfirmedEventWithLabel() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            // Create a valid EventCreateDTO for a confirmed event
            EventCreateDTO dto = new EventCreateDTO(
                    "Workout",
                    getValidEventStartFuture(fixedClock),
                    getValidEventEndFuture(fixedClock),
                    "Morning workout",         // description
                    TestConstants.VALID_LABEL_ID,
                    false                      // isDraft
            );

            // Mock labelService to return a label entity for the given labelId
            Label label = TestUtils.createValidLabel(user, "test");
            when(labelService.getLabelEntityById(dto.labelId())).thenReturn(label);

            // Mock eventBO.createEvent to return a created event entity
            Event createdEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            createdEvent.setLabel(label);

            when(eventBO.createEvent(any(Event.class))).thenReturn(createdEvent);

            // Mock eventResponseDTOFactory to return the response DTO from createdEvent
            EventResponseDTO expectedResponse = createEventResponseDTO(createdEvent);
            when(eventResponseDTOFactory.createFromEvent(createdEvent)).thenReturn(expectedResponse);

            // Act
            EventResponseDTO response = eventService.createEvent(dto);

            // Assert
            assertNotNull(response, "Response should not be null.");
            assertEquals(expectedResponse, response, "Response should match expected EventResponseDTO.");

            // Verify interactions
            verify(labelService).getLabelEntityById(dto.labelId());
            verify(eventBO).createEvent(any(Event.class));
            verify(eventResponseDTOFactory).createFromEvent(createdEvent);
        }

        @Test
        void shouldCreateDraftEventWithLabel() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            // Create a valid EventCreateDTO for a draft event
            EventCreateDTO dto = new EventCreateDTO(
                    "Draft Meeting",
                    getValidEventStartFuture(fixedClock),
                    getValidEventEndFuture(fixedClock),
                    "Planning draft",          // description
                    TestConstants.VALID_LABEL_ID,
                    true                      // isDraft
            );

            // Mock labelService to return a label entity for the given labelId
            Label label = TestUtils.createValidLabel(user, "test");
            when(labelService.getLabelEntityById(dto.labelId())).thenReturn(label);

            // Mock eventBO.createEvent to return a created draft event entity
            Event draftEvent = TestUtils.createValidFullDraftEvent(user, fixedClock);
            draftEvent.setLabel(label);

            when(eventBO.createEvent(any(Event.class))).thenReturn(draftEvent);

            // Mock eventResponseDTOFactory to return the response DTO from draftEvent
            EventResponseDTO expectedResponse = createEventResponseDTO(draftEvent);
            when(eventResponseDTOFactory.createFromEvent(draftEvent)).thenReturn(expectedResponse);

            // Act
            EventResponseDTO response = eventService.createEvent(dto);

            // Assert
            assertNotNull(response, "Response should not be null.");
            assertEquals(expectedResponse, response, "Response should match expected EventResponseDTO.");

            // Verify interactions
            verify(labelService).getLabelEntityById(dto.labelId());
            verify(eventBO).createEvent(any(Event.class));
            verify(eventResponseDTOFactory).createFromEvent(draftEvent);
        }

        @Test
        void shouldUseUnlabeledIfLabelIdIsNull() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            // Create an EventCreateDTO with null labelId
            EventCreateDTO dto = new EventCreateDTO(
                    "No Label Event",
                    getValidEventStartFuture(fixedClock),
                    getValidEventEndFuture(fixedClock),
                    "No label description",   // description
                    null,                     // labelId is null
                    false                     // isDraft = false
            );

            // The user's unlabeled label
            Label unlabeled = user.getUnlabeled();

            // Mock eventBO.createEvent to return a created event with unlabeled
            Event createdEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            createdEvent.setLabel(unlabeled);

            when(eventBO.createEvent(any(Event.class))).thenReturn(createdEvent);

            // Mock eventResponseDTOFactory to convert createdEvent to response DTO
            EventResponseDTO expectedResponse = createEventResponseDTO(createdEvent);
            when(eventResponseDTOFactory.createFromEvent(createdEvent)).thenReturn(expectedResponse);

            // Act
            EventResponseDTO response = eventService.createEvent(dto);

            // Assert
            assertNotNull(response, "Response should not be null.");
            assertEquals(expectedResponse, response, "Response should match expected EventResponseDTO.");

            // Verify that labelService was NOT called (since labelId is null)
            verifyNoInteractions(labelService);

            // Verify other interactions
            verify(eventBO).createEvent(any(Event.class));
            verify(eventResponseDTOFactory).createFromEvent(createdEvent);
        }

    }

    @Nested
    class CreateImpromptuEventTests {

        @Test
        void shouldCreateImpromptuEventSuccessfully() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            Clock userClock = fixedClock;
            when(clockProvider.getClockForUser(user)).thenReturn(userClock);

            ZonedDateTime now = ZonedDateTime.now(userClock);

            // Prepare the created impromptu event
            Event createdEvent = Event.createImpromptuEvent(now, user);
            createdEvent.setLabel(user.getUnlabeled());
            // impromptu events are created as unconfirmed by default

            when(eventBO.createEvent(any(Event.class))).thenReturn(createdEvent);

            EventResponseDTO expectedResponse = createEventResponseDTO(createdEvent);
            when(eventResponseDTOFactory.createFromEvent(createdEvent)).thenReturn(expectedResponse);

            // Act
            EventResponseDTO response = eventService.createImpromptuEvent();

            // Assert
            assertNotNull(response, "Response should not be null.");
            assertEquals(expectedResponse, response, "Response should match expected EventResponseDTO.");

            // Verify event creation using createImpromptuEvent
            verify(eventBO).createEvent(any(Event.class));

            // Verify DTO conversion
            verify(eventResponseDTOFactory).createFromEvent(createdEvent);
        }

        @Test
        void shouldAutomaticallyPinNewImpromptuEvent() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            ZonedDateTime now = ZonedDateTime.now(fixedClock);
            Event createdEvent = Event.createImpromptuEvent(now, user);
            createdEvent.setLabel(user.getUnlabeled());
            // impromptu events are created as unconfirmed by default

            when(eventBO.createEvent(any(Event.class))).thenReturn(createdEvent);
            when(eventResponseDTOFactory.createFromEvent(createdEvent)).thenReturn(createEventResponseDTO(createdEvent));

            // Act
            eventService.createImpromptuEvent();

            // Assert
            verify(userBO).updateUser(user); // User should be updated after setting pinned event
        }

        @Test
        void shouldReplacePreviousPinnedEvent() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            ZonedDateTime now = ZonedDateTime.now(fixedClock);
            
            // Create a previously pinned event
            Event previousEvent = Event.createImpromptuEvent(now.minusMinutes(30), user);
            user.setPinnedImpromptuEvent(previousEvent);
            
            Event newEvent = Event.createImpromptuEvent(now, user);
            newEvent.setLabel(user.getUnlabeled());
            // impromptu events are created as unconfirmed by default

            when(eventBO.createEvent(any(Event.class))).thenReturn(newEvent);
            when(eventResponseDTOFactory.createFromEvent(newEvent)).thenReturn(createEventResponseDTO(newEvent));

            // Act
            eventService.createImpromptuEvent();

            // Assert - user should be updated with new pinned event
            verify(userBO).updateUser(user);
        }

    }

    @Nested
    class ConfirmEventTests {

        @Test
        void shouldConfirmUnconfirmedEvent() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            Long eventId = TestConstants.EVENT_ID;

            Event unconfirmedEvent = TestUtils.createValidFullDraftEvent(user, fixedClock);
            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(unconfirmedEvent));

            // Ownership validation
            doNothing().when(ownershipValidator).validateEventOwnership(user.getId(), unconfirmedEvent);

            // Mock confirmation
            Event confirmedEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            when(eventBO.confirmEvent(unconfirmedEvent)).thenReturn(confirmedEvent);

            // Mock DTO conversion
            EventResponseDTO expectedResponse = createEventResponseDTO(confirmedEvent);
            when(eventResponseDTOFactory.createFromEvent(confirmedEvent)).thenReturn(expectedResponse);

            // Act
            EventResponseDTO response = eventService.confirmEvent(eventId);

            // Assert
            assertNotNull(response, "Response should not be null.");
            assertEquals(expectedResponse, response, "Response should match expected DTO.");

            // Verify interactions
            verify(eventBO).getEventById(eventId);
            verify(ownershipValidator).validateEventOwnership(user.getId(), unconfirmedEvent);
            verify(eventBO).confirmEvent(unconfirmedEvent);
            verify(eventResponseDTOFactory).createFromEvent(confirmedEvent);
        }

        @Test
        void shouldAutoUnpinImpromptuEventOnConfirm() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            Long eventId = TestConstants.EVENT_ID;

            // Create an impromptu event that's pinned
            Event impromptuEvent = TestUtils.createValidImpromptuEventWithId(eventId, user, fixedClock);
            impromptuEvent.setUnconfirmed(true);
            user.setPinnedImpromptuEvent(impromptuEvent);

            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(impromptuEvent));
            doNothing().when(ownershipValidator).validateEventOwnership(user.getId(), impromptuEvent);

            Event confirmedEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            when(eventBO.confirmEvent(impromptuEvent)).thenReturn(confirmedEvent);
            when(eventResponseDTOFactory.createFromEvent(confirmedEvent)).thenReturn(createEventResponseDTO(confirmedEvent));

            // Act
            eventService.confirmEvent(eventId);

            // Assert - user should be updated after unpinning
            verify(userBO).updateUser(user);
        }

        @Test
        void shouldNotAutoUnpinIfEventNotImpromptu() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            Long eventId = TestConstants.EVENT_ID;

            // Create a regular draft event (not impromptu)
            Event regularEvent = TestUtils.createValidFullDraftEvent(user, fixedClock);

            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(regularEvent));
            doNothing().when(ownershipValidator).validateEventOwnership(user.getId(), regularEvent);

            Event confirmedEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            when(eventBO.confirmEvent(regularEvent)).thenReturn(confirmedEvent);
            when(eventResponseDTOFactory.createFromEvent(confirmedEvent)).thenReturn(createEventResponseDTO(confirmedEvent));

            // Act
            eventService.confirmEvent(eventId);

            // Assert - user should not be updated since no unpinning needed
            verify(userBO, never()).updateUser(any(User.class));
        }

        @Test
        void shouldThrowIfEventNotFound() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            Long eventId = TestConstants.EVENT_ID;

            when(eventBO.getEventById(eventId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(EventNotFoundException.class, () -> eventService.confirmEvent(eventId));

            // Verify interactions
            verify(eventBO).getEventById(eventId);
            verifyNoInteractions(ownershipValidator);
            verifyNoMoreInteractions(eventBO);
            verifyNoInteractions(eventResponseDTOFactory);
        }

        @Test
        void shouldThrowIfEventAlreadyConfirmed() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            Long eventId = TestConstants.EVENT_ID;

            // Create a confirmed event
            Event confirmedEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(confirmedEvent));

            // Ownership validation
            doNothing().when(ownershipValidator).validateEventOwnership(user.getId(), confirmedEvent);

            // Act & Assert
            assertThrows(EventAlreadyConfirmedException.class, () -> eventService.confirmEvent(eventId));

            // Verify interactions
            verify(eventBO).getEventById(eventId);
            verify(ownershipValidator).validateEventOwnership(user.getId(), confirmedEvent);
            verifyNoMoreInteractions(eventBO);
            verifyNoInteractions(eventResponseDTOFactory);
        }

    }

    @Nested
    class ConfirmAndCompleteEventTests {

        @Test
        void shouldConfirmAndCompleteEventSuccessfully() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            Long eventId = TestConstants.EVENT_ID;

            // Create an unconfirmed event with past end time
            Event unconfirmedEvent = TestUtils.createValidFullDraftEvent(user, fixedClock);
            ZonedDateTime pastEndTime = ZonedDateTime.now(fixedClock).minusHours(1);
            unconfirmedEvent.setEndTime(pastEndTime);

            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(unconfirmedEvent));

            // Ownership validation
            doNothing().when(ownershipValidator).validateEventOwnership(user.getId(), unconfirmedEvent);

            // Mock confirmation returns the same event (since confirmEvent mutates or returns a new event)
            when(eventBO.confirmEvent(unconfirmedEvent)).thenReturn(unconfirmedEvent);

            // Mock clockProvider
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            // Mock eventPatchHandler to simulate applying completion patch successfully
            when(eventPatchHandler.applyPatch(any(Event.class), any(EventUpdateDTO.class))).thenReturn(true);

            // Mock eventBO.updateEvent to return the updated event
            when(eventBO.updateEvent(any(), eq(unconfirmedEvent))).thenReturn(unconfirmedEvent);

            // Mock eventResponseDTOFactory
            EventResponseDTO expectedResponse = createEventResponseDTO(unconfirmedEvent);
            when(eventResponseDTOFactory.createFromEvent(unconfirmedEvent)).thenReturn(expectedResponse);

            // Act
            EventResponseDTO response = eventService.confirmAndCompleteEvent(eventId);

            // Assert
            assertNotNull(response, "Response should not be null.");
            assertEquals(expectedResponse, response, "Response should match expected DTO.");

            // Verify interactions
            verify(eventBO, times(2)).getEventById(eventId); // once in confirmAndCompleteEvent, once in updateEvent
            verify(ownershipValidator, times(2)).validateEventOwnership(eq(user.getId()), any(Event.class));
        }

        @Test
        void shouldAutoUnpinImpromptuEventOnConfirmAndComplete() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            Long eventId = TestConstants.EVENT_ID;

            // Create an impromptu event that's pinned
            Event impromptuEvent = TestUtils.createValidImpromptuEventWithId(eventId, user, fixedClock);
            impromptuEvent.setUnconfirmed(true);
            impromptuEvent.setStartTime(ZonedDateTime.now(fixedClock).minusHours(2));
            impromptuEvent.setEndTime(ZonedDateTime.now(fixedClock).minusHours(1)); // past end time
            user.setPinnedImpromptuEvent(impromptuEvent);

            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(impromptuEvent));
            doNothing().when(ownershipValidator).validateEventOwnership(user.getId(), impromptuEvent);
            when(eventBO.confirmEvent(impromptuEvent)).thenReturn(impromptuEvent);
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);
            when(eventPatchHandler.applyPatch(any(Event.class), any(EventUpdateDTO.class))).thenReturn(true);
            when(eventBO.updateEvent(any(), eq(impromptuEvent))).thenReturn(impromptuEvent);
            when(eventResponseDTOFactory.createFromEvent(impromptuEvent)).thenReturn(createEventResponseDTO(impromptuEvent));

            // Act
            eventService.confirmAndCompleteEvent(eventId);

            // Assert - user should be updated after unpinning
            verify(userBO).updateUser(user); // Only once for unpin operation

            verify(eventBO).confirmEvent(impromptuEvent);
            verify(eventPatchHandler).applyPatch(any(Event.class), any(EventUpdateDTO.class));
            verify(eventBO).updateEvent(any(), eq(impromptuEvent));
            verify(eventResponseDTOFactory).createFromEvent(impromptuEvent);
        }

        @Test
        void shouldThrowIfEventNotFound() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            Long eventId = TestConstants.EVENT_ID;

            when(eventBO.getEventById(eventId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(EventNotFoundException.class, () -> eventService.confirmAndCompleteEvent(eventId));

            // Verify interactions
            verify(eventBO).getEventById(eventId);
            verifyNoInteractions(ownershipValidator);
            verifyNoMoreInteractions(eventBO);
            verifyNoInteractions(eventPatchHandler);
            verifyNoInteractions(eventResponseDTOFactory);
        }

        @Test
        void shouldThrowIfEventAlreadyConfirmed() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            Long eventId = TestConstants.EVENT_ID;

            // Create a confirmed event
            Event confirmedEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(confirmedEvent));

            // Ownership validation
            doNothing().when(ownershipValidator).validateEventOwnership(user.getId(), confirmedEvent);

            // Act & Assert
            assertThrows(EventAlreadyConfirmedException.class, () -> eventService.confirmAndCompleteEvent(eventId));

            // Verify interactions
            verify(eventBO).getEventById(eventId);
            verify(ownershipValidator).validateEventOwnership(user.getId(), confirmedEvent);
            verifyNoMoreInteractions(eventBO);
            verifyNoInteractions(eventPatchHandler);
            verifyNoInteractions(eventResponseDTOFactory);
        }

        @Test
        void shouldThrowInvalidTimeExceptionIfEndTimeIsAfterNow() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            Long eventId = TestConstants.EVENT_ID;

            // Create an unconfirmed event with future end time
            Event unconfirmedEvent = TestUtils.createValidFullDraftEvent(user, fixedClock);
            ZonedDateTime futureEndTime = ZonedDateTime.now(fixedClock).plusHours(2);
            unconfirmedEvent.setEndTime(futureEndTime);

            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(unconfirmedEvent));

            // Ownership validation
            doNothing().when(ownershipValidator).validateEventOwnership(user.getId(), unconfirmedEvent);

            // Mock confirmation returns the same event
            when(eventBO.confirmEvent(unconfirmedEvent)).thenReturn(unconfirmedEvent);

            // Mock clockProvider
            when(clockProvider.getClockForUser(user)).thenReturn(fixedClock);

            // Act & Assert
            InvalidTimeException ex = assertThrows(InvalidTimeException.class,
                    () -> eventService.confirmAndCompleteEvent(eventId));

            assertEquals(ErrorCode.INVALID_COMPLETION_STATUS, ex.getErrorCode(), "Exception should have correct error code.");

            // Verify interactions
            verify(eventBO).getEventById(eventId);
            verify(ownershipValidator).validateEventOwnership(user.getId(), unconfirmedEvent);
            verify(eventBO).confirmEvent(unconfirmedEvent);
            verify(clockProvider).getClockForUser(user);

            // Should NOT proceed to applyPatch or updateEvent
            verifyNoInteractions(eventPatchHandler);
            verifyNoMoreInteractions(eventBO);
            verifyNoInteractions(eventResponseDTOFactory);
        }

    }

    @Nested
    class UpdateEventTests{

        @Test
        void shouldUpdateEventSuccessfullyWhenPatchChangesEvent() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            Long eventId = TestConstants.EVENT_ID;

            Event existingEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(existingEvent));

            // Ownership validation
            doNothing().when(ownershipValidator).validateEventOwnership(user.getId(), existingEvent);

            // Mock applyPatch to indicate a change occurred
            EventUpdateDTO dto = new EventUpdateDTO(
                    Optional.of("Updated Name"),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    null
            );
            when(eventPatchHandler.applyPatch(existingEvent, dto)).thenReturn(true);

            // Mock eventBO.updateEvent
            when(eventBO.updateEvent(any(), eq(existingEvent))).thenReturn(existingEvent);

            // Mock DTO factory
            EventResponseDTO expectedResponse = createEventResponseDTO(existingEvent);
            when(eventResponseDTOFactory.createFromEvent(existingEvent)).thenReturn(expectedResponse);

            // Act
            EventResponseDTO response = eventService.updateEvent(eventId, dto);

            // Assert
            assertNotNull(response, "Response should not be null.");
            assertEquals(expectedResponse, response, "Response should match expected DTO.");

            // Verify interactions
            verify(eventBO).getEventById(eventId);
            verify(ownershipValidator).validateEventOwnership(user.getId(), existingEvent);
            verify(eventPatchHandler).applyPatch(existingEvent, dto);
            verify(eventBO).updateEvent(any(), eq(existingEvent));
            verify(eventResponseDTOFactory).createFromEvent(existingEvent);
        }

        @Test
        void shouldReturnEventUnchangedWhenPatchDoesNotChangeEvent() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            Long eventId = TestConstants.EVENT_ID;

            Event existingEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(existingEvent));

            // Ownership validation
            doNothing().when(ownershipValidator).validateEventOwnership(user.getId(), existingEvent);

            // DTO with all fields null (ignored)
            EventUpdateDTO dto = new EventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Mock applyPatch to indicate no change
            when(eventPatchHandler.applyPatch(existingEvent, dto)).thenReturn(false);

            // Mock DTO factory
            EventResponseDTO expectedResponse = createEventResponseDTO(existingEvent);
            when(eventResponseDTOFactory.createFromEvent(existingEvent)).thenReturn(expectedResponse);

            // Act
            EventResponseDTO response = eventService.updateEvent(eventId, dto);

            // Assert
            assertNotNull(response, "Response should not be null.");
            assertEquals(expectedResponse, response, "Response should match expected DTO.");

            // Verify interactions
            verify(eventBO).getEventById(eventId);
            verify(ownershipValidator).validateEventOwnership(user.getId(), existingEvent);
            verify(eventPatchHandler).applyPatch(existingEvent, dto);
            verify(eventResponseDTOFactory).createFromEvent(existingEvent);
            verifyNoMoreInteractions(eventBO); // eventBO.updateEvent should not be called
        }

        @Test
        void shouldThrowIfEventNotFound() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            Long eventId = TestConstants.EVENT_ID;

            when(eventBO.getEventById(eventId)).thenReturn(Optional.empty());

            EventUpdateDTO dto = new EventUpdateDTO(
                    Optional.of("Updated Name"),
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Act & Assert
            assertThrows(EventNotFoundException.class, () -> eventService.updateEvent(eventId, dto));

            // Verify interactions
            verify(eventBO).getEventById(eventId);
            verifyNoInteractions(ownershipValidator);
            verifyNoInteractions(eventPatchHandler);
            verifyNoInteractions(eventResponseDTOFactory);
        }

        @Test
        void shouldThrowIfUserDoesNotOwnEvent() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            Long eventId = TestConstants.EVENT_ID;

            Event existingEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(existingEvent));

            // Ownership validation throws exception
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, user.getId()))
                    .when(ownershipValidator).validateEventOwnership(user.getId(), existingEvent);

            EventUpdateDTO dto = new EventUpdateDTO(
                    Optional.of("Updated Name"),
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Act & Assert
            assertThrows(UserOwnershipException.class, () -> eventService.updateEvent(eventId, dto));

            // Verify interactions
            verify(eventBO).getEventById(eventId);
            verify(ownershipValidator).validateEventOwnership(user.getId(), existingEvent);
            verifyNoInteractions(eventPatchHandler);
            verifyNoInteractions(eventResponseDTOFactory);
        }

    }

    @Nested
    class DeleteEventTests {

        @Test
        void shouldDeleteEventSuccessfully() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            Long eventId = TestConstants.EVENT_ID;

            Event existingEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(existingEvent));

            // Ownership validation
            doNothing().when(ownershipValidator).validateEventOwnership(user.getId(), existingEvent);

            // Mock delete
            doNothing().when(eventBO).deleteEvent(eventId);

            // Act
            eventService.deleteEvent(eventId);

            // Verify interactions
            verify(eventBO).getEventById(eventId);
            verify(ownershipValidator).validateEventOwnership(user.getId(), existingEvent);
            verify(eventBO).deleteEvent(eventId);
        }

        @Test
        void shouldAutoUnpinImpromptuEventOnDelete() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            Long eventId = TestConstants.EVENT_ID;

            // Create an impromptu event that's pinned
            Event impromptuEvent = TestUtils.createValidImpromptuEventWithId(eventId, user, fixedClock);
            user.setPinnedImpromptuEvent(impromptuEvent);

            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(impromptuEvent));
            doNothing().when(ownershipValidator).validateEventOwnership(user.getId(), impromptuEvent);
            doNothing().when(eventBO).deleteEvent(eventId);

            // Act
            eventService.deleteEvent(eventId);

            // Assert - user should be updated before deletion
            verify(userBO).updateUser(user);
            verify(eventBO).deleteEvent(eventId);
        }

        @Test
        void shouldNotAutoUnpinIfEventNotImpromptuOnDelete() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            Long eventId = TestConstants.EVENT_ID;

            // Create a regular event (not impromptu)
            Event regularEvent = TestUtils.createValidScheduledEvent(user, fixedClock);

            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(regularEvent));
            doNothing().when(ownershipValidator).validateEventOwnership(user.getId(), regularEvent);
            doNothing().when(eventBO).deleteEvent(eventId);

            // Act
            eventService.deleteEvent(eventId);

            // Assert - user should not be updated since no unpinning needed
            verify(userBO, never()).updateUser(any(User.class));
            verify(eventBO).deleteEvent(eventId);
        }

        @Test
        void shouldThrowIfEventNotFound() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            Long eventId = TestConstants.EVENT_ID;

            when(eventBO.getEventById(eventId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(EventNotFoundException.class, () -> eventService.deleteEvent(eventId));

            // Verify interactions
            verify(eventBO).getEventById(eventId);
            verifyNoInteractions(ownershipValidator);
            verify(eventBO, never()).deleteEvent(anyLong());
        }

        @Test
        void shouldThrowIfUserDoesNotOwnEvent() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            Long eventId = TestConstants.EVENT_ID;

            Event existingEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            when(eventBO.getEventById(eventId)).thenReturn(Optional.of(existingEvent));

            // Ownership validation fails
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, user.getId()))
                    .when(ownershipValidator).validateEventOwnership(user.getId(), existingEvent);

            // Act & Assert
            assertThrows(UserOwnershipException.class, () -> eventService.deleteEvent(eventId));

            // Verify interactions
            verify(eventBO).getEventById(eventId);
            verify(ownershipValidator).validateEventOwnership(user.getId(), existingEvent);
            verify(eventBO, never()).deleteEvent(anyLong());
        }

    }

    @Nested
    class DeleteUnconfirmedEventsForCurrentUserTests {

        @Test
        void deletesAllDraftEventsForCurrentUser() {
            // Arrange
            User viewer = createValidUserEntityWithId();
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);

            // Act
            eventService.deleteUnconfirmedEventsForCurrentUser();

            // Assert
            verify(authenticatedUserProvider).getCurrentUser();
            verify(eventBO).deleteAllUnconfirmedEventsByUser(viewer.getId());
            verifyNoMoreInteractions(eventBO);
        }

        @Test
        void doesNothingWhenNoDraftsExist() {
            // Arrange
            User viewer = createValidUserEntityWithId();
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);

            // Act
            eventService.deleteUnconfirmedEventsForCurrentUser();

            // Assert
            verify(authenticatedUserProvider).getCurrentUser();
            verify(eventBO).deleteAllUnconfirmedEventsByUser(viewer.getId());
            verifyNoMoreInteractions(eventBO);
        }

    }

    @Nested
    class UpdateFutureEventsFromRecurringEventTests {

        @Test
        void shouldUpdateFutureEventsSuccessfully() {
            // Arrange
            RecurringEvent recurringEvent = mock(RecurringEvent.class);
            Set<String> changedFields = Set.of("name", "startTime");
            ZoneId userZoneId = ZoneId.of("America/New_York");
            int expectedUpdateCount = 5;

            when(recurringEvent.getId()).thenReturn(1L);
            when(eventBO.updateFutureEventsFromRecurringEvent(recurringEvent, changedFields, userZoneId))
                    .thenReturn(expectedUpdateCount);

            // Act
            int actualUpdateCount = eventService.updateFutureEventsFromRecurringEvent(
                    recurringEvent, changedFields, userZoneId);

            // Assert
            assertEquals(expectedUpdateCount, actualUpdateCount);
            verify(eventBO).updateFutureEventsFromRecurringEvent(recurringEvent, changedFields, userZoneId);
            verifyNoMoreInteractions(eventBO);
        }

        @Test
        void shouldReturnZeroWhenNoFutureEvents() {
            // Arrange
            RecurringEvent recurringEvent = mock(RecurringEvent.class);
            Set<String> changedFields = Set.of("description");
            ZoneId userZoneId = ZoneId.of("UTC");

            when(recurringEvent.getId()).thenReturn(2L);
            when(eventBO.updateFutureEventsFromRecurringEvent(recurringEvent, changedFields, userZoneId))
                    .thenReturn(0);

            // Act
            int actualUpdateCount = eventService.updateFutureEventsFromRecurringEvent(
                    recurringEvent, changedFields, userZoneId);

            // Assert
            assertEquals(0, actualUpdateCount);
            verify(eventBO).updateFutureEventsFromRecurringEvent(recurringEvent, changedFields, userZoneId);
            verifyNoMoreInteractions(eventBO);
        }

        @Test
        void shouldHandleDifferentChangedFields() {
            // Arrange
            RecurringEvent recurringEvent = mock(RecurringEvent.class);
            Set<String> changedFields = Set.of("name", "endTime", "label");
            ZoneId userZoneId = ZoneId.of("Europe/London");
            int expectedUpdateCount = 3;

            when(recurringEvent.getId()).thenReturn(3L);
            when(eventBO.updateFutureEventsFromRecurringEvent(recurringEvent, changedFields, userZoneId))
                    .thenReturn(expectedUpdateCount);

            // Act
            int actualUpdateCount = eventService.updateFutureEventsFromRecurringEvent(
                    recurringEvent, changedFields, userZoneId);

            // Assert
            assertEquals(expectedUpdateCount, actualUpdateCount);
            verify(eventBO).updateFutureEventsFromRecurringEvent(recurringEvent, changedFields, userZoneId);
            verifyNoMoreInteractions(eventBO);
        }

        @Test
        void shouldRespectUserTimezone() {
            // Arrange
            RecurringEvent recurringEvent = mock(RecurringEvent.class);
            Set<String> changedFields = Set.of("startTime");
            ZoneId userZoneId = ZoneId.of("Asia/Tokyo");
            int expectedUpdateCount = 2;

            when(recurringEvent.getId()).thenReturn(4L);
            when(eventBO.updateFutureEventsFromRecurringEvent(recurringEvent, changedFields, userZoneId))
                    .thenReturn(expectedUpdateCount);

            // Act
            int actualUpdateCount = eventService.updateFutureEventsFromRecurringEvent(
                    recurringEvent, changedFields, userZoneId);

            // Assert
            assertEquals(expectedUpdateCount, actualUpdateCount);
            verify(eventBO).updateFutureEventsFromRecurringEvent(recurringEvent, changedFields, userZoneId);
            verifyNoMoreInteractions(eventBO);
        }

        @Test
        void shouldHandleEmptyChangedFields() {
            // Arrange
            RecurringEvent recurringEvent = mock(RecurringEvent.class);
            Set<String> changedFields = Set.of();
            ZoneId userZoneId = ZoneId.of("UTC");

            when(recurringEvent.getId()).thenReturn(5L);
            when(eventBO.updateFutureEventsFromRecurringEvent(recurringEvent, changedFields, userZoneId))
                    .thenReturn(0);

            // Act
            int actualUpdateCount = eventService.updateFutureEventsFromRecurringEvent(
                    recurringEvent, changedFields, userZoneId);

            // Assert
            assertEquals(0, actualUpdateCount);
            verify(eventBO).updateFutureEventsFromRecurringEvent(recurringEvent, changedFields, userZoneId);
            verifyNoMoreInteractions(eventBO);
        }

        @Test
        void shouldHandleLargeUpdateCount() {
            // Arrange
            RecurringEvent recurringEvent = mock(RecurringEvent.class);
            Set<String> changedFields = Set.of("name", "startTime", "endTime");
            ZoneId userZoneId = ZoneId.of("America/Los_Angeles");
            int expectedUpdateCount = 100; // Large recurring event series

            when(recurringEvent.getId()).thenReturn(6L);
            when(eventBO.updateFutureEventsFromRecurringEvent(recurringEvent, changedFields, userZoneId))
                    .thenReturn(expectedUpdateCount);

            // Act
            int actualUpdateCount = eventService.updateFutureEventsFromRecurringEvent(
                    recurringEvent, changedFields, userZoneId);

            // Assert
            assertEquals(expectedUpdateCount, actualUpdateCount);
            verify(eventBO).updateFutureEventsFromRecurringEvent(recurringEvent, changedFields, userZoneId);
            verifyNoMoreInteractions(eventBO);
        }

    }

    @Nested
    class GetConfirmedEventsForCurrentUserFilterTests {

        @Test
        void shouldApplyAllTimeFilter() {
            // Arrange
            User viewer = createValidUserEntityWithId();
            EventFilterDTO filter = new EventFilterDTO(null, TimeFilter.ALL, null, null, false, false);
            PagedList<Event> mockEvents = mock(PagedList.class);
            when(mockEvents.stream()).thenReturn(Stream.empty());
            when(mockEvents.getTotalSize()).thenReturn(0L);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(clockProvider.getClockForUser(viewer)).thenReturn(fixedClock);
            when(eventDAO.findConfirmedEvents(eq(viewer.getId()), any(EventFilterDTO.class), eq(0), eq(10)))
                    .thenReturn(mockEvents);

            // Act
            eventService.getConfirmedEventsForCurrentUser(filter, 0, 10);

            // Assert - Verify the sanitized filter has FAR_PAST and now (reference time for incomplete filtering)
            verify(eventDAO).findConfirmedEvents(eq(viewer.getId()), argThat(sanitizedFilter -> 
                sanitizedFilter.start().equals(TimeUtils.FAR_PAST) &&
                sanitizedFilter.end().equals(nowInUserZone) &&
                sanitizedFilter.timeFilter() == TimeFilter.ALL
            ), eq(0), eq(10));
        }

        @Test
        void shouldApplyPastOnlyFilter() {
            // Arrange
            User viewer = createValidUserEntityWithId();
            EventFilterDTO filter = new EventFilterDTO(null, TimeFilter.PAST_ONLY, null, null, false, false);
            PagedList<Event> mockEvents = mock(PagedList.class);
            when(mockEvents.stream()).thenReturn(Stream.empty());
            when(mockEvents.getTotalSize()).thenReturn(0L);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(clockProvider.getClockForUser(viewer)).thenReturn(fixedClock);
            when(eventDAO.findConfirmedEvents(eq(viewer.getId()), any(EventFilterDTO.class), eq(0), eq(10)))
                    .thenReturn(mockEvents);

            // Act
            eventService.getConfirmedEventsForCurrentUser(filter, 0, 10);

            // Assert - Verify the sanitized filter has FAR_PAST to now
            verify(eventDAO).findConfirmedEvents(eq(viewer.getId()), argThat(sanitizedFilter -> 
                sanitizedFilter.start().equals(TimeUtils.FAR_PAST) &&
                sanitizedFilter.end().equals(nowInUserZone) &&
                sanitizedFilter.timeFilter() == TimeFilter.ALL
            ), eq(0), eq(10));
        }

        @Test
        void shouldApplyFutureOnlyFilter() {
            // Arrange
            User viewer = createValidUserEntityWithId();
            EventFilterDTO filter = new EventFilterDTO(null, TimeFilter.FUTURE_ONLY, null, null, false, false);
            PagedList<Event> mockEvents = mock(PagedList.class);
            when(mockEvents.stream()).thenReturn(Stream.empty());
            when(mockEvents.getTotalSize()).thenReturn(0L);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(clockProvider.getClockForUser(viewer)).thenReturn(fixedClock);
            when(eventDAO.findConfirmedEvents(eq(viewer.getId()), any(EventFilterDTO.class), eq(0), eq(10)))
                    .thenReturn(mockEvents);

            // Act
            eventService.getConfirmedEventsForCurrentUser(filter, 0, 10);

            // Assert - Verify the sanitized filter has now to now (reference time for incomplete filtering)
            verify(eventDAO).findConfirmedEvents(eq(viewer.getId()), argThat(sanitizedFilter -> 
                sanitizedFilter.start().equals(nowInUserZone) &&
                sanitizedFilter.end().equals(nowInUserZone) &&
                sanitizedFilter.timeFilter() == TimeFilter.ALL
            ), eq(0), eq(10));
        }

        @Test
        void shouldApplyCustomFilterWithBothDates() {
            // Arrange
            User viewer = createValidUserEntityWithId();
            ZonedDateTime customStart = getValidEventStartFuture(fixedClock);
            ZonedDateTime customEnd = getValidEventEndFuture(fixedClock);
            EventFilterDTO filter = new EventFilterDTO(null, TimeFilter.CUSTOM, customStart, customEnd, false, false);
            PagedList<Event> mockEvents = mock(PagedList.class);
            when(mockEvents.stream()).thenReturn(Stream.empty());
            when(mockEvents.getTotalSize()).thenReturn(0L);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(clockProvider.getClockForUser(viewer)).thenReturn(fixedClock);
            when(eventDAO.findConfirmedEvents(eq(viewer.getId()), any(EventFilterDTO.class), eq(0), eq(10)))
                    .thenReturn(mockEvents);

            // Act
            eventService.getConfirmedEventsForCurrentUser(filter, 0, 10);

            // Assert - Verify the sanitized filter preserves custom start but uses now as reference time
            verify(eventDAO).findConfirmedEvents(eq(viewer.getId()), argThat(sanitizedFilter -> 
                sanitizedFilter.start().equals(customStart) &&
                sanitizedFilter.end().equals(nowInUserZone) &&
                sanitizedFilter.timeFilter() == TimeFilter.ALL
            ), eq(0), eq(10));
        }

        @Test
        void shouldApplyCustomFilterWithNullStart() {
            // Arrange
            User viewer = createValidUserEntityWithId();
            ZonedDateTime customEnd = getValidEventEndFuture(fixedClock);
            EventFilterDTO filter = new EventFilterDTO(null, TimeFilter.CUSTOM, null, customEnd, false, false);
            PagedList<Event> mockEvents = mock(PagedList.class);
            when(mockEvents.stream()).thenReturn(Stream.empty());
            when(mockEvents.getTotalSize()).thenReturn(0L);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(clockProvider.getClockForUser(viewer)).thenReturn(fixedClock);
            when(eventDAO.findConfirmedEvents(eq(viewer.getId()), any(EventFilterDTO.class), eq(0), eq(10)))
                    .thenReturn(mockEvents);

            // Act
            eventService.getConfirmedEventsForCurrentUser(filter, 0, 10);

            // Assert - Verify the sanitized filter defaults null start to FAR_PAST and uses now as reference time
            verify(eventDAO).findConfirmedEvents(eq(viewer.getId()), argThat(sanitizedFilter -> 
                sanitizedFilter.start().equals(TimeUtils.FAR_PAST) &&
                sanitizedFilter.end().equals(nowInUserZone) &&
                sanitizedFilter.timeFilter() == TimeFilter.ALL
            ), eq(0), eq(10));
        }

        @Test
        void shouldApplyCustomFilterWithNullEnd() {
            // Arrange
            User viewer = createValidUserEntityWithId();
            ZonedDateTime customStart = getValidEventStartFuture(fixedClock);
            EventFilterDTO filter = new EventFilterDTO(null, TimeFilter.CUSTOM, customStart, null, false, false);
            PagedList<Event> mockEvents = mock(PagedList.class);
            when(mockEvents.stream()).thenReturn(Stream.empty());
            when(mockEvents.getTotalSize()).thenReturn(0L);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(clockProvider.getClockForUser(viewer)).thenReturn(fixedClock);
            when(eventDAO.findConfirmedEvents(eq(viewer.getId()), any(EventFilterDTO.class), eq(0), eq(10)))
                    .thenReturn(mockEvents);

            // Act
            eventService.getConfirmedEventsForCurrentUser(filter, 0, 10);

            // Assert - Verify the sanitized filter preserves custom start and uses now as reference time
            verify(eventDAO).findConfirmedEvents(eq(viewer.getId()), argThat(sanitizedFilter -> 
                sanitizedFilter.start().equals(customStart) &&
                sanitizedFilter.end().equals(nowInUserZone) &&
                sanitizedFilter.timeFilter() == TimeFilter.ALL
            ), eq(0), eq(10));
        }

        @Test
        void shouldThrowInvalidTimeExceptionForCustomFilterWithStartAfterEnd() {
            // Arrange
            User viewer = createValidUserEntityWithId();
            ZonedDateTime customStart = getValidEventEndFuture(fixedClock); // Later time
            ZonedDateTime customEnd = getValidEventStartFuture(fixedClock); // Earlier time
            EventFilterDTO filter = new EventFilterDTO(null, TimeFilter.CUSTOM, customStart, customEnd, false, false);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(clockProvider.getClockForUser(viewer)).thenReturn(fixedClock);

            // Act & Assert
            InvalidTimeException exception = assertThrows(InvalidTimeException.class, () -> 
                eventService.getConfirmedEventsForCurrentUser(filter, 0, 10));

            assertEquals(ErrorCode.INVALID_TIME_RANGE, exception.getErrorCode());
            verifyNoInteractions(eventDAO);
        }

    }

    @Nested
    class UnpinImpromptuEventForCurrentUserTests {

        @Test
        void shouldUnpinCurrentUserImpromptuEvent() {
            // Arrange
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            // Act
            eventService.unpinImpromptuEventForCurrentUser();

            // Assert
            verify(userBO).updateUser(user);
        }

        @Test
        void shouldCallUserBODirectly() {
            // Arrange
            User testUser = createValidUserEntityWithId();
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);

            // Act
            eventService.unpinImpromptuEventForCurrentUser();

            // Assert
            verify(userBO).updateUser(testUser);
            verifyNoMoreInteractions(userBO);
        }

    }

}

