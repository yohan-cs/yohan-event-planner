package com.yohan.event_planner.domain.enums;

/**
 * Enum representing predefined time-based filters for event queries.
 *
 * <p>This enum defines high-level time window behavior for querying events.
 * Clients may optionally supply {@code start} and {@code end} when using {@link #CUSTOM}.
 * </p>
 */
public enum TimeFilter {

    /**
     * Include all events, regardless of time.
     * <p>{@code start} and {@code end} are ignored.</p>
     */
    ALL,

    /**
     * Include only past events.
     * <p>{@code start} and {@code end} are ignored; time range is set from FAR_PAST to now.</p>
     */
    PAST_ONLY,

    /**
     * Include only future events.
     * <p>{@code start} and {@code end} are ignored; time range is set from now to FAR_FUTURE.</p>
     */
    FUTURE_ONLY,

    /**
     * Use a custom time window based on {@code start} and {@code end}.
     * <p>If {@code start} is null, it defaults to FAR_PAST. If {@code end} is null, it defaults to FAR_FUTURE.</p>
     */
    CUSTOM
}
