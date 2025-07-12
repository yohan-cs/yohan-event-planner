package com.yohan.event_planner.service;

import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.LabelCreateDTO;
import com.yohan.event_planner.dto.LabelResponseDTO;
import com.yohan.event_planner.dto.LabelUpdateDTO;
import com.yohan.event_planner.exception.InvalidLabelAssociationException;
import com.yohan.event_planner.exception.LabelException;
import com.yohan.event_planner.exception.LabelNotFoundException;
import com.yohan.event_planner.exception.LabelOwnershipException;
import com.yohan.event_planner.exception.SystemManagedEntityException;
import com.yohan.event_planner.exception.UserNotFoundException;
import com.yohan.event_planner.mapper.LabelMapper;
import com.yohan.event_planner.repository.LabelRepository;
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

import static com.yohan.event_planner.exception.ErrorCode.DUPLICATE_LABEL;
import static com.yohan.event_planner.exception.ErrorCode.NULL_FIELD_NOT_ALLOWED;
import static com.yohan.event_planner.exception.ErrorCode.SYSTEM_MANAGED_LABEL;

/**
 * Implementation of {@link LabelService} providing comprehensive label management functionality.
 * 
 * <p>This service manages user-defined labels for event categorization, enforcing business rules around
 * ownership, uniqueness, and system-managed entities. Labels serve as the primary categorization mechanism
 * in the event planning system, supporting event organization, time tracking, and analytics.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>CRUD Operations</strong>: Full create, read, update, delete lifecycle management</li>
 *   <li><strong>Ownership Validation</strong>: Ensures users can only access their own labels</li>
 *   <li><strong>Uniqueness Enforcement</strong>: Prevents duplicate label names per user</li>
 *   <li><strong>System Protection</strong>: Blocks modification of system-managed labels (e.g., "Unlabeled")</li>
 * </ul>
 * 
 * <h2>Business Rules</h2>
 * <ul>
 *   <li><strong>Per-User Uniqueness</strong>: Each user can have only one label with a given name</li>
 *   <li><strong>System Label Protection</strong>: Default "Unlabeled" label cannot be modified or deleted</li>
 *   <li><strong>Ownership Isolation</strong>: Users cannot access labels owned by other users</li>
 *   <li><strong>Cascading Operations</strong>: Label deletion affects associated events and time buckets</li>
 * </ul>
 * 
 * <h2>System Integration</h2>
 * <p>This service integrates with multiple system components:</p>
 * <ul>
 *   <li><strong>Event Management</strong>: Labels categorize events and recurring events</li>
 *   <li><strong>Time Tracking</strong>: Labels generate time statistics through bucket aggregation</li>
 *   <li><strong>Badge System</strong>: Multiple labels can be grouped under badges for analytics</li>
 *   <li><strong>Search & Filtering</strong>: Labels enable event discovery and organization</li>
 * </ul>
 * 
 * <h2>Special Label Handling</h2>
 * <p>The service provides special handling for the system-managed "Unlabeled" label:</p>
 * <ul>
 *   <li>Automatically excluded from user label listings</li>
 *   <li>Cannot be updated or deleted by users</li>
 *   <li>Serves as default categorization for uncategorized events</li>
 *   <li>Maintained per-user for consistency</li>
 * </ul>
 * 
 * <h2>Error Handling</h2>
 * <p>The service throws specific exceptions for different failure scenarios:</p>
 * <ul>
 *   <li><strong>LabelNotFoundException</strong>: When requested labels don't exist</li>
 *   <li><strong>LabelOwnershipException</strong>: When users attempt to access others' labels</li>
 *   <li><strong>LabelException</strong>: For duplicate name violations</li>
 *   <li><strong>SystemManagedEntityException</strong>: For protected system label modifications</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li>Efficient batch operations for label validation</li>
 *   <li>Optimized queries for user-specific label retrieval</li>
 *   <li>Lazy loading of user relationships to minimize database hits</li>
 *   <li>Transactional boundaries for consistency</li>
 * </ul>
 * 
 * @see LabelService
 * @see Label
 * @see LabelRepository
 * @see LabelMapper
 * @see OwnershipValidator
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
@Service
public class LabelServiceImpl implements LabelService {

    private static final Logger logger = LoggerFactory.getLogger(LabelServiceImpl.class);
    private final UserBO userBO;
    private final LabelRepository labelRepository;
    private final LabelMapper labelMapper;
    private final OwnershipValidator ownershipValidator;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    /**
     * Constructs a new LabelServiceImpl with the required dependencies.
     * 
     * <p>
     * This constructor initializes the service with all necessary components for
     * label management including user operations, data persistence, mapping,
     * security validation, and authentication context.
     * </p>
     *
     * @param userBO provides user business operations and lifecycle management
     * @param labelRepository handles label data persistence and queries
     * @param labelMapper converts between domain entities and DTOs
     * @param ownershipValidator enforces label ownership security rules
     * @param authenticatedUserProvider provides current user authentication context
     */
    public LabelServiceImpl(
            UserBO userBO,
            LabelRepository labelRepository,
            LabelMapper labelMapper,
            OwnershipValidator ownershipValidator,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.userBO = userBO;
        this.labelRepository = labelRepository;
        this.labelMapper = labelMapper;
        this.ownershipValidator = ownershipValidator;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation first retrieves the label from the repository, then
     * validates that the current authenticated user owns the label before
     * returning the response DTO. This ensures proper security isolation
     * between users.
     * </p>
     * 
     * @throws LabelNotFoundException if no label exists with the given ID
     * @throws LabelOwnershipException if the label is not owned by the current user
     */
    @Override
    public LabelResponseDTO getLabelById(Long labelId) {
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new LabelNotFoundException(labelId));
        User currentUser = authenticatedUserProvider.getCurrentUser();
        ownershipValidator.validateLabelOwnership(currentUser.getId(), label);
        return labelMapper.toResponseDTO(label);
    }


    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation performs the following operations:
     * </p>
     * <ol>
     *   <li>Validates that the specified user exists using UserBO</li>
     *   <li>Retrieves all labels for the user, sorted alphabetically by name</li>
     *   <li>Filters out system-managed labels (specifically the "Unlabeled" label)</li>
     *   <li>Converts the filtered labels to response DTOs</li>
     * </ol>
     * 
     * <p>
     * The method is marked as read-only transactional to ensure consistent
     * data reads during the multi-step filtering process.
     * </p>
     * 
     * @throws UserNotFoundException if the specified user does not exist
     */
    @Override
    @Transactional(readOnly = true)
    public List<LabelResponseDTO> getLabelsByUser(Long userId) {
        logger.debug("Fetching labels for user ID {}", userId);

        // Fetch the user using UserBO and ensure it's not null by throwing an exception if not found
        User user = userBO.getUserById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Fetch all labels sorted by name
        List<Label> labels = labelRepository.findAllByCreatorIdOrderByNameAsc(userId);

        // Filter out the "Unlabeled" label by its ID (unlabeledId on User entity)
        int originalCount = labels.size();
        labels = labels.stream()
                .filter(label -> !label.getId().equals(user.getUnlabeled().getId()))  // Exclude based on the unlabeledId
                .collect(Collectors.toList());
        
        logger.debug("Filtered {} labels to {} (excluded {} system labels) for user {}", 
                     originalCount, labels.size(), originalCount - labels.size(), userId);

        return labels.stream()
                .map(labelMapper::toResponseDTO)
                .toList();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation directly retrieves the label entity from the repository
     * without performing ownership validation. This method is intended for internal
     * use where ownership has already been validated or where the calling context
     * requires access to the raw domain entity.
     * </p>
     * 
     * <p>
     * <strong>Security Note:</strong> This method does not validate ownership. 
     * Callers must ensure appropriate security checks are performed before 
     * using the returned entity.
     * </p>
     * 
     * @throws LabelNotFoundException if no label exists with the given ID
     */
    @Override
    public Label getLabelEntityById(Long labelId) {
        return labelRepository.findById(labelId)
                .orElseThrow(() -> new LabelNotFoundException(labelId));
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation handles batch retrieval efficiently by:
     * </p>
     * <ul>
     *   <li>Returning an empty set immediately for null or empty input</li>
     *   <li>Using repository batch lookup for existing IDs</li>
     *   <li>Silently ignoring non-existent label IDs</li>
     * </ul>
     * 
     * <p>
     * <strong>Security Note:</strong> This method does not validate ownership.
     * It's intended for scenarios where ownership validation is performed
     * separately, such as in bulk operations or internal service calls.
     * </p>
     */
    @Override
    public Set<Label> getLabelsByIds(Set<Long> labelIds) {
        if (labelIds == null || labelIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(labelRepository.findAllById(labelIds));
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation performs the following validation and creation steps:
     * </p>
     * <ol>
     *   <li>Retrieves the current authenticated user as the label creator</li>
     *   <li>Validates that no label with the same name already exists for the user</li>
     *   <li>Creates and persists the new label entity</li>
     *   <li>Returns the created label as a response DTO</li>
     * </ol>
     * 
     * <p>
     * The uniqueness constraint is enforced per-user, meaning different users
     * can have labels with the same name without conflict.
     * </p>
     * 
     * @throws LabelException if a label with the same name already exists for the user
     */
    @Override
    public LabelResponseDTO createLabel(LabelCreateDTO dto) {
        logger.info("Creating new label '{}'", dto.name());
        User creator = authenticatedUserProvider.getCurrentUser();

        // Trim whitespace from label name
        String trimmedName = dto.name().trim();
        if (trimmedName.isEmpty()) {
            logger.warn("Attempt to create label with empty name (after trimming) for user {}", creator.getId());
            throw new LabelException(NULL_FIELD_NOT_ALLOWED, "Label name cannot be empty");
        }

        if (labelRepository.existsByNameAndCreator(trimmedName, creator)) {
            logger.warn("Attempt to create duplicate label '{}' for user {}", trimmedName, creator.getId());
            throw new LabelException(DUPLICATE_LABEL, trimmedName);
        }

        Label savedLabel = labelRepository.save(new Label(trimmedName, creator));
        logger.debug("Successfully created label with ID {} for user {}", savedLabel.getId(), creator.getId());
        return labelMapper.toResponseDTO(savedLabel);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation provides comprehensive update functionality with the following steps:
     * </p>
     * <ol>
     *   <li>Retrieves the existing label and validates it exists</li>
     *   <li>Validates that the current user owns the label</li>
     *   <li>Ensures the label is not a system-managed label (e.g., "Unlabeled")</li>
     *   <li>Processes name updates with duplicate validation</li>
     *   <li>Only persists changes if actual modifications are made</li>
     *   <li>Returns the updated label as a response DTO</li>
     * </ol>
     * 
     * <p>
     * The method uses optimistic updating - if no actual changes are detected,
     * no database save operation is performed. This improves performance and
     * reduces unnecessary database writes.
     * </p>
     * 
     * @throws LabelNotFoundException if no label exists with the given ID
     * @throws LabelOwnershipException if the label is not owned by the current user  
     * @throws SystemManagedEntityException if attempting to modify a system-managed label
     * @throws LabelException if the new name conflicts with an existing label name for the user
     */
    @Override
    @Transactional
    public LabelResponseDTO updateLabel(Long labelId, LabelUpdateDTO dto) {
        logger.info("Updating label ID {}", labelId);
        User currentUser = authenticatedUserProvider.getCurrentUser();
        Long currentUserId = currentUser.getId();

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new LabelNotFoundException(labelId));

        ownershipValidator.validateLabelOwnership(currentUserId, label);
        requireNotUserDefaultLabel(currentUser, label);

        boolean updated = false;

        // Check if the DTO explicitly contains a null name (this would come from JSON like {"name": null})
        // We need to distinguish between omitted fields and explicitly null fields
        // Since Spring converts JSON null to Java null, we reject explicit null values
        // For true omission, the field wouldn't be in the JSON at all, but Jackson still sets it to null
        // This is a limitation of using record DTOs - we'll accept this behavior and validate at service level
        
        if (dto.name() != null) {
            // Trim whitespace from label name
            String trimmedName = dto.name().trim();
            if (trimmedName.isEmpty()) {
                logger.warn("Attempt to update label {} with empty name (after trimming) for user {}", labelId, currentUserId);
                throw new LabelException(NULL_FIELD_NOT_ALLOWED, "Label name cannot be empty");
            }
            
            if (!trimmedName.equals(label.getName())) {
                if (labelRepository.existsByNameAndCreator(trimmedName, currentUser)) {
                    logger.warn("Attempt to update label {} to duplicate name '{}' for user {}", labelId, trimmedName, currentUserId);
                    throw new LabelException(DUPLICATE_LABEL, trimmedName);
                }
                logger.debug("Updating label {} name from '{}' to '{}'", labelId, label.getName(), trimmedName);
                label.setName(trimmedName);
                updated = true;
            } else {
                logger.debug("Label {} name unchanged (after trimming), skipping update", labelId);
            }
        }

        if (updated) {
            label = labelRepository.save(label);
        }

        return labelMapper.toResponseDTO(label);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation performs secure label deletion with the following validations:
     * </p>
     * <ol>
     *   <li>Retrieves the existing label and validates it exists</li>
     *   <li>Validates that the current user owns the label</li>
     *   <li>Ensures the label is not a system-managed label (e.g., "Unlabeled")</li>
     *   <li>Permanently removes the label from the database</li>
     * </ol>
     * 
     * <p>
     * <strong>Important:</strong> Label deletion has cascading effects on associated
     * entities such as events, badges, and time buckets. These relationships are
     * handled by the database foreign key constraints and JPA cascade settings.
     * </p>
     * 
     * @throws LabelNotFoundException if no label exists with the given ID
     * @throws LabelOwnershipException if the label is not owned by the current user
     * @throws SystemManagedEntityException if attempting to delete a system-managed label
     */
    @Override
    public void deleteLabel(Long labelId) {
        logger.info("Deleting label ID {}", labelId);
        User currentUser = authenticatedUserProvider.getCurrentUser();
        Long currentUserId = currentUser.getId();

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new LabelNotFoundException(labelId));
        ownershipValidator.validateLabelOwnership(currentUserId, label);
        requireNotUserDefaultLabel(label.getCreator(), label);
        labelRepository.delete(label);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation performs efficient batch validation using the following approach:
     * </p>
     * <ol>
     *   <li>Returns immediately for null or empty input collections</li>
     *   <li>Retrieves all requested labels in a single database query</li>
     *   <li>Creates a map for efficient lookup of retrieved labels</li>
     *   <li>Validates existence and ownership for each requested label ID</li>
     *   <li>Throws specific exceptions for the first validation failure encountered</li>
     * </ol>
     * 
     * <p>
     * This method is designed for bulk operations where multiple labels need
     * validation before proceeding with business logic. It provides fail-fast
     * behavior, stopping at the first validation error.
     * </p>
     * 
     * <p>
     * <strong>Performance Note:</strong> Uses a single database query regardless
     * of the number of label IDs, making it efficient for bulk operations.
     * </p>
     * 
     * @throws LabelOwnershipException if any label exists but isn't owned by the user
     * @throws InvalidLabelAssociationException if any label ID doesn't exist
     */
    @Override
    public void validateExistenceAndOwnership(Set<Long> labelIds, Long userId) {
        if (labelIds == null || labelIds.isEmpty()) return;
        
        logger.debug("Validating existence and ownership for {} labels for user {}", labelIds.size(), userId);

        List<Label> labels = labelRepository.findAllById(labelIds);
        Map<Long, Label> labelMap = labels.stream()
                .collect(Collectors.toMap(Label::getId, Function.identity()));

        Set<Long> nonExistentIds = new HashSet<>();
        Set<Long> notOwnedIds = new HashSet<>();
        
        for (Long id : labelIds) {
            Label label = labelMap.get(id);
            if (label == null) {
                nonExistentIds.add(id);
            } else if (!label.getCreator().getId().equals(userId)) {
                notOwnedIds.add(id);
            }
        }
        
        // Check for ownership violations first (403 Forbidden)
        if (!notOwnedIds.isEmpty()) {
            throw new LabelOwnershipException(notOwnedIds.iterator().next(), userId);
        }
        
        // Then check for non-existent labels (400 Bad Request)
        if (!nonExistentIds.isEmpty()) {
            throw new InvalidLabelAssociationException(nonExistentIds);
        }
    }

    /**
     * Validates that the given label is not the user's system-managed "Unlabeled" label.
     * 
     * <p>
     * System-managed labels like "Unlabeled" cannot be modified or deleted by users.
     * This method ensures that operations attempting to modify such labels are rejected.
     * </p>
     *
     * @param user the user who owns the label
     * @param label the label to validate
     * @throws SystemManagedEntityException if the label is the user's "Unlabeled" label
     */
    private void requireNotUserDefaultLabel(User user, Label label) {
        if (label.equals(user.getUnlabeled())) {
            throw new SystemManagedEntityException(SYSTEM_MANAGED_LABEL);
        }
    }
}
