package com.yohan.event_planner.service;

import com.yohan.event_planner.business.RecurringEventBO;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.LabelTimeBucket;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.LabelMonthStatsDTO;
import com.yohan.event_planner.repository.EventRepository;
import com.yohan.event_planner.repository.LabelRepository;
import com.yohan.event_planner.repository.LabelTimeBucketRepository;
import com.yohan.event_planner.security.AuthenticatedUserProvider;
import com.yohan.event_planner.security.OwnershipValidator;
import com.yohan.event_planner.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static com.yohan.event_planner.domain.enums.TimeBucketType.MONTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class MonthlyCalendarServiceImplTest {

    private RecurringEventBO recurringEventBO;
    private RecurrenceRuleService recurrenceRuleService;
    private LabelTimeBucketRepository labelTimeBucketRepository;
    private EventRepository eventRepository;
    private LabelRepository labelRepository;
    private AuthenticatedUserProvider authenticatedUserProvider;
    private OwnershipValidator ownershipValidator;

    private MonthlyCalendarServiceImpl monthlyCalendarService;

    @BeforeEach
    void setUp() {
        recurringEventBO = mock(RecurringEventBO.class);
        recurrenceRuleService = mock(RecurrenceRuleService.class);
        labelTimeBucketRepository = mock(LabelTimeBucketRepository.class);
        eventRepository = mock(EventRepository.class);
        labelRepository = mock(LabelRepository.class);
        authenticatedUserProvider = mock(AuthenticatedUserProvider.class);
        ownershipValidator = mock(OwnershipValidator.class);

        monthlyCalendarService = new MonthlyCalendarServiceImpl(
                recurringEventBO,
                recurrenceRuleService,
                labelTimeBucketRepository,
                eventRepository,
                labelRepository,
                authenticatedUserProvider,
                ownershipValidator
        );
    }

    @Nested
    class GetMonthlyBucketStatsTests {

        @Test
        void shouldReturnTotalStatsForValidLabelAndMonth() {
            // Arrange
            Long labelId = 1L;
            int year = 2025;
            int month = 6;

            // Create user and label via TestUtils
            User viewer = TestUtils.createValidUserEntityWithId();
            viewer.setTimezone("UTC"); // ✅ Set timezone directly for determinism

            Label label = TestUtils.createValidLabelWithId(labelId, "Strength Training", viewer);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(labelRepository.findById(labelId)).thenReturn(Optional.of(label));

            // Mock ownership validation (no exception thrown = valid)
            doNothing().when(ownershipValidator).validateLabelOwnership(viewer.getId(), label);

            // Mock LabelTimeBucket repository
            LabelTimeBucket mockBucket = TestUtils.createValidMonthBucket(
                    viewer.getId(),
                    labelId,
                    label.getName(),
                    year,
                    month,
                    120 // duration minutes
            );

            when(labelTimeBucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    viewer.getId(), labelId, MONTH, year, month)).thenReturn(Optional.of(mockBucket));

            // Define expected UTC start/end times (timezone is UTC so no conversion shift)
            ZonedDateTime startOfMonthUtc = ZonedDateTime.of(2025, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            ZonedDateTime endOfMonthUtc = ZonedDateTime.of(2025, 6, 30, 23, 59, 0, 0, ZoneOffset.UTC);

            // Mock count of completed events
            when(eventRepository.countByLabelIdAndEventDateBetweenAndIsCompleted(
                    labelId,
                    startOfMonthUtc,
                    endOfMonthUtc
            )).thenReturn(5L);

            // Act
            LabelMonthStatsDTO result = monthlyCalendarService.getMonthlyBucketStats(labelId, year, month);

            // Assert
            verify(eventRepository).countByLabelIdAndEventDateBetweenAndIsCompleted(
                    labelId,
                    startOfMonthUtc,
                    endOfMonthUtc
            );

            assertNotNull(result);
            assertEquals(5L, result.totalEvents());
            assertEquals(120L, result.totalTimeSpent());
            assertEquals("Strength Training", result.labelName());
        }

        @Test
        void shouldReturnZeroForMonthIfNoBucketFound() {
            // Arrange
            Long labelId = 1L;
            int year = 2025;
            int month = 6;

            // Create user and label via TestUtils
            User viewer = TestUtils.createValidUserEntityWithId();
            viewer.setTimezone("UTC"); // ✅ Set timezone directly for determinism

            Label label = TestUtils.createValidLabelWithId(labelId, "Strength Training", viewer);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(labelRepository.findById(labelId)).thenReturn(Optional.of(label));

            // Mock ownership validation
            doNothing().when(ownershipValidator).validateLabelOwnership(viewer.getId(), label);

            // Mock LabelTimeBucket repository to return empty
            when(labelTimeBucketRepository.findByUserIdAndLabelIdAndBucketTypeAndBucketYearAndBucketValue(
                    viewer.getId(), labelId, MONTH, year, month)).thenReturn(Optional.empty());

            // Define expected UTC start/end times (timezone is UTC so no conversion shift)
            ZonedDateTime startOfMonthUtc = ZonedDateTime.of(2025, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            ZonedDateTime endOfMonthUtc = ZonedDateTime.of(2025, 6, 30, 23, 59, 0, 0, ZoneOffset.UTC);

            // Mock count of completed events
            when(eventRepository.countByLabelIdAndEventDateBetweenAndIsCompleted(
                    labelId,
                    startOfMonthUtc,
                    endOfMonthUtc
            )).thenReturn(3L);

            // Act
            LabelMonthStatsDTO result = monthlyCalendarService.getMonthlyBucketStats(labelId, year, month);

            // Assert
            assertNotNull(result);
            assertEquals(3L, result.totalEvents());
            assertEquals(0L, result.totalTimeSpent());
            assertEquals("Strength Training", result.labelName());
        }

    }

    @Nested
    class GetDatesByLabelTests {

        @Test
        void shouldReturnUniqueEventDatesForLabelInGivenMonth() {
            // Arrange
            Long labelId = 1L;
            int year = 2025;
            int month = 6;

            User viewer = TestUtils.createValidUserEntityWithId();
            viewer.setTimezone("UTC");

            Label label = TestUtils.createValidLabelWithId(labelId, "Strength Training", viewer);

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);
            when(labelRepository.findById(labelId)).thenReturn(Optional.of(label));
            doNothing().when(ownershipValidator).validateLabelOwnership(viewer.getId(), label);

            ZonedDateTime startOfMonthUtc = ZonedDateTime.of(2025, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            ZonedDateTime endOfMonthUtc = ZonedDateTime.of(2025, 6, 30, 23, 59, 0, 0, ZoneOffset.UTC);

            Event event1 = Event.createEvent("Event 1",
                    ZonedDateTime.of(2025, 6, 10, 15, 0, 0, 0, ZoneOffset.UTC),
                    ZonedDateTime.of(2025, 6, 10, 18, 0, 0, 0, ZoneOffset.UTC),
                    viewer);
            event1.setLabel(label);
            event1.setCompleted(true);

            Event event2 = Event.createEvent("Event 2",
                    ZonedDateTime.of(2025, 6, 15, 10, 0, 0, 0, ZoneOffset.UTC),
                    ZonedDateTime.of(2025, 6, 15, 14, 0, 0, 0, ZoneOffset.UTC),
                    viewer);
            event2.setLabel(label);
            event2.setCompleted(true);

            Event event3 = Event.createEvent("Event 3",
                    ZonedDateTime.of(2025, 6, 17, 10, 0, 0, 0, ZoneOffset.UTC),
                    ZonedDateTime.of(2025, 6, 17, 12, 0, 0, 0, ZoneOffset.UTC),
                    viewer);
            event3.setLabel(label);
            event3.setCompleted(true);

            when(eventRepository.findByLabelIdAndEventDateBetweenAndIsCompleted(
                    labelId,
                    startOfMonthUtc,
                    endOfMonthUtc
            )).thenReturn(List.of(event1, event2, event3));

            // Act
            List<LocalDate> result = monthlyCalendarService.getDatesByLabel(labelId, year, month);

            // Assert
            assertNotNull(result);
            assertEquals(3, result.size());
            assertTrue(result.contains(LocalDate.of(2025, 6, 10)));
            assertTrue(result.contains(LocalDate.of(2025, 6, 15)));
            assertTrue(result.contains(LocalDate.of(2025, 6, 17)));
        }
    }

    @Nested
    class GetDatesWithEventsByMonthTests {

        @Test
        void shouldReturnCombinedDatesForScheduledAndRecurringEvents() {
            // Arrange
            int year = 2025;
            int month = 6;

            User viewer = TestUtils.createValidUserEntityWithId();
            viewer.setTimezone("UTC");
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);

            // region --- Mock scheduled events ---
            Event scheduledEvent1 = Event.createEvent(
                    "Scheduled Event 1",
                    ZonedDateTime.of(2025, 6, 5, 9, 0, 0, 0, ZoneOffset.UTC),
                    ZonedDateTime.of(2025, 6, 5, 10, 0, 0, 0, ZoneOffset.UTC),
                    viewer
            );
            scheduledEvent1.setLabel(TestUtils.createValidLabelWithId(1L, viewer));

            Event scheduledEvent2 = Event.createEvent(
                    "Scheduled Event 2",
                    ZonedDateTime.of(2025, 6, 10, 14, 0, 0, 0, ZoneOffset.UTC),
                    ZonedDateTime.of(2025, 6, 10, 16, 0, 0, 0, ZoneOffset.UTC),
                    viewer
            );
            scheduledEvent2.setLabel(TestUtils.createValidLabelWithId(2L, viewer));

            when(eventRepository.findConfirmedEventsForUserBetween(
                    eq(viewer.getId()),
                    any(ZonedDateTime.class),
                    any(ZonedDateTime.class)
            )).thenReturn(List.of(scheduledEvent1, scheduledEvent2));
            // endregion

            // region --- Mock recurring events ---
            RecurringEvent recurringEvent = TestUtils.createValidRecurringEvent(viewer, Clock.fixed(
                    ZonedDateTime.of(2025, 5, 30, 0, 0, 0, 0, ZoneOffset.UTC).toInstant(),
                    ZoneOffset.UTC
            ));

            recurringEvent.setStartDate(LocalDate.of(2025, 5, 1));
            recurringEvent.setEndDate(LocalDate.of(2025, 7, 31));

            when(recurringEventBO.getConfirmedRecurringEventsForUserInRange(
                    eq(viewer.getId()),
                    any(LocalDate.class),
                    any(LocalDate.class)
            )).thenReturn(List.of(recurringEvent));

            // Mock recurrenceRuleService to return true only for specific dates
            when(recurrenceRuleService.occursOn(any(), any(LocalDate.class)))
                    .thenReturn(false);
            when(recurrenceRuleService.occursOn(any(), eq(LocalDate.of(2025, 6, 15))))
                    .thenReturn(true);
            when(recurrenceRuleService.occursOn(any(), eq(LocalDate.of(2025, 6, 20))))
                    .thenReturn(true);
            // endregion

            // Act
            List<LocalDate> result = monthlyCalendarService.getDatesWithEventsByMonth(year, month);

            // Assert
            assertNotNull(result);
            assertTrue(result.contains(LocalDate.of(2025, 6, 5)));   // scheduled event
            assertTrue(result.contains(LocalDate.of(2025, 6, 10)));  // scheduled event
            assertTrue(result.contains(LocalDate.of(2025, 6, 15)));  // recurring event
            assertTrue(result.contains(LocalDate.of(2025, 6, 20)));  // recurring event
            assertEquals(4, result.size()); // should contain only these 4 unique dates
        }

        @Test
        void shouldReturnEmptyListWhenNoScheduledOrRecurringEvents() {
            // Arrange
            int year = 2025;
            int month = 6;

            User viewer = TestUtils.createValidUserEntityWithId();
            viewer.setTimezone("UTC");
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);

            // Mock event repository to return no scheduled events
            when(eventRepository.findConfirmedEventsForUserBetween(
                    eq(viewer.getId()),
                    any(ZonedDateTime.class),
                    any(ZonedDateTime.class)
            )).thenReturn(List.of());

            // Mock recurringEventBO to return no recurring events
            when(recurringEventBO.getConfirmedRecurringEventsForUserInRange(
                    eq(viewer.getId()),
                    any(LocalDate.class),
                    any(LocalDate.class)
            )).thenReturn(List.of());

            // Act
            List<LocalDate> result = monthlyCalendarService.getDatesWithEventsByMonth(year, month);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Result should be empty when no events exist for the month");
        }

        @Test
        void shouldIncludeBothStartAndEndDatesForMultiDayScheduledEvent() {
            // Arrange
            int year = 2025;
            int month = 6;

            User viewer = TestUtils.createValidUserEntityWithId();
            viewer.setTimezone("UTC");
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);

            // Create a scheduled event starting on May 31 and ending on June 1
            Event spanningEvent = Event.createEvent(
                    "Spanning Event",
                    ZonedDateTime.of(2025, 5, 31, 22, 0, 0, 0, ZoneOffset.UTC),
                    ZonedDateTime.of(2025, 6, 1, 2, 0, 0, 0, ZoneOffset.UTC),
                    viewer
            );
            spanningEvent.setLabel(TestUtils.createValidLabelWithId(1L, viewer));

            when(eventRepository.findConfirmedEventsForUserBetween(
                    eq(viewer.getId()),
                    any(ZonedDateTime.class),
                    any(ZonedDateTime.class)
            )).thenReturn(List.of(spanningEvent));

            // Mock recurringEventBO to return no recurring events
            when(recurringEventBO.getConfirmedRecurringEventsForUserInRange(
                    eq(viewer.getId()),
                    any(LocalDate.class),
                    any(LocalDate.class)
            )).thenReturn(List.of());

            // Act
            List<LocalDate> result = monthlyCalendarService.getDatesWithEventsByMonth(year, month);

            // Assert
            assertNotNull(result);
            assertTrue(result.contains(LocalDate.of(2025, 5, 31)), "Should include start date (May 31)");
            assertTrue(result.contains(LocalDate.of(2025, 6, 1)), "Should include end date (June 1)");
            assertEquals(2, result.size(), "Should contain both start and end dates");
        }

        @Test
        void shouldIncludeRecurringEventDatesWithinMonthRange() {
            // Arrange
            int year = 2025;
            int month = 6;

            User viewer = TestUtils.createValidUserEntityWithId();
            viewer.setTimezone("UTC");
            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);

            // No scheduled events for this test
            when(eventRepository.findConfirmedEventsForUserBetween(
                    eq(viewer.getId()),
                    any(ZonedDateTime.class),
                    any(ZonedDateTime.class)
            )).thenReturn(List.of());

            // Create a recurring event spanning May to July with weekly recurrence on Mondays and Fridays
            RecurringEvent recurringEvent = TestUtils.createValidRecurringEvent(viewer, Clock.fixed(
                    ZonedDateTime.of(2025, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant(),
                    ZoneOffset.UTC
            ));
            recurringEvent.setStartDate(LocalDate.of(2025, 5, 1));
            recurringEvent.setEndDate(LocalDate.of(2025, 7, 31));

            when(recurringEventBO.getConfirmedRecurringEventsForUserInRange(
                    eq(viewer.getId()),
                    any(LocalDate.class),
                    any(LocalDate.class)
            )).thenReturn(List.of(recurringEvent));

            // General fallback first
            when(recurrenceRuleService.occursOn(any(), any(LocalDate.class)))
                    .thenReturn(false);

            // Specific dates override fallback
            when(recurrenceRuleService.occursOn(any(), eq(LocalDate.of(2025, 6, 2))))
                    .thenReturn(true);
            when(recurrenceRuleService.occursOn(any(), eq(LocalDate.of(2025, 6, 6))))
                    .thenReturn(true);
            when(recurrenceRuleService.occursOn(any(), eq(LocalDate.of(2025, 6, 9))))
                    .thenReturn(true);

            // Act
            List<LocalDate> result = monthlyCalendarService.getDatesWithEventsByMonth(year, month);

            // Assert
            assertNotNull(result);
            assertTrue(result.contains(LocalDate.of(2025, 6, 2)), "Should include June 2");
            assertTrue(result.contains(LocalDate.of(2025, 6, 6)), "Should include June 6");
            assertTrue(result.contains(LocalDate.of(2025, 6, 9)), "Should include June 9");
            assertEquals(3, result.size(), "Should include exactly the 3 expected recurring dates");
        }

        @Test
        void shouldDefaultToCurrentYearMonthWhenNullProvided() {
            // Arrange
            User viewer = TestUtils.createValidUserEntityWithId();
            viewer.setTimezone("UTC");

            when(authenticatedUserProvider.getCurrentUser()).thenReturn(viewer);

            // Mock event repository to return a scheduled event
            Event event = Event.createEvent("Test Event",
                    ZonedDateTime.of(2025, 7, 10, 10, 0, 0, 0, ZoneOffset.UTC),
                    ZonedDateTime.of(2025, 7, 10, 12, 0, 0, 0, ZoneOffset.UTC),
                    viewer);
            when(eventRepository.findConfirmedEventsForUserBetween(
                    eq(viewer.getId()), any(), any())
            ).thenReturn(List.of(event));

            // Mock recurring event BO to return empty
            when(recurringEventBO.getConfirmedRecurringEventsForUserInRange(
                    eq(viewer.getId()), any(), any())
            ).thenReturn(List.of());

            // Act
            List<LocalDate> result = monthlyCalendarService.getDatesWithEventsByMonth(null, null);

            // Assert
            assertNotNull(result);
            assertFalse(result.isEmpty(), "Result should not be empty");
            assertTrue(result.contains(LocalDate.of(2025, 7, 10)), "Should include July 10");

            // Verify that eventRepository was called with any valid range
            verify(eventRepository).findConfirmedEventsForUserBetween(
                    eq(viewer.getId()), any(), any()
            );
        }

    }
}
