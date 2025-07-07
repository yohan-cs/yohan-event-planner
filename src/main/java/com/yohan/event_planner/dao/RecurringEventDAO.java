package com.yohan.event_planner.dao;

import com.blazebit.persistence.PagedList;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.dto.RecurringEventFilterDTO;

public interface RecurringEventDAO {

    /**
     * Finds all confirmed recurring events for a user that match the given filter criteria.
     * This excludes any draft or incomplete recurring event templates.
     * <p>
     * Can be used for both frontend search queries and internal processing,
     * such as generating virtual events within a time window.
     *
     * @param userId the ID of the user whose recurring events to retrieve
     * @param filter the filter criteria (time window, label, etc.)
     * @return a list of confirmed recurring events matching the filter
     */
    PagedList<RecurringEvent> findConfirmedRecurringEvents(Long userId, RecurringEventFilterDTO filter, int pageNumber, int pageSize);
}