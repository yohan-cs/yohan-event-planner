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
