package com.yohan.event_planner.dao;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.PagedList;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.dto.RecurringEventFilterDTO;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Data Access Object implementation for RecurringEvent entities using Blaze-Persistence.
 * 
 * <p>This implementation provides optimized queries for confirmed recurring events with support for:
 * <ul>
 *   <li>Complex filtering using {@link RecurringEventFilterDTO} criteria</li>
 *   <li>Efficient pagination with {@link PagedList} results</li>
 *   <li>Eager fetching of creator and label associations</li>
 *   <li>Time window filtering for recurring event date ranges</li>
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
 *   <li>Efficient date range queries using overlapping date logic</li>
 * </ul>
 * 
 * <h2>Filtering Strategy</h2>
 * <p>The implementation applies filters in a specific order for optimal query performance:
 * user ownership, confirmation status, label filtering, time windows, and finally sorting.</p>
 * 
 * @author Event Planner Development Team
 * @version 2.0.0
 * @since 1.0.0
 * @see RecurringEventDAO
 * @see RecurringEventFilterDTO
 * @see com.blazebit.persistence.CriteriaBuilder
 */
@Repository
public class RecurringEventDAOImpl implements RecurringEventDAO {

    private static final Logger logger = LoggerFactory.getLogger(RecurringEventDAOImpl.class);

    private final CriteriaBuilderFactory cbf;
    private final EntityManager em;

    /**
     * Constructs a new RecurringEventDAOImpl with required Blaze-Persistence dependencies.
     * 
     * @param cbf the CriteriaBuilderFactory for creating Blaze-Persistence queries
     * @param em the EntityManager for JPA operations
     */
    public RecurringEventDAOImpl(
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
     * user ownership → confirmation status → label filtering → time window → sorting.</p>
     * 
     * <h3>Query Optimization</h3>
     * <ul>
     *   <li>Eager fetches creator and label to prevent N+1 queries</li>
     *   <li>Uses indexed columns for efficient filtering</li>
     *   <li>Applies overlapping date logic for time window filtering</li>
     * </ul>
     * 
     * @param userId the ID of the user whose recurring events to retrieve
     * @param filter the filter criteria containing time window, label, and sorting options
     * @param pageNumber zero-based page number for pagination
     * @param pageSize maximum number of results per page
     * @return paginated list of confirmed recurring events matching all filter criteria
     */
    @Override
    public PagedList<RecurringEvent> findConfirmedRecurringEvents(Long userId, RecurringEventFilterDTO filter, int pageNumber, int pageSize) {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(filter, "filter cannot be null");
        
        logger.debug("Finding confirmed recurring events for user {} with filter: labelId={}, timeFilter={}, startDate={}, endDate={}, sortDescending={}, page={}, size={}",
                userId, filter.labelId(), filter.timeFilter(), filter.startDate(), filter.endDate(), filter.sortDescending(), pageNumber, pageSize);

        CriteriaBuilder<RecurringEvent> cb = createBaseQuery();

        applyUserFilter(userId, cb);
        applyOnlyConfirmedFilter(cb);
        applyLabelFilter(filter, cb);
        applyTimeWindowFilter(filter, cb);
        applySortOrder(filter, cb);

        PagedList<RecurringEvent> results = cb.page(pageNumber, pageSize).getResultList();
        
        logger.info("Retrieved {} confirmed recurring events for user {} (page {}/{}, total elements: {})",
                results.size(), userId, pageNumber + 1, results.getTotalPages(), results.getTotalSize());
        
        return results;
    }

    /**
     * Applies user ownership filtering to restrict results to the specified user's recurring events.
     * 
     * <p>This is a security-critical filter that ensures users can only access their own
     * recurring events. Uses indexed creator.id for efficient filtering.</p>
     * 
     * @param userId the ID of the user whose recurring events to retrieve
     * @param cb the CriteriaBuilder to apply the filter to
     */
    /**
     * Creates the base query with optimized eager fetching strategy.
     * 
     * <p>Establishes the foundation query with strategic eager fetching to prevent
     * N+1 query problems when accessing creator and label associations.</p>
     * 
     * @return configured CriteriaBuilder with base fetch strategy
     */
    private CriteriaBuilder<RecurringEvent> createBaseQuery() {
        return cbf.create(em, RecurringEvent.class)
                .fetch("creator")
                .fetch("label");
    }

    /**
     * Applies user ownership filtering to restrict results to the specified user's recurring events.
     * 
     * <p>This is a security-critical filter that ensures users can only access their own
     * recurring events. Uses indexed creator.id for efficient filtering.</p>
     * 
     * @param userId the ID of the user whose recurring events to retrieve
     * @param cb the CriteriaBuilder to apply the filter to
     */
    private void applyUserFilter(Long userId, CriteriaBuilder<RecurringEvent> cb) {
        logger.debug("Applying user filter for userId: {}", userId);
        cb.where("creator.id").eq(userId);
    }

    /**
     * Applies confirmation status filtering to exclude draft/unconfirmed recurring events.
     * 
     * <p>Only returns confirmed recurring events that are ready for instance generation
     * and user display. Draft events are excluded from all query results.</p>
     * 
     * @param cb the CriteriaBuilder to apply the filter to
     */
    private void applyOnlyConfirmedFilter(CriteriaBuilder<RecurringEvent> cb) {
        cb.where("unconfirmed").eq(false);
    }

    /**
     * Applies optional label-based filtering when a label ID is specified in the filter.
     * 
     * <p>When labelId is provided, restricts results to recurring events associated with
     * that specific label. Label ownership validation is handled at the service layer.</p>
     * 
     * @param filter the filter containing optional label ID criteria
     * @param cb the CriteriaBuilder to apply the filter to
     */
    private void applyLabelFilter(RecurringEventFilterDTO filter, CriteriaBuilder<RecurringEvent> cb) {
        if (filter.labelId() != null) {
            logger.debug("Applying label filter for labelId: {}", filter.labelId());
            cb.where("label.id").eq(filter.labelId());
        }
    }

    /**
     * Applies time window filtering using overlapping date range logic.
     * 
     * <p>Filters for recurring events whose date ranges overlap with the specified
     * time window. Uses standard interval overlap logic where two ranges [A,B] and [C,D]
     * overlap if A ≤ D and C ≤ B.</p>
     * 
     * <h3>Overlap Logic</h3>
     * <ul>
     *   <li>Recurring event starts before or on the filter end date</li>
     *   <li>Recurring event ends after or on the filter start date</li>
     * </ul>
     * 
     * <p><strong>Note:</strong> The service layer resolves TimeFilter enum values to actual
     * LocalDate boundaries before calling this method.</p>
     * 
     * @param filter the filter containing resolved start and end dates
     * @param cb the CriteriaBuilder to apply the filter to
     */
    private void applyTimeWindowFilter(RecurringEventFilterDTO filter, CriteriaBuilder<RecurringEvent> cb) {
        // Service layer has already resolved TimeFilter to actual dates
        LocalDate startDate = filter.startDate();
        LocalDate endDate = filter.endDate();
        
        logger.debug("Applying time window filter: startDate <= {} AND endDate >= {}", endDate, startDate);
        
        // Filter for recurring events that overlap with the time window
        cb.where("startDate").le(endDate);
        cb.where("endDate").ge(startDate);
    }

    /**
     * Applies sorting criteria based on the filter's sort direction preference.
     * 
     * <p>Implements a multi-level sort strategy for consistent, deterministic ordering:
     * <ol>
     *   <li>Primary: startDate (temporal ordering)</li>
     *   <li>Secondary: endDate (duration-based tiebreaker)</li>
     *   <li>Tertiary: id (stable sort for identical dates)</li>
     * </ol>
     * 
     * <p>The multi-level sort ensures consistent pagination behavior and prevents
     * duplicate results across page boundaries when multiple recurring events
     * share the same start date.</p>
     * 
     * @param filter the filter containing sort direction preference
     * @param cb the CriteriaBuilder to apply sorting to
     */
    private void applySortOrder(RecurringEventFilterDTO filter, CriteriaBuilder<RecurringEvent> cb) {
        boolean descending = Boolean.TRUE.equals(filter.sortDescending());
        logger.debug("Applying sort order: {} by startDate, endDate, id", descending ? "DESC" : "ASC");
        
        if (descending) {
            cb.orderByDesc("startDate")
                    .orderByDesc("endDate")
                    .orderByDesc("id");
        } else {
            cb.orderByAsc("startDate")
                    .orderByAsc("endDate")
                    .orderByAsc("id");
        }
    }
}
