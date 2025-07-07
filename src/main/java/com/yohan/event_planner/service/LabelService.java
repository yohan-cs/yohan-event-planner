package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.LabelCreateDTO;
import com.yohan.event_planner.dto.LabelResponseDTO;
import com.yohan.event_planner.dto.LabelUpdateDTO;

import java.util.List;
import java.util.Set;

/**
 * Service interface for managing user-defined event labels.
 *
 * <p>
 * Supports full CRUD operations for labels, including ownership validation,
 * uniqueness constraints, and structured mappings to DTOs.
 * </p>
 */
public interface LabelService {

    /**
     * Retrieves a label by its unique identifier as a response DTO.
     *
     * @param labelId the ID of the label
     * @return the matching label in response format
     */
    LabelResponseDTO getLabelById(Long labelId);

    List<LabelResponseDTO> getLabelsByUser(Long userId);

    /**
     * Retrieves a label by its ID as a domain entity.
     *
     * @param labelId the ID of the label
     * @return the corresponding {@link Label} entity
     */
    Label getLabelEntityById(Long labelId);

    Set<Label> getLabelsByIds(Set<Long> labelIds);

    /**
     * Creates a new label for the authenticated user.
     *
     * @param dto the label creation payload
     * @return the newly created label in response format
     */
    LabelResponseDTO createLabel(LabelCreateDTO dto);

    /**
     * Updates the name of a label owned by the authenticated user.
     *
     * @param labelId the ID of the label to update
     * @param dto the update payload
     * @return the updated label in response format
     */
    LabelResponseDTO updateLabel(Long labelId, LabelUpdateDTO dto);

    /**
     * Deletes a label owned by the authenticated user.
     *
     * @param labelId the ID of the label to delete
     */
    void deleteLabel(Long labelId);

    /**
     * Validates that all given label IDs exist and are owned by the given user.
     *
     * <p>
     * Throws {@code LabelNotFoundException} or {@code LabelOwnershipException} as needed.
     * </p>
     *
     * @param labelIds the label IDs to validate
     * @param userId   the ID of the user who must own all the labels
     */
    void validateExistenceAndOwnership(Set<Long> labelIds, Long userId);
}
