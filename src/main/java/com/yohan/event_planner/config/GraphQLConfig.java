package com.yohan.event_planner.config;

import com.yohan.event_planner.graphql.scalar.DateScalar;
import com.yohan.event_planner.graphql.scalar.DateTimeScalar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * Configuration class for GraphQL integration in the Event Planner application.
 *
 * <p>
 * This configuration sets up GraphQL alongside the existing REST API, providing
 * a flexible query interface for complex data requirements. It specifically configures
 * custom scalar types for proper date and time handling, which is crucial for an
 * event management system that deals extensively with temporal data.
 * </p>
 *
 * <h2>GraphQL Integration Benefits</h2>
 * <ul>
 *   <li><strong>Flexible Queries</strong>: Clients can request exactly the data they need</li>
 *   <li><strong>Single Endpoint</strong>: Reduces the number of API calls needed</li>
 *   <li><strong>Strong Typing</strong>: Schema-first approach with compile-time validation</li>
 *   <li><strong>Real-time Updates</strong>: Support for subscriptions (future enhancement)</li>
 * </ul>
 *
 * <h2>Custom Scalar Types</h2>
 * <p>
 * The application defines custom scalar types to handle Java's modern time API
 * properly within GraphQL queries. This ensures that date and time values are
 * serialized and deserialized correctly across the GraphQL interface.
 * </p>
 *
 * <h3>Supported Scalars</h3>
 * <ul>
 *   <li><strong>DateTime</strong>: Full timestamp with timezone information (ISO 8601)</li>
 *   <li><strong>Date</strong>: Date-only values without time component (ISO 8601)</li>
 * </ul>
 *
 * <h2>Use Cases in Event Planner</h2>
 * <ul>
 *   <li><strong>Complex User Queries</strong>: Fetch user profile with nested event data</li>
 *   <li><strong>Calendar Views</strong>: Retrieve events with specific field selections</li>
 *   <li><strong>Dashboard Data</strong>: Aggregate multiple data types in single request</li>
 *   <li><strong>Mobile Optimization</strong>: Minimize data transfer with precise queries</li>
 * </ul>
 *
 * <h2>Schema Location</h2>
 * <p>
 * The GraphQL schema is defined in {@code src/main/resources/graphql/schema.graphqls}
 * and includes type definitions for all major domain entities and their relationships.
 * </p>
 *
 * @see com.yohan.event_planner.graphql.scalar.DateTimeScalar
 * @see com.yohan.event_planner.graphql.scalar.DateScalar
 * @see org.springframework.graphql.execution.RuntimeWiringConfigurer
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
public class GraphQLConfig {

    /**
     * Configures the GraphQL runtime wiring with custom scalar types.
     *
     * <p>
     * The RuntimeWiringConfigurer allows us to extend the default GraphQL setup
     * with custom scalar types that properly handle Java's modern time API.
     * This is essential for an event management system where precise date and
     * time handling is critical.
     * </p>
     *
     * <h3>Scalar Type Registration</h3>
     * <ul>
     *   <li><strong>DATE_TIME</strong>: Maps to {@code LocalDateTime} and {@code ZonedDateTime}</li>
     *   <li><strong>DATE</strong>: Maps to {@code LocalDate}</li>
     * </ul>
     *
     * <h3>Error Handling</h3>
     * <p>
     * The custom scalars include validation to ensure that only valid date/time
     * strings are accepted. Invalid formats will result in GraphQL validation
     * errors before reaching the business logic layer.
     * </p>
     *
     * <h3>Format Support</h3>
     * <ul>
     *   <li><strong>DateTime Format</strong>: ISO 8601 with timezone (e.g., "2024-12-25T10:30:00Z")</li>
     *   <li><strong>Date Format</strong>: ISO 8601 date only (e.g., "2024-12-25")</li>
     * </ul>
     *
     * <h3>Example Usage in Schema</h3>
     * <pre>{@code
     * type Event {
     *   id: ID!
     *   title: String!
     *   startTime: DateTime!
     *   endTime: DateTime
     *   date: Date!
     * }
     * }</pre>
     *
     * @return a configured RuntimeWiringConfigurer with custom scalar types
     * @see com.yohan.event_planner.graphql.scalar.DateTimeScalar#DATE_TIME
     * @see com.yohan.event_planner.graphql.scalar.DateScalar#DATE
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(DateTimeScalar.DATE_TIME)
                .scalar(DateScalar.DATE);
    }
}
