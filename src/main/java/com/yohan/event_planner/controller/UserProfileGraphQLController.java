package com.yohan.event_planner.controller;

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

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class UserProfileGraphQLController {

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

    @QueryMapping
    public UserProfileResponseDTO userProfile(@Argument String username) {
        Long viewerId = authenticatedUserProvider.getCurrentUser().getId();
        return userService.getUserProfile(username, viewerId);
    }

    @QueryMapping
    public EventRecapResponseDTO eventRecap(@Argument Long eventId) {
        return eventRecapService.getEventRecap(eventId);
    }

    // ==============================
    // region SchemaMappings
    // ==============================

    @SchemaMapping(typeName = "UserProfile", field = "weekView")
    public WeekViewDTO weekView(UserProfileResponseDTO profile,
                                @Argument("anchorDate") LocalDate anchorDate) {
        return eventService.generateWeekView(anchorDate);
    }

    // ==============================
    // region Mutations - UserHeader
    // ==============================

    @MutationMapping
    public UserHeaderResponseDTO updateUserHeader(@Argument("input") UserHeaderUpdateDTO input) {
        Long userId = authenticatedUserProvider.getCurrentUser().getId();
        return userService.updateUserHeader(userId, input);
    }

    // ==============================
    // region Mutations - Badges
    // ==============================

    @MutationMapping
    public BadgeResponseDTO updateBadge(@Argument("id") Long badgeId, @Argument("input") BadgeUpdateDTO input) {
        return badgeService.updateBadge(badgeId, input);
    }

    @MutationMapping
    public Boolean deleteBadge(@Argument("id") Long badgeId) {
        badgeService.deleteBadge(badgeId);
        return true;
    }

    @MutationMapping
    public Boolean reorderBadges(@Argument("ids") List<Long> ids) {
        Long userId = authenticatedUserProvider.getCurrentUser().getId();
        badgeService.reorderBadges(userId, ids);
        return true;
    }

    @MutationMapping
    public Boolean reorderBadgeLabels(@Argument("badgeId") Long badgeId,
                                      @Argument("labelOrder") List<Long> labelOrder) {
        badgeService.reorderBadgeLabels(badgeId, labelOrder);
        return true;
    }

    // ==============================
    // region Mutations - Events
    // ==============================

    @MutationMapping
    public EventResponseDTO updateEvent(@Argument String id, @Argument UpdateEventInput input) {
        Long eventId = Long.valueOf(id);
        EventUpdateDTO updateDTO = mapToEventUpdateDTO(input);
        return eventService.updateEvent(eventId, updateDTO);
    }

    @MutationMapping
    public Boolean deleteEvent(@Argument("id") Long eventId) {
        eventService.deleteEvent(eventId);
        return true;
    }

    // ==============================
    // region Mutations - EventRecaps
    // ==============================

    @MutationMapping
    public EventRecapResponseDTO addEventRecap(@Argument AddEventRecapInput input) {
        List<RecapMediaCreateDTO> media = null;
        if (input.media() != null) {
            media = input.media().stream()
                    .map(m -> new RecapMediaCreateDTO(
                            m.mediaUrl(),
                            m.mediaType() != null ? RecapMediaType.valueOf(m.mediaType().toUpperCase()) : null, // safer enum conversion with null check
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

        return eventRecapService.addEventRecap(dto);
    }


    @MutationMapping
    public EventRecapResponseDTO updateEventRecap(@Argument Long eventId, @Argument UpdateEventRecapInput input) {
        List<RecapMediaCreateDTO> media = null;
        if (input.media() != null) {
            media = input.media().stream()
                    .map(m -> new RecapMediaCreateDTO(
                            m.mediaUrl(),
                            m.mediaType() != null ? RecapMediaType.valueOf(m.mediaType().toUpperCase()) : null, // safer enum conversion with null check
                            m.durationSeconds(),
                            m.mediaOrder()
                    ))
                    .collect(Collectors.toList());
        }

        EventRecapUpdateDTO dto = new EventRecapUpdateDTO(
                input.notes(),
                media
        );

        return eventRecapService.updateEventRecap(eventId, dto);
    }

    @MutationMapping
    public EventRecapResponseDTO confirmEventRecap(@Argument Long eventId) {
        return eventRecapService.confirmEventRecap(eventId);
    }

    @MutationMapping
    public Boolean deleteEventRecap(@Argument Long eventId) {
        eventRecapService.deleteEventRecap(eventId);
        return true;
    }

    // ==============================
    // region Mutations - RecapMedia
    // ==============================

    @MutationMapping
    public RecapMediaResponseDTO addRecapMedia(@Argument Long recapId, @Argument @Valid CreateRecapMediaInput input) {
        RecapMediaCreateDTO dto = new RecapMediaCreateDTO(
                input.mediaUrl(),
                input.mediaType() != null ? RecapMediaType.valueOf(input.mediaType().toUpperCase()) : null, // correct enum used
                input.durationSeconds(),
                input.mediaOrder()
        );

        return recapMediaService.addRecapMedia(recapId, dto);
    }

    @MutationMapping
    public RecapMediaResponseDTO updateRecapMedia(@Argument Long mediaId, @Argument @Valid UpdateRecapMediaInput input) {
        RecapMediaUpdateDTO dto = new RecapMediaUpdateDTO(
                input.mediaUrl(),
                input.mediaType(),
                input.durationSeconds()
        );

        return recapMediaService.updateRecapMedia(mediaId, dto);
    }


    @MutationMapping
    public Boolean deleteRecapMedia(@Argument Long mediaId) {
        recapMediaService.deleteRecapMedia(mediaId);
        return true;
    }

    @MutationMapping
    public Boolean reorderRecapMedia(@Argument Long recapId, @Argument List<Long> mediaOrder) {
        recapMediaService.reorderRecapMedia(recapId, mediaOrder);
        return true;
    }



    // ==============================
    // region Private Mapping Helpers
    // ==============================

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

    private EventRecapCreateDTO mapToEventRecapCreateDTO(AddEventRecapInput input) {
        List<RecapMediaCreateDTO> media = input.media() == null ? null :
                input.media().stream()
                        .map(m -> new RecapMediaCreateDTO(
                                m.mediaUrl(),
                                RecapMediaType.valueOf(m.mediaType()),
                                m.durationSeconds(),
                                m.mediaOrder()
                        ))
                        .collect(Collectors.toList());

        return new EventRecapCreateDTO(
                Long.valueOf(input.eventId()),
                input.notes(),
                input.recapName(),
                input.isUnconfirmed() != null ? input.isUnconfirmed() : false,
                media
        );
    }

    private Optional<String> mapField(UpdateFieldInput<String> field) {
        if (field == null) return null;
        if (!field.isPresent()) return Optional.ofNullable(null);
        return Optional.ofNullable(field.getValue());
    }

    private <T, R> Optional<R> mapField(UpdateFieldInput<T> field, Function<T, R> mapper) {
        if (field == null) return null;
        if (!field.isPresent()) return Optional.ofNullable(null);
        return Optional.ofNullable(field.getValue()).map(mapper);
    }

    private Optional<ZonedDateTime> mapZonedDateTimeField(UpdateFieldInput<?> field) {
        if (field == null) return null;
        if (!field.isPresent()) return Optional.ofNullable(null);
        Object value = field.getValue();
        if (value == null) return Optional.ofNullable(null);
        if (value instanceof ZonedDateTime zdt) return Optional.of(zdt);
        if (value instanceof String s) return Optional.of(ZonedDateTime.parse(s));
        throw new IllegalArgumentException("Unexpected value type for ZonedDateTime field: " + value.getClass());
    }

    // endregion
}
