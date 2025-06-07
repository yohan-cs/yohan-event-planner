package com.yohan.event_planner.business.handler;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class EventPatchHandlerTest {

    private EventPatchHandler eventPatchHandler;
    private User user;
    private Event baseEvent;

    @BeforeEach
    void setUp() {
        eventPatchHandler = new EventPatchHandler();
        user = TestUtils.createUserEntityWithId();
        baseEvent = TestUtils.createTimedEventEntityWithId(100L, user);
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
            ZonedDateTime newEnd = baseEvent.getEndTime().plusHours(1);
            EventUpdateDTO dto = new EventUpdateDTO(null, null, Optional.of(newEnd), Optional.empty());

            boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

            assertTrue(changed);
            assertEquals(newEnd, baseEvent.getEndTime());
        }

        @Test
        void testPatchEndTimeNoOp() {
            EventUpdateDTO dto = new EventUpdateDTO(null, null, Optional.ofNullable(baseEvent.getEndTime()), null);

            boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

            assertFalse(changed);
        }

        @Test
        void testPatchStartAndEndTimeSuccess() {
            ZonedDateTime newStart = baseEvent.getStartTime().plusDays(1);
            ZonedDateTime newEnd = baseEvent.getEndTime().plusDays(1);
            EventUpdateDTO dto = new EventUpdateDTO(null, newStart, Optional.of(newEnd), null);

            boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

            assertTrue(changed);
            assertEquals(newStart, baseEvent.getStartTime());
            assertEquals(newEnd, baseEvent.getEndTime());
        }

        @Test
        void testPatchStartAndEndTimeNoOp() {
            EventUpdateDTO dto = new EventUpdateDTO(
                    null,
                    baseEvent.getStartTime(),
                    Optional.ofNullable(baseEvent.getEndTime()),
                    null
            );

            boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

            assertFalse(changed);
        }

        @Test
        void testPatchDescriptionSuccess() {
            EventUpdateDTO dto = new EventUpdateDTO(null, null, null, Optional.of("Updated description"));

            boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

            assertTrue(changed);
            assertEquals("Updated description", baseEvent.getDescription());
        }

        @Test
        void testPatchDescriptionNoOp() {
            EventUpdateDTO dto = new EventUpdateDTO(null, null, null, Optional.ofNullable(baseEvent.getDescription()));

            boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

            assertFalse(changed);
        }

    }

    @Test
    void testPatchFromUntimedToTimed_setsEndTimeAndDuration() {
        // Arrange
        baseEvent = TestUtils.createUntimedEventEntityWithId(200L, user);
        ZonedDateTime newEnd = baseEvent.getStartTime().plusHours(2);
        EventUpdateDTO dto = new EventUpdateDTO(null, null, Optional.of(newEnd), Optional.empty());

        // Act
        boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

        // Assert
        assertTrue(changed);
        assertEquals(newEnd, baseEvent.getEndTime());
        assertEquals(120, baseEvent.getDurationMinutes());
    }

    @Test
    void testPatchFromTimedToUntimed_removesEndTimeAndDuration() {
        // Arrange
        baseEvent = TestUtils.createTimedEventEntityWithId(201L, user);
        EventUpdateDTO dto = new EventUpdateDTO(null, null, Optional.empty(), Optional.empty());

        // Act
        boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

        // Assert
        assertTrue(changed);
        assertNull(baseEvent.getEndTime());
        assertNull(baseEvent.getDurationMinutes());
    }

    @Test
    void testPatchFromUntimedToUntimedNoOp_returnsFalse() {
        // Arrange
        baseEvent = TestUtils.createUntimedEventEntityWithId(202L, user);
        EventUpdateDTO dto = new EventUpdateDTO(null, null, Optional.empty(), Optional.empty());

        // Act
        boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

        // Assert
        assertFalse(changed);
        assertNull(baseEvent.getEndTime());
        assertNull(baseEvent.getDurationMinutes());
    }

    @Test
    void testPatchStartTimeOnUntimedEvent_success() {
        // Arrange
        baseEvent = TestUtils.createUntimedEventEntityWithId(203L, user);
        ZonedDateTime newStart = baseEvent.getStartTime().plusDays(1);
        EventUpdateDTO dto = new EventUpdateDTO(null, newStart, Optional.empty(), Optional.empty());

        // Act
        boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

        // Assert
        assertTrue(changed);
        assertEquals(newStart, baseEvent.getStartTime());
        assertNull(baseEvent.getEndTime());
        assertNull(baseEvent.getDurationMinutes());
    }

    @Test
    void testPatchStartTimeOnUntimedEvent_noOp() {
        // Arrange
        baseEvent = TestUtils.createUntimedEventEntityWithId(204L, user);
        EventUpdateDTO dto = new EventUpdateDTO(null, baseEvent.getStartTime(), Optional.empty(), Optional.empty());

        // Act
        boolean changed = eventPatchHandler.applyPatch(baseEvent, dto, user);

        // Assert
        assertFalse(changed);
        assertEquals(baseEvent.getStartTime(), baseEvent.getStartTime());
        assertNull(baseEvent.getEndTime());
        assertNull(baseEvent.getDurationMinutes());
    }

}
