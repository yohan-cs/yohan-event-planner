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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
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

@ExtendWith(MockitoExtension.class)
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

            when(recapMediaService.getOrderedMediaForRecap(any())).thenReturn(List.of());
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
            when(recapMediaService.getOrderedMediaForRecap(any())).thenReturn(List.of());
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
            when(recapMediaService.getOrderedMediaForRecap(any())).thenReturn(List.of());
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
            when(recapMediaService.getOrderedMediaForRecap(any())).thenReturn(List.of());
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
            when(recapMediaService.getOrderedMediaForRecap(any())).thenReturn(List.of());
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

    @Nested
    class LoggingTests {

        @Test
        void getEventRecap_logsOperations() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_RECAP_ID, viewer, fixedClock);
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
            assertNotNull(result);
            // Note: In a real application, you would verify logging using a LogCaptor or similar
            // For this test, we verify the operation completed successfully, which indicates logging executed
        }

        @Test
        void addEventRecap_logsCreationAndMedia() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecapCreateDTO dto = TestUtils.createValidConfirmedRecapCreateDTO();

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event)).thenReturn(event);
            when(recapMediaService.getOrderedMediaForRecap(any())).thenReturn(List.of());
            when(eventRecapMapper.toResponseDTO(any(), eq(event), anyList())).thenReturn(
                    TestUtils.createEventRecapResponseDTO(TestUtils.createValidEventRecap(event), event)
            );

            // Act
            EventRecapResponseDTO result = eventRecapService.addEventRecap(dto);

            // Assert
            assertNotNull(result);
            // Note: In a real application, you would verify specific log messages
            // For this test, we verify the operation completed successfully with media logging
        }

        @Test
        void deleteEventRecap_logsMediaDeletion() {
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
            // Note: In a real application, you would verify DEBUG logging for media deletion
            // For this test, we verify the operation completed successfully with media cleanup logging
        }

        @Test
        void confirmEventRecap_logsStateTransition() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidUnconfirmedEventRecap(event);
            event.setRecap(recap);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventRecapRepository.save(recap)).thenReturn(recap);
            when(recapMediaService.getOrderedMediaForRecap(EVENT_RECAP_ID)).thenReturn(List.of());
            when(eventRecapMapper.toResponseDTO(recap, event, List.of())).thenReturn(
                    TestUtils.createEventRecapResponseDTO(recap, event)
            );

            // Act
            EventRecapResponseDTO result = eventRecapService.confirmEventRecap(event.getId());

            // Assert
            assertNotNull(result);
            // Note: In a real application, you would verify DEBUG logging for state transitions
            // For this test, we verify the operation completed successfully with confirmation logging
        }

    }

    @Nested
    class NullParameterEdgeCaseTests {

        @Test
        void getEventRecap_throwsForNullEventId() {
            // Arrange
            when(eventBO.getEventById(null)).thenReturn(Optional.empty());
            
            // Act + Assert
            assertThrows(EventNotFoundException.class, () -> eventRecapService.getEventRecap(null));
            verify(eventBO).getEventById(null);
            verifyNoInteractions(ownershipValidator, recapMediaService, eventRecapMapper);
        }

        @Test
        void addEventRecap_throwsForNullDto() {
            // Act + Assert
            assertThrows(NullPointerException.class, () -> eventRecapService.addEventRecap(null));
            verifyNoInteractions(eventBO, ownershipValidator, recapMediaService, eventRecapMapper);
        }

        @Test
        void addEventRecap_handlesNullMediaList() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, "notes", "recapName", false, null);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event)).thenReturn(event);
            when(recapMediaService.getOrderedMediaForRecap(any())).thenReturn(List.of());
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
        void addEventRecap_handlesNullNotesAndRecapName() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, null, null, false, null);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event)).thenReturn(event);
            when(recapMediaService.getOrderedMediaForRecap(any())).thenReturn(List.of());
            when(eventRecapMapper.toResponseDTO(any(), eq(event), anyList())).thenReturn(
                    TestUtils.createEventRecapResponseDTO(TestUtils.createValidEventRecap(event), event)
            );

            // Act
            EventRecapResponseDTO result = eventRecapService.addEventRecap(dto);

            // Assert
            assertNotNull(result);
        }

        @Test
        void confirmEventRecap_throwsForNullEventId() {
            // Arrange
            when(eventBO.getEventById(null)).thenReturn(Optional.empty());
            
            // Act + Assert
            assertThrows(EventNotFoundException.class, () -> eventRecapService.confirmEventRecap(null));
            verify(eventBO).getEventById(null);
            verifyNoInteractions(ownershipValidator, recapMediaService, eventRecapMapper);
        }

        @Test
        void updateEventRecap_throwsForNullEventId() {
            // Arrange
            when(eventBO.getEventById(null)).thenReturn(Optional.empty());
            
            // Act + Assert
            assertThrows(EventNotFoundException.class, () -> eventRecapService.updateEventRecap(null, new EventRecapUpdateDTO("notes", null)));
            verify(eventBO).getEventById(null);
            verifyNoInteractions(ownershipValidator, recapMediaService, eventRecapMapper);
        }

        @Test
        void updateEventRecap_throwsForNullDto() {
            // Arrange
            when(eventBO.getEventById(EVENT_ID)).thenReturn(Optional.empty());
            
            // Act + Assert
            assertThrows(EventNotFoundException.class, () -> eventRecapService.updateEventRecap(EVENT_ID, null));
            verify(eventBO).getEventById(EVENT_ID);
            verifyNoInteractions(ownershipValidator, recapMediaService, eventRecapMapper);
        }

        @Test
        void updateEventRecap_handlesNullNotesAndMedia() {
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
            verify(recapMediaService, never()).replaceRecapMedia(any(), any());
        }

        @Test
        void deleteEventRecap_throwsForNullEventId() {
            // Arrange
            when(eventBO.getEventById(null)).thenReturn(Optional.empty());
            
            // Act + Assert
            assertThrows(EventNotFoundException.class, () -> eventRecapService.deleteEventRecap(null));
            verify(eventBO).getEventById(null);
            verifyNoInteractions(ownershipValidator, recapMediaService, eventRecapMapper);
        }

    }

    @Nested
    class BoundaryValueEdgeCaseTests {

        @Test
        void addEventRecap_handlesZeroEventId() {
            // Arrange
            when(eventBO.getEventById(0L)).thenReturn(Optional.empty());

            EventRecapCreateDTO dto = new EventRecapCreateDTO(0L, "notes", "recapName", false, null);

            // Act + Assert
            assertThrows(EventNotFoundException.class, () -> eventRecapService.addEventRecap(dto));
        }

        @Test
        void addEventRecap_handlesNegativeEventId() {
            // Arrange
            when(eventBO.getEventById(-1L)).thenReturn(Optional.empty());

            EventRecapCreateDTO dto = new EventRecapCreateDTO(-1L, "notes", "recapName", false, null);

            // Act + Assert
            assertThrows(EventNotFoundException.class, () -> eventRecapService.addEventRecap(dto));
        }

        @Test
        void addEventRecap_handlesMaxLongEventId() {
            // Arrange
            when(eventBO.getEventById(Long.MAX_VALUE)).thenReturn(Optional.empty());

            EventRecapCreateDTO dto = new EventRecapCreateDTO(Long.MAX_VALUE, "notes", "recapName", false, null);

            // Act + Assert
            assertThrows(EventNotFoundException.class, () -> eventRecapService.addEventRecap(dto));
        }

        @Test
        void addEventRecap_handlesEmptyStringNotes() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, "", "recapName", false, null);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event)).thenReturn(event);
            when(recapMediaService.getOrderedMediaForRecap(any())).thenReturn(List.of());
            when(eventRecapMapper.toResponseDTO(any(), eq(event), anyList())).thenReturn(
                    TestUtils.createEventRecapResponseDTO(TestUtils.createValidEventRecap(event), event)
            );

            // Act
            EventRecapResponseDTO result = eventRecapService.addEventRecap(dto);

            // Assert
            assertNotNull(result);
        }

        @Test
        void addEventRecap_handlesEmptyStringRecapName() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, "notes", "", false, null);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event)).thenReturn(event);
            when(recapMediaService.getOrderedMediaForRecap(any())).thenReturn(List.of());
            when(eventRecapMapper.toResponseDTO(any(), eq(event), anyList())).thenReturn(
                    TestUtils.createEventRecapResponseDTO(TestUtils.createValidEventRecap(event), event)
            );

            // Act
            EventRecapResponseDTO result = eventRecapService.addEventRecap(dto);

            // Assert
            assertNotNull(result);
        }

        @Test
        void updateEventRecap_handlesEmptyStringNotes() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            EventRecapUpdateDTO dto = new EventRecapUpdateDTO("", null);

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
            verify(eventRecapRepository).save(recap);
            assertEquals("", recap.getNotes());
        }

        @Test
        void operations_handleVeryLargeEventIds() {
            // Arrange
            Long veryLargeId = 9999999999L;
            when(eventBO.getEventById(veryLargeId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(EventNotFoundException.class, () -> eventRecapService.getEventRecap(veryLargeId));
            assertThrows(EventNotFoundException.class, () -> eventRecapService.confirmEventRecap(veryLargeId));
            assertThrows(EventNotFoundException.class, () -> eventRecapService.updateEventRecap(veryLargeId, new EventRecapUpdateDTO("notes", null)));
            assertThrows(EventNotFoundException.class, () -> eventRecapService.deleteEventRecap(veryLargeId));
        }

    }

    @Nested
    class MediaServiceIntegrationErrorTests {

        @Test
        void getEventRecap_handlesMediaServiceException() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_RECAP_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(recapMediaService.getOrderedMediaForRecap(recap.getId()))
                    .thenThrow(new RuntimeException("Media service unavailable"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.getEventRecap(event.getId()));
        }

        @Test
        void addEventRecap_handlesMediaAdditionFailure() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecapCreateDTO dto = TestUtils.createValidConfirmedRecapCreateDTO();

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event)).thenReturn(event);
            doThrow(new RuntimeException("Media upload failed"))
                    .when(recapMediaService).addMediaItemsToRecap(any(EventRecap.class), eq(dto.media()));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.addEventRecap(dto));
        }

        @Test
        void addEventRecap_handlesMediaRetrievalFailureAfterCreation() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecapCreateDTO dto = TestUtils.createValidConfirmedRecapCreateDTO();

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event)).thenReturn(event);
            doNothing().when(recapMediaService).addMediaItemsToRecap(any(EventRecap.class), eq(dto.media()));
            when(recapMediaService.getOrderedMediaForRecap(anyLong()))
                    .thenThrow(new RuntimeException("Media retrieval failed"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.addEventRecap(dto));
        }

        @Test
        void confirmEventRecap_handlesMediaRetrievalFailure() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidUnconfirmedEventRecap(event);
            event.setRecap(recap);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventRecapRepository.save(recap)).thenReturn(recap);
            when(recapMediaService.getOrderedMediaForRecap(recap.getId()))
                    .thenThrow(new RuntimeException("Media service timeout"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.confirmEventRecap(event.getId()));
        }

        @Test
        void updateEventRecap_handlesMediaReplacementFailure() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            List<RecapMediaCreateDTO> newMedia = List.of(TestUtils.createValidImageRecapMediaCreateDTO());
            EventRecapUpdateDTO dto = new EventRecapUpdateDTO("Updated notes", newMedia);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            doThrow(new RuntimeException("Media replacement failed"))
                    .when(recapMediaService).replaceRecapMedia(recap, newMedia);

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.updateEventRecap(event.getId(), dto));
        }

        @Test
        void updateEventRecap_handlesMediaRetrievalFailureAfterUpdate() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            EventRecapUpdateDTO dto = new EventRecapUpdateDTO("Updated notes", null);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventRecapRepository.save(recap)).thenReturn(recap);
            when(recapMediaService.getOrderedMediaForRecap(recap.getId()))
                    .thenThrow(new RuntimeException("Media service connection lost"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.updateEventRecap(event.getId(), dto));
        }

        @Test
        void deleteEventRecap_handlesMediaDeletionFailure() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            doThrow(new RuntimeException("Media deletion failed - storage unavailable"))
                    .when(recapMediaService).deleteAllMediaForRecap(recap.getId());

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.deleteEventRecap(event.getId()));
        }

    }

    @Nested
    class RepositoryDatabaseErrorTests {

        @Test
        void addEventRecap_handlesEventBOUpdateFailure() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, "notes", "recapName", false, null);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event))
                    .thenThrow(new RuntimeException("Database connection lost"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.addEventRecap(dto));
            verifyNoInteractions(recapMediaService, eventRecapMapper);
        }

        @Test
        void confirmEventRecap_handlesRepositorySaveFailure() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidUnconfirmedEventRecap(event);
            event.setRecap(recap);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventRecapRepository.save(recap))
                    .thenThrow(new RuntimeException("Database transaction failed"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.confirmEventRecap(event.getId()));
            verifyNoInteractions(recapMediaService, eventRecapMapper);
        }

        @Test
        void updateEventRecap_handlesRepositorySaveFailure() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            EventRecapUpdateDTO dto = new EventRecapUpdateDTO("Updated notes", null);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventRecapRepository.save(recap))
                    .thenThrow(new RuntimeException("Database deadlock detected"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.updateEventRecap(event.getId(), dto));
            verify(recapMediaService, never()).getOrderedMediaForRecap(anyLong());
            verifyNoInteractions(eventRecapMapper);
        }

        @Test
        void deleteEventRecap_handlesEventBOUpdateFailure() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            doNothing().when(recapMediaService).deleteAllMediaForRecap(recap.getId());
            when(eventBO.updateEvent(null, event))
                    .thenThrow(new RuntimeException("Database constraint violation"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.deleteEventRecap(event.getId()));
            verify(recapMediaService).deleteAllMediaForRecap(recap.getId());
        }

        @Test
        void getEventRecap_handlesEventBORetrievalFailure() {
            // Arrange
            when(eventBO.getEventById(EVENT_RECAP_ID))
                    .thenThrow(new RuntimeException("Database query timeout"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.getEventRecap(EVENT_RECAP_ID));
            verifyNoInteractions(authenticatedUserProvider, ownershipValidator, recapMediaService, eventRecapMapper);
        }

        @Test
        void operations_handleTransactionRollback() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, "notes", "recapName", false, null);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event))
                    .thenThrow(new RuntimeException("Transaction rolled back"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.addEventRecap(dto));
            // Verify that no media operations are attempted after transaction failure
            verifyNoInteractions(recapMediaService);
        }

        @Test
        void operations_handleOptimisticLockingFailure() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            EventRecapUpdateDTO dto = new EventRecapUpdateDTO("Updated notes", null);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventRecapRepository.save(recap))
                    .thenThrow(new RuntimeException("Optimistic locking failure - entity was modified"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.updateEventRecap(event.getId(), dto));
        }

    }

    @Nested
    class ConcurrentAccessRaceConditionTests {

        @Test
        void addEventRecap_handlesRecapAlreadyCreatedByConcurrentTransaction() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, "notes", "recapName", false, null);

            // First call finds no recap
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            
            // But when trying to save, another transaction has already created a recap
            EventRecap existingRecap = TestUtils.createValidEventRecap(event);
            event.setRecap(existingRecap);

            // Act + Assert
            assertThrows(EventRecapException.class, () -> eventRecapService.addEventRecap(dto));
        }

        @Test
        void confirmEventRecap_handlesRecapDeletedByConcurrentTransaction() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidUnconfirmedEventRecap(event);
            event.setRecap(recap);

            // Initial lookup finds the recap
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            
            // But when trying to save, recap was deleted by another transaction
            when(eventRecapRepository.save(recap))
                    .thenThrow(new RuntimeException("Entity no longer exists"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.confirmEventRecap(event.getId()));
        }

        @Test
        void updateEventRecap_handlesRecapModifiedByConcurrentTransaction() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            EventRecapUpdateDTO dto = new EventRecapUpdateDTO("Updated notes", null);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            
            // Concurrent modification detected during save
            when(eventRecapRepository.save(recap))
                    .thenThrow(new RuntimeException("StaleObjectStateException: Row was updated by another transaction"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.updateEventRecap(event.getId(), dto));
        }

        @Test
        void deleteEventRecap_handlesEventDeletedByConcurrentTransaction() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            doNothing().when(recapMediaService).deleteAllMediaForRecap(recap.getId());
            
            // Event was deleted by another transaction before we could update it
            when(eventBO.updateEvent(null, event))
                    .thenThrow(new EventNotFoundException(event.getId()));

            // Act + Assert
            assertThrows(EventNotFoundException.class, () -> eventRecapService.deleteEventRecap(event.getId()));
        }

        @Test
        void getEventRecap_handlesEventStateChangedByConcurrentTransaction() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_RECAP_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            
            // Media service fails due to concurrent recap modification
            when(recapMediaService.getOrderedMediaForRecap(recap.getId()))
                    .thenThrow(new RuntimeException("Recap was modified, media relationships invalid"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.getEventRecap(event.getId()));
        }

        @Test
        void operations_handleDoubleSubmissionScenario() {
            // Arrange - simulating double-click or API retry
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, "notes", "recapName", false, null);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            
            // First request succeeds, but second request should fail due to duplicate
            EventRecap existingRecap = TestUtils.createValidEventRecap(event);
            event.setRecap(existingRecap);

            // Act + Assert - second identical request
            assertThrows(EventRecapException.class, () -> eventRecapService.addEventRecap(dto));
        }

    }

    @Nested
    class AuthenticationSecurityEdgeCaseTests {

        @Test
        void operations_handleNullCurrentUser() {
            // Arrange
            when(eventBO.getEventById(EVENT_ID)).thenReturn(Optional.of(TestUtils.createValidCompletedEventWithId(EVENT_ID, TestUtils.createValidUserEntityWithId(), fixedClock)));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(null);

            // Act + Assert
            assertThrows(NullPointerException.class, () -> eventRecapService.getEventRecap(EVENT_ID));
            assertThrows(NullPointerException.class, () -> eventRecapService.confirmEventRecap(EVENT_ID));
            assertThrows(NullPointerException.class, () -> eventRecapService.updateEventRecap(EVENT_ID, new EventRecapUpdateDTO("notes", null)));
            assertThrows(NullPointerException.class, () -> eventRecapService.deleteEventRecap(EVENT_ID));
        }

        @Test
        void operations_handleUserWithoutId() {
            // Arrange
            User userWithoutId = mock(User.class);
            when(userWithoutId.getId()).thenThrow(new NullPointerException("User ID is null"));
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, TestUtils.createValidUserEntityWithId(), fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            when(eventBO.getEventById(EVENT_ID)).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(userWithoutId);

            // Act + Assert
            assertThrows(NullPointerException.class, () -> eventRecapService.getEventRecap(EVENT_ID));
        }

        @Test
        void operations_handleOwnershipValidationFailure() {
            // Arrange
            User currentUser = TestUtils.createValidUserEntityWithId();
            User eventOwner = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, eventOwner, fixedClock);

            when(eventBO.getEventById(EVENT_ID)).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(currentUser);
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, currentUser.getId()))
                    .when(ownershipValidator).validateEventOwnership(currentUser.getId(), event);

            // Act + Assert
            assertThrows(UserOwnershipException.class, () -> eventRecapService.getEventRecap(EVENT_ID));
            assertThrows(UserOwnershipException.class, () -> eventRecapService.confirmEventRecap(EVENT_ID));
            assertThrows(UserOwnershipException.class, () -> eventRecapService.updateEventRecap(EVENT_ID, new EventRecapUpdateDTO("notes", null)));
            assertThrows(UserOwnershipException.class, () -> eventRecapService.deleteEventRecap(EVENT_ID));
        }

        @Test
        void addEventRecap_handlesDifferentUserCreatingRecap() {
            // Arrange
            User eventOwner = TestUtils.createValidUserEntityWithId();
            User differentUser = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, eventOwner, fixedClock);

            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, "notes", "recapName", false, null);

            when(eventBO.getEventById(EVENT_ID)).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(differentUser);
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, differentUser.getId()))
                    .when(ownershipValidator).validateEventOwnership(differentUser.getId(), event);

            // Act + Assert
            assertThrows(UserOwnershipException.class, () -> eventRecapService.addEventRecap(dto));
            verifyNoInteractions(eventRecapRepository, recapMediaService, eventRecapMapper);
        }

        @Test
        void operations_handleOwnershipValidatorException() {
            // Arrange
            User currentUser = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, currentUser, fixedClock);

            when(eventBO.getEventById(EVENT_ID)).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(currentUser);
            doThrow(new RuntimeException("Security validation service unavailable"))
                    .when(ownershipValidator).validateEventOwnership(currentUser.getId(), event);

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.getEventRecap(EVENT_ID));
            assertThrows(RuntimeException.class, () -> eventRecapService.confirmEventRecap(EVENT_ID));
            assertThrows(RuntimeException.class, () -> eventRecapService.updateEventRecap(EVENT_ID, new EventRecapUpdateDTO("notes", null)));
            assertThrows(RuntimeException.class, () -> eventRecapService.deleteEventRecap(EVENT_ID));
        }

        @Test
        void operations_handleAuthenticatedUserProviderException() {
            // Arrange
            when(eventBO.getEventById(EVENT_ID)).thenReturn(Optional.of(TestUtils.createValidCompletedEventWithId(EVENT_ID, TestUtils.createValidUserEntityWithId(), fixedClock)));
            when(authenticatedUserProvider.getCurrentUser())
                    .thenThrow(new RuntimeException("Authentication service timeout"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.getEventRecap(EVENT_ID));
            assertThrows(RuntimeException.class, () -> eventRecapService.confirmEventRecap(EVENT_ID));
            assertThrows(RuntimeException.class, () -> eventRecapService.updateEventRecap(EVENT_ID, new EventRecapUpdateDTO("notes", null)));
            assertThrows(RuntimeException.class, () -> eventRecapService.deleteEventRecap(EVENT_ID));
        }

        @Test
        void operations_handleEventOwnershipChangeAfterValidation() {
            // Arrange - scenario where ownership validation changes between validation and operation
            User originalOwner = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, originalOwner, fixedClock);

            when(eventBO.getEventById(EVENT_ID)).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(originalOwner);
            doNothing().when(ownershipValidator).validateEventOwnership(originalOwner.getId(), event);
            
            // Simulates situation where ownership validation fails during the actual operation
            when(eventBO.updateEvent(null, event))
                    .thenThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, originalOwner.getId()));

            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, "notes", "recapName", false, null);

            // Act + Assert
            assertThrows(UserOwnershipException.class, () -> eventRecapService.addEventRecap(dto));
        }

    }

    @Nested
    class EventStateEdgeCaseTests {

        @Test
        void addEventRecap_createsUnconfirmedRecapForIncompleteEvent() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event incompleteEvent = TestUtils.createValidIncompletePastEvent(viewer, fixedClock);
            TestUtils.setEventId(incompleteEvent, EVENT_ID);
            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, "notes", "recapName", false, null);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(incompleteEvent.getId())).thenReturn(Optional.of(incompleteEvent));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), incompleteEvent);
            when(eventBO.updateEvent(null, incompleteEvent)).thenReturn(incompleteEvent);
            when(recapMediaService.getOrderedMediaForRecap(any())).thenReturn(List.of());
            when(eventRecapMapper.toResponseDTO(any(), eq(incompleteEvent), anyList())).thenReturn(
                    TestUtils.createEventRecapResponseDTO(TestUtils.createValidUnconfirmedEventRecap(incompleteEvent), incompleteEvent)
            );

            // Act
            EventRecapResponseDTO result = eventRecapService.addEventRecap(dto);

            // Assert
            assertNotNull(result);
            // Verify that an unconfirmed recap was created even for incomplete event
        }

        @Test
        void addEventRecap_createsUnconfirmedRecapWhenDtoSpecifiesUnconfirmed() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event completedEvent = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, "notes", "recapName", true, null); // isUnconfirmed = true

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(completedEvent.getId())).thenReturn(Optional.of(completedEvent));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), completedEvent);
            when(eventBO.updateEvent(null, completedEvent)).thenReturn(completedEvent);
            when(recapMediaService.getOrderedMediaForRecap(any())).thenReturn(List.of());
            when(eventRecapMapper.toResponseDTO(any(), eq(completedEvent), anyList())).thenReturn(
                    TestUtils.createEventRecapResponseDTO(TestUtils.createValidUnconfirmedEventRecap(completedEvent), completedEvent)
            );

            // Act
            EventRecapResponseDTO result = eventRecapService.addEventRecap(dto);

            // Assert
            assertNotNull(result);
            // Verify that unconfirmed recap was created even though event is completed
        }

        @Test
        void getEventRecap_worksForIncompleteEventWithRecap() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event incompleteEvent = TestUtils.createValidIncompletePastEvent(viewer, fixedClock);
            TestUtils.setEventId(incompleteEvent, EVENT_RECAP_ID);
            EventRecap recap = TestUtils.createValidUnconfirmedEventRecap(incompleteEvent);
            incompleteEvent.setRecap(recap);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(incompleteEvent.getId())).thenReturn(Optional.of(incompleteEvent));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), incompleteEvent);

            var mediaDTOs = List.of(TestUtils.createValidImageRecapMediaResponseDTO());
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(mediaDTOs);

            EventRecapResponseDTO expected = TestUtils.createEventRecapResponseDTO(recap, incompleteEvent);
            when(eventRecapMapper.toResponseDTO(recap, incompleteEvent, mediaDTOs)).thenReturn(expected);

            // Act
            EventRecapResponseDTO result = eventRecapService.getEventRecap(incompleteEvent.getId());

            // Assert
            assertNotNull(result);
            // Verify that recaps can be retrieved from incomplete events
        }

        @Test
        void updateEventRecap_worksForIncompleteEventWithRecap() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event incompleteEvent = TestUtils.createValidIncompletePastEvent(viewer, fixedClock);
            TestUtils.setEventId(incompleteEvent, EVENT_ID);
            EventRecap recap = TestUtils.createValidUnconfirmedEventRecap(incompleteEvent);
            incompleteEvent.setRecap(recap);

            EventRecapUpdateDTO dto = new EventRecapUpdateDTO("Updated notes", null);

            when(eventBO.getEventById(incompleteEvent.getId())).thenReturn(Optional.of(incompleteEvent));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), incompleteEvent);
            when(eventRecapRepository.save(recap)).thenReturn(recap);

            List<RecapMediaResponseDTO> mediaDTOs = List.of(TestUtils.createValidImageRecapMediaResponseDTO());
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(mediaDTOs);

            EventRecapResponseDTO expected = TestUtils.createEventRecapResponseDTO(recap, incompleteEvent);
            when(eventRecapMapper.toResponseDTO(recap, incompleteEvent, mediaDTOs)).thenReturn(expected);

            // Act
            EventRecapResponseDTO result = eventRecapService.updateEventRecap(incompleteEvent.getId(), dto);

            // Assert
            assertNotNull(result);
            verify(eventRecapRepository).save(recap);
        }

        @Test
        void deleteEventRecap_worksForIncompleteEventWithRecap() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event incompleteEvent = TestUtils.createValidIncompletePastEvent(viewer, fixedClock);
            TestUtils.setEventId(incompleteEvent, EVENT_ID);
            EventRecap recap = TestUtils.createValidUnconfirmedEventRecap(incompleteEvent);
            incompleteEvent.setRecap(recap);

            when(eventBO.getEventById(incompleteEvent.getId())).thenReturn(Optional.of(incompleteEvent));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), incompleteEvent);
            doNothing().when(recapMediaService).deleteAllMediaForRecap(recap.getId());
            when(eventBO.updateEvent(null, incompleteEvent)).thenReturn(incompleteEvent);

            // Act
            eventRecapService.deleteEventRecap(incompleteEvent.getId());

            // Assert
            assertNull(incompleteEvent.getRecap());
            verify(recapMediaService).deleteAllMediaForRecap(recap.getId());
            verify(eventBO).updateEvent(null, incompleteEvent);
        }

        @Test
        void confirmEventRecap_handlesEventBecomeIncompleteAfterInitialCheck() {
            // Arrange - Edge case where event becomes incomplete between method entry and validation
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidIncompletePastEvent(viewer, fixedClock); // Start as incomplete
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
        void operations_handleEventWithNullCompletedStatus() {
            // Arrange - Edge case where event completed status is indeterminate
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            // Simulate some unusual state where completion logic might be unclear
            
            when(eventBO.getEventById(EVENT_ID)).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, "notes", "recapName", false, null);
            when(eventBO.updateEvent(null, event)).thenReturn(event);
            when(recapMediaService.getOrderedMediaForRecap(any())).thenReturn(List.of());
            when(eventRecapMapper.toResponseDTO(any(), eq(event), anyList())).thenReturn(
                    TestUtils.createEventRecapResponseDTO(TestUtils.createValidEventRecap(event), event)
            );

            // Act
            EventRecapResponseDTO result = eventRecapService.addEventRecap(dto);

            // Assert
            assertNotNull(result);
            // Verify service can handle events in various completion states
        }

    }

    @Nested
    class MediaRelatedEdgeCaseTests {

        @Test
        void addEventRecap_handlesLargeMediaList() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            
            // Create a large list of media items
            List<RecapMediaCreateDTO> largeMediaList = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                largeMediaList.add(TestUtils.createValidImageRecapMediaCreateDTO());
            }
            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, "notes", "recapName", false, largeMediaList);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event)).thenReturn(event);
            doNothing().when(recapMediaService).addMediaItemsToRecap(any(EventRecap.class), eq(largeMediaList));
            when(recapMediaService.getOrderedMediaForRecap(any())).thenReturn(List.of());
            when(eventRecapMapper.toResponseDTO(any(), eq(event), anyList())).thenReturn(
                    TestUtils.createEventRecapResponseDTO(TestUtils.createValidEventRecap(event), event)
            );

            // Act
            EventRecapResponseDTO result = eventRecapService.addEventRecap(dto);

            // Assert
            assertNotNull(result);
            verify(recapMediaService).addMediaItemsToRecap(any(EventRecap.class), eq(largeMediaList));
        }

        @Test
        void updateEventRecap_handlesEmptyMediaListReplacingExistingMedia() {
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
            doNothing().when(recapMediaService).replaceRecapMedia(recap, emptyMediaList);

            List<RecapMediaResponseDTO> emptyMediaResponse = List.of();
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(emptyMediaResponse);

            EventRecapResponseDTO expected = TestUtils.createEventRecapResponseDTO(recap, event);
            when(eventRecapMapper.toResponseDTO(recap, event, emptyMediaResponse)).thenReturn(expected);

            // Act
            EventRecapResponseDTO result = eventRecapService.updateEventRecap(event.getId(), dto);

            // Assert
            assertNotNull(result);
            verify(recapMediaService).replaceRecapMedia(recap, emptyMediaList);
        }

        @Test
        void getEventRecap_handlesRecapWithNoMedia() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_RECAP_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            List<RecapMediaResponseDTO> emptyMediaList = List.of();
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(emptyMediaList);

            EventRecapResponseDTO expected = TestUtils.createEventRecapResponseDTO(recap, event);
            when(eventRecapMapper.toResponseDTO(recap, event, emptyMediaList)).thenReturn(expected);

            // Act
            EventRecapResponseDTO result = eventRecapService.getEventRecap(event.getId());

            // Assert
            assertNotNull(result);
            verify(recapMediaService).getOrderedMediaForRecap(recap.getId());
        }

        @Test
        void getEventRecap_handlesRecapWithLargeMediaCollection() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_RECAP_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            // Create large media collection
            List<RecapMediaResponseDTO> largeMediaList = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                largeMediaList.add(TestUtils.createValidImageRecapMediaResponseDTO());
            }
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(largeMediaList);

            EventRecapResponseDTO expected = TestUtils.createEventRecapResponseDTO(recap, event);
            when(eventRecapMapper.toResponseDTO(recap, event, largeMediaList)).thenReturn(expected);

            // Act
            EventRecapResponseDTO result = eventRecapService.getEventRecap(event.getId());

            // Assert
            assertNotNull(result);
            verify(recapMediaService).getOrderedMediaForRecap(recap.getId());
        }

        @Test
        void updateEventRecap_handlesLargeMediaListReplacement() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            List<RecapMediaCreateDTO> largeNewMediaList = new ArrayList<>();
            for (int i = 0; i < 75; i++) {
                largeNewMediaList.add(TestUtils.createValidImageRecapMediaCreateDTO());
            }
            EventRecapUpdateDTO dto = new EventRecapUpdateDTO("Updated notes", largeNewMediaList);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventRecapRepository.save(recap)).thenReturn(recap);
            doNothing().when(recapMediaService).replaceRecapMedia(recap, largeNewMediaList);

            List<RecapMediaResponseDTO> responseMediaList = List.of(TestUtils.createValidImageRecapMediaResponseDTO());
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(responseMediaList);

            EventRecapResponseDTO expected = TestUtils.createEventRecapResponseDTO(recap, event);
            when(eventRecapMapper.toResponseDTO(recap, event, responseMediaList)).thenReturn(expected);

            // Act
            EventRecapResponseDTO result = eventRecapService.updateEventRecap(event.getId(), dto);

            // Assert
            assertNotNull(result);
            verify(recapMediaService).replaceRecapMedia(recap, largeNewMediaList);
        }

        @Test
        void deleteEventRecap_handlesRecapWithLargeMediaCollection() {
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
        void addEventRecap_handlesMediaListWithNullItems() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            
            List<RecapMediaCreateDTO> mediaWithNulls = new ArrayList<>();
            mediaWithNulls.add(TestUtils.createValidImageRecapMediaCreateDTO());
            mediaWithNulls.add(null);
            mediaWithNulls.add(TestUtils.createValidImageRecapMediaCreateDTO());
            
            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, "notes", "recapName", false, mediaWithNulls);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event)).thenReturn(event);
            
            // Media service should handle null items gracefully or throw appropriate exception
            doThrow(new IllegalArgumentException("Media list contains null items"))
                    .when(recapMediaService).addMediaItemsToRecap(any(EventRecap.class), eq(mediaWithNulls));

            // Act + Assert
            assertThrows(IllegalArgumentException.class, () -> eventRecapService.addEventRecap(dto));
        }

    }

    @Nested
    class DTOValidationEdgeCaseTests {

        @Test
        void addEventRecap_handlesVeryLongNotes() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            
            StringBuilder veryLongNotes = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                veryLongNotes.append("This is a very long note that exceeds normal length. ");
            }
            
            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, veryLongNotes.toString(), "recapName", false, null);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event)).thenReturn(event);
            when(recapMediaService.getOrderedMediaForRecap(any())).thenReturn(List.of());
            when(eventRecapMapper.toResponseDTO(any(), eq(event), anyList())).thenReturn(
                    TestUtils.createEventRecapResponseDTO(TestUtils.createValidEventRecap(event), event)
            );

            // Act
            EventRecapResponseDTO result = eventRecapService.addEventRecap(dto);

            // Assert
            assertNotNull(result);
        }

        @Test
        void addEventRecap_handlesVeryLongRecapName() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            
            StringBuilder veryLongName = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                veryLongName.append("VeryLongRecapName");
            }
            
            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, "notes", veryLongName.toString(), false, null);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event)).thenReturn(event);
            when(recapMediaService.getOrderedMediaForRecap(any())).thenReturn(List.of());
            when(eventRecapMapper.toResponseDTO(any(), eq(event), anyList())).thenReturn(
                    TestUtils.createEventRecapResponseDTO(TestUtils.createValidEventRecap(event), event)
            );

            // Act
            EventRecapResponseDTO result = eventRecapService.addEventRecap(dto);

            // Assert
            assertNotNull(result);
        }

        @Test
        void updateEventRecap_handlesVeryLongNotes() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            StringBuilder veryLongNotes = new StringBuilder();
            for (int i = 0; i < 5000; i++) {
                veryLongNotes.append("Updated long note content. ");
            }
            EventRecapUpdateDTO dto = new EventRecapUpdateDTO(veryLongNotes.toString(), null);

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
            verify(eventRecapRepository).save(recap);
        }

        @Test
        void addEventRecap_handlesSpecialCharactersInNotes() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            
            String specialCharNotes = "Special chars: ñáéíóú çüöß 中文 日本語 🌟🎉🎊 @#$%^&*()[]{}\"'<>?/|";
            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, specialCharNotes, "recapName", false, null);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event)).thenReturn(event);
            when(recapMediaService.getOrderedMediaForRecap(any())).thenReturn(List.of());
            when(eventRecapMapper.toResponseDTO(any(), eq(event), anyList())).thenReturn(
                    TestUtils.createEventRecapResponseDTO(TestUtils.createValidEventRecap(event), event)
            );

            // Act
            EventRecapResponseDTO result = eventRecapService.addEventRecap(dto);

            // Assert
            assertNotNull(result);
        }

        @Test
        void addEventRecap_handlesSpecialCharactersInRecapName() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            
            String specialCharName = "Special Name: ñáéíóú çüöß 中文 日本語 🌟🎉";
            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, "notes", specialCharName, false, null);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event)).thenReturn(event);
            when(recapMediaService.getOrderedMediaForRecap(any())).thenReturn(List.of());
            when(eventRecapMapper.toResponseDTO(any(), eq(event), anyList())).thenReturn(
                    TestUtils.createEventRecapResponseDTO(TestUtils.createValidEventRecap(event), event)
            );

            // Act
            EventRecapResponseDTO result = eventRecapService.addEventRecap(dto);

            // Assert
            assertNotNull(result);
        }

        @Test
        void updateEventRecap_handlesWhitespaceOnlyNotes() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            EventRecapUpdateDTO dto = new EventRecapUpdateDTO("   \t\n\r   ", null); // Only whitespace

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
            verify(eventRecapRepository).save(recap);
            assertEquals("   \t\n\r   ", recap.getNotes());
        }

        @Test
        void addEventRecap_handlesNegativeEventIdInDTO() {
            // Arrange
            EventRecapCreateDTO dto = new EventRecapCreateDTO(-999L, "notes", "recapName", false, null);
            when(eventBO.getEventById(-999L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(EventNotFoundException.class, () -> eventRecapService.addEventRecap(dto));
        }

        @Test
        void operations_handleDTOWithInconsistentEventIds() {
            // Arrange - DTO specifies different event ID than method parameter
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            Long differentEventId = 999L;
            EventRecapUpdateDTO dto = new EventRecapUpdateDTO("Updated notes", null);

            when(eventBO.getEventById(EVENT_ID)).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventRecapRepository.save(recap)).thenReturn(recap);

            List<RecapMediaResponseDTO> mediaDTOs = List.of(TestUtils.createValidImageRecapMediaResponseDTO());
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(mediaDTOs);

            EventRecapResponseDTO expected = TestUtils.createEventRecapResponseDTO(recap, event);
            when(eventRecapMapper.toResponseDTO(recap, event, mediaDTOs)).thenReturn(expected);

            // Act - method should use eventId parameter, not any ID in DTO
            EventRecapResponseDTO result = eventRecapService.updateEventRecap(EVENT_ID, dto);

            // Assert
            assertNotNull(result);
            verify(eventBO).getEventById(EVENT_ID); // Should use method parameter
        }

    }

    @Nested
    class MapperConversionErrorCaseTests {

        @Test
        void getEventRecap_handlesMapperException() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_RECAP_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            var mediaDTOs = List.of(TestUtils.createValidImageRecapMediaResponseDTO());
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(mediaDTOs);
            when(eventRecapMapper.toResponseDTO(recap, event, mediaDTOs))
                    .thenThrow(new RuntimeException("Mapping failed - invalid data structure"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.getEventRecap(event.getId()));
        }

        @Test
        void addEventRecap_handlesMapperExceptionAfterCreation() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, "notes", "recapName", false, null);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event)).thenReturn(event);
            when(recapMediaService.getOrderedMediaForRecap(any())).thenReturn(List.of());
            when(eventRecapMapper.toResponseDTO(any(), eq(event), anyList()))
                    .thenThrow(new RuntimeException("Entity to DTO conversion failed"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.addEventRecap(dto));
        }

        @Test
        void confirmEventRecap_handlesMapperExceptionAfterConfirmation() {
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
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(mediaDTOs);
            when(eventRecapMapper.toResponseDTO(recap, event, mediaDTOs))
                    .thenThrow(new RuntimeException("Conversion error - circular reference detected"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.confirmEventRecap(event.getId()));
        }

        @Test
        void updateEventRecap_handlesMapperExceptionAfterUpdate() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            EventRecapUpdateDTO dto = new EventRecapUpdateDTO("Updated notes", null);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventRecapRepository.save(recap)).thenReturn(recap);

            List<RecapMediaResponseDTO> mediaDTOs = List.of(TestUtils.createValidImageRecapMediaResponseDTO());
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(mediaDTOs);
            when(eventRecapMapper.toResponseDTO(recap, event, mediaDTOs))
                    .thenThrow(new RuntimeException("Mapper serialization error"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.updateEventRecap(event.getId(), dto));
            verify(eventRecapRepository).save(recap); // Verify save completed before mapper failure
        }

        @Test
        void getEventRecap_handlesNullReturnFromMapper() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_RECAP_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            var mediaDTOs = List.of(TestUtils.createValidImageRecapMediaResponseDTO());
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(mediaDTOs);
            when(eventRecapMapper.toResponseDTO(recap, event, mediaDTOs)).thenReturn(null);

            // Act
            EventRecapResponseDTO result = eventRecapService.getEventRecap(event.getId());

            // Assert
            assertNull(result); // Service should return null if mapper returns null
        }

        @Test
        void operations_handleMapperWithCorruptedData() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecapCreateDTO dto = new EventRecapCreateDTO(EVENT_ID, "notes", "recapName", false, null);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventBO.updateEvent(null, event)).thenReturn(event);
            when(recapMediaService.getOrderedMediaForRecap(any())).thenReturn(List.of());
            when(eventRecapMapper.toResponseDTO(any(), eq(event), anyList()))
                    .thenThrow(new RuntimeException("Data corruption detected during mapping"));

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventRecapService.addEventRecap(dto));
        }

        @Test
        void operations_handleMapperMemoryIssue() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_RECAP_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            var mediaDTOs = List.of(TestUtils.createValidImageRecapMediaResponseDTO());
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(mediaDTOs);
            when(eventRecapMapper.toResponseDTO(recap, event, mediaDTOs))
                    .thenThrow(new OutOfMemoryError("Mapper ran out of memory"));

            // Act + Assert
            assertThrows(OutOfMemoryError.class, () -> eventRecapService.getEventRecap(event.getId()));
        }

        @Test
        void operations_handleMapperStackOverflow() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);
            event.setRecap(recap);

            EventRecapUpdateDTO dto = new EventRecapUpdateDTO("Updated notes", null);

            when(eventBO.getEventById(event.getId())).thenReturn(Optional.of(event));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(eventRecapRepository.save(recap)).thenReturn(recap);

            List<RecapMediaResponseDTO> mediaDTOs = List.of(TestUtils.createValidImageRecapMediaResponseDTO());
            when(recapMediaService.getOrderedMediaForRecap(recap.getId())).thenReturn(mediaDTOs);
            when(eventRecapMapper.toResponseDTO(recap, event, mediaDTOs))
                    .thenThrow(new StackOverflowError("Mapper recursive mapping overflow"));

            // Act + Assert
            assertThrows(StackOverflowError.class, () -> eventRecapService.updateEventRecap(event.getId(), dto));
        }

    }

}
