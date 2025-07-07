package com.yohan.event_planner.service;


import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.dto.EventResponseDTO;
import com.yohan.event_planner.dto.EventResponseDTOFactory;
import com.yohan.event_planner.dto.RecurringEventResponseDTO;
import com.yohan.event_planner.repository.EventRepository;
import com.yohan.event_planner.repository.RecurringEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MyEventsServiceImpl implements MyEventsService {

    private final EventService eventService;
    private final RecurringEventService recurringEventService;

    public MyEventsServiceImpl(
            EventService eventService,
            RecurringEventService recurringEventService
    ) {
        this.eventService = eventService;
        this.recurringEventService = recurringEventService;
    }

    @Override
    public List<RecurringEventResponseDTO> getRecurringEventsPage(
            LocalDate endDateCursor,
            LocalDate startDateCursor,
            LocalTime startTimeCursor,
            LocalTime endTimeCursor,
            Long idCursor,
            int limit
    ) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }

        return recurringEventService.getConfirmedRecurringEventsPage(
                endDateCursor,
                startDateCursor,
                startTimeCursor,
                endTimeCursor,
                idCursor,
                limit
        );
    }


    @Override
    public List<EventResponseDTO> getEventsPage(
            ZonedDateTime endTimeCursor,
            ZonedDateTime startTimeCursor,
            Long idCursor,
            int limit
    ) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }

        return eventService.getConfirmedEventsPage(
                endTimeCursor,
                startTimeCursor,
                idCursor,
                limit
        );
    }

}