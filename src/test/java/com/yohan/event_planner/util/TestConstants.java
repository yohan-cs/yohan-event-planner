package com.yohan.event_planner.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TestConstants {

    public static final String VALID_USERNAME = "yungbuck";
    public static final String VALID_PASSWORD = "BuckusIsDope123!";
    public static final String VALID_EMAIL = "yungbuck@email.com";
    public static final String VALID_FIRST_NAME = "Bug";
    public static final String VALID_LAST_NAME = "Woong";
    public static final String VALID_TIMEZONE = "America/New_York";

    public static final Long USER_ID = 1L;

    public static final Long EVENT_ID = 100L;
    public static final String VALID_EVENT_TITLE = "Walk Acorn";
    public static final int VALID_EVENT_DURATION_MINUTES = 60;
    public static final String VALID_EVENT_DESCRIPTION = "Make sure he doesn't overheat.";

    public static final ZonedDateTime VALID_EVENT_START =
            ZonedDateTime.of(2024, 1, 1, 7, 0, 0, 0, ZoneId.of("UTC"));

    public static final ZonedDateTime VALID_EVENT_END =
            ZonedDateTime.of(2024, 1, 1, 8, 0, 0, 0, ZoneId.of("UTC"));

    public static final String BASE64_TEST_SECRET = "b2RlcmVuY29kZWRzZWNyZXRzaG91bGRiZWxvbmdpbnN0cmluZw==";

    private TestConstants() {
        // Prevent instantiation
    }
}
