package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.Badge;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.BadgeCreateDTO;
import com.yohan.event_planner.dto.BadgeResponseDTO;
import com.yohan.event_planner.dto.BadgeUpdateDTO;
import com.yohan.event_planner.dto.TimeStatsDTO;
import com.yohan.event_planner.exception.BadgeNotFoundException;
import com.yohan.event_planner.exception.BadgeOwnershipException;
import com.yohan.event_planner.exception.IncompleteBadgeLabelReorderListException;
import com.yohan.event_planner.mapper.BadgeMapper;
import com.yohan.event_planner.repository.BadgeRepository;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import static com.yohan.event_planner.util.TestConstants.VALID_BADGE_NAME;
import static com.yohan.event_planner.util.TestConstants.VALID_BADGE_NAME_OTHER;
import static com.yohan.event_planner.util.TestConstants.VALID_BADGE_NAME_THIRD;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class BadgeServiceImplTest {

    private BadgeRepository badgeRepository;
    private BadgeStatsService badgeStatsService;
    private LabelService labelService;
    private OwnershipValidator ownershipValidator;
    private AuthenticatedUserProvider authenticatedUserProvider;
    private BadgeMapper badgeMapper;
    private BadgeServiceImpl badgeService;
    private User testUser;

    @BeforeEach
    void setUp() {
        badgeRepository = mock(BadgeRepository.class);
        badgeStatsService = mock(BadgeStatsService.class);
        labelService = mock(LabelService.class);
        ownershipValidator = mock(OwnershipValidator.class);
        authenticatedUserProvider = mock(AuthenticatedUserProvider.class);
        badgeMapper = mock(BadgeMapper.class);
        badgeService = new BadgeServiceImpl(
                badgeRepository,
                badgeStatsService,
                labelService,
                ownershipValidator,
                authenticatedUserProvider,
                badgeMapper
        );
        testUser = TestUtils.createValidUserEntityWithId();
        when(authenticatedUserProvider.getCurrentUser()).thenReturn(testUser);
    }

    @Nested
    class GetBadgesByUserTests {

        @Test
        void returnsEmptyList_whenNoBadgesExist() {
            // Arrange
            when(badgeRepository.findByUserIdOrderBySortOrderAsc(testUser.getId()))
                    .thenReturn(List.of());

            // Act
            List<BadgeResponseDTO> result = badgeService.getBadgesByUser(testUser.getId());

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        void returnsSingleBadgeWithStats_whenOneBadgeExists() {
            // Arrange
            Badge badge = TestUtils.createValidBadgeWithLabelIds(testUser, Set.of(100L));
            TestUtils.setBadgeId(badge, 1L);

            TimeStatsDTO stats = new TimeStatsDTO(10, 20, 30, 5, 15, 50);
            BadgeResponseDTO dto = mock(BadgeResponseDTO.class);

            when(badgeRepository.findByUserIdOrderBySortOrderAsc(testUser.getId()))
                    .thenReturn(List.of(badge));
            when(badgeStatsService.computeStatsForBadge(badge, testUser.getId()))
                    .thenReturn(stats);
            when(badgeMapper.toResponseDTO(eq(badge), eq(stats), anySet()))
                    .thenReturn(dto);

            // Act
            List<BadgeResponseDTO> result = badgeService.getBadgesByUser(testUser.getId());

            // Assert
            assertThat(result).containsExactly(dto);
        }

        @Test
        void returnsBadgesInCorrectOrder_whenMultipleExist() {
            // Arrange
            Badge badge1 = TestUtils.createValidBadgeWithLabelIds(testUser, Set.of(100L));
            Badge badge2 = TestUtils.createValidBadgeWithLabelIds(testUser, Set.of(200L));
            badge1.setSortOrder(0);
            badge2.setSortOrder(1);
            TestUtils.setBadgeId(badge1, 1L);
            TestUtils.setBadgeId(badge2, 2L);

            TimeStatsDTO stats1 = new TimeStatsDTO(1, 2, 3, 4, 5, 6);
            TimeStatsDTO stats2 = new TimeStatsDTO(6, 5, 4, 3, 2, 1);
            BadgeResponseDTO dto1 = mock(BadgeResponseDTO.class);
            BadgeResponseDTO dto2 = mock(BadgeResponseDTO.class);

            when(badgeRepository.findByUserIdOrderBySortOrderAsc(testUser.getId()))
                    .thenReturn(List.of(badge1, badge2));
            when(badgeStatsService.computeStatsForBadge(badge1, testUser.getId()))
                    .thenReturn(stats1);
            when(badgeStatsService.computeStatsForBadge(badge2, testUser.getId()))
                    .thenReturn(stats2);
            when(badgeMapper.toResponseDTO(eq(badge1), eq(stats1), anySet())).thenReturn(dto1);
            when(badgeMapper.toResponseDTO(eq(badge2), eq(stats2), anySet())).thenReturn(dto2);

            // Act
            List<BadgeResponseDTO> result = badgeService.getBadgesByUser(testUser.getId());

            // Assert
            assertThat(result).containsExactly(dto1, dto2);
        }
    }

    @Nested
    class CreateBadgeTests {

        @Test
        void createsBadge_withValidLabels() {
            // Arrange
            Set<Long> labelIds = Set.of(100L, 101L);
            BadgeCreateDTO dto = new BadgeCreateDTO("Test Badge", labelIds);

            Badge badgeToSave = TestUtils.createValidBadgeWithLabelIds(testUser, labelIds);
            Badge savedBadge = TestUtils.createValidBadgeWithLabelIds(testUser, labelIds);
            TestUtils.setBadgeId(savedBadge, 1L);

            TimeStatsDTO stats = new TimeStatsDTO(5, 10, 15, 2, 4, 25);
            BadgeResponseDTO responseDTO = mock(BadgeResponseDTO.class);

            when(badgeRepository.findMaxSortOrderByUserId(testUser.getId())).thenReturn(Optional.of(2));
            when(badgeRepository.save(any(Badge.class))).thenReturn(savedBadge);
            when(badgeStatsService.computeStatsForBadge(savedBadge, testUser.getId())).thenReturn(stats);
            when(badgeMapper.toResponseDTO(eq(savedBadge), eq(stats), anySet())).thenReturn(responseDTO);

            // Act
            BadgeResponseDTO result = badgeService.createBadge(dto);

            // Assert
            verify(labelService).validateExistenceAndOwnership(labelIds, testUser.getId());
            assertThat(result).isEqualTo(responseDTO);
        }

        @Test
        void createsBadge_withEmptyTracking() {
            // Arrange
            BadgeCreateDTO dto = new BadgeCreateDTO("Empty Badge", null);

            Badge savedBadge = TestUtils.createEmptyBadge(testUser, "Empty Badge");
            TestUtils.setBadgeId(savedBadge, 1L);

            TimeStatsDTO stats = new TimeStatsDTO(0, 0, 0, 0, 0, 0);
            BadgeResponseDTO responseDTO = mock(BadgeResponseDTO.class);

            when(badgeRepository.findMaxSortOrderByUserId(testUser.getId())).thenReturn(Optional.empty());
            when(badgeRepository.save(any(Badge.class))).thenReturn(savedBadge);
            when(badgeStatsService.computeStatsForBadge(savedBadge, testUser.getId())).thenReturn(stats);
            when(badgeMapper.toResponseDTO(eq(savedBadge), eq(stats), anySet())).thenReturn(responseDTO);

            // Act
            BadgeResponseDTO result = badgeService.createBadge(dto);

            // Assert
            verify(labelService, never()).validateExistenceAndOwnership(any(), anyLong());
            assertThat(result).isEqualTo(responseDTO);
        }

        @Test
        void createsBadge_withOnlyLabels() {
            // Arrange
            Set<Long> labelIds = Set.of(100L);
            BadgeCreateDTO dto = new BadgeCreateDTO("Label Badge", labelIds);

            Badge savedBadge = TestUtils.createValidBadgeWithLabelIds(testUser, labelIds);
            TestUtils.setBadgeId(savedBadge, 1L);

            TimeStatsDTO stats = new TimeStatsDTO(3, 6, 9, 1, 2, 15);
            BadgeResponseDTO responseDTO = mock(BadgeResponseDTO.class);

            when(badgeRepository.findMaxSortOrderByUserId(testUser.getId())).thenReturn(Optional.of(0));
            when(badgeRepository.save(any(Badge.class))).thenReturn(savedBadge);
            when(badgeStatsService.computeStatsForBadge(savedBadge, testUser.getId())).thenReturn(stats);
            when(badgeMapper.toResponseDTO(eq(savedBadge), eq(stats), anySet())).thenReturn(responseDTO);

            // Act
            BadgeResponseDTO result = badgeService.createBadge(dto);

            // Assert
            verify(labelService).validateExistenceAndOwnership(labelIds, testUser.getId());
            assertThat(result).isEqualTo(responseDTO);
        }
    }

    @Nested
    class UpdateBadgeTests {

        @Test
        void updatesBadge_nameOnly_success() {
            // Arrange
            Badge badge = TestUtils.createValidBadgeWithIdAndOwner(1L, testUser);
            BadgeUpdateDTO dto = TestUtils.createBadgeUpdateDTORenameOnly("Updated Name");

            when(badgeRepository.findById(1L)).thenReturn(Optional.of(badge));
            TimeStatsDTO stats = new TimeStatsDTO(0, 0, 0, 0, 0, 0);
            when(badgeStatsService.computeStatsForBadge(badge, testUser.getId())).thenReturn(stats);
            when(badgeMapper.toResponseDTO(eq(badge), eq(stats), eq(Set.of()))).thenReturn(mock(BadgeResponseDTO.class));

            // Act
            BadgeResponseDTO response = badgeService.updateBadge(1L, dto);

            // Assert
            assertEquals("Updated Name", badge.getName());
            verify(ownershipValidator).validateBadgeOwnership(testUser.getId(), badge);
            verify(badgeStatsService).computeStatsForBadge(badge, testUser.getId());
            verify(badgeMapper).toResponseDTO(eq(badge), eq(stats), eq(Set.of()));
        }

        @Test
        void throws_whenBadgeNotFound() {
            // Arrange
            when(badgeRepository.findById(1L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(BadgeNotFoundException.class, () ->
                    badgeService.updateBadge(1L, new BadgeUpdateDTO(null))
            );
        }
    }

    @Nested
    class DeleteBadgeTests {

        @Test
        void deletesBadgeSuccessfully_whenUserOwnsIt() {
            // Arrange
            Long badgeId = 100L;
            Badge badge = TestUtils.createValidBadgeWithIdAndOwner(badgeId, testUser);
            when(badgeRepository.findById(badgeId)).thenReturn(Optional.of(badge));

            // Act
            badgeService.deleteBadge(badgeId);

            // Assert
            verify(ownershipValidator).validateBadgeOwnership(testUser.getId(), badge);
            verify(badgeRepository).delete(badge);
        }

        @Test
        void throwsException_whenBadgeNotFound() {
            // Arrange
            Long badgeId = 404L;
            when(badgeRepository.findById(badgeId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(BadgeNotFoundException.class, () -> badgeService.deleteBadge(badgeId));
            verify(badgeRepository, never()).delete(any());
            verify(ownershipValidator, never()).validateBadgeOwnership(any(), any());
        }

        @Test
        void throwsException_whenUserDoesNotOwnBadge() {
            // Arrange
            Long badgeId = 200L;
            Badge badge = TestUtils.createValidBadgeWithIdAndOwner(badgeId, testUser);
            when(badgeRepository.findById(badgeId)).thenReturn(Optional.of(badge));
            doThrow(new BadgeOwnershipException(testUser.getId(), badge.getId()))
                    .when(ownershipValidator).validateBadgeOwnership(testUser.getId(), badge);

            // Act + Assert
            assertThrows(BadgeOwnershipException.class, () -> badgeService.deleteBadge(badgeId));
            verify(ownershipValidator).validateBadgeOwnership(testUser.getId(), badge);
            verify(badgeRepository, never()).delete(any());
        }
    }

    @Nested
    class ReorderBadgesTests {

        @Test
        void reordersBadges_successfully() {
            // Arrange
            Long badgeId1 = 100L;
            Long badgeId2 = 101L;
            Long badgeId3 = 102L;

            Badge badge1 = TestUtils.createEmptyBadge(testUser, VALID_BADGE_NAME);
            Badge badge2 = TestUtils.createEmptyBadge(testUser, VALID_BADGE_NAME_OTHER);
            Badge badge3 = TestUtils.createEmptyBadge(testUser, VALID_BADGE_NAME_THIRD);
            TestUtils.setBadgeId(badge1, badgeId1);
            TestUtils.setBadgeId(badge2, badgeId2);
            TestUtils.setBadgeId(badge3, badgeId3);

            List<Long> newOrder = List.of(badgeId3, badgeId1, badgeId2);
            List<Badge> badgeList = List.of(badge1, badge2, badge3);

            when(badgeRepository.findAllById(newOrder)).thenReturn(badgeList);
            when(badgeRepository.findByUserId(testUser.getId())).thenReturn(badgeList);

            // Act
            badgeService.reorderBadges(testUser.getId(), newOrder);

            // Assert: sortOrder was set correctly
            assertEquals(0, badge3.getSortOrder());
            assertEquals(1, badge1.getSortOrder());
            assertEquals(2, badge2.getSortOrder());

            // Verify saveAll called with correct sort orders
            verify(badgeRepository).saveAll(argThat(saved -> {
                Map<Long, Integer> expectedOrders = Map.of(
                        badgeId3, 0,
                        badgeId1, 1,
                        badgeId2, 2
                );
                return StreamSupport.stream(saved.spliterator(), false)
                        .allMatch(b -> expectedOrders.get(b.getId()).equals(b.getSortOrder()));
            }));
        }

        @Test
        void reordersBadges_failsWhenBadgeNotFound() {
            // Arrange
            Long missingId = 999L;
            when(badgeRepository.findAllById(Set.of(missingId))).thenReturn(List.of());

            // Act + Assert
            assertThrows(BadgeNotFoundException.class, () ->
                    badgeService.reorderBadges(testUser.getId(), List.of(missingId)));
        }

        @Test
        void reordersBadges_throwsIfNotOwner() {
            // Arrange
            Long badgeId = 100L;
            List<Long> inputOrder = List.of(badgeId);
            User otherUser = TestUtils.createValidUserEntityWithId(999L);
            Badge badge = TestUtils.createEmptyBadge(otherUser, VALID_BADGE_NAME);
            TestUtils.setBadgeId(badge, badgeId);

            when(badgeRepository.findAllById(inputOrder)).thenReturn(List.of(badge));
            when(badgeRepository.findByUserId(testUser.getId())).thenReturn(List.of());

            // Act + Assert
            assertThrows(BadgeOwnershipException.class, () ->
                    badgeService.reorderBadges(testUser.getId(), inputOrder));

            verify(badgeRepository, never()).saveAll(any());
        }
    }

    @Nested
    class ReorderBadgeLabelsTest {

        @Test
        void reordersBadgeLabels_successfully() {
            // Arrange
            Long badgeId = 1L;
            List<Long> existingLabelOrder = List.of(10L, 20L, 30L);
            List<Long> newLabelOrder = List.of(30L, 10L, 20L);

            Badge badge = TestUtils.createEmptyBadge(testUser, VALID_BADGE_NAME);
            TestUtils.setBadgeId(badge, badgeId);
            badge.addLabelIds(Set.copyOf(existingLabelOrder));  // add labels and sync labelOrder

            when(badgeRepository.findById(badgeId)).thenReturn(Optional.of(badge));

            // Act
            badgeService.reorderBadgeLabels(badgeId, newLabelOrder);

            // Assert
            assertEquals(newLabelOrder, badge.getLabelOrder());
            verify(ownershipValidator).validateBadgeOwnership(testUser.getId(), badge);
            verify(badgeRepository).save(badge);
        }

        @Test
        void throwsBadgeNotFoundException_whenBadgeMissing() {
            // Arrange
            Long badgeId = 404L;
            when(badgeRepository.findById(badgeId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(BadgeNotFoundException.class, () ->
                    badgeService.reorderBadgeLabels(badgeId, List.of(1L, 2L, 3L)));

            verify(badgeRepository, never()).save(any());
            verify(ownershipValidator, never()).validateBadgeOwnership(any(), any());
        }

        @Test
        void throwsBadgeOwnershipException_whenUserDoesNotOwnBadge() {
            // Arrange
            Long badgeId = 1L;
            Badge badge = TestUtils.createEmptyBadge(testUser, VALID_BADGE_NAME);
            TestUtils.setBadgeId(badge, badgeId);

            when(badgeRepository.findById(badgeId)).thenReturn(Optional.of(badge));
            doThrow(new BadgeOwnershipException(testUser.getId(), badgeId))
                    .when(ownershipValidator).validateBadgeOwnership(testUser.getId(), badge);

            // Act + Assert
            assertThrows(BadgeOwnershipException.class, () ->
                    badgeService.reorderBadgeLabels(badgeId, List.of(1L, 2L)));

            verify(ownershipValidator).validateBadgeOwnership(testUser.getId(), badge);
            verify(badgeRepository, never()).save(any());
        }

        @Test
        void throwsIncompleteBadgeLabelReorderListException_whenLabelOrderContainsInvalidLabelIds() {
            // Arrange
            Long badgeId = 1L;
            List<Long> existingLabelOrder = List.of(10L, 20L, 30L);
            List<Long> invalidNewOrder = List.of(10L, 20L, 999L);  // 999L doesn't belong

            Badge badge = TestUtils.createEmptyBadge(testUser, VALID_BADGE_NAME);
            TestUtils.setBadgeId(badge, badgeId);
            badge.addLabelIds(Set.copyOf(existingLabelOrder));

            when(badgeRepository.findById(badgeId)).thenReturn(Optional.of(badge));

            // Act + Assert
            assertThrows(IncompleteBadgeLabelReorderListException.class, () ->
                    badgeService.reorderBadgeLabels(badgeId, invalidNewOrder));

            verify(ownershipValidator).validateBadgeOwnership(testUser.getId(), badge);
            verify(badgeRepository, never()).save(any());
        }

        @Test
        void throwsIncompleteBadgeReorderListException_whenLabelOrderMissingLabels() {
            // Arrange
            Long badgeId = 1L;
            List<Long> existingLabelOrder = List.of(10L, 20L, 30L);
            List<Long> incompleteOrder = List.of(10L, 20L);  // missing 30L

            Badge badge = TestUtils.createEmptyBadge(testUser, VALID_BADGE_NAME);
            TestUtils.setBadgeId(badge, badgeId);
            badge.addLabelIds(Set.copyOf(existingLabelOrder));

            when(badgeRepository.findById(badgeId)).thenReturn(Optional.of(badge));

            // Act + Assert
            assertThrows(IncompleteBadgeLabelReorderListException.class, () ->
                    badgeService.reorderBadgeLabels(badgeId, incompleteOrder));

            verify(ownershipValidator).validateBadgeOwnership(testUser.getId(), badge);
            verify(badgeRepository, never()).save(any());
        }
    }

    @Nested
    class ValidateExistenceAndOwnershipTests {

        @Test
        void validatesOwnership_whenAllBadgesExistAndBelongToUser() {
            // Arrange
            Long badgeId1 = 1L;
            Long badgeId2 = 2L;
            Set<Long> badgeIds = Set.of(badgeId1, badgeId2);

            Badge badge1 = TestUtils.createEmptyBadge(testUser, VALID_BADGE_NAME);
            Badge badge2 = TestUtils.createEmptyBadge(testUser, VALID_BADGE_NAME_OTHER);
            TestUtils.setBadgeId(badge1, badgeId1);
            TestUtils.setBadgeId(badge2, badgeId2);

            when(badgeRepository.findAllById(badgeIds)).thenReturn(List.of(badge1, badge2));

            // Act + Assert
            assertDoesNotThrow(() -> badgeService.validateExistenceAndOwnership(badgeIds, testUser.getId()));

            verify(ownershipValidator).validateBadgeOwnership(testUser.getId(), badge1);
            verify(ownershipValidator).validateBadgeOwnership(testUser.getId(), badge2);
        }

        @Test
        void throwsBadgeNotFoundException_whenOneBadgeMissing() {
            // Arrange
            Long existingId = 1L;
            Long missingId = 999L;
            Set<Long> badgeIds = Set.of(existingId, missingId);

            Badge badge = TestUtils.createEmptyBadge(testUser, VALID_BADGE_NAME);
            TestUtils.setBadgeId(badge, existingId);

            when(badgeRepository.findAllById(badgeIds)).thenReturn(List.of(badge));

            // Act + Assert
            assertThrows(BadgeNotFoundException.class, () ->
                    badgeService.validateExistenceAndOwnership(badgeIds, testUser.getId()));
        }

        @Test
        void throwsBadgeOwnershipException_whenUserDoesNotOwnBadge() {
            // Arrange
            Long badgeId = 1L;
            Set<Long> badgeIds = Set.of(badgeId);

            Badge badge = TestUtils.createEmptyBadge(testUser, VALID_BADGE_NAME);
            TestUtils.setBadgeId(badge, badgeId);

            when(badgeRepository.findAllById(badgeIds)).thenReturn(List.of(badge));
            doThrow(new BadgeOwnershipException(testUser.getId(), badgeId))
                    .when(ownershipValidator).validateBadgeOwnership(testUser.getId(), badge);

            // Act + Assert
            assertThrows(BadgeOwnershipException.class, () ->
                    badgeService.validateExistenceAndOwnership(badgeIds, testUser.getId()));
        }
    }

}
