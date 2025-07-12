package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.RecurringEventResponseDTO;
import com.yohan.event_planner.exception.ErrorCode;
import com.yohan.event_planner.exception.InvalidCalendarParameterException;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MyEventsServiceImpl}.
 * 
 * <p>Tests the coordination layer functionality, parameter validation,
 * and proper delegation to specialized services.</p>
 */
@ExtendWith(MockitoExtension.class)
class MyEventsServiceImplTest {

    @Mock
    private EventService eventService;

    @Mock
    private RecurringEventService recurringEventService;

    private MyEventsServiceImpl myEventsService;
    private final Clock fixedClock = Clock.systemUTC();

    @BeforeEach
    void setUp() {
        myEventsService = new MyEventsServiceImpl(eventService, recurringEventService);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create instance successfully with valid dependencies")
        void shouldCreateInstance_WhenValidDependencies() {
            // Act & Assert
            MyEventsServiceImpl service = new MyEventsServiceImpl(eventService, recurringEventService);
            
            assertNotNull(service);
        }

        @Test
        @DisplayName("Should throw NullPointerException when EventService is null")
        void shouldThrowException_WhenEventServiceIsNull() {
            // Act & Assert
            NullPointerException exception = assertThrows(NullPointerException.class, () -> 
                new MyEventsServiceImpl(null, recurringEventService));
            
            assertEquals("EventService cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw NullPointerException when RecurringEventService is null")
        void shouldThrowException_WhenRecurringEventServiceIsNull() {
            // Act & Assert
            NullPointerException exception = assertThrows(NullPointerException.class, () -> 
                new MyEventsServiceImpl(eventService, null));
            
            assertEquals("RecurringEventService cannot be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("getRecurringEventsPage Tests")
    class GetRecurringEventsPageTests {

        private final LocalDate endDateCursor = LocalDate.of(2024, 12, 31);
        private final LocalDate startDateCursor = LocalDate.of(2024, 1, 1);
        private final LocalTime startTimeCursor = LocalTime.of(9, 0);
        private final LocalTime endTimeCursor = LocalTime.of(17, 0);
        private final Long idCursor = 100L;

        @Test
        @DisplayName("Should return recurring events when valid parameters provided")
        void shouldReturnRecurringEvents_WhenValidParameters() {
            // Arrange
            int limit = 10;
            User creator = TestUtils.createValidUserEntityWithId();
            RecurringEvent mockEvent = TestUtils.createValidRecurringEventWithId(creator, 1L, fixedClock);
            RecurringEventResponseDTO mockEventDTO = TestUtils.createRecurringEventResponseDTO(mockEvent);
            List<RecurringEventResponseDTO> expectedEvents = List.of(mockEventDTO);
            
            when(recurringEventService.getConfirmedRecurringEventsPage(
                    endDateCursor, startDateCursor, startTimeCursor, endTimeCursor, idCursor, limit))
                    .thenReturn(expectedEvents);

            // Act
            List<RecurringEventResponseDTO> result = myEventsService.getRecurringEventsPage(
                    endDateCursor, startDateCursor, startTimeCursor, endTimeCursor, idCursor, limit);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(expectedEvents, result);
            
            verify(recurringEventService).getConfirmedRecurringEventsPage(
                    endDateCursor, startDateCursor, startTimeCursor, endTimeCursor, idCursor, limit);
        }

        @Test
        @DisplayName("Should handle null cursors correctly")
        void shouldHandleNullCursors_WhenNoFiltering() {
            // Arrange
            int limit = 5;
            List<RecurringEventResponseDTO> expectedEvents = Collections.emptyList();
            
            when(recurringEventService.getConfirmedRecurringEventsPage(
                    null, null, null, null, null, limit))
                    .thenReturn(expectedEvents);

            // Act
            List<RecurringEventResponseDTO> result = myEventsService.getRecurringEventsPage(
                    null, null, null, null, null, limit);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            
            verify(recurringEventService).getConfirmedRecurringEventsPage(
                    null, null, null, null, null, limit);
        }

        @Test
        @DisplayName("Should throw InvalidCalendarParameterException when limit is zero")
        void shouldThrowException_WhenLimitIsZero() {
            // Act & Assert
            InvalidCalendarParameterException exception = assertThrows(
                    InvalidCalendarParameterException.class, 
                    () -> myEventsService.getRecurringEventsPage(
                            endDateCursor, startDateCursor, startTimeCursor, endTimeCursor, idCursor, 0));
            
            assertEquals("The pagination parameter is invalid.", exception.getMessage());
            assertEquals(ErrorCode.INVALID_PAGINATION_PARAMETER, exception.getErrorCode());
            verifyNoInteractions(recurringEventService);
        }

        @Test
        @DisplayName("Should throw InvalidCalendarParameterException when limit is negative")
        void shouldThrowException_WhenLimitIsNegative() {
            // Act & Assert
            InvalidCalendarParameterException exception = assertThrows(
                    InvalidCalendarParameterException.class, 
                    () -> myEventsService.getRecurringEventsPage(
                            endDateCursor, startDateCursor, startTimeCursor, endTimeCursor, idCursor, -5));
            
            assertEquals("The pagination parameter is invalid.", exception.getMessage());
            assertEquals(ErrorCode.INVALID_PAGINATION_PARAMETER, exception.getErrorCode());
            verifyNoInteractions(recurringEventService);
        }

        @Test
        @DisplayName("Should handle boundary limit values correctly")
        void shouldHandleBoundaryLimitValues() {
            // Arrange
            int minLimit = 1;
            User creator = TestUtils.createValidUserEntityWithId();
            RecurringEvent mockEvent = TestUtils.createValidRecurringEventWithId(creator, 1L, fixedClock);
            RecurringEventResponseDTO mockEventDTO = TestUtils.createRecurringEventResponseDTO(mockEvent);
            List<RecurringEventResponseDTO> expectedEvents = List.of(mockEventDTO);
            
            when(recurringEventService.getConfirmedRecurringEventsPage(
                    null, null, null, null, null, minLimit))
                    .thenReturn(expectedEvents);

            // Act
            List<RecurringEventResponseDTO> result = myEventsService.getRecurringEventsPage(
                    null, null, null, null, null, minLimit);

            // Assert
            assertEquals(1, result.size());
            verify(recurringEventService).getConfirmedRecurringEventsPage(
                    null, null, null, null, null, minLimit);
        }

        @Test
        @DisplayName("Should handle large limit values correctly")
        void shouldHandleLargeLimitValues() {
            // Arrange
            int largeLimit = Integer.MAX_VALUE;
            List<RecurringEventResponseDTO> expectedEvents = Collections.emptyList();
            
            when(recurringEventService.getConfirmedRecurringEventsPage(
                    null, null, null, null, null, largeLimit))
                    .thenReturn(expectedEvents);

            // Act
            List<RecurringEventResponseDTO> result = myEventsService.getRecurringEventsPage(
                    null, null, null, null, null, largeLimit);

            // Assert
            assertTrue(result.isEmpty());
            verify(recurringEventService).getConfirmedRecurringEventsPage(
                    null, null, null, null, null, largeLimit);
        }

        @Test
        @DisplayName("Should handle partial cursor combinations correctly")
        void shouldHandlePartialCursorCombinations() {
            // Arrange
            int limit = 10;
            // Test with only some cursors provided
            LocalDate onlyEndDate = LocalDate.of(2024, 6, 15);
            Long onlyId = 50L;
            List<RecurringEventResponseDTO> expectedEvents = Collections.emptyList();
            
            when(recurringEventService.getConfirmedRecurringEventsPage(
                    onlyEndDate, null, null, null, onlyId, limit))
                    .thenReturn(expectedEvents);

            // Act
            List<RecurringEventResponseDTO> result = myEventsService.getRecurringEventsPage(
                    onlyEndDate, null, null, null, onlyId, limit);

            // Assert
            assertTrue(result.isEmpty());
            verify(recurringEventService).getConfirmedRecurringEventsPage(
                    onlyEndDate, null, null, null, onlyId, limit);
        }

        @Test
        @DisplayName("Should throw exception for Integer.MIN_VALUE limit")
        void shouldThrowException_WhenLimitIsMinValue() {
            // Act & Assert
            InvalidCalendarParameterException exception = assertThrows(
                    InvalidCalendarParameterException.class, 
                    () -> myEventsService.getRecurringEventsPage(
                            null, null, null, null, null, Integer.MIN_VALUE));
            
            assertEquals("The pagination parameter is invalid.", exception.getMessage());
            assertEquals(ErrorCode.INVALID_PAGINATION_PARAMETER, exception.getErrorCode());
            verifyNoInteractions(recurringEventService);
        }

        @Test
        @DisplayName("Should propagate exceptions from RecurringEventService")
        void shouldPropagateException_WhenRecurringEventServiceThrows() {
            // Arrange
            int limit = 10;
            RuntimeException serviceException = new RuntimeException("Service failed");
            
            when(recurringEventService.getConfirmedRecurringEventsPage(
                    null, null, null, null, null, limit))
                    .thenThrow(serviceException);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, 
                    () -> myEventsService.getRecurringEventsPage(null, null, null, null, null, limit));
            
            assertEquals("Service failed", exception.getMessage());
            verify(recurringEventService).getConfirmedRecurringEventsPage(
                    null, null, null, null, null, limit);
        }
    }

    @Nested
    @DisplayName("getEventsPage Tests")
    class GetEventsPageTests {

        private final ZonedDateTime endTimeCursor = ZonedDateTime.now().plusDays(1);
        private final ZonedDateTime startTimeCursor = ZonedDateTime.now().minusDays(1);
        private final Long idCursor = 200L;

        @Test
        @DisplayName("Should return events when valid parameters provided")
        void shouldReturnEvents_WhenValidParameters() {
            // Arrange
            int limit = 15;
            User creator = TestUtils.createValidUserEntityWithId();
            Event mockEvent = TestUtils.createValidScheduledEventWithId(1L, creator, fixedClock);
            EventResponseDTO mockEventDTO = TestUtils.createEventResponseDTO(mockEvent);
            List<EventResponseDTO> expectedEvents = List.of(mockEventDTO);
            
            when(eventService.getConfirmedEventsPage(endTimeCursor, startTimeCursor, idCursor, limit))
                    .thenReturn(expectedEvents);

            // Act
            List<EventResponseDTO> result = myEventsService.getEventsPage(
                    endTimeCursor, startTimeCursor, idCursor, limit);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(expectedEvents, result);
            
            verify(eventService).getConfirmedEventsPage(endTimeCursor, startTimeCursor, idCursor, limit);
        }

        @Test
        @DisplayName("Should handle null cursors correctly")
        void shouldHandleNullCursors_WhenNoFiltering() {
            // Arrange
            int limit = 20;
            List<EventResponseDTO> expectedEvents = Collections.emptyList();
            
            when(eventService.getConfirmedEventsPage(null, null, null, limit))
                    .thenReturn(expectedEvents);

            // Act
            List<EventResponseDTO> result = myEventsService.getEventsPage(null, null, null, limit);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
            
            verify(eventService).getConfirmedEventsPage(null, null, null, limit);
        }

        @Test
        @DisplayName("Should throw InvalidCalendarParameterException when limit is zero")
        void shouldThrowException_WhenLimitIsZero() {
            // Act & Assert
            InvalidCalendarParameterException exception = assertThrows(
                    InvalidCalendarParameterException.class, 
                    () -> myEventsService.getEventsPage(endTimeCursor, startTimeCursor, idCursor, 0));
            
            assertEquals("The pagination parameter is invalid.", exception.getMessage());
            assertEquals(ErrorCode.INVALID_PAGINATION_PARAMETER, exception.getErrorCode());
            verifyNoInteractions(eventService);
        }

        @Test
        @DisplayName("Should throw InvalidCalendarParameterException when limit is negative")
        void shouldThrowException_WhenLimitIsNegative() {
            // Act & Assert
            InvalidCalendarParameterException exception = assertThrows(
                    InvalidCalendarParameterException.class, 
                    () -> myEventsService.getEventsPage(endTimeCursor, startTimeCursor, idCursor, -10));
            
            assertEquals("The pagination parameter is invalid.", exception.getMessage());
            assertEquals(ErrorCode.INVALID_PAGINATION_PARAMETER, exception.getErrorCode());
            verifyNoInteractions(eventService);
        }

        @Test
        @DisplayName("Should handle multiple events correctly")
        void shouldReturnMultipleEvents_WhenEventsExist() {
            // Arrange
            int limit = 25;
            User creator = TestUtils.createValidUserEntityWithId();
            Event event1 = TestUtils.createValidScheduledEventWithId(1L, creator, fixedClock);
            Event event2 = TestUtils.createValidScheduledEventWithId(2L, creator, fixedClock);
            Event event3 = TestUtils.createValidScheduledEventWithId(3L, creator, fixedClock);
            
            List<EventResponseDTO> expectedEvents = List.of(
                    TestUtils.createEventResponseDTO(event1), 
                    TestUtils.createEventResponseDTO(event2),
                    TestUtils.createEventResponseDTO(event3)
            );
            
            when(eventService.getConfirmedEventsPage(null, null, null, limit))
                    .thenReturn(expectedEvents);

            // Act
            List<EventResponseDTO> result = myEventsService.getEventsPage(null, null, null, limit);

            // Assert
            assertEquals(3, result.size());
            assertEquals(expectedEvents, result);
            verify(eventService).getConfirmedEventsPage(null, null, null, limit);
        }

        @Test
        @DisplayName("Should handle extreme cursor values correctly")
        void shouldHandleExtremeCursorValues() {
            // Arrange
            ZonedDateTime extremePast = ZonedDateTime.parse("1900-01-01T00:00:00Z");
            ZonedDateTime extremeFuture = ZonedDateTime.parse("2200-12-31T23:59:59Z");
            Long extremeId = Long.MAX_VALUE;
            int limit = 5;
            
            when(eventService.getConfirmedEventsPage(extremePast, extremeFuture, extremeId, limit))
                    .thenReturn(Collections.emptyList());

            // Act
            List<EventResponseDTO> result = myEventsService.getEventsPage(
                    extremePast, extremeFuture, extremeId, limit);

            // Assert
            assertTrue(result.isEmpty());
            verify(eventService).getConfirmedEventsPage(extremePast, extremeFuture, extremeId, limit);
        }

        @Test
        @DisplayName("Should handle partial cursor combinations correctly")
        void shouldHandlePartialCursorCombinations() {
            // Arrange
            int limit = 8;
            // Test with only some cursors provided
            ZonedDateTime onlyStartTime = ZonedDateTime.now().minusHours(2);
            List<EventResponseDTO> expectedEvents = Collections.emptyList();
            
            when(eventService.getConfirmedEventsPage(null, onlyStartTime, null, limit))
                    .thenReturn(expectedEvents);

            // Act
            List<EventResponseDTO> result = myEventsService.getEventsPage(
                    null, onlyStartTime, null, limit);

            // Assert
            assertTrue(result.isEmpty());
            verify(eventService).getConfirmedEventsPage(null, onlyStartTime, null, limit);
        }

        @Test
        @DisplayName("Should throw exception for Integer.MIN_VALUE limit")
        void shouldThrowException_WhenLimitIsMinValue() {
            // Act & Assert
            InvalidCalendarParameterException exception = assertThrows(
                    InvalidCalendarParameterException.class, 
                    () -> myEventsService.getEventsPage(null, null, null, Integer.MIN_VALUE));
            
            assertEquals("The pagination parameter is invalid.", exception.getMessage());
            assertEquals(ErrorCode.INVALID_PAGINATION_PARAMETER, exception.getErrorCode());
            verifyNoInteractions(eventService);
        }

        @Test
        @DisplayName("Should handle large limit values correctly")
        void shouldHandleLargeLimitValues() {
            // Arrange
            int largeLimit = Integer.MAX_VALUE;
            List<EventResponseDTO> expectedEvents = Collections.emptyList();
            
            when(eventService.getConfirmedEventsPage(null, null, null, largeLimit))
                    .thenReturn(expectedEvents);

            // Act
            List<EventResponseDTO> result = myEventsService.getEventsPage(null, null, null, largeLimit);

            // Assert
            assertTrue(result.isEmpty());
            verify(eventService).getConfirmedEventsPage(null, null, null, largeLimit);
        }

        @Test
        @DisplayName("Should propagate exceptions from EventService")
        void shouldPropagateException_WhenEventServiceThrows() {
            // Arrange
            int limit = 15;
            RuntimeException serviceException = new RuntimeException("Event service failed");
            
            when(eventService.getConfirmedEventsPage(null, null, null, limit))
                    .thenThrow(serviceException);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, 
                    () -> myEventsService.getEventsPage(null, null, null, limit));
            
            assertEquals("Event service failed", exception.getMessage());
            verify(eventService).getConfirmedEventsPage(null, null, null, limit);
        }
    }

    @Nested
    @DisplayName("Cursor Date Validation Tests")
    class CursorDateValidationTests {

        @Test
        @DisplayName("Should handle inconsistent date cursors for recurring events")
        void shouldHandleInconsistentDateCursors_RecurringEvents() {
            // Arrange
            int limit = 10;
            LocalDate startDate = LocalDate.of(2024, 12, 31);
            LocalDate endDate = LocalDate.of(2024, 1, 1); // End before start
            LocalTime startTime = LocalTime.of(10, 0);
            LocalTime endTime = LocalTime.of(9, 0); // End before start
            Long id = 1L;
            
            when(recurringEventService.getConfirmedRecurringEventsPage(
                    endDate, startDate, startTime, endTime, id, limit))
                    .thenReturn(Collections.emptyList());

            // Act
            List<RecurringEventResponseDTO> result = myEventsService.getRecurringEventsPage(
                    endDate, startDate, startTime, endTime, id, limit);

            // Assert
            assertTrue(result.isEmpty());
            verify(recurringEventService).getConfirmedRecurringEventsPage(
                    endDate, startDate, startTime, endTime, id, limit);
        }

        @Test
        @DisplayName("Should handle inconsistent datetime cursors for events")
        void shouldHandleInconsistentDateTimeCursors_Events() {
            // Arrange
            int limit = 10;
            ZonedDateTime startTime = ZonedDateTime.now().plusDays(1);
            ZonedDateTime endTime = ZonedDateTime.now().minusDays(1); // End before start
            Long id = 1L;
            
            when(eventService.getConfirmedEventsPage(endTime, startTime, id, limit))
                    .thenReturn(Collections.emptyList());

            // Act
            List<EventResponseDTO> result = myEventsService.getEventsPage(endTime, startTime, id, limit);

            // Assert
            assertTrue(result.isEmpty());
            verify(eventService).getConfirmedEventsPage(endTime, startTime, id, limit);
        }

        @Test
        @DisplayName("Should handle extreme date ranges for recurring events")
        void shouldHandleExtremeDateRanges_RecurringEvents() {
            // Arrange
            int limit = 5;
            LocalDate extremePast = LocalDate.of(1900, 1, 1);
            LocalDate extremeFuture = LocalDate.of(2200, 12, 31);
            LocalTime midnight = LocalTime.MIDNIGHT;
            LocalTime endOfDay = LocalTime.of(23, 59, 59);
            Long extremeId = Long.MAX_VALUE;
            
            when(recurringEventService.getConfirmedRecurringEventsPage(
                    extremeFuture, extremePast, midnight, endOfDay, extremeId, limit))
                    .thenReturn(Collections.emptyList());

            // Act
            List<RecurringEventResponseDTO> result = myEventsService.getRecurringEventsPage(
                    extremeFuture, extremePast, midnight, endOfDay, extremeId, limit);

            // Assert
            assertTrue(result.isEmpty());
            verify(recurringEventService).getConfirmedRecurringEventsPage(
                    extremeFuture, extremePast, midnight, endOfDay, extremeId, limit);
        }
    }

    @Nested
    @DisplayName("Service Exception Type Tests")
    class ServiceExceptionTypeTests {

        @Test
        @DisplayName("Should propagate IllegalArgumentException from RecurringEventService")
        void shouldPropagateIllegalArgumentException_RecurringEventService() {
            // Arrange
            int limit = 10;
            IllegalArgumentException serviceException = new IllegalArgumentException("Invalid cursor combination");
            
            when(recurringEventService.getConfirmedRecurringEventsPage(
                    null, null, null, null, null, limit))
                    .thenThrow(serviceException);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> myEventsService.getRecurringEventsPage(null, null, null, null, null, limit));
            
            assertEquals("Invalid cursor combination", exception.getMessage());
            verify(recurringEventService).getConfirmedRecurringEventsPage(
                    null, null, null, null, null, limit);
        }

        @Test
        @DisplayName("Should propagate IllegalStateException from EventService")
        void shouldPropagateIllegalStateException_EventService() {
            // Arrange
            int limit = 10;
            IllegalStateException serviceException = new IllegalStateException("Service not ready");
            
            when(eventService.getConfirmedEventsPage(null, null, null, limit))
                    .thenThrow(serviceException);

            // Act & Assert
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> myEventsService.getEventsPage(null, null, null, limit));
            
            assertEquals("Service not ready", exception.getMessage());
            verify(eventService).getConfirmedEventsPage(null, null, null, limit);
        }

        @Test
        @DisplayName("Should propagate custom exceptions from RecurringEventService")
        void shouldPropagateCustomException_RecurringEventService() {
            // Arrange
            int limit = 10;
            InvalidCalendarParameterException serviceException = new InvalidCalendarParameterException(
                    ErrorCode.INVALID_CALENDAR_PARAMETER);
            
            when(recurringEventService.getConfirmedRecurringEventsPage(
                    null, null, null, null, null, limit))
                    .thenThrow(serviceException);

            // Act & Assert
            InvalidCalendarParameterException exception = assertThrows(InvalidCalendarParameterException.class,
                    () -> myEventsService.getRecurringEventsPage(null, null, null, null, null, limit));
            
            assertEquals("The calendar parameter is invalid.", exception.getMessage());
            assertEquals(ErrorCode.INVALID_CALENDAR_PARAMETER, exception.getErrorCode());
            verify(recurringEventService).getConfirmedRecurringEventsPage(
                    null, null, null, null, null, limit);
        }
    }

    @Nested
    @DisplayName("Parameter Combination Validation Tests")
    class ParameterCombinationValidationTests {

        @Test
        @DisplayName("Should handle time cursor without date cursor for recurring events")
        void shouldHandleTimeWithoutDateCursor_RecurringEvents() {
            // Arrange
            int limit = 10;
            LocalTime startTime = LocalTime.of(14, 30);
            LocalTime endTime = LocalTime.of(16, 45);
            
            when(recurringEventService.getConfirmedRecurringEventsPage(
                    null, null, startTime, endTime, null, limit))
                    .thenReturn(Collections.emptyList());

            // Act
            List<RecurringEventResponseDTO> result = myEventsService.getRecurringEventsPage(
                    null, null, startTime, endTime, null, limit);

            // Assert
            assertTrue(result.isEmpty());
            verify(recurringEventService).getConfirmedRecurringEventsPage(
                    null, null, startTime, endTime, null, limit);
        }

        @Test
        @DisplayName("Should handle ID cursor with extreme values")
        void shouldHandleIdCursorExtremeValues() {
            // Arrange
            int limit = 5;
            Long negativeId = -1L;
            Long zeroId = 0L;
            Long maxId = Long.MAX_VALUE;
            
            when(eventService.getConfirmedEventsPage(null, null, negativeId, limit))
                    .thenReturn(Collections.emptyList());
            when(eventService.getConfirmedEventsPage(null, null, zeroId, limit))
                    .thenReturn(Collections.emptyList());
            when(eventService.getConfirmedEventsPage(null, null, maxId, limit))
                    .thenReturn(Collections.emptyList());

            // Act & Assert
            List<EventResponseDTO> resultNegative = myEventsService.getEventsPage(null, null, negativeId, limit);
            List<EventResponseDTO> resultZero = myEventsService.getEventsPage(null, null, zeroId, limit);
            List<EventResponseDTO> resultMax = myEventsService.getEventsPage(null, null, maxId, limit);

            assertTrue(resultNegative.isEmpty());
            assertTrue(resultZero.isEmpty());
            assertTrue(resultMax.isEmpty());
            
            verify(eventService).getConfirmedEventsPage(null, null, negativeId, limit);
            verify(eventService).getConfirmedEventsPage(null, null, zeroId, limit);
            verify(eventService).getConfirmedEventsPage(null, null, maxId, limit);
        }

        @Test
        @DisplayName("Should handle mixed cursor completeness scenarios")
        void shouldHandleMixedCursorCompleteness() {
            // Arrange
            int limit = 8;
            LocalDate onlyEndDate = LocalDate.of(2024, 6, 15);
            LocalTime onlyStartTime = LocalTime.of(12, 0);
            Long onlyId = 42L;
            
            when(recurringEventService.getConfirmedRecurringEventsPage(
                    onlyEndDate, null, onlyStartTime, null, onlyId, limit))
                    .thenReturn(Collections.emptyList());

            // Act
            List<RecurringEventResponseDTO> result = myEventsService.getRecurringEventsPage(
                    onlyEndDate, null, onlyStartTime, null, onlyId, limit);

            // Assert
            assertTrue(result.isEmpty());
            verify(recurringEventService).getConfirmedRecurringEventsPage(
                    onlyEndDate, null, onlyStartTime, null, onlyId, limit);
        }
    }
}