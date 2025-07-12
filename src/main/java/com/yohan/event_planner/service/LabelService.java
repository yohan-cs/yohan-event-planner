package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.Label;
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

    /**
     * Retrieves all labels created by the specified user, excluding system-managed labels.
     * 
     * <p>
     * Returns labels sorted alphabetically by name. System-managed labels such as 
     * "Unlabeled" are automatically filtered out from the results.
     * </p>
     *
     * @param userId the ID of the user whose labels to retrieve
     * @return a list of label response DTOs sorted alphabetically by name
     * @throws UserNotFoundException if the user is not found
     */
    List<LabelResponseDTO> getLabelsByUser(Long userId);

    /**
     * Retrieves a label by its ID as a domain entity.
     *
     * @param labelId the ID of the label
     * @return the corresponding {@link Label} entity
     * @throws LabelNotFoundException if no label exists with the given ID
     */
    Label getLabelEntityById(Long labelId);

    /**
     * Retrieves multiple labels by their IDs as domain entities.
     * 
     * <p>
     * Returns only the labels that exist in the database. If some IDs don't 
     * correspond to existing labels, they are silently ignored. No ownership
     * validation is performed by this method.
     * </p>
     *
     * @param labelIds the set of label IDs to retrieve (null or empty set returns empty set)
     * @return a set of matching label entities (may be smaller than input if some IDs don't exist)
     */
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
     * Performs batch validation of label existence and ownership. If any label
     * doesn't exist or isn't owned by the specified user, throws appropriate exceptions.
     * If the input is null or empty, no validation is performed.
     * </p>
     *
     * @param labelIds the label IDs to validate (null or empty set is allowed)
     * @param userId   the ID of the user who must own all the labels
     * @throws LabelOwnershipException if any label exists but isn't owned by the user
     * @throws InvalidLabelAssociationException if any label ID doesn't exist
     */
    void validateExistenceAndOwnership(Set<Long> labelIds, Long userId);
}
