package com.yohan.event_planner.business.handler;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.exception.InvalidEventStateException;
import com.yohan.event_planner.exception.LabelOwnershipException;
import com.yohan.event_planner.security.OwnershipValidator;
import com.yohan.event_planner.service.LabelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static com.yohan.event_planner.exception.ErrorCode.EVENT_NOT_CONFIRMED;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_END_TIME;
import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_NAME;
import static com.yohan.event_planner.exception.ErrorCode.UNAUTHORIZED_LABEL_ACCESS;
import static com.yohan.event_planner.util.TestConstants.EVENT_ID;
import static com.yohan.event_planner.util.TestConstants.FUTURE_LABEL_ID;
import static com.yohan.event_planner.util.TestConstants.UNLABELED_LABEL_ID;
import static com.yohan.event_planner.util.TestConstants.VALID_EVENT_TITLE;
import static com.yohan.event_planner.util.TestConstants.VALID_LABEL_ID;
import static com.yohan.event_planner.util.TestConstants.getValidEventEndFuture;
import static com.yohan.event_planner.util.TestConstants.getValidEventStartFuture;
import static com.yohan.event_planner.util.TestUtils.createEmptyDraftEvent;
import static com.yohan.event_planner.util.TestUtils.createValidLabelWithId;
import static com.yohan.event_planner.util.TestUtils.createValidScheduledEventWithId;
import static com.yohan.event_planner.util.TestUtils.createValidUserEntityWithId;
import static com.yohan.event_planner.util.TestUtils.setUnlabeledLabel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


/**
 * Comprehensive test suite for {@link EventPatchHandler} focusing on:
 * 
 * <h3>Skip vs Clear Semantics</h3>
 * <p>Tests the sophisticated Optional field handling where:
 * <ul>
 *   <li><strong>null</strong> = skip field (no change)</li>
 *   <li><strong>Optional.empty()</strong> = clear field (set to null)</li>
 *   <li><strong>Optional.of(value)</strong> = update field (set to value)</li>
 * </ul>
 * 
 * <h3>Field-Specific Behavior</h3>
 * <p>Tests individual field patching logic including:
 * <ul>
 *   <li>Name, start time, end time, description patching</li>
 *   <li>Label patching with "Unlabeled" resolution</li>
 *   <li>Completion validation and state transitions</li>
 * </ul>
 * 
 * <h3>Multi-Field Scenarios</h3>
 * <p>Tests complex scenarios with multiple field updates including:
 * <ul>
 *   <li>Partial update failures and rollback behavior</li>
 *   <li>Service interaction failures during patching</li>
 *   <li>Ownership validation failures</li>
 *   <li>Complete event transformations</li>
 * </ul>
 * 
 * <h3>Edge Cases</h3>
 * <p>Tests boundary conditions and error scenarios:
 * <ul>
 *   <li>Completing events with missing required fields</li>
 *   <li>Draft event completion restrictions</li>
 *   <li>Return value accuracy for change detection</li>
 * </ul>
 */
public class EventPatchHandlerTest {

    private LabelService labelService;
    private OwnershipValidator ownershipValidator;
    private EventPatchHandler eventPatchHandler;
    private User user;
    private Event baseEvent;
    private Clock clock;

    @BeforeEach
    void setUp() {
        user = createValidUserEntityWithId();

        ZoneId userZone = ZoneId.of(user.getTimezone());

        clock = mock(Clock.class);
        ZonedDateTime fixedTime = ZonedDateTime.now(userZone);


        when(clock.instant()).thenReturn(fixedTime.toInstant());
        when(clock.getZone()).thenReturn(userZone);

        labelService = mock(LabelService.class);
        ownershipValidator = mock(OwnershipValidator.class);
        eventPatchHandler = new EventPatchHandler(labelService, ownershipValidator);

        Label unlabeled = createValidLabelWithId(UNLABELED_LABEL_ID, user);
        setUnlabeledLabel(user, unlabeled);

        baseEvent = createValidScheduledEventWithId(EVENT_ID, user, clock);
        baseEvent.setLabel(createValidLabelWithId(VALID_LABEL_ID, user));
    }

    @Nested
    class ApplyPatchTests {

        // region --- Name Patching ---

        @Test
        void testPatchName_successfulUpdate() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            event.setLabel(createValidLabelWithId(VALID_LABEL_ID, user));
            EventUpdateDTO dto = new EventUpdateDTO(
                    Optional.of("Updated Event Name"),
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Act
            boolean result = eventPatchHandler.applyPatch(event, dto);

            // Assert
            assertTrue(result);
            assertEquals("Updated Event Name", event.getName());
            assertFalse(event.isUnconfirmed());
        }

        @Test
        void testPatchName_noChange() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            event.setLabel(createValidLabelWithId(VALID_LABEL_ID, user));
            EventUpdateDTO dto = new EventUpdateDTO(
                    Optional.of(event.getName()),
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Act
            boolean result = eventPatchHandler.applyPatch(event, dto);

            // Assert
            assertFalse(result);
            assertEquals(VALID_EVENT_TITLE, event.getName());
            assertFalse(event.isUnconfirmed());
        }

        // endregion

        // region --- Start Time Patching ---

        @Test
        void testPatchStartTime_successfulUpdate() {
            // Arrange
            ZonedDateTime baseStart = getValidEventStartFuture(clock);
            ZonedDateTime newStart = baseStart.plusHours(1);
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            EventUpdateDTO dto = new EventUpdateDTO(
                    null,
                    Optional.of(newStart),
                    null,
                    Optional.empty(),
                    Optional.empty(),
                    null
            );

            // Act
            boolean result = eventPatchHandler.applyPatch(event, dto);

            // Assert
            assertTrue(result);
            assertEquals(newStart.toInstant(), event.getStartTime().toInstant());
            assertFalse(event.isUnconfirmed());
        }

        @Test
        void testPatchStartTime_noChange() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            ZonedDateTime expectedStart = getValidEventStartFuture(clock); // from TestConstants
            EventUpdateDTO dto = new EventUpdateDTO(
                    null,
                    Optional.of(event.getStartTime()), // same start time
                    null,                              // preserve end time
                    null,                              // preserve description
                    null,                              // preserve label
                    null                               // preserve completion status
            );

            // Act
            boolean result = eventPatchHandler.applyPatch(event, dto);

            // Assert
            assertFalse(result);
            assertEquals(expectedStart.toInstant(), event.getStartTime().toInstant());
            assertFalse(event.isUnconfirmed());
        }


        // endregion

        // region --- End Time Patching ---

        @Test
        void testPatchEndTime_successfulUpdate() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            ZonedDateTime newEndTime = getValidEventEndFuture(clock).plusHours(1); // dynamic now
            EventUpdateDTO dto = new EventUpdateDTO(
                    null,
                    null,
                    Optional.of(newEndTime), // updated end time
                    null,
                    null,
                    null
            );

            // Act
            boolean result = eventPatchHandler.applyPatch(event, dto);

            // Assert
            assertTrue(result);
            assertEquals(newEndTime.toInstant(), event.getEndTime().toInstant());
            assertFalse(event.isUnconfirmed());
        }

        @Test
        void testPatchEndTime_noChange() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            ZonedDateTime expectedEnd = getValidEventEndFuture(clock);
            EventUpdateDTO dto = new EventUpdateDTO(
                    null,
                    null,
                    Optional.of(event.getEndTime()), // same end time
                    null,
                    null,
                    null
            );

            // Act
            boolean result = eventPatchHandler.applyPatch(event, dto);

            // Assert
            assertFalse(result);
            assertEquals(expectedEnd.toInstant(), event.getEndTime().toInstant());
            assertFalse(event.isUnconfirmed());
        }

        // endregion

        // region --- Description Patching ---

        @Test
        void testPatchDescription_successfulUpdate() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            event.setLabel(createValidLabelWithId(VALID_LABEL_ID, user));
            EventUpdateDTO dto = new EventUpdateDTO(
                    null,
                    null,
                    null,
                    Optional.of("Updated event description"),
                    null,
                    null
            );

            // Act
            boolean result = eventPatchHandler.applyPatch(event, dto);

            // Assert
            assertTrue(result);
            assertEquals("Updated event description", event.getDescription());
            assertFalse(event.isUnconfirmed());
        }

        @Test
        void testPatchDescription_noChange() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            event.setLabel(createValidLabelWithId(VALID_LABEL_ID, user));
            String existingDescription = event.getDescription();
            EventUpdateDTO dto = new EventUpdateDTO(
                    null,
                    null,
                    null,
                    Optional.of(existingDescription), // same description
                    null,
                    null
            );

            // Act
            boolean result = eventPatchHandler.applyPatch(event, dto);

            // Assert
            assertFalse(result);
            assertEquals(existingDescription, event.getDescription());
            assertFalse(event.isUnconfirmed());
        }

        @Test
        void testPatchDescription_clearDescription() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            event.setLabel(createValidLabelWithId(VALID_LABEL_ID, user));
            event.setDescription("Some description");
            EventUpdateDTO dto = new EventUpdateDTO(
                    null,
                    null,
                    null,
                    Optional.ofNullable(null), // clear description
                    null,
                    null
            );

            // Act
            boolean result = eventPatchHandler.applyPatch(event, dto);

            // Assert
            assertTrue(result);
            assertNull(event.getDescription());
            assertFalse(event.isUnconfirmed()); // not a required field
        }

        // endregion

        // region --- Label Patching ---

        @Test
        void testPatchLabel_successfulUpdate() {
            // Arrange
            Label originalLabel = createValidLabelWithId(VALID_LABEL_ID, user);
            Label newLabel = createValidLabelWithId(FUTURE_LABEL_ID, user);
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            event.setLabel(originalLabel);

            when(labelService.getLabelEntityById(FUTURE_LABEL_ID)).thenReturn(newLabel);

            EventUpdateDTO dto = new EventUpdateDTO(
                    null, null, null, null,
                    Optional.of(newLabel.getId()), // update to new label
                    null
            );

            // Act
            boolean result = eventPatchHandler.applyPatch(event, dto);

            // Assert
            assertTrue(result);
            assertEquals(FUTURE_LABEL_ID, event.getLabel().getId());
            assertFalse(event.isUnconfirmed());
            verify(ownershipValidator).validateLabelOwnership(user.getId(), newLabel);
        }

        @Test
        void testPatchLabel_noChange() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            Label currentLabel = createValidLabelWithId(VALID_LABEL_ID, user);
            event.setLabel(currentLabel);

            EventUpdateDTO dto = new EventUpdateDTO(
                    null, null, null, null,
                    Optional.of(VALID_LABEL_ID), // same label as before
                    null
            );

            // Act
            boolean result = eventPatchHandler.applyPatch(event, dto);

            // Assert
            assertFalse(result);
            assertEquals(VALID_LABEL_ID, event.getLabel().getId());
            assertFalse(event.isUnconfirmed());
            verifyNoInteractions(ownershipValidator);
        }

        @Test
        void testPatchLabel_clearLabel_setsToUnlabeled() {
            // Arrange
            Label originalLabel = createValidLabelWithId(VALID_LABEL_ID, user);
            Label unlabeled = createValidLabelWithId(UNLABELED_LABEL_ID, user);
            setUnlabeledLabel(user, unlabeled);

            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            event.setLabel(originalLabel);

            when(labelService.getLabelEntityById(UNLABELED_LABEL_ID)).thenReturn(unlabeled);

            EventUpdateDTO dto = new EventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    Optional.ofNullable(null), // clear label
                    null
            );

            // Act
            boolean result = eventPatchHandler.applyPatch(event, dto);

            // Assert
            assertTrue(result);
            assertEquals(UNLABELED_LABEL_ID, event.getLabel().getId());
            assertFalse(event.isUnconfirmed());
            verify(ownershipValidator).validateLabelOwnership(user.getId(), unlabeled);
        }

        @Test
        void testPatchLabel_invalidOwnership_throwsException() {
            // Arrange
            Label originalLabel = createValidLabelWithId(VALID_LABEL_ID, user);
            Label newLabel = createValidLabelWithId(FUTURE_LABEL_ID, user);

            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            event.setLabel(originalLabel);

            when(labelService.getLabelEntityById(FUTURE_LABEL_ID)).thenReturn(newLabel);
            doThrow(new LabelOwnershipException(UNAUTHORIZED_LABEL_ACCESS, newLabel.getId())).when(ownershipValidator)
                    .validateLabelOwnership(user.getId(), newLabel);

            EventUpdateDTO dto = new EventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    Optional.of(FUTURE_LABEL_ID),
                    null
            );

            // Act + Assert
            assertThrows(LabelOwnershipException.class, () -> eventPatchHandler.applyPatch(event, dto));
            verify(ownershipValidator).validateLabelOwnership(user.getId(), newLabel);
        }

        // endregion

        // region --- isCompleted Patching ---

        @Test
        void testMarkCompleted_successfulUpdate() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            event.setLabel(createValidLabelWithId(VALID_LABEL_ID, user));
            assertFalse(event.isCompleted());

            EventUpdateDTO dto = new EventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    true // mark as completed
            );

            // Act
            boolean result = eventPatchHandler.applyPatch(event, dto);

            // Assert
            assertTrue(result);
            assertTrue(event.isCompleted());
            assertFalse(event.isUnconfirmed());
        }

        @Test
        void testMarkCompleted_missingEndTime_throwsException() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            event.setEndTime(null); // force invalid state
            event.setLabel(createValidLabelWithId(VALID_LABEL_ID, user));
            assertNull(event.getEndTime());

            EventUpdateDTO dto = new EventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    true // try to mark as completed
            );

            // Act + Assert
            InvalidEventStateException ex = assertThrows(InvalidEventStateException.class,
                    () -> eventPatchHandler.applyPatch(event, dto));

            assertEquals(MISSING_EVENT_END_TIME, ex.getErrorCode());
        }

        @Test
        void testMarkUncompleted_successfulUpdate() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            event.setLabel(createValidLabelWithId(VALID_LABEL_ID, user));
            event.setCompleted(true); // already completed

            EventUpdateDTO dto = new EventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    false // mark as uncompleted
            );

            // Act
            boolean result = eventPatchHandler.applyPatch(event, dto);

            // Assert
            assertTrue(result);
            assertFalse(event.isCompleted());
            assertFalse(event.isUnconfirmed());
        }

        @Test
        void testMarkCompleted_onDraftEvent_throwsException() {
            // Arrange
            Event event = createEmptyDraftEvent(user);
            event.setLabel(createValidLabelWithId(VALID_LABEL_ID, user));
            assertTrue(event.isUnconfirmed());

            EventUpdateDTO dto = new EventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    true // mark as completed
            );

            // Act + Assert
            InvalidEventStateException ex = assertThrows(InvalidEventStateException.class,
                    () -> eventPatchHandler.applyPatch(event, dto));

            assertEquals(EVENT_NOT_CONFIRMED, ex.getErrorCode());
        }

        @Test
        void testMarkCompleted_noChange_returnsFalse() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            event.setLabel(createValidLabelWithId(VALID_LABEL_ID, user));
            event.setCompleted(true); // already completed

            EventUpdateDTO dto = new EventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    true // same value
            );

            // Act
            boolean result = eventPatchHandler.applyPatch(event, dto);

            // Assert
            assertFalse(result);
            assertTrue(event.isCompleted());
        }
        // endregion

        // region --- Skip vs Clear Semantics Tests ---

        @Test
        void testSkipVsClearSemantics_allFieldsSkipped() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            event.setLabel(createValidLabelWithId(VALID_LABEL_ID, user));
            event.setDescription("Original description");
            
            String originalName = event.getName();
            ZonedDateTime originalStart = event.getStartTime();
            ZonedDateTime originalEnd = event.getEndTime();
            String originalDescription = event.getDescription();
            Long originalLabelId = event.getLabel().getId();
            boolean originalCompleted = event.isCompleted();

            // All fields null = skip all
            EventUpdateDTO dto = new EventUpdateDTO(null, null, null, null, null, null);

            // Act
            boolean result = eventPatchHandler.applyPatch(event, dto);

            // Assert - no changes made
            assertFalse(result);
            assertEquals(originalName, event.getName());
            assertEquals(originalStart, event.getStartTime());
            assertEquals(originalEnd, event.getEndTime());
            assertEquals(originalDescription, event.getDescription());
            assertEquals(originalLabelId, event.getLabel().getId());
            assertEquals(originalCompleted, event.isCompleted());
        }

        @Test
        void testSkipVsClearSemantics_allOptionalFieldsCleared() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            event.setLabel(createValidLabelWithId(VALID_LABEL_ID, user));
            event.setDescription("Original description");

            Label unlabeled = createValidLabelWithId(UNLABELED_LABEL_ID, user);
            when(labelService.getLabelEntityById(UNLABELED_LABEL_ID)).thenReturn(unlabeled);

            // All Optional fields empty = clear all
            EventUpdateDTO dto = new EventUpdateDTO(
                    Optional.empty(),      // clear name
                    Optional.empty(),      // clear start time
                    Optional.empty(),      // clear end time
                    Optional.empty(),      // clear description
                    Optional.empty(),      // clear label (sets to unlabeled)
                    null                   // skip completion
            );

            // Act
            boolean result = eventPatchHandler.applyPatch(event, dto);

            // Assert - all Optional fields cleared
            assertTrue(result);
            assertNull(event.getName());
            assertNull(event.getStartTime());
            assertNull(event.getEndTime());
            assertNull(event.getDescription());
            assertEquals(UNLABELED_LABEL_ID, event.getLabel().getId()); // cleared to unlabeled
            assertFalse(event.isCompleted()); // unchanged (was skipped)
        }

        @Test
        void testSkipVsClearSemantics_mixedSkipClearUpdate() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            event.setLabel(createValidLabelWithId(VALID_LABEL_ID, user));
            event.setDescription("Original description");

            ZonedDateTime newStart = getValidEventStartFuture(clock).plusHours(2);
            Label newLabel = createValidLabelWithId(FUTURE_LABEL_ID, user);
            when(labelService.getLabelEntityById(FUTURE_LABEL_ID)).thenReturn(newLabel);

            // Mixed: skip some, clear some, update some
            EventUpdateDTO dto = new EventUpdateDTO(
                    Optional.of("New Name"),    // update name
                    Optional.of(newStart),      // update start time
                    null,                       // skip end time
                    Optional.empty(),           // clear description
                    Optional.of(FUTURE_LABEL_ID), // update label
                    true                        // update completion
            );

            // Act
            boolean result = eventPatchHandler.applyPatch(event, dto);

            // Assert - verify mixed behavior
            assertTrue(result);
            assertEquals("New Name", event.getName());           // updated
            assertEquals(newStart.toInstant(), event.getStartTime().toInstant()); // updated (compare instants for UTC storage)
            assertEquals(getValidEventEndFuture(clock).toInstant(), event.getEndTime().toInstant()); // unchanged (skipped)
            assertNull(event.getDescription());                 // cleared
            assertEquals(FUTURE_LABEL_ID, event.getLabel().getId()); // updated
            assertTrue(event.isCompleted());                    // updated
        }

        @Test
        void testSkipVsClearSemantics_noActualChanges() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            event.setLabel(createValidLabelWithId(VALID_LABEL_ID, user));
            event.setDescription(null); // already null
            event.setCompleted(false);  // already false

            // Set values to same as current - should result in no changes
            EventUpdateDTO dto = new EventUpdateDTO(
                    Optional.of(event.getName()),      // same name
                    Optional.of(event.getStartTime()), // same start time
                    Optional.of(event.getEndTime()),   // same end time
                    Optional.empty(),                  // already null
                    Optional.of(event.getLabel().getId()), // same label
                    false                              // same completion
            );

            // Act
            boolean result = eventPatchHandler.applyPatch(event, dto);

            // Assert - no changes made despite fields being present
            assertFalse(result);
        }

        // endregion

        // region --- Multi-Field Update Scenarios ---

        @Test
        void testMultiFieldUpdate_clearingRequiredFieldsThenTryingToComplete() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            event.setLabel(createValidLabelWithId(VALID_LABEL_ID, user));

            // Clear name and try to complete - should fail
            EventUpdateDTO dto = new EventUpdateDTO(
                    Optional.empty(),  // clear name
                    null,              // keep start time
                    null,              // keep end time
                    null,              // keep description
                    null,              // keep label
                    true               // try to complete
            );

            // Act + Assert
            InvalidEventStateException ex = assertThrows(InvalidEventStateException.class,
                    () -> eventPatchHandler.applyPatch(event, dto));
            assertEquals(MISSING_EVENT_NAME, ex.getErrorCode());
        }

        @Test
        void testMultiFieldUpdate_labelServiceFailureDuringMultiFieldUpdate() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            event.setLabel(createValidLabelWithId(VALID_LABEL_ID, user));

            // Mock label service failure
            when(labelService.getLabelEntityById(FUTURE_LABEL_ID))
                    .thenThrow(new RuntimeException("Label service failed"));

            EventUpdateDTO dto = new EventUpdateDTO(
                    Optional.of("New Name"),        // update name
                    null,                           // keep start time
                    null,                           // keep end time
                    Optional.of("New Description"), // update description
                    Optional.of(FUTURE_LABEL_ID),   // update label (will fail)
                    null                            // keep completion
            );

            // Act + Assert
            assertThrows(RuntimeException.class, () -> eventPatchHandler.applyPatch(event, dto));
            
            // Event should be partially updated (name and description changed, label failed)
            assertEquals("New Name", event.getName());
            assertEquals("New Description", event.getDescription());
            assertEquals(VALID_LABEL_ID, event.getLabel().getId()); // unchanged due to failure
        }

        @Test
        void testMultiFieldUpdate_ownershipValidationFailure() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            event.setLabel(createValidLabelWithId(VALID_LABEL_ID, user));

            Label unauthorizedLabel = createValidLabelWithId(FUTURE_LABEL_ID, user);
            when(labelService.getLabelEntityById(FUTURE_LABEL_ID)).thenReturn(unauthorizedLabel);
            doThrow(new LabelOwnershipException(UNAUTHORIZED_LABEL_ACCESS, FUTURE_LABEL_ID))
                    .when(ownershipValidator).validateLabelOwnership(user.getId(), unauthorizedLabel);

            EventUpdateDTO dto = new EventUpdateDTO(
                    Optional.of("New Name"),        // update name
                    null,                           // keep start time
                    null,                           // keep end time
                    Optional.of("New Description"), // update description
                    Optional.of(FUTURE_LABEL_ID),   // update label (will fail ownership)
                    null                            // keep completion
            );

            // Act + Assert
            assertThrows(LabelOwnershipException.class, () -> eventPatchHandler.applyPatch(event, dto));
            
            // Event should be partially updated (name and description changed, label failed)
            assertEquals("New Name", event.getName());
            assertEquals("New Description", event.getDescription());
            assertEquals(VALID_LABEL_ID, event.getLabel().getId()); // unchanged due to failure
        }

        @Test
        void testMultiFieldUpdate_completeEventTransformation() {
            // Arrange - create draft event
            Event draftEvent = createEmptyDraftEvent(user);
            draftEvent.setLabel(createValidLabelWithId(VALID_LABEL_ID, user));

            ZonedDateTime startTime = getValidEventStartFuture(clock);
            ZonedDateTime endTime = getValidEventEndFuture(clock);

            // Transform draft to complete event in one patch
            EventUpdateDTO dto = new EventUpdateDTO(
                    Optional.of("Completed Event"),
                    Optional.of(startTime),
                    Optional.of(endTime),
                    Optional.of("Event description"),
                    null,  // keep current label
                    null   // keep as draft (can't complete draft directly)
            );

            // Act
            boolean result = eventPatchHandler.applyPatch(draftEvent, dto);

            // Assert - event transformed but still draft
            assertTrue(result);
            assertEquals("Completed Event", draftEvent.getName());
            assertEquals(startTime.toInstant(), draftEvent.getStartTime().toInstant());
            assertEquals(endTime.toInstant(), draftEvent.getEndTime().toInstant());
            assertEquals("Event description", draftEvent.getDescription());
            assertTrue(draftEvent.isUnconfirmed()); // still draft
            assertFalse(draftEvent.isCompleted());
        }

        @Test
        void testMultiFieldUpdate_simultaneousTimeAndLabelChanges() {
            // Arrange
            Event event = createValidScheduledEventWithId(EVENT_ID, user, clock);
            event.setLabel(createValidLabelWithId(VALID_LABEL_ID, user));

            ZonedDateTime newStart = getValidEventStartFuture(clock).plusHours(3);
            ZonedDateTime newEnd = getValidEventEndFuture(clock).plusHours(3);
            Label newLabel = createValidLabelWithId(FUTURE_LABEL_ID, user);
            when(labelService.getLabelEntityById(FUTURE_LABEL_ID)).thenReturn(newLabel);

            EventUpdateDTO dto = new EventUpdateDTO(
                    null,                           // keep name
                    Optional.of(newStart),          // update start time
                    Optional.of(newEnd),            // update end time
                    null,                           // keep description
                    Optional.of(FUTURE_LABEL_ID),   // update label
                    true                            // complete event
            );

            // Act
            boolean result = eventPatchHandler.applyPatch(event, dto);

            // Assert - all changes applied
            assertTrue(result);
            assertEquals(newStart.toInstant(), event.getStartTime().toInstant());
            assertEquals(newEnd.toInstant(), event.getEndTime().toInstant());
            assertEquals(FUTURE_LABEL_ID, event.getLabel().getId());
            assertTrue(event.isCompleted());
            verify(ownershipValidator).validateLabelOwnership(user.getId(), newLabel);
        }

        // endregion
    }

}
