package com.yohan.event_planner.service;

import com.yohan.event_planner.business.UserBO;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.LabelCreateDTO;
import com.yohan.event_planner.dto.LabelResponseDTO;
import com.yohan.event_planner.dto.LabelUpdateDTO;
import com.yohan.event_planner.exception.LabelException;
import com.yohan.event_planner.exception.LabelNotFoundException;
import com.yohan.event_planner.exception.LabelOwnershipException;
import com.yohan.event_planner.exception.SystemManagedEntityException;
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

    @Override
    public LabelResponseDTO getLabelById(Long labelId) {
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new LabelNotFoundException(labelId));
        ownershipValidator.validateLabelOwnership(label.getCreator().getId(), label);
        return labelMapper.toResponseDTO(label);
    }


    @Override
    @Transactional(readOnly = true)
    public List<LabelResponseDTO> getLabelsByUser(Long userId) {
        logger.debug("Fetching labels for user ID {}", userId);

        // Fetch the user using UserBO and ensure it's not null by throwing an exception if not found
        User user = userBO.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Fetch all labels sorted by name
        List<Label> labels = labelRepository.findAllByCreatorIdOrderByNameAsc(userId);

        // Filter out the "Unlabeled" label by its ID (unlabeledId on User entity)
        labels = labels.stream()
                .filter(label -> !label.getId().equals(user.getUnlabeled().getId()))  // Exclude based on the unlabeledId
                .collect(Collectors.toList());

        return labels.stream()
                .map(labelMapper::toResponseDTO)
                .toList();
    }

    @Override
    public Label getLabelEntityById(Long labelId) {
        return labelRepository.findById(labelId)
                .orElseThrow(() -> new LabelNotFoundException(labelId));
    }

    @Override
    public Set<Label> getLabelsByIds(Set<Long> labelIds) {
        if (labelIds == null || labelIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(labelRepository.findAllById(labelIds));
    }

    @Override
    public LabelResponseDTO createLabel(LabelCreateDTO dto) {
        logger.info("Creating new label '{}'", dto.name());
        User creator = authenticatedUserProvider.getCurrentUser();

        if (labelRepository.existsByNameAndCreator(dto.name(), creator)) {
            throw new LabelException(DUPLICATE_LABEL, dto.name());
        }

        return labelMapper.toResponseDTO(labelRepository.save(new Label(dto.name(), creator)));
    }

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

        if (dto.name() != null && !dto.name().equals(label.getName())) {
            if (labelRepository.existsByNameAndCreator(dto.name(), currentUser)) {
                throw new LabelException(DUPLICATE_LABEL, dto.name());
            }
            label.setName(dto.name());
            updated = true;
        }

        if (updated) {
            label = labelRepository.save(label);
        }

        return labelMapper.toResponseDTO(label);
    }

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

    @Override
    public void validateExistenceAndOwnership(Set<Long> labelIds, Long userId) {
        if (labelIds == null || labelIds.isEmpty()) return;

        List<Label> labels = labelRepository.findAllById(labelIds);
        Map<Long, Label> labelMap = labels.stream()
                .collect(Collectors.toMap(Label::getId, Function.identity()));

        for (Long id : labelIds) {
            Label label = labelMap.get(id);
            if (label == null) {
                throw new LabelNotFoundException(id);
            }
            if (!label.getCreator().getId().equals(userId)) {
                throw new LabelOwnershipException(id, userId);
            }
        }
    }

    private void requireNotUserDefaultLabel(User user, Label label) {
        if (label.equals(user.getUnlabeled())) {
            throw new SystemManagedEntityException(SYSTEM_MANAGED_LABEL);
        }
    }
}
