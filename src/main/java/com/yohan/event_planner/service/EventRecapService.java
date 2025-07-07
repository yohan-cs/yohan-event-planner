package com.yohan.event_planner.service;

import com.yohan.event_planner.dto.EventRecapCreateDTO;
import com.yohan.event_planner.dto.EventRecapResponseDTO;
import com.yohan.event_planner.dto.EventRecapUpdateDTO;
import com.yohan.event_planner.exception.EventRecapException;
import com.yohan.event_planner.exception.EventRecapNotFoundException;
import com.yohan.event_planner.exception.InvalidEventStateException;
import com.yohan.event_planner.exception.UserOwnershipException;

/**
 * Service interface for managing event recaps.
 *
 * <p>
 * A recap represents a user's reflection or notes about a completed event.
 * Only the creator of the event may add, update, view, or delete a recap.
 * Each event may have at most one associated recap.
 * </p>
 *
 * <p>
 * Recaps are only allowed for events marked as {@code isCompleted == true}.
 * Attempting to operate on events that are not completed will result in an error.
 * </p>
 */
public interface EventRecapService {

    /**
     * Retrieves the recap associated with the specified event.
     *
     * @param eventId the ID of the completed event
     * @return the recap details for the event
     * @throws InvalidEventStateException   if the event is not marked as completed
     * @throws UserOwnershipException       if the event is not owned by the current user
     * @throws EventRecapNotFoundException  if no recap exists for the event
     */
    EventRecapResponseDTO getEventRecap(Long eventId);

    EventRecapResponseDTO addEventRecap(EventRecapCreateDTO dto);

    EventRecapResponseDTO confirmEventRecap(Long eventId);

    EventRecapResponseDTO updateEventRecap(Long eventId, EventRecapUpdateDTO dto);

    /**
     * Deletes the recap associated with the specified event.
     *
     * @param eventId the ID of the completed event
     * @throws InvalidEventStateException   if the event is not marked as completed
     * @throws UserOwnershipException       if the event is not owned by the current user
     * @throws EventRecapNotFoundException  if no recap exists for the event
     */
    void deleteEventRecap(Long eventId);
}