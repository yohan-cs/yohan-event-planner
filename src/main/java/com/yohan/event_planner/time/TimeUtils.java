package com.yohan.event_planner.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public final class TimeUtils {

    public static final ZonedDateTime FAR_PAST = Instant.EPOCH.atZone(ZoneOffset.UTC);

    public static final ZonedDateTime FAR_FUTURE = ZonedDateTime.of(5000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    public static final LocalDate FAR_PAST_DATE = FAR_PAST.toLocalDate();

    public static final LocalDate FAR_FUTURE_DATE = LocalDate.of(5000, 1, 1);

    private TimeUtils() {
        // Prevent instantiation
    }
}
