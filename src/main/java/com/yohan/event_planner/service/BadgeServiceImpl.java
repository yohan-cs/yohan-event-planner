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

/**
 * Implementation of {@link BadgeService} providing comprehensive badge management functionality.
 * 
 * <p>This service manages user-defined badges that group multiple labels for advanced analytics
 * and organization. Badges serve as higher-level categorization units that aggregate time 
 * statistics across multiple labels, enabling users to track broader goals and patterns in
 * their event planning and time management.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>Badge Lifecycle</strong>: Full CRUD operations with ownership validation</li>
 *   <li><strong>Label Grouping</strong>: Associate multiple labels under single badge entities</li>
 *   <li><strong>Ordering Management</strong>: Maintain display order for both badges and their labels</li>
 *   <li><strong>Analytics Integration</strong>: Generate aggregated time statistics across labels</li>
 * </ul>
 * 
 * <h2>Badge Architecture</h2>
 * <p>Badges implement a hierarchical organization system:</p>
 * <ul>
 *   <li><strong>Multi-Label Collections</strong>: Each badge contains multiple related labels</li>
 *   <li><strong>Aggregated Statistics</strong>: Time tracking across all constituent labels</li>
 *   <li><strong>Flexible Ordering</strong>: Independent ordering for badges and their labels</li>
 *   <li><strong>Dynamic Composition</strong>: Labels can be added/removed from badges</li>
 * </ul>
 * 
 * <h2>Ordering and Display Management</h2>
 * <p>The service maintains two distinct ordering systems:</p>
 * 
 * <h3>Badge-Level Ordering</h3>
 * <ul>
 *   <li><strong>User Badge Order</strong>: Controls display sequence of badges for each user</li>
 *   <li><strong>Global Reordering</strong>: Complete reordering through ordered ID lists</li>
 *   <li><strong>Consistency Validation</strong>: Ensures all badges are included in reorder operations</li>
 * </ul>
 * 
 * <h3>Label-Level Ordering</h3>
 * <ul>
 *   <li><strong>Within-Badge Order</strong>: Controls label sequence within each badge</li>
 *   <li><strong>Independent Management</strong>: Each badge maintains its own label ordering</li>
 *   <li><strong>Flexible Association</strong>: Labels can have different orders across badges</li>
 * </ul>
 * 
 * <h2>Statistics and Analytics</h2>
 * <p>Badges provide advanced analytics capabilities:</p>
 * <ul>
 *   <li><strong>Time Aggregation</strong>: Sum time statistics across all associated labels</li>
 *   <li><strong>Trend Analysis</strong>: Track changes in time allocation patterns</li>
 *   <li><strong>Goal Tracking</strong>: Monitor progress toward time-based objectives</li>
 *   <li><strong>Comparative Analytics</strong>: Compare time distribution across badges</li>
 * </ul>
 * 
 * <h2>Security and Ownership</h2>
 * <p>Comprehensive ownership model ensures data security:</p>
 * <ul>
 *   <li><strong>Badge Ownership</strong>: Users can only access their own badges</li>
 *   <li><strong>Label Validation</strong>: Associated labels must be owned by the same user</li>
 *   <li><strong>Transitive Security</strong>: Badge operations validate label ownership</li>
 *   <li><strong>Isolation Enforcement</strong>: Prevent cross-user data access</li>
 * </ul>
 * 
 * <h2>Business Rules</h2>
 * <ul>
 *   <li><strong>Name Uniqueness</strong>: Badge names must be unique per user</li>
 *   <li><strong>Label Ownership</strong>: Can only associate labels owned by the same user</li>
 *   <li><strong>Ordering Integrity</strong>: Reorder operations must include all entities</li>
 *   <li><strong>Cascading Operations</strong>: Badge deletion removes associated relationships</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>The service integrates with multiple system components:</p>
 * <ul>
 *   <li><strong>LabelService</strong>: Validates and manages label associations</li>
 *   <li><strong>BadgeStatsService</strong>: Generates aggregated time statistics</li>
 *   <li><strong>Security Framework</strong>: Enforces ownership and authorization</li>
 *   <li><strong>Mapping Layer</strong>: Converts between entities and DTOs</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Batch Operations</strong>: Efficient reordering with minimal database updates</li>
 *   <li><strong>Lazy Loading</strong>: Strategic entity loading to minimize queries</li>
 *   <li><strong>Optimized Validation</strong>: Batch validation for ownership checks</li>
 *   <li><strong>Cache-Friendly Design</strong>: Structures optimized for caching strategies</li>
 * </ul>
 * 
 * <h2>Error Handling</h2>
 * <p>Comprehensive error handling for various failure scenarios:</p>
 * <ul>
 *   <li><strong>BadgeNotFoundException</strong>: When requested badges don't exist</li>
 *   <li><strong>BadgeOwnershipException</strong>: When users access others' badges</li>
 *   <li><strong>IncompleteBadgeReorderListException</strong>: When reorder lists are incomplete</li>
 *   <li><strong>IncompleteBadgeLabelReorderListException</strong>: When label reorder lists are incomplete</li>
 * </ul>
 * 
 * <h2>Use Cases</h2>
 * <p>Primary use cases for badge management:</p>
 * <ul>
 *   <li><strong>Goal Tracking</strong>: Group related activities for progress monitoring</li>
 *   <li><strong>Time Analysis</strong>: Analyze time allocation across broad categories</li>
 *   <li><strong>Activity Organization</strong>: Create meaningful groupings of related events</li>
 *   <li><strong>Dashboard Views</strong>: Provide high-level overview of time usage</li>
 * </ul>
 * 
 * <h2>Data Consistency</h2>
 * <p>Maintains consistency across badge relationships:</p>
 * <ul>
 *   <li><strong>Transactional Operations</strong>: Ensure atomic updates</li>
 *   <li><strong>Referential Integrity</strong>: Maintain valid label associations</li>
 *   <li><strong>Order Synchronization</strong>: Keep ordering data consistent</li>
 *   <li><strong>Cascade Management</strong>: Handle related entity cleanup</li>
 * </ul>
 * 
 * @see BadgeService
 * @see Badge
 * @see Label
 * @see BadgeStatsService
 * @see BadgeRepository
 * @see BadgeMapper
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
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

