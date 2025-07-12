package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.EventRecap;
import com.yohan.event_planner.domain.RecapMedia;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.RecapMediaCreateDTO;
import com.yohan.event_planner.dto.RecapMediaResponseDTO;
import com.yohan.event_planner.dto.RecapMediaUpdateDTO;
import com.yohan.event_planner.exception.EventRecapNotFoundException;
import com.yohan.event_planner.exception.IncompleteRecapMediaReorderListException;
import com.yohan.event_planner.exception.RecapMediaNotFoundException;
import com.yohan.event_planner.mapper.RecapMediaMapper;
import com.yohan.event_planner.repository.EventRecapRepository;
import com.yohan.event_planner.repository.RecapMediaRepository;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * Implementation of {@link RecapMediaService} providing comprehensive media management for event recaps.
 * 
 * <p>This service manages multimedia attachments (images, videos, audio) associated with event recaps,
 * following an Instagram-style approach where media items are tightly coupled to their parent recap.
 * It provides full CRUD operations with emphasis on ordering, bulk operations, and ownership validation.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>Media Lifecycle</strong>: Create, read, update, delete individual media items</li>
 *   <li><strong>Bulk Operations</strong>: Add multiple items, replace all recap media</li>
 *   <li><strong>Ordering Management</strong>: Explicit ordering with reordering capabilities</li>
 *   <li><strong>Ownership Validation</strong>: Ensure users can only access their own media</li>
 * </ul>
 * 
 * <h2>Media Types and Storage</h2>
 * <p>Supports three primary media types with external storage strategy:</p>
 * <ul>
 *   <li><strong>IMAGE</strong>: Static images (photos, screenshots, graphics)</li>
 *   <li><strong>VIDEO</strong>: Video content with optional duration tracking</li>
 *   <li><strong>AUDIO</strong>: Audio recordings with optional duration tracking</li>
 * </ul>
 * 
 * <h2>Ordering Strategy</h2>
 * <p>Media items maintain explicit ordering within recaps:</p>
 * <ul>
 *   <li><strong>Sequential Ordering</strong>: Items ordered by mediaOrder field (0, 1, 2, ...)</li>
 *   <li><strong>Flexible Assignment</strong>: Manual order specification or automatic assignment</li>
 *   <li><strong>Reordering Support</strong>: Complete reordering through ordered ID lists</li>
 *   <li><strong>Consistency Validation</strong>: Ensures all media items are included in reorder operations</li>
 * </ul>
 * 
 * <h2>Bulk Operations</h2>
 * <p>Efficient operations for managing multiple media items:</p>
 * <ul>
 *   <li><strong>Batch Addition</strong>: Add multiple media items in single transaction</li>
 *   <li><strong>Complete Replacement</strong>: Replace all recap media atomically</li>
 *   <li><strong>Automatic Ordering</strong>: Assign sequential orders when not specified</li>
 *   <li><strong>Mixed Order Handling</strong>: Support both explicit and automatic ordering</li>
 * </ul>
 * 
 * <h2>Security and Ownership</h2>
 * <p>Comprehensive ownership validation ensures data security:</p>
 * <ul>
 *   <li><strong>Recap-Level Validation</strong>: Verify ownership through parent recap's event</li>
 *   <li><strong>Media-Level Validation</strong>: Direct media access validated through recap chain</li>
 *   <li><strong>Transitive Ownership</strong>: Media ownership derived from event ownership</li>
 *   <li><strong>Consistent Authorization</strong>: All operations validate current user permissions</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Batch Operations</strong>: Minimize database round trips for bulk operations</li>
 *   <li><strong>Efficient Ordering</strong>: Optimized queries for ordered media retrieval</li>
 *   <li><strong>Transactional Boundaries</strong>: Ensure consistency across multi-step operations</li>
 *   <li><strong>Lazy Loading</strong>: Minimize unnecessary entity loading</li>
 * </ul>
 * 
 * <h2>Error Handling</h2>
 * <p>Comprehensive error handling for various failure scenarios:</p>
 * <ul>
 *   <li><strong>EventRecapNotFoundException</strong>: When parent recap doesn't exist</li>
 *   <li><strong>RecapMediaNotFoundException</strong>: When requested media doesn't exist</li>
 *   <li><strong>IncompleteRecapMediaReorderListException</strong>: When reorder lists are incomplete</li>
 *   <li><strong>Ownership Violations</strong>: When users access others' media</li>
 * </ul>
 * 
 * <h2>Integration Points</h2>
 * <p>This service integrates with:</p>
 * <ul>
 *   <li><strong>Event Recap System</strong>: Manages media attachments for recaps</li>
 *   <li><strong>External Storage</strong>: Handles URL-based media references</li>
 *   <li><strong>Security Framework</strong>: Validates user permissions for all operations</li>
 *   <li><strong>Mapping Layer</strong>: Converts between entities and DTOs</li>
 * </ul>
 * 
 * @see RecapMediaService
 * @see RecapMedia
 * @see EventRecap
 * @see RecapMediaRepository
 * @see RecapMediaMapper
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
@Service
@Transactional
public class RecapMediaServiceImpl implements RecapMediaService {

    private static final Logger logger = LoggerFactory.getLogger(RecapMediaServiceImpl.class);

    private final EventRecapRepository recapRepository;
    private final RecapMediaRepository recapMediaRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final OwnershipValidator ownershipValidator;
    private final RecapMediaMapper recapMediaMapper;

    public RecapMediaServiceImpl(
            EventRecapRepository recapRepository,
            RecapMediaRepository recapMediaRepository,
            AuthenticatedUserProvider authenticatedUserProvider,
            OwnershipValidator ownershipValidator,
            RecapMediaMapper recapMediaMapper
    ) {
        this.recapRepository = recapRepository;
        this.recapMediaRepository = recapMediaRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.ownershipValidator = ownershipValidator;
        this.recapMediaMapper = recapMediaMapper;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation performs ownership validation before retrieving media items.
     * The returned list is ordered by the mediaOrder field in ascending order (0, 1, 2, ...).</p>
     *
     * @param recapId the ID of the recap whose media to retrieve
     * @return ordered list of media response DTOs
     * @throws EventRecapNotFoundException if the recap doesn't exist
     * @throws UserOwnershipException if the current user doesn't own the recap's event
     */
    @Override
    public List<RecapMediaResponseDTO> getOrderedMediaForRecap(Long recapId) {
        logger.debug("Retrieving ordered media for recap: {}", recapId);
        
        // Validate ownership first
        EventRecap recap = getOwnedRecap(recapId);

        // Retrieve ordered media from repository
        List<RecapMedia> mediaItems = recapMediaRepository.findByRecapIdOrderByMediaOrder(recapId);
        logger.debug("Found {} media items for recap: {}", mediaItems.size(), recapId);

        // Map each entity to its response DTO
        return mediaItems.stream()
                .map(recapMediaMapper::toResponseDTO)
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>If no explicit order is provided in the DTO, the media item will be assigned
     * the next available order (current count of media items for the recap).</p>
     *
     * @param recapId the ID of the recap to add media to
     * @param dto the media creation DTO containing URL, type, and optional order
     * @return response DTO of the created media item
     * @throws EventRecapNotFoundException if the recap doesn't exist
     * @throws UserOwnershipException if the current user doesn't own the recap's event
     */
    @Override
    public RecapMediaResponseDTO addRecapMedia(Long recapId, RecapMediaCreateDTO dto) {
        logger.debug("Adding media to recap: {} with type: {}", recapId, dto.mediaType());
        
        EventRecap recap = getOwnedRecap(recapId);

        int order = (dto.mediaOrder() != null)
                ? dto.mediaOrder()
                : recapMediaRepository.countByRecapId(recapId);
        
        logger.debug("Assigning media order: {} for recap: {}", order, recapId);

        RecapMedia media = new RecapMedia(
                recap,
                dto.mediaUrl(),
                dto.mediaType(),
                dto.durationSeconds(),
                order
        );

        recapMediaRepository.save(media);
        logger.info("Successfully added media to recap: {} with order: {}", recapId, order);
        return recapMediaMapper.toResponseDTO(media);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation supports mixed ordering scenarios:
     * <ul>
     *   <li>If all items have explicit orders, those orders are used</li>
     *   <li>If some items lack orders, they are assigned sequential orders starting from the current count</li>
     *   <li>If no items have orders, all are assigned sequential orders starting from current count</li>
     * </ul>
     * All entities are persisted in a single batch operation for efficiency.</p>
     *
     * @param recap the recap entity to add media to
     * @param mediaList list of media creation DTOs to add (null or empty list is safely ignored)
     */
    @Override
    public void addMediaItemsToRecap(EventRecap recap, List<RecapMediaCreateDTO> mediaList) {
        if (mediaList == null || mediaList.isEmpty()) {
            logger.debug("No media items to add to recap: {}", recap.getId());
            return;
        }
        
        logger.debug("Adding {} media items to recap: {}", mediaList.size(), recap.getId());

        List<RecapMedia> entities = new ArrayList<>();

        boolean hasImplicitOrders = mediaList.stream().anyMatch(dto -> dto.mediaOrder() == null);
        int currentOrder = hasImplicitOrders ? recapMediaRepository.countByRecapId(recap.getId()) : 0;
        
        logger.debug("Starting order assignment from: {} for recap: {}", currentOrder, recap.getId());

        for (RecapMediaCreateDTO dto : mediaList) {
            int order = (dto.mediaOrder() != null) ? dto.mediaOrder() : currentOrder++;
            entities.add(new RecapMedia(recap, dto.mediaUrl(), dto.mediaType(), dto.durationSeconds(), order));
        }

        recapMediaRepository.saveAll(entities);
        logger.info("Successfully added {} media items to recap: {}", entities.size(), recap.getId());
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is an atomic operation - all existing media is deleted first,
     * then new media items are added. Uses {@link #addMediaItemsToRecap(EventRecap, List)}
     * for the addition logic.</p>
     *
     * @param recap the recap entity whose media to replace
     * @param mediaList list of new media items to set (null or empty results in no media)
     */
    @Override
    public void replaceRecapMedia(EventRecap recap, List<RecapMediaCreateDTO> mediaList) {
        logger.debug("Replacing all media for recap: {} with {} new items", recap.getId(), 
                    mediaList != null ? mediaList.size() : 0);
        
        // Delete existing media
        recapMediaRepository.deleteByRecapId(recap.getId());
        logger.debug("Deleted existing media for recap: {}", recap.getId());

        // Add new media items
        addMediaItemsToRecap(recap, mediaList);
        logger.info("Successfully replaced media for recap: {}", recap.getId());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Only non-null fields in the DTO will be updated. This allows for partial updates
     * where clients can update only specific fields without affecting others.
     * The implementation optimizes by only saving when actual changes are detected.</p>
     *
     * @param mediaId the ID of the media item to update
     * @param dto the update DTO containing fields to modify
     * @return response DTO of the updated media item
     * @throws RecapMediaNotFoundException if the media item doesn't exist
     * @throws UserOwnershipException if the current user doesn't own the media's recap
     */
    @Override
    public RecapMediaResponseDTO updateRecapMedia(Long mediaId, RecapMediaUpdateDTO dto) {
        logger.debug("Updating media: {}", mediaId);
        
        RecapMedia media = getOwnedMedia(mediaId);
        boolean hasChanges = false;

        if (dto.mediaUrl() != null) {
            media.setMediaUrl(dto.mediaUrl());
            hasChanges = true;
        }
        if (dto.mediaType() != null) {
            media.setMediaType(dto.mediaType());
            hasChanges = true;
        }
        if (dto.durationSeconds() != null) {
            media.setDurationSeconds(dto.durationSeconds());
            hasChanges = true;
        }
        
        if (hasChanges) {
            recapMediaRepository.save(media);
            logger.info("Successfully updated media: {}", mediaId);
        } else {
            logger.debug("No changes detected for media: {}", mediaId);
        }
        
        return recapMediaMapper.toResponseDTO(media);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Validates ownership before deletion. The media item is permanently removed
     * from the database.</p>
     *
     * @param mediaId the ID of the media item to delete
     * @throws RecapMediaNotFoundException if the media item doesn't exist
     * @throws UserOwnershipException if the current user doesn't own the media's recap
     */
    @Override
    public void deleteRecapMedia(Long mediaId) {
        logger.debug("Deleting media: {}", mediaId);
        
        RecapMedia media = getOwnedMedia(mediaId);
        recapMediaRepository.delete(media);
        logger.info("Successfully deleted media: {}", mediaId);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Validates recap ownership before performing bulk deletion. All media items
     * associated with the recap are permanently removed from the database.</p>
     *
     * @param recapId the ID of the recap whose media items should be deleted
     * @throws EventRecapNotFoundException if the recap doesn't exist
     * @throws UserOwnershipException if the current user doesn't own the recap's event
     */
    @Override
    public void deleteAllMediaForRecap(Long recapId) {
        logger.debug("Deleting all media for recap: {}", recapId);
        
        getOwnedRecap(recapId); // validates ownership
        recapMediaRepository.deleteByRecapId(recapId);
        logger.info("Successfully deleted all media for recap: {}", recapId);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation enforces strict validation:
     * <ul>
     *   <li>The ordered list must contain exactly the same IDs as existing media items</li>
     *   <li>No IDs can be missing or duplicated</li>
     *   <li>All media items will be assigned new sequential orders (0, 1, 2, ...)</li>
     * </ul>
     * The operation is atomic - either all items are reordered or none are.</p>
     *
     * @param recapId the ID of the recap whose media should be reordered
     * @param orderedMediaIds list of media IDs in their desired order
     * @throws EventRecapNotFoundException if the recap doesn't exist
     * @throws UserOwnershipException if the current user doesn't own the recap's event
     * @throws IncompleteRecapMediaReorderListException if the ID list doesn't match existing media
     * @throws RecapMediaNotFoundException if any ID in the list doesn't exist (shouldn't occur with proper validation)
     */
    @Override
    public void reorderRecapMedia(Long recapId, List<Long> orderedMediaIds) {
        logger.debug("Reordering {} media items for recap: {}", orderedMediaIds.size(), recapId);
        
        EventRecap recap = getOwnedRecap(recapId);

        List<RecapMedia> mediaItems = recapMediaRepository.findByRecapId(recapId);
        logger.debug("Found {} existing media items for reordering", mediaItems.size());

        // Check for size mismatch first
        if (mediaItems.size() != orderedMediaIds.size()) {
            logger.warn("Incomplete reorder list for recap: {} - expected {} items, got {}", 
                       recapId, mediaItems.size(), orderedMediaIds.size());
            throw new IncompleteRecapMediaReorderListException(recap.getId());
        }

        // Check for duplicates in the ordered list
        if (orderedMediaIds.size() != orderedMediaIds.stream().distinct().count()) {
            logger.warn("Duplicate IDs found in reorder list for recap: {}", recapId);
            throw new IncompleteRecapMediaReorderListException(recap.getId());
        }

        // Check if all ordered IDs exist in the media items
        Set<Long> existingMediaIds = mediaItems.stream().map(RecapMedia::getId).collect(toSet());
        if (!existingMediaIds.containsAll(orderedMediaIds)) {
            logger.warn("Unknown media IDs found in reorder list for recap: {}", recapId);
            throw new IncompleteRecapMediaReorderListException(recap.getId());
        }

        for (int i = 0; i < orderedMediaIds.size(); i++) {
            Long id = orderedMediaIds.get(i);
            RecapMedia media = mediaItems.stream()
                    .filter(m -> m.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new RecapMediaNotFoundException(id));
            media.setMediaOrder(i);
        }

        recapMediaRepository.saveAll(mediaItems);
        logger.info("Successfully reordered {} media items for recap: {}", mediaItems.size(), recapId);
    }

    // === Private helper methods ===

    /**
     * Retrieves a recap by ID and validates that the current user owns its parent event.
     *
     * @param recapId the ID of the recap to retrieve
     * @return the recap entity if it exists and the user owns it
     * @throws EventRecapNotFoundException if the recap doesn't exist
     * @throws UserOwnershipException if the current user doesn't own the recap's event
     */
    private EventRecap getOwnedRecap(Long recapId) {
        EventRecap recap = recapRepository.findById(recapId)
                .orElseThrow(() -> new EventRecapNotFoundException(recapId));

        User currentUser = authenticatedUserProvider.getCurrentUser();
        ownershipValidator.validateEventOwnership(currentUser.getId(), recap.getEvent());
        return recap;
    }

    /**
     * Retrieves a media item by ID and validates that the current user owns its parent recap's event.
     *
     * @param mediaId the ID of the media item to retrieve
     * @return the media entity if it exists and the user owns it
     * @throws RecapMediaNotFoundException if the media item doesn't exist
     * @throws UserOwnershipException if the current user doesn't own the media's recap
     */
    private RecapMedia getOwnedMedia(Long mediaId) {
        RecapMedia media = recapMediaRepository.findById(mediaId)
                .orElseThrow(() -> new RecapMediaNotFoundException(mediaId));

        User currentUser = authenticatedUserProvider.getCurrentUser();
        ownershipValidator.validateEventOwnership(currentUser.getId(), media.getRecap().getEvent());
        return media;
    }
}
