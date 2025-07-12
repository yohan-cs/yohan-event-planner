package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.EventRecap;
import com.yohan.event_planner.domain.RecapMedia;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.RecapMediaType;
import com.yohan.event_planner.dto.RecapMediaCreateDTO;
import com.yohan.event_planner.dto.RecapMediaResponseDTO;
import com.yohan.event_planner.dto.RecapMediaUpdateDTO;
import com.yohan.event_planner.exception.EventRecapNotFoundException;
import com.yohan.event_planner.exception.IncompleteRecapMediaReorderListException;
import com.yohan.event_planner.exception.RecapMediaNotFoundException;
import com.yohan.event_planner.exception.UserOwnershipException;
import com.yohan.event_planner.mapper.RecapMediaMapper;
import com.yohan.event_planner.repository.EventRecapRepository;
import com.yohan.event_planner.repository.RecapMediaRepository;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import com.yohan.event_planner.util.RecapMediaResponseDTOAssertions;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_USER_ACCESS;
import static com.yohan.event_planner.util.TestConstants.EVENT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


public class RecapMediaServiceImplTest {

    private EventRecapRepository recapRepository;
    private RecapMediaRepository recapMediaRepository;
    private AuthenticatedUserProvider authenticatedUserProvider;
    private OwnershipValidator ownershipValidator;
    private RecapMediaMapper recapMediaMapper;
    private Clock fixedClock;

    private RecapMediaServiceImpl recapMediaService;

    @BeforeEach
    void setUp() {
        this.recapRepository = mock(EventRecapRepository.class);
        this.recapMediaRepository = mock(RecapMediaRepository.class);
        this.authenticatedUserProvider = mock(AuthenticatedUserProvider.class);
        this.ownershipValidator = mock(OwnershipValidator.class);
        this.recapMediaMapper = mock(RecapMediaMapper.class);

        fixedClock = Clock.fixed(Instant.parse("2025-06-29T12:00:00Z"), ZoneId.of("UTC"));

        recapMediaService = new RecapMediaServiceImpl(
                recapRepository,
                recapMediaRepository,
                authenticatedUserProvider,
                ownershipValidator,
                recapMediaMapper
        );
    }

    @Nested
    class GetOrderedMediaForRecapTests {

        @Test
        void retrievesAndMapsOrderedMediaSuccessfully() {
            // Arrange
            Long recapId = 1L;
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            RecapMedia media1 = new RecapMedia(recap, "url1", RecapMediaType.IMAGE, null, 0);
            RecapMedia media2 = new RecapMedia(recap, "url2", RecapMediaType.VIDEO, 30, 1);

            RecapMediaResponseDTO response1 = new RecapMediaResponseDTO(1L, "url1", RecapMediaType.IMAGE, null, 0);
            RecapMediaResponseDTO response2 = new RecapMediaResponseDTO(2L, "url2", RecapMediaType.VIDEO, 30, 1);

            when(recapRepository.findById(recapId)).thenReturn(Optional.of(recap));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(recapMediaRepository.findByRecapIdOrderByMediaOrder(recapId)).thenReturn(List.of(media1, media2));
            when(recapMediaMapper.toResponseDTO(media1)).thenReturn(response1);
            when(recapMediaMapper.toResponseDTO(media2)).thenReturn(response2);

            // Act
            List<RecapMediaResponseDTO> result = recapMediaService.getOrderedMediaForRecap(recapId);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            RecapMediaResponseDTOAssertions.assertRecapMediaResponseDTOEquals(response1, result.get(0));
            RecapMediaResponseDTOAssertions.assertRecapMediaResponseDTOEquals(response2, result.get(1));

            verify(recapRepository).findById(recapId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verify(recapMediaRepository).findByRecapIdOrderByMediaOrder(recapId);
            verify(recapMediaMapper).toResponseDTO(media1);
            verify(recapMediaMapper).toResponseDTO(media2);
        }

        @Test
        void throwsWhenRecapNotFound() {
            // Arrange
            Long recapId = 1L;
            when(recapRepository.findById(recapId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(EventRecapNotFoundException.class, () -> recapMediaService.getOrderedMediaForRecap(recapId));

            verify(recapRepository).findById(recapId);
            verifyNoInteractions(authenticatedUserProvider, ownershipValidator, recapMediaRepository, recapMediaMapper);
        }

        @Test
        void throwsWhenUserDoesNotOwnRecap() {
            // Arrange
            Long recapId = 1L;
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            when(recapRepository.findById(recapId)).thenReturn(Optional.of(recap));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, viewer.getId()))
                    .when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            // Act + Assert
            assertThrows(UserOwnershipException.class, () -> recapMediaService.getOrderedMediaForRecap(recapId));

            verify(recapRepository).findById(recapId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verifyNoInteractions(recapMediaRepository, recapMediaMapper);
        }

        @Test
        void returnsEmptyListWhenNoMediaExists() {
            // Arrange
            Long recapId = 1L;
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            when(recapRepository.findById(recapId)).thenReturn(Optional.of(recap));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(recapMediaRepository.findByRecapIdOrderByMediaOrder(recapId)).thenReturn(List.of());

            // Act
            List<RecapMediaResponseDTO> result = recapMediaService.getOrderedMediaForRecap(recapId);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(recapRepository).findById(recapId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verify(recapMediaRepository).findByRecapIdOrderByMediaOrder(recapId);
            verifyNoInteractions(recapMediaMapper);
        }

    }

    @Nested
    class AddRecapMediaTests {

        @Test
        void addsMediaWithExplicitOrder() {
            // Arrange
            Long recapId = 1L;

            // Create user and event
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            // Create DTO with explicit order
            RecapMediaCreateDTO dto = new RecapMediaCreateDTO(
                    "https://example.com/explicit.jpg",
                    RecapMediaType.IMAGE,
                    null,
                    5 // explicit order
            );

            // Expected media entity (could also build with TestUtils if you add a factory later)
            RecapMedia media = new RecapMedia(
                    recap,
                    dto.mediaUrl(),
                    dto.mediaType(),
                    dto.durationSeconds(),
                    dto.mediaOrder()
            );

            // Expected response
            RecapMediaResponseDTO expectedResponse = new RecapMediaResponseDTO(
                    1L,
                    dto.mediaUrl(),
                    dto.mediaType(),
                    dto.durationSeconds(),
                    dto.mediaOrder()
            );

            when(recapRepository.findById(recapId)).thenReturn(Optional.of(recap));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), recap.getEvent());
            when(recapMediaRepository.save(any(RecapMedia.class))).thenReturn(media);
            when(recapMediaMapper.toResponseDTO(any())).thenReturn(expectedResponse);

            // Act
            RecapMediaResponseDTO result = recapMediaService.addRecapMedia(recapId, dto);

            // Assert
            assertNotNull(result);
            RecapMediaResponseDTOAssertions.assertRecapMediaResponseDTOEquals(expectedResponse, result);

            verify(recapRepository).findById(recapId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), recap.getEvent());
            verify(recapMediaRepository).save(any(RecapMedia.class));
            verify(recapMediaMapper).toResponseDTO(any());
        }

        @Test
        void addsMediaWithImplicitOrder() {
            // Arrange
            Long recapId = 1L;

            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            // Create DTO with mediaOrder = null to test implicit order
            RecapMediaCreateDTO dto = new RecapMediaCreateDTO(
                    "https://example.com/image.jpg",
                    RecapMediaType.IMAGE,
                    null,
                    null // <-- mediaOrder = null to trigger countByRecapId
            );

            int existingMediaCount = 3;

            RecapMedia media = new RecapMedia(
                    recap,
                    dto.mediaUrl(),
                    dto.mediaType(),
                    dto.durationSeconds(),
                    existingMediaCount
            );

            RecapMediaResponseDTO expectedResponse = TestUtils.createValidImageRecapMediaResponseDTO();

            when(recapRepository.findById(recapId)).thenReturn(Optional.of(recap));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), recap.getEvent());
            when(recapMediaRepository.countByRecapId(recapId)).thenReturn(existingMediaCount);
            when(recapMediaRepository.save(any(RecapMedia.class))).thenReturn(media);
            when(recapMediaMapper.toResponseDTO(any())).thenReturn(expectedResponse);

            // Act
            RecapMediaResponseDTO result = recapMediaService.addRecapMedia(recapId, dto);

            // Assert
            assertNotNull(result);
            RecapMediaResponseDTOAssertions.assertRecapMediaResponseDTOEquals(expectedResponse, result);

            verify(recapRepository).findById(recapId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), recap.getEvent());
            verify(recapMediaRepository).countByRecapId(recapId); // now should be called
            verify(recapMediaRepository).save(any(RecapMedia.class));
            verify(recapMediaMapper).toResponseDTO(any());
        }

        @Test
        void throwsWhenRecapNotFound() {
            // Arrange
            Long recapId = 1L;
            RecapMediaCreateDTO dto = TestUtils.createValidImageRecapMediaCreateDTO();

            when(recapRepository.findById(recapId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(EventRecapNotFoundException.class, () -> recapMediaService.addRecapMedia(recapId, dto));

            verify(recapRepository).findById(recapId);
            verifyNoInteractions(authenticatedUserProvider);
            verifyNoInteractions(ownershipValidator);
            verifyNoInteractions(recapMediaRepository);
            verifyNoInteractions(recapMediaMapper);
        }

        @Test
        void throwsWhenUserDoesNotOwnRecap() {
            // Arrange
            Long recapId = 1L;

            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            RecapMediaCreateDTO dto = TestUtils.createValidImageRecapMediaCreateDTO();

            when(recapRepository.findById(recapId)).thenReturn(Optional.of(recap));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, viewer.getId()))
                    .when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            // Act + Assert
            assertThrows(UserOwnershipException.class, () -> recapMediaService.addRecapMedia(recapId, dto));

            verify(recapRepository).findById(recapId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verifyNoInteractions(recapMediaRepository);
            verifyNoInteractions(recapMediaMapper);
        }

        @Test
        void handlesImplicitOrderWhenNoExistingMedia() {
            // Arrange
            Long recapId = 1L;

            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            // Create DTO with mediaOrder = null to test implicit order starting from 0
            RecapMediaCreateDTO dto = new RecapMediaCreateDTO(
                    "https://example.com/first-media.jpg",
                    RecapMediaType.IMAGE,
                    null,
                    null // <-- mediaOrder = null to trigger countByRecapId
            );

            int existingMediaCount = 0; // No existing media

            RecapMedia media = new RecapMedia(
                    recap,
                    dto.mediaUrl(),
                    dto.mediaType(),
                    dto.durationSeconds(),
                    existingMediaCount // Should get order 0
            );

            RecapMediaResponseDTO expectedResponse = TestUtils.createValidImageRecapMediaResponseDTO();

            when(recapRepository.findById(recapId)).thenReturn(Optional.of(recap));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), recap.getEvent());
            when(recapMediaRepository.countByRecapId(recapId)).thenReturn(existingMediaCount);
            when(recapMediaRepository.save(any(RecapMedia.class))).thenReturn(media);
            when(recapMediaMapper.toResponseDTO(any())).thenReturn(expectedResponse);

            // Act
            RecapMediaResponseDTO result = recapMediaService.addRecapMedia(recapId, dto);

            // Assert
            assertNotNull(result);
            RecapMediaResponseDTOAssertions.assertRecapMediaResponseDTOEquals(expectedResponse, result);

            verify(recapRepository).findById(recapId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), recap.getEvent());
            verify(recapMediaRepository).countByRecapId(recapId); // Should be called for implicit order
            verify(recapMediaRepository).save(argThat(savedMedia -> 
                savedMedia.getMediaOrder() == 0 // Should be assigned order 0
            ));
            verify(recapMediaMapper).toResponseDTO(any());
        }

    }

    @Nested
    class AddMediaItemsToRecapTests {

        @Test
        void addsMultipleMediaItemsWithMixedOrders() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            RecapMediaCreateDTO dto1 = new RecapMediaCreateDTO(
                    "https://example.com/image1.jpg",
                    RecapMediaType.IMAGE,
                    null,
                    null // implicit order
            );

            RecapMediaCreateDTO dto2 = new RecapMediaCreateDTO(
                    "https://example.com/image2.jpg",
                    RecapMediaType.IMAGE,
                    null,
                    5 // explicit order
            );

            List<RecapMediaCreateDTO> mediaList = List.of(dto1, dto2);

            int existingMediaCount = 2;

            when(recapMediaRepository.countByRecapId(recap.getId())).thenReturn(existingMediaCount);

            // Act
            recapMediaService.addMediaItemsToRecap(recap, mediaList);

            // Assert
            verify(recapMediaRepository).countByRecapId(recap.getId());
            verify(recapMediaRepository).saveAll(argThat(entities -> {
                if (!(entities instanceof List)) return false;
                List<?> list = (List<?>) entities;
                if (list.size() != 2) return false;

                RecapMedia first = (RecapMedia) list.get(0);
                RecapMedia second = (RecapMedia) list.get(1);

                boolean firstCorrect = first.getMediaUrl().equals(dto1.mediaUrl())
                        && first.getMediaOrder() == existingMediaCount;

                boolean secondCorrect = second.getMediaUrl().equals(dto2.mediaUrl())
                        && second.getMediaOrder() == dto2.mediaOrder();

                return firstCorrect && secondCorrect;
            }));

            verifyNoMoreInteractions(recapMediaRepository, recapRepository, authenticatedUserProvider, ownershipValidator, recapMediaMapper);
        }

        @Test
        void skipsWhenMediaListIsNull() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            // Act
            recapMediaService.addMediaItemsToRecap(recap, null);

            // Assert
            verifyNoInteractions(recapMediaRepository);
        }

        @Test
        void skipsWhenMediaListIsEmpty() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            // Act
            recapMediaService.addMediaItemsToRecap(recap, List.of());

            // Assert
            verifyNoInteractions(recapMediaRepository);
        }



        @Test
        void addsAllMediaWithExplicitOrdersOnly() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            RecapMediaCreateDTO dto1 = new RecapMediaCreateDTO(
                    "https://example.com/image1.jpg",
                    RecapMediaType.IMAGE,
                    null,
                    2
            );

            RecapMediaCreateDTO dto2 = new RecapMediaCreateDTO(
                    "https://example.com/image2.jpg",
                    RecapMediaType.IMAGE,
                    null,
                    5
            );

            List<RecapMediaCreateDTO> mediaList = List.of(dto1, dto2);

            // Act
            recapMediaService.addMediaItemsToRecap(recap, mediaList);

            // Assert
            verify(recapMediaRepository, never()).countByRecapId(anyLong());
            verify(recapMediaRepository).saveAll(argThat(entities -> {
                if (!(entities instanceof List)) return false;
                List<?> list = (List<?>) entities;
                if (list.size() != 2) return false;

                RecapMedia first = (RecapMedia) list.get(0);
                RecapMedia second = (RecapMedia) list.get(1);

                return first.getMediaOrder() == 2 && second.getMediaOrder() == 5;
            }));
        }

        @Test
        void addsAllMediaWithImplicitOrdersOnly() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            RecapMediaCreateDTO dto1 = new RecapMediaCreateDTO(
                    "https://example.com/image1.jpg",
                    RecapMediaType.IMAGE,
                    null,
                    null
            );

            RecapMediaCreateDTO dto2 = new RecapMediaCreateDTO(
                    "https://example.com/image2.jpg",
                    RecapMediaType.IMAGE,
                    null,
                    null
            );

            List<RecapMediaCreateDTO> mediaList = List.of(dto1, dto2);

            int existingMediaCount = 5;

            when(recapMediaRepository.countByRecapId(recap.getId())).thenReturn(existingMediaCount);

            // Act
            recapMediaService.addMediaItemsToRecap(recap, mediaList);

            // Assert
            verify(recapMediaRepository).countByRecapId(recap.getId());
            verify(recapMediaRepository).saveAll(argThat(entities -> {
                if (!(entities instanceof List)) return false;
                List<?> list = (List<?>) entities;
                if (list.size() != 2) return false;

                RecapMedia first = (RecapMedia) list.get(0);
                RecapMedia second = (RecapMedia) list.get(1);

                return first.getMediaOrder() == 5 && second.getMediaOrder() == 6;
            }));
        }

        @Test
        void addsMixedExplicitAndImplicitOrdersCorrectly() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            RecapMediaCreateDTO explicitDto = new RecapMediaCreateDTO(
                    "https://example.com/explicit.jpg",
                    RecapMediaType.IMAGE,
                    null,
                    4
            );

            RecapMediaCreateDTO implicitDto = new RecapMediaCreateDTO(
                    "https://example.com/implicit.jpg",
                    RecapMediaType.IMAGE,
                    null,
                    null
            );

            List<RecapMediaCreateDTO> mediaList = List.of(explicitDto, implicitDto);

            int existingMediaCount = 3; // implicit should start from here

            when(recapMediaRepository.countByRecapId(recap.getId())).thenReturn(existingMediaCount);

            // Act
            recapMediaService.addMediaItemsToRecap(recap, mediaList);

            // Assert
            verify(recapMediaRepository).countByRecapId(recap.getId());
            verify(recapMediaRepository).saveAll(argThat(entities -> {
                if (!(entities instanceof List)) return false;
                List<?> list = (List<?>) entities;
                if (list.size() != 2) return false;

                RecapMedia first = (RecapMedia) list.get(0);
                RecapMedia second = (RecapMedia) list.get(1);

                // Check explicit order remains and implicit gets assigned starting from existingMediaCount
                return first.getMediaOrder() == 4 && second.getMediaOrder() == existingMediaCount;
            }));
        }

        @Test
        void handlesLargeMediaListCorrectly() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            // Create a large list of media items (25 items)
            List<RecapMediaCreateDTO> largeMediaList = new ArrayList<>();
            for (int i = 0; i < 25; i++) {
                largeMediaList.add(new RecapMediaCreateDTO(
                        "https://example.com/media" + i + ".jpg",
                        RecapMediaType.IMAGE,
                        null,
                        null // All implicit orders
                ));
            }

            int existingMediaCount = 5;
            when(recapMediaRepository.countByRecapId(recap.getId())).thenReturn(existingMediaCount);

            // Act
            recapMediaService.addMediaItemsToRecap(recap, largeMediaList);

            // Assert
            verify(recapMediaRepository).countByRecapId(recap.getId());
            verify(recapMediaRepository).saveAll(argThat(entities -> {
                if (!(entities instanceof List)) return false;
                List<?> list = (List<?>) entities;
                if (list.size() != 25) return false;

                // Verify sequential ordering starting from existingMediaCount
                for (int i = 0; i < 25; i++) {
                    RecapMedia media = (RecapMedia) list.get(i);
                    if (media.getMediaOrder() != existingMediaCount + i) {
                        return false;
                    }
                }
                return true;
            }));
        }

        @Test
        void handlesOrderCollisionsInMixedScenario() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            // Create scenario where explicit order might collide with implicit assignment
            RecapMediaCreateDTO explicitFirst = new RecapMediaCreateDTO(
                    "https://example.com/explicit.jpg",
                    RecapMediaType.IMAGE,
                    null,
                    3 // Explicit order = 3
            );

            RecapMediaCreateDTO implicit1 = new RecapMediaCreateDTO(
                    "https://example.com/implicit1.jpg",
                    RecapMediaType.IMAGE,
                    null,
                    null // Will get implicit order starting from current count
            );

            RecapMediaCreateDTO explicitSecond = new RecapMediaCreateDTO(
                    "https://example.com/explicit2.jpg",
                    RecapMediaType.IMAGE,
                    null,
                    1 // Explicit order = 1
            );

            RecapMediaCreateDTO implicit2 = new RecapMediaCreateDTO(
                    "https://example.com/implicit2.jpg",
                    RecapMediaType.IMAGE,
                    null,
                    null // Will get next implicit order
            );

            List<RecapMediaCreateDTO> mediaList = List.of(explicitFirst, implicit1, explicitSecond, implicit2);

            int existingMediaCount = 2; // Implicit orders will start from 2
            when(recapMediaRepository.countByRecapId(recap.getId())).thenReturn(existingMediaCount);

            // Act
            recapMediaService.addMediaItemsToRecap(recap, mediaList);

            // Assert
            verify(recapMediaRepository).countByRecapId(recap.getId());
            verify(recapMediaRepository).saveAll(argThat(entities -> {
                if (!(entities instanceof List)) return false;
                List<?> list = (List<?>) entities;
                if (list.size() != 4) return false;

                RecapMedia first = (RecapMedia) list.get(0);  // explicit order 3
                RecapMedia second = (RecapMedia) list.get(1); // implicit order 2
                RecapMedia third = (RecapMedia) list.get(2);  // explicit order 1
                RecapMedia fourth = (RecapMedia) list.get(3); // implicit order 3

                return first.getMediaOrder() == 3 &&    // explicit
                       second.getMediaOrder() == 2 &&   // implicit starts from existingMediaCount
                       third.getMediaOrder() == 1 &&    // explicit
                       fourth.getMediaOrder() == 3;     // implicit continues from 2 -> 3
            }));
        }

    }

    @Nested
    class ReplaceRecapMediaTests {

        @Test
        void replacesExistingMediaWithNewList() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            List<RecapMediaCreateDTO> newMediaList = List.of(
                    TestUtils.createValidImageRecapMediaCreateDTO(),
                    TestUtils.createValidVideoRecapMediaCreateDTO()
            );

            // Act
            recapMediaService.replaceRecapMedia(recap, newMediaList);

            // Assert
            verify(recapMediaRepository).deleteByRecapId(recap.getId());
            verify(recapMediaRepository).saveAll(anyList());
        }

        @Test
        void replacesWithEmptyMediaListDeletesExistingOnly() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            List<RecapMediaCreateDTO> emptyMediaList = List.of();

            // Act
            recapMediaService.replaceRecapMedia(recap, emptyMediaList);

            // Assert
            verify(recapMediaRepository).deleteByRecapId(recap.getId());
            verify(recapMediaRepository, never()).countByRecapId(anyLong());
            verify(recapMediaRepository, never()).saveAll(anyList());
        }

        @Test
        void replacesWithNullMediaListDeletesExistingOnly() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            // Act
            recapMediaService.replaceRecapMedia(recap, null);

            // Assert
            verify(recapMediaRepository).deleteByRecapId(recap.getId());
            verify(recapMediaRepository, never()).countByRecapId(anyLong());
            verify(recapMediaRepository, never()).saveAll(anyList());
        }

    }

    @Nested
    class UpdateRecapMediaTests {

        @Test
        void updatesAllFields() {
            // Arrange
            Long mediaId = 1L;

            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            RecapMedia media = TestUtils.createValidVideoRecapMediaWithId(recap, mediaId);

            RecapMediaUpdateDTO dto = new RecapMediaUpdateDTO(
                    "https://example.com/updated.mp4",
                    RecapMediaType.VIDEO,
                    45
            );

            RecapMediaResponseDTO expectedResponse = TestUtils.createValidVideoRecapMediaResponseDTO();

            when(recapMediaRepository.findById(mediaId)).thenReturn(Optional.of(media));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(recapMediaRepository.save(any(RecapMedia.class))).thenReturn(media);
            when(recapMediaMapper.toResponseDTO(any())).thenReturn(expectedResponse);

            // Act
            RecapMediaResponseDTO result = recapMediaService.updateRecapMedia(mediaId, dto);

            // Assert
            assertNotNull(result);
            RecapMediaResponseDTOAssertions.assertRecapMediaResponseDTOEquals(expectedResponse, result);

            assertEquals(dto.mediaUrl(), media.getMediaUrl());
            assertEquals(dto.mediaType(), media.getMediaType());
            assertEquals(dto.durationSeconds(), media.getDurationSeconds());

            verify(recapMediaRepository).findById(mediaId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verify(recapMediaRepository).save(media);
            verify(recapMediaMapper).toResponseDTO(media);
        }

        @Test
        void updatesPartialFields() {
            // Arrange
            Long mediaId = 1L;

            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            RecapMedia media = TestUtils.createValidImageRecapMediaWithId(recap, mediaId);

            RecapMediaUpdateDTO dto = new RecapMediaUpdateDTO(
                    "https://example.com/updated.jpg",
                    null, // only URL updated
                    null
            );

            RecapMediaResponseDTO expectedResponse = TestUtils.createValidImageRecapMediaResponseDTO();

            when(recapMediaRepository.findById(mediaId)).thenReturn(Optional.of(media));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(recapMediaRepository.save(any(RecapMedia.class))).thenReturn(media);
            when(recapMediaMapper.toResponseDTO(any())).thenReturn(expectedResponse);

            // Act
            RecapMediaResponseDTO result = recapMediaService.updateRecapMedia(mediaId, dto);

            // Assert
            assertNotNull(result);
            RecapMediaResponseDTOAssertions.assertRecapMediaResponseDTOEquals(expectedResponse, result);

            assertEquals(dto.mediaUrl(), media.getMediaUrl());
            assertEquals(RecapMediaType.IMAGE, media.getMediaType()); // unchanged
            assertNull(media.getDurationSeconds()); // unchanged

            verify(recapMediaRepository).findById(mediaId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verify(recapMediaRepository).save(media);
            verify(recapMediaMapper).toResponseDTO(media);
        }

        @Test
        void throwsWhenMediaNotFound() {
            // Arrange
            Long mediaId = 1L;
            RecapMediaUpdateDTO dto = new RecapMediaUpdateDTO(
                    "https://example.com/updated.jpg",
                    null,
                    null
            );

            when(recapMediaRepository.findById(mediaId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(RecapMediaNotFoundException.class, () -> recapMediaService.updateRecapMedia(mediaId, dto));

            verify(recapMediaRepository).findById(mediaId);
            verifyNoInteractions(authenticatedUserProvider, ownershipValidator, recapMediaMapper);
        }

        @Test
        void optimizesByNotSavingWhenNoChangesDetected() {
            // Arrange
            Long mediaId = 1L;

            RecapMedia media = TestUtils.createValidImageRecapMediaWithId(mediaId);
            String originalUrl = media.getMediaUrl();
            RecapMediaType originalType = media.getMediaType();
            Integer originalDuration = media.getDurationSeconds();

            RecapMediaUpdateDTO dto = new RecapMediaUpdateDTO(null, null, null);

            when(recapMediaRepository.findById(mediaId)).thenReturn(Optional.of(media));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(media.getRecap().getEvent().getCreator());
            doNothing().when(ownershipValidator).validateEventOwnership(anyLong(), any());

            RecapMediaResponseDTO expectedResponse = TestUtils.createValidImageRecapMediaResponseDTO();
            when(recapMediaMapper.toResponseDTO(media)).thenReturn(expectedResponse);

            // Act
            RecapMediaResponseDTO result = recapMediaService.updateRecapMedia(mediaId, dto);

            // Assert
            assertNotNull(result);
            RecapMediaResponseDTOAssertions.assertRecapMediaResponseDTOEquals(expectedResponse, result);

            // Verify no changes were made to the entity
            assertEquals(originalUrl, media.getMediaUrl());
            assertEquals(originalType, media.getMediaType());
            assertEquals(originalDuration, media.getDurationSeconds());

            verify(recapMediaRepository).findById(mediaId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(anyLong(), any());
            verify(recapMediaRepository, never()).save(any()); // Critical: save should NOT be called
            verify(recapMediaMapper).toResponseDTO(media);
        }

        @Test
        void throwsWhenUserDoesNotOwnMedia() {
            // Arrange
            Long mediaId = 1L;

            RecapMedia media = TestUtils.createValidImageRecapMediaWithId(mediaId);
            RecapMediaUpdateDTO dto = new RecapMediaUpdateDTO(
                    "https://example.com/updated_image.jpg",
                    RecapMediaType.VIDEO,
                    60
            );

            when(recapMediaRepository.findById(mediaId)).thenReturn(Optional.of(media));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(media.getRecap().getEvent().getCreator());
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, media.getRecap().getEvent().getCreator().getId()))
                    .when(ownershipValidator).validateEventOwnership(anyLong(), any());

            // Act + Assert
            assertThrows(UserOwnershipException.class, () -> recapMediaService.updateRecapMedia(mediaId, dto));

            verify(recapMediaRepository).findById(mediaId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(anyLong(), any());
            verifyNoMoreInteractions(recapMediaRepository);
            verifyNoInteractions(recapMediaMapper);
        }

    }

    @Nested
    class DeleteRecapMediaTests {

        @Test
        void deletesMediaSuccessfully() {
            // Arrange
            Long mediaId = 1L;
            RecapMedia media = TestUtils.createValidImageRecapMediaWithId(mediaId);

            when(recapMediaRepository.findById(mediaId)).thenReturn(Optional.of(media));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(media.getRecap().getEvent().getCreator());
            doNothing().when(ownershipValidator).validateEventOwnership(anyLong(), any());

            // Act
            recapMediaService.deleteRecapMedia(mediaId);

            // Assert
            verify(recapMediaRepository).findById(mediaId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(anyLong(), any());
            verify(recapMediaRepository).delete(media);
        }

        @Test
        void throwsWhenMediaNotFound() {
            // Arrange
            Long mediaId = 1L;

            when(recapMediaRepository.findById(mediaId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(RecapMediaNotFoundException.class, () -> recapMediaService.deleteRecapMedia(mediaId));

            verify(recapMediaRepository).findById(mediaId);
            verifyNoInteractions(authenticatedUserProvider);
            verifyNoInteractions(ownershipValidator);
            verify(recapMediaRepository, never()).delete(any());
        }

        @Test
        void throwsWhenUserDoesNotOwnMedia() {
            // Arrange
            Long mediaId = 1L;
            RecapMedia media = TestUtils.createValidImageRecapMediaWithId(mediaId);

            when(recapMediaRepository.findById(mediaId)).thenReturn(Optional.of(media));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(media.getRecap().getEvent().getCreator());
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, media.getRecap().getEvent().getCreator().getId()))
                    .when(ownershipValidator).validateEventOwnership(anyLong(), any());

            // Act + Assert
            assertThrows(UserOwnershipException.class, () -> recapMediaService.deleteRecapMedia(mediaId));

            verify(recapMediaRepository).findById(mediaId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(anyLong(), any());
            verify(recapMediaRepository, never()).delete(any());
        }
    }

    @Nested
    class DeleteAllMediaForRecapTests {

        @Test
        void deletesAllMediaSuccessfully() {
            // Arrange
            Long recapId = 1L;
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            when(recapRepository.findById(recapId)).thenReturn(Optional.of(recap));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            // Act
            recapMediaService.deleteAllMediaForRecap(recapId);

            // Assert
            verify(recapRepository).findById(recapId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verify(recapMediaRepository).deleteByRecapId(recapId);
        }

        @Test
        void throwsWhenRecapNotFound() {
            // Arrange
            Long recapId = 1L;
            when(recapRepository.findById(recapId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(EventRecapNotFoundException.class, () -> recapMediaService.deleteAllMediaForRecap(recapId));

            verify(recapRepository).findById(recapId);
            verifyNoInteractions(authenticatedUserProvider);
            verifyNoInteractions(ownershipValidator);
            verifyNoInteractions(recapMediaRepository);
        }

        @Test
        void throwsWhenUserDoesNotOwnRecap() {
            // Arrange
            Long recapId = 1L;
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            when(recapRepository.findById(recapId)).thenReturn(Optional.of(recap));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, viewer.getId()))
                    .when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            // Act + Assert
            assertThrows(UserOwnershipException.class, () -> recapMediaService.deleteAllMediaForRecap(recapId));

            verify(recapRepository).findById(recapId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verifyNoInteractions(recapMediaRepository);
        }
    }

    @Nested
    class ReorderRecapMediaTests {

        @Test
        void reordersMediaSuccessfully() {
            // Arrange
            Long recapId = 1L;
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            RecapMedia media1 = TestUtils.createValidImageRecapMediaWithId(recap, 10L);
            RecapMedia media2 = TestUtils.createValidVideoRecapMediaWithId(recap, 20L);

            List<RecapMedia> mediaItems = List.of(media1, media2);
            List<Long> orderedIds = List.of(20L, 10L);

            when(recapRepository.findById(recapId)).thenReturn(Optional.of(recap));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(recapMediaRepository.findByRecapId(recapId)).thenReturn(mediaItems);

            // Act
            recapMediaService.reorderRecapMedia(recapId, orderedIds);

            // Assert
            assertEquals(1, media1.getMediaOrder()); // swapped
            assertEquals(0, media2.getMediaOrder());

            verify(recapRepository).findById(recapId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verify(recapMediaRepository).findByRecapId(recapId);
            verify(recapMediaRepository).saveAll(mediaItems);
        }

        @Test
        void throwsWhenRecapNotFound() {
            // Arrange
            Long recapId = 1L;
            when(recapRepository.findById(recapId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(EventRecapNotFoundException.class,
                    () -> recapMediaService.reorderRecapMedia(recapId, List.of(1L)));

            verify(recapRepository).findById(recapId);
            verifyNoInteractions(authenticatedUserProvider);
            verifyNoInteractions(ownershipValidator);
            verifyNoInteractions(recapMediaRepository);
        }

        @Test
        void throwsWhenUserDoesNotOwnRecap() {
            // Arrange
            Long recapId = 1L;
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            when(recapRepository.findById(recapId)).thenReturn(Optional.of(recap));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doThrow(new UserOwnershipException(UNAUTHORIZED_USER_ACCESS, viewer.getId()))
                    .when(ownershipValidator).validateEventOwnership(viewer.getId(), event);

            // Act + Assert
            assertThrows(UserOwnershipException.class,
                    () -> recapMediaService.reorderRecapMedia(recapId, List.of(1L)));

            verify(recapRepository).findById(recapId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verifyNoInteractions(recapMediaRepository);
        }

        @Test
        void throwsWhenOrderedIdsSizeDoesNotMatch() {
            // Arrange
            Long recapId = 1L;
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            RecapMedia media1 = TestUtils.createValidImageRecapMediaWithId(recap, 10L);

            when(recapRepository.findById(recapId)).thenReturn(Optional.of(recap));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(recapMediaRepository.findByRecapId(recapId)).thenReturn(List.of(media1));

            // Act + Assert
            assertThrows(IncompleteRecapMediaReorderListException.class,
                    () -> recapMediaService.reorderRecapMedia(recapId, List.of(10L, 20L)));

            verify(recapRepository).findById(recapId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verify(recapMediaRepository).findByRecapId(recapId);
            verifyNoMoreInteractions(recapMediaRepository);
        }

        @Test
        void throwsWhenOrderedIdsContainUnknownId() {
            // Arrange
            Long recapId = 1L;
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            RecapMedia media1 = TestUtils.createValidImageRecapMediaWithId(recap, 10L);

            when(recapRepository.findById(recapId)).thenReturn(Optional.of(recap));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(recapMediaRepository.findByRecapId(recapId)).thenReturn(List.of(media1));

            // Act + Assert
            assertThrows(IncompleteRecapMediaReorderListException.class,
                    () -> recapMediaService.reorderRecapMedia(recapId, List.of(999L)));

            verify(recapRepository).findById(recapId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verify(recapMediaRepository).findByRecapId(recapId);
            verifyNoMoreInteractions(recapMediaRepository);
        }

        @Test
        void reordersEmptyMediaListSuccessfully() {
            // Arrange
            Long recapId = 1L;
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            List<RecapMedia> emptyMediaList = List.of();
            List<Long> emptyOrderedIds = List.of();

            when(recapRepository.findById(recapId)).thenReturn(Optional.of(recap));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(recapMediaRepository.findByRecapId(recapId)).thenReturn(emptyMediaList);

            // Act
            recapMediaService.reorderRecapMedia(recapId, emptyOrderedIds);

            // Assert
            verify(recapRepository).findById(recapId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verify(recapMediaRepository).findByRecapId(recapId);
            verify(recapMediaRepository).saveAll(emptyMediaList);
        }

        @Test
        void throwsWhenOrderedIdsContainDuplicates() {
            // Arrange
            Long recapId = 1L;
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            RecapMedia media1 = TestUtils.createValidImageRecapMediaWithId(recap, 10L);
            RecapMedia media2 = TestUtils.createValidVideoRecapMediaWithId(recap, 20L);

            List<RecapMedia> mediaItems = List.of(media1, media2);
            List<Long> duplicateIds = List.of(10L, 10L); // Duplicate ID

            when(recapRepository.findById(recapId)).thenReturn(Optional.of(recap));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(recapMediaRepository.findByRecapId(recapId)).thenReturn(mediaItems);

            // Act + Assert
            assertThrows(IncompleteRecapMediaReorderListException.class,
                    () -> recapMediaService.reorderRecapMedia(recapId, duplicateIds));

            verify(recapRepository).findById(recapId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verify(recapMediaRepository).findByRecapId(recapId);
            verifyNoMoreInteractions(recapMediaRepository);
        }

        @Test
        void throwsWhenOrderedListIsMissingMediaId() {
            // Arrange
            Long recapId = 1L;
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            RecapMedia media1 = TestUtils.createValidImageRecapMediaWithId(recap, 10L);
            RecapMedia media2 = TestUtils.createValidVideoRecapMediaWithId(recap, 20L);

            List<RecapMedia> mediaItems = List.of(media1, media2);
            List<Long> incompleteIds = List.of(10L); // Missing media2's ID

            when(recapRepository.findById(recapId)).thenReturn(Optional.of(recap));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(recapMediaRepository.findByRecapId(recapId)).thenReturn(mediaItems);

            // Act + Assert
            assertThrows(IncompleteRecapMediaReorderListException.class,
                    () -> recapMediaService.reorderRecapMedia(recapId, incompleteIds));

            verify(recapRepository).findById(recapId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verify(recapMediaRepository).findByRecapId(recapId);
            verifyNoMoreInteractions(recapMediaRepository);
        }

        @Test
        void throwsIncompleteReorderListWhenOrderedIdsContainUnknownId() {
            // This tests validation where ordered list contains IDs not in existing media
            Long recapId = 1L;
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            // Create a media item with ID 10L
            RecapMedia media1 = TestUtils.createValidImageRecapMediaWithId(recap, 10L);
            
            // But ordered list contains 20L (which isn't in the media items)
            List<RecapMedia> mediaItems = List.of(media1);
            List<Long> orderedIds = List.of(20L); // ID not in mediaItems

            when(recapRepository.findById(recapId)).thenReturn(Optional.of(recap));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(recapMediaRepository.findByRecapId(recapId)).thenReturn(mediaItems);

            // Act + Assert
            // This should throw IncompleteRecapMediaReorderListException during validation
            assertThrows(IncompleteRecapMediaReorderListException.class,
                    () -> recapMediaService.reorderRecapMedia(recapId, orderedIds));

            verify(recapRepository).findById(recapId);
            verify(authenticatedUserProvider).getCurrentUser();
            verify(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            verify(recapMediaRepository).findByRecapId(recapId);
            verifyNoMoreInteractions(recapMediaRepository);
        }
    }

    @Nested
    class MediaTypeSpecificTests {

        @Test
        void handlesAudioMediaTypeCorrectly() {
            // Arrange
            Long recapId = 1L;
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            RecapMediaCreateDTO audioDto = new RecapMediaCreateDTO(
                    "https://example.com/audio.mp3",
                    RecapMediaType.AUDIO,
                    120, // 2 minutes duration
                    0
            );

            RecapMedia audioMedia = new RecapMedia(recap, audioDto.mediaUrl(), audioDto.mediaType(), audioDto.durationSeconds(), audioDto.mediaOrder());
            RecapMediaResponseDTO expectedResponse = new RecapMediaResponseDTO(1L, audioDto.mediaUrl(), audioDto.mediaType(), audioDto.durationSeconds(), audioDto.mediaOrder());

            when(recapRepository.findById(recapId)).thenReturn(Optional.of(recap));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(recapMediaRepository.save(any(RecapMedia.class))).thenReturn(audioMedia);
            when(recapMediaMapper.toResponseDTO(any())).thenReturn(expectedResponse);

            // Act
            RecapMediaResponseDTO result = recapMediaService.addRecapMedia(recapId, audioDto);

            // Assert
            assertNotNull(result);
            assertEquals(RecapMediaType.AUDIO, result.mediaType());
            assertEquals(120, result.durationSeconds());
            RecapMediaResponseDTOAssertions.assertRecapMediaResponseDTOEquals(expectedResponse, result);

            verify(recapMediaRepository).save(argThat(media -> 
                media.getMediaType() == RecapMediaType.AUDIO && 
                media.getDurationSeconds() == 120
            ));
        }

        @Test
        void handlesImageMediaWithNullDuration() {
            // Arrange
            Long recapId = 1L;
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            RecapMediaCreateDTO imageDto = new RecapMediaCreateDTO(
                    "https://example.com/image.jpg",
                    RecapMediaType.IMAGE,
                    null, // Images should not have duration
                    0
            );

            RecapMedia imageMedia = new RecapMedia(recap, imageDto.mediaUrl(), imageDto.mediaType(), imageDto.durationSeconds(), imageDto.mediaOrder());
            RecapMediaResponseDTO expectedResponse = new RecapMediaResponseDTO(1L, imageDto.mediaUrl(), imageDto.mediaType(), null, imageDto.mediaOrder());

            when(recapRepository.findById(recapId)).thenReturn(Optional.of(recap));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(recapMediaRepository.save(any(RecapMedia.class))).thenReturn(imageMedia);
            when(recapMediaMapper.toResponseDTO(any())).thenReturn(expectedResponse);

            // Act
            RecapMediaResponseDTO result = recapMediaService.addRecapMedia(recapId, imageDto);

            // Assert
            assertNotNull(result);
            assertEquals(RecapMediaType.IMAGE, result.mediaType());
            assertNull(result.durationSeconds());
            RecapMediaResponseDTOAssertions.assertRecapMediaResponseDTOEquals(expectedResponse, result);

            verify(recapMediaRepository).save(argThat(media -> 
                media.getMediaType() == RecapMediaType.IMAGE && 
                media.getDurationSeconds() == null
            ));
        }

        @Test
        void handlesVideoMediaWithDuration() {
            // Arrange
            Long recapId = 1L;
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            RecapMediaCreateDTO videoDto = new RecapMediaCreateDTO(
                    "https://example.com/video.mp4",
                    RecapMediaType.VIDEO,
                    300, // 5 minutes duration
                    0
            );

            RecapMedia videoMedia = new RecapMedia(recap, videoDto.mediaUrl(), videoDto.mediaType(), videoDto.durationSeconds(), videoDto.mediaOrder());
            RecapMediaResponseDTO expectedResponse = new RecapMediaResponseDTO(1L, videoDto.mediaUrl(), videoDto.mediaType(), videoDto.durationSeconds(), videoDto.mediaOrder());

            when(recapRepository.findById(recapId)).thenReturn(Optional.of(recap));
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            doNothing().when(ownershipValidator).validateEventOwnership(viewer.getId(), event);
            when(recapMediaRepository.save(any(RecapMedia.class))).thenReturn(videoMedia);
            when(recapMediaMapper.toResponseDTO(any())).thenReturn(expectedResponse);

            // Act
            RecapMediaResponseDTO result = recapMediaService.addRecapMedia(recapId, videoDto);

            // Assert
            assertNotNull(result);
            assertEquals(RecapMediaType.VIDEO, result.mediaType());
            assertEquals(300, result.durationSeconds());
            RecapMediaResponseDTOAssertions.assertRecapMediaResponseDTOEquals(expectedResponse, result);

            verify(recapMediaRepository).save(argThat(media -> 
                media.getMediaType() == RecapMediaType.VIDEO && 
                media.getDurationSeconds() == 300
            ));
        }

        @Test
        void handlesAllMediaTypesInBulkOperation() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            Event event = TestUtils.createValidCompletedEventWithId(EVENT_ID, viewer, fixedClock);
            EventRecap recap = TestUtils.createValidEventRecap(event);

            List<RecapMediaCreateDTO> mixedMediaList = List.of(
                    new RecapMediaCreateDTO("https://example.com/image.jpg", RecapMediaType.IMAGE, null, 0),
                    new RecapMediaCreateDTO("https://example.com/video.mp4", RecapMediaType.VIDEO, 180, 1),
                    new RecapMediaCreateDTO("https://example.com/audio.mp3", RecapMediaType.AUDIO, 240, 2)
            );

            // Act
            recapMediaService.addMediaItemsToRecap(recap, mixedMediaList);

            // Assert
            verify(recapMediaRepository).saveAll(argThat(entities -> {
                if (!(entities instanceof List)) return false;
                List<?> list = (List<?>) entities;
                if (list.size() != 3) return false;

                RecapMedia imageMedia = (RecapMedia) list.get(0);
                RecapMedia videoMedia = (RecapMedia) list.get(1);
                RecapMedia audioMedia = (RecapMedia) list.get(2);

                return imageMedia.getMediaType() == RecapMediaType.IMAGE && imageMedia.getDurationSeconds() == null &&
                       videoMedia.getMediaType() == RecapMediaType.VIDEO && videoMedia.getDurationSeconds() == 180 &&
                       audioMedia.getMediaType() == RecapMediaType.AUDIO && audioMedia.getDurationSeconds() == 240;
            }));
        }
    }

}
