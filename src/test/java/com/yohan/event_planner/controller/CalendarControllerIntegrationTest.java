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


    }
}