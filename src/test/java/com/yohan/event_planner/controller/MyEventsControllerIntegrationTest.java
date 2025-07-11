package com.yohan.event_planner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yohan.event_planner.domain.Event;
import com.yohan.event_planner.domain.RecurringEvent;
import com.yohan.event_planner.domain.User;
import com.yohan.event_planner.repository.EventRepository;
import com.yohan.event_planner.repository.RecurringEventRepository;
import com.yohan.event_planner.util.TestConfig;
import com.yohan.event_planner.util.TestDataHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import({TestConfig.class, com.yohan.event_planner.config.TestEmailConfig.class})
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class MyEventsControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestDataHelper testDataHelper;
    @Autowired private EventRepository eventRepository;
    @Autowired private RecurringEventRepository recurringEventRepository;

    private String jwt;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        var auth = testDataHelper.registerAndLoginUserWithUser("myevents");
        this.jwt = auth.jwt();
        this.user = auth.user();
    }

    @Nested
    class GetMyEventsTests {

        @Test
        void testGetMyEvents_ShouldReturnConfirmedEventsAndRecurringEvents() throws Exception {
            // Arrange
            Event confirmedEvent = testDataHelper.createAndPersistScheduledEvent(user, "Confirmed Event");
            RecurringEvent confirmedRecurringEvent = testDataHelper.createAndPersistRecurringEvent(user, "Confirmed Recurring Event");

            // Act + Assert
            mockMvc.perform(get("/myevents")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events").isArray())
                    .andExpect(jsonPath("$.events[0].id").value(confirmedEvent.getId()))
                    .andExpect(jsonPath("$.recurringEvents").isArray())
                    .andExpect(jsonPath("$.recurringEvents[0].id").value(confirmedRecurringEvent.getId()));
        }

        @Test
        void testUnauthorizedGetMyEvents_ShouldReturnUnauthorized() throws Exception {
            mockMvc.perform(get("/myevents"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testGetMyEvents_ReturnsEmptyListsWhenNoData() throws Exception {
            mockMvc.perform(get("/myevents")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events").isArray())
                    .andExpect(jsonPath("$.events").isEmpty())
                    .andExpect(jsonPath("$.recurringEvents").isArray())
                    .andExpect(jsonPath("$.recurringEvents").isEmpty());
        }

        @Test
        void testGetMyDrafts_ReturnsEmptyListsWhenNoDrafts() throws Exception {
            mockMvc.perform(get("/myevents/drafts")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventDrafts").isArray())
                    .andExpect(jsonPath("$.eventDrafts").isEmpty())
                    .andExpect(jsonPath("$.recurringEventDrafts").isArray())
                    .andExpect(jsonPath("$.recurringEventDrafts").isEmpty());
        }

    }

    @Nested
    class GetMyDraftsTests {

        @Test
        void testGetMyDrafts_ShouldReturnEventAndRecurringEventDrafts() throws Exception {
            // Arrange
            Event draftEvent = testDataHelper.createAndPersistScheduledEvent(user, "Draft Event");
            draftEvent.setUnconfirmed(true);
            testDataHelper.saveAndFlush(draftEvent);

            RecurringEvent draftRecurringEvent = testDataHelper.createAndPersistRecurringEvent(user, "Draft Recurring Event");
            draftRecurringEvent.setUnconfirmed(true);
            testDataHelper.saveAndFlush(draftRecurringEvent);

            // Act + Assert
            mockMvc.perform(get("/myevents/drafts")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventDrafts").isArray())
                    .andExpect(jsonPath("$.eventDrafts[0].id").value(draftEvent.getId()))
                    .andExpect(jsonPath("$.recurringEventDrafts").isArray())
                    .andExpect(jsonPath("$.recurringEventDrafts[0].id").value(draftRecurringEvent.getId()));
        }

        @Test
        void testUnauthorizedGetMyDrafts_ShouldReturnUnauthorized() throws Exception {
            mockMvc.perform(get("/myevents/drafts"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class DeleteMyDraftsTests {

        @Test
        void testDeleteAllDrafts_ShouldDeleteEventAndRecurringEventDrafts() throws Exception {
            // Arrange
            Event draftEvent = testDataHelper.createAndPersistScheduledEvent(user, "Draft Event");
            draftEvent.setUnconfirmed(true);
            testDataHelper.saveAndFlush(draftEvent);

            RecurringEvent draftRecurringEvent = testDataHelper.createAndPersistRecurringEvent(user, "Draft Recurring Event");
            draftRecurringEvent.setUnconfirmed(true);
            testDataHelper.saveAndFlush(draftRecurringEvent);

            // Act
            mockMvc.perform(delete("/myevents/drafts")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNoContent());

            // Assert
            List<Event> remainingDraftEvents = eventRepository.findAll().stream()
                    .filter(Event::isUnconfirmed)
                    .toList();
            List<RecurringEvent> remainingDraftRecurringEvents = recurringEventRepository.findAll().stream()
                    .filter(RecurringEvent::isUnconfirmed)
                    .toList();

            assertThat(remainingDraftEvents).isEmpty();
            assertThat(remainingDraftRecurringEvents).isEmpty();
        }

        @Test
        void testUnauthorizedDeleteAllDrafts_ShouldReturnUnauthorized() throws Exception {
            mockMvc.perform(delete("/myevents/drafts"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void testDeleteAllDrafts_WhenNoDraftsExist_ShouldReturnNoContent() throws Exception {
            mockMvc.perform(delete("/myevents/drafts")
                            .header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isNoContent());
        }
    }
}
