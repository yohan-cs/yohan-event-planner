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

    /**
     * Constructs a new BadgeService implementation with required dependencies.
     * 
     * <p>This constructor establishes the service's integration with the data layer,
     * security framework, statistics service, and mapping infrastructure. All
     * dependencies are required for proper service operation.</p>
     * 
     * @param badgeRepository repository for badge data access operations
     * @param badgeStatsService service for computing aggregated time statistics across badge labels
     * @param labelService service for label validation and retrieval operations
     * @param ownershipValidator validator for ensuring proper authorization and user ownership
     * @param authenticatedUserProvider provider for current user context and authentication state
     * @param badgeMapper mapper for converting between domain entities and DTOs
     */
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

    /**
     * {@inheritDoc}
     * 
     * <p>This implementation fetches the badge entity, computes statistics through
     * the BadgeStatsService, resolves associated label information, and maps the
     * complete data to a response DTO.</p>
     */
    @Override
    public BadgeResponseDTO getBadgeById(Long badgeId) {
        logger.debug("Retrieving badge with ID: {}", badgeId);
        
        // Fetch the badge from the repository
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> {
                    logger.warn("Badge not found with ID: {}", badgeId);
                    return new BadgeNotFoundException(badgeId);
                });

        // Compute stats and resolve labels for the badge
        TimeStatsDTO stats = badgeStatsService.computeStatsForBadge(badge, badge.getUser().getId());
        var resolvedLabels = resolveLabelsForBadge(badge);

        logger.debug("Successfully retrieved badge ID: {} for user: {}", badgeId, badge.getUser().getId());
        return badgeMapper.toResponseDTO(badge, stats, resolvedLabels);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>This implementation fetches badges using the repository's ordering query,
     * computes statistics for each badge, and resolves label information to provide
     * complete badge data sorted by user preference.</p>
     */
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

    /**
     * {@inheritDoc}
     * 
     * <p>This implementation creates a new badge entity with the next available sort order,
     * validates any provided label associations, saves the badge, and returns the complete
     * badge information with computed statistics.</p>
     */
    @Override
    @Transactional
    public BadgeResponseDTO createBadge(BadgeCreateDTO createRequest) {
        logger.info("Creating new badge '{}' with {} label associations", createRequest.name(), 
                   createRequest.labelIds() != null ? createRequest.labelIds().size() : 0);
        User creator = authenticatedUserProvider.getCurrentUser();

        int sortOrder = badgeRepository.findMaxSortOrderByUserId(creator.getId()).orElse(-1) + 1;
        logger.debug("Assigning sort order {} to new badge for user: {}", sortOrder, creator.getId());

        Badge badge = new Badge(createRequest.name(), creator, sortOrder);
        Set<Long> resolvedLabelIds = new HashSet<>();

        if (createRequest.labelIds() != null && !createRequest.labelIds().isEmpty()) {
            logger.debug("Validating {} label associations for badge creation", createRequest.labelIds().size());
            labelService.validateExistenceAndOwnership(createRequest.labelIds(), creator.getId());
            resolvedLabelIds.addAll(createRequest.labelIds());
        }

        badge.addLabelIds(resolvedLabelIds);
        Badge savedBadge = badgeRepository.save(badge);
        TimeStatsDTO stats = badgeStatsService.computeStatsForBadge(savedBadge, creator.getId());
        var resolvedLabels = resolveLabelsForBadge(savedBadge);

        logger.info("Successfully created badge '{}' with ID: {} for user: {}", 
                   createRequest.name(), savedBadge.getId(), creator.getId());
        return badgeMapper.toResponseDTO(savedBadge, stats, resolvedLabels);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>This implementation fetches the badge, validates ownership, applies any
     * provided updates, and returns the updated badge with current statistics
     * and resolved label information.</p>
     */
    @Override
    @Transactional
    public BadgeResponseDTO updateBadge(Long badgeId, BadgeUpdateDTO updateRequest) {
        logger.debug("Updating badge ID: {} with data: {}", badgeId, updateRequest);
        User currentUser = authenticatedUserProvider.getCurrentUser();

        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> {
                    logger.warn("Badge not found with ID: {} during update", badgeId);
                    return new BadgeNotFoundException(badgeId);
                });

        ownershipValidator.validateBadgeOwnership(currentUser.getId(), badge);

        if (updateRequest.name() != null) {
            String oldName = badge.getName();
            badge.setName(updateRequest.name());
            logger.debug("Updated badge ID: {} name from '{}' to '{}'", badgeId, oldName, updateRequest.name());
        }

        TimeStatsDTO stats = badgeStatsService.computeStatsForBadge(badge, badge.getUser().getId());
        var resolvedLabels = resolveLabelsForBadge(badge);

        logger.info("Successfully updated badge ID: {} for user: {}", badgeId, currentUser.getId());
        return badgeMapper.toResponseDTO(badge, stats, resolvedLabels);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>This implementation fetches the badge, validates ownership, and performs
     * the deletion. Associated label relationships are automatically removed,
     * but the labels themselves remain unchanged.</p>
     */
    @Override
    @Transactional
    public void deleteBadge(Long badgeId) {
        logger.info("Deleting badge ID: {}", badgeId);
        User currentUser = authenticatedUserProvider.getCurrentUser();
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> {
                    logger.warn("Badge not found with ID: {} during deletion", badgeId);
                    return new BadgeNotFoundException(badgeId);
                });

        ownershipValidator.validateBadgeOwnership(currentUser.getId(), badge);
        
        logger.debug("Deleting badge '{}' with {} label associations", badge.getName(), badge.getLabelIds().size());
        badgeRepository.delete(badge);
        logger.info("Successfully deleted badge ID: {} for user: {}", badgeId, currentUser.getId());
    }

    /**
     * {@inheritDoc}
     * 
     * <p>This implementation performs comprehensive validation including existence checks,
     * ownership validation, and completeness verification before applying the new sort
     * order to all badges. The operation ensures data consistency by requiring all
     * user-owned badges to be included in the reorder request.</p>
     */
    @Override
    @Transactional
    public void reorderBadges(Long userId, List<Long> orderedBadgeIds) {
        if (orderedBadgeIds == null) {
            logger.warn("Badge reorder failed - null badge list for user: {}", userId);
            throw new IncompleteBadgeReorderListException();
        }

        // Check if user has any badges - if not, empty list is valid
        List<Badge> userBadges = badgeRepository.findByUserId(userId);
        if (userBadges.isEmpty() && orderedBadgeIds.isEmpty()) {
            logger.info("User {} has no badges - empty reorder list is valid", userId);
            return;
        }

        if (orderedBadgeIds.isEmpty()) {
            logger.warn("Badge reorder failed - empty badge list for user {} who has {} badges", userId, userBadges.size());
            throw new IncompleteBadgeReorderListException();
        }

        logger.info("Reordering {} badges for user: {}", orderedBadgeIds.size(), userId);
        logger.debug("Badge reorder sequence: {}", orderedBadgeIds);

        // Fetch and validate badges
        List<Badge> requestedBadges = badgeRepository.findAllById(orderedBadgeIds);
        validateBadgeExistenceAndOwnership(new HashSet<>(orderedBadgeIds), userId, requestedBadges);

        // Ensure completeness - all user badges must be included
        validateReorderCompleteness(userId, orderedBadgeIds);

        // Apply new sort order and save
        applyBadgeSortOrder(requestedBadges, orderedBadgeIds);
        badgeRepository.saveAll(requestedBadges);
        
        logger.info("Successfully reordered badges for user: {}", userId);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>This implementation validates badge ownership, ensures all currently associated
     * labels are included in the reorder request, and applies the new label order.
     * The operation maintains referential integrity by requiring complete label lists.</p>
     */
    @Override
    @Transactional
    public void reorderBadgeLabels(Long badgeId, List<Long> orderedLabelIds) {
        if (orderedLabelIds == null || orderedLabelIds.isEmpty()) {
            logger.warn("Badge label reorder failed - empty or null label list for badge: {}", badgeId);
            throw new IncompleteBadgeLabelReorderListException();
        }

        logger.info("Reordering {} labels for badge ID: {}", orderedLabelIds.size(), badgeId);
        logger.debug("Label reorder sequence for badge {}: {}", badgeId, orderedLabelIds);

        User currentUser = authenticatedUserProvider.getCurrentUser();

        // Fetch the badge
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> {
                    logger.warn("Badge not found with ID: {} during label reorder", badgeId);
                    return new BadgeNotFoundException(badgeId);
                });

        // Validate ownership
        ownershipValidator.validateBadgeOwnership(currentUser.getId(), badge);

        // Get existing label IDs from badge
        Set<Long> existingLabelIds = new HashSet<>(badge.getLabelOrder());
        logger.debug("Badge {} currently has {} labels: {}", badgeId, existingLabelIds.size(), existingLabelIds);

        // Check that all provided IDs exist in current label set
        Set<Long> providedLabelIds = new HashSet<>(orderedLabelIds);
        if (!existingLabelIds.containsAll(providedLabelIds)) {
            // Found a label ID that doesn't belong to this badge
            logger.warn("Invalid label reorder for badge: {}. Existing: {}, Provided: {}", 
                       badgeId, existingLabelIds, providedLabelIds);
            throw new IncompleteBadgeLabelReorderListException();
        }

        // Ensure no missing labels (completeness check)
        if (!providedLabelIds.containsAll(existingLabelIds)) {
            logger.warn("Incomplete label reorder for badge: {}. Missing labels in request. Existing: {}, Provided: {}", 
                       badgeId, existingLabelIds, providedLabelIds);
            throw new IncompleteBadgeLabelReorderListException();
        }

        // Apply new order
        badge.setLabelOrder(orderedLabelIds);

        badgeRepository.save(badge);
        logger.info("Successfully reordered labels for badge ID: {}", badgeId);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>This implementation uses batch retrieval for efficient validation of multiple
     * badges, checking both existence and ownership in a single database query followed
     * by validation logic.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public void validateExistenceAndOwnership(Set<Long> badgeIds, Long userId) {
        logger.debug("Validating existence and ownership of {} badges for user: {}", badgeIds.size(), userId);
        
        List<Badge> badges = badgeRepository.findAllById(badgeIds);
        Map<Long, Badge> badgeMap = badges.stream()
                .collect(Collectors.toMap(Badge::getId, Function.identity()));

        logger.debug("Found {} out of {} requested badges in database", badges.size(), badgeIds.size());

        for (Long badgeId : badgeIds) {
            Badge badge = badgeMap.get(badgeId);
            if (badge == null) {
                logger.warn("Badge validation failed - badge not found: {} for user: {}", badgeId, userId);
                throw new BadgeNotFoundException(badgeId);
            }
            ownershipValidator.validateBadgeOwnership(userId, badge);
        }
        
        logger.debug("Successfully validated all {} badges for user: {}", badgeIds.size(), userId);
    }

    /**
     * Validates that all badge IDs exist and are owned by the specified user.
     * 
     * <p>This method provides comprehensive validation for badge operations by checking
     * both existence and ownership in a single operation. It throws specific exceptions
     * for different failure scenarios to provide clear error context.</p>
     * 
     * @param badgeIds the badge IDs to validate
     * @param userId the user ID to validate ownership against  
     * @param badges the fetched badges to validate
     * @throws BadgeNotFoundException if any badge is missing
     * @throws BadgeOwnershipException if any badge isn't owned by the user
     */
    private void validateBadgeExistenceAndOwnership(Set<Long> badgeIds, Long userId, List<Badge> badges) {
        Map<Long, Badge> badgeMap = badges.stream()
                .collect(Collectors.toMap(Badge::getId, Function.identity()));

        for (Long badgeId : badgeIds) {
            Badge badge = badgeMap.get(badgeId);
            if (badge == null) {
                logger.warn("Badge reorder failed - badge not found: {} for user: {}", badgeId, userId);
                throw new BadgeNotFoundException(badgeId);
            }
            if (!badge.getUser().getId().equals(userId)) {
                logger.warn("Badge ownership violation during reorder for user: {}. Badge: {} owned by: {}", 
                           userId, badge.getId(), badge.getUser().getId());
                throw new BadgeOwnershipException(badge.getId(), userId);
            }
        }
    }

    /**
     * Validates that the reorder request includes all user-owned badges for completeness.
     * 
     * <p>This validation ensures data consistency by requiring that reorder operations
     * include ALL badges owned by the user, preventing partial reordering that could
     * lead to inconsistent sort orders.</p>
     * 
     * @param userId the user whose badges are being reordered
     * @param orderedBadgeIds the complete list of badge IDs in the reorder request
     * @throws IncompleteBadgeReorderListException if not all user badges are included
     */
    private void validateReorderCompleteness(Long userId, List<Long> orderedBadgeIds) {
        List<Badge> userBadges = badgeRepository.findByUserId(userId);
        Set<Long> userBadgeIds = userBadges.stream()
                .map(Badge::getId)
                .collect(Collectors.toSet());

        if (!new HashSet<>(orderedBadgeIds).equals(userBadgeIds)) {
            logger.warn("Badge reorder failed - incomplete badge list for user: {}. Requested: {}, User owns: {}", 
                       userId, orderedBadgeIds.size(), userBadgeIds.size());
            throw new IncompleteBadgeReorderListException();
        }
    }

    /**
     * Applies sort order to badges based on their position in the ordered list.
     * 
     * <p>This method updates the sort order of badges to match their position in the
     * provided ordered list, with the first badge receiving sort order 0, the second
     * receiving sort order 1, and so on.</p>
     * 
     * @param badges the badges to update with new sort orders
     * @param orderedIds the list of badge IDs in desired order
     */
    private void applyBadgeSortOrder(List<Badge> badges, List<Long> orderedIds) {
        Map<Long, Badge> badgeMap = badges.stream()
                .collect(Collectors.toMap(Badge::getId, Function.identity()));
        
        for (int i = 0; i < orderedIds.size(); i++) {
            badgeMap.get(orderedIds.get(i)).setSortOrder(i);
        }
    }

    /**
     * Resolves label details for all labels associated with a badge.
     * 
     * <p>This method fetches complete label information for all label IDs associated
     * with the badge and converts them to simplified DTOs for inclusion in badge responses.
     * The method leverages the LabelService to ensure proper access control and data retrieval.</p>
     * 
     * @param badge the badge whose labels to resolve
     * @return set of label DTOs with ID, name, and color information, empty set if no labels associated
     */
    private Set<BadgeLabelDTO> resolveLabelsForBadge(Badge badge) {
        return labelService.getLabelsByIds(badge.getLabelIds()).stream()
                .map(label -> new BadgeLabelDTO(label.getId(), label.getName(), label.getColor()))
                .collect(Collectors.toSet());
    }
}

