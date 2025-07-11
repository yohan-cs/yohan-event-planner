package com.yohan.event_planner.config;

import com.yohan.event_planner.time.ClockProvider;
import com.yohan.event_planner.time.ClockProviderImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Configuration class for time-related components in the Event Planner application.
 *
 * <p>
 * This configuration provides centralized time management through dependency injection,
 * enabling consistent and testable time operations throughout the application. It is
 * particularly crucial for an event management system where precise timing, scheduling,
 * and temporal calculations are fundamental to core functionality.
 * </p>
 *
 * <h2>Design Benefits</h2>
 * <ul>
 *   <li><strong>Testability</strong>: Allows time mocking for deterministic unit tests</li>
 *   <li><strong>Consistency</strong>: Single source of truth for current time across the application</li>
 *   <li><strong>UTC Standardization</strong>: All internal time operations use UTC for precision</li>
 *   <li><strong>Dependency Injection</strong>: Clock can be easily replaced for testing scenarios</li>
 * </ul>
 *
 * <h2>Event Planner Time Strategy</h2>
 * <p>
 * The application follows a UTC-centric approach where all times are stored and processed
 * in UTC internally, with timezone conversion happening only at the presentation layer.
 * This ensures consistency across different user timezones and simplifies temporal calculations.
 * </p>
 *
 * <h3>Time Handling Layers</h3>
 * <ul>
 *   <li><strong>Storage Layer</strong>: All timestamps stored in UTC in the database</li>
 *   <li><strong>Business Layer</strong>: All calculations performed in UTC using this Clock</li>
 *   <li><strong>Presentation Layer</strong>: Timezone conversion for user display</li>
 * </ul>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li><strong>Event Scheduling</strong>: Determining current time for event creation</li>
 *   <li><strong>Recurring Events</strong>: Calculating next occurrence times</li>
 *   <li><strong>Audit Logging</strong>: Timestamping user actions and system events</li>
 *   <li><strong>Token Expiry</strong>: Managing JWT and refresh token lifetimes</li>
 *   <li><strong>Soft Deletion</strong>: Scheduling user account deletion dates</li>
 * </ul>
 *
 * <h2>Testing Support</h2>
 * <p>
 * The ClockProvider abstraction allows tests to inject fixed or controllable time sources,
 * enabling deterministic testing of time-sensitive business logic such as recurring event
 * generation, token expiry validation, and scheduled operations.
 * </p>
 *
 * @see com.yohan.event_planner.time.ClockProvider
 * @see com.yohan.event_planner.time.ClockProviderImpl
 * @see java.time.Clock
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
public class TimeConfig {

    /**
     * Creates a ClockProvider bean that wraps the system Clock.
     *
     * <p>
     * The ClockProvider serves as an abstraction layer over Java's Clock class,
     * providing a consistent interface for time operations throughout the application.
     * This abstraction is particularly valuable for testing, where deterministic
     * time values are essential for reliable test execution.
     * </p>
     *
     * <h3>ClockProvider Benefits</h3>
     * <ul>
     *   <li><strong>Test Mocking</strong>: Easy to mock for controlled time scenarios</li>
     *   <li><strong>Interface Consistency</strong>: Uniform time access across services</li>
     *   <li><strong>Future Extensions</strong>: Can add timing utilities and convenience methods</li>
     * </ul>
     *
     * <h3>Integration Example</h3>
     * <pre>{@code
     * @Service
     * public class EventService {
     *     private final ClockProvider clockProvider;
     *     
     *     public Event createEvent(EventCreateDTO dto) {
     *         Instant now = clockProvider.instant();
     *         // Use 'now' for created timestamp...
     *     }
     * }
     * }</pre>
     *
     * @param clock the system Clock instance to be wrapped
     * @return a ClockProvider implementation for application-wide time access
     * @see com.yohan.event_planner.time.ClockProvider
     */
    @Bean
    public ClockProvider clockProvider(Clock clock) {
        return new ClockProviderImpl(clock);
    }
    
    /**
     * Creates a system UTC Clock bean for precise time operations.
     *
     * <p>
     * This Clock instance provides the current time in UTC timezone, which serves
     * as the foundation for all temporal operations in the Event Planner application.
     * Using UTC ensures consistency across different server deployments and user
     * timezones, simplifying temporal calculations and database operations.
     * </p>
     *
     * <h3>UTC Choice Rationale</h3>
     * <ul>
     *   <li><strong>Global Consistency</strong>: No ambiguity across different deployment environments</li>
     *   <li><strong>Database Alignment</strong>: Matches UTC storage strategy in PostgreSQL</li>
     *   <li><strong>Calculation Simplicity</strong>: No DST or timezone offset complications</li>
     *   <li><strong>International Support</strong>: Works uniformly for users in any timezone</li>
     * </ul>
     *
     * <h3>Time Precision</h3>
     * <p>
     * The system clock provides nanosecond precision, which is more than sufficient
     * for event scheduling scenarios. The precision is automatically managed by the
     * underlying operating system and JVM implementation.
     * </p>
     *
     * <h3>Testing Override</h3>
     * <p>
     * In test configurations, this bean can be overridden with a fixed or controllable
     * Clock implementation to enable deterministic testing of time-sensitive operations.
     * </p>
     *
     * @return a Clock instance configured for UTC system time
     * @see java.time.Clock#systemUTC()
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
