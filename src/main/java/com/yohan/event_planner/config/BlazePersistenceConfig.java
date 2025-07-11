package com.yohan.event_planner.config;

import com.blazebit.persistence.Criteria;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.spi.CriteriaBuilderConfiguration;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for integrating Blaze-Persistence with the Spring Boot application.
 *
 * <p>
 * Blaze-Persistence is an advanced JPA extension that provides powerful querying capabilities,
 * including support for subquery operations, advanced projections, and complex filtering
 * that go beyond standard JPA Criteria API limitations. This configuration enables the
 * application to utilize these enhanced query features for complex event management operations.
 * </p>
 *
 * <h2>Key Features Enabled</h2>
 * <ul>
 *   <li><strong>Advanced Subqueries</strong>: Complex nested queries for event filtering</li>
 *   <li><strong>Efficient Projections</strong>: Custom result mapping for performance optimization</li>
 *   <li><strong>Window Functions</strong>: Advanced aggregation and ranking operations</li>
 *   <li><strong>CTE Support</strong>: Common Table Expressions for complex hierarchical queries</li>
 * </ul>
 *
 * <h2>Use Cases in Event Planner</h2>
 * <ul>
 *   <li><strong>Event Statistics</strong>: Complex aggregations across multiple time periods</li>
 *   <li><strong>User Analytics</strong>: Advanced reporting with nested data relationships</li>
 *   <li><strong>Calendar Queries</strong>: Efficient filtering of events with complex criteria</li>
 *   <li><strong>Badge Calculations</strong>: Complex scoring and achievement computations</li>
 * </ul>
 *
 * <h2>Integration Benefits</h2>
 * <ul>
 *   <li><strong>Performance</strong>: More efficient queries than standard JPA alternatives</li>
 *   <li><strong>Type Safety</strong>: Compile-time query validation and type checking</li>
 *   <li><strong>Maintainability</strong>: Cleaner query code compared to native SQL</li>
 *   <li><strong>Database Portability</strong>: Works across different database vendors</li>
 * </ul>
 *
 * @see com.blazebit.persistence.CriteriaBuilderFactory
 * @see com.blazebit.persistence.CriteriaBuilder
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
public class BlazePersistenceConfig {

    /**
     * The JPA EntityManagerFactory used to create the Blaze-Persistence CriteriaBuilderFactory.
     * This factory is automatically injected by Spring and provides the database connection
     * and entity metadata required for query construction.
     */
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    /**
     * Creates and configures the Blaze-Persistence CriteriaBuilderFactory bean.
     *
     * <p>
     * The CriteriaBuilderFactory is the main entry point for creating type-safe,
     * advanced queries using Blaze-Persistence. It extends the standard JPA
     * functionality with powerful features like subqueries, window functions,
     * and advanced projections.
     * </p>
     *
     * <h3>Configuration Details</h3>
     * <ul>
     *   <li><strong>Default Configuration</strong>: Uses Blaze-Persistence default settings</li>
     *   <li><strong>EntityManager Integration</strong>: Binds to the application's JPA setup</li>
     *   <li><strong>Automatic Discovery</strong>: Automatically discovers entity classes</li>
     *   <li><strong>Thread Safety</strong>: Factory is thread-safe and can be shared</li>
     * </ul>
     *
     * <h3>Usage Examples</h3>
     * <p>
     * The CriteriaBuilderFactory is primarily used in DAO implementations for complex
     * filtering operations with DTOs like EventFilterDTO and RecurringEventFilterDTO.
     * </p>
     * 
     * <h4>Event Filtering with EventFilterDTO</h4>
     * <pre>{@code
     * public PagedList<Event> findConfirmedEvents(Long userId, EventFilterDTO filter, int page, int size) {
     *     CriteriaBuilder<Event> cb = cbf.create(em, Event.class)
     *         .fetch("creator")
     *         .fetch("label");
     *     
     *     cb.where("creator.id").eq(userId);
     *     cb.where("unconfirmed").eq(false);
     *     cb.where("startTime").ge(filter.start());
     *     cb.where("endTime").le(filter.end());
     *     
     *     if (filter.labelId() != null) {
     *         cb.where("label.id").eq(filter.labelId());
     *     }
     *     
     *     return cb.page(page, size).getResultList();
     * }
     * }</pre>
     *
     * <h4>Recurring Event Filtering with RecurringEventFilterDTO</h4>
     * <pre>{@code
     * public PagedList<RecurringEvent> findRecurringEvents(Long userId, RecurringEventFilterDTO filter) {
     *     CriteriaBuilder<RecurringEvent> cb = cbf.create(em, RecurringEvent.class)
     *         .fetch("creator")
     *         .fetch("label");
     *     
     *     cb.where("creator.id").eq(userId);
     *     cb.where("startDate").le(filter.endDate());
     *     cb.where("endDate").ge(filter.startDate());
     *     
     *     return cb.orderByAsc("startDate").getResultList();
     * }
     * }</pre>
     *
     * @return a configured CriteriaBuilderFactory for creating advanced queries
     * @see com.blazebit.persistence.Criteria#getDefault()
     * @see com.blazebit.persistence.spi.CriteriaBuilderConfiguration#createCriteriaBuilderFactory(EntityManagerFactory)
     */
    @Bean
    public CriteriaBuilderFactory criteriaBuilderFactory() {
        CriteriaBuilderConfiguration config = Criteria.getDefault();
        return config.createCriteriaBuilderFactory(entityManagerFactory);
    }
}
