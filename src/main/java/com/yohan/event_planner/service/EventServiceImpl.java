package com.yohan.event_planner.service;

import com.yohan.event_planner.business.EventBO;
import com.yohan.event_planner.business.handler.EventPatchHandler;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.EventCreateDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.exception.EventNotFoundException;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Concrete implementation of the {@link EventService} interface.
 *
 * <p>
 * Coordinates validation, business logic, and DTO conversion for managing {@link Event} entities.
 * This implementation enforces ownership checks via {@link OwnershipValidator} and delegates
 * domain logic to {@link EventBO}.
 * </p>
 *
 * <h2>Time Handling</h2>
 * <ul>
 *     <li>All start and end times are stored internally in UTC.</li>
 *     <li>Original time zone IDs are stored separately to support accurate local time display.</li>
 *     <li>Time zone adjustments for response objects are based on the viewer's profile time zone.</li>
 * </ul>
 *
 * <h2>Duration Handling</h2>
 * <ul>
 *     <li>{@code durationMinutes} is calculated during event creation if {@code endTime} is provided.</li>
 *     <li>During update operations, {@code durationMinutes} is updated or cleared via the patch handler.</li>
 *     <li>Duration is expressed in whole minutes and stored explicitly in the {@link Event} entity.</li>
 * </ul>
 *
 * <p>
 * This class is not responsible for persistence or direct repository access.
 * It delegates those concerns to the {@link EventBO}.
 * </p>
 */
@Service
public class EventServiceImpl implements EventService {

    private static final Logger logger = LoggerFactory.getLogger(EventServiceImpl.class);

    private final EventBO eventBO;
    private final EventPatchHandler eventPatchHandler;
    private final OwnershipValidator ownershipValidator;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public EventServiceImpl(
            EventBO eventBO,
            EventPatchHandler eventPatchHandler,
            OwnershipValidator ownershipValidator,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.eventBO = eventBO;
        this.eventPatchHandler = eventPatchHandler;
        this.ownershipValidator = ownershipValidator;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventResponseDTO getEventById(Long eventId) {
        Event event = eventBO.getEventById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        User viewer = authenticatedUserProvider.getCurrentUser();
        return buildEventResponseDTO(event, viewer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<EventResponseDTO> getEventsByUser(Long userId) {
        List<Event> events = eventBO.getEventsByUser(userId);
        User viewer = authenticatedUserProvider.getCurrentUser();
        return events.stream()
                .map(event -> buildEventResponseDTO(event, viewer))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<EventResponseDTO> getEventsByUserAndDateRange(Long userId, ZonedDateTime start, ZonedDateTime end) {
        List<Event> events = eventBO.getEventsByUserAndDateRange(userId, start, end);
        User viewer = authenticatedUserProvider.getCurrentUser();
        return events.stream()
                .map(event -> buildEventResponseDTO(event, viewer))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<EventResponseDTO> getEventsByCurrentUser() {
        User currentUser = authenticatedUserProvider.getCurrentUser();
        List<Event> events = eventBO.getEventsByUser(currentUser.getId());
        return events.stream()
                .map(event -> buildEventResponseDTO(event, currentUser))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<EventResponseDTO> getEventsByCurrentUserAndDateRange(ZonedDateTime start, ZonedDateTime end) {
        User currentUser = authenticatedUserProvider.getCurrentUser();
        List<Event> events = eventBO.getEventsByUserAndDateRange(currentUser.getId(), start, end);
        return events.stream()
                .map(event -> buildEventResponseDTO(event, currentUser))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventResponseDTO createEvent(EventCreateDTO dto) {
        User creator = authenticatedUserProvider.getCurrentUser();

        // Times are stored in UTC. Time zone fields are tracked separately and preserved if they differ.
        Event event = new Event(
                dto.name(),
                dto.startTime(),
                creator
        );
        event.setEndTime(dto.endTime());
        event.setDescription(dto.description());

        if (dto.endTime() != null) {
            long duration = java.time.Duration.between(
                    dto.startTime().withZoneSameInstant(ZoneOffset.UTC),
                    dto.endTime().withZoneSameInstant(ZoneOffset.UTC)
            ).toMinutes();
            event.setDurationMinutes((int) duration);
        }

        Event created = eventBO.createEvent(event);
        return buildEventResponseDTO(created, creator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventResponseDTO updateEvent(Long eventId, EventUpdateDTO dto) {
        Event event = eventBO.getEventById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        User user = authenticatedUserProvider.getCurrentUser();
        Long currentUserId = user.getId();
        ownershipValidator.validateEventOwnership(currentUserId, event);

        boolean changed = eventPatchHandler.applyPatch(event, dto, user);
        Event updated = changed ? eventBO.updateEvent(event) : event;

        return buildEventResponseDTO(updated, user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteEvent(Long eventId) {
        Event existing = eventBO.getEventById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        User currentUser = authenticatedUserProvider.getCurrentUser();
        ownershipValidator.validateEventOwnership(currentUser.getId(), existing);

        eventBO.deleteEvent(eventId);
    }

    /**
     * Builds a timezone-adjusted {@link EventResponseDTO} for the given event and viewer.
     * If the viewer's timezone differs from the original event timezone, original timestamps
     * will be included as secondary fields in the response.
     *
     * @param event  the event to convert
     * @param viewer the authenticated user viewing the event
     * @return a fully populated {@link EventResponseDTO}
     */
    private EventResponseDTO buildEventResponseDTO(Event event, User viewer) {
        String creatorZone = event.getCreator().getTimezone();
        String startZone = event.getStartTimezone();
        String endZone = event.getEndTimezone();

        ZonedDateTime startTimeUtc = event.getStartTime();
        ZonedDateTime endTimeUtc = event.getEndTime();
        Integer durationMinutes = event.getDurationMinutes();

        String startTimeZone = !creatorZone.equals(startZone) ? startZone : null;
        String endTimeZone = !creatorZone.equals(endZone) ? endZone : null;

        return new EventResponseDTO(
                event.getId(),
                event.getName(),
                startTimeUtc,
                endTimeUtc,
                durationMinutes,
                startTimeZone,
                endTimeZone,
                event.getDescription(),
                event.getCreator().getId(),
                event.getCreator().getUsername(),
                creatorZone
        );
    }
}
