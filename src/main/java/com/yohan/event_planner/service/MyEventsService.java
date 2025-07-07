package com.yohan.event_planner.service;


import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.RecurringEventResponseDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

public interface MyEventsService {

    /**
     * Fetches a page of recurring events for the user, ordered by endDate DESC, startDate DESC, startTime DESC, endTime DESC, id DESC.
     * Used for infinite horizontal scroll on the recurring events row.
     *
     * @param endDateCursor    the endDate of the last item in the previous page
     * @param startDateCursor  the startDate of the last item in the previous page
     * @param startTimeCursor  the startTime of the last item in the previous page
     * @param endTimeCursor    the endTime of the last item in the previous page
     * @param idCursor         the id of the last item in the previous page
     * @param limit            number of items to fetch
     * @return a list of recurring event response DTOs
     */
    List<RecurringEventResponseDTO> getRecurringEventsPage(
            LocalDate endDateCursor,
            LocalDate startDateCursor,
            LocalTime startTimeCursor,
            LocalTime endTimeCursor,
            Long idCursor,
            int limit
    );

    /**
     * Fetches a page of regular events for the user, ordered by endTime DESC, startTime DESC, id DESC.
     * Used for infinite horizontal scroll on the events row.
     *
     * @param endTimeCursor   the endTime of the last item in the previous page
     * @param startTimeCursor the startTime of the last item in the previous page
     * @param idCursor        the id of the last item in the previous page
     * @param limit           number of items to fetch
     * @return a list of event response DTOs
     */
    List<EventResponseDTO> getEventsPage(
            ZonedDateTime endTimeCursor,
            ZonedDateTime startTimeCursor,
            Long idCursor,
            int limit
    );
}