package com.yohan.event_planner.business.handler;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class EventPatchHandlerTest {

    private EventPatchHandler eventPatchHandler;
    private User user;
    private Event baseEvent;

    @BeforeEach
    void setUp() {
        eventPatchHandler = new EventPatchHandler();
        user = TestUtils.createUserEntityWithId();
        baseEvent = TestUtils.createEventEntityWithId(100L, user);
    }

    @Nested
    class ApplyPatchTests {

        @Test
        void testPatchNameSuccess() {
            // Arrange
            EventUpdateDTO dto = new EventUpdateDTO("New Name", null, null, null);

            // Act
            boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

            // Assert
            assertTrue(changed);
            assertEquals("New Name", baseEvent.getName());
        }

        @Test
        void testPatchNameNoOp() {
            // Arrange
            EventUpdateDTO dto = new EventUpdateDTO(baseEvent.getName(), null, null, null);

            // Act
            boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

            // Assert
            assertFalse(changed);
            assertEquals(baseEvent.getName(), baseEvent.getName());
        }

        @Test
        void testPatchStartTimeSuccess() {
            // Arrange
            ZonedDateTime newStart = baseEvent.getStartTime().plusHours(1);
            EventUpdateDTO dto = new EventUpdateDTO(null, newStart, null, null);

            // Act
            boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

            // Assert
            assertTrue(changed);
            assertEquals(newStart, baseEvent.getStartTime());
        }

        @Test
        void testPatchStartTimeNoOp() {
            // Arrange
            EventUpdateDTO dto = new EventUpdateDTO(null, baseEvent.getStartTime(), null, null);

            // Act
            boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

            // Assert
            assertFalse(changed);
        }

        @Test
        void testPatchEndTimeSuccess() {
            // Arrange
            ZonedDateTime newEnd = baseEvent.getEndTime().plusHours(1);
            EventUpdateDTO dto = new EventUpdateDTO(null, null, newEnd, null);

            // Act
            boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

            // Assert
            assertTrue(changed);
            assertEquals(newEnd, baseEvent.getEndTime());
        }

        @Test
        void testPatchEndTimeNoOp() {
            // Arrange
            EventUpdateDTO dto = new EventUpdateDTO(null, null, baseEvent.getEndTime(), null);

            // Act
            boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

            // Assert
            assertFalse(changed);
        }

        @Test
        void testPatchStartAndEndTimeSuccess() {
            // Arrange
            ZonedDateTime newStart = baseEvent.getStartTime().plusDays(1);
            ZonedDateTime newEnd = baseEvent.getEndTime().plusDays(1);
            EventUpdateDTO dto = new EventUpdateDTO(null, newStart, newEnd, null);

            // Act
            boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

            // Assert
            assertTrue(changed);
            assertEquals(newStart, baseEvent.getStartTime());
            assertEquals(newEnd, baseEvent.getEndTime());
        }

        @Test
        void testPatchStartAndEndTimeNoOp() {
            // Arrange
            EventUpdateDTO dto = new EventUpdateDTO(null, baseEvent.getStartTime(), baseEvent.getEndTime(), null);

            // Act
            boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

            // Assert
            assertFalse(changed);
        }

        @Test
        void testPatchDescriptionSuccess() {
            // Arrange
            EventUpdateDTO dto = new EventUpdateDTO(null, null, null, "Updated description");

            // Act
            boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

            // Assert
            assertTrue(changed);
            assertEquals("Updated description", baseEvent.getDescription());
        }

        @Test
        void testPatchDescriptionNoOp() {
            // Arrange
            EventUpdateDTO dto = new EventUpdateDTO(null, null, null, baseEvent.getDescription());

            // Act
            boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

            // Assert
            assertFalse(changed);
        }

    }
}
