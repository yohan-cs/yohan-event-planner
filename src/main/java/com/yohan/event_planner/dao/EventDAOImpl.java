package com.yohan.event_planner.dao;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.PagedList;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.dto.EventFilterDTO;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Data Access Object implementation for Event entities using Blaze-Persistence.
 * 
 * <p>This implementation provides optimized queries for confirmed events with support for:
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
 * @see EventDAO
 * @see EventFilterDTO
 * @see com.blazebit.persistence.CriteriaBuilder
 */
@Repository
public class EventDAOImpl implements EventDAO {

    private static final Logger logger = LoggerFactory.getLogger(EventDAOImpl.class);

    private final CriteriaBuilderFactory cbf;
    private final EntityManager em;

    /**
     * Constructs a new EventDAOImpl with required Blaze-Persistence dependencies.
     * 
     * @param cbf the CriteriaBuilderFactory for creating Blaze-Persistence queries
     * @param em the EntityManager for JPA operations
     */
    public EventDAOImpl(
            CriteriaBuilderFactory cbf,
            EntityManager em
    ) {
        this.cbf = cbf;
        this.em = em;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>This implementation uses Blaze-Persistence for optimized querying with strategic
     * eager fetching and efficient filtering. The query applies filters in sequence:
     * user ownership → confirmation status → label filtering → time window → completion state → sorting.</p>
     * 
     * <h3>Query Optimization</h3>
     * <ul>
     *   <li>Eager fetches creator and label to prevent N+1 queries</li>
     *   <li>Uses indexed columns for efficient filtering</li>
     *   <li>Applies timezone-aware time window filtering</li>
     * </ul>
     * 
     * @param userId the ID of the user whose events to retrieve
     * @param filter the filter criteria containing time window, label, completion state, and sorting options
     * @param pageNumber zero-based page number for pagination
     * @param pageSize maximum number of results per page
     * @return paginated list of confirmed events matching all filter criteria
     */
    @Override
    public PagedList<Event> findConfirmedEvents(Long userId, EventFilterDTO filter, int pageNumber, int pageSize) {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(filter, "filter cannot be null");
        
        logger.debug("Finding confirmed events for user {} with filter: labelId={}, timeFilter={}, start={}, end={}, includeIncompletePastEvents={}, sortDescending={}, page={}, size={}",
                userId, filter.labelId(), filter.timeFilter(), filter.start(), filter.end(), filter.includeIncompletePastEvents(), filter.sortDescending(), pageNumber, pageSize);

        CriteriaBuilder<Event> cb = createBaseQuery();

        applyUserFilter(userId, cb);
        applyOnlyConfirmedFilter(cb);
        applyLabelFilter(filter, cb);
        applyTimeWindowFilter(filter, cb);
        applyIncompletePastEventFilter(filter, cb);
        applySortOrder(filter, cb);

        PagedList<Event> results = cb.page(pageNumber, pageSize).getResultList();
        
        logger.info("Retrieved {} confirmed events for user {} (page {}/{}, total elements: {})",
                results.size(), userId, pageNumber + 1, results.getTotalPages(), results.getTotalSize());
        
        return results;
    }

    /**
     * Creates the base query with optimized eager fetching strategy.
     * 
     * <p>Establishes the foundation query with strategic eager fetching to prevent
     * N+1 query problems when accessing creator and label associations.</p>
     * 
     * @return configured CriteriaBuilder with base fetch strategy
     */
    private CriteriaBuilder<Event> createBaseQuery() {
        return cbf.create(em, Event.class)
                .fetch("creator")
                .fetch("label");
    }

    /**
     * Applies user ownership filtering to restrict results to the specified user's events.
     * 
     * <p>This is a security-critical filter that ensures users can only access their own
     * events. Uses indexed creator.id for efficient filtering.</p>
     * 
     * @param userId the ID of the user whose events to retrieve
     * @param cb the CriteriaBuilder to apply the filter to
     */
    private void applyUserFilter(Long userId, CriteriaBuilder<Event> cb) {
        logger.debug("Applying user filter for userId: {}", userId);
        cb.where("creator.id").eq(userId);
    }

    /**
     * Applies confirmation status filtering to exclude draft/unconfirmed events.
     * 
     * <p>Only returns confirmed events that are ready for display
     * and user interaction. Draft events are excluded from all query results.</p>
     * 
     * @param cb the CriteriaBuilder to apply the filter to
     */
    private void applyOnlyConfirmedFilter(CriteriaBuilder<Event> cb) {
        cb.where("unconfirmed").eq(false);
    }

    /**
     * Applies optional label-based filtering when a label ID is specified in the filter.
     * 
     * <p>When labelId is provided, restricts results to events associated with
     * that specific label. Label ownership validation is handled at the service layer.</p>
     * 
     * @param filter the filter containing optional label ID criteria
     * @param cb the CriteriaBuilder to apply the filter to
     */
    private void applyLabelFilter(EventFilterDTO filter, CriteriaBuilder<Event> cb) {
        if (filter.labelId() != null) {
            logger.debug("Applying label filter for labelId: {}", filter.labelId());
            cb.where("label.id").eq(filter.labelId());
        }
    }

    /**
     * Applies time window filtering using timezone-aware date range logic.
     * 
     * <p>Filters for events whose time ranges overlap with the specified
     * time window. Uses standard interval overlap logic where two ranges [A,B] and [C,D]
     * overlap if A ≤ D and C ≤ B.</p>
     * 
     * <h3>Overlap Logic</h3>
     * <ul>
     *   <li>Event starts before or at the filter end time</li>
     *   <li>Event ends after or at the filter start time</li>
     * </ul>
     * 
     * <p><strong>Note:</strong> The service layer resolves TimeFilter enum values to actual
     * ZonedDateTime boundaries before calling this method.</p>
     * 
     * @param filter the filter containing resolved start and end times
     * @param cb the CriteriaBuilder to apply the filter to
     */
    private void applyTimeWindowFilter(EventFilterDTO filter, CriteriaBuilder<Event> cb) {
        // Service layer has already resolved TimeFilter to actual times
        ZonedDateTime startTime = filter.start();
        ZonedDateTime endTime = filter.end();
        
        logger.debug("Applying time window filter: startTime <= {} AND endTime >= {}", endTime, startTime);
        
        // Filter for events that overlap with the time window
        cb.where("startTime").le(endTime);
        cb.where("endTime").ge(startTime);
    }

    /**
     * Applies filtering to optionally exclude incomplete past events.
     * 
     * <p>When includeIncompletePastEvents is false, only includes events that are either:
     * <ul>
     *   <li>Future events (end time after reference time), OR</li>
     *   <li>Past events that are marked as completed</li>
     * </ul>
     * 
     * <p><strong>Note:</strong> The service layer provides a resolved reference time
     * based on the user's timezone before calling this method.</p>
     * 
     * @param filter the filter containing the incomplete past events preference and reference time
     * @param cb the CriteriaBuilder to apply the filter to
     */
    private void applyIncompletePastEventFilter(EventFilterDTO filter, CriteriaBuilder<Event> cb) {
        if (Boolean.FALSE.equals(filter.includeIncompletePastEvents())) {
            // Service layer provides reference time in the 'end' field when this filter is used
            ZonedDateTime referenceTime = filter.end();
            logger.debug("Applying incomplete past event filter: excluding incomplete events before {}", referenceTime);
            cb.whereOr()
                    .where("endTime").gt(referenceTime)
                    .where("isCompleted").eq(true)
                    .endOr();
        }
    }

    /**
     * Applies sorting criteria based on the filter's sort direction preference.
     * 
     * <p>Implements a multi-level sort strategy for consistent, deterministic ordering:
     * <ol>
     *   <li>Primary: startTime (temporal ordering)</li>
     *   <li>Secondary: id (stable sort for identical times)</li>
     * </ol>
     * 
     * <p>The multi-level sort ensures consistent pagination behavior and prevents
     * duplicate results across page boundaries when multiple events
     * share the same start time.</p>
     * 
     * @param filter the filter containing sort direction preference
     * @param cb the CriteriaBuilder to apply sorting to
     */
    private void applySortOrder(EventFilterDTO filter, CriteriaBuilder<Event> cb) {
        boolean descending = Boolean.TRUE.equals(filter.sortDescending());
        logger.debug("Applying sort order: {} by startTime, id", descending ? "DESC" : "ASC");
        
        if (descending) {
            cb.orderByDesc("startTime").orderByDesc("id");
        } else {
            cb.orderByAsc("startTime").orderByAsc("id");
        }
    }
}
