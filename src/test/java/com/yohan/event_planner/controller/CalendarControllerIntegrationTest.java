package com.yohan.event_planner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.Label;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.dto.EventUpdateDTO;
import com.yohan.event_planner.service.EventService;
import com.yohan.event_planner.util.TestAuthUtils;
import com.yohan.event_planner.util.TestConfig;
import com.yohan.event_planner.util.TestDataHelper;
import com.yohan.event_planner.util.TestUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import({TestConfig.class, com.yohan.event_planner.config.TestEmailConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class CalendarControllerIntegrationTest {

    @Autowired
    private EventService eventService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TestDataHelper testDataHelper;
    private Clock fixedClock;

    private String jwt;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        TestAuthUtils.AuthResult auth = testDataHelper.registerAndLoginUserWithUser("calendartest");
        this.jwt = auth.jwt();
        this.user = auth.user();

        fixedClock =  Clock.fixed(
                Instant.parse("2025-07-10T12:00:00Z"), ZoneOffset.UTC);
    }

    @Nested
    class GetMonthlyCalendarViewTests {

        @Test
        void shouldReturn401WhenNoAuthToken() throws Exception {
            mockMvc.perform(get("/calendar"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturn403WhenAccessingOthersLabel() throws Exception {
            // Arrange: create another user and their label
            TestAuthUtils.AuthResult otherAuth = testDataHelper.registerAndLoginUserWithUser("otheruser");
            Label otherUserLabel = testDataHelper.createAndPersistLabel(otherAuth.user(), "Other User Label");
            
            // Act + Assert: attempt to access other user's label should fail
            mockMvc.perform(get("/calendar")
                            .param("labelId", otherUserLabel.getId().toString())
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturnDatesWithEventsWhenNoLabelProvided() throws Exception {
            // Arrange: create and persist label for the user
            Label label = testDataHelper.createAndPersistLabel(user, "Test Label");

            // Arrange: create scheduled event in July
            LocalDate july10 = LocalDate.of(2025, 7, 10);
            ZonedDateTime start = july10.atTime(9, 0).atZone(ZoneId.of("UTC"));
            ZonedDateTime end = start.plusHours(1);

            Event event = TestUtils.createValidScheduledEvent(user, fixedClock);
            event.setLabel(label); // ensure event references the persisted label
            event.setStartTime(start);
            event.setEndTime(end);
            testDataHelper.saveAndFlush(event);

            // Act + Assert
            mockMvc.perform(get("/calendar")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventDates").isArray())
                    .andExpect(jsonPath("$.eventDates").isNotEmpty())
                    .andExpect(jsonPath("$.bucketMonthStats").doesNotExist());
        }

        @Test
        void shouldReturnEmptyWhenNoEventsExist() throws Exception {
            // Act + Assert
            mockMvc.perform(get("/calendar")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventDates").isArray())
                    .andExpect(jsonPath("$.eventDates").isEmpty())
                    .andExpect(jsonPath("$.bucketMonthStats").doesNotExist());
        }

        @Test
        void shouldReturnMonthlyStatsForLabel() throws Exception {
            // Arrange: fixed month and year (July 2025)
            int testMonth = 7;
            int testYear = 2025;

            // Create label for user
            Label label = testDataHelper.createAndPersistLabel(user, "Workout");

            // Create two incomplete events with that label, on July 1 and July 2, 2025
            Event event1 = TestUtils.createValidIncompletePastEvent(user, fixedClock);
            event1.setName("Event 1");
            event1.setStartTime(ZonedDateTime.of(testYear, testMonth, 1, 9, 0, 0, 0, ZoneOffset.UTC));
            event1.setEndTime(event1.getStartTime().plusHours(1));
            event1.setLabel(label);
            testDataHelper.saveAndFlush(event1);

            Event event2 = TestUtils.createValidIncompletePastEvent(user, fixedClock);
            event2.setName("Event 2");
            event2.setStartTime(ZonedDateTime.of(testYear, testMonth, 2, 14, 0, 0, 0, ZoneOffset.UTC));
            event2.setEndTime(event2.getStartTime().plusHours(1));
            event2.setLabel(label);
            testDataHelper.saveAndFlush(event2);

            // Act: PATCH both events to mark them complete
            EventUpdateDTO markComplete = new EventUpdateDTO(null, null, null, null, null, true);

            mockMvc.perform(patch("/events/{id}", event1.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(markComplete))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                    .andExpect(status().isOk());

            mockMvc.perform(patch("/events/{id}", event2.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(markComplete))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                    .andExpect(status().isOk());

            // Assert: GET /calendar returns both completed events
            mockMvc.perform(get("/calendar")
                            .param("labelId", label.getId().toString())
                            .param("month", String.valueOf(testMonth))
                            .param("year", String.valueOf(testYear))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bucketMonthStats.totalEvents").value(2))
                    .andExpect(jsonPath("$.eventDates").isArray())
                    .andExpect(jsonPath("$.eventDates").value(Matchers.containsInAnyOrder(
                            LocalDate.of(testYear, testMonth, 1).toString(),
                            LocalDate.of(testYear, testMonth, 2).toString()
                    )));
        }

        @Test
        void shouldHandleInvalidMonthParameter() throws Exception {
            // Act + Assert: invalid month should return 400
            mockMvc.perform(get("/calendar")
                            .param("month", "13")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldHandleInvalidYearParameter() throws Exception {
            // Act + Assert: invalid year should return 400
            mockMvc.perform(get("/calendar")
                            .param("year", "abc")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldDefaultToCurrentMonthWhenParametersNotProvided() throws Exception {
            // Arrange: create event for current month (July 2025 based on fixed clock)
            Label label = testDataHelper.createAndPersistLabel(user, "Current Month Label");
            
            LocalDate currentDate = LocalDate.of(2025, 7, 10); // Based on fixed clock
            ZonedDateTime start = currentDate.atTime(9, 0).atZone(ZoneId.of("UTC"));
            ZonedDateTime end = start.plusHours(1);

            Event event = TestUtils.createValidScheduledEvent(user, fixedClock);
            event.setLabel(label);
            event.setStartTime(start);
            event.setEndTime(end);
            testDataHelper.saveAndFlush(event);

            // Act + Assert: request without year/month should default to current month
            mockMvc.perform(get("/calendar")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventDates").isArray())
                    .andExpect(jsonPath("$.eventDates").isNotEmpty())
                    .andExpect(jsonPath("$.eventDates").value(Matchers.containsInAnyOrder(
                            currentDate.toString()
                    )));
        }

        @Test
        void shouldReturn404WhenLabelNotFound() throws Exception {
            // Act + Assert: non-existent label should return 404
            mockMvc.perform(get("/calendar")
                            .param("labelId", "999999")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturn400ForNegativeYear() throws Exception {
            // Act + Assert: negative year should return 400
            mockMvc.perform(get("/calendar")
                            .param("year", "-1")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400ForZeroYear() throws Exception {
            // Act + Assert: zero year should return 400
            mockMvc.perform(get("/calendar")
                            .param("year", "0")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400ForZeroMonth() throws Exception {
            // Act + Assert: zero month should return 400
            mockMvc.perform(get("/calendar")
                            .param("month", "0")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400ForNegativeMonth() throws Exception {
            // Act + Assert: negative month should return 400
            mockMvc.perform(get("/calendar")
                            .param("month", "-1")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400ForDecimalMonth() throws Exception {
            // Act + Assert: decimal month should return 400
            mockMvc.perform(get("/calendar")
                            .param("month", "1.5")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn400ForDecimalYear() throws Exception {
            // Act + Assert: decimal year should return 400
            mockMvc.perform(get("/calendar")
                            .param("year", "2025.5")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isBadRequest());
        }


        @Test
        void shouldHandleTimezoneCorrectlyForMidnightEvents() throws Exception {
            // Arrange: Set user timezone to EST (UTC-5 in July, actually EDT UTC-4)
            user.setTimezone("America/New_York");
            testDataHelper.saveAndFlush(user);
            
            Label label = testDataHelper.createAndPersistLabel(user, "Midnight Test Label");
            
            // Create event at 11:30 PM EDT on July 10, 2025 (3:30 AM UTC July 11)
            // This event should appear on July 10 in user's local timezone
            ZonedDateTime eventTimeEDT = ZonedDateTime.of(2025, 7, 10, 23, 30, 0, 0, ZoneId.of("America/New_York"));
            ZonedDateTime eventTimeUTC = eventTimeEDT.withZoneSameInstant(ZoneOffset.UTC);
            
            Event event = TestUtils.createValidScheduledEvent(user, fixedClock);
            event.setLabel(label);
            event.setStartTime(eventTimeUTC);
            event.setEndTime(eventTimeUTC.plusHours(1));
            testDataHelper.saveAndFlush(event);
            
            // Debug: Print the actual UTC time to understand what's happening
            System.out.println("Event time in EDT: " + eventTimeEDT);
            System.out.println("Event time in UTC: " + eventTimeUTC);
            
            // Act + Assert: event should appear on both July 10 and July 11 since it spans both dates in user's timezone
            // The service should convert the UTC event back to user's timezone and show all dates spanned
            mockMvc.perform(get("/calendar")
                            .param("year", "2025")
                            .param("month", "7")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventDates").isArray())
                    .andExpect(jsonPath("$.eventDates").value(Matchers.containsInAnyOrder(
                            LocalDate.of(2025, 7, 10).toString(),
                            LocalDate.of(2025, 7, 11).toString()
                    )));
        }

        @Test
        void shouldHandleDifferentTimezoneForSameEvent() throws Exception {
            // Arrange: Create event at specific UTC time
            Label label = testDataHelper.createAndPersistLabel(user, "Timezone Test Label");
            
            // Event at 2:00 AM UTC on July 11, 2025
            ZonedDateTime eventTimeUTC = ZonedDateTime.of(2025, 7, 11, 2, 0, 0, 0, ZoneOffset.UTC);
            
            Event event = TestUtils.createValidScheduledEvent(user, fixedClock);
            event.setLabel(label);
            event.setStartTime(eventTimeUTC);
            event.setEndTime(eventTimeUTC.plusHours(1));
            testDataHelper.saveAndFlush(event);
            
            // Test 1: User in UTC timezone should see event on July 11
            user.setTimezone("UTC");
            testDataHelper.saveAndFlush(user);
            
            mockMvc.perform(get("/calendar")
                            .param("year", "2025")
                            .param("month", "7")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventDates").value(Matchers.containsInAnyOrder(
                            LocalDate.of(2025, 7, 11).toString()
                    )));
            
            // Test 2: User in PST timezone should see event on July 10 (2 AM UTC = 7 PM PDT July 10)
            user.setTimezone("America/Los_Angeles");
            testDataHelper.saveAndFlush(user);
            
            mockMvc.perform(get("/calendar")
                            .param("year", "2025")
                            .param("month", "7")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventDates").value(Matchers.containsInAnyOrder(
                            LocalDate.of(2025, 7, 10).toString()
                    )));
        }

        @Test
        void shouldIncludeBothRegularAndRecurringEvents() throws Exception {
            // Arrange: Create label
            Label label = testDataHelper.createAndPersistLabel(user, "Mixed Events Label");
            
            // Create regular event on July 5, 2025
            Event regularEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            regularEvent.setLabel(label);
            regularEvent.setStartTime(ZonedDateTime.of(2025, 7, 5, 9, 0, 0, 0, ZoneOffset.UTC));
            regularEvent.setEndTime(regularEvent.getStartTime().plusHours(1));
            testDataHelper.saveAndFlush(regularEvent);
            
            // Create recurring event that occurs daily starting July 10, 2025
            var recurringEvent = testDataHelper.createAndPersistRecurringEvent(user, "Daily Recurring Event");
            recurringEvent.setLabel(label);
            recurringEvent.setUnconfirmed(false); // Make it confirmed so it shows up in calendar
            // Explicitly set start and end dates to match test expectations
            recurringEvent.setStartDate(LocalDate.of(2025, 7, 10)); // Start July 10, 2025
            recurringEvent.setEndDate(LocalDate.of(2025, 8, 9));   // End August 9, 2025
            testDataHelper.saveAndFlush(recurringEvent);
            
            // Act + Assert: calendar should include both regular and recurring events
            mockMvc.perform(get("/calendar")
                            .param("year", "2025")
                            .param("month", "7")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventDates").isArray())
                    .andExpect(jsonPath("$.eventDates").value(Matchers.hasSize(Matchers.greaterThan(1))))
                    .andExpect(jsonPath("$.eventDates").value(Matchers.hasItem(
                            LocalDate.of(2025, 7, 5).toString()  // Regular event
                    )))
                    .andExpect(jsonPath("$.eventDates").value(Matchers.hasItem(
                            LocalDate.of(2025, 7, 12).toString()  // Recurring event occurrence starts July 12 (Saturday)
                    )));
        }

        @Test
        void shouldHandleMultiDayEvents() throws Exception {
            // Arrange: Create label
            Label label = testDataHelper.createAndPersistLabel(user, "Multi-Day Events Label");
            
            // Create event spanning July 10-12, 2025
            Event multiDayEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            multiDayEvent.setLabel(label);
            multiDayEvent.setStartTime(ZonedDateTime.of(2025, 7, 10, 9, 0, 0, 0, ZoneOffset.UTC));
            multiDayEvent.setEndTime(ZonedDateTime.of(2025, 7, 12, 17, 0, 0, 0, ZoneOffset.UTC));
            testDataHelper.saveAndFlush(multiDayEvent);
            
            // Act + Assert: calendar should include all dates the event spans
            mockMvc.perform(get("/calendar")
                            .param("year", "2025")
                            .param("month", "7")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventDates").isArray())
                    .andExpect(jsonPath("$.eventDates").value(Matchers.hasItem(
                            LocalDate.of(2025, 7, 10).toString()  // Start date
                    )))
                    .andExpect(jsonPath("$.eventDates").value(Matchers.hasItem(
                            LocalDate.of(2025, 7, 12).toString()  // End date
                    )));
        }

        @Test
        void shouldCalculateStatsCorrectlyForMultipleEvents() throws Exception {
            // Arrange: Create label
            Label label = testDataHelper.createAndPersistLabel(user, "Stats Test Label");
            
            // Create 3 incomplete events with specific durations
            Event event1 = TestUtils.createValidIncompletePastEvent(user, fixedClock);
            event1.setLabel(label);
            event1.setStartTime(ZonedDateTime.of(2025, 7, 1, 9, 0, 0, 0, ZoneOffset.UTC));
            event1.setEndTime(event1.getStartTime().plusHours(2)); // 2 hours
            testDataHelper.saveAndFlush(event1);
            
            Event event2 = TestUtils.createValidIncompletePastEvent(user, fixedClock);
            event2.setLabel(label);
            event2.setStartTime(ZonedDateTime.of(2025, 7, 5, 14, 0, 0, 0, ZoneOffset.UTC));
            event2.setEndTime(event2.getStartTime().plusMinutes(90)); // 1.5 hours
            testDataHelper.saveAndFlush(event2);
            
            Event event3 = TestUtils.createValidIncompletePastEvent(user, fixedClock);
            event3.setLabel(label);
            event3.setStartTime(ZonedDateTime.of(2025, 7, 8, 10, 0, 0, 0, ZoneOffset.UTC));
            event3.setEndTime(event3.getStartTime().plusMinutes(30)); // 0.5 hours
            testDataHelper.saveAndFlush(event3);
            
            // Complete all events
            EventUpdateDTO markComplete = new EventUpdateDTO(null, null, null, null, null, true);
            
            for (Event event : List.of(event1, event2, event3)) {
                mockMvc.perform(patch("/events/{id}", event.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(markComplete))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt))
                        .andExpect(status().isOk());
            }
            
            // Act + Assert: verify stats show correct event count
            mockMvc.perform(get("/calendar")
                            .param("labelId", label.getId().toString())
                            .param("year", "2025")
                            .param("month", "7")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bucketMonthStats.totalEvents").value(3))
                    .andExpect(jsonPath("$.eventDates").isArray())
                    .andExpect(jsonPath("$.eventDates").value(Matchers.hasSize(3)))
                    .andExpect(jsonPath("$.eventDates").value(Matchers.containsInAnyOrder(
                            LocalDate.of(2025, 7, 1).toString(),
                            LocalDate.of(2025, 7, 5).toString(),
                            LocalDate.of(2025, 7, 8).toString()
                    )));
        }

        @Test
        void shouldReturnZeroStatsForLabelWithNoCompletedEvents() throws Exception {
            // Arrange: Create label
            Label label = testDataHelper.createAndPersistLabel(user, "No Completed Events Label");
            
            // Create incomplete event (not completed)
            Event incompleteEvent = TestUtils.createValidIncompletePastEvent(user, fixedClock);
            incompleteEvent.setLabel(label);
            incompleteEvent.setStartTime(ZonedDateTime.of(2025, 7, 15, 9, 0, 0, 0, ZoneOffset.UTC));
            incompleteEvent.setEndTime(incompleteEvent.getStartTime().plusHours(1));
            testDataHelper.saveAndFlush(incompleteEvent);
            
            // Act + Assert: stats should show 0 events since none are completed
            mockMvc.perform(get("/calendar")
                            .param("labelId", label.getId().toString())
                            .param("year", "2025")
                            .param("month", "7")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bucketMonthStats.totalEvents").value(0))
                    .andExpect(jsonPath("$.eventDates").isArray())
                    .andExpect(jsonPath("$.eventDates").isEmpty());
        }

        @Test
        void shouldHandleEventsSpanningMonthBoundaries() throws Exception {
            // Arrange: Create label
            Label label = testDataHelper.createAndPersistLabel(user, "Month Boundary Label");
            
            // Create event starting in June and ending in July
            Event crossMonthEvent = TestUtils.createValidScheduledEvent(user, fixedClock);
            crossMonthEvent.setLabel(label);
            crossMonthEvent.setStartTime(ZonedDateTime.of(2025, 6, 30, 20, 0, 0, 0, ZoneOffset.UTC));
            crossMonthEvent.setEndTime(ZonedDateTime.of(2025, 7, 1, 4, 0, 0, 0, ZoneOffset.UTC));
            testDataHelper.saveAndFlush(crossMonthEvent);
            
            
            // Test June view - should include the event (start date is June 30)
            mockMvc.perform(get("/calendar")
                            .param("year", "2025")
                            .param("month", "6")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventDates").isArray())
                    .andExpect(jsonPath("$.eventDates").value(Matchers.hasItem(
                            LocalDate.of(2025, 6, 30).toString()
                    )));
            
            // Test July view - should also include the event (end date is July 1)
            mockMvc.perform(get("/calendar")
                            .param("year", "2025")
                            .param("month", "7")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventDates").isArray())
                    .andExpect(jsonPath("$.eventDates").value(Matchers.hasItem(
                            LocalDate.of(2025, 7, 1).toString()
                    )));
        }

    }

    @Nested
    class PerformanceTests {

        @Test
        void shouldHandleLargeMonthResponse() throws Exception {
            // Arrange: create many events for one month
            Label label = testDataHelper.createAndPersistLabel(user, "Performance Test Label");
            
            // Create 50 events across July 2025
            for (int day = 1; day <= 30; day++) {
                LocalDate eventDate = LocalDate.of(2025, 7, day);
                ZonedDateTime start = eventDate.atTime(9, 0).atZone(ZoneId.of("UTC"));
                ZonedDateTime end = start.plusHours(1);

                Event event = TestUtils.createValidScheduledEvent(user, fixedClock);
                event.setLabel(label);
                event.setStartTime(start);
                event.setEndTime(end);
                testDataHelper.saveAndFlush(event);
            }

            // Act + Assert: should handle large response efficiently
            mockMvc.perform(get("/calendar")
                            .param("year", "2025")
                            .param("month", "7")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventDates").isArray())
                    .andExpect(jsonPath("$.eventDates.length()").value(30));
        }

    }
}