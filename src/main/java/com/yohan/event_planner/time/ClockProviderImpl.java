package com.yohan.event_planner.time;

import com.yohan.event_planner.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Default implementation of {@link ClockProvider} that wraps a base Clock instance.
 *
 * <p>This implementation provides timezone-aware clock instances by wrapping a base
 * Clock (typically UTC) and creating derived clocks for specific timezones. The
 * underlying time source remains consistent across all derived clocks.</p>
 *
 * <h2>Implementation Strategy</h2>
 * <p>Uses Java's {@link Clock#withZone(ZoneId)} method to create timezone-specific
 * views of the base clock. This ensures:</p>
 * <ul>
 *   <li>Consistent underlying time source across all operations</li>
 *   <li>Efficient clock creation without additional system calls</li>
 *   <li>Proper timezone offset calculations handled by Java Time API</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This implementation is thread-safe as it only reads from the immutable base
 * clock and creates new Clock instances without maintaining mutable state.</p>
 *
 * @see ClockProvider
 * @see java.time.Clock
 * @author Event Planner Development Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class ClockProviderImpl implements ClockProvider {

    private static final Logger logger = LoggerFactory.getLogger(ClockProviderImpl.class);

    /**
     * The base clock used as the time source for all derived clocks.
     * Typically configured as UTC system clock in production.
     */
    private final Clock baseClock;

    /**
     * Constructs a ClockProviderImpl with the specified base clock.
     *
     * <p>The base clock serves as the time source for all timezone-specific
     * clocks created by this provider. In production, this is typically
     * a UTC system clock configured in {@link com.yohan.event_planner.config.TimeConfig}.</p>
     *
     * @param baseClock the base clock to use as the time source, must not be null
     * @throws NullPointerException if baseClock is null
     */
    public ClockProviderImpl(Clock baseClock) {
        this.baseClock = Objects.requireNonNull(baseClock, "Base clock cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new Clock instance that shows the same instant as the base clock
     * but in the specified timezone. The conversion is handled efficiently by the
     * Java Time API without additional system calls.</p>
     */
    @Override
    public Clock getClockForZone(ZoneId zoneId) {
        logger.debug("Creating clock for timezone: {}", zoneId);
        return baseClock.withZone(zoneId);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the timezone from the user's profile and delegates to
     * {@link #getClockForZone(ZoneId)}. The user's timezone must be a valid
     * timezone identifier as stored in the database.</p>
     */
    @Override
    public Clock getClockForUser(User user) {
        logger.debug("Creating clock for user: {} with timezone: {}", 
                    user.getUsername(), user.getTimezone());
        ZoneId userZone = ZoneId.of(user.getTimezone());
        return baseClock.withZone(userZone);
    }
}