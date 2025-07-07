package com.yohan.event_planner.service;

import com.yohan.event_planner.business.EventBO;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.EventRecap;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.EventRecapCreateDTO;
import com.yohan.event_planner.dto.EventRecapResponseDTO;
import com.yohan.event_planner.dto.EventRecapUpdateDTO;
import com.yohan.event_planner.dto.RecapMediaCreateDTO;
import com.yohan.event_planner.dto.RecapMediaResponseDTO;
import com.yohan.event_planner.exception.EventNotFoundException;
import com.yohan.event_planner.exception.EventRecapAlreadyConfirmedException;
import com.yohan.event_planner.exception.EventRecapException;
import com.yohan.event_planner.exception.EventRecapNotFoundException;
import com.yohan.event_planner.exception.InvalidEventStateException;
import com.yohan.event_planner.exception.UserOwnershipException;
import com.yohan.event_planner.mapper.EventRecapMapper;
import com.yohan.event_planner.repository.EventRecapRepository;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import com.yohan.event_planner.util.EventRecapResponseDTOAssertions;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_USER_ACCESS;
import static com.yohan.event_planner.util.TestConstants.EVENT_ID;
import static com.yohan.event_planner.util.TestConstants.EVENT_RECAP_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class EventRecapServiceImplTest {

    private EventBO eventBO;
    private EventRecapRepository eventRecapRepository;
    private OwnershipValidator ownershipValidator;
    private EventRecapMapper eventRecapMapper;
    private AuthenticatedUserProvider authenticatedUserProvider;
    private RecapMediaService recapMediaService;
    private Clock fixedClock;

    private EventRecapServiceImpl eventRecapService;

    @BeforeEach
    void setUp() {
        this.eventBO = mock(EventBO.class);
        this.eventRecapRepository = mock(EventRecapRepository.class);
        this.ownershipValidator = mock(OwnershipValidator.class);
        this.eventRecapMapper = mock(EventRecapMapper.class);
        this.authenticatedUserProvider = mock(AuthenticatedUserProvider.class);
        this.recapMediaService = mock(RecapMediaService.class);

        fixedClock = Clock.fixed(Instant.parse("2025-06-29T12:00:00Z"), ZoneId.of("UTC"));

        eventRecapService = new EventRecapServiceImpl(
                eventBO,
                eventRecapRepository,
                ownershipValidator,
                eventRecapMapper,
                authenticatedUserProvider,
                recapMediaService
        );
    }

    @Nested
    class GetEventRecapTests {

        @Test
        void returnsRecapWhenExists() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_RECAP_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            var mediaDTOs = List.of(
                    TestUtils.createValidImageRecapMediaResponseDTO(),
                    TestUtils.createValidVideoRecapMediaResponseDTO()
            );
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(mediaDTOs);

            EventRecapResponseDTO expected = TestUtils.createEventRecapResponseDTO(recap, event);
            when(eventRecapMapper.toResponseDTO(recap, event, mediaDTOs)).thenReturn(expected);

            // Act
            EventRecapResponseDTO result = eventRecapService.getEventRecap(event.getId());

            // Assert
            EventRecapResponseDTOAssertions.assertEventRecapResponseDTOEquals(expected, result);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(eventBO).getEventById(event.getId());
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verify(recapMediaService).getOrderedMediaForRecap(recap.getId());
            verify(eventRecapMapper).toResponseDTO(recap, event, mediaDTOs);
        }

        @Test
        void throwsWhenRecapNotFound() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            // Act + Assert
            assertThrows(EventRecapNotFoundException.class,
                    () -> eventRecapService.getEventRecap(event.getId()));

            verify(eventBO).getEventById(event.getId());
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verifyNoInteractions(recapMediaService);
            verifyNoInteractions(eventRecapMapper);
        }

        @Test
        void throwsWhenEventNotFound() {
            // Arrange
            when(eventBO.getEventById(EVENT_ID)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(EventNotFoundException.class,
                    () -> eventRecapService.getEventRecap(EVENT_ID));

            verify(eventBO).getEventById(EVENT_ID);
            verifyNoInteractions(ownershipValidator);
            verifyNoInteractions(recapMediaService);
            verifyNoInteractions(eventRecapMapper);
        }

        @Test
        void throwsWhenUserDoesNotOwnEvent() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, viewer.getId()))
                    .when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            // Act + Assert
            assertThrows(UserOwnershipException.class,
                    () -> eventRecapService.getEventRecap(event.getId()));

            verify(eventBO).getEventById(event.getId());
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verifyNoInteractions(recapMediaService);
            verifyNoInteractions(eventRecapMapper);
        }

        @Test
        void returnsRecapWithEmptyMediaListWhenNoMedia() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_RECAP_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            var emptyMediaDTOs = List.<RecapMediaResponseDTO>of();
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(emptyMediaDTOs);

            EventRecapResponseDTO expected = TestUtils.createEventRecapResponseDTO(recap, event);
            when(eventRecapMapper.toResponseDTO(recap, event, emptyMediaDTOs)).thenReturn(expected);

            // Act
            EventRecapResponseDTO result = eventRecapService.getEventRecap(event.getId());

            // Assert
            EventRecapResponseDTOAssertions.assertEventRecapResponseDTOEquals(expected, result);
            verify(eventBO).getEventById(event.getId());
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verify(recapMediaService).getOrderedMediaForRecap(recap.getId());
            verify(eventRecapMapper).toResponseDTO(recap, event, emptyMediaDTOs);
        }

        @Test
        void returnsRecapForDraftEvent() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidFullDraftEvent(viewer, fixedClock);
            TestUtils.setEventId(event, EVENT_ID);

            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            var mediaDTOs = List.of(TestUtils.createValidImageRecapMediaResponseDTO());
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(mediaDTOs);

            EventRecapResponseDTO expected = TestUtils.createEventRecapResponseDTO(recap, event);
            when(eventRecapMapper.toResponseDTO(recap, event, mediaDTOs)).thenReturn(expected);

            // Act
            EventRecapResponseDTO result = eventRecapService.getEventRecap(event.getId());

            // Assert
            EventRecapResponseDTOAssertions.assertEventRecapResponseDTOEquals(expected, result);
            verify(eventBO).getEventById(event.getId());
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verify(recapMediaService).getOrderedMediaForRecap(recap.getId());
            verify(eventRecapMapper).toResponseDTO(recap, event, mediaDTOs);
        }

    }

    @Nested
    class AddEventRecapTests {

        @Test
        void addsConfirmedRecapForCompletedEvent() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecapCreateDTO dto = TestUtils.createValidConfirmedRecapCreateDTO();

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            // Create a recap with ID using TestUtils
            EventRecap recapWithId = TestUtils.createValidEventRecap(event);

            // Mock updateEvent to attach the recap with ID
            when(eventBO.updateEvent(null, event)).thenAnswer(invocation -> {
                event.setRecap(recapWithId);
                return event;
            });

            List<RecapMediaResponseDTO> mediaDTOs = List.of(TestUtils.createValidImageRecapMediaResponseDTO());
            when(recapMediaService.getOrderedMediaForRecap(recapWithId.getId())).thenReturn(mediaDTOs);

            EventRecapResponseDTO expected = TestUtils.createEventRecapResponseDTO(recapWithId, event);
            when(eventRecapMapper.toResponseDTO(recapWithId, event, mediaDTOs)).thenReturn(expected);

            // Act
            EventRecapResponseDTO result = eventRecapService.addEventRecap(dto);

            // Assert
            assertNotNull(result);
            EventRecapResponseDTOAssertions.assertEventRecapResponseDTOEquals(expected, result);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(eventBO).getEventById(event.getId());
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verify(eventBO).updateEvent(null, event);
            verify(recapMediaService).getOrderedMediaForRecap(recapWithId.getId());
            verify(eventRecapMapper).toResponseDTO(recapWithId, event, mediaDTOs);
        }

        @Test
        void addsDraftRecapForIncompleteEvent() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidIncompletePastEvent(viewer, fixedClock);
            TestUtils.setEventId(event, EVENT_ID);

            EventRecapCreateDTO dto = TestUtils.createValidConfirmedRecapCreateDTO(); // confirmed requested

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event)).thenReturn(event);

            when(recapMediaService.getOrderedMediaForRecap(anyLong())).thenReturn(List.of());
            when(eventRecapMapper.toResponseDTO(any(), eq(event), anyList())).thenReturn(
                    TestUtils.createEventRecapResponseDTO(TestUtils.createValidEventRecap(event), event)
            );

            // Act
            EventRecapResponseDTO result = eventRecapService.addEventRecap(dto);

            // Assert
            assertNotNull(result);
            verify(eventBO).updateEvent(null, event);
        }

        @Test
        void addsDraftRecapWhenDtoIsUnconfirmedTrue() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecapCreateDTO dto = TestUtils.createValidUnconfirmedRecapCreateDTO();

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event)).thenReturn(event);
            when(recapMediaService.getOrderedMediaForRecap(anyLong())).thenReturn(List.of());
            when(eventRecapMapper.toResponseDTO(any(), eq(event), anyList())).thenReturn(
                    TestUtils.createEventRecapResponseDTO(TestUtils.createValidEventRecap(event), event)
            );

            // Act
            EventRecapResponseDTO result = eventRecapService.addEventRecap(dto);

            // Assert
            assertNotNull(result);
            verify(eventBO).updateEvent(null, event);
        }

        @Test
        void throwsWhenRecapAlreadyExists() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap existingRecap = TestUtils.createValidEventRecap(event);
            event.setRecap(existingRecap);

            EventRecapCreateDTO dto = TestUtils.createValidConfirmedRecapCreateDTO();

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            // Act + Assert
            assertThrows(EventRecapException.class, () -> eventRecapService.addEventRecap(dto));
            verify(eventBO).getEventById(event.getId());
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verifyNoMoreInteractions(eventBO);
            verifyNoInteractions(recapMediaService);
            verifyNoInteractions(eventRecapMapper);
        }

        @Test
        void throwsWhenEventNotFound() {
            // Arrange
            EventRecapCreateDTO dto = TestUtils.createValidConfirmedRecapCreateDTO();
            when(eventBO.getEventById(dto.eventId())).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(EventNotFoundException.class, () -> eventRecapService.addEventRecap(dto));
            verify(eventBO).getEventById(dto.eventId());
            verifyNoInteractions(recapMediaService);
            verifyNoInteractions(eventRecapMapper);
        }

        @Test
        void throwsWhenUserDoesNotOwnEvent() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecapCreateDTO dto = TestUtils.createValidConfirmedRecapCreateDTO();

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, viewer.getId()))
                    .when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            // Act + Assert
            assertThrows(UserOwnershipException.class, () -> eventRecapService.addEventRecap(dto));
            verify(eventBO).getEventById(event.getId());
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verifyNoMoreInteractions(eventBO);
            verifyNoInteractions(recapMediaService);
            verifyNoInteractions(eventRecapMapper);
        }

        @Test
        void addsRecapWithoutMedia() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, "notes", "recapName", false, null);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event)).thenReturn(event);
            when(recapMediaService.getOrderedMediaForRecap(anyLong())).thenReturn(List.of());
            when(eventRecapMapper.toResponseDTO(any(), eq(event), anyList())).thenReturn(
                    TestUtils.createEventRecapResponseDTO(TestUtils.createValidEventRecap(event), event)
            );

            // Act
            EventRecapResponseDTO result = eventRecapService.addEventRecap(dto);

            // Assert
            assertNotNull(result);
            verify(recapMediaService, never()).addMediaItemsToRecap(any(), any());
        }

        @Test
        void addsRecapWithMedia() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecapCreateDTO dto = TestUtils.createValidConfirmedRecapCreateDTO();

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event)).thenReturn(event);
            when(recapMediaService.getOrderedMediaForRecap(anyLong())).thenReturn(List.of());
            when(eventRecapMapper.toResponseDTO(any(), eq(event), anyList())).thenReturn(
                    TestUtils.createEventRecapResponseDTO(TestUtils.createValidEventRecap(event), event)
            );

            // Act
            EventRecapResponseDTO result = eventRecapService.addEventRecap(dto);

            // Assert
            assertNotNull(result);
            verify(recapMediaService).addMediaItemsToRecap(any(), eq(dto.media()));
        }

        @Test
        void addsRecapWithEmptyMediaListDoesNotPersistMedia() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, "notes", "recapName", false, List.of()); // empty list

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event)).thenReturn(event);
            when(recapMediaService.getOrderedMediaForRecap(anyLong())).thenReturn(List.of());
            when(eventRecapMapper.toResponseDTO(any(), eq(event), anyList())).thenReturn(
                    TestUtils.createEventRecapResponseDTO(TestUtils.createValidEventRecap(event), event)
            );

            // Act
            EventRecapResponseDTO result = eventRecapService.addEventRecap(dto);

            // Assert
            assertNotNull(result);
            verify(recapMediaService, never()).addMediaItemsToRecap(any(), any());
        }

    }

    @Nested
    class ConfirmEventRecapTests {

        @Test
        void confirmsUnconfirmedRecapSuccessfully() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidUnconfirmedEventRecap(event);
            event.setRecap(recap);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventRecapRepository.save(recap)).thenReturn(recap);

            List<RecapMediaResponseDTO> mediaDTOs = List.of(TestUtils.createValidImageRecapMediaResponseDTO());
            when(recapMediaService.getOrderedMediaForRecap(EVENT_RECAP_ID)).thenReturn(mediaDTOs);

            EventRecapResponseDTO expected = TestUtils.createEventRecapResponseDTO(recap, event);
            when(eventRecapMapper.toResponseDTO(recap, event, mediaDTOs)).thenReturn(expected);

            // Act
            EventRecapResponseDTO result = eventRecapService.confirmEventRecap(event.getId());

            // Assert
            assertNotNull(result);
            EventRecapResponseDTOAssertions.assertEventRecapResponseDTOEquals(expected, result);
            verify(eventRecapRepository).save(recap);
            verify(recapMediaService).getOrderedMediaForRecap(EVENT_RECAP_ID);
            verify(eventRecapMapper).toResponseDTO(recap, event, mediaDTOs);
        }

        @Test
        void throwsWhenEventNotCompleted() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidIncompletePastEvent(viewer, fixedClock);
            TestUtils.setEventId(event, EVENT_ID);
            EventRecap recap = TestUtils.createValidUnconfirmedEventRecap(event);
            event.setRecap(recap);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            // Act + Assert
            assertThrows(InvalidEventStateException.class, () -> eventRecapService.confirmEventRecap(event.getId()));
            verifyNoInteractions(eventRecapRepository, recapMediaService, eventRecapMapper);
        }

        @Test
        void throwsWhenNoRecapExists() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            event.setRecap(null);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            // Act + Assert
            assertThrows(EventRecapNotFoundException.class, () -> eventRecapService.confirmEventRecap(event.getId()));
            verifyNoInteractions(eventRecapRepository, recapMediaService, eventRecapMapper);
        }

        @Test
        void throwsWhenRecapAlreadyConfirmed() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event); // already confirmed recap
            event.setRecap(recap);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            // Act + Assert
            assertThrows(EventRecapAlreadyConfirmedException.class, () -> eventRecapService.confirmEventRecap(event.getId()));
            verifyNoInteractions(eventRecapRepository, recapMediaService, eventRecapMapper);
        }

        @Test
        void throwsWhenEventNotFound() {
            // Arrange
            when(eventBO.getEventById(EVENT_ID)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(EventNotFoundException.class, () -> eventRecapService.confirmEventRecap(EVENT_ID));
            verifyNoInteractions(eventRecapRepository, recapMediaService, eventRecapMapper);
        }

        @Test
        void throwsWhenUserDoesNotOwnEvent() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidUnconfirmedEventRecap(event);
            event.setRecap(recap);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, viewer.getId()))
                    .when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            // Act + Assert
            assertThrows(UserOwnershipException.class, () -> eventRecapService.confirmEventRecap(event.getId()));
            verifyNoInteractions(eventRecapRepository, recapMediaService, eventRecapMapper);
        }

    }

    @Nested
    class UpdateEventRecapTests {

        @Test
        void updatesNotesOnly() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            String updatedNotes = "Updated notes";
            EventRecapUpdateDTO dto = new EventRecapUpdateDTO(updatedNotes, null);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventRecapRepository.save(recap)).thenReturn(recap);

            List<RecapMediaResponseDTO> mediaDTOs = List.of(TestUtils.createValidImageRecapMediaResponseDTO());
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(mediaDTOs);

            EventRecapResponseDTO expected = TestUtils.createEventRecapResponseDTO(recap, event);
            when(eventRecapMapper.toResponseDTO(recap, event, mediaDTOs)).thenReturn(expected);

            // Act
            EventRecapResponseDTO result = eventRecapService.updateEventRecap(event.getId(), dto);

            // Assert
            assertNotNull(result);
            assertEquals(updatedNotes, recap.getNotes());
            verify(eventRecapRepository).save(recap);
            verify(recapMediaService).getOrderedMediaForRecap(recap.getId());
            verify(eventRecapMapper).toResponseDTO(recap, event, mediaDTOs);
        }

        @Test
        void updatesMediaOnly() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            List<RecapMediaCreateDTO> newMedia = List.of(
                    TestUtils.createValidImageRecapMediaCreateDTO(),
                    TestUtils.createValidVideoRecapMediaCreateDTO()
            );
            EventRecapUpdateDTO dto = new EventRecapUpdateDTO(null, newMedia);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventRecapRepository.save(recap)).thenReturn(recap);

            List<RecapMediaResponseDTO> mediaDTOs = List.of(TestUtils.createValidImageRecapMediaResponseDTO());
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(mediaDTOs);

            EventRecapResponseDTO expected = TestUtils.createEventRecapResponseDTO(recap, event);
            when(eventRecapMapper.toResponseDTO(recap, event, mediaDTOs)).thenReturn(expected);

            // Act
            EventRecapResponseDTO result = eventRecapService.updateEventRecap(event.getId(), dto);

            // Assert
            assertNotNull(result);
            verify(recapMediaService).replaceRecapMedia(recap, newMedia);
            verify(eventRecapRepository).save(recap);
            verify(eventRecapMapper).toResponseDTO(recap, event, mediaDTOs);
        }

        @Test
        void updatesNotesAndMedia() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            String updatedNotes = "Updated notes";
            List<RecapMediaCreateDTO> newMedia = List.of(
                    TestUtils.createValidImageRecapMediaCreateDTO(),
                    TestUtils.createValidVideoRecapMediaCreateDTO()
            );
            EventRecapUpdateDTO dto = new EventRecapUpdateDTO(updatedNotes, newMedia);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventRecapRepository.save(recap)).thenReturn(recap);

            List<RecapMediaResponseDTO> mediaDTOs = List.of(TestUtils.createValidImageRecapMediaResponseDTO());
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(mediaDTOs);

            EventRecapResponseDTO expected = TestUtils.createEventRecapResponseDTO(recap, event);
            when(eventRecapMapper.toResponseDTO(recap, event, mediaDTOs)).thenReturn(expected);

            // Act
            EventRecapResponseDTO result = eventRecapService.updateEventRecap(event.getId(), dto);

            // Assert
            assertNotNull(result);
            assertEquals(updatedNotes, recap.getNotes());
            verify(recapMediaService).replaceRecapMedia(recap, newMedia);
            verify(eventRecapRepository).save(recap);
            verify(eventRecapMapper).toResponseDTO(recap, event, mediaDTOs);
        }

        @Test
        void noChangesProvidedDoesNotSave() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            EventRecapUpdateDTO dto = new EventRecapUpdateDTO(null, null);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            List<RecapMediaResponseDTO> mediaDTOs = List.of(TestUtils.createValidImageRecapMediaResponseDTO());
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(mediaDTOs);

            EventRecapResponseDTO expected = TestUtils.createEventRecapResponseDTO(recap, event);
            when(eventRecapMapper.toResponseDTO(recap, event, mediaDTOs)).thenReturn(expected);

            // Act
            EventRecapResponseDTO result = eventRecapService.updateEventRecap(event.getId(), dto);

            // Assert
            assertNotNull(result);
            verify(eventRecapRepository, never()).save(any());
            verify(eventRecapMapper).toResponseDTO(recap, event, mediaDTOs);
        }

        @Test
        void throwsWhenNoRecapExists() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            event.setRecap(null);

            EventRecapUpdateDTO dto = new EventRecapUpdateDTO(
                    "notes",
                    List.of(
                            TestUtils.createValidImageRecapMediaCreateDTO(),
                            TestUtils.createValidVideoRecapMediaCreateDTO()
                    )
            );

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            // Act + Assert
            assertThrows(EventRecapNotFoundException.class, () -> eventRecapService.updateEventRecap(event.getId(), dto));
            verifyNoInteractions(eventRecapRepository, recapMediaService, eventRecapMapper);
        }

        @Test
        void throwsWhenEventNotFound() {
            // Arrange
            EventRecapUpdateDTO dto = new EventRecapUpdateDTO(
                    "notes",
                    List.of(
                            TestUtils.createValidImageRecapMediaCreateDTO(),
                            TestUtils.createValidVideoRecapMediaCreateDTO()
                    )
            );
            when(eventBO.getEventById(EVENT_ID)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(EventNotFoundException.class, () -> eventRecapService.updateEventRecap(EVENT_ID, dto));
            verifyNoInteractions(eventRecapRepository, recapMediaService, eventRecapMapper);
        }

        @Test
        void throwsWhenUserDoesNotOwnEvent() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            EventRecapUpdateDTO dto = new EventRecapUpdateDTO(
                    "notes",
                    List.of(
                            TestUtils.createValidImageRecapMediaCreateDTO(),
                            TestUtils.createValidVideoRecapMediaCreateDTO()
                    )
            );

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, viewer.getId()))
                    .when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            // Act + Assert
            assertThrows(UserOwnershipException.class, () -> eventRecapService.updateEventRecap(event.getId(), dto));
            verifyNoInteractions(eventRecapRepository, recapMediaService, eventRecapMapper);
        }

        @Test
        void updatesNotesToSameValueDoesNotSave() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            String sameNotes = recap.getNotes();
            EventRecapUpdateDTO dto = new EventRecapUpdateDTO(sameNotes, null);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            List<RecapMediaResponseDTO> mediaDTOs = List.of(TestUtils.createValidImageRecapMediaResponseDTO());
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(mediaDTOs);

            EventRecapResponseDTO expected = TestUtils.createEventRecapResponseDTO(recap, event);
            when(eventRecapMapper.toResponseDTO(recap, event, mediaDTOs)).thenReturn(expected);

            // Act
            EventRecapResponseDTO result = eventRecapService.updateEventRecap(event.getId(), dto);

            // Assert
            assertNotNull(result);
            verify(eventRecapRepository, never()).save(any());
            verify(recapMediaService).getOrderedMediaForRecap(recap.getId());
            verify(eventRecapMapper).toResponseDTO(recap, event, mediaDTOs);
        }

        @Test
        void updatesMediaWithEmptyList() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            List<RecapMediaCreateDTO> emptyMediaList = List.of();
            EventRecapUpdateDTO dto = new EventRecapUpdateDTO(null, emptyMediaList);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventRecapRepository.save(recap)).thenReturn(recap);

            List<RecapMediaResponseDTO> mediaDTOs = List.of(); // response after replacement
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(mediaDTOs);

            EventRecapResponseDTO expected = TestUtils.createEventRecapResponseDTO(recap, event);
            when(eventRecapMapper.toResponseDTO(recap, event, mediaDTOs)).thenReturn(expected);

            // Act
            EventRecapResponseDTO result = eventRecapService.updateEventRecap(event.getId(), dto);

            // Assert
            assertNotNull(result);
            verify(recapMediaService).replaceRecapMedia(recap, emptyMediaList);
            verify(eventRecapRepository).save(recap);
            verify(recapMediaService).getOrderedMediaForRecap(recap.getId());
            verify(eventRecapMapper).toResponseDTO(recap, event, mediaDTOs);
        }

    }

    @Nested
    class DeleteEventRecapTests {

        @Test
        void deletesRecapSuccessfully() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            doNothing().when(recapMediaService).deleteAllMediaForRecap(recap.getId());
            when(eventBO.updateEvent(null, event)).thenReturn(event);

            // Act
            eventRecapService.deleteEventRecap(event.getId());

            // Assert
            assertNull(event.getRecap());
            verify(recapMediaService).deleteAllMediaForRecap(recap.getId());
            verify(eventBO).updateEvent(null, event);
        }

        @Test
        void throwsWhenNoRecapExists() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            event.setRecap(null);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            // Act + Assert
            assertThrows(EventRecapNotFoundException.class, () -> eventRecapService.deleteEventRecap(event.getId()));
            verify(recapMediaService, never()).deleteAllMediaForRecap(anyLong());
            verify(eventBO, never()).updateEvent(any(), any());
        }

        @Test
        void throwsWhenEventNotFound() {
            // Arrange
            when(eventBO.getEventById(EVENT_ID)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(EventNotFoundException.class, () -> eventRecapService.deleteEventRecap(EVENT_ID));
            verify(eventBO).getEventById(EVENT_ID);
            verifyNoInteractions(recapMediaService);
        }

        @Test
        void throwsWhenUserDoesNotOwnEvent() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, viewer.getId()))
                    .when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            // Act + Assert
            assertThrows(UserOwnershipException.class, () -> eventRecapService.deleteEventRecap(event.getId()));
            verifyNoInteractions(recapMediaService);
            verify(eventBO, never()).updateEvent(any(), any());
        }

    }

}
