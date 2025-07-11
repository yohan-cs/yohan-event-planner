package com.yohan.event_planner.service;

import com.blazebit.persistence.PagedList;
import com.yohan.event_planner.business.EventBO;
import com.yohan.event_planner.business.RecurringEventBO;
import com.yohan.event_planner.business.handler.RecurringEventPatchHandler;
import com.yohan.event_planner.dao.RecurringEventDAO;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.TimeFilter;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventResponseDTOFactory;
import com.yohan.event_planner.dto.RecurringEventCreateDTO;
import com.yohan.event_planner.dto.RecurringEventFilterDTO;
import com.yohan.event_planner.dto.RecurringEventResponseDTO;
import com.yohan.event_planner.dto.RecurringEventUpdateDTO;
import com.yohan.event_planner.exception.InvalidSkipDayException;
import com.yohan.event_planner.exception.RecurringEventAlreadyConfirmedException;
import com.yohan.event_planner.exception.RecurringEventNotFoundException;
import com.yohan.event_planner.exception.UserOwnershipException;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import com.yohan.event_planner.time.ClockProvider;
import com.yohan.event_planner.util.RecurringEventResponseDTOAssertions;
import com.yohan.event_planner.util.TestConstants;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_USER_ACCESS;
import static com.yohan.event_planner.util.TestConstants.USER_ID_OTHER;
import static com.yohan.event_planner.util.TestConstants.VALID_LABEL_ID;
import static com.yohan.event_planner.util.TestConstants.VALID_RECURRING_EVENT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
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


public class RecurringEventServiceImplTest {

    private static final LocalDate FIXED_TEST_DATE = LocalDate.of(2025, 6, 29);

    private RecurringEventBO recurringEventBO;
    private LabelService labelService;
    private RecurringEventDAO recurringEventDAO;
    private RecurringEventPatchHandler recurringEventPatchHandler;
    private RecurrenceRuleService recurrenceRuleService;
    private OwnershipValidator ownershipValidator;
    private AuthenticatedUserProvider authenticatedUserProvider;
    private EventResponseDTOFactory eventResponseDTOFactory;
    private ClockProvider clockProvider;
    private Clock fixedClock;

    private RecurringEventServiceImpl recurringEventService;

    @BeforeEach
    void setUp() {
        recurringEventBO = mock(RecurringEventBO.class);
        labelService = mock(LabelService.class);
        recurringEventDAO = mock(RecurringEventDAO.class);
        recurringEventPatchHandler = mock(RecurringEventPatchHandler.class);
        recurrenceRuleService = mock(RecurrenceRuleService.class);
        ownershipValidator = mock(OwnershipValidator.class);
        authenticatedUserProvider = mock(AuthenticatedUserProvider.class);
        eventResponseDTOFactory = mock(EventResponseDTOFactory.class);
        clockProvider = mock(ClockProvider.class);

        fixedClock = Clock.fixed(Instant.parse("2025-06-29T12:00:00Z"), ZoneId.of("UTC"));

        // Mock clockProvider to return fixedClock for any zone
        when(clockProvider.getClockForZone(any(ZoneId.class))).thenReturn(fixedClock);
        when(clockProvider.getClockForUser(any(User.class))).thenReturn(fixedClock);

        recurringEventService = new RecurringEventServiceImpl(
                recurringEventBO,
                mock(EventBO.class),
                labelService,
                recurringEventDAO,
                recurringEventPatchHandler,
                recurrenceRuleService,
                ownershipValidator,
                authenticatedUserProvider,
                eventResponseDTOFactory,
                clockProvider
        );
    }


    @Nested
    class GetRecurringEventByIdTests {

        @Test
        void testReturnsDtoWhenEventIsConfirmedAndViewerIsNotCreator() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId(1L);
            User viewer = TestUtils.createValidUserEntityWithId(2L);
            RecurringEvent event = TestUtils.createValidRecurringEventWithId(creator, 100L, fixedClock);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(recurringEventBO.getRecurringEventById(100L)).thenReturn(Optional.of(event));

            // Act
            RecurringEventResponseDTO result = recurringEventService.getRecurringEventById(100L);

            // Assert
            assertEquals(event.getId(), result.id());
            assertEquals(event.getName(), result.name());
        }

        @Test
        void testReturnsDtoWhenViewerIsCreatorEvenIfUnconfirmed() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId(1L);
            RecurringEvent event = TestUtils.createUnconfirmedRecurringEvent(creator, fixedClock);
            TestUtils.setRecurringEventId(event, 200L);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(creator);
            when(recurringEventBO.getRecurringEventById(200L)).thenReturn(Optional.of(event));

            // Act
            RecurringEventResponseDTO result = recurringEventService.getRecurringEventById(200L);

            // Assert
            assertEquals(200L, result.id());
            assertTrue(result.unconfirmed());
        }

        @Test
        void testThrowsWhenUnconfirmedAndViewerIsNotCreator() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId(1L);
            User viewer = TestUtils.createValidUserEntityWithId(3L);
            RecurringEvent draft = TestUtils.createUnconfirmedRecurringEvent(creator, fixedClock);
            TestUtils.setRecurringEventId(draft, 300L);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(recurringEventBO.getRecurringEventById(300L)).thenReturn(Optional.of(draft));

            // Act + Assert
            assertThrows(RecurringEventNotFoundException.class, () ->
                    recurringEventService.getRecurringEventById(300L));
        }

        @Test
        void testThrowsWhenRecurringEventNotFound() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId(4L);
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(recurringEventBO.getRecurringEventById(400L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(RecurringEventNotFoundException.class, () ->
                    recurringEventService.getRecurringEventById(400L));
        }
    }

    @Nested
    class GetConfirmedRecurringEventsByUserTests {

        @Test
        void testGetConfirmedRecurringEventsForCurrentUser_Success() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId(1L);
            RecurringEvent recurringEvent = TestUtils.createValidRecurringEventWithId(viewer, 100L, fixedClock);
            RecurringEventResponseDTO expectedDto = TestUtils.createRecurringEventResponseDTO(recurringEvent);

            RecurringEventFilterDTO filter = new RecurringEventFilterDTO(
                    VALID_LABEL_ID,
                    TimeFilter.ALL, // Adjust if your enum requires a specific value
                    null,
                    null,
                    true
            );

            int pageNumber = 0;
            int pageSize = 10;

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(clockProvider.getClockForUser(viewer)).thenReturn(fixedClock);

            PagedList<RecurringEvent> pagedRecurringEvents = mock(PagedList.class);
            when(pagedRecurringEvents.stream()).thenReturn(Stream.of(recurringEvent));
            when(pagedRecurringEvents.getTotalSize()).thenReturn(1L);

            // Expect the resolved filter (TimeFilter.ALL resolves to FAR_PAST_DATE and FAR_FUTURE_DATE)
            RecurringEventFilterDTO expectedResolvedFilter = new RecurringEventFilterDTO(
                    filter.labelId(),
                    filter.timeFilter(),
                    com.yohan.event_planner.time.TimeUtils.FAR_PAST_DATE,
                    com.yohan.event_planner.time.TimeUtils.FAR_FUTURE_DATE,
                    filter.sortDescending()
            );
            
            when(recurringEventDAO.findConfirmedRecurringEvents(eq(viewer.getId()), eq(expectedResolvedFilter), eq(pageNumber), eq(pageSize)))
                    .thenReturn(pagedRecurringEvents);

            // Act
            Page<RecurringEventResponseDTO> results = recurringEventService.getConfirmedRecurringEventsForCurrentUser(filter, pageNumber, pageSize);

            // Assert
            assertEquals(1, results.getTotalElements());

            RecurringEventResponseDTO actualDto = results.getContent().get(0);
            RecurringEventResponseDTOAssertions.assertRecurringEventResponseDTOEquals(actualDto, expectedDto);
        }

        @Test
        void testGetConfirmedRecurringEventsForCurrentUser_EmptyResults() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId(1L);

            RecurringEventFilterDTO filter = new RecurringEventFilterDTO(
                    VALID_LABEL_ID,
                    TimeFilter.ALL, // Adjust as needed
                    null,
                    null,
                    true
            );

            int pageNumber = 0;
            int pageSize = 10;

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(clockProvider.getClockForUser(viewer)).thenReturn(fixedClock);

            PagedList<RecurringEvent> pagedRecurringEvents = mock(PagedList.class);
            when(pagedRecurringEvents.stream()).thenReturn(Stream.empty());
            when(pagedRecurringEvents.getTotalSize()).thenReturn(0L);

            // Expect the resolved filter (TimeFilter.ALL resolves to FAR_PAST_DATE and FAR_FUTURE_DATE)
            RecurringEventFilterDTO expectedResolvedFilter = new RecurringEventFilterDTO(
                    filter.labelId(),
                    filter.timeFilter(),
                    com.yohan.event_planner.time.TimeUtils.FAR_PAST_DATE,
                    com.yohan.event_planner.time.TimeUtils.FAR_FUTURE_DATE,
                    filter.sortDescending()
            );
            
            when(recurringEventDAO.findConfirmedRecurringEvents(eq(viewer.getId()), eq(expectedResolvedFilter), eq(pageNumber), eq(pageSize)))
                    .thenReturn(pagedRecurringEvents);

            // Act
            Page<RecurringEventResponseDTO> results = recurringEventService.getConfirmedRecurringEventsForCurrentUser(filter, pageNumber, pageSize);

            // Assert
            assertTrue(results.isEmpty(), "Expected no recurring events in results");
            assertEquals(0, results.getTotalElements(), "Expected total elements to be zero");
        }

    }


    @Nested
    class GetUnconfirmedRecurringEventsForCurrentUserTests {

        @Test
        void testGetUnconfirmedRecurringEventsForCurrentUser_Success() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId(1L);
            RecurringEvent recurringEvent = TestUtils.createUnconfirmedRecurringEvent(viewer, fixedClock);
            RecurringEventResponseDTO expectedDto = TestUtils.createRecurringEventResponseDTO(recurringEvent);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(recurringEventBO.getUnconfirmedRecurringEventsForUserInRange(eq(viewer.getId()), any(), any()))
                    .thenReturn(List.of(recurringEvent));

            // Act
            List<RecurringEventResponseDTO> results = recurringEventService.getUnconfirmedRecurringEventsForCurrentUser();

            // Assert
            assertEquals(1, results.size());
            RecurringEventResponseDTO actualDto = results.get(0);
            RecurringEventResponseDTOAssertions.assertRecurringEventResponseDTOEquals(actualDto, expectedDto);
        }

        @Test
        void testGetUnconfirmedRecurringEventsForCurrentUser_EmptyResults() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId(1L);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(recurringEventBO.getUnconfirmedRecurringEventsForUserInRange(eq(viewer.getId()), any(), any()))
                    .thenReturn(Collections.emptyList());

            // Act
            List<RecurringEventResponseDTO> results = recurringEventService.getUnconfirmedRecurringEventsForCurrentUser();

            // Assert
            assertTrue(results.isEmpty(), "Expected empty list of unconfirmed recurring events.");
        }

        @Test
        void testGetUnconfirmedRecurringEventsForCurrentUser_NoUnconfirmedEvents() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId(1L);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(recurringEventBO.getUnconfirmedRecurringEventsForUserInRange(eq(viewer.getId()), any(), any()))
                    .thenReturn(Collections.emptyList());

            // Act
            List<RecurringEventResponseDTO> results = recurringEventService.getUnconfirmedRecurringEventsForCurrentUser();

            // Assert
            assertTrue(results.isEmpty(), "Expected empty list when no unconfirmed events exist.");
        }

        @Test
        void testGetUnconfirmedRecurringEventsForCurrentUser_AuthenticatedUser() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId(1L);
            RecurringEvent recurringEvent = TestUtils.createUnconfirmedRecurringEvent(viewer, fixedClock);
            RecurringEventResponseDTO expectedDto = TestUtils.createRecurringEventResponseDTO(recurringEvent);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(recurringEventBO.getUnconfirmedRecurringEventsForUserInRange(eq(viewer.getId()), any(), any()))
                    .thenReturn(List.of(recurringEvent));

            // Act
            List<RecurringEventResponseDTO> results = recurringEventService.getUnconfirmedRecurringEventsForCurrentUser();

            // Assert
            assertEquals(1, results.size());
            RecurringEventResponseDTO actualDto = results.get(0);
            RecurringEventResponseDTOAssertions.assertRecurringEventResponseDTOEquals(actualDto, expectedDto);
        }
    }

    @Nested
    class CreateRecurringEventTests {

        @Test
        void createsConfirmedRecurringEventSuccessfully() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(creator);

            RecurringEventCreateDTO dto = new RecurringEventCreateDTO(
                    TestConstants.VALID_EVENT_TITLE,
                    TestConstants.getValidEventStartFuture(fixedClock).toLocalTime(),
                    TestConstants.getValidEventEndFuture(fixedClock).toLocalTime(),
                    TestConstants.getValidEventStartDate(fixedClock),
                    TestConstants.getValidEventEndDate(fixedClock),
                    TestConstants.VALID_EVENT_DESCRIPTION,
                    VALID_LABEL_ID,
                    TestConstants.VALID_WEEKLY_RECURRENCE_RULE,
                    Set.of(FIXED_TEST_DATE.plusDays(1)),
                    false
            );

            ParsedRecurrenceInput parsed = mock(ParsedRecurrenceInput.class);
            when(recurrenceRuleService.parseFromString(dto.recurrenceRule())).thenReturn(parsed);
            when(recurrenceRuleService.buildSummary(parsed, dto.startDate(), dto.endDate())).thenReturn("Every Mon/Wed/Fri until end date");

            Label label = TestUtils.createValidLabelWithId(VALID_LABEL_ID, creator);
            when(labelService.getLabelEntityById(dto.labelId())).thenReturn(label);

            RecurringEvent savedEvent = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);
            when(recurringEventBO.createRecurringEventWithValidation(any())).thenReturn(savedEvent);

            // Act
            RecurringEventResponseDTO response = recurringEventService.createRecurringEvent(dto);

            // Assert
            assertNotNull(response);
            assertEquals(savedEvent.getId(), response.id());
            assertEquals(savedEvent.getName(), response.name());

            verify(recurrenceRuleService).parseFromString(dto.recurrenceRule());
            verify(recurrenceRuleService).buildSummary(parsed, dto.startDate(), dto.endDate());
            verify(labelService).getLabelEntityById(dto.labelId());
            verify(recurringEventBO).createRecurringEventWithValidation(any());
        }

        @Test
        void createsDraftRecurringEventSuccessfully() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(creator);

            RecurringEventCreateDTO dto = new RecurringEventCreateDTO(
                    "Draft Session",
                    null, null,
                    FIXED_TEST_DATE,
                    FIXED_TEST_DATE.plusDays(5),
                    null,
                    null,
                    TestConstants.VALID_WEEKLY_RECURRENCE_RULE,
                    null,
                    true
            );

            RecurringEvent draftEvent = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);
            when(recurringEventBO.createRecurringEventWithValidation(any())).thenReturn(draftEvent);

            // Act
            RecurringEventResponseDTO response = recurringEventService.createRecurringEvent(dto);

            // Assert
            assertNotNull(response);
            assertEquals(draftEvent.getId(), response.id());

            verify(authenticatedUserProvider).getCurrentUser();
            verify(recurringEventBO).createRecurringEventWithValidation(any());
        }

        @Test
        void usesUnlabeledWhenLabelIdIsNull() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(creator);

            RecurringEventCreateDTO dto = new RecurringEventCreateDTO(
                    "No Label Session",
                    TestConstants.getValidEventStartFuture(fixedClock).toLocalTime(),
                    TestConstants.getValidEventEndFuture(fixedClock).toLocalTime(),
                    FIXED_TEST_DATE,
                    FIXED_TEST_DATE.plusDays(1),
                    null,
                    null, // labelId is null
                    TestConstants.VALID_WEEKLY_RECURRENCE_RULE,
                    null,
                    false
            );

            ParsedRecurrenceInput parsed = mock(ParsedRecurrenceInput.class);
            when(recurrenceRuleService.parseFromString(dto.recurrenceRule())).thenReturn(parsed);
            when(recurrenceRuleService.buildSummary(parsed, dto.startDate(), dto.endDate())).thenReturn("Every day");

            RecurringEvent savedEvent = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);
            when(recurringEventBO.createRecurringEventWithValidation(any())).thenReturn(savedEvent);

            // Act
            RecurringEventResponseDTO response = recurringEventService.createRecurringEvent(dto);

            // Assert
            assertNotNull(response);
            assertEquals(savedEvent.getId(), response.id());
            verify(recurringEventBO).createRecurringEventWithValidation(any());
        }

        @Test
        void createsRecurringEventWithSkipDays() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(creator);

            Set<LocalDate> skipDays = Set.of(FIXED_TEST_DATE.plusDays(2));

            RecurringEventCreateDTO dto = new RecurringEventCreateDTO(
                    "Session With Skips",
                    TestConstants.getValidEventStartFuture(fixedClock).toLocalTime(),
                    TestConstants.getValidEventEndFuture(fixedClock).toLocalTime(),
                    FIXED_TEST_DATE,
                    FIXED_TEST_DATE.plusDays(10),
                    "Skipping some days",
                    VALID_LABEL_ID,
                    TestConstants.VALID_WEEKLY_RECURRENCE_RULE,
                    skipDays,
                    false
            );

            ParsedRecurrenceInput parsed = mock(ParsedRecurrenceInput.class);
            when(recurrenceRuleService.parseFromString(dto.recurrenceRule())).thenReturn(parsed);
            when(recurrenceRuleService.buildSummary(parsed, dto.startDate(), dto.endDate())).thenReturn("Every Mon/Wed/Fri until end date");

            Label label = TestUtils.createValidLabelWithId(VALID_LABEL_ID, creator);
            when(labelService.getLabelEntityById(dto.labelId())).thenReturn(label);

            RecurringEvent savedEvent = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);
            when(recurringEventBO.createRecurringEventWithValidation(any())).thenReturn(savedEvent);

            // Act
            RecurringEventResponseDTO response = recurringEventService.createRecurringEvent(dto);

            // Assert
            assertNotNull(response);
            verify(recurrenceRuleService).parseFromString(dto.recurrenceRule());
            verify(recurrenceRuleService).buildSummary(parsed, dto.startDate(), dto.endDate());
            verify(labelService).getLabelEntityById(dto.labelId());
            verify(recurringEventBO).createRecurringEventWithValidation(argThat(event ->
                    skipDays.equals(event.getSkipDays())
            ));
        }

    }

    @Nested
    class ConfirmRecurringEventTests {

        @Test
        void confirmsUnconfirmedRecurringEventSuccessfully() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);

            RecurringEvent recurringEvent = TestUtils.createValidRecurringEventWithId(viewer, VALID_RECURRING_EVENT_ID, fixedClock);
            recurringEvent.setUnconfirmed(true);

            when(recurringEventBO.getRecurringEventById(VALID_RECURRING_EVENT_ID))
                    .thenReturn(Optional.of(recurringEvent));

            doNothing().when(ownershipValidator).validateRecurringEventOwnership(viewer.getId(), recurringEvent);

            RecurringEvent confirmedEvent = TestUtils.createValidRecurringEventWithId(viewer, VALID_RECURRING_EVENT_ID, fixedClock);
            confirmedEvent.setUnconfirmed(false);

            when(recurringEventBO.confirmRecurringEventWithValidation(recurringEvent))
                    .thenReturn(confirmedEvent);

            // Act
            RecurringEventResponseDTO response = recurringEventService.confirmRecurringEvent(VALID_RECURRING_EVENT_ID);

            // Assert
            assertNotNull(response);
            assertEquals(confirmedEvent.getId(), response.id());
            assertFalse(confirmedEvent.isUnconfirmed());

            verify(authenticatedUserProvider).getCurrentUser();
            verify(recurringEventBO).getRecurringEventById(VALID_RECURRING_EVENT_ID);
            verify(ownershipValidator).validateRecurringEventOwnership(viewer.getId(), recurringEvent);
            verify(recurringEventBO).confirmRecurringEventWithValidation(recurringEvent);
        }

        @Test
        void throwsWhenRecurringEventNotFound() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);

            when(recurringEventBO.getRecurringEventById(VALID_RECURRING_EVENT_ID))
                    .thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(RecurringEventNotFoundException.class,
                    () -> recurringEventService.confirmRecurringEvent(VALID_RECURRING_EVENT_ID));

            verify(authenticatedUserProvider).getCurrentUser();
            verify(recurringEventBO).getRecurringEventById(VALID_RECURRING_EVENT_ID);
            verifyNoMoreInteractions(ownershipValidator, recurringEventBO);
        }

        @Test
        void throwsWhenRecurringEventAlreadyConfirmed() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);

            RecurringEvent confirmedEvent = TestUtils.createValidRecurringEventWithId(viewer, VALID_RECURRING_EVENT_ID, fixedClock);
            confirmedEvent.setUnconfirmed(false);

            when(recurringEventBO.getRecurringEventById(VALID_RECURRING_EVENT_ID))
                    .thenReturn(Optional.of(confirmedEvent));

            doNothing().when(ownershipValidator).validateRecurringEventOwnership(viewer.getId(), confirmedEvent);

            // Act + Assert
            assertThrows(RecurringEventAlreadyConfirmedException.class,
                    () -> recurringEventService.confirmRecurringEvent(VALID_RECURRING_EVENT_ID));

            verify(authenticatedUserProvider).getCurrentUser();
            verify(recurringEventBO).getRecurringEventById(VALID_RECURRING_EVENT_ID);
            verify(ownershipValidator).validateRecurringEventOwnership(viewer.getId(), confirmedEvent);
            verifyNoMoreInteractions(recurringEventBO);
        }

        @Test
        void throwsWhenUserDoesNotOwnRecurringEvent() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);

            User otherUser = TestUtils.createValidUserEntityWithId(USER_ID_OTHER);
            RecurringEvent recurringEvent = TestUtils.createValidRecurringEventWithId(otherUser, VALID_RECURRING_EVENT_ID, fixedClock);
            recurringEvent.setUnconfirmed(true);

            when(recurringEventBO.getRecurringEventById(VALID_RECURRING_EVENT_ID))
                    .thenReturn(Optional.of(recurringEvent));

            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, viewer.getId()))
                    .when(ownershipValidator).validateRecurringEventOwnership(viewer.getId(), recurringEvent);

            // Act + Assert
            assertThrows(UserOwnershipException.class,
                    () -> recurringEventService.confirmRecurringEvent(VALID_RECURRING_EVENT_ID));

            verify(authenticatedUserProvider).getCurrentUser();
            verify(recurringEventBO).getRecurringEventById(VALID_RECURRING_EVENT_ID);
            verify(ownershipValidator).validateRecurringEventOwnership(viewer.getId(), recurringEvent);
            verifyNoMoreInteractions(recurringEventBO);
        }

    }

    @Nested
    class UpdateRecurringEventTests {

        @Test
        void updatesRecurringEventWhenPatchApplied() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            RecurringEvent existing = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    Optional.of("Updated Name"), // update name
                    null, // skip startTime
                    null, // skip endTime
                    null, // skip startDate
                    null, // skip endDate
                    null, // skip description
                    null, // skip labelId
                    null // skip recurrenceRule
            );

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(creator);
            when(recurringEventBO.getRecurringEventById(VALID_RECURRING_EVENT_ID)).thenReturn(Optional.of(existing));
            when(recurringEventPatchHandler.applyPatch(existing, dto)).thenReturn(true);
            when(recurringEventBO.updateRecurringEvent(existing)).thenReturn(existing);

            // Act
            RecurringEventResponseDTO response = recurringEventService.updateRecurringEvent(VALID_RECURRING_EVENT_ID, dto);

            // Assert
            assertNotNull(response);
            assertEquals(existing.getId(), response.id());
            verify(recurringEventBO).updateRecurringEvent(existing);
            verify(recurringEventPatchHandler).applyPatch(existing, dto);
        }

        @Test
        void doesNotUpdateWhenNoChangesApplied() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            RecurringEvent existing = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null, // skip name
                    null, // skip startTime
                    null, // skip endTime
                    null, // skip startDate
                    null, // skip endDate
                    null, // skip description
                    null, // skip labelId
                    null // skip recurrenceRule
            );

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(creator);
            when(recurringEventBO.getRecurringEventById(VALID_RECURRING_EVENT_ID)).thenReturn(Optional.of(existing));
            when(recurringEventPatchHandler.applyPatch(existing, dto)).thenReturn(false);

            // Act
            RecurringEventResponseDTO response = recurringEventService.updateRecurringEvent(VALID_RECURRING_EVENT_ID, dto);

            // Assert
            assertNotNull(response);
            assertEquals(existing.getId(), response.id());
            verify(recurringEventBO, never()).updateRecurringEvent(any());
        }

        @Test
        void throwsWhenRecurringEventNotFound() {
            // Arrange
            when(recurringEventBO.getRecurringEventById(VALID_RECURRING_EVENT_ID)).thenReturn(Optional.empty());

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    Optional.of("Attempted Update"), // attempt to update
                    null, null, null, null, null, null, null
            );

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(TestUtils.createValidUserEntityWithId());

            // Act + Assert
            assertThrows(RecurringEventNotFoundException.class, () ->
                    recurringEventService.updateRecurringEvent(VALID_RECURRING_EVENT_ID, dto));
        }

        @Test
        void throwsWhenUserNotOwner() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            RecurringEvent event = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(TestUtils.createValidUserEntityWithId());
            when(recurringEventBO.getRecurringEventById(VALID_RECURRING_EVENT_ID)).thenReturn(Optional.of(event));
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, USER_ID_OTHER))
                    .when(ownershipValidator).validateRecurringEventOwnership(anyLong(), eq(event));

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    Optional.of("Attempted Unauthorized Update"),
                    null, null, null, null, null, null, null
            );

            // Act + Assert
            assertThrows(UserOwnershipException.class, () ->
                    recurringEventService.updateRecurringEvent(VALID_RECURRING_EVENT_ID, dto));

            verify(ownershipValidator).validateRecurringEventOwnership(anyLong(), eq(event));
            verify(recurringEventBO, never()).updateRecurringEvent(any());
        }

        @Test
        void clearsFieldsWhenOptionalEmptyProvided() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            RecurringEvent existing = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    Optional.empty(), // clear name
                    Optional.empty(), // clear startTime
                    Optional.empty(), // clear endTime
                    Optional.empty(), // clear startDate
                    Optional.empty(), // clear endDate
                    Optional.empty(), // clear description
                    Optional.empty(), // clear labelId
                    Optional.empty() // clear recurrenceRule
            );

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(creator);
            when(recurringEventBO.getRecurringEventById(VALID_RECURRING_EVENT_ID)).thenReturn(Optional.of(existing));
            when(recurringEventPatchHandler.applyPatch(existing, dto)).thenReturn(true);
            when(recurringEventBO.updateRecurringEvent(existing)).thenReturn(existing);

            // Act
            RecurringEventResponseDTO response = recurringEventService.updateRecurringEvent(VALID_RECURRING_EVENT_ID, dto);

            // Assert
            assertNotNull(response);
            verify(recurringEventPatchHandler).applyPatch(existing, dto);
            verify(recurringEventBO).updateRecurringEvent(existing);
        }
    }

    @Nested
    class DeleteRecurringEventTests {

        @Test
        void deletesRecurringEventSuccessfully() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            RecurringEvent existing = TestUtils.createValidRecurringEventWithId(viewer, VALID_RECURRING_EVENT_ID, fixedClock);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(recurringEventBO.getRecurringEventById(existing.getId())).thenReturn(Optional.of(existing));

            // Act
            recurringEventService.deleteRecurringEvent(existing.getId());

            // Assert
            verify(ownershipValidator).validateRecurringEventOwnership(viewer.getId(), existing);
            verify(recurringEventBO).deleteRecurringEvent(existing.getId());
        }

        @Test
        void throwsExceptionWhenRecurringEventNotFound() {
            // Arrange
            Long nonExistentId = 999L;
            User viewer = TestUtils.createValidUserEntityWithId();

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(recurringEventBO.getRecurringEventById(nonExistentId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(RecurringEventNotFoundException.class, () -> recurringEventService.deleteRecurringEvent(nonExistentId));

            verify(recurringEventBO, never()).deleteRecurringEvent(any());
        }

        @Test
        void throwsExceptionWhenUserIsNotOwner() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            User otherUser = TestUtils.createValidUserEntityWithId(USER_ID_OTHER);
            RecurringEvent existing = TestUtils.createValidRecurringEventWithId(otherUser, VALID_RECURRING_EVENT_ID, fixedClock);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(recurringEventBO.getRecurringEventById(existing.getId())).thenReturn(Optional.of(existing));

            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, viewer.getId()))
                    .when(ownershipValidator).validateRecurringEventOwnership(viewer.getId(), existing);

            // Act + Assert
            assertThrows(UserOwnershipException.class, () -> recurringEventService.deleteRecurringEvent(existing.getId()));

            verify(recurringEventBO, never()).deleteRecurringEvent(any());
        }

    }

    @Nested
    class DeleteUnconfirmedRecurringEventsForCurrentUserTests {

        @Test
        void testDeleteUnconfirmedRecurringEventsForCurrentUser_Success() {
            // Arrange
            User user = TestUtils.createValidUserEntityWithId();
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(user);

            // Act
            recurringEventService.deleteUnconfirmedRecurringEventsForCurrentUser();

            // Assert
            verify(authenticatedUserProvider).getCurrentUser();
            verify(recurringEventBO).deleteAllUnconfirmedRecurringEventsByUser(user.getId());
        }

    }

    @Nested
    class AddSkipDaysTests {

        @Test
        void addsValidSkipDaysSuccessfully() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            RecurringEvent event = TestUtils.createValidRecurringEventWithId(viewer, VALID_RECURRING_EVENT_ID, fixedClock);
            LocalDate today = FIXED_TEST_DATE;
            Set<LocalDate> skipDays = Set.of(today.plusDays(1), today.plusDays(2));  // Valid skip days (future dates)

            // Mock the dependencies
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);  // Ensure the current user is the viewer
            when(recurringEventBO.getRecurringEventById(event.getId())).thenReturn(Optional.of(event));  // Mock the event retrieval

            // Act
            RecurringEventResponseDTO response = recurringEventService.addSkipDays(event.getId(), skipDays);

            // Assert the skip days were successfully added
            assertTrue(event.getSkipDays().containsAll(skipDays));

            // Verify the interactions
            verify(recurringEventBO).updateRecurringEvent(event);
            verify(ownershipValidator).validateRecurringEventOwnership(viewer.getId(), event);

            // Use the custom assertion to verify that the response is as expected
            RecurringEventResponseDTO expectedResponse = TestUtils.createRecurringEventResponseDTO(event);
            RecurringEventResponseDTOAssertions.assertRecurringEventResponseDTOEquals(response, expectedResponse);
        }

        @Test
        void throwsWhenRecurringEventNotFound() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);  // Mock the current user
            when(recurringEventBO.getRecurringEventById(VALID_RECURRING_EVENT_ID)).thenReturn(Optional.empty());  // Mock that the event is not found

            // Act + Assert
            RecurringEventNotFoundException exception = assertThrows(RecurringEventNotFoundException.class, () ->
                    recurringEventService.addSkipDays(VALID_RECURRING_EVENT_ID, Set.of(FIXED_TEST_DATE.plusDays(1)))
            );

            // Assert the exception message (if your exception has a message or custom code)
            assertEquals("Recurring event with ID " + VALID_RECURRING_EVENT_ID + " not found", exception.getMessage());
        }

        @Test
        void throwsWhenSkipDaysContainPastDates() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            RecurringEvent event = TestUtils.createValidRecurringEventWithId(viewer, VALID_RECURRING_EVENT_ID, fixedClock);
            LocalDate today = LocalDate.now(fixedClock);
            Set<LocalDate> skipDays = Set.of(today.minusDays(1), today.plusDays(2));  // Past date (today - 1)

            // Mock the dependencies
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);  // Ensure the current user is the viewer
            when(recurringEventBO.getRecurringEventById(event.getId())).thenReturn(Optional.of(event));  // Mock the event retrieval

            // Act + Assert
            InvalidSkipDayException ex = assertThrows(InvalidSkipDayException.class, () ->
                    recurringEventService.addSkipDays(event.getId(), skipDays)  // Adding skip days with a past date
            );

            // Assert that the exception contains the past date (invalid date)
            assertTrue(ex.getInvalidDates().contains(today.minusDays(1)), "Invalid date should be in the exception");

            // Verify that the recurring event update was never called since the skip day was invalid
            verify(recurringEventBO, never()).updateRecurringEvent(any());
        }

        @Test
        void ignoresNullDatesAndAddsValidOnes() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            RecurringEvent event = TestUtils.createValidRecurringEventWithId(viewer, VALID_RECURRING_EVENT_ID, fixedClock);
            LocalDate today = FIXED_TEST_DATE;
            Set<LocalDate> skipDays = new HashSet<>();
            skipDays.add(today.plusDays(1));  // Valid skip day
            skipDays.add(null);  // Invalid (null) skip day

            // Mock the dependencies
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);  // Ensure the current user is the viewer
            when(recurringEventBO.getRecurringEventById(event.getId())).thenReturn(Optional.of(event));  // Mock the event retrieval

            // Act + Assert
            InvalidSkipDayException ex = assertThrows(InvalidSkipDayException.class, () ->
                    recurringEventService.addSkipDays(event.getId(), skipDays)  // Adding skip days with a null value
            );

            // Assert that the exception contains the null date
            assertTrue(ex.getInvalidDates().contains(null));  // Null should be identified as an invalid date

            // Verify that updateRecurringEvent was never called since the skip day was invalid
            verify(recurringEventBO, never()).updateRecurringEvent(any());
        }

        @Test
        void callingWithEmptySkipDaysDoesNothing() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            RecurringEvent event = TestUtils.createValidRecurringEventWithId(viewer, VALID_RECURRING_EVENT_ID, fixedClock);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);  // Ensure the current user is the viewer
            when(recurringEventBO.getRecurringEventById(event.getId())).thenReturn(Optional.of(event));  // Mock the event retrieval

            // Act
            recurringEventService.addSkipDays(event.getId(), Collections.emptySet());  // Pass an empty set

            // Assert that the skip days remain empty (i.e., no change)
            assertTrue(event.getSkipDays().isEmpty(), "Skip days should remain empty when passed an empty set");

            // Verify that updateRecurringEvent was called, even though no skip days were added
            verify(recurringEventBO).updateRecurringEvent(event);
        }

    }

    @Nested
    class RemoveSkipDaysTests {

        @Test
        void removesValidFutureSkipDaysSuccessfully() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            RecurringEvent event = TestUtils.createValidRecurringEventWithId(viewer, VALID_RECURRING_EVENT_ID, fixedClock);

            LocalDate futureDate1 = LocalDate.now(fixedClock).plusDays(1);
            LocalDate futureDate2 = LocalDate.now(fixedClock).plusDays(2);
            event.addSkipDay(futureDate1);  // Add skip days to the event
            event.addSkipDay(futureDate2);

            // Mock the dependencies
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);  // Ensure the current user is the viewer
            when(recurringEventBO.getRecurringEventById(event.getId())).thenReturn(Optional.of(event));  // Mock event retrieval
            when(clockProvider.getClockForZone(ZoneId.of(event.getCreator().getTimezone()))).thenReturn(fixedClock);  // Mock clock provider

            // Mock removeSkipDaysWithConflictValidation method to simulate the removal of skip days
            Mockito.doAnswer(invocation -> {
                Set<LocalDate> skipDaysToRemove = invocation.getArgument(1); // Get skip days to remove
                skipDaysToRemove.forEach(event::removeSkipDay); // Remove them from the event
                return null; // Void method
            }).when(recurringEventBO).removeSkipDaysWithConflictValidation(eq(event), anySet());

            Set<LocalDate> toRemove = Set.of(futureDate1);  // Skip day to remove

            // Act
            recurringEventService.removeSkipDays(event.getId(), toRemove);  // Call method to remove skip days

            // Assert that the skip day was removed from the event's skip days list
            assertFalse(event.getSkipDays().contains(futureDate1), "Skip day should be removed from the event's skip days list");

            // Ensure that the skip day still exists for the other valid skip day (futureDate2)
            assertTrue(event.getSkipDays().contains(futureDate2), "Other valid skip day should remain");

            // Verify that removeSkipDaysWithConflictValidation method was called with correct arguments
            verify(recurringEventBO).removeSkipDaysWithConflictValidation(eq(event), eq(toRemove));

            // Verify that the skip day is correctly removed from the event
            assertEquals(1, event.getSkipDays().size(), "Only one skip day should remain in the event");
        }

        @Test
        void throwsWhenRecurringEventNotFound() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();  // Ensure a valid user is created
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);  // Mock the current user
            when(recurringEventBO.getRecurringEventById(VALID_RECURRING_EVENT_ID)).thenReturn(Optional.empty());  // Mock event retrieval as not found

            // Act + Assert
            assertThrows(RecurringEventNotFoundException.class, () ->
                    recurringEventService.removeSkipDays(VALID_RECURRING_EVENT_ID, Set.of(FIXED_TEST_DATE.plusDays(1)))
            );  // Assert that the exception is thrown when the event is not found

            // Verify that no update operation is performed
            verify(recurringEventBO, never()).updateRecurringEvent(any());
        }

        @Test
        void throwsWhenRemovingPastDate() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            RecurringEvent event = TestUtils.createValidRecurringEventWithId(viewer, VALID_RECURRING_EVENT_ID, fixedClock);

            LocalDate pastDate = LocalDate.now(fixedClock).minusDays(1);  // Past date to remove

            // Mock the dependencies
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);  // Ensure the current user is the viewer
            when(recurringEventBO.getRecurringEventById(event.getId())).thenReturn(Optional.of(event));  // Mock event retrieval
            when(clockProvider.getClockForZone(ZoneId.of(event.getCreator().getTimezone()))).thenReturn(fixedClock);  // Mock clock provider

            // Act + Assert
            InvalidSkipDayException ex = assertThrows(InvalidSkipDayException.class, () ->
                    recurringEventService.removeSkipDays(event.getId(), Set.of(pastDate))  // Attempt to remove a past skip day
            );

            assertTrue(ex.getInvalidDates().contains(pastDate), "The exception should contain the past date in the invalid dates set.");

            // Verify that the event retrieval happened
            verify(recurringEventBO).getRecurringEventById(event.getId());

            // Verify that the skip day removal method was NOT called (since the date is invalid)
            verify(recurringEventBO, never()).removeSkipDaysWithConflictValidation(any(), any());
        }

        @Test
        void throwsWhenNullDatesProvided() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            RecurringEvent event = TestUtils.createValidRecurringEventWithId(viewer, VALID_RECURRING_EVENT_ID, fixedClock);

            LocalDate futureDate = LocalDate.now(fixedClock).plusDays(1);
            event.addSkipDay(futureDate);  // Add valid skip day

            // Mock the dependencies
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);  // Ensure the current user is the viewer
            when(recurringEventBO.getRecurringEventById(event.getId())).thenReturn(Optional.of(event));  // Mock event retrieval
            when(clockProvider.getClockForZone(ZoneId.of(event.getCreator().getTimezone()))).thenReturn(fixedClock);  // Mock clock provider

            // Create input set with null date and valid future date
            Set<LocalDate> input = new HashSet<>();
            input.add(null);  // Invalid null date
            input.add(futureDate);  // Valid future date

            // Act + Assert
            InvalidSkipDayException ex = assertThrows(InvalidSkipDayException.class, () ->
                    recurringEventService.removeSkipDays(event.getId(), input)  // Attempt to remove skip days
            );

            assertTrue(ex.getInvalidDates().contains(null), "The exception should contain the null date in the invalid dates set.");

            // Verify that the event retrieval method was called
            verify(recurringEventBO).getRecurringEventById(event.getId());

            // Verify that the skip day removal method was NOT called (since there's a null date)
            verify(recurringEventBO, never()).removeSkipDaysWithConflictValidation(any(), any());
        }

        @Test
        void callingWithEmptySetStillCallsBOButDoesNothing() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            RecurringEvent event = TestUtils.createValidRecurringEventWithId(viewer, VALID_RECURRING_EVENT_ID, fixedClock);

            // Mock the dependencies
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);  // Ensure the current user is the viewer
            when(recurringEventBO.getRecurringEventById(event.getId())).thenReturn(Optional.of(event));  // Mock event retrieval
            when(clockProvider.getClockForZone(ZoneId.of(event.getCreator().getTimezone()))).thenReturn(fixedClock);  // Mock clock provider

            Set<LocalDate> emptySet = Collections.emptySet();  // Empty set of skip days

            // Act
            recurringEventService.removeSkipDays(event.getId(), emptySet);  // Call method with an empty set

            // Assert
            // Verify that removeSkipDaysWithConflictValidation was called with the empty set
            verify(recurringEventBO).removeSkipDaysWithConflictValidation(eq(event), eq(emptySet));
        }

        @Test
        void throwsWhenUserDoesNotOwnEvent() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            User otherUser = TestUtils.createValidUserEntityWithId(USER_ID_OTHER);  // User who owns the event

            RecurringEvent event = TestUtils.createValidRecurringEventWithId(otherUser, VALID_RECURRING_EVENT_ID, fixedClock);  // Event owned by another user

            // Mock the dependencies
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);  // Ensure the current user is the viewer
            when(recurringEventBO.getRecurringEventById(event.getId())).thenReturn(Optional.of(event));  // Mock event retrieval

            // Simulate that the viewer is not the owner of the event
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, viewer.getId()))
                    .when(ownershipValidator).validateRecurringEventOwnership(viewer.getId(), event);

            // Act + Assert
            // Assert that UserOwnershipException is thrown when trying to remove skip days from an event the user doesn't own
            assertThrows(UserOwnershipException.class, () ->
                    recurringEventService.removeSkipDays(event.getId(), Set.of(FIXED_TEST_DATE.plusDays(1)))
            );

            // Verify that the update method was not called, as the user does not own the event
            verify(recurringEventBO, never()).updateRecurringEvent(any());
        }

    }
}
