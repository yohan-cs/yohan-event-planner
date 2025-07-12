package com.yohan.event_planner.time;

import com.yohan.event_planner.domain.User;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Provides timezone-aware Clock instances for the Event Planner application.
 *
 * <p>This interface abstracts time operations throughout the application, enabling
 * consistent time handling and deterministic testing. It supports the application's
 * UTC-centric time strategy where all internal operations use UTC, with timezone
 * conversion happening only at presentation boundaries.</p>
 *
 * <h2>Design Philosophy</h2>
 * <ul>
 *   <li><strong>Testability</strong>: Enables clock mocking for deterministic unit tests</li>
 *   <li><strong>Consistency</strong>: Single abstraction for time across all layers</li>
 *   <li><strong>Timezone Support</strong>: Provides user-specific timezone handling</li>
 * </ul>
 *
 * <h2>Usage Patterns</h2>
 * <p>Services should inject this interface and use it for all time-related operations:</p>
 * <pre>{@code
 * @Service
 * public class EventService {
 *     private final ClockProvider clockProvider;
 *     
 *     public void processEvent(User user) {
 *         Clock userClock = clockProvider.getClockForUser(user);
 *         ZonedDateTime userTime = ZonedDateTime.now(userClock);
 *         // Process with user's timezone context...
 *     }
 * }
 * }</pre>
 *
 * @see ClockProviderImpl
 * @see com.yohan.event_planner.config.TimeConfig
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 1.0.0
 */
public interface ClockProvider {
    
    /**
     * Creates a Clock instance configured for the specified timezone.
     *
     * <p>This method provides timezone-aware clock instances while maintaining
     * the same underlying time source. The returned clock will show the same
     * instant as the base clock but in the specified timezone context.</p>
     *
     * @param zoneId the timezone to configure the clock for, must not be null
     * @return a Clock instance configured for the specified timezone
     * @throws NullPointerException if zoneId is null
     * @throws java.time.zone.ZoneRulesException if the zoneId is invalid
     */
    Clock getClockForZone(ZoneId zoneId);
    
    /**
     * Creates a Clock instance configured for the user's timezone.
     *
     * <p>Convenience method that extracts the timezone from the user's profile
     * and returns an appropriately configured clock. This is the primary method
     * for getting user-context-aware time operations.</p>
     *
     * <p>The user's timezone is obtained from {@link User#getTimezone()} and
     * must be a valid timezone identifier.</p>
     *
     * @param user the user whose timezone should be used, must not be null
     * @return a Clock instance configured for the user's timezone
     * @throws NullPointerException if user is null or user.getTimezone() is null
     * @throws java.time.zone.ZoneRulesException if the user's timezone is invalid
     */
    Clock getClockForUser(User user);
}