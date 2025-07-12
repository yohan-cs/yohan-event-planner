package com.yohan.event_planner.dao;

import com.blazebit.persistence.PagedList;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.dto.EventFilterDTO;

/**
 * Data Access Object for advanced event query operations using Blaze-Persistence.
 * 
 * <p>This DAO provides optimized queries for confirmed events with support for:
 * <ul>
 *   <li>Complex filtering using {@link EventFilterDTO} criteria</li>
 *   <li>Efficient pagination with {@link PagedList} results</li>
 *   <li>Eager fetching of creator and label associations</li>
 *   <li>Time window filtering for event date ranges</li>
 * </ul>
 * 
 * <h2>Architecture Context</h2>
 * <p>This DAO operates within the data access layer, providing a clean abstraction between
 * the service layer business logic and the underlying persistence mechanism. It leverages
 * Blaze-Persistence for advanced JPA querying capabilities beyond standard Spring Data repositories.</p>
 * 
 * <h2>Query Optimization</h2>
 * <p>All queries are optimized for performance through:</p>
 * <ul>
 *   <li>Strategic eager fetching to avoid N+1 queries</li>
 *   <li>Indexed filtering on user, confirmation status, and labels</li>
 *   <li>Efficient date range queries using timezone-aware filtering</li>
 * </ul>
 * 
 * <h2>Filtering Strategy</h2>
 * <p>The implementation applies filters in a specific order for optimal query performance:
 * user ownership, confirmation status, label filtering, time windows, completion state, and finally sorting.</p>
 * 
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 * @see EventFilterDTO
 * @see com.blazebit.persistence.CriteriaBuilder
 */
public interface EventDAO {

    /**
     * Finds all confirmed events for a user that match the given filter criteria.
     * This excludes any draft or incomplete event templates.
     * <p>
     * Can be used for both frontend search queries and internal processing,
     * such as displaying events within a time window.
     *
     * @param userId the ID of the user whose events to retrieve
     * @param filter the filter criteria (time window, label, completion state, etc.)
     * @param pageNumber zero-based page number for pagination
     * @param pageSize maximum number of results per page
     * @return a paginated list of confirmed events matching the filter
     */
    PagedList<Event> findConfirmedEvents(Long userId, EventFilterDTO filter, int pageNumber, int pageSize);
}