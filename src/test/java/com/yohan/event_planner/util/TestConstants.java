package com.yohan.event_planner.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class TestConstants {

    // ========== USER CONSTANTS ==========
    public static final String VALID_USERNAME = "yungbuck";
    public static final String VALID_PASSWORD = "BuckusIsDope42!";
    public static final String VALID_EMAIL = "yungbuck@email.com";
    public static final String VALID_FIRST_NAME = "Bug";
    public static final String VALID_LAST_NAME = "Woong";
    public static final String VALID_TIMEZONE = "America/New_York";

    public static final Long USER_ID = 1L;
    public static final Long USER_ID_OTHER = 2L;

    // ========== EVENT CONSTANTS ==========
    public static final Long EVENT_ID = 100L;
    public static final String VALID_EVENT_TITLE = "Walk Acorn";
    public static final int VALID_EVENT_DURATION_MINUTES = 60;
    public static final String VALID_EVENT_DESCRIPTION = "Make sure he doesn't overheat.";

    // ========== RECURRING EVENT CONSTANTS ==========
    public static final Long VALID_RECURRING_EVENT_ID = 500L;
    public static final String VALID_WEEKLY_RECURRENCE_RULE = "WEEKLY:MONDAY,WEDNESDAY,FRIDAY";
    public static final String VALID_MONTHLY_RECURRENCE_RULE = "MONTHLY:2:TUESDAY,THURSDAY";

    // ========== EVENT RECAP CONSTANTS ==========
    public static final Long EVENT_RECAP_ID = 101L;
    public static final String VALID_RECAP_NAME = "Jiu Jitsu Gi Training";
    public static final String VALID_RECAP_NOTES = "Felt productive and refreshed after the walk.";

    // ========== LABEL CONSTANTS ==========
    public static final String VALID_LABEL_NAME = "Running";
    public static final Long VALID_LABEL_ID = 10L;
    public static final Long FUTURE_LABEL_ID = 106L;
    public static final Long INCOMPLETE_LABEL_ID = 107L;
    public static final Long COMPLETED_LABEL_ID = 105L;
    public static final Long UNLABELED_LABEL_ID = 108L;
    public static final Long UNFILLED_DRAFT_LABEL_ID = 102L;
    public static final Long PARTIAL_DRAFT_LABEL_ID = 103L;
    public static final Long FULL_DRAFT_LABEL_ID = 109L;
    public static final Long IMPROMPTU_LABEL_ID = 104L;

    // ========== BADGE CONSTANTS ==========
    public static final Long VALID_BADGE_ID = 301L;
    public static final Long OTHER_BADGE_ID = 302L;
    public static final String VALID_BADGE_NAME = "Consistency";
    public static final String VALID_BADGE_NAME_OTHER = "Balance";
    public static final String VALID_BADGE_NAME_THIRD = "Strength";

    // ========== SECURITY CONSTANTS ==========
    public static final String BASE64_TEST_SECRET = "b2RlcmVuY29kZWRzZWNyZXRzaG91bGRiZWxvbmdpbnN0cmluZw==";

    // ========== UTILITY METHODS ==========

    /**
     * Gets the user's timezone as a ZoneId.
     */
    public static ZoneId getUserZone() {
        return ZoneId.of(VALID_TIMEZONE);
    }

    /**
     * Gets a future event start time (+5 hours from clock time), truncated to minute precision.
     */
    public static ZonedDateTime getValidEventStartFuture(Clock clock) {
        return ZonedDateTime.now(clock).plusHours(5).truncatedTo(ChronoUnit.MINUTES);
    }

    /**
     * Gets a future event end time (+2 hours from future start time), truncated to minute precision.
     */
    public static ZonedDateTime getValidEventEndFuture(Clock clock) {
        return getValidEventStartFuture(clock).plusHours(2).truncatedTo(ChronoUnit.MINUTES);
    }

    /**
     * Gets a past event start time (-72 hours from clock time), truncated to minute precision.
     */
    public static ZonedDateTime getValidEventStartPast(Clock clock) {
        return ZonedDateTime.now(clock).minusHours(72).truncatedTo(ChronoUnit.MINUTES);
    }

    /**
     * Gets a past event end time (+2 hours from past start time), truncated to minute precision.
     */
    public static ZonedDateTime getValidEventEndPast(Clock clock) {
        return getValidEventStartPast(clock).plusHours(2).truncatedTo(ChronoUnit.MINUTES);
    }

    /**
     * Gets a valid event start date (same day as clock time).
     */
    public static LocalDate getValidEventStartDate(Clock clock) {
        return ZonedDateTime.now(clock).toLocalDate();
    }

    /**
     * Gets a valid event end date (+30 days from start date).
     */
    public static LocalDate getValidEventEndDate(Clock clock) {
        return getValidEventStartDate(clock).plusDays(30);
    }

    /**
     * Gets the current UTC time from the fixed clock, truncated to minute precision.
     */
    public static ZonedDateTime getFixedNowUtc(Clock clock) {
        return ZonedDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
    }

    /**
     * Gets today's date in the user's timezone from the fixed clock.
     */
    public static LocalDate getFixedTodayUserZone(Clock clock) {
        return getFixedNowUtc(clock).withZoneSameInstant(getUserZone()).toLocalDate();
    }

    private TestConstants() {
        // Prevent instantiation
    }
}
