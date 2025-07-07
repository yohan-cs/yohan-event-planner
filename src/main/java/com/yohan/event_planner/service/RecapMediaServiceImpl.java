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
