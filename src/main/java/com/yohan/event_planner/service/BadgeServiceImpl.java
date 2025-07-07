package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.Badge;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.BadgeCreateDTO;
import com.yohan.event_planner.dto.BadgeLabelDTO;
import com.yohan.event_planner.dto.BadgeResponseDTO;
import com.yohan.event_planner.dto.BadgeUpdateDTO;
import com.yohan.event_planner.dto.TimeStatsDTO;
import com.yohan.event_planner.exception.BadgeNotFoundException;
import com.yohan.event_planner.exception.BadgeOwnershipException;
import com.yohan.event_planner.exception.IncompleteBadgeLabelReorderListException;
import com.yohan.event_planner.exception.IncompleteBadgeReorderListException;
import com.yohan.event_planner.mapper.BadgeMapper;
import com.yohan.event_planner.repository.BadgeRepository;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BadgeServiceImpl implements BadgeService {

    private static final Logger logger = LoggerFactory.getLogger(BadgeServiceImpl.class);
    private final BadgeRepository badgeRepository;
    private final BadgeStatsService badgeStatsService;
    private final LabelService labelService;
    private final OwnershipValidator ownershipValidator;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final BadgeMapper badgeMapper;

    public BadgeServiceImpl(
            BadgeRepository badgeRepository,
            BadgeStatsService badgeStatsService,
            LabelService labelService,
            OwnershipValidator ownershipValidator,
            AuthenticatedUserProvider authenticatedUserProvider,
            BadgeMapper badgeMapper
    ) {
        this.badgeRepository = badgeRepository;
        this.badgeStatsService = badgeStatsService;
        this.labelService = labelService;
        this.ownershipValidator = ownershipValidator;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.badgeMapper = badgeMapper;
    }

    @Override
    public BadgeResponseDTO getBadgeById(Long badgeId) {
        // Fetch the badge from the repository
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new BadgeNotFoundException(badgeId));

        // Compute stats and resolve labels for the badge
        TimeStatsDTO stats = badgeStatsService.computeStatsForBadge(badge, badge.getUser().getId());
        var resolvedLabels = resolveLabelsForBadge(badge);

        return badgeMapper.toResponseDTO(badge, stats, resolvedLabels);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BadgeResponseDTO> getBadgesByUser(Long userId) {
        logger.debug("Fetching badges for user ID {}", userId);
        List<Badge> badges = badgeRepository.findByUserIdOrderBySortOrderAsc(userId);
        return badges.stream()
                .map(badge -> {
                    TimeStatsDTO stats = badgeStatsService.computeStatsForBadge(badge, userId);
                    var resolvedLabels = resolveLabelsForBadge(badge);
                    return badgeMapper.toResponseDTO(badge, stats, resolvedLabels);
                })
                .toList();
    }

    @Override
    @Transactional
    public BadgeResponseDTO createBadge(BadgeCreateDTO dto) {
        logger.info("Creating new badge '{}'", dto.name());
        User creator = authenticatedUserProvider.getCurrentUser();

        int sortOrder = badgeRepository.findMaxSortOrderByUserId(creator.getId()).orElse(-1) + 1;

        Badge badge = new Badge(dto.name(), creator, sortOrder);
        Set<Long> resolvedLabelIds = new HashSet<>();

        if (dto.labelIds() != null && !dto.labelIds().isEmpty()) {
            labelService.validateExistenceAndOwnership(dto.labelIds(), creator.getId());
            resolvedLabelIds.addAll(dto.labelIds());
        }

        badge.addLabelIds(resolvedLabelIds);
        Badge saved = badgeRepository.save(badge);
        TimeStatsDTO stats = badgeStatsService.computeStatsForBadge(saved, creator.getId());
        var resolvedLabels = resolveLabelsForBadge(saved);

        return badgeMapper.toResponseDTO(saved, stats, resolvedLabels);
    }

    @Override
    @Transactional
    public BadgeResponseDTO updateBadge(Long badgeId, BadgeUpdateDTO dto) {
        logger.info("Updating badge ID {}", badgeId);
        User user = authenticatedUserProvider.getCurrentUser();

        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new BadgeNotFoundException(badgeId));

        ownershipValidator.validateBadgeOwnership(user.getId(), badge);

        if (dto.name() != null) {
            badge.setName(dto.name());
        }

        TimeStatsDTO stats = badgeStatsService.computeStatsForBadge(badge, badge.getUser().getId());
        var resolvedLabels = resolveLabelsForBadge(badge);

        return badgeMapper.toResponseDTO(badge, stats, resolvedLabels);
    }

    @Override
    @Transactional
    public void deleteBadge(Long badgeId) {
        logger.info("Deleting badge ID {}", badgeId);
        User user = authenticatedUserProvider.getCurrentUser();
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new BadgeNotFoundException(badgeId));

        ownershipValidator.validateBadgeOwnership(user.getId(), badge);

        badgeRepository.delete(badge);
    }

    @Override
    @Transactional
    public void reorderBadges(Long userId, List<Long> orderedBadgeIds) {
        if (orderedBadgeIds == null || orderedBadgeIds.isEmpty()) {
            throw new IncompleteBadgeReorderListException();
        }

        // Fetch all badges in the request
        List<Badge> allBadges = badgeRepository.findAllById(orderedBadgeIds);
        Set<Long> foundBadgeIds = allBadges.stream()
                .map(Badge::getId)
                .collect(Collectors.toSet());

        // Check for missing badge IDs (truly nonexistent)
        for (Long badgeId : orderedBadgeIds) {
            if (!foundBadgeIds.contains(badgeId)) {
                throw new BadgeNotFoundException(badgeId);
            }
        }

        // Check ownership of each badge
        for (Badge badge : allBadges) {
            if (!badge.getUser().getId().equals(userId)) {
                throw new BadgeOwnershipException(badge.getId(), userId);
            }
        }

        // Fetch all badges the user owns
        List<Badge> userBadges = badgeRepository.findByUserId(userId);
        Set<Long> userBadgeIds = userBadges.stream()
                .map(Badge::getId)
                .collect(Collectors.toSet());

        // Ensure all owned badges are included (no missing owned badges in reorder list)
        if (!new HashSet<>(orderedBadgeIds).equals(userBadgeIds)) {
            throw new IncompleteBadgeReorderListException();
        }

        // Apply sort order
        Map<Long, Badge> badgeMap = allBadges.stream()
                .collect(Collectors.toMap(Badge::getId, Function.identity()));

        for (int i = 0; i < orderedBadgeIds.size(); i++) {
            Badge badge = badgeMap.get(orderedBadgeIds.get(i));
            badge.setSortOrder(i);
        }

        badgeRepository.saveAll(badgeMap.values());
    }

    @Override
    @Transactional
    public void reorderBadgeLabels(Long badgeId, List<Long> labelOrder) {
        if (labelOrder == null || labelOrder.isEmpty()) {
            throw new IncompleteBadgeLabelReorderListException();
        }

        User user = authenticatedUserProvider.getCurrentUser();

        // Fetch the badge
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new BadgeNotFoundException(badgeId));

        // Validate ownership
        ownershipValidator.validateBadgeOwnership(user.getId(), badge);

        // Get existing label IDs from badge
        Set<Long> existingLabelIds = new HashSet<>(badge.getLabelOrder());

        // Check that all provided IDs exist in current label set
        Set<Long> providedIds = new HashSet<>(labelOrder);
        if (!existingLabelIds.containsAll(providedIds)) {
            // Found a label ID that doesn't belong to this badge
            throw new IncompleteBadgeLabelReorderListException();
        }

        // Ensure no missing labels (completeness check)
        if (!providedIds.containsAll(existingLabelIds)) {
            throw new IncompleteBadgeLabelReorderListException();
        }

        // Apply new order
        badge.setLabelOrder(labelOrder);

        badgeRepository.save(badge);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateExistenceAndOwnership(Set<Long> badgeIds, Long userId) {
        List<Badge> badges = badgeRepository.findAllById(badgeIds);
        Map<Long, Badge> badgeMap = badges.stream()
                .collect(Collectors.toMap(Badge::getId, Function.identity()));

        for (Long badgeId : badgeIds) {
            Badge badge = badgeMap.get(badgeId);
            if (badge == null) {
                throw new BadgeNotFoundException(badgeId);
            }
            ownershipValidator.validateBadgeOwnership(userId, badge);
        }
    }

    private Set<BadgeLabelDTO> resolveLabelsForBadge(Badge badge) {
        return labelService.getLabelsByIds(badge.getLabelIds()).stream()
                .map(label -> new BadgeLabelDTO(label.getId(), label.getName()))
                .collect(Collectors.toSet());
    }
}

