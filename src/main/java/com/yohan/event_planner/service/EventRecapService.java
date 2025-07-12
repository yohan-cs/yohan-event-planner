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

    /**
     * Creates a new recap for an event.
     *
     * <p>
     * Creates either a confirmed or draft recap based on the event's completion status
     * and the DTO's unconfirmed flag. Only event owners can create recaps, and each
     * event can have at most one recap.
     * </p>
     *
     * @param dto the recap creation data transfer object
     * @return the created recap details
     * @throws InvalidEventStateException   if the event is not in an appropriate state
     * @throws UserOwnershipException       if the event is not owned by the current user
     * @throws EventRecapException          if a recap already exists for the event
     * @throws EventNotFoundException       if the event does not exist
     */
    EventRecapResponseDTO addEventRecap(EventRecapCreateDTO dto);

    /**
     * Confirms a draft recap, marking it as finalized.
     *
     * <p>
     * Transitions a recap from draft state to confirmed state. Once confirmed,
     * the recap cannot be modified. Only works on completed events with existing
     * unconfirmed recaps.
     * </p>
     *
     * @param eventId the ID of the completed event
     * @return the confirmed recap details
     * @throws InvalidEventStateException        if the event is not marked as completed
     * @throws UserOwnershipException            if the event is not owned by the current user
     * @throws EventRecapNotFoundException       if no recap exists for the event
     * @throws EventRecapAlreadyConfirmedException if the recap is already confirmed
     */
    EventRecapResponseDTO confirmEventRecap(Long eventId);

    /**
     * Updates an existing recap's content.
     *
     * <p>
     * Updates the recap's notes and/or media content. Only the event owner can
     * update recaps. Media updates replace all existing media with the provided
     * media list.
     * </p>
     *
     * @param eventId the ID of the event whose recap should be updated
     * @param dto the recap update data transfer object
     * @return the updated recap details
     * @throws UserOwnershipException       if the event is not owned by the current user
     * @throws EventRecapNotFoundException  if no recap exists for the event
     * @throws EventNotFoundException       if the event does not exist
     */
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