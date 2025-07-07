package com.yohan.event_planner.util;

import com.yohan.event_planner.dto.RecapMediaResponseDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Assertion helper for comparing {@link RecapMediaResponseDTO} objects in tests.
 */
public class RecapMediaResponseDTOAssertions {

    public static void assertRecapMediaResponseDTOEquals(RecapMediaResponseDTO expected, RecapMediaResponseDTO actual) {
        assertNotNull(expected, "Expected RecapMediaResponseDTO should not be null");
        assertNotNull(actual, "Actual RecapMediaResponseDTO should not be null");

        assertEquals(expected.id(), actual.id(), "Media ID does not match");
        assertEquals(expected.mediaUrl(), actual.mediaUrl(), "Media URL does not match");
        assertEquals(expected.mediaType(), actual.mediaType(), "Media type does not match");
        assertEquals(expected.durationSeconds(), actual.durationSeconds(), "Media durationSeconds does not match");
        assertEquals(expected.mediaOrder(), actual.mediaOrder(), "Media order does not match");
    }

}
