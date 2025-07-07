package com.yohan.event_planner.util;

import com.yohan.event_planner.dto.EventRecapResponseDTO;
import com.yohan.event_planner.dto.RecapMediaResponseDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Assertion helper for comparing {@link EventRecapResponseDTO} objects in tests.
 */
public class EventRecapResponseDTOAssertions {

    public static void assertEventRecapResponseDTOEquals(EventRecapResponseDTO expected, EventRecapResponseDTO actual) {
        assertNotNull(expected);
        assertNotNull(actual);

        assertEquals(expected.id(), actual.id(), "Recap ID does not match");
        assertEquals(expected.eventName(), actual.eventName(), "Event name does not match");
        assertEquals(expected.username(), actual.username(), "Username does not match");
        assertEquals(expected.date().toInstant(), actual.date().toInstant(), "Event start date does not match");
        assertEquals(expected.durationMinutes(), actual.durationMinutes(), "Duration minutes do not match");
        assertEquals(expected.labelName(), actual.labelName(), "Label name does not match");
        assertEquals(expected.notes(), actual.notes(), "Recap notes do not match");

        assertEquals(expected.media().size(), actual.media().size(), "Number of media items does not match");

        for (int i = 0; i < expected.media().size(); i++) {
            RecapMediaResponseDTO expectedMedia = expected.media().get(i);
            RecapMediaResponseDTO actualMedia = actual.media().get(i);

            assertEquals(expectedMedia.id(), actualMedia.id(), "Media ID does not match at index " + i);
            assertEquals(expectedMedia.mediaUrl(), actualMedia.mediaUrl(), "Media URL does not match at index " + i);
            assertEquals(expectedMedia.mediaType(), actualMedia.mediaType(), "Media type does not match at index " + i);
            assertEquals(expectedMedia.durationSeconds(), actualMedia.durationSeconds(), "Media duration does not match at index " + i);
            assertEquals(expectedMedia.mediaOrder(), actualMedia.mediaOrder(), "Media order does not match at index " + i);
        }
    }
}
