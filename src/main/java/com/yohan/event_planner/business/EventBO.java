package com.yohan.event_planner.business;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.exception.EventNotFoundException;
import com.yohan.event_planner.exception.ConflictException;
import com.yohan.event_planner.exception.EventOwnershipException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Business Object (BO) interface for managing {@link Event} entities.
 *
 * <p>
 * Encapsulates business-level operations related to event scheduling,
 * including validation, conflict detection, and persistence delegation.
 * </p>
 *
 * <p>
 * This layer assumes that authorization and ownership checks are handled by
 * the service layer. It does not enforce access control.
 * </p>
 */
public interface EventBO {

    /**
     * Retrieves an event by its unique ID.
     *
     * @param eventId the ID of the event to retrieve
     * @return an {@link Optional} containing the event if found; otherwise empty
     */
    Optional<Event> getEventById(Long eventId);

    /**
     * Retrieves all events created by a specific user.
     *
     * @param userId the ID of the user who created the events
     * @return a list of events created by the user (may be empty, never {@code null})
     */
    List<Event> getEventsByUser(Long userId);

    /**
     * Retrieves events created by a user that start within the given date-time range.
     *
     * @param userId the ID of the event creator
     * @param start  the inclusive lower bound for event start time
     * @param end    the exclusive upper bound for event start time
     * @return a list of matching events (may be empty, never {@code null})
     */
    List<Event> getEventsByUserAndDateRange(Long userId, ZonedDateTime start, ZonedDateTime end);

    /**
     * Creates a new event after performing validation.
     *
     * @param event the event to create (must include start time, end time, and creator)
     * @return the saved {@link Event}
     * @throws IllegalArgumentException if time validation fails
     * @throws ConflictException if the event conflicts with another event
     */
    Event createEvent(Event event);

    /**
     * Updates an existing event after performing validation.
     *
     * @param event the event to update (must include ID and creator)
     * @return the updated and saved {@link Event}
     * @throws IllegalArgumentException if time validation fails
     * @throws ConflictException if the event overlaps with another event
     */
    Event updateEvent(Event event);

    /**
     * Deletes an event by its ID.
     *
     * @param eventId the ID of the event to delete
     * @throws EventNotFoundException if no event exists for the given ID
     * @throws EventOwnershipException if the current user does not own the event
     */
    void deleteEvent(Long eventId);

    /**
     * Validates that an event's start time occurs strictly before its end time.
     *
     * @param event the event to validate
     * @throws IllegalArgumentException if start time is equal to or after end time
     */
    void validateEventTimes(Event event);

    /**
     * Checks whether an event conflicts with another event by the same user.
     * If updating, the current event is excluded from the check.
     *
     * @param event the event to check for conflicts
     * @throws ConflictException if the event overlaps with another event
     */
    void checkForConflicts(Event event);
}
