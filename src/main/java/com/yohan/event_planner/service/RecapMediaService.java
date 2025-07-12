package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.EventRecap;
import com.yohan.event_planner.dto.RecapMediaCreateDTO;
import com.yohan.event_planner.dto.RecapMediaResponseDTO;
import com.yohan.event_planner.dto.RecapMediaUpdateDTO;

import java.util.List;

/**
 * Service interface for managing media items attached to event recaps.
 *
 * <p>
 * Supports adding, updating, deleting, retrieving, and reordering media items within a recap.
 * Designed following Instagram-style media embedding, where media is always tied to its parent recap.
 * </p>
 */
public interface RecapMediaService {

    /**
     * Retrieves all media items for a recap, ordered by their mediaOrder field.
     *
     * @param recapId the ID of the recap
     * @return list of media response DTOs in order
     * @throws com.yohan.event_planner.exception.EventRecapNotFoundException if the recap doesn't exist
     * @throws com.yohan.event_planner.exception.UserOwnershipException if the current user doesn't own the recap's event
     */
    List<RecapMediaResponseDTO> getOrderedMediaForRecap(Long recapId);

    /**
     * Adds a new media item to a recap.
     *
     * @param recapId the ID of the recap to add media to
     * @param dto the media creation DTO
     * @return the created media response DTO
     * @throws com.yohan.event_planner.exception.EventRecapNotFoundException if the recap doesn't exist
     * @throws com.yohan.event_planner.exception.UserOwnershipException if the current user doesn't own the recap's event
     */
    RecapMediaResponseDTO addRecapMedia(Long recapId, RecapMediaCreateDTO dto);

    /**
     * Adds multiple media items to a recap.
     *
     * @param recap the recap entity
     * @param mediaList list of media creation DTOs to add
     */
    void addMediaItemsToRecap(EventRecap recap, List<RecapMediaCreateDTO> mediaList);

    /**
     * Replaces all media items of a recap with the provided new list.
     *
     * @param recap the recap entity
     * @param mediaList list of new media items to set
     */
    void replaceRecapMedia(EventRecap recap, List<RecapMediaCreateDTO> mediaList);

    /**
     * Updates an existing media item.
     *
     * @param mediaId the ID of the media item to update
     * @param dto the media update DTO
     * @return the updated media response DTO
     * @throws com.yohan.event_planner.exception.RecapMediaNotFoundException if the media item doesn't exist
     * @throws com.yohan.event_planner.exception.UserOwnershipException if the current user doesn't own the media's recap
     */
    RecapMediaResponseDTO updateRecapMedia(Long mediaId, RecapMediaUpdateDTO dto);

    /**
     * Deletes a media item.
     *
     * @param mediaId the ID of the media item to delete
     * @throws com.yohan.event_planner.exception.RecapMediaNotFoundException if the media item doesn't exist
     * @throws com.yohan.event_planner.exception.UserOwnershipException if the current user doesn't own the media's recap
     */
    void deleteRecapMedia(Long mediaId);

    /**
     * Deletes all media items associated with a recap.
     *
     * @param recapId the ID of the recap whose media items should be deleted
     * @throws com.yohan.event_planner.exception.EventRecapNotFoundException if the recap doesn't exist
     * @throws com.yohan.event_planner.exception.UserOwnershipException if the current user doesn't own the recap's event
     */
    void deleteAllMediaForRecap(Long recapId);

    /**
     * Reorders media items within a recap based on the provided list order.
     *
     * @param recapId the ID of the recap whose media should be reordered
     * @param orderedMediaIds list of media IDs in their desired order
     * @throws com.yohan.event_planner.exception.EventRecapNotFoundException if the recap doesn't exist
     * @throws com.yohan.event_planner.exception.UserOwnershipException if the current user doesn't own the recap's event
     * @throws com.yohan.event_planner.exception.IncompleteRecapMediaReorderListException if the ID list doesn't match existing media
     * @throws com.yohan.event_planner.exception.RecapMediaNotFoundException if any ID in the list doesn't exist
     */
    void reorderRecapMedia(Long recapId, List<Long> orderedMediaIds);

}
