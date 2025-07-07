package com.yohan.event_planner.service;

import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.EventChangeContextDTO;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.RecurringEventCreateDTO;
import com.yohan.event_planner.dto.RecurringEventCreationResultDTO;
import com.yohan.event_planner.dto.RecurringEventFilterDTO;
import com.yohan.event_planner.dto.RecurringEventResponseDTO;
import com.yohan.event_planner.dto.RecurringEventUpdateDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

public interface RecurringEventService {

    /**
     * Retrieves a recurring event by ID for the given user.
     *
     * <p>Admins may access others' events, but regular users can only access their own.</p>
     *
     * @param recurringEventId the ID of the recurring event
     * @return the requested recurring event
     */
    RecurringEventResponseDTO getRecurringEventById(Long recurringEventId);

    Page<RecurringEventResponseDTO> getConfirmedRecurringEventsForCurrentUser(RecurringEventFilterDTO filter, int pageNumber, int pageSize);

    List<RecurringEventResponseDTO> getConfirmedRecurringEventsPage(
            LocalDate endDateCursor,
            LocalDate startDateCursor,
            LocalTime startTimeCursor,
            LocalTime endTimeCursor,
            Long idCursor,
            int limit
    );


    /**
     * Returns all unconfirmed (draft) recurring events owned by the user.
     *
     * <p>This is a self-only operation.</p>
     *
     * @return list of unconfirmed recurring events
     */
    List<RecurringEventResponseDTO> getUnconfirmedRecurringEventsForCurrentUser();

    /**
     * Creates a new recurring event in draft or confirmed state, depending on input.
     *
     * @param dto the creation DTO containing recurrence details
     * @return the created recurring event
     */
    RecurringEventResponseDTO createRecurringEvent(RecurringEventCreateDTO dto);


    RecurringEventResponseDTO confirmRecurringEvent(Long recurringEventId);

    /**
     * Updates an existing recurring event.
     *
     * <p>If the event is confirmed, only future occurrences will be updated. Past instances remain unchanged.</p>
     *
     * @param recurringEventId the ID of the recurring event to update
     * @param dto the update DTO with the new values
     * @return the updated recurring event
     */
    RecurringEventResponseDTO updateRecurringEvent(Long recurringEventId, RecurringEventUpdateDTO dto);

    /**
     * Deletes a recurring event and all future associated events.
     *
     * <p>Past instances are not affected unless explicitly deleted elsewhere.</p>
     *
     * @param recurringEventId the ID of the recurring event to delete
     */
    void deleteRecurringEvent(Long recurringEventId);
    void deleteUnconfirmedRecurringEventsForCurrentUser();

    RecurringEventResponseDTO addSkipDays(Long recurringEventId, Set<LocalDate> skipDaysToAdd);
    RecurringEventResponseDTO removeSkipDays(Long recurringEventId, Set<LocalDate> skipDaysToRemove);

    List<EventResponseDTO> generateVirtuals(Long userId, ZonedDateTime startTime, ZonedDateTime endTime, ZoneId userZoneId);
}
