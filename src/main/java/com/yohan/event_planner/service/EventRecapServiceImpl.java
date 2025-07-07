package com.yohan.event_planner.service;

import com.yohan.event_planner.business.EventBO;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.EventRecap;
import com.yohan.event_planner.domain.RecapMedia;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.EventRecapCreateDTO;
import com.yohan.event_planner.dto.EventRecapResponseDTO;
import com.yohan.event_planner.dto.EventRecapUpdateDTO;
import com.yohan.event_planner.dto.RecapMediaCreateDTO;
import com.yohan.event_planner.dto.RecapMediaResponseDTO;
import com.yohan.event_planner.exception.ErrorCode;
import com.yohan.event_planner.exception.EventNotFoundException;
import com.yohan.event_planner.exception.EventRecapAlreadyConfirmedException;
import com.yohan.event_planner.exception.EventRecapException;
import com.yohan.event_planner.exception.EventRecapNotFoundException;
import com.yohan.event_planner.exception.InvalidEventStateException;
import com.yohan.event_planner.mapper.EventRecapMapper;
import com.yohan.event_planner.repository.EventRecapRepository;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

import static com.yohan.event_planner.exception.ErrorCode.DUPLICATE_EVENT_RECAP;
import static com.yohan.event_planner.exception.ErrorCode.RECAP_ON_INCOMPLETE_EVENT;

@Service
@Transactional
public class EventRecapServiceImpl implements EventRecapService {

    private final EventBO eventBO;
    private final EventRecapRepository recapRepository;
    private final OwnershipValidator ownershipValidator;
    private final EventRecapMapper eventRecapMapper;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final RecapMediaService recapMediaService;

    public EventRecapServiceImpl(
            EventBO eventBO,
            EventRecapRepository recapRepository,
            OwnershipValidator ownershipValidator,
            EventRecapMapper eventRecapMapper,
            AuthenticatedUserProvider authenticatedUserProvider,
            RecapMediaService recapMediaService
    ) {
        this.eventBO = eventBO;
        this.recapRepository = recapRepository;
        this.ownershipValidator = ownershipValidator;
        this.eventRecapMapper = eventRecapMapper;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.recapMediaService = recapMediaService;
    }

    @Override
    public EventRecapResponseDTO getEventRecap(Long eventId) {
        Event event = getOwnedEvent(eventId);

        EventRecap recap = event.getRecap();
        if (recap == null) {
            throw new EventRecapNotFoundException(eventId);
        }

        List<RecapMediaResponseDTO> mediaDTOs = recapMediaService.getOrderedMediaForRecap(recap.getId());

        return eventRecapMapper.toResponseDTO(recap, event, mediaDTOs);
    }

    @Override
    public EventRecapResponseDTO addEventRecap(EventRecapCreateDTO dto) {
        Event event = getOwnedEvent(dto.eventId());

        if (event.getRecap() != null) {
            throw new EventRecapException(ErrorCode.DUPLICATE_EVENT_RECAP, event.getId());
        }

        boolean isDraft = !event.isCompleted() || dto.isUnconfirmed();

        EventRecap recap = isDraft
                ? EventRecap.createUnconfirmedRecap(event, event.getCreator(), dto.notes(), dto.recapName())
                : EventRecap.createConfirmedRecap(event, event.getCreator(), dto.notes(), dto.recapName());

        event.setRecap(recap);

        // Persist event + recap
        Event saved = eventBO.updateEvent(null, event);

        // Persist media if provided
        if (dto.media() != null && !dto.media().isEmpty()) {
            recapMediaService.addMediaItemsToRecap(saved.getRecap(), dto.media());
        }

        // Retrieve ordered media for response
        List<RecapMediaResponseDTO> orderedMedia = recapMediaService.getOrderedMediaForRecap(saved.getRecap().getId());

        // Return response including ordered media
        return eventRecapMapper.toResponseDTO(saved.getRecap(), saved, orderedMedia);
    }

    @Override
    public EventRecapResponseDTO confirmEventRecap(Long eventId) {
        Event event = getOwnedEvent(eventId);

        if (!event.isCompleted()) {
            throw new InvalidEventStateException(ErrorCode.RECAP_ON_INCOMPLETE_EVENT);
        }

        EventRecap recap = event.getRecap();
        if (recap == null) {
            throw new EventRecapNotFoundException(eventId);
        }

        if (!recap.isUnconfirmed()) {
            throw new EventRecapAlreadyConfirmedException(eventId);
        }

        recap.setUnconfirmed(false);
        recapRepository.save(recap);

        // Retrieve ordered media for response
        List<RecapMediaResponseDTO> orderedMedia = recapMediaService.getOrderedMediaForRecap(recap.getId());

        return eventRecapMapper.toResponseDTO(recap, event, orderedMedia);
    }

    @Override
    public EventRecapResponseDTO updateEventRecap(Long eventId, EventRecapUpdateDTO dto) {
        Event event = getOwnedEvent(eventId);

        EventRecap recap = event.getRecap();
        if (recap == null) {
            throw new EventRecapNotFoundException(eventId);
        }

        boolean updated = false;

        String newNotes = dto.notes();
        if (newNotes != null && !Objects.equals(recap.getNotes(), newNotes)) {
            recap.setNotes(newNotes);
            updated = true;
        }

        // Replace media if provided
        if (dto.media() != null) {
            recapMediaService.replaceRecapMedia(recap, dto.media());
            updated = true;
        }

        if (updated) {
            recapRepository.save(recap);
        }

        // Retrieve ordered media for response
        List<RecapMediaResponseDTO> orderedMedia = recapMediaService.getOrderedMediaForRecap(recap.getId());

        return eventRecapMapper.toResponseDTO(recap, event, orderedMedia);
    }

    @Override
    public void deleteEventRecap(Long eventId) {
        Event event = getOwnedEvent(eventId);

        EventRecap recap = event.getRecap();
        if (recap == null) {
            throw new EventRecapNotFoundException(eventId);
        }

        recapMediaService.deleteAllMediaForRecap(recap.getId());

        event.setRecap(null);
        eventBO.updateEvent(null, event);
    }

    private Event getOwnedEvent(Long eventId) {
        Event event = eventBO.getEventById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        User user = authenticatedUserProvider.getCurrentUser();
        ownershipValidator.validateEventOwnership(user.getId(), event);

        return event;
    }

}
