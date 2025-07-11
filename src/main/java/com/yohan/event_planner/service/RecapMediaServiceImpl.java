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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

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

    private final EventRecapRepository recapRepository;
    private final RecapMediaRepository recapMediaRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final OwnershipValidator ownershipValidator;
    private final RecapMediaMapper recapMediaMapper;

    private RecapMediaServiceImpl recapMediaService;

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

    @Override
    public List<RecapMediaResponseDTO> getOrderedMediaForRecap(Long recapId) {
        // Validate ownership first
        EventRecap recap = getOwnedRecap(recapId);

        // Retrieve ordered media from repository
        List<RecapMedia> mediaItems = recapMediaRepository.findByRecapIdOrderByMediaOrder(recapId);

        // Map each entity to its response DTO
        return mediaItems.stream()
                .map(recapMediaMapper::toResponseDTO)
                .toList();
    }

    @Override
    public RecapMediaResponseDTO addRecapMedia(Long recapId, RecapMediaCreateDTO dto) {
        EventRecap recap = getOwnedRecap(recapId);

        int order = (dto.mediaOrder() != null)
                ? dto.mediaOrder()
                : recapMediaRepository.countByRecapId(recapId);

        RecapMedia media = new RecapMedia(
                recap,
                dto.mediaUrl(),
                dto.mediaType(),
                dto.durationSeconds(),
                order
        );

        recapMediaRepository.save(media);
        return recapMediaMapper.toResponseDTO(media);
    }

    @Override
    public void addMediaItemsToRecap(EventRecap recap, List<RecapMediaCreateDTO> mediaList) {
        if (mediaList == null || mediaList.isEmpty()) return;

        List<RecapMedia> entities = new ArrayList<>();

        boolean hasImplicitOrders = mediaList.stream().anyMatch(dto -> dto.mediaOrder() == null);
        int currentOrder = hasImplicitOrders ? recapMediaRepository.countByRecapId(recap.getId()) : 0;

        for (RecapMediaCreateDTO dto : mediaList) {
            int order = (dto.mediaOrder() != null) ? dto.mediaOrder() : currentOrder++;
            entities.add(new RecapMedia(recap, dto.mediaUrl(), dto.mediaType(), dto.durationSeconds(), order));
        }

        recapMediaRepository.saveAll(entities);
    }

    @Override
    public void replaceRecapMedia(EventRecap recap, List<RecapMediaCreateDTO> mediaList) {
        // Delete existing media
        recapMediaRepository.deleteByRecapId(recap.getId());

        // Add new media items
        addMediaItemsToRecap(recap, mediaList);
    }

    @Override
    public RecapMediaResponseDTO updateRecapMedia(Long mediaId, RecapMediaUpdateDTO dto) {
        RecapMedia media = getOwnedMedia(mediaId);

        if (dto.mediaUrl() != null) {
            media.setMediaUrl(dto.mediaUrl());
        }
        if (dto.mediaType() != null) {
            media.setMediaType(dto.mediaType());
        }
        if (dto.durationSeconds() != null) {
            media.setDurationSeconds(dto.durationSeconds());
        }

        recapMediaRepository.save(media);
        return recapMediaMapper.toResponseDTO(media);
    }

    @Override
    public void deleteRecapMedia(Long mediaId) {
        RecapMedia media = getOwnedMedia(mediaId);
        recapMediaRepository.delete(media);
    }

    @Override
    public void deleteAllMediaForRecap(Long recapId) {
        getOwnedRecap(recapId); // validates ownership
        recapMediaRepository.deleteByRecapId(recapId);
    }

    @Override
    public void reorderRecapMedia(Long recapId, List<Long> orderedMediaIds) {
        EventRecap recap = getOwnedRecap(recapId);

        List<RecapMedia> mediaItems = recapMediaRepository.findByRecapId(recapId);

        if (mediaItems.size() != orderedMediaIds.size()
                || !mediaItems.stream().map(RecapMedia::getId).collect(toSet()).containsAll(orderedMediaIds)) {
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
    }

    // === Private helper methods ===

    private EventRecap getOwnedRecap(Long recapId) {
        EventRecap recap = recapRepository.findById(recapId)
                .orElseThrow(() -> new EventRecapNotFoundException(recapId));

        User currentUser = authenticatedUserProvider.getCurrentUser();
        ownershipValidator.validateEventOwnership(currentUser.getId(), recap.getEvent());
        return recap;
    }

    private RecapMedia getOwnedMedia(Long mediaId) {
        RecapMedia media = recapMediaRepository.findById(mediaId)
                .orElseThrow(() -> new RecapMediaNotFoundException(mediaId));

        User currentUser = authenticatedUserProvider.getCurrentUser();
        ownershipValidator.validateEventOwnership(currentUser.getId(), media.getRecap().getEvent());
        return media;
    }
}
