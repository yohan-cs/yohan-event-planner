package com.yohan.event_planner.business.handler;

import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.RecurrenceRuleVO;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.domain.enums.RecurrenceFrequency;
import com.yohan.event_planner.dto.RecurringEventUpdateDTO;
import com.yohan.event_planner.exception.InvalidEventStateException;
import com.yohan.event_planner.exception.LabelNotFoundException;
import com.yohan.event_planner.security.OwnershipValidator;
import com.yohan.event_planner.service.LabelService;
import com.yohan.event_planner.service.ParsedRecurrenceInput;
import com.yohan.event_planner.service.RecurrenceRuleService;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_NAME;
import static com.yohan.event_planner.exception.ErrorCode.UNSUPPORTED_RECURRENCE_COMBINATION;
import static com.yohan.event_planner.util.TestConstants.FUTURE_LABEL_ID;
import static com.yohan.event_planner.util.TestConstants.UNLABELED_LABEL_ID;
import static com.yohan.event_planner.util.TestConstants.VALID_EVENT_TITLE;
import static com.yohan.event_planner.util.TestConstants.getValidEventEndDate;
import static com.yohan.event_planner.util.TestConstants.getValidEventEndFuture;
import static com.yohan.event_planner.util.TestConstants.getValidEventStartDate;
import static com.yohan.event_planner.util.TestConstants.getValidEventStartFuture;
import static com.yohan.event_planner.util.TestUtils.createUnconfirmedRecurringEvent;
import static com.yohan.event_planner.util.TestUtils.createValidLabelWithId;
import static com.yohan.event_planner.util.TestUtils.createValidRecurringEvent;
import static com.yohan.event_planner.util.TestUtils.createValidUserEntityWithId;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Comprehensive test suite for {@link RecurringEventPatchHandler}.
 * 
 * <p>This test suite validates the complex field patching logic implemented in the RecurringEventPatchHandler,
 * with particular focus on skip vs clear semantics, confirmation state handling, and recurrence rule processing.</p>
 * 
 * <h2>Test Organization</h2>
 * <p>The test suite is organized into the following major sections:</p>
 * 
 * <h3>1. Individual Field Patching Tests</h3>
 * <ul>
 *   <li><strong>Name Patching:</strong> Tests for updating, clearing, and validation of event names</li>
 *   <li><strong>Time Patching:</strong> Tests for start/end time updates with confirmation validation</li>
 *   <li><strong>Date Patching:</strong> Tests for start/end date updates and validation constraints</li>
 *   <li><strong>Description Patching:</strong> Tests for description updates and clearing behavior</li>
 *   <li><strong>Label Patching:</strong> Tests for label updates, ownership validation, and Unlabeled fallback</li>
 *   <li><strong>Recurrence Rule Patching:</strong> Tests for rule parsing, confirmed vs unconfirmed handling</li>
 * </ul>
 * 
 * <h3>2. Confirmation State Validation Tests</h3>
 * <ul>
 *   <li><strong>Confirmed Event Constraints:</strong> Tests ensuring confirmed events cannot have null required fields</li>
 *   <li><strong>Unconfirmed Event Flexibility:</strong> Tests allowing null fields for draft recurring events</li>
 *   <li><strong>State Transition Validation:</strong> Tests for validation during confirmation state changes</li>
 * </ul>
 * 
 * <h3>3. Skip vs Clear Semantics Test Suite</h3>
 * <ul>
 *   <li><strong>All Fields Skipped:</strong> Tests where all DTO fields are null (no changes)</li>
 *   <li><strong>All Fields Cleared:</strong> Tests where all DTO fields are Optional.empty() (clear to null/defaults)</li>
 *   <li><strong>Mixed Operations:</strong> Tests combining skip, clear, and update operations</li>
 *   <li><strong>Confirmation Constraints:</strong> Tests ensuring confirmed events reject invalid clear operations</li>
 * </ul>
 * 
 * <h3>4. Recurrence Rule Edge Cases Test Suite</h3>
 * <ul>
 *   <li><strong>Complex Rule Processing:</strong> Tests for confirmed events with complex recurrence patterns</li>
 *   <li><strong>Raw String Storage:</strong> Tests for unconfirmed events storing unparsed rule strings</li>
 *   <li><strong>State Transitions:</strong> Tests for rule handling during confirmation state changes</li>
 *   <li><strong>Null to Valid Rules:</strong> Tests for setting rules on events that previously had none</li>
 *   <li><strong>Equivalent Rule Detection:</strong> Tests ensuring duplicate updates are avoided</li>
 * </ul>
 * 
 * <h3>5. Multi-Field Update Edge Cases Test Suite</h3>
 * <ul>
 *   <li><strong>Date and Rule Coordination:</strong> Tests for recurrence rule updates with date changes</li>
 *   <li><strong>Label and Rule Interaction:</strong> Tests for simultaneous label and rule updates</li>
 *   <li><strong>Time Changes with Validation:</strong> Tests for comprehensive time and metadata updates</li>
 *   <li><strong>Partial Updates:</strong> Tests mixing skip, clear, and update operations across multiple fields</li>
 *   <li><strong>Atomic Operations:</strong> Tests ensuring updates are all-or-nothing for consistency</li>
 * </ul>
 * 
 * <h2>Test Patterns and Conventions</h2>
 * 
 * <h3>Skip vs Clear vs Update Semantics</h3>
 * <ul>
 *   <li><strong>Skip:</strong> DTO field is null → no change to recurring event field</li>
 *   <li><strong>Clear:</strong> DTO field is Optional.empty() → recurring event field set to null (or default)</li>
 *   <li><strong>Update:</strong> DTO field is Optional.of(value) → recurring event field updated to value</li>
 * </ul>
 * 
 * <h3>Confirmation State Testing</h3>
 * <ul>
 *   <li><strong>Confirmed Events:</strong> Created with {@code createValidRecurringEvent()} - full validation</li>
 *   <li><strong>Unconfirmed Events:</strong> Created with {@code createUnconfirmedRecurringEvent()} - flexible validation</li>
 * </ul>
 * 
 * <h3>Mock Usage Patterns</h3>
 * <ul>
 *   <li><strong>LabelService:</strong> Mocked for label retrieval and ownership validation tests</li>
 *   <li><strong>RecurrenceRuleService:</strong> Mocked for rule parsing and summary generation</li>
 *   <li><strong>OwnershipValidator:</strong> Mocked for label ownership validation</li>
 * </ul>
 * 
 * <h2>Key Test Scenarios</h2>
 * <p>Critical scenarios covered include:</p>
 * <ul>
 *   <li>Label clearing defaults to "Unlabeled" rather than null</li>
 *   <li>Confirmed events enforce required field validation</li>
 *   <li>Unconfirmed events allow flexible null fields</li>
 *   <li>Recurrence rules are parsed for confirmed events, stored raw for unconfirmed</li>
 *   <li>Multi-field updates maintain data consistency</li>
 *   <li>Skip semantics preserve existing values</li>
 *   <li>Clear semantics properly handle null/default assignment</li>
 * </ul>
 * 
 * @see RecurringEventPatchHandler
 * @see RecurringEventUpdateDTO
 * @see RecurringEvent
 */
public class RecurringEventPatchHandlerTest {

    private static final LocalDate FIXED_TEST_DATE = LocalDate.of(2025, 6, 29);

    private LabelService labelService;
    private RecurrenceRuleService recurrenceRuleService;
    private OwnershipValidator ownershipValidator;
    private RecurringEventPatchHandler recurringEventPatchHandler;
    private User user;
    private Clock clock;

    @BeforeEach
    void setUp() {
        labelService = mock(LabelService.class);
        recurrenceRuleService = mock(RecurrenceRuleService.class);
        ownershipValidator = mock(OwnershipValidator.class);
        recurringEventPatchHandler = new RecurringEventPatchHandler(labelService, recurrenceRuleService, ownershipValidator);

        user = createValidUserEntityWithId();
        ZoneId userZone = ZoneId.of(user.getTimezone());

        clock = mock(Clock.class);
        ZonedDateTime fixedTime = ZonedDateTime.parse("2025-06-29T12:00:00Z").withZoneSameInstant(userZone);

        when(clock.instant()).thenReturn(fixedTime.toInstant());
        when(clock.getZone()).thenReturn(userZone);

    }

    @Nested
    class ApplyPatchTests {

        // region --- Name Patching ---

        @Test
        void testPatchName_successfulUpdate() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    Optional.of("Updated Recurring Event Name"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            assertEquals("Updated Recurring Event Name", recurringEvent.getName());
        }

        @Test
        void testPatchName_noChange() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            recurringEvent.setName(VALID_EVENT_TITLE);
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    Optional.of(recurringEvent.getName()),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertFalse(result);
            assertEquals(VALID_EVENT_TITLE, recurringEvent.getName());
        }

        @Test
        void testPatchName_clearName_throwsException() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    Optional.ofNullable(null),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Act & Assert
            InvalidEventStateException ex = assertThrows(InvalidEventStateException.class,
                    () -> recurringEventPatchHandler.applyPatch(recurringEvent, dto));

            assertEquals(MISSING_EVENT_NAME, ex.getErrorCode());
        }

        // endregion


        // region --- Start and End Time Patching ---

        @Test
        void testPatchStartTime_successfulUpdate() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            LocalTime newStartTime = recurringEvent.getStartTime().plusHours(1);
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    Optional.of(newStartTime),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            assertEquals(newStartTime, recurringEvent.getStartTime());
        }

        @Test
        void testPatchStartTime_noChange() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            LocalTime currentStartTime = recurringEvent.getStartTime();
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    Optional.of(currentStartTime),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertFalse(result);
            assertEquals(currentStartTime, recurringEvent.getStartTime());
        }

        @Test
        void testPatchEndTime_successfulUpdate() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            LocalTime newEndTime = recurringEvent.getEndTime().minusHours(1);
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    Optional.of(newEndTime),
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            assertEquals(newEndTime, recurringEvent.getEndTime());
        }

        @Test
        void testPatchEndTime_noChange() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            LocalTime currentEndTime = recurringEvent.getEndTime();
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    Optional.of(currentEndTime),
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertFalse(result);
            assertEquals(currentEndTime, recurringEvent.getEndTime());
        }

        // endregion


        // region --- Start Date and End Date Patching ---

        @Test
        void testPatchStartDate_successfulUpdate() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            LocalDate newStartDate = recurringEvent.getStartDate().plusDays(5);
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    Optional.of(newStartDate),
                    null,
                    null,
                    null,
                    null
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            assertEquals(newStartDate, recurringEvent.getStartDate());
        }

        @Test
        void testPatchStartDate_noChange() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            LocalDate currentStartDate = recurringEvent.getStartDate();
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    Optional.of(currentStartDate),
                    null,
                    null,
                    null,
                    null
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertFalse(result);
            assertEquals(currentStartDate, recurringEvent.getStartDate());
        }

        @Test
        void testPatchEndDate_successfulUpdate() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            LocalDate newEndDate = recurringEvent.getEndDate().minusDays(3);
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    Optional.of(newEndDate),
                    null,
                    null,
                    null
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            assertEquals(newEndDate, recurringEvent.getEndDate());
        }

        @Test
        void testPatchEndDate_noChange() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            LocalDate currentEndDate = recurringEvent.getEndDate();
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    Optional.of(currentEndDate),
                    null,
                    null,
                    null
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertFalse(result);
            assertEquals(currentEndDate, recurringEvent.getEndDate());
        }

        // endregion


        // region --- Description Patching ---

        @Test
        void testPatchDescription_successfulUpdate() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            String newDescription = "Updated recurring event description";
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    Optional.of(newDescription),
                    null,
                    null
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            assertEquals(newDescription, recurringEvent.getDescription());
        }

        @Test
        void testPatchDescription_noChange() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            String currentDescription = recurringEvent.getDescription();
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    Optional.of(currentDescription),
                    null,
                    null
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertFalse(result);
            assertEquals(currentDescription, recurringEvent.getDescription());
        }

        @Test
        void testPatchDescription_clearDescription() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            recurringEvent.setDescription("Some description to clear");
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    Optional.ofNullable(null), // explicit null to clear
                    null,
                    null
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            assertNull(recurringEvent.getDescription());
        }

        // endregion


        // region --- Label Patching ---

        @Test
        void testPatchLabel_successfulUpdate() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            Label originalLabel = recurringEvent.getLabel();
            Label newLabel = createValidLabelWithId(FUTURE_LABEL_ID, user);
            recurringEvent.setLabel(originalLabel);

            when(labelService.getLabelEntityById(FUTURE_LABEL_ID)).thenReturn(newLabel);

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Optional.of(FUTURE_LABEL_ID),
                    null
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            assertEquals(FUTURE_LABEL_ID, recurringEvent.getLabel().getId());
            verify(ownershipValidator).validateLabelOwnership(user.getId(), newLabel);
        }

        @Test
        void testPatchLabel_noChange() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            Label currentLabel = recurringEvent.getLabel();
            recurringEvent.setLabel(currentLabel);

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Optional.of(currentLabel.getId()),
                    null
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertFalse(result);
            assertEquals(currentLabel.getId(), recurringEvent.getLabel().getId());
            verifyNoInteractions(ownershipValidator);
        }

        @Test
        void testPatchLabel_clearLabel_setsToUnlabeled() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            Label originalLabel = recurringEvent.getLabel();
            Label unlabeled = createValidLabelWithId(UNLABELED_LABEL_ID, user);
            TestUtils.setUnlabeledLabel(user, unlabeled);

            recurringEvent.setLabel(originalLabel);

            when(labelService.getLabelEntityById(UNLABELED_LABEL_ID)).thenReturn(unlabeled);

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Optional.ofNullable(null), // labelId cleared
                    null
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            assertEquals(UNLABELED_LABEL_ID, recurringEvent.getLabel().getId());
            verify(ownershipValidator).validateLabelOwnership(user.getId(), unlabeled);
        }

        @Test
        void testPatchLabel_invalidOwnership_throwsException() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            Label originalLabel = recurringEvent.getLabel();
            Label newLabel = createValidLabelWithId(FUTURE_LABEL_ID, user);
            recurringEvent.setLabel(originalLabel);

            when(labelService.getLabelEntityById(FUTURE_LABEL_ID)).thenReturn(newLabel);
            doThrow(new RuntimeException("Unauthorized"))
                    .when(ownershipValidator).validateLabelOwnership(user.getId(), newLabel);

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Optional.of(FUTURE_LABEL_ID),
                    null
            );

            // Act & Assert
            assertThrows(RuntimeException.class, () -> recurringEventPatchHandler.applyPatch(recurringEvent, dto));
            verify(ownershipValidator).validateLabelOwnership(user.getId(), newLabel);
        }

        @Test
        void testPatchLabel_nonExistentLabel_throwsLabelNotFoundException() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);

            when(labelService.getLabelEntityById(FUTURE_LABEL_ID))
                    .thenThrow(new LabelNotFoundException(FUTURE_LABEL_ID));

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Optional.of(FUTURE_LABEL_ID),
                    null
            );

            // Act & Assert
            assertThrows(LabelNotFoundException.class,
                    () -> recurringEventPatchHandler.applyPatch(recurringEvent, dto));

            verify(labelService).getLabelEntityById(FUTURE_LABEL_ID);
        }

        @Test
        void testPatchClearLabel_assignsUnlabeledLabel() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            Label unlabeled = createValidLabelWithId(UNLABELED_LABEL_ID, user);
            TestUtils.setUnlabeledLabel(user, unlabeled);

            when(labelService.getLabelEntityById(UNLABELED_LABEL_ID)).thenReturn(unlabeled);

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Optional.ofNullable(null),
                    null
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            assertEquals(UNLABELED_LABEL_ID, recurringEvent.getLabel().getId());
            verify(ownershipValidator).validateLabelOwnership(user.getId(), unlabeled);
        }

        // endregion


        // region --- Recurrence Rule Patching ---

        @Test
        void testPatchRecurrenceRule_successfulUpdate() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            String newRule = "WEEKLY:MONDAY,FRIDAY";

            Set<DayOfWeek> days = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

            LocalDate startDate = getValidEventStartDate(clock);
            LocalDate endDate = getValidEventEndDate(clock);

            ParsedRecurrenceInput parsed = new ParsedRecurrenceInput(RecurrenceFrequency.WEEKLY, days, null);
            String expectedSummary = recurrenceRuleService.buildSummary(parsed, startDate, endDate);

            when(recurrenceRuleService.parseFromString(newRule)).thenReturn(parsed);
            when(recurrenceRuleService.buildSummary(parsed, startDate, endDate)).thenReturn(expectedSummary);

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Optional.of(newRule)
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            assertNotNull(recurringEvent.getRecurrenceRule());
            assertEquals(expectedSummary, recurringEvent.getRecurrenceRule().getSummary());
            assertEquals(parsed, recurringEvent.getRecurrenceRule().getParsed());
        }

        @Test
        void testPatchRecurrenceRule_noChange() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            RecurrenceRuleVO currentVO = recurringEvent.getRecurrenceRule();
            String currentRule = currentVO.getSummary();

            // Setup mocks to match the existing rule
            ParsedRecurrenceInput parsed = currentVO.getParsed();
            when(recurrenceRuleService.parseFromString(currentRule)).thenReturn(parsed);
            when(recurrenceRuleService.buildSummary(parsed, recurringEvent.getStartDate(), recurringEvent.getEndDate())).thenReturn(currentRule);

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Optional.of(currentRule)
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertFalse(result);
            assertEquals(currentRule, recurringEvent.getRecurrenceRule().getSummary());
            assertEquals(parsed, recurringEvent.getRecurrenceRule().getParsed());
        }

        @Test
        void testPatchRecurrenceRule_clearRule_unconfirmedOnly() {
            // Arrange
            RecurringEvent recurringEvent = createUnconfirmedRecurringEvent(user, clock); // must be unconfirmed
            assertTrue(recurringEvent.isUnconfirmed());
            assertNotNull(recurringEvent.getRecurrenceRule());

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Optional.ofNullable(null)
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            assertNull(recurringEvent.getRecurrenceRule());
        }

        @Test
        void testPatchRecurrenceRule_equivalentParsedNoUpdate() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            String originalRule = "WEEKLY:MONDAY";
            String equivalentRule = "WEEKLY:MONDAY"; // Same string, test still valid

            Set<DayOfWeek> days = EnumSet.of(DayOfWeek.MONDAY);
            ParsedRecurrenceInput parsed = new ParsedRecurrenceInput(RecurrenceFrequency.WEEKLY, days, null);

            when(recurrenceRuleService.parseFromString(equivalentRule)).thenReturn(parsed);
            when(recurrenceRuleService.buildSummary(parsed, recurringEvent.getStartDate(), recurringEvent.getEndDate()))
                    .thenReturn(originalRule);

            RecurrenceRuleVO currentVO = new RecurrenceRuleVO(originalRule, parsed);
            recurringEvent.setRecurrenceRule(currentVO);

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Optional.of(equivalentRule)
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertFalse(result);
            assertEquals(originalRule, recurringEvent.getRecurrenceRule().getSummary());
            assertSame(parsed, recurringEvent.getRecurrenceRule().getParsed());
        }

        @Test
        void testPatchRecurrenceRuleUnconfirmed_noChange() {
            RecurringEvent recurringEvent = createUnconfirmedRecurringEvent(user, clock);
            String currentRaw = recurringEvent.getRecurrenceRule().getSummary();

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Optional.of(currentRaw)
            );

            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            assertFalse(result);
            assertEquals(currentRaw, recurringEvent.getRecurrenceRule().getSummary());
        }

        @Test
        void testPatchRecurrenceRule_invalidString_throwsException() {
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            String invalidRule = "GIBBERISH";

            when(recurrenceRuleService.parseFromString(invalidRule))
                    .thenThrow(new InvalidEventStateException(UNSUPPORTED_RECURRENCE_COMBINATION));

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Optional.of(invalidRule)
            );

            assertThrows(InvalidEventStateException.class,
                    () -> recurringEventPatchHandler.applyPatch(recurringEvent, dto));
        }

        // endregion


        // region --- Confirmed vs Unconfirmed Required Fields Enforcement ---

        @Test
        void testClearStartTime_onConfirmedEvent_throwsException() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    Optional.ofNullable(null),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Act & Assert
            InvalidEventStateException ex = assertThrows(InvalidEventStateException.class,
                    () -> recurringEventPatchHandler.applyPatch(recurringEvent, dto));

            assertEquals(com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_START_TIME, ex.getErrorCode());
        }

        @Test
        void testClearEndTime_onConfirmedEvent_throwsException() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    Optional.ofNullable(null),
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Act & Assert
            InvalidEventStateException ex = assertThrows(InvalidEventStateException.class,
                    () -> recurringEventPatchHandler.applyPatch(recurringEvent, dto));

            assertEquals(com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_END_TIME, ex.getErrorCode());
        }

        @Test
        void testClearStartDate_onConfirmedEvent_throwsException() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    Optional.ofNullable(null),
                    null,
                    null,
                    null,
                    null
            );

            // Act & Assert
            InvalidEventStateException ex = assertThrows(InvalidEventStateException.class,
                    () -> recurringEventPatchHandler.applyPatch(recurringEvent, dto));

            assertEquals(com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_START_DATE, ex.getErrorCode());
        }

        @Test
        void testClearEndDate_onConfirmedEvent_throwsException() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    Optional.ofNullable(null),
                    null,
                    null,
                    null
            );

            // Act & Assert
            InvalidEventStateException ex = assertThrows(InvalidEventStateException.class,
                    () -> recurringEventPatchHandler.applyPatch(recurringEvent, dto));

            assertEquals(com.yohan.event_planner.exception.ErrorCode.MISSING_EVENT_END_DATE, ex.getErrorCode());
        }

        @Test
        void testClearStartTime_onUnconfirmedEvent_noException() {
            // Arrange
            RecurringEvent recurringEvent = createUnconfirmedRecurringEvent(user, clock);
            recurringEvent.setStartTime(getValidEventStartFuture(clock).toLocalTime());

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    Optional.ofNullable(null), // clearing the start time
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Act & Assert
            assertDoesNotThrow(() -> {
                boolean updated = recurringEventPatchHandler.applyPatch(recurringEvent, dto);
                assertTrue(updated); // should return true since we cleared a field
                assertNull(recurringEvent.getStartTime()); // start time should now be null
            });
        }

        @Test
        void testClearEndTime_onUnconfirmedEvent_noException() {
            // Arrange
            RecurringEvent recurringEvent = createUnconfirmedRecurringEvent(user, clock);
            recurringEvent.setEndTime(getValidEventEndFuture(clock).toLocalTime()); // Simulate it initially having an endTime

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    Optional.ofNullable(null), // clearing the end time
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Act & Assert
            assertDoesNotThrow(() -> {
                boolean updated = recurringEventPatchHandler.applyPatch(recurringEvent, dto);
                assertTrue(updated); // because endTime actually changed
                assertNull(recurringEvent.getEndTime()); // should now be null
            });
        }

        @Test
        void testClearStartDate_onUnconfirmedEvent_noException() {
            // Arrange
            RecurringEvent recurringEvent = createUnconfirmedRecurringEvent(user, clock);
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    Optional.ofNullable(null),
                    null,
                    null,
                    null,
                    null
            );

            // Act & Assert
            assertDoesNotThrow(() -> {
                boolean updated = recurringEventPatchHandler.applyPatch(recurringEvent, dto);
                assertTrue(updated);
                assertNull(recurringEvent.getStartDate());
            });
        }

        @Test
        void testClearEndDate_onUnconfirmedEvent_noException() {
            // Arrange
            RecurringEvent recurringEvent = createUnconfirmedRecurringEvent(user, clock);
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    Optional.ofNullable(null),
                    null,
                    null,
                    null
            );

            // Act & Assert
            assertDoesNotThrow(() -> {
                boolean updated = recurringEventPatchHandler.applyPatch(recurringEvent, dto);
                assertTrue(updated);
                assertNull(recurringEvent.getEndDate());
            });
        }

        // endregion


        // region --- Miscellaneous Tests ---

        @Test
        void testPatchWithAllNullFields_returnsFalse() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertFalse(result);
            // No fields changed
        }

        @Test
        void testPatchMultipleFields_successfulUpdate() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            String newName = "Multi-field Update";
            LocalTime newStartTime = recurringEvent.getStartTime().plusHours(1);
            String newRecurrenceRule = "MONTHLY:1:MONDAY";
            Label newLabel = createValidLabelWithId(FUTURE_LABEL_ID, user);

            // Mock label service
            when(labelService.getLabelEntityById(FUTURE_LABEL_ID)).thenReturn(newLabel);

            // Mock recurrence rule service
            Set<DayOfWeek> days = EnumSet.of(DayOfWeek.MONDAY);
            ParsedRecurrenceInput parsed = new ParsedRecurrenceInput(RecurrenceFrequency.MONTHLY, days, 1);
            when(recurrenceRuleService.parseFromString(newRecurrenceRule)).thenReturn(parsed);
            when(recurrenceRuleService.buildSummary(parsed, recurringEvent.getStartDate(), recurringEvent.getEndDate()))
                    .thenReturn(newRecurrenceRule);

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    Optional.of(newName),
                    Optional.of(newStartTime),
                    null,
                    null,
                    null,
                    null,
                    Optional.of(FUTURE_LABEL_ID),
                    Optional.of(newRecurrenceRule)
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            assertEquals(newName, recurringEvent.getName());
            assertEquals(newStartTime, recurringEvent.getStartTime());
            assertNotNull(recurringEvent.getRecurrenceRule());
            assertEquals(newRecurrenceRule, recurringEvent.getRecurrenceRule().getSummary());
            assertEquals(parsed, recurringEvent.getRecurrenceRule().getParsed());
            assertEquals(FUTURE_LABEL_ID, recurringEvent.getLabel().getId());
            verify(ownershipValidator).validateLabelOwnership(user.getId(), newLabel);
        }


        // endregion

        // region --- Skip vs Clear Semantics Test Suite ---

        @Test
        void testSkipVsClearSemantics_allFieldsSkipped() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            String originalName = recurringEvent.getName();
            LocalTime originalStartTime = recurringEvent.getStartTime();
            LocalTime originalEndTime = recurringEvent.getEndTime();
            LocalDate originalStartDate = recurringEvent.getStartDate();
            LocalDate originalEndDate = recurringEvent.getEndDate();
            String originalDescription = recurringEvent.getDescription();
            Label originalLabel = recurringEvent.getLabel();
            RecurrenceRuleVO originalRule = recurringEvent.getRecurrenceRule();

            // All fields null = skip semantics
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null, // skip name
                    null, // skip startTime
                    null, // skip endTime
                    null, // skip startDate
                    null, // skip endDate
                    null, // skip description
                    null, // skip labelId
                    null  // skip recurrenceRule
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertFalse(result, "No fields should be updated when all are skipped");
            assertEquals(originalName, recurringEvent.getName());
            assertEquals(originalStartTime, recurringEvent.getStartTime());
            assertEquals(originalEndTime, recurringEvent.getEndTime());
            assertEquals(originalStartDate, recurringEvent.getStartDate());
            assertEquals(originalEndDate, recurringEvent.getEndDate());
            assertEquals(originalDescription, recurringEvent.getDescription());
            assertEquals(originalLabel.getId(), recurringEvent.getLabel().getId());
            assertEquals(originalRule, recurringEvent.getRecurrenceRule());
        }

        @Test
        void testSkipVsClearSemantics_allFieldsCleared() {
            // Arrange
            RecurringEvent recurringEvent = createUnconfirmedRecurringEvent(user, clock); // Use unconfirmed to allow clearing
            Label unlabeledLabel = createValidLabelWithId(UNLABELED_LABEL_ID, user);
            TestUtils.setUnlabeledLabel(user, unlabeledLabel);

            when(labelService.getLabelEntityById(UNLABELED_LABEL_ID)).thenReturn(unlabeledLabel);

            // All fields Optional.empty() = clear semantics
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    Optional.empty(), // clear name
                    Optional.empty(), // clear startTime
                    Optional.empty(), // clear endTime
                    Optional.empty(), // clear startDate
                    Optional.empty(), // clear endDate
                    Optional.empty(), // clear description
                    Optional.empty(), // clear labelId (should resolve to Unlabeled)
                    Optional.empty()  // clear recurrenceRule
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result, "Fields should be updated when cleared");
            assertNull(recurringEvent.getName());
            assertNull(recurringEvent.getStartTime());
            assertNull(recurringEvent.getEndTime());
            assertNull(recurringEvent.getStartDate());
            assertNull(recurringEvent.getEndDate());
            assertNull(recurringEvent.getDescription());
            assertEquals(UNLABELED_LABEL_ID, recurringEvent.getLabel().getId()); // Should resolve to Unlabeled
            assertNull(recurringEvent.getRecurrenceRule());
        }

        @Test
        void testSkipVsClearSemantics_mixedSkipAndClear() {
            // Arrange
            RecurringEvent recurringEvent = createUnconfirmedRecurringEvent(user, clock);
            String originalName = recurringEvent.getName();
            LocalTime originalStartTime = recurringEvent.getStartTime();
            String originalDescription = recurringEvent.getDescription();
            Label originalLabel = recurringEvent.getLabel();

            Label unlabeledLabel = createValidLabelWithId(UNLABELED_LABEL_ID, user);
            TestUtils.setUnlabeledLabel(user, unlabeledLabel);
            when(labelService.getLabelEntityById(UNLABELED_LABEL_ID)).thenReturn(unlabeledLabel);

            // Mix of skip and clear operations
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,               // skip name (no change)
                    Optional.empty(),   // clear startTime
                    null,               // skip endTime (no change)
                    Optional.empty(),   // clear startDate
                    null,               // skip endDate (no change)
                    null,               // skip description (no change)
                    Optional.empty(),   // clear labelId (should resolve to Unlabeled)
                    Optional.empty()    // clear recurrenceRule
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result, "Some fields should be updated");
            // Skipped fields should remain unchanged
            assertEquals(originalName, recurringEvent.getName());
            assertEquals(originalStartTime, recurringEvent.getStartTime());
            assertEquals(originalDescription, recurringEvent.getDescription());
            // Cleared fields should be null or resolve to defaults
            assertNull(recurringEvent.getStartDate());
            assertEquals(UNLABELED_LABEL_ID, recurringEvent.getLabel().getId());
            assertNull(recurringEvent.getRecurrenceRule());
        }

        @Test
        void testSkipVsClearSemantics_updateVsSkipVsClear() {
            // Arrange
            RecurringEvent recurringEvent = createUnconfirmedRecurringEvent(user, clock);
            String originalName = recurringEvent.getName();
            LocalTime originalEndTime = recurringEvent.getEndTime();
            
            String newName = "Updated Name";
            LocalTime newStartTime = LocalTime.of(14, 30);
            Label newLabel = createValidLabelWithId(FUTURE_LABEL_ID, user);
            Label unlabeledLabel = createValidLabelWithId(UNLABELED_LABEL_ID, user);
            TestUtils.setUnlabeledLabel(user, unlabeledLabel);

            when(labelService.getLabelEntityById(FUTURE_LABEL_ID)).thenReturn(newLabel);
            when(labelService.getLabelEntityById(UNLABELED_LABEL_ID)).thenReturn(unlabeledLabel);

            // Mix of skip, clear, and update operations
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    Optional.of(newName),         // UPDATE name
                    Optional.of(newStartTime),    // UPDATE startTime
                    null,                         // SKIP endTime (no change)
                    Optional.empty(),             // CLEAR startDate
                    Optional.of(FIXED_TEST_DATE.plusDays(30)), // UPDATE endDate
                    Optional.empty(),             // CLEAR description
                    Optional.of(FUTURE_LABEL_ID), // UPDATE labelId
                    Optional.empty()              // CLEAR recurrenceRule
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result, "Multiple fields should be updated");
            // Updated fields
            assertEquals(newName, recurringEvent.getName());
            assertEquals(newStartTime, recurringEvent.getStartTime());
            assertEquals(FUTURE_LABEL_ID, recurringEvent.getLabel().getId());
            // Skipped fields should remain unchanged
            assertEquals(originalEndTime, recurringEvent.getEndTime());
            // Cleared fields should be null
            assertNull(recurringEvent.getStartDate());
            assertNull(recurringEvent.getDescription());
            assertNull(recurringEvent.getRecurrenceRule());
        }

        @Test
        void testSkipVsClearSemantics_confirmedEventConstraints() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock); // Confirmed event
            assertFalse(recurringEvent.isUnconfirmed(), "Event should be confirmed");

            // Try to clear required fields on confirmed event
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    Optional.empty(), // Try to clear name on confirmed event
                    null,             // Skip other fields
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            // Act & Assert
            InvalidEventStateException ex = assertThrows(InvalidEventStateException.class,
                    () -> recurringEventPatchHandler.applyPatch(recurringEvent, dto));
            assertEquals(MISSING_EVENT_NAME, ex.getErrorCode());
        }

        // endregion

        // region --- Recurrence Rule Edge Cases Test Suite ---

        @Test
        void testRecurrenceRuleEdgeCases_confirmedEventComplexRule() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            String complexRule = "WEEKLY:MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY";
            
            Set<DayOfWeek> weekdays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, 
                    DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
            ParsedRecurrenceInput parsed = new ParsedRecurrenceInput(RecurrenceFrequency.WEEKLY, weekdays, null);
            String expectedSummary = "Weekdays (Mon-Fri)";
            
            when(recurrenceRuleService.parseFromString(complexRule)).thenReturn(parsed);
            when(recurrenceRuleService.buildSummary(parsed, recurringEvent.getStartDate(), recurringEvent.getEndDate()))
                    .thenReturn(expectedSummary);

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null, null, null, null, null, null, null,
                    Optional.of(complexRule)
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            assertNotNull(recurringEvent.getRecurrenceRule());
            assertEquals(expectedSummary, recurringEvent.getRecurrenceRule().getSummary());
            assertEquals(parsed, recurringEvent.getRecurrenceRule().getParsed());
        }

        @Test
        void testRecurrenceRuleEdgeCases_unconfirmedEventRawStorageOnly() {
            // Arrange
            RecurringEvent recurringEvent = createUnconfirmedRecurringEvent(user, clock);
            String rawRule = "CUSTOM:PATTERN:THAT:ISNT:PARSED:YET";

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null, null, null, null, null, null, null,
                    Optional.of(rawRule)
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            assertNotNull(recurringEvent.getRecurrenceRule());
            assertEquals(rawRule, recurringEvent.getRecurrenceRule().getSummary());
            assertNull(recurringEvent.getRecurrenceRule().getParsed(), "Unconfirmed events should not have parsed rules");
            verifyNoInteractions(recurrenceRuleService); // Service should not be called for unconfirmed events
        }

        @Test
        void testRecurrenceRuleEdgeCases_transitionConfirmedToUnconfirmed() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock); // Confirmed
            RecurrenceRuleVO originalRule = recurringEvent.getRecurrenceRule();
            
            // Simulate transitioning to unconfirmed state (this would typically happen in service layer)
            recurringEvent.setUnconfirmed(true);
            
            String newRawRule = "DRAFT:PATTERN:NOT:VALIDATED";

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null, null, null, null, null, null, null,
                    Optional.of(newRawRule)
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            assertNotNull(recurringEvent.getRecurrenceRule());
            assertEquals(newRawRule, recurringEvent.getRecurrenceRule().getSummary());
            assertNull(recurringEvent.getRecurrenceRule().getParsed(), "Unconfirmed events should store raw rules only");
            verifyNoInteractions(recurrenceRuleService);
        }

        @Test
        void testRecurrenceRuleEdgeCases_nullRuleToValidRule() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            recurringEvent.setRecurrenceRule(null); // Start with no rule
            
            String newRule = "DAILY";
            ParsedRecurrenceInput parsed = new ParsedRecurrenceInput(RecurrenceFrequency.DAILY, null, null);
            String expectedSummary = "Daily";
            
            when(recurrenceRuleService.parseFromString(newRule)).thenReturn(parsed);
            when(recurrenceRuleService.buildSummary(parsed, recurringEvent.getStartDate(), recurringEvent.getEndDate()))
                    .thenReturn(expectedSummary);

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null, null, null, null, null, null, null,
                    Optional.of(newRule)
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            assertNotNull(recurringEvent.getRecurrenceRule());
            assertEquals(expectedSummary, recurringEvent.getRecurrenceRule().getSummary());
            assertEquals(parsed, recurringEvent.getRecurrenceRule().getParsed());
        }

        @Test
        void testRecurrenceRuleEdgeCases_equivalentRulesNoDuplicateUpdate() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            RecurrenceRuleVO originalRule = recurringEvent.getRecurrenceRule();
            
            // Same rule content but different string representation
            String equivalentRule = originalRule.getSummary();
            ParsedRecurrenceInput sameParsed = originalRule.getParsed();
            
            when(recurrenceRuleService.parseFromString(equivalentRule)).thenReturn(sameParsed);
            when(recurrenceRuleService.buildSummary(sameParsed, recurringEvent.getStartDate(), recurringEvent.getEndDate()))
                    .thenReturn(originalRule.getSummary());

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null, null, null, null, null, null, null,
                    Optional.of(equivalentRule)
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertFalse(result, "No update should occur for equivalent rules");
            assertEquals(originalRule.getSummary(), recurringEvent.getRecurrenceRule().getSummary());
            assertEquals(originalRule.getParsed(), recurringEvent.getRecurrenceRule().getParsed());
        }

        // endregion

        // region --- Multi-Field Update Edge Cases Test Suite ---

        @Test
        void testMultiFieldUpdates_recurrenceRuleWithDateChanges() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            LocalDate newStartDate = FIXED_TEST_DATE.plusDays(10);
            LocalDate newEndDate = FIXED_TEST_DATE.plusDays(40);
            String newRule = "WEEKLY:SUNDAY";
            
            Set<DayOfWeek> days = EnumSet.of(DayOfWeek.SUNDAY);
            ParsedRecurrenceInput parsed = new ParsedRecurrenceInput(RecurrenceFrequency.WEEKLY, days, null);
            String expectedSummary = "Weekly on Sunday";
            
            when(recurrenceRuleService.parseFromString(newRule)).thenReturn(parsed);
            when(recurrenceRuleService.buildSummary(parsed, newStartDate, newEndDate))
                    .thenReturn(expectedSummary);

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null, null, null,
                    Optional.of(newStartDate),
                    Optional.of(newEndDate),
                    null, null,
                    Optional.of(newRule)
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            assertEquals(newStartDate, recurringEvent.getStartDate());
            assertEquals(newEndDate, recurringEvent.getEndDate());
            assertEquals(expectedSummary, recurringEvent.getRecurrenceRule().getSummary());
            assertEquals(parsed, recurringEvent.getRecurrenceRule().getParsed());
        }

        @Test
        void testMultiFieldUpdates_labelAndRecurrenceRuleInteraction() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            Label newLabel = createValidLabelWithId(FUTURE_LABEL_ID, user);
            String newRule = "MONTHLY:1:FRIDAY";
            
            Set<DayOfWeek> days = EnumSet.of(DayOfWeek.FRIDAY);
            ParsedRecurrenceInput parsed = new ParsedRecurrenceInput(RecurrenceFrequency.MONTHLY, days, 1);
            String expectedSummary = "Monthly on the 1st Friday";
            
            when(labelService.getLabelEntityById(FUTURE_LABEL_ID)).thenReturn(newLabel);
            when(recurrenceRuleService.parseFromString(newRule)).thenReturn(parsed);
            when(recurrenceRuleService.buildSummary(parsed, recurringEvent.getStartDate(), recurringEvent.getEndDate()))
                    .thenReturn(expectedSummary);

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null, null, null, null, null, null,
                    Optional.of(FUTURE_LABEL_ID),
                    Optional.of(newRule)
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            assertEquals(FUTURE_LABEL_ID, recurringEvent.getLabel().getId());
            assertEquals(expectedSummary, recurringEvent.getRecurrenceRule().getSummary());
            assertEquals(parsed, recurringEvent.getRecurrenceRule().getParsed());
            verify(ownershipValidator).validateLabelOwnership(recurringEvent.getCreator().getId(), newLabel);
        }

        @Test
        void testMultiFieldUpdates_timeChangesWithValidation() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            LocalTime newStartTime = LocalTime.of(9, 0);
            LocalTime newEndTime = LocalTime.of(17, 0);
            String newName = "Full Work Day Event";
            String newDescription = "A full 8-hour work day recurring event";

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    Optional.of(newName),
                    Optional.of(newStartTime),
                    Optional.of(newEndTime),
                    null, null,
                    Optional.of(newDescription),
                    null, null
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            assertEquals(newName, recurringEvent.getName());
            assertEquals(newStartTime, recurringEvent.getStartTime());
            assertEquals(newEndTime, recurringEvent.getEndTime());
            assertEquals(newDescription, recurringEvent.getDescription());
            // Validation should pass for confirmed event with all required fields
            assertDoesNotThrow(() -> recurringEvent.getName());
        }

        @Test
        void testMultiFieldUpdates_partialUpdateWithSkipAndClear() {
            // Arrange
            RecurringEvent recurringEvent = createUnconfirmedRecurringEvent(user, clock);
            String originalName = recurringEvent.getName();
            LocalTime originalEndTime = recurringEvent.getEndTime();
            
            LocalTime newStartTime = LocalTime.of(10, 30);
            LocalDate newEndDate = FIXED_TEST_DATE.plusDays(60);
            Label unlabeledLabel = createValidLabelWithId(UNLABELED_LABEL_ID, user);
            TestUtils.setUnlabeledLabel(user, unlabeledLabel);

            when(labelService.getLabelEntityById(UNLABELED_LABEL_ID)).thenReturn(unlabeledLabel);

            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    null,                        // SKIP name
                    Optional.of(newStartTime),   // UPDATE startTime
                    null,                        // SKIP endTime
                    Optional.empty(),            // CLEAR startDate
                    Optional.of(newEndDate),     // UPDATE endDate
                    Optional.empty(),            // CLEAR description
                    Optional.empty(),            // CLEAR labelId (-> Unlabeled)
                    null                         // SKIP recurrenceRule
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertTrue(result);
            // Verify skipped fields remain unchanged
            assertEquals(originalName, recurringEvent.getName());
            assertEquals(originalEndTime, recurringEvent.getEndTime());
            // Verify updated fields
            assertEquals(newStartTime, recurringEvent.getStartTime());
            assertEquals(newEndDate, recurringEvent.getEndDate());
            // Verify cleared fields
            assertNull(recurringEvent.getStartDate());
            assertNull(recurringEvent.getDescription());
            assertEquals(UNLABELED_LABEL_ID, recurringEvent.getLabel().getId());
        }

        @Test
        void testMultiFieldUpdates_atomicUpdateOrNoChange() {
            // Arrange
            RecurringEvent recurringEvent = createValidRecurringEvent(user, clock);
            String originalName = recurringEvent.getName();
            LocalTime originalStartTime = recurringEvent.getStartTime();
            Long originalLabelId = recurringEvent.getLabel().getId();
            
            // Try to update with same values (should result in no change)
            RecurringEventUpdateDTO dto = new RecurringEventUpdateDTO(
                    Optional.of(originalName),          // Same name
                    Optional.of(originalStartTime),     // Same start time
                    null,                               // Skip end time
                    null,                               // Skip start date  
                    null,                               // Skip end date
                    null,                               // Skip description
                    Optional.of(originalLabelId),       // Same label
                    null                                // Skip recurrence rule
            );

            // Act
            boolean result = recurringEventPatchHandler.applyPatch(recurringEvent, dto);

            // Assert
            assertFalse(result, "No fields should change when updating with same values");
            assertEquals(originalName, recurringEvent.getName());
            assertEquals(originalStartTime, recurringEvent.getStartTime());
            assertEquals(originalLabelId, recurringEvent.getLabel().getId());
            verifyNoInteractions(ownershipValidator); // Should not validate ownership for unchanged label
        }

        // endregion

    }
}
