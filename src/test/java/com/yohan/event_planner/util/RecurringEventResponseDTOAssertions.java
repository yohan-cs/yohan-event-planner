package com.yohan.event_planner.util;

import com.yohan.event_planner.dto.RecurringEventResponseDTO;
import org.junit.jupiter.api.Assertions;

public class RecurringEventResponseDTOAssertions {

    /**
     * Asserts that two RecurringEventResponseDTO objects are equal.
     * Compares all fields of the RecurringEventResponseDTO to ensure they are the same.
     *
     * @param actual   the actual RecurringEventResponseDTO
     * @param expected the expected RecurringEventResponseDTO
     */
    public static void assertRecurringEventResponseDTOEquals(RecurringEventResponseDTO actual, RecurringEventResponseDTO expected) {
        Assertions.assertEquals(expected.id(), actual.id(), "Event ID should be equal");
        Assertions.assertEquals(expected.name(), actual.name(), "Event name should be equal");
        Assertions.assertEquals(expected.startTime(), actual.startTime(), "Start time should be equal");
        Assertions.assertEquals(expected.endTime(), actual.endTime(), "End time should be equal");
        Assertions.assertEquals(expected.startDate(), actual.startDate(), "Start date should be equal");
        Assertions.assertEquals(expected.endDate(), actual.endDate(), "End date should be equal");
        Assertions.assertEquals(expected.description(), actual.description(), "Description should be equal");
        Assertions.assertEquals(expected.label(), actual.label(), "Label should be equal");
        Assertions.assertEquals(expected.recurrenceSummary(), actual.recurrenceSummary(), "Recurrence summary should be equal");
        Assertions.assertEquals(expected.creatorUsername(), actual.creatorUsername(), "Creator username should be equal");
        Assertions.assertEquals(expected.unconfirmed(), actual.unconfirmed(), "Unconfirmed status should be equal");
    }
}
