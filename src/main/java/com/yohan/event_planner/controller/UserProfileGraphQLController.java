package com.yohan.event_planner.controller;

import com.yohan.event_planner.constants.ApplicationConstants;
import com.yohan.event_planner.domain.enums.RecapMediaType;
import com.yohan.event_planner.dto.BadgeResponseDTO;
import com.yohan.event_planner.dto.BadgeUpdateDTO;
import com.yohan.event_planner.dto.EventRecapCreateDTO;
import com.yohan.event_planner.dto.EventRecapResponseDTO;
import com.yohan.event_planner.dto.EventRecapUpdateDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.dto.RecapMediaCreateDTO;
import com.yohan.event_planner.dto.RecapMediaResponseDTO;
import com.yohan.event_planner.dto.RecapMediaUpdateDTO;
import com.yohan.event_planner.dto.UserHeaderResponseDTO;
import com.yohan.event_planner.dto.UserHeaderUpdateDTO;
import com.yohan.event_planner.dto.UserProfileResponseDTO;
import com.yohan.event_planner.dto.WeekViewDTO;
import com.yohan.event_planner.graphql.input.AddEventRecapInput;
import com.yohan.event_planner.graphql.input.CreateRecapMediaInput;
import com.yohan.event_planner.graphql.input.UpdateEventInput;
import com.yohan.event_planner.graphql.input.UpdateEventRecapInput;
import com.yohan.event_planner.graphql.input.UpdateFieldInput;
import com.yohan.event_planner.graphql.input.UpdateRecapMediaInput;
import com.yohan.event_planner.security.AuthenticatedUserProvider;

import com.yohan.event_planner.service.BadgeService;
import com.yohan.event_planner.service.EventRecapService;
import com.yohan.event_planner.service.EventService;
import com.yohan.event_planner.service.RecapMediaService;
import com.yohan.event_planner.service.UserService;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * GraphQL controller providing user profile management operations.
 * 
 * <p>This controller handles GraphQL queries and mutations for user profiles, including:
 * <ul>
 *   <li>User profile queries and user header updates</li>
 *   <li>Badge management (create, update, delete, reorder)</li>
 *   <li>Event management (update, delete)</li>
 *   <li>Event recap operations (create, update, delete)</li>
 *   <li>Recap media management</li>
 * </ul>
 * 
 * <p>All operations require authentication via JWT tokens, enforced through
 * {@link AuthenticatedUserProvider}. The controller delegates business logic
 * to appropriate service classes and focuses solely on GraphQL input/output
 * transformation.
 * 
 * <p>Supports partial updates using {@link UpdateFieldInput} pattern, allowing
 * clients to specify only fields they want to modify while preserving others.
 * 
 * <p>Architecture Role: This controller serves as a thin GraphQL adapter layer
 * that translates GraphQL operations into service calls, maintaining clear
 * separation between the GraphQL schema and business logic.
 */
@Controller
public class UserProfileGraphQLController {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileGraphQLController.class);

    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final UserService userService;
    private final BadgeService badgeService;
    private final EventService eventService;
    private final EventRecapService eventRecapService;
    private final RecapMediaService recapMediaService;

    public UserProfileGraphQLController(
            AuthenticatedUserProvider authenticatedUserProvider,
            UserService userService,
            BadgeService badgeService,
            EventService eventService,
            EventRecapService eventRecapService,
            RecapMediaService recapMediaService

    ) {
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.userService = userService;
        this.badgeService = badgeService;
        this.eventService = eventService;
        this.eventRecapService = eventRecapService;
        this.recapMediaService = recapMediaService;
    }

    // ==============================
    // region Queries
    // ==============================

    /**
     * Retrieves a user profile by username with viewer context.
     * 
     * @param username the username of the profile to retrieve
     * @return user profile data visible to the authenticated viewer
     * @throws UnauthorizedException if no valid JWT token is provided
     * @throws UserNotFoundException if the requested username does not exist
     */
    @QueryMapping
    public UserProfileResponseDTO userProfile(@Argument String username) {
        Long viewerId = authenticatedUserProvider.getCurrentUser().getId();
        logger.debug("Processing userProfile query for username: {} by viewer: {}", username, viewerId);
        
        UserProfileResponseDTO profile = userService.getUserProfile(username, viewerId);
        logger.debug("Successfully retrieved profile for username: {} by viewer: {}", username, viewerId);
        return profile;
    }

    /**
     * Retrieves an event recap by event ID.
     * 
     * @param eventId the ID of the event whose recap to retrieve
     * @return event recap data
     * @throws UnauthorizedException if no valid JWT token is provided
     * @throws EventRecapNotFoundException if no recap exists for the event
     */
    @QueryMapping
    public EventRecapResponseDTO eventRecap(@Argument Long eventId) {
        logger.debug("Processing eventRecap query for eventId: {}", eventId);
        
        EventRecapResponseDTO recap = eventRecapService.getEventRecap(eventId);
        logger.debug("Successfully retrieved recap for eventId: {}", eventId);
        return recap;
    }

    // ==============================
    // region SchemaMappings
    // ==============================

    /**
     * Schema mapping to provide week view data for a user profile.
     * 
     * @param profile the user profile context (automatically provided by GraphQL)
     * @param anchorDate the date to center the week view around
     * @return week view data containing 7 days of events
     * @throws UnauthorizedException if no valid JWT token is provided
     */
    @SchemaMapping(typeName = "UserProfile", field = "weekView")
    public WeekViewDTO weekView(UserProfileResponseDTO profile,
                                @Argument("anchorDate") LocalDate anchorDate) {
        logger.debug("Generating weekView for anchorDate: {}", anchorDate);
        return eventService.generateWeekView(anchorDate);
    }

    // ==============================
    // region Mutations - UserHeader
    // ==============================

    /**
     * Updates the authenticated user's header information (bio, profile picture).
     * 
     * @param input update data containing optional bio and profile picture URL
     * @return updated user header information
     * @throws UnauthorizedException if no valid JWT token is provided
     */
    @MutationMapping
    public UserHeaderResponseDTO updateUserHeader(@Argument("input") UserHeaderUpdateDTO input) {
        Long userId = authenticatedUserProvider.getCurrentUser().getId();
        logger.info("User {} updating header: bio={}, profilePicture={}", 
            userId, input.bio() != null, input.profilePictureUrl() != null);
        
        UserHeaderResponseDTO result = userService.updateUserHeader(userId, input);
        logger.info("Successfully updated header for user {}", userId);
        return result;
    }

    // ==============================
    // region Mutations - Badges
    // ==============================

    /**
     * Updates a badge belonging to the authenticated user.
     * 
     * @param badgeId the ID of the badge to update
     * @param input update data containing optional badge modifications
     * @return updated badge information
     * @throws UnauthorizedException if no valid JWT token is provided
     * @throws BadgeNotFoundException if the badge does not exist
     * @throws UnauthorizedBadgeAccessException if user doesn't own the badge
     */
    @MutationMapping
    public BadgeResponseDTO updateBadge(@Argument("id") Long badgeId, @Argument("input") BadgeUpdateDTO input) {
        logger.info("User updating badge {}: name={}", 
            badgeId, input.name() != null ? "provided" : "unchanged");
        
        BadgeResponseDTO result = badgeService.updateBadge(badgeId, input);
        logger.info("Successfully updated badge {}", badgeId);
        return result;
    }

    /**
     * Deletes a badge belonging to the authenticated user.
     * 
     * @param badgeId the ID of the badge to delete
     * @return true if deletion succeeded
     * @throws UnauthorizedException if no valid JWT token is provided
     * @throws BadgeNotFoundException if the badge does not exist
     * @throws UnauthorizedBadgeAccessException if user doesn't own the badge
     */
    @MutationMapping
    public Boolean deleteBadge(@Argument("id") Long badgeId) {
        Long userId = authenticatedUserProvider.getCurrentUser().getId();
        logger.info("User {} deleting badge {}", userId, badgeId);
        
        badgeService.deleteBadge(badgeId);
        logger.info("Successfully deleted badge {} for user {}", badgeId, userId);
        return ApplicationConstants.GRAPHQL_OPERATION_SUCCESS;
    }

    /**
     * Reorders badges for the authenticated user.
     * 
     * @param ids ordered list of badge IDs representing the new sequence
     * @return true if reordering succeeded
     * @throws UnauthorizedException if no valid JWT token is provided
     * @throws BadgeNotFoundException if any badge ID doesn't exist
     * @throws UnauthorizedBadgeAccessException if user doesn't own all badges
     */
    @MutationMapping
    public Boolean reorderBadges(@Argument("ids") List<Long> ids) {
        Long userId = authenticatedUserProvider.getCurrentUser().getId();
        logger.info("User {} reordering {} badges", userId, ids.size());
        
        badgeService.reorderBadges(userId, ids);
        logger.info("Successfully reordered badges for user {}", userId);
        return ApplicationConstants.GRAPHQL_OPERATION_SUCCESS;
    }

    /**
     * Reorders labels within a badge for the authenticated user.
     * 
     * @param badgeId the ID of the badge whose labels to reorder
     * @param labelOrder ordered list of label IDs representing the new sequence
     * @return true if reordering succeeded
     * @throws UnauthorizedException if no valid JWT token is provided
     * @throws BadgeNotFoundException if the badge doesn't exist
     * @throws UnauthorizedBadgeAccessException if user doesn't own the badge
     * @throws IncompleteBadgeLabelReorderListException if label order is incomplete
     */
    @MutationMapping
    public Boolean reorderBadgeLabels(@Argument("badgeId") Long badgeId,
                                      @Argument("labelOrder") List<Long> labelOrder) {
        logger.info("Reordering {} labels for badge {}", labelOrder.size(), badgeId);
        
        badgeService.reorderBadgeLabels(badgeId, labelOrder);
        logger.info("Successfully reordered labels for badge {}", badgeId);
        return ApplicationConstants.GRAPHQL_OPERATION_SUCCESS;
    }

    // ==============================
    // region Mutations - Events
    // ==============================

    /**
     * Updates an event belonging to the authenticated user.
     * 
     * @param id the event ID as a string
     * @param input update data containing optional event modifications
     * @return updated event information
     * @throws UnauthorizedException if no valid JWT token is provided
     * @throws EventNotFoundException if the event does not exist
     * @throws UserOwnershipViolationException if user doesn't own the event
     * @throws InvalidEventTimeException if start time is after end time
     */
    @MutationMapping
    public EventResponseDTO updateEvent(@Argument String id, @Argument UpdateEventInput input) {
        Long eventId = Long.valueOf(id);
        Long userId = authenticatedUserProvider.getCurrentUser().getId();
        logger.info("User {} updating event {}: name={}, description={}, completed={}", 
            userId, eventId, 
            input.name() != null && input.name().isPresent() ? "provided" : "unchanged",
            input.description() != null && input.description().isPresent() ? "provided" : "unchanged",
            input.isCompleted() != null && input.isCompleted().isPresent() ? "provided" : "unchanged");
        
        EventUpdateDTO updateDTO = mapToEventUpdateDTO(input);
        EventResponseDTO result = eventService.updateEvent(eventId, updateDTO);
        logger.info("Successfully updated event {} for user {}", eventId, userId);
        return result;
    }

    /**
     * Deletes an event belonging to the authenticated user.
     * 
     * @param eventId the ID of the event to delete
     * @return true if deletion succeeded
     * @throws UnauthorizedException if no valid JWT token is provided
     * @throws EventNotFoundException if the event does not exist
     * @throws UserOwnershipViolationException if user doesn't own the event
     */
    @MutationMapping
    public Boolean deleteEvent(@Argument("id") Long eventId) {
        Long userId = authenticatedUserProvider.getCurrentUser().getId();
        logger.info("User {} deleting event {}", userId, eventId);
        
        eventService.deleteEvent(eventId);
        logger.info("Successfully deleted event {} for user {}", eventId, userId);
        return ApplicationConstants.GRAPHQL_OPERATION_SUCCESS;
    }

    // ==============================
    // region Mutations - EventRecaps
    // ==============================

    /**
     * Adds a recap to an event owned by the authenticated user.
     * 
     * @param input recap creation data including notes, media, and confirmation status
     * @return created event recap information
     * @throws UnauthorizedException if no valid JWT token is provided
     * @throws EventNotFoundException if the event does not exist
     * @throws UserOwnershipViolationException if user doesn't own the event
     */
    @MutationMapping
    public EventRecapResponseDTO addEventRecap(@Argument AddEventRecapInput input) {
        Long userId = authenticatedUserProvider.getCurrentUser().getId();
        logger.info("User {} adding recap to event {}: hasMedia={}, isUnconfirmed={}", 
            userId, input.eventId(), 
            input.media() != null && !input.media().isEmpty(),
            input.isUnconfirmed() != null ? input.isUnconfirmed() : false);
        
        List<RecapMediaCreateDTO> media = null;
        if (input.media() != null) {
            media = input.media().stream()
                    .map(m -> new RecapMediaCreateDTO(
                            m.mediaUrl(),
                            parseMediaType(m.mediaType()),
                            m.durationSeconds(),
                            m.mediaOrder()
                    ))
                    .collect(Collectors.toList());
        }

        EventRecapCreateDTO dto = new EventRecapCreateDTO(
                Long.valueOf(input.eventId()),
                input.notes(),
                input.recapName(),
                input.isUnconfirmed() != null ? input.isUnconfirmed() : false,
                media
        );

        EventRecapResponseDTO result = eventRecapService.addEventRecap(dto);
        logger.info("Successfully added recap to event {} for user {}", input.eventId(), userId);
        return result;
    }


    /**
     * Updates an event recap belonging to the authenticated user.
     * 
     * @param eventId the ID of the event whose recap to update
     * @param input update data containing optional recap modifications
     * @return updated event recap information
     * @throws UnauthorizedException if no valid JWT token is provided
     * @throws EventRecapNotFoundException if no recap exists for the event
     * @throws UserOwnershipViolationException if user doesn't own the event
     */
    @MutationMapping
    public EventRecapResponseDTO updateEventRecap(@Argument Long eventId, @Argument UpdateEventRecapInput input) {
        Long userId = authenticatedUserProvider.getCurrentUser().getId();
        logger.info("User {} updating recap for event {}: hasNotes={}, hasMedia={}", 
            userId, eventId, 
            input.notes() != null,
            input.media() != null && !input.media().isEmpty());
        
        List<RecapMediaCreateDTO> media = null;
        if (input.media() != null) {
            media = input.media().stream()
                    .map(m -> new RecapMediaCreateDTO(
                            m.mediaUrl(),
                            parseMediaType(m.mediaType()),
                            m.durationSeconds(),
                            m.mediaOrder()
                    ))
                    .collect(Collectors.toList());
        }

        EventRecapUpdateDTO dto = new EventRecapUpdateDTO(
                input.notes(),
                media
        );

        EventRecapResponseDTO result = eventRecapService.updateEventRecap(eventId, dto);
        logger.info("Successfully updated recap for event {} by user {}", eventId, userId);
        return result;
    }

    /**
     * Confirms an unconfirmed event recap belonging to the authenticated user.
     * 
     * @param eventId the ID of the event whose recap to confirm
     * @return confirmed event recap information
     * @throws UnauthorizedException if no valid JWT token is provided
     * @throws EventRecapNotFoundException if no recap exists for the event
     * @throws UserOwnershipViolationException if user doesn't own the event
     */
    @MutationMapping
    public EventRecapResponseDTO confirmEventRecap(@Argument Long eventId) {
        Long userId = authenticatedUserProvider.getCurrentUser().getId();
        logger.info("User {} confirming recap for event {}", userId, eventId);
        
        EventRecapResponseDTO result = eventRecapService.confirmEventRecap(eventId);
        logger.info("Successfully confirmed recap for event {} by user {}", eventId, userId);
        return result;
    }

    /**
     * Deletes an event recap belonging to the authenticated user.
     * 
     * @param eventId the ID of the event whose recap to delete
     * @return true if deletion succeeded
     * @throws UnauthorizedException if no valid JWT token is provided
     * @throws EventRecapNotFoundException if no recap exists for the event
     * @throws UserOwnershipViolationException if user doesn't own the event
     */
    @MutationMapping
    public Boolean deleteEventRecap(@Argument Long eventId) {
        Long userId = authenticatedUserProvider.getCurrentUser().getId();
        logger.info("User {} deleting recap for event {}", userId, eventId);
        
        eventRecapService.deleteEventRecap(eventId);
        logger.info("Successfully deleted recap for event {} by user {}", eventId, userId);
        return ApplicationConstants.GRAPHQL_OPERATION_SUCCESS;
    }

    // ==============================
    // region Mutations - RecapMedia
    // ==============================

    /**
     * Adds media to a recap belonging to the authenticated user.
     * 
     * @param recapId the ID of the recap to add media to
     * @param input media creation data including URL, type, and duration
     * @return created recap media information
     * @throws UnauthorizedException if no valid JWT token is provided
     * @throws EventRecapNotFoundException if the recap does not exist
     * @throws UserOwnershipViolationException if user doesn't own the recap
     * @throws InvalidUrlException if the media URL is invalid
     * @throws InvalidDurationException if duration is negative
     */
    @MutationMapping
    public RecapMediaResponseDTO addRecapMedia(@Argument Long recapId, @Argument @Valid CreateRecapMediaInput input) {
        Long userId = authenticatedUserProvider.getCurrentUser().getId();
        logger.info("User {} adding media to recap {}: type={}, duration={}", 
            userId, recapId, input.mediaType(), input.durationSeconds());
        
        RecapMediaCreateDTO dto = new RecapMediaCreateDTO(
                input.mediaUrl(),
                parseMediaType(input.mediaType()),
                input.durationSeconds(),
                input.mediaOrder()
        );

        RecapMediaResponseDTO result = recapMediaService.addRecapMedia(recapId, dto);
        logger.info("Successfully added media to recap {} for user {}", recapId, userId);
        return result;
    }

    /**
     * Updates recap media belonging to the authenticated user.
     * 
     * @param mediaId the ID of the media to update
     * @param input update data containing optional media modifications
     * @return updated recap media information
     * @throws UnauthorizedException if no valid JWT token is provided
     * @throws RecapMediaNotFoundException if the media does not exist
     * @throws UserOwnershipViolationException if user doesn't own the media
     * @throws InvalidUrlException if the media URL is invalid
     * @throws InvalidDurationException if duration is negative
     */
    @MutationMapping
    public RecapMediaResponseDTO updateRecapMedia(@Argument Long mediaId, @Argument @Valid UpdateRecapMediaInput input) {
        Long userId = authenticatedUserProvider.getCurrentUser().getId();
        logger.info("User {} updating media {}: hasUrl={}, hasType={}, hasDuration={}", 
            userId, mediaId,
            input.mediaUrl() != null,
            input.mediaType() != null,
            input.durationSeconds() != null);
        
        RecapMediaUpdateDTO dto = new RecapMediaUpdateDTO(
                input.mediaUrl(),
                input.mediaType(),
                input.durationSeconds()
        );

        RecapMediaResponseDTO result = recapMediaService.updateRecapMedia(mediaId, dto);
        logger.info("Successfully updated media {} for user {}", mediaId, userId);
        return result;
    }


    /**
     * Deletes recap media belonging to the authenticated user.
     * 
     * @param mediaId the ID of the media to delete
     * @return true if deletion succeeded
     * @throws UnauthorizedException if no valid JWT token is provided
     * @throws RecapMediaNotFoundException if the media does not exist
     * @throws UserOwnershipViolationException if user doesn't own the media
     */
    @MutationMapping
    public Boolean deleteRecapMedia(@Argument Long mediaId) {
        Long userId = authenticatedUserProvider.getCurrentUser().getId();
        logger.info("User {} deleting media {}", userId, mediaId);
        
        recapMediaService.deleteRecapMedia(mediaId);
        logger.info("Successfully deleted media {} for user {}", mediaId, userId);
        return ApplicationConstants.GRAPHQL_OPERATION_SUCCESS;
    }

    /**
     * Reorders media within a recap belonging to the authenticated user.
     * 
     * @param recapId the ID of the recap whose media to reorder
     * @param mediaOrder ordered list of media IDs representing the new sequence
     * @return true if reordering succeeded
     * @throws UnauthorizedException if no valid JWT token is provided
     * @throws EventRecapNotFoundException if the recap does not exist
     * @throws UserOwnershipViolationException if user doesn't own the recap
     * @throws IncompleteRecapMediaReorderListException if media order is incomplete
     */
    @MutationMapping
    public Boolean reorderRecapMedia(@Argument Long recapId, @Argument List<Long> mediaOrder) {
        Long userId = authenticatedUserProvider.getCurrentUser().getId();
        logger.info("User {} reordering {} media items for recap {}", userId, mediaOrder.size(), recapId);
        
        recapMediaService.reorderRecapMedia(recapId, mediaOrder);
        logger.info("Successfully reordered media for recap {} by user {}", recapId, userId);
        return ApplicationConstants.GRAPHQL_OPERATION_SUCCESS;
    }



    // ==============================
    // region Private Mapping Helpers
    // ==============================

    /**
     * Maps GraphQL UpdateEventInput to EventUpdateDTO for service layer.
     * 
     * <p>Handles the {@link UpdateFieldInput} pattern by converting optional
     * field updates to appropriate DTO format. Null fields are preserved
     * as null in the DTO, while absent fields are represented as null Optional.
     * 
     * @param input GraphQL update input with optional field semantics
     * @return DTO suitable for service layer consumption
     * @throws IllegalArgumentException if datetime parsing fails
     */
    private EventUpdateDTO mapToEventUpdateDTO(UpdateEventInput input) {
        return new EventUpdateDTO(
                mapField(input.name()),
                mapZonedDateTimeField(input.startTime()),
                mapZonedDateTimeField(input.endTime()),
                mapField(input.description()),
                mapField(input.labelId(), Long::valueOf),
                input.isCompleted() != null && input.isCompleted().isPresent()
                        ? input.isCompleted().getValue()
                        : null
        );
    }


    /**
     * Maps an UpdateFieldInput to an Optional for string fields.
     * 
     * @param field the field input to map
     * @return null if field is null, empty Optional if field is absent, 
     *         Optional containing value if field is present
     */
    private Optional<String> mapField(UpdateFieldInput<String> field) {
        if (field == null) return null;
        if (!field.isPresent()) return Optional.ofNullable(null);
        return Optional.ofNullable(field.getValue());
    }

    /**
     * Maps an UpdateFieldInput to an Optional with type transformation.
     * 
     * @param <T> the input field type
     * @param <R> the output type after mapping
     * @param field the field input to map
     * @param mapper function to transform the field value
     * @return null if field is null, empty Optional if field is absent,
     *         Optional containing mapped value if field is present
     */
    private <T, R> Optional<R> mapField(UpdateFieldInput<T> field, Function<T, R> mapper) {
        if (field == null) return null;
        if (!field.isPresent()) return Optional.ofNullable(null);
        return Optional.ofNullable(field.getValue()).map(mapper);
    }

    /**
     * Maps an UpdateFieldInput to an Optional ZonedDateTime with type coercion.
     * 
     * @param field the field input containing datetime data
     * @return null if field is null, empty Optional if field is absent,
     *         Optional containing parsed ZonedDateTime if field is present
     * @throws IllegalArgumentException if the field value cannot be parsed as ZonedDateTime
     */
    private Optional<ZonedDateTime> mapZonedDateTimeField(UpdateFieldInput<?> field) {
        if (field == null) return null;
        if (!field.isPresent()) return Optional.ofNullable(null);
        Object value = field.getValue();
        if (value == null) return Optional.ofNullable(null);
        if (value instanceof ZonedDateTime zdt) return Optional.of(zdt);
        if (value instanceof String s) return Optional.of(ZonedDateTime.parse(s));
        throw new IllegalArgumentException(ApplicationConstants.INVALID_DATETIME_FORMAT_MESSAGE + ": " + value.getClass());
    }

    /**
     * Safely parses a media type string to RecapMediaType enum.
     * 
     * @param mediaType the media type string to parse
     * @return parsed RecapMediaType or null if input is null
     * @throws IllegalArgumentException if the media type is invalid
     */
    private RecapMediaType parseMediaType(String mediaType) {
        return mediaType != null ? RecapMediaType.valueOf(mediaType.toUpperCase()) : null;
    }

    // endregion
}
