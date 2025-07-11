package com.yohan.event_planner.validation;

import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.RecurrenceRuleVO;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.exception.ConflictException;
import com.yohan.event_planner.repository.EventRepository;
import com.yohan.event_planner.repository.RecurringEventRepository;
import com.yohan.event_planner.service.RecurrenceRuleService;
import com.yohan.event_planner.time.TimeUtils;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.yohan.event_planner.util.TestConstants.EVENT_ID;
import static com.yohan.event_planner.util.TestConstants.VALID_RECURRING_EVENT_ID;
import static com.yohan.event_planner.util.TestConstants.VALID_TIMEZONE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConflictValidatorImplTest {

    private static final LocalDate FIXED_TEST_DATE = LocalDate.of(2025, 6, 29);

    private EventRepository eventRepository;
    private RecurringEventRepository recurringEventRepository;
    private RecurrenceRuleService recurrenceRuleService;
    private ConflictValidatorImpl conflictValidator;

    private User user;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        recurringEventRepository = mock(RecurringEventRepository.class);
        recurrenceRuleService = mock(RecurrenceRuleService.class);
        conflictValidator = new ConflictValidatorImpl(eventRepository, recurringEventRepository, recurrenceRuleService);

        user = TestUtils.createValidUserEntityWithId();
        fixedClock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneOffset.UTC);
    }

    @Nested
    class ValidateNoConflictsEventTests {

        @Test
        void passesWhenNoConflicts() {
            // Arrange
            Event event = TestUtils.createValidScheduledEventWithId(EVENT_ID, user, fixedClock);

            when(eventRepository.findConflictingEventIds(any(), any(), any(), any()))
                    .thenReturn(Collections.emptySet());

            when(recurringEventRepository.findPotentialConflictingRecurringEvents(anyLong(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            // Act + Assert
            assertDoesNotThrow(() -> conflictValidator.validateNoConflicts(event));
        }

        @Test
        void throwsConflictExceptionWhenNormalEventConflictExists() {
            // Arrange
            Event event = TestUtils.createValidScheduledEventWithId(EVENT_ID, user, fixedClock);
            Set<Long> conflicts = Set.of(999L);

            when(eventRepository.findConflictingEventIds(any(), any(), any(), any()))
                    .thenReturn(conflicts);

            // Act + Assert
            ConflictException ex = assertThrows(ConflictException.class,
                    () -> conflictValidator.validateNoConflicts(event));

            assertTrue(ex.getMessage().contains("conflicts with events"));
        }

        @Test
        void throwsConflictExceptionWhenRecurringEventConflictExists() {
            // Arrange
            Event event = TestUtils.createValidScheduledEventWithId(EVENT_ID, user, fixedClock);
            RecurringEvent recurringEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);


            when(eventRepository.findConflictingEventIds(any(), any(), any(), any()))
                    .thenReturn(Collections.emptySet());

            when(recurringEventRepository.findPotentialConflictingRecurringEvents(anyLong(), any(), any(), any()))
                    .thenReturn(List.of(recurringEvent));

            // Return the same date as the event to ensure there's an actual conflict
            LocalDate eventDate = event.getStartTime().withZoneSameInstant(ZoneId.of(user.getTimezone())).toLocalDate();
            when(recurrenceRuleService.expandRecurrence(
                    eq(recurringEvent.getRecurrenceRule().getParsed()),
                    eq(eventDate),
                    eq(eventDate),
                    eq(recurringEvent.getSkipDays())
            )).thenReturn(List.of(eventDate));

            // Act + Assert
            assertThrows(ConflictException.class, () -> conflictValidator.validateNoConflicts(event));
        }

        @Test
        void validateNoConflicts_multiDayEventDetectsRecurringConflict() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            ZonedDateTime startTime = ZonedDateTime.of(FIXED_TEST_DATE, LocalTime.of(22, 0), ZoneId.of(VALID_TIMEZONE));
            ZonedDateTime endTime = startTime.plusDays(1).withHour(6);

            Event multiDayEvent = TestUtils.createValidScheduledEventWithId(EVENT_ID, creator, fixedClock);
            multiDayEvent.setStartTime(startTime);
            multiDayEvent.setEndTime(endTime);

            RecurringEvent recurringEvent = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);
            recurringEvent.setStartTime(LocalTime.of(5, 0)); // ensure overlap with multiDayEvent end segment
            recurringEvent.setEndTime(LocalTime.of(7, 0));

            // Mock no normal event conflicts
            when(eventRepository.findConflictingEventIds(any(), any(), any(), any()))
                    .thenReturn(Collections.emptySet());

            // Mock recurring event repo
            when(recurringEventRepository.findConfirmedRecurringEventsForUserBetween(any(), any(), any()))
                    .thenReturn(List.of(recurringEvent));

            // Mock recurrence expansion to include an overlapping date
            when(recurrenceRuleService.expandRecurrence(any(), any(), any(), any()))
                    .thenReturn(List.of(startTime.toLocalDate(), endTime.toLocalDate())); // both dates for clarity

            // Act + Assert
            assertThrows(ConflictException.class, () -> conflictValidator.validateNoConflicts(multiDayEvent));
        }

        @Test
        void validateNoConflicts_multiDayEventNoTimeOverlap_doesNotThrow() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            ZonedDateTime startTime = ZonedDateTime.of(FIXED_TEST_DATE, LocalTime.of(22, 0), ZoneId.of(VALID_TIMEZONE));
            ZonedDateTime endTime = startTime.plusDays(1).withHour(6);

            Event multiDayEvent = TestUtils.createValidScheduledEventWithId(EVENT_ID, creator, fixedClock);
            multiDayEvent.setStartTime(startTime);
            multiDayEvent.setEndTime(endTime);

            RecurringEvent recurringEvent = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);
            recurringEvent.setStartTime(LocalTime.of(7,0));
            recurringEvent.setEndTime(LocalTime.of(8,0));

            when(eventRepository.findConflictingEventIds(any(), any(), any(), any()))
                    .thenReturn(Collections.emptySet());
            when(recurringEventRepository.findConfirmedRecurringEventsForUserBetween(any(), any(), any()))
                    .thenReturn(List.of(recurringEvent));
            when(recurrenceRuleService.expandRecurrence(any(), any(), any(), any()))
                    .thenReturn(List.of(startTime.toLocalDate()));

            // Act + Assert
            assertDoesNotThrow(() -> conflictValidator.validateNoConflicts(multiDayEvent));
        }

        @Test
        void validateNoConflicts_normalEventOvernightOverlapsRecurringEvent_throwsConflict() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            ZonedDateTime startTime = ZonedDateTime.of(FIXED_TEST_DATE, LocalTime.of(22, 0), ZoneId.of(VALID_TIMEZONE));
            ZonedDateTime endTime = startTime.plusHours(10); // Ends next day at 8 AM

            Event overnightEvent = TestUtils.createValidScheduledEventWithId(EVENT_ID, creator, fixedClock);
            overnightEvent.setStartTime(startTime);
            overnightEvent.setEndTime(endTime);

            RecurringEvent recurringEvent = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);
            recurringEvent.setStartTime(LocalTime.of(7, 0)); // 7 AM
            recurringEvent.setEndTime(LocalTime.of(9, 0));   // 9 AM

            when(eventRepository.findConflictingEventIds(any(), any(), any(), any()))
                    .thenReturn(Collections.emptySet());
            when(recurringEventRepository.findConfirmedRecurringEventsForUserBetween(any(), any(), any()))
                    .thenReturn(List.of(recurringEvent));
            when(recurrenceRuleService.expandRecurrence(any(), any(), any(), any()))
                    .thenReturn(List.of(endTime.toLocalDate()));

            // Act + Assert
            assertThrows(ConflictException.class, () -> conflictValidator.validateNoConflicts(overnightEvent));
        }

    }

    @Nested
    class ValidateNoConflictsRecurringEventTests {

        @Test
        void passesWhenNoConflicts() {
            // Arrange
            RecurringEvent recurringEvent = TestUtils.createValidRecurringEventWithId(user, VALID_RECURRING_EVENT_ID, fixedClock);

            when(recurringEventRepository.findOverlappingRecurringEvents(anyLong(), any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            // Act + Assert
            assertDoesNotThrow(() -> conflictValidator.validateNoConflicts(recurringEvent));
        }

        @Test
        void throwsConflictExceptionWhenInfiniteVsInfiniteConflict() {
            // Arrange
            RecurringEvent newEvent = TestUtils.createValidRecurringEventWithId(user, 201L, fixedClock);
            newEvent.setEndDate(TimeUtils.FAR_FUTURE_DATE);

            RecurringEvent existingEvent = TestUtils.createValidRecurringEventWithId(user, 202L, fixedClock);
            existingEvent.setEndDate(TimeUtils.FAR_FUTURE_DATE);

            when(recurringEventRepository.findOverlappingRecurringEvents(anyLong(), any(), any(), any(), any()))
                    .thenReturn(List.of(existingEvent));

            when(recurrenceRuleService.expandRecurrence(any(), any(), any(), any()))
                    .thenReturn(List.of(LocalDate.now(fixedClock)));

            // Act + Assert
            assertThrows(ConflictException.class, () -> conflictValidator.validateNoConflicts(newEvent));
        }

        @Test
        void throwsConflictExceptionWhenOverlapDetectedWithin31Days() {
            // Arrange
            RecurringEvent newEvent = TestUtils.createValidRecurringEventWithId(user, 301L, fixedClock);
            RecurringEvent existingEvent = TestUtils.createValidRecurringEventWithId(user, 302L, fixedClock);

            when(recurringEventRepository.findOverlappingRecurringEvents(anyLong(), any(), any(), any(), any()))
                    .thenReturn(List.of(existingEvent));

            when(recurrenceRuleService.expandRecurrence(any(), any(), any(), any()))
                    .thenReturn(List.of(LocalDate.now(fixedClock)));

            // Act + Assert
            assertThrows(ConflictException.class, () -> conflictValidator.validateNoConflicts(newEvent));
        }

        @Test
        void skipsSelfWhenUpdating() {
            // Arrange
            RecurringEvent newEvent = TestUtils.createValidRecurringEventWithId(user, 401L, fixedClock);
            RecurringEvent existingEvent = TestUtils.createValidRecurringEventWithId(user, 401L, fixedClock); // same ID

            when(recurringEventRepository.findOverlappingRecurringEvents(anyLong(), any(), any(), any(), any()))
                    .thenReturn(List.of(existingEvent));

            // Act + Assert
            assertDoesNotThrow(() -> conflictValidator.validateNoConflicts(newEvent));
        }

        @Test
        void validateNoConflicts_recurringEventsNoSharedDays_doesNotThrow() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            RecurringEvent newEvent = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID + 1, fixedClock);
            RecurringEvent existingEvent = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);

            when(recurringEventRepository.findOverlappingRecurringEvents(any(), any(), any(), any(), any()))
                    .thenReturn(List.of(existingEvent));

            // Replace recurrence rules using factory
            newEvent.setRecurrenceRule(
                    TestUtils.createValidWeeklyRecurrenceRuleVO(Set.of(DayOfWeek.MONDAY), fixedClock)
            );
            existingEvent.setRecurrenceRule(
                    TestUtils.createValidWeeklyRecurrenceRuleVO(Set.of(DayOfWeek.TUESDAY), fixedClock)
            );

            // Act + Assert
            assertDoesNotThrow(() -> conflictValidator.validateNoConflicts(newEvent));
        }

        @Test
        void validateNoConflicts_recurringEventsNoDateOverlap_doesNotThrow() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            RecurringEvent newEvent = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID + 1, fixedClock);
            RecurringEvent existingEvent = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);

            newEvent.setStartDate(FIXED_TEST_DATE.plusDays(10));
            newEvent.setEndDate(FIXED_TEST_DATE.plusDays(15));
            existingEvent.setStartDate(FIXED_TEST_DATE);
            existingEvent.setEndDate(FIXED_TEST_DATE.plusDays(5));

            when(recurringEventRepository.findOverlappingRecurringEvents(any(), any(), any(), any(), any()))
                    .thenReturn(List.of(existingEvent));

            // Act + Assert
            assertDoesNotThrow(() -> conflictValidator.validateNoConflicts(newEvent));
        }

        @Test
        void validateNoConflicts_recurringEventsOverlapMoreThan31Days_capsWindow() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            RecurringEvent newEvent = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID + 1, fixedClock);
            RecurringEvent existingEvent = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);

            LocalDate startDate = FIXED_TEST_DATE;
            LocalDate endDate = startDate.plusDays(60);

            newEvent.setStartDate(startDate);
            newEvent.setEndDate(endDate);
            existingEvent.setStartDate(startDate);
            existingEvent.setEndDate(endDate);

            // Use factory to set recurrence rules with same day (e.g. MONDAY) to trigger conflict logic
            RecurrenceRuleVO recurrenceRuleVO = TestUtils.createValidWeeklyRecurrenceRuleVO(Set.of(DayOfWeek.MONDAY), fixedClock);
            newEvent.setRecurrenceRule(recurrenceRuleVO);
            existingEvent.setRecurrenceRule(recurrenceRuleVO);

            when(recurringEventRepository.findOverlappingRecurringEvents(any(), any(), any(), any(), any()))
                    .thenReturn(List.of(existingEvent));

            // Mock recurrence expansion to return at least one overlapping date within capped 31-day window
            when(recurrenceRuleService.expandRecurrence(any(), any(), any(), any()))
                    .thenReturn(List.of(LocalDate.now(fixedClock)));

            // Act + Assert
            assertThrows(ConflictException.class, () -> conflictValidator.validateNoConflicts(newEvent));

            // Verify expansion capped to 31 days (optional assertion if you want to ensure capped logic)
            verify(recurrenceRuleService, atLeastOnce()).expandRecurrence(
                    any(), eq(startDate), eq(startDate.plusDays(31)), any()
            );
        }

        @Test
        void validateNoConflicts_recurringEventSelfUpdate_doesNotThrow() {
            User creator = TestUtils.createValidUserEntityWithId();
            RecurringEvent recurringEvent = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);

            when(recurringEventRepository.findOverlappingRecurringEvents(any(), any(), any(), any(), any()))
                    .thenReturn(List.of(recurringEvent)); // includes self

            assertDoesNotThrow(() -> conflictValidator.validateNoConflicts(recurringEvent));
        }

        @Test
        void validateNoConflicts_infiniteVsFiniteRecurringEventWithSharedDay_throwsConflict() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            RecurrenceRuleVO recurrenceRuleVO = TestUtils.createValidWeeklyRecurrenceRuleVO(Set.of(DayOfWeek.MONDAY), fixedClock);

            // Infinite event with far future end date
            RecurringEvent infiniteEvent = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID + 1, fixedClock);
            infiniteEvent.setEndDate(TimeUtils.FAR_FUTURE_DATE); // Infinite
            infiniteEvent.setStartDate(FIXED_TEST_DATE);
            infiniteEvent.setRecurrenceRule(recurrenceRuleVO);

            // Finite event with a specific end date
            RecurringEvent finiteEvent = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);
            finiteEvent.setStartDate(FIXED_TEST_DATE);
            finiteEvent.setEndDate(FIXED_TEST_DATE.plusDays(10)); // Overlapping finite range
            finiteEvent.setRecurrenceRule(recurrenceRuleVO);

            // Mock the repository to return the finite event as an overlapping event
            when(recurringEventRepository.findOverlappingRecurringEvents(any(), any(), any(), any(), any()))
                    .thenReturn(List.of(finiteEvent));

            // Mock expandRecurrence to simulate overlapping dates
            when(recurrenceRuleService.expandRecurrence(any(), any(), any(), any()))
                    .thenReturn(List.of(LocalDate.now(fixedClock)));

            // Act + Assert
            assertThrows(ConflictException.class, () -> conflictValidator.validateNoConflicts(infiniteEvent));
        }

        @Test
        void validateNoConflicts_infiniteVsInfiniteWithSharedDays_throwsConflict() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();

            // Create a recurrence rule that occurs every Monday
            RecurrenceRuleVO recurrenceRuleVO = TestUtils.createValidWeeklyRecurrenceRuleVO(Set.of(DayOfWeek.MONDAY), fixedClock);

            // Infinite event 1 with far future end date
            RecurringEvent infiniteEvent1 = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID + 1, fixedClock);
            infiniteEvent1.setEndDate(TimeUtils.FAR_FUTURE_DATE);
            infiniteEvent1.setRecurrenceRule(recurrenceRuleVO);

            // Infinite event 2 with far future end date
            RecurringEvent infiniteEvent2 = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);
            infiniteEvent2.setEndDate(TimeUtils.FAR_FUTURE_DATE);
            infiniteEvent2.setRecurrenceRule(recurrenceRuleVO);

            // Mock repository to return the second infinite event as a candidate for overlap
            when(recurringEventRepository.findOverlappingRecurringEvents(any(), any(), any(), any(), any()))
                    .thenReturn(List.of(infiniteEvent2));

            // Act + Assert
            assertThrows(ConflictException.class, () -> conflictValidator.validateNoConflicts(infiniteEvent1));
        }

        @Test
        void validateNoConflicts_finiteVsInfiniteRecurringEventWithSharedDay_throwsConflict() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            RecurrenceRuleVO recurrenceRuleVO = TestUtils.createValidWeeklyRecurrenceRuleVO(Set.of(DayOfWeek.MONDAY), fixedClock);

            // Finite event with specific start and end dates
            RecurringEvent finiteEvent = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID + 1, fixedClock);
            finiteEvent.setStartDate(FIXED_TEST_DATE);
            finiteEvent.setEndDate(FIXED_TEST_DATE.plusDays(10)); // Finite event
            finiteEvent.setRecurrenceRule(recurrenceRuleVO);

            // Infinite event with far future end date
            RecurringEvent infiniteEvent = TestUtils.createValidRecurringEventWithId(creator, VALID_RECURRING_EVENT_ID, fixedClock);
            infiniteEvent.setStartDate(FIXED_TEST_DATE);
            infiniteEvent.setEndDate(TimeUtils.FAR_FUTURE_DATE); // Infinite event
            infiniteEvent.setRecurrenceRule(recurrenceRuleVO);

            // Mock repository to return the infinite event as a candidate for overlap
            when(recurringEventRepository.findOverlappingRecurringEvents(any(), any(), any(), any(), any()))
                    .thenReturn(List.of(infiniteEvent));

            // Mock expandRecurrence to simulate overlapping dates (both events are recurring on Mondays)
            when(recurrenceRuleService.expandRecurrence(any(), any(), any(), any()))
                    .thenReturn(List.of(LocalDate.now(fixedClock)));

            // Act + Assert
            assertThrows(ConflictException.class, () -> conflictValidator.validateNoConflicts(finiteEvent));
        }

    }



    @Nested
    class ValidateNoConflictsForSkipDaysTests {

        @Test
        void validateNoConflictsForSkipDays_NoConflictWhenSkipDaysDoNotOverlap() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            RecurrenceRuleVO recurrenceRuleVO = TestUtils.createValidWeeklyRecurrenceRuleVO(Set.of(DayOfWeek.MONDAY), fixedClock);

            RecurringEvent recurringEvent = TestUtils.createValidRecurringEventWithId(creator, 1L, fixedClock);
            recurringEvent.setStartDate(FIXED_TEST_DATE);
            recurringEvent.setEndDate(FIXED_TEST_DATE.plusDays(10));
            recurringEvent.setRecurrenceRule(recurrenceRuleVO);

            Set<LocalDate> skipDaysToRemove = Set.of(FIXED_TEST_DATE.plusDays(2)); // Skip day to remove

            // No conflicts, return empty list of overlapping events
            when(recurringEventRepository.findOverlappingRecurringEvents(any(), any(), any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            // Act + Assert
            assertDoesNotThrow(() -> conflictValidator.validateNoConflictsForSkipDays(recurringEvent, skipDaysToRemove));
        }

        @Test
        void validateNoConflictsForSkipDays_ThrowsConflictWhenSkipDaysOverlap() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            RecurrenceRuleVO recurrenceRuleVO = TestUtils.createValidWeeklyRecurrenceRuleVO(Set.of(DayOfWeek.MONDAY), fixedClock);

            RecurringEvent recurringEvent = TestUtils.createValidRecurringEventWithId(creator, 1L, fixedClock);
            recurringEvent.setStartDate(FIXED_TEST_DATE);
            recurringEvent.setEndDate(FIXED_TEST_DATE.plusDays(10));
            recurringEvent.setRecurrenceRule(recurrenceRuleVO);

            RecurringEvent conflictingEvent = TestUtils.createValidRecurringEventWithId(creator, 2L, fixedClock);
            conflictingEvent.setStartDate(FIXED_TEST_DATE);
            conflictingEvent.setEndDate(FIXED_TEST_DATE.plusDays(10));
            conflictingEvent.setRecurrenceRule(recurrenceRuleVO);

            Set<LocalDate> skipDaysToRemove = Set.of(FIXED_TEST_DATE.plusDays(2)); // Skip day to remove

            // Simulate a conflict with the conflicting event
            when(recurringEventRepository.findOverlappingRecurringEvents(any(), any(), any(), any(), any()))
                    .thenReturn(Collections.singletonList(conflictingEvent));

            // Mock occursOn to simulate that the skip day is part of the conflicting event's recurrence rule
            when(recurrenceRuleService.occursOn(any(), eq(FIXED_TEST_DATE.plusDays(2))))
                    .thenReturn(true); // The skip day should be part of the existing event's recurrence rule

            // Act + Assert
            assertThrows(ConflictException.class, () -> conflictValidator.validateNoConflictsForSkipDays(recurringEvent, skipDaysToRemove));
        }

        @Test
        void validateNoConflictsForSkipDays_SkipsSelfUpdateWhenCheckingForConflicts() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            RecurrenceRuleVO recurrenceRuleVO = TestUtils.createValidWeeklyRecurrenceRuleVO(Set.of(DayOfWeek.MONDAY), fixedClock);

            RecurringEvent recurringEvent = TestUtils.createValidRecurringEventWithId(creator, 1L, fixedClock);
            recurringEvent.setStartDate(FIXED_TEST_DATE);
            recurringEvent.setEndDate(FIXED_TEST_DATE.plusDays(10));
            recurringEvent.setRecurrenceRule(recurrenceRuleVO);

            Set<LocalDate> skipDaysToRemove = Set.of(FIXED_TEST_DATE.plusDays(2)); // Skip day to remove

            // Simulate no conflicts
            when(recurringEventRepository.findOverlappingRecurringEvents(any(), any(), any(), any(), any()))
                    .thenReturn(Collections.singletonList(recurringEvent)); // Return self in the conflict check

            // Mock expandRecurrence to simulate that the skip day is valid and doesn't cause a conflict
            when(recurrenceRuleService.expandRecurrence(any(), any(), any(), any()))
                    .thenReturn(List.of(FIXED_TEST_DATE.plusDays(2)));

            // Act + Assert
            assertDoesNotThrow(() -> conflictValidator.validateNoConflictsForSkipDays(recurringEvent, skipDaysToRemove));
        }

        @Test
        void validateNoConflictsForSkipDays_SkipsNoConflictIfNoSharedDays() {
            // Arrange
            User creator = TestUtils.createValidUserEntityWithId();
            RecurrenceRuleVO recurrenceRuleVO = TestUtils.createValidWeeklyRecurrenceRuleVO(Set.of(DayOfWeek.TUESDAY), fixedClock);

            RecurringEvent recurringEvent = TestUtils.createValidRecurringEventWithId(creator, 1L, fixedClock);
            recurringEvent.setStartDate(FIXED_TEST_DATE);
            recurringEvent.setEndDate(FIXED_TEST_DATE.plusDays(10));
            recurringEvent.setRecurrenceRule(recurrenceRuleVO);

            RecurringEvent conflictingEvent = TestUtils.createValidRecurringEventWithId(creator, 2L, fixedClock);
            conflictingEvent.setStartDate(FIXED_TEST_DATE);
            conflictingEvent.setEndDate(FIXED_TEST_DATE.plusDays(10));
            conflictingEvent.setRecurrenceRule(recurrenceRuleVO);

            Set<LocalDate> skipDaysToRemove = Set.of(FIXED_TEST_DATE.plusDays(2)); // Skip day to remove

            // Simulate no overlap due to different recurrence days
            when(recurringEventRepository.findOverlappingRecurringEvents(any(), any(), any(), any(), any()))
                    .thenReturn(Collections.singletonList(conflictingEvent));

            // Mock expandRecurrence to simulate no overlap
            when(recurrenceRuleService.expandRecurrence(any(), any(), any(), any()))
                    .thenReturn(List.of(FIXED_TEST_DATE.plusDays(3))); // No overlap with skip day

            // Act + Assert
            assertDoesNotThrow(() -> conflictValidator.validateNoConflictsForSkipDays(recurringEvent, skipDaysToRemove));
        }

    }

}
