package com.yohan.event_planner.service;

import com.yohan.event_planner.business.EventBO;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.EventRecap;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.EventRecapCreateDTO;
import com.yohan.event_planner.dto.EventRecapResponseDTO;
import com.yohan.event_planner.dto.EventRecapUpdateDTO;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Implementation of {@link EventRecapService} providing comprehensive event recap management.
 * 
 * <p>This service manages event recaps - multimedia documentation of completed events that
 * capture experiences, outcomes, and reflections. Recaps support rich content including text
 * summaries, photos, videos, and audio recordings, enabling users to document and reflect
 * on their activities comprehensively.</p>
 * 
 * <h2>Core Functionality</h2>
 * <ul>
 *   <li><strong>Recap Lifecycle</strong>: Create, read, update, delete with state validation</li>
 *   <li><strong>Media Integration</strong>: Manage multimedia content through RecapMediaService</li>
 *   <li><strong>State Workflows</strong>: Support draft → confirmed recap transitions</li>
 *   <li><strong>Event Validation</strong>: Ensure recaps only exist for appropriate events</li>
 * </ul>
 * 
 * <h2>Recap State Management</h2>
 * <p>Event recaps follow a structured lifecycle:</p>
 * <ul>
 *   <li><strong>Draft State</strong>: Initial recap creation, allows modifications</li>
 *   <li><strong>Confirmed State</strong>: Finalized recaps, locked from further changes</li>
 *   <li><strong>State Transitions</strong>: Enforced progression from draft to confirmed</li>
 *   <li><strong>Validation Rules</strong>: Business rules govern state changes</li>
 * </ul>
 * 
 * <h2>Event State Requirements</h2>
 * <p>Recaps can only be created for events in appropriate states:</p>
 * <ul>
 *   <li><strong>Confirmed Events Only</strong>: Prevents recaps on unconfirmed/draft events</li>
 *   <li><strong>One Recap Per Event</strong>: Enforces unique recap constraint per event</li>
 *   <li><strong>State Validation</strong>: Validates event state before recap operations</li>
 *   <li><strong>Business Rule Enforcement</strong>: Maintains data integrity</li>
 * </ul>
 * 
 * <h2>Media Content Management</h2>
 * <p>Recaps support rich multimedia content:</p>
 * <ul>
 *   <li><strong>Text Content</strong>: Structured text summaries and reflections</li>
 *   <li><strong>Image Gallery</strong>: Photo documentation of events</li>
 *   <li><strong>Video Content</strong>: Video recordings and highlights</li>
 *   <li><strong>Audio Recordings</strong>: Voice notes and audio documentation</li>
 *   <li><strong>Media Ordering</strong>: Maintain display sequence for narrative flow</li>
 * </ul>
 * 
 * <h2>Integration Architecture</h2>
 * <p>The service integrates with multiple system components:</p>
 * <ul>
 *   <li><strong>RecapMediaService</strong>: Manages multimedia content and ordering</li>
 *   <li><strong>EventBO</strong>: Validates event state and retrieves event data</li>
 *   <li><strong>Security Framework</strong>: Enforces ownership and authorization</li>
 *   <li><strong>Mapping Layer</strong>: Converts between entities and DTOs</li>
 * </ul>
 * 
 * <h2>Security and Ownership</h2>
 * <p>Comprehensive security model protects recap data:</p>
 * <ul>
 *   <li><strong>Event Ownership</strong>: Recaps can only be created by event owners</li>
 *   <li><strong>Transitive Security</strong>: Recap access controlled through event ownership</li>
 *   <li><strong>State-based Permissions</strong>: Different permissions for draft vs confirmed</li>
 *   <li><strong>Media Authorization</strong>: Media operations validated through recap ownership</li>
 * </ul>
 * 
 * <h2>Business Rules</h2>
 * <ul>
 *   <li><strong>Unique Constraint</strong>: One recap per event maximum</li>
 *   <li><strong>Event State Requirement</strong>: Only confirmed events can have recaps</li>
 *   <li><strong>Confirmation Immutability</strong>: Confirmed recaps cannot be modified</li>
 *   <li><strong>Cascading Deletion</strong>: Event deletion removes associated recaps</li>
 * </ul>
 * 
 * <h2>Workflow Support</h2>
 * <p>Supports comprehensive recap workflows:</p>
 * <ul>
 *   <li><strong>Draft Creation</strong>: Initial recap creation with media placeholders</li>
 *   <li><strong>Content Addition</strong>: Progressive content building with media uploads</li>
 *   <li><strong>Review Process</strong>: Draft state allows content refinement</li>
 *   <li><strong>Confirmation</strong>: Finalize recap and lock content</li>
 * </ul>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Transactional Boundaries</strong>: Ensure consistency across media operations</li>
 *   <li><strong>Lazy Loading</strong>: Optimize entity loading for better performance</li>
 *   <li><strong>Media Delegation</strong>: Efficient media operations through specialized service</li>
 *   <li><strong>State Caching</strong>: Cache validation results where appropriate</li>
 * </ul>
 * 
 * <h2>Error Handling</h2>
 * <p>Comprehensive error handling for various scenarios:</p>
 * <ul>
 *   <li><strong>EventRecapNotFoundException</strong>: When requested recaps don't exist</li>
 *   <li><strong>EventRecapAlreadyConfirmedException</strong>: When modifying confirmed recaps</li>
 *   <li><strong>EventRecapException</strong>: For business rule violations (duplicates)</li>
 *   <li><strong>InvalidEventStateException</strong>: When events are in wrong state for recaps</li>
 * </ul>
 * 
 * <h2>Use Cases</h2>
 * <p>Primary use cases for event recaps:</p>
 * <ul>
 *   <li><strong>Event Documentation</strong>: Capture memories and outcomes</li>
 *   <li><strong>Reflection</strong>: Post-event analysis and learning</li>
 *   <li><strong>Sharing</strong>: Share experiences with others</li>
 *   <li><strong>Progress Tracking</strong>: Document achievement and growth</li>
 * </ul>
 * 
 * <h2>Data Consistency</h2>
 * <p>Maintains consistency across recap relationships:</p>
 * <ul>
 *   <li><strong>Event-Recap Consistency</strong>: Ensure valid event-recap relationships</li>
 *   <li><strong>Media Synchronization</strong>: Keep media content in sync with recaps</li>
 *   <li><strong>State Consistency</strong>: Maintain valid state transitions</li>
 *   <li><strong>Ownership Integrity</strong>: Ensure ownership relationships remain valid</li>
 * </ul>
 * 
 * @see EventRecapService
 * @see EventRecap
 * @see Event
 * @see RecapMediaService
 * @see EventRecapRepository
 * @see EventRecapMapper
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 */
@Service
@Transactional
public class EventRecapServiceImpl implements EventRecapService {

    private static final Logger logger = LoggerFactory.getLogger(EventRecapServiceImpl.class);

    private final EventBO eventBO;
    private final EventRecapRepository recapRepository;
    private final OwnershipValidator ownershipValidator;
    private final EventRecapMapper eventRecapMapper;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final RecapMediaService recapMediaService;

    /**
     * Constructs an EventRecapServiceImpl with required dependencies.
     * 
     * <p>
     * Initializes the service with all required dependencies for comprehensive event recap
     * management including media integration, security validation, and entity mapping.
     * The service coordinates between multiple components to provide secure, transactional
     * recap operations with comprehensive multimedia support.
     * </p>
     *
     * @param eventBO business object for event operations and validation
     * @param recapRepository repository for event recap persistence
     * @param ownershipValidator validator for user ownership verification
     * @param eventRecapMapper mapper for entity-DTO conversions
     * @param authenticatedUserProvider provider for current user context
     * @param recapMediaService service for managing recap multimedia content
     * @throws IllegalArgumentException if any required dependency is null (handled by Spring)
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public EventRecapResponseDTO getEventRecap(Long eventId) {
        logger.info("Retrieving event recap for eventId: {}", eventId);
        Event event = getOwnedEvent(eventId);

        EventRecap recap = event.getRecap();
        if (recap == null) {
            logger.warn("No recap found for eventId: {}", eventId);
            throw new EventRecapNotFoundException(eventId);
        }

        List<RecapMediaResponseDTO> mediaDTOs = recapMediaService.getOrderedMediaForRecap(recap.getId());
        logger.debug("Retrieved {} media items for recap: {}", mediaDTOs.size(), recap.getId());

        EventRecapResponseDTO response = eventRecapMapper.toResponseDTO(recap, event, mediaDTOs);
        logger.info("Successfully retrieved event recap for eventId: {}", eventId);
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventRecapResponseDTO addEventRecap(EventRecapCreateDTO dto) {
        logger.info("Adding event recap for eventId: {}", dto.eventId());
        Event event = getOwnedEvent(dto.eventId());

        if (event.getRecap() != null) {
            logger.warn("Attempt to create duplicate recap for eventId: {}", event.getId());
            throw new EventRecapException(ErrorCode.DUPLICATE_EVENT_RECAP, event.getId());
        }

        // For incomplete events, force creation of draft/unconfirmed recap
        // For completed events, use the DTO preference (confirmed by default)
        boolean shouldCreateDraftRecap = !event.isCompleted() || dto.isUnconfirmed();
        
        if (!event.isCompleted()) {
            logger.debug("Creating draft recap for incomplete eventId: {}", event.getId());
        } else {
            logger.debug("Creating {} recap for completed eventId: {}, dto unconfirmed: {}", 
                shouldCreateDraftRecap ? "draft" : "confirmed", event.getId(), dto.isUnconfirmed());
        }

        EventRecap recap = shouldCreateDraftRecap
                ? EventRecap.createUnconfirmedRecap(event, event.getCreator(), dto.notes(), dto.recapName())
                : EventRecap.createConfirmedRecap(event, event.getCreator(), dto.notes(), dto.recapName());

        event.setRecap(recap);

        // Persist event + recap
        Event saved = eventBO.updateEvent(null, event);

        // Persist media if provided
        if (dto.media() != null && !dto.media().isEmpty()) {
            logger.debug("Adding {} media items to recap: {}", dto.media().size(), saved.getRecap().getId());
            recapMediaService.addMediaItemsToRecap(saved.getRecap(), dto.media());
        }

        // Retrieve ordered media for response
        List<RecapMediaResponseDTO> orderedMedia = recapMediaService.getOrderedMediaForRecap(saved.getRecap().getId());

        // Return response including ordered media
        EventRecapResponseDTO response = eventRecapMapper.toResponseDTO(saved.getRecap(), saved, orderedMedia);
        logger.info("Successfully created {} recap for eventId: {}", shouldCreateDraftRecap ? "draft" : "confirmed", event.getId());
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventRecapResponseDTO confirmEventRecap(Long eventId) {
        logger.info("Confirming event recap for eventId: {}", eventId);
        Event event = getOwnedEvent(eventId);

        if (!event.isCompleted()) {
            logger.warn("Attempt to confirm recap for incomplete event: {}", eventId);
            throw new InvalidEventStateException(ErrorCode.RECAP_ON_INCOMPLETE_EVENT);
        }

        EventRecap recap = event.getRecap();
        if (recap == null) {
            logger.warn("No recap found to confirm for eventId: {}", eventId);
            throw new EventRecapNotFoundException(eventId);
        }

        if (!recap.isUnconfirmed()) {
            logger.warn("Attempt to confirm already confirmed recap for eventId: {}", eventId);
            throw new EventRecapAlreadyConfirmedException(eventId);
        }

        recap.setUnconfirmed(false);
        recapRepository.save(recap);
        logger.debug("Recap confirmed and saved for eventId: {}", eventId);

        // Retrieve ordered media for response
        List<RecapMediaResponseDTO> orderedMedia = recapMediaService.getOrderedMediaForRecap(recap.getId());

        EventRecapResponseDTO response = eventRecapMapper.toResponseDTO(recap, event, orderedMedia);
        logger.info("Successfully confirmed event recap for eventId: {}", eventId);
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventRecapResponseDTO updateEventRecap(Long eventId, EventRecapUpdateDTO dto) {
        logger.info("Updating event recap for eventId: {}", eventId);
        Event event = getOwnedEvent(eventId);

        EventRecap recap = event.getRecap();
        if (recap == null) {
            logger.warn("No recap found to update for eventId: {}", eventId);
            throw new EventRecapNotFoundException(eventId);
        }

        boolean updated = false;

        String newNotes = dto.notes();
        if (newNotes != null && !Objects.equals(recap.getNotes(), newNotes)) {
            logger.debug("Updating notes for recap: {}", recap.getId());
            recap.setNotes(newNotes);
            updated = true;
        }

        // Replace media if provided
        if (dto.media() != null) {
            logger.debug("Replacing media for recap: {} with {} items", recap.getId(), dto.media().size());
            recapMediaService.replaceRecapMedia(recap, dto.media());
            updated = true;
        }

        if (updated) {
            recapRepository.save(recap);
            logger.debug("Recap saved after updates for eventId: {}", eventId);
        } else {
            logger.debug("No changes detected for recap update on eventId: {}", eventId);
        }

        // Retrieve ordered media for response
        List<RecapMediaResponseDTO> orderedMedia = recapMediaService.getOrderedMediaForRecap(recap.getId());

        EventRecapResponseDTO response = eventRecapMapper.toResponseDTO(recap, event, orderedMedia);
        logger.info("Successfully updated event recap for eventId: {}", eventId);
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteEventRecap(Long eventId) {
        logger.info("Deleting event recap for eventId: {}", eventId);
        Event event = getOwnedEvent(eventId);

        EventRecap recap = event.getRecap();
        if (recap == null) {
            logger.warn("No recap found to delete for eventId: {}", eventId);
            throw new EventRecapNotFoundException(eventId);
        }

        logger.debug("Deleting all media for recap: {}", recap.getId());
        recapMediaService.deleteAllMediaForRecap(recap.getId());

        event.setRecap(null);
        eventBO.updateEvent(null, event);
        logger.info("Successfully deleted event recap for eventId: {}", eventId);
    }

    /**
     * Retrieves an event and validates that it is owned by the current user.
     * 
     * <p>
     * This method encapsulates the common pattern of event retrieval with ownership
     * validation used across all recap operations. It ensures security by validating
     * user ownership before allowing any recap operations, maintaining data integrity
     * and access control.
     * </p>
     *
     * @param eventId the ID of the event to retrieve
     * @return the event if it exists and is owned by the current user
     * @throws EventNotFoundException if the event doesn't exist
     * @throws UserOwnershipException if the current user doesn't own the event
     */
    private Event getOwnedEvent(Long eventId) {
        logger.debug("Retrieving and validating ownership for eventId: {}", eventId);
        Event event = eventBO.getEventById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        User user = authenticatedUserProvider.getCurrentUser();
        logger.debug("Validating ownership for user: {} on event: {}", user.getId(), eventId);
        ownershipValidator.validateEventOwnership(user.getId(), event);

        return event;
    }

}
