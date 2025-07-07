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

public class RecurringEventPatchHandlerTest {

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
        ZonedDateTime fixedTime = ZonedDateTime.now(userZone);

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
                    null,
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
                    null,
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
                    null,
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
                    null,
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
                    null,
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
                    null,
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
                    Optional.of(newRule),
                    null
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
                    Optional.of(currentRule),
                    null
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
                    Optional.ofNullable(null),
                    null
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
                    Optional.of(equivalentRule),
                    null
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
                    Optional.of(currentRaw),
                    null
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
                    Optional.of(invalidRule),
                    null
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
                    Optional.of(newRecurrenceRule),
                    null
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

    }
}
