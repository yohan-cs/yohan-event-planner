package com.yohan.event_planner.dao;

import com.blazebit.persistence.PagedList;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.dto.EventFilterDTO;


public interface EventDAO {

    /**
     * Searches for events based on the specified user ID and optional filter criteria.
     *
     * @param userId the ID of the user whose events to search
     * @param filter the filter criteria for events (label, time window, etc.)
     * @return a list of matching events
     */
    PagedList<Event> findConfirmedEvents(Long userId, EventFilterDTO filter, int pageNumber, int pageSize);
}